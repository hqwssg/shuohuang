package com.ruoyi.carbon.report.service.impl;

import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.carbon.report.domain.CalculationCandidate;
import com.ruoyi.carbon.report.domain.CalculationNodeDataRow;
import com.ruoyi.carbon.report.domain.ReportActivityRow;
import com.ruoyi.carbon.report.domain.ReportDeptSubject;
import com.ruoyi.carbon.report.domain.ReportTask;
import com.ruoyi.carbon.report.domain.dto.PrefillResult;
import com.ruoyi.carbon.report.mapper.EmissionCalculationMapper;
import com.ruoyi.carbon.report.mapper.ReportActivityMapper;
import com.ruoyi.carbon.report.mapper.ReportDeptSubjectMapper;
import com.ruoyi.carbon.report.mapper.ReportTaskMapper;
import com.ruoyi.carbon.report.source.PrefillAssembler;
import com.ruoyi.carbon.report.source.PrefillCommitter;
import com.ruoyi.carbon.report.source.ReportUnitConverter;
import com.ruoyi.carbon.report.service.IReportSourceService;
import com.ruoyi.carbon.report.service.IReportTaskService;
import com.ruoyi.common.core.constant.HttpStatus;
import com.ruoyi.common.core.exception.ServiceException;

@Service
public class ReportSourceServiceImpl implements IReportSourceService
{
    @Autowired
    private ReportDeptSubjectMapper subjectMapper;

    @Autowired
    private EmissionCalculationMapper calculationMapper;

    @Autowired
    private ReportActivityMapper activityMapper;

    @Autowired
    private ReportTaskMapper reportTaskMapper;

    @Autowired
    private IReportTaskService reportTaskService;

    private final PrefillAssembler assembler = new PrefillAssembler(new ReportUnitConverter());
    private final PrefillCommitter committer = new PrefillCommitter();

    @Override
    public List<CalculationCandidate> listCalculations(Long deptId, Date periodStart, Date periodEnd)
    {
        ReportDeptSubject subject = requireSubject(deptId);
        List<CalculationCandidate> all = calculationMapper.selectCandidates(subject.getSubjectNodeId(), periodStart, periodEnd);
        return assembler.selectable(all);
    }

    @Override
    public PrefillResult preview(Long reportId, Long deptId, Long calculationTemplateId)
    {
        ReportTask task = reportTaskService.get(reportId, deptId);
        ReportDeptSubject subject = requireSubject(task.getDeptId());
        Long calcId = calculationTemplateId != null ? calculationTemplateId : task.getCalculationTemplateId();
        if (calcId == null)
        {
            throw new ServiceException("请选择核算任务", HttpStatus.BAD_REQUEST);
        }
        List<CalculationNodeDataRow> rows = calculationMapper.selectNodeData(calcId, subject.getEnergyAllocation());
        return assembler.assemble(subject.getEnergyAllocation(), rows);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PrefillResult commit(Long reportId, Long deptId, Long calculationTemplateId)
    {
        ReportTask task = reportTaskService.get(reportId, deptId);
        Long calcId = calculationTemplateId != null ? calculationTemplateId : task.getCalculationTemplateId();
        PrefillResult result = preview(reportId, deptId, calcId);
        activityMapper.deleteFuels(reportId, deptId);
        activityMapper.deleteElectricity(reportId, deptId);
        activityMapper.deleteHeat(reportId, deptId);
        for (ReportActivityRow row : committer.toFuelRows(task, result))
        {
            activityMapper.insertFuel(row);
        }
        for (ReportActivityRow row : committer.toElectricityRows(task, result))
        {
            activityMapper.insertElectricity(row);
        }
        for (ReportActivityRow row : committer.toHeatRows(task, result))
        {
            activityMapper.insertHeat(row);
        }
        Integer expected = task.getVersion();
        committer.apply(task, result, calcId);
        task.setVersion(expected);
        if (reportTaskMapper.updateWithVersion(task) == 0)
        {
            throw new ServiceException("version conflict", HttpStatus.CONFLICT);
        }
        task.setVersion(expected + 1);
        return result;
    }

    private ReportDeptSubject requireSubject(Long deptId)
    {
        ReportDeptSubject subject = subjectMapper.selectByDeptId(deptId);
        if (subject == null || !subject.isEnabled())
        {
            throw new ServiceException("当前部门未启用报告对照", HttpStatus.BAD_REQUEST);
        }
        return subject;
    }
}
