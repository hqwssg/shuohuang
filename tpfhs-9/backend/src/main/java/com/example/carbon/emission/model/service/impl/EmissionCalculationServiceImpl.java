package com.example.carbon.emission.model.service.impl;

import com.example.carbon.emission.model.entity.*;
import com.example.carbon.emission.model.repository.*;
import com.example.carbon.emission.model.service.EmissionCalculationService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 碳排放核算服务实现
 *
 * 完整核算流程：
 * <ol>
 *   <li>Step1：创建核算任务记录（CalculationTemplate），解析 taskConfig 确定核算周期</li>
 *   <li>Step2：遍历模版节点，创建核算节点快照（CalculationNode）</li>
 *   <li>Step3：仅对采集节点（typeId=3）计算能耗计量值并生成 CalculationNodeData（写入 emission_collection_node_data 表）。
 *       本步骤所有操作只针对采集节点进行，emission_collection_node_data 表只保存采集节点核算统计的数据。</li>
 *   <li>Step4：所有采集点核算完成后，统一对核算节点（typeId=2）和根节点（typeId=1）进行逐级汇总，
 *       按能源场景与排放小类保存直接值、子树小计及实际因子。</li>
 *   <li>Step5：完成核算，更新任务状态为成功（2）或失败（3）</li>
 * </ol>
 *
 * 当前进度：emission_collection_node_data 表记录采集节点（typeId=3）的核算结果，
 * emission_calc_node_summary 表记录核算节点与根节点的逐级汇总。任一节点计算失败不中断整体核算；整体异常则置 status=3。
 */
@Service
public class EmissionCalculationServiceImpl implements EmissionCalculationService {

    private static final Logger logger = LoggerFactory.getLogger(EmissionCalculationServiceImpl.class);

    private static final int SCALE = 6;

    /** 节点类型ID：根节点 */
    private static final int TYPE_ID_ROOT = 1;
    /** 节点类型ID：核算子节点 */
    private static final int TYPE_ID_CALCULATION = 2;
    /** 节点类型ID：采集节点 */
    private static final int TYPE_ID_COLLECTION = 3;
    /** 节点类型ID：运输节点 */
    private static final int TYPE_ID_TRANSPORT = 4;

    /** 错误码：Step4 逐级汇总失败 */
    private static final int ERROR_CODE_STEP4 = 2;

    /** 采集点类型：1-电力表，2-化石燃料，3-外购热能 */
    private static final int COLLECTION_TYPE_ELECTRICITY = 1;
    private static final int COLLECTION_TYPE_FOSSIL = 2;
    private static final int COLLECTION_TYPE_HEAT = 3;

    /** 核算任务状态 */
    private static final int TEMPLATE_STATUS_RUNNING = 1;
    private static final int TEMPLATE_STATUS_SUCCESS = 2;
    private static final int TEMPLATE_STATUS_FAILED = 3;

    @Autowired
    private EmissionNodeRepository emissionNodeRepository;
    @Autowired
    private EmissionNodeConfigRepository emissionNodeConfigRepository;
    @Autowired
    private EmissionNodeInfoRepository emissionNodeInfoRepository;
    @Autowired
    private MeterInfoRepository meterInfoRepository;
    @Autowired
    private FossilFuelMeterInfoRepository fossilFuelMeterInfoRepository;
    @Autowired
    private PurchasedHeatMeterInfoRepository purchasedHeatMeterInfoRepository;
    @Autowired
    private CalculationTemplateRepository calculationTemplateRepository;
    @Autowired
    private CalculationNodeRepository calculationNodeRepository;
    @Autowired
    private CalculationNodeDataRepository calculationNodeDataRepository;
    @Autowired
    private TemplateRepository templateRepository;
    @Autowired
    private EnergyMeasurementCalculator energyMeasurementCalculator;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private DefaultFactorRepository defaultFactorRepository;

    /**
     * 核算节点逐级汇总处理器。
     * 仅在 "job" profile 下存在 Bean；非 job profile（纯 web）时为 null，
     * 使用时需判空并记录 WARN 日志。
     */
    @Autowired(required = false)
    private CalcNodeSummaryProcessor calcNodeSummaryProcessor;

    @Override
    @Transactional
    public void executeEmissionCalculation(Long templateId) {
        logger.info("***开始执行碳排放核算，模版ID: {}", templateId);

        // Step 1：创建核算任务记录
        Template template = templateRepository.findById(templateId).orElse(null);
        if (template == null) {
            logger.error("***模版不存在，模版ID: {}", templateId);
            return;
        }

        LocalDate[] cycle = parseCalculationCycle(template.getTaskConfig());
        LocalDate cycleStartDate = cycle[0];
        LocalDate cycleEndDate = cycle[1];
        logger.info("***核算周期: {} ~ {}, 模版ID: {}", cycleStartDate, cycleEndDate, templateId);

        CalculationTemplate calcTemplate = new CalculationTemplate();
        calcTemplate.setTemplateId(templateId);
        calcTemplate.setTaskInitiator(1);
        calcTemplate.setStatus(TEMPLATE_STATUS_RUNNING);
        calcTemplate.setCalculationCycleStartDate(cycleStartDate);
        calcTemplate.setCalculationCycleEndDate(cycleEndDate);
        calcTemplate.setCalculationStartTime(LocalDateTime.now());
        calcTemplate = calculationTemplateRepository.save(calcTemplate);

        try {
            // Step 2：先完整创建快照，再把设计态父节点转换为父快照ID。
            // emission_node.parent_id 与 emission_calculation_node.parent_id 的语义不同，不能直接复制。
            List<EmissionNode> nodes = emissionNodeRepository.findByTemplateId(templateId);
            logger.info("***模版下节点数: {}, 模版ID: {}", nodes.size(), templateId);
            Map<Long, CalculationNode> snapshotBySourceId = new HashMap<>();
            for (EmissionNode node : nodes) {
                CalculationNode calcNode = new CalculationNode();
                calcNode.setCalculationTemplateId(calcTemplate.getId());
                calcNode.setNodeId(node.getId());
                calcNode.setName(node.getName());
                calcNode.setTypeId(node.getTypeId());
                calcNode.setParentId(null);

                // 核算子节点（typeId=2）查询 nodeCategory
                if (node.getTypeId() != null && node.getTypeId() == TYPE_ID_CALCULATION) {
                    emissionNodeInfoRepository.findByNodeId(node.getId())
                            .ifPresent(info -> calcNode.setNodeCategory(info.getNodeCategory()));
                }
                CalculationNode saved = calculationNodeRepository.save(calcNode);
                if (snapshotBySourceId.putIfAbsent(node.getId(), saved) != null) {
                    throw new IllegalStateException("同一模版存在重复源节点，nodeId=" + node.getId());
                }
            }

            for (EmissionNode node : nodes) {
                Long parentSourceId = node.getParentId();
                if (parentSourceId == null) {
                    continue;
                }
                CalculationNode snapshot = snapshotBySourceId.get(node.getId());
                CalculationNode parentSnapshot = snapshotBySourceId.get(parentSourceId);
                if (snapshot == null || parentSnapshot == null) {
                    throw new IllegalStateException("创建核算快照失败：找不到父源节点，sourceNodeId="
                            + node.getId() + ", parentSourceId=" + parentSourceId);
                }
                snapshot.setParentId(parentSnapshot.getId());
            }
            calculationNodeRepository.saveAll(new ArrayList<>(snapshotBySourceId.values()));

            Long factorTemplateId = template.getFactorTemplateId();
            // Step 3：完整快照树保存后，再处理采集节点。
            for (EmissionNode node : nodes) {
                if (node.getTypeId() != null && node.getTypeId() == TYPE_ID_COLLECTION) {
                    CalculationNode calcNode = snapshotBySourceId.get(node.getId());
                    try {
                        processCollectionNode(calcNode.getId(), node.getId(), factorTemplateId,
                                cycleStartDate, cycleEndDate);
                    } catch (Exception e) {
                        logger.error("***节点核算失败，节点ID: {}, 模版ID: {}", node.getId(), templateId, e);
                    }
                }
            }

            // 所有采集点（typeId=3）统计核算完成后，在此统一对核算节点（typeId=2）
            // 和根节点（typeId=1）进行操作（汇总下级节点能耗、按因子折算等）。

            // Step 4：核算节点与根节点逐级汇总（自底向上）
            try {
                logger.info("[Step4] 开始逐级汇总核算节点，模版ID: {}, 核算ID: {}",
                        templateId, calcTemplate.getId());
                if (calcNodeSummaryProcessor == null) {
                    logger.warn("[Step4] CalcNodeSummaryProcessor Bean 未启用，非job profile，跳过。");
                } else {
                    int summaryRows = calcNodeSummaryProcessor.process(calcTemplate.getId());
                    if (hasSummarizableCollectionData(calcTemplate.getId()) && summaryRows == 0) {
                        calcTemplate.setErrorCode(ERROR_CODE_STEP4);
                        throw new IllegalStateException("存在有效采集节点数据，但未生成任何汇总结果");
                    }
                    logger.info("[Step4] 逐级汇总完成，模版ID: {}, 核算ID: {}",
                            templateId, calcTemplate.getId());
                }
            } catch (Exception e) {
                logger.error("[Step4] 失败，模版ID: {}, 核算ID: {}", templateId, calcTemplate.getId(), e);
                calcTemplate.setErrorCode(ERROR_CODE_STEP4);
                // 直接抛到外层异常分支，由外层统一置 status=FAILED
                throw new RuntimeException("核算节点逐级汇总失败", e);
            }

            // Step 5：完成核算
            calcTemplate.setStatus(TEMPLATE_STATUS_SUCCESS);
            calcTemplate.setCalculationEndTime(LocalDateTime.now());
            calculationTemplateRepository.save(calcTemplate);
            logger.info("***碳排放核算完成，模版ID: {}, 核算ID: {}", templateId, calcTemplate.getId());

        } catch (Exception e) {
            logger.error("***碳排放核算失败，模版ID: {}, 核算ID: {}", templateId, calcTemplate.getId(), e);
            calcTemplate.setStatus(TEMPLATE_STATUS_FAILED);
            if (calcTemplate.getErrorCode() == null || calcTemplate.getErrorCode() == 0) {
                calcTemplate.setErrorCode(1);
            }
            calcTemplate.setCalculationEndTime(LocalDateTime.now());
            calculationTemplateRepository.save(calcTemplate);
        }
    }

    /**
     * 处理单个采集节点：查询配置和采集点信息，调用计算器，保存 CalculationNodeData。
     * <p>
     * 因子回退机制：采集点因子优先使用节点配置（emission_node_config.carbon_emission_factor），
     * 为空时回退到因子模版缺省因子（emission_default_factor 表中 factor_template_id
     * + subcategory_code/name 匹配且 status=1 的记录）。
     * 即"采集点因子优先使用节点配置，为空时回退到因子模版缺省因子"。
     *
     * @param calcNodeId      核算主表ID（calculation_node 主键）
     * @param nodeId          采集节点配置ID（emission_node_config 主键）
     * @param factorTemplateId 当前核算模版关联的因子模版ID（用于查模版缺省因子）
     * @param cycleStartDate  核算周期开始日期
     * @param cycleEndDate    核算周期结束日期
     */
    private void processCollectionNode(Long calcNodeId, Long nodeId, Long factorTemplateId,
                                       LocalDate cycleStartDate, LocalDate cycleEndDate) {
        EmissionNodeConfig config = emissionNodeConfigRepository.findByNodeId(nodeId).orElse(null);
        if (config == null) {
            logger.warn("***节点配置不存在，节点ID: {}", nodeId);
            saveErrorNodeData(calcNodeId, nodeId, "节点配置不存在");
            return;
        }

        Integer collectionPointType = config.getCollectionPointType();
        Long collectionPointId = config.getCollectionPointId();
        if (collectionPointType == null || collectionPointId == null) {
            logger.warn("***采集点信息不完整，节点ID: {}", nodeId);
            saveErrorNodeData(calcNodeId, config, "采集点信息不完整");
            return;
        }

        // 获取采集点信息
        CollectionPointInfo cpInfo = getCollectionPointInfo(collectionPointType, collectionPointId);
        if (cpInfo == null) {
            logger.warn("***采集点不存在，类型: {}, ID: {}", collectionPointType, collectionPointId);
            saveErrorNodeData(calcNodeId, config, "采集点不存在");
            return;
        }

        // 调用能耗计量值计算器
        EnergyMeasurementCalculator.Result calcResult = energyMeasurementCalculator.calculate(
                collectionPointType, collectionPointId, cpInfo.isCumulative,
                cycleStartDate, cycleEndDate);

        // 创建 CalculationNodeData
        CalculationNodeData data = new CalculationNodeData();
        data.setCalculationNodeId(calcNodeId);
        data.setCollectionPointType(collectionPointType);
        data.setCollectionPointId(collectionPointId);
        data.setEmissionCategory(config.getEmissionCategory());
        data.setCarbonEmissionFactor(config.getCarbonEmissionFactor());

        // 采集点字段
        data.setEmissionSubcategory(cpInfo.emissionSubcategory);
        data.setIsCumulative(cpInfo.isCumulative);
        data.setIsMobileSource(cpInfo.isMobileSource);
        data.setMeasurementUnit(cpInfo.measurementUnit);
        data.setEnergyCategoryL1(cpInfo.energyCategoryL1);
        data.setEnergyCategoryL2(cpInfo.energyCategoryL2);
        data.setEnergyCategoryL3(cpInfo.energyCategoryL3);
        data.setEnergyUseCategory(cpInfo.energyUseCategory);

        // 计算结果
        data.setEnergyMeasurementValue(calcResult.getEnergyMeasurementValue());
        data.setDataStatus(calcResult.getDataStatus());
        data.setCollectionRecordStartDate(calcResult.getCollectionRecordStartDate());
        data.setCollectionRecordEndDate(calcResult.getCollectionRecordEndDate());
        data.setDataMissingDescription(calcResult.getDataMissingDescription());
        data.setAdjustedEnergyValue(calcResult.getAdjustedEnergyValue());

        // 碳排放量 = 能耗计量值 × 碳排放因子
        // 因子回退机制：采集点因子优先使用节点配置（config.getCarbonEmissionFactor()），
        // 为空时回退到因子模版缺省因子（emission_default_factor 表 template_id + 编码/名称匹配）
        BigDecimal factor = config.getCarbonEmissionFactor();
        if (factor == null && factorTemplateId != null && cpInfo.emissionSubcategory != null) {
            // 节点未配置因子 -> 按因子模版ID和编码/名称查模版缺省因子。
            DefaultFactor df = defaultFactorRepository
                    .findByTemplateIdAndSubcategoryCode(factorTemplateId, cpInfo.emissionSubcategory)
                    .orElseGet(() -> defaultFactorRepository
                            .findByTemplateIdAndSubcategoryName(factorTemplateId, cpInfo.emissionSubcategory)
                            .orElse(null));
            // 步骤2：仅当缺省因子记录存在、有值且启用(status=1)时才采用，否则 factor 保持为 null
            if (df != null && df.getFactorValue() != null && df.getStatus() != null && df.getStatus() == 1) {
                factor = df.getFactorValue();
                logger.info("***节点未设置因子，使用模版缺省因子：节点ID={}, 小类={}, 因子值={}",
                        nodeId, cpInfo.emissionSubcategory, factor);
            }
        }
        if (factor != null) {
            data.setCarbonEmissionFactor(factor);
            BigDecimal energyVal = calcResult.getEnergyMeasurementValue();
            if (energyVal == null) {
                energyVal = BigDecimal.ZERO;
            }
            data.setCarbonEmission(energyVal.multiply(factor).setScale(SCALE, RoundingMode.HALF_UP));

            BigDecimal adjustedVal = calcResult.getAdjustedEnergyValue();
            if (adjustedVal == null) {
                adjustedVal = BigDecimal.ZERO;
            }
            data.setAdjustedCarbonEmission(adjustedVal.multiply(factor).setScale(SCALE, RoundingMode.HALF_UP));
        }

        calculationNodeDataRepository.save(data);
        logger.debug("***节点核算完成，节点ID: {}, 数据状态: {}", nodeId, calcResult.getDataStatus());
    }

    /**
     * 只有本次存在完整采集数据时，才要求 Step4 至少生成一条汇总结果。
     * 无数据或不完整数据仍可按既有规则结束，不把合法空场景误判为汇总故障。
     */
    private boolean hasSummarizableCollectionData(Long calcTemplateId) {
        List<CalculationNode> collectionNodes = calculationNodeRepository
                .findByCalculationTemplateId(calcTemplateId).stream()
                .filter(node -> node.getTypeId() != null && node.getTypeId() == TYPE_ID_COLLECTION)
                .toList();
        if (collectionNodes.isEmpty()) {
            return false;
        }
        List<Long> ids = collectionNodes.stream().map(CalculationNode::getId).toList();
        return calculationNodeDataRepository.findByCalculationNodeIdIn(ids).stream()
                .anyMatch(data -> data.getDataStatus() != null && data.getDataStatus() == 1);
    }

    /**
     * 根据采集点类型查询对应采集点表，提取计算所需字段
     */
    private CollectionPointInfo getCollectionPointInfo(Integer collectionPointType, Long collectionPointId) {
        CollectionPointInfo info = new CollectionPointInfo();
        switch (collectionPointType) {
            case COLLECTION_TYPE_ELECTRICITY:
                return meterInfoRepository.findById(collectionPointId).map(m -> {
                    info.isCumulative = m.getIsCumulative();
                    info.emissionSubcategory = m.getEmissionSubcategory();
                    info.isMobileSource = m.getIsMobileSource();
                    info.measurementUnit = m.getMeasurementUnit();
                    info.energyCategoryL1 = m.getEnergyCategoryL1();
                    info.energyCategoryL2 = m.getEnergyCategoryL2();
                    info.energyCategoryL3 = m.getEnergyCategoryL3();
                    info.energyUseCategory = m.getEnergyUseCategory();
                    return info;
                }).orElse(null);
            case COLLECTION_TYPE_FOSSIL:
                return fossilFuelMeterInfoRepository.findById(collectionPointId).map(m -> {
                    info.isCumulative = m.getIsCumulative();
                    info.emissionSubcategory = m.getFuelType();
                    info.isMobileSource = m.getIsMobileSource();
                    info.measurementUnit = m.getMeasurementUnit();
                    info.energyCategoryL1 = m.getEnergyCategoryL1();
                    info.energyCategoryL2 = m.getEnergyCategoryL2();
                    info.energyCategoryL3 = m.getEnergyCategoryL3();
                    info.energyUseCategory = m.getEnergyUseCategory();
                    return info;
                }).orElse(null);
            case COLLECTION_TYPE_HEAT:
                return purchasedHeatMeterInfoRepository.findById(collectionPointId).map(m -> {
                    info.isCumulative = m.getIsCumulative();
                    info.emissionSubcategory = m.getHeatType();
                    info.isMobileSource = m.getIsMobileSource();
                    info.measurementUnit = m.getMeasurementUnit();
                    info.energyCategoryL1 = m.getEnergyCategoryL1();
                    info.energyCategoryL2 = m.getEnergyCategoryL2();
                    info.energyCategoryL3 = m.getEnergyCategoryL3();
                    info.energyUseCategory = m.getEnergyUseCategory();
                    return info;
                }).orElse(null);
            default:
                return null;
        }
    }

    /**
     * 保存节点核算失败的占位记录（无配置时）
     */
    private void saveErrorNodeData(Long calcNodeId, Long nodeId, String description) {
        CalculationNodeData data = new CalculationNodeData();
        data.setCalculationNodeId(calcNodeId);
        data.setDataStatus(2);
        data.setDataMissingDescription(description);
        data.setEnergyMeasurementValue(BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP));
        data.setAdjustedEnergyValue(BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP));
        calculationNodeDataRepository.save(data);
    }

    /**
     * 保存节点核算失败的占位记录（有配置但采集点异常时）
     */
    private void saveErrorNodeData(Long calcNodeId, EmissionNodeConfig config, String description) {
        CalculationNodeData data = new CalculationNodeData();
        data.setCalculationNodeId(calcNodeId);
        data.setCollectionPointType(config.getCollectionPointType());
        data.setCollectionPointId(config.getCollectionPointId());
        data.setEmissionCategory(config.getEmissionCategory());
        data.setCarbonEmissionFactor(config.getCarbonEmissionFactor());
        data.setDataStatus(2);
        data.setDataMissingDescription(description);
        data.setEnergyMeasurementValue(BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP));
        data.setAdjustedEnergyValue(BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP));
        calculationNodeDataRepository.save(data);
    }

    /**
     * 解析 taskConfig 确定核算周期起止日期
     *
     * 复用 EmissionScheduler 中的 taskConfig 解析逻辑（cycleType/delayDays），
     * 基于"延迟核算日期"后的日期推算上一个统计周期：
     * <ul>
     *   <li>DAILY：前一天</li>
     *   <li>WEEKLY：上一周（周一至周日）</li>
     *   <li>MONTHLY：上个月（1日至月末）</li>
     *   <li>QUARTERLY：上一季度</li>
     *   <li>YEARLY：上一年（1月1日至12月31日）</li>
     * </ul>
     * taskConfig 为空或解析失败时，默认按"上月1日到上月末日"。
     *
     * @param taskConfigJson taskConfig JSON 字符串
     * @return [核算周期起始日期, 核算周期截止日期]
     */
    private LocalDate[] parseCalculationCycle(String taskConfigJson) {
        LocalDate now = LocalDate.now();
        LocalDate defaultStart = now.minusMonths(1).withDayOfMonth(1);
        LocalDate defaultEnd = now.minusMonths(1).withDayOfMonth(now.minusMonths(1).lengthOfMonth());

        if (taskConfigJson == null || taskConfigJson.isEmpty()) {
            return new LocalDate[]{defaultStart, defaultEnd};
        }

        try {
            JsonNode taskConfig = objectMapper.readTree(taskConfigJson);
            String cycleType = getTextOrNull(taskConfig, "cycleType");
            int delayDays = taskConfig.has("delayDays") ? taskConfig.get("delayDays").asInt(0) : 0;
            if (delayDays < 0) {
                delayDays = 0;
            }
            LocalDate adjustedDate = now.minusDays(delayDays);

            if (cycleType == null) {
                return new LocalDate[]{defaultStart, defaultEnd};
            }

            switch (cycleType) {
                case "DAILY":
                    return new LocalDate[]{adjustedDate.minusDays(1), adjustedDate.minusDays(1)};
                case "WEEKLY": {
                    LocalDate monday = adjustedDate.with(DayOfWeek.MONDAY);
                    LocalDate prevMonday = monday.minusWeeks(1);
                    LocalDate prevSunday = prevMonday.plusDays(6);
                    return new LocalDate[]{prevMonday, prevSunday};
                }
                case "MONTHLY": {
                    LocalDate prevMonth = adjustedDate.minusMonths(1);
                    return new LocalDate[]{
                            prevMonth.withDayOfMonth(1),
                            prevMonth.withDayOfMonth(prevMonth.lengthOfMonth())
                    };
                }
                case "QUARTERLY": {
                    return computePreviousQuarter(adjustedDate);
                }
                case "YEARLY": {
                    int prevYear = adjustedDate.getYear() - 1;
                    return new LocalDate[]{
                            LocalDate.of(prevYear, 1, 1),
                            LocalDate.of(prevYear, 12, 31)
                    };
                }
                default:
                    return new LocalDate[]{defaultStart, defaultEnd};
            }
        } catch (Exception e) {
            logger.warn("***解析 taskConfig 失败，使用默认核算周期(上月): {}", e.getMessage());
            return new LocalDate[]{defaultStart, defaultEnd};
        }
    }

    /**
     * 计算上一季度的起止日期
     */
    private LocalDate[] computePreviousQuarter(LocalDate date) {
        int month = date.getMonthValue();
        int prevYear = date.getYear();
        int prevQuarterStartMonth;
        if (month >= 1 && month <= 3) {
            // 当前Q1，上一Q4属于去年
            prevQuarterStartMonth = 10;
            prevYear = date.getYear() - 1;
        } else if (month >= 4 && month <= 6) {
            prevQuarterStartMonth = 1;
        } else if (month >= 7 && month <= 9) {
            prevQuarterStartMonth = 4;
        } else {
            prevQuarterStartMonth = 7;
        }
        LocalDate qStart = LocalDate.of(prevYear, prevQuarterStartMonth, 1);
        LocalDate qEnd = qStart.plusMonths(2).withDayOfMonth(qStart.plusMonths(2).lengthOfMonth());
        return new LocalDate[]{qStart, qEnd};
    }

    /**
     * 安全获取 JSON 字段值（复用 EmissionScheduler 的同名逻辑）
     */
    private String getTextOrNull(JsonNode node, String fieldName) {
        JsonNode fieldNode = node.get(fieldName);
        if (fieldNode == null || fieldNode.isNull()) {
            return null;
        }
        String value = fieldNode.asText();
        if ("null".equals(value) || value.isEmpty()) {
            return null;
        }
        return value;
    }

    /**
     * 采集点信息持有者（统一三张采集点表的字段访问）
     */
    private static class CollectionPointInfo {
        Integer isCumulative;
        String emissionSubcategory;
        Integer isMobileSource;
        String measurementUnit;
        String energyCategoryL1;
        String energyCategoryL2;
        String energyCategoryL3;
        String energyUseCategory;
    }
}
