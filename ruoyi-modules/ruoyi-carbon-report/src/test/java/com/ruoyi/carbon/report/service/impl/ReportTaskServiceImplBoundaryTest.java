package com.ruoyi.carbon.report.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import org.junit.jupiter.api.Test;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.carbon.report.domain.ReportTask;

class ReportTaskServiceImplBoundaryTest
{
    @Test
    void splitsBoundaryPayloadIntoEngineShapeAndNotes()
    {
        ReportTask task = new ReportTask();
        ReportTaskServiceImpl.applyBoundaryPayload(task, "{"
                + "\"boundary_description\":\"8站3中心\","
                + "\"accounting_method\":\"陆上运输指南\","
                + "\"fuel_boundary_exclusion\":\"机车归机辆\","
                + "\"emission_sources\":[\"化石燃料燃烧\",\"净购入电力\"],"
                + "\"emission_source_rows\":[{\"category\":\"化石燃料燃烧\",\"facility\":\"食堂\",\"location\":\"食堂\",\"source\":\"天然气\"}]"
                + "}");
        JSONObject boundary = JSON.parseObject(task.getOrganizationBoundaryJson());
        assertEquals("8站3中心", boundary.getString("boundary_description"));
        assertEquals("陆上运输指南", boundary.getString("accounting_method"));
        assertEquals(2, boundary.getJSONArray("emission_sources").size());
        assertEquals("食堂", boundary.getJSONArray("emission_source_rows").getJSONObject(0).getString("facility"));
        assertFalse(boundary.containsKey("fuel_boundary_exclusion"));
        JSONObject notes = JSON.parseObject(task.getEmissionBoundaryNotesJson());
        assertEquals("机车归机辆", notes.getString("fuel_boundary_exclusion"));
    }
}
