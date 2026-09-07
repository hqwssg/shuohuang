package com.example.carbon.emission.model.repository;

import com.example.carbon.emission.model.entity.DataDict;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 数据字典Repository接口
 * 提供数据字典分类的数据访问操作
 */
@Repository
public interface DataDictRepository extends JpaRepository<DataDict, Long> {
    
    /**
     * 根据字典编码查询字典
     * @param dictCode 字典编码
     * @return 字典对象（Optional）
     */
    Optional<DataDict> findByDictCode(String dictCode);
    
    /**
     * 根据状态查询字典列表
     * @param status 状态：1-启用，0-禁用
     * @return 字典列表
     */
    List<DataDict> findByStatus(Integer status);
    
    /**
     * 查询所有字典，按排序号升序
     * @return 字典列表
     */
    List<DataDict> findAllByOrderBySortOrderAsc();
    
    /**
     * 检查字典编码是否存在
     * @param dictCode 字典编码
     * @return 是否存在
     */
    boolean existsByDictCode(String dictCode);
    
    /**
     * 根据字典名称模糊查询
     * @param dictName 字典名称关键字
     * @return 字典列表
     */
    List<DataDict> findByDictNameContaining(String dictName);
}