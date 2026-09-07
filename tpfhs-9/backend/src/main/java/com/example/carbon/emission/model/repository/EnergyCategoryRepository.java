package com.example.carbon.emission.model.repository;

import com.example.carbon.emission.model.entity.EnergyCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EnergyCategoryRepository extends JpaRepository<EnergyCategory, Long> {

    List<EnergyCategory> findByLevelOrderBySortOrderAsc(Integer level);

    List<EnergyCategory> findByParentIdOrderBySortOrderAsc(Long parentId);

    List<EnergyCategory> findAllByOrderByLevelAscSortOrderAsc();

    /**
     * 判断分类编码是否已存在（用于新增时的唯一性校验）
     */
    boolean existsByCategoryCode(String categoryCode);

    /**
     * 判断分类编码是否已被其他记录占用（用于编辑时的唯一性校验）
     */
    boolean existsByCategoryCodeAndIdNot(String categoryCode, Long id);

    /**
     * 根据父级ID统计子分类数量（用于删除前的子分类存在性校验）
     */
    long countByParentId(Long parentId);
}
