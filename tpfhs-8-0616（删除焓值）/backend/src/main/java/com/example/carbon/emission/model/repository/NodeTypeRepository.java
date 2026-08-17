package com.example.carbon.emission.model.repository;

import com.example.carbon.emission.model.entity.NodeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NodeTypeRepository extends JpaRepository<NodeType, Integer> {
    
    Optional<NodeType> findByTypeName(String typeName);
}