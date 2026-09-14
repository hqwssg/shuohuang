package com.ruoyi.carbon.report.mapper;

import java.util.Date;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.carbon.report.domain.CalculationCandidate;
import com.ruoyi.carbon.report.domain.CalculationNodeDataRow;
import com.ruoyi.carbon.report.domain.DefaultFactorRow;
import com.ruoyi.carbon.report.domain.FactorTemplateLink;
import com.ruoyi.carbon.report.domain.FossilFactorRow;

public interface EmissionCalculationMapper
{
    List<CalculationCandidate> selectCandidates(
            @Param("subjectNodeId") Long subjectNodeId,
            @Param("periodStart") Date periodStart,
            @Param("periodEnd") Date periodEnd);

    List<CalculationNodeDataRow> selectNodeData(
            @Param("calculationTemplateId") Long calculationTemplateId,
            @Param("energyAllocation") String energyAllocation);

    FactorTemplateLink selectFactorTemplateLink(@Param("calculationTemplateId") Long calculationTemplateId);

    List<DefaultFactorRow> selectDefaultFactors(@Param("templateId") Long templateId);

    FossilFactorRow selectFossilFactor(@Param("id") Long id);
}
