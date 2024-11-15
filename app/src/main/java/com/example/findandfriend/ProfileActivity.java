package com.example.findandfriend;

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

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    private ImageView profileImage;
    private TextView profileName, profileEmail;
    private Button btnEditProfile, btnViewFavorites, btnLogout;
    private Uri imageUri;
    private static final String FILE_NAME = "user_credentials.txt";
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

        // load data
        loadUserData();

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
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);  // 清除任务栈，防止返回键回到ProfileActivity
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
    //parse user data
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
}
