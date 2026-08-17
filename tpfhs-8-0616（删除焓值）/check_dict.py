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
    
    cursor.execute("SELECT * FROM data_dict WHERE dict_code IN ('accounting_scenario', 'energy_use')")
    dicts = cursor.fetchall()
    
    print("数据字典:")
    for d in dicts:
        print(f"ID: {d[0]}, Code: {d[1]}, Name: {d[2]}")
        cursor.execute("SELECT item_code, item_value FROM data_dict_item WHERE dict_id = %s ORDER BY sort_order", (d[0],))
        items = cursor.fetchall()
        for item in items:
            print(f"  - {item[0]}: {item[1]}")
    
    conn.close()
except Exception as e:
    print(f"Error: {e}")
