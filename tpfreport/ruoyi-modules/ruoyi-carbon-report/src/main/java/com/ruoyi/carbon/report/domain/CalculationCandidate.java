package com.ruoyi.carbon.report.domain;

import java.math.BigDecimal;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;

public class CalculationCandidate
{
    private Long id;
    private Integer status;
    private String templateName;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date cycleStart;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date cycleEnd;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date finishedAt;
    private BigDecimal totalEmission;
    private String warning;

    public boolean isSuccess()
    {
        return status != null && status == 2;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public String getTemplateName() { return templateName; }
    public void setTemplateName(String templateName) { this.templateName = templateName; }
    public Date getCycleStart() { return cycleStart; }
    public void setCycleStart(Date cycleStart) { this.cycleStart = cycleStart; }
    public Date getCycleEnd() { return cycleEnd; }
    public void setCycleEnd(Date cycleEnd) { this.cycleEnd = cycleEnd; }
    public Date getFinishedAt() { return finishedAt; }
    public void setFinishedAt(Date finishedAt) { this.finishedAt = finishedAt; }
    public BigDecimal getTotalEmission() { return totalEmission; }
    public void setTotalEmission(BigDecimal totalEmission) { this.totalEmission = totalEmission; }
    public String getWarning() { return warning; }
    public void setWarning(String warning) { this.warning = warning; }
}
