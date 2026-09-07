package com.example.carbon.emission.model.service.impl;

import com.example.carbon.emission.model.dto.*;
import com.example.carbon.emission.model.entity.MeterInfo;
import com.example.carbon.emission.model.entity.Point;
import com.example.carbon.emission.model.entity.StationInterval;
import com.example.carbon.emission.model.repository.MeterInfoRepository;
import com.example.carbon.emission.model.repository.PointRepository;
import com.example.carbon.emission.model.repository.StationIntervalRepository;
import com.example.carbon.emission.model.service.MeterSettingsService;
import com.example.carbon.emission.model.service.SystemConfigService;
import com.example.carbon.emission.model.util.PinyinUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.Comparator;

/**
 * 电表设置服务实现类
 * 
 * 实现站点/区间-集中器-电表的树状结构管理，包括：
 * 1. 树的构建与查询
 * 2. 节点的增删改查
 * 3. 复杂的启用/停用级联逻辑
 * 4. 节点在同级内的上移/下移
 */
@Service
public class MeterSettingsServiceImpl implements MeterSettingsService {
    
    @Autowired
    private StationIntervalRepository stationRepository;
    
    @Autowired
    private PointRepository pointRepository;
    
    @Autowired
    private MeterInfoRepository meterRepository;
    
    @Autowired
    private SystemConfigService configService;

    @Autowired
    private ObjectMapper objectMapper;
    
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    // ==================== 树构建 ====================
    
    @Override
    public MeterTreeNodeDTO getTree() {
        MeterTreeNodeDTO root = new MeterTreeNodeDTO();
        root.setId(0L);
        root.setName("电表设置");
        root.setNodeType("root");
        root.setStatus(1);
        root.setStatusText("启用");
        
        // 使用批量查询优化性能
        List<StationInterval> stations = stationRepository.findAllByOrderBySortOrderAsc();
        
        if (stations.isEmpty()) {
            root.setChildren(new ArrayList<>());
            root.setHasChildren(false);
            return root;
        }
        
        // 批量获取所有站点ID
        List<Long> stationIds = stations.stream()
                .map(StationInterval::getId)
                .collect(Collectors.toList());
        
        // 批量查询所有集中器
        List<Point> allConcentrators = pointRepository.findByStationIntervalIdIn(stationIds);
        
        // 按站点ID分组集中器
        Map<Long, List<Point>> concentratorsByStation = allConcentrators.stream()
                .collect(Collectors.groupingBy(Point::getStationIntervalId));
        
        // 获取所有集中器ID
        List<Long> concentratorIds = allConcentrators.stream()
                .map(Point::getId)
                .collect(Collectors.toList());
        
        // 批量查询所有电表
        List<MeterInfo> allMeters = concentratorIds.isEmpty() ? new ArrayList<>() 
                : meterRepository.findByPointIdIn(concentratorIds);
        
        // 按集中器ID分组电表
        Map<Long, List<MeterInfo>> metersByPoint = allMeters.stream()
                .collect(Collectors.groupingBy(MeterInfo::getPointId));
        
        // 构建树结构
        List<MeterTreeNodeDTO> stationNodes = new ArrayList<>();
        
        for (StationInterval station : stations) {
            MeterTreeNodeDTO stationNode = convertToStationNode(station);
            List<Point> concentrators = concentratorsByStation.getOrDefault(station.getId(), new ArrayList<>());
            
            // 按sortOrder排序
            concentrators.sort(Comparator.comparingInt(p -> p.getSortOrder() != null ? p.getSortOrder() : 0));
            
            List<MeterTreeNodeDTO> concentratorNodes = new ArrayList<>();
            
            for (Point concentrator : concentrators) {
                MeterTreeNodeDTO concNode = convertToConcentratorNode(concentrator);
                List<MeterInfo> meters = metersByPoint.getOrDefault(concentrator.getId(), new ArrayList<>());
                // 只处理当前集中器的电表，避免重复
                List<MeterTreeNodeDTO> meterNodes = buildMeterTree(meters, null);
                
                concNode.setChildren(meterNodes);
                concNode.setHasChildren(!meterNodes.isEmpty());
                concentratorNodes.add(concNode);
            }
            
            stationNode.setChildren(concentratorNodes);
            stationNode.setHasChildren(!concentratorNodes.isEmpty());
            stationNodes.add(stationNode);
        }
        
        root.setChildren(stationNodes);
        root.setHasChildren(!stationNodes.isEmpty());
        return root;
    }
    
    /**
     * 构建电表子树（处理电表间的父子关系）
     * 按sortOrder升序排列
     */
    private List<MeterTreeNodeDTO> buildMeterTree(List<MeterInfo> allMeters, Long parentMeterId) {
        List<MeterTreeNodeDTO> result = new ArrayList<>();
        List<MeterInfo> sortedMeters = allMeters.stream()
                .sorted(Comparator.comparingInt(m -> m.getSortOrder() != null ? m.getSortOrder() : 0))
                .collect(Collectors.toList());
        for (MeterInfo meter : sortedMeters) {
            boolean isOrphan = meter.getParentMeterId() == null || meter.getParentMeterId() == 0L;
            if (parentMeterId == null) {
                if (isOrphan) {
                    MeterTreeNodeDTO node = convertToMeterNode(meter);
                    List<MeterTreeNodeDTO> children = buildChildMeters(allMeters, meter.getId());
                    node.setChildren(children);
                    node.setHasChildren(!children.isEmpty());
                    result.add(node);
                }
            } else if (parentMeterId.equals(meter.getParentMeterId())) {
                MeterTreeNodeDTO node = convertToMeterNode(meter);
                List<MeterTreeNodeDTO> children = buildChildMeters(allMeters, meter.getId());
                node.setChildren(children);
                node.setHasChildren(!children.isEmpty());
                result.add(node);
            }
        }
        return result;
    }
    
    /**
     * 递归构建某电表的子电表列表，按sortOrder升序排列
     */
    private List<MeterTreeNodeDTO> buildChildMeters(List<MeterInfo> allMeters, Long parentId) {
        List<MeterTreeNodeDTO> result = new ArrayList<>();
        List<MeterInfo> sortedMeters = allMeters.stream()
                .filter(m -> parentId.equals(m.getParentMeterId()))
                .sorted(Comparator.comparingInt(m -> m.getSortOrder() != null ? m.getSortOrder() : 0))
                .collect(Collectors.toList());
        for (MeterInfo meter : sortedMeters) {
            MeterTreeNodeDTO node = convertToMeterNode(meter);
            List<MeterTreeNodeDTO> children = buildChildMeters(allMeters, meter.getId());
            node.setChildren(children);
            node.setHasChildren(!children.isEmpty());
            result.add(node);
        }
        return result;
    }
    
    // ==================== 节点转换 ====================
    
    /**
     * 将站点/区间实体转换为树节点DTO
     * 
     * @param s 站点/区间实体
     * @return 树节点DTO，nodeType 为 "station"，包含名称、拼音编码、状态、描述等字段
     */
    private MeterTreeNodeDTO convertToStationNode(StationInterval s) {
        MeterTreeNodeDTO dto = new MeterTreeNodeDTO();
        dto.setId(s.getId());
        dto.setName(s.getName());
        dto.setNodeType("station");
        dto.setStatus(s.getStatus() != null ? s.getStatus() : 1);
        dto.setStatusText(dto.getStatus() == 1 ? "启用" : "停用");
        dto.setPinyinCode(s.getPinyinCode());
        dto.setDescription(s.getDescription());
        dto.setCreatedAt(s.getCreatedAt() != null ? s.getCreatedAt().format(formatter) : null);
        dto.setUpdatedAt(s.getUpdatedAt() != null ? s.getUpdatedAt().format(formatter) : null);
        return dto;
    }
    
    /**
     * 将集中器实体转换为树节点DTO
     * 
     * @param p 集中器实体
     * @return 树节点DTO，nodeType 为 "concentrator"，parentId 指向所属站点/区间ID
     */
    private MeterTreeNodeDTO convertToConcentratorNode(Point p) {
        MeterTreeNodeDTO dto = new MeterTreeNodeDTO();
        dto.setId(p.getId());
        dto.setName(p.getName());
        dto.setNodeType("concentrator");
        dto.setStatus(p.getStatus() != null ? p.getStatus() : 1);
        dto.setStatusText(dto.getStatus() == 1 ? "启用" : "停用");
        dto.setPinyinCode(p.getPinyinCode());
        dto.setDescription(p.getDescription());
        dto.setParentId(p.getStationIntervalId());
        dto.setTransformerCapacity(p.getTransformerCapacity());
        dto.setConcentratorAddress(p.getConcentratorAddress());
        dto.setCreatedAt(p.getCreatedAt() != null ? p.getCreatedAt().format(formatter) : null);
        dto.setUpdatedAt(p.getUpdatedAt() != null ? p.getUpdatedAt().format(formatter) : null);
        return dto;
    }
    
    /**
     * 将电表实体转换为树节点DTO
     * 
     * @param m 电表实体
     * @return 树节点DTO，nodeType 为 "meter"，parentId 指向上级电表ID，pointId 指向所属集中器ID
     */
    private MeterTreeNodeDTO convertToMeterNode(MeterInfo m) {
        MeterTreeNodeDTO dto = new MeterTreeNodeDTO();
        dto.setId(m.getId());
        dto.setName(m.getName());
        dto.setNodeType("meter");
        dto.setStatus(m.getStatus() != null ? m.getStatus() : 1);
        dto.setStatusText(dto.getStatus() == 1 ? "启用" : "停用");
        dto.setPinyinCode(m.getPinyinCode());
        // MeterInfo 没有 description 字段，用 purposeDescription 作为描述展示
        dto.setDescription(m.getPurposeDescription());
        dto.setParentId(m.getParentMeterId());
        dto.setPointId(m.getPointId());
        dto.setMeterType(m.getMeterType());
        dto.setMeterModel(m.getMeterModel());
        dto.setMeterAddress(m.getMeterAddress());
        dto.setPowerCategory(m.getPowerCategory());
        dto.setEnergyAllocation(m.getEnergyAllocation());
        dto.setMeterReadingMethod(m.getMeterReadingMethod() != null ? m.getMeterReadingMethod() : 1);
        dto.setAutoMeterReadingConfig(m.getAutoMeterReadingConfig());
        dto.setIsCumulative(m.getIsCumulative());
        dto.setIsMobileSource(m.getIsMobileSource());
        dto.setMeasurementUnit(m.getMeasurementUnit());
        dto.setDataSourceSystem(m.getDataSourceSystem());
        dto.setEmissionSubcategory(m.getEmissionSubcategory());
        dto.setBillingCycleUnit(m.getBillingCycleUnit());
        dto.setBillingCycleStartDate(m.getBillingCycleStartDate());
        dto.setBillingCycleLength(m.getBillingCycleLength());
        dto.setEnergyCategoryL1(m.getEnergyCategoryL1());
        dto.setEnergyCategoryL2(m.getEnergyCategoryL2());
        dto.setEnergyCategoryL3(m.getEnergyCategoryL3());
        dto.setEnergyUseCategory(m.getEnergyUseCategory());
        dto.setParentChildRelationship(m.getParentChildRelationship());
        dto.setIsVirtualMeter(m.getIsVirtualMeter());
        dto.setIsAllocationChild(m.getIsAllocationChild());
        dto.setAllocationRatio(m.getAllocationRatio());
        dto.setSortOrder(m.getSortOrder());
        dto.setCreatedAt(m.getCreatedAt() != null ? m.getCreatedAt().format(formatter) : null);
        dto.setUpdatedAt(m.getUpdatedAt() != null ? m.getUpdatedAt().format(formatter) : null);
        return dto;
    }
    
    // ==================== 详情查询 ====================
    
    @Override
    public MeterTreeNodeDTO getNodeDetail(String nodeType, Long id) {
        switch (nodeType) {
            case "station":
                return stationRepository.findById(id)
                    .map(this::convertToStationNode)
                    .orElseThrow(() -> new RuntimeException("站点/区间不存在: " + id));
            case "concentrator":
                return pointRepository.findById(id)
                    .map(this::convertToConcentratorNode)
                    .orElseThrow(() -> new RuntimeException("集中器不存在: " + id));
            case "meter":
                return meterRepository.findById(id)
                    .map(this::convertToMeterNode)
                    .orElseThrow(() -> new RuntimeException("电表不存在: " + id));
            default:
                throw new RuntimeException("未知的节点类型: " + nodeType);
        }
    }
    
    // ==================== 创建 ====================
    
    @Override
    @Transactional
    public MeterTreeNodeDTO createStation(CreateStationRequest request) {
        if (stationRepository.existsByName(request.getName())) {
            throw new RuntimeException("站点/区间名称已存在: " + request.getName());
        }
        StationInterval s = new StationInterval();
        s.setName(request.getName());
        s.setPinyinCode(PinyinUtils.generatePinyinCode(request.getName()));
        s.setDescription(request.getDescription());
        s.setCreatedBy(request.getCreatedBy());
        s.setUpdatedBy(request.getCreatedBy());
        s.setStatus(1);
        s.setSortOrder(getNextStationSortOrder());
        StationInterval saved = stationRepository.save(s);
        return convertToStationNode(saved);
    }
    
    @Override
    @Transactional
    public MeterTreeNodeDTO createConcentrator(CreateConcentratorRequest request) {
        StationInterval station = stationRepository.findById(request.getStationIntervalId())
            .orElseThrow(() -> new RuntimeException("站点/区间不存在: " + request.getStationIntervalId()));
        if (pointRepository.existsByName(request.getName())) {
            throw new RuntimeException("集中器名称已存在: " + request.getName());
        }
        Point p = new Point();
        p.setName(request.getName());
        p.setPinyinCode(PinyinUtils.generatePinyinCode(request.getName()));
        p.setStationIntervalId(request.getStationIntervalId());
        p.setTransformerCapacity(request.getTransformerCapacity());
        p.setConcentratorAddress(request.getConcentratorAddress());
        p.setDescription(request.getDescription());
        p.setCreatedBy(request.getCreatedBy());
        p.setUpdatedBy(request.getCreatedBy());
        p.setStatus(1);
        p.setSortOrder(getNextConcentratorSortOrder(request.getStationIntervalId()));
        Point saved = pointRepository.save(p);
        return convertToConcentratorNode(saved);
    }
    
    /**
     * 创建电表（含实体电表、虚拟电表、虚拟分摊子电表）
     * <p>
     * 校验集中器存在性、电表名称唯一性、上级电表与当前电表同属一个集中器后，
     * 将请求字段写入 MeterInfo 实体并保存。对新增字段做 null 安全默认处理：
     * parentChildRelationship 默认 1（总表计数=分表之和），
     * isVirtualMeter 默认 0（实体电表，由前端入口决定是否创建虚拟电表），
     * isAllocationChild 默认 0（非分摊子电表，前端创建分摊子电表时显式置 1），
     * allocationRatio 默认 1.0，energyCategoryL1/L2/L3、energyUseCategory 透传。
     *
     * @param request 电表创建请求（CreateMeterRequest），含电表基础信息与新增的能耗分类/虚拟电表/分摊字段
     * @return 创建成功后的电表树节点 DTO（MeterTreeNodeDTO）
     * @throws RuntimeException 集中器不存在 / 电表名称已存在 / 上级电表不存在 / 上级电表不在同一集中器
     */
    @Override
    @Transactional
    public MeterTreeNodeDTO createMeter(CreateMeterRequest request) {
        Point point = pointRepository.findById(request.getPointId())
            .orElseThrow(() -> new RuntimeException("集中器不存在: " + request.getPointId()));
        if (meterRepository.existsByName(request.getName())) {
            throw new RuntimeException("电表名称已存在: " + request.getName());
        }
        if (request.getParentMeterId() != null && request.getParentMeterId() > 0) {
            MeterInfo parentMeter = meterRepository.findById(request.getParentMeterId())
                .orElseThrow(() -> new RuntimeException("上级电表不存在: " + request.getParentMeterId()));
            if (!parentMeter.getPointId().equals(request.getPointId())) {
                throw new RuntimeException("上级电表必须在同一集中器下");
            }
        }
        MeterInfo m = new MeterInfo();
        m.setName(request.getName());
        m.setPinyinCode(PinyinUtils.generatePinyinCode(request.getName()));
        m.setPointId(request.getPointId());
        m.setParentMeterId(request.getParentMeterId() != null ? request.getParentMeterId() : 0L);
        m.setMeterType(request.getMeterType());
        m.setMeterModel(request.getMeterModel());
        m.setMeterAddress(request.getMeterAddress());
        // description / purposeDescription 兼容：优先 purposeDescription，其次 description
        String purposeDesc = request.getPurposeDescription() != null
                ? request.getPurposeDescription() : request.getDescription();
        m.setPurposeDescription(purposeDesc);
        m.setEnergyAllocation(request.getEnergyAllocation());
        m.setPowerCategory(request.getPowerCategory());
        m.setMeterReadingMethod(request.getMeterReadingMethod() != null ? request.getMeterReadingMethod() : 1);
        m.setParentChildRelationship(request.getParentChildRelationship() != null ? request.getParentChildRelationship() : 1);
        m.setIsVirtualMeter(request.getIsVirtualMeter() != null ? request.getIsVirtualMeter() : 0);
        m.setIsAllocationChild(request.getIsAllocationChild() != null ? request.getIsAllocationChild() : 0);
        m.setAllocationRatio(request.getAllocationRatio() != null ? request.getAllocationRatio() : java.math.BigDecimal.ONE);
        m.setAutoMeterReadingConfig(request.getAutoMeterReadingConfig());
        m.setIsCumulative(request.getIsCumulative() != null ? request.getIsCumulative() : 0);
        m.setIsMobileSource(request.getIsMobileSource() != null ? request.getIsMobileSource() : 0);
        m.setMeasurementUnit(request.getMeasurementUnit());
        m.setDataSourceSystem(request.getDataSourceSystem());
        m.setEmissionSubcategory(request.getEmissionSubcategory() != null ? request.getEmissionSubcategory() : "外购电力");
        m.setBillingCycleUnit(request.getBillingCycleUnit());
        m.setBillingCycleStartDate(request.getBillingCycleStartDate());
        m.setBillingCycleLength(request.getBillingCycleLength() != null ? request.getBillingCycleLength() : 1);
        m.setEnergyCategoryL1(request.getEnergyCategoryL1());
        m.setEnergyCategoryL2(request.getEnergyCategoryL2());
        m.setEnergyCategoryL3(request.getEnergyCategoryL3());
        m.setEnergyUseCategory(request.getEnergyUseCategory());
        m.setCreatedBy(request.getCreatedBy());
        m.setUpdatedBy(request.getCreatedBy());
        m.setStatus(request.getStatus() != null ? request.getStatus() : 1);
        m.setSortOrder(request.getSortOrder() != null
                ? request.getSortOrder()
                : getNextMeterSortOrder(request.getPointId(), request.getParentMeterId()));
        MeterInfo saved = meterRepository.save(m);
        return convertToMeterNode(saved);
    }
    
    // ==================== 更新 ====================

    /**
     * 更新节点（支持 station/concentrator/meter 三种类型）
     * <p>
     * Spring 将 @RequestBody Object 反序列化为 LinkedHashMap，这里通过 ObjectMapper 转为对应的强类型 DTO，
     * 避免 ClassCastException。对于 meter 类型，更新时遵循以下规则：
     * <ul>
     *   <li>parentChildRelationship、isAllocationChild、allocationRatio 可由用户修改</li>
     *   <li>isVirtualMeter 由系统维护，不允许用户通过此接口修改（见内联注释）</li>
     *   <li>energyCategoryL1/L2/L3、energyUseCategory 支持更新</li>
     * </ul>
     *
     * @param nodeType 节点类型：station/concentrator/meter
     * @param id       节点ID
     * @param request  更新请求体（Object，运行时按 nodeType 转为 CreateStationRequest/CreateConcentratorRequest/CreateMeterRequest）
     * @return 更新成功后的树节点 DTO
     * @throws RuntimeException 节点不存在 / 名称已存在
     */
    @Override
    @Transactional
    public MeterTreeNodeDTO updateNode(String nodeType, Long id, Object request) {
        // Spring 将 @RequestBody Object 反序列化为 LinkedHashMap，
        // 这里通过 ObjectMapper 转为对应的强类型 DTO，避免 ClassCastException。
        switch (nodeType) {
            case "station": {
                CreateStationRequest req = objectMapper.convertValue(request, CreateStationRequest.class);
                StationInterval s = stationRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("站点/区间不存在: " + id));
                if (!s.getName().equals(req.getName()) && stationRepository.existsByName(req.getName())) {
                    throw new RuntimeException("站点/区间名称已存在: " + req.getName());
                }
                s.setName(req.getName());
                s.setPinyinCode(PinyinUtils.generatePinyinCode(req.getName()));
                s.setDescription(req.getDescription());
                s.setUpdatedBy(req.getCreatedBy());
                return convertToStationNode(stationRepository.save(s));
            }
            case "concentrator": {
                CreateConcentratorRequest req = objectMapper.convertValue(request, CreateConcentratorRequest.class);
                Point p = pointRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("集中器不存在: " + id));
                if (!p.getName().equals(req.getName()) && pointRepository.existsByName(req.getName())) {
                    throw new RuntimeException("集中器名称已存在: " + req.getName());
                }
                p.setName(req.getName());
                p.setPinyinCode(PinyinUtils.generatePinyinCode(req.getName()));
                p.setTransformerCapacity(req.getTransformerCapacity());
                p.setConcentratorAddress(req.getConcentratorAddress());
                p.setDescription(req.getDescription());
                p.setUpdatedBy(req.getCreatedBy());
                return convertToConcentratorNode(pointRepository.save(p));
            }
            case "meter": {
                CreateMeterRequest req = objectMapper.convertValue(request, CreateMeterRequest.class);
                MeterInfo m = meterRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("电表不存在: " + id));
                if (!m.getName().equals(req.getName()) && meterRepository.existsByName(req.getName())) {
                    throw new RuntimeException("电表名称已存在: " + req.getName());
                }
                m.setName(req.getName());
                m.setPinyinCode(PinyinUtils.generatePinyinCode(req.getName()));
                if (req.getMeterType() != null) m.setMeterType(req.getMeterType());
                if (req.getMeterModel() != null) m.setMeterModel(req.getMeterModel());
                if (req.getMeterAddress() != null) m.setMeterAddress(req.getMeterAddress());
                // description / purposeDescription 兼容
                if (req.getPurposeDescription() != null) {
                    m.setPurposeDescription(req.getPurposeDescription());
                } else if (req.getDescription() != null) {
                    m.setPurposeDescription(req.getDescription());
                }
                if (req.getEnergyAllocation() != null) m.setEnergyAllocation(req.getEnergyAllocation());
                if (req.getPowerCategory() != null) m.setPowerCategory(req.getPowerCategory());
                if (req.getMeterReadingMethod() != null) m.setMeterReadingMethod(req.getMeterReadingMethod());
                if (req.getAutoMeterReadingConfig() != null) m.setAutoMeterReadingConfig(req.getAutoMeterReadingConfig());
                if (req.getIsCumulative() != null) m.setIsCumulative(req.getIsCumulative());
                if (req.getIsMobileSource() != null) m.setIsMobileSource(req.getIsMobileSource());
                if (req.getMeasurementUnit() != null) m.setMeasurementUnit(req.getMeasurementUnit());
                if (req.getDataSourceSystem() != null) m.setDataSourceSystem(req.getDataSourceSystem());
                if (req.getEmissionSubcategory() != null) m.setEmissionSubcategory(req.getEmissionSubcategory());
                if (req.getBillingCycleUnit() != null) m.setBillingCycleUnit(req.getBillingCycleUnit());
                if (req.getBillingCycleStartDate() != null) m.setBillingCycleStartDate(req.getBillingCycleStartDate());
                if (req.getBillingCycleLength() != null) m.setBillingCycleLength(req.getBillingCycleLength());
                if (req.getEnergyCategoryL1() != null) m.setEnergyCategoryL1(req.getEnergyCategoryL1());
                if (req.getEnergyCategoryL2() != null) m.setEnergyCategoryL2(req.getEnergyCategoryL2());
                if (req.getEnergyCategoryL3() != null) m.setEnergyCategoryL3(req.getEnergyCategoryL3());
                if (req.getEnergyUseCategory() != null) m.setEnergyUseCategory(req.getEnergyUseCategory());
                if (req.getParentChildRelationship() != null) m.setParentChildRelationship(req.getParentChildRelationship());
                // isVirtualMeter 不在此更新，由系统维护，不允许用户修改
                if (req.getIsAllocationChild() != null) m.setIsAllocationChild(req.getIsAllocationChild());
                if (req.getAllocationRatio() != null) m.setAllocationRatio(req.getAllocationRatio());
                if (req.getSortOrder() != null) m.setSortOrder(req.getSortOrder());
                if (req.getStatus() != null) m.setStatus(req.getStatus());
                m.setUpdatedBy(req.getCreatedBy());
                return convertToMeterNode(meterRepository.save(m));
            }
            default:
                throw new RuntimeException("未知的节点类型: " + nodeType);
        }
    }
    
    // ==================== 删除 ====================
    
    @Override
    @Transactional
    public void deleteNode(String nodeType, Long id, Long userId) {
        switch (nodeType) {
            case "station": {
                StationInterval s = stationRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("站点/区间不存在: " + id));
                if (s.getStatus() == 1) {
                    throw new RuntimeException("该节点处于启用状态，请先停用后再删除");
                }
                stationRepository.delete(s);
                break;
            }
            case "concentrator": {
                Point p = pointRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("集中器不存在: " + id));
                if (p.getStatus() == 1) {
                    throw new RuntimeException("该节点处于启用状态，请先停用后再删除");
                }
                pointRepository.delete(p);
                break;
            }
            case "meter": {
                MeterInfo m = meterRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("电表不存在: " + id));
                if (m.getStatus() == 1) {
                    throw new RuntimeException("该节点处于启用状态，请先停用后再删除");
                }
                // 先将子电表的parent置为0
                List<MeterInfo> childMeters = meterRepository.findByParentMeterId(id);
                for (MeterInfo child : childMeters) {
                    child.setParentMeterId(0L);
                    meterRepository.save(child);
                }
                meterRepository.delete(m);
                break;
            }
            default:
                throw new RuntimeException("未知的节点类型: " + nodeType);
        }
    }
    
    // ==================== 启用/停用（核心复杂逻辑）====================
    
    @Override
    @Transactional
    public String toggleStatus(String nodeType, Long id, Long userId) {
        switch (nodeType) {
            case "station":
                return toggleStationStatus(id);
            case "concentrator":
                return toggleConcentratorStatus(id);
            case "meter":
                return toggleMeterStatus(id);
            default:
                throw new RuntimeException("未知的节点类型: " + nodeType);
        }
    }
    
    /**
     * 站点/区间启停逻辑
     */
    private String toggleStationStatus(Long id) {
        StationInterval station = stationRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("站点/区间不存在: " + id));
        
        // 原状态为启用 → 执行停用
        if (station.getStatus() == 1) {
            List<Point> concentrators = pointRepository.findByStationIntervalId(id);
            boolean hasEnabledConcentrator = concentrators.stream().anyMatch(c -> c.getStatus() == 1);
            
            if (!hasEnabledConcentrator) {
                // 所有集中器都已停用，可以直接停用站点
                station.setStatus(0);
                stationRepository.save(station);
                return "站点/区间停用成功！";
            }
            
            // 存在启用的集中器，检查配置
            Integer disableAllConcentrators = configService.getConfigInt("Disable_all_concentrators", 0);
            Integer disableAllMeter = configService.getConfigInt("Disable_all_meter", 0);
            
            // 即使Disable_all_concentrators=1，但Disable_all_meter=0也无法一键禁用
            if (disableAllConcentrators == 1 && disableAllMeter == 1) {
                // 一键禁用所有集中器及其下的电表
                for (Point c : concentrators) {
                    if (c.getStatus() == 1) {
                        // 检查集中器下电表
                        List<MeterInfo> meters = meterRepository.findByPointId(c.getId());
                        boolean hasEnabledMeter = meters.stream().anyMatch(m -> m.getStatus() == 1);
                        if (hasEnabledMeter) {
                            for (MeterInfo m : meters) {
                                if (m.getStatus() == 1) {
                                    m.setStatus(0);
                                    meterRepository.save(m);
                                }
                            }
                        }
                        c.setStatus(0);
                        pointRepository.save(c);
                    }
                }
                station.setStatus(0);
                stationRepository.save(station);
                return "站点/区间及其下属所有集中器和电表已全部停用！";
            } else {
                // 无法一键禁用，给出具体提示
                if (disableAllConcentrators == 0) {
                    String names = concentrators.stream()
                        .filter(c -> c.getStatus() == 1)
                        .map(Point::getName)
                        .collect(Collectors.joining("、"));
                    return "区域/站点停用失败！该区域/站点下属的数据集中器还有处于启动状态的（" + names + "），且配置选项Disable_all_concentrators=0，必须把下属所有数据集中器设置为停用状态后，才能停用该区域。";
                } else {
                    // Disable_all_concentrators=1但Disable_all_meter=0
                    StringBuilder msg = new StringBuilder();
                    for (Point c : concentrators) {
                        if (c.getStatus() == 1) {
                            List<MeterInfo> meters = meterRepository.findByPointId(c.getId());
                            boolean hasEnabledMeter = meters.stream().anyMatch(m -> m.getStatus() == 1);
                            if (hasEnabledMeter) {
                                String meterNames = meters.stream()
                                    .filter(m -> m.getStatus() == 1)
                                    .map(MeterInfo::getName)
                                    .collect(Collectors.joining("、"));
                                msg.append("数据集中器停用失败！").append(c.getName())
                                    .append("下面有电表处于启用状态（").append(meterNames)
                                    .append("），且配置选项Disable_all_meter=0，必须把下属所有电表设置为停用状态后，才能停用该数据集中器。\n");
                            } else {
                                c.setStatus(0);
                                pointRepository.save(c);
                            }
                        }
                    }
                    if (msg.length() > 0) {
                        msg.insert(0, "区域/站点停用失败！\n");
                        return msg.toString();
                    } else {
                        station.setStatus(0);
                        stationRepository.save(station);
                        return "站点/区间停用成功！";
                    }
                }
            }
        } else {
            // 原状态为停用 → 执行启用
            station.setStatus(1);
            stationRepository.save(station);
            return "站点/区间启用成功！";
        }
    }
    
    /**
     * 集中器启停逻辑
     */
    private String toggleConcentratorStatus(Long id) {
        Point concentrator = pointRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("集中器不存在: " + id));
        
        if (concentrator.getStatus() == 1) {
            // 执行停用
            List<MeterInfo> meters = meterRepository.findByPointId(id);
            boolean hasEnabledMeter = meters.stream().anyMatch(m -> m.getStatus() == 1);
            
            if (!hasEnabledMeter) {
                concentrator.setStatus(0);
                pointRepository.save(concentrator);
                return "数据集中器停用成功！";
            }
            
            Integer disableAllMeter = configService.getConfigInt("Disable_all_meter", 0);
            if (disableAllMeter == 1) {
                // 一键禁用所有电表
                for (MeterInfo m : meters) {
                    if (m.getStatus() == 1) {
                        m.setStatus(0);
                        meterRepository.save(m);
                    }
                }
                concentrator.setStatus(0);
                pointRepository.save(concentrator);
                return "数据集中器及其下属所有电表已全部停用！";
            } else {
                String names = meters.stream()
                    .filter(m -> m.getStatus() == 1)
                    .map(MeterInfo::getName)
                    .collect(Collectors.joining("、"));
                return "数据集中器停用失败！" + concentrator.getName() + 
                    "下面有电表处于启用状态（" + names + 
                    "），且配置选项Disable_all_meter=0，必须把下属所有电表设置为停用状态后，才能停用该数据集中器。";
            }
        } else {
            concentrator.setStatus(1);
            pointRepository.save(concentrator);
            return "数据集中器启用成功！";
        }
    }
    
    /**
     * 电表启停逻辑
     */
    private String toggleMeterStatus(Long id) {
        MeterInfo meter = meterRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("电表不存在: " + id));
        
        if (meter.getStatus() == 1) {
            // 执行停用 - 先检查子电表
            List<MeterInfo> childMeters = meterRepository.findByParentMeterId(id);
            boolean hasEnabledChild = childMeters.stream().anyMatch(m -> m.getStatus() == 1);
            
            if (!hasEnabledChild) {
                meter.setStatus(0);
                meterRepository.save(meter);
                return "电表停用成功！";
            }
            
            Integer disableAllMeter = configService.getConfigInt("Disable_all_meter", 0);
            if (disableAllMeter == 1) {
                for (MeterInfo child : childMeters) {
                    if (child.getStatus() == 1) {
                        child.setStatus(0);
                        meterRepository.save(child);
                    }
                }
                meter.setStatus(0);
                meterRepository.save(meter);
                return "电表及其下属所有子表已全部停用！";
            } else {
                String names = childMeters.stream()
                    .filter(m -> m.getStatus() == 1)
                    .map(MeterInfo::getName)
                    .collect(Collectors.joining("、"));
                return "电表停用失败！" + meter.getName() + 
                    "下面有下级电表处于启用状态（" + names + 
                    "），且配置选项Disable_all_meter=0，必须把下属所有电表设置为停用状态后，才能停用电表。";
            }
        } else {
            meter.setStatus(1);
            meterRepository.save(meter);
            return "电表启用成功！";
        }
    }
    
    // ==================== 上移/下移 ====================
    
    @Override
    @Transactional
    public void moveUp(String nodeType, Long id, Long userId) {
        switch (nodeType) {
            case "station":
                moveStationUp(id);
                break;
            case "concentrator":
                moveConcentratorUp(id);
                break;
            case "meter":
                moveMeterUp(id);
                break;
            default:
                throw new RuntimeException("未知的节点类型: " + nodeType);
        }
    }
    
    @Override
    @Transactional
    public void moveDown(String nodeType, Long id, Long userId) {
        switch (nodeType) {
            case "station":
                moveStationDown(id);
                break;
            case "concentrator":
                moveConcentratorDown(id);
                break;
            case "meter":
                moveMeterDown(id);
                break;
            default:
                throw new RuntimeException("未知的节点类型: " + nodeType);
        }
    }
    
    /**
     * 站点上移：与前一个站点交换sortOrder
     */
    private void moveStationUp(Long id) {
        StationInterval current = stationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("站点/区间不存在: " + id));
        
        List<StationInterval> allStations = stationRepository.findAllByOrderBySortOrderAsc();
        int index = findStationIndex(allStations, id);
        
        if (index <= 0) {
            throw new RuntimeException("已经是第一个节点，无法上移");
        }
        
        StationInterval prev = allStations.get(index - 1);
        swapStationSortOrder(current, prev);
    }
    
    /**
     * 站点下移：与后一个站点交换sortOrder
     */
    private void moveStationDown(Long id) {
        StationInterval current = stationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("站点/区间不存在: " + id));
        
        List<StationInterval> allStations = stationRepository.findAllByOrderBySortOrderAsc();
        int index = findStationIndex(allStations, id);
        
        if (index >= allStations.size() - 1) {
            throw new RuntimeException("已经是最后一个节点，无法下移");
        }
        
        StationInterval next = allStations.get(index + 1);
        swapStationSortOrder(current, next);
    }
    
    /**
     * 集中器上移：与同站点下前一个集中器交换sortOrder
     */
    private void moveConcentratorUp(Long id) {
        Point current = pointRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("集中器不存在: " + id));
        
        List<Point> siblings = pointRepository.findByStationIntervalIdOrderBySortOrderAsc(current.getStationIntervalId());
        int index = findConcentratorIndex(siblings, id);
        
        if (index <= 0) {
            throw new RuntimeException("已经是第一个节点，无法上移");
        }
        
        Point prev = siblings.get(index - 1);
        swapConcentratorSortOrder(current, prev);
    }
    
    /**
     * 集中器下移：与同站点下后一个集中器交换sortOrder
     */
    private void moveConcentratorDown(Long id) {
        Point current = pointRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("集中器不存在: " + id));
        
        List<Point> siblings = pointRepository.findByStationIntervalIdOrderBySortOrderAsc(current.getStationIntervalId());
        int index = findConcentratorIndex(siblings, id);
        
        if (index >= siblings.size() - 1) {
            throw new RuntimeException("已经是最后一个节点，无法下移");
        }
        
        Point next = siblings.get(index + 1);
        swapConcentratorSortOrder(current, next);
    }
    
    /**
     * 电表上移：与同父节点下前一个电表交换sortOrder
     */
    private void moveMeterUp(Long id) {
        MeterInfo current = meterRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("电表不存在: " + id));
        
        List<MeterInfo> siblings = findMeterSiblings(current);
        int index = findMeterIndex(siblings, id);
        
        if (index <= 0) {
            throw new RuntimeException("已经是第一个节点，无法上移");
        }
        
        MeterInfo prev = siblings.get(index - 1);
        swapMeterSortOrder(current, prev);
    }
    
    /**
     * 电表下移：与同父节点下后一个电表交换sortOrder
     */
    private void moveMeterDown(Long id) {
        MeterInfo current = meterRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("电表不存在: " + id));
        
        List<MeterInfo> siblings = findMeterSiblings(current);
        int index = findMeterIndex(siblings, id);
        
        if (index >= siblings.size() - 1) {
            throw new RuntimeException("已经是最后一个节点，无法下移");
        }
        
        MeterInfo next = siblings.get(index + 1);
        swapMeterSortOrder(current, next);
    }
    
    // ==================== 辅助方法 ====================
    
    /**
     * 查找站点列表中的索引
     */
    private int findStationIndex(List<StationInterval> stations, Long id) {
        for (int i = 0; i < stations.size(); i++) {
            if (stations.get(i).getId().equals(id)) {
                return i;
            }
        }
        return -1;
    }
    
    /**
     * 查找集中器列表中的索引
     */
    private int findConcentratorIndex(List<Point> points, Long id) {
        for (int i = 0; i < points.size(); i++) {
            if (points.get(i).getId().equals(id)) {
                return i;
            }
        }
        return -1;
    }
    
    /**
     * 查找电表列表中的索引
     */
    private int findMeterIndex(List<MeterInfo> meters, Long id) {
        for (int i = 0; i < meters.size(); i++) {
            if (meters.get(i).getId().equals(id)) {
                return i;
            }
        }
        return -1;
    }
    
    /**
     * 获取电表的同级兄弟列表（同父节点下的电表）
     */
    private List<MeterInfo> findMeterSiblings(MeterInfo current) {
        Long parentMeterId = current.getParentMeterId();
        if (parentMeterId == null || parentMeterId == 0L) {
            // 顶级电表，按pointId查找
            return meterRepository.findByPointIdOrderBySortOrderAsc(current.getPointId());
        } else {
            // 有父电表，按parentMeterId查找
            return meterRepository.findByParentMeterIdOrderBySortOrderAsc(parentMeterId);
        }
    }
    
    /**
     * 交换两个站点的sortOrder
     */
    private void swapStationSortOrder(StationInterval a, StationInterval b) {
        Integer tempOrder = a.getSortOrder();
        a.setSortOrder(b.getSortOrder() != null ? b.getSortOrder() : 0);
        b.setSortOrder(tempOrder != null ? tempOrder : 0);
        stationRepository.save(a);
        stationRepository.save(b);
    }
    
    /**
     * 交换两个集中器的sortOrder
     */
    private void swapConcentratorSortOrder(Point a, Point b) {
        Integer tempOrder = a.getSortOrder();
        a.setSortOrder(b.getSortOrder() != null ? b.getSortOrder() : 0);
        b.setSortOrder(tempOrder != null ? tempOrder : 0);
        pointRepository.save(a);
        pointRepository.save(b);
    }
    
    /**
     * 交换两个电表的sortOrder
     */
    private void swapMeterSortOrder(MeterInfo a, MeterInfo b) {
        Integer tempOrder = a.getSortOrder();
        a.setSortOrder(b.getSortOrder() != null ? b.getSortOrder() : 0);
        b.setSortOrder(tempOrder != null ? tempOrder : 0);
        meterRepository.save(a);
        meterRepository.save(b);
    }
    
    /**
     * 获取下一个站点排序值
     */
    private int getNextStationSortOrder() {
        List<StationInterval> all = stationRepository.findAll();
        return all.stream()
                .mapToInt(s -> s.getSortOrder() != null ? s.getSortOrder() : 0)
                .max()
                .orElse(0) + 1;
    }
    
    /**
     * 获取下一个集中器排序值（同一站点下）
     */
    private int getNextConcentratorSortOrder(Long stationIntervalId) {
        List<Point> siblings = pointRepository.findByStationIntervalId(stationIntervalId);
        return siblings.stream()
                .mapToInt(p -> p.getSortOrder() != null ? p.getSortOrder() : 0)
                .max()
                .orElse(0) + 1;
    }
    
    /**
     * 获取下一个电表排序值（同一父节点下）
     */
    private int getNextMeterSortOrder(Long pointId, Long parentMeterId) {
        List<MeterInfo> siblings;
        if (parentMeterId == null || parentMeterId == 0L) {
            siblings = meterRepository.findByPointId(pointId);
        } else {
            siblings = meterRepository.findByParentMeterId(parentMeterId);
        }
        return siblings.stream()
                .mapToInt(m -> m.getSortOrder() != null ? m.getSortOrder() : 0)
                .max()
                .orElse(0) + 1;
    }
    
    // ==================== 更改电表级联关系 ====================
    
    /**
     * 更改电表级联关系（修改电表的上级电表）
     * 实现关键逻辑：
     * 1. 校验新的上级电表是否存在且与当前电表同属一个集中器
     * 2. 校验不能将自身设置为上级电表
     * 3. 通过 isChildMeter 递归校验防止循环引用（不能将子表设为上级）
     * 4. parentMeterId 为 0 或 null 时表示解除上级关系
     * 
     * @param meterId 待修改的电表ID
     * @param request 修改请求，包含新的 parentMeterId 和操作人 userId
     * @return 修改后的电表节点DTO
     * @throws RuntimeException 当电表不存在、上级电表不在同一集中器、循环引用等校验失败时抛出
     */
    @Override
    @Transactional
    public MeterTreeNodeDTO changeMeterCascade(Long meterId, ChangeMeterCascadeRequest request) {
        MeterInfo meter = meterRepository.findById(meterId)
            .orElseThrow(() -> new RuntimeException("电表不存在: " + meterId));
        
        Long newParentMeterId = request.getParentMeterId();
        
        // 如果设置了新的上级电表，验证其有效性
        if (newParentMeterId != null && newParentMeterId > 0) {
            MeterInfo parentMeter = meterRepository.findById(newParentMeterId)
                .orElseThrow(() -> new RuntimeException("上级电表不存在: " + newParentMeterId));
            
            // 验证上级电表必须在同一集中器下
            if (!parentMeter.getPointId().equals(meter.getPointId())) {
                throw new RuntimeException("上级电表必须在同一集中器下");
            }
            
            // 验证不能将自身设置为上级
            if (newParentMeterId.equals(meterId)) {
                throw new RuntimeException("不能将自身设置为上级电表");
            }
            
            // 验证不能将子表设置为上级（防止循环引用）
            if (isChildMeter(meterId, newParentMeterId)) {
                throw new RuntimeException("不能将子表设置为上级电表，会造成循环引用");
            }
        }
        
        meter.setParentMeterId(newParentMeterId != null ? newParentMeterId : 0L);
        meter.setUpdatedBy(request.getUserId());
        MeterInfo saved = meterRepository.save(meter);
        return convertToMeterNode(saved);
    }
    
    /**
     * 检查targetId是否是meterId的子表（递归检查）
     */
    private boolean isChildMeter(Long meterId, Long targetId) {
        List<MeterInfo> children = meterRepository.findByParentMeterId(meterId);
        for (MeterInfo child : children) {
            if (child.getId().equals(targetId)) {
                return true;
            }
            if (isChildMeter(child.getId(), targetId)) {
                return true;
            }
        }
        return false;
    }
    
    // ==================== 更改电表从属数据集中器 ====================

    /**
     * 更改电表的从属数据集中器（同时可修改上级电表）
     * <p>
     * 校验逻辑：
     * 1. 新集中器必须存在
     * 2. 若指定上级电表，则上级电表必须在新集中器下
     * 3. 不能将自身设置为上级
     * 4. 不能将子表设置为上级（防止循环引用）
     * <p>
     * 更新逻辑：
     * - 更新当前电表的 point_id 和 parent_meter_id
     * - 递归更新其下所有子孙电表的 point_id（保证整棵子树的从属集中器一致）
     *
     * @param meterId 电表ID
     * @param request 包含新的 pointId（集中器ID）、parentMeterId（上级电表ID）、userId（操作人）
     * @return 更新后的电表树节点 DTO
     * @throws RuntimeException 当集中器不存在、上级电表不在新集中器下、循环引用等校验失败时抛出
     */
    @Override
    @Transactional
    public MeterTreeNodeDTO changeMeterPoint(Long meterId, ChangeMeterPointRequest request) {
        MeterInfo meter = meterRepository.findById(meterId)
            .orElseThrow(() -> new RuntimeException("电表不存在: " + meterId));
        
        Long newPointId = request.getPointId();
        Long newParentMeterId = request.getParentMeterId();
        
        // 验证新的集中器存在
        Point newPoint = pointRepository.findById(newPointId)
            .orElseThrow(() -> new RuntimeException("集中器不存在: " + newPointId));
        
        // 如果设置了新的上级电表，验证其有效性
        if (newParentMeterId != null && newParentMeterId > 0) {
            MeterInfo parentMeter = meterRepository.findById(newParentMeterId)
                .orElseThrow(() -> new RuntimeException("上级电表不存在: " + newParentMeterId));
            
            // 验证上级电表必须在新的集中器下
            if (!parentMeter.getPointId().equals(newPointId)) {
                throw new RuntimeException("上级电表必须在新的集中器下");
            }
            
            // 验证不能将自身设置为上级
            if (newParentMeterId.equals(meterId)) {
                throw new RuntimeException("不能将自身设置为上级电表");
            }
            
            // 验证不能将子表设置为上级（防止循环引用）
            if (isChildMeter(meterId, newParentMeterId)) {
                throw new RuntimeException("不能将子表设置为上级电表，会造成循环引用");
            }
        }
        
        meter.setPointId(newPointId);
        meter.setParentMeterId(newParentMeterId != null ? newParentMeterId : 0L);
        meter.setUpdatedBy(request.getUserId());
        MeterInfo saved = meterRepository.save(meter);
        // 递归更新所有子节点的所属集中器
        updateChildMetersPoint(meter.getId(), newPointId, request.getUserId());
        return convertToMeterNode(saved);
    }

    /**
     * 递归更新指定电表下所有子电表的所属集中器
     * 当更改某电表的从属集中器时，其下所有子电表也应同步切换到新的集中器
     *
     * @param parentMeterId 父电表ID
     * @param newPointId    新的集中器ID
     * @param userId        操作人ID
     */
    private void updateChildMetersPoint(Long parentMeterId, Long newPointId, Long userId) {
        List<MeterInfo> children = meterRepository.findByParentMeterId(parentMeterId);
        for (MeterInfo child : children) {
            child.setPointId(newPointId);
            child.setUpdatedBy(userId);
            meterRepository.save(child);
            // 递归处理孙节点
            updateChildMetersPoint(child.getId(), newPointId, userId);
        }
    }
    
    // ==================== 查询辅助方法 ====================
    
    /**
     * 获取指定集中器下的所有电表（可排除指定电表）
     * 实现关键逻辑：按 sortOrder 升序查询集中器下电表，再通过 stream 过滤排除指定ID
     * 
     * @param pointId        集中器ID
     * @param excludeMeterId 需排除的电表ID（通常为当前电表自身）；为 null 时不排除任何电表
     * @return 电表节点DTO列表（不含子节点树结构），按 sortOrder 升序排列
     */
    @Override
    public List<MeterTreeNodeDTO> getMetersByPoint(Long pointId, Long excludeMeterId) {
        List<MeterInfo> meters = meterRepository.findByPointIdOrderBySortOrderAsc(pointId);
        return meters.stream()
            .filter(m -> excludeMeterId == null || !m.getId().equals(excludeMeterId))
            .map(this::convertToMeterNode)
            .collect(Collectors.toList());
    }
    
    /**
     * 获取指定站点/区间下的所有集中器
     * 
     * @param stationId 站点/区间ID
     * @return 集中器节点DTO列表（不含子节点树结构），按 sortOrder 升序排列
     */
    @Override
    public List<MeterTreeNodeDTO> getConcentratorsByStation(Long stationId) {
        List<Point> concentrators = pointRepository.findByStationIntervalIdOrderBySortOrderAsc(stationId);
        return concentrators.stream()
            .map(this::convertToConcentratorNode)
            .collect(Collectors.toList());
    }

    // ==================== 自动抄表接口测试 ====================

    /**
     * 测试自动抄表接口
     * 实现策略：
     *   - HTTP_API 类型：使用 Java HttpClient 发起一次 GET 请求，根据响应状态码判断连通性
     *   - MODBUS_TCP / MQTT 类型：校验配置完整性后返回“配置有效”，实际协议连通性测试待接入对应客户端库
     *
     * @param meterId 电表ID（可选，为 null 表示新建模式下的预测试）
     * @param config  抄表接口配置
     * @return 测试结果，包含 success、message、可选 data
     */
    @Override
    public Map<String, Object> testMeterReading(Long meterId, Map<String, Object> config) {
        Map<String, Object> result = new LinkedHashMap<>();

        // 编辑模式下校验电表是否存在
        if (meterId != null) {
            meterRepository.findById(meterId)
                .orElseThrow(() -> new RuntimeException("电表不存在: " + meterId));
        }

        // 1. 基础校验
        if (config == null || config.isEmpty()) {
            result.put("success", false);
            result.put("message", "未提供抄表接口配置");
            return result;
        }
        String interfaceType = String.valueOf(config.getOrDefault("interfaceType", ""));
        String apiUrl = String.valueOf(config.getOrDefault("apiUrl", "")).trim();
        if (apiUrl.isEmpty() || "null".equals(apiUrl)) {
            result.put("success", false);
            result.put("message", "接口地址不能为空");
            return result;
        }

        // 2. 按协议分支处理
        try {
            switch (interfaceType) {
                case "HTTP_API": {
                    int timeoutSec = toInt(config.get("timeout"), 30);
                    java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
                            .connectTimeout(java.time.Duration.ofSeconds(timeoutSec))
                            .build();
                    java.net.http.HttpRequest.Builder reqBuilder = java.net.http.HttpRequest.newBuilder()
                            .uri(java.net.URI.create(apiUrl))
                            .timeout(java.time.Duration.ofSeconds(timeoutSec))
                            .GET();
                    // 认证
                    String authType = String.valueOf(config.getOrDefault("authType", "NONE"));
                    if ("BASIC".equals(authType)) {
                        String user = String.valueOf(config.getOrDefault("username", ""));
                        String pass = String.valueOf(config.getOrDefault("password", ""));
                        String token = java.util.Base64.getEncoder()
                                .encodeToString((user + ":" + pass).getBytes(java.nio.charset.StandardCharsets.UTF_8));
                        reqBuilder.header("Authorization", "Basic " + token);
                    } else if ("TOKEN".equals(authType)) {
                        String token = String.valueOf(config.getOrDefault("token", ""));
                        reqBuilder.header("Authorization", "Bearer " + token);
                    }
                    java.net.http.HttpResponse<String> resp = client.send(
                            reqBuilder.build(),
                            java.net.http.HttpResponse.BodyHandlers.ofString());
                    int code = resp.statusCode();
                    if (code >= 200 && code < 300) {
                        result.put("success", true);
                        result.put("message", "HTTP 接口连通正常，状态码：" + code);
                        result.put("data", resp.body());
                    } else {
                        result.put("success", false);
                        result.put("message", "HTTP 接口返回非 2xx 状态码：" + code);
                    }
                    break;
                }
                case "MODBUS_TCP":
                case "MQTT": {
                    // 仅校验关键字段，实际协议连通性测试待接入对应客户端库
                    Object portObj = config.get("port");
                    if (portObj == null) {
                        result.put("success", false);
                        result.put("message", "端口不能为空");
                        return result;
                    }
                    result.put("success", true);
                    result.put("message", interfaceType + " 配置校验通过，" + apiUrl + ":" + portObj
                            + "（实际协议连通性测试待接入对应客户端）");
                    break;
                }
                default: {
                    result.put("success", false);
                    result.put("message", "不支持的接口类型：" + interfaceType);
                }
            }
        } catch (IllegalArgumentException e) {
            result.put("success", false);
            result.put("message", "接口地址格式无效：" + e.getMessage());
        } catch (java.io.IOException | InterruptedException e) {
            result.put("success", false);
            result.put("message", "接口调用失败：" + e.getClass().getSimpleName() + " - " + e.getMessage());
        }
        return result;
    }

    /**
     * 安全地将对象转为 int
     */
    private int toInt(Object value, int defaultValue) {
        if (value == null) return defaultValue;
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
