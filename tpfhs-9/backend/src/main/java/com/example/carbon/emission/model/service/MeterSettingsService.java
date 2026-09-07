package com.example.carbon.emission.model.service;

import com.example.carbon.emission.model.dto.*;

/**
 * 电表设置服务接口
 * 管理站点/区间、集中器、电表的树状结构及相关操作
 */
public interface MeterSettingsService {
    
    /**
     * 获取完整的电表设置树
     * 
     * @return 树状结构DTO，根节点为"电表设置"
     */
    MeterTreeNodeDTO getTree();
    
    /**
     * 根据节点类型和ID获取详情
     * 
     * @param nodeType 节点类型：station/concentrator/meter
     * @param id 节点ID
     * @return 节点详情
     */
    MeterTreeNodeDTO getNodeDetail(String nodeType, Long id);
    
    /**
     * 创建站点/区间
     * 
     * @param request 创建请求
     * @return 创建的节点DTO
     */
    MeterTreeNodeDTO createStation(CreateStationRequest request);
    
    /**
     * 创建集中器
     * 
     * @param request 创建请求
     * @return 创建的节点DTO
     */
    MeterTreeNodeDTO createConcentrator(CreateConcentratorRequest request);
    
    /**
     * 创建电表
     * 
     * @param request 创建请求
     * @return 创建的节点DTO
     */
    MeterTreeNodeDTO createMeter(CreateMeterRequest request);
    
    /**
     * 更新节点信息
     * 
     * @param nodeType 节点类型
     * @param id 节点ID
     * @param request 更新请求
     * @return 更新后的节点DTO
     */
    MeterTreeNodeDTO updateNode(String nodeType, Long id, Object request);
    
    /**
     * 删除节点（及其子节点）
     * 只有停用状态的节点才能删除
     * 
     * @param nodeType 节点类型
     * @param id 节点ID
     * @param userId 操作人ID
     * @throws RuntimeException 如果节点处于启用状态
     */
    void deleteNode(String nodeType, Long id, Long userId);
    
    /**
     * 切换节点状态（启用/停用）
     * 包含复杂的级联检查逻辑
     * 
     * @param nodeType 节点类型
     * @param id 节点ID
     * @param userId 操作人ID
     * @return 操作结果消息
     */
    String toggleStatus(String nodeType, Long id, Long userId);
    
    /**
     * 上移节点
     * 
     * @param nodeType 节点类型
     * @param id 节点ID
     * @param userId 操作人ID
     */
    void moveUp(String nodeType, Long id, Long userId);
    
    /**
     * 下移节点
     * 
     * @param nodeType 节点类型
     * @param id 节点ID
     * @param userId 操作人ID
     */
    void moveDown(String nodeType, Long id, Long userId);
    
    /**
     * 更改电表级联关系
     * 修改电表的上级电表
     * 
     * @param meterId 电表ID
     * @param request 修改请求
     * @return 修改后的电表节点DTO
     */
    MeterTreeNodeDTO changeMeterCascade(Long meterId, ChangeMeterCascadeRequest request);
    
    /**
     * 更改电表从属数据集中器
     * 修改电表所属的集中器，同时可修改上级电表
     * 
     * @param meterId 电表ID
     * @param request 修改请求
     * @return 修改后的电表节点DTO
     */
    MeterTreeNodeDTO changeMeterPoint(Long meterId, ChangeMeterPointRequest request);
    
    /**
     * 获取指定集中器下的所有电表（除自身外）
     * 
     * @param pointId 集中器ID
     * @param excludeMeterId 排除的电表ID
     * @return 电表列表
     */
    java.util.List<MeterTreeNodeDTO> getMetersByPoint(Long pointId, Long excludeMeterId);
    
    /**
     * 获取指定站点/区间下的所有集中器
     *
     * @param stationId 站点ID
     * @return 集中器列表
     */
    java.util.List<MeterTreeNodeDTO> getConcentratorsByStation(Long stationId);

    /**
     * 测试自动抄表接口
     * 根据传入的配置尝试连接/调用抄表接口，返回是否可正常获取数据
     *
     * @param meterId 电表ID（可选，为 null 表示新建模式下的预测试）
     * @param config  抄表接口配置（与 emission_meter_info.auto_meter_reading_config 一致）
     * @return 测试结果，包含 success 标志和描述信息
     */
    java.util.Map<String, Object> testMeterReading(Long meterId, java.util.Map<String, Object> config);
}
