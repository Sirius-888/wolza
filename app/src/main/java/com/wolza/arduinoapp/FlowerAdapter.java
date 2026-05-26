package com.wolza.arduinoapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class FlowerAdapter extends RecyclerView.Adapter<FlowerAdapter.ViewHolder> {

    private List<FlowerListActivity.Flower> flowerList;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public FlowerAdapter(FlowerListActivity context, List<FlowerListActivity.Flower> flowerList) {
        this.flowerList = flowerList;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_flower, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FlowerListActivity.Flower flower = flowerList.get(position);

        holder.flowerName.setText(flower.getName());

        // Load image from URL using Glide
        if (flower.getImageUrl() != null && !flower.getImageUrl().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(flower.getImageUrl())
                    .placeholder(R.drawable.ic_flower)
                    .error(R.drawable.ic_flower)
                    .into(holder.flowerImage);
        } else {
            holder.flowerImage.setImageResource(R.drawable.ic_flower);
        }
    }

    @Override
    public int getItemCount() {
        return flowerList.size();
    }

    public void updateList(List<FlowerListActivity.Flower> newList) {
        this.flowerList = newList;
        notifyDataSetChanged();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        ImageView flowerImage;
        TextView flowerName;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = (CardView) itemView;
            flowerImage = itemView.findViewById(R.id.flowerImage);
            flowerName = itemView.findViewById(R.id.flowerName);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        int position = getAdapterPosition();
                        if (position != RecyclerView.NO_POSITION) {
                            listener.onItemClick(position);
                        }
                    }
                }
            });
        }
    }
}