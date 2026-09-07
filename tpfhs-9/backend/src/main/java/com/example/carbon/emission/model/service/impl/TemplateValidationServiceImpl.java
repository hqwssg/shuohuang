package com.example.carbon.emission.model.service.impl;

import com.example.carbon.emission.model.dto.TemplateValidationResultVO;
import com.example.carbon.emission.model.entity.DefaultFactor;
import com.example.carbon.emission.model.entity.EmissionFactorTemplate;
import com.example.carbon.emission.model.entity.EmissionNode;
import com.example.carbon.emission.model.entity.EmissionNodeConfig;
import com.example.carbon.emission.model.entity.FossilFuelMeterInfo;
import com.example.carbon.emission.model.entity.MeterInfo;
import com.example.carbon.emission.model.entity.PurchasedHeatMeterInfo;
import com.example.carbon.emission.model.entity.Template;
import com.example.carbon.emission.model.repository.DefaultFactorRepository;
import com.example.carbon.emission.model.repository.EmissionFactorTemplateRepository;
import com.example.carbon.emission.model.repository.EmissionNodeConfigRepository;
import com.example.carbon.emission.model.repository.EmissionNodeRepository;
import com.example.carbon.emission.model.repository.FossilFuelMeterInfoRepository;
import com.example.carbon.emission.model.repository.MeterInfoRepository;
import com.example.carbon.emission.model.repository.PurchasedHeatMeterInfoRepository;
import com.example.carbon.emission.model.repository.TemplateRepository;
import com.example.carbon.emission.model.service.TemplateValidationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 碳排放核算模版校验服务实现
 * <p>
 * 校验结果以三级颜色区分并保存为富文本HTML：
 * 错误-红(#F56C6C)、告警-橙(#E6A23C)、提示-蓝(#409EFF)、通过-绿(#67C23A)。
 */
@Service
public class TemplateValidationServiceImpl implements TemplateValidationService {

    private static final Logger log = LoggerFactory.getLogger(TemplateValidationServiceImpl.class);

    private static final String LEVEL_ERROR = "ERROR";
    private static final String LEVEL_WARNING = "WARNING";
    private static final String LEVEL_INFO = "INFO";

    private static final String COLOR_ERROR = "#F56C6C";
    private static final String COLOR_WARNING = "#E6A23C";
    private static final String COLOR_INFO = "#409EFF";
    private static final String COLOR_PASS = "#67C23A";
    private static final String COLOR_SUMMARY = "#909399";

    private static final String LABEL_ERROR = "错误";
    private static final String LABEL_WARNING = "告警";
    private static final String LABEL_INFO = "提示";

    /** 节点类型：2-核算节点，3-采集节点 */
    private static final int NODE_TYPE_CALC = 2;
    private static final int NODE_TYPE_COLLECTION = 3;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private TemplateRepository templateRepository;

    @Autowired
    private EmissionNodeRepository nodeRepository;

    @Autowired
    private EmissionNodeConfigRepository configRepository;

    @Autowired
    private MeterInfoRepository meterInfoRepository;

    @Autowired
    private FossilFuelMeterInfoRepository fossilFuelMeterInfoRepository;

    @Autowired
    private PurchasedHeatMeterInfoRepository purchasedHeatMeterInfoRepository;

    @Autowired
    private DefaultFactorRepository defaultFactorRepository;

    @Autowired
    private EmissionFactorTemplateRepository factorTemplateRepository;

    /**
     * 执行模版校验并保存结果
     * <p>
     * 依次执行5项检查：因子模版设置、采集点存在且启用、能耗小类覆盖、
     * 单独因子与模版因子一致性、空核算节点。汇总后按最高严重级别确定
     * checkResult（4-存在错误 > 3-存在告警 > 2-存在提示 > 1-完全正确），
     * 并将富文本详情保存到 emission_template。
     *
     * @param templateId 核算模版ID
     * @return 校验结果（含富文本与结构化明细）
     */
    @Override
    @Transactional
    public TemplateValidationResultVO validateTemplate(Long templateId) {
        Template template = templateRepository.findById(templateId)
                .orElseThrow(() -> new RuntimeException("模版不存在: " + templateId));
        if (template.getTemplateType() == null || template.getTemplateType() != 2) {
            throw new RuntimeException("仅核算模版支持校验");
        }

        List<TemplateValidationResultVO.Item> items = new ArrayList<>();

        // ---------- 检查1：模版是否设置了碳排放因子模版 ----------
        Long factorTemplateId = template.getFactorTemplateId();
        List<DefaultFactor> factorTemplateFactors = new ArrayList<>();
        if (factorTemplateId == null) {
            items.add(new TemplateValidationResultVO.Item(LEVEL_ERROR,
                    "模版未设置碳排放因子模版，请在模版属性中选择"));
        } else {
            Optional<EmissionFactorTemplate> ftOpt = factorTemplateRepository.findById(factorTemplateId);
            if (ftOpt.isEmpty()) {
                items.add(new TemplateValidationResultVO.Item(LEVEL_ERROR,
                        "模版关联的碳排放因子模版不存在（ID=" + factorTemplateId + "），可能已被删除，请重新选择"));
            } else {
                factorTemplateFactors = defaultFactorRepository
                        .findByTemplateIdOrderBySubcategoryCodeAsc(factorTemplateId);
                if (factorTemplateFactors.isEmpty()) {
                    items.add(new TemplateValidationResultVO.Item(LEVEL_WARNING,
                            "碳排放因子模版「" + ftOpt.get().getTemplateName() + "」中未设置任何缺省因子"));
                }
            }
        }

        // ---------- 加载模版节点及配置 ----------
        List<EmissionNode> nodes = nodeRepository.findByTemplateId(templateId);
        if (nodes.isEmpty()) {
            items.add(new TemplateValidationResultVO.Item(LEVEL_WARNING, "模版中没有任何节点"));
        }

        // parentId → 子节点索引（用于空核算节点检查）
        Map<Long, List<EmissionNode>> childrenMap = nodes.stream()
                .filter(n -> n.getParentId() != null)
                .collect(Collectors.groupingBy(EmissionNode::getParentId));

        // ---------- 检查2/3/4：遍历采集节点 ----------
        // 未覆盖小类 → 节点名列表（聚合同一小类的告警）
        Map<String, List<String>> uncoveredMap = new LinkedHashMap<>();
        // 已在告警/提示中报告过的小类（避免重复输出）
        Set<String> reportedSubcategories = new HashSet<>();

        for (EmissionNode node : nodes) {
            if (node.getTypeId() == null || node.getTypeId() != NODE_TYPE_COLLECTION) {
                continue;
            }
            String nodeName = node.getName();

            EmissionNodeConfig config = configRepository.findByNodeId(node.getId()).orElse(null);
            if (config == null) {
                items.add(new TemplateValidationResultVO.Item(LEVEL_WARNING,
                        "采集节点「" + nodeName + "」缺少节点配置信息"));
                continue;
            }

            // ---------- 检查2：采集点是否存在且启用 ----------
            String meterName = null;
            String meterSubcategory = null;
            if (config.getCollectionPointType() != null && config.getCollectionPointId() != null) {
                Integer type = config.getCollectionPointType();
                Long pointId = config.getCollectionPointId();
                switch (type) {
                    case 1: {
                        Optional<MeterInfo> m = meterInfoRepository.findById(pointId);
                        if (m.isEmpty()) {
                            items.add(new TemplateValidationResultVO.Item(LEVEL_ERROR,
                                    "采集节点「" + nodeName + "」引用的电力表不存在（采集点ID=" + pointId + "）"));
                        } else {
                            MeterInfo meter = m.get();
                            meterName = meter.getName();
                            meterSubcategory = meter.getEmissionSubcategory();
                            if (meter.getStatus() != null && meter.getStatus() == 0) {
                                items.add(new TemplateValidationResultVO.Item(LEVEL_ERROR,
                                        "采集节点「" + nodeName + "」引用的电力表「" + meterName + "」处于停用状态"));
                            }
                        }
                        break;
                    }
                    case 2: {
                        Optional<FossilFuelMeterInfo> m = fossilFuelMeterInfoRepository.findById(pointId);
                        if (m.isEmpty()) {
                            items.add(new TemplateValidationResultVO.Item(LEVEL_ERROR,
                                    "采集节点「" + nodeName + "」引用的化石燃料采集点不存在（采集点ID=" + pointId + "）"));
                        } else {
                            FossilFuelMeterInfo meter = m.get();
                            meterName = meter.getName();
                            meterSubcategory = meter.getFuelType(); // 列emission_subcategory：化石燃料种类
                            if (meter.getStatus() != null && meter.getStatus() == 0) {
                                items.add(new TemplateValidationResultVO.Item(LEVEL_ERROR,
                                        "采集节点「" + nodeName + "」引用的化石燃料采集点「" + meterName + "」处于停用状态"));
                            }
                        }
                        break;
                    }
                    case 3: {
                        Optional<PurchasedHeatMeterInfo> m = purchasedHeatMeterInfoRepository.findById(pointId);
                        if (m.isEmpty()) {
                            items.add(new TemplateValidationResultVO.Item(LEVEL_ERROR,
                                    "采集节点「" + nodeName + "」引用的外购热能采集点不存在（采集点ID=" + pointId + "）"));
                        } else {
                            PurchasedHeatMeterInfo meter = m.get();
                            meterName = meter.getName();
                            meterSubcategory = meter.getHeatType(); // 列emission_subcategory：热力种类
                            if (meter.getStatus() != null && meter.getStatus() == 0) {
                                items.add(new TemplateValidationResultVO.Item(LEVEL_ERROR,
                                        "采集节点「" + nodeName + "」引用的外购热能采集点「" + meterName + "」处于停用状态"));
                            }
                        }
                        break;
                    }
                    default:
                        items.add(new TemplateValidationResultVO.Item(LEVEL_ERROR,
                                "采集节点「" + nodeName + "」的采集点类型非法（" + type + "）"));
                }
            }

            // 节点小类：优先取节点配置，缺省回退采集点表中的小类
            String nodeSubcategory = trimToNull(config.getEmissionSubcategory());
            if (nodeSubcategory == null) {
                nodeSubcategory = trimToNull(meterSubcategory);
            }

            // ---------- 检查3：因子模版能耗小类覆盖检查 ----------
            if (nodeSubcategory != null && !factorTemplateFactors.isEmpty()) {
                DefaultFactor matched = findMatchedFactor(factorTemplateFactors, nodeSubcategory);
                if (matched == null) {
                    uncoveredMap.computeIfAbsent(nodeSubcategory, k -> new ArrayList<>()).add(nodeName);
                }

                // ---------- 检查4：单独设置的因子与因子模版一致性 ----------
                BigDecimal nodeFactor = config.getCarbonEmissionFactor();
                if (nodeFactor != null && matched != null && matched.getFactorValue() != null
                        && nodeFactor.compareTo(matched.getFactorValue()) != 0) {
                    items.add(new TemplateValidationResultVO.Item(LEVEL_INFO,
                            "采集节点「" + nodeName + "」单独设置的碳排放因子（" + nodeFactor.stripTrailingZeros().toPlainString()
                                    + "）与因子模版中小类「" + nodeSubcategory + "」的因子值（"
                                    + matched.getFactorValue().stripTrailingZeros().toPlainString() + "）不一致，请确认"));
                }
            }
        }

        // 输出未覆盖小类的聚合告警
        for (Map.Entry<String, List<String>> entry : uncoveredMap.entrySet()) {
            String subcategory = entry.getKey();
            List<String> nodeNames = entry.getValue();
            items.add(new TemplateValidationResultVO.Item(LEVEL_WARNING,
                    "能耗小类「" + subcategory + "」未被碳排放因子模版覆盖，涉及 " + nodeNames.size()
                            + " 个采集节点（" + String.join("、", nodeNames)
                            + "），若这些节点未单独设置碳排放因子将无法完成核算"));
        }

        // ---------- 检查5：空核算节点（下属无任何采集节点） ----------
        for (EmissionNode node : nodes) {
            if (node.getTypeId() == null || node.getTypeId() != NODE_TYPE_CALC) {
                continue;
            }
            if (!hasCollectionDescendant(node.getId(), childrenMap, new HashSet<>())) {
                items.add(new TemplateValidationResultVO.Item(LEVEL_WARNING,
                        "核算节点「" + node.getName() + "」下没有任何下属采集节点"));
            }
        }

        // ---------- 汇总并保存 ----------
        long errorCount = items.stream().filter(i -> LEVEL_ERROR.equals(i.getLevel())).count();
        long warningCount = items.stream().filter(i -> LEVEL_WARNING.equals(i.getLevel())).count();
        long infoCount = items.stream().filter(i -> LEVEL_INFO.equals(i.getLevel())).count();

        int checkResult;
        if (errorCount > 0) {
            checkResult = 4;
        } else if (warningCount > 0) {
            checkResult = 3;
        } else if (infoCount > 0) {
            checkResult = 2;
        } else {
            checkResult = 1;
        }

        LocalDateTime checkTime = LocalDateTime.now();
        String html = buildCheckMessageHtml(checkTime, errorCount, warningCount, infoCount, items);

        template.setCheckResult(checkResult);
        template.setCheckTime(checkTime);
        template.setCheckMessage(html);
        templateRepository.save(template);

        log.info("模版校验完成：templateId={}，结果={}，错误={}，告警={}，提示={}",
                templateId, checkResult, errorCount, warningCount, infoCount);

        TemplateValidationResultVO vo = new TemplateValidationResultVO();
        vo.setCheckResult(checkResult);
        vo.setCheckTime(checkTime);
        vo.setCheckMessage(html);
        vo.setItems(items);
        return vo;
    }

    /**
     * 查询模版最近一次保存的校验结果（不重新校验）
     *
     * @param templateId 核算模版ID
     * @return 校验结果；未校验过时 checkResult=0、checkMessage 为空
     */
    @Override
    public TemplateValidationResultVO getValidationResult(Long templateId) {
        Template template = templateRepository.findById(templateId)
                .orElseThrow(() -> new RuntimeException("模版不存在: " + templateId));
        TemplateValidationResultVO vo = new TemplateValidationResultVO();
        vo.setCheckResult(template.getCheckResult() != null ? template.getCheckResult() : 0);
        vo.setCheckTime(template.getCheckTime());
        vo.setCheckMessage(template.getCheckMessage());
        return vo;
    }

    /**
     * 在因子模版缺省因子中查找与小类匹配的因子
     * <p>
     * 匹配规则：小类字符串与因子的 subcategoryName 或 subcategoryCode
     * 忽略大小写、忽略首尾空白比对。
     *
     * @param factors         因子模版缺省因子列表
     * @param nodeSubcategory 采集节点小类（编码或中文名）
     * @return 匹配的因子；未匹配返回 null
     */
    private DefaultFactor findMatchedFactor(List<DefaultFactor> factors, String nodeSubcategory) {
        String target = nodeSubcategory.trim();
        for (DefaultFactor factor : factors) {
            if (equalsNorm(target, factor.getSubcategoryName())
                    || equalsNorm(target, factor.getSubcategoryCode())) {
                return factor;
            }
        }
        return null;
    }

    /**
     * 判断采集节点在其子树中是否至少存在一个采集节点（含自身）
     *
     * @param nodeId      起始节点ID
     * @param childrenMap parentId→子节点索引
     * @param visited     已访问节点ID（防环）
     * @return 子树中存在采集节点返回 true
     */
    private boolean hasCollectionDescendant(Long nodeId, Map<Long, List<EmissionNode>> childrenMap,
                                            Set<Long> visited) {
        if (!visited.add(nodeId)) {
            return false;
        }
        List<EmissionNode> children = childrenMap.get(nodeId);
        if (children == null) {
            return false;
        }
        for (EmissionNode child : children) {
            if (child.getTypeId() != null && child.getTypeId() == NODE_TYPE_COLLECTION) {
                return true;
            }
            if (hasCollectionDescendant(child.getId(), childrenMap, visited)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 构造校验结果富文本HTML
     * <p>
     * 首行为灰色汇总行（校验时间+各级数量），随后每条明细按级别着色：
     * 错误-红、告警-橙、提示-蓝；无任何问题时输出绿色通过行。
     *
     * @param checkTime    校验时间
     * @param errorCount   错误数
     * @param warningCount 告警数
     * @param infoCount    提示数
     * @param items        明细列表
     * @return 富文本HTML
     */
    private String buildCheckMessageHtml(LocalDateTime checkTime, long errorCount,
                                         long warningCount, long infoCount,
                                         List<TemplateValidationResultVO.Item> items) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div style=\"color:").append(COLOR_SUMMARY).append(";margin-bottom:6px;\">")
                .append("校验时间：").append(checkTime.format(TIME_FORMATTER))
                .append("｜错误 ").append(errorCount)
                .append(" 项｜告警 ").append(warningCount)
                .append(" 项｜提示 ").append(infoCount)
                .append(" 项</div>");

        if (items.isEmpty()) {
            sb.append("<div style=\"color:").append(COLOR_PASS).append(";\">")
                    .append("【通过】模版校验完成，未发现问题</div>");
            return sb.toString();
        }

        for (TemplateValidationResultVO.Item item : items) {
            String color;
            String label;
            switch (item.getLevel()) {
                case LEVEL_ERROR:
                    color = COLOR_ERROR;
                    label = LABEL_ERROR;
                    break;
                case LEVEL_WARNING:
                    color = COLOR_WARNING;
                    label = LABEL_WARNING;
                    break;
                default:
                    color = COLOR_INFO;
                    label = LABEL_INFO;
            }
            sb.append("<div style=\"color:").append(color).append(";line-height:1.8;\">")
                    .append("【").append(label).append("】")
                    .append(escapeHtml(item.getMessage()))
                    .append("</div>");
        }
        return sb.toString();
    }

    /**
     * HTML转义，防止模版/节点名称中的特殊字符破坏富文本结构
     */
    private String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    /**
     * 忽略大小写与首尾空白的字符串比对
     */
    private boolean equalsNorm(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        return a.trim().equalsIgnoreCase(b.trim());
    }

    /**
     * trim 工具：空白返回 null
     */
    private String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
