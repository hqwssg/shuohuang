package com.example.carbon.emission.model.controller;

import com.example.carbon.emission.model.entity.GhgUnit;
import com.example.carbon.emission.model.service.GhgUnitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 温室气体质量单位 Controller
 * <p>
 * 提供碳排放因子的分子单位查询接口，供前端因子单位下拉使用。
 * 数据由数据库初始化固定（kgCO2、tCO2、kgCH4、tCH4、kgN2O、tN2O）。
 */
@RestController
@RequestMapping("/api/ghg-units")
@CrossOrigin(origins = "*")
public class GhgUnitController {

    @Autowired
    private GhgUnitService ghgUnitService;

    /**
     * 查询全部启用的温室气体质量单位（按 sort_order 升序）
     */
    @GetMapping
    public ResponseEntity<List<GhgUnit>> listAll() {
        return ResponseEntity.ok(ghgUnitService.findAllEnabled());
    }

    /**
     * 按 ghg_code 查询单条
     * @param code 温室气体质量单位编码（如 kgCO2、tCO2）
     */
    @GetMapping("/{code}")
    public ResponseEntity<GhgUnit> getByCode(@PathVariable String code) {
        GhgUnit item = ghgUnitService.findByCode(code);
        if (item == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(item);
    }
}
