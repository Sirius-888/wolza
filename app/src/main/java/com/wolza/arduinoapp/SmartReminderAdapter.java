package com.wolza.arduinoapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class SmartReminderAdapter extends RecyclerView.Adapter<SmartReminderAdapter.ViewHolder> {

    private List<SmartReminder> reminders;
    private OnReminderActionListener listener;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM", new Locale("ru"));

    public interface OnReminderActionListener {
        void onWaterClick(SmartReminder reminder, int position);
        void onDetailsClick(SmartReminder reminder);
        void onDeleteClick(SmartReminder reminder, int position);
    }

    public SmartReminderAdapter(List<SmartReminder> reminders, OnReminderActionListener listener) {
        this.reminders = reminders;
        this.listener = listener;
    }

    public void updateList(List<SmartReminder> newList) {
        this.reminders = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_smart_reminder, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SmartReminder reminder = reminders.get(position);

        holder.tvPlantName.setText(reminder.getPlantName());
        holder.tvNextWatering.setText(dateFormat.format(reminder.getNextWateringDate()));
        holder.tvStatus.setText(reminder.getStatusText());
        holder.tvStatus.setTextColor(reminder.getStatusColor());

        int health = reminder.getPlantCondition() != null ? reminder.getPlantCondition().healthPercent : 80;
        holder.progressHealth.setProgress(health);
        holder.tvWateredCount.setText("🌱 Поливов: " + reminder.getWateredCount());

        holder.btnWater.setOnClickListener(v -> {
            if (listener != null) listener.onWaterClick(reminder, position);
        });

        holder.btnDetails.setOnClickListener(v -> {
            if (listener != null) listener.onDetailsClick(reminder);
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) listener.onDeleteClick(reminder, position);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return reminders == null ? 0 : reminders.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvPlantName, tvStatus, tvNextWatering, tvWateredCount;
        ProgressBar progressHealth;
        Button btnWater, btnDetails;
        CardView cardView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPlantName = itemView.findViewById(R.id.tvPlantName);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvNextWatering = itemView.findViewById(R.id.tvNextWatering);
            tvWateredCount = itemView.findViewById(R.id.tvWateredCount);
            progressHealth = itemView.findViewById(R.id.progressHealth);
            btnWater = itemView.findViewById(R.id.btnWater);
            btnDetails = itemView.findViewById(R.id.btnDetails);
            cardView = (CardView) itemView;
        }
    }
}