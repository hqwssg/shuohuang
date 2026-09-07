package com.example.carbon.emission.model.repository;

import com.example.carbon.emission.model.entity.Point;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 集中器数据访问接口
 */
@Repository
public interface PointRepository extends JpaRepository<Point, Long> {
    
    /**
     * 根据名称查询集中器
     *
     * @param name 集中器名称
     * @return 集中器列表
     */
    List<Point> findByName(String name);
    
    /**
     * 根据拼音首字母编码查询
     *
     * @param pinyinCode 拼音首字母编码
     * @return 集中器列表
     */
    List<Point> findByPinyinCodeStartingWith(String pinyinCode);
    
    /**
     * 根据所属站点/区间ID查询集中器
     *
     * @param stationIntervalId 站点/区间ID
     * @return 集中器列表
     */
    List<Point> findByStationIntervalId(Long stationIntervalId);
    
    /**
     * 根据所属站点/区间ID查询集中器按排序顺序
     *
     * @param stationIntervalId 站点/区间ID
     * @return 集中器列表（按sortOrder升序）
     */
    List<Point> findByStationIntervalIdOrderBySortOrderAsc(Long stationIntervalId);
    
    /**
     * 根据状态查询集中器
     *
     * @param status 状态：1-启用，0-停用
     * @return 集中器列表
     */
    List<Point> findByStatus(Integer status);
    
    /**
     * 根据关键词搜索集中器
     * 支持按名称和拼音编码模糊搜索
     *
     * @param keyword 搜索关键词
     * @return 匹配的集中器列表
     */
    @Query("SELECT p FROM Point p WHERE " +
           "(:keyword IS NULL OR p.name LIKE %:keyword% OR p.pinyinCode LIKE %:keyword%)")
    List<Point> searchByKeyword(@Param("keyword") String keyword);
    
    /**
     * 根据关键词和站点/区间ID搜索集中器
     *
     * @param keyword 搜索关键词
     * @param stationIntervalId 站点/区间ID
     * @return 匹配的集中器列表
     */
    @Query("SELECT p FROM Point p WHERE p.stationIntervalId = :stationIntervalId AND " +
           "(:keyword IS NULL OR p.name LIKE %:keyword% OR p.pinyinCode LIKE %:keyword%)")
    List<Point> searchByKeywordAndStationIntervalId(@Param("keyword") String keyword, 
                                                     @Param("stationIntervalId") Long stationIntervalId);
    
    /**
     * 检查名称是否已存在
     *
     * @param name 集中器名称
     * @return 是否存在
     */
    boolean existsByName(String name);
    
    /**
     * 根据站点/区间ID列表批量查询集中器
     *
     * @param stationIntervalIds 站点/区间ID列表
     * @return 集中器列表
     */
    List<Point> findByStationIntervalIdIn(List<Long> stationIntervalIds);
}
