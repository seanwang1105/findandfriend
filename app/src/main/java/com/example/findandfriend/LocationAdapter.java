package com.example.findandfriend;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class LocationAdapter extends RecyclerView.Adapter<LocationAdapter.LocationViewHolder> {

    private List<Location> locationList;
    private OnLocationClickListener listener;

    // Define an interface to handle click events
    public interface OnLocationClickListener {
        void onLocationClick(Location location);
    }

    // Constructor，data passing and click listener
    public LocationAdapter(List<Location> locationList, OnLocationClickListener listener) {
        this.locationList = locationList;
        this.listener = listener;
    }

    // Create ViewHolder，define item layout
    @NonNull
    @Override
    public LocationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_location, parent, false);
        return new LocationViewHolder(view);
    }

    // boundle data to ViewHolder
    @Override
    public void onBindViewHolder(@NonNull LocationViewHolder holder, int position) {
        Location location = locationList.get(position);
        holder.bind(location, listener);
    }

    @Override
    public int getItemCount() {
        return locationList.size();
    }

    // define ViewHolder class，to display item in RecyclerView
    public static class LocationViewHolder extends RecyclerView.ViewHolder {
        private TextView locationName;
        private TextView locationDescription;
        //private TextView locationid;

        public LocationViewHolder(@NonNull View itemView) {
            super(itemView);
            //locationid = itemView.findViewById(R.id.location_id);
            locationName = itemView.findViewById(R.id.location_name);
            locationDescription = itemView.findViewById(R.id.location_description);
        }

        // boundle data to view and set click event
        public void bind(final Location location, final OnLocationClickListener listener) {
            //locationid.setText(location.getId());
            locationName.setText(location.getName());
            locationDescription.setText(location.getDescription());
            itemView.setOnClickListener(v -> listener.onLocationClick(location));
        }
    }
}

