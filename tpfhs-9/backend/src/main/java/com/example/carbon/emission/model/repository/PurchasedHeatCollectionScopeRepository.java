package com.example.carbon.emission.model.repository;

import com.example.carbon.emission.model.entity.PurchasedHeatCollectionScope;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 外购热能采集范围数据访问接口
 */
@Repository
public interface PurchasedHeatCollectionScopeRepository extends JpaRepository<PurchasedHeatCollectionScope, Long> {

    /**
     * 查询所有采集范围并按排序顺序升序排列
     *
     * @return 采集范围列表
     */
    List<PurchasedHeatCollectionScope> findAllByOrderBySortOrderAsc();

    /**
     * 检查采集范围名称是否已存在
     *
     * @param scopeName 采集范围名称
     * @return 是否存在
     */
    boolean existsByScopeName(String scopeName);

    /**
     * 获取最大的排序顺序值
     *
     * @return 最大排序值
     */
    @Query("SELECT MAX(s.sortOrder) FROM PurchasedHeatCollectionScope s")
    Integer findMaxSortOrder();
}