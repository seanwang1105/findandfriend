package com.example.findandfriend;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    private ImageView profileImage;
    private TextView profileName, profileEmail;
    private Button btnEditProfile, btnViewFavorites, btnLogout,btnDeleteAcc;
    private Uri imageUri;
    private static final String FILE_NAME = "user_credentials.txt";
    private static final String TAG = "ProfileActivity";
    private String email;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.profile_activity);

        // initialize view
        profileImage = findViewById(R.id.profile_image);
        profileName = findViewById(R.id.profile_name);
        profileEmail = findViewById(R.id.profile_email);
        btnEditProfile = findViewById(R.id.btn_edit_profile);
        btnViewFavorites = findViewById(R.id.btn_view_favorites);
        btnLogout = findViewById(R.id.btn_logout);
        btnDeleteAcc=findViewById(R.id.btn_DeleteAccount);

        // load data
        loadUserData();
        downloadFavoritePlaces(email);
        // Edit buttion click event
        btnEditProfile.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, EditProfileActivity.class);
            intent.putExtra("name", profileName.getText().toString());
            intent.putExtra("email", profileEmail.getText().toString());
            intent.putExtra("imageUri", imageUri);
            editProfileLauncher.launch(intent);
        });

        // collection buttion event
        btnViewFavorites.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // goto collection
                Intent intent = new Intent(ProfileActivity.this, FavoritesActivity.class);
                startActivity(intent);
            }
        });

        // quit button click event
        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // logout function
                Toast.makeText(ProfileActivity.this, "Logged out successfully", Toast.LENGTH_SHORT).show();

                Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);

                finish();
            }
        });
        btnDeleteAcc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // logout function
                Toast.makeText(ProfileActivity.this, "Account will be deleted", Toast.LENGTH_SHORT).show();
                // Delete the user credentials file
                File file = new File(getFilesDir(), "user_credentials.txt");
                if (file.exists()) {
                    boolean deleted = file.delete();
                    if (deleted) {
                        Toast.makeText(ProfileActivity.this, "User credentials deleted", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(ProfileActivity.this, "Failed to delete user credentials", Toast.LENGTH_SHORT).show();
                    }
                }
                //delete user data function
                DeleteAccount_Func();
                Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
        });
        //get BottomNavigationView
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);

        // set default to  Home layout
        bottomNavigationView.setSelectedItemId(R.id.nav_home);

        //handle BottomNavigationView menu click event
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                Fragment selectedFragment = null;

                int itemId = item.getItemId();
                if (itemId == R.id.nav_home) {
                    // goto MainActivity
                    Intent intent = new Intent(ProfileActivity.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    return true;

                } else if (itemId == R.id.nav_discover) {
                    // goto MiddlePointActivity
                    Intent intent = new Intent(ProfileActivity.this, MiddlePointActivity.class);
                    startActivity(intent);
                    return true;

                } else if (itemId == R.id.nav_friends) {
                    // goto FriendsActivityFeedActivity
                    Intent intent = new Intent(ProfileActivity.this, FriendsActivityFeedActivity.class);
                    startActivity(intent);
                    return true;
                } else if (itemId == R.id.nav_profile) {
                    //selectedFragment = new ProfileFragment();
                    return true;
                }
                return true;
            }
        });
    }

    // load user data
    private void loadUserData() {
        // Initialize the request queue
        RequestQueue queue = Volley.newRequestQueue(this);
        String[] savedCredentials = loadCredentials();

        if (savedCredentials != null) {
            email = savedCredentials[0];
        }

        SharedPreferences sharedPreferences = getSharedPreferences("auth", MODE_PRIVATE);
        String token = sharedPreferences.getString("token", null);

        if (token == null || email.isEmpty()) {
            Toast.makeText(ProfileActivity.this, "Missing token, please login again", Toast.LENGTH_SHORT).show();
            return;
        }

        // Define the URL with the email parameter
        String url = getString(R.string.IP) + "/get_data" + "?email=" + email;

        // Create a StringRequest
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                response -> {
                    try {
                        JSONObject jsonResponse = new JSONObject(response);

                        if (jsonResponse.has("error")) {
                            Toast.makeText(ProfileActivity.this, "No user data available.", Toast.LENGTH_SHORT).show();
                        } else {
                            // Extract user data
                            String name = jsonResponse.getString("name");
                            String lastVisitPlace = jsonResponse.optString("last_visit_place", "N/A");
                            String lastVisitRating = jsonResponse.optString("last_visit_rating", "N/A");
                            //String avatarUrl = jsonResponse.optString("avatar", "");

                            // Update UI elements with user data
                            profileName.setText(name);
                            profileEmail.setText(email);
                            //lastVisitPlaceTextView.setText(lastVisitPlace);
                           // lastVisitRatingTextView.setText(lastVisitRating);

                            // Load avatar image
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(ProfileActivity.this, "Failed to parse user data.", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    Toast.makeText(ProfileActivity.this, "Error occurred while downloading user data.", Toast.LENGTH_SHORT).show();
                }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                headers.put("x-access-token", token); // Add token to request header
                return headers;
            }
        };

        // Add the request to the request queue
        queue.add(stringRequest);
    }
    //delete account from server:
    private void DeleteAccount_Func(){
        // Initialize the request queue
        RequestQueue queue = Volley.newRequestQueue(this);
        String[] savedCredentials = loadCredentials();

        if (savedCredentials != null) {
            email = savedCredentials[0];
        }

        SharedPreferences sharedPreferences = getSharedPreferences("auth", MODE_PRIVATE);
        String token = sharedPreferences.getString("token", null);

        if (token == null || email.isEmpty()) {
            Toast.makeText(ProfileActivity.this, "Missing token, please login again", Toast.LENGTH_SHORT).show();
            return;
        }

        // Define the URL with the email parameter
        String serverUrl = getString(R.string.IP) + "/delete_account";

        JSONObject loginData = new JSONObject();
        try {
            loginData.put("email", email);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        // Create a StringRequest
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, serverUrl, loginData,
                response -> {
                    try {

                        if (response.has("error")) {
                            String status=response.getString("error");
                            Toast.makeText(ProfileActivity.this, status, Toast.LENGTH_SHORT).show();
                        } else {
                            String status=response.getString("status");
                            Toast.makeText(ProfileActivity.this, "User data delete successfully.", Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(ProfileActivity.this, "Failed to delete user data.", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    Toast.makeText(ProfileActivity.this, "Error while delete user data.", Toast.LENGTH_SHORT).show();
                }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                headers.put("x-access-token", token); // Add token to request header
                return headers;
            }
        };
        jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(
                200000, // Initial timeout in ms (e.g., 10 seconds)
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES, // Retry count
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        // Add the request to the request queue
        queue.add(jsonObjectRequest);
    }
   //Load credentials to get email
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


    // use ActivityResultLauncher instead of startActivityForResult to get edit result
    private final ActivityResultLauncher<Intent> editProfileLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
            Intent data = result.getData();
            String name = data.getStringExtra("name");
            String avatar="no";
            imageUri = data.getParcelableExtra("imageUri");

            profileName.setText(name);
            profileEmail.setText(email);
            if (imageUri != null) {
                profileImage.setImageURI(imageUri);
            }
            //get token
            SharedPreferences sharedPreferences = getSharedPreferences("auth", MODE_PRIVATE);
            String token = sharedPreferences.getString("token", null);
            // Send updated data to server
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("email", email);
                jsonObject.put("name", name);
                jsonObject.put("avatar",avatar);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            String url = getString(R.string.IP) + "/update_data";
            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url, jsonObject,
                    response -> {
                           if (response.optString("status").equals("Data updated successfully")) {
                                Toast.makeText(ProfileActivity.this, "Data updated successfully", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(ProfileActivity.this, "Data updated error", Toast.LENGTH_SHORT).show();
                            }
                    },
                    error -> {
                        Toast.makeText(ProfileActivity.this, "Error occurred while updating user data.", Toast.LENGTH_SHORT).show();
                    }) {
                @Override
                public Map<String, String> getHeaders() throws AuthFailureError {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("x-access-token", token); // Add token to request header
                    return headers;
                }
            };

            // Add the request to the request queue
            Volley.newRequestQueue(this).add(jsonObjectRequest);
        }
    });

    private void downloadFavoritePlaces(String email) {
        String url = getString(R.string.IP) +"/get_favorite_places"; // Replace with your server's endpoint

        // Create a JSON object to send the email as a parameter
        JSONObject requestParams = new JSONObject();
        try {
            requestParams.put("email", email);
        } catch (JSONException e) {
            Log.e(TAG, "Failed to create JSON for request: ", e);
            return;
        }
        SharedPreferences sharedPreferences = getSharedPreferences("auth", MODE_PRIVATE);
        String token = sharedPreferences.getString("token", null);
        // Make the HTTP GET request using Volley or another library
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                Request.Method.POST,
                url,
                requestParams,
                response -> {
                    try {
                        // Parse the response to extract the favorite places
                        JSONArray favoritePlaces = response.getJSONArray("favorite_places");

                        // Save the favorite places to saved_places.json
                        savePlacesToLocalFile(favoritePlaces);

                        Toast.makeText(this, "Favorite places downloaded and saved!", Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "Downloaded places: " + favoritePlaces.toString());
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing server response: ", e);
                    }
                },
                error -> {
                    // Handle errors
                    Toast.makeText(this, "Failed to download favorite places.", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error fetching favorite places: ", error);
                }

        ){
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                headers.put("x-access-token", token); // Add token to request header
                return headers;
            }
        };

        // Add the request to the Volley queue
        Volley.newRequestQueue(this).add(jsonObjectRequest);
    }

    private void savePlacesToLocalFile(JSONArray favoritePlaces) {
        String filename = "saved_places.json";

        try {
            // Save the JSON array to the local file
            FileOutputStream fos = openFileOutput(filename, Context.MODE_PRIVATE);
            fos.write(favoritePlaces.toString().getBytes());
            fos.close();

            Log.d(TAG, "Saved places to local file: " + favoritePlaces.toString());
        } catch (IOException e) {
            Log.e(TAG, "Error saving favorite places to local file: ", e);
        }
    }
}
