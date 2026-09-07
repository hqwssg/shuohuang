package com.example.carbon.emission.model.controller;

import com.example.carbon.emission.model.entity.FactorUnit;
import com.example.carbon.emission.model.service.FactorUnitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 碳排放因子单位 Controller
 * <p>
 * 提供按小类查询可选因子单位的接口，供前端因子单位下拉使用。
 * 每行一个合法的因子单位组合（分子+分母），按小类限定。
 */
@RestController
@RequestMapping("/api/factor-units")
@CrossOrigin(origins = "*")
public class FactorUnitController {

    @Autowired
    private FactorUnitService factorUnitService;

    /**
     * 查询全部启用的因子单位（按 sort_order 升序）
     */
    @GetMapping
    public ResponseEntity<List<FactorUnit>> listAll() {
        return ResponseEntity.ok(factorUnitService.findAllEnabled());
    }

    /**
     * 按排放数据小类编码查询启用单位（按 sort_order 升序）
     * @param code 排放数据小类编码，如 PE_PF、FF_D
     */
    @GetMapping("/by-subcategory/{code}")
    public ResponseEntity<List<FactorUnit>> listBySubcategory(@PathVariable String code) {
        return ResponseEntity.ok(factorUnitService.findBySubcategoryCode(code));
    }

    /**
     * 按多个小类批量查询启用单位（前端一次拉取多小类下拉用）
     * <p>
     * 例：GET /api/factor-units/by-subcategories?codes=PE_PF,FF_D
     * @param codes 逗号分隔的小类编码
     */
    @GetMapping("/by-subcategories")
    public ResponseEntity<List<FactorUnit>> listBySubcategories(@RequestParam("codes") String codes) {
        if (codes == null || codes.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        List<String> codeList = Arrays.stream(codes.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
        return ResponseEntity.ok(factorUnitService.findBySubcategoryCodes(codeList));
    }
}
