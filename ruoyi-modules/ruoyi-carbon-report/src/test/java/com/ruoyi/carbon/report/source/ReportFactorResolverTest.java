package com.ruoyi.carbon.report.source;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import com.ruoyi.carbon.report.domain.DefaultFactorRow;
import com.ruoyi.carbon.report.domain.FactorTemplateLink;
import com.ruoyi.carbon.report.domain.ReportActivityRow;
import com.ruoyi.carbon.report.domain.ReportTask;
import com.ruoyi.carbon.report.domain.dto.ReportDraft;
import com.ruoyi.carbon.report.domain.dto.ReportFactorPlan;
import com.ruoyi.carbon.report.mapper.EmissionCalculationMapper;

class ReportFactorResolverTest
{
    @Test
    void asksWhenTemplateHasNoFactors()
    {
        EmissionCalculationMapper mapper = mock(EmissionCalculationMapper.class);
        when(mapper.selectFactorTemplateLink(11L)).thenReturn(null);
        when(mapper.selectDefaultFactors(11L)).thenReturn(List.of());
        ReportFactorResolver resolver = new ReportFactorResolver(mapper);
        ReportFactorPlan plan = resolver.plan(draftWithDiesel(11L));
        assertFalse(plan.isComplete());
        assertEquals(List.of("柴油"), plan.getMissing());
        assertNull(resolver.toSnapshot(plan, false));
        Map<String, Object> snapshot = resolver.toSnapshot(plan, true);
        assertEquals("default", snapshot.get("source"));
        @SuppressWarnings("unchecked")
        Map<String, Object> diesel = ((Map<String, Map<String, Object>>) snapshot.get("fuel_factors")).get("diesel");
        assertEquals(new BigDecimal("0.8753"), diesel.get("carbon_content"));
    }

    @Test
    void usesTemplateElectricityFactor()
    {
        DefaultFactorRow row = new DefaultFactorRow();
        row.setSubcategoryCode("EL");
        row.setFactorValue(new BigDecimal("0.5306"));
        row.setFactorUnit("kgCO2/kWh");
        row.setStatus(1);
        FactorTemplateLink link = new FactorTemplateLink();
        link.setFactorTemplateId(7L);
        link.setFactorTemplateName("肃宁因子");
        EmissionCalculationMapper mapper = mock(EmissionCalculationMapper.class);
        when(mapper.selectFactorTemplateLink(11L)).thenReturn(link);
        when(mapper.selectDefaultFactors(7L)).thenReturn(List.of(row));
        ReportFactorPlan plan = new ReportFactorResolver(mapper).plan(draftWithElectricity(11L));
        assertTrue(plan.isComplete());
        assertEquals(new BigDecimal("0.5306"), plan.getResolved().get("electricity").get("value"));
        Map<String, Object> snapshot = new ReportFactorResolver(mapper).toSnapshot(plan, false);
        assertEquals("template", snapshot.get("source"));
        assertEquals(new BigDecimal("0.5306"), snapshot.get("electricity_factor_tco2_per_mwh"));
    }

    @Test
    void kgPerMwhDividesToTons()
    {
        assertEquals(0, new BigDecimal("0.536600").compareTo(
                ReportFactorResolver.normalizeElectricity(new BigDecimal("536.6"), "kgCO2/MWh")));
    }

    private static ReportDraft draftWithDiesel(Long calcId)
    {
        ReportActivityRow fuel = new ReportActivityRow();
        fuel.setFuelType("diesel");
        fuel.setNormalizedValue(new BigDecimal("10"));
        ReportDraft draft = base(calcId);
        draft.getFuels().add(fuel);
        return draft;
    }

    private static ReportDraft draftWithElectricity(Long calcId)
    {
        ReportActivityRow elec = new ReportActivityRow();
        elec.setNormalizedValue(new BigDecimal("100"));
        ReportDraft draft = base(calcId);
        draft.getElectricity().add(elec);
        return draft;
    }

    private static ReportDraft base(Long calcId)
    {
        ReportTask task = new ReportTask();
        task.setCalculationTemplateId(calcId);
        task.setReportYear(2025);
        ReportDraft draft = new ReportDraft();
        draft.setTask(task);
        return draft;
    }
}
