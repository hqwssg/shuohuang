package com.example.carbon.emission.model.config;

import com.example.carbon.emission.model.entity.DataDict;
import com.example.carbon.emission.model.entity.DataDictItem;
import com.example.carbon.emission.model.repository.DataDictItemRepository;
import com.example.carbon.emission.model.repository.DataDictRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 数据字典初始化器
 * 
 * 在应用启动时自动初始化"核算场景"和"能耗用途"两个新增的数据字典分类及其字典项。
 * 采用幂等性设计，若字典已存在则跳过初始化。
 */
@Component
public class DataDictInitializer {

    private static final Logger logger = LoggerFactory.getLogger(DataDictInitializer.class);

    @Autowired
    private DataDictRepository dictRepository;

    @Autowired
    private DataDictItemRepository itemRepository;

    /**
     * 应用启动时执行的初始化方法
     * 
     * 通过 @PostConstruct 注解标记，在Spring容器初始化完成后自动执行。
     * 依次初始化核算场景和能耗用途两个字典分类。
     */
    @PostConstruct
    public void init() {
        logger.info("Initializing data dictionary...");
        initAccountingScenarios();
        initEnergyUses();
        logger.info("Data dictionary initialization completed.");
    }

    /**
     * 初始化核算场景字典
     * 
     * 若"accounting_scenario"字典不存在，则创建该字典并添加15个预设的核算场景选项：
     * 牵引变电所/牵引供电、列车运行、行车调度/通信指挥、车辆维修/机修车间、线路维护保养、
     * 场站系统、车库、车间浴室、办公楼、职工食堂、职工宿舍、内部运营车辆、
     * 内部运营车辆（单台车）、废弃物处理场所、其他场景
     */
    private void initAccountingScenarios() {
        if (dictRepository.findByDictCode("accounting_scenario").isEmpty()) {
            DataDict dict = new DataDict();
            dict.setDictCode("accounting_scenario");
            dict.setDictName("核算场景");
            dict.setDescription("碳排放来源归类场景");
            DataDict savedDict = dictRepository.save(dict);

            createDictItem(savedDict.getId(), "TRACTION_POWER", "牵引变电所/牵引供电", 1);
            createDictItem(savedDict.getId(), "TRAIN_OPERATION", "列车运行", 2);
            createDictItem(savedDict.getId(), "DISPATCH_COMM", "行车调度/通信指挥", 3);
            createDictItem(savedDict.getId(), "VEHICLE_REPAIR", "车辆维修/机修车间", 4);
            createDictItem(savedDict.getId(), "LINE_MAINTENANCE", "线路维护保养", 5);
            createDictItem(savedDict.getId(), "STATION_SYSTEM", "场站系统", 6);
            createDictItem(savedDict.getId(), "GARAGE", "车库", 7);
            createDictItem(savedDict.getId(), "WORKSHOP_BATHROOM", "车间浴室", 8);
            createDictItem(savedDict.getId(), "OFFICE_BUILDING", "办公楼", 9);
            createDictItem(savedDict.getId(), "STAFF_CANTEEN", "职工食堂", 10);
            createDictItem(savedDict.getId(), "STAFF_DORMITORY", "职工宿舍", 11);
            createDictItem(savedDict.getId(), "INTERNAL_VEHICLE", "内部运营车辆", 12);
            createDictItem(savedDict.getId(), "INTERNAL_VEHICLE_SINGLE", "内部运营车辆（单台车）", 13);
            createDictItem(savedDict.getId(), "WASTE_DISPOSAL", "废弃物处理场所", 14);
            createDictItem(savedDict.getId(), "OTHER_SCENARIO", "其他场景", 15);
        }
    }

    /**
     * 初始化能耗用途字典
     * 
     * 若"energy_use"字典不存在，则创建该字典并添加11个预设的能耗用途选项：
     * 照明、空调、取暖、供热、电梯、水泵、汽车、设备用能、列车运行用能、混合用能计量、其他
     */
    private void initEnergyUses() {
        if (dictRepository.findByDictCode("energy_use").isEmpty()) {
            DataDict dict = new DataDict();
            dict.setDictCode("energy_use");
            dict.setDictName("能耗用途");
            dict.setDescription("能源消耗用途分类");
            DataDict savedDict = dictRepository.save(dict);

            createDictItem(savedDict.getId(), "LIGHTING", "照明", 1);
            createDictItem(savedDict.getId(), "AIR_CONDITIONING", "空调", 2);
            createDictItem(savedDict.getId(), "HEATING", "取暖", 3);
            createDictItem(savedDict.getId(), "HEAT_SUPPLY", "供热", 4);
            createDictItem(savedDict.getId(), "ELEVATOR", "电梯", 5);
            createDictItem(savedDict.getId(), "WATER_PUMP", "水泵", 6);
            createDictItem(savedDict.getId(), "VEHICLE", "汽车", 7);
            createDictItem(savedDict.getId(), "EQUIPMENT", "设备用能", 8);
            createDictItem(savedDict.getId(), "TRAIN_OPERATION_ENERGY", "列车运行用能", 9);
            createDictItem(savedDict.getId(), "MIXED_METERING", "混合用能计量", 10);
            createDictItem(savedDict.getId(), "OTHER_USE", "其他", 11);
        }
    }

    /**
     * 创建单个字典项
     * 
     * @param dictId 所属字典ID
     * @param itemCode 字典项编码
     * @param itemValue 字典项显示值
     * @param sortOrder 排序序号
     */
    private void createDictItem(Long dictId, String itemCode, String itemValue, int sortOrder) {
        DataDictItem item = new DataDictItem();
        item.setDictId(dictId);
        item.setItemCode(itemCode);
        item.setItemValue(itemValue);
        item.setSortOrder(sortOrder);
        item.setStatus(1);  // 1表示启用状态
        itemRepository.save(item);
    }
}