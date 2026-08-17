package com.example.carbon.emission.model.repository;

import com.example.carbon.emission.model.entity.EmissionNodeConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmissionNodeConfigRepository extends JpaRepository<EmissionNodeConfig, Long> {
    
    Optional<EmissionNodeConfig> findByNodeId(Long nodeId);
    
    void deleteByNodeId(Long nodeId);
}