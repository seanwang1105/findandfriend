// SearchFriendActivity.java
package com.example.findandfriend;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import android.widget.Button;
import android.widget.Toast;
import android.content.Intent;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SearchFriendActivity extends AppCompatActivity {

    private SearchView searchView;
    private Button searchButton;
    private RecyclerView searchResultsRecyclerView;
    private RecyclerView friendRequestsRecyclerView;
    private FriendSearchAdapter friendSearchAdapter;
    private FriendRequestAdapter friendRequestAdapter;
    private List<Friend> searchResults;
    private List<FriendRequest> friendRequests;
    private static final String TAG = "SearchFriendActivity";
    private String SERVER_URL;  // Replace with your server's IP and port
    private String FRIEND_REQUESTS_URL;  // Replace with your server's IP and port
    private static final String FILE_NAME = "user_credentials.txt";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_friend);

        SERVER_URL = getString(R.string.IP) + "/search_friends";
        FRIEND_REQUESTS_URL = getString(R.string.IP) + "/get_friend_requests";

        searchView = findViewById(R.id.search_view);
        searchButton = findViewById(R.id.button_return);
        searchResultsRecyclerView = findViewById(R.id.recycler_view_search_results);
        friendRequestsRecyclerView = findViewById(R.id.recycler_view_friend_requests);
        //BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation_search);

        searchResults = new ArrayList<>();
        friendRequests = new ArrayList<>();
        friendSearchAdapter = new FriendSearchAdapter(this, searchResults);
        friendRequestAdapter = new FriendRequestAdapter(this, friendRequests);

        searchResultsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        searchResultsRecyclerView.setAdapter(friendSearchAdapter);

        friendRequestsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        friendRequestsRecyclerView.setAdapter(friendRequestAdapter);

        setupSearchView();

        // Go click event
        searchButton.setOnClickListener(v -> {
                Intent intent = new Intent(SearchFriendActivity.this, FriendsActivityFeedActivity.class);
                startActivity(intent);
           });
        // Query for friend request
        downloadFriendRequests();

    }

    private void setupSearchView() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                if (query.length() > 2) {
                    try {
                        // Perform search
                        Log.e("MiddleActivity", "searing push button");
                        performSearch(query);

                    } catch (Exception e) {
                        // Log the error and show a message
                        Log.e("MiddleActivity", "Error during search: " + e.getMessage());
                        Toast.makeText(SearchFriendActivity.this, "Error occurred while searching.", Toast.LENGTH_SHORT).show();
                    }
                }
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                // Filter as the user types
                return true;
            }
        });
    }
    private void performSearch(String query) {
        String url = SERVER_URL + "?query=" + query;
        RequestQueue queue = Volley.newRequestQueue(this);

        SharedPreferences sharedPreferences = getSharedPreferences("auth", MODE_PRIVATE);
        String token = sharedPreferences.getString("token", null);

        if (token == null) {
            Toast.makeText(SearchFriendActivity.this, "Authorization token is missing. Please log in again.", Toast.LENGTH_SHORT).show();
            return;
        }

        // create request and Authorization header
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                response -> {
                    parseSearchResults(response);
                },
                error -> {
                    Log.e(TAG, "Error during search: " + error.getMessage());
                    Toast.makeText(SearchFriendActivity.this, "Error occurred while searching.", Toast.LENGTH_SHORT).show();
                }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                headers.put("x-access-token", token); // add token to request head
                return headers;
            }
        };

        queue.add(stringRequest);
    }

    private void parseSearchResults(String response) {
        try {
            searchResults.clear();
            JSONObject jsonResponse = new JSONObject(response);
            JSONArray friendsArray = jsonResponse.getJSONArray("friends");

            for (int i = 0; i < friendsArray.length(); i++) {
                JSONObject friendObj = friendsArray.getJSONObject(i);
                String name = friendObj.getString("name");
                String email = friendObj.getString("email");
                double latitude = friendObj.optDouble("latitude", 0.0); // Assuming location data exists, default to 0.0
                double longitude = friendObj.optDouble("longitude", 0.0);
                String rating = friendObj.optString("rating", "N/A"); // Assuming rating exists, default to "N/A"

                Friend friend = new Friend(email, name, latitude, longitude, rating, R.drawable.ic_friend_avatar1);
                searchResults.add(friend);
            }

            friendSearchAdapter.notifyDataSetChanged();

        } catch (JSONException e) {
            Log.e(TAG, "Error parsing search results: ", e);
            Toast.makeText(this, "Failed to load search results.", Toast.LENGTH_SHORT).show();
        }
    }

    private void downloadFriendRequests() {
        RequestQueue queue = Volley.newRequestQueue(this);

        String[] savedCredentials = loadCredentials();
        String email = "";
        if (savedCredentials != null) {
            email = savedCredentials[0];
        }
        SharedPreferences sharedPreferences = getSharedPreferences("auth", MODE_PRIVATE);
        String token = sharedPreferences.getString("token", null);
         // replace with user email address

        if (token == null || email == null) {
            Toast.makeText(SearchFriendActivity.this, "Missing token, please login again", Toast.LENGTH_SHORT).show();
            return;
        }

        String url = FRIEND_REQUESTS_URL + "?email=" + email;

        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                response -> {
                    parseFriendRequests(response);
                },
                error -> {
                    Log.e(TAG, "Error downloading friend requests: " + error.getMessage());
                    Toast.makeText(SearchFriendActivity.this, "Error occurred while downloading friend requests.", Toast.LENGTH_SHORT).show();
                }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                headers.put("x-access-token", token); // add token to request head
                return headers;
            }
        };

        queue.add(stringRequest);
    }

    private void parseFriendRequests(String response) {
        System.out.println("response from server is:" + response);
        if (friendRequests == null) {
            friendRequests = new ArrayList<>();
        }

        try {
            friendRequests.clear();
            JSONObject fr_response = new JSONObject(response);
            System.out.println("get response");
            JSONArray requestsArray = fr_response.getJSONArray("friend_requests");

            if (requestsArray.length() == 0) {
                // No friend requests found
                Toast.makeText(this, "No friend requests available.", Toast.LENGTH_SHORT).show();
            } else {
                // Populate friend requests if any exist
                for (int i = 0; i < requestsArray.length(); i++) {
                    JSONObject requestObj = requestsArray.getJSONObject(i);
                    int requestId = requestObj.getInt("request_id");
                    String fromEmail = requestObj.getString("from_email");
                    String fromName = requestObj.getString("from_name");

                    FriendRequest request = new FriendRequest(requestId, fromEmail, fromName);
                    friendRequests.add(request);
                    Toast.makeText(this, fromName + " (" + fromEmail + ") friend request", Toast.LENGTH_LONG).show();
                }
            }

            friendRequestAdapter.notifyDataSetChanged();

        } catch (JSONException e) {
            Log.e(TAG, "Error parsing friend requests: ", e);
            Toast.makeText(this, "Failed to load friend requests.", Toast.LENGTH_SHORT).show();
        }
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
