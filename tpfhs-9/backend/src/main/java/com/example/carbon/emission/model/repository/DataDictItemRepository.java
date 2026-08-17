package com.example.carbon.emission.model.repository;

import com.example.carbon.emission.model.entity.DataDictItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DataDictItemRepository extends JpaRepository<DataDictItem, Long> {
    
    List<DataDictItem> findByDictIdAndStatusOrderBySortOrder(Long dictId, Integer status);
    
    List<DataDictItem> findByDictIdAndParentCodeAndStatusOrderBySortOrder(Long dictId, String parentCode, Integer status);
    
    List<DataDictItem> findByDictIdOrderBySortOrder(Long dictId);
}