package com.example.carbon.emission.model.repository;

import com.example.carbon.emission.model.entity.DataSourceSystem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DataSourceSystemRepository extends JpaRepository<DataSourceSystem, Long> {
    
    List<DataSourceSystem> findBySystemNameContaining(String systemName);
    
    List<DataSourceSystem> findByPinyinCodeStartingWith(String pinyinCode);
    
    @Query("SELECT d FROM DataSourceSystem d WHERE " +
           "(:keyword IS NULL OR d.systemName LIKE %:keyword% OR d.pinyinCode LIKE %:keyword%)")
    List<DataSourceSystem> searchByKeyword(@Param("keyword") String keyword);
    
    boolean existsBySystemName(String systemName);
}
