package com.example.carbon.emission.model.repository;

import com.example.carbon.emission.model.entity.PurchasedHeatCollectionSubScope;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 外购热能采集细分范围数据访问接口
 */
@Repository
public interface PurchasedHeatCollectionSubScopeRepository extends JpaRepository<PurchasedHeatCollectionSubScope, Long> {

    /**
     * 根据所属采集范围ID查询细分范围列表并按排序顺序升序排列
     *
     * @param collectionScopeId 采集范围ID
     * @return 细分范围列表
     */
    List<PurchasedHeatCollectionSubScope> findByCollectionScopeIdOrderBySortOrderAsc(Long collectionScopeId);

    /**
     * 检查细分范围名称是否已存在
     *
     * @param subScopeName 细分范围名称
     * @return 是否存在
     */
    boolean existsBySubScopeName(String subScopeName);

    /**
     * 根据所属采集范围ID查询最大的排序顺序值
     *
     * @param collectionScopeId 采集范围ID
     * @return 最大排序值
     */
    @Query("SELECT MAX(s.sortOrder) FROM PurchasedHeatCollectionSubScope s WHERE s.collectionScopeId = :collectionScopeId")
    Integer findMaxSortOrderByCollectionScopeId(@Param("collectionScopeId") Long collectionScopeId);
}