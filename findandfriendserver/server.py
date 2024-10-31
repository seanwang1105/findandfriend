from flask import Flask, request, jsonify
import sqlite3

app = Flask(__name__)

# Database helper function
def get_db_connection():
    conn = sqlite3.connect('user_data.db')
    conn.row_factory = sqlite3.Row
    return conn

# Initialize the database with necessary tables
def init_db():
    conn = get_db_connection()
    cursor = conn.cursor()
    cursor.execute('''
    CREATE TABLE IF NOT EXISTS users (
        id INTEGER PRIMARY KEY,
        email TEXT UNIQUE,
        password TEXT,
        last_visit_place TEXT,
        last_visit_rating REAL,
        avatar BLOB
    )
    ''')
    cursor.execute('''
    CREATE TABLE IF NOT EXISTS friends (
        user_id INTEGER,
        friend_email TEXT,
        FOREIGN KEY (user_id) REFERENCES users (id)
    )
    ''')
    cursor.execute('''
    CREATE TABLE IF NOT EXISTS favorite_places (
        user_id INTEGER,
        place_name TEXT,
        place_address TEXT,
        FOREIGN KEY (user_id) REFERENCES users (id)
    )
    ''')
    conn.commit()
    conn.close()

init_db()  # Call once to set up the database

@app.route('/')
def home():
    print("Received a request from", request.remote_addr)
    return "Hello from Flask!"

@app.route('/register', methods=['POST'])
def register():
    data = request.json
    email = data.get('email')
    password = data.get('password')
    print("data get from apps",email,"+",password)
    conn = get_db_connection()
    cursor = conn.cursor()
    try:
        cursor.execute('INSERT INTO users (email, password) VALUES (?, ?)', (email, password))
        conn.commit()
    except sqlite3.IntegrityError:
        return jsonify({"error": "User already exists"}), 400
    finally:
        conn.close()
    return jsonify({"status": "User registered successfully"}), 201

@app.route('/login', methods=['POST'])
def login():
    data = request.json
    email = data.get('email')
    password = data.get('password')

    conn = get_db_connection()
    cursor = conn.cursor()
    cursor.execute('SELECT * FROM users WHERE email = ? AND password = ?', (email, password))
    user = cursor.fetchone()
    conn.close()
    if user:
        return jsonify({
            "status": "Login successful",
            "email": user['email'],
            "password": user['password']  # 返回数据库存储的密码
        }), 200
    else:
        return jsonify({"error": "Invalid credentials"}), 401

@app.route('/update_data', methods=['POST'])
def update_data():
    data = request.json
    email = data.get('email')
    friend_list = data.get('friend_list', [])
    favorite_places = data.get('favorite_places', [])
    avatar = data.get('avatar')

    conn = get_db_connection()
    cursor = conn.cursor()

    # Update avatar
    cursor.execute('UPDATE users SET avatar = ? WHERE email = ?', (avatar, email))

    # Clear and update friend list
    cursor.execute('DELETE FROM friends WHERE user_id = (SELECT id FROM users WHERE email = ?)', (email,))
    user_id = cursor.execute('SELECT id FROM users WHERE email = ?', (email,)).fetchone()['id']
    for friend_email in friend_list:
        cursor.execute('INSERT INTO friends (user_id, friend_email) VALUES (?, ?)', (user_id, friend_email))

    # Clear and update favorite places
    cursor.execute('DELETE FROM favorite_places WHERE user_id = ?', (user_id,))
    for place in favorite_places:
        cursor.execute('INSERT INTO favorite_places (user_id, place_name, place_address) VALUES (?, ?, ?)',
                       (user_id, place['name'], place['address']))

    conn.commit()
    conn.close()
    return jsonify({"status": "Data updated successfully"}), 200

@app.route('/get_data', methods=['GET'])
def get_data():
    email = request.args.get('email')

    conn = get_db_connection()
    cursor = conn.cursor()

    # Get user data
    cursor.execute('SELECT last_visit_place, last_visit_rating, avatar FROM users WHERE email = ?', (email,))
    user_data = cursor.fetchone()
    if not user_data:
        return jsonify({"error": "User not found"}), 404

    # Get friend list
    cursor.execute('SELECT friend_email FROM friends WHERE user_id = (SELECT id FROM users WHERE email = ?)', (email,))
    friends = [row['friend_email'] for row in cursor.fetchall()]

    # Get favorite places
    cursor.execute('SELECT place_name, place_address FROM favorite_places WHERE user_id = (SELECT id FROM users WHERE email = ?)', (email,))
    favorites = [{"name": row['place_name'], "address": row['place_address']} for row in cursor.fetchall()]

    conn.close()
    return jsonify({
        "last_visit_place": user_data['last_visit_place'],
        "last_visit_rating": user_data['last_visit_rating'],
        "avatar": user_data['avatar'],
        "friend_list": friends,
        "favorite_places": favorites
    }), 200

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000)

