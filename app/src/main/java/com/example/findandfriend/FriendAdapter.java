package com.example.findandfriend;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

public class FriendAdapter extends RecyclerView.Adapter<FriendAdapter.FriendViewHolder> {

    private Context context;
    private List<Friend> friendList;  // define private Friend class
    private List<Friend> selectedFriends = new ArrayList<>();
    private static final String FILE_NAME = "user_credentials.txt";
    private String email;
    private final OnFriendDeleteListener deleteListener;
    public FriendAdapter(Context context, List<Friend> friendList,OnFriendDeleteListener deleteListener) {
        this.context = context;
        this.friendList = friendList;
        this.deleteListener = deleteListener;
    }

    public void updateFriends(List<Friend> newFriends) {
        this.friendList.clear();
        this.friendList.addAll(newFriends);
        notifyDataSetChanged();
    }

    public void updateFriendDistances(List<Friend> updatedFriends) {
        for (Friend updatedFriend : updatedFriends) {
            for (Friend friend : friendList) {
                if (friend.id.equals(updatedFriend.id)) {  // Assuming each friend has a unique 'id'
                    friend.timeAtLocation = updatedFriend.timeAtLocation;  // Update only the distance/time field
                    break;
                }
            }
        }
        notifyDataSetChanged();  // Notify that only distance data has changed
    }

    @NonNull
    @Override
    public FriendViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.friend_list_item, parent, false);
        return new FriendViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FriendViewHolder holder, int position) {
        Friend friend = friendList.get(position);
        System.out.println("friend name is "+friend.name+friend.email);
        if (friend.name == "null") {
            System.out.println("friend name is null ");
            holder.friendName.setText(friend.email);
        }
        else {
            holder.friendName.setText(friend.name);
        }
        holder.timeds.setText(friend.timeAtLocation);
        holder.checkBox.setChecked(selectedFriends.contains(friend));

        holder.checkBox.setOnClickListener(v -> {
            if (holder.checkBox.isChecked()) {
                System.out.println("added friend is :"+friend);
                selectedFriends.add(friend);
            } else {
                selectedFriends.remove(friend);
            }
        });

        holder.deleteButton.setOnClickListener(v -> {
            int adapterPosition = holder.getAdapterPosition();
            if (adapterPosition != RecyclerView.NO_POSITION) {
                if (deleteListener != null) {
                    deleteListener.onFriendDelete(adapterPosition);
                }
                sendDeleteRequest(friend, adapterPosition);
            }
        });
    }

    @Override
    public int getItemCount() {
        return friendList.size();
    }

    private void sendDeleteRequest(Friend friend, int position) {
        String url = context.getString(R.string.IP) + "/delete_friend";
        RequestQueue queue = Volley.newRequestQueue(context);
        SharedPreferences sharedPreferences = context.getSharedPreferences("auth", Context.MODE_PRIVATE);
        String token = sharedPreferences.getString("token", null);
        String[] savedCredentials = loadCredentials();
        if (savedCredentials != null) {
            email = savedCredentials[0];
        }
        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("sender_email", email);
            requestBody.put("friend_email", friend.email);
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url, requestBody,
                response -> {
                    Toast.makeText(context, "Friend deleted successfully", Toast.LENGTH_SHORT).show();
                    removeFriend(position);
                },
                error -> Toast.makeText(context, "Failed to delete friend", Toast.LENGTH_SHORT).show()
        ) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                headers.put("x-access-token", token); // Add the token to headers
                return headers;
            }
        };

        queue.add(jsonObjectRequest);
    }
    public List<Friend> getSelectedFriends() {
        return selectedFriends;
    }
    public void removeFriend(int position) {
        if (position >= 0 && position < friendList.size()) {
            friendList.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, friendList.size());
        } else {
            System.out.println("Invalid position for removal: " + position);
        }
    }

    public interface OnFriendDeleteListener {
        void onFriendDelete(int position);
    }

    public static class FriendViewHolder extends RecyclerView.ViewHolder {
        TextView friendName;
        TextView timeds;
        CheckBox checkBox;
        Button deleteButton;
        public FriendViewHolder(@NonNull View itemView) {
            super(itemView);
            friendName = itemView.findViewById(R.id.friend_name);
            timeds = itemView.findViewById(R.id.time_distance);
            checkBox = itemView.findViewById(R.id.checkbox_select);
            deleteButton = itemView.findViewById(R.id.delete_button);
        }
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
}
