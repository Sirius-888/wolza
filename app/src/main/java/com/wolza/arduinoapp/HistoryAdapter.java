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

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder> {

    private List<HistoryItem> historyList;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public HistoryAdapter(List<HistoryItem> historyList) {
        this.historyList = historyList;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_history, parent, false);
        return new HistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        HistoryItem item = historyList.get(position);

        holder.tvName.setText(item.getName());
        holder.tvDescription.setText(item.getDescription());
        holder.tvTimestamp.setText(item.getTimestamp());

        if ("search".equals(item.getType())) {
            holder.ivIcon.setImageResource(R.drawable.ic_search);
            holder.ivIcon.setColorFilter(0xFF4CAF50);
            holder.tvType.setText("🔍 Search");
            holder.tvType.setBackgroundResource(R.drawable.badge_search);
        } else {
            holder.ivIcon.setImageResource(R.drawable.ic_doctor);
            holder.ivIcon.setColorFilter(0xFFF44336);
            holder.tvType.setText("🩺 Doctor");
            holder.tvType.setBackgroundResource(R.drawable.badge_doctor);
        }

        // Load image if available
        if (item.getImageUrl() != null && !item.getImageUrl().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(item.getImageUrl())
                    .placeholder(R.drawable.ic_flower)
                    .into(holder.ivIcon);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return historyList.size();
    }

    public void updateList(List<HistoryItem> newList) {
        this.historyList = newList;
        notifyDataSetChanged();
    }

    static class HistoryViewHolder extends RecyclerView.ViewHolder {
        ImageView ivIcon;
        TextView tvName, tvDescription, tvTimestamp, tvType;

        public HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.ivIcon);
            tvName = itemView.findViewById(R.id.tvName);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
            tvType = itemView.findViewById(R.id.tvType);
        }
    }
}