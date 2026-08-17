package com.example.carbon.emission.model.service.impl;

import com.example.carbon.emission.model.dto.CreateNodeRequest;
import com.example.carbon.emission.model.dto.NodeConfigDTO;
import com.example.carbon.emission.model.dto.NodeDTO;
import com.example.carbon.emission.model.dto.NodeInfoDTO;
import com.example.carbon.emission.model.dto.UpdateNodeRequest;
import com.example.carbon.emission.model.entity.EmissionNode;
import com.example.carbon.emission.model.entity.EmissionNodeConfig;
import com.example.carbon.emission.model.entity.EmissionNodeInfo;
import com.example.carbon.emission.model.entity.NodeType;
import com.example.carbon.emission.model.entity.SystemConfig;
import com.example.carbon.emission.model.entity.Template;
import com.example.carbon.emission.model.repository.EmissionNodeConfigRepository;
import com.example.carbon.emission.model.repository.EmissionNodeInfoRepository;
import com.example.carbon.emission.model.repository.EmissionNodeRepository;
import com.example.carbon.emission.model.repository.NodeTypeRepository;
import com.example.carbon.emission.model.repository.SystemConfigRepository;
import com.example.carbon.emission.model.repository.TemplateRepository;
import com.example.carbon.emission.model.service.EmissionNodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 排放节点服务实现类
 * 
 * 提供节点的增删改查、配置管理以及节点移动等功能。
 * 支持树形结构的节点管理，处理节点配置和节点信息的关联操作。
 */
@Service
public class EmissionNodeServiceImpl implements EmissionNodeService {
    
    @Autowired
    private EmissionNodeRepository nodeRepository;
    
    @Autowired
    private EmissionNodeConfigRepository configRepository;
    
    @Autowired
    private NodeTypeRepository typeRepository;
    
    @Autowired
    private TemplateRepository templateRepository;
    
    @Autowired
    private EmissionNodeInfoRepository nodeInfoRepository;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private SystemConfigRepository systemConfigRepository;
    
    /**
     * 排放类别与子类别的映射关系
     * 键为排放大类名称，值为对应的子类列表
     */
    private static final Map<String, List<String>> EMISSION_SUBCATEGORIES = Map.of(
        "购入的电力", List.of("生产设施用电", "辅助生产系统用电", "附属生产系统用电"),
        "购入的热力", List.of("热力数据", "质量单位计量的热水", "质量单位计量的蒸汽"),
        "化石燃料", List.of("汽油", "柴油", "原油", "燃料油", "液化天然气", "液化石油气", "天然气", "高炉煤气", "转炉煤气", "焦炉煤气", "烟煤", "褐煤", "焦炭", "石油焦"),
        "废弃物处理", List.of("固体废弃物处理排放", "废水处理排放")
    );
    
    /**
     * 排放数据大类编码映射
     * 键为排放大类名称，值为对应的编码
     */
    private static final Map<String, String> EMISSION_CATEGORY_CODES = Map.of(
        "购入的电力", "EP",
        "购入的热力", "HE",
        "输出的电力", "EO",
        "化石燃料", "FF",
        "废弃物处理", "WD",
        "废物处理", "WD",
        "电力", "EP"
    );
    
    @Override
    public NodeDTO getTree() {
        return getTree(null);
    }
    
    public NodeDTO getTree(Long templateId) {
        List<EmissionNode> allNodes;
        if (templateId != null) {
            allNodes = nodeRepository.findByTemplateId(templateId);
        } else {
            allNodes = nodeRepository.findAll();
        }
        
        EmissionNode root = allNodes.stream()
            .filter(n -> n.getParentId() == null)
            .findFirst()
            .orElseGet(() -> createRootNode(templateId));
        
        if (templateId != null) {
            allNodes = nodeRepository.findByTemplateId(templateId);
        } else {
            allNodes = nodeRepository.findAll();
        }
        
        Map<Long, List<EmissionNode>> childrenMap = allNodes.stream()
            .filter(n -> n.getParentId() != null)
            .collect(Collectors.groupingBy(EmissionNode::getParentId));
        
        Map<Integer, NodeType> typeMap = typeRepository.findAll().stream()
            .collect(Collectors.toMap(NodeType::getId, t -> t));
        
        return buildTree(root, childrenMap, typeMap);
    }
    
    private EmissionNode createRootNode() {
        return createRootNode(null);
    }
    
    private EmissionNode createRootNode(Long templateId) {
        NodeType rootType = typeRepository.findByTypeName("root")
            .orElseThrow(() -> new RuntimeException("根节点类型不存在"));
        
        EmissionNode root = new EmissionNode();
        root.setName("碳排放核算");
        root.setTypeId(rootType.getId());
        root.setParentId(null);
        root.setTemplateId(templateId);
        root.setSortOrder(0);
        
        return nodeRepository.save(root);
    }
    
    private NodeDTO buildTree(EmissionNode node, Map<Long, List<EmissionNode>> childrenMap, Map<Integer, NodeType> typeMap) {
        NodeDTO dto = new NodeDTO();
        dto.setId(node.getId());
        dto.setName(node.getName());
        dto.setTypeId(node.getTypeId());
        dto.setParentId(node.getParentId());
        dto.setLocomotiveType(node.getLocomotiveType());
        
        NodeType type = typeMap.get(node.getTypeId());
        if (type != null) {
            dto.setTypeName(type.getTypeName());
            dto.setCanHaveChildren(type.getCanHaveChildren());
        }
        
        dto.setConfig(getNodeConfigInternal(node.getId()));
        dto.setNodeInfo(getNodeInfoInternal(node.getId()));
        
        List<EmissionNode> children = childrenMap.getOrDefault(node.getId(), new ArrayList<>());
        if (!children.isEmpty()) {
            dto.setChildren(children.stream()
            .sorted((a, b) -> {
                Integer sortOrderA = a.getSortOrder();
                Integer sortOrderB = b.getSortOrder();
                return Integer.compare(sortOrderA != null ? sortOrderA : 0, sortOrderB != null ? sortOrderB : 0);
            })
            .map(child -> buildTree(child, childrenMap, typeMap))
            .collect(Collectors.toList()));
        }
        
        return dto;
    }
    
    /**
     * 获取节点信息内部方法
     * 
     * 根据节点ID查询emission_node_info表，将实体转换为DTO返回。
     * 
     * @param nodeId 节点ID
     * @return NodeInfoDTO 对象，包含节点编码、简称、核算标识等信息；未找到返回null
     */
    private NodeInfoDTO getNodeInfoInternal(Long nodeId) {
        return nodeInfoRepository.findByNodeId(nodeId)
            .map(info -> {
                NodeInfoDTO dto = new NodeInfoDTO();
                dto.setNodeId(info.getNodeId());
                dto.setNodeCode(info.getNodeCode());
                dto.setShortName(info.getShortName());
                dto.setIncludeInCalculation(info.getIncludeInCalculation());
                dto.setNodeCategory(info.getNodeCategory());
                dto.setUnitDescription(info.getUnitDescription());
                dto.setOrgBoundaryDescription(info.getOrgBoundaryDescription());
                dto.setOperationBoundaryDescription(info.getOperationBoundaryDescription());
                return dto;
            })
            .orElse(null);
    }
    
    @Override
    public NodeDTO getNodeById(Long id) {
        EmissionNode node = nodeRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("节点不存在: " + id));
        
        NodeType type = typeRepository.findById(node.getTypeId())
            .orElse(null);
        
        NodeDTO dto = new NodeDTO();
        dto.setId(node.getId());
        dto.setName(node.getName());
        dto.setTypeId(node.getTypeId());
        dto.setParentId(node.getParentId());
        dto.setLocomotiveType(node.getLocomotiveType());
        
        if (type != null) {
            dto.setTypeName(type.getTypeName());
            dto.setCanHaveChildren(type.getCanHaveChildren());
        }
        
        dto.setConfig(getNodeConfigInternal(node.getId()));
        dto.setNodeInfo(getNodeInfoInternal(node.getId()));
        
        return dto;
    }
    
    @Override
    @Transactional
    public NodeDTO createNode(CreateNodeRequest request) {
        NodeType type = typeRepository.findById(request.getTypeId())
            .orElseThrow(() -> new RuntimeException("节点类型不存在: " + request.getTypeId()));
        
        EmissionNode parent = null;
        Long templateId = request.getTemplateId();
        if (request.getParentId() != null) {
            parent = nodeRepository.findById(request.getParentId())
                .orElseThrow(() -> new RuntimeException("父节点不存在: " + request.getParentId()));
            templateId = parent.getTemplateId();
        }
        
        EmissionNode node = new EmissionNode();
        node.setName(request.getName());
        node.setTypeId(request.getTypeId());
        node.setParentId(request.getParentId());
        node.setTemplateId(templateId);
        node.setLocomotiveType(request.getLocomotiveType());
        Long createdBy = request.getCreatedBy();
        node.setCreatedBy(createdBy);
        node.setUpdatedBy(createdBy);
        
        if (templateId != null) {
            node.setSortOrder(nodeRepository.countByTemplateIdAndParentId(templateId, request.getParentId()).intValue());
        } else {
            node.setSortOrder(nodeRepository.countByParentId(request.getParentId()).intValue());
        }
        
        EmissionNode savedNode = nodeRepository.save(node);
        
        if (request.getConfig() != null) {
            saveConfig(savedNode.getId(), request.getConfig(), request.getCreatedBy());
        }
        
        if (request.getTypeId() == 2 && request.getNodeInfo() != null) {
            saveNodeInfo(savedNode.getId(), request.getNodeInfo(), request.getCreatedBy());
        }
        
        if (request.getTypeId() == 4) {
            createTransportChildNodes(savedNode, request.getCreatedBy());
        }
        
        updateTemplateTimestamp(templateId);
        
        return getNodeById(savedNode.getId());
    }
    
    /**
     * 保存节点信息
     * 
     * 根据节点ID查找现有信息，不存在则创建新记录。
     * 将DTO中的字段映射到实体并保存。
     * 
     * @param nodeId 节点ID
     * @param infoDTO 节点信息DTO
     * @param userId 操作用户ID（用于记录创建人和更新人）
     */
    private void saveNodeInfo(Long nodeId, NodeInfoDTO infoDTO, Long userId) {
        // 查找现有记录，不存在则创建新对象
        EmissionNodeInfo info = nodeInfoRepository.findByNodeId(nodeId)
            .orElse(new EmissionNodeInfo());
        
        // 设置基础字段
        info.setNodeId(nodeId);
        info.setNodeCode(infoDTO.getNodeCode());
        info.setShortName(infoDTO.getShortName());
        info.setIncludeInCalculation(infoDTO.getIncludeInCalculation() != null ? infoDTO.getIncludeInCalculation() : true);
        info.setNodeCategory(infoDTO.getNodeCategory());
        info.setUnitDescription(infoDTO.getUnitDescription());
        info.setOrgBoundaryDescription(infoDTO.getOrgBoundaryDescription());
        info.setOperationBoundaryDescription(infoDTO.getOperationBoundaryDescription());
        
        // 设置操作人信息
        if (userId != null) {
            if (info.getId() == null) {
                info.setCreatedBy(userId);
            }
            info.setUpdatedBy(userId);
        }
        
        // 保存到数据库
        nodeInfoRepository.save(info);
    }
    
    private void createTransportChildNodes(EmissionNode transportNode, Long userId) {
        NodeType dataType = typeRepository.findByTypeName("data_collection")
            .orElseThrow(() -> new RuntimeException("数据采集点类型不存在"));
        
        EmissionNode dataNode = new EmissionNode();
        dataNode.setName(transportNode.getName() + "-数据采集点");
        dataNode.setTypeId(dataType.getId());
        dataNode.setParentId(transportNode.getId());
        dataNode.setSortOrder(0);
        dataNode.setCreatedBy(userId);
        dataNode.setUpdatedBy(userId);
        EmissionNode savedDataNode = nodeRepository.save(dataNode);
        
        NodeConfigDTO config = new NodeConfigDTO();
        config.setStatisticalCaliber("生产排放");
        config.setEmissionCategory("化石燃料");
        config.setDataSource("手工录入");
        config.setAllocationRatio(new BigDecimal("100.00"));
        config.setHasSubTable(false);
        
        saveConfig(savedDataNode.getId(), config, userId);
    }
    
    /**
     * 保存节点配置（简化版，不指定操作用户）
     * 
     * @param nodeId 节点ID
     * @param configDTO 配置DTO
     */
    private void saveConfig(Long nodeId, NodeConfigDTO configDTO) {
        saveConfig(nodeId, configDTO, null);
    }
    
    /**
     * 保存节点配置
     * 
     * 根据节点ID查找或创建配置记录，将DTO中的配置数据映射到实体并保存。
     * 支持新增和更新两种操作，通过ID是否存在判断。
     * 
     * @param nodeId 节点ID
     * @param configDTO 配置DTO，包含所有配置字段
     * @param userId 操作用户ID（可选），用于记录创建人和更新人
     */
    private void saveConfig(Long nodeId, NodeConfigDTO configDTO, Long userId) {
        EmissionNodeConfig config = configRepository.findByNodeId(nodeId)
            .orElse(new EmissionNodeConfig());
        
        config.setNodeId(nodeId);
        config.setStatisticalCaliber(configDTO.getStatisticalCaliber());
        config.setEmissionCategory(configDTO.getEmissionCategory());
        config.setEmissionSubcategory(configDTO.getEmissionSubcategory());
        config.setCarbonEmissionFactor(configDTO.getCarbonEmissionFactor());
        config.setCarbonEmissionFactorDescription(configDTO.getCarbonEmissionFactorDescription());
        config.setDataSource(configDTO.getDataSource());
        
        // 新增字段：核算场景、能耗用途、是否累计量、是否移动源
        config.setAccountingScenario(configDTO.getAccountingScenario());
        config.setEnergyUse(configDTO.getEnergyUse());
        config.setIsCumulative(configDTO.getIsCumulative() != null ? configDTO.getIsCumulative() : "true");
        config.setIsMobileSource(configDTO.getIsMobileSource() != null ? configDTO.getIsMobileSource() : "false");
        config.setMeasurementUnit(configDTO.getMeasurementUnit());
        config.setDataSourceSystem(configDTO.getDataSourceSystem());
        config.setAcquisitionMethod(configDTO.getAcquisitionMethod());
        
        config.setAllocationRatio(configDTO.getAllocationRatio() != null ? configDTO.getAllocationRatio() : new BigDecimal("100.00"));
        config.setHasSubTable(configDTO.getHasSubTable() != null ? configDTO.getHasSubTable() : false);
        config.setErrorConstraint(configDTO.getErrorConstraint());
        config.setUpdateCycle(configDTO.getUpdateCycle());
        config.setUpdateTime(configDTO.getUpdateTime());
        
        // 序列化任务配置为JSON字符串
        if (configDTO.getTaskConfig() != null) {
            try {
                config.setTaskConfig(objectMapper.writeValueAsString(configDTO.getTaskConfig()));
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to serialize taskConfig", e);
            }
        } else {
            config.setTaskConfig(null);
        }
        
        config.setCollectionDescription(configDTO.getCollectionDescription());
        config.setEquipmentCode(configDTO.getEquipmentCode());
        //config.setMeasurementUnit(configDTO.getMeasurementUnit());
        
        // 设置创建人和更新人
        if (userId != null) {
            if (config.getId() == null) {
                config.setCreatedBy(userId);
            }
            config.setUpdatedBy(userId);
        }
        
        configRepository.save(config);
    }
    
    @Override
    @Transactional
    public NodeDTO updateNode(Long id, UpdateNodeRequest request) {
        EmissionNode node = nodeRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("节点不存在: " + id));
        
        Long templateId = node.getTemplateId();
        
        if (request.getName() != null) {
            node.setName(request.getName());
        }
        Long updatedBy = request.getUpdatedBy();
        node.setUpdatedBy(updatedBy);
        
        EmissionNode savedNode = nodeRepository.save(node);
        
        if (request.getConfig() != null) {
            saveConfig(savedNode.getId(), request.getConfig(), request.getUpdatedBy());
        }
        
        if (request.getNodeInfo() != null) {
            saveNodeInfo(savedNode.getId(), request.getNodeInfo(), request.getUpdatedBy());
        }
        
        updateTemplateTimestamp(templateId);
        
        return getNodeById(savedNode.getId());
    }
    
    @Override
    @Transactional
    public void deleteNode(Long id) {
        EmissionNode node = nodeRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("节点不存在: " + id));
        
        Long templateId = node.getTemplateId();
        
        nodeRepository.deleteById(id);
        
        updateTemplateTimestamp(templateId);
    }
    
    @Override
    public NodeConfigDTO getNodeConfig(Long nodeId) {
        return getNodeConfigInternal(nodeId);
    }
    
    private NodeConfigDTO getNodeConfigInternal(Long nodeId) {
        return configRepository.findByNodeId(nodeId)
            .map(this::convertToDTO)
            .orElse(null);
    }
    
    @Override
    @Transactional
    public NodeConfigDTO updateNodeConfig(Long nodeId, NodeConfigDTO configDTO, Long userId) {
        saveConfig(nodeId, configDTO, userId);
        return getNodeConfigInternal(nodeId);
    }
    
    private NodeConfigDTO convertToDTO(EmissionNodeConfig config) {
        NodeConfigDTO dto = new NodeConfigDTO();
        dto.setNodeId(config.getNodeId());
        dto.setStatisticalCaliber(config.getStatisticalCaliber());
        dto.setEmissionCategory(config.getEmissionCategory());
        dto.setEmissionSubcategory(config.getEmissionSubcategory());
        dto.setCarbonEmissionFactor(config.getCarbonEmissionFactor());
        dto.setCarbonEmissionFactorDescription(config.getCarbonEmissionFactorDescription());
        dto.setDataSource(config.getDataSource());
        dto.setAccountingScenario(config.getAccountingScenario());
        dto.setEnergyUse(config.getEnergyUse());
        dto.setIsCumulative(config.getIsCumulative());
        dto.setIsMobileSource(config.getIsMobileSource());
        dto.setMeasurementUnit(config.getMeasurementUnit());
        dto.setDataSourceSystem(config.getDataSourceSystem());
        dto.setAcquisitionMethod(config.getAcquisitionMethod());
        dto.setAllocationRatio(config.getAllocationRatio());
        dto.setHasSubTable(config.getHasSubTable());
        dto.setErrorConstraint(config.getErrorConstraint());
        dto.setUpdateCycle(config.getUpdateCycle());
        dto.setUpdateTime(config.getUpdateTime());
        
        if (config.getTaskConfig() != null && !config.getTaskConfig().isEmpty()) {
            try {
                dto.setTaskConfig(objectMapper.readValue(config.getTaskConfig(), Object.class));
            } catch (JsonProcessingException e) {
                dto.setTaskConfig(config.getTaskConfig());
            }
        }
        
        dto.setCollectionDescription(config.getCollectionDescription());
        dto.setEquipmentCode(config.getEquipmentCode());
        //dto.setMeasurementUnit(config.getMeasurementUnit());
        
        return dto;
    }
    
    private void updateTemplateTimestamp(Long templateId) {
        if (templateId != null) {
            templateRepository.findById(templateId).ifPresent(template -> {
                template.setUpdatedAt(java.time.LocalDateTime.now());
                Integer version = template.getVersion();
                template.setVersion(version != null ? version + 1 : 1);
                templateRepository.save(template);
            });
        }
    }
    
    @Override
    @Transactional
    public void moveNode(Long nodeId, String direction) {
        EmissionNode node = nodeRepository.findById(nodeId)
            .orElseThrow(() -> new RuntimeException("节点不存在: " + nodeId));
        
        List<EmissionNode> siblings;
        if (node.getTemplateId() != null) {
            siblings = nodeRepository.findByTemplateIdAndParentId(node.getTemplateId(), node.getParentId());
        } else {
            siblings = nodeRepository.findByParentId(node.getParentId());
        }
        
        siblings.sort((a, b) -> {
            Integer sortOrderA = a.getSortOrder();
            Integer sortOrderB = b.getSortOrder();
            return Integer.compare(sortOrderA != null ? sortOrderA : 0, sortOrderB != null ? sortOrderB : 0);
        });
        
        int currentIndex = -1;
        for (int i = 0; i < siblings.size(); i++) {
            if (siblings.get(i).getId().equals(nodeId)) {
                currentIndex = i;
                break;
            }
        }
        
        if (currentIndex == -1) {
            return;
        }
        
        int newIndex;
        if ("up".equals(direction) && currentIndex > 0) {
            newIndex = currentIndex - 1;
        } else if ("down".equals(direction) && currentIndex < siblings.size() - 1) {
            newIndex = currentIndex + 1;
        } else {
            return;
        }
        
        EmissionNode other = siblings.get(newIndex);
        Integer nodeSortOrder = node.getSortOrder() != null ? node.getSortOrder() : 0;
        Integer otherSortOrder = other.getSortOrder() != null ? other.getSortOrder() : 0;
        node.setSortOrder(otherSortOrder);
        other.setSortOrder(nodeSortOrder);
        
        nodeRepository.save(node);
        nodeRepository.save(other);
        
        updateTemplateTimestamp(node.getTemplateId());
    }
    
    /**
     * 获取下一个可用的节点ID（当前数据库最大ID+1）
     * 
     * @return 下一个可用的节点ID，如果数据库为空则返回1
     */
    @Override
    public Long getNextNodeId() {
        Long maxId = nodeRepository.findMaxId();
        return maxId != null ? maxId + 1 : 1;
    }
    
    /**
     * 生成采集设备编号
     * 
     * 根据系统配置的编码规则（Auto_coding_rules）生成设备编号，支持4种编码规则：
     * 规则1: 一级编码-ID
     * 规则2: 一级编码-二级编码-ID
     * 规则3: 一级编码-大类编码-ID
     * 规则4: 一级编码-二级编码-大类编码-ID
     * 
     * @param nodeId 采集点节点ID或父节点ID
     * @param emissionCategory 排放数据大类（如"化石燃料"、"电力"等）
     * @param newNodeId 新节点ID（可选，用于新建节点时生成编号）
     * @return 生成的设备编号字符串
     */
    @Override
    public String generateEquipmentCode(Long nodeId, String emissionCategory, Long newNodeId) {
        EmissionNode parent = nodeRepository.findById(nodeId).orElse(null);
        
        String codingRule = systemConfigRepository.findByConfigKey("Auto_coding_rules")
            .map(SystemConfig::getConfigValue)
            .orElse("4");
        
        Long actualNodeId = newNodeId != null ? newNodeId : nodeId;
        String idStr = String.format("%06d", actualNodeId);
        if (idStr.length() > 6) {
            idStr = idStr.substring(idStr.length() - 6);
        }
        
        String firstLevelCode = "";
        String secondLevelCode = "";
        String parentCode = "";
        
        if (parent != null) {
            if (parent.getParentId() == null) {
                firstLevelCode = "GE";
                secondLevelCode = "";
            } else {
                EmissionNode firstLevelNode = null;
                EmissionNode secondLevelNode = null;
                EmissionNode current = parent;
                
                while (current != null && current.getParentId() != null) {
                    EmissionNode ancestor = nodeRepository.findById(current.getParentId()).orElse(null);
                    if (ancestor != null && ancestor.getParentId() == null) {
                        firstLevelNode = current;
                        break;
                    }
                    current = ancestor;
                }
                
                if (firstLevelNode != null) {
                    firstLevelCode = nodeInfoRepository.findByNodeId(firstLevelNode.getId())
                        .map(EmissionNodeInfo::getNodeCode)
                        .orElse("");
                    
                    if (parent != firstLevelNode) {
                        current = parent;
                        while (current != null && current.getParentId() != null) {
                            EmissionNode ancestor = nodeRepository.findById(current.getParentId()).orElse(null);
                            if (ancestor != null && ancestor.getId().equals(firstLevelNode.getId())) {
                                secondLevelNode = current;
                                break;
                            }
                            current = ancestor;
                        }
                        
                        if (secondLevelNode != null) {
                            secondLevelCode = nodeInfoRepository.findByNodeId(secondLevelNode.getId())
                                .map(EmissionNodeInfo::getNodeCode)
                                .orElse("");
                        }
                    }
                }
            }
            parentCode = nodeInfoRepository.findByNodeId(parent.getId())
                .map(EmissionNodeInfo::getNodeCode)
                .orElse("");
        }
        
        String categoryCode = EMISSION_CATEGORY_CODES.getOrDefault(emissionCategory, "");
        
        List<String> parts = new ArrayList<>();
        
        switch (codingRule) {
            case "1":
                if (firstLevelCode != null && !firstLevelCode.isEmpty()) {
                    parts.add(firstLevelCode);
                }
                parts.add(idStr);
                break;
            case "2":
                if (firstLevelCode != null && !firstLevelCode.isEmpty()) {
                    parts.add(firstLevelCode);
                }
                if (secondLevelCode != null && !secondLevelCode.isEmpty()) {
                    parts.add(secondLevelCode);
                }
                parts.add(idStr);
                break;
            case "3":
                if (firstLevelCode != null && !firstLevelCode.isEmpty()) {
                    parts.add(firstLevelCode);
                }
                if (categoryCode != null && !categoryCode.isEmpty()) {
                    parts.add(categoryCode);
                }
                parts.add(idStr);
                break;
            case "4":
            default:
                if (firstLevelCode != null && !firstLevelCode.isEmpty()) {
                    parts.add(firstLevelCode);
                }
                if (secondLevelCode != null && !secondLevelCode.isEmpty()) {
                    parts.add(secondLevelCode);
                }
                if (categoryCode != null && !categoryCode.isEmpty()) {
                    parts.add(categoryCode);
                }
                parts.add(idStr);
                break;
        }
        
        return String.join("-", parts);
    }
    
    /**
     * 获取节点的层级（从根节点开始计算）
     * 
     * @param nodeId 节点ID
     * @return 节点层级，根节点返回0，一级子节点返回1，以此类推
     */
    private int getNodeLevel(Long nodeId) {
        if (nodeId == null) {
            return 0;
        }
        
        int level = 0;
        EmissionNode node = nodeRepository.findById(nodeId).orElse(null);
        while (node != null && node.getParentId() != null) {
            level++;
            node = nodeRepository.findById(node.getParentId()).orElse(null);
        }
        
        return level;
    }
    
    /**
     * 通过父节点ID获取一级节点编码
     * 
     * 向上遍历节点树，找到父节点是根节点的节点（一级子节点），返回其编码
     * 如果父节点直接位于根节点下，则返回"GE"
     * 
     * @param parentId 父节点ID
     * @return 一级节点编码，如果找不到则返回空字符串
     */
    private String getFirstLevelCodeByParentId(Long parentId) {
        if (parentId == null) {
            return "";
        }
        
        EmissionNode current = nodeRepository.findById(parentId).orElse(null);
        if (current == null) {
            return "";
        }
        
        while (current != null && current.getParentId() != null) {
            EmissionNode p = nodeRepository.findById(current.getParentId()).orElse(null);
            if (p == null || p.getParentId() == null) {
                break;
            }
            current = p;
        }
        
        return nodeInfoRepository.findByNodeId(current.getId())
            .map(EmissionNodeInfo::getNodeCode)
            .orElse("");
    }
    
    /**
     * 通过父节点ID获取二级节点编码
     * 
     * 向上遍历节点树，找到父节点是一级节点的节点（二级子节点），返回其编码
     * 如果父节点是一级节点（即父节点的父节点是根节点或null），则返回空字符串（不存在二级节点）
     * 
     * @param parentId 父节点ID
     * @return 二级节点编码，如果找不到或父节点是一级节点则返回空字符串
     */
    private String getSecondLevelCodeByParentId(Long parentId) {
        if (parentId == null) {
            return "";
        }
        
        EmissionNode parent = nodeRepository.findById(parentId).orElse(null);
        if (parent == null) {
            return "";
        }
        
        EmissionNode grandParent = nodeRepository.findById(parent.getParentId()).orElse(null);
        if (grandParent == null || grandParent.getParentId() == null) {
            return "";
        }
        
        EmissionNode current = parent;
        
        while (current != null) {
            EmissionNode p = nodeRepository.findById(current.getParentId()).orElse(null);
            if (p == null) {
                break;
            }
            
            EmissionNode gp = nodeRepository.findById(p.getParentId()).orElse(null);
            
            if (gp == null) {
                break;
            }
            
            if (gp.getParentId() == null) {
                return nodeInfoRepository.findByNodeId(current.getId())
                    .map(EmissionNodeInfo::getNodeCode)
                    .orElse("");
            }
            
            current = p;
        }
        
        return "";
    }
    
    /**
     * 获取节点所在树的一级节点编码
     * 
     * 向上遍历节点树，找到父节点是根节点的节点（一级子节点），返回其编码
     * 如果采集节点直接位于根节点下，则返回"GE"
     * 
     * @param node 节点对象
     * @return 一级节点编码，如果找不到则返回空字符串
     */
    private String getFirstLevelNodeCode(EmissionNode node) {
        if (node == null || node.getParentId() == null) {
            return "";
        }
        
        EmissionNode current = node;
        EmissionNode firstLevel = null;
        
        while (current != null) {
            EmissionNode p = nodeRepository.findById(current.getParentId()).orElse(null);
            if (p == null || p.getParentId() == null) {
                firstLevel = current;
                break;
            }
            current = p;
        }
        
        if (firstLevel == null) {
            return "";
        }
        
        return nodeInfoRepository.findByNodeId(firstLevel.getId())
            .map(EmissionNodeInfo::getNodeCode)
            .orElse("");
    }
    
    /**
     * 获取节点所在树的二级节点编码
     * 
     * 向上遍历节点树，找到父节点是一级节点的节点（二级子节点），返回其编码
     * 如果采集节点位于一级点下（即父节点是一级节点），则返回空字符串（不存在二级节点）
     * 
     * @param node 节点对象
     * @return 二级节点编码，如果找不到或采集节点位于一级点下则返回空字符串
     */
    private String getSecondLevelNodeCode(EmissionNode node) {
        if (node == null || node.getParentId() == null) {
            return "";
        }
        
        EmissionNode parent = nodeRepository.findById(node.getParentId()).orElse(null);
        if (parent != null) {
            EmissionNode grandParent = nodeRepository.findById(parent.getParentId()).orElse(null);
            if (grandParent == null || grandParent.getParentId() == null) {
                return "";
            }
        }
        
        EmissionNode current = node;
        
        while (current != null) {
            EmissionNode p = nodeRepository.findById(current.getParentId()).orElse(null);
            if (p == null) {
                break;
            }
            
            EmissionNode gp = nodeRepository.findById(p.getParentId()).orElse(null);
            
            if (gp == null) {
                break;
            }
            
            if (gp.getParentId() == null) {
                return nodeInfoRepository.findByNodeId(current.getId())
                    .map(EmissionNodeInfo::getNodeCode)
                    .orElse("");
            }
            
            current = p;
        }
        
        return "";
    }
}
