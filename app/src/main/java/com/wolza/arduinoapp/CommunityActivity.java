package com.wolza.arduinoapp;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class CommunityActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private EditText etMessage;
    private Button btnSend;
    private ProgressBar progressBar;
    private ImageView btnBack;
    private TextView tvEmpty;

    private MessageAdapter adapter;
    private List<ChatMessage> messageList;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    private String currentUserName = "User";
    private String currentUserAvatar = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_community);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupToolbar();
        loadUserData();
        setupRecyclerView();
        loadMessages();

        btnSend.setOnClickListener(v -> sendMessage());
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerView);
        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);
        progressBar = findViewById(R.id.progressBar);
        btnBack = findViewById(R.id.btnBack);
        tvEmpty = findViewById(R.id.tvEmpty);

        messageList = new ArrayList<>();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("🌍 Community Chat");
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        }

        btnBack.setOnClickListener(v -> finish());
    }

    private void loadUserData() {
        if (currentUser == null) return;

        String name = currentUser.getDisplayName();
        if (name != null && !name.isEmpty()) {
            currentUserName = name;
        } else {
            String email = currentUser.getEmail();
            if (email != null && email.contains("@")) {
                currentUserName = email.substring(0, email.indexOf("@"));
            } else {
                currentUserName = "User";
            }
        }

        db.collection("users").document(currentUser.getUid())
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        String savedName = document.getString("name");
                        if (savedName != null && !savedName.isEmpty()) {
                            currentUserName = savedName;
                        }
                        String avatar = document.getString("avatar");
                        if (avatar != null && !avatar.isEmpty()) {
                            currentUserAvatar = avatar;
                        }
                    }
                });
    }

    private void setupRecyclerView() {
        adapter = new MessageAdapter(messageList, currentUser != null ? currentUser.getUid() : "");
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void loadMessages() {
        progressBar.setVisibility(View.VISIBLE);

        db.collection("chat_messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((value, error) -> {
                    progressBar.setVisibility(View.GONE);

                    if (error != null) {
                        Toast.makeText(this, "Error loading messages", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    messageList.clear();
                    if (value != null) {
                        for (QueryDocumentSnapshot doc : value) {
                            ChatMessage msg = doc.toObject(ChatMessage.class);
                            msg.setId(doc.getId());
                            messageList.add(msg);
                        }
                    }

                    adapter.notifyDataSetChanged();

                    if (messageList.isEmpty()) {
                        tvEmpty.setVisibility(View.VISIBLE);
                        recyclerView.setVisibility(View.GONE);
                    } else {
                        tvEmpty.setVisibility(View.GONE);
                        recyclerView.setVisibility(View.VISIBLE);
                        recyclerView.smoothScrollToPosition(messageList.size() - 1);
                    }
                });
    }

    private void sendMessage() {
        String message = etMessage.getText().toString().trim();
        if (TextUtils.isEmpty(message)) {
            etMessage.setError("Type a message");
            return;
        }

        if (currentUser == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSend.setEnabled(false);

        ChatMessage chatMessage = new ChatMessage(
                currentUser.getUid(),
                currentUserName,
                message,
                "",
                currentUserAvatar,
                System.currentTimeMillis()
        );

        db.collection("chat_messages").add(chatMessage)
                .addOnSuccessListener(docRef -> {
                    etMessage.setText("");
                    btnSend.setEnabled(true);

                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(etMessage.getWindowToken(), 0);
                })
                .addOnFailureListener(e -> {
                    btnSend.setEnabled(true);
                    Toast.makeText(this, "Failed to send: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // ========== ADAPTER ==========

    class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {

        private List<ChatMessage> messages;
        private String currentUserId;

        public MessageAdapter(List<ChatMessage> messages, String currentUserId) {
            this.messages = messages;
            this.currentUserId = currentUserId;
        }

        @NonNull
        @Override
        public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_chat_message, parent, false);
            return new MessageViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
            ChatMessage msg = messages.get(position);

            holder.tvUserName.setText(msg.getUserName());
            holder.tvMessage.setText(msg.getMessage());
            holder.tvTime.setText(msg.getFormattedTime());

            if (msg.getUserId().equals(currentUserId)) {
                holder.itemView.setBackgroundResource(R.drawable.chat_bubble_own);
            } else {
                holder.itemView.setBackgroundResource(R.drawable.chat_bubble_other);
            }

            if (msg.getUserAvatar() != null && !msg.getUserAvatar().isEmpty()) {
                Glide.with(holder.itemView.getContext())
                        .load(msg.getUserAvatar())
                        .placeholder(R.drawable.ic_profile)
                        .circleCrop()
                        .into(holder.ivAvatar);
            } else {
                holder.ivAvatar.setImageResource(R.drawable.ic_profile);
            }
        }

        @Override
        public int getItemCount() {
            return messages.size();
        }

        class MessageViewHolder extends RecyclerView.ViewHolder {
            ImageView ivAvatar;
            TextView tvUserName, tvMessage, tvTime;

            public MessageViewHolder(@NonNull View itemView) {
                super(itemView);
                ivAvatar = itemView.findViewById(R.id.ivAvatar);
                tvUserName = itemView.findViewById(R.id.tvUserName);
                tvMessage = itemView.findViewById(R.id.tvMessage);
                tvTime = itemView.findViewById(R.id.tvTime);
            }
        }
    }
}