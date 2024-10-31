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

        // 设置带返回箭头的 ActionBar
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);  // 启用返回箭头
            actionBar.setTitle("My Favorite Places");
        }

        listViewFavorites = findViewById(R.id.list_view_favorites);

        // 加载并显示收藏的商户信息
        List<String> favoritePlaces = loadFavoritePlaces();
        if (favoritePlaces.isEmpty()) {
            Toast.makeText(this, "No favorites saved.", Toast.LENGTH_SHORT).show();
        } else {
            // 使用 ArrayAdapter 将收藏的商户信息显示在 ListView 中
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, favoritePlaces);
            listViewFavorites.setAdapter(adapter);
        }
    }

    // 返回箭头功能实现，点击返回箭头回到 ProfileActivity
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            // 返回上一个 Activity（ProfileActivity）
            Intent intent = new Intent(FavoritesActivity.this, ProfileActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // 加载本地保存的收藏商户信息
    private List<String> loadFavoritePlaces() {
        String filename = "saved_places.json";
        List<String> favoritePlaces = new ArrayList<>();

        try {
            // 打开本地文件
            FileInputStream fis = openFileInput(filename);
            int size = fis.available();
            byte[] buffer = new byte[size];
            fis.read(buffer);
            fis.close();

            // 将文件内容转换为字符串
            String jsonString = new String(buffer, "UTF-8");

            // 解析 JSON 文件
            JSONArray savedPlacesArray = new JSONArray(jsonString);
            System.out.println("read content in favorite");
            System.out.println(savedPlacesArray);
            // 提取商户名称和地址
            for (int i = 0; i < savedPlacesArray.length(); i++) {
                JSONObject place = savedPlacesArray.getJSONObject(i);
                String name = place.getString("name");
                String address = place.getString("address");
                if (address == null){
                    address="no address";
                }
                // 拼接商户名称和地址
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
