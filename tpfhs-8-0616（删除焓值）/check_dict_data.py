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
    
    print("=== 检查数据字典 ===")
    cursor.execute("SELECT id, dict_code, dict_name FROM data_dict")
    dicts = cursor.fetchall()
    
    for d in dicts:
        print(f"\n字典ID: {d[0]}, 编码: {d[1]}, 名称: {d[2]}")
        cursor.execute("SELECT item_code, item_value FROM data_dict_item WHERE dict_id = %s ORDER BY sort_order", (d[0],))
        items = cursor.fetchall()
        print("字典项:")
        for item in items:
            print(f"  - {item[0]}: {item[1]}")
    
    print("\n=== 检查核算场景字典项数量 ===")
    cursor.execute("SELECT COUNT(*) FROM data_dict_item WHERE dict_id = (SELECT id FROM data_dict WHERE dict_code = 'accounting_scenario')")
    count = cursor.fetchone()[0]
    print(f"核算场景字典项数量: {count}")
    
    cursor.execute("SELECT COUNT(*) FROM data_dict_item WHERE dict_id = (SELECT id FROM data_dict WHERE dict_code = 'energy_use')")
    count = cursor.fetchone()[0]
    print(f"能耗用途字典项数量: {count}")
    
    conn.close()
except Exception as e:
    print(f"Error: {e}")
    import traceback
    traceback.print_exc()
