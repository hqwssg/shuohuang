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

import java.util.ArrayList;
import java.util.HashMap;
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
    
    /**
     * 获取所有节点的树形结构（无模版过滤）
     * 
     * @return 节点树结构DTO，包含所有节点信息
     */
    @Override
    public NodeDTO getTree() {
        return getTree(null);
    }
    
    /**
     * 获取指定模版下的节点树形结构
     * 
     * 从数据库查询所有节点数据，构建父子关系映射，
     * 然后递归构建完整的节点树。
     * 如果根节点不存在，会自动创建一个默认根节点。
     * 
     * @param templateId 模版ID，为null时返回所有节点
     * @return 节点树结构DTO
     */
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
    
    /**
     * 创建新节点
     * 
     * 根据请求参数创建节点，包括：
     * 1. 验证节点类型是否存在
     * 2. 如果指定了父节点，从父节点获取模版ID
     * 3. 设置节点基本信息（名称、类型、排序等）
     * 4. 保存节点到数据库
     * 5. 如果有配置信息，保存节点配置
     * 6. 如果是核算子节点(typeId=2)且有节点信息，保存节点信息
     * 7. 如果是运输节点(typeId=4)，自动创建数据采集子节点
     * 8. 更新模版时间戳
     * 
     * @param request 创建节点请求，包含：
     *        - name: 节点名称（必填）
     *        - typeId: 节点类型ID（必填）
     *        - parentId: 父节点ID（可选）
     *        - templateId: 模版ID（可选，若指定父节点则从父节点获取）
     *        - locomotiveType: 机车类型（可选）
     *        - config: 节点配置（可选）
     *        - nodeInfo: 节点信息（核算子节点时必填）
     *        - createdBy: 创建人ID
     * @return 创建后的节点DTO
     */
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
        config.setEmissionCategory("化石燃料");
        
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
        config.setEmissionCategory(configDTO.getEmissionCategory());
        config.setEmissionSubcategory(configDTO.getEmissionSubcategory());
        config.setCarbonEmissionFactor(configDTO.getCarbonEmissionFactor());
        config.setCarbonEmissionFactorDescription(configDTO.getCarbonEmissionFactorDescription());
        
        config.setCollectionDescription(configDTO.getCollectionDescription());
        config.setEquipmentCode(configDTO.getEquipmentCode());
        // 采集点关联信息（排放数据采集点通过"选择采集点"关联）
        config.setCollectionPointType(configDTO.getCollectionPointType());
        config.setCollectionPointId(configDTO.getCollectionPointId());
        
        // 设置创建人和更新人
        if (userId != null) {
            if (config.getId() == null) {
                config.setCreatedBy(userId);
            }
            config.setUpdatedBy(userId);
        }
        
        configRepository.save(config);
    }
    
    /**
     * 更新节点信息
     * 
     * 根据请求参数更新节点的名称、配置和信息。
     * 如果请求中包含config或nodeInfo，则分别更新对应的数据。
     * 更新后会更新模版的时间戳。
     * 
     * @param id 节点ID
     * @param request 更新请求，可包含：
     *        - name: 新的节点名称（可选）
     *        - config: 新的节点配置（可选）
     *        - nodeInfo: 新的节点信息（可选）
     *        - updatedBy: 更新人ID
     * @return 更新后的节点DTO
     */
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
    
    /**
     * 删除节点
     * 
     * 删除指定ID的节点，会级联删除其子节点和相关配置数据。
     * 删除后会更新模版的时间戳。
     * 
     * @param id 要删除的节点ID
     * @throws RuntimeException 当节点不存在时抛出异常
     */
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
        // 节点配置变更同样属于模版内容修改：更新时间戳并重置校验结果
        nodeRepository.findById(nodeId).ifPresent(node -> updateTemplateTimestamp(node.getTemplateId()));
        return getNodeConfigInternal(nodeId);
    }
    
    private NodeConfigDTO convertToDTO(EmissionNodeConfig config) {
        NodeConfigDTO dto = new NodeConfigDTO();
        dto.setNodeId(config.getNodeId());
        dto.setEmissionCategory(config.getEmissionCategory());
        dto.setEmissionSubcategory(config.getEmissionSubcategory());
        dto.setCarbonEmissionFactor(config.getCarbonEmissionFactor());
        dto.setCarbonEmissionFactorDescription(config.getCarbonEmissionFactorDescription());
        
        dto.setCollectionDescription(config.getCollectionDescription());
        dto.setEquipmentCode(config.getEquipmentCode());
        dto.setCollectionPointType(config.getCollectionPointType());
        dto.setCollectionPointId(config.getCollectionPointId());
        
        return dto;
    }
    
    private void updateTemplateTimestamp(Long templateId) {
        if (templateId != null) {
            templateRepository.findById(templateId).ifPresent(template -> {
                template.setUpdatedAt(java.time.LocalDateTime.now());
                Integer version = template.getVersion();
                template.setVersion(version != null ? version + 1 : 1);
                // 模版内容被修改（节点增删改），校验结果自动重置为未检查
                template.setCheckResult(0);
                template.setCheckTime(null);
                template.setCheckMessage(null);
                templateRepository.save(template);
            });
        }
    }
    
    /**
     * 移动节点位置
     * 
     * 在同级节点之间移动节点（上移或下移），通过交换排序顺序实现。
     * 节点只能在同级节点间移动，不能跨级移动。
     * 如果节点已在最前/最后位置，移动操作无效。
     * 
     * @param nodeId 要移动的节点ID
     * @param direction 移动方向："up"（上移）或 "down"（下移）
     * @throws RuntimeException 当节点不存在时抛出异常
     */
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

    /**
     * 挂载节点模版
     *
     * 将指定节点模版（templateType=1）所包含的全部子节点复制到当前模版树中，
     * 挂载到目标父节点（typeId=1/2 的"排放核算点"）之下。
     * 每个被复制的节点都会通过 source_node_id 字段记录其来源节点ID。
     *
     * 实现要点：
     * 1. 校验目标父节点存在且 typeId 为 1（根节点）或 2（核算子节点）
     * 2. 校验来源模版存在且为节点模版（templateType=1）
     * 3. 定位来源模版的根节点（parentId 为 null），复制其全部子节点（不含根节点本身）
     *    到目标父节点之下，递归复制整个子树
     * 4. 通过 nodeIdMap 维护来源节点ID→新节点ID的映射，保证父子关系正确
     * 5. 每个新节点 source_node_id 记录其来源节点ID
     * 6. 同步复制 emission_node_config 与 emission_node_info（核算子节点）
     * 7. 顶层挂载节点 sort_order 从目标父节点下现有最大 sort_order+1 开始递增，
     *    子树内部保持来源 sort_order
     *
     * @param parentId           目标父节点ID（挂载位置）
     * @param sourceTemplateId   节点模版ID（来源模版）
     * @param createdBy          创建人ID
     * @return 挂载后的目标父节点DTO
     */
    @Override
    @Transactional
    public NodeDTO mountNodeTemplate(Long parentId, Long sourceTemplateId, Long createdBy) {
        EmissionNode parent = nodeRepository.findById(parentId)
            .orElseThrow(() -> new RuntimeException("目标父节点不存在: " + parentId));

        if (parent.getTypeId() == null || (parent.getTypeId() != 1 && parent.getTypeId() != 2)) {
            throw new RuntimeException("仅可在根节点或核算子节点下挂载节点模版");
        }

        Long targetTemplateId = parent.getTemplateId();

        Template sourceTemplate = templateRepository.findById(sourceTemplateId)
            .orElseThrow(() -> new RuntimeException("来源节点模版不存在: " + sourceTemplateId));
        if (sourceTemplate.getTemplateType() == null || sourceTemplate.getTemplateType() != 1) {
            throw new RuntimeException("仅可挂载节点模版（templateType=1）");
        }

        List<EmissionNode> sourceNodes = nodeRepository.findByTemplateId(sourceTemplateId);
        if (sourceNodes.isEmpty()) {
            throw new RuntimeException("来源节点模版无任何节点");
        }

        // 定位来源模版的根节点（parentId 为 null）
        EmissionNode sourceRoot = sourceNodes.stream()
            .filter(n -> n.getParentId() == null)
            .findFirst()
            .orElseThrow(() -> new RuntimeException("来源节点模版缺少根节点"));

        // 来源根节点的直接子节点 → 挂载到目标父节点之下
        List<EmissionNode> topLevelSourceNodes = sourceNodes.stream()
            .filter(n -> sourceRoot.getId().equals(n.getParentId()))
            .sorted((a, b) -> {
                Integer sa = a.getSortOrder();
                Integer sb = b.getSortOrder();
                return Integer.compare(sa != null ? sa : 0, sb != null ? sb : 0);
            })
            .collect(Collectors.toList());

        if (topLevelSourceNodes.isEmpty()) {
            // 来源模版根节点下无子节点，无可挂载内容
            return getNodeById(parentId);
        }

        // 目标父节点下现有最大 sort_order，新顶层节点从 maxOrder+1 开始递增
        Integer existingCount = nodeRepository.countByTemplateIdAndParentId(targetTemplateId, parentId);
        int baseSortOrder = existingCount != null ? existingCount : 0;

        // 构建来源节点 parentId→children 索引，供递归复制使用
        Map<Long, List<EmissionNode>> sourceChildrenMap = sourceNodes.stream()
            .filter(n -> n.getParentId() != null)
            .collect(Collectors.groupingBy(EmissionNode::getParentId));

        Map<Long, Long> nodeIdMap = new HashMap<>();
        int[] sortOrderCounter = { baseSortOrder };

        for (EmissionNode topNode : topLevelSourceNodes) {
            copySubtreeNode(topNode, parent.getId(), targetTemplateId, true,
                sortOrderCounter, sourceChildrenMap, nodeIdMap, createdBy);
        }

        updateTemplateTimestamp(targetTemplateId);

        return getNodeById(parentId);
    }

    /**
     * 递归复制来源子树到目标模版
     *
     * @param sourceNode          当前来源节点
     * @param targetParentId      新父节点ID（顶层=目标父节点；子层=nodeIdMap 映射结果）
     * @param targetTemplateId   目标模版ID
     * @param isTopLevel         是否为顶层挂载节点（决定 sort_order 计算方式）
     * @param sortOrderCounter   顶层节点 sort_order 计数器（数组形式以支持可变递增）
     * @param sourceChildrenMap  来源节点 parentId→children 索引
     * @param nodeIdMap           来源ID→新ID 映射（递归过程中累加）
     * @param createdBy          创建人ID
     */
    private void copySubtreeNode(EmissionNode sourceNode, Long targetParentId, Long targetTemplateId,
                                 boolean isTopLevel, int[] sortOrderCounter,
                                 Map<Long, List<EmissionNode>> sourceChildrenMap,
                                 Map<Long, Long> nodeIdMap, Long createdBy) {
        EmissionNode newNode = new EmissionNode();
        newNode.setName(sourceNode.getName());
        newNode.setTypeId(sourceNode.getTypeId());
        newNode.setParentId(targetParentId);
        newNode.setTemplateId(targetTemplateId);
        newNode.setLocomotiveType(sourceNode.getLocomotiveType());
        // 记录来源节点ID，建立复制节点与来源节点的对应关系
        newNode.setSourceNodeId(sourceNode.getId());
        if (isTopLevel) {
            // 顶层节点：递增 sort_order，避免与目标父节点下现有同级节点冲突
            newNode.setSortOrder(sortOrderCounter[0]++);
        } else {
            // 子树内部：保持来源 sort_order
            newNode.setSortOrder(sourceNode.getSortOrder());
        }
        newNode.setCreatedBy(createdBy);
        newNode.setUpdatedBy(createdBy);

        EmissionNode savedNode = nodeRepository.save(newNode);
        nodeIdMap.put(sourceNode.getId(), savedNode.getId());

        // 复制 emission_node_config
        configRepository.findByNodeId(sourceNode.getId()).ifPresent(sourceConfig -> {
            EmissionNodeConfig newConfig = new EmissionNodeConfig();
            newConfig.setNodeId(savedNode.getId());
            newConfig.setEmissionCategory(sourceConfig.getEmissionCategory());
            newConfig.setEmissionSubcategory(sourceConfig.getEmissionSubcategory());
            newConfig.setCarbonEmissionFactor(sourceConfig.getCarbonEmissionFactor());
            newConfig.setCarbonEmissionFactorDescription(sourceConfig.getCarbonEmissionFactorDescription());
            newConfig.setCollectionDescription(sourceConfig.getCollectionDescription());
            newConfig.setEquipmentCode(sourceConfig.getEquipmentCode());
            newConfig.setCollectionPointType(sourceConfig.getCollectionPointType());
            newConfig.setCollectionPointId(sourceConfig.getCollectionPointId());
            newConfig.setCollectionPointStatus(sourceConfig.getCollectionPointStatus());
            newConfig.setCreatedBy(createdBy);
            newConfig.setUpdatedBy(createdBy);
            configRepository.save(newConfig);
        });

        // 复制 emission_node_info（核算子节点 typeId=2 通常携带）
        nodeInfoRepository.findByNodeId(sourceNode.getId()).ifPresent(sourceInfo -> {
            EmissionNodeInfo newInfo = new EmissionNodeInfo();
            newInfo.setNodeId(savedNode.getId());
            newInfo.setNodeCode(sourceInfo.getNodeCode());
            newInfo.setShortName(sourceInfo.getShortName());
            newInfo.setIncludeInCalculation(sourceInfo.getIncludeInCalculation() != null
                ? sourceInfo.getIncludeInCalculation() : true);
            newInfo.setNodeCategory(sourceInfo.getNodeCategory());
            newInfo.setUnitDescription(sourceInfo.getUnitDescription());
            newInfo.setOrgBoundaryDescription(sourceInfo.getOrgBoundaryDescription());
            newInfo.setOperationBoundaryDescription(sourceInfo.getOperationBoundaryDescription());
            newInfo.setCreatedBy(createdBy);
            newInfo.setUpdatedBy(createdBy);
            nodeInfoRepository.save(newInfo);
        });

        // 递归复制子节点
        List<EmissionNode> sourceChildren = sourceChildrenMap.getOrDefault(sourceNode.getId(), new ArrayList<>());
        sourceChildren.sort((a, b) -> {
            Integer sa = a.getSortOrder();
            Integer sb = b.getSortOrder();
            return Integer.compare(sa != null ? sa : 0, sb != null ? sb : 0);
        });
        for (EmissionNode child : sourceChildren) {
            copySubtreeNode(child, savedNode.getId(), targetTemplateId, false,
                sortOrderCounter, sourceChildrenMap, nodeIdMap, createdBy);
        }
    }
}
