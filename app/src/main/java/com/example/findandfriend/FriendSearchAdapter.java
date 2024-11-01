// FriendSearchAdapter.java
package com.example.findandfriend;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Handler;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FriendSearchAdapter extends RecyclerView.Adapter<FriendSearchAdapter.ViewHolder> {

    private Context context;
    private List<Friend> friends;

    public FriendSearchAdapter(Context context, List<Friend> friends) {
        this.context = context;
        this.friends = friends;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView nameTextView;
        public Button addButton;

        public ViewHolder(View view) {
            super(view);
            nameTextView = view.findViewById(R.id.textview_friend_name);
            addButton = view.findViewById(R.id.button_add_friend);
        }
    }

    @NonNull
    @Override
    public FriendSearchAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_search_result_friend, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FriendSearchAdapter.ViewHolder holder, int position) {
        Friend friend = friends.get(position);
        System.out.println("friendsearchadpter friend name is:"+friend.name);
        holder.nameTextView.setText(friend.name);

        holder.addButton.setOnClickListener(v -> {
            // Send friend request to server for confirmation
            sendFriendRequest(friend);
        });
    }

    @Override
    public int getItemCount() {
        return friends.size();
    }

    private void sendFriendRequest(Friend friend) {
        // Replace with your server's friend request API URL
        String url = "https://yourserver.com/api/send_friend_request";

        RequestQueue queue = Volley.newRequestQueue(context);

        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                response -> {
                    // Handle success
                    Toast.makeText(context, "Friend request sent to " + friend.name, Toast.LENGTH_SHORT).show();

                    // Simulate confirmation for the purpose of this example
                    simulateFriendConfirmation(friend);

                },
                error -> {
                    // Handle error
                    simulateFriendConfirmation(friend);
                    Toast.makeText(context, "Error sending friend request", Toast.LENGTH_SHORT).show();
                }) {
            @Override
            protected Map<String, String> getParams() {
                // Send friend ID in the request body
                Map<String, String> params = new HashMap<>();
                params.put("friend_id", friend.id);
                return params;
            }
        };

        queue.add(stringRequest);
    }

    private void simulateFriendConfirmation(Friend friend) {
        // Simulate friend confirming the request after some time
        new Handler().postDelayed(() -> {
            // Add friend to local JSON file
            try {
                addFriendToLocal(friend);
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }

            Toast.makeText(context, friend.name + " has accepted your friend request!", Toast.LENGTH_SHORT).show();
        }, 5000); // simulate 5 seconds delay
    }

    private void addFriendToLocal(Friend friend) throws JSONException {
        try {
            FileInputStream fis = context.openFileInput("friends.json");
            StringBuilder sb = new StringBuilder();
            int ch;
            while((ch = fis.read()) != -1) {
                sb.append((char) ch);
            }
            fis.close();
            JSONArray friendsArray = new JSONArray(sb.toString());
            System.out.println("friend in Json files are:"+friendsArray);
            // Check if friend already exists
            for (int i = 0; i < friendsArray.length(); i++) {
                JSONObject obj = friendsArray.getJSONObject(i);
                if (obj.getString("id").equals(friend.id)) {
                    // Friend already exists
                    return;
                }
            }

            JSONObject friendObject = new JSONObject();
            friendObject.put("id", friend.id);
            friendObject.put("name", friend.name);
            friendObject.put("latitude", friend.latitude);
            friendObject.put("longitude", friend.longitude);
            friendObject.put("timeAtLocation", friend.timeAtLocation);
            friendsArray.put(friendObject);

            FileOutputStream fos = context.openFileOutput("friends.json", Context.MODE_PRIVATE);
            fos.write(friendsArray.toString().getBytes());
            fos.close();
        } catch (FileNotFoundException e) {
            // File doesn't exist, create new
            JSONArray friendsArray = new JSONArray();
            JSONObject friendObject = new JSONObject();
            friendObject.put("id", friend.id);
            friendObject.put("name", friend.name);
            friendObject.put("latitude", friend.latitude);
            friendObject.put("longitude", friend.longitude);
            friendObject.put("timeAtLocation", friend.timeAtLocation);
            friendsArray.put(friendObject);

            try {
                FileOutputStream fos = context.openFileOutput("friends.json", Context.MODE_PRIVATE);
                fos.write(friendsArray.toString().getBytes());
                fos.close();
            } catch (Exception ex) {
                System.out.println("write file to friend json error new file");
                ex.printStackTrace();
            }
        } catch (Exception e) {
            System.out.println("write file to friend json error");
            e.printStackTrace();
        }
    }
}

