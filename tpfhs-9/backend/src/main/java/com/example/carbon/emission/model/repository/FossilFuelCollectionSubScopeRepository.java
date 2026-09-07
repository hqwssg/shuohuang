package com.example.carbon.emission.model.repository;

import com.example.carbon.emission.model.entity.FossilFuelCollectionSubScope;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FossilFuelCollectionSubScopeRepository extends JpaRepository<FossilFuelCollectionSubScope, Long> {
    List<FossilFuelCollectionSubScope> findByCollectionScopeIdOrderBySortOrderAsc(Long collectionScopeId);
    boolean existsBySubScopeName(String subScopeName);

    @Query("SELECT MAX(s.sortOrder) FROM FossilFuelCollectionSubScope s WHERE s.collectionScopeId = :scopeId")
    Integer findMaxSortOrderByCollectionScopeId(@Param("scopeId") Long collectionScopeId);
}
