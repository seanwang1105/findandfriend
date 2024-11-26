package com.example.findandfriend;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.findandfriend.SearchResultsAdapter;
import com.google.android.libraries.places.api.model.AutocompletePrediction;

import java.util.List;

public class ViewPagerAdapter extends RecyclerView.Adapter<ViewPagerAdapter.ViewHolder> {

    private final RecyclerView.Adapter<?>[] pageAdapters;

    public ViewPagerAdapter(RecyclerView.Adapter<?>... pageAdapters) {
        this.pageAdapters = pageAdapters;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.viewpager_recyclerview, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.recyclerView.setLayoutManager(new LinearLayoutManager(holder.recyclerView.getContext()));
        holder.recyclerView.setAdapter(pageAdapters[position]);
    }
    @Override
    public int getItemCount() {
        return pageAdapters.length;
    }
    // Method to notify all pages of data changes
    public void notifyAllPagesChanged() {
        for (RecyclerView.Adapter<?> adapter : pageAdapters) {
            if (adapter != null) {
                adapter.notifyDataSetChanged();
            }
        }
        notifyDataSetChanged(); // Notify ViewPager itself of changes
    }
    static class ViewHolder extends RecyclerView.ViewHolder {
        RecyclerView recyclerView;

        ViewHolder(View itemView) {
            super(itemView);
            recyclerView = itemView.findViewById(R.id.recycler_view);
        }
    }
}
