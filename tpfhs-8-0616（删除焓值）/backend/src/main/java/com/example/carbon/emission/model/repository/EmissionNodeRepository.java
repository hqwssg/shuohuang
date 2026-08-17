package com.example.carbon.emission.model.repository;

import com.example.carbon.emission.model.entity.EmissionNode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmissionNodeRepository extends JpaRepository<EmissionNode, Long> {
    
    List<EmissionNode> findByParentId(Long parentId);
    
    List<EmissionNode> findByParentIdOrderBySortOrder(Long parentId);
    
    Optional<EmissionNode> findByNameAndParentId(String name, Long parentId);
    
    Integer countByParentId(Long parentId);
    
    List<EmissionNode> findByTemplateId(Long templateId);
    
    List<EmissionNode> findByTemplateIdAndParentId(Long templateId, Long parentId);
    
    Integer countByTemplateIdAndParentId(Long templateId, Long parentId);
    
    void deleteByTemplateId(Long templateId);
    
    List<EmissionNode> findByTypeId(Integer typeId);
    
    /**
     * 获取当前数据库中最大的节点ID
     * 
     * @return 最大的节点ID，如果数据库为空则返回null
     */
    @Query("SELECT MAX(n.id) FROM EmissionNode n")
    Long findMaxId();
}