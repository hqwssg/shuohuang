package com.ruoyi.carbon.report.source;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import com.ruoyi.carbon.report.domain.ReportActivityRow;
import com.ruoyi.carbon.report.domain.ReportTask;
import com.ruoyi.carbon.report.domain.dto.PrefillLine;
import com.ruoyi.carbon.report.domain.dto.PrefillResult;
import com.ruoyi.carbon.report.generation.SnapshotHasher;

/**
 * Applies auto-prefill onto a report task without recomputing emissions.
 */
public class PrefillCommitter
{
    public void apply(ReportTask task, PrefillResult result, Long calculationTemplateId)
    {
        task.setCalculationTemplateId(calculationTemplateId);
        task.setSourceHash(hash(calculationTemplateId, result));
        task.setVersion(task.getVersion() == null ? 1 : task.getVersion() + 1);
    }

    public List<ReportActivityRow> toFuelRows(ReportTask task, PrefillResult result)
    {
        List<ReportActivityRow> rows = new ArrayList<>();
        int order = 0;
        for (PrefillLine line : result.getFuels())
        {
            rows.add(toRow(task, line, "fuel", order++));
        }
        return rows;
    }

    public List<ReportActivityRow> toElectricityRows(ReportTask task, PrefillResult result)
    {
        List<ReportActivityRow> rows = new ArrayList<>();
        int order = 0;
        for (PrefillLine line : result.getElectricity())
        {
            ReportActivityRow row = toRow(task, line, "electricity", order++);
            row.setScope("purchased-electricity");
            rows.add(row);
        }
        return rows;
    }

    public List<ReportActivityRow> toHeatRows(ReportTask task, PrefillResult result)
    {
        List<ReportActivityRow> rows = new ArrayList<>();
        int order = 0;
        for (PrefillLine line : result.getHeat())
        {
            ReportActivityRow row = toRow(task, line, "heat", order++);
            row.setSite(line.getFacility());
            rows.add(row);
        }
        return rows;
    }

    public void overrideNormalized(ReportActivityRow row, BigDecimal value, String reason)
    {
        row.setNormalizedValue(value);
        row.setQuantity(value);
        row.setDataOrigin("OVERRIDE");
        row.setOverrideReason(reason);
    }

    public static String hash(Long calculationTemplateId, PrefillResult result)
    {
        StringBuilder builder = new StringBuilder();
        builder.append(calculationTemplateId).append('|');
        result.getFuels().forEach(line -> builder.append(line.getSourceHash()).append(','));
        result.getElectricity().forEach(line -> builder.append(line.getSourceHash()).append(','));
        result.getHeat().forEach(line -> builder.append(line.getSourceHash()).append(','));
        return SnapshotHasher.sha256(builder.toString());
    }

    private static ReportActivityRow toRow(ReportTask task, PrefillLine line, String kind, int order)
    {
        ReportActivityRow row = new ReportActivityRow();
        row.setDeptId(task.getDeptId());
        row.setReportId(task.getId());
        row.setKind(kind);
        row.setFuelType(line.getFuelType());
        row.setFacility(line.getFacility());
        row.setQuantity(line.getNormalizedValue());
        row.setUnit(line.getNormalizedUnit());
        row.setSourceName(line.getFacility());
        row.setDataOrigin(line.getDataOrigin() == null ? "AUTO" : line.getDataOrigin());
        row.setSourceTable(line.getSourceTable());
        row.setSourceId(line.getSourceId());
        row.setSourceValue(line.getSourceValue());
        row.setSourceUnit(line.getSourceUnit());
        row.setNormalizedValue(line.getNormalizedValue());
        row.setNormalizedUnit(line.getNormalizedUnit());
        row.setSourceHash(line.getSourceHash());
        row.setOverrideReason(line.getOverrideReason());
        row.setSortOrder(order);
        return row;
    }
}
