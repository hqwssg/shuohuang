package com.example.carbon.emission.model.service;

import java.util.Map;

public interface DataDictService {
    
    Map<String, Object> getAllOptions();
    
    Map<String, String> getDictItems(String dictCode);
    
    Map<String, String> getDictItemsByParent(String dictCode, String parentCode);
}