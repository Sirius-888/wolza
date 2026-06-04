package com.wolza.arduinoapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

import de.hdodenhof.circleimageview.CircleImageView;

public class CommunityActivity extends AppCompatActivity {

    private TabLayout tabLayout;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private ImageView btnBack;
    private ImageView btnAddGroup;
    private TextView tvEmpty;

    private CommunityAdapter adapter;
    private List<CommunityGroup> groupList;
    private List<ChatRoom> chatRoomList;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    private ListenerRegistration chatsListener;
    private ListenerRegistration groupsListener;

    private boolean isChatsTab = true;

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
        setupRecyclerView();
        setupTabs();
        
        // Start listening to both data sources in background
        loadDirectChats();
        loadJoinedGroups();
        
        // Initialize UI display
        updateUI();

        btnAddGroup.setOnClickListener(v -> showAddGroupOptions());
    }

    private void initViews() {
        tabLayout = findViewById(R.id.tabLayout);
        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);
        btnBack = findViewById(R.id.btnBack);
        btnAddGroup = findViewById(R.id.btnAddGroup);
        tvEmpty = findViewById(R.id.tvEmpty);

        groupList = new ArrayList<>();
        chatRoomList = new ArrayList<>();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("");
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        }
        btnBack.setOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        adapter = new CommunityAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void setupTabs() {
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                isChatsTab = tab.getPosition() == 0;
                updateUI();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void updateUI() {
        adapter.notifyDataSetChanged();
        if (isChatsTab) {
            btnAddGroup.setVisibility(View.GONE);
            if (chatRoomList.isEmpty()) {
                tvEmpty.setText("💬 No direct messages yet.\n\nStart a conversation from a seller's item!");
                tvEmpty.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
            } else {
                tvEmpty.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
            }
        } else {
            btnAddGroup.setVisibility(View.VISIBLE);
            tvEmpty.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void loadDirectChats() {
        progressBar.setVisibility(View.VISIBLE);
        chatsListener = db.collection("direct_chats")
                .whereArrayContains("members", currentUser.getUid())
                .addSnapshotListener((value, error) -> {
                    progressBar.setVisibility(View.GONE);
                    if (error != null) return;

                    chatRoomList.clear();
                    if (value != null) {
                        for (QueryDocumentSnapshot doc : value) {
                            ChatRoom room = new ChatRoom();
                            room.id = doc.getId();

                            List<String> members = (List<String>) doc.get("members");
                            String partnerId = "";
                            if (members != null) {
                                for (String m : members) {
                                    if (!m.equals(currentUser.getUid())) {
                                        partnerId = m;
                                        break;
                                    }
                                }
                            }
                            room.partnerId = partnerId;

                            Map<String, String> names = (Map<String, String>) doc.get("memberNames");
                            if (names != null && !partnerId.isEmpty()) {
                                room.partnerName = names.get(partnerId);
                            }
                            if (room.partnerName == null || room.partnerName.isEmpty()) {
                                room.partnerName = "User";
                            }

                            Map<String, String> avatars = (Map<String, String>) doc.get("memberAvatars");
                            if (avatars != null && !partnerId.isEmpty()) {
                                room.partnerAvatar = avatars.get(partnerId);
                            }

                            room.lastMessage = doc.getString("lastMessage");
                            if (room.lastMessage == null) room.lastMessage = "";

                            Long ts = doc.getLong("lastMessageTimestamp");
                            room.lastMessageTimestamp = ts != null ? ts : 0;

                            chatRoomList.add(room);
                        }
                    }

                    // Sort chats newest first
                    Collections.sort(chatRoomList, (a, b) -> Long.compare(b.lastMessageTimestamp, a.lastMessageTimestamp));

                    if (isChatsTab) {
                        updateUI();
                    }
                });
    }

    private void loadJoinedGroups() {
        groupsListener = db.collection("community_groups")
                .whereArrayContains("members", currentUser.getUid())
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;

                    groupList.clear();
                    if (value != null) {
                        for (QueryDocumentSnapshot doc : value) {
                            CommunityGroup group = doc.toObject(CommunityGroup.class);
                            groupList.add(group);
                        }
                    }

                    if (!isChatsTab) {
                        updateUI();
                    }
                });
    }

    private void showAddGroupOptions() {
        String[] options = {"Create a New Group", "Join Group via Code"};
        new AlertDialog.Builder(this)
                .setTitle("Community Groups")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        showCreateGroupDialog();
                    } else {
                        showJoinGroupDialog();
                    }
                }).show();
    }

    private void showCreateGroupDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_create_group, null);
        TextInputEditText etGroupName = dialogView.findViewById(R.id.etGroupName);

        new AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton("Create", (dialog, which) -> {
                    String groupName = etGroupName.getText().toString().trim();
                    if (!TextUtils.isEmpty(groupName)) {
                        createGroup(groupName);
                    } else {
                        Toast.makeText(this, "Group name cannot be empty", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showJoinGroupDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_join_group, null);
        TextInputEditText etGroupCode = dialogView.findViewById(R.id.etGroupCode);

        new AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton("Join", (dialog, which) -> {
                    String code = etGroupCode.getText().toString().trim().toUpperCase();
                    if (!TextUtils.isEmpty(code)) {
                        joinGroup(code);
                    } else {
                        Toast.makeText(this, "Group code cannot be empty", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void createGroup(String name) {
        progressBar.setVisibility(View.VISIBLE);
        String code = generateGroupCode();
        DocumentReference groupRef = db.collection("community_groups").document();
        String groupId = groupRef.getId();

        CommunityGroup group = new CommunityGroup(name, code, currentUser.getUid(), System.currentTimeMillis());
        group.setId(groupId);

        groupRef.set(group)
                .addOnSuccessListener(aVoid -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Group Created! Code: " + code, Toast.LENGTH_LONG).show();
                    openGroupDetail(group);
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Failed to create group: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void joinGroup(String code) {
        progressBar.setVisibility(View.VISIBLE);
        db.collection("community_groups")
                .whereEqualTo("code", code)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(this, "No group found with code: " + code, Toast.LENGTH_LONG).show();
                        return;
                    }

                    QueryDocumentSnapshot doc = (QueryDocumentSnapshot) queryDocumentSnapshots.getDocuments().get(0);
                    CommunityGroup group = doc.toObject(CommunityGroup.class);

                    db.collection("community_groups").document(group.getId())
                            .update("members", FieldValue.arrayUnion(currentUser.getUid()))
                            .addOnSuccessListener(aVoid -> {
                                progressBar.setVisibility(View.GONE);
                                Toast.makeText(this, "Joined " + group.getName() + "!", Toast.LENGTH_SHORT).show();
                                openGroupDetail(group);
                            })
                            .addOnFailureListener(e -> {
                                progressBar.setVisibility(View.GONE);
                                Toast.makeText(this, "Failed to join: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Error finding group: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void openGroupDetail(CommunityGroup group) {
        Intent intent = new Intent(this, GroupDetailActivity.class);
        intent.putExtra("group", group);
        startActivity(intent);
    }

    private String generateGroupCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder();
        Random rnd = new Random();
        while (sb.length() < 5) {
            int index = (int) (rnd.nextFloat() * chars.length());
            sb.append(chars.charAt(index));
        }
        return sb.toString();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (chatsListener != null) chatsListener.remove();
        if (groupsListener != null) groupsListener.remove();
    }

    // ========== CHAT ROOM DATA MODEL ==========

    public static class ChatRoom {
        public String id;
        public String partnerId;
        public String partnerName;
        public String partnerAvatar;
        public String lastMessage;
        public long lastMessageTimestamp;
        public ChatRoom() {}
    }

    // ========== UNIFIED COMMUNITY ADAPTER ==========

    class CommunityAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private static final int TYPE_CHAT = 0;
        private static final int TYPE_GROUP = 1;

        @Override
        public int getItemViewType(int position) {
            return isChatsTab ? TYPE_CHAT : TYPE_GROUP;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (viewType == TYPE_CHAT) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_room, parent, false);
                return new ChatViewHolder(view);
            } else {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_community_group, parent, false);
                return new GroupViewHolder(view);
            }
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            if (getItemViewType(position) == TYPE_CHAT) {
                ChatRoom room = chatRoomList.get(position);
                ChatViewHolder chatHolder = (ChatViewHolder) holder;
                chatHolder.tvPartnerName.setText(room.partnerName);
                chatHolder.tvLastMessage.setText(room.lastMessage);

                // Format timestamp
                if (room.lastMessageTimestamp > 0) {
                    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault());
                    chatHolder.tvTimestamp.setText(sdf.format(new java.util.Date(room.lastMessageTimestamp)));
                } else {
                    chatHolder.tvTimestamp.setText("");
                }

                // Partner Avatar
                if (room.partnerAvatar != null && !room.partnerAvatar.isEmpty()) {
                    try {
                        byte[] bytes = Base64.decode(room.partnerAvatar, Base64.DEFAULT);
                        Glide.with(CommunityActivity.this).load(bytes).placeholder(R.drawable.ic_profile).into(chatHolder.ivPartnerAvatar);
                    } catch (Exception e) {
                        chatHolder.ivPartnerAvatar.setImageResource(R.drawable.ic_profile);
                    }
                } else {
                    chatHolder.ivPartnerAvatar.setImageResource(R.drawable.ic_profile);
                }

                chatHolder.itemView.setOnClickListener(v -> {
                    Intent intent = new Intent(CommunityActivity.this, DirectChatActivity.class);
                    intent.putExtra("chatId", room.id);
                    intent.putExtra("partnerId", room.partnerId);
                    startActivity(intent);
                });
            } else {
                if (position == 0) {
                    GroupViewHolder groupHolder = (GroupViewHolder) holder;
                    groupHolder.tvGroupName.setText("Global Community Chat");
                    groupHolder.tvGroupCode.setText("Public Room");

                    groupHolder.itemView.setOnClickListener(v -> {
                        Intent intent = new Intent(CommunityActivity.this, DirectChatActivity.class);
                        intent.putExtra("chatId", "global_chat");
                        startActivity(intent);
                    });
                } else {
                    CommunityGroup group = groupList.get(position - 1);
                    GroupViewHolder groupHolder = (GroupViewHolder) holder;
                    groupHolder.tvGroupName.setText(group.getName());
                    groupHolder.tvGroupCode.setText("Code: " + group.getCode());

                    groupHolder.itemView.setOnClickListener(v -> openGroupDetail(group));
                }
            }
        }

        @Override
        public int getItemCount() {
            return isChatsTab ? chatRoomList.size() : (groupList.size() + 1);
        }

        class ChatViewHolder extends RecyclerView.ViewHolder {
            CircleImageView ivPartnerAvatar;
            TextView tvPartnerName, tvLastMessage, tvTimestamp;

            ChatViewHolder(@NonNull View itemView) {
                super(itemView);
                ivPartnerAvatar = itemView.findViewById(R.id.ivPartnerAvatar);
                tvPartnerName = itemView.findViewById(R.id.tvPartnerName);
                tvLastMessage = itemView.findViewById(R.id.tvLastMessage);
                tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
            }
        }

        class GroupViewHolder extends RecyclerView.ViewHolder {
            TextView tvGroupName, tvGroupCode;

            GroupViewHolder(@NonNull View itemView) {
                super(itemView);
                tvGroupName = itemView.findViewById(R.id.tvGroupName);
                tvGroupCode = itemView.findViewById(R.id.tvGroupCode);
            }
        }
    }
}
