package com.example.carbon.emission.model.repository;

import com.example.carbon.emission.model.entity.CalculationNode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 碳排放核算节点数据访问接口
 */
@Repository
public interface CalculationNodeRepository extends JpaRepository<CalculationNode, Long> {

    /**
     * 根据核算任务记录ID查询本次核算涉及的节点快照
     *
     * @param calculationTemplateId 核算任务记录ID（emission_calculation_template.id）
     * @return 核算节点列表
     */
    List<CalculationNode> findByCalculationTemplateId(Long calculationTemplateId);

    /**
     * 根据核算任务记录ID和节点ID查询特定节点快照
     *
     * @param calculationTemplateId 核算任务记录ID
     * @param nodeId 源节点ID
     * @return 核算节点列表
     */
    List<CalculationNode> findByCalculationTemplateIdAndNodeId(Long calculationTemplateId, Long nodeId);
}
