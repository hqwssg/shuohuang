package com.example.carbon.emission.model.repository;

import com.example.carbon.emission.model.entity.PurchasedHeatMeterInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 外购热能用量信息表采集点数据访问接口
 */
@Repository
public interface PurchasedHeatMeterInfoRepository extends JpaRepository<PurchasedHeatMeterInfo, Long> {

    /**
     * 根据所属细分范围ID查询采集点列表并按排序顺序升序排列
     *
     * @param subScopeId 细分范围ID
     * @return 采集点列表
     */
    List<PurchasedHeatMeterInfo> findBySubScopeIdOrderBySortOrderAsc(Long subScopeId);

    /**
     * 根据上级计量表ID查询下级采集点列表并按排序顺序升序排列
     *
     * @param parentMeterId 上级计量表ID
     * @return 下级采集点列表
     */
    List<PurchasedHeatMeterInfo> findByParentMeterIdOrderBySortOrderAsc(Long parentMeterId);

    /**
     * 根据状态查询采集点列表
     *
     * @param status 状态：1-启用，0-停用
     * @return 采集点列表
     */
    List<PurchasedHeatMeterInfo> findByStatus(Integer status);

    /**
     * 根据关键词搜索采集点
     * 支持按名称和拼音编码模糊搜索
     *
     * @param keyword 搜索关键词
     * @return 匹配的采集点列表
     */
    @Query("SELECT m FROM PurchasedHeatMeterInfo m WHERE " +
           "(:keyword IS NULL OR m.name LIKE %:keyword% OR m.pinyinCode LIKE %:keyword%)")
    List<PurchasedHeatMeterInfo> searchByKeyword(@Param("keyword") String keyword);

    /**
     * 根据关键词和细分范围ID搜索采集点
     *
     * @param keyword 搜索关键词
     * @param subScopeId 细分范围ID
     * @return 匹配的采集点列表
     */
    @Query("SELECT m FROM PurchasedHeatMeterInfo m WHERE m.subScopeId = :subScopeId AND " +
           "(:keyword IS NULL OR m.name LIKE %:keyword% OR m.pinyinCode LIKE %:keyword%)")
    List<PurchasedHeatMeterInfo> searchByKeywordAndSubScopeId(@Param("keyword") String keyword,
                                                              @Param("subScopeId") Long subScopeId);

    /**
     * 检查名称是否已存在
     *
     * @param name 采集点名称
     * @return 是否存在
     */
    boolean existsByName(String name);

    /**
     * 根据细分范围ID列表批量查询采集点
     *
     * @param subScopeIds 细分范围ID列表
     * @return 采集点列表
     */
    List<PurchasedHeatMeterInfo> findBySubScopeIdIn(List<Long> subScopeIds);

    /**
     * 更新外购热能采集点的上次采集时间
     * 完成对该采集点数据采集后调用，回填采集点表 last_collection_time 字段
     *
     * @param id 采集点ID
     * @param time 上次采集时间（对应 emission_collection_record.collection_time 的值）
     * @return 受影响行数
     */
    @Modifying
    @Query("UPDATE PurchasedHeatMeterInfo m SET m.lastCollectionTime = :time WHERE m.id = :id")
    int updateLastCollectionTime(@Param("id") Long id, @Param("time") LocalDateTime time);
}