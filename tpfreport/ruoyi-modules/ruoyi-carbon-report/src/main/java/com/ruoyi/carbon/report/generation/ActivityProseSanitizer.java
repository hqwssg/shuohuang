package com.ruoyi.carbon.report.generation;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;

/**
 * Keeps activity_prose in the engine ActivityProseContext shape. Drops special_note and unknown keys.
 */
public final class ActivityProseSanitizer
{
    private ActivityProseSanitizer()
    {
    }

    public static JSONObject sanitize(String json)
    {
        if (json == null || json.isBlank())
        {
            return new JSONObject();
        }
        JSONObject raw;
        try
        {
            raw = JSON.parseObject(json);
        }
        catch (Exception ex)
        {
            return new JSONObject();
        }
        return sanitize(raw);
    }

    public static JSONObject sanitize(JSONObject raw)
    {
        JSONObject out = new JSONObject();
        if (raw == null)
        {
            return out;
        }
        copySection(out, raw, "natural_gas",
                "usage", "metering_method", "unit_note", "standard_condition_note", "change_note");
        copyDocs(out, raw, "natural_gas");
        copySection(out, raw, "liquid_fuel", "usage", "metering_method");
        copyDocs(out, raw, "liquid_fuel");
        copySection(out, raw, "electricity",
                "non_fossil_power_note", "grid_source", "settlement_note");
        copyDocs(out, raw, "electricity");
        copySection(out, raw, "heat",
                "contract_note", "payment_method", "estimation_method", "no_resale_note");
        copyDocs(out, raw, "heat");
        return out;
    }

    public static boolean isBlank(JSONObject prose)
    {
        if (prose == null || prose.isEmpty())
        {
            return true;
        }
        for (String key : prose.keySet())
        {
            JSONObject section = prose.getJSONObject(key);
            if (section == null || section.isEmpty())
            {
                continue;
            }
            for (String field : section.keySet())
            {
                Object value = section.get(field);
                if (value instanceof JSONArray)
                {
                    if (!((JSONArray) value).isEmpty())
                    {
                        return false;
                    }
                }
                else if (value != null && !String.valueOf(value).isBlank())
                {
                    return false;
                }
            }
        }
        return true;
    }

    private static void copySection(JSONObject out, JSONObject raw, String section, String... fields)
    {
        JSONObject src = raw.getJSONObject(section);
        if (src == null)
        {
            return;
        }
        JSONObject dest = out.getJSONObject(section);
        if (dest == null)
        {
            dest = new JSONObject();
        }
        for (String field : fields)
        {
            String value = src.getString(field);
            if (value != null && !value.isBlank())
            {
                dest.put(field, value);
            }
        }
        if (!dest.isEmpty())
        {
            out.put(section, dest);
        }
    }

    private static void copyDocs(JSONObject out, JSONObject raw, String section)
    {
        JSONObject src = raw.getJSONObject(section);
        if (src == null)
        {
            return;
        }
        Object docsRaw = src.get("source_documents");
        JSONArray docs = new JSONArray();
        if (docsRaw instanceof JSONArray)
        {
            docs = (JSONArray) docsRaw;
        }
        else if (docsRaw instanceof String && !((String) docsRaw).isBlank())
        {
            for (String part : ((String) docsRaw).split("[,，]"))
            {
                docs.add(part);
            }
        }
        if (docs.isEmpty())
        {
            return;
        }
        JSONArray cleaned = new JSONArray();
        for (int i = 0; i < docs.size(); i++)
        {
            String item = String.valueOf(docs.get(i)).trim();
            if (!item.isBlank())
            {
                cleaned.add(item);
            }
        }
        if (cleaned.isEmpty())
        {
            return;
        }
        JSONObject dest = out.getJSONObject(section);
        if (dest == null)
        {
            dest = new JSONObject();
            out.put(section, dest);
        }
        dest.put("source_documents", cleaned);
    }
}
