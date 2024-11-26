package com.example.findandfriend;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FriendRequestAdapter extends RecyclerView.Adapter<FriendRequestAdapter.FriendRequestViewHolder> {

    private Context context;
    private List<FriendRequest> friendRequests;
    private final String SERVER_URL;

    public FriendRequestAdapter(Context context, List<FriendRequest> friendRequests) {
        this.context = context;
        this.friendRequests = friendRequests;
        this.SERVER_URL = context.getString(R.string.IP) + "/respond_friend_request";
    }

    @NonNull
    @Override
    public FriendRequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.friend_request_item, parent, false);
        return new FriendRequestViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FriendRequestViewHolder holder, int position) {
        FriendRequest request = friendRequests.get(position);
        holder.friendName.setText(request.getFromName());
        holder.friendEmail.setText(request.getFromEmail());

        holder.acceptButton.setOnClickListener(v -> {
            respondToFriendRequest(request.getRequestId(), "Accepted",position);
        });

        holder.declineButton.setOnClickListener(v -> {
            respondToFriendRequest(request.getRequestId(), "Rejected",position);
        });
    }
    private void respondToFriendRequest(int requestId, String action,int position) {
        RequestQueue queue = Volley.newRequestQueue(context);

        // Build the JSON payload
        JSONObject jsonData = new JSONObject();
        try {
            jsonData.put("request_id", requestId);
            jsonData.put("action", action);
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }

        // Load the token from SharedPreferences
        SharedPreferences sharedPreferences = context.getSharedPreferences("auth", Context.MODE_PRIVATE);
        String token = sharedPreferences.getString("token", null);
        if (token == null) {
            Toast.makeText(context, "Authorization token is missing. Please log in again.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create the request
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, SERVER_URL, jsonData,
                response -> {
                    Toast.makeText(context, "Friend request " + action.toLowerCase() + "ed successfully", Toast.LENGTH_SHORT).show();
                    friendRequests.remove(position);
                    notifyItemRemoved(position);
                    notifyItemRangeChanged(position, friendRequests.size());
                },
                error -> {
                    Log.e("FriendRequestAdapter", "Error: " + error.getMessage());
                    Toast.makeText(context, "Failed to " + action.toLowerCase() + " request", Toast.LENGTH_SHORT).show();
                }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                headers.put("x-access-token", token); // Add the token to headers
                return headers;
            }
        };

        queue.add(request);
    }

    @Override
    public int getItemCount() {
        return friendRequests.size();
    }

    static class FriendRequestViewHolder extends RecyclerView.ViewHolder {
        TextView friendName;
        TextView friendEmail;
        Button acceptButton;
        Button declineButton;

        public FriendRequestViewHolder(@NonNull View itemView) {
            super(itemView);
            friendName = itemView.findViewById(R.id.text_friend_name);
            friendEmail = itemView.findViewById(R.id.text_friend_email);
            acceptButton = itemView.findViewById(R.id.button_accept);
            declineButton = itemView.findViewById(R.id.button_decline);
        }
    }
}
