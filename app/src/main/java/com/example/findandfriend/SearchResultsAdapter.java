package com.example.findandfriend;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.libraries.places.api.model.AutocompletePrediction;

import java.util.List;

public class SearchResultsAdapter extends RecyclerView.Adapter<SearchResultsAdapter.ViewHolder> {

    private List<AutocompletePrediction> predictionList;
    private OnItemClickListener onItemClickListener;

    public interface OnItemClickListener {
        void onItemClick(AutocompletePrediction prediction);
    }

    public SearchResultsAdapter(List<AutocompletePrediction> predictionList, OnItemClickListener listener) {
        this.predictionList = predictionList;
        this.onItemClickListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_place, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AutocompletePrediction prediction = predictionList.get(position);
        holder.placeName.setText(prediction.getPrimaryText(null));
        holder.placeAddress.setText(prediction.getSecondaryText(null));
        holder.placeRating.setText("5 stars");
        holder.itemView.setOnClickListener(v -> onItemClickListener.onItemClick(prediction));
    }

    @Override
    public int getItemCount() {
        return predictionList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView placeName, placeAddress,placeRating;

        ViewHolder(View itemView) {
            super(itemView);
            placeName = itemView.findViewById(R.id.place_name);
            placeAddress = itemView.findViewById(R.id.place_address);
            placeRating = itemView.findViewById(R.id.place_rating);
        }
    }
}
