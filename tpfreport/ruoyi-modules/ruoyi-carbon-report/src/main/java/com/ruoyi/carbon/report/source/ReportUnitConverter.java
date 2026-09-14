package com.ruoyi.carbon.report.source;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

/**
 * Converts calculation units to GHG report units. Missing factor is a gap, not a silent guess.
 */
public class ReportUnitConverter
{
    public static final String UNIT_10K_M3 = "10k_m3";
    public static final String UNIT_T = "t";
    public static final String UNIT_MWH = "MWh";
    public static final String UNIT_GJ = "GJ";

    private final Map<String, BigDecimal> factors = new HashMap<>();

    public ReportUnitConverter()
    {
        put("FF_NG", "万Nm3", UNIT_10K_M3, BigDecimal.ONE);
        put("FF_NG", "万Nm³", UNIT_10K_M3, BigDecimal.ONE);
        put("FF_NG", "10k_m3", UNIT_10K_M3, BigDecimal.ONE);
        put("FF_G", "t", UNIT_T, BigDecimal.ONE);
        put("FF_D", "t", UNIT_T, BigDecimal.ONE);
        put("FF_G", "L", UNIT_T, new BigDecimal("0.00085"));
        put("FF_D", "L", UNIT_T, new BigDecimal("0.00085"));
        put("EL", "MWh", UNIT_MWH, BigDecimal.ONE);
        put("EL", "kWh", UNIT_MWH, new BigDecimal("0.001"));
        put("PH_HD", "GJ", UNIT_GJ, BigDecimal.ONE);
    }

    public void register(String subcategory, String fromUnit, String toUnit, BigDecimal factor)
    {
        put(subcategory, fromUnit, toUnit, factor);
    }

    public Conversion convert(String subcategory, BigDecimal value, String fromUnit)
    {
        if (value == null)
        {
            return Conversion.missing(fromUnit, targetUnit(subcategory));
        }
        String target = targetUnit(subcategory);
        if (fromUnit != null && fromUnit.equalsIgnoreCase(target))
        {
            return Conversion.ok(value, fromUnit, value, target);
        }
        BigDecimal factor = factors.get(key(subcategory, fromUnit, target));
        if (factor == null)
        {
            return Conversion.missing(fromUnit, target);
        }
        return Conversion.ok(value, fromUnit, value.multiply(factor).setScale(6, RoundingMode.HALF_UP), target);
    }

    public static String targetUnit(String subcategory)
    {
        if ("FF_NG".equalsIgnoreCase(subcategory))
        {
            return UNIT_10K_M3;
        }
        if (subcategory != null && subcategory.startsWith("FF_"))
        {
            return UNIT_T;
        }
        if (subcategory != null && subcategory.startsWith("PH_"))
        {
            return UNIT_GJ;
        }
        return UNIT_MWH;
    }

    public static String reportFuelType(String subcategory)
    {
        if ("FF_NG".equalsIgnoreCase(subcategory))
        {
            return "natural_gas";
        }
        if ("FF_G".equalsIgnoreCase(subcategory))
        {
            return "gasoline";
        }
        if ("FF_D".equalsIgnoreCase(subcategory))
        {
            return "diesel";
        }
        return null;
    }

    private void put(String subcategory, String fromUnit, String toUnit, BigDecimal factor)
    {
        factors.put(key(subcategory, fromUnit, toUnit), factor);
    }

    private static String key(String subcategory, String fromUnit, String toUnit)
    {
        return String.valueOf(subcategory).toUpperCase() + "|" + String.valueOf(fromUnit) + "|" + toUnit;
    }

    public static final class Conversion
    {
        public final boolean ok;
        public final BigDecimal sourceValue;
        public final String sourceUnit;
        public final BigDecimal normalizedValue;
        public final String normalizedUnit;

        private Conversion(boolean ok, BigDecimal sourceValue, String sourceUnit, BigDecimal normalizedValue, String normalizedUnit)
        {
            this.ok = ok;
            this.sourceValue = sourceValue;
            this.sourceUnit = sourceUnit;
            this.normalizedValue = normalizedValue;
            this.normalizedUnit = normalizedUnit;
        }

        public static Conversion ok(BigDecimal sourceValue, String sourceUnit, BigDecimal normalizedValue, String normalizedUnit)
        {
            return new Conversion(true, sourceValue, sourceUnit, normalizedValue, normalizedUnit);
        }

        public static Conversion missing(String sourceUnit, String normalizedUnit)
        {
            return new Conversion(false, null, sourceUnit, null, normalizedUnit);
        }
    }
}
