package com.example.carbon.emission.model.repository;

import com.example.carbon.emission.model.entity.DefaultFactor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * 系统缺省碳排放因子设置 Repository
 */
@Repository
public interface DefaultFactorRepository extends JpaRepository<DefaultFactor, Long> {

    /**
     * 按状态查询全部缺省因子（按排放数据小类编码升序）
     */
    List<DefaultFactor> findByStatusOrderBySubcategoryCodeAsc(Integer status);

    /**
     * 按模版ID查询缺省因子（按排放数据小类编码升序）
     * templateId 为 null 时查系统缺省
     */
    List<DefaultFactor> findByTemplateIdOrderBySubcategoryCodeAsc(Long templateId);

    /**
     * 按模版ID+状态查询缺省因子
     */
    List<DefaultFactor> findByTemplateIdAndStatusOrderBySubcategoryCodeAsc(Long templateId, Integer status);

    /**
     * 按模版ID+排放数据小类编码查询（联合唯一）
     */
    Optional<DefaultFactor> findByTemplateIdAndSubcategoryCode(Long templateId, String subcategoryCode);

    /**
     * 按模版ID+排放数据小类名称查询（采集点存中文名时使用）
     */
    Optional<DefaultFactor> findByTemplateIdAndSubcategoryName(Long templateId, String subcategoryName);

    /**
     * 判断指定模版+排放数据小类编码是否已存在缺省因子
     */
    boolean existsByTemplateIdAndSubcategoryCode(Long templateId, String subcategoryCode);

    /**
     * 按模版ID删除全部缺省因子（删模版时级联清理）
     */
    void deleteByTemplateId(Long templateId);

    /**
     * 按排放数据小类编码查询（系统缺省层，templateId=NULL 的场景由应用层控制）
     */
    Optional<DefaultFactor> findBySubcategoryCode(String subcategoryCode);

    /**
     * 判断指定排放数据小类编码是否已存在缺省因子
     */
    boolean existsBySubcategoryCode(String subcategoryCode);

    /**
     * 按排放数据小类编码集合批量查询
     */
    List<DefaultFactor> findBySubcategoryCodeIn(Collection<String> subcategoryCodes);
}
