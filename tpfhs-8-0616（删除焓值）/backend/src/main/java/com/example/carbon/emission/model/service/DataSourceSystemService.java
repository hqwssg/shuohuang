package com.example.carbon.emission.model.service;

import com.example.carbon.emission.model.entity.DataSourceSystem;

import java.util.List;

/**
 * 数据来源系统服务接口
 * 定义数据来源系统的增删改查和搜索功能
 */
public interface DataSourceSystemService {
    
    /**
     * 获取所有数据来源系统
     * @return 数据来源系统列表
     */
    List<DataSourceSystem> getAllDataSourceSystems();
    
    /**
     * 根据ID获取数据来源系统
     * @param id 系统ID
     * @return 数据来源系统对象，不存在返回null
     */
    DataSourceSystem getDataSourceSystemById(Long id);
    
    /**
     * 根据关键词搜索数据来源系统
     * 对系统名称进行模糊搜索
     * @param keyword 搜索关键词
     * @return 匹配的系统列表
     */
    List<DataSourceSystem> searchByKeyword(String keyword);
    
    /**
     * 根据拼音首字母搜索数据来源系统
     * @param pinyinCode 拼音首字母
     * @return 匹配的系统列表
     */
    List<DataSourceSystem> searchByPinyinCode(String pinyinCode);
    
    /**
     * 创建新的数据来源系统
     * 自动生成拼音编码
     * @param dataSourceSystem 系统信息
     * @param userId 创建人ID
     * @return 创建后的系统对象
     */
    DataSourceSystem createDataSourceSystem(DataSourceSystem dataSourceSystem, Long userId);
    
    /**
     * 更新数据来源系统
     * 更新时重新生成拼音编码
     * @param id 系统ID
     * @param dataSourceSystem 要更新的系统信息
     * @param userId 更新人ID
     * @return 更新后的系统对象
     */
    DataSourceSystem updateDataSourceSystem(Long id, DataSourceSystem dataSourceSystem, Long userId);
    
    /**
     * 删除数据来源系统
     * @param id 系统ID
     */
    void deleteDataSourceSystem(Long id);
    
    /**
     * 检查系统名称是否已存在
     * @param systemName 系统名称
     * @return 已存在返回true，否则返回false
     */
    boolean isSystemNameExists(String systemName);
}
