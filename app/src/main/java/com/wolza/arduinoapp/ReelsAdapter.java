package com.wolza.arduinoapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class ReelsAdapter extends RecyclerView.Adapter<ReelsAdapter.ReelViewHolder> {

    private List<ReelItem> reelItems;

    public ReelsAdapter(List<ReelItem> reelItems) {
        this.reelItems = reelItems;
    }

    @NonNull
    @Override
    public ReelViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ReelViewHolder(
                LayoutInflater.from(parent.getContext()).inflate(R.layout.item_reel, parent, false)
        );
    }

    @Override
    public void onBindViewHolder(@NonNull ReelViewHolder holder, int position) {
        holder.setReelData(reelItems.get(position));
    }

    @Override
    public int getItemCount() {
        return reelItems.size();
    }

    static class ReelViewHolder extends RecyclerView.ViewHolder {

        VideoView videoView;
        ImageView ivPhoto;
        TextView tvUsername, tvDescription;
        CircleImageView ivAvatar;
        ImageView btnMessage, btnCall;

        public ReelViewHolder(@NonNull View itemView) {
            super(itemView);
            videoView = itemView.findViewById(R.id.videoViewReel);
            ivPhoto = itemView.findViewById(R.id.ivReelPhoto);
            tvUsername = itemView.findViewById(R.id.tvReelUsername);
            tvDescription = itemView.findViewById(R.id.tvReelDescription);
            ivAvatar = itemView.findViewById(R.id.ivReelUserAvatar);
            btnMessage = itemView.findViewById(R.id.btnReelMessage);
            btnCall = itemView.findViewById(R.id.btnReelCall);
        }

        void setReelData(ReelItem reelItem) {
            tvUsername.setText(reelItem.getUserName());
            tvDescription.setText(reelItem.getDescription());

            if (reelItem.isVideo()) {
                ivPhoto.setVisibility(View.GONE);
                videoView.setVisibility(View.VISIBLE);
                videoView.setVideoPath(reelItem.getVideoUrl());
                videoView.setOnPreparedListener(mp -> {
                    mp.start();
                    float videoRatio = mp.getVideoWidth() / (float) mp.getVideoHeight();
                    float screenRatio = videoView.getWidth() / (float) videoView.getHeight();
                    float scale = videoRatio / screenRatio;
                    if (scale >= 1f) {
                        videoView.setScaleX(scale);
                    } else {
                        videoView.setScaleY(1f / scale);
                    }
                });
                videoView.setOnCompletionListener(mp -> mp.start());
            } else {
                videoView.setVisibility(View.GONE);
                ivPhoto.setVisibility(View.VISIBLE);
                Glide.with(itemView.getContext()).load(reelItem.getImageUrl()).into(ivPhoto);
            }

            if (reelItem.getUserAvatar() != null && !reelItem.getUserAvatar().isEmpty()) {
                Glide.with(itemView.getContext()).load(reelItem.getUserAvatar()).placeholder(R.drawable.ic_profile).into(ivAvatar);
            }

            // Profile click opens profile page
            View.OnClickListener openProfileListener = v -> {
                android.content.Intent intent = new android.content.Intent(itemView.getContext(), ProfileActivity.class);
                intent.putExtra("userId", reelItem.getUserId());
                itemView.getContext().startActivity(intent);
            };
            tvUsername.setOnClickListener(openProfileListener);
            ivAvatar.setOnClickListener(openProfileListener);

            // Message click opens direct chat
            btnMessage.setOnClickListener(v -> {
                com.google.firebase.auth.FirebaseUser currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
                if (currentUser == null) {
                    android.widget.Toast.makeText(itemView.getContext(), "Please login first", android.widget.Toast.LENGTH_SHORT).show();
                    return;
                }
                if (reelItem.getUserId() != null && reelItem.getUserId().equals(currentUser.getUid())) {
                    android.widget.Toast.makeText(itemView.getContext(), "This is your own reel", android.widget.Toast.LENGTH_SHORT).show();
                    return;
                }
                android.content.Intent intent = new android.content.Intent(itemView.getContext(), DirectChatActivity.class);
                intent.putExtra("partnerId", reelItem.getUserId());
                itemView.getContext().startActivity(intent);
            });

            // Call click calls the user
            btnCall.setOnClickListener(v -> {
                if (reelItem.getUserId() == null) return;
                com.google.firebase.firestore.FirebaseFirestore.getInstance()
                        .collection("users").document(reelItem.getUserId()).get()
                        .addOnSuccessListener(documentSnapshot -> {
                            if (documentSnapshot.exists()) {
                                String phone = documentSnapshot.getString("phone");
                                String number = (phone != null && !phone.isEmpty()) ? phone : "5550199";
                                android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_DIAL);
                                intent.setData(android.net.Uri.parse("tel:" + number));
                                itemView.getContext().startActivity(intent);
                            } else {
                                android.widget.Toast.makeText(itemView.getContext(), "No phone number available", android.widget.Toast.LENGTH_SHORT).show();
                            }
                        });
            });
        }
    }
}
