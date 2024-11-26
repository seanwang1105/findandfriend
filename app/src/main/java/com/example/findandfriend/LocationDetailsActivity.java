package com.example.findandfriend;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.libraries.places.api.model.PhotoMetadata;
import android.widget.TextView;
import android.widget.Button;
import android.widget.Toast;
import android.util.Log;
import android.content.Context;
import android.net.Uri;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.PhotoMetadata;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.FetchPhotoRequest;
import com.google.android.libraries.places.api.net.FetchPhotoResponse;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.PlacesClient;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.io.ByteArrayOutputStream;
import java.util.Map;

public class LocationDetailsActivity extends AppCompatActivity {

    private ViewPager2 viewPager2;
    private List<Integer> imageList;
    private TextView locationName;
    private TextView locationDescription;
    private Button btnMeetHere;
    private Button btnR;
    private Button btnSaveForLater;
    private Button btnNav;
    private static final String TAG = "LocationDetailsActivity";
    private PlacesClient placesClient;
    private static final String FILE_NAME = "user_credentials.txt";
    private String email;

    ArrayList<byte[]> imagesByteArrayList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.location_details);


        locationName = findViewById(R.id.location_name);
        locationDescription = findViewById(R.id.location_description);
        btnMeetHere = findViewById(R.id.btn_meet_here);
        btnR = findViewById(R.id.btn_Review);
        viewPager2 = findViewById(R.id.image_carousel);
        btnSaveForLater = findViewById(R.id.btn_save_for_later);
        btnNav = findViewById(R.id.btn_Nav);

        // Initialize image list (Replace these with your actual images in drawable)
        imageList = new ArrayList<>();
        imageList.add(R.drawable.image1);
        imageList.add(R.drawable.image2);
        placesClient = Places.createClient(this);

        // Get location details from the intent
        String locationId = getIntent().getStringExtra("placeID");
        String locationname = getIntent().getStringExtra("placeName");
        double locationRate  = getIntent().getDoubleExtra("placeRating", 0.0);
        double destinationLat  = getIntent().getDoubleExtra("placeLatitude", 0.0);
        double destinationLng  = getIntent().getDoubleExtra("placeLongitude", 0.0);
        String address_d = getIntent().getStringExtra("placeAddress");

        List<Friend> selectedFriends = SelectedFriendShareData.getInstance().getSelectedFriends();

        if (locationId != null) {
            fetchPlaceAndPhoto(locationId);

        } else {
            Toast.makeText(this, "Place ID can't be found", Toast.LENGTH_SHORT).show();
        }


        loadLocationDetails(locationId,locationname,locationRate,imagesByteArrayList );
        // Meet here button click
        btnMeetHere.setOnClickListener(view -> {
            String[] savedCredentials = loadCredentials();

            if (savedCredentials != null) {
                email = savedCredentials[0];
            }
            String locationName = this.locationName.getText().toString();
            double locationLatitude = destinationLat; // From intent
            double locationLongitude = destinationLng; // From intent
            // Call the standalone function
            createMeeting(email, locationName, locationLatitude, locationLongitude, selectedFriends);
            Toast.makeText(LocationDetailsActivity.this, "Meeting set at " + locationName, Toast.LENGTH_SHORT).show();
        });
        // Go click event
        btnR.setOnClickListener(view -> {
                        Intent intent = new Intent(LocationDetailsActivity.this, ReviewRatingActivity.class);
                        intent.putExtra("placeName",locationname);
                        startActivity(intent);
                    });
        //Save for later
        btnSaveForLater.setOnClickListener(view -> {
                    String name = locationName.getText().toString();
                    String description = address_d;
                   savePlaceForLater(name, description);
                });
        //Navgation
        btnNav.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigateToLocation(destinationLat, destinationLng);
            }
        });
        // get BottomNavigationView
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);

        // set default manu to (Home)
        bottomNavigationView.setSelectedItemId(R.id.nav_home);

        // handle BottomNavigationView menu click event
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int itemId = item.getItemId();

                if (itemId == R.id.nav_home) {
                    // return to main
                    Intent intent = new Intent(LocationDetailsActivity.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    return true;

                } else if (itemId == R.id.nav_discover) {
                    //goto MiddlePointActivity
                    Intent intent = new Intent(LocationDetailsActivity.this, MiddlePointActivity.class);
                    startActivity(intent);
                    return true;

                } else if (itemId == R.id.nav_friends) {
                    // goto FriendsActivityFeedActivity
                    Intent intent = new Intent(LocationDetailsActivity.this, FriendsActivityFeedActivity.class);
                    startActivity(intent);
                    return true;

                } else if (itemId == R.id.nav_profile) {
                    // goto ProfileActivity:
                    Intent intent = new Intent(LocationDetailsActivity.this, ProfileActivity.class);
                    startActivity(intent);
                }

                return true;
            }
        });
    }

    //
    private void savePlaceForLater(String name, String address_d) {
        String url = getString(R.string.IP) +"/upload_favorite_place"; // Replace with your server's endpoint
        String[] savedCredentials = loadCredentials();

        SharedPreferences sharedPreferences = getSharedPreferences("auth", MODE_PRIVATE);
        String token = sharedPreferences.getString("token", null);

        if (savedCredentials != null) {
            email = savedCredentials[0];
        }
        // Create a JSON object for the request payload
        JSONObject placeData = new JSONObject();
        try {
            placeData.put("email",email);
            placeData.put("name", name);
            placeData.put("address", address_d);
        } catch (JSONException e) {
            Log.e(TAG, "Failed to create JSON for upload: ", e);
            return;
        }

        // Use Volley or another HTTP library to send the request
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                Request.Method.POST,
                url,
                placeData,
                response -> {
                    // Handle success
                    Toast.makeText(this, "Place uploaded to server!", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Server response: " + response.toString());
                },
                error -> {
                    // Handle error
                    Toast.makeText(this, "Failed to upload place to server.", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Server error: ", error);
                }
        ){
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                headers.put("x-access-token", token); // Add token to request header
                return headers;
            }
        };

        // Add the request to the Volley request queue
        Volley.newRequestQueue(this).add(jsonObjectRequest);
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
       // Mock method to load location details based on location ID
    private void loadLocationDetails(String locationId,String locationname,double locationRate,ArrayList<byte[]> imagesByteArrayList) {
        if (locationId == "1") {
            // Set up the adapter with the image list
            ImageSliderAdapter adapter = new ImageSliderAdapter(imagesByteArrayList);
            viewPager2.setAdapter(adapter);
            locationName.setText(locationname);
            locationDescription.setText(String.valueOf(locationRate));
        } else if (locationId == "2") {
            ImageSliderAdapter adapter = new ImageSliderAdapter(imagesByteArrayList);
            viewPager2.setAdapter(adapter);
            locationName.setText(locationname);
            locationDescription.setText(String.valueOf(locationRate));
        }
        else{
            ImageSliderAdapter adapter = new ImageSliderAdapter(imagesByteArrayList);
            viewPager2.setAdapter(adapter);
            locationName.setText(locationname);
            locationDescription.setText(String.valueOf(locationRate));
        }
    }

    //navigation function
    private void navigateToLocation(double lat, double lng) {
        Uri gmmIntentUri = Uri.parse("google.navigation:q=" + lat + "," + lng);

        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");

        if (mapIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(mapIntent);
        } else {
            System.out.println("Google Maps is not installed.");
        }
    }

    private void fetchPlaceAndPhoto(String placeId) {
        //
        List<Place.Field> placeFields = Arrays.asList(Place.Field.CURRENT_SECONDARY_OPENING_HOURS,Place.Field.PHOTO_METADATAS);
        FetchPlaceRequest request = FetchPlaceRequest.builder(placeId, placeFields).build();
        System.out.println("in fetch function1"+placeId);
        placesClient.fetchPlace(request).addOnSuccessListener((response) -> {
            Place place = response.getPlace();
            List<PhotoMetadata> photoMetadataList = place.getPhotoMetadatas();
            System.out.println("in fetch function2");
            if (photoMetadataList != null && !photoMetadataList.isEmpty()) {
                 for (PhotoMetadata photoMetadata : photoMetadataList) {
                    System.out.println("collecting photos....");
                    FetchPhotoRequest photoRequest = FetchPhotoRequest.builder(photoMetadata)
                            .setMaxWidth(500) // 可根据需求调整
                            .setMaxHeight(500)
                            .build();

                    placesClient.fetchPhoto(photoRequest).addOnSuccessListener((fetchPhotoResponse) -> {
                        Bitmap bitmap = fetchPhotoResponse.getBitmap();
                        ByteArrayOutputStream stream = new ByteArrayOutputStream();
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                        byte[] byteArray = stream.toByteArray();

                        imagesByteArrayList.add(byteArray);


                        if(imagesByteArrayList.size() == photoMetadataList.size()) {
                            ImageSliderAdapter adapter = new ImageSliderAdapter(imagesByteArrayList);
                            viewPager2.setAdapter(adapter);
                        }
                    }).addOnFailureListener((exception) -> {
                        System.out.println("in fetch function 3");
                        exception.printStackTrace();
                    });
                }
                System.out.println("loop function complete");
            }
        }).addOnFailureListener((exception) -> {
            System.out.println("in fetch function 4");
            exception.printStackTrace();
        });
    }
    private void createMeeting(String senderEmail, String locationName, double locationLatitude, double locationLongitude, List<Friend> selectedFriends) {
        if (senderEmail == null || senderEmail.isEmpty() || selectedFriends == null || selectedFriends.isEmpty()) {
            Toast.makeText(this, "Sender email and selected friends are required", Toast.LENGTH_SHORT).show();
            return;
        }
        SharedPreferences sharedPreferences = getSharedPreferences("auth", MODE_PRIVATE);
        String token = sharedPreferences.getString("token", null);
        // Prepare the JSON payload
        JSONArray friendsEmails = new JSONArray();
        for (Friend friend : selectedFriends) {
            if (friend.getId() !="me") {
                friendsEmails.put(friend.getEmail());
            }
        }

        JSONObject payload = new JSONObject();
        try {
            payload.put("sender_email", senderEmail);
            payload.put("location_name", locationName);
            payload.put("location_latitude", locationLatitude);
            payload.put("location_longitude", locationLongitude);
            payload.put("friends_emails", friendsEmails);
        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to prepare request", Toast.LENGTH_SHORT).show();
            return;
        }

        // Send POST request using Volley
        String url = getString(R.string.IP)+"/create_meeting"; // Replace with your server URL
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, payload,
                response -> {
                    // Handle success
                    try {
                        String status = response.getString("status");
                        if (status.equals("Meeting created successfully")) {
                            int meetingId = response.getInt("meeting_id");
                            Toast.makeText(this, "Meeting created! ID: " + meetingId, Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "Unexpected response", Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Failed to parse response", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    // Handle error
                    Toast.makeText(this, "Failed to create meeting: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
        ) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                headers.put("x-access-token", token); // Add token to request header
                return headers;
            }
        };

        // Add the request to the Volley queue
        RequestQueue requestQueue = Volley.newRequestQueue(this);
        requestQueue.add(request);
    }

}
