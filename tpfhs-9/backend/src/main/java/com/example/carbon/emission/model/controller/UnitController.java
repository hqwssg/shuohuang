package com.example.carbon.emission.model.controller;

import com.example.carbon.emission.model.entity.UnitConversion;
import com.example.carbon.emission.model.entity.UnitStandard;
import com.example.carbon.emission.model.service.UnitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 计量单位统一 Controller
 * <p>
 * 提供标准单位查询、转换系数查询、单位换算等接口，
 * 供前端下拉选项和后端业务换算使用。
 */
@RestController
@RequestMapping("/api/units")
@CrossOrigin(origins = "*")
public class UnitController {

    @Autowired
    private UnitService unitService;

    /**
     * 查询全部启用的标准单位（按 sort_order 升序）
     */
    @GetMapping("/standards")
    public ResponseEntity<List<UnitStandard>> listAllStandards() {
        return ResponseEntity.ok(unitService.findAllEnabled());
    }

    /**
     * 按单位大类查询标准单位
     * @param category ELECTRIC/HEAT/LIQUID_FUEL/GAS_FUEL/SOLID_FUEL/AREA
     */
    @GetMapping("/standards/category/{category}")
    public ResponseEntity<List<UnitStandard>> listByCategory(@PathVariable String category) {
        return ResponseEntity.ok(unitService.findByCategory(category));
    }

    /**
     * 按小类编码查询其全部转换关系
     * @param subcategoryCode 排放数据小类编码（如 FF_D）
     */
    @GetMapping("/conversions/{subcategoryCode}")
    public ResponseEntity<List<UnitConversion>> listConversions(@PathVariable String subcategoryCode) {
        return ResponseEntity.ok(unitService.findConversions(subcategoryCode));
    }

    /**
     * 按小类+源单位查询可转换的目标单位列表（下拉联动用）
     */
    @GetMapping("/conversions/{subcategoryCode}/from/{fromUnitCode}")
    public ResponseEntity<List<UnitConversion>> listConversionsFrom(
            @PathVariable String subcategoryCode,
            @PathVariable String fromUnitCode) {
        return ResponseEntity.ok(unitService.findConversionsFrom(subcategoryCode, fromUnitCode));
    }

    /**
     * 单位换算
     * @param subcategoryCode 排放数据小类编码
     * @param fromUnitCode   源单位编码
     * @param toUnitCode     目标单位编码
     * @param value          源数值
     * @return { from, to, inputValue, outputValue }
     */
    @GetMapping("/convert")
    public ResponseEntity<Map<String, Object>> convert(
            @RequestParam String subcategoryCode,
            @RequestParam String fromUnitCode,
            @RequestParam String toUnitCode,
            @RequestParam BigDecimal value) {
        BigDecimal result = unitService.convert(subcategoryCode, fromUnitCode, toUnitCode, value);
        Map<String, Object> resp = new HashMap<>();
        resp.put("subcategoryCode", subcategoryCode);
        resp.put("fromUnitCode", fromUnitCode);
        resp.put("toUnitCode", toUnitCode);
        resp.put("inputValue", value);
        resp.put("outputValue", result);
        return ResponseEntity.ok(resp);
    }

    /**
     * 校验单位编码是否合法
     */
    @GetMapping("/validate")
    public ResponseEntity<Map<String, Object>> validate(@RequestParam String unitCode) {
        Map<String, Object> resp = new HashMap<>();
        boolean valid = unitService.isValidUnitCode(unitCode);
        resp.put("unitCode", unitCode);
        resp.put("valid", valid);
        return ResponseEntity.ok(resp);
    }
}
