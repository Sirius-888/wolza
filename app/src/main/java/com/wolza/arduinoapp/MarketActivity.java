package com.wolza.arduinoapp;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.util.Base64;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class MarketActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TextView tvEmpty, tvMyListings;
    private ImageView btnBack;
    private FloatingActionButton fabAddItem;
    private TabLayout tabLayout;
    private MarketAdapter adapter;
    
    private List<MarketItem> allItems = new ArrayList<>();
    private List<MarketItem> filteredItems = new ArrayList<>();
    private List<MarketItem> myItems = new ArrayList<>();

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;

    private boolean showMyListings = false;
    private int currentMode = 0; // 0: Plants, 1: Tools, 2: Wolza Shop

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }

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
        setupTabs();
        setupFab();
        loadItems();

        tvMyListings.setOnClickListener(v -> {
            showMyListings = !showMyListings;
            updateListDisplay();
        });
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerView);
        tvEmpty = findViewById(R.id.tvEmpty);
        tvMyListings = findViewById(R.id.tvMyListings);
        btnBack = findViewById(R.id.btnBack);
        fabAddItem = findViewById(R.id.fabAddItem);
        tabLayout = findViewById(R.id.tabLayout);
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
        adapter = new MarketAdapter(this, filteredItems);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        recyclerView.setAdapter(adapter);

        adapter.setOnItemClickListener(position -> {
            MarketItem item = filteredItems.get(position);
            showItemDialog(item);
        });
    }

    private void setupTabs() {
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentMode = tab.getPosition();
                updateListDisplay();
            }
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}
            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupFab() {
        fabAddItem.setOnClickListener(v -> {
            if (currentMode == 2) {
                checkAdminAccess();
            } else {
                Intent intent = new Intent(MarketActivity.this, AddItemActivity.class);
                intent.putExtra("category", currentMode == 0 ? "Plants" : "Tools");
                startActivityForResult(intent, 100);
            }
        });
    }

    private void checkAdminAccess() {
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        input.setHint("Enter Admin Code");

        new AlertDialog.Builder(this)
                .setTitle("Admin Access")
                .setMessage("Enter the code to edit Wolza Shop")
                .setView(input)
                .setPositiveButton("Verify", (dialog, which) -> {
                    String code = input.getText().toString();
                    if (code.equals("chgitem777")) {
                        Intent intent = new Intent(MarketActivity.this, AddItemActivity.class);
                        intent.putExtra("category", "Wolza");
                        startActivityForResult(intent, 100);
                    } else {
                        Toast.makeText(this, "Incorrect Code!", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void loadItems() {
        db.collection("market_items")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    allItems.clear();
                    myItems.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        MarketItem item = document.toObject(MarketItem.class);
                        item.setId(document.getId());
                        allItems.add(item);
                        if (currentUser != null && item.getSellerId() != null && item.getSellerId().equals(currentUser.getUid())) {
                            myItems.add(item);
                        }
                    }
                    updateListDisplay();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error loading items", Toast.LENGTH_SHORT).show());
    }

    private void updateListDisplay() {
        filteredItems.clear();
        String targetCategory;
        switch (currentMode) {
            case 1: targetCategory = "Tools"; break;
            case 2: targetCategory = "Wolza"; break;
            default: targetCategory = "Plants"; break;
        }

        List<MarketItem> sourceList = showMyListings ? myItems : allItems;
        
        for (MarketItem item : sourceList) {
            String itemCat = item.getCategory() != null ? item.getCategory() : "Plants";
            if (itemCat.equalsIgnoreCase(targetCategory)) {
                filteredItems.add(item);
            }
        }

        if (showMyListings) {
            tvMyListings.setText("📦 Showing My Listings");
            tvMyListings.setTextColor(getResources().getColor(android.R.color.holo_orange_dark, getTheme()));
        } else {
            tvMyListings.setText("📦 Showing All Listings");
            tvMyListings.setTextColor(getResources().getColor(android.R.color.holo_green_dark, getTheme()));
        }

        adapter.updateList(filteredItems);
        updateEmptyState(filteredItems);
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

        if (item.getCategory() != null && item.getCategory().equals("Wolza")) {
            imgSeller.setImageResource(R.drawable.ic_launcher_foreground); // Wolza logo placeholder
            tvSeller.setText("Official Wolza Shop");
            btnCall.setVisibility(View.GONE);
            btnMessage.setOnClickListener(v -> Toast.makeText(this, "Contacting Wolza Support...", Toast.LENGTH_SHORT).show());
        } else if (item.getSellerId() != null) {
            // Clicking seller image or name opens their profile
            View.OnClickListener openProfileListener = v -> {
                Intent profileIntent = new Intent(MarketActivity.this, ProfileActivity.class);
                profileIntent.putExtra("userId", item.getSellerId());
                startActivity(profileIntent);
            };
            imgSeller.setOnClickListener(openProfileListener);
            tvSeller.setOnClickListener(openProfileListener);

            db.collection("users").document(item.getSellerId()).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String avatarBase64 = documentSnapshot.getString("avatar");
                            String phone = documentSnapshot.getString("phone");
                            if (avatarBase64 != null && !avatarBase64.isEmpty()) {
                                try {
                                    byte[] bytes = Base64.decode(avatarBase64, Base64.DEFAULT);
                                    Glide.with(this).load(bytes).into(imgSeller);
                                } catch (Exception e) {
                                    imgSeller.setImageResource(R.drawable.ic_community);
                                }
                            }
                            btnCall.setOnClickListener(v -> {
                                String number = (phone != null && !phone.isEmpty()) ? phone : "5550199";
                                Intent intent = new Intent(Intent.ACTION_DIAL);
                                intent.setData(Uri.parse("tel:" + number));
                                startActivity(intent);
                            });
                        }
                    });

            btnMessage.setOnClickListener(v -> {
                if (currentUser == null) {
                    Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (item.getSellerId().equals(currentUser.getUid())) {
                    Toast.makeText(this, "This is your own listing", Toast.LENGTH_SHORT).show();
                    return;
                }
                Intent chatIntent = new Intent(MarketActivity.this, DirectChatActivity.class);
                chatIntent.putExtra("partnerId", item.getSellerId());
                startActivity(chatIntent);
            });
        }

        new AlertDialog.Builder(this)
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
        private String id, name, description, price, imageUrl, sellerId, sellerName, category;
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
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
    }
}
