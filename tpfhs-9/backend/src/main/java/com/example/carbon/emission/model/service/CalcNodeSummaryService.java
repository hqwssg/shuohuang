package com.example.carbon.emission.model.service;

import com.example.carbon.emission.model.entity.CalcNodeSummary;

import java.time.LocalDate;
import java.util.List;

/**
 * 碳排放核算节点汇总 Service
 * <p>
 * 提供核算节点汇总数据的查询能力，支持按核算任务、按维度过滤、
 * 按源节点+核算周期跨模板汇总等场景。
 */
public interface CalcNodeSummaryService {

    /**
     * 按核算任务维度查询汇总数据
     * <p>
     * 以 templateId 为主查询条件，其余参数为 null 时不参与过滤，
     * 在 Service 层通过 Stream 进行条件筛选，保证查询方式的灵活性。
     *
     * @param templateId       核算任务记录ID（必填）
     * @param nodeId           核算节点ID（可选，null 不过滤）
     * @param subcategoryCode  排放数据小类编码（可选，null 不过滤）
     * @param l1               能源大类（可选，null 不过滤）
     * @param l2               能源中类（可选，null 不过滤）
     * @param l3               能源小类（可选，null 不过滤）
     * @return 符合条件的汇总数据列表
     */
    List<CalcNodeSummary> queryByTemplate(Long templateId, Long nodeId, String subcategoryCode,
                                          String l1, String l2, String l3);

    /**
     * 按源节点+核算周期范围跨模板查询汇总数据
     * <p>
     * 先筛选出核算周期起始日期在 [start, end] 区间内且执行成功(status=2)
     * 的全部核算任务，再取出这些任务中 sourceNodeId 匹配的汇总记录合并返回。
     *
     * @param sourceNodeId 源节点ID（emission_node.id）
     * @param start        核算周期起始日期（包含）
     * @param end          核算周期起始日期（包含）
     * @return 跨模板合并后的汇总数据列表
     */
    List<CalcNodeSummary> queryBySourceNodeAndCycle(Long sourceNodeId, LocalDate start, LocalDate end);
}
