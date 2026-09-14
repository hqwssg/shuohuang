package com.ruoyi.carbon.report.domain.dto;

import java.util.ArrayList;
import java.util.List;

public class PrefillResult
{
    private final List<PrefillLine> fuels = new ArrayList<>();
    private final List<PrefillLine> electricity = new ArrayList<>();
    private final List<PrefillLine> heat = new ArrayList<>();
    private final List<String> gaps = new ArrayList<>();

    public List<PrefillLine> getFuels() { return fuels; }
    public List<PrefillLine> getElectricity() { return electricity; }
    public List<PrefillLine> getHeat() { return heat; }
    public List<String> getGaps() { return gaps; }
}
