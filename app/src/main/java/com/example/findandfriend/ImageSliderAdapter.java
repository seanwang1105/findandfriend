package com.example.findandfriend;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;

public class ImageSliderAdapter extends RecyclerView.Adapter<ImageSliderAdapter.ImageViewHolder> {
    private ArrayList<byte[]> images;

    public ImageSliderAdapter(ArrayList<byte[]> images) {
        this.images = images;
    }

    // Create a ViewHolder that holds a reference to each image view
    public static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;

        public ImageViewHolder(View view) {
            super(view);
            imageView = view.findViewById(R.id.slider_image); // Specify the ImageViewID of the image
        }
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // 使用 LayoutInflater 加载 item 布局
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.image_item, parent, false);
        return new ImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        // Get a byte array and convert it into a bitmap
        byte[] imageData = images.get(position);
        Bitmap bitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.length);

        // Set Bitmap for imageView being held
        holder.imageView.setImageBitmap(bitmap);
    }

    @Override
    public int getItemCount() {
        return images.size();
    }
}
