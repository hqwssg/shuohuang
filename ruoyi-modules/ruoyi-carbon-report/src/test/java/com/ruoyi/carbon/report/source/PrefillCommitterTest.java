package com.ruoyi.carbon.report.source;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import com.ruoyi.carbon.report.domain.ReportActivityRow;
import com.ruoyi.carbon.report.domain.ReportTask;
import com.ruoyi.carbon.report.domain.dto.PrefillLine;
import com.ruoyi.carbon.report.domain.dto.PrefillResult;
import com.ruoyi.carbon.report.domain.dto.ReportDraft;

class PrefillCommitterTest
{
    private final PrefillCommitter committer = new PrefillCommitter();

    @Test
    void commitWritesSourceChainAndBumpsVersion()
    {
        ReportTask task = task(1);
        PrefillResult result = sample();
        committer.apply(task, result, 88L);
        List<ReportActivityRow> rows = committer.toFuelRows(task, result);
        assertEquals(2, task.getVersion());
        assertEquals(88L, task.getCalculationTemplateId());
        assertEquals("AUTO", rows.get(0).getDataOrigin());
        assertEquals("emission_collection_node_data", rows.get(0).getSourceTable());
        assertEquals("11", rows.get(0).getSourceId());
        assertEquals(0, new BigDecimal("4").compareTo(rows.get(0).getSourceValue()));
    }

    @Test
    void overrideKeepsSourceValue()
    {
        ReportActivityRow row = committer.toFuelRows(task(1), sample()).get(0);
        BigDecimal source = row.getSourceValue();
        committer.overrideNormalized(row, new BigDecimal("8"), "本报告覆盖");
        assertEquals(0, source.compareTo(row.getSourceValue()));
        assertEquals("OVERRIDE", row.getDataOrigin());
        assertEquals("本报告覆盖", row.getOverrideReason());
    }

    @Test
    void refreshCreatesNewVersionWithoutChangingOldSnapshot()
    {
        ReportTask task = task(3);
        String oldSnapshot = "{\"version\":3,\"hash\":\"old\"}";
        PrefillResult next = sample();
        committer.apply(task, next, 88L);
        assertEquals(4, task.getVersion());
        assertEquals("{\"version\":3,\"hash\":\"old\"}", oldSnapshot);
    }

    @Test
    void missingWorkloadIsFlaggedNotSilentZero()
    {
        ReportDraft draft = new ReportDraft();
        draft.setTask(task(1));
        draft.getFuels().add(new ReportActivityRow());
        draft.setWorkloadCount(0);
        var result = ReportValidator.validate(draft);
        assertEquals(Boolean.TRUE, result.get("missingWorkload"));
        assertTrue(result.get("mustFix").toString().contains("工作量"));
    }

    private static ReportTask task(int version)
    {
        ReportTask task = new ReportTask();
        task.setId(9L);
        task.setDeptId(301L);
        task.setVersion(version);
        return task;
    }

    private static PrefillResult sample()
    {
        PrefillResult result = new PrefillResult();
        PrefillLine line = new PrefillLine();
        line.setDataOrigin("AUTO");
        line.setSourceTable("emission_collection_node_data");
        line.setSourceId("11");
        line.setFuelType("gasoline");
        line.setFacility("公务用车");
        line.setSourceValue(new BigDecimal("4"));
        line.setSourceUnit("t");
        line.setNormalizedValue(new BigDecimal("4"));
        line.setNormalizedUnit("t");
        line.setSourceHash("abc");
        result.getFuels().add(line);
        return result;
    }
}
