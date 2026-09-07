package com.example.carbon.emission.model.controller;

import com.example.carbon.emission.model.entity.EmissionFactorTemplate;
import com.example.carbon.emission.model.service.EmissionFactorTemplateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/factor-templates")
@CrossOrigin(origins = "*")
public class EmissionFactorTemplateController {

    @Autowired
    private EmissionFactorTemplateService service;

    /**
     * 查询全部排放因子模版（按创建时间倒序）
     *
     * @return 模版列表；空列表返回 200 + 空 body
     */
    @GetMapping
    public ResponseEntity<List<EmissionFactorTemplate>> listAll() {
        return ResponseEntity.ok(service.findAll());
    }

    /**
     * 按主键ID查询单条排放因子模版
     *
     * @param id 模版主键ID
     * @return 找到时返回 200 + 模版实体；不存在时返回 404
     */
    @GetMapping("/{id}")
    public ResponseEntity<EmissionFactorTemplate> getById(@PathVariable Long id) {
        EmissionFactorTemplate template = service.findById(id);
        if (template == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(template);
    }

    /**
     * 新增排放因子模版
     * <p>
     * 校验：模版名称必填且不能与已有名称重复；通过后构造实体并调用 service.create，
     * service 层会再次校验唯一性。名称重复返回 400 + "模版名称已存在"。
     *
     * @param req 模版请求体（templateName 必填，templateDescription/isShared/createdBy 可选）
     * @return 200 + {success, message, data}；校验失败返回 400 + {success, message}
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@RequestBody TemplateRequest req) {
        Map<String, Object> response = new HashMap<>();
        if (req.getTemplateName() == null || req.getTemplateName().isBlank()) {
            response.put("success", false);
            response.put("message", "模版名称不能为空");
            return ResponseEntity.badRequest().body(response);
        }
        EmissionFactorTemplate entity = new EmissionFactorTemplate();
        entity.setTemplateName(req.getTemplateName().trim());
        entity.setTemplateDescription(req.getTemplateDescription());
        entity.setIsShared(req.getIsShared());
        entity.setCreatedBy(req.getCreatedBy());
        EmissionFactorTemplate saved = service.create(entity);
        if (saved == null) {
            response.put("success", false);
            response.put("message", "模版名称已存在");
            return ResponseEntity.badRequest().body(response);
        }
        response.put("success", true);
        response.put("message", "新增成功");
        response.put("data", saved);
        return ResponseEntity.ok(response);
    }

    /**
     * 按主键ID更新排放因子模版
     * <p>
     * 仅更新非空字段：名称、描述、是否共享、状态、更新人。记录不存在返回 400 + "记录不存在"。
     *
     * @param id  模版主键ID
     * @param req 更新请求体（含 templateName/templateDescription/isShared/status/updatedBy）
     * @return 200 + {success, message, data}；记录不存在返回 400 + {success, message}
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> update(@PathVariable Long id, @RequestBody TemplateRequest req) {
        Map<String, Object> response = new HashMap<>();
        EmissionFactorTemplate entity = new EmissionFactorTemplate();
        entity.setTemplateName(req.getTemplateName());
        entity.setTemplateDescription(req.getTemplateDescription());
        entity.setIsShared(req.getIsShared());
        entity.setStatus(req.getStatus());
        entity.setUpdatedBy(req.getUpdatedBy());
        EmissionFactorTemplate saved = service.update(id, entity);
        if (saved == null) {
            response.put("success", false);
            response.put("message", "记录不存在");
            return ResponseEntity.badRequest().body(response);
        }
        response.put("success", true);
        response.put("message", "更新成功");
        response.put("data", saved);
        return ResponseEntity.ok(response);
    }

    /**
     * 按主键ID删除排放因子模版
     *
     * @param id 模版主键ID
     * @return 200 + {success, message}；记录不存在时 success=false + "记录不存在"
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        boolean ok = service.delete(id);
        if (ok) {
            response.put("success", true);
            response.put("message", "删除成功");
            return ResponseEntity.ok(response);
        }
        response.put("success", false);
        response.put("message", "记录不存在");
        return ResponseEntity.ok(response);
    }

    /**
     * 切换模版启停状态（启用/禁用互切，1<->0）
     *
     * @param id  模版主键ID
     * @param req 含 updatedBy 记录更新人
     * @return 200 + {success, message, data}；记录不存在时 success=false
     */
    @PutMapping("/{id}/toggle-status")
    public ResponseEntity<Map<String, Object>> toggleStatus(@PathVariable Long id, @RequestBody TemplateRequest req) {
        Map<String, Object> response = new HashMap<>();
        boolean ok = service.toggleStatus(id, req.getUpdatedBy());
        if (!ok) {
            response.put("success", false);
            response.put("message", "记录不存在");
            return ResponseEntity.ok(response);
        }
        EmissionFactorTemplate saved = service.findById(id);
        response.put("success", true);
        response.put("message", "状态已切换");
        response.put("data", saved);
        return ResponseEntity.ok(response);
    }

    /**
     * 切换模版共享状态（共享/私有互切，1<->0）
     *
     * @param id  模版主键ID
     * @param req 含 updatedBy 记录更新人
     * @return 200 + {success, message, data}；记录不存在时 success=false
     */
    @PutMapping("/{id}/toggle-shared")
    public ResponseEntity<Map<String, Object>> toggleShared(@PathVariable Long id, @RequestBody TemplateRequest req) {
        Map<String, Object> response = new HashMap<>();
        boolean ok = service.toggleShared(id, req.getUpdatedBy());
        if (!ok) {
            response.put("success", false);
            response.put("message", "记录不存在");
            return ResponseEntity.ok(response);
        }
        EmissionFactorTemplate saved = service.findById(id);
        response.put("success", true);
        response.put("message", "共享状态已切换");
        response.put("data", saved);
        return ResponseEntity.ok(response);
    }

    /**
     * 查询模版被核算模版引用的次数（用于前端编辑锁定判断）
     * <p>
     * 传 userId 时仅统计"他人创建的核算模版"引用数（自己引用不锁定）；不传则统计全部引用。
     *
     * @param id     模版主键ID
     * @param userId 当前用户ID（可选）；不为空时仅统计他人引用
     * @return 200 + {success, count}
     */
    @GetMapping("/{id}/reference-count")
    public ResponseEntity<Map<String, Object>> getReferenceCount(
            @PathVariable Long id,
            @RequestParam(value = "userId", required = false) Long userId) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        // 传 userId 时仅统计"他人创建的核算模版"引用数（用于编辑锁定判断）；不传则统计全部
        response.put("count", service.countReferences(id, userId));
        return ResponseEntity.ok(response);
    }

    /**
     * 拷贝源模版生成新模版（含其下全部缺省因子）
     * <p>
     * 校验：sourceId 必填、newName 必填且不能与已有名称重复；service 层会拷贝源模版的
     * 描述/共享标识并复制全部 DefaultFactor 到新模版下。
     *
     * @param req 拷贝请求体（sourceId/newName 必填，createdBy 可选）
     * @return 200 + {success, message, data}；校验失败或源不存在返回 400 + {success, message}
     */
    @PostMapping("/copy")
    public ResponseEntity<Map<String, Object>> copyTemplate(@RequestBody CopyRequest req) {
        Map<String, Object> response = new HashMap<>();
        if (req.getSourceId() == null) {
            response.put("success", false);
            response.put("message", "源模版ID不能为空");
            return ResponseEntity.badRequest().body(response);
        }
        if (req.getNewName() == null || req.getNewName().isBlank()) {
            response.put("success", false);
            response.put("message", "新模版名称不能为空");
            return ResponseEntity.badRequest().body(response);
        }
        EmissionFactorTemplate saved = service.copyTemplate(req.getSourceId(), req.getNewName(), req.getCreatedBy());
        if (saved == null) {
            response.put("success", false);
            response.put("message", "拷贝失败：源模版不存在或名称已存在");
            return ResponseEntity.badRequest().body(response);
        }
        response.put("success", true);
        response.put("message", "拷贝成功");
        response.put("data", saved);
        return ResponseEntity.ok(response);
    }

    public static class CopyRequest {
        private Long sourceId;
        private String newName;
        private Long createdBy;

        public Long getSourceId() {
            return sourceId;
        }

        public void setSourceId(Long sourceId) {
            this.sourceId = sourceId;
        }

        public String getNewName() {
            return newName;
        }

        public void setNewName(String newName) {
            this.newName = newName;
        }

        public Long getCreatedBy() {
            return createdBy;
        }

        public void setCreatedBy(Long createdBy) {
            this.createdBy = createdBy;
        }
    }

    public static class TemplateRequest {
        private String templateName;
        private String templateDescription;
        private Integer isShared;
        private Integer status;
        private Long createdBy;
        private Long updatedBy;

        public String getTemplateName() {
            return templateName;
        }

        public void setTemplateName(String templateName) {
            this.templateName = templateName;
        }

        public String getTemplateDescription() {
            return templateDescription;
        }

        public void setTemplateDescription(String templateDescription) {
            this.templateDescription = templateDescription;
        }

        public Integer getIsShared() {
            return isShared;
        }

        public void setIsShared(Integer isShared) {
            this.isShared = isShared;
        }

        public Integer getStatus() {
            return status;
        }

        public void setStatus(Integer status) {
            this.status = status;
        }

        public Long getCreatedBy() {
            return createdBy;
        }

        public void setCreatedBy(Long createdBy) {
            this.createdBy = createdBy;
        }

        public Long getUpdatedBy() {
            return updatedBy;
        }

        public void setUpdatedBy(Long updatedBy) {
            this.updatedBy = updatedBy;
        }
    }
}
