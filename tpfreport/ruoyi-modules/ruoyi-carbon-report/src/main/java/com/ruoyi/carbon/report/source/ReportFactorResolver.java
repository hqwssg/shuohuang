package com.ruoyi.carbon.report.source;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.ruoyi.carbon.report.domain.DefaultFactorRow;
import com.ruoyi.carbon.report.domain.FactorTemplateLink;
import com.ruoyi.carbon.report.domain.FossilFactorRow;
import com.ruoyi.carbon.report.domain.ReportActivityRow;
import com.ruoyi.carbon.report.domain.dto.ReportDraft;
import com.ruoyi.carbon.report.domain.dto.ReportFactorPlan;
import com.ruoyi.carbon.report.mapper.EmissionCalculationMapper;

/**
 * Resolves report factors from the calculation template's factor template.
 * Does not silently fall back to guide defaults.
 */
public class ReportFactorResolver
{
    private final EmissionCalculationMapper mapper;

    public ReportFactorResolver(EmissionCalculationMapper mapper)
    {
        this.mapper = mapper;
    }

    public ReportFactorPlan plan(ReportDraft draft)
    {
        ReportFactorPlan plan = new ReportFactorPlan();
        List<String> needed = neededSlots(draft);
        plan.setNeeded(needed);
        Long calcId = draft.getTask() == null ? null : draft.getTask().getCalculationTemplateId();
        plan.setCalculationTemplateId(calcId);
        if (needed.isEmpty())
        {
            return plan;
        }
        if (calcId == null)
        {
            markAllMissing(plan, needed, "未选择核算任务");
            return plan;
        }
        FactorTemplateLink link = mapper.selectFactorTemplateLink(calcId);
        if (link != null)
        {
            plan.setEmissionTemplateName(link.getEmissionTemplateName());
            plan.setFactorTemplateId(link.getFactorTemplateId());
            plan.setFactorTemplateName(link.getFactorTemplateName());
        }
        List<DefaultFactorRow> rows = loadTemplateRows(link, calcId);
        if (plan.getFactorTemplateId() == null && rows.isEmpty())
        {
            markAllMissing(plan, needed, "现用核算模板未关联因子模板");
            return plan;
        }
        for (String slot : needed)
        {
            Map<String, Object> resolved = buildSlot(slot, findRow(rows, slot));
            if (resolved == null)
            {
                plan.getMissingSlots().add(slot);
                plan.getMissing().add(ReportFactorDefaults.label(slot));
            }
            else
            {
                plan.getResolved().put(slot, resolved);
            }
        }
        if (!plan.getMissingSlots().isEmpty())
        {
            plan.setReason(plan.getFactorTemplateId() == null
                    ? "现用核算模板未关联因子模板"
                    : "因子模板缺少部分排放因子");
        }
        return plan;
    }

    public Map<String, Object> toSnapshot(ReportFactorPlan plan, boolean useDefaultFallback)
    {
        if (plan == null || plan.getNeeded() == null || plan.getNeeded().isEmpty())
        {
            return null;
        }
        if (!plan.getMissingSlots().isEmpty() && !useDefaultFallback)
        {
            return null;
        }
        Map<String, Map<String, Object>> slots = new LinkedHashMap<>(plan.getResolved());
        boolean usedDefault = false;
        for (String slot : plan.getMissingSlots())
        {
            Map<String, Object> fallback = ReportFactorDefaults.slot(slot);
            if (fallback != null)
            {
                slots.put(slot, fallback);
                usedDefault = true;
            }
        }
        Map<String, Object> factors = new LinkedHashMap<>();
        factors.put("version", usedDefault && slots.size() == plan.getMissingSlots().size()
                ? ReportFactorDefaults.VERSION
                : (plan.getFactorTemplateId() == null ? ReportFactorDefaults.VERSION : "template:" + plan.getFactorTemplateId()));
        factors.put("source", usedDefault ? (plan.getResolved().isEmpty() ? "default" : "mixed") : "template");
        Map<String, Object> fuels = new LinkedHashMap<>();
        for (String slot : List.of("natural_gas", "gasoline", "diesel"))
        {
            if (slots.containsKey(slot))
            {
                fuels.put(slot, slots.get(slot));
            }
        }
        factors.put("fuel_factors", fuels);
        if (slots.containsKey("electricity"))
        {
            factors.put("electricity_factor_tco2_per_mwh", slots.get("electricity").get("value"));
        }
        if (slots.containsKey("heat"))
        {
            factors.put("heat_factor_tco2_per_gj", slots.get("heat").get("value"));
        }
        return factors;
    }

    static List<String> neededSlots(ReportDraft draft)
    {
        Set<String> slots = new LinkedHashSet<>();
        if (draft.getFuels() != null)
        {
            for (ReportActivityRow row : draft.getFuels())
            {
                if (row.getFuelType() != null && !row.getFuelType().isBlank())
                {
                    slots.add(row.getFuelType());
                }
            }
        }
        if (draft.getElectricity() != null && !draft.getElectricity().isEmpty())
        {
            slots.add("electricity");
        }
        if (draft.getHeat() != null && !draft.getHeat().isEmpty())
        {
            slots.add("heat");
        }
        return new ArrayList<>(slots);
    }

    private List<DefaultFactorRow> loadTemplateRows(FactorTemplateLink link, Long calcId)
    {
        List<DefaultFactorRow> rows = new ArrayList<>();
        if (link != null && link.getFactorTemplateId() != null)
        {
            addAll(rows, mapper.selectDefaultFactors(link.getFactorTemplateId()));
        }
        if (link != null && link.getEmissionTemplateId() != null)
        {
            addAll(rows, mapper.selectDefaultFactors(link.getEmissionTemplateId()));
        }
        addAll(rows, mapper.selectDefaultFactors(calcId));
        return rows;
    }

    private static void addAll(List<DefaultFactorRow> target, List<DefaultFactorRow> extra)
    {
        if (extra == null || extra.isEmpty())
        {
            return;
        }
        target.addAll(extra);
    }

    private static void markAllMissing(ReportFactorPlan plan, List<String> needed, String reason)
    {
        plan.setReason(reason);
        for (String slot : needed)
        {
            plan.getMissingSlots().add(slot);
            plan.getMissing().add(ReportFactorDefaults.label(slot));
        }
    }

    private Map<String, Object> buildSlot(String slot, DefaultFactorRow row)
    {
        if (row == null)
        {
            return null;
        }
        if ("electricity".equals(slot))
        {
            BigDecimal value = normalizeElectricity(row.getFactorValue(), row.getFactorUnit());
            if (value == null)
            {
                return null;
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("value", value);
            item.put("source", "template");
            item.put("name", nvl(row.getFactorName()));
            return item;
        }
        if ("heat".equals(slot))
        {
            BigDecimal value = normalizeHeat(row.getFactorValue(), row.getFactorUnit());
            if (value == null)
            {
                return null;
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("value", value);
            item.put("source", "template");
            item.put("name", nvl(row.getFactorName()));
            return item;
        }
        return buildFuel(slot, row);
    }

    private Map<String, Object> buildFuel(String slot, DefaultFactorRow row)
    {
        String unit = "natural_gas".equals(slot) ? ReportUnitConverter.UNIT_10K_M3 : ReportUnitConverter.UNIT_T;
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("fuel_type", slot);
        item.put("expected_unit", unit);
        item.put("source", "template");
        item.put("name", nvl(row.getFactorName()));
        if ("FOSSIL".equalsIgnoreCase(row.getFactorSource()) && row.getFactorId() != null)
        {
            FossilFactorRow fossil = mapper.selectFossilFactor(row.getFactorId());
            if (fossil != null && fossil.getLowerHeatingValue() != null
                    && fossil.getCarbonContentPerUnitHeat() != null
                    && fossil.getFuelOxidationRate() != null)
            {
                BigDecimal carbon = fossil.getLowerHeatingValue()
                        .multiply(fossil.getCarbonContentPerUnitHeat())
                        .divide(new BigDecimal("1000"), 6, RoundingMode.HALF_UP);
                item.put("carbon_content", carbon);
                item.put("oxidation_rate", fossil.getFuelOxidationRate());
                item.put("ncv", fossil.getLowerHeatingValue());
                return item;
            }
            if (fossil != null && fossil.getEmissionFactor() != null)
            {
                item.put("emission_factor", fossil.getEmissionFactor());
                return item;
            }
        }
        if (row.getFactorValue() != null)
        {
            item.put("emission_factor", row.getFactorValue());
            return item;
        }
        return null;
    }

    static DefaultFactorRow findRow(List<DefaultFactorRow> rows, String slot)
    {
        if (rows == null)
        {
            return null;
        }
        for (DefaultFactorRow row : rows)
        {
            if (row == null || (row.getStatus() != null && row.getStatus() != 1))
            {
                continue;
            }
            if (matches(row, slot))
            {
                return row;
            }
        }
        return null;
    }

    static boolean matches(DefaultFactorRow row, String slot)
    {
        String code = row.getSubcategoryCode() == null ? "" : row.getSubcategoryCode().trim();
        String name = row.getSubcategoryName() == null ? "" : row.getSubcategoryName();
        if ("natural_gas".equals(slot))
        {
            return "FF_NG".equalsIgnoreCase(code) || name.contains("天然气");
        }
        if ("gasoline".equals(slot))
        {
            return "FF_G".equalsIgnoreCase(code) || name.contains("汽油");
        }
        if ("diesel".equals(slot))
        {
            return "FF_D".equalsIgnoreCase(code) || name.contains("柴油");
        }
        if ("electricity".equals(slot))
        {
            return "EL".equalsIgnoreCase(code) || "PE".equalsIgnoreCase(code) || "PE_PF".equalsIgnoreCase(code)
                    || name.contains("电力") || name.contains("外购电");
        }
        if ("heat".equals(slot))
        {
            return code.toUpperCase().startsWith("PH_") || name.contains("热力") || name.contains("外购热");
        }
        return false;
    }

    static BigDecimal normalizeElectricity(BigDecimal value, String unit)
    {
        if (value == null)
        {
            return null;
        }
        String normalized = compactUnit(unit);
        if (normalized.isEmpty() || normalized.contains("tco2/mwh") || normalized.contains("kgco2/kwh"))
        {
            return value;
        }
        if (normalized.contains("kgco2/mwh"))
        {
            return value.divide(new BigDecimal("1000"), 6, RoundingMode.HALF_UP);
        }
        if (normalized.contains("tco2/kwh"))
        {
            return value.multiply(new BigDecimal("1000"));
        }
        return value;
    }

    static BigDecimal normalizeHeat(BigDecimal value, String unit)
    {
        if (value == null)
        {
            return null;
        }
        String normalized = compactUnit(unit);
        if (normalized.isEmpty() || normalized.contains("tco2/gj"))
        {
            return value;
        }
        if (normalized.contains("kgco2/gj"))
        {
            return value.divide(new BigDecimal("1000"), 6, RoundingMode.HALF_UP);
        }
        if (normalized.contains("tco2/mj"))
        {
            return value.multiply(new BigDecimal("1000"));
        }
        return value;
    }

    private static String compactUnit(String unit)
    {
        if (unit == null)
        {
            return "";
        }
        return unit.toLowerCase()
                .replace("₂", "2")
                .replace(" ", "")
                .replace("（", "")
                .replace("）", "")
                .replace("(", "")
                .replace(")", "");
    }

    private static String nvl(String value)
    {
        return value == null ? "" : value;
    }
}
