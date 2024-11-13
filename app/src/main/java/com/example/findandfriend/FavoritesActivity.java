package com.example.findandfriend;


import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FavoritesActivity extends AppCompatActivity {

    private ListView listViewFavorites;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorites);

        // Create an action bar with a return arrow
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);  // Enable return arrow
            actionBar.setTitle("My Favorite Places");
        }

        listViewFavorites = findViewById(R.id.list_view_favorites);

        // Load and display favorite merchant information
        List<String> favoritePlaces = loadFavoritePlaces();
        if (favoritePlaces.isEmpty()) {
            Toast.makeText(this, "No favorites saved.", Toast.LENGTH_SHORT).show();
        } else {
            // Use ArrayAdapter to display favorite merchant information
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, favoritePlaces);
            listViewFavorites.setAdapter(adapter);
        }
    }

    // On return arrow click, load the ProfileActivity
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            // Return to the previous Activity（ProfileActivity）
            Intent intent = new Intent(FavoritesActivity.this, ProfileActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // Load locally saved merchant information
    private List<String> loadFavoritePlaces() {
        String filename = "saved_places.json";
        List<String> favoritePlaces = new ArrayList<>();

        try {
            // Open Local file
            FileInputStream fis = openFileInput(filename);
            int size = fis.available();
            byte[] buffer = new byte[size];
            fis.read(buffer);
            fis.close();

            // Convert file buffer to strings
            String jsonString = new String(buffer, "UTF-8");

            // Parse JSON Files
            JSONArray savedPlacesArray = new JSONArray(jsonString);
            System.out.println("read content in favorite");
            System.out.println(savedPlacesArray);
            // JSONify all saved places and add to favorites
            for (int i = 0; i < savedPlacesArray.length(); i++) {
                JSONObject place = savedPlacesArray.getJSONObject(i);
                String name = place.getString("name");
                String address = place.getString("address");
                if (address == null){
                    address="N/A";
                }
                // Format merchant name and address and add it to FavoritePlaces
                String placeDetails = "Name: " + name + "\nAddress: " + address;
                favoritePlaces.add(placeDetails);
            }

        } catch (IOException | JSONException e) {
            Toast.makeText(this, "Failed to load favorites.", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }

        return favoritePlaces;
    }
}
