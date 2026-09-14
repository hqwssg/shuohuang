package com.ruoyi.carbon.report.source;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Guide defaults used only after the user confirms a missing template factor.
 */
public final class ReportFactorDefaults
{
    public static final String VERSION = "transport-default-2024";

    private ReportFactorDefaults()
    {
    }

    public static Map<String, Object> slot(String slot)
    {
        if ("natural_gas".equals(slot))
        {
            return fuel("natural_gas", "5.9564", "0.99", "10k_m3", "389.31");
        }
        if ("gasoline".equals(slot))
        {
            return fuel("gasoline", "0.8467", "0.98", "t", "44.80");
        }
        if ("diesel".equals(slot))
        {
            return fuel("diesel", "0.8753", "0.98", "t", "43.33");
        }
        if ("electricity".equals(slot))
        {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("value", new BigDecimal("0.5366"));
            item.put("source", "default");
            return item;
        }
        if ("heat".equals(slot))
        {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("value", new BigDecimal("0.11"));
            item.put("source", "default");
            return item;
        }
        return null;
    }

    public static String label(String slot)
    {
        if ("natural_gas".equals(slot))
        {
            return "天然气";
        }
        if ("gasoline".equals(slot))
        {
            return "汽油";
        }
        if ("diesel".equals(slot))
        {
            return "柴油";
        }
        if ("electricity".equals(slot))
        {
            return "外购电力";
        }
        if ("heat".equals(slot))
        {
            return "外购热力";
        }
        return slot;
    }

    private static Map<String, Object> fuel(String type, String carbon, String oxidation, String unit, String ncv)
    {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("fuel_type", type);
        item.put("carbon_content", new BigDecimal(carbon));
        item.put("oxidation_rate", new BigDecimal(oxidation));
        item.put("expected_unit", unit);
        item.put("ncv", new BigDecimal(ncv));
        item.put("source", "default");
        return item;
    }
}
