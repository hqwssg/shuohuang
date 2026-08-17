package com.example.carbon.emission.model.dto;

import lombok.Data;

/**
 * 节点信息数据传输对象
 * 
 * 用于核算子节点(typeId=2)的信息传输，包含节点编码、简称、核算标识、类型及边界说明等字段。
 */
@Data
public class NodeInfoDTO {
    
    /** 节点ID */
    private Long nodeId;
    
    /** 节点编码 */
    private String nodeCode;
    
    /** 
     * 节点名称简称
     * 用于自动生成子节点名称的前缀
     */
    private String shortName;
    
    /** 是否纳入碳排放核算 */
    private Boolean includeInCalculation;
    
    /** 节点类型（总公司/分公司/站点/区域） */
    private String nodeCategory;
    
    /** 单位说明，解释单位职责、范围 */
    private String unitDescription;
    
    /** 组织边界说明，明确哪些单位、区域纳入核算 */
    private String orgBoundaryDescription;
    
    /** 运营边界说明，明确自有、租赁、外包、代管设施处理口径 */
    private String operationBoundaryDescription;
}