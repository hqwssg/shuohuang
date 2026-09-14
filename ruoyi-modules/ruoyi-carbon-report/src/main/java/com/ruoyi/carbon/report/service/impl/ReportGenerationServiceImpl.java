package com.ruoyi.carbon.report.service.impl;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.Executor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import com.ruoyi.carbon.report.domain.ReportFile;
import com.ruoyi.carbon.report.domain.ReportGenerationJob;
import com.ruoyi.carbon.report.domain.dto.ReportDraft;
import com.ruoyi.carbon.report.domain.dto.ReportFactorPlan;
import com.ruoyi.carbon.report.generation.CarbonReportRendererProperties;
import com.ruoyi.carbon.report.generation.DiskArtifactStore;
import com.ruoyi.carbon.report.generation.RenderPlan;
import com.ruoyi.carbon.report.generation.RenderResult;
import com.ruoyi.carbon.report.generation.ReportRenderer;
import com.ruoyi.carbon.report.generation.ReportSnapshotAssembler;
import com.ruoyi.carbon.report.generation.SnapshotHasher;
import com.ruoyi.carbon.report.mapper.EmissionCalculationMapper;
import com.ruoyi.carbon.report.mapper.ReportDeptProfileMapper;
import com.ruoyi.carbon.report.mapper.ReportFileMapper;
import com.ruoyi.carbon.report.mapper.ReportGenerationJobMapper;
import com.ruoyi.carbon.report.service.IReportGenerationService;
import com.ruoyi.carbon.report.service.IReportTaskService;
import com.ruoyi.carbon.report.service.ReportAccessGuard;
import com.ruoyi.carbon.report.source.ReportFactorResolver;
import com.ruoyi.common.core.constant.HttpStatus;
import com.ruoyi.common.core.exception.ServiceException;

@Service
public class ReportGenerationServiceImpl implements IReportGenerationService
{
    private final ReportSnapshotAssembler assembler = new ReportSnapshotAssembler();
    private final ReportFactorResolver factorResolver;
    private final IReportTaskService reportTaskService;
    private final ReportDeptProfileMapper profileMapper;
    private final ReportGenerationJobMapper jobMapper;
    private final ReportFileMapper fileMapper;
    private final ReportRenderer reportRenderer;
    private final CarbonReportRendererProperties properties;
    private final Executor executor;
    private final DiskArtifactStore artifacts;

    public ReportGenerationServiceImpl(
            IReportTaskService reportTaskService,
            ReportDeptProfileMapper profileMapper,
            ReportGenerationJobMapper jobMapper,
            ReportFileMapper fileMapper,
            ReportRenderer reportRenderer,
            CarbonReportRendererProperties properties,
            EmissionCalculationMapper calculationMapper,
            @Qualifier("carbonReportExecutor") Executor executor)
    {
        this.reportTaskService = reportTaskService;
        this.profileMapper = profileMapper;
        this.jobMapper = jobMapper;
        this.fileMapper = fileMapper;
        this.reportRenderer = reportRenderer;
        this.properties = properties;
        this.factorResolver = new ReportFactorResolver(calculationMapper);
        this.executor = executor;
        this.artifacts = new DiskArtifactStore(properties.artifactDirPath());
    }

    @Override
    public ReportFactorPlan factorPlan(Long reportId, Long deptId)
    {
        return factorResolver.plan(reportTaskService.getDraft(reportId, deptId));
    }

    @Override
    public ReportGenerationJob create(Long reportId, Long deptId, String outputKind, boolean aiEnabled, boolean useDefaultFactors)
    {
        ReportDraft draft = reportTaskService.getDraft(reportId, deptId);
        ReportFactorPlan plan = factorResolver.plan(draft);
        if (!plan.isComplete() && !useDefaultFactors)
        {
            throw new ServiceException(missingMessage(plan), HttpStatus.BAD_REQUEST);
        }
        Map<String, Object> factors = factorResolver.toSnapshot(plan, useDefaultFactors);
        String snapshot = assembler.assemble(draft, profileMapper.selectByDeptId(deptId), factors);
        String hash = SnapshotHasher.sha256(snapshot);
        String kind = outputKind == null ? "docx" : outputKind;
        ReportFile existing = fileMapper.selectByDeptSnapshotRole(deptId, hash, kindRole(kind));
        if (existing != null)
        {
            return cachedJob(reportId, deptId, kind, hash, snapshot, existing);
        }
        ReportGenerationJob job = new ReportGenerationJob();
        job.setDeptId(deptId);
        job.setReportId(reportId);
        job.setOutputKind(kind);
        job.setStatus("queued");
        job.setProgress(0);
        job.setSnapshotSha256(hash);
        job.setInputSnapshotJson(snapshot);
        job.setAiEnabled(aiEnabled ? "1" : "0");
        job.setLlmCallCount(0);
        jobMapper.insert(job);
        executor.execute(() -> run(job, snapshot, aiEnabled));
        return job;
    }

    @Override
    public ReportGenerationJob latest(Long reportId, Long deptId)
    {
        reportTaskService.get(reportId, deptId);
        ReportGenerationJob job = jobMapper.selectLatestSucceeded(reportId, deptId);
        if (job == null)
        {
            return null;
        }
        if (job.getDocxFileId() == null)
        {
            job.setDocxFileId(jobMapper.selectLatestDocxFileId(reportId, deptId));
        }
        if (job.getPdfFileId() == null)
        {
            job.setPdfFileId(jobMapper.selectLatestPdfFileId(reportId, deptId));
        }
        return job;
    }

    @Override
    public ReportGenerationJob get(Long reportId, Long jobId, Long deptId)
    {
        reportTaskService.get(reportId, deptId);
        ReportGenerationJob job = jobMapper.selectById(jobId);
        if (job == null || !deptId.equals(job.getDeptId()) || !reportId.equals(job.getReportId()))
        {
            ReportAccessGuard.requireTask(null, deptId);
        }
        return job;
    }

    @Override
    public ReportFile downloadMeta(Long artifactId, Long deptId)
    {
        return artifacts.requireDept(fileMapper.selectById(artifactId), deptId);
    }

    @Override
    public InputStream downloadStream(Long artifactId, Long deptId)
    {
        try
        {
            return artifacts.open(downloadMeta(artifactId, deptId), deptId);
        }
        catch (ServiceException ex)
        {
            throw ex;
        }
        catch (Exception ex)
        {
            throw new ServiceException("not found", HttpStatus.NOT_FOUND);
        }
    }

    private void run(ReportGenerationJob job, String snapshot, boolean aiEnabled)
    {
        try
        {
            job.setStatus("processing");
            job.setProgress(10);
            jobMapper.update(job);
            ReportFile docx = fileMapper.selectByDeptSnapshotRole(job.getDeptId(), job.getSnapshotSha256(), "docx");
            RenderPlan plan = RenderPlan.of(job.getOutputKind(), docx != null, aiEnabled);
            job.setProgress(plan.callLlm ? 40 : 70);
            jobMapper.update(job);
            Path workRoot = properties.workDirPath().resolve("job-" + job.getId());
            Files.createDirectories(workRoot);
            Path producedDocx = docx == null ? null : artifacts.resolve(docx);
            if (plan.generateDocx)
            {
                Path tempDocx = workRoot.resolve("report.docx");
                RenderResult result = reportRenderer.generateDocx(snapshot, plan.callLlm, tempDocx);
                Path stored = artifacts.prepare(job.getDeptId(), job.getSnapshotSha256(), "docx", "docx");
                artifacts.moveInto(result.getOutputPath(), stored);
                docx = insertFile(job, stored, "docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
                producedDocx = stored;
                job.setDocxFileId(docx.getId());
                job.setLlmCallCount(plan.callLlm ? 1 : 0);
            }
            else if (docx != null)
            {
                job.setDocxFileId(docx.getId());
                job.setLlmCallCount(0);
            }
            if (plan.convertPdf)
            {
                job.setProgress(90);
                jobMapper.update(job);
                Path tempPdf = workRoot.resolve("report.pdf");
                reportRenderer.convertPdf(producedDocx, tempPdf);
                Path storedPdf = artifacts.prepare(job.getDeptId(), job.getSnapshotSha256(), "pdf", "pdf");
                artifacts.moveInto(tempPdf, storedPdf);
                ReportFile pdf = insertFile(job, storedPdf, "pdf", "application/pdf");
                job.setPdfFileId(pdf.getId());
            }
            job.setProgress(100);
            job.setStatus("succeeded");
            jobMapper.update(job);
            reportTaskService.markGenerated(job.getReportId(), job.getDeptId());
        }
        catch (Exception ex)
        {
            job.setStatus("failed");
            job.setErrorMessage(ex.getMessage());
            jobMapper.update(job);
        }
    }

    private ReportGenerationJob cachedJob(Long reportId, Long deptId, String kind, String hash,
            String snapshot, ReportFile existing)
    {
        ReportGenerationJob job = new ReportGenerationJob();
        job.setDeptId(deptId);
        job.setReportId(reportId);
        job.setOutputKind(kind);
        job.setStatus("succeeded");
        job.setProgress(100);
        job.setSnapshotSha256(hash);
        job.setInputSnapshotJson(snapshot);
        job.setLlmCallCount(0);
        if ("pdf".equalsIgnoreCase(kind))
        {
            job.setPdfFileId(existing.getId());
            ReportFile docx = fileMapper.selectByDeptSnapshotRole(deptId, hash, "docx");
            if (docx != null)
            {
                job.setDocxFileId(docx.getId());
            }
        }
        else
        {
            job.setDocxFileId(existing.getId());
        }
        jobMapper.insert(job);
        reportTaskService.markGenerated(reportId, deptId);
        return job;
    }

    private ReportFile insertFile(ReportGenerationJob job, Path stored, String role, String mime) throws Exception
    {
        ReportFile file = new ReportFile();
        file.setDeptId(job.getDeptId());
        file.setReportId(job.getReportId());
        file.setKind("generated");
        file.setRole(role);
        file.setObjectKey(artifacts.relativeKey(stored));
        file.setOriginalName(stored.getFileName().toString());
        file.setMimeType(mime);
        file.setSizeBytes(Files.size(stored));
        file.setSnapshotSha256(job.getSnapshotSha256());
        fileMapper.insert(file);
        return file;
    }

    private static String kindRole(String kind)
    {
        return "pdf".equalsIgnoreCase(kind) ? "pdf" : "docx";
    }

    private static String missingMessage(ReportFactorPlan plan)
    {
        String reason = plan.getReason() == null || plan.getReason().isBlank() ? "现用核算模板缺少因子" : plan.getReason();
        if (plan.getMissing() == null || plan.getMissing().isEmpty())
        {
            return reason;
        }
        return reason + "：" + String.join("、", plan.getMissing()) + "。请确认是否使用系统默认因子。";
    }
}
