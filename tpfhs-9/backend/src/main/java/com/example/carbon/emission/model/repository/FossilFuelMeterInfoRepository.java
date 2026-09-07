package com.example.carbon.emission.model.repository;

import com.example.carbon.emission.model.entity.FossilFuelMeterInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface FossilFuelMeterInfoRepository extends JpaRepository<FossilFuelMeterInfo, Long> {
    List<FossilFuelMeterInfo> findBySubScopeIdOrderBySortOrderAsc(Long subScopeId);
    List<FossilFuelMeterInfo> findByParentMeterIdOrderBySortOrderAsc(Long parentMeterId);

    @Query("SELECT MAX(m.sortOrder) FROM FossilFuelMeterInfo m WHERE m.subScopeId = :subScopeId")
    Integer findMaxSortOrderBySubScopeId(@Param("subScopeId") Long subScopeId);

    /**
     * 根据状态查询采集点列表
     *
     * @param status 状态：1-启用，0-停用
     * @return 采集点列表
     */
    List<FossilFuelMeterInfo> findByStatus(Integer status);

    /**
     * 更新化石燃料采集点的上次采集时间
     * 完成对该采集点数据采集后调用，回填采集点表 last_collection_time 字段
     *
     * @param id 采集点ID
     * @param time 上次采集时间（对应 emission_collection_record.collection_time 的值）
     * @return 受影响行数
     */
    @Modifying
    @Query("UPDATE FossilFuelMeterInfo m SET m.lastCollectionTime = :time WHERE m.id = :id")
    int updateLastCollectionTime(@Param("id") Long id, @Param("time") LocalDateTime time);
}
