import mysql.connector
import socket
import secrets

secret_key = secrets.token_hex(32)
print(secret_key)
# get host name

from_email = 'alice@example.com'
to_email = 'mike@example.com'

# Connect to the database
conn = mysql.connector.connect(
    host='localhost',
    user='findandfriend',
    password='QWEqwe1234',
    database='user_data'
)

# Create a cursor object
cursor = conn.cursor()

# Modify the ALTER TABLE command to set default values
alter_table_sql = '''
ALTER TABLE users
ADD COLUMN last_visit_place_reviews VARCHAR(255),
ADD COLUMN last_longitude FLOAT,
ADD COLUMN last_latitude FLOAT
'''

try:
    cursor.execute(alter_table_sql)
    conn.commit()
    print("Columns last_longitude and last_latitude added successfully.")
except mysql.connector.Error as err:
    print(f"Error: {err}")

# Update existing records to have default values

update_sql = '''
UPDATE users
SET last_visit_place = '',
    last_visit_rating = 0.0,
    last_visit_place_reviews = '',
    last_longitude=0.0,
    last_latitude=0.0
'''
try:
    cursor.execute(update_sql)
    conn.commit()
    print("Existing records updated with default values for last_longitude and last_latitude.")
except mysql.connector.Error as err:
    print(f"Error updating existing records: {err}")

# Retrieve user IDs
cursor.execute('SELECT id FROM users WHERE email = %s', (from_email,))
from_user = cursor.fetchone()
cursor.execute('SELECT id FROM users WHERE email = %s', (to_email,))
to_user = cursor.fetchone()

if from_user and to_user:
    from_user_id = from_user[0]
    to_user_id = to_user[0]
    print(f"User IDs: {from_user_id}, {to_user_id}")
else:
    print("One or both users not found.")

# Uncomment and modify these lines if you need to insert new users or friend requests
#try:
#      cursor.execute('INSERT INTO friends(user_id, friend_id) VALUES (%s, %s)', (5,4))
#     cursor.execute('INSERT INTO users (name, email, password) VALUES (%s, %s, %s)', ("Mike Johnson", "mike@example.com", "password456"))
#     cursor.execute('INSERT INTO friend_requests (from_user_id, to_user_id, status) VALUES (%s, %s, %s)', (from_user_id, to_user_id, 'Pending'))
#      conn.commit()
#except mysql.connector.IntegrityError as e:
#      print("Error inserting data:", e)

# Query and print user data
print("User data:")
cursor.execute('SELECT * FROM users')
rows = cursor.fetchall()
for row in rows:
    print(row)

# Query and print friend requests
print("Friend request list:")
cursor.execute('SELECT * FROM friend_requests')
rows = cursor.fetchall()
for row in rows:
    print(row)

# Query and print friends list
print("Friends list:")
cursor.execute('SELECT * FROM friends')
rows = cursor.fetchall()
for row in rows:
    print(row)

print("favorite place list:")
cursor.execute('SELECT * FROM favorite_places')
rows = cursor.fetchall()
for row in rows:
    print(row)

print("meeting list:")
cursor.execute('SELECT * FROM meetings')
rows = cursor.fetchall()
for row in rows:
    print(row)

print("meeting_participants list:")
cursor.execute('SELECT * FROM meeting_participants')
rows = cursor.fetchall()
for row in rows:
    print(row)
# Close database connection
cursor.close()
conn.close()

