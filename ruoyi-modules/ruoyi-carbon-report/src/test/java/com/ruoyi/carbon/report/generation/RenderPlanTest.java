package com.ruoyi.carbon.report.generation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class RenderPlanTest
{
    @Test
    void pdfWithExistingDocxDoesNotGenerateOrCallLlm()
    {
        RenderPlan plan = RenderPlan.of("pdf", true, true);
        assertFalse(plan.generateDocx);
        assertTrue(plan.convertPdf);
        assertFalse(plan.callLlm);
    }

    @Test
    void firstDocxWithAiCallsLlm()
    {
        RenderPlan plan = RenderPlan.of("docx", false, true);
        assertTrue(plan.generateDocx);
        assertFalse(plan.convertPdf);
        assertTrue(plan.callLlm);
    }
}
