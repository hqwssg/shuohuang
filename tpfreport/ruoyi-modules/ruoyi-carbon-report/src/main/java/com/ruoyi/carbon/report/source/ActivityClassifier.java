package com.ruoyi.carbon.report.source;

/**
 * Maps emission subcategory codes to report activity buckets.
 */
public final class ActivityClassifier
{
    private ActivityClassifier()
    {
    }

    public static boolean isFuel(String subcategory)
    {
        return subcategory != null && subcategory.startsWith("FF_");
    }

    public static boolean isElectricity(String subcategory)
    {
        return subcategory != null && (subcategory.startsWith("EL") || "PE".equalsIgnoreCase(subcategory)
                || "purchased-electricity".equalsIgnoreCase(subcategory));
    }

    public static boolean isHeat(String subcategory)
    {
        return subcategory != null && subcategory.startsWith("PH_");
    }

    public static String bucket(String subcategory)
    {
        if (isFuel(subcategory))
        {
            return "fuel";
        }
        if (isElectricity(subcategory))
        {
            return "electricity";
        }
        if (isHeat(subcategory))
        {
            return "heat";
        }
        return "unknown";
    }
}
