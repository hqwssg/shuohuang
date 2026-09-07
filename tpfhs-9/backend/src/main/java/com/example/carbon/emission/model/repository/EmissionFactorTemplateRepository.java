package com.example.carbon.emission.model.repository;

import com.example.carbon.emission.model.entity.EmissionFactorTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmissionFactorTemplateRepository extends JpaRepository<EmissionFactorTemplate, Long> {

    List<EmissionFactorTemplate> findAllByOrderByCreatedAtDesc();

    List<EmissionFactorTemplate> findByStatusOrderByCreatedAtDesc(Integer status);

    boolean existsByTemplateName(String templateName);
}
