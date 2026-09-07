package com.example.carbon.emission.model.service.impl;

import com.example.carbon.emission.model.entity.CalcNodeSummary;
import com.example.carbon.emission.model.entity.CalculationTemplate;
import com.example.carbon.emission.model.repository.CalcNodeSummaryRepository;
import com.example.carbon.emission.model.repository.CalculationTemplateRepository;
import com.example.carbon.emission.model.service.CalcNodeSummaryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 碳排放核算节点汇总 Service 实现
 */
@Service
@Transactional(readOnly = true)
public class CalcNodeSummaryServiceImpl implements CalcNodeSummaryService {

    private static final Logger log = LoggerFactory.getLogger(CalcNodeSummaryServiceImpl.class);

    /**
     * 核算节点汇总数据 Repository
     */
    @Autowired
    private CalcNodeSummaryRepository calcNodeSummaryRepository;

    /**
     * 核算任务模板 Repository，用于按日期范围筛选成功的核算任务
     */
    @Autowired
    private CalculationTemplateRepository calculationTemplateRepository;

    @Override
    public List<CalcNodeSummary> queryByTemplate(Long templateId, Long nodeId, String subcategoryCode,
                                                 String l1, String l2, String l3) {
        if (templateId == null) {
            log.warn("queryByTemplate 调用失败：templateId 为 null");
            return Collections.emptyList();
        }

        // 步骤1：以 templateId 为主键拉取该核算任务的全部汇总记录
        List<CalcNodeSummary> all = calcNodeSummaryRepository.findByCalculationTemplateId(templateId);
        if (all == null || all.isEmpty()) {
            return Collections.emptyList();
        }

        // 步骤2：在 Service 层通过 Stream 做维度过滤；参数为 null 时不加入条件
        Stream<CalcNodeSummary> stream = all.stream();

        if (nodeId != null) {
            stream = stream.filter(s -> Objects.equals(nodeId, s.getCalculationNodeId()));
        }
        if (subcategoryCode != null && !subcategoryCode.isBlank()) {
            stream = stream.filter(s -> Objects.equals(subcategoryCode, s.getEmissionSubcategory()));
        }
        if (l1 != null && !l1.isBlank()) {
            stream = stream.filter(s -> Objects.equals(l1, s.getEnergyCategoryL1()));
        }
        if (l2 != null && !l2.isBlank()) {
            stream = stream.filter(s -> Objects.equals(l2, s.getEnergyCategoryL2()));
        }
        if (l3 != null && !l3.isBlank()) {
            stream = stream.filter(s -> Objects.equals(l3, s.getEnergyCategoryL3()));
        }

        return stream.collect(Collectors.toList());
    }

    @Override
    public List<CalcNodeSummary> queryBySourceNodeAndCycle(Long sourceNodeId, LocalDate start, LocalDate end) {
        if (sourceNodeId == null || start == null || end == null) {
            log.warn("queryBySourceNodeAndCycle 调用失败：存在必填参数为 null，sourceNodeId={}, start={}, end={}",
                    sourceNodeId, start, end);
            return Collections.emptyList();
        }

        // 步骤1：查询核算周期起始日期在 [start, end] 区间内且执行成功(status=2)的核算任务
        List<CalculationTemplate> templates = calculationTemplateRepository
                .findByCalculationCycleStartDateBetweenAndStatus(start, end, 2);

        if (templates == null || templates.isEmpty()) {
            log.debug("queryBySourceNodeAndCycle：日期范围内无成功的核算任务，start={}, end={}", start, end);
            return Collections.emptyList();
        }

        // 步骤2：提取模板ID集合
        List<Long> templateIds = templates.stream()
                .map(CalculationTemplate::getId)
                .collect(Collectors.toList());

        // 步骤3：批量查询这些模板下的汇总记录
        List<CalcNodeSummary> summaries = calcNodeSummaryRepository.findByCalculationTemplateIdIn(templateIds);
        if (summaries == null || summaries.isEmpty()) {
            return Collections.emptyList();
        }

        // 步骤4：按 sourceNodeId 做二次过滤，返回匹配结果
        return summaries.stream()
                .filter(s -> Objects.equals(sourceNodeId, s.getSourceNodeId()))
                .collect(Collectors.toList());
    }
}
