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

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle("My Favorite Places");
        }

        listViewFavorites = findViewById(R.id.list_view_favorites);

        List<String> favoritePlaces = loadFavoritePlaces();
        if (favoritePlaces.isEmpty()) {
            Toast.makeText(this, "No favorites saved.", Toast.LENGTH_SHORT).show();
        } else {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, favoritePlaces);
            listViewFavorites.setAdapter(adapter);
        }
    }


    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            Intent intent = new Intent(FavoritesActivity.this, ProfileActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private List<String> loadFavoritePlaces() {
        String filename = "saved_places.json";
        List<String> favoritePlaces = new ArrayList<>();

        try {
            FileInputStream fis = openFileInput(filename);
            int size = fis.available();
            byte[] buffer = new byte[size];
            fis.read(buffer);
            fis.close();

            String jsonString = new String(buffer, "UTF-8");

            JSONArray savedPlacesArray = new JSONArray(jsonString);
            System.out.println("read content in favorite");
            System.out.println(savedPlacesArray);
            for (int i = 0; i < savedPlacesArray.length(); i++) {
                JSONObject place = savedPlacesArray.getJSONObject(i);
                String name = place.getString("name");
                String address = place.getString("address");
                if (address == null){
                    address="no address";
                }
                String placeDetails = "Name: " + name + "\nAddress: " + address;
                favoritePlaces.add(placeDetails);
            }

        } catch (IOException | JSONException e) {
            Toast.makeText(this, "Failed to load favorites,may still in downloading", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }

        return favoritePlaces;
    }
}
