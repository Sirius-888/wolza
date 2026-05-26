package com.wolza.arduinoapp;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class PostDetailActivity extends AppCompatActivity {

    private CommunityPost post;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    private RecyclerView rvComments;
    private EditText etComment;
    private ImageButton btnSendComment;
    private CommentAdapter adapter;
    private List<CommunityComment> commentList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_detail);

        post = (CommunityPost) getIntent().getSerializableExtra("post");
        if (post == null) {
            finish();
            return;
        }

        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        initViews();
        setupPostHeader();
        setupRecyclerView();
        loadComments();

        btnSendComment.setOnClickListener(v -> sendComment());
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    private void initViews() {
        rvComments = findViewById(R.id.rvComments);
        etComment = findViewById(R.id.etComment);
        btnSendComment = findViewById(R.id.btnSendComment);
    }

    private void setupPostHeader() {
        TextView tvUser = findViewById(R.id.tvUserName);
        TextView tvTime = findViewById(R.id.tvPostTime);
        TextView tvTitle = findViewById(R.id.tvPostTitle);
        TextView tvContent = findViewById(R.id.tvPostContent);
        ImageView ivPostImage = findViewById(R.id.ivPostImage);
        CircleImageView ivAvatar = findViewById(R.id.ivUserAvatar);

        tvUser.setText(post.getUserName());
        tvTime.setText(post.getFormattedTime());
        tvTitle.setText(post.getTitle());
        tvContent.setText(post.getContent());

        if (post.getImageUrl() != null && !post.getImageUrl().isEmpty()) {
            ivPostImage.setVisibility(View.VISIBLE);
            Glide.with(this).load(post.getImageUrl()).into(ivPostImage);
        } else {
            ivPostImage.setVisibility(View.GONE);
        }

        if (post.getUserAvatar() != null && !post.getUserAvatar().isEmpty()) {
            Glide.with(this).load(post.getUserAvatar()).placeholder(R.drawable.ic_profile).into(ivAvatar);
        }
    }

    private void setupRecyclerView() {
        commentList = new ArrayList<>();
        adapter = new CommentAdapter(commentList);
        rvComments.setLayoutManager(new LinearLayoutManager(this));
        rvComments.setAdapter(adapter);
    }

    private void loadComments() {
        db.collection("community_comments")
                .whereEqualTo("postId", post.getId())
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;
                    commentList.clear();
                    if (value != null) {
                        for (QueryDocumentSnapshot doc : value) {
                            CommunityComment comment = doc.toObject(CommunityComment.class);
                            commentList.add(comment);
                        }
                    }
                    adapter.notifyDataSetChanged();
                });
    }

    private void sendComment() {
        String content = etComment.getText().toString().trim();
        if (TextUtils.isEmpty(content)) return;
        if (currentUser == null) return;

        String userName = currentUser.getDisplayName() != null ? currentUser.getDisplayName() : "User";
        
        CommunityComment comment = new CommunityComment(
                post.getId(),
                currentUser.getUid(),
                userName,
                "", // avatar
                content,
                System.currentTimeMillis()
        );

        db.collection("community_comments").add(comment)
                .addOnSuccessListener(doc -> {
                    etComment.setText("");
                    // Increment comment count in post document
                    db.collection("community_posts").document(post.getId())
                            .update("commentCount", FieldValue.increment(1));
                });
    }

    // --- Adapter ---
    class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {
        private List<CommunityComment> comments;

        public CommentAdapter(List<CommunityComment> comments) { this.comments = comments; }

        @NonNull
        @Override
        public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_community_comment, parent, false);
            return new CommentViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
            CommunityComment comment = comments.get(position);
            holder.tvUser.setText(comment.getUserName());
            holder.tvContent.setText(comment.getContent());
            holder.tvTime.setText(comment.getFormattedTime());
            
            if (comment.getUserAvatar() != null && !comment.getUserAvatar().isEmpty()) {
                Glide.with(holder.itemView.getContext()).load(comment.getUserAvatar()).placeholder(R.drawable.ic_profile).into(holder.ivAvatar);
            }
        }

        @Override
        public int getItemCount() { return comments.size(); }

        class CommentViewHolder extends RecyclerView.ViewHolder {
            TextView tvUser, tvContent, tvTime;
            CircleImageView ivAvatar;
            public CommentViewHolder(View v) {
                super(v);
                tvUser = v.findViewById(R.id.tvCommentUser);
                tvContent = v.findViewById(R.id.tvCommentContent);
                tvTime = v.findViewById(R.id.tvCommentTime);
                ivAvatar = v.findViewById(R.id.ivCommentAvatar);
            }
        }
    }
}
