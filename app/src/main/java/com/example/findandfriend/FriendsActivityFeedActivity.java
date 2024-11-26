package com.example.findandfriend;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FriendsActivityFeedActivity extends AppCompatActivity {

    private RecyclerView activityFeedRecyclerView;
    private Button btn_addfriend;
    private ActivityFeedAdapter Feedadapter;
    private RequestQueue requestQueue;
    private String email;
    private static final String FILE_NAME = "user_credentials.txt";
    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.friends_activity_feed);

        activityFeedRecyclerView = findViewById(R.id.activity_feed_list);
        activityFeedRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        btn_addfriend = findViewById(R.id.find_friend);

        Feedadapter = new ActivityFeedAdapter(new ArrayList<>());
        activityFeedRecyclerView.setAdapter(Feedadapter);

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);

        btn_addfriend.setOnClickListener(v -> {
            Intent intent = new Intent(FriendsActivityFeedActivity.this, SearchFriendActivity.class);
            startActivity(intent);
        });
        String[] savedCredentials = loadCredentials();

        if (savedCredentials != null) {
            email = savedCredentials[0];
        }
        // Set default menu to Home
        bottomNavigationView.setSelectedItemId(R.id.nav_home);

        // Process BottomNavigationView click events
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                Intent intent = new Intent(FriendsActivityFeedActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                return true;
            } else if (itemId == R.id.nav_discover) {
                Intent intent = new Intent(FriendsActivityFeedActivity.this, MiddlePointActivity.class);
                startActivity(intent);
                return true;
            } else if (itemId == R.id.nav_friends) {
                return true;
            } else if (itemId == R.id.nav_profile) {
                Intent intent = new Intent(FriendsActivityFeedActivity.this, ProfileActivity.class);
                startActivity(intent);
            }

            return true;
        });

        // Initialize Volley request queue
        requestQueue = Volley.newRequestQueue(this);

        // Fetch friend activity feed from the server
        fetchFriendActivityFeed();
    }

    private void fetchFriendActivityFeed() {
        String url =  getString(R.string.IP)+"/get_friend_details"; // Replace <your-server-url> with your server URL

        SharedPreferences sharedPreferences = getSharedPreferences("auth", MODE_PRIVATE);
        String token = sharedPreferences.getString("token", null);

        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("email", email); // Replace with the logged-in user's email
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                Request.Method.POST,
                url,
                requestBody,
                response -> {
                    try {
                        // Parse the JSON response
                        JSONArray friendsArray = response.getJSONArray("friends");
                        List<ActivityFeed> feedData = new ArrayList<>();

                        for (int i = 0; i < friendsArray.length(); i++) {
                            JSONObject friend = friendsArray.getJSONObject(i);
                            String name = friend.getString("name");
                            String lastVisitPlace = friend.optString("last_visit_place", "Unknown");
                            String lastVisitRating = friend.optString("last_visit_rating", "No rating");
                            String lastReviews = friend.optString("last_visit_place_reviews","No review");
                            System.out.println("last visit activity is"+lastVisitPlace.length());
                            if (lastVisitPlace.length()>0){
                            feedData.add(new ActivityFeed(name, lastVisitPlace, lastVisitRating,lastReviews));
                          }
                        }
                        // Update RecyclerView
                        Feedadapter.updateData(feedData);
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(FriendsActivityFeedActivity.this, "Error parsing JSON", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    Log.e("Volley", "Error: " + error.getMessage());
                    Toast.makeText(FriendsActivityFeedActivity.this, "Failed to fetch data", Toast.LENGTH_SHORT).show();
                }
        ){
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                headers.put("x-access-token", token); // Add token to request header
                return headers;
            }};

        // Add the request to the Volley queue
        requestQueue.add(jsonObjectRequest);
    }
    private String[] loadCredentials() {
        FileInputStream fis = null;
        try {
            fis = openFileInput(FILE_NAME);
            byte[] buffer = new byte[fis.available()];
            fis.read(buffer);
            String credentials = new String(buffer);
            return credentials.split(",");  // split by comma to separate email and password
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
