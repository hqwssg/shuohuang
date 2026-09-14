package com.ruoyi.carbon.report.domain.dto;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ReportFactorPlan
{
    private Long calculationTemplateId;
    private String emissionTemplateName;
    private Long factorTemplateId;
    private String factorTemplateName;
    private List<String> needed = new ArrayList<>();
    private List<String> missing = new ArrayList<>();
    private List<String> missingSlots = new ArrayList<>();
    private Map<String, Map<String, Object>> resolved = new LinkedHashMap<>();
    private String reason = "";

    public boolean isComplete()
    {
        return missingSlots.isEmpty();
    }

    public Long getCalculationTemplateId() { return calculationTemplateId; }
    public void setCalculationTemplateId(Long calculationTemplateId) { this.calculationTemplateId = calculationTemplateId; }
    public String getEmissionTemplateName() { return emissionTemplateName; }
    public void setEmissionTemplateName(String emissionTemplateName) { this.emissionTemplateName = emissionTemplateName; }
    public Long getFactorTemplateId() { return factorTemplateId; }
    public void setFactorTemplateId(Long factorTemplateId) { this.factorTemplateId = factorTemplateId; }
    public String getFactorTemplateName() { return factorTemplateName; }
    public void setFactorTemplateName(String factorTemplateName) { this.factorTemplateName = factorTemplateName; }
    public List<String> getNeeded() { return needed; }
    public void setNeeded(List<String> needed) { this.needed = needed; }
    public List<String> getMissing() { return missing; }
    public void setMissing(List<String> missing) { this.missing = missing; }
    public List<String> getMissingSlots() { return missingSlots; }
    public void setMissingSlots(List<String> missingSlots) { this.missingSlots = missingSlots; }
    public Map<String, Map<String, Object>> getResolved() { return resolved; }
    public void setResolved(Map<String, Map<String, Object>> resolved) { this.resolved = resolved; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
