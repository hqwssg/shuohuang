package com.example.carbon.emission.model.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 电表设置树节点DTO
 * 用于前端展示树状结构
 */
@Data
public class MeterTreeNodeDTO {
    
    /**
     * 节点ID
     */
    private Long id;
    
    /**
     * 节点名称
     */
    private String name;
    
    /**
     * 节点类型：root-根节点, station-站点/区间, concentrator-集中器, meter-电表
     */
    private String nodeType;
    
    /**
     * 状态：1-启用，0-停用
     */
    private Integer status;
    
    /**
     * 状态文本：启用/停用
     */
    private String statusText;
    
    /**
     * 拼音首字母编码
     */
    private String pinyinCode;
    
    /**
     * 描述信息
     */
    private String description;
    
    /**
     * 所属父节点ID
     */
    private Long parentId;
    
    /**
     * 所属集中器ID（电表节点专用）
     */
    private Long pointId;
    
    /**
     * 变压器容量（集中器节点）
     */
    private String transformerCapacity;
    
    /**
     * 集中器地址（集中器节点）
     */
    private String concentratorAddress;
    
    /**
     * 电表类型（电表节点）
     */
    private String meterType;
    
    /**
     * 电表型号（电表节点）
     */
    private String meterModel;
    
    /**
     * 电表地址（电表节点）
     */
    private String meterAddress;
    
    /**
     * 用电分类（电表节点）
     */
    private String powerCategory;
    
    /**
     * 能耗数据划拨（电表节点）
     */
    private String energyAllocation;
    
    /**
     * 抄表方式：1-自动抄表，0-人工抄表（电表节点）
     */
    private Integer meterReadingMethod;

    /**
     * 总表与分表关系：1-总表计数等于各下属分表计数之和，2-总表计数不等于各下属分表计数之和（电表节点）
     */
    private Integer parentChildRelationship;

    /**
     * 是否虚拟电表：1-是虚拟电表，0-不是虚拟电表（电表节点）
     */
    private Integer isVirtualMeter;

    /**
     * 是否分摊子电表：1-是，0-否（电表节点）
     */
    private Integer isAllocationChild;

    /**
     * 分摊比例，缺省值1.0（电表节点）
     */
    private java.math.BigDecimal allocationRatio;
    
    /**
     * 自动抄表接口配置（电表节点）
     */
    private String autoMeterReadingConfig;

    /**
     * 是否累加量：1-是，0-否（电表节点）
     */
    private Integer isCumulative;

    /**
     * 是否移动源：1-是，0-否（电表节点）
     */
    private Integer isMobileSource;

    /**
     * 计量单位（电表节点）
     */
    private String measurementUnit;

    /**
     * 数据来源系统（电表节点）
     */
    private String dataSourceSystem;

    /**
     * 用电分类：外购电力、新能源发电（自发自用）（电表节点）
     */
    private String emissionSubcategory;

    /**
     * 非累加量计费周期单位：1-周，2-月，3-季度，4-年（电表节点）
     */
    private Integer billingCycleUnit;

    /**
     * 非累加量计费周期起始日期偏移量（电表节点）
     */
    private Integer billingCycleStartDate;

    /**
     * 非累加量计费周期长度（按月计）（电表节点）
     */
    private Integer billingCycleLength;

    /**
     * 能耗一级分类编码（电表节点）
     */
    private String energyCategoryL1;

    /**
     * 能耗二级分类编码（电表节点）
     */
    private String energyCategoryL2;

    /**
     * 能耗三级分类编码（电表节点）
     */
    private String energyCategoryL3;

    /**
     * 能耗用途分类（电表节点）
     */
    private String energyUseCategory;

    /**
     * 排序顺序
     */
    private Integer sortOrder;
    
    /**
     * 子节点列表
     */
    private List<MeterTreeNodeDTO> children = new ArrayList<>();
    
    /**
     * 是否有子节点
     */
    private boolean hasChildren;
    
    /**
     * 创建时间
     */
    private String createdAt;
    
    /**
     * 更新时间
     */
    private String updatedAt;
}
