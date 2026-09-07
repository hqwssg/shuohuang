package com.example.carbon.emission.model.repository;

import com.example.carbon.emission.model.entity.StationInterval;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 站点/区间数据访问接口
 */
@Repository
public interface StationIntervalRepository extends JpaRepository<StationInterval, Long> {
    
    /**
     * 根据名称查询站点/区间
     *
     * @param name 站点/区间名称
     * @return 站点/区间列表
     */
    List<StationInterval> findByName(String name);
    
    /**
     * 根据拼音首字母编码查询
     *
     * @param pinyinCode 拼音首字母编码
     * @return 站点/区间列表
     */
    List<StationInterval> findByPinyinCodeStartingWith(String pinyinCode);
    
    /**
     * 根据状态查询站点/区间
     *
     * @param status 状态：1-启用，0-停用
     * @return 站点/区间列表
     */
    List<StationInterval> findByStatus(Integer status);
    
    /**
     * 根据关键词搜索站点/区间
     * 支持按名称和拼音编码模糊搜索
     *
     * @param keyword 搜索关键词
     * @return 匹配的站点/区间列表
     */
    @Query("SELECT s FROM StationInterval s WHERE " +
           "(:keyword IS NULL OR s.name LIKE %:keyword% OR s.pinyinCode LIKE %:keyword%)")
    List<StationInterval> searchByKeyword(@Param("keyword") String keyword);
    
    /**
     * 检查名称是否已存在
     *
     * @param name 站点/区间名称
     * @return 是否存在
     */
    boolean existsByName(String name);
    
    /**
     * 查询所有站点/区间按排序顺序
     *
     * @return 站点/区间列表（按sortOrder升序）
     */
    List<StationInterval> findAllByOrderBySortOrderAsc();
    
    /**
     * 根据状态查询站点/区间按排序顺序
     *
     * @param status 状态：1-启用，0-停用
     * @return 站点/区间列表（按sortOrder升序）
     */
    List<StationInterval> findByStatusOrderBySortOrderAsc(Integer status);
}
