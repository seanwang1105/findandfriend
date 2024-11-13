package com.example.findandfriend;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.content.Intent;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.view.ViewGroup;
import android.widget.Toast;
import android.util.Log;
import android.location.Location;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.tasks.OnSuccessListener;

import com.example.findandfriend.Friend;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private MapView mapView;
    private GoogleMap googleMap;
    private LinearLayout mainOriginalContent;  // initialize LinearLayout
    private Button btnZoomIn, btnZoomOut;
    private RecyclerView friendListView;
    private FriendAdapter friendAdapter;
    private Button btnGo;
    private FloatingActionButton btGoFloat;
    private static final String TAG = "MapsActivity";
    private FusedLocationProviderClient fusedLocationClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
    private KalmanLatLong kalmanFilter = new KalmanLatLong(3);
    private TextView locationTextView;
    private static final int REQUEST_LOCATION_PERMISSION = 1;
    private double currentlogitute;
    private double currentlatitute;
    private BottomNavigationView bottomNavigationView;
    private Marker myselfMarker;
    private int invalidLocationCount = 0;
    private boolean updateflag;

    private static final double EARTH_RADIUS = 3959.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        updateflag=false;

        // Initialize MapView
        mapView = findViewById(R.id.map);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        friendListView = findViewById(R.id.friend_list);
        btnGo = findViewById(R.id.btn_go);
        btGoFloat = findViewById(R.id.fab_create_activity);
        bottomNavigationView = findViewById(R.id.bottom_navigation);


        // Initially set views to GONE or disabled
        mapView.setVisibility(View.GONE);
        friendListView.setVisibility(View.GONE);
        btnGo.setEnabled(false);
        btGoFloat.setEnabled(false);
        bottomNavigationView.getMenu().findItem(R.id.nav_discover).setEnabled(false);

        checkSensorAvailability();
        // Define location request settings
        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(5000); // Update every 5 seconds
        locationRequest.setFastestInterval(2000); // Fastest possible update interval
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        /*
        // Request location permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            requestLocationUpdates();
        } else {
            // Request the location permission at runtime
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
        }
        */

        // Define location callback
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (locationResult == null || locationResult.getLocations().isEmpty()) {
                    // data missing error
                    Log.e(TAG, "Location data is missing or unavailable.");
                    handleLocationFailure();
                    return;
                }
                System.out.println(locationResult);
                for (Location location : locationResult.getLocations()) {
                    // get location information
                    if (location == null || location.getAccuracy() < 0 || location.getLatitude() == 0.0 || location.getLongitude() == 0.0) {
                        // handle error
                        Log.e(TAG, "Received invalid location data.");
                        handleInvalidLocationData();
                        continue; // skip error data
                    }

                    double latitude = location.getLatitude();
                    double longitude = location.getLongitude();
                    float accuracy = location.getAccuracy();
                    long timeStamp = location.getTime();

                    // use kalmanfilter
                    kalmanFilter.process(latitude, longitude, accuracy, timeStamp);
                    currentlatitute = kalmanFilter.getLat();
                    currentlogitute = kalmanFilter.getLng();

                    // add marker on map
                    if (googleMap != null) {
                        LatLng currentLatLng = new LatLng(currentlatitute, currentlogitute);
                        runOnUiThread(() -> {
                            if (myselfMarker == null) {
                                // create new marker
                                MarkerOptions markerOptions = new MarkerOptions()
                                        .position(currentLatLng)
                                        .title("My Location")
                                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
                                myselfMarker = googleMap.addMarker(markerOptions);
                                // move camera to current position
                                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 10));
                            } else {
                                // update marker
                                myselfMarker.setPosition(currentLatLng);
                            }
                        });
                    }
                    List<Friend> friends = getSampleFriends(currentlatitute,currentlogitute);
                    if (!updateflag && !friends.isEmpty()) {
                        friendAdapter.updateFriendDistances(friends);;
                        friendAdapter.notifyDataSetChanged();
                        //updateflag=true;
                    }
                }
            }
        };

        requestLocationUpdates();

        // Go click event
        btnGo.setOnClickListener(v -> {
            List<Friend> selectedFriends = friendAdapter.getSelectedFriends();
            selectedFriends.add(new Friend("me","Me", currentlatitute, currentlogitute, "0 min", R.drawable.ic_friend_avatar1));
            if (!selectedFriends.isEmpty()) {
                Intent intent = new Intent(MainActivity.this, MiddlePointActivity.class);
                intent.putParcelableArrayListExtra("selected_friends", new ArrayList<>(selectedFriends));
                startActivity(intent);
            }
        });

        // floating click event
        btGoFloat.setOnClickListener(v -> {
            List<Friend> selectedFriends = friendAdapter.getSelectedFriends();

            if (!selectedFriends.isEmpty()) {
                Intent intent = new Intent(MainActivity.this, MiddlePointActivity.class);
                intent.putParcelableArrayListExtra("selected_friends", new ArrayList<>(selectedFriends));
                startActivity(intent);
            }
        });

        int screenHeight = getResources().getDisplayMetrics().heightPixels;
        ViewGroup.LayoutParams params = mapView.getLayoutParams();
        params.height = screenHeight / 3;  // set 1/3 of screen height
        mapView.setLayoutParams(params);

        btnZoomIn = findViewById(R.id.btn_zoom_in);
        btnZoomOut = findViewById(R.id.btn_zoom_out);


        // set zoom in and out event
        btnZoomIn.setOnClickListener(v -> {
            if (googleMap != null) {
                googleMap.animateCamera(CameraUpdateFactory.zoomIn());
            }
        });

        btnZoomOut.setOnClickListener(v -> {
            if (googleMap != null) {
                googleMap.animateCamera(CameraUpdateFactory.zoomOut());
            }
        });
        // initialize friend list
        List<Friend> friends = getSampleFriends(currentlatitute,currentlogitute);
        friendAdapter = new FriendAdapter(this, friends);

        // set RecyclerView
        friendListView.setLayoutManager(new LinearLayoutManager(this));
        friendListView.setAdapter(friendAdapter);

         // default to Home layout
        bottomNavigationView.setSelectedItemId(R.id.nav_home);
        // handle BottomNavigationView menu click event
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                Fragment selectedFragment = null;

                int itemId = item.getItemId();
                if (itemId == R.id.nav_home) {
                   return true;

                } else if (itemId == R.id.nav_discover) {
                    // goto MiddlePointActivity
                    Intent intent = new Intent(MainActivity.this, MiddlePointActivity.class);
                    startActivity(intent);
                    return true;

                } else if (itemId == R.id.nav_friends) {
                    // goto FriendsActivityFeedActivity
                    Intent intent = new Intent(MainActivity.this, FriendsActivityFeedActivity.class);
                    startActivity(intent);
                    return true;
                } else if (itemId == R.id.nav_profile) {
                    // goto FriendsActivityFeedActivity
                    Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
                    startActivity(intent);
                    return true;
                }
                return true;
            }
        });

    }


    private void requestLocationUpdates() {
        if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
        } else {
            // Request permissions
            requestPermissions(new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 100);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableLocationDependentFeatures();
                requestLocationUpdates();
            } else {
                disableLocationDependentFeatures();
                Toast.makeText(this, "Location permission is required for this feature.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void disableLocationDependentFeatures() {
        // Disable UI elements if permission is denied
        if (mapView != null) {
            mapView.setVisibility(View.GONE);
        }
        if (friendListView != null) {
            friendListView.setVisibility(View.GONE);
        }
        if (btnGo != null) {
            btnGo.setEnabled(false);
        }
        if (btGoFloat != null) {
            btGoFloat.setEnabled(false);
        }
        if (bottomNavigationView != null) {
            bottomNavigationView.getMenu().findItem(R.id.nav_discover).setEnabled(false);
        }
    }
    private void enableLocationDependentFeatures() {
        // Make the MapView and other UI components visible or enabled
        if (mapView != null) {
            mapView.setVisibility(View.VISIBLE);
        }
        if (friendListView != null) {
            friendListView.setVisibility(View.VISIBLE);
        }
        if (btnGo != null) {
            btnGo.setEnabled(true);
        }
        if (btGoFloat != null) {
            btGoFloat.setEnabled(true);
        }
        if (bottomNavigationView != null) {
            bottomNavigationView.getMenu().findItem(R.id.nav_discover).setEnabled(true);
        };
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        Log.d(TAG, "Google Maps API called successfully!");
        this.googleMap = googleMap;
        // verify map loaded
        if (googleMap != null) {
            // add friend info
            List<Friend> friends = getSampleFriends(currentlatitute,currentlogitute);
            for (Friend friend : friends) {
                addFriendMarker(friend);
            }

            // set marker listener, click for details
            googleMap.setOnMarkerClickListener(marker -> {
                marker.showInfoWindow();  // display details
                return true;
            });

            // set InfoWindow event
            googleMap.setOnInfoWindowClickListener(marker -> {
                //display details
            });
        } else {
            Log.e(TAG, "GoogleMap is not ready yet!");
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState); // Save map state
    }

    private void addFriendMarker(Friend friend) {
        LatLng friendLocation = new LatLng(friend.latitude, friend.longitude);
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
            MarkerOptions markerOptions = new MarkerOptions()
                    .position(friendLocation)
                    .title(friend.name)
                    .snippet("Time at location: " + friend.timeAtLocation);
            googleMap.addMarker(markerOptions);
        }
    }

    private void handleLocationFailure() {
        Toast.makeText(MainActivity.this, "Unable to get location. Please check GPS and network settings.", Toast.LENGTH_LONG).show();

        disableLocationDependentFeatures();

        LatLng defaultLatLng = new LatLng(0, 0);
        if (googleMap != null) {
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLatLng, 5));
        }

        requestLocationUpdates();
    }

    private void handleInvalidLocationData() {
        Toast.makeText(MainActivity.this, "Received invalid location data. Please check your sensor.", Toast.LENGTH_LONG).show();

        invalidLocationCount++;
        Log.e(TAG, "Invalid sensor data detected.");

          if (invalidLocationCount >= 3) {
            Toast.makeText(MainActivity.this, "Multiple invalid location readings. Please check your sensor.", Toast.LENGTH_LONG).show();
            invalidLocationCount = 0;
        }
    }

    private void checkSensorAvailability() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);


        boolean isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        if (!isGpsEnabled) {
            Toast.makeText(this, "GPS is disabled. Please enable it for better location accuracy.", Toast.LENGTH_LONG).show();
            Log.e(TAG, "GPS is not available.");
        }


        boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        if (!isNetworkEnabled) {
            Toast.makeText(this, "Network location is disabled. Please enable it.", Toast.LENGTH_LONG).show();
            Log.e(TAG, "Network location is not available.");
        }
    }

    private List<Friend> getSampleFriends(double mylatitute,double mylogitute) {
        List<Friend> friends = new ArrayList<>();

        try {
            FileInputStream fis = openFileInput("friends.json");
            StringBuilder sb = new StringBuilder();
            int ch;
            while((ch = fis.read()) != -1) {
                sb.append((char) ch);
            }
            fis.close();

            JSONArray friendsArray = new JSONArray(sb.toString());
            System.out.println("read friend array is:"+friendsArray);
            for (int i = 0; i < friendsArray.length(); i++) {
                JSONObject friendObject = friendsArray.getJSONObject(i);
                String id = friendObject.getString("id");
                String name = friendObject.getString("name");
                double latitude = friendObject.getDouble("latitude");
                double longitude = friendObject.getDouble("longitude");
                String timeAtLocation = friendObject.getString("timeAtLocation");
                int avatarResourceId = R.drawable.ic_friend_avatar1; // default avatar
                System.out.println("myself position to calculate is:"+ mylatitute + mylogitute);
                String disAtlocation = String.format("%.0f",haversine(latitude,longitude,mylatitute,mylogitute));
                disAtlocation=disAtlocation+"miles";
                Friend friend = new Friend(id, name, latitude, longitude, disAtlocation, avatarResourceId);
                friends.add(friend);
            }
        } catch (FileNotFoundException e) {
            // File not found, return empty list or default friends
            System.out.println("friend.json is not found");
        } catch (Exception e) {
            e.printStackTrace();
        }

        return friends;
    }

    // Calculate distance between user and friend
    private double haversine(double lat1, double lon1, double lat2, double lon2) {
            // Convert latitude and longitude from degrees to radians
            lat1 = Math.toRadians(lat1);
            lon1 = Math.toRadians(lon1);
            lat2 = Math.toRadians(lat2);
            lon2 = Math.toRadians(lon2);

            // Differences in latitudes and longitudes
            double dLat = lat2 - lat1;
            double dLon = lon2 - lon1;

            // Haversine formula
            double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                    Math.cos(lat1) * Math.cos(lat2) *
                            Math.sin(dLon / 2) * Math.sin(dLon / 2);
            double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

            // Calculate the distance in miles
            return EARTH_RADIUS * c;
        }

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
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            enableLocationDependentFeatures();
            requestLocationUpdates();
        } else {
            disableLocationDependentFeatures();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mapView != null) {
            mapView.onPause();
        }
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mapView != null) {
            mapView.onStop();
        }
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        fusedLocationClient.removeLocationUpdates(locationCallback);
        mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (mapView != null) {
            mapView.onLowMemory();
        }
    }
}
