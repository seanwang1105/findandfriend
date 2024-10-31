// SearchFriendActivity.java
package com.example.findandfriend;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Button;
import android.widget.Toast;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkError;
import com.android.volley.NoConnectionError;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.ServerError;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class SearchFriendActivity extends AppCompatActivity {

    private SearchView searchView;
    private Button searchButton;
    private RecyclerView searchResultsRecyclerView;
    private FriendSearchAdapter friendSearchAdapter;
    private List<Friend> searchResults;

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
        /*
        // set default menu to (Home)
        bottomNavigationView.setSelectedItemId(R.id.nav_home);

        // process BottomNavigationView click event
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int itemId = item.getItemId();

                if (itemId == R.id.nav_home) {
                    // return to main menu
                    Intent intent = new Intent(SearchFriendActivity.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    return true;

                } else if (itemId == R.id.nav_discover) {
                    // goto MiddlePointActivity
                    Intent intent = new Intent(SearchFriendActivity.this, MiddlePointActivity.class);
                    startActivity(intent);
                    return true;

                } else if (itemId == R.id.nav_friends) {
                    // goto FriendsActivityFeedActivity
                    Intent intent = new Intent(SearchFriendActivity.this, FriendsActivityFeedActivity.class);
                    startActivity(intent);
                    return true;

                } else if (itemId == R.id.nav_profile) {
                    // goto ProfileActivity:
                    Intent intent = new Intent(SearchFriendActivity.this, ProfileActivity.class);
                    startActivity(intent);
                }

                return true;
            }
        });
      */
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

    /*
    private void performSearch(String query) {
        // Replace with your server's search API URL
        String url = "https://yourserver.com/api/search_friends?name=" + query;

        RequestQueue queue = Volley.newRequestQueue(this);

        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                response -> {
                    // Parse the JSON response and update the RecyclerView
                    parseSearchResults(response);
                },
                error -> {

                    parseSearchResults("response");
                    //handleVolleyError(error);
                  // Handle error
                    Toast.makeText(this, "Error fetching search results", Toast.LENGTH_SHORT).show();
                });

        stringRequest.setRetryPolicy(new DefaultRetryPolicy(
                10000, // Timeout in milliseconds (e.g., 10 seconds)
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES, // Number of retries
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        queue.add(stringRequest);
    }

   private void parseSearchResults(String response) {
       try {
           JSONArray jsonArray = new JSONArray(response);
           searchResults.clear();
           for (int i = 0; i < jsonArray.length(); i++) {
               JSONObject friendObject = jsonArray.getJSONObject(i);
               String id = friendObject.getString("id");
               String name = friendObject.getString("name");
               double latitude = friendObject.getDouble("latitude");
               double longitude = friendObject.getDouble("longitude");
               String timeAtLocation = friendObject.getString("timeAtLocation");
               int avatarResourceId = R.drawable.ic_friend_avatar1;

               Friend friend = new Friend(id, name, latitude, longitude, timeAtLocation, avatarResourceId);
               searchResults.add(friend);
           }
           friendSearchAdapter.notifyDataSetChanged();
       } catch (Exception e) {
           e.printStackTrace();
           Toast.makeText(this, "Error parsing search results", Toast.LENGTH_SHORT).show();
         }
   }

   private void handleVolleyError(VolleyError error) {
       if (error instanceof TimeoutError || error instanceof NoConnectionError) {
           // This indicates that the server did not respond or there's no internet connection
           Toast.makeText(this, "Server not responding. Please check your internet connection.", Toast.LENGTH_LONG).show();
       } else if (error instanceof AuthFailureError) {
           // Error indicating that there was an Authentication Failure while performing the request
           Toast.makeText(this, "Authentication error.", Toast.LENGTH_LONG).show();
       } else if (error instanceof ServerError) {
           // Indicates that the server responded with an error response
           Toast.makeText(this, "Server error.", Toast.LENGTH_LONG).show();
       } else if (error instanceof NetworkError) {
           // Indicates that there was network error while performing the request
           Toast.makeText(this, "Network error.", Toast.LENGTH_LONG).show();
       } else if (error instanceof ParseError) {
           // Indicates that the server response could not be parsed
           Toast.makeText(this, "Parse error.", Toast.LENGTH_LONG).show();
       } else {
           Toast.makeText(this, "An unexpected error occurred.", Toast.LENGTH_LONG).show();
       }

       // Optionally, log the error or perform additional actions
       Log.e("VolleyError", "Error occurred", error);
   }
    */
    private void performSearch(String query){

        parseSearchResults("response");
    }
    private void parseSearchResults(String response) {
        Toast.makeText(this, "build friend list", Toast.LENGTH_SHORT).show();
        searchResults.clear();
        Friend friend = new Friend("4", "john clue", 37.4, -122, "10min", R.drawable.ic_friend_avatar1);
        searchResults.add(friend);
        Friend friend1 = new Friend("2", "Emily clue", 37.0, -112, "10min", R.drawable.ic_friend_avatar1);
        searchResults.add(friend1);
        Friend friend2 = new Friend("3", "Bob clue", 38.4, -132, "10min", R.drawable.ic_friend_avatar1);
        searchResults.add(friend2);
        System.out.println(searchResults);
        friendSearchAdapter.notifyDataSetChanged();
    }

}
