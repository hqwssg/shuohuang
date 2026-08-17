package com.example.carbon.emission.model.repository;

import com.example.carbon.emission.model.entity.DataDict;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DataDictRepository extends JpaRepository<DataDict, Long> {
    
    Optional<DataDict> findByDictCode(String dictCode);
    
    List<DataDict> findByStatus(Integer status);
}