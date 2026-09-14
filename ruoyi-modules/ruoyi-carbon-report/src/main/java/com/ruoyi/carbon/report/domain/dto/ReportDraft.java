package com.ruoyi.carbon.report.domain.dto;

import java.util.ArrayList;
import java.util.List;
import com.ruoyi.carbon.report.domain.ReportActivityRow;
import com.ruoyi.carbon.report.domain.ReportEquipment;
import com.ruoyi.carbon.report.domain.ReportFile;
import com.ruoyi.carbon.report.domain.ReportGenerationJob;
import com.ruoyi.carbon.report.domain.ReportMonitoringDevice;
import com.ruoyi.carbon.report.domain.ReportTask;
import com.ruoyi.carbon.report.domain.ReportWorkload;

public class ReportDraft
{
    private ReportTask task;
    private final List<ReportActivityRow> fuels = new ArrayList<>();
    private final List<ReportActivityRow> electricity = new ArrayList<>();
    private final List<ReportActivityRow> heat = new ArrayList<>();
    private final List<ReportEquipment> equipment = new ArrayList<>();
    private final List<ReportWorkload> workload = new ArrayList<>();
    private final List<ReportMonitoringDevice> monitoring = new ArrayList<>();
    private final List<ReportFile> images = new ArrayList<>();
    private int workloadCount;
    private boolean missingWorkload;
    private ReportGenerationJob latestJob;

    public ReportTask getTask() { return task; }
    public void setTask(ReportTask task) { this.task = task; }
    public List<ReportActivityRow> getFuels() { return fuels; }
    public List<ReportActivityRow> getElectricity() { return electricity; }
    public List<ReportActivityRow> getHeat() { return heat; }
    public List<ReportEquipment> getEquipment() { return equipment; }
    public List<ReportWorkload> getWorkload() { return workload; }
    public List<ReportMonitoringDevice> getMonitoring() { return monitoring; }
    public List<ReportFile> getImages() { return images; }
    public int getWorkloadCount() { return workloadCount; }
    public void setWorkloadCount(int workloadCount) { this.workloadCount = workloadCount; }
    public boolean isMissingWorkload() { return missingWorkload; }
    public void setMissingWorkload(boolean missingWorkload) { this.missingWorkload = missingWorkload; }
    public ReportGenerationJob getLatestJob() { return latestJob; }
    public void setLatestJob(ReportGenerationJob latestJob) { this.latestJob = latestJob; }
}
