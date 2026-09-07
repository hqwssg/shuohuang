package com.example.carbon.emission.model.repository;

import com.example.carbon.emission.model.entity.Template;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TemplateRepository extends JpaRepository<Template, Long> {
    List<Template> findAllByOrderByCreatedAtDesc();
    List<Template> findByCreatedByOrderByCreatedAtDesc(Long userId);
    List<Template> findByEnabledTrue();
    /** 查询启用状态下指定类型的模版（templateType：1-节点模版，2-核算模版） */
    List<Template> findByEnabledTrueAndTemplateType(Integer templateType);

    /** 统计引用了指定因子模版的核算模版数量 */
    long countByFactorTemplateId(Long factorTemplateId);

    /** 统计引用了指定因子模版、且创建人不是指定用户的核算模版数量（用于判断因子模版是否被他人引用而锁定） */
    long countByFactorTemplateIdAndCreatedByNot(Long factorTemplateId, Long createdBy);
}