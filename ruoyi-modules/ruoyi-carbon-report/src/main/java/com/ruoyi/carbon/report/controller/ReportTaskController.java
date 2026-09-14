package com.ruoyi.carbon.report.controller;

import java.nio.file.Path;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.alibaba.fastjson2.JSON;
import com.ruoyi.carbon.report.domain.ReportDeptProfile;
import com.ruoyi.carbon.report.domain.ReportFile;
import com.ruoyi.carbon.report.domain.ReportTask;
import com.ruoyi.carbon.report.domain.dto.CreateReportRequest;
import com.ruoyi.carbon.report.domain.dto.ReportDraft;
import com.ruoyi.carbon.report.generation.ReportImageSlots;
import com.ruoyi.carbon.report.service.IReportDeptProfileService;
import com.ruoyi.carbon.report.service.IReportGenerationService;
import com.ruoyi.carbon.report.service.IReportSourceService;
import com.ruoyi.carbon.report.service.IReportTaskService;
import com.ruoyi.carbon.report.source.ReportValidator;
import com.ruoyi.common.core.constant.HttpStatus;
import com.ruoyi.common.core.exception.ServiceException;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.security.annotation.RequiresPermissions;
import com.ruoyi.common.security.utils.SecurityUtils;

@RestController
@RequestMapping("/carbon/report")
public class ReportTaskController extends BaseController
{
    @Autowired
    private IReportTaskService reportTaskService;

    @Autowired
    private IReportSourceService reportSourceService;

    @Autowired
    private IReportGenerationService reportGenerationService;

    @Autowired
    private IReportDeptProfileService reportDeptProfileService;

    @RequiresPermissions("carbon:report:list")
    @GetMapping("/tasks")
    public AjaxResult list(ReportTask query)
    {
        if (query.getDeptId() == null)
        {
            query.setDeptId(currentDept(null));
        }
        else
        {
            query.setDeptId(currentDept(query.getDeptId()));
        }
        List<ReportTask> list = reportTaskService.list(query);
        return success(list);
    }

    @RequiresPermissions("carbon:report:add")
    @Log(title = "碳排放报告", businessType = BusinessType.INSERT)
    @PostMapping("/tasks")
    public AjaxResult create(@RequestBody CreateReportRequest request)
    {
        Long deptId = currentDept(request.getDeptId());
        ReportTask task = reportTaskService.create(request, deptId, SecurityUtils.getUsername());
        if (request.getCalculationTemplateId() != null)
        {
            reportSourceService.commit(task.getId(), deptId, request.getCalculationTemplateId());
            task = reportTaskService.get(task.getId(), deptId);
        }
        return success(task);
    }

    @RequiresPermissions("carbon:report:remove")
    @Log(title = "碳排放报告", businessType = BusinessType.DELETE)
    @DeleteMapping("/tasks/{id}")
    public AjaxResult remove(@PathVariable Long id)
    {
        reportTaskService.remove(id, currentDept(null), SecurityUtils.getUsername());
        return success();
    }

    @RequiresPermissions("carbon:report:query")
    @GetMapping("/tasks/{id}")
    public AjaxResult get(@PathVariable Long id)
    {
        return success(reportTaskService.get(id, currentDept(null)));
    }

    @RequiresPermissions("carbon:report:query")
    @GetMapping("/tasks/{id}/draft")
    public AjaxResult draft(@PathVariable Long id)
    {
        return success(reportTaskService.getDraft(id, currentDept(null)));
    }

    @RequiresPermissions("carbon:report:edit")
    @Log(title = "碳排放报告章节", businessType = BusinessType.UPDATE)
    @PutMapping("/tasks/{id}/sections/{section}")
    public AjaxResult saveSection(@PathVariable Long id, @PathVariable String section, @RequestBody Map<String, Object> body)
    {
        Integer version = body.get("version") == null ? null : Integer.valueOf(String.valueOf(body.get("version")));
        Object data = body.get("data");
        String payload = data == null ? "{}" : JSON.toJSONString(data);
        return success(reportTaskService.saveSection(id, currentDept(null), section, version, payload, SecurityUtils.getUsername()));
    }

    @RequiresPermissions("carbon:report:query")
    @GetMapping("/source/calculations")
    public AjaxResult calculations(@RequestParam Date periodStart, @RequestParam Date periodEnd)
    {
        return success(reportSourceService.listCalculations(currentDept(null), periodStart, periodEnd));
    }

    @RequiresPermissions("carbon:report:edit")
    @PostMapping("/tasks/{id}/prefill/preview")
    public AjaxResult prefillPreview(@PathVariable Long id, @RequestBody(required = false) CreateReportRequest body)
    {
        Long calcId = body == null ? null : body.getCalculationTemplateId();
        return success(reportSourceService.preview(id, currentDept(null), calcId));
    }

    @RequiresPermissions("carbon:report:edit")
    @Log(title = "碳排放报告数据预填", businessType = BusinessType.IMPORT)
    @PostMapping("/tasks/{id}/prefill/commit")
    public AjaxResult prefillCommit(@PathVariable Long id, @RequestBody(required = false) CreateReportRequest body)
    {
        Long calcId = body == null ? null : body.getCalculationTemplateId();
        return success(reportSourceService.commit(id, currentDept(null), calcId));
    }

    @RequiresPermissions("carbon:report:validate")
    @PostMapping("/tasks/{id}/validate")
    public AjaxResult validate(@PathVariable Long id)
    {
        ReportDraft draft = reportTaskService.getDraft(id, currentDept(null));
        return success(ReportValidator.validate(draft));
    }

    @RequiresPermissions("carbon:report:generate")
    @Log(title = "碳排放报告生成", businessType = BusinessType.EXPORT)
    @PostMapping("/tasks/{id}/generation-jobs")
    public AjaxResult generate(@PathVariable Long id, @RequestBody Map<String, Object> body)
    {
        String kind = String.valueOf(body.getOrDefault("outputKind", "docx"));
        boolean ai = Boolean.parseBoolean(String.valueOf(body.getOrDefault("aiEnabled", false)));
        boolean useDefaultFactors = Boolean.parseBoolean(String.valueOf(body.getOrDefault("useDefaultFactors", false)));
        return success(reportGenerationService.create(id, currentDept(null), kind, ai, useDefaultFactors));
    }

    @RequiresPermissions("carbon:report:generate")
    @GetMapping("/tasks/{id}/factor-plan")
    public AjaxResult factorPlan(@PathVariable Long id)
    {
        return success(reportGenerationService.factorPlan(id, currentDept(null)));
    }

    @RequiresPermissions("carbon:report:query")
    @GetMapping("/tasks/{id}/generation-jobs/latest")
    public AjaxResult latestJob(@PathVariable Long id)
    {
        return success(reportGenerationService.latest(id, currentDept(null)));
    }

    @RequiresPermissions("carbon:report:query")
    @GetMapping("/tasks/{id}/generation-jobs/{jobId}")
    public AjaxResult job(@PathVariable Long id, @PathVariable Long jobId)
    {
        return success(reportGenerationService.get(id, jobId, currentDept(null)));
    }

    @RequiresPermissions("carbon:report:download")
    @GetMapping("/artifacts/{id}/download")
    public ResponseEntity<InputStreamResource> download(@PathVariable Long id)
    {
        ReportFile meta = reportGenerationService.downloadMeta(id, currentDept(null));
        InputStreamResource body = new InputStreamResource(reportGenerationService.downloadStream(id, currentDept(null)));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + meta.getOriginalName() + "\"")
                .contentType(MediaType.parseMediaType(meta.getMimeType() == null ? "application/octet-stream" : meta.getMimeType()))
                .body(body);
    }

    @RequiresPermissions("carbon:report:profile:query")
    @GetMapping("/dept-profile/{deptId}")
    public AjaxResult profile(@PathVariable Long deptId)
    {
        return success(reportDeptProfileService.get(currentDept(deptId)));
    }

    @RequiresPermissions("carbon:report:profile:edit")
    @Log(title = "碳排放报告部门配置", businessType = BusinessType.UPDATE)
    @PutMapping("/dept-profile/{deptId}")
    public AjaxResult saveProfile(@PathVariable Long deptId, @RequestBody ReportDeptProfile profile)
    {
        return success(reportDeptProfileService.save(profile, currentDept(deptId), SecurityUtils.getUsername()));
    }

    @RequiresPermissions("carbon:report:query")
    @GetMapping("/default-images/{role}")
    public ResponseEntity<FileSystemResource> defaultImage(@PathVariable String role)
    {
        if (!ReportImageSlots.isRole(role))
        {
            throw new ServiceException("不支持的图片角色", HttpStatus.BAD_REQUEST);
        }
        Path path = reportTaskService.defaultImagePath(role);
        String mime = ReportImageSlots.requireSlot(role).mimeType;
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(mime))
                .header(HttpHeaders.CACHE_CONTROL, "private, max-age=3600")
                .body(new FileSystemResource(path.toFile()));
    }

    @RequiresPermissions("carbon:report:edit")
    @Log(title = "碳排放报告", businessType = BusinessType.UPDATE)
    @PostMapping(value = "/tasks/{id}/images/{role}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AjaxResult uploadImage(@PathVariable Long id, @PathVariable String role, @RequestParam("file") MultipartFile file)
    {
        if (file == null || file.isEmpty())
        {
            throw new ServiceException("请选择图片", HttpStatus.BAD_REQUEST);
        }
        try
        {
            return success(reportTaskService.uploadImage(id, currentDept(null), role, file.getOriginalFilename(),
                    file.getContentType(), file.getSize(), file.getInputStream(), SecurityUtils.getUsername()));
        }
        catch (ServiceException ex)
        {
            throw ex;
        }
        catch (Exception ex)
        {
            throw new ServiceException("上传图片失败: " + ex.getMessage());
        }
    }

    @RequiresPermissions("carbon:report:edit")
    @Log(title = "碳排放报告图片", businessType = BusinessType.DELETE)
    @DeleteMapping("/tasks/{id}/images/{role}")
    public AjaxResult resetImage(@PathVariable Long id, @PathVariable String role)
    {
        reportTaskService.resetImage(id, currentDept(null), role);
        return success();
    }

    private Long currentDept(Long requested)
    {
        Long userDept = SecurityUtils.getLoginUser().getSysUser().getDeptId();
        if (requested == null)
        {
            return userDept;
        }
        com.ruoyi.carbon.report.service.ReportAccessGuard.requireDept(requested, userDept);
        return requested;
    }
}
