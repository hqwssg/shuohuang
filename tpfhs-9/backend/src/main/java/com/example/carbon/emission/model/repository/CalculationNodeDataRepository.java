package com.example.carbon.emission.model.repository;

import com.example.carbon.emission.model.entity.CalculationNodeData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

/**
 * 碳排放核算节点数据访问接口
 */
@Repository
public interface CalculationNodeDataRepository extends JpaRepository<CalculationNodeData, Long> {

    /**
     * 根据核算节点记录ID查询该节点本次核算的能耗与碳排放计算结果
     *
     * @param calculationNodeId 核算节点记录ID（emission_calculation_node.id）
     * @return 核算节点数据列表
     */
    List<CalculationNodeData> findByCalculationNodeId(Long calculationNodeId);

    /**
     * 根据核算节点记录ID集合批量查询核算结果
     *
     * @param calcNodeIds 核算节点记录ID集合
     * @return 核算节点数据列表
     */
    List<CalculationNodeData> findByCalculationNodeIdIn(Collection<Long> calcNodeIds);

    /**
     * 根据采集点类型和采集点ID查询核算历史
     * 用于按采集点回溯历次核算结果
     *
     * @param collectionPointType 采集点类型：1-电力表，2-化石燃料，3-外购热能
     * @param collectionPointId 采集点ID
     * @return 核算节点数据列表
     */
    List<CalculationNodeData> findByCollectionPointTypeAndCollectionPointId(
            Integer collectionPointType, Long collectionPointId);

    /**
     * 根据数据状态查询核算记录
     * 用于筛选数据异常（不完整/超范围/无数据）的核算记录
     *
     * @param dataStatus 数据状态：1-完整，2-没有数据，3-数据不完整，4-数据超范围，5-数据不完整且超范围
     * @return 核算节点数据列表
     */
    List<CalculationNodeData> findByDataStatus(Integer dataStatus);
}
