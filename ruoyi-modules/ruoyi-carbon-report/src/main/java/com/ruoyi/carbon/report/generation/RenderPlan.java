package com.ruoyi.carbon.report.generation;

public final class RenderPlan
{
    public final boolean generateDocx;
    public final boolean convertPdf;
    public final boolean callLlm;

    public RenderPlan(boolean generateDocx, boolean convertPdf, boolean callLlm)
    {
        this.generateDocx = generateDocx;
        this.convertPdf = convertPdf;
        this.callLlm = callLlm;
    }

    public static RenderPlan of(String outputKind, boolean hasDocx, boolean aiEnabled)
    {
        boolean pdf = "pdf".equalsIgnoreCase(outputKind);
        boolean generate = !hasDocx;
        return new RenderPlan(generate, pdf, generate && aiEnabled);
    }
}
