package com.example.carbon.emission.model.service.impl;

import com.example.carbon.emission.model.entity.DataDict;
import com.example.carbon.emission.model.entity.DataDictItem;
import com.example.carbon.emission.model.repository.DataDictItemRepository;
import com.example.carbon.emission.model.repository.DataDictRepository;
import com.example.carbon.emission.model.service.DataDictService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 数据字典服务实现类
 * 
 * 提供数据字典的查询和管理功能，支持获取各类字典项用于前端下拉框展示。
 */
@Service
public class DataDictServiceImpl implements DataDictService {
    
    @Autowired
    private DataDictRepository dictRepository;
    
    @Autowired
    private DataDictItemRepository itemRepository;
    
    /**
     * 获取所有数据字典选项
     * 
     * 返回系统中所有预定义的数据字典分类及其选项列表，用于前端下拉框数据源。
     * 
     * @return Map<String, Object> 包含所有字典分类的选项映射
     *         - locomotiveTypes: 机车类型列表
     *         - statisticalCalibers: 统计口径列表
     *         - emissionCategories: 排放类别列表
     *         - emissionSubcategories: 排放子类别映射（按类别分组）
     *         - dataSources: 数据来源列表
     *         - nodeCategories: 节点类型列表
     *         - accountingScenarios: 核算场景列表（新增）
     *         - energyUses: 能耗用途列表（新增）
     */
    @Override
    public Map<String, Object> getAllOptions() {
        Map<String, Object> options = new HashMap<>();
        
        options.put("locomotiveTypes", getDictItems("locomotive_type").values());
        options.put("statisticalCalibers", getDictItems("statistical_caliber").values());
        options.put("emissionCategories", getDictItems("emission_category").values());
        
        // 构建排放子类别树形结构
        Map<String, Object> subcategories = new HashMap<>();
        DataDict categoryDict = dictRepository.findByDictCode("emission_category").orElse(null);
        if (categoryDict != null) {
            List<DataDictItem> categories = itemRepository.findByDictIdAndStatusOrderBySortOrder(categoryDict.getId(), 1);
            for (DataDictItem category : categories) {
                subcategories.put(category.getItemValue(), 
                    getDictItemsByParent("emission_subcategory", category.getItemCode()).values());
            }
        }
        options.put("emissionSubcategories", subcategories);
        
        options.put("dataSources", getDictItems("data_source").values());
        options.put("nodeCategories", getDictItems("node_category"));
        
        // 获取核算场景和能耗用途字典（新增字段）
        Map<String, String> accountingScenarios = getDictItems("accounting_scenario");
        Map<String, String> energyUses = getDictItems("energy_use");
        options.put("accountingScenarios", accountingScenarios.values());
        options.put("energyUses", energyUses.values());
        
        return options;
    }
    
    /**
     * 根据字典编码获取字典项列表
     * 
     * @param dictCode 字典编码（如：accounting_scenario, energy_use）
     * @return Map<String, String> 字典项编码与值的映射
     */
    @Override
    public Map<String, String> getDictItems(String dictCode) {
        DataDict dict = dictRepository.findByDictCode(dictCode).orElse(null);
        if (dict == null) {
            return new HashMap<>();
        }
        List<DataDictItem> items = itemRepository.findByDictIdAndStatusOrderBySortOrder(dict.getId(), 1);
        return items.stream()
            .collect(Collectors.toMap(DataDictItem::getItemCode, DataDictItem::getItemValue, (existing, replacement) -> existing));
    }
    
    /**
     * 根据字典编码和父级编码获取子字典项列表
     * 
     * 用于获取有层级关系的字典项，如排放子类别。
     * 
     * @param dictCode 字典编码
     * @param parentCode 父级编码
     * @return Map<String, String> 子字典项编码与值的映射
     */
    @Override
    public Map<String, String> getDictItemsByParent(String dictCode, String parentCode) {
        DataDict dict = dictRepository.findByDictCode(dictCode).orElse(null);
        if (dict == null) {
            return new HashMap<>();
        }
        List<DataDictItem> items = itemRepository.findByDictIdAndParentCodeAndStatusOrderBySortOrder(
            dict.getId(), parentCode, 1);
        return items.stream()
            .collect(Collectors.toMap(DataDictItem::getItemCode, DataDictItem::getItemValue, (existing, replacement) -> existing));
    }
}