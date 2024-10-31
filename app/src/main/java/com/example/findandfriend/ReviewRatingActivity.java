package com.example.findandfriend;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.Toast;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ReviewRatingActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;
    private RatingBar ratingBar;
    private EditText reviewText;
    private Button submitReviewButton;
    private Uri selectedImageUri;
    private Button addPhotoButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.review_rating);

        ratingBar = findViewById(R.id.rating_bar);
        reviewText = findViewById(R.id.comment_box);
        submitReviewButton = findViewById(R.id.submit_review_button);
        addPhotoButton =findViewById(R.id.upload_photo_button);
        // Submit review button click
        addPhotoButton.setOnClickListener(view -> {
            //add photo handling
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, PICK_IMAGE_REQUEST);
        });

        submitReviewButton.setOnClickListener(view -> {
            float rating = ratingBar.getRating();
            String review = reviewText.getText().toString();
            submitReview(rating, review);
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

    // Mock method to handle the review submission
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            selectedImageUri = data.getData();
            Toast.makeText(this, "Photo selected", Toast.LENGTH_SHORT).show();
        }
    }

    private void submitReview(float rating, String review) {
        Toast.makeText(this, "Review Submitted!\nRating: " + rating + "\nReview: " + review, Toast.LENGTH_LONG).show();
    }

    private void uploadImage(Uri imageUri) {
        OkHttpClient client = new OkHttpClient();
        try {
            byte[] imageBytes = getContentResolver().openInputStream(imageUri).readAllBytes();
            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("image", "photo.jpg",
                            RequestBody.create(imageBytes, MediaType.parse("image/jpeg")))
                    .build();
            Request request = new Request.Builder()
                    .url("https://yourserver.com/upload") // replace with your server URL
                    .post(requestBody)
                    .build();
            new Thread(() -> {
                try (Response response = client.newCall(request).execute()) {
                    runOnUiThread(() -> {
                        if (response.isSuccessful()) {
                            Toast.makeText(ReviewRatingActivity.this, "Photo uploaded successfully", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(ReviewRatingActivity.this, "Photo upload failed", Toast.LENGTH_SHORT).show();
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                    runOnUiThread(() -> Toast.makeText(ReviewRatingActivity.this, "Upload error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                }
            }).start();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Image processing failed", Toast.LENGTH_SHORT).show();
        }
    }
}
