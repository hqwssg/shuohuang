package com.example.carbon.emission.model.repository;

import com.example.carbon.emission.model.entity.CalculationTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

/**
 * 碳排放核算模版数据访问接口
 */
@Repository
public interface CalculationTemplateRepository extends JpaRepository<CalculationTemplate, Long> {

    /**
     * 根据模版ID查询核算历史
     *
     * @param templateId 碳排放模版ID（emission_template.id）
     * @return 核算任务记录列表
     */
    List<CalculationTemplate> findByTemplateId(Long templateId);

    /**
     * 根据核算状态查询记录
     * 用于查找待执行(0)、执行中(1)、失败需重试(3)的核算任务
     *
     * @param status 核算状态：0-还未开始，1-正在执行，2-成功，3-失败
     * @return 核算任务记录列表
     */
    List<CalculationTemplate> findByStatus(Integer status);

    /**
     * 根据核算状态集合查询记录
     *
     * @param statuses 核算状态集合
     * @return 核算任务记录列表
     */
    List<CalculationTemplate> findByStatusIn(Collection<Integer> statuses);

    /**
     * 按核算周期起始日期范围+状态查询核算任务记录
     * <p>
     * 用于按源节点+核算周期查询时，先筛选出符合日期区间且执行成功(status=2)的模板
     *
     * @param start  核算周期起始日期（包含）
     * @param end    核算周期起始日期（包含）
     * @param status 核算状态
     * @return 核算任务记录列表
     */
    List<CalculationTemplate> findByCalculationCycleStartDateBetweenAndStatus(
            LocalDate start, LocalDate end, Integer status);
}
