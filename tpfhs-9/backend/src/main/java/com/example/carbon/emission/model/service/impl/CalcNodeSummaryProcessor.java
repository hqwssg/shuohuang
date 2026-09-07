package com.example.carbon.emission.model.service.impl;

import com.example.carbon.emission.model.entity.CalcNodeSummary;
import com.example.carbon.emission.model.entity.CalcUnitDefault;
import com.example.carbon.emission.model.entity.CalculationNode;
import com.example.carbon.emission.model.entity.CalculationNodeData;
import com.example.carbon.emission.model.entity.DataDictItem;
import com.example.carbon.emission.model.repository.CalcNodeSummaryRepository;
import com.example.carbon.emission.model.repository.CalcUnitDefaultRepository;
import com.example.carbon.emission.model.repository.CalculationNodeDataRepository;
import com.example.carbon.emission.model.repository.CalculationNodeRepository;
import com.example.carbon.emission.model.repository.DataDictItemRepository;
import com.example.carbon.emission.model.service.UnitConversionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 核算节点逐级汇总处理器
 * <p>
 * 职责：把 emission_collection_node_data（采集节点的核算结果）
 * 按「核算节点 × 场景三级 × 小类」汇总到 emission_calc_node_summary，
 * 并自底向上合并。
 * <p>
 * 仅在 job profile 下实例化，避免 web 容器扫描到产生 Bean。
 */
@Service
@Profile("job")
public class CalcNodeSummaryProcessor {

    private static final Logger log = LoggerFactory.getLogger(CalcNodeSummaryProcessor.class);

    /** 小数精度 */
    private static final int SCALE = 6;

    /** 节点类型ID：根节点 */
    private static final int TYPE_ID_ROOT = 1;
    /** 节点类型ID：核算节点 */
    private static final int TYPE_ID_CALCULATION = 2;
    /** 节点类型ID：采集节点 */
    private static final int TYPE_ID_COLLECTION = 3;
    /** 节点类型ID：运输节点 */
    private static final int TYPE_ID_TRANSPORT = 4;

    /** 汇总来源位掩码：直属采集节点 */
    private static final int SUMMARY_SOURCE_DIRECT = 1;
    /** 汇总来源位掩码：子节点 subtotal 合并 */
    private static final int SUMMARY_SOURCE_CHILDREN = 2;

    @Autowired
    private CalculationNodeRepository calculationNodeRepository;

    @Autowired
    private CalculationNodeDataRepository calculationNodeDataRepository;

    @Autowired
    private CalcNodeSummaryRepository calcNodeSummaryRepository;

    @Autowired
    private UnitConversionService unitConversionService;

    @Autowired
    private DataDictItemRepository dataDictItemRepository;

    @Autowired
    private CalcUnitDefaultRepository calcUnitDefaultRepository;

    /**
     * 执行逐级汇总主流程
     *
     * @param calcTemplateId 核算任务记录ID（emission_calculation_template.id）
     */
    public void process(Long calcTemplateId) {
        // 1. 幂等：先删该模板下所有历史汇总
        calcNodeSummaryRepository.deleteByCalculationTemplateId(calcTemplateId);

        // 2. 拉取全部快照节点
        List<CalculationNode> allNodes = calculationNodeRepository.findByCalculationTemplateId(calcTemplateId);
        log.info("[Step4][tpl={}] 本次核算快照节点数: {}", calcTemplateId, allNodes.size());

        // 过滤汇总目标：type=1(root)、2(calc)、4(transport) 都走同一算法
        List<CalculationNode> targetNodes = allNodes.stream()
                .filter(n -> n.getTypeId() != null && (n.getTypeId() == TYPE_ID_ROOT
                        || n.getTypeId() == TYPE_ID_CALCULATION
                        || n.getTypeId() == TYPE_ID_TRANSPORT))
                .collect(Collectors.toList());
        if (targetNodes.isEmpty()) {
            log.info("[Step4][tpl={}] 无核算/根/运输节点，跳过", calcTemplateId);
            return;
        }

        // 3. 构建节点辅助 Map
        Map<Long, CalculationNode> byId = allNodes.stream()
                .collect(Collectors.toMap(CalculationNode::getId, Function.identity()));

        // 3.1 按 parent 分组（子节点列表）：key=parentId，value=直属子节点
        Map<Long, List<CalculationNode>> childrenByParent = allNodes.stream()
                .filter(n -> n.getParentId() != null)
                .collect(Collectors.groupingBy(CalculationNode::getParentId));

        // 3.2 计算 level：根=1，逐级+1；无 parent 的节点 level=1
        Map<Long, Integer> levelMap = new HashMap<>();
        for (CalculationNode n : targetNodes) {
            computeLevel(n.getId(), byId, levelMap);
        }

        // 3.3 标记 is_leaf_calc_node：直属 childrenByParent 里全为 type!=1/2/4 → true
        Map<Long, Boolean> leafMap = new HashMap<>();
        for (CalculationNode n : targetNodes) {
            List<CalculationNode> children = childrenByParent.getOrDefault(n.getId(), Collections.emptyList());
            boolean allCollection = !children.isEmpty() && children.stream()
                    .allMatch(c -> c.getTypeId() != null
                            && c.getTypeId() != TYPE_ID_ROOT
                            && c.getTypeId() != TYPE_ID_CALCULATION
                            && c.getTypeId() != TYPE_ID_TRANSPORT);
            leafMap.put(n.getId(), allCollection);
        }

        // 4. emission_category 反查缓存（subcategory → parent_code），懒加载避免 N+1
        Map<String, String> subcategoryToCategoryCache = new ConcurrentHashMap<>();

        // 5. 排序：层级倒序（最深先），保证叶子先处理，父节点后处理
        List<CalculationNode> ordered = new ArrayList<>(targetNodes);
        ordered.sort(Comparator.comparingInt((CalculationNode n) ->
                levelMap.getOrDefault(n.getId(), 1)).reversed());

        log.info("[Step4][tpl={}] 待汇总目标节点数={}, 最大层级深度={}",
                calcTemplateId, ordered.size(),
                levelMap.values().stream().max(Integer::compareTo).orElse(1));

        long overallStart = System.currentTimeMillis();
        int totalRows = 0;

        for (CalculationNode node : ordered) {
            long nodeStart = System.currentTimeMillis();
            totalRows += processOneNode(calcTemplateId, node, byId, childrenByParent,
                    levelMap, leafMap, subcategoryToCategoryCache);

            log.info("[Step4][tpl={}] 节点处理完成 name={} id={} typeId={} level={} leaf={} 耗时={}ms",
                    calcTemplateId, node.getName(), node.getId(), node.getTypeId(),
                    levelMap.getOrDefault(node.getId(), 1),
                    leafMap.getOrDefault(node.getId(), false),
                    System.currentTimeMillis() - nodeStart);
        }

        log.info("[Step4][tpl={}] 全量节点处理完成：{} 个节点、共写入 {} 条汇总行、总耗时={}ms",
                calcTemplateId, ordered.size(), totalRows,
                System.currentTimeMillis() - overallStart);
    }

    // ---------- 辅助方法 ----------

    /**
     * 递归计算 level（含 memo）。遇到无 parent 的节点 level=1
     */
    private int computeLevel(Long nodeId, Map<Long, CalculationNode> byId,
                             Map<Long, Integer> levelMap) {
        if (levelMap.containsKey(nodeId)) {
            return levelMap.get(nodeId);
        }
        CalculationNode n = byId.get(nodeId);
        if (n == null || n.getParentId() == null || !byId.containsKey(n.getParentId())) {
            levelMap.put(nodeId, 1);
            return 1;
        }
        int parentLevel = computeLevel(n.getParentId(), byId, levelMap);
        int currentLevel = parentLevel + 1;
        levelMap.put(nodeId, currentLevel);
        return currentLevel;
    }

    /**
     * 单节点处理，返回写入的汇总行数
     */
    private int processOneNode(Long calcTemplateId, CalculationNode node,
                               Map<Long, CalculationNode> byId,
                               Map<Long, List<CalculationNode>> childrenByParent,
                               Map<Long, Integer> levelMap, Map<Long, Boolean> leafMap,
                               Map<String, String> subcategoryToCategoryCache) {

        Long calcNodeId = node.getId();
        // 幂等清理：删本节点之前的汇总
        calcNodeSummaryRepository.deleteByCalculationNodeId(calcNodeId);

        // Step A：统计本节点 direct（直属采集节点 type=3 的 collection_node_data）
        List<Long> directCollectionCalcNodeIds = childrenByParent
                .getOrDefault(calcNodeId, Collections.emptyList()).stream()
                .filter(c -> c.getTypeId() != null && c.getTypeId() == TYPE_ID_COLLECTION)
                .map(CalculationNode::getId)
                .collect(Collectors.toList());

        // 分组 key: (L1, L2, L3, subcategory)
        final class GroupKey {
            final String l1;
            final String l2;
            final String l3;
            final String subcategory;

            GroupKey(String l1, String l2, String l3, String subcategory) {
                this.l1 = l1;
                this.l2 = l2;
                this.l3 = l3;
                this.subcategory = subcategory;
            }

            String l1() { return l1; }
            String l2() { return l2; }
            String l3() { return l3; }
            String subcategory() { return subcategory; }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (!(o instanceof GroupKey)) return false;
                GroupKey g = (GroupKey) o;
                return java.util.Objects.equals(l1, g.l1)
                        && java.util.Objects.equals(l2, g.l2)
                        && java.util.Objects.equals(l3, g.l3)
                        && java.util.Objects.equals(subcategory, g.subcategory);
            }

            @Override
            public int hashCode() {
                return java.util.Objects.hash(l1, l2, l3, subcategory);
            }
        }

        Map<GroupKey, Agg> directAggMap = new HashMap<>();
        if (!directCollectionCalcNodeIds.isEmpty()) {
            List<CalculationNodeData> rows =
                    calculationNodeDataRepository.findByCalculationNodeIdIn(directCollectionCalcNodeIds);
            for (CalculationNodeData r : rows) {
                if (r.getEmissionSubcategory() == null) {
                    continue;
                }

                String l1 = r.getEnergyCategoryL1() == null ? "" : r.getEnergyCategoryL1();
                String l2 = r.getEnergyCategoryL2() == null ? "" : r.getEnergyCategoryL2();
                String l3 = r.getEnergyCategoryL3() == null ? "" : r.getEnergyCategoryL3();
                String sub = r.getEmissionSubcategory();
                GroupKey g = new GroupKey(l1, l2, l3, sub);

                // 取 adjusted 值进行换算
                BigDecimal energy = (r.getAdjustedEnergyValue() != null) ?
                        r.getAdjustedEnergyValue() : BigDecimal.ZERO;
                BigDecimal carbon = (r.getAdjustedCarbonEmission() != null) ?
                        r.getAdjustedCarbonEmission() : BigDecimal.ZERO;
                String mu = r.getMeasurementUnit();

                try {
                    // 先查该小类的核算单位，用于后续计算换算系数
                    String calcUnit = lookupCalcUnit(sub);

                    // 1) energy 换算到核算单位
                    BigDecimal convertedEnergy = unitConversionService.convertToCalculationUnit(sub, mu, energy);
                    if (convertedEnergy != null) {
                        energy = convertedEnergy;
                    }

                    // 2) 对 carbon 乘同换算系数：
                    //    carbon_old = energy_old × factor
                    //    carbon_new = energy_new × factor = carbon_old × (energy_new / energy_old)
                    //    系数 = convert(sub, fromUnit, calcUnit, 1)
                    if (mu != null && calcUnit != null && !mu.equals(calcUnit)) {
                        BigDecimal conversionFactor = unitConversionService.convert(
                                sub, mu, calcUnit, BigDecimal.ONE);
                        if (conversionFactor != null
                                && conversionFactor.compareTo(BigDecimal.ONE) != 0
                                && carbon != null) {
                            carbon = carbon.multiply(conversionFactor)
                                    .setScale(SCALE, RoundingMode.HALF_UP);
                        }
                    }
                } catch (Exception ignore) {
                    // 不中断，保持原值；UnitConversionService 内部已有 WARN
                }

                BigDecimal factorVal = (r.getCarbonEmissionFactor() != null) ?
                        r.getCarbonEmissionFactor() : BigDecimal.ZERO;

                Agg agg = directAggMap.computeIfAbsent(g, k -> new Agg());
                agg.directEnergy = agg.directEnergy.add(energy == null ? BigDecimal.ZERO : energy);
                agg.directCarbon = agg.directCarbon.add(carbon == null ? BigDecimal.ZERO : carbon);
                // factor 按 energy 加权累加
                BigDecimal w = energy == null ? BigDecimal.ZERO : energy;
                agg.factorWeighted = agg.factorWeighted.add(w.multiply(factorVal));
                agg.energyWeight = agg.energyWeight.add(w);
            }
        }

        // Step B：子节点 subtotal 汇总（非 leaf 节点才处理）
        Boolean isLeaf = leafMap.getOrDefault(calcNodeId, false);
        Map<GroupKey, Agg> childrenAggMap = new HashMap<>();
        boolean hasChildrenAgg = false;

        if (!isLeaf) {
            List<Long> childTargetNodeIds = childrenByParent
                    .getOrDefault(calcNodeId, Collections.emptyList()).stream()
                    .filter(c -> c.getTypeId() != null && (
                            c.getTypeId() == TYPE_ID_ROOT
                                    || c.getTypeId() == TYPE_ID_CALCULATION
                                    || c.getTypeId() == TYPE_ID_TRANSPORT))
                    .map(CalculationNode::getId)
                    .collect(Collectors.toList());
            if (!childTargetNodeIds.isEmpty()) {
                List<CalcNodeSummary> childSummaries =
                        calcNodeSummaryRepository.findByCalculationNodeIdIn(childTargetNodeIds);
                hasChildrenAgg = !childSummaries.isEmpty();
                for (CalcNodeSummary s : childSummaries) {
                    String l1 = s.getEnergyCategoryL1() == null ? "" : s.getEnergyCategoryL1();
                    String l2 = s.getEnergyCategoryL2() == null ? "" : s.getEnergyCategoryL2();
                    String l3 = s.getEnergyCategoryL3() == null ? "" : s.getEnergyCategoryL3();
                    String sub = s.getEmissionSubcategory();
                    if (sub == null) {
                        continue;
                    }
                    GroupKey g = new GroupKey(l1, l2, l3, sub);
                    Agg a = childrenAggMap.computeIfAbsent(g, k -> new Agg());
                    a.subtotalEnergyChildren = a.subtotalEnergyChildren.add(
                            nvl(s.getSubtotalEnergyValue()));
                    a.subtotalCarbonChildren = a.subtotalCarbonChildren.add(
                            nvl(s.getSubtotalCarbonEmission()));
                }
            }
        }

        // Step C：合并 direct + 子节点，构建汇总实体列表
        Set<GroupKey> allKeys = new HashSet<>();
        allKeys.addAll(directAggMap.keySet());
        allKeys.addAll(childrenAggMap.keySet());

        if (allKeys.isEmpty()) {
            return 0;
        }

        List<CalcNodeSummary> saveList = new ArrayList<>(allKeys.size());

        for (GroupKey g : allKeys) {
            Agg d = directAggMap.getOrDefault(g, Agg.EMPTY);
            Agg c = childrenAggMap.getOrDefault(g, Agg.EMPTY);

            BigDecimal directEnergy = d.directEnergy;
            BigDecimal directCarbon = d.directCarbon;
            BigDecimal subtotalEnergy = directEnergy.add(c.subtotalEnergyChildren);
            BigDecimal subtotalCarbon = directCarbon.add(c.subtotalCarbonChildren);

            // 加权平均因子
            BigDecimal factorAvg = BigDecimal.ZERO;
            BigDecimal ew = d.energyWeight;
            if (ew != null && ew.compareTo(BigDecimal.ZERO) > 0 && d.factorWeighted != null) {
                factorAvg = d.factorWeighted.divide(ew, SCALE, RoundingMode.HALF_UP);
            }

            CalcNodeSummary s = new CalcNodeSummary();
            s.setCalculationTemplateId(calcTemplateId);
            s.setCalculationNodeId(calcNodeId);
            s.setSourceNodeId(node.getNodeId());
            s.setParentCalcNodeId(node.getParentId());
            s.setNodeLevel(levelMap.getOrDefault(calcNodeId, 1));
            s.setIsLeafCalcNode(isLeaf ? 1 : 0);
            s.setEnergyCategoryL1(g.l1().isEmpty() ? null : g.l1());
            s.setEnergyCategoryL2(g.l2().isEmpty() ? null : g.l2());
            s.setEnergyCategoryL3(g.l3().isEmpty() ? null : g.l3());
            s.setEmissionSubcategory(g.subcategory());

            // 反查 emission_category（parent_code），走缓存
            String category = subcategoryToCategoryCache.computeIfAbsent(g.subcategory(),
                    key -> dataDictItemRepository.findByItemCode(key)
                            .map(DataDictItem::getParentCode).orElse(null));
            s.setEmissionCategory(category);

            s.setDirectEnergyValue(directEnergy.setScale(SCALE, RoundingMode.HALF_UP));
            s.setDirectCarbonEmission(directCarbon.setScale(SCALE, RoundingMode.HALF_UP));
            s.setSubtotalEnergyValue(subtotalEnergy.setScale(SCALE, RoundingMode.HALF_UP));
            s.setSubtotalCarbonEmission(subtotalCarbon.setScale(SCALE, RoundingMode.HALF_UP));

            // 核算单位编码
            String calcUnit = lookupCalcUnit(g.subcategory());
            s.setCalculationUnitCode(calcUnit);

            s.setCarbonEmissionFactor(factorAvg);

            // summary_source 位掩码
            int src = 0;
            if (directAggMap.containsKey(g)) {
                src |= SUMMARY_SOURCE_DIRECT;
            }
            if (hasChildrenAgg && childrenAggMap.containsKey(g)) {
                src |= SUMMARY_SOURCE_CHILDREN;
            }
            s.setSummarySource(src);

            saveList.add(s);
        }

        calcNodeSummaryRepository.saveAll(saveList);
        return saveList.size();
    }

    // --- helper 工具 ---

    /**
     * 通过小类编码查询缺省核算单位编码
     *
     * @param subcategoryCode 排放数据小类编码
     * @return 核算单位编码；找不到返回 null
     */
    private String lookupCalcUnit(String subcategoryCode) {
        if (subcategoryCode == null) {
            return null;
        }
        return calcUnitDefaultRepository.findBySubcategoryCode(subcategoryCode)
                .map(CalcUnitDefault::getCalculationUnit)
                .orElse(null);
    }

    /**
     * null 安全的 BigDecimal 取值
     */
    private static BigDecimal nvl(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    /**
     * 分组累加器
     */
    private static class Agg {
        static final Agg EMPTY = new Agg();

        BigDecimal directEnergy = BigDecimal.ZERO;
        BigDecimal directCarbon = BigDecimal.ZERO;
        BigDecimal factorWeighted = BigDecimal.ZERO;
        BigDecimal energyWeight = BigDecimal.ZERO;
        BigDecimal subtotalEnergyChildren = BigDecimal.ZERO;
        BigDecimal subtotalCarbonChildren = BigDecimal.ZERO;
    }
}
