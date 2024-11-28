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

import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FriendMeetingAdapter extends RecyclerView.Adapter<FriendMeetingAdapter.ViewHolder> {

    private final Context context;
    private List<MeetingFriend> friendMeetingList;
    private OnItemClickListener listener;
    private String SERVER_URL_RESPOND;
    private final String SERVER_URL_DELETE;
    private static final String FILE_NAME = "user_credentials.txt";
    private String email;

    public interface OnItemClickListener {
        void onItemClick(MeetingFriend friendmeet);  // Handle item touch event
        void onAccept(MeetingFriend friendmeet);    // Handle accept event
        void onReject(MeetingFriend friendmeet);
        void onDelete(MeetingFriend friendmeet);// Handle reject event
    }

    public FriendMeetingAdapter(Context context,List<MeetingFriend> friendMeetingList, OnItemClickListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("OnItemClickListener cannot be null");
        }
        this.context = context;
        this.friendMeetingList = friendMeetingList;
        this.listener=listener;
        if (context != null) {
            this.SERVER_URL_RESPOND = context.getString(R.string.IP) + "/respond_meeting";
            this.SERVER_URL_DELETE = context.getString(R.string.IP) + "/delete_meeting_participant";

        } else {
            throw new IllegalArgumentException("Context cannot be null");
        }
    }
    public void updateMeetingFriends(List<MeetingFriend> newMeetingFriends) {
        this.friendMeetingList = newMeetingFriends;
        notifyDataSetChanged(); // Notify the adapter that the dataset has changed
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_friend_meeting, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MeetingFriend friendmeet = friendMeetingList.get(position);
        holder.name.setText(friendmeet.name);
        holder.MLname.setText(friendmeet.meet_location_name);
        holder.location.setText("Location: " + friendmeet.latitude + ", " + friendmeet.longitude);
        holder.Mstatus.setText(friendmeet.meet_status);
        // Handle item click to show friend on map
        holder.itemView.setOnClickListener(v -> {
                System.out.println("FriendMeetingAdapter Item clicked: " + friendmeet.name);
                listener.onItemClick(friendmeet);
                });

        // Handle accept and reject button clicks
        holder.btnAccept.setOnClickListener(v -> {
            if (listener != null) {
                listener.onAccept(friendmeet);
            }
            respondToMeetingRequest(friendmeet.getMid(),"Accepted",position);
        });
        holder.btnReject.setOnClickListener(v -> {
            if (listener != null) {
                listener.onReject(friendmeet);
            }
            respondToMeetingRequest(friendmeet.getMid(),"Rejected",position);
        });
        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDelete(friendmeet);
            }
            deleteMeetingParticipant(friendmeet.getMid(), position);
        });
    }

    @Override
    public int getItemCount() {
        return friendMeetingList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name,MLname,location,Mstatus;
        Button btnAccept, btnReject,btnDelete;

        ViewHolder(View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.friend_name);
            MLname=itemView.findViewById(R.id.friend_location_name);
            location = itemView.findViewById(R.id.friend_location);
            Mstatus=itemView.findViewById(R.id.meet_status);
            btnAccept = itemView.findViewById(R.id.btn_accept);
            btnReject = itemView.findViewById(R.id.btn_reject);
            btnDelete=itemView.findViewById(R.id.btn_delete);
        }
    }

    private void respondToMeetingRequest(int Mid,String action,int position) {
        RequestQueue queue = Volley.newRequestQueue(context);
        SharedPreferences sharedPreferences = context.getSharedPreferences("auth", Context.MODE_PRIVATE);
        String token = sharedPreferences.getString("token", null);
        String[] savedCredentials = loadCredentials();
        if (savedCredentials != null) {
            email = savedCredentials[0];
        }
        // Build the JSON payload
        JSONObject jsonData = new JSONObject();
        try {
            jsonData.put("email",email);
            jsonData.put("request_id", Mid);
            jsonData.put("action", action);
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }

        // Load the token from SharedPreferences

        if (token == null) {
            Toast.makeText(context, "Authorization token is missing. Please log in again.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create the request
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, SERVER_URL_RESPOND, jsonData,
                response -> {
                    Toast.makeText(context, "Friend request " + action.toLowerCase() + "ed successfully", Toast.LENGTH_SHORT).show();
                   // friendRequests.remove(position);
                   // notifyItemRemoved(position);
                   // notifyItemRangeChanged(position, friendRequests.size());
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
    private void deleteMeetingParticipant(int meetingId, int position) {
        RequestQueue queue = Volley.newRequestQueue(context);
        SharedPreferences sharedPreferences = context.getSharedPreferences("auth", Context.MODE_PRIVATE);
        String token = sharedPreferences.getString("token", null);
        String[] savedCredentials = loadCredentials();
        if (savedCredentials != null) {
            email = savedCredentials[0];
        }
        if (token == null) {
            Toast.makeText(context, "Authorization token is missing. Please log in again.", Toast.LENGTH_SHORT).show();
            return;
        }

        JSONObject jsonData = new JSONObject();
        try {
            jsonData.put("meeting_id", meetingId);
            jsonData.put("email", email);
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, SERVER_URL_DELETE, jsonData,
                response -> {
                    Toast.makeText(context, "Meeting participant deleted successfully", Toast.LENGTH_SHORT).show();
                    friendMeetingList.remove(position);
                    notifyItemRemoved(position);
                    notifyItemRangeChanged(position, friendMeetingList.size());
                },
                error -> {
                    Log.e("MeetingAdapter", "Error: " + error.getMessage());
                    Toast.makeText(context, "Failed to delete meeting participant", Toast.LENGTH_SHORT).show();
                }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                headers.put("x-access-token", token);
                return headers;
            }
        };

        queue.add(request);
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

