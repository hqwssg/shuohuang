package com.ruoyi.carbon.report.generation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import com.alibaba.fastjson2.JSONObject;

class ActivityProseSanitizerTest
{
    @Test
    void dropsSpecialNoteAndUnknownKeys()
    {
        JSONObject cleaned = ActivityProseSanitizer.sanitize("{"
                + "\"special_note\":\"旧稿备注\","
                + "\"natural_gas\":{\"usage\":\"食堂灶具\",\"bogus\":\"x\","
                + "\"source_documents\":[\"天然气统计表\"]},"
                + "\"extra\":{\"foo\":\"bar\"}"
                + "}");
        assertFalse(cleaned.containsKey("special_note"));
        assertFalse(cleaned.containsKey("extra"));
        JSONObject gas = cleaned.getJSONObject("natural_gas");
        assertEquals("食堂灶具", gas.getString("usage"));
        assertFalse(gas.containsKey("bogus"));
        assertEquals("天然气统计表", gas.getJSONArray("source_documents").getString(0));
        assertFalse(ActivityProseSanitizer.isBlank(cleaned));
    }

    @Test
    void specialNoteOnlyIsBlank()
    {
        JSONObject cleaned = ActivityProseSanitizer.sanitize("{\"special_note\":\"只有非法键\"}");
        assertTrue(ActivityProseSanitizer.isBlank(cleaned));
        assertTrue(cleaned.isEmpty() || cleaned.getJSONObject("natural_gas") == null);
    }
}
