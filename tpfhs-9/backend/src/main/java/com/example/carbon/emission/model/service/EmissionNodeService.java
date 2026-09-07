package com.example.carbon.emission.model.service;

import com.example.carbon.emission.model.dto.CreateNodeRequest;
import com.example.carbon.emission.model.dto.NodeDTO;
import com.example.carbon.emission.model.dto.NodeConfigDTO;
import com.example.carbon.emission.model.dto.UpdateNodeRequest;

/**
 * 碳排放节点服务接口
 * 
 * 提供节点的增删改查、配置管理以及节点移动等功能。
 */
public interface EmissionNodeService {
    
    /**
     * 获取完整的节点树形结构
     * 
     * @return 根节点及其所有子节点
     */
    NodeDTO getTree();
    
    /**
     * 获取指定模板的节点树形结构
     * 
     * @param templateId 模板ID
     * @return 根节点及其所有子节点
     */
    NodeDTO getTree(Long templateId);
    
    /**
     * 根据ID获取节点详情
     * 
     * @param id 节点ID
     * @return 节点DTO
     */
    NodeDTO getNodeById(Long id);
    
    /**
     * 创建新节点
     * 
     * @param request 创建请求
     * @return 创建的节点DTO
     */
    NodeDTO createNode(CreateNodeRequest request);
    
    /**
     * 更新节点信息
     * 
     * @param id 节点ID
     * @param request 更新请求
     * @return 更新后的节点DTO
     */
    NodeDTO updateNode(Long id, UpdateNodeRequest request);
    
    /**
     * 删除节点
     * 
     * @param id 节点ID
     */
    void deleteNode(Long id);
    
    /**
     * 获取节点配置
     * 
     * @param nodeId 节点ID
     * @return 节点配置DTO
     */
    NodeConfigDTO getNodeConfig(Long nodeId);
    
    /**
     * 更新节点配置
     * 
     * @param nodeId 节点ID
     * @param config 配置DTO
     * @param userId 更新人ID
     * @return 更新后的配置DTO
     */
    NodeConfigDTO updateNodeConfig(Long nodeId, NodeConfigDTO config, Long userId);
    
    /**
     * 移动节点位置（上下移动）
     * 
     * @param nodeId 节点ID
     * @param direction 移动方向（"up" 或 "down"）
     */
    void moveNode(Long nodeId, String direction);
    
    /**
     * 获取下一个可用的节点ID（当前最大ID+1）
     * 
     * @return 下一个可用的节点ID
     */
    Long getNextNodeId();
    
    /**
     * 生成采集设备编号
     *
     * @param nodeId 采集点节点ID或父节点ID
     * @param emissionCategory 排放数据大类
     * @param newNodeId 新节点ID（可选，用于新建节点时生成编号）
     * @return 生成的设备编号
     */
    String generateEquipmentCode(Long nodeId, String emissionCategory, Long newNodeId);

    /**
     * 挂载节点模版
     *
     * 将指定节点模版（templateType=1）所包含的全部子节点复制到当前模版树中，
     * 挂载到目标父节点（typeId=1/2 的"排放核算点"）之下。
     * 每个被复制的节点都会通过 source_node_id 字段记录其来源节点ID。
     *
     * @param parentId           目标父节点ID（挂载位置）
     * @param sourceTemplateId   节点模版ID（来源模版）
     * @param createdBy          创建人ID
     * @return 挂载后的目标父节点DTO
     */
    NodeDTO mountNodeTemplate(Long parentId, Long sourceTemplateId, Long createdBy);
}
