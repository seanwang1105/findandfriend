// FriendSearchAdapter.java
package com.example.findandfriend;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Handler;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FriendSearchAdapter extends RecyclerView.Adapter<FriendSearchAdapter.ViewHolder> {

    private Context context;
    private List<Friend> friends;
    private final String SERVER_URL;

    private static final String FILE_NAME = "user_credentials.txt";

    public FriendSearchAdapter(Context context, List<Friend> friends) {
        this.context = context;
        this.friends = friends;
        this.SERVER_URL = context.getString(R.string.IP) + "/send_friend_request";
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
        holder.nameTextView.setText(friend.name + "->"+friend.email);

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
<<<<<<< HEAD
=======
        String url = context.getString(R.string.IP)  + "/send_friend_request";
>>>>>>> b9aee5775f8170ec6926ffe8074d78c51a09567a

        String[] savedCredentials = loadCredentials();
        String email = "";
        if (savedCredentials != null) {
            email = savedCredentials[0];
        }
        JSONObject FrData = new JSONObject();
        try {
            FrData.put("from_email",email);
            FrData.put("to_email", friend.email);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        SharedPreferences sharedPreferences = context.getSharedPreferences("auth", Context.MODE_PRIVATE);
        String token = sharedPreferences.getString("token", null);
        if (token == null) {
            Toast.makeText(context, "Authorization token is missing. Please log in again.", Toast.LENGTH_SHORT).show();
            return;
        }

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, SERVER_URL,FrData,
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
            // Load the token from SharedPreferences
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                headers.put("x-access-token", token); // Add the token to headers
                return headers;
            }

        };

        jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(
                20000, // Initial timeout in ms (e.g., 10 seconds)
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES, // Retry count
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        // add to request quene
        RequestQueue requestQueue = Volley.newRequestQueue(context);
        requestQueue.add(jsonObjectRequest);
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
    private String[] loadCredentials() {
        FileInputStream fis = null;
        try {
            fis = context.openFileInput(FILE_NAME);
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
            friendObject.put("email",friend.email);
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

