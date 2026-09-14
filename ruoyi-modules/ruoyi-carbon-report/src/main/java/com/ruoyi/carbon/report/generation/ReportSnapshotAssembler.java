package com.ruoyi.carbon.report.generation;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.carbon.report.domain.ReportActivityRow;
import com.ruoyi.carbon.report.domain.ReportDeptProfile;
import com.ruoyi.carbon.report.domain.ReportEquipment;
import com.ruoyi.carbon.report.domain.ReportFile;
import com.ruoyi.carbon.report.domain.ReportMonitoringDevice;
import com.ruoyi.carbon.report.domain.ReportTask;
import com.ruoyi.carbon.report.domain.ReportWorkload;
import com.ruoyi.carbon.report.domain.dto.ReportDraft;

/**
 * Builds an immutable ReportInput-shaped snapshot. Does not recompute emissions.
 */
public class ReportSnapshotAssembler
{
    public String assemble(ReportDraft draft, ReportDeptProfile profile)
    {
        return assemble(draft, profile, null);
    }

    public String assemble(ReportDraft draft, ReportDeptProfile profile, Map<String, Object> factors)
    {
        ReportTask task = draft.getTask();
        JSONObject metadata = parse(task.getMetadataJson());
        JSONObject entity = parse(task.getEntityProfileJson());
        JSONObject boundary = parse(task.getOrganizationBoundaryJson());
        JSONObject notes = parse(task.getEmissionBoundaryNotesJson());
        JSONObject prose = ActivityProseSanitizer.sanitize(task.getActivityProseJson());
        JSONObject chapter6 = parse(task.getChapter6Json());
        if (profile != null)
        {
            metadata.putIfAbsent("entity_name", nvl(profile.getLegalName()));
            metadata.putIfAbsent("compiler", nvl(profile.getDefaultCompiler()));
            metadata.putIfAbsent("approver", nvl(profile.getDefaultApprover()));
            metadata.putIfAbsent("validator", nvl(profile.getDefaultValidator()));
            metadata.putIfAbsent("reviewer", nvl(profile.getDefaultReviewer()));
            metadata.putIfAbsent("checker", nvl(profile.getDefaultChecker()));
            metadata.putIfAbsent("authors", nvl(profile.getDefaultAuthors()));
            metadata.putIfAbsent("header_text", nvl(profile.getHeaderTemplate()));
            entity.putIfAbsent("short_name", nvl(profile.getShortName()));
            entity.putIfAbsent("business_description", nvl(profile.getBusinessDescription()));
            entity.putIfAbsent("carbon_department", nvl(profile.getCarbonDepartment()));
            entity.putIfAbsent("company_overview", nvl(profile.getCompanyOverview()));
            entity.putIfAbsent("process_description", nvl(profile.getProcessDescription()));
            fillIfBlank(boundary, "boundary_description", profile.getDefaultOrgBoundary());
            fillIfBlank(boundary, "accounting_method", profile.getDefaultAccountingMethod());
            fillIfBlank(notes, "fuel_boundary_exclusion", profile.getDefaultExclusionNote());
            if (ActivityProseSanitizer.isBlank(prose))
            {
                prose = ActivityProseSanitizer.sanitize(profile.getDefaultActivityProseJson());
            }
            if (chapter6.isEmpty() || chapter6.getJSONObject("emission_intensity") == null)
            {
                JSONObject fromProfile = parse(profile.getDefaultChapter6Json());
                if (!fromProfile.isEmpty())
                {
                    chapter6 = fromProfile;
                }
            }
        }
        metadata.putIfAbsent("entity_name", nvl(task.getTitle()));
        metadata.putIfAbsent("report_year", task.getReportYear());
        metadata.putIfAbsent("report_period", task.getTitle());
        metadata.putIfAbsent("compiler", "");
        metadata.putIfAbsent("compile_date", LocalDate.now().toString());
        entity.putIfAbsent("short_name", "");
        entity.putIfAbsent("business_description", "");
        entity.putIfAbsent("carbon_department", "");

        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("entity_code", profile == null || blank(profile.getEntityCode()) ? "suning" : profile.getEntityCode());
        snapshot.put("period_grain", "MONTH".equalsIgnoreCase(task.getPeriodType()) ? "month" : "year");
        snapshot.put("metadata", metadata);
        snapshot.put("entity_profile", entity);
        snapshot.put("organization_boundary", boundary);
        snapshot.put("activity_prose", prose);
        snapshot.put("fuel_activity", fuels(draft.getFuels()));
        snapshot.put("electricity_activity", electricity(draft.getElectricity()));
        snapshot.put("heat_activity", heat(draft.getHeat()));
        snapshot.put("equipment", equipment(draft, profile));
        snapshot.put("workload", workload(draft.getWorkload()));
        snapshot.put("monitoring_devices", monitoring(draft.getMonitoring()));
        snapshot.put("report_tables", List.of());
        snapshot.put("images", ReportImageSlots.snapshotImages(draft.getImages()));
        snapshot.put("chapter6", chapter6);
        snapshot.put("emission_boundary_notes", notes);
        if (task.getPriorYearJson() != null && !task.getPriorYearJson().isBlank())
        {
            JSONObject prior = parse(task.getPriorYearJson());
            if (prior.get("total_emissions_tco2") != null)
            {
                snapshot.put("prior_year", prior);
            }
        }
        if (factors != null && !factors.isEmpty())
        {
            snapshot.put("factors", factors);
        }
        return JSON.toJSONString(snapshot);
    }

    private static List<Map<String, Object>> fuels(List<ReportActivityRow> rows)
    {
        List<Map<String, Object>> list = new ArrayList<>();
        for (ReportActivityRow row : rows)
        {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("fuel_type", row.getFuelType());
            item.put("facility", nvl(row.getFacility()));
            item.put("quantity", row.getNormalizedValue() == null ? 0 : row.getNormalizedValue());
            item.put("unit", nvl(row.getNormalizedUnit()));
            item.put("source_name", nvl(row.getSourceName()));
            list.add(item);
        }
        return list;
    }

    private static List<Map<String, Object>> electricity(List<ReportActivityRow> rows)
    {
        List<Map<String, Object>> list = new ArrayList<>();
        for (ReportActivityRow row : rows)
        {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("scope", row.getScope() == null ? "purchased-electricity" : row.getScope());
            item.put("quantity_mwh", row.getNormalizedValue() == null ? 0 : row.getNormalizedValue());
            item.put("source_name", nvl(row.getSourceName()));
            list.add(item);
        }
        return list;
    }

    private static List<Map<String, Object>> heat(List<ReportActivityRow> rows)
    {
        List<Map<String, Object>> list = new ArrayList<>();
        for (ReportActivityRow row : rows)
        {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("site", row.getSite() == null ? nvl(row.getFacility()) : row.getSite());
            item.put("quantity_gj", row.getNormalizedValue() == null ? 0 : row.getNormalizedValue());
            item.put("source_name", nvl(row.getSourceName()));
            list.add(item);
        }
        return list;
    }

    private static List<Map<String, Object>> equipment(ReportDraft draft, ReportDeptProfile profile)
    {
        if (draft.getEquipment() != null && !draft.getEquipment().isEmpty())
        {
            return equipmentFromRows(draft.getEquipment());
        }
        if (profile != null && !blank(profile.getDefaultEquipmentJson()))
        {
            return equipmentFromJson(profile.getDefaultEquipmentJson());
        }
        return List.of();
    }

    private static List<Map<String, Object>> equipmentFromRows(List<ReportEquipment> rows)
    {
        List<Map<String, Object>> list = new ArrayList<>();
        for (ReportEquipment row : rows)
        {
            list.add(equipmentItem(
                    nvl(row.getName()),
                    nvl(row.getCategory()),
                    row.getQuantity() == null ? 0 : row.getQuantity(),
                    nvl(row.getUnit()),
                    nvl(row.getModel()),
                    nvl(row.getEnergyType()),
                    nvl(row.getRemark())));
        }
        return list;
    }

    private static List<Map<String, Object>> equipmentFromJson(String json)
    {
        JSONArray items;
        try
        {
            items = JSON.parseArray(json);
        }
        catch (Exception ex)
        {
            return List.of();
        }
        if (items == null || items.isEmpty())
        {
            return List.of();
        }
        List<Map<String, Object>> list = new ArrayList<>();
        for (int i = 0; i < items.size(); i++)
        {
            JSONObject item = items.getJSONObject(i);
            if (item == null)
            {
                continue;
            }
            String energy = item.getString("energy_type");
            if (blank(energy))
            {
                energy = item.getString("energyType");
            }
            list.add(equipmentItem(
                    nvl(item.getString("name")),
                    nvl(item.getString("category")),
                    item.get("quantity") == null ? 0 : item.get("quantity"),
                    nvl(item.getString("unit")),
                    nvl(item.getString("model")),
                    nvl(energy),
                    nvl(item.getString("remark"))));
        }
        return list;
    }

    private static Map<String, Object> equipmentItem(String name, String category, Object quantity,
            String unit, String model, String energyType, String remark)
    {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("name", name);
        item.put("category", category);
        item.put("quantity", quantity);
        item.put("unit", unit);
        item.put("model", model);
        item.put("energy_type", energyType);
        item.put("remark", remark);
        return item;
    }

    private static List<Map<String, Object>> workload(List<ReportWorkload> rows)
    {
        List<Map<String, Object>> list = new ArrayList<>();
        if (rows == null)
        {
            return list;
        }
        for (ReportWorkload row : rows)
        {
            if (row.getQuantity() == null || row.getQuantity().signum() <= 0)
            {
                continue;
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", nvl(row.getName()));
            item.put("quantity", row.getQuantity());
            item.put("unit", nvl(row.getUnit()));
            list.add(item);
        }
        return list;
    }

    private static List<Map<String, Object>> monitoring(List<ReportMonitoringDevice> rows)
    {
        List<Map<String, Object>> list = new ArrayList<>();
        if (rows == null)
        {
            return list;
        }
        for (ReportMonitoringDevice row : rows)
        {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", nvl(row.getName()));
            item.put("model", nvl(row.getModel()));
            item.put("accuracy", nvl(row.getAccuracy()));
            item.put("measurement_range", nvl(row.getMeasurementRange()));
            item.put("location", nvl(row.getLocation()));
            item.put("calibration_frequency", nvl(row.getCalibrationFrequency()));
            list.add(item);
        }
        return list;
    }

    private static JSONObject parse(String json)
    {
        if (json == null || json.isBlank())
        {
            return new JSONObject();
        }
        return JSON.parseObject(json);
    }

    private static boolean blank(String value)
    {
        return value == null || value.isBlank();
    }

    private static void fillIfBlank(JSONObject target, String key, String fallback)
    {
        if (blank(target.getString(key)) && !blank(fallback))
        {
            target.put(key, fallback);
        }
    }

    private static String nvl(String value)
    {
        return value == null ? "" : value;
    }
}
