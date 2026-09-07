package com.example.carbon.emission.model.repository;

import com.example.carbon.emission.model.entity.MeterInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 电表信息数据访问接口
 */
@Repository
public interface MeterInfoRepository extends JpaRepository<MeterInfo, Long> {
    
    /**
     * 根据名称查询电表
     *
     * @param name 电表名称
     * @return 电表列表
     */
    List<MeterInfo> findByName(String name);
    
    /**
     * 根据拼音首字母编码查询
     *
     * @param pinyinCode 拼音首字母编码
     * @return 电表列表
     */
    List<MeterInfo> findByPinyinCodeStartingWith(String pinyinCode);
    
    /**
     * 根据所属集中器ID查询电表
     *
     * @param pointId 集中器ID
     * @return 电表列表
     */
    List<MeterInfo> findByPointId(Long pointId);
    
    /**
     * 根据所属集中器ID查询电表按排序顺序
     *
     * @param pointId 集中器ID
     * @return 电表列表（按sortOrder升序）
     */
    List<MeterInfo> findByPointIdOrderBySortOrderAsc(Long pointId);
    
    /**
     * 根据上级电表ID查询下级电表
     *
     * @param parentMeterId 上级电表ID
     * @return 下级电表列表
     */
    List<MeterInfo> findByParentMeterId(Long parentMeterId);
    
    /**
     * 根据上级电表ID查询下级电表按排序顺序
     *
     * @param parentMeterId 上级电表ID
     * @return 下级电表列表（按sortOrder升序）
     */
    List<MeterInfo> findByParentMeterIdOrderBySortOrderAsc(Long parentMeterId);
    
    /**
     * 根据能耗数据划拨方式查询
     *
     * @param energyAllocation 能耗数据划拨方式
     * @return 电表列表
     */
    List<MeterInfo> findByEnergyAllocation(String energyAllocation);
    
    /**
     * 根据状态查询电表
     *
     * @param status 状态：1-启用，0-停用
     * @return 电表列表
     */
    List<MeterInfo> findByStatus(Integer status);
    
    /**
     * 根据关键词搜索电表
     * 支持按名称和拼音编码模糊搜索
     *
     * @param keyword 搜索关键词
     * @return 匹配的电表列表
     */
    @Query("SELECT m FROM MeterInfo m WHERE " +
           "(:keyword IS NULL OR m.name LIKE %:keyword% OR m.pinyinCode LIKE %:keyword%)")
    List<MeterInfo> searchByKeyword(@Param("keyword") String keyword);
    
    /**
     * 根据关键词和集中器ID搜索电表
     *
     * @param keyword 搜索关键词
     * @param pointId 集中器ID
     * @return 匹配的电表列表
     */
    @Query("SELECT m FROM MeterInfo m WHERE m.pointId = :pointId AND " +
           "(:keyword IS NULL OR m.name LIKE %:keyword% OR m.pinyinCode LIKE %:keyword%)")
    List<MeterInfo> searchByKeywordAndPointId(@Param("keyword") String keyword, 
                                               @Param("pointId") Long pointId);
    
    /**
     * 检查名称是否已存在
     *
     * @param name 电表名称
     * @return 是否存在
     */
    boolean existsByName(String name);
    
    /**
     * 根据集中器ID列表批量查询电表
     *
     * @param pointIds 集中器ID列表
     * @return 电表列表
     */
    List<MeterInfo> findByPointIdIn(List<Long> pointIds);

    /**
     * 更新电表的上次采集时间
     * 完成对该采集点数据采集后调用，回填采集点表 last_collection_time 字段
     *
     * @param id 电表ID
     * @param time 上次采集时间（对应 emission_collection_record.collection_time 的值）
     * @return 受影响行数
     */
    @Modifying
    @Query("UPDATE MeterInfo m SET m.lastCollectionTime = :time WHERE m.id = :id")
    int updateLastCollectionTime(@Param("id") Long id, @Param("time") LocalDateTime time);
}
