from flask import Flask, request, jsonify
import sqlite3
import bcrypt
import jwt
import datetime
import logging
from functools import wraps
from flask import Flask
from flask_sqlalchemy import SQLAlchemy
import mysql.connector
from mysql.connector import Error
from mysql.connector.errors import IntegrityError

app = Flask(__name__)
app.config['SECRET_KEY'] = 'e7e1c1bd18770643305df571525897e77d0db5a18e70c084d03e9daa0fa4c6b9'

# Configure logging
logging.basicConfig(
    filename='internet_traffic.log',
    level=logging.INFO,
    format='%(asctime)s %(levelname)s %(message)s'
)

# Middleware to log each request
@app.before_request
def log_request_info():
    logging.info(f"Received Request: {request.method} {request.url}")
    logging.info(f"Headers: {request.headers}")
    logging.info(f"Body: {request.get_data()}")

# Middleware to log each response
@app.after_request
def log_response_info(response):
    logging.info(f"Sending Response: {response.status}")
    logging.info(f"Headers: {response.headers}")
    logging.info(f"Body: {response.get_data(as_text=True)}")
    return response

#setup Database connector
def get_db_connection():
    conn = mysql.connector.connect(
        host='localhost',
        user='findandfriend',
        password='QWEqwe1234',
        database='user_data'
    )
    return conn

#initialize database
def init_db():
    conn = get_db_connection()
    cursor = conn.cursor()

    # create users table
    cursor.execute('''
    CREATE TABLE IF NOT EXISTS users (
        id INT AUTO_INCREMENT PRIMARY KEY,
        email VARCHAR(255) UNIQUE,
        password VARCHAR(255),
        name VARCHAR(255),
        last_visit_place VARCHAR(255),
        last_visit_rating FLOAT,
        last_visit_place_reviews VARCHAR(255),
        last_longitude FLOAT,
        last_latitude FLOAT,
        avatar VARCHAR(255)
    )
    ''')

    #create friends table
    cursor.execute('''
    CREATE TABLE IF NOT EXISTS friends (
        user_id INT,
        friend_id INT,
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
        FOREIGN KEY (friend_id) REFERENCES users (id) ON DELETE CASCADE
    )
    ''')

    # create favorite_places table
    cursor.execute('''
    CREATE TABLE IF NOT EXISTS favorite_places (
        user_id INT,
        place_name VARCHAR(255),
        place_address VARCHAR(255),
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
    )
    ''')

    # create friend_requests table
    cursor.execute('''
    CREATE TABLE IF NOT EXISTS friend_requests (
        id INT AUTO_INCREMENT PRIMARY KEY,
        from_user_id INT,
        to_user_id INT,
        from_user_email VARCHAR(255),
        to_user_email VARCHAR(255),
        status ENUM('Pending', 'Accepted', 'Declined') DEFAULT 'Pending',
        timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        FOREIGN KEY (from_user_id) REFERENCES users (id) ON DELETE CASCADE,
        FOREIGN KEY (to_user_id) REFERENCES users (id) ON DELETE CASCADE
    )
    ''')
    # create meeting_requests table
    cursor.execute('''
    CREATE TABLE IF NOT EXISTS meetings (
        id INT AUTO_INCREMENT PRIMARY KEY,
        sender_email VARCHAR(255) NOT NULL,
        location_name VARCHAR(255),
        location_latitude FLOAT,
        location_longitude FLOAT,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    )
    ''')

    # create meeting_participants table
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS meeting_participants (
        id INT AUTO_INCREMENT PRIMARY KEY,
        meeting_id INT NOT NULL,
        participant_email VARCHAR(255) NOT NULL,
        status ENUM('Pending', 'Accepted', 'Rejected') DEFAULT 'Pending',
        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        FOREIGN KEY (meeting_id) REFERENCES meetings (id) ON DELETE CASCADE
    )
    ''')
    conn.commit()
    conn.close()

# initialize database
init_db()


#add token for authorization

def token_required(f):
    @wraps(f)
    def decorated(*args, **kwargs):
        token = None

        # JWT is passed in the request header
        if 'x-access-token' in request.headers:
            token = request.headers['x-access-token']

        if not token:
            return jsonify({'error': 'Token is missing!'}), 401

        try:
            data = jwt.decode(token, app.config['SECRET_KEY'], algorithms=["HS256"])
            current_user_email = data['email']
        except jwt.ExpiredSignatureError:
            return jsonify({'error': 'Token has expired!'}), 401
        except jwt.InvalidTokenError:
            return jsonify({'error': 'Invalid token!'}), 401

        return f(current_user_email, *args, **kwargs)

    return decorated

#home route
@app.route('/')
def home():
    print("Received a request from", request.remote_addr)
    return "Hello from Flask!"

#register function
@app.route('/register', methods=['POST'])
def register():
    data = request.json
    email = data.get('email')
    password = data.get('password')
    print("data get from apps", email, "+", password)
    hashed_password = bcrypt.hashpw(password.encode('utf-8'), bcrypt.gensalt())

    conn = get_db_connection()
    cursor = conn.cursor(dictionary=True)
    try:
        cursor.execute('INSERT INTO users (email, password) VALUES (%s, %s)', (email, hashed_password))
        conn.commit()
    except IntegrityError as e:
        # Check if the error is a duplicate entry error for the 'email' field
        if "Duplicate entry" in str(e):
            print("user already exist")
            return jsonify({"error": "User already exists"}), 400
        else:
            # Log and return a generic error message for other integrity errors
            print("Database error:", e)
            return jsonify({"error": "Database error occurred"}), 500
    finally:
        conn.close()

        # Return success response if user registration is successful
    return jsonify({"status": "User registered successfully"}), 201

#login function
@app.route('/login', methods=['POST'])
def login():
    data = request.json
    email = data.get('email')
    password = data.get('password')

    print(email,password)
    conn = get_db_connection()
    cursor = conn.cursor(dictionary=True) # Setting dictionary=True to get results as dictionaries

    try:
        # Execute query
        cursor.execute('SELECT * FROM users WHERE email = %s', (email,))
        user = cursor.fetchone()
        print("user is:",user)
        # Verify user and password
        if user and bcrypt.checkpw(password.encode('utf-8'), user['password'].encode('utf-8')):
            # Generate JWT Token
            token = jwt.encode({
                'email': email,
                'exp': datetime.datetime.utcnow() + datetime.timedelta(hours=24)
            }, app.config['SECRET_KEY'], algorithm="HS256")
            print("login successful")

            if isinstance(token,bytes):
                token=token.decode('utf-8')

            return jsonify({
                "status": "Login successful",
                "token": token
            }), 200
        else:
            return jsonify({"error": "Invalid credentials"}), 401

    except Exception as e:
        print("Error:", e)
        return jsonify({"error": "Server error"}), 500
    finally:
        cursor.close()
        conn.close()

# Friend search endpoint
@app.route('/search_friends', methods=['GET'])
@token_required
def search_friends(current_user_email):
    query = request.args.get('query', '')  # Get the search query from the request
    if not query:
        return jsonify({"error": "No search query provided"}), 400

    conn = get_db_connection()
    cursor = conn.cursor(dictionary=True)

    # Search for users where either the email or name matches the query
    cursor.execute("SELECT email, name FROM users WHERE email LIKE %s OR name LIKE %s",
                   (f'%{query}%', f'%{query}%'))
    results = cursor.fetchall()
    conn.close()

    if results:
        friends = [{"email": row["email"], "name": row["name"]} for row in results]
        return jsonify({"status": "Success", "friends": friends}), 200
    else:
        return jsonify({"status": "No friends found"}), 404

# Update user data
@app.route('/update_data', methods=['POST'])
@token_required
def update_data(current_user_email):
    data = request.json
    print("update request email is:",data)
    email = data.get('email')
    name = data.get('name')
    avatar = data.get('avatar')

    print("update request email is:", email)
    print("update request name is:", name)
    print("update request avatar is:", avatar)

    if isinstance(avatar, bytes):
        avatar = avatar.decode('utf-8')

    conn = get_db_connection()
    cursor = conn.cursor(dictionary=True)

    # Update avatar
    cursor.execute('UPDATE users SET name = %s,avatar = %s WHERE email = %s', (name,avatar,email))
    '''
    # user id
    cursor.execute('SELECT id FROM users WHERE email = %s', (email,))
    user = cursor.fetchone()
    if not user:
        conn.close()
        return jsonify({"error": "User not found"}), 404
    user_id = user['id']

    # update friend list
    cursor.execute('DELETE FROM friends WHERE user_id = %s', (user_id,))
    for friend_email in friend_list:
        # get userid
        cursor.execute('SELECT id FROM users WHERE email = %s', (friend_email,))
        friend = cursor.fetchone()
        if friend:
            friend_id = friend['id']
            cursor.execute('INSERT INTO friends (user_id, friend_id) VALUES (%s, %s)', (user_id, friend_id))

    # Clear and update favorite places
    cursor.execute('DELETE FROM favorite_places WHERE user_id = %s', (user_id,))
    for place in favorite_places:
        cursor.execute('INSERT INTO favorite_places (user_id, place_name, place_address) VALUES (%s, %s, %s)',
                       (user_id, place['name'], place['address']))
    '''
    conn.commit()
    conn.close()
    return jsonify({"status": "Data updated successfully"}), 200

# get user data include friend list
@app.route('/get_data', methods=['GET'])
@token_required
def get_data(current_user_email):
    email = request.args.get('email')
    print("get data email reveived",email)
    conn = get_db_connection()
    cursor = conn.cursor(dictionary=True)

    # Get user data
    cursor.execute('SELECT id,name,last_visit_place, last_visit_rating FROM users WHERE email = %s', (email,))
    user_data = cursor.fetchone()
    if not user_data:
        return jsonify({"error": "User not found"}), 404

    user_id = user_data['id']

    # Get friend list
    cursor.execute('''
    SELECT u.email, u.name
    FROM friends f
    JOIN users u ON f.friend_id = u.id
    WHERE f.user_id = %s
    ''', (user_id,))
    friends = [{"email": row["email"], "name": row["name"]} for row in cursor.fetchall()]

    # Get favorite places
    cursor.execute('SELECT place_name, place_address FROM favorite_places WHERE user_id = %s', (user_id,))
    favorites = [{"name": row['place_name'], "address": row['place_address']} for row in cursor.fetchall()]

    conn.close()
    return jsonify({
        "name":user_data['name'],
        "last_visit_place": user_data['last_visit_place'],
        "last_visit_rating": user_data['last_visit_rating'],
        #"avatar": user_data['avatar'],
        "friend_list": friends,
        "favorite_places": favorites
    }), 200

# send friending request
@app.route('/send_friend_request', methods=['POST'])
def send_friend_request():
    data = request.json
    from_email = data.get('from_email')
    to_email = data.get('to_email')

    if not from_email or not to_email:
        return jsonify({"error": "Both from_email and to_email are required"}), 400

    conn = get_db_connection()
    cursor = conn.cursor(dictionary=True)

    # get requestor and receiver user ID
    cursor.execute('SELECT id FROM users WHERE email = %s', (from_email,))
    from_user = cursor.fetchone()
    cursor.execute('SELECT id FROM users WHERE email = %s', (to_email,))
    to_user = cursor.fetchone()

    if not from_user or not to_user:
        conn.close()
        return jsonify({"error": "User not found"}), 404

    from_user_id = from_user['id']
    to_user_id = to_user['id']

    # check repeated friend list
    cursor.execute('SELECT * FROM friend_requests WHERE from_user_id = %s AND to_user_id = %s AND status = "Pending"', (from_user_id, to_user_id))
    existing_request = cursor.fetchone()
    cursor.execute('SELECT * FROM friends WHERE user_id = %s AND friend_id = %s', (from_user_id, to_user_id))
    existing_friend = cursor.fetchone()

    if existing_request:
        conn.close()
        return jsonify({"error": "Friend request already sent"}), 400
    if existing_friend:
        conn.close()
        return jsonify({"error": "You are already friends"}), 400

    # insrt new frined list
    cursor.execute('INSERT INTO friend_requests (from_user_id, to_user_id, status) VALUES (%s, %s, %s)', (from_user_id, to_user_id, 'Pending'))
    conn.commit()
    conn.close()

    return jsonify({"status": "Friend request sent successfully"}), 200

# get friend list
@app.route('/get_friend_requests', methods=['GET'])
def get_friend_requests():
    email = request.args.get('email')
    print(email)
    if not email:
        return jsonify({"error": "Email is required"}), 400

    conn = get_db_connection()
    cursor = conn.cursor(dictionary=True)

    # get user id
    cursor.execute('SELECT id FROM users WHERE email = %s', (email,))
    user = cursor.fetchone()

    if not user:
        conn.close()
        return jsonify({"error": "User not found"}), 404

    user_id = user['id']

    # get ongoing friend list
    cursor.execute('''
    SELECT fr.id, u.email AS from_email, u.name AS from_name, fr.timestamp
    FROM friend_requests fr
    JOIN users u ON fr.from_user_id = u.id
    WHERE fr.to_user_id = %s AND fr.status = "Pending"
    ''', (user_id,))

    requests = cursor.fetchall()
    conn.close()

    if requests:
        friend_requests = []
        for row in requests:
            friend_requests.append({
                "request_id": row["id"],
                "from_email": row["from_email"],
                "from_name": row["from_name"],
                "timestamp": row["timestamp"]
            })
        return jsonify({"status": "Success", "friend_requests": friend_requests}), 200
    else:
        return jsonify({"status": "No friend requests"}), 200

# accept or reject friend request
@app.route('/respond_friend_request', methods=['POST'])
def respond_friend_request():
    data = request.json
    request_id = data.get('request_id')
    action = data.get('action')  # Accept or Decline

    if not request_id or not action:
        return jsonify({"error": "Both request_id and action are required"}), 400

    if action not in ['Accepted', 'Declined']:
        return jsonify({"error": "Invalid action"}), 400

    conn = get_db_connection()
    cursor = conn.cursor(dictionary=True)

    # get friend requesting
    cursor.execute('SELECT * FROM friend_requests WHERE id = %s AND status = "Pending"', (request_id,))
    friend_request = cursor.fetchone()

    if not friend_request:
        conn.close()
        return jsonify({"error": "Friend request not found or already handled"}), 404

    from_user_id = friend_request['from_user_id']
    to_user_id = friend_request['to_user_id']

    # update friend request status
    cursor.execute('UPDATE friend_requests SET status = %s WHERE id = %s', (action, request_id))

    if action == 'Accepted':
        # add friend to friend list on two sides
        cursor.execute('INSERT INTO friends (user_id, friend_id) VALUES (%s, %s)', (from_user_id, to_user_id))
        cursor.execute('INSERT INTO friends (user_id, friend_id) VALUES (%s, %s)', (to_user_id, from_user_id))

    conn.commit()
    conn.close()

    return jsonify({"status": f"Friend request {action.lower()}ed successfully"}), 200

#get friend list from server
@app.route('/get_friend_list', methods=['GET'])
@token_required
def get_friend_list(current_user_email):
    print(" I am in friend list request now")
    email = request.args.get('email')  # Get email from request parameters
    print("email for getting friend list is:",email)
    if not email:
        return jsonify({"error": "Email parameter is missing"}), 400

    conn = get_db_connection()
    cursor = conn.cursor(dictionary=True)

    # Retrieve the user ID based on the provided email
    cursor.execute('SELECT id FROM users WHERE email = %s', (email,))
    user = cursor.fetchone()
    if not user:
        conn.close()
        return jsonify({"error": "User not found"}), 404

    user_id = user['id']

    # Fetch friends for the specified user
    cursor.execute('''
    SELECT u.id, u.name, u.email, u.last_latitude AS latitude, u.last_longitude AS longitude
    FROM friends f
    JOIN users u ON f.friend_id = u.id
    WHERE f.user_id = %s
    ''', (user_id,))

    friends = [{"id": row["id"], "name": row["name"], "email": row["email"], "latitude": row["latitude"],
                "longitude": row["longitude"]} for row in cursor.fetchall()]

    print("friend list is:",friends)
    conn.close()
    return jsonify({"status": "success", "friends": friends}), 200

@app.route('/update_location', methods=['POST'])
@token_required
def update_location(current_user_email):
    data = request.get_json()
    email = data.get('email')
    latitude = data.get('latitude')
    longitude = data.get('longitude')

    print("location update received:",email,latitude,longitude)
    if latitude is None or longitude is None:
        return jsonify({"error": "Latitude and longitude are required"}), 400
    # Update last_latitude and last_longitude in the database

    conn = get_db_connection()
    cursor = conn.cursor(dictionary=True)

    try:
        cursor.execute('''
            UPDATE users
            SET last_latitude = %s, last_longitude = %s
            WHERE email = %s
        ''', (latitude, longitude, email))
        conn.commit()
    except mysql.connector.Error as e:
        print("Database error:", e)
        return jsonify({"error": "Database update failed"}), 500
    finally:
        cursor.close()
        conn.close()

    return jsonify({"status": "Location updated successfully"}), 200

@app.route('/upload_favorite_place', methods=['POST'])
@token_required
def upload_favorite_place(current_user_email):
    data = request.json
    email=data.get('email')
    print("update favorite places from email:",email)
    place_name = data.get('name')
    place_address = data.get('address')

    if not place_name or not place_address:
        return jsonify({"error": "Name and address are required"}), 400

    conn = get_db_connection()
    cursor = conn.cursor(dictionary=True)

    try:
        # Get user ID based on email
        cursor.execute('SELECT id FROM users WHERE email = %s', (email,))
        user = cursor.fetchone()

        if not user:
            return jsonify({"error": "User not found"}), 404

        user_id = user['id']

        # Insert the favorite place into the database
        cursor.execute('INSERT INTO favorite_places (user_id, place_name, place_address) VALUES (%s, %s, %s)',
                       (user_id, place_name, place_address))
        conn.commit()
    except mysql.connector.Error as e:
        print("Database error:", e)
        return jsonify({"error": "Database operation failed"}), 500
    finally:
        conn.close()

    return jsonify({"status": "Favorite place uploaded successfully"}), 201

@app.route('/get_favorite_places', methods=['POST'])
@token_required
def get_favorite_places(current_user_email):
    data=request.json
    email=data.get('email')
    print("get favorite places from email:",email)
    conn = get_db_connection()
    cursor = conn.cursor(dictionary=True)

    try:
        # Get user ID based on email
        cursor.execute('SELECT id FROM users WHERE email = %s', (email,))
        user = cursor.fetchone()

        if not user:
            return jsonify({"error": "User not found"}), 404

        user_id = user['id']

        # Fetch favorite places from the database
        cursor.execute('SELECT place_name AS name, place_address AS address FROM favorite_places WHERE user_id = %s', (user_id,))
        favorite_places = cursor.fetchall()

        return jsonify({"favorite_places": favorite_places}), 200

    except mysql.connector.Error as e:
        print("Database error:", e)
        return jsonify({"error": "Failed to fetch favorite places"}), 500
    finally:
        conn.close()

@app.route('/get_friend_details', methods=['POST'])
@token_required
def get_friend_details(current_user_email):
    data = request.json
    email = data.get('email')
    print("get friend details request",email)
    if not email:
        return jsonify({"error": "Email is required"}), 400

    conn = get_db_connection()
    cursor = conn.cursor(dictionary=True)

    try:
        # Fetch the user ID
        cursor.execute('SELECT id FROM users WHERE email = %s', (email,))
        user = cursor.fetchone()

        if not user:
            return jsonify({"error": "User not found"}), 404

        user_id = user['id']

        # Fetch friends' details
        cursor.execute('''
        SELECT u.email, u.name, u.last_visit_place, u.last_visit_rating, u.last_visit_place_reviews
        FROM friends f
        JOIN users u ON f.friend_id = u.id
        WHERE f.user_id = %s
        ''', (user_id,))

        friends_activities = []
        for row in cursor.fetchall():
            friend_info = {
                "email": row["email"],
                "name": row["name"] if row["name"] else row["email"],  # Use email if name is NULL
                "last_visit_place": row["last_visit_place"],
                "last_visit_rating": row["last_visit_rating"],
                "last_visit_place_reviews": row["last_visit_place_reviews"]
            }
            friends_activities.append(friend_info)

        return jsonify({"friends": friends_activities}), 200

    except mysql.connector.Error as e:
        print("Database error:", e)
        return jsonify({"error": "Failed to fetch friend details"}), 500
    finally:
        cursor.close()
        conn.close()
@app.route('/update_last_visit', methods=['POST'])
@token_required
def update_last_visit(current_user_email):
    data = request.json

    email = data.get('email')
    last_visit_place = data.get('last_visit_place')
    last_visit_rating = data.get('last_visit_rating')
    last_visit_place_reviews = data.get('last_visit_place_reviews')
    print("update last visit request",email)
    if not email:
        return jsonify({"error": "Email is required"}), 400

    conn = get_db_connection()
    cursor = conn.cursor(dictionary=True)

    try:
        # Check if user exists
        cursor.execute('SELECT id FROM users WHERE email = %s', (email,))
        user = cursor.fetchone()

        if not user:
            return jsonify({"error": "User not found"}), 404

        # Update last visit details
        cursor.execute('''
            UPDATE users
            SET last_visit_place = %s, last_visit_rating = %s, last_visit_place_reviews = %s
            WHERE email = %s
        ''', (last_visit_place, last_visit_rating, last_visit_place_reviews, email))
        conn.commit()

    except mysql.connector.Error as e:
        print("Database error:", e)
        return jsonify({"error": "Failed to update last visit details"}), 500
    finally:
        cursor.close()
        conn.close()

    return jsonify({"status": "Last visit details updated successfully"}), 200

@app.route('/create_meeting', methods=['POST'])
@token_required
def create_meeting(current_user_email):
    data = request.json
    sender_email = data.get('sender_email')
    location_name = data.get('location_name')
    location_latitude = data.get('location_latitude')
    location_longitude = data.get('location_longitude')
    friends_emails = data.get('friends_emails')

    print("Meeting created by:", sender_email)

    # Validate input
    if not sender_email or not location_name or not location_latitude or not location_longitude or not friends_emails:
        return jsonify({"error": "All fields are required"}), 400

    conn = get_db_connection()
    cursor = conn.cursor(dictionary=True)

    try:
        # Insert meeting into meetings table
        cursor.execute('''
            INSERT INTO meetings (sender_email, location_name, location_latitude, location_longitude)
            VALUES (%s, %s, %s, %s)
        ''', (sender_email, location_name, location_latitude, location_longitude))
        meeting_id = cursor.lastrowid

        # Add sender as a participant with "accept" status
        cursor.execute('''
            INSERT INTO meeting_participants (meeting_id, participant_email, status)
            VALUES (%s, %s, %s)
        ''', (meeting_id, sender_email, 'Accepted'))

        # Add other participants to meeting_participants table
        for email in friends_emails:
            cursor.execute('''
                INSERT INTO meeting_participants (meeting_id, participant_email, status)
                VALUES (%s, %s, %s)
            ''', (meeting_id, email, 'pending'))

        conn.commit()
    except mysql.connector.Error as e:
        conn.rollback()
        print("Database error:", e)
        return jsonify({"error": "Failed to create meeting"}), 500
    finally:
        cursor.close()
        conn.close()

    return jsonify({"status": "Meeting created successfully", "meeting_id": meeting_id}), 201


@app.route('/respond_meeting', methods=['POST'])
@token_required
def respond_meeting(current_user_email):
    data = request.json
    email=data.get("email")
    meeting_id = data.get('request_id')
    response = data.get('action')  # Accept or Reject
    print("meeting respond by :",meeting_id)
    if not meeting_id or response not in ['Accepted', 'Rejected']:
        return jsonify({"error": "Invalid meeting ID or response"}), 400

    conn = get_db_connection()
    cursor = conn.cursor(dictionary=True)

    try:
        # Update response in meeting_participants table
        cursor.execute('''
            UPDATE meeting_participants
            SET status = %s, updated_at = CURRENT_TIMESTAMP
            WHERE meeting_id = %s AND participant_email = %s
        ''', (response, meeting_id, email))

        if cursor.rowcount == 0:
            return jsonify({"error": "Meeting not found or unauthorized"}), 404

        conn.commit()
    except mysql.connector.Error as e:
        conn.rollback()
        print("Database error:", e)
        return jsonify({"error": "Failed to respond to meeting"}), 500
    finally:
        cursor.close()
        conn.close()

    return jsonify({"status": "Response recorded successfully"}), 200

@app.route('/meeting_status', methods=['GET'])
@token_required
def meeting_status(current_user_email):
    # Retrieve the email parameter from the request
    email = request.args.get('email')
    print("meeting staus queried by :",email)
    if not email:
        return jsonify({"error": "Email is required"}), 400

    conn = get_db_connection()
    cursor = conn.cursor(dictionary=True)

    try:
        # Fetch all meeting IDs where the given email is a participant
        cursor.execute('''
            SELECT DISTINCT meeting_id
            FROM meeting_participants
            WHERE participant_email = %s
        ''', (email,))
        meetings = cursor.fetchall()

        if not meetings:
            return jsonify({"error": "No meetings found for this email"}), 404

        result = []

        # For each meeting, fetch meeting details and participant status summary
        for meeting in meetings:
            meeting_id = meeting['meeting_id']

            # Fetch meeting details
            cursor.execute('''
                SELECT sender_email, location_name, location_latitude, location_longitude
                FROM meetings
                WHERE id = %s
            ''', (meeting_id,))
            meeting_details = cursor.fetchone()

            if not meeting_details:
                continue

            # Fetch participant statuses for the meeting
            cursor.execute('''
                SELECT status
                FROM meeting_participants
                WHERE meeting_id = %s
            ''', (meeting_id,))
            participants = cursor.fetchall()

            # Calculate the status counts
            accepted = sum(1 for p in participants if p['status'].lower() == 'accepted')
            rejected = sum(1 for p in participants if p['status'].lower() == 'rejected')
            pending = sum(1 for p in participants if p['status'].lower() == 'pending')

            # Create a status summary string
            status_summary = f"{accepted} accepted, {rejected} rejected, {pending} pending"
            timestamp = meeting_details.get('timestamp')
            # Add the meeting to the result
            result.append({
                "meeting_id": meeting_id,
                "meeting_details": {
                    "sender_email": meeting_details['sender_email'],
                    "location_name": meeting_details['location_name'],
                    "location_latitude": meeting_details['location_latitude'],
                    "location_longitude": meeting_details['location_longitude']
                },
                 "status_summary": f"{status_summary} (Last updated: {timestamp})"
            })

        conn.close()
        return jsonify(result), 200
    except mysql.connector.Error as e:
        print("Database error:", e)
        return jsonify({"error": "Failed to fetch meeting status"}), 500
    finally:
        cursor.close()
        conn.close()

@app.route('/delete_friend', methods=['POST'])
@token_required
def delete_friend(current_user_email):
    """
    Delete friendship between the sender and the friend.
    """
    data = request.json
    sender_email = data.get('sender_email')
    friend_email = data.get('friend_email')
    print("friend deleted by :",sender_email,friend_email)
    if not sender_email or not friend_email:
        return jsonify({"error": "Both sender_email and friend_email are required"}), 400

    conn = get_db_connection()
    cursor = conn.cursor(dictionary=True)

    try:
        # Get user IDs for sender and friend
        cursor.execute('SELECT id FROM users WHERE email = %s', (sender_email,))
        sender = cursor.fetchone()

        cursor.execute('SELECT id FROM users WHERE email = %s', (friend_email,))
        friend = cursor.fetchone()

        if not sender or not friend:
            return jsonify({"error": "One or both users not found"}), 404

        sender_id = sender['id']
        friend_id = friend['id']

        # Delete friendship in both directions
        cursor.execute('DELETE FROM friends WHERE user_id = %s AND friend_id = %s', (sender_id, friend_id))
        cursor.execute('DELETE FROM friends WHERE user_id = %s AND friend_id = %s', (friend_id, sender_id))

        conn.commit()
        return jsonify({"status": "Friendship deleted successfully"}), 200

    except mysql.connector.Error as e:
        print("Database error:", e)
        conn.rollback()
        return jsonify({"error": "Failed to delete friendship"}), 500
    finally:
        cursor.close()
        conn.close()

@app.route('/delete_meeting_participant', methods=['POST'])
@token_required
def delete_meeting_participant(current_user_email):
    """
    Delete a participant from a meeting.
    """
    data = request.json
    email = data.get('email')
    meeting_id = data.get('meeting_id')
    print("meeting deleted by :",email,meeting_id)
    if not email or not meeting_id:
        return jsonify({"error": "Both email and meeting_id are required"}), 400

    conn = get_db_connection()
    cursor = conn.cursor(dictionary=True)

    try:
        # Verify if the participant exists in the meeting
        cursor.execute('''
            SELECT * FROM meeting_participants
            WHERE meeting_id = %s AND participant_email = %s
        ''', (meeting_id, email))
        participant = cursor.fetchone()

        if not participant:
            return jsonify({"error": "Participant not found in the specified meeting"}), 404

        # Delete the participant from the meeting
        cursor.execute('''
            DELETE FROM meeting_participants
            WHERE meeting_id = %s AND participant_email = %s
        ''', (meeting_id, email))

        conn.commit()
        return jsonify({"status": "Participant deleted successfully"}), 200

    except mysql.connector.Error as e:
        print("Database error:", e)
        conn.rollback()
        return jsonify({"error": "Failed to delete participant"}), 500
    finally:
        cursor.close()
        conn.close()

@app.route('/delete_account', methods=['POST'])
@token_required

def delete_account(current_user_email):
    """
    Deletes the user account and all associated friendships from the database.
    """
    data = request.json
    email = data.get('email')
    print("account deleted by:",email)
    if not email:
        return jsonify({"error": "Email is required"}), 400

    conn = get_db_connection()
    cursor = conn.cursor(dictionary=True)

    try:
        # Get the user ID for the provided email
        cursor.execute('SELECT id FROM users WHERE email = %s', (email,))
        user = cursor.fetchone()

        if not user:
            return jsonify({"error": "User not found"}), 404

        user_id = user['id']

        # Delete all friendships involving the user
        cursor.execute('DELETE FROM friends WHERE user_id = %s OR friend_id = %s', (user_id, user_id))

        # Delete the user account
        cursor.execute('DELETE FROM users WHERE id = %s', (user_id,))

        conn.commit()
        return jsonify({"status": "Account and associated data deleted successfully"}), 200

    except mysql.connector.Error as e:
        print("Database error:", e)
        conn.rollback()
        return jsonify({"error": "Failed to delete account"}), 500
    finally:
        cursor.close()
        conn.close()


if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000)

