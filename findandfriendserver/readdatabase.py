import mysql.connector
import socket
import secrets

secret_key = secrets.token_hex(32)
print(secret_key)
# 获取主机名

# 连接到数据库（如果不存在会创建）
conn = mysql.connector.connect(
    host='localhost',
    user='findandfriend',
    password='QWEqwe1234',
    database='user_data'
)

# 创建一个游标对象
cursor = conn.cursor()
try:
    cursor.execute('INSERT INTO users (name, email, password) VALUES (%s, %s, %s)', ("Alice Clark", "alice@example.com", "password123"))
    cursor.execute('INSERT INTO users (name, email, password) VALUES (%s, %s, %s)', ("John Clark", "john@example.com", "password456"))
    cursor.execute('INSERT INTO users (name, email, password) VALUES (%s, %s, %s)', ("Luke Clark", "luke@example.com", "password789"))
    cursor.execute('INSERT INTO users (name, email, password) VALUES (%s, %s, %s)', ("Emily Clark", "emily@example.com", "password000"))
    conn.commit()
except mysql.connector.IntegrityError as e:
    print("error")
# 查询数据
cursor.execute('SELECT * FROM users')
rows = cursor.fetchall()

for row in rows:
    print(row)

# 关闭数据库连接
conn.close()
