package com.ruoyi.carbon.report.domain;

public class FactorTemplateLink
{
    private Long calculationTemplateId;
    private Long emissionTemplateId;
    private String emissionTemplateName;
    private Long factorTemplateId;
    private String factorTemplateName;

    public Long getCalculationTemplateId() { return calculationTemplateId; }
    public void setCalculationTemplateId(Long calculationTemplateId) { this.calculationTemplateId = calculationTemplateId; }
    public Long getEmissionTemplateId() { return emissionTemplateId; }
    public void setEmissionTemplateId(Long emissionTemplateId) { this.emissionTemplateId = emissionTemplateId; }
    public String getEmissionTemplateName() { return emissionTemplateName; }
    public void setEmissionTemplateName(String emissionTemplateName) { this.emissionTemplateName = emissionTemplateName; }
    public Long getFactorTemplateId() { return factorTemplateId; }
    public void setFactorTemplateId(Long factorTemplateId) { this.factorTemplateId = factorTemplateId; }
    public String getFactorTemplateName() { return factorTemplateName; }
    public void setFactorTemplateName(String factorTemplateName) { this.factorTemplateName = factorTemplateName; }
}
