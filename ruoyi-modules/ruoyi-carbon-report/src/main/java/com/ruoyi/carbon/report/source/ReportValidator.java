package com.ruoyi.carbon.report.source;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.ruoyi.carbon.report.domain.dto.ReportDraft;

public final class ReportValidator
{
    private ReportValidator()
    {
    }

    public static Map<String, Object> validate(ReportDraft draft)
    {
        List<String> mustFix = new ArrayList<>();
        List<String> shouldConfirm = new ArrayList<>();
        boolean missingWorkload = draft == null || draft.getWorkloadCount() <= 0;
        if (missingWorkload)
        {
            mustFix.add("缺少工作量，不得静默按 0 生成");
        }
        int activity = draft == null ? 0
                : draft.getFuels().size() + draft.getElectricity().size() + draft.getHeat().size();
        if (activity == 0)
        {
            mustFix.add("缺少自动带入的活动数据");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("mustFix", mustFix);
        result.put("shouldConfirm", shouldConfirm);
        result.put("missingWorkload", missingWorkload);
        result.put("ok", mustFix.isEmpty());
        return result;
    }
}
