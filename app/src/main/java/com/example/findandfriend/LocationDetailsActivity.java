package com.example.findandfriend;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;
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
import java.util.List;
import java.io.ByteArrayOutputStream;

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


        if (locationId != null) {
            fetchPlaceAndPhoto(locationId);

        } else {
            Toast.makeText(this, "Place ID can't be found", Toast.LENGTH_SHORT).show();
        }


        loadLocationDetails(locationId,locationname,locationRate,imagesByteArrayList );
        // Meet here button click
        btnMeetHere.setOnClickListener(view -> {
            Toast.makeText(LocationDetailsActivity.this, "Meeting set at " + locationName.getText(), Toast.LENGTH_SHORT).show();
        });
        // Go click event
        btnR.setOnClickListener(view -> {
                        Intent intent = new Intent(LocationDetailsActivity.this, ReviewRatingActivity.class);
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
                // 创建导航Intent
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
        String filename = "saved_places.json";
        JSONArray savedPlacesArray;

        try {
            // 读取现有的 saved_places.json 文件
            FileInputStream fis = openFileInput(filename);
            int size = fis.available();
            byte[] buffer = new byte[size];
            fis.read(buffer);
            fis.close();

            String jsonString = new String(buffer, "UTF-8");
            savedPlacesArray = new JSONArray(jsonString);
            Log.d(TAG, "read place: " + savedPlacesArray.toString());
        } catch (IOException | JSONException e) {
            // 如果文件不存在或读取失败，创建一个新的 JSONArray
            System.out.println("read file not exist");
            savedPlacesArray = new JSONArray();
        }

        try {
            // 创建一个新的地点对象
            JSONObject newPlace = new JSONObject();
            newPlace.put("name", name);
            newPlace.put("address", address_d);

            // 将新地点添加到数组中
            savedPlacesArray.put(newPlace);

            // 将更新后的数组写回到 saved_places.json 文件中
            FileOutputStream fos = openFileOutput(filename, Context.MODE_PRIVATE);
            fos.write(savedPlacesArray.toString().getBytes());
            fos.close();

            Toast.makeText(this, "Place saved for later!", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Saved place: " + newPlace.toString());

        } catch (JSONException | IOException e) {
            Toast.makeText(this, "Failed to save place.", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Error saving place: ", e);
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
        // 使用Google地图的URI schema，"google.navigation:q=latitude,longitude"
        Uri gmmIntentUri = Uri.parse("google.navigation:q=" + lat + "," + lng);

        // 创建Intent，启动Google地图
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");

        // 确认用户的设备是否有Google地图应用
        if (mapIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(mapIntent);
        } else {
            // 提示用户安装Google地图
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
                // 创建字节数组列表来存储所有图片
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

                        // 将字节数组添加到列表
                        imagesByteArrayList.add(byteArray);

                        // 检查所有图片是否都已添加
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
}
