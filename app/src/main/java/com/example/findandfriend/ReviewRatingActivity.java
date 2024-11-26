package com.example.findandfriend;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.AuthFailureError;
import com.android.volley.RequestQueue;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;



public class ReviewRatingActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;
    private RatingBar ratingBar;
    private EditText reviewText;
    private Button submitReviewButton;
    private Uri selectedImageUri;
    private Button addPhotoButton;
    private static final String FILE_NAME = "user_credentials.txt";
    private static final String TAG = "ReviewRatingActivity";
    private String email="";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.review_rating);
        String[] savedCredentials = loadCredentials();

        if (savedCredentials != null) {
            email = savedCredentials[0];
        }

        ratingBar = findViewById(R.id.rating_bar);
        reviewText = findViewById(R.id.comment_box);
        submitReviewButton = findViewById(R.id.submit_review_button);
        addPhotoButton =findViewById(R.id.upload_photo_button);
        String locationname = getIntent().getStringExtra("placeName");
        // Submit review button click
        addPhotoButton.setOnClickListener(view -> {
            //add photo handling
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, PICK_IMAGE_REQUEST);
        });

        submitReviewButton.setOnClickListener(view -> {
            float rating = ratingBar.getRating();
            String review = reviewText.getText().toString();
            submitReview(locationname,rating, review);
            if (selectedImageUri != null) {
                uploadImage(selectedImageUri);
            } else {
                Toast.makeText(this, "No photo selected", Toast.LENGTH_SHORT).show();
            }
        });
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);

        // (Home)
        bottomNavigationView.setSelectedItemId(R.id.nav_home);

        // BottomNavigationView CLICK
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int itemId = item.getItemId();

                if (itemId == R.id.nav_home) {
                    // RETURN TO MAIN
                    Intent intent = new Intent(ReviewRatingActivity.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    return true;

                } else if (itemId == R.id.nav_discover) {
                    // GOTO MiddlePointActivity
                    Intent intent = new Intent(ReviewRatingActivity.this, MiddlePointActivity.class);
                    startActivity(intent);
                    return true;

                } else if (itemId == R.id.nav_friends) {
                    // GOTO FriendsActivityFeedActivity
                    Intent intent = new Intent(ReviewRatingActivity.this, FriendsActivityFeedActivity.class);
                    startActivity(intent);
                    return true;

                } else if (itemId == R.id.nav_profile) {
                    // GOTO ProfileActivity:
                    Intent intent = new Intent(ReviewRatingActivity.this, ProfileActivity.class);
                    startActivity(intent);
                }

                return true;
            }
        });
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
    // Mock method to handle the review submission
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            selectedImageUri = data.getData();
            Toast.makeText(this, "Photo selected", Toast.LENGTH_SHORT).show();
        }
    }

    private void submitReview(String locationName, float rating, String review) {
        // Your backend endpoint URL
        String url = getString(R.string.IP) +"/update_last_visit";
        SharedPreferences sharedPreferences = getSharedPreferences("auth", MODE_PRIVATE);
        String token = sharedPreferences.getString("token", null);
        // Prepare JSON payload
        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("email", email); // Replace with dynamic email
            requestBody.put("last_visit_place", locationName);
            requestBody.put("last_visit_rating", rating);
            requestBody.put("last_visit_place_reviews", review);
        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error preparing data", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create a request
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                Request.Method.POST,
                url,
                requestBody,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        // Handle successful response
                        Toast.makeText(
                                getApplicationContext(),
                                "Review Submitted Successfully!",
                                Toast.LENGTH_LONG
                        ).show();
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // Handle error response
                        Toast.makeText(
                                getApplicationContext(),
                                "Error submitting review: " + error.getMessage(),
                                Toast.LENGTH_LONG
                        ).show();
                    }
                }
        ) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                headers.put("x-access-token", token); // Add token to request header
                return headers;
            }
        };

        // Add request to Volley request queue
        RequestQueue requestQueue = Volley.newRequestQueue(this);
        requestQueue.add(jsonObjectRequest);
    }

    private void uploadImage(Uri imageUri) {
          }
}
