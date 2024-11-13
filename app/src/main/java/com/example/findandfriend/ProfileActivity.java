package com.example.findandfriend;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import org.json.JSONException;
import org.json.JSONObject;

public class ProfileActivity extends AppCompatActivity {

    private ImageView profileImage;
    private TextView profileName, profileEmail;
    private Button btnEditProfile, btnViewFavorites, btnLogout;
    private Uri imageUri;

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

        // Edit button click event
        btnEditProfile.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, EditProfileActivity.class);
            intent.putExtra("name", profileName.getText().toString());
            intent.putExtra("email", profileEmail.getText().toString());
            intent.putExtra("imageUri", imageUri);  // Pass current image URI
            editProfileLauncher.launch(intent);  // Launch the edit page
        });

        // collection button event
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
        String url = getString(R.string.IP) + "/get_data";

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null, response -> {
            try {
                profileName.setText(response.getString("name"));
                profileEmail.setText(response.getString("email"));
                String avatarUrl = response.getString("avatar");
                if (!avatarUrl.isEmpty()) {
                    imageUri = Uri.parse(avatarUrl);
                    profileImage.setImageURI(imageUri);
                }
                // Handle friends and favorite places if needed
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }, error -> Toast.makeText(ProfileActivity.this, "Failed to load data", Toast.LENGTH_SHORT).show());

        Volley.newRequestQueue(this).add(jsonObjectRequest);
    }


    // use ActivityResultLauncher instead of startActivityForResult to get edit result
    private final ActivityResultLauncher<Intent> editProfileLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
            Intent data = result.getData();
            String name = data.getStringExtra("name");
            String email = data.getStringExtra("email");
            imageUri = data.getParcelableExtra("imageUri");

            profileName.setText(name);
            profileEmail.setText(email);
            if (imageUri != null) {
                profileImage.setImageURI(imageUri);
            }

            // Send updated data to server
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("email", email);
                jsonObject.put("name", name);
                jsonObject.put("avatar", imageUri.toString());
            } catch (JSONException e) {
                e.printStackTrace();
            }

            String url = getString(R.string.IP) + "/update_data";
            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url, jsonObject, response -> Toast.makeText(ProfileActivity.this, "Data updated successfully", Toast.LENGTH_SHORT).show(), error -> Toast.makeText(ProfileActivity.this, "Failed to update data", Toast.LENGTH_SHORT).show());

            Volley.newRequestQueue(this).add(jsonObjectRequest);
        }
    });
}
