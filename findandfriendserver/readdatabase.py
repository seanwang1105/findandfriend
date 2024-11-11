import mysql.connector
import socket
import secrets

secret_key = secrets.token_hex(32)
print(secret_key)
# get host name

from_email = 'john@example.com'
to_email = 'mike@example.com'
# connect to database
conn = mysql.connector.connect(
    host='localhost',
    user='findandfriend',
    password='QWEqwe1234',
    database='user_data'
)

# create a user objective
cursor = conn.cursor()
cursor.execute('SELECT id FROM users WHERE email = %s', (from_email,))
from_user = cursor.fetchone()
cursor.execute('SELECT id FROM users WHERE email = %s', (to_email,))
to_user = cursor.fetchone()
from_user_id = from_user[0]
to_user_id = to_user[0]
print(from_user_id, to_user_id,from_user,to_user)
try:

    #cursor.execute('INSERT INTO users (name, email, password) VALUES (%s, %s, %s)', ("Alice Clark", "alice@example.com", "password123"))
    #cursor.execute('INSERT INTO users (name, email, password) VALUES (%s, %s, %s)', ("John Clark", "john@example.com", "password456"))
    #cursor.execute('INSERT INTO users (name, email, password) VALUES (%s, %s, %s)', ("Luke Clark", "luke@example.com", "password789"))
    #cursor.execute('INSERT INTO users (name, email, password) VALUES (%s, %s, %s)', ("Emily Clark", "emily@example.com", "password000"))
    cursor.execute('INSERT INTO friend_requests (from_user_id, to_user_id, status) VALUES (%s, %s, %s)', (from_user_id, to_user_id, 'Pending'))
    conn.commit()
except mysql.connector.IntegrityError as e:
    print("error")
# query data
cursor.execute('SELECT * FROM users')
rows = cursor.fetchall()
for row in rows:
    print(row)

cursor.execute('SELECT * FROM friend_requests')
rows = cursor.fetchall()

for row in rows:
    print(row)

# close database connection
conn.close()
