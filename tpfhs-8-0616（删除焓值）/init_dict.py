import sqlite3

try:
    conn = sqlite3.connect('test.db')
    cursor = conn.cursor()
    
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS data_dict (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            dict_code TEXT,
            dict_name TEXT,
            description TEXT
        )
    ''')
    
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS data_dict_item (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            dict_id INTEGER,
            item_code TEXT,
            item_value TEXT,
            parent_code TEXT,
            sort_order INTEGER,
            status INTEGER
        )
    ''')
    
    cursor.execute("SELECT * FROM data_dict WHERE dict_code = 'accounting_scenario'")
    if cursor.fetchone() is None:
        cursor.execute("INSERT INTO data_dict (dict_code, dict_name, description) VALUES (?, ?, ?)", 
                      ('accounting_scenario', '核算场景', '碳排放来源归类场景'))
        dict_id = cursor.lastrowid
        
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
            cursor.execute("INSERT INTO data_dict_item (dict_id, item_code, item_value, sort_order, status) VALUES (?, ?, ?, ?, 1)",
                          (dict_id, code, value, order))
    
    cursor.execute("SELECT * FROM data_dict WHERE dict_code = 'energy_use'")
    if cursor.fetchone() is None:
        cursor.execute("INSERT INTO data_dict (dict_code, dict_name, description) VALUES (?, ?, ?)",
                      ('energy_use', '能耗用途', '能源消耗用途分类'))
        dict_id = cursor.lastrowid
        
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
            cursor.execute("INSERT INTO data_dict_item (dict_id, item_code, item_value, sort_order, status) VALUES (?, ?, ?, ?, 1)",
                          (dict_id, code, value, order))
    
    conn.commit()
    print("Data dictionary initialized successfully!")
    
    cursor.execute("SELECT * FROM data_dict")
    print("\nData Dict:")
    for row in cursor.fetchall():
        print(row)
        
    cursor.execute("SELECT * FROM data_dict_item")
    print("\nData Dict Items:")
    for row in cursor.fetchall():
        print(row)
    
    conn.close()
except Exception as e:
    print(f"Error: {e}")
    import traceback
    traceback.print_exc()
