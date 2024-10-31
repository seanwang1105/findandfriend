package com.example.findandfriend;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Color;
import android.graphics.Typeface;
import android.content.Context;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.Toast;


import android.util.Log;
import android.view.MenuItem;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.appcompat.widget.SearchView;

import com.example.findandfriend.Location;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.PhotoMetadata;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.FetchPhotoRequest;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsResponse;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.example.findandfriend.Friend;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MiddlePointActivity extends AppCompatActivity implements OnMapReadyCallback {

    private MapView mapView;
    private GoogleMap googleMap;
    private FriendAdapter friendAdapter;
    private static final String TAG = "MapsActivity";
    private SearchView searchView;
    private RecyclerView searchResultsList;
    private PlacesClient placesClient;
    private SearchResultsAdapter searchResultsAdapter;
    private List<AutocompletePrediction> predictionList = new ArrayList<>();
    private FloatingActionButton btMeetFloat;
    private Place place;
    private LatLng placeLatLng;
    private LatLng averLatlng;
    private double averlogitute;
    private double averlatitute;
    private int fri_num;
    private double navlogitute;
    private double navlatitude;
    private Bitmap placeImage;
    private boolean friendselected;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.middle_point_activity);

        btMeetFloat=findViewById(R.id.fab_meet_here);
        // initial MapView
        mapView = findViewById(R.id.map_middle_point);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        int screenHeight = getResources().getDisplayMetrics().heightPixels;
        ViewGroup.LayoutParams params = mapView.getLayoutParams();
        params.height = screenHeight / 3;  // set to 1/3 of screen height
        mapView.setLayoutParams(params);



        // Initialize Places API client
        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), getString(R.string.google_maps_key));
        }
        placesClient = Places.createClient(this);

        // Initialize search bar
        searchView = findViewById(R.id.search_view);
        Log.e("MiddleActivity", "searchBar is established");


        if (searchView == null) {
            Log.e("MiddleActivity", "searchBar is null. Check the layout XML for R.id.search_bar.");
            return;  // Exit early if searchBar is null
        }

        setupSearchView();

        // Go click event
        btMeetFloat.setOnClickListener(v -> {

            Intent intent = new Intent(MiddlePointActivity.this, LocationDetailsActivity.class);
            if (place != null) {
                System.out.println("placeid from middle:" + place.getId());
                intent.putExtra("placeID", place.getId());
                intent.putExtra("placeName", place.getName());
                intent.putExtra("placeRating", place.getRating());
                intent.putExtra("placeLatitude", placeLatLng.latitude);
                intent.putExtra("placeLongitude", placeLatLng.longitude);
                intent.putExtra("placeAddress", place.getFormattedAddress());
            }
            else{
                intent.putExtra("placeID","No id");
                intent.putExtra("placeName","No name");
                intent.putExtra("placeRating","0.0");
                intent.putExtra("placeLatitude",averlatitute);
                intent.putExtra("placeLongitude",averlogitute);
                intent.putExtra("placeAddress","No place");
            }
            startActivity(intent);
        });
        /*
        searchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 2) {
                    try {
                        // Perform search
                        performSearch(s.toString());
                    } catch (Exception e) {
                        // Log the error and show a message
                        Log.e("MiddleActivity", "Error during search: " + e.getMessage());
                        Toast.makeText(MiddlePointActivity.this, "Error occurred while searching.", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
        */
        // get friends data from MainActivity
        List<Friend> selectedFriends = getIntent().getParcelableArrayListExtra("selected_friends");
        friendAdapter = new FriendAdapter(this,  selectedFriends);

        // Initialize RecyclerView
        searchResultsList = findViewById(R.id.search_results_list);
        searchResultsList.setLayoutManager(new LinearLayoutManager(this));
        searchResultsAdapter = new SearchResultsAdapter(predictionList, prediction -> {
            // When an item is clicked, fetch more details about the place
            fetchPlaceDetails(prediction.getPlaceId());
        });
        searchResultsList.setAdapter(searchResultsAdapter);

        // initial BottomNavigationView
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);

        // set default menu to (Discover)
        bottomNavigationView.setSelectedItemId(R.id.nav_discover);

        // handle BottomNavigationView menu click event
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int itemId = item.getItemId();

                if (itemId == R.id.nav_home) {
                    // goto MainActivity
                    Intent intent = new Intent(MiddlePointActivity.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    return true;

                } else if (itemId == R.id.nav_discover) {
                    // stay with current
                    return true;

                } else if (itemId == R.id.nav_friends) {
                    // goto FriendsActivityFeedActivity
                    Intent intent = new Intent(MiddlePointActivity.this, FriendsActivityFeedActivity.class);
                    startActivity(intent);
                    return true;
                }else if (itemId == R.id.nav_profile) {
                    // goto FriendsActivityFeedActivity
                    Intent intent = new Intent(MiddlePointActivity.this, ProfileActivity.class);
                    startActivity(intent);
                    return true;
                }
                return true;
            }
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
                        Toast.makeText(MiddlePointActivity.this, "Error occurred while searching.", Toast.LENGTH_SHORT).show();
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
        // ... other methods ...
    private void performSearch(String query) {

        FindAutocompletePredictionsRequest request = FindAutocompletePredictionsRequest.builder()
                .setQuery(query)
                .build();
        Log.e("MiddleActivity", "in searching now");
        placesClient.findAutocompletePredictions(request).addOnSuccessListener((response) -> {
            predictionList.clear();
            predictionList.addAll(response.getAutocompletePredictions());
            searchResultsAdapter.notifyDataSetChanged();
        }).addOnFailureListener((exception) -> {
            Toast.makeText(this, "Search failed: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void fetchPlaceDetails(String placeId) {
        List<Place.Field> placeFields = Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG, Place.Field.RATING, Place.Field.FORMATTED_ADDRESS,Place.Field.PHOTO_METADATAS);
        FetchPlaceRequest request = FetchPlaceRequest.builder(placeId, placeFields).build();

        placesClient.fetchPlace(request).addOnSuccessListener((response) -> {
            place = response.getPlace();
            placeLatLng = place.getLatLng();
            if (placeLatLng != null) {
                Log.e("MiddleActivity", "print now");
                Object obj =place.getId();
                Log.d("TAG", "Variable type: " + obj.getClass().getName());
               googleMap.addMarker(new MarkerOptions().position(placeLatLng).title(place.getName()));
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(placeLatLng, 10));
            }
        }).addOnFailureListener((exception) -> {
            Toast.makeText(this, "Failed to fetch place details: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        Log.d(TAG, "Google Maps API called successfully!");
        this.googleMap = googleMap;
        // map loaded verify
        if (googleMap != null) {

            // add friends and suggested location info
            List<Friend> selectedFriends = getIntent().getParcelableArrayListExtra("selected_friends");
            /*
            List<Location> locations = getSampleLocations();
            for (Location location : locations) {
                addLocationMarker(location);
            }
            */
            averlogitute = 0;
            averlatitute =0;
            fri_num=0;
            if (selectedFriends != null && !selectedFriends.isEmpty()) {
                for (Friend friend : selectedFriends) {
                    addFriendMarker(friend);
                    averlogitute = averlogitute+friend.longitude;
                    averlatitute =averlatitute +friend.latitude;
                    fri_num=fri_num+1;
                    System.out.println("current position is:"+friend.longitude+friend.latitude);
                    navlogitute=friend.longitude;
                    navlatitude=friend.latitude;
                }
                averLatlng = new LatLng(averlatitute/fri_num, averlogitute/fri_num);
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(averLatlng, 5));
                // set listener to display details
                googleMap.setOnMarkerClickListener(marker -> {
                    marker.showInfoWindow();
                    return true;
                });

                // set infoWindow click event
                googleMap.setOnInfoWindowClickListener(marker -> {
                });
                }
                else{
                    Log.e(TAG, "No Friend selected!");
                };
            }
                else{
                    Log.e(TAG, "GoogleMap is not ready yet!");
                };
            }

        private void addFriendMarker(Friend friend) {
            LatLng friendLocation = new LatLng(friend.latitude, friend.longitude);

            // load avatar if no avatar or load fail just skip
            Bitmap bitmap = BitmapFactory.decodeResource(getResources(), friend.avatarResourceId);
            if (bitmap != null) {
                Bitmap smallMarker = Bitmap.createScaledBitmap(bitmap, 100, 100, false);
                MarkerOptions markerOptions = new MarkerOptions()
                        .position(friendLocation)
                        .title(friend.name)
                        .snippet("Time at location: " + friend.timeAtLocation)
                        .icon(BitmapDescriptorFactory.fromBitmap(smallMarker));

                googleMap.addMarker(markerOptions);
            } else {
                Log.e(TAG, "Failed to load avatar for friend: " + friend.name);
                // no avatar or load fail
                MarkerOptions markerOptions = new MarkerOptions()
                        .position(friendLocation)
                        .title(friend.name)
                        .snippet("Time at location: " + friend.timeAtLocation);
                googleMap.addMarker(markerOptions);
            }
        }

    private void transferImagesToOtherActivity(ArrayList<byte[]> imagesByteArrayList) {
        Intent intent = new Intent(this, LocationDetailsActivity.class);
        intent.putExtra("images", imagesByteArrayList);
        startActivity(intent);
    }
/*
// add Location Marker
private void addLocationMarker(Location location) {
    LatLng locationLatLng = new LatLng(location.getLatitude(), location.getLongitude());
    Bitmap markerBitmap = createCustomMarker(this, String.valueOf(location.getId()));

    MarkerOptions markerOptions = new MarkerOptions()
            .position(locationLatLng)
            .title(location.getName())
            .icon(BitmapDescriptorFactory.fromBitmap(markerBitmap));

    // mark on map
    googleMap.addMarker(markerOptions);

}
// Sample location data
    private List<Location> getSampleLocations() {
        List<Location> locations = new ArrayList<>();
        locations.add(new Location(1, "Cafe Blue", "Best place to meet", 35.0522, -118.2440));
        locations.add(new Location(2, "Green Park", "Perfect for a walk", 33.0522, -118.2427));
        return locations;
    }

    //set customer marker for location
    private Bitmap createCustomMarker(Context context, String idText) {
        Bitmap markerBitmap = Bitmap.createBitmap(50, 50, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(markerBitmap);

        Paint backgroundPaint = new Paint();
        backgroundPaint.setColor(Color.BLUE);


        canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(), backgroundPaint);


        Paint textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(40);
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        textPaint.setAntiAlias(true);


        int xPos = (canvas.getWidth() / 2) - 20;
        int yPos = (int) ((canvas.getHeight() / 2) - ((textPaint.descent() + textPaint.ascent()) / 2));


        canvas.drawText(idText, xPos, yPos, textPaint);

        return markerBitmap;
    }
    */

    //Map lifecycle management
    @Override
    protected void onStart() {
        super.onStart();
        if (mapView != null) {
            mapView.onStart();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mapView != null) {
            mapView.onResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mapView != null) {
            mapView.onPause();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mapView != null) {
            mapView.onStop();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mapView != null) {
            mapView.onDestroy();
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }
}
