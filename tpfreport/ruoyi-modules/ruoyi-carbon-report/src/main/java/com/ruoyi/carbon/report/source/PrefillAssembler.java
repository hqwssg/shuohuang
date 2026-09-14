package com.ruoyi.carbon.report.source;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.stream.Collectors;
import com.ruoyi.carbon.report.domain.CalculationCandidate;
import com.ruoyi.carbon.report.domain.CalculationNodeDataRow;
import com.ruoyi.carbon.report.domain.dto.PrefillLine;
import com.ruoyi.carbon.report.domain.dto.PrefillResult;

/**
 * Builds report activity lines from calculation snapshots. Does not recompute emissions.
 */
public class PrefillAssembler
{
    public static final int SUCCESS_STATUS = 2;

    private final ReportUnitConverter converter;

    public PrefillAssembler(ReportUnitConverter converter)
    {
        this.converter = converter;
    }

    public List<CalculationCandidate> selectable(List<CalculationCandidate> all)
    {
        return all.stream().filter(CalculationCandidate::isSuccess).collect(Collectors.toList());
    }

    public PrefillResult assemble(String energyAllocation, List<CalculationNodeDataRow> rows)
    {
        PrefillResult result = new PrefillResult();
        for (CalculationNodeDataRow row : rows)
        {
            if (row.getEnergyAllocation() != null && !row.getEnergyAllocation().equals(energyAllocation))
            {
                continue;
            }
            String bucket = ActivityClassifier.bucket(row.getEmissionSubcategory());
            PrefillLine line = new PrefillLine();
            line.setSourceTable("emission_collection_node_data");
            line.setSourceId(row.getId() == null ? null : String.valueOf(row.getId()));
            line.setSubcategory(row.getEmissionSubcategory());
            line.setFacility(row.getFacilityName());
            line.setCarbonEmission(row.getCarbonEmission());
            line.setDataOrigin("AUTO");
            ReportUnitConverter.Conversion conversion = converter.convert(
                    row.getEmissionSubcategory(), row.preferredEnergyValue(), row.getMeasurementUnit());
            line.setSourceValue(row.preferredEnergyValue());
            line.setSourceUnit(row.getMeasurementUnit());
            if (!conversion.ok)
            {
                result.getGaps().add("缺少单位换算: " + row.getEmissionSubcategory() + " " + row.getMeasurementUnit());
                continue;
            }
            line.setNormalizedValue(conversion.normalizedValue);
            line.setNormalizedUnit(conversion.normalizedUnit);
            line.setSourceHash(hash(row.getId(), conversion.normalizedValue, conversion.normalizedUnit, row.getCarbonEmission()));
            if ("fuel".equals(bucket))
            {
                line.setFuelType(ReportUnitConverter.reportFuelType(row.getEmissionSubcategory()));
                if (line.getFuelType() == null)
                {
                    result.getGaps().add("报告不支持燃料小类: " + row.getEmissionSubcategory());
                    continue;
                }
                result.getFuels().add(line);
            }
            else if ("electricity".equals(bucket))
            {
                result.getElectricity().add(line);
            }
            else if ("heat".equals(bucket))
            {
                result.getHeat().add(line);
            }
            else
            {
                result.getGaps().add("无法归类: " + row.getEmissionSubcategory());
            }
        }
        return result;
    }

    public BigDecimal sumEmissions(List<CalculationNodeDataRow> rows)
    {
        return rows.stream()
                .map(CalculationNodeDataRow::getCarbonEmission)
                .filter(v -> v != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static String hash(Object... parts)
    {
        try
        {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String joined = java.util.Arrays.stream(parts).map(String::valueOf).collect(Collectors.joining("|"));
            return HexFormat.of().formatHex(digest.digest(joined.getBytes(StandardCharsets.UTF_8)));
        }
        catch (Exception ex)
        {
            throw new IllegalStateException(ex);
        }
    }

    public static List<PrefillLine> copyWithoutTouchingSource(List<PrefillLine> lines, BigDecimal overrideValue, String reason)
    {
        List<PrefillLine> copied = new ArrayList<>();
        for (PrefillLine line : lines)
        {
            PrefillLine next = line.copy();
            if (overrideValue != null)
            {
                next.setNormalizedValue(overrideValue);
                next.setDataOrigin("OVERRIDE");
                next.setOverrideReason(reason);
            }
            copied.add(next);
        }
        return copied;
    }
}
