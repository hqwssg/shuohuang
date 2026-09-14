package com.ruoyi.carbon.report.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import com.ruoyi.carbon.report.domain.ReportEquipment;
import com.ruoyi.carbon.report.domain.ReportTask;

class ReportTaskServiceImplAssetsTest
{
    @Test
    void parsesCamelAndSnakeEnergyTypeWithoutTouchingProfile()
    {
        ReportTask task = new ReportTask();
        task.setId(9L);
        task.setDeptId(301L);
        List<ReportEquipment> rows = ReportTaskServiceImpl.parseEquipmentPayload(task, "{"
                + "\"equipment\":["
                + "{\"name\":\"线路维修车\",\"category\":\"机械设备\",\"quantity\":11,\"unit\":\"台\","
                + "\"model\":\"GC-270\",\"energyType\":\"柴油\",\"remark\":\"本年\"},"
                + "{\"name\":\"办公用车\",\"quantity\":\"8\",\"energy_type\":\"汽油\"}"
                + "]}", "admin");
        assertEquals(2, rows.size());
        assertEquals(Long.valueOf(9L), rows.get(0).getReportId());
        assertEquals(Long.valueOf(301L), rows.get(0).getDeptId());
        assertEquals("线路维修车", rows.get(0).getName());
        assertEquals(0, new BigDecimal("11").compareTo(rows.get(0).getQuantity()));
        assertEquals("柴油", rows.get(0).getEnergyType());
        assertEquals("MANUAL", rows.get(0).getDataOrigin());
        assertEquals("汽油", rows.get(1).getEnergyType());
        assertEquals("办公用车", rows.get(1).getName());
    }

    @Test
    void skipsWorkloadWithoutPositiveQuantity()
    {
        ReportTask task = new ReportTask();
        task.setId(9L);
        task.setDeptId(301L);
        var rows = ReportTaskServiceImpl.parseWorkloadPayload(task, "{"
                + "\"workload\":["
                + "{\"name\":\"线路维修里程\",\"unit\":\"km\"},"
                + "{\"name\":\"检修作业天\",\"quantity\":320,\"unit\":\"天\"},"
                + "{\"name\":\"\",\"quantity\":1,\"unit\":\"天\"}"
                + "]}", "admin");
        assertEquals(1, rows.size());
        assertEquals("检修作业天", rows.get(0).getName());
        assertEquals("MANUAL", rows.get(0).getDataOrigin());
        assertEquals(0, new BigDecimal("320").compareTo(rows.get(0).getQuantity()));
    }

    @Test
    void parsesMonitoringCamelAndSnakeFields()
    {
        ReportTask task = new ReportTask();
        task.setId(9L);
        task.setDeptId(301L);
        var rows = ReportTaskServiceImpl.parseMonitoringPayload(task, "{"
                + "\"monitoring\":["
                + "{\"name\":\"肃宁外购电表\",\"model\":\"DTZ341\",\"accuracy\":\"0.5S\","
                + "\"calibrationFrequency\":\"每年1次\",\"measurement_range\":\"0-100\"},"
                + "{\"name\":\"\"}"
                + "]}", "admin");
        assertEquals(1, rows.size());
        assertEquals("肃宁外购电表", rows.get(0).getName());
        assertEquals("DTZ341", rows.get(0).getModel());
        assertEquals("每年1次", rows.get(0).getCalibrationFrequency());
        assertEquals("0-100", rows.get(0).getMeasurementRange());
        assertEquals("MANUAL", rows.get(0).getDataOrigin());
    }

    @Test
    void priorYearPayloadKeepsFourNumbersAndExtractsFromSnapshotTrend()
    {
        String json = ReportTaskServiceImpl.applyPriorYearPayload("{"
                + "\"report_year\":2024,"
                + "\"total_emissions_tco2\":100,"
                + "\"fuel_combustion_tco2\":40,"
                + "\"electricity_emissions_tco2\":50,"
                + "\"heat_emissions_tco2\":10,"
                + "\"noise\":true"
                + "}");
        assertTrue(json.contains("\"report_year\":2024"));
        assertTrue(json.contains("total_emissions_tco2"));
        assertFalse(json.contains("noise"));
        String fromSnap = ReportTaskServiceImpl.extractPriorFromSnapshot("{"
                + "\"chapter6\":{\"trend_years\":[{\"year\":2024,\"total_emissions_tco2\":88,"
                + "\"fuel_combustion_tco2\":10,\"electricity_emissions_tco2\":20,\"heat_emissions_tco2\":58}]}"
                + "}", 2024);
        assertTrue(fromSnap.contains("\"report_year\":2024"));
        assertTrue(fromSnap.contains("88"));
    }
}
