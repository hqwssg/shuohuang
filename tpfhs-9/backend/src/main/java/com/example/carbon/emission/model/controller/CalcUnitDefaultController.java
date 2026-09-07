package com.example.carbon.emission.model.controller;

import com.example.carbon.emission.model.entity.CalcUnitDefault;
import com.example.carbon.emission.model.service.CalcUnitDefaultService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 碳排放核算能耗缺省单位 Controller
 * <p>
 * 行数据固定（按数据库初始化的 22 条排放数据小类），
 * 前端只允许修改每行的核算单位、报告单位、备注，
 * 不提供新增/删除接口，避免因增减行导致与字典不一致。
 * 换算系数不再存储，由 emission_unit_conversion 表动态查询。
 */
@RestController
@RequestMapping("/api/calc-unit-defaults")
@CrossOrigin(origins = "*")
public class CalcUnitDefaultController {

    private static final Logger log = LoggerFactory.getLogger(CalcUnitDefaultController.class);

    @Autowired
    private CalcUnitDefaultService calcUnitDefaultService;

    /**
     * 查询全部缺省单位设置（按排放数据小类编码升序）
     */
    @GetMapping
    public ResponseEntity<List<CalcUnitDefault>> listAll() {
        List<CalcUnitDefault> list = calcUnitDefaultService.findAll();
        return ResponseEntity.ok(list);
    }

    /**
     * 按排放数据小类编码查询单条记录
     */
    @GetMapping("/{subcategoryCode}")
    public ResponseEntity<CalcUnitDefault> getByCode(@PathVariable String subcategoryCode) {
        CalcUnitDefault item = calcUnitDefaultService.findBySubcategoryCode(subcategoryCode);
        if (item == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(item);
    }

    /**
     * 更新单条缺省单位设置（按 subcategoryCode 定位）
     * <p>
     * 入参：<br>
     * - subcategoryCode  : 排放数据小类编码（必填，路径参数）<br>
     * - calculationUnit  : 核算单位编码（必填，必须为 emission_unit_standard.unit_code）<br>
     * - reportUnit       : 报告单位编码（必填，必须为 emission_unit_standard.unit_code）<br>
     * - remark           : 备注说明（可选）<br>
     * - updatedBy        : 更新人ID（可选）
     */
    @PutMapping("/{subcategoryCode}")
    public ResponseEntity<Map<String, Object>> update(@PathVariable String subcategoryCode,
                                                      @RequestBody UpdateRequest req) {
        Map<String, Object> resp = new HashMap<>();
        if (req.getCalculationUnit() == null || req.getCalculationUnit().isBlank()) {
            resp.put("success", false);
            resp.put("message", "核算单位不能为空");
            return ResponseEntity.badRequest().body(resp);
        }
        if (req.getReportUnit() == null || req.getReportUnit().isBlank()) {
            resp.put("success", false);
            resp.put("message", "报告单位不能为空");
            return ResponseEntity.badRequest().body(resp);
        }
        CalcUnitDefault saved = calcUnitDefaultService.update(
                subcategoryCode,
                req.getCalculationUnit().trim(),
                req.getReportUnit().trim(),
                req.getRemark(),
                req.getUpdatedBy()
        );
        if (saved == null) {
            resp.put("success", false);
            resp.put("message", "记录不存在或单位编码非法（必须为标准单位 emission_unit_standard.unit_code）");
            return ResponseEntity.badRequest().body(resp);
        }
        resp.put("success", true);
        resp.put("message", "更新成功");
        resp.put("data", saved);
        return ResponseEntity.ok(resp);
    }

    /**
     * 更新请求体
     */
    public static class UpdateRequest {
        private String calculationUnit;
        private String reportUnit;
        private String remark;
        private Long updatedBy;

        public String getCalculationUnit() { return calculationUnit; }
        public void setCalculationUnit(String v) { this.calculationUnit = v; }
        public String getReportUnit() { return reportUnit; }
        public void setReportUnit(String v) { this.reportUnit = v; }
        public String getRemark() { return remark; }
        public void setRemark(String v) { this.remark = v; }
        public Long getUpdatedBy() { return updatedBy; }
        public void setUpdatedBy(Long v) { this.updatedBy = v; }
    }
}
