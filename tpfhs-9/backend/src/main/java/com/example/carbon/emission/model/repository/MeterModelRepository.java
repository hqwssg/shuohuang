package com.example.carbon.emission.model.repository;

import com.example.carbon.emission.model.entity.MeterModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 电表型号数据访问接口
 */
@Repository
public interface MeterModelRepository extends JpaRepository<MeterModel, Long> {
    
    /**
     * 根据电表型号模糊查询
     */
    List<MeterModel> findByModelNameContaining(String modelName);
    
    /**
     * 根据所属类型查询
     */
    List<MeterModel> findByModelType(String modelType);
    
    /**
     * 根据电表型号精确查询
     */
    MeterModel findByModelName(String modelName);
}
