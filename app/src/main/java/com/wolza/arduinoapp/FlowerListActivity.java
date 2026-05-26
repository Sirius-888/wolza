package com.wolza.arduinoapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class FlowerListActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private EditText searchBox;
    private ImageView btnBack;
    private TextView tvResultCount;
    private ProgressBar progressBar;
    private View emptyState;

    private FlowerAdapter adapter;
    private List<Flower> flowerList;
    private List<Flower> filteredList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flower_list);

        initViews();
        setupToolbar();
        setupRecyclerView();
        setupSearch();
        setupClickListeners();

        loadFlowersFromJSON();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerView);
        searchBox = findViewById(R.id.searchBox);
        btnBack = findViewById(R.id.btnBack);
        tvResultCount = findViewById(R.id.tvResultCount);
        progressBar = findViewById(R.id.progressBar);
        emptyState = findViewById(R.id.emptyState);

        flowerList = new ArrayList<>();
        filteredList = new ArrayList<>();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("All Flowers");
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        }
    }

    private void setupRecyclerView() {
        adapter = new FlowerAdapter(this, filteredList);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        recyclerView.setAdapter(adapter);

        adapter.setOnItemClickListener(position -> {
            Flower clickedFlower = filteredList.get(position);

            // Add to history
            HistoryActivity.addToHistory(FlowerListActivity.this, "search",
                    clickedFlower.getName(),
                   clickedFlower.getDescription(),
                   clickedFlower.getImageUrl());

            // Open detail activity
            Intent intent = new Intent(FlowerListActivity.this, FlowerDetailActivity.class);
            intent.putExtra("flower", clickedFlower);
            startActivity(intent);
        });
    }

    private void setupSearch() {
        searchBox.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterFlowers(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());
    }

    private void loadFlowersFromJSON() {
        showLoading(true);

        try {
            InputStream inputStream = getAssets().open("flowers.json");
            InputStreamReader reader = new InputStreamReader(inputStream);

            Gson gson = new Gson();
            Type listType = new TypeToken<List<Flower>>() {}.getType();

            flowerList = gson.fromJson(reader, listType);

            filteredList.clear();
            filteredList.addAll(flowerList);
            adapter.updateList(filteredList);
            updateResultCount();
            showLoading(false);

        } catch (Exception e) {
            showLoading(false);
            Toast.makeText(this, "Error loading flowers: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    private void filterFlowers(String query) {
        filteredList.clear();

        if (query.isEmpty()) {
            filteredList.addAll(flowerList);
        } else {
            String lowerCaseQuery = query.toLowerCase().trim();
            for (Flower flower : flowerList) {
                if (flower.getName().toLowerCase().contains(lowerCaseQuery)) {
                    filteredList.add(flower);
                }
            }
        }

        adapter.updateList(filteredList);
        updateResultCount();

        if (emptyState != null) {
            if (filteredList.isEmpty()) {
                emptyState.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
            } else {
                emptyState.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
            }
        }
    }

    private void updateResultCount() {
        if (tvResultCount != null && filteredList != null) {
            tvResultCount.setText(filteredList.size() + " flowers found");
        }
    }

    private void showLoading(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        if (recyclerView != null) {
            recyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    // Flower Model Class
    public static class Flower implements java.io.Serializable {
        private static final long serialVersionUID = 1L;

        private String name;
        private String scientific_name;
        private String description;
        private String image_url;
        private String category;

        public Flower() {}

        public Flower(String name, String scientific_name, String description, String image_url, String category) {
            this.name = name;
            this.scientific_name = scientific_name;
            this.description = description;
            this.image_url = image_url;
            this.category = category;
        }

        public String getName() { return name; }
        public String getScientificName() { return scientific_name; }
        public String getDescription() { return description; }
        public String getImageUrl() { return image_url; }
        public String getCategory() { return category; }

        public void setName(String name) { this.name = name; }
        public void setScientificName(String scientific_name) { this.scientific_name = scientific_name; }
        public void setDescription(String description) { this.description = description; }
        public void setImageUrl(String image_url) { this.image_url = image_url; }
        public void setCategory(String category) { this.category = category; }
    }
}