package com.example.carbon.emission.model.repository;

import com.example.carbon.emission.model.entity.ElectricityCarbonEmissionFactor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ElectricityCarbonEmissionFactorRepository extends JpaRepository<ElectricityCarbonEmissionFactor, Long> {
    
    Optional<ElectricityCarbonEmissionFactor> findByFactorName(String factorName);
    
    List<ElectricityCarbonEmissionFactor> findByFactorNameContaining(String factorName);
    
    boolean existsByFactorName(String factorName);
    
    boolean existsByFactorNameAndIdNot(String factorName, Long id);
}