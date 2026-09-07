package com.example.carbon.emission.model.controller;

import com.example.carbon.emission.model.entity.UnitConversion;
import com.example.carbon.emission.model.service.UnitService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 碳排放核算单位转换系数设置 Controller
 * <p>
 * 针对不同排放数据小类（emission_subcategory）维护成对的单位转换系数，
 * 例如汽油 L↔t（双向显式存储）。源单位、目标单位必须从标准单位
 * （emission_unit_standard）下拉选择，禁止人工录入，避免标准不统一导致后续计算失败。
 * <p>
 * 转换公式：<strong>目标值 = 源值 × conversionFactor</strong>
 */
@RestController
@RequestMapping("/api/unit-conversions")
@CrossOrigin(origins = "*")
public class UnitConversionController {

    private static final Logger log = LoggerFactory.getLogger(UnitConversionController.class);

    @Autowired
    private UnitService unitService;

    /**
     * 查询全部单位转换系数（按小类编码、源单位编码升序）
     *
     * @return 转换系数列表；无数据返回 200 + 空列表
     */
    @GetMapping
    public ResponseEntity<List<UnitConversion>> listAll() {
        return ResponseEntity.ok(unitService.findAllConversions());
    }

    /**
     * 新增单位转换系数
     * <p>
     * 入参：<br>
     * - subcategoryCode    : 排放数据小类编码（必填）<br>
     * - fromUnitCode       : 源单位编码（必填，必须为标准单位）<br>
     * - toUnitCode         : 目标单位编码（必填，必须为标准单位，且 ≠ 源单位）<br>
     * - conversionFactor   : 转换系数（必填，目标值 = 源值 × conversionFactor）<br>
     * - remark             : 备注（可选）
     *
     * @param req 新增请求体
     * @return 200 + {success, message, data}；校验失败或唯一冲突返回 400 + {success, message}
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@RequestBody UnitConversionRequest req) {
        Map<String, Object> resp = new HashMap<>();
        if (isBlank(req.getSubcategoryCode())) {
            return badRequest(resp, "排放数据小类编码不能为空");
        }
        if (isBlank(req.getFromUnitCode())) {
            return badRequest(resp, "源单位编码不能为空");
        }
        if (isBlank(req.getToUnitCode())) {
            return badRequest(resp, "目标单位编码不能为空");
        }
        if (req.getFromUnitCode().trim().equalsIgnoreCase(req.getToUnitCode().trim())) {
            return badRequest(resp, "源单位与目标单位不能相同");
        }
        if (req.getConversionFactor() == null) {
            return badRequest(resp, "转换系数不能为空");
        }
        if (!unitService.isValidUnitCode(req.getFromUnitCode())) {
            return badRequest(resp, "源单位编码非标准单位：" + req.getFromUnitCode());
        }
        if (!unitService.isValidUnitCode(req.getToUnitCode())) {
            return badRequest(resp, "目标单位编码非标准单位：" + req.getToUnitCode());
        }

        UnitConversion entity = new UnitConversion();
        entity.setSubcategoryCode(req.getSubcategoryCode().trim());
        entity.setFromUnitCode(req.getFromUnitCode().trim());
        entity.setToUnitCode(req.getToUnitCode().trim());
        entity.setConversionFactor(req.getConversionFactor());
        entity.setRemark(req.getRemark());

        UnitConversion saved = unitService.createConversion(entity);
        if (saved == null) {
            return badRequest(resp, "该小类下该单位对的转换系数已存在，请直接修改");
        }
        resp.put("success", true);
        resp.put("message", "新增成功");
        resp.put("data", saved);
        return ResponseEntity.ok(resp);
    }

    /**
     * 更新单位转换系数（按 id 定位）
     * <p>
     * 可更新字段：fromUnitCode、toUnitCode、conversionFactor、remark。
     *
     * @param id  待更新记录主键ID
     * @param req 更新请求体
     * @return 200 + {success, message, data}；校验失败或记录不存在返回 400 + {success, message}
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> update(@PathVariable Long id,
                                                      @RequestBody UnitConversionRequest req) {
        Map<String, Object> resp = new HashMap<>();
        if (req.getFromUnitCode() != null && req.getToUnitCode() != null
                && req.getFromUnitCode().trim().equalsIgnoreCase(req.getToUnitCode().trim())) {
            return badRequest(resp, "源单位与目标单位不能相同");
        }
        if (req.getFromUnitCode() != null && !req.getFromUnitCode().isBlank()
                && !unitService.isValidUnitCode(req.getFromUnitCode())) {
            return badRequest(resp, "源单位编码非标准单位：" + req.getFromUnitCode());
        }
        if (req.getToUnitCode() != null && !req.getToUnitCode().isBlank()
                && !unitService.isValidUnitCode(req.getToUnitCode())) {
            return badRequest(resp, "目标单位编码非标准单位：" + req.getToUnitCode());
        }

        UnitConversion entity = new UnitConversion();
        entity.setFromUnitCode(req.getFromUnitCode());
        entity.setToUnitCode(req.getToUnitCode());
        entity.setConversionFactor(req.getConversionFactor());
        entity.setRemark(req.getRemark());

        UnitConversion saved = unitService.updateConversion(id, entity);
        if (saved == null) {
            return badRequest(resp, "记录不存在或该小类下该单位对的转换系数已存在");
        }
        resp.put("success", true);
        resp.put("message", "更新成功");
        resp.put("data", saved);
        return ResponseEntity.ok(resp);
    }

    /**
     * 删除单位转换系数（按 id 定位）
     *
     * @param id 待删除记录主键ID
     * @return 200 + {success, message}；记录不存在时 success=false + "记录不存在"
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable Long id) {
        Map<String, Object> resp = new HashMap<>();
        boolean ok = unitService.deleteConversion(id);
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
     * 构造 400 响应
     */
    private ResponseEntity<Map<String, Object>> badRequest(Map<String, Object> resp, String message) {
        resp.put("success", false);
        resp.put("message", message);
        return ResponseEntity.badRequest().body(resp);
    }

    /**
     * 空白判断
     */
    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    /**
     * 新增/更新请求体
     */
    public static class UnitConversionRequest {
        private String subcategoryCode;
        private String fromUnitCode;
        private String toUnitCode;
        private BigDecimal conversionFactor;
        private String remark;

        public String getSubcategoryCode() { return subcategoryCode; }
        public void setSubcategoryCode(String v) { this.subcategoryCode = v; }
        public String getFromUnitCode() { return fromUnitCode; }
        public void setFromUnitCode(String v) { this.fromUnitCode = v; }
        public String getToUnitCode() { return toUnitCode; }
        public void setToUnitCode(String v) { this.toUnitCode = v; }
        public BigDecimal getConversionFactor() { return conversionFactor; }
        public void setConversionFactor(BigDecimal v) { this.conversionFactor = v; }
        public String getRemark() { return remark; }
        public void setRemark(String v) { this.remark = v; }
    }
}
