package com.wolza.arduinoapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class MarketActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TextView tvEmpty, tvMyListings;
    private ImageView btnBack;
    private FloatingActionButton fabAddItem;
    private MarketAdapter adapter;
    private List<MarketItem> marketItems;
    private List<MarketItem> myItems;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;

    private boolean showMyListings = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_market);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        initViews();
        setupToolbar();
        setupRecyclerView();
        setupFab();
        loadItems();

        tvMyListings.setOnClickListener(v -> {
            showMyListings = !showMyListings;
            if (showMyListings) {
                tvMyListings.setText("📦 My Listings");
                tvMyListings.setTextColor(getResources().getColor(android.R.color.holo_orange_dark, getTheme()));
                adapter.updateList(myItems);
                updateEmptyState(myItems);
            } else {
                tvMyListings.setText("📦 All Listings");
                tvMyListings.setTextColor(getResources().getColor(android.R.color.holo_green_dark, getTheme()));
                adapter.updateList(marketItems);
                updateEmptyState(marketItems);
            }
        });
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerView);
        tvEmpty = findViewById(R.id.tvEmpty);
        tvMyListings = findViewById(R.id.tvMyListings);
        btnBack = findViewById(R.id.btnBack);
        fabAddItem = findViewById(R.id.fabAddItem);

        marketItems = new ArrayList<>();
        myItems = new ArrayList<>();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("🌿 Flower Market");
        }
        btnBack.setOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        adapter = new MarketAdapter(this, marketItems);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        recyclerView.setAdapter(adapter);

        adapter.setOnItemClickListener(position -> {
            MarketItem item = showMyListings ? myItems.get(position) : marketItems.get(position);
            showItemDialog(item);
        });
    }

    private void setupFab() {
        fabAddItem.setOnClickListener(v -> {
            Intent intent = new Intent(MarketActivity.this, AddItemActivity.class);
            startActivityForResult(intent, 100);
        });
    }

    private void loadItems() {
        db.collection("market_items")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    marketItems.clear();
                    myItems.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        MarketItem item = document.toObject(MarketItem.class);
                        item.setId(document.getId());
                        marketItems.add(item);
                        if (currentUser != null && item.getSellerId() != null && item.getSellerId().equals(currentUser.getUid())) {
                            myItems.add(item);
                        }
                    }
                    adapter.updateList(marketItems);
                    updateEmptyState(marketItems);
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error loading items", Toast.LENGTH_SHORT).show());
    }

    private void showItemDialog(MarketItem item) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_market_item, null);
        ImageView imgItem = dialogView.findViewById(R.id.imgItem);
        CircleImageView imgSeller = dialogView.findViewById(R.id.imgSeller);
        TextView tvName = dialogView.findViewById(R.id.tvName);
        TextView tvPrice = dialogView.findViewById(R.id.tvPrice);
        TextView tvDescription = dialogView.findViewById(R.id.tvDescription);
        TextView tvSeller = dialogView.findViewById(R.id.tvSeller);
        ImageView btnMessage = dialogView.findViewById(R.id.btnMessage);
        ImageView btnCall = dialogView.findViewById(R.id.btnCall);

        tvName.setText(item.getName());
        tvPrice.setText(item.getPrice() + " AMD");
        tvDescription.setText(item.getDescription());
        tvSeller.setText(item.getSellerName());

        // Load Item Image
        String imageSource = item.getImageUrl();
        if (imageSource != null && !imageSource.isEmpty()) {
            if (imageSource.startsWith("http")) {
                Glide.with(this).load(imageSource).placeholder(R.drawable.ic_flower).into(imgItem);
            } else {
                try {
                    byte[] bytes = Base64.decode(imageSource, Base64.NO_WRAP);
                    Glide.with(this).load(bytes).placeholder(R.drawable.ic_flower).into(imgItem);
                } catch (Exception e) {
                    imgItem.setImageResource(R.drawable.ic_flower);
                }
            }
        }

        // Load Seller Profile Image
        if (item.getSellerId() != null) {
            db.collection("users").document(item.getSellerId()).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String avatarBase64 = documentSnapshot.getString("avatar");
                            String phone = documentSnapshot.getString("phone"); // We'll try to get phone
                            if (avatarBase64 != null && !avatarBase64.isEmpty()) {
                                try {
                                    byte[] bytes = Base64.decode(avatarBase64, Base64.DEFAULT);
                                    Glide.with(this).load(bytes).into(imgSeller);
                                } catch (Exception e) {
                                    imgSeller.setImageResource(R.drawable.ic_community);
                                }
                            }
                            
                            // Setup Call Button
                            btnCall.setOnClickListener(v -> {
                                String number = (phone != null && !phone.isEmpty()) ? phone : "5550199"; // Default or placeholder
                                Intent intent = new Intent(Intent.ACTION_DIAL);
                                intent.setData(Uri.parse("tel:" + number));
                                startActivity(intent);
                            });
                        }
                    });
        }

        btnMessage.setOnClickListener(v -> Toast.makeText(this, "Messaging " + item.getSellerName() + "...", Toast.LENGTH_SHORT).show());

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton("Close", null)
                .show();
    }

    private void updateEmptyState(List<MarketItem> list) {
        if (list == null || list.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK) loadItems();
    }

    public static class MarketItem {
        private String id, name, description, price, imageUrl, sellerId, sellerName;
        private long timestamp;
        public MarketItem() {}
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getPrice() { return price; }
        public void setPrice(String price) { this.price = price; }
        public String getImageUrl() { return imageUrl; }
        public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
        public String getSellerId() { return sellerId; }
        public void setSellerId(String sellerId) { this.sellerId = sellerId; }
        public String getSellerName() { return sellerName; }
        public void setSellerName(String sellerName) { this.sellerName = sellerName; }
        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    }
}