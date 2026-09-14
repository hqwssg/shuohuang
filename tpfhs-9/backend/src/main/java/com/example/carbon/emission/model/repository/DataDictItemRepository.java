package com.example.carbon.emission.model.repository;

import com.example.carbon.emission.model.entity.DataDictItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 数据字典项Repository接口
 * 提供数据字典项的数据访问操作
 */
@Repository
public interface DataDictItemRepository extends JpaRepository<DataDictItem, Long> {
    
    /**
     * 根据字典ID和状态查询字典项（按排序号升序）
     * @param dictId 字典ID
     * @param status 状态：1-启用，0-禁用
     * @return 字典项列表
     */
    List<DataDictItem> findByDictIdAndStatusOrderBySortOrder(Long dictId, Integer status);
    
    /**
     * 根据字典ID、父级编码和状态查询字典项（按排序号升序）
     * @param dictId 字典ID
     * @param parentCode 父级编码
     * @param status 状态
     * @return 字典项列表
     */
    List<DataDictItem> findByDictIdAndParentCodeAndStatusOrderBySortOrder(Long dictId, String parentCode, Integer status);
    
    /**
     * 根据字典ID查询所有字典项（按排序号升序）
     * @param dictId 字典ID
     * @return 字典项列表
     */
    List<DataDictItem> findByDictIdOrderBySortOrder(Long dictId);
    
    /**
     * 根据字典ID和字典项编码查询
     * @param dictId 字典ID
     * @param itemCode 字典项编码
     * @return 字典项对象（Optional）
     */
    Optional<DataDictItem> findByDictIdAndItemCode(Long dictId, String itemCode);

    /**
     * 在指定字典范围内按展示值查询，避免不同字典存在同名项时误匹配。
     */
    Optional<DataDictItem> findByDictIdAndItemValue(Long dictId, String itemValue);
    
    /**
     * 检查字典项编码是否存在
     * @param dictId 字典ID
     * @param itemCode 字典项编码
     * @return 是否存在
     */
    boolean existsByDictIdAndItemCode(Long dictId, String itemCode);
    
    /**
     * 根据父级编码查询字典项
     * @param parentCode 父级编码
     * @return 字典项列表
     */
    List<DataDictItem> findByParentCode(String parentCode);
    
    /**
     * 根据字典ID删除所有字典项
     * @param dictId 字典ID
     */
    void deleteByDictId(Long dictId);

    /**
     * 根据字典项编码查询（用于通过 subcategory 反查 parent_code 大类）
     * @param itemCode 字典项编码
     * @return 字典项对象（Optional）
     */
    Optional<DataDictItem> findByItemCode(String itemCode);
}
