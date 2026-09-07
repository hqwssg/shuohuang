package com.example.carbon.emission.model.service;

import com.example.carbon.emission.model.dto.TemplateValidationResultVO;

/**
 * 碳排放核算模版校验服务
 * <p>
 * 针对"核算模版"执行完整性校验，并将结果（含富文本告警信息）持久化到
 * emission_template 表的 check_result / check_time / check_message 字段。
 */
public interface TemplateValidationService {

    /**
     * 执行模版校验并保存结果
     * <p>
     * 校验内容：<br>
     * 1. 模版是否设置了"碳排放因子模版"；<br>
     * 2. 模版中每个采集节点引用的采集点，是否在对应节点表
     *    （emission_meter_info / emission_fossil_fuel_meter_info /
     *    emission_purchased_heat_meter_info）中存在且为启用状态；<br>
     * 3. 碳排放因子模版中设置的"能耗小类"能否完全覆盖模版中每个采集节点
     *    所设置的小类，未完全覆盖输出告警；<br>
     * 4. 采集节点单独设置的碳排放因子与因子模版中同小类因子值是否一致，
     *    不一致输出提示；<br>
     * 5. 是否存在空的核算节点（下属无任何采集节点），存在输出告警。
     *
     * @param templateId 核算模版ID
     * @return 校验结果（含富文本）
     * @throws RuntimeException 模版不存在或非核算模版时抛出
     */
    TemplateValidationResultVO validateTemplate(Long templateId);

    /**
     * 查询模版最近一次保存的校验结果（不重新校验）
     *
     * @param templateId 核算模版ID
     * @return 校验结果；未校验过时 checkResult=0、checkMessage 为空
     */
    TemplateValidationResultVO getValidationResult(Long templateId);
}
