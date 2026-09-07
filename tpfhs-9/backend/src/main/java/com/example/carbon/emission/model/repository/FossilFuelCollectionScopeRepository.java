package com.example.carbon.emission.model.repository;

import com.example.carbon.emission.model.entity.FossilFuelCollectionScope;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FossilFuelCollectionScopeRepository extends JpaRepository<FossilFuelCollectionScope, Long> {
    List<FossilFuelCollectionScope> findAllByOrderBySortOrderAsc();
    boolean existsByScopeName(String scopeName);

    @Query("SELECT MAX(s.sortOrder) FROM FossilFuelCollectionScope s")
    Integer findMaxSortOrder();
}
