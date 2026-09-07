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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * TemplateValidationServiceImpl 单元测试（纯 Mockito，不启动 Spring 容器）
 * <p>
 * 覆盖模版校验的 5 项检查逻辑、结果定级、富文本生成、结果持久化，
 * 以及 getValidationResult 的读取逻辑。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("碳排放核算模版校验服务测试")
class TemplateValidationServiceImplTest {

    private static final Long TEMPLATE_ID = 1L;
    private static final Long FACTOR_TEMPLATE_ID = 9L;

    @Mock
    private TemplateRepository templateRepository;

    @Mock
    private EmissionNodeRepository nodeRepository;

    @Mock
    private EmissionNodeConfigRepository configRepository;

    @Mock
    private MeterInfoRepository meterInfoRepository;

    @Mock
    private FossilFuelMeterInfoRepository fossilFuelMeterInfoRepository;

    @Mock
    private PurchasedHeatMeterInfoRepository purchasedHeatMeterInfoRepository;

    @Mock
    private DefaultFactorRepository defaultFactorRepository;

    @Mock
    private EmissionFactorTemplateRepository factorTemplateRepository;

    @InjectMocks
    private TemplateValidationServiceImpl service;

    // ========================= 测试前置数据构造 =========================

    /** 构造核算模版 */
    private Template calcTemplate(Long factorTemplateId) {
        Template t = new Template();
        t.setId(TEMPLATE_ID);
        t.setName("核算模版A");
        t.setTemplateType(2);
        t.setFactorTemplateId(factorTemplateId);
        return t;
    }

    /** 构造节点：typeId 1-根节点，2-核算节点，3-采集节点 */
    private EmissionNode node(long id, String name, int typeId, Long parentId) {
        EmissionNode n = new EmissionNode();
        n.setId(id);
        n.setName(name);
        n.setTypeId(typeId);
        n.setParentId(parentId);
        n.setTemplateId(TEMPLATE_ID);
        return n;
    }

    /** 构造采集节点配置 */
    private EmissionNodeConfig config(long nodeId, Integer pointType, Long pointId,
                                      String subcategory, BigDecimal ownFactor) {
        EmissionNodeConfig c = new EmissionNodeConfig();
        c.setNodeId(nodeId);
        c.setCollectionPointType(pointType);
        c.setCollectionPointId(pointId);
        c.setEmissionSubcategory(subcategory);
        c.setCarbonEmissionFactor(ownFactor);
        return c;
    }

    /** 构造因子模版缺省因子 */
    private DefaultFactor factor(String code, String name, String value) {
        DefaultFactor f = new DefaultFactor();
        f.setTemplateId(FACTOR_TEMPLATE_ID);
        f.setSubcategoryCode(code);
        f.setSubcategoryName(name);
        f.setFactorValue(new BigDecimal(value));
        return f;
    }

    /** 构造启用的电力表 */
    private MeterInfo electricMeter(String name, String subcategory, Integer status) {
        MeterInfo m = new MeterInfo();
        m.setName(name);
        m.setEmissionSubcategory(subcategory);
        m.setStatus(status);
        return m;
    }

    /** 桩：因子模版存在且含给定缺省因子 */
    private void stubFactorTemplate(DefaultFactor... factors) {
        EmissionFactorTemplate ft = new EmissionFactorTemplate();
        ft.setId(FACTOR_TEMPLATE_ID);
        ft.setTemplateName("因子模版A");
        when(factorTemplateRepository.findById(FACTOR_TEMPLATE_ID)).thenReturn(Optional.of(ft));
        when(defaultFactorRepository.findByTemplateIdOrderBySubcategoryCodeAsc(FACTOR_TEMPLATE_ID))
                .thenReturn(List.of(factors));
    }

    /** 标准三级节点树：根(1) → 核算(2) → 采集(3) */
    private List<EmissionNode> standardTree() {
        return List.of(
                node(1L, "厂区", 1, null),
                node(2L, "车间", 2, 1L),
                node(3L, "电表节点", 3, 2L)
        );
    }

    // ========================= 入参校验 =========================

    @Test
    @DisplayName("模版不存在时抛出异常")
    void validate_templateNotFound_throws() {
        when(templateRepository.findById(TEMPLATE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.validateTemplate(TEMPLATE_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("模版不存在");
    }

    @Test
    @DisplayName("节点模版（templateType=1）不支持校验，抛出异常")
    void validate_notCalcTemplate_throws() {
        Template t = calcTemplate(FACTOR_TEMPLATE_ID);
        t.setTemplateType(1);
        when(templateRepository.findById(TEMPLATE_ID)).thenReturn(Optional.of(t));

        assertThatThrownBy(() -> service.validateTemplate(TEMPLATE_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("仅核算模版支持校验");
    }

    // ========================= 检查1：碳排放因子模版设置 =========================

    @Test
    @DisplayName("未设置因子模版：错误，结果=4，并保存富文本到模版")
    void validate_noFactorTemplate_errorResult4() {
        Template t = calcTemplate(null);
        when(templateRepository.findById(TEMPLATE_ID)).thenReturn(Optional.of(t));
        when(nodeRepository.findByTemplateId(TEMPLATE_ID)).thenReturn(List.of());

        TemplateValidationResultVO vo = service.validateTemplate(TEMPLATE_ID);

        assertThat(vo.getCheckResult()).isEqualTo(4);
        // 空节点列表额外产生"模版中没有任何节点"告警，共2项
        assertThat(vo.getItems()).hasSize(2);
        assertThat(vo.getItems().get(0).getLevel()).isEqualTo("ERROR");
        assertThat(vo.getItems().get(0).getMessage()).contains("未设置碳排放因子模版");
        assertThat(vo.getCheckTime()).isNotNull();

        // 结果持久化到模版
        ArgumentCaptor<Template> captor = ArgumentCaptor.forClass(Template.class);
        verify(templateRepository).save(captor.capture());
        Template saved = captor.getValue();
        assertThat(saved.getCheckResult()).isEqualTo(4);
        assertThat(saved.getCheckTime()).isNotNull();
        assertThat(saved.getCheckMessage()).contains("【错误】").contains("校验时间：");
    }

    @Test
    @DisplayName("关联的因子模版已被删除：错误")
    void validate_factorTemplateDeleted_error() {
        Template t = calcTemplate(FACTOR_TEMPLATE_ID);
        when(templateRepository.findById(TEMPLATE_ID)).thenReturn(Optional.of(t));
        when(factorTemplateRepository.findById(FACTOR_TEMPLATE_ID)).thenReturn(Optional.empty());
        when(nodeRepository.findByTemplateId(TEMPLATE_ID)).thenReturn(List.of());

        TemplateValidationResultVO vo = service.validateTemplate(TEMPLATE_ID);

        assertThat(vo.getCheckResult()).isEqualTo(4);
        assertThat(vo.getItems().get(0).getMessage()).contains("不存在");
    }

    @Test
    @DisplayName("因子模版未设置任何缺省因子：告警，结果=3")
    void validate_factorTemplateWithoutFactors_warningResult3() {
        Template t = calcTemplate(FACTOR_TEMPLATE_ID);
        when(templateRepository.findById(TEMPLATE_ID)).thenReturn(Optional.of(t));
        EmissionFactorTemplate ft = new EmissionFactorTemplate();
        ft.setId(FACTOR_TEMPLATE_ID);
        ft.setTemplateName("空因子模版");
        when(factorTemplateRepository.findById(FACTOR_TEMPLATE_ID)).thenReturn(Optional.of(ft));
        when(defaultFactorRepository.findByTemplateIdOrderBySubcategoryCodeAsc(FACTOR_TEMPLATE_ID))
                .thenReturn(List.of());
        when(nodeRepository.findByTemplateId(TEMPLATE_ID)).thenReturn(List.of());

        TemplateValidationResultVO vo = service.validateTemplate(TEMPLATE_ID);

        assertThat(vo.getCheckResult()).isEqualTo(3);
        // 空节点列表额外产生"模版中没有任何节点"告警，共2项
        assertThat(vo.getItems()).hasSize(2);
        assertThat(vo.getItems().get(0).getLevel()).isEqualTo("WARNING");
        assertThat(vo.getItems().get(0).getMessage()).contains("未设置任何缺省因子");
    }

    // ========================= 检查2：采集点存在且启用 =========================

    @Test
    @DisplayName("采集节点引用的电力表不存在：错误")
    void validate_electricMeterMissing_error() {
        Template t = calcTemplate(FACTOR_TEMPLATE_ID);
        when(templateRepository.findById(TEMPLATE_ID)).thenReturn(Optional.of(t));
        stubFactorTemplate(factor("PE_01", "外购电力", "0.5568"));
        when(nodeRepository.findByTemplateId(TEMPLATE_ID)).thenReturn(standardTree());
        when(configRepository.findByNodeId(3L))
                .thenReturn(Optional.of(config(3L, 1, 101L, "外购电力", null)));
        when(meterInfoRepository.findById(101L)).thenReturn(Optional.empty());

        TemplateValidationResultVO vo = service.validateTemplate(TEMPLATE_ID);

        assertThat(vo.getCheckResult()).isEqualTo(4);
        assertThat(vo.getItems()).anySatisfy(item -> {
            assertThat(item.getLevel()).isEqualTo("ERROR");
            assertThat(item.getMessage()).contains("电表节点").contains("电力表不存在");
        });
    }

    @Test
    @DisplayName("电力表处于停用状态：错误")
    void validate_electricMeterDisabled_error() {
        Template t = calcTemplate(FACTOR_TEMPLATE_ID);
        when(templateRepository.findById(TEMPLATE_ID)).thenReturn(Optional.of(t));
        stubFactorTemplate(factor("PE_01", "外购电力", "0.5568"));
        when(nodeRepository.findByTemplateId(TEMPLATE_ID)).thenReturn(standardTree());
        when(configRepository.findByNodeId(3L))
                .thenReturn(Optional.of(config(3L, 1, 101L, "外购电力", null)));
        when(meterInfoRepository.findById(101L))
                .thenReturn(Optional.of(electricMeter("一号电表", "外购电力", 0)));

        TemplateValidationResultVO vo = service.validateTemplate(TEMPLATE_ID);

        assertThat(vo.getCheckResult()).isEqualTo(4);
        assertThat(vo.getItems()).anySatisfy(item -> {
            assertThat(item.getLevel()).isEqualTo("ERROR");
            assertThat(item.getMessage()).contains("一号电表").contains("停用状态");
        });
    }

    @Test
    @DisplayName("化石燃料采集点不存在：错误")
    void validate_fossilMeterMissing_error() {
        Template t = calcTemplate(FACTOR_TEMPLATE_ID);
        when(templateRepository.findById(TEMPLATE_ID)).thenReturn(Optional.of(t));
        stubFactorTemplate(factor("FF_D", "天然气", "2.1622"));
        when(nodeRepository.findByTemplateId(TEMPLATE_ID)).thenReturn(standardTree());
        when(configRepository.findByNodeId(3L))
                .thenReturn(Optional.of(config(3L, 2, 201L, "天然气", null)));
        when(fossilFuelMeterInfoRepository.findById(201L)).thenReturn(Optional.empty());

        TemplateValidationResultVO vo = service.validateTemplate(TEMPLATE_ID);

        assertThat(vo.getCheckResult()).isEqualTo(4);
        assertThat(vo.getItems()).anySatisfy(item -> {
            assertThat(item.getLevel()).isEqualTo("ERROR");
            assertThat(item.getMessage()).contains("化石燃料采集点不存在");
        });
    }

    @Test
    @DisplayName("外购热能采集点停用：错误")
    void validate_heatMeterDisabled_error() {
        Template t = calcTemplate(FACTOR_TEMPLATE_ID);
        when(templateRepository.findById(TEMPLATE_ID)).thenReturn(Optional.of(t));
        stubFactorTemplate(factor("HEAT_01", "外购蒸汽", "0.11"));
        when(nodeRepository.findByTemplateId(TEMPLATE_ID)).thenReturn(standardTree());
        when(configRepository.findByNodeId(3L))
                .thenReturn(Optional.of(config(3L, 3, 301L, "外购蒸汽", null)));
        PurchasedHeatMeterInfo meter = new PurchasedHeatMeterInfo();
        meter.setName("蒸汽表");
        meter.setHeatType("外购蒸汽");
        meter.setStatus(0);
        when(purchasedHeatMeterInfoRepository.findById(301L)).thenReturn(Optional.of(meter));

        TemplateValidationResultVO vo = service.validateTemplate(TEMPLATE_ID);

        assertThat(vo.getCheckResult()).isEqualTo(4);
        assertThat(vo.getItems()).anySatisfy(item -> {
            assertThat(item.getLevel()).isEqualTo("ERROR");
            assertThat(item.getMessage()).contains("外购热能采集点").contains("停用状态");
        });
    }

    // ========================= 检查3：能耗小类覆盖 =========================

    @Test
    @DisplayName("采集节点小类未被因子模版覆盖：告警，按小类聚合并列出节点")
    void validate_subcategoryNotCovered_warning() {
        Template t = calcTemplate(FACTOR_TEMPLATE_ID);
        when(templateRepository.findById(TEMPLATE_ID)).thenReturn(Optional.of(t));
        stubFactorTemplate(factor("PE_01", "外购电力", "0.5568"));
        when(nodeRepository.findByTemplateId(TEMPLATE_ID)).thenReturn(standardTree());
        when(configRepository.findByNodeId(3L))
                .thenReturn(Optional.of(config(3L, 1, 101L, "天然气", new BigDecimal("2.1"))));
        when(meterInfoRepository.findById(101L))
                .thenReturn(Optional.of(electricMeter("一号电表", "外购电力", 1)));

        TemplateValidationResultVO vo = service.validateTemplate(TEMPLATE_ID);

        assertThat(vo.getCheckResult()).isEqualTo(3);
        assertThat(vo.getItems()).anySatisfy(item -> {
            assertThat(item.getLevel()).isEqualTo("WARNING");
            assertThat(item.getMessage()).contains("天然气").contains("未被碳排放因子模版覆盖").contains("电表节点");
        });
    }

    // ========================= 检查4：单独因子与因子模版一致性 =========================

    @Test
    @DisplayName("节点单独因子与因子模版不一致：提示，结果=2")
    void validate_ownFactorMismatch_infoResult2() {
        Template t = calcTemplate(FACTOR_TEMPLATE_ID);
        when(templateRepository.findById(TEMPLATE_ID)).thenReturn(Optional.of(t));
        stubFactorTemplate(factor("PE_01", "外购电力", "0.5568"));
        when(nodeRepository.findByTemplateId(TEMPLATE_ID)).thenReturn(standardTree());
        when(configRepository.findByNodeId(3L))
                .thenReturn(Optional.of(config(3L, 1, 101L, "外购电力", new BigDecimal("0.6"))));
        when(meterInfoRepository.findById(101L))
                .thenReturn(Optional.of(electricMeter("一号电表", "外购电力", 1)));

        TemplateValidationResultVO vo = service.validateTemplate(TEMPLATE_ID);

        assertThat(vo.getCheckResult()).isEqualTo(2);
        assertThat(vo.getItems()).hasSize(1);
        assertThat(vo.getItems().get(0).getLevel()).isEqualTo("INFO");
        assertThat(vo.getItems().get(0).getMessage())
                .contains("不一致")
                .contains("0.6")
                .contains("0.5568");
    }

    @Test
    @DisplayName("节点单独因子与因子模版一致：无提示，结果=1")
    void validate_ownFactorMatch_result1() {
        Template t = calcTemplate(FACTOR_TEMPLATE_ID);
        when(templateRepository.findById(TEMPLATE_ID)).thenReturn(Optional.of(t));
        stubFactorTemplate(factor("PE_01", "外购电力", "0.5568"));
        when(nodeRepository.findByTemplateId(TEMPLATE_ID)).thenReturn(standardTree());
        when(configRepository.findByNodeId(3L))
                .thenReturn(Optional.of(config(3L, 1, 101L, "外购电力", new BigDecimal("0.5568"))));
        when(meterInfoRepository.findById(101L))
                .thenReturn(Optional.of(electricMeter("一号电表", "外购电力", 1)));

        TemplateValidationResultVO vo = service.validateTemplate(TEMPLATE_ID);

        assertThat(vo.getCheckResult()).isEqualTo(1);
        assertThat(vo.getItems()).isEmpty();
        assertThat(vo.getCheckMessage()).contains("【通过】");
    }

    // ========================= 检查5：空核算节点 =========================

    @Test
    @DisplayName("核算节点下没有任何下属采集节点：告警")
    void validate_emptyCalcNode_warning() {
        Template t = calcTemplate(FACTOR_TEMPLATE_ID);
        when(templateRepository.findById(TEMPLATE_ID)).thenReturn(Optional.of(t));
        stubFactorTemplate(factor("PE_01", "外购电力", "0.5568"));
        when(nodeRepository.findByTemplateId(TEMPLATE_ID)).thenReturn(List.of(
                node(1L, "厂区", 1, null),
                node(2L, "空车间", 2, 1L) // 核算节点，无子节点
        ));

        TemplateValidationResultVO vo = service.validateTemplate(TEMPLATE_ID);

        assertThat(vo.getCheckResult()).isEqualTo(3);
        assertThat(vo.getItems()).hasSize(1);
        assertThat(vo.getItems().get(0).getMessage()).contains("空车间").contains("没有任何下属采集节点");
    }

    @Test
    @DisplayName("核算节点的下级核算节点下存在采集节点：不告警（DFS深层检查）")
    void validate_calcNodeWithDeepCollectionDescendant_noWarning() {
        Template t = calcTemplate(FACTOR_TEMPLATE_ID);
        when(templateRepository.findById(TEMPLATE_ID)).thenReturn(Optional.of(t));
        stubFactorTemplate(factor("PE_01", "外购电力", "0.5568"));
        when(nodeRepository.findByTemplateId(TEMPLATE_ID)).thenReturn(List.of(
                node(1L, "厂区", 1, null),
                node(2L, "车间", 2, 1L),
                node(3L, "工段", 2, 2L),
                node(4L, "电表节点", 3, 3L)
        ));
        when(configRepository.findByNodeId(4L))
                .thenReturn(Optional.of(config(4L, 1, 101L, "外购电力", null)));
        when(meterInfoRepository.findById(101L))
                .thenReturn(Optional.of(electricMeter("一号电表", "外购电力", 1)));

        TemplateValidationResultVO vo = service.validateTemplate(TEMPLATE_ID);

        assertThat(vo.getCheckResult()).isEqualTo(1);
        assertThat(vo.getItems()).isEmpty();
    }

    // ========================= 综合场景 =========================

    @Test
    @DisplayName("完整正确场景：因子模版覆盖、采集点启用、无空核算节点 → 结果=1并持久化")
    void validate_happyPath_result1_andSaved() {
        Template t = calcTemplate(FACTOR_TEMPLATE_ID);
        when(templateRepository.findById(TEMPLATE_ID)).thenReturn(Optional.of(t));
        stubFactorTemplate(factor("PE_01", "外购电力", "0.5568"));
        when(nodeRepository.findByTemplateId(TEMPLATE_ID)).thenReturn(standardTree());
        when(configRepository.findByNodeId(3L))
                .thenReturn(Optional.of(config(3L, 1, 101L, "外购电力", null)));
        when(meterInfoRepository.findById(101L))
                .thenReturn(Optional.of(electricMeter("一号电表", "外购电力", 1)));

        TemplateValidationResultVO vo = service.validateTemplate(TEMPLATE_ID);

        assertThat(vo.getCheckResult()).isEqualTo(1);
        assertThat(vo.getItems()).isEmpty();
        assertThat(vo.getCheckTime()).isNotNull();
        assertThat(vo.getCheckMessage()).contains("【通过】").contains("错误 0");

        ArgumentCaptor<Template> captor = ArgumentCaptor.forClass(Template.class);
        verify(templateRepository).save(captor.capture());
        Template saved = captor.getValue();
        assertThat(saved.getCheckResult()).isEqualTo(1);
        assertThat(saved.getCheckTime()).isNotNull();
        assertThat(saved.getCheckMessage()).isNotBlank();
    }

    @Test
    @DisplayName("节点配置缺失小类时回退使用采集点表中的小类参与覆盖检查")
    void validate_subcategoryFallbackToMeter() {
        Template t = calcTemplate(FACTOR_TEMPLATE_ID);
        when(templateRepository.findById(TEMPLATE_ID)).thenReturn(Optional.of(t));
        stubFactorTemplate(factor("PE_01", "外购电力", "0.5568"));
        when(nodeRepository.findByTemplateId(TEMPLATE_ID)).thenReturn(standardTree());
        // 节点配置未设置小类（null），电力表小类为"外购电力"，因子模版可覆盖 → 不告警
        when(configRepository.findByNodeId(3L))
                .thenReturn(Optional.of(config(3L, 1, 101L, null, null)));
        when(meterInfoRepository.findById(101L))
                .thenReturn(Optional.of(electricMeter("一号电表", "外购电力", 1)));

        TemplateValidationResultVO vo = service.validateTemplate(TEMPLATE_ID);

        assertThat(vo.getCheckResult()).isEqualTo(1);
        assertThat(vo.getItems()).isEmpty();
    }

    @Test
    @DisplayName("模版无任何节点：告警")
    void validate_noNodes_warning() {
        Template t = calcTemplate(FACTOR_TEMPLATE_ID);
        when(templateRepository.findById(TEMPLATE_ID)).thenReturn(Optional.of(t));
        stubFactorTemplate(factor("PE_01", "外购电力", "0.5568"));
        when(nodeRepository.findByTemplateId(TEMPLATE_ID)).thenReturn(List.of());

        TemplateValidationResultVO vo = service.validateTemplate(TEMPLATE_ID);

        assertThat(vo.getCheckResult()).isEqualTo(3);
        assertThat(vo.getItems()).anySatisfy(item ->
                assertThat(item.getMessage()).contains("模版中没有任何节点"));
    }

    @Test
    @DisplayName("多级别并存时按最高严重级别定级：错误优先于告警和提示")
    void validate_mixedLevels_errorWins() {
        Template t = calcTemplate(FACTOR_TEMPLATE_ID);
        when(templateRepository.findById(TEMPLATE_ID)).thenReturn(Optional.of(t));
        stubFactorTemplate(factor("PE_01", "外购电力", "0.5568"));
        // 空核算节点（告警）+ 采集点不存在（错误）+ 单独因子不一致（提示）
        when(nodeRepository.findByTemplateId(TEMPLATE_ID)).thenReturn(List.of(
                node(1L, "厂区", 1, null),
                node(2L, "空车间", 2, 1L),
                node(3L, "电表节点", 3, 1L)
        ));
        when(configRepository.findByNodeId(3L))
                .thenReturn(Optional.of(config(3L, 1, 101L, "外购电力", new BigDecimal("0.6"))));
        when(meterInfoRepository.findById(101L)).thenReturn(Optional.empty());

        TemplateValidationResultVO vo = service.validateTemplate(TEMPLATE_ID);

        assertThat(vo.getCheckResult()).isEqualTo(4);
        assertThat(vo.getItems()).extracting(TemplateValidationResultVO.Item::getLevel)
                .contains("ERROR", "WARNING", "INFO");
    }

    // ========================= getValidationResult =========================

    @Test
    @DisplayName("查询校验结果：未校验过时返回 checkResult=0")
    void getValidationResult_notChecked_returnsZero() {
        Template t = calcTemplate(FACTOR_TEMPLATE_ID);
        t.setCheckResult(null);
        when(templateRepository.findById(TEMPLATE_ID)).thenReturn(Optional.of(t));

        TemplateValidationResultVO vo = service.getValidationResult(TEMPLATE_ID);

        assertThat(vo.getCheckResult()).isZero();
        assertThat(vo.getCheckTime()).isNull();
        assertThat(vo.getCheckMessage()).isNull();
    }

    @Test
    @DisplayName("查询校验结果：返回模版中存储的最近一次结果")
    void getValidationResult_returnsStored() {
        Template t = calcTemplate(FACTOR_TEMPLATE_ID);
        t.setCheckResult(3);
        t.setCheckTime(LocalDateTime.of(2026, 9, 4, 10, 0, 0));
        t.setCheckMessage("<div>【告警】测试</div>");
        when(templateRepository.findById(TEMPLATE_ID)).thenReturn(Optional.of(t));

        TemplateValidationResultVO vo = service.getValidationResult(TEMPLATE_ID);

        assertThat(vo.getCheckResult()).isEqualTo(3);
        assertThat(vo.getCheckTime()).isEqualTo(LocalDateTime.of(2026, 9, 4, 10, 0, 0));
        assertThat(vo.getCheckMessage()).contains("【告警】");
    }
}
