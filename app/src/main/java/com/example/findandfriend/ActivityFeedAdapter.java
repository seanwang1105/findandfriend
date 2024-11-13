package com.example.findandfriend;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class ActivityFeedAdapter extends RecyclerView.Adapter<ActivityFeedAdapter.ActivityFeedViewHolder> {

    private List<ActivityFeed> activityFeedList;

    // constructor，load in ActivityFeed data
    public ActivityFeedAdapter(List<ActivityFeed> activityFeedList) {
        this.activityFeedList = activityFeedList;
    }

    @NonNull
    @Override
    public ActivityFeedViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_activity_feed, parent, false);
        return new ActivityFeedViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ActivityFeedViewHolder holder, int position) {
        ActivityFeed activityFeed = activityFeedList.get(position);
        holder.bind(activityFeed);
    }

    @Override
    public int getItemCount() {
        return activityFeedList.size();
    }

    // Define ViewHolder class
    public static class ActivityFeedViewHolder extends RecyclerView.ViewHolder {

        private TextView userName;
        private TextView activityDescription;
        private TextView rating;

        public ActivityFeedViewHolder(@NonNull View itemView) {
            super(itemView);
            userName = itemView.findViewById(R.id.user_name);
            activityDescription = itemView.findViewById(R.id.activity_description);
            rating = itemView.findViewById(R.id.rating);
        }

        // Bundle the data to view
        public void bind(ActivityFeed activityFeed) {
            userName.setText(activityFeed.getUserName());
            activityDescription.setText(activityFeed.getActivityDescription());
            rating.setText(activityFeed.getRating());
        }
    }
}
