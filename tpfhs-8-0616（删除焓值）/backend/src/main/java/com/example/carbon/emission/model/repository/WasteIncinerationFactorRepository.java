package com.example.carbon.emission.model.repository;

import com.example.carbon.emission.model.entity.WasteIncinerationFactor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WasteIncinerationFactorRepository extends JpaRepository<WasteIncinerationFactor, Long> {

    Optional<WasteIncinerationFactor> findByEmissionFactorName(String emissionFactorName);

    boolean existsByEmissionFactorName(String emissionFactorName);

    boolean existsByEmissionFactorNameAndIdNot(String emissionFactorName, Long id);
}
