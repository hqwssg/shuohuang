package com.example.carbon.emission.model.controller;

import com.example.carbon.emission.model.entity.FossilFuelCollectionScope;
import com.example.carbon.emission.model.entity.FossilFuelCollectionSubScope;
import com.example.carbon.emission.model.entity.FossilFuelMeterInfo;
import com.example.carbon.emission.model.repository.FossilFuelCollectionScopeRepository;
import com.example.carbon.emission.model.repository.FossilFuelCollectionSubScopeRepository;
import com.example.carbon.emission.model.repository.FossilFuelMeterInfoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/fossil-fuel-collection")
@CrossOrigin(origins = "*", allowCredentials = "false")
public class FossilFuelCollectionController {

    @Autowired
    private FossilFuelCollectionScopeRepository scopeRepository;

    @Autowired
    private FossilFuelCollectionSubScopeRepository subScopeRepository;

    @Autowired
    private FossilFuelMeterInfoRepository meterRepository;

    /**
     * 获取完整的化石燃料采集树形结构
     * <p>
     * 构建从根节点"化石燃料采集"开始的完整树形结构，层级关系为：
     * root → scope（采集范围）→ sub_scope（细分范围）→ meter（采集点，支持多级子表）
     * <p>
     * 采集点节点通过 parent_meter_id 维护父子关系，只有顶级采集点（parent_meter_id=0）
     * 直接挂在细分范围下，子采集点通过 buildMeterNode 递归构建为 children。
     *
     * @return ResponseEntity 包含完整树形结构的 Map，键包括 id、name、nodeType、children 等
     */
    @GetMapping("/tree")
    public ResponseEntity<Map<String, Object>> getTree() {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("id", "root");
        root.put("name", "化石燃料采集");
        root.put("nodeType", "root");

        List<Map<String, Object>> scopeNodes = new ArrayList<>();
        List<FossilFuelCollectionScope> scopes = scopeRepository.findAllByOrderBySortOrderAsc();

        for (FossilFuelCollectionScope scope : scopes) {
            Map<String, Object> scopeNode = new LinkedHashMap<>();
            scopeNode.put("id", scope.getId());
            scopeNode.put("name", scope.getScopeName());
            scopeNode.put("nodeType", "scope");
            scopeNode.put("pinyinCode", scope.getPinyinCode());
            scopeNode.put("sortOrder", scope.getSortOrder());
            scopeNode.put("description", scope.getDescription());
            scopeNode.put("status", scope.getStatus());
            scopeNode.put("createdAt", scope.getCreatedAt());
            scopeNode.put("updatedAt", scope.getUpdatedAt());

            List<Map<String, Object>> subScopeNodes = new ArrayList<>();
            List<FossilFuelCollectionSubScope> subScopes = subScopeRepository.findByCollectionScopeIdOrderBySortOrderAsc(scope.getId());

            for (FossilFuelCollectionSubScope subScope : subScopes) {
                Map<String, Object> subScopeNode = new LinkedHashMap<>();
                subScopeNode.put("id", subScope.getId());
                subScopeNode.put("name", subScope.getSubScopeName());
                subScopeNode.put("nodeType", "sub_scope");
                subScopeNode.put("pinyinCode", subScope.getPinyinCode());
                subScopeNode.put("sortOrder", subScope.getSortOrder());
                subScopeNode.put("description", subScope.getDescription());
                subScopeNode.put("status", subScope.getStatus());
                subScopeNode.put("createdAt", subScope.getCreatedAt());
                subScopeNode.put("updatedAt", subScope.getUpdatedAt());
                subScopeNode.put("scopeName", scope.getScopeName());

                List<FossilFuelMeterInfo> meters = meterRepository.findBySubScopeIdOrderBySortOrderAsc(subScope.getId());
                List<Map<String, Object>> meterNodes = new ArrayList<>();
                for (FossilFuelMeterInfo meter : meters) {
                    if (meter.getParentMeterId() == null || meter.getParentMeterId() == 0L) {
                        meterNodes.add(buildMeterNode(meter, subScope.getSubScopeName(), subScope.getCollectionScopeId()));
                    }
                }
                subScopeNode.put("children", meterNodes);
                subScopeNodes.add(subScopeNode);
            }
            scopeNode.put("children", subScopeNodes);
            scopeNodes.add(scopeNode);
        }
        root.put("children", scopeNodes);
        return ResponseEntity.ok(root);
    }

    /**
     * 递归构建采集点树节点（含子采集点）
     * <p>
     * 将 FossilFuelMeterInfo 实体转换为前端树形结构所需的 Map 节点，
     * 并递归构建其下所有子采集点（通过 parent_meter_id 关联）为 children 列表。
     *
     * @param meter             采集点实体对象
     * @param subScopeName      所属细分范围名称（用于前端展示）
     * @param collectionScopeId 所属采集范围ID（用于"更改所属细分范围"弹窗过滤可选项）
     * @return Map 包含 id、name、nodeType、fuelType、parentMeterId、subScopeId、children 等键的节点
     */
    private Map<String, Object> buildMeterNode(FossilFuelMeterInfo meter, String subScopeName, Long collectionScopeId) {
        Map<String, Object> node = new LinkedHashMap<>();
        node.put("id", meter.getId());
        node.put("name", meter.getName());
        node.put("nodeType", "meter");
        node.put("pinyinCode", meter.getPinyinCode());
        node.put("sortOrder", meter.getSortOrder());
        node.put("fuelType", meter.getFuelType());
        node.put("meterModel", meter.getMeterModel());
        node.put("purposeDescription", meter.getPurposeDescription());
        node.put("parentMeterId", meter.getParentMeterId());
        node.put("energyAllocation", meter.getEnergyAllocation());
        node.put("meterReadingMethod", meter.getMeterReadingMethod());
        node.put("status", meter.getStatus());
        node.put("energyUse", meter.getEnergyUse());
        node.put("autoMeterReadingConfig", meter.getAutoMeterReadingConfig());
        // 9个新字段
        node.put("isCumulative", meter.getIsCumulative());
        node.put("isMobileSource", meter.getIsMobileSource());
        node.put("measurementUnit", meter.getMeasurementUnit());
        node.put("dataSourceSystem", meter.getDataSourceSystem());
        node.put("billingCycleUnit", meter.getBillingCycleUnit());
        node.put("billingCycleStartDate", meter.getBillingCycleStartDate());
        node.put("billingCycleLength", meter.getBillingCycleLength());
        node.put("energyCategoryL1", meter.getEnergyCategoryL1());
        node.put("energyCategoryL2", meter.getEnergyCategoryL2());
        node.put("energyCategoryL3", meter.getEnergyCategoryL3());
        node.put("energyUseCategory", meter.getEnergyUseCategory());
        node.put("subScopeId", meter.getSubScopeId());
        node.put("subScopeName", subScopeName);
        node.put("collectionScopeId", collectionScopeId);
        node.put("parentMeterName", getParentMeterName(meter.getParentMeterId()));

        List<Map<String, Object>> children = new ArrayList<>();
        List<FossilFuelMeterInfo> childMeters = meterRepository.findByParentMeterIdOrderBySortOrderAsc(meter.getId());
        for (FossilFuelMeterInfo child : childMeters) {
            children.add(buildMeterNode(child, subScopeName, collectionScopeId));
        }
        node.put("children", children);
        return node;
    }

    /**
     * 根据上级采集点ID查询其名称
     * 用于在树节点中展示采集点的上级采集点名称（parentMeterName 字段）
     *
     * @param parentMeterId 上级采集点ID，为 null 或 0 表示无上级
     * @return 上级采集点名称字符串；无上级时返回空字符串
     */
    private String getParentMeterName(Long parentMeterId) {
        if (parentMeterId == null || parentMeterId == 0L) {
            return "";
        }
        return meterRepository.findById(parentMeterId)
                .map(FossilFuelMeterInfo::getName)
                .orElse("");
    }

    @GetMapping("/scopes")
    public ResponseEntity<List<FossilFuelCollectionScope>> getAllScopes() {
        return ResponseEntity.ok(scopeRepository.findAllByOrderBySortOrderAsc());
    }

    @GetMapping("/scopes/{id}")
    public ResponseEntity<?> getScopeById(@PathVariable Long id) {
        FossilFuelCollectionScope scope = scopeRepository.findById(id)
                .orElse(null);
        if (scope == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Scope not found");
            return ResponseEntity.status(404).body(error);
        }
        return ResponseEntity.ok(scope);
    }

    @PostMapping("/scopes")
    public ResponseEntity<?> createScope(@RequestBody FossilFuelCollectionScope scope) {
        if (scopeRepository.existsByScopeName(scope.getScopeName())) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Scope name already exists");
            return ResponseEntity.status(400).body(error);
        }
        Integer maxOrder = scopeRepository.findMaxSortOrder();
        scope.setSortOrder(maxOrder == null ? 0 : maxOrder + 1);
        return ResponseEntity.ok(scopeRepository.save(scope));
    }

    @PutMapping("/scopes/{id}")
    public ResponseEntity<?> updateScope(@PathVariable Long id, @RequestBody FossilFuelCollectionScope scopeDetails) {
        FossilFuelCollectionScope scope = scopeRepository.findById(id)
                .orElse(null);
        if (scope == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Scope not found");
            return ResponseEntity.status(404).body(error);
        }
        if (!scope.getScopeName().equals(scopeDetails.getScopeName())
                && scopeRepository.existsByScopeName(scopeDetails.getScopeName())) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Scope name already exists");
            return ResponseEntity.status(400).body(error);
        }
        scope.setScopeName(scopeDetails.getScopeName());
        scope.setPinyinCode(scopeDetails.getPinyinCode());
        if (scopeDetails.getSortOrder() != null) {
            scope.setSortOrder(scopeDetails.getSortOrder());
        }
        scope.setDescription(scopeDetails.getDescription());
        return ResponseEntity.ok(scopeRepository.save(scope));
    }

    @DeleteMapping("/scopes/{id}")
    public ResponseEntity<?> deleteScope(@PathVariable Long id) {
        FossilFuelCollectionScope scope = scopeRepository.findById(id)
                .orElse(null);
        if (scope == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Scope not found");
            return ResponseEntity.status(404).body(error);
        }
        List<FossilFuelCollectionSubScope> subScopes = subScopeRepository.findByCollectionScopeIdOrderBySortOrderAsc(id);
        for (FossilFuelCollectionSubScope subScope : subScopes) {
            List<FossilFuelMeterInfo> meters = meterRepository.findBySubScopeIdOrderBySortOrderAsc(subScope.getId());
            meterRepository.deleteAll(meters);
        }
        subScopeRepository.deleteAll(subScopes);
        scopeRepository.delete(scope);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Deleted successfully");
        return ResponseEntity.ok(response);
    }

    /**
     * 获取所有细分范围列表（含所属采集范围名称）
     * 返回的每项包含 id、name（细分范围名称）、scopeId、scopeName（所属采集范围名称）
     *
     * @return ResponseEntity 包含细分范围列表的 List
     */
    @GetMapping("/sub-scopes")
    public ResponseEntity<List<Map<String, Object>>> getAllSubScopes() {
        List<FossilFuelCollectionSubScope> subScopes = subScopeRepository.findAll();
        List<Map<String, Object>> result = new ArrayList<>();
        for (FossilFuelCollectionSubScope subScope : subScopes) {
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("id", subScope.getId());
            node.put("name", subScope.getSubScopeName());
            node.put("scopeId", subScope.getCollectionScopeId());
            FossilFuelCollectionScope scope = scopeRepository.findById(subScope.getCollectionScopeId()).orElse(null);
            if (scope != null) {
                node.put("scopeName", scope.getScopeName());
            }
            result.add(node);
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/sub-scopes/{id}")
    public ResponseEntity<?> getSubScopeById(@PathVariable Long id) {
        FossilFuelCollectionSubScope subScope = subScopeRepository.findById(id)
                .orElse(null);
        if (subScope == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "SubScope not found");
            return ResponseEntity.status(404).body(error);
        }
        return ResponseEntity.ok(subScope);
    }

    @GetMapping("/scopes/{scopeId}/sub-scopes")
    public ResponseEntity<List<FossilFuelCollectionSubScope>> getSubScopesByScope(@PathVariable Long scopeId) {
        return ResponseEntity.ok(subScopeRepository.findByCollectionScopeIdOrderBySortOrderAsc(scopeId));
    }

    @PostMapping("/sub-scopes")
    public ResponseEntity<?> createSubScope(@RequestBody FossilFuelCollectionSubScope subScope) {
        if (subScopeRepository.existsBySubScopeName(subScope.getSubScopeName())) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "SubScope name already exists");
            return ResponseEntity.status(400).body(error);
        }
        Integer maxOrder = subScopeRepository.findMaxSortOrderByCollectionScopeId(subScope.getCollectionScopeId());
        subScope.setSortOrder(maxOrder == null ? 0 : maxOrder + 1);
        return ResponseEntity.ok(subScopeRepository.save(subScope));
    }

    @PutMapping("/sub-scopes/{id}")
    public ResponseEntity<?> updateSubScope(@PathVariable Long id, @RequestBody FossilFuelCollectionSubScope subScopeDetails) {
        FossilFuelCollectionSubScope subScope = subScopeRepository.findById(id)
                .orElse(null);
        if (subScope == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "SubScope not found");
            return ResponseEntity.status(404).body(error);
        }
        if (!subScope.getSubScopeName().equals(subScopeDetails.getSubScopeName())
                && subScopeRepository.existsBySubScopeName(subScopeDetails.getSubScopeName())) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "SubScope name already exists");
            return ResponseEntity.status(400).body(error);
        }
        subScope.setSubScopeName(subScopeDetails.getSubScopeName());
        subScope.setPinyinCode(subScopeDetails.getPinyinCode());
        if (subScopeDetails.getSortOrder() != null) {
            subScope.setSortOrder(subScopeDetails.getSortOrder());
        }
        subScope.setCollectionScopeId(subScopeDetails.getCollectionScopeId());
        subScope.setDescription(subScopeDetails.getDescription());
        return ResponseEntity.ok(subScopeRepository.save(subScope));
    }

    @DeleteMapping("/sub-scopes/{id}")
    public ResponseEntity<?> deleteSubScope(@PathVariable Long id) {
        FossilFuelCollectionSubScope subScope = subScopeRepository.findById(id)
                .orElse(null);
        if (subScope == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "SubScope not found");
            return ResponseEntity.status(404).body(error);
        }
        List<FossilFuelMeterInfo> meters = meterRepository.findBySubScopeIdOrderBySortOrderAsc(id);
        meterRepository.deleteAll(meters);
        subScopeRepository.delete(subScope);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Deleted successfully");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/meters")
    public ResponseEntity<List<FossilFuelMeterInfo>> getAllMeters() {
        return ResponseEntity.ok(meterRepository.findAll());
    }

    @GetMapping("/meters/{id}")
    public ResponseEntity<?> getMeterById(@PathVariable Long id) {
        FossilFuelMeterInfo meter = meterRepository.findById(id)
                .orElse(null);
        if (meter == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Meter not found");
            return ResponseEntity.status(404).body(error);
        }
        return ResponseEntity.ok(meter);
    }

    @GetMapping("/sub-scopes/{subScopeId}/meters")
    public ResponseEntity<List<FossilFuelMeterInfo>> getMetersBySubScope(@PathVariable Long subScopeId) {
        return ResponseEntity.ok(meterRepository.findBySubScopeIdOrderBySortOrderAsc(subScopeId));
    }

    @PostMapping("/meters")
    public ResponseEntity<FossilFuelMeterInfo> createMeter(@RequestBody FossilFuelMeterInfo meter) {
        Integer maxOrder = meterRepository.findMaxSortOrderBySubScopeId(meter.getSubScopeId());
        meter.setSortOrder(maxOrder == null ? 0 : maxOrder + 1);
        return ResponseEntity.ok(meterRepository.save(meter));
    }

    @PutMapping("/meters/{id}")
    public ResponseEntity<?> updateMeter(@PathVariable Long id, @RequestBody FossilFuelMeterInfo meterDetails) {
        FossilFuelMeterInfo meter = meterRepository.findById(id)
                .orElse(null);
        if (meter == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Meter not found");
            return ResponseEntity.status(404).body(error);
        }
        meter.setName(meterDetails.getName());
        meter.setPinyinCode(meterDetails.getPinyinCode());
        meter.setSubScopeId(meterDetails.getSubScopeId());
        meter.setFuelType(meterDetails.getFuelType());
        meter.setMeterModel(meterDetails.getMeterModel());
        meter.setPurposeDescription(meterDetails.getPurposeDescription());
        meter.setParentMeterId(meterDetails.getParentMeterId());
        meter.setEnergyAllocation(meterDetails.getEnergyAllocation());
        meter.setMeterReadingMethod(meterDetails.getMeterReadingMethod());
        // 9个新字段
        meter.setIsCumulative(meterDetails.getIsCumulative());
        meter.setIsMobileSource(meterDetails.getIsMobileSource());
        meter.setMeasurementUnit(meterDetails.getMeasurementUnit());
        meter.setDataSourceSystem(meterDetails.getDataSourceSystem());
        meter.setBillingCycleUnit(meterDetails.getBillingCycleUnit());
        meter.setBillingCycleStartDate(meterDetails.getBillingCycleStartDate());
        meter.setBillingCycleLength(meterDetails.getBillingCycleLength());
        meter.setEnergyCategoryL1(meterDetails.getEnergyCategoryL1());
        meter.setEnergyCategoryL2(meterDetails.getEnergyCategoryL2());
        meter.setEnergyCategoryL3(meterDetails.getEnergyCategoryL3());
        meter.setEnergyUseCategory(meterDetails.getEnergyUseCategory());
        if (meterDetails.getSortOrder() != null) {
            meter.setSortOrder(meterDetails.getSortOrder());
        }
        meter.setEnergyUse(meterDetails.getEnergyUse());
        meter.setAutoMeterReadingConfig(meterDetails.getAutoMeterReadingConfig());
        return ResponseEntity.ok(meterRepository.save(meter));
    }

    @DeleteMapping("/meters/{id}")
    public ResponseEntity<?> deleteMeter(@PathVariable Long id) {
        FossilFuelMeterInfo meter = meterRepository.findById(id)
                .orElse(null);
        if (meter == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Meter not found");
            return ResponseEntity.status(404).body(error);
        }
        List<FossilFuelMeterInfo> childMeters = meterRepository.findByParentMeterIdOrderBySortOrderAsc(id);
        for (FossilFuelMeterInfo child : childMeters) {
            child.setParentMeterId(0L);
            meterRepository.save(child);
        }
        meterRepository.delete(meter);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Deleted successfully");
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        String nodeType = (String) request.get("nodeType");
        Object statusObj = request.get("status");
        Integer status = null;
        if (statusObj instanceof Number) {
            status = ((Number) statusObj).intValue();
        }
        if (status == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Status field is required");
            return ResponseEntity.status(400).body(error);
        }
        if ("scope".equals(nodeType)) {
            FossilFuelCollectionScope scope = scopeRepository.findById(id).orElse(null);
            if (scope == null) return notFound();
            scope.setStatus(status);
            return ResponseEntity.ok(scopeRepository.save(scope));
        } else if ("sub_scope".equals(nodeType)) {
            FossilFuelCollectionSubScope subScope = subScopeRepository.findById(id).orElse(null);
            if (subScope == null) return notFound();
            subScope.setStatus(status);
            return ResponseEntity.ok(subScopeRepository.save(subScope));
        } else {
            FossilFuelMeterInfo meter = meterRepository.findById(id).orElse(null);
            if (meter == null) return notFound();
            meter.setStatus(status);
            return ResponseEntity.ok(meterRepository.save(meter));
        }
    }

    private ResponseEntity<Map<String, String>> notFound() {
        Map<String, String> error = new HashMap<>();
        error.put("error", "节点不存在");
        return ResponseEntity.status(404).body(error);
    }

    @PutMapping("/scopes/{id}/sort")
    public ResponseEntity<?> updateScopeSort(@PathVariable Long id, @RequestBody Map<String, Integer> sortMap) {
        FossilFuelCollectionScope scope = scopeRepository.findById(id)
                .orElse(null);
        if (scope == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Scope not found");
            return ResponseEntity.status(404).body(error);
        }
        Integer sortOrder = sortMap.get("sortOrder");
        if (sortOrder == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "sortOrder field is required");
            return ResponseEntity.status(400).body(error);
        }
        scope.setSortOrder(sortOrder);
        return ResponseEntity.ok(scopeRepository.save(scope));
    }

    @PutMapping("/sub-scopes/{id}/sort")
    public ResponseEntity<?> updateSubScopeSort(@PathVariable Long id, @RequestBody Map<String, Integer> sortMap) {
        FossilFuelCollectionSubScope subScope = subScopeRepository.findById(id)
                .orElse(null);
        if (subScope == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "SubScope not found");
            return ResponseEntity.status(404).body(error);
        }
        Integer sortOrder = sortMap.get("sortOrder");
        if (sortOrder == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "sortOrder field is required");
            return ResponseEntity.status(400).body(error);
        }
        subScope.setSortOrder(sortOrder);
        return ResponseEntity.ok(subScopeRepository.save(subScope));
    }

    @PutMapping("/meters/{id}/sort")
    public ResponseEntity<?> updateMeterSort(@PathVariable Long id, @RequestBody Map<String, Integer> sortMap) {
        FossilFuelMeterInfo meter = meterRepository.findById(id)
                .orElse(null);
        if (meter == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Meter not found");
            return ResponseEntity.status(404).body(error);
        }
        Integer sortOrder = sortMap.get("sortOrder");
        if (sortOrder == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "sortOrder field is required");
            return ResponseEntity.status(400).body(error);
        }
        meter.setSortOrder(sortOrder);
        return ResponseEntity.ok(meterRepository.save(meter));
    }

    /**
     * 获取指定细分范围下的采集点列表（可排除指定采集点）
     * 用于"更改采集点级联关系"和"更改采集点所属细分范围"弹窗中展示可选的上级采集点
     *
     * @param subScopeId     细分范围ID
     * @param excludeMeterId 需排除的采集点ID（通常为当前操作的采集点自身），为 null 时不排除
     * @return ResponseEntity 包含采集点列表的 List，每项含 id、name、parentMeterId、sortOrder
     */
    @GetMapping("/meters-by-sub-scope")
    public ResponseEntity<List<Map<String, Object>>> getMetersBySubScope(
            @RequestParam Long subScopeId,
            @RequestParam(required = false) Long excludeMeterId) {
        List<FossilFuelMeterInfo> meters = meterRepository.findBySubScopeIdOrderBySortOrderAsc(subScopeId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (FossilFuelMeterInfo meter : meters) {
            if (excludeMeterId != null && excludeMeterId.equals(meter.getId())) {
                continue;
            }
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("id", meter.getId());
            node.put("name", meter.getName());
            node.put("parentMeterId", meter.getParentMeterId());
            node.put("sortOrder", meter.getSortOrder());
            result.add(node);
        }
        return ResponseEntity.ok(result);
    }

    /**
     * 更改采集点的级联关系（上级采集点）
     * 仅更新 parent_meter_id，不改变所属细分范围
     *
     * @param id      采集点ID
     * @param request 请求体，包含 parentMeterId（新的上级采集点ID，0表示无上级）
     * @return ResponseEntity 更新后的采集点对象；采集点不存在时返回 404
     */
    @PutMapping("/meters/{id}/cascade")
    public ResponseEntity<?> updateMeterCascade(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        FossilFuelMeterInfo meter = meterRepository.findById(id)
                .orElse(null);
        if (meter == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Meter not found");
            return ResponseEntity.status(404).body(error);
        }
        Object parentMeterIdObj = request.get("parentMeterId");
        Long parentMeterId = 0L;
        if (parentMeterIdObj instanceof Number) {
            parentMeterId = ((Number) parentMeterIdObj).longValue();
        }
        meter.setParentMeterId(parentMeterId);
        return ResponseEntity.ok(meterRepository.save(meter));
    }

    /**
     * 更改采集点的所属细分范围（同时可更新上级采集点）
     * 更新当前采集点的 sub_scope_id 和 parent_meter_id，并递归更新其下所有子孙采集点的 sub_scope_id
     *
     * @param id      采集点ID
     * @param request 请求体，包含 subScopeId（新细分范围ID）、parentMeterId（新上级采集点ID，0表示无上级）
     * @return ResponseEntity 更新后的采集点对象；采集点不存在时返回 404
     */
    @PutMapping("/meters/{id}/sub-scope")
    public ResponseEntity<?> updateMeterSubScope(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        FossilFuelMeterInfo meter = meterRepository.findById(id)
                .orElse(null);
        if (meter == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Meter not found");
            return ResponseEntity.status(404).body(error);
        }
        Object subScopeIdObj = request.get("subScopeId");
        Long subScopeId = null;
        if (subScopeIdObj instanceof Number) {
            subScopeId = ((Number) subScopeIdObj).longValue();
        }
        Object parentMeterIdObj = request.get("parentMeterId");
        Long parentMeterId = 0L;
        if (parentMeterIdObj instanceof Number) {
            parentMeterId = ((Number) parentMeterIdObj).longValue();
        }
        meter.setSubScopeId(subScopeId);
        meter.setParentMeterId(parentMeterId);
        FossilFuelMeterInfo saved = meterRepository.save(meter);
        // 递归更新所有子节点的所属细分范围
        updateChildMetersSubScope(meter.getId(), subScopeId);
        return ResponseEntity.ok(saved);
    }

    /**
     * 递归更新指定采集点下所有子采集点的所属细分范围
     * @param parentMeterId 父采集点ID
     * @param newSubScopeId 新的细分范围ID
     */
    private void updateChildMetersSubScope(Long parentMeterId, Long newSubScopeId) {
        List<FossilFuelMeterInfo> children = meterRepository.findByParentMeterIdOrderBySortOrderAsc(parentMeterId);
        for (FossilFuelMeterInfo child : children) {
            child.setSubScopeId(newSubScopeId);
            meterRepository.save(child);
            // 递归处理孙节点
            updateChildMetersSubScope(child.getId(), newSubScopeId);
        }
    }

    @PostMapping("/{nodeType}/{id}/move-up")
    public ResponseEntity<?> moveUp(@PathVariable String nodeType, @PathVariable Long id) {
        try {
            boolean moved = move(nodeType, id, -1);
            if (!moved) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "已经是第一个");
                return ResponseEntity.badRequest().body(error);
            }
            return ResponseEntity.ok(Map.of("message", "上移成功"));
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PostMapping("/{nodeType}/{id}/move-down")
    public ResponseEntity<?> moveDown(@PathVariable String nodeType, @PathVariable Long id) {
        try {
            boolean moved = move(nodeType, id, 1);
            if (!moved) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "已经是最后一个");
                return ResponseEntity.badRequest().body(error);
            }
            return ResponseEntity.ok(Map.of("message", "下移成功"));
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    private boolean move(String nodeType, Long id, int direction) {
        switch (nodeType) {
            case "scope": {
                FossilFuelCollectionScope current = scopeRepository.findById(id).orElse(null);
                if (current == null) throw new RuntimeException("节点不存在");
                List<FossilFuelCollectionScope> siblings = scopeRepository.findAllByOrderBySortOrderAsc();
                int idx = findIndex(siblings, id);
                if (idx < 0) throw new RuntimeException("节点不存在");
                int targetIdx = idx + direction;
                if (targetIdx < 0 || targetIdx >= siblings.size()) return false;
                FossilFuelCollectionScope target = siblings.get(targetIdx);
                Integer tmp = current.getSortOrder();
                current.setSortOrder(target.getSortOrder());
                target.setSortOrder(tmp);
                scopeRepository.save(current);
                scopeRepository.save(target);
                return true;
            }
            case "sub_scope": {
                FossilFuelCollectionSubScope current = subScopeRepository.findById(id).orElse(null);
                if (current == null) throw new RuntimeException("节点不存在");
                List<FossilFuelCollectionSubScope> siblings = subScopeRepository.findByCollectionScopeIdOrderBySortOrderAsc(current.getCollectionScopeId());
                int idx = findIndex(siblings, id);
                if (idx < 0) throw new RuntimeException("节点不存在");
                int targetIdx = idx + direction;
                if (targetIdx < 0 || targetIdx >= siblings.size()) return false;
                FossilFuelCollectionSubScope target = siblings.get(targetIdx);
                Integer tmp = current.getSortOrder();
                current.setSortOrder(target.getSortOrder());
                target.setSortOrder(tmp);
                subScopeRepository.save(current);
                subScopeRepository.save(target);
                return true;
            }
            case "meter": {
                FossilFuelMeterInfo current = meterRepository.findById(id).orElse(null);
                if (current == null) throw new RuntimeException("节点不存在");
                List<FossilFuelMeterInfo> siblings;
                if (current.getParentMeterId() != null && current.getParentMeterId() > 0) {
                    siblings = meterRepository.findByParentMeterIdOrderBySortOrderAsc(current.getParentMeterId());
                } else {
                    siblings = meterRepository.findBySubScopeIdOrderBySortOrderAsc(current.getSubScopeId());
                    siblings.removeIf(m -> m.getParentMeterId() != null && m.getParentMeterId() > 0);
                }
                int idx = findIndex(siblings, id);
                if (idx < 0) throw new RuntimeException("节点不存在");
                int targetIdx = idx + direction;
                if (targetIdx < 0 || targetIdx >= siblings.size()) return false;
                FossilFuelMeterInfo target = siblings.get(targetIdx);
                Integer tmp = current.getSortOrder();
                current.setSortOrder(target.getSortOrder());
                target.setSortOrder(tmp);
                meterRepository.save(current);
                meterRepository.save(target);
                return true;
            }
            default:
                throw new RuntimeException("未知的节点类型: " + nodeType);
        }
    }

    private int findIndex(List<? extends Object> list, Long id) {
        for (int i = 0; i < list.size(); i++) {
            Object item = list.get(i);
            Long itemId = null;
            if (item instanceof FossilFuelCollectionScope) {
                itemId = ((FossilFuelCollectionScope) item).getId();
            } else if (item instanceof FossilFuelCollectionSubScope) {
                itemId = ((FossilFuelCollectionSubScope) item).getId();
            } else if (item instanceof FossilFuelMeterInfo) {
                itemId = ((FossilFuelMeterInfo) item).getId();
            }
            if (itemId != null && itemId.equals(id)) return i;
        }
        return -1;
    }

    // ==================== 自动抄表接口测试 ====================

    /**
     * 测试自动抄表接口（编辑模式，采集点已存在）
     */
    @PostMapping("/meter/{id}/test-reading")
    public ResponseEntity<?> testMeterReading(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        try {
            meterRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("采集点不存在: " + id));
            @SuppressWarnings("unchecked")
            Map<String, Object> config = (Map<String, Object>) body.get("config");
            return ResponseEntity.ok(doTestReading(config));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 测试自动抄表接口（新建模式，采集点尚未保存）
     */
    @PostMapping("/meter/test-reading")
    public ResponseEntity<?> testMeterReadingWithoutId(@RequestBody Map<String, Object> body) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> config = (Map<String, Object>) body.get("config");
            return ResponseEntity.ok(doTestReading(config));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 执行自动抄表接口测试
     * HTTP_API：发起一次 GET 请求验证连通性
     * MODBUS_TCP / MQTT：校验配置完整性
     */
    private Map<String, Object> doTestReading(Map<String, Object> config) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (config == null || config.isEmpty()) {
            result.put("success", false);
            result.put("message", "未提供抄表接口配置");
            return result;
        }
        String interfaceType = String.valueOf(config.getOrDefault("interfaceType", ""));
        String apiUrl = String.valueOf(config.getOrDefault("apiUrl", "")).trim();
        if (apiUrl.isEmpty() || "null".equals(apiUrl)) {
            result.put("success", false);
            result.put("message", "接口地址不能为空");
            return result;
        }
        try {
            switch (interfaceType) {
                case "HTTP_API": {
                    int timeoutSec = toInt(config.get("timeout"), 30);
                    java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
                            .connectTimeout(java.time.Duration.ofSeconds(timeoutSec))
                            .build();
                    java.net.http.HttpRequest.Builder reqBuilder = java.net.http.HttpRequest.newBuilder()
                            .uri(java.net.URI.create(apiUrl))
                            .timeout(java.time.Duration.ofSeconds(timeoutSec))
                            .GET();
                    String authType = String.valueOf(config.getOrDefault("authType", "NONE"));
                    if ("BASIC".equals(authType)) {
                        String user = String.valueOf(config.getOrDefault("username", ""));
                        String pass = String.valueOf(config.getOrDefault("password", ""));
                        String token = java.util.Base64.getEncoder()
                                .encodeToString((user + ":" + pass).getBytes(java.nio.charset.StandardCharsets.UTF_8));
                        reqBuilder.header("Authorization", "Basic " + token);
                    } else if ("TOKEN".equals(authType)) {
                        String token = String.valueOf(config.getOrDefault("token", ""));
                        reqBuilder.header("Authorization", "Bearer " + token);
                    }
                    java.net.http.HttpResponse<String> resp = client.send(
                            reqBuilder.build(),
                            java.net.http.HttpResponse.BodyHandlers.ofString());
                    int code = resp.statusCode();
                    if (code >= 200 && code < 300) {
                        result.put("success", true);
                        result.put("message", "HTTP 接口连通正常，状态码：" + code);
                        result.put("data", resp.body());
                    } else {
                        result.put("success", false);
                        result.put("message", "HTTP 接口返回非 2xx 状态码：" + code);
                    }
                    break;
                }
                case "MODBUS_TCP":
                case "MQTT": {
                    Object portObj = config.get("port");
                    if (portObj == null) {
                        result.put("success", false);
                        result.put("message", "端口不能为空");
                        return result;
                    }
                    result.put("success", true);
                    result.put("message", interfaceType + " 配置校验通过，" + apiUrl + ":" + portObj
                            + "（实际协议连通性测试待接入对应客户端）");
                    break;
                }
                default:
                    result.put("success", false);
                    result.put("message", "不支持的接口类型：" + interfaceType);
            }
        } catch (IllegalArgumentException e) {
            result.put("success", false);
            result.put("message", "接口地址格式无效：" + e.getMessage());
        } catch (java.io.IOException | InterruptedException e) {
            result.put("success", false);
            result.put("message", "接口调用失败：" + e.getClass().getSimpleName() + " - " + e.getMessage());
        }
        return result;
    }

    private int toInt(Object value, int defaultValue) {
        if (value == null) return defaultValue;
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
