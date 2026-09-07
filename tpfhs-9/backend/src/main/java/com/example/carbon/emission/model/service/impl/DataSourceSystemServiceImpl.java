package com.example.carbon.emission.model.service.impl;

import com.example.carbon.emission.model.entity.DataSourceSystem;
import com.example.carbon.emission.model.repository.DataSourceSystemRepository;
import com.example.carbon.emission.model.service.DataSourceSystemService;
import com.example.carbon.emission.model.util.PinyinUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 数据来源系统服务实现类
 * 实现数据来源系统的业务逻辑
 */
@Service
@Transactional
public class DataSourceSystemServiceImpl implements DataSourceSystemService {
    
    @Autowired
    private DataSourceSystemRepository dataSourceSystemRepository;
    
    /**
     * 获取所有数据来源系统
     * @return 所有数据来源系统列表
     */
    @Override
    public List<DataSourceSystem> getAllDataSourceSystems() {
        return dataSourceSystemRepository.findAll();
    }
    
    /**
     * 根据ID获取数据来源系统
     * @param id 系统ID
     * @return 系统对象，不存在返回null
     */
    @Override
    public DataSourceSystem getDataSourceSystemById(Long id) {
        return dataSourceSystemRepository.findById(id).orElse(null);
    }
    
    /**
     * 根据关键词模糊搜索数据来源系统
     * @param keyword 搜索关键词
     * @return 匹配的系统列表
     */
    @Override
    public List<DataSourceSystem> searchByKeyword(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return dataSourceSystemRepository.findAll();
        }
        return dataSourceSystemRepository.searchByKeyword(keyword.trim());
    }
    
    /**
     * 根据拼音首字母搜索数据来源系统
     * @param pinyinCode 拼音首字母
     * @return 匹配的系统列表
     */
    @Override
    public List<DataSourceSystem> searchByPinyinCode(String pinyinCode) {
        if (pinyinCode == null || pinyinCode.trim().isEmpty()) {
            return dataSourceSystemRepository.findAll();
        }
        return dataSourceSystemRepository.findByPinyinCodeStartingWith(pinyinCode.trim().toUpperCase());
    }
    
    /**
     * 创建数据来源系统
     * 自动生成拼音编码
     * @param dataSourceSystem 系统信息
     * @param userId 创建人ID
     * @return 创建后的系统对象
     */
    @Override
    public DataSourceSystem createDataSourceSystem(DataSourceSystem dataSourceSystem, Long userId) {
        if (dataSourceSystem.getSystemName() == null || dataSourceSystem.getSystemName().trim().isEmpty()) {
            throw new RuntimeException("数据来源系统名称不能为空");
        }
        
        dataSourceSystem.setSystemName(dataSourceSystem.getSystemName().trim());
        dataSourceSystem.setPinyinCode(PinyinUtils.generatePinyinCode(dataSourceSystem.getSystemName()));
        
        if (userId != null) {
            dataSourceSystem.setCreatedBy(userId);
            dataSourceSystem.setUpdatedBy(userId);
        }
        
        return dataSourceSystemRepository.save(dataSourceSystem);
    }
    
    /**
     * 更新数据来源系统
     * 自动重新生成拼音编码
     * @param id 系统ID
     * @param dataSourceSystem 更新的系统信息
     * @param userId 更新人ID
     * @return 更新后的系统对象
     */
    @Override
    public DataSourceSystem updateDataSourceSystem(Long id, DataSourceSystem dataSourceSystem, Long userId) {
        Optional<DataSourceSystem> existingOpt = dataSourceSystemRepository.findById(id);
        if (existingOpt.isEmpty()) {
            throw new RuntimeException("数据来源系统不存在: " + id);
        }
        
        DataSourceSystem existing = existingOpt.get();
        
        if (dataSourceSystem.getSystemName() != null && !dataSourceSystem.getSystemName().trim().isEmpty()) {
            existing.setSystemName(dataSourceSystem.getSystemName().trim());
            existing.setPinyinCode(PinyinUtils.generatePinyinCode(dataSourceSystem.getSystemName()));
        }
        
        if (dataSourceSystem.getDescription() != null) {
            existing.setDescription(dataSourceSystem.getDescription());
        }
        
        if (userId != null) {
            existing.setUpdatedBy(userId);
        }
        
        return dataSourceSystemRepository.save(existing);
    }
    
    /**
     * 删除数据来源系统
     * @param id 系统ID
     */
    @Override
    public void deleteDataSourceSystem(Long id) {
        if (!dataSourceSystemRepository.existsById(id)) {
            throw new RuntimeException("数据来源系统不存在: " + id);
        }
        dataSourceSystemRepository.deleteById(id);
    }
    
    /**
     * 检查系统名称是否已存在
     * @param systemName 系统名称
     * @return 已存在返回true
     */
    @Override
    public boolean isSystemNameExists(String systemName) {
        if (systemName == null || systemName.trim().isEmpty()) {
            return false;
        }
        return dataSourceSystemRepository.existsBySystemName(systemName.trim());
    }
}
