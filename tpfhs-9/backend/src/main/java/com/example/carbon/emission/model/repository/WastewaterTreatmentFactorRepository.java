package com.example.carbon.emission.model.repository;

import com.example.carbon.emission.model.entity.WastewaterTreatmentFactor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WastewaterTreatmentFactorRepository extends JpaRepository<WastewaterTreatmentFactor, Long> {

    Optional<WastewaterTreatmentFactor> findByEmissionFactorName(String emissionFactorName);

    boolean existsByEmissionFactorName(String emissionFactorName);

    boolean existsByEmissionFactorNameAndIdNot(String emissionFactorName, Long id);
}
