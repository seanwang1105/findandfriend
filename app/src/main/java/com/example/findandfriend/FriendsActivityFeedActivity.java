package com.example.findandfriend;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import java.util.ArrayList;
import com.example.findandfriend.ActivityFeed;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class FriendsActivityFeedActivity extends AppCompatActivity {

    private RecyclerView activityFeedRecyclerView;
    private Button btn_addfriend;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.friends_activity_feed);

        activityFeedRecyclerView = findViewById(R.id.activity_feed_list);
        activityFeedRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        btn_addfriend=findViewById(R.id.find_friend);

        // Adapter to display the activity feed
        ActivityFeedAdapter adapter = new ActivityFeedAdapter(getSampleFeedData());
        activityFeedRecyclerView.setAdapter(adapter);
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);

        btn_addfriend.setOnClickListener(v -> {
                // 创建Intent并传递选定的好友数据到MiddlePointActivity
                Intent intent = new Intent(FriendsActivityFeedActivity.this, SearchFriendActivity.class);
                startActivity(intent);

        });

        // set default menu to (Home)
        bottomNavigationView.setSelectedItemId(R.id.nav_home);

        // process BottomNavigationView click event
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int itemId = item.getItemId();

                if (itemId == R.id.nav_home) {
                    // return to main menu
                    Intent intent = new Intent(FriendsActivityFeedActivity.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    return true;

                } else if (itemId == R.id.nav_discover) {
                    // goto MiddlePointActivity
                    Intent intent = new Intent(FriendsActivityFeedActivity.this, MiddlePointActivity.class);
                    startActivity(intent);
                    return true;

                } else if (itemId == R.id.nav_friends) {
                    // goto FriendsActivityFeedActivity
                    return true;

                } else if (itemId == R.id.nav_profile) {
                    // goto ProfileActivity:
                    Intent intent = new Intent(FriendsActivityFeedActivity.this, ProfileActivity.class);
                    startActivity(intent);
                }

                return true;
            }
        });
    }

    // Sample data for the friends activity feed
    private List<ActivityFeed> getSampleFeedData() {
        List<ActivityFeed> feed = new ArrayList<>();
        feed.add(new ActivityFeed("John", "Visited Cafe Blue", "5 stars"));
        feed.add(new ActivityFeed("Anna", "Rated Green Park", "4 stars"));
        return feed;
    }
}
