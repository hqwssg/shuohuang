package com.example.carbon.emission.model.service.impl;

import com.example.carbon.emission.model.entity.MeterModel;
import com.example.carbon.emission.model.repository.MeterModelRepository;
import com.example.carbon.emission.model.service.MeterModelService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 电表型号服务实现类
 */
@Service
public class MeterModelServiceImpl implements MeterModelService {
    
    private static final Logger logger = LoggerFactory.getLogger(MeterModelServiceImpl.class);
    
    @Autowired
    private MeterModelRepository meterModelRepository;
    
    /**
     * 获取所有电表型号列表
     * 
     * @return 电表型号列表，按数据库默认顺序返回；无数据时返回空列表
     */
    @Override
    public List<MeterModel> findAll() {
        return meterModelRepository.findAll();
    }
    
    /**
     * 根据主键ID查询单个电表型号
     * 
     * @param id 电表型号主键ID
     * @return 匹配的电表型号对象；不存在时返回 null
     */
    @Override
    public MeterModel findById(Long id) {
        return meterModelRepository.findById(id).orElse(null);
    }
    
    /**
     * 根据型号名称模糊查询电表型号列表
     * 
     * @param modelName 型号名称关键字（支持部分匹配）
     * @return 符合条件的电表型号列表；无匹配时返回空列表
     */
    @Override
    public List<MeterModel> findByModelNameContaining(String modelName) {
        return meterModelRepository.findByModelNameContaining(modelName);
    }
    
    /**
     * 保存电表型号（新增或更新）
     * 当 meterModel 的 id 为空时执行新增，否则执行更新
     * 
     * @param meterModel 待保存的电表型号对象
     * @return 保存后的电表型号对象（含生成的主键ID）
     */
    @Override
    public MeterModel save(MeterModel meterModel) {
        logger.info("保存电表型号: {}", meterModel.getModelName());
        return meterModelRepository.save(meterModel);
    }
    
    /**
     * 根据主键ID删除电表型号
     * 
     * @param id 待删除的电表型号主键ID
     * @throws org.springframework.dao.EmptyResultDataAccessException 当ID不存在时抛出
     */
    @Override
    public void deleteById(Long id) {
        logger.info("删除电表型号, ID: {}", id);
        meterModelRepository.deleteById(id);
    }
    
    /**
     * 更新电表型号信息
     * 仅更新型号名称、所属类型、描述三个字段，并记录修改人
     * 
     * @param id         待更新的电表型号主键ID
     * @param meterModel 包含新字段值的电表型号对象（modelName、modelType、description）
     * @param userId     操作人ID，写入 updatedBy 字段
     * @return 更新后的电表型号对象
     * @throws RuntimeException 当指定ID的电表型号不存在时抛出
     */
    @Override
    public MeterModel update(Long id, MeterModel meterModel, Long userId) {
        MeterModel existing = meterModelRepository.findById(id).orElse(null);
        if (existing == null) {
            throw new RuntimeException("电表型号不存在，ID: " + id);
        }
        
        existing.setModelName(meterModel.getModelName());
        existing.setModelType(meterModel.getModelType());
        existing.setDescription(meterModel.getDescription());
        existing.setUpdatedBy(userId);
        
        logger.info("更新电表型号, ID: {}, 型号: {}", id, meterModel.getModelName());
        return meterModelRepository.save(existing);
    }
}
