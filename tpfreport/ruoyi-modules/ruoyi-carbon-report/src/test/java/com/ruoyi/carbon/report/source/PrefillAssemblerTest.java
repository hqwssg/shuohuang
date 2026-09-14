package com.ruoyi.carbon.report.source;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import com.ruoyi.carbon.report.domain.CalculationCandidate;
import com.ruoyi.carbon.report.domain.CalculationNodeDataRow;
import com.ruoyi.carbon.report.domain.dto.PrefillLine;
import com.ruoyi.carbon.report.domain.dto.PrefillResult;

class PrefillAssemblerTest
{
    private final PrefillAssembler assembler = new PrefillAssembler(new ReportUnitConverter());

    @Test
    void failedCalculationIsNotSelectable()
    {
        CalculationCandidate failed = new CalculationCandidate();
        failed.setStatus(3);
        CalculationCandidate ok = new CalculationCandidate();
        ok.setStatus(2);
        assertEquals(1, assembler.selectable(List.of(failed, ok)).size());
    }

    @Test
    void doesNotCrossEnergyAllocation()
    {
        PrefillResult result = assembler.assemble("suring", List.of(row("FF_NG", "yuanping", "10k_m3", "2", "9")));
        assertTrue(result.getFuels().isEmpty());
    }

    @Test
    void keepsOfficialEmissionInsteadOfRecomputing()
    {
        CalculationNodeDataRow row = row("FF_D", "suring", "t", "10", "31.0");
        PrefillResult result = assembler.assemble("suring", List.of(row));
        assertEquals(1, result.getFuels().size());
        assertEquals(0, new BigDecimal("31.0").compareTo(result.getFuels().get(0).getCarbonEmission()));
        assertEquals(0, new BigDecimal("31.0").compareTo(assembler.sumEmissions(List.of(row))));
    }

    @Test
    void overrideDoesNotChangeSourceValue()
    {
        PrefillResult result = assembler.assemble("suring", List.of(row("FF_G", "suring", "t", "4", "12")));
        BigDecimal source = result.getFuels().get(0).getSourceValue();
        List<PrefillLine> overridden = PrefillAssembler.copyWithoutTouchingSource(
                result.getFuels(), new BigDecimal("8"), "本报告覆盖");
        assertEquals(0, source.compareTo(overridden.get(0).getSourceValue()));
        assertEquals("OVERRIDE", overridden.get(0).getDataOrigin());
        assertEquals("本报告覆盖", overridden.get(0).getOverrideReason());
    }

    private static CalculationNodeDataRow row(String sub, String alloc, String unit, String energy, String emission)
    {
        CalculationNodeDataRow row = new CalculationNodeDataRow();
        row.setId(11L);
        row.setEmissionSubcategory(sub);
        row.setEnergyAllocation(alloc);
        row.setMeasurementUnit(unit);
        row.setEnergyMeasurementValue(new BigDecimal(energy));
        row.setCarbonEmission(new BigDecimal(emission));
        row.setFacilityName("facility");
        return row;
    }
}
