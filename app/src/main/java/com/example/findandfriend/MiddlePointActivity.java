package com.example.findandfriend;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
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
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
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
import androidx.viewpager2.widget.ViewPager2;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
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

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MiddlePointActivity extends AppCompatActivity implements OnMapReadyCallback {

    private MapView mapView;
    private GoogleMap googleMap;
    private FriendAdapter friendAdapter;
    private Button mButton;
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
    private double meeting_request_longitude;
    private double meeting_request_latitude;
    private Bitmap placeImage;
    private boolean friend_meet_loaded =false;
    private String friend_meet_location;

    private int currentPage = 0;
    private static final String FILE_NAME = "user_credentials.txt";
    private String email="";

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.middle_point_activity);

        btMeetFloat=findViewById(R.id.fab_meet_here);
        mButton=findViewById(R.id.meet_button);
        // initial MapView
        mapView = findViewById(R.id.map_middle_point);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        int screenHeight = getResources().getDisplayMetrics().heightPixels;
        ViewGroup.LayoutParams params = mapView.getLayoutParams();
        params.height = screenHeight / 3;  // set to 1/3 of screen height
        mapView.setLayoutParams(params);

        String[] savedCredentials = loadCredentials();

        if (savedCredentials != null) {
            email = savedCredentials[0];
        }

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
        List<Friend> selectedFriends = SelectedFriendShareData.getInstance().getSelectedFriends();
        friendAdapter = new FriendAdapter(this, selectedFriends, position -> {
            // Handle friend deletion
            Friend deletedFriend = selectedFriends.get(position);
            selectedFriends.remove(position); // Remove from the list
            friendAdapter.notifyItemRemoved(position); // Notify the adapter
            Toast.makeText(MiddlePointActivity.this, deletedFriend.name + " deleted", Toast.LENGTH_SHORT).show();
        });
        System.out.println("receive selected friend :"+selectedFriends);
        // Go click event
        btMeetFloat.setOnClickListener(v -> {

            Intent intent = new Intent(MiddlePointActivity.this, LocationDetailsActivity.class);

                intent.putExtra("placeID","MID");
                intent.putExtra("placeName","Meet in Middle");
                intent.putExtra("placeRating","0.0");
                intent.putExtra("placeLatitude",averlatitute);
                intent.putExtra("placeLongitude",averlogitute);
                intent.putExtra("placeAddress","Load as middle point");
            startActivity(intent);
        });
        // get friends data from MainActivity


        // Initialize RecyclerView
        ViewPager2 viewPager = findViewById(R.id.view_pager);

        //searchResultsList.setLayoutManager(new LinearLayoutManager(this));
        searchResultsAdapter = new SearchResultsAdapter(predictionList, prediction -> {
            // When an item is clicked, fetch more details about the place
            fetchPlaceDetails(prediction.getPlaceId());
        });

        List<MeetingFriend>meetingFriends = new ArrayList<>();
// Step 1: Fetch Meeting Statuses from Server
        FriendMeetingAdapter friendMeetingAdapter = new FriendMeetingAdapter(this,meetingFriends, new FriendMeetingAdapter.OnItemClickListener() {
            @Override
                public void onItemClick(MeetingFriend friendMeet) {
                    // Show friendMeet on the map
                    friend_meet_loaded = true;
                    friend_meet_location = friendMeet.getLocationName();
                    meeting_request_longitude = friendMeet.getLongitude();
                    meeting_request_latitude = friendMeet.getLatitude();
                    LatLng friendLatLng = new LatLng(friendMeet.getLatitude(), friendMeet.getLongitude());

                    googleMap.clear();  // Clear existing markers
                    googleMap.addMarker(new MarkerOptions().position(friendLatLng).title(friendMeet.getSenderEmail()));
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(friendLatLng, 12));
                }

                @Override
                public void onAccept(MeetingFriend friendMeet) {
                    Toast.makeText(MiddlePointActivity.this, "Accepted: " + friendMeet.getSenderEmail(), Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onReject(MeetingFriend friendMeet) {
                    Toast.makeText(MiddlePointActivity.this, "Rejected: " + friendMeet.getSenderEmail(), Toast.LENGTH_SHORT).show();
                }
                @Override
               public void onDelete(MeetingFriend friendMeet) {
                    Toast.makeText(MiddlePointActivity.this, "Deleted: " + friendMeet.getSenderEmail(), Toast.LENGTH_SHORT).show();
               }
            });

            // Step 3: Update the ViewPager or RecyclerView with the Adapter
// Set the updated adapter to ViewPager



// Initialize ViewPager2 Adapter
        ViewPagerAdapter viewPagerAdapter = new ViewPagerAdapter(searchResultsAdapter, friendMeetingAdapter);
        viewPager.setAdapter(viewPagerAdapter);

// Listen for page changes
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                currentPage = position; // Update the current page index
            }
        });
        // fetch the meeting request from server
        fetchMeetingStatuses(email, new MeetingStatusCallback() {
            @Override
            public void onSuccess(List<MeetingFriend> meetingFriends) {
                if (!meetingFriends.isEmpty()) {
                    // Update the FriendMeetingAdapter with the fetched data
                    friendMeetingAdapter.updateMeetingFriends(meetingFriends);

                    // Notify the ViewPagerAdapter of data changes
                    viewPagerAdapter.notifyAllPagesChanged();
                } else {
                    // Handle empty meetingFriends list
                    Toast.makeText(MiddlePointActivity.this, "No meeting friends found", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String errorMessage) {
                // Log error and show empty list message
                Toast.makeText(MiddlePointActivity.this, "Error loading meeting statuses", Toast.LENGTH_SHORT).show();
            }
        });
        //initialize button to transfer message:
        mButton.setOnClickListener(v->{
            Intent intent = new Intent(MiddlePointActivity.this, LocationDetailsActivity.class);
            if (currentPage == 0) {
                // Currently viewing SearchResultAdapter
                Log.d("ButtonClick", "SearchResultAdapter is active");
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
                    intent.putExtra("placeID","MID");
                    intent.putExtra("placeName","Meet in Middle");
                    intent.putExtra("placeRating","0.0");
                    intent.putExtra("placeLatitude",averlatitute);
                    intent.putExtra("placeLongitude",averlogitute);
                    intent.putExtra("placeAddress","Load as middle point");
                }
                startActivity(intent);
            } else if (currentPage == 1) {
                // Currently viewing MeetRequestAdapter
                Log.d("ButtonClick", "MeetRequestAdapter is active");
                if (friend_meet_loaded) {
                    intent.putExtra("placeID", "No ID");
                    intent.putExtra("placeName", friend_meet_location);
                    intent.putExtra("placeRating", "No rating");
                    intent.putExtra("placeLatitude",meeting_request_latitude);
                    intent.putExtra("placeLongitude", meeting_request_longitude);
                    intent.putExtra("placeAddress", "");
                }
                else{
                    System.out.println("placeid from middle:" + place.getId());
                    intent.putExtra("placeID","MID");
                    intent.putExtra("placeName","Meet in Middle");
                    intent.putExtra("placeRating","0.0");
                    intent.putExtra("placeLatitude",averlatitute);
                    intent.putExtra("placeLongitude",averlogitute);
                    intent.putExtra("placeAddress","Load as middle point");
                }
                friend_meet_loaded=false;
                startActivity(intent);
            } else {
                Log.d("ButtonClick", "Unknown page");
            }
                    });
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
            List<Friend> selectedFriends = SelectedFriendShareData.getInstance().getSelectedFriends();
            System.out.println("call from mapready call back");
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

    // get friend request from server:
    // Fetch meeting requests from the server (replace with actual server logic)
    private void fetchMeetingStatuses(String email, MeetingStatusCallback callback) {
        fetchMeetingStatusesFromServer(email, new MeetingStatusCallback() {
            @Override
            public void onSuccess(List<MeetingFriend> meetingFriends) {
                // Return the list of MeetingFriends to the callback
                callback.onSuccess(meetingFriends);
            }

            @Override
            public void onError(String errorMessage) {
                // Return an empty list on error
                callback.onSuccess(new ArrayList<>()); // Return empty list
                // Optionally show the error to the user
                Toast.makeText(MiddlePointActivity.this, "Error: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchMeetingStatusesFromServer(String email, MeetingStatusCallback callback) {
        String url = getString(R.string.IP) +"/meeting_status?email=" + email; // Replace with your server's URL
        SharedPreferences sharedPreferences = getSharedPreferences("auth", MODE_PRIVATE);
        String token = sharedPreferences.getString("token", null);
        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        List<MeetingFriend> meetingFriends = new ArrayList<>();

                        // Parse response array
                        for (int i = 0; i < response.length(); i++) {
                            JSONObject meetingObj = response.getJSONObject(i);

                            int meetingId = meetingObj.getInt("meeting_id");
                            JSONObject meetingDetails = meetingObj.getJSONObject("meeting_details");

                            String senderEmail = meetingDetails.getString("sender_email");
                            String locationName = meetingDetails.getString("location_name");
                            double locationLatitude = meetingDetails.getDouble("location_latitude");
                            double locationLongitude = meetingDetails.getDouble("location_longitude");
                            String statusSummary = meetingObj.getString("status_summary");

                            // Add to the meetingFriends list
                            MeetingFriend meetingFriend = new MeetingFriend(meetingId,
                                    senderEmail,
                                    locationName,
                                    locationLatitude,
                                    locationLongitude,
                                    statusSummary
                            );
                            meetingFriends.add(meetingFriend);
                        }

                        // Pass the list back to the callback
                        callback.onSuccess(meetingFriends);
                    } catch (JSONException e) {
                        e.printStackTrace();
                        callback.onError("Failed to parse meeting status response");
                    }
                },
                error -> {
                    // Handle Volley error
                    callback.onError("Failed to fetch meeting statuses: " + error.getMessage());
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
        RequestQueue requestQueue = Volley.newRequestQueue(this); // Replace 'context' with your Activity or Application Context
        requestQueue.add(request);
    }

    public interface MeetingStatusCallback {
        void onSuccess(List<MeetingFriend> meetingFriends);
        void onError(String errorMessage);
    }
    //load user email:
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
