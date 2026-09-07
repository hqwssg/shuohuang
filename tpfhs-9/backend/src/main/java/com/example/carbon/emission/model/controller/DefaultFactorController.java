package com.example.carbon.emission.model.controller;

import com.example.carbon.emission.model.entity.DefaultFactor;
import com.example.carbon.emission.model.service.DefaultFactorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 系统缺省碳排放因子设置 Controller
 * <p>
 * 针对不同排放数据小类（emission_subcategory）维护系统默认使用的碳排放因子，
 * 支持新增/查询/修改/删除。因子从对应因子库（电力/化石燃料/热力/固废焚烧/
 * 废水处理）中选择，factor_source 标记来源，factor_id 为因子库主键，
 * 名称/值/单位/说明以快照形式保存。
 */
@RestController
@RequestMapping("/api/default-factors")
@CrossOrigin(origins = "*")
public class DefaultFactorController {

    private static final Logger log = LoggerFactory.getLogger(DefaultFactorController.class);

    /**
     * 合法的因子库来源
     */
    private static final Set<String> VALID_FACTOR_SOURCES = Set.of(
            "ELECTRICITY", "FOSSIL", "THERMAL", "WASTE_INCINERATION", "WASTEWATER");

    @Autowired
    private DefaultFactorService defaultFactorService;

    /**
     * 查询全部启用的缺省因子（status=1，按排放数据小类编码升序）
     *
     * @return 启用状态缺省因子列表；无数据返回 200 + 空列表
     */
    @GetMapping
    public ResponseEntity<List<DefaultFactor>> listAll() {
        return ResponseEntity.ok(defaultFactorService.findAll());
    }

    /**
     * 按模版ID查询缺省因子（按排放数据小类编码升序）
     * templateId 为 null 或 0 时查系统缺省
     *
     * @param templateId 模版ID；为 null/0 时表示查询系统缺省因子
     * @return 该模版下的缺省因子列表；无数据返回 200 + 空列表
     */
    @GetMapping("/by-template/{templateId}")
    public ResponseEntity<List<DefaultFactor>> listByTemplate(@PathVariable Long templateId) {
        return ResponseEntity.ok(defaultFactorService.findByTemplateId(templateId));
    }

    /**
     * 按模版ID删除全部缺省因子（删模版时级联清理）
     *
     * @param templateId 模版ID
     * @return 200 + {success, message}
     */
    @DeleteMapping("/by-template/{templateId}")
    public ResponseEntity<Map<String, Object>> deleteByTemplate(@PathVariable Long templateId) {
        defaultFactorService.deleteByTemplateId(templateId);
        Map<String, Object> resp = new HashMap<>();
        resp.put("success", true);
        resp.put("message", "已清空模版下的缺省因子");
        return ResponseEntity.ok(resp);
    }

    /**
     * 按排放数据小类编码查询单条缺省因子
     *
     * @param subcategoryCode 排放数据小类编码
     * @return 200 + 实体；不存在返回 404
     */
    @GetMapping("/{subcategoryCode}")
    public ResponseEntity<DefaultFactor> getByCode(@PathVariable String subcategoryCode) {
        DefaultFactor item = defaultFactorService.findBySubcategoryCode(subcategoryCode);
        if (item == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(item);
    }

    /**
     * 新增缺省因子
     * <p>
     * 入参：<br>
     * - subcategoryCode    : 排放数据小类编码（必填，唯一）<br>
     * - subcategoryName    : 排放数据小类名称（可选，冗余展示）<br>
     * - factorSource       : 因子库来源（必填，ELECTRICITY/FOSSIL/THERMAL/WASTE_INCINERATION/WASTEWATER）<br>
     * - factorId           : 所选因子在因子库表的主键ID（可选）<br>
     * - factorName         : 因子名称快照（可选）<br>
     * - factorValue        : 因子值快照（可选）<br>
     * - factorUnit         : 因子单位快照（可选）<br>
     * - factorDescription  : 因子说明快照（可选）<br>
     * - remark             : 备注（可选）<br>
     * - createdBy          : 创建人ID（可选）
     *
     * @param req 新增请求体（subcategoryCode、factorSource 必填，其余可选）
     * @return 200 + {success, message, data}；校验失败或重复返回 400 + {success, message}
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@RequestBody DefaultFactorRequest req) {
        Map<String, Object> resp = new HashMap<>();
        if (req.getSubcategoryCode() == null || req.getSubcategoryCode().isBlank()) {
            resp.put("success", false);
            resp.put("message", "排放数据小类编码不能为空");
            return ResponseEntity.badRequest().body(resp);
        }
        if (req.getFactorSource() == null || req.getFactorSource().isBlank()) {
            resp.put("success", false);
            resp.put("message", "因子库来源不能为空");
            return ResponseEntity.badRequest().body(resp);
        }
        String factorSource = req.getFactorSource().trim();
        if (!VALID_FACTOR_SOURCES.contains(factorSource)) {
            resp.put("success", false);
            resp.put("message", "因子库来源非法，必须为 ELECTRICITY/FOSSIL/THERMAL/WASTE_INCINERATION/WASTEWATER 之一");
            return ResponseEntity.badRequest().body(resp);
        }
        DefaultFactor entity = new DefaultFactor();
        entity.setTemplateId(req.getTemplateId());
        entity.setSubcategoryCode(req.getSubcategoryCode().trim());
        entity.setSubcategoryName(req.getSubcategoryName());
        entity.setFactorSource(factorSource);
        entity.setFactorId(req.getFactorId());
        entity.setFactorName(req.getFactorName());
        entity.setFactorValue(req.getFactorValue());
        entity.setFactorUnit(req.getFactorUnit());
        entity.setFactorDescription(req.getFactorDescription());
        entity.setRemark(req.getRemark());
        entity.setCreatedBy(req.getCreatedBy());
        DefaultFactor saved = defaultFactorService.create(entity);
        if (saved == null) {
            resp.put("success", false);
            resp.put("message", "该模版下该能耗小类的缺省因子已存在，请直接修改");
            return ResponseEntity.badRequest().body(resp);
        }
        resp.put("success", true);
        resp.put("message", "新增成功");
        resp.put("data", saved);
        return ResponseEntity.ok(resp);
    }

    /**
     * 更新缺省因子（按 id 定位）
     * <p>
     * 可更新因子选择（factorSource/factorId/factorName/factorValue/factorUnit/
     * factorDescription）、备注、状态；updatedBy 记录更新人。
     *
     * @param id  待更新记录主键ID
     * @param req 更新请求体（含可更新字段及 updatedBy）
     * @return 200 + {success, message, data}；校验失败或记录不存在返回 400 + {success, message}
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> update(@PathVariable Long id,
                                                      @RequestBody DefaultFactorRequest req) {
        Map<String, Object> resp = new HashMap<>();
        if (req.getFactorSource() != null && !req.getFactorSource().isBlank()
                && !VALID_FACTOR_SOURCES.contains(req.getFactorSource().trim())) {
            resp.put("success", false);
            resp.put("message", "因子库来源非法，必须为 ELECTRICITY/FOSSIL/THERMAL/WASTE_INCINERATION/WASTEWATER 之一");
            return ResponseEntity.badRequest().body(resp);
        }
        DefaultFactor entity = new DefaultFactor();
        entity.setSubcategoryName(req.getSubcategoryName());
        if (req.getFactorSource() != null && !req.getFactorSource().isBlank()) {
            entity.setFactorSource(req.getFactorSource().trim());
        }
        entity.setFactorId(req.getFactorId());
        entity.setFactorName(req.getFactorName());
        entity.setFactorValue(req.getFactorValue());
        entity.setFactorUnit(req.getFactorUnit());
        entity.setFactorDescription(req.getFactorDescription());
        entity.setRemark(req.getRemark());
        entity.setStatus(req.getStatus());
        entity.setUpdatedBy(req.getUpdatedBy());
        DefaultFactor saved = defaultFactorService.update(id, entity);
        if (saved == null) {
            resp.put("success", false);
            resp.put("message", "记录不存在");
            return ResponseEntity.badRequest().body(resp);
        }
        resp.put("success", true);
        resp.put("message", "更新成功");
        resp.put("data", saved);
        return ResponseEntity.ok(resp);
    }

    /**
     * 删除缺省因子（按 id 定位）
     *
     * @param id 待删除记录主键ID
     * @return 200 + {success, message}；记录不存在时 success=false + "记录不存在"
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable Long id) {
        Map<String, Object> resp = new HashMap<>();
        boolean ok = defaultFactorService.delete(id);
        if (!ok) {
            resp.put("success", false);
            resp.put("message", "记录不存在");
            return ResponseEntity.ok(resp);
        }
        resp.put("success", true);
        resp.put("message", "删除成功");
        return ResponseEntity.ok(resp);
    }

    /**
     * 新增/更新请求体
     */
    public static class DefaultFactorRequest {
        private Long templateId;
        private String subcategoryCode;
        private String subcategoryName;
        private String factorSource;
        private Long factorId;
        private String factorName;
        private BigDecimal factorValue;
        private String factorUnit;
        private String factorDescription;
        private String remark;
        private Integer status;
        private Long createdBy;
        private Long updatedBy;

        public Long getTemplateId() { return templateId; }
        public void setTemplateId(Long v) { this.templateId = v; }
        public String getSubcategoryCode() { return subcategoryCode; }
        public void setSubcategoryCode(String v) { this.subcategoryCode = v; }
        public String getSubcategoryName() { return subcategoryName; }
        public void setSubcategoryName(String v) { this.subcategoryName = v; }
        public String getFactorSource() { return factorSource; }
        public void setFactorSource(String v) { this.factorSource = v; }
        public Long getFactorId() { return factorId; }
        public void setFactorId(Long v) { this.factorId = v; }
        public String getFactorName() { return factorName; }
        public void setFactorName(String v) { this.factorName = v; }
        public BigDecimal getFactorValue() { return factorValue; }
        public void setFactorValue(BigDecimal v) { this.factorValue = v; }
        public String getFactorUnit() { return factorUnit; }
        public void setFactorUnit(String v) { this.factorUnit = v; }
        public String getFactorDescription() { return factorDescription; }
        public void setFactorDescription(String v) { this.factorDescription = v; }
        public String getRemark() { return remark; }
        public void setRemark(String v) { this.remark = v; }
        public Integer getStatus() { return status; }
        public void setStatus(Integer v) { this.status = v; }
        public Long getCreatedBy() { return createdBy; }
        public void setCreatedBy(Long v) { this.createdBy = v; }
        public Long getUpdatedBy() { return updatedBy; }
        public void setUpdatedBy(Long v) { this.updatedBy = v; }
    }
}
