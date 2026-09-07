package com.example.carbon.emission.model.repository;

import com.example.carbon.emission.model.entity.CalcNodeSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

/**
 * 碳排放核算节点汇总 Repository
 */
@Repository
public interface CalcNodeSummaryRepository extends JpaRepository<CalcNodeSummary, Long> {

    /**
     * 按核算任务记录ID删除全部汇总数据
     */
    void deleteByCalculationTemplateId(Long templateId);

    /**
     * 按核算任务记录ID查询全部汇总数据
     */
    List<CalcNodeSummary> findByCalculationTemplateId(Long templateId);

    /**
     * 按核算任务记录ID集合批量查询汇总数据
     * <p>
     * 用于按源节点+核算周期跨模板汇总查询
     */
    List<CalcNodeSummary> findByCalculationTemplateIdIn(Collection<Long> templateIds);

    /**
     * 按核算节点ID查询汇总数据
     */
    List<CalcNodeSummary> findByCalculationNodeId(Long calcNodeId);

    /**
     * 按核算节点ID集合批量查询汇总数据
     */
    List<CalcNodeSummary> findByCalculationNodeIdIn(Collection<Long> calcNodeIds);

    /**
     * 按核算任务记录ID + 源节点ID查询汇总数据
     */
    List<CalcNodeSummary> findByCalculationTemplateIdAndSourceNodeId(Long templateId, Long sourceNodeId);

    /**
     * 按核算节点ID删除汇总数据
     */
    void deleteByCalculationNodeId(Long calcNodeId);
}
