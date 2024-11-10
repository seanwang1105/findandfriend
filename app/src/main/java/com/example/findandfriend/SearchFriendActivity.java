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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SearchFriendActivity extends AppCompatActivity {

    private SearchView searchView;
    private Button searchButton;
    private RecyclerView searchResultsRecyclerView;
    private FriendSearchAdapter friendSearchAdapter;
    private List<Friend> searchResults;
    private static final String TAG = "SearchFriendActivity";
    private final String SERVER_URL = getString(R.string.IP) + "/search_friends";  // Replace with your server's IP and port

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_friend);

        searchView = findViewById(R.id.search_view);
        searchButton = findViewById(R.id.button_return);
        searchResultsRecyclerView = findViewById(R.id.recycler_view_search_results);
        //BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation_search);

        searchResults = new ArrayList<>();
        friendSearchAdapter = new FriendSearchAdapter(this, searchResults);
        searchResultsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        searchResultsRecyclerView.setAdapter(friendSearchAdapter);

        setupSearchView();

        // Go click event
        searchButton.setOnClickListener(v -> {
                Intent intent = new Intent(SearchFriendActivity.this, FriendsActivityFeedActivity.class);
                startActivity(intent);
           });

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

}
