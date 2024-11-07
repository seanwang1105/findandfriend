package com.example.findandfriend;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class FriendAdapter extends RecyclerView.Adapter<FriendAdapter.FriendViewHolder> {

    private Context context;
    private List<Friend> friendList;  // define private Friend class
    private List<Friend> selectedFriends = new ArrayList<>();

    public FriendAdapter(Context context, List<Friend> friendList) {
        this.context = context;
        this.friendList = friendList;
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
        holder.friendName.setText(friend.name);
        holder.timeds.setText(friend.timeAtLocation);
        holder.checkBox.setChecked(selectedFriends.contains(friend));

        holder.checkBox.setOnClickListener(v -> {
            if (holder.checkBox.isChecked()) {
                selectedFriends.add(friend);
            } else {
                selectedFriends.remove(friend);
            }
        });
    }

    @Override
    public int getItemCount() {
        return friendList.size();
    }

    public List<Friend> getSelectedFriends() {
        return selectedFriends;
    }

    public static class FriendViewHolder extends RecyclerView.ViewHolder {
        TextView friendName;
        TextView timeds;
        CheckBox checkBox;

        public FriendViewHolder(@NonNull View itemView) {
            super(itemView);
            friendName = itemView.findViewById(R.id.friend_name);
            timeds = itemView.findViewById(R.id.time_distance);
            checkBox = itemView.findViewById(R.id.checkbox_select);
        }
    }
}
