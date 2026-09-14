package com.ruoyi.carbon.report.source;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import com.ruoyi.carbon.report.domain.CalculationCandidate;
import com.ruoyi.carbon.report.domain.CalculationNodeDataRow;
import com.ruoyi.carbon.report.domain.dto.PrefillResult;

/**
 * In-memory equivalent of sql/emission-adapter-fixture.sql:
 * two BRANCH trees, one success and one failed calculation, plus a leaked allocation row.
 */
class EmissionSourceAdapterTest
{
    private final PrefillAssembler assembler = new PrefillAssembler(new ReportUnitConverter());

    @Test
    void failedTaskIsNotSelectable()
    {
        CalculationCandidate success = candidate(11L, 2);
        CalculationCandidate failed = candidate(12L, 3);
        assertEquals(1, assembler.selectable(List.of(success, failed)).size());
        assertEquals(11L, assembler.selectable(List.of(success, failed)).get(0).getId());
    }

    @Test
    void suringDoesNotPullYuanpingRow()
    {
        PrefillResult result = assembler.assemble("suring", List.of(
                row(1L, "FF_NG", "suring", "10k_m3", "2", "9"),
                row(2L, "FF_NG", "yuanping", "10k_m3", "8", "36")));
        assertEquals(1, result.getFuels().size());
        assertEquals("1", result.getFuels().get(0).getSourceId());
    }

    @Test
    void emissionEqualsOfficialSummary()
    {
        CalculationNodeDataRow row = row(1L, "FF_NG", "suring", "10k_m3", "2", "9");
        PrefillResult result = assembler.assemble("suring", List.of(row));
        assertEquals(0, new BigDecimal("9").compareTo(result.getFuels().get(0).getCarbonEmission()));
        assertEquals(0, new BigDecimal("9").compareTo(assembler.sumEmissions(List.of(row))));
    }

    @Test
    void naturalGasKeepsReportUnit()
    {
        PrefillResult result = assembler.assemble("suring",
                List.of(row(1L, "FF_NG", "suring", "10k_m3", "2", "9")));
        assertEquals("10k_m3", result.getFuels().get(0).getNormalizedUnit());
        assertTrue(result.getGaps().isEmpty());
    }

    private static CalculationCandidate candidate(Long id, int status)
    {
        CalculationCandidate candidate = new CalculationCandidate();
        candidate.setId(id);
        candidate.setStatus(status);
        return candidate;
    }

    private static CalculationNodeDataRow row(Long id, String sub, String alloc, String unit, String energy, String emission)
    {
        CalculationNodeDataRow row = new CalculationNodeDataRow();
        row.setId(id);
        row.setEmissionSubcategory(sub);
        row.setEnergyAllocation(alloc);
        row.setMeasurementUnit(unit);
        row.setEnergyMeasurementValue(new BigDecimal(energy));
        row.setCarbonEmission(new BigDecimal(emission));
        return row;
    }
}
