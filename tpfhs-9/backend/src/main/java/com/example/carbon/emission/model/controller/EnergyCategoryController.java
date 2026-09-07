package com.example.carbon.emission.model.controller;

import com.example.carbon.emission.model.entity.EnergyCategory;
import com.example.carbon.emission.model.repository.EnergyCategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 能耗分类数据字典控制器
 * <p>
 * 提供能耗使用场景的三级分类（生产用能/生产辅助用能/综合用能）的查询和管理接口，
 * 支持一级、二级、三级分类的增删改查操作。
 */
@RestController
@RequestMapping("/api/energy-categories")
@CrossOrigin(origins = "*", allowCredentials = "false")
public class EnergyCategoryController {

    @Autowired
    private EnergyCategoryRepository categoryRepository;

    /**
     * 获取所有能耗分类（扁平列表，按层级和排序顺序排列）
     * 前端可按 parentId 和 level 自行构建级联关系
     *
     * @return ResponseEntity 包含所有能耗分类的列表
     */
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAllCategories() {
        List<EnergyCategory> categories = categoryRepository.findAllByOrderByLevelAscSortOrderAsc();
        return ResponseEntity.ok(convertToList(categories));
    }

    /**
     * 获取指定层级的能耗分类
     *
     * @param level 分类层级：1-一级，2-二级，3-三级
     * @return ResponseEntity 包含指定层级分类的列表
     */
    @GetMapping("/level/{level}")
    public ResponseEntity<List<Map<String, Object>>> getByLevel(@PathVariable Integer level) {
        List<EnergyCategory> categories = categoryRepository.findByLevelOrderBySortOrderAsc(level);
        return ResponseEntity.ok(convertToList(categories));
    }

    /**
     * 获取指定父级下的子分类
     *
     * @param parentId 父级分类ID
     * @return ResponseEntity 包含子分类的列表
     */
    @GetMapping("/children/{parentId}")
    public ResponseEntity<List<Map<String, Object>>> getChildren(@PathVariable Long parentId) {
        List<EnergyCategory> categories = categoryRepository.findByParentIdOrderBySortOrderAsc(parentId);
        return ResponseEntity.ok(convertToList(categories));
    }

    /**
     * 新增能耗分类
     * <p>
     * 规则：
     * 1. 分类编码（categoryCode）不可重复，必填
     * 2. 分类名称（categoryName）必填
     * 3. 父级ID（parentId）：一级分类为空，二级分类传一级ID，三级分类传二级ID
     * 4. 层级（level）必须与父级层级连续（父级为N，子级应为N+1）
     * 5. 排序（sortOrder）为空时自动取同级最大值+1
     *
     * @param body 分类数据
     * @return ResponseEntity 包含新增后分类信息的Map
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@RequestBody Map<String, Object> body) {
        String categoryCode = getString(body, "categoryCode");
        String categoryName = getString(body, "categoryName");
        Long parentId = getNullableLong(body, "parentId");
        Integer level = getNullableInteger(body, "level");
        String remark = getString(body, "remark");
        Integer sortOrder = getNullableInteger(body, "sortOrder");
        Integer status = getNullableInteger(body, "status");
        Long createdBy = getNullableLong(body, "createdBy");

        // 必填校验
        if (categoryCode == null || categoryCode.trim().isEmpty()) {
            throw new RuntimeException("分类编码不能为空");
        }
        if (categoryName == null || categoryName.trim().isEmpty()) {
            throw new RuntimeException("分类名称不能为空");
        }
        if (level == null) {
            throw new RuntimeException("分类层级不能为空");
        }

        // 编码唯一性校验
        if (categoryRepository.existsByCategoryCode(categoryCode)) {
            throw new RuntimeException("分类编码已存在: " + categoryCode);
        }

        // 父级与层级一致性校验
        if (parentId != null) {
            EnergyCategory parent = categoryRepository.findById(parentId)
                    .orElseThrow(() -> new RuntimeException("父级分类不存在: " + parentId));
            if (level != parent.getLevel() + 1) {
                throw new RuntimeException("分类层级与父级不匹配，应为: " + (parent.getLevel() + 1));
            }
        } else {
            if (level != 1) {
                throw new RuntimeException("一级分类的父级必须为空");
            }
        }

        // 排序自动取同级最大+1
        if (sortOrder == null) {
            sortOrder = computeNextSortOrder(parentId, level);
        }

        // 状态默认启用
        if (status == null) {
            status = 1;
        }

        EnergyCategory entity = new EnergyCategory();
        entity.setCategoryCode(categoryCode);
        entity.setCategoryName(categoryName);
        entity.setParentId(parentId);
        entity.setLevel(level);
        entity.setRemark(remark);
        entity.setSortOrder(sortOrder);
        entity.setStatus(status);
        entity.setCreatedBy(createdBy);

        EnergyCategory saved = categoryRepository.save(entity);
        return ResponseEntity.ok(convertToMap(saved));
    }

    /**
     * 修改能耗分类
     * <p>
     * 可修改字段：分类编码、分类名称、备注、排序、状态；层级与父级不可修改。
     *
     * @param id   分类ID
     * @param body 分类数据
     * @return ResponseEntity 包含更新后分类信息的Map
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        EnergyCategory entity = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("能耗分类不存在: " + id));

        String categoryCode = getString(body, "categoryCode");
        String categoryName = getString(body, "categoryName");
        String remark = getString(body, "remark");
        Integer sortOrder = getNullableInteger(body, "sortOrder");
        Integer status = getNullableInteger(body, "status");
        Long updatedBy = getNullableLong(body, "updatedBy");

        // 必填校验
        if (categoryCode == null || categoryCode.trim().isEmpty()) {
            throw new RuntimeException("分类编码不能为空");
        }
        if (categoryName == null || categoryName.trim().isEmpty()) {
            throw new RuntimeException("分类名称不能为空");
        }

        // 编码唯一性校验（排除自身）
        if (categoryRepository.existsByCategoryCodeAndIdNot(categoryCode, id)) {
            throw new RuntimeException("分类编码已存在: " + categoryCode);
        }

        entity.setCategoryCode(categoryCode);
        entity.setCategoryName(categoryName);
        if (remark != null) {
            entity.setRemark(remark);
        }
        if (sortOrder != null) {
            entity.setSortOrder(sortOrder);
        }
        if (status != null) {
            entity.setStatus(status);
        }
        entity.setUpdatedBy(updatedBy);

        EnergyCategory saved = categoryRepository.save(entity);
        return ResponseEntity.ok(convertToMap(saved));
    }

    /**
     * 删除能耗分类
     * <p>
     * 注意：由于表上配置了 ON DELETE CASCADE，删除父级分类会级联删除其下属所有子分类。
     * 如需避免误删，前端应在删除前提示用户。
     *
     * @param id 分类ID
     * @return ResponseEntity<Void>
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!categoryRepository.existsById(id)) {
            throw new RuntimeException("能耗分类不存在: " + id);
        }
        categoryRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }

    // ============== 私有辅助方法 ==============

    /**
     * 计算同级下一个排序序号（取当前最大+1）
     *
     * @param parentId 父级ID（一级分类为null）
     * @param level    层级
     * @return 下一个排序序号
     */
    private Integer computeNextSortOrder(Long parentId, Integer level) {
        List<EnergyCategory> siblings;
        if (parentId != null) {
            siblings = categoryRepository.findByParentIdOrderBySortOrderAsc(parentId);
        } else {
            siblings = categoryRepository.findByLevelOrderBySortOrderAsc(level);
        }
        if (siblings.isEmpty()) {
            return 1;
        }
        EnergyCategory last = siblings.get(siblings.size() - 1);
        return (last.getSortOrder() == null ? 0 : last.getSortOrder()) + 1;
    }

    /**
     * 将实体列表转换为前端友好的Map列表
     */
    private List<Map<String, Object>> convertToList(List<EnergyCategory> categories) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (EnergyCategory cat : categories) {
            result.add(convertToMap(cat));
        }
        return result;
    }

    /**
     * 将单个实体转换为前端友好的Map
     */
    private Map<String, Object> convertToMap(EnergyCategory cat) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", cat.getId());
        item.put("categoryCode", cat.getCategoryCode());
        item.put("categoryName", cat.getCategoryName());
        item.put("parentId", cat.getParentId());
        item.put("level", cat.getLevel());
        item.put("remark", cat.getRemark());
        item.put("sortOrder", cat.getSortOrder());
        item.put("status", cat.getStatus());
        item.put("createdBy", cat.getCreatedBy());
        item.put("updatedBy", cat.getUpdatedBy());
        item.put("createdAt", cat.getCreatedAt());
        item.put("updatedAt", cat.getUpdatedAt());
        return item;
    }

    /**
     * 从请求体中安全获取字符串值
     */
    private String getString(Map<String, Object> body, String key) {
        Object val = body.get(key);
        return val == null ? null : val.toString();
    }

    /**
     * 从请求体中安全获取可空Long值
     */
    private Long getNullableLong(Map<String, Object> body, String key) {
        Object val = body.get(key);
        if (val == null || val.toString().trim().isEmpty()) {
            return null;
        }
        return Long.valueOf(val.toString().trim());
    }

    /**
     * 从请求体中安全获取可空Integer值
     */
    private Integer getNullableInteger(Map<String, Object> body, String key) {
        Object val = body.get(key);
        if (val == null || val.toString().trim().isEmpty()) {
            return null;
        }
        return Integer.valueOf(val.toString().trim());
    }
}
