import pymysql

try:
    conn = pymysql.connect(
        host='localhost',
        user='root',
        password='123456',
        database='carbon_emission',
        charset='utf8mb4'
    )
    
    cursor = conn.cursor()
    
    print("=== 添加新字段到 emission_node_config 表 ===")
    
    try:
        cursor.execute("ALTER TABLE emission_node_config ADD COLUMN accounting_scenario VARCHAR(100) DEFAULT NULL COMMENT '核算场景'")
        print("添加 accounting_scenario 字段成功")
    except Exception as e:
        print(f"accounting_scenario 字段可能已存在: {e}")
    
    try:
        cursor.execute("ALTER TABLE emission_node_config ADD COLUMN energy_use VARCHAR(100) DEFAULT NULL COMMENT '能耗用途'")
        print("添加 energy_use 字段成功")
    except Exception as e:
        print(f"energy_use 字段可能已存在: {e}")
    
    try:
        cursor.execute("ALTER TABLE emission_node_config ADD COLUMN is_cumulative VARCHAR(10) DEFAULT 'true' COMMENT '是否累计量'")
        print("添加 is_cumulative 字段成功")
    except Exception as e:
        print(f"is_cumulative 字段可能已存在: {e}")
    
    try:
        cursor.execute("ALTER TABLE emission_node_config ADD COLUMN is_mobile_source VARCHAR(10) DEFAULT 'false' COMMENT '是否移动源'")
        print("添加 is_mobile_source 字段成功")
    except Exception as e:
        print(f"is_mobile_source 字段可能已存在: {e}")
    
    print("\n=== 插入核算场景数据字典 ===")
    
    cursor.execute("INSERT INTO data_dict (dict_code, dict_name, description) VALUES ('accounting_scenario', '核算场景', '碳排放来源归类场景') ON DUPLICATE KEY UPDATE dict_name = VALUES(dict_name), description = VALUES(description)")
    print("插入/更新核算场景字典成功")
    
    cursor.execute("SELECT id FROM data_dict WHERE dict_code = 'accounting_scenario'")
    dict_id = cursor.fetchone()[0]
    
    scenarios = [
        ('TRACTION_POWER', '牵引变电所/牵引供电', 1),
        ('TRAIN_OPERATION', '列车运行', 2),
        ('DISPATCH_COMM', '行车调度/通信指挥', 3),
        ('VEHICLE_REPAIR', '车辆维修/机修车间', 4),
        ('LINE_MAINTENANCE', '线路维护保养', 5),
        ('STATION_SYSTEM', '场站系统', 6),
        ('GARAGE', '车库', 7),
        ('WORKSHOP_BATHROOM', '车间浴室', 8),
        ('OFFICE_BUILDING', '办公楼', 9),
        ('STAFF_CANTEEN', '职工食堂', 10),
        ('STAFF_DORMITORY', '职工宿舍', 11),
        ('INTERNAL_VEHICLE', '内部运营车辆', 12),
        ('INTERNAL_VEHICLE_SINGLE', '内部运营车辆（单台车）', 13),
        ('WASTE_DISPOSAL', '废弃物处理场所', 14),
        ('OTHER_SCENARIO', '其他场景', 15)
    ]
    
    for code, value, order in scenarios:
        cursor.execute("INSERT INTO data_dict_item (dict_id, item_code, item_value, sort_order, status) VALUES (%s, %s, %s, %s, 1) ON DUPLICATE KEY UPDATE item_value = VALUES(item_value), sort_order = VALUES(sort_order)",
                      (dict_id, code, value, order))
    print("插入/更新核算场景字典项成功")
    
    print("\n=== 插入能耗用途数据字典 ===")
    
    cursor.execute("INSERT INTO data_dict (dict_code, dict_name, description) VALUES ('energy_use', '能耗用途', '能源消耗用途分类') ON DUPLICATE KEY UPDATE dict_name = VALUES(dict_name), description = VALUES(description)")
    print("插入/更新能耗用途字典成功")
    
    cursor.execute("SELECT id FROM data_dict WHERE dict_code = 'energy_use'")
    dict_id = cursor.fetchone()[0]
    
    uses = [
        ('LIGHTING', '照明', 1),
        ('AIR_CONDITIONING', '空调', 2),
        ('HEATING', '取暖', 3),
        ('HEAT_SUPPLY', '供热', 4),
        ('ELEVATOR', '电梯', 5),
        ('WATER_PUMP', '水泵', 6),
        ('VEHICLE', '汽车', 7),
        ('EQUIPMENT', '设备用能', 8),
        ('TRAIN_OPERATION_ENERGY', '列车运行用能', 9),
        ('MIXED_METERING', '混合用能计量', 10),
        ('OTHER_USE', '其他', 11)
    ]
    
    for code, value, order in uses:
        cursor.execute("INSERT INTO data_dict_item (dict_id, item_code, item_value, sort_order, status) VALUES (%s, %s, %s, %s, 1) ON DUPLICATE KEY UPDATE item_value = VALUES(item_value), sort_order = VALUES(sort_order)",
                      (dict_id, code, value, order))
    print("插入/更新能耗用途字典项成功")
    
    conn.commit()
    print("\n=== 数据库更新完成 ===")
    
    conn.close()
except Exception as e:
    print(f"Error: {e}")
    import traceback
    traceback.print_exc()
