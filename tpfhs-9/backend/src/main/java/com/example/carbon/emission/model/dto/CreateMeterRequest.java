package com.example.carbon.emission.model.dto;

import lombok.Data;

/**
 * 创建电表请求
 */
@Data
public class CreateMeterRequest {
    
    /**
     * 电表名称
     */
    private String name;
    
    /**
     * 拼音首字母编码
     */
    private String pinyinCode;
    
    /**
     * 所属集中器ID
     */
    private Long pointId;
    
    /**
     * 上级电表ID
     */
    private Long parentMeterId;
    
    /**
     * 电表类型
     */
    private String meterType;
    
    /**
     * 电表型号
     */
    private String meterModel;
    
    /**
     * 电表地址
     */
    private String meterAddress;
    
    /**
     * 用途描述
     */
    private String purposeDescription;
    
    /**
     * 描述信息（兼容前端 formData.description）
     */
    private String description;
    
    /**
     * 能耗数据划拨
     */
    private String energyAllocation;
    
    /**
     * 用电分类
     */
    private String powerCategory;
    
    /**
     * 抄表方式：1-自动抄表，0-人工抄表
     */
    private Integer meterReadingMethod;

    /**
     * 总表与分表关系：1-总表计数等于各下属分表计数之和，2-总表计数不等于各下属分表计数之和
     */
    private Integer parentChildRelationship;

    /**
     * 是否虚拟电表：1-是虚拟电表，0-不是虚拟电表
     */
    private Integer isVirtualMeter;

    /**
     * 是否分摊子电表：1-是，0-否
     */
    private Integer isAllocationChild;

    /**
     * 分摊比例，缺省值1.0
     */
    private java.math.BigDecimal allocationRatio;
    
    /**
     * 自动抄表接口配置
     */
    private String autoMeterReadingConfig;

    /**
     * 是否累加量：1-是，0-否
     */
    private Integer isCumulative;

    /**
     * 是否移动源：1-是，0-否
     */
    private Integer isMobileSource;

    /**
     * 计量单位
     */
    private String measurementUnit;

    /**
     * 数据来源系统
     */
    private String dataSourceSystem;

    /**
     * 用电分类：外购电力、新能源发电（自发自用）
     */
    private String emissionSubcategory;

    /**
     * 非累加量计费周期单位：1-周，2-月，3-季度，4-年
     */
    private Integer billingCycleUnit;

    /**
     * 非累加量计费周期起始日期偏移量
     */
    private Integer billingCycleStartDate;

    /**
     * 非累加量计费周期长度（按月计）
     */
    private Integer billingCycleLength;

    /**
     * 能耗一级分类编码
     */
    private String energyCategoryL1;

    /**
     * 能耗二级分类编码
     */
    private String energyCategoryL2;

    /**
     * 能耗三级分类编码
     */
    private String energyCategoryL3;

    /**
     * 能耗用途分类
     */
    private String energyUseCategory;

    /**
     * 排序顺序
     */
    private Integer sortOrder;

    /**
     * 状态：1-启用，0-停用
     */
    private Integer status;
    
    /**
     * 创建人ID
     */
    private Long createdBy;
}
