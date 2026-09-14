package com.ruoyi.carbon.report.generation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class DefaultProcessRunnerTest
{
    @Test
    void truncatesOversizedStderr() throws Exception
    {
        byte[] huge = "x".repeat(5000).getBytes(StandardCharsets.UTF_8);
        String text = DefaultProcessRunner.readLimited(new ByteArrayInputStream(huge), 100);
        assertEquals(100, text.getBytes(StandardCharsets.UTF_8).length);
        assertTrue(text.chars().allMatch(ch -> ch == 'x'));
    }
}
