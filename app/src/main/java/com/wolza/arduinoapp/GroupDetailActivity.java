package com.wolza.arduinoapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GroupDetailActivity extends AppCompatActivity {

    private RecyclerView rvPosts;
    private FloatingActionButton fabAddPost;
    private TextView tvGroupName;
    private ImageView btnBack;

    private FirebaseFirestore db;
    private CommunityGroup group;
    private PostAdapter adapter;
    private List<CommunityPost> postList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_detail);

        group = (CommunityGroup) getIntent().getSerializableExtra("group");
        if (group == null) {
            finish();
            return;
        }

        db = FirebaseFirestore.getInstance();
        initViews();
        setupRecyclerView();
        loadPosts();

        fabAddPost.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddPostActivity.class);
            intent.putExtra("groupId", group.getId());
            startActivity(intent);
        });

        btnBack.setOnClickListener(v -> finish());
    }

    private void initViews() {
        rvPosts = findViewById(R.id.rvPosts);
        fabAddPost = findViewById(R.id.fabAddPost);
        tvGroupName = findViewById(R.id.tvGroupName);
        btnBack = findViewById(R.id.btnBack);
        
        tvGroupName.setText(group.getName());
    }

    private void setupRecyclerView() {
        postList = new ArrayList<>();
        adapter = new PostAdapter(postList);
        rvPosts.setLayoutManager(new LinearLayoutManager(this));
        rvPosts.setAdapter(adapter);
    }

    private void loadPosts() {
        // Simplified query: No 'orderBy' to avoid Firestore Index requirements/Permission errors.
        // We sort the list in Java instead.
        db.collection("community_posts")
                .whereEqualTo("groupId", group.getId())
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Toast.makeText(this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    postList.clear();
                    if (value != null) {
                        for (QueryDocumentSnapshot doc : value) {
                            CommunityPost post = doc.toObject(CommunityPost.class);
                            post.setId(doc.getId());
                            postList.add(post);
                        }
                    }
                    
                    // Sort by timestamp newest first in Java
                    Collections.sort(postList, (a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
                    adapter.notifyDataSetChanged();
                });
    }

    class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {
        private List<CommunityPost> posts;
        public PostAdapter(List<CommunityPost> posts) { this.posts = posts; }

        @NonNull
        @Override
        public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_community_post, parent, false);
            return new PostViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
            CommunityPost post = posts.get(position);
            holder.tvTitle.setText(post.getTitle());
            holder.tvUser.setText(post.getUserName());
            holder.tvTime.setText(post.getFormattedTime());
            holder.tvContent.setText(post.getContent());
            holder.tvCommentCount.setText(post.getCommentCount() + " hints/comments");

            if (post.getImageUrl() != null && !post.getImageUrl().isEmpty()) {
                holder.ivImage.setVisibility(View.VISIBLE);
                Glide.with(holder.itemView.getContext()).load(post.getImageUrl()).into(holder.ivImage);
            } else {
                holder.ivImage.setVisibility(View.GONE);
            }

            // Load author's avatar
            String avatarBase64 = post.getUserAvatar();
            if (avatarBase64 != null && !avatarBase64.isEmpty()) {
                try {
                    byte[] bytes = android.util.Base64.decode(avatarBase64, android.util.Base64.DEFAULT);
                    Glide.with(holder.itemView.getContext()).load(bytes)
                            .placeholder(R.drawable.ic_profile).into(holder.ivAvatar);
                } catch (Exception e) {
                    holder.ivAvatar.setImageResource(R.drawable.ic_profile);
                }
            } else {
                holder.ivAvatar.setImageResource(R.drawable.ic_profile);
            }

            // Click listener to view author profile
            View.OnClickListener openAuthorProfile = v -> {
                Intent intent = new Intent(GroupDetailActivity.this, ProfileActivity.class);
                intent.putExtra("userId", post.getUserId());
                startActivity(intent);
            };
            holder.ivAvatar.setOnClickListener(openAuthorProfile);
            holder.tvUser.setOnClickListener(openAuthorProfile);

            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(GroupDetailActivity.this, PostDetailActivity.class);
                intent.putExtra("post", post);
                startActivity(intent);
            });
        }

        @Override
        public int getItemCount() { return posts.size(); }

        class PostViewHolder extends RecyclerView.ViewHolder {
            TextView tvTitle, tvUser, tvTime, tvContent, tvCommentCount;
            ImageView ivImage, ivAvatar;
            public PostViewHolder(View v) {
                super(v);
                tvTitle = v.findViewById(R.id.tvPostTitle);
                tvUser = v.findViewById(R.id.tvUserName);
                tvTime = v.findViewById(R.id.tvPostTime);
                tvContent = v.findViewById(R.id.tvPostContent);
                tvCommentCount = v.findViewById(R.id.tvCommentCount);
                ivImage = v.findViewById(R.id.ivPostImage);
                ivAvatar = v.findViewById(R.id.ivUserAvatar);
            }
        }
    }
}
