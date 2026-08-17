import mysql.connector

try:
    cnx = mysql.connector.connect(
        user='root',
        password='123456',
        host='localhost',
        database='carbon_emission'
    )
    cursor = cnx.cursor()
    
    # 先检查用户是否已存在
    cursor.execute("SELECT * FROM user WHERE username = 'admin'")
    if cursor.fetchone():
        print('用户 admin 已存在')
    else:
        cursor.execute("INSERT INTO user (username, password, name) VALUES ('admin', 'admin123', '管理员')")
        cnx.commit()
        print('用户添加成功')
    
    cursor.close()
    cnx.close()
except Exception as e:
    print(f'错误: {e}')