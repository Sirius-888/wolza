package com.wolza.arduinoapp;

import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.util.List;

public class MarketAdapter extends RecyclerView.Adapter<MarketAdapter.MarketViewHolder> {

    private List<MarketActivity.MarketItem> items;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public MarketAdapter(MarketActivity context, List<MarketActivity.MarketItem> items) {
        this.items = items;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public MarketViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_market, parent, false);
        return new MarketViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MarketViewHolder holder, int position) {
        MarketActivity.MarketItem item = items.get(position);

        holder.tvName.setText(item.getName());
        holder.tvPrice.setText("💰 " + item.getPrice() + " AMD");
        holder.tvSeller.setText("👤 " + item.getSellerName());

        String imageSource = item.getImageUrl();
        if (imageSource != null && !imageSource.isEmpty()) {
            if (imageSource.startsWith("http")) {
                Glide.with(holder.itemView.getContext())
                        .load(imageSource)
                        .placeholder(R.drawable.ic_flower)
                        .error(R.drawable.ic_flower)
                        .centerCrop()
                        .into(holder.imgItem);
            } else {
                try {
                    // Match the encoding format (NO_WRAP) used in ImageUtils
                    byte[] imageByteArray = Base64.decode(imageSource, Base64.NO_WRAP);
                    Glide.with(holder.itemView.getContext())
                            .asBitmap()
                            .load(imageByteArray)
                            .diskCacheStrategy(DiskCacheStrategy.NONE) // Don't cache base64, save memory
                            .placeholder(R.drawable.ic_flower)
                            .error(R.drawable.ic_flower)
                            .centerCrop()
                            .into(holder.imgItem);
                } catch (Exception e) {
                    holder.imgItem.setImageResource(R.drawable.ic_flower);
                }
            }
        } else {
            holder.imgItem.setImageResource(R.drawable.ic_flower);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void updateList(List<MarketActivity.MarketItem> newList) {
        this.items = newList;
        notifyDataSetChanged();
    }

    static class MarketViewHolder extends RecyclerView.ViewHolder {
        ImageView imgItem;
        TextView tvName, tvPrice, tvSeller;

        public MarketViewHolder(@NonNull View itemView) {
            super(itemView);
            imgItem = itemView.findViewById(R.id.imgItem);
            tvName = itemView.findViewById(R.id.tvName);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            tvSeller = itemView.findViewById(R.id.tvSeller);
        }
    }
}