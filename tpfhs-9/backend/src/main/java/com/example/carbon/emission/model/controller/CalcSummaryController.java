package com.example.carbon.emission.model.controller;

import com.example.carbon.emission.model.entity.CalcNodeSummary;
import com.example.carbon.emission.model.service.CalcNodeSummaryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

/**
 * 核算节点汇总 Controller
 * <p>
 * 提供按核算任务、按组织节点 + 周期范围查询节点汇总数据的接口，
 * 供前端核算结果展示、周期性排放对比等场景使用。
 */
@RestController
@RequestMapping("/api/calc-summary")
@CrossOrigin(origins = "*")
public class CalcSummaryController {

    @Autowired
    private CalcNodeSummaryService calcSummaryService;
    @Autowired
    private com.example.carbon.emission.model.security.SummaryScopeService summaryScope;

    /**
     * 按核算任务 ID 查询节点汇总
     *
     * @param templateId      核算任务 ID（calculation_template.id）
     * @param nodeId          可选：节点快照 ID
     * @param subcategoryCode 可选：排放小类编码，如 PE_PF
     * @param l1              可选：一级场景
     * @param l2              可选：二级场景
     * @param l3              可选：三级场景
     * @return 匹配的节点汇总列表
     */
    @GetMapping("/by-template/{templateId}")
    public ResponseEntity<List<CalcNodeSummary>> byTemplate(
            @PathVariable Long templateId,
            @RequestParam(value = "nodeId", required = false) Long nodeId,
            @RequestParam(value = "subcategoryCode", required = false) String subcategoryCode,
            @RequestParam(value = "l1", required = false) String l1,
            @RequestParam(value = "l2", required = false) String l2,
            @RequestParam(value = "l3", required = false) String l3) {
        return ResponseEntity.ok(
                summaryScope.filter(calcSummaryService.queryByTemplate(templateId, nodeId, subcategoryCode, l1, l2, l3)));
    }

    /**
     * 按组织节点 + 周期范围查询节点汇总
     *
     * @param sourceNodeId   源节点 ID（必填）
     * @param cycleStartDate 起始日，格式 yyyy-MM-dd（必填）
     * @param cycleEndDate   截止日，格式 yyyy-MM-dd（必填）
     * @return 匹配周期内所有成功核算的汇总列表；参数非法时返回 400 及中文错误说明
     */
    @GetMapping("/by-node-and-cycle")
    public ResponseEntity<?> byNodeAndCycle(
            @RequestParam(value = "sourceNodeId", required = false) Long sourceNodeId,
            @RequestParam(value = "cycleStartDate", required = false) String cycleStartDate,
            @RequestParam(value = "cycleEndDate", required = false) String cycleEndDate) {
        if (sourceNodeId == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "参数 sourceNodeId 缺失或为空"));
        }
        if (cycleStartDate == null || cycleStartDate.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "参数 cycleStartDate 缺失或为空"));
        }
        if (cycleEndDate == null || cycleEndDate.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "参数 cycleEndDate 缺失或为空"));
        }
        LocalDate start;
        LocalDate end;
        try {
            start = LocalDate.parse(cycleStartDate);
        } catch (DateTimeParseException e) {
            return ResponseEntity.badRequest().body(
                    Map.of("message", "cycleStartDate 日期格式错误，应为 yyyy-MM-dd（如 2026-01-01）"));
        }
        try {
            end = LocalDate.parse(cycleEndDate);
        } catch (DateTimeParseException e) {
            return ResponseEntity.badRequest().body(
                    Map.of("message", "cycleEndDate 日期格式错误，应为 yyyy-MM-dd（如 2026-12-31）"));
        }
        return ResponseEntity.ok(summaryScope.filter(calcSummaryService.queryBySourceNodeAndCycle(sourceNodeId, start, end)));
    }
}
