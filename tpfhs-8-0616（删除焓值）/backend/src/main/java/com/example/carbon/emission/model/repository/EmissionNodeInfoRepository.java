package com.example.carbon.emission.model.repository;

import com.example.carbon.emission.model.entity.EmissionNodeInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmissionNodeInfoRepository extends JpaRepository<EmissionNodeInfo, Long> {
    
    Optional<EmissionNodeInfo> findByNodeId(Long nodeId);
    
    void deleteByNodeId(Long nodeId);
}