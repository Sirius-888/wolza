package com.wolza.arduinoapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class HistoryActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TextView tvEmpty;
    private TextView tvClearHistory;
    private TextView tabAll, tabSearch, tabDoctor;
    private ImageView btnBack;

    private HistoryAdapter adapter;
    private List<HistoryItem> allHistory;
    private List<HistoryItem> currentList;

    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "HistoryPrefs";
    private static final String KEY_HISTORY = "history_list";

    private String currentTab = "all";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        initViews();
        setupToolbar();
        setupRecyclerView();   // ✅ FIX: moved BEFORE loadHistory
        loadHistory();
        setupTabs();

        tvClearHistory.setOnClickListener(v -> clearAllHistory());
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerView);
        tvEmpty = findViewById(R.id.tvEmpty);
        tvClearHistory = findViewById(R.id.tvClearHistory);
        tabAll = findViewById(R.id.tabAll);
        tabSearch = findViewById(R.id.tabSearch);
        tabDoctor = findViewById(R.id.tabDoctor);
        btnBack = findViewById(R.id.btnBack);

        allHistory = new ArrayList<>();
        currentList = new ArrayList<>();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("📜 History");
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        }

        btnBack.setOnClickListener(v -> finish());
    }

    private void loadHistory() {
        String json = sharedPreferences.getString(KEY_HISTORY, "");

        if (!json.isEmpty()) {
            Gson gson = new Gson();
            Type type = new TypeToken<List<HistoryItem>>() {}.getType();
            allHistory = gson.fromJson(json, type);

            // ✅ safety fix
            if (allHistory == null) {
                allHistory = new ArrayList<>();
            }
        }

        // Sort by newest
        allHistory.sort((a, b) -> Long.compare(b.getTimestampLong(), a.getTimestampLong()));

        updateCurrentList();
    }

    private void saveHistory() {
        Gson gson = new Gson();
        String json = gson.toJson(allHistory);
        sharedPreferences.edit().putString(KEY_HISTORY, json).apply();
    }

    private void setupRecyclerView() {
        adapter = new HistoryAdapter(currentList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        adapter.setOnItemClickListener(position -> {
            HistoryItem item = currentList.get(position);
            showItemDetails(item);
        });
    }

    private void setupTabs() {
        tabAll.setOnClickListener(v -> {
            currentTab = "all";
            updateTabColors();
            updateCurrentList();
        });

        tabSearch.setOnClickListener(v -> {
            currentTab = "search";
            updateTabColors();
            updateCurrentList();
        });

        tabDoctor.setOnClickListener(v -> {
            currentTab = "doctor";
            updateTabColors();
            updateCurrentList();
        });
    }

    private void updateTabColors() {
        int selectedColor = getColor(android.R.color.white);
        int unselectedColor = getColor(R.color.text_secondary);

        if ("all".equals(currentTab)) {
            tabAll.setBackgroundResource(R.drawable.tab_selected);
            tabAll.setTextColor(selectedColor);

            tabSearch.setBackgroundResource(R.drawable.tab_unselected);
            tabSearch.setTextColor(unselectedColor);

            tabDoctor.setBackgroundResource(R.drawable.tab_unselected);
            tabDoctor.setTextColor(unselectedColor);

        } else if ("search".equals(currentTab)) {
            tabSearch.setBackgroundResource(R.drawable.tab_selected);
            tabSearch.setTextColor(selectedColor);

            tabAll.setBackgroundResource(R.drawable.tab_unselected);
            tabAll.setTextColor(unselectedColor);

            tabDoctor.setBackgroundResource(R.drawable.tab_unselected);
            tabDoctor.setTextColor(unselectedColor);

        } else if ("doctor".equals(currentTab)) {
            tabDoctor.setBackgroundResource(R.drawable.tab_selected);
            tabDoctor.setTextColor(selectedColor);

            tabAll.setBackgroundResource(R.drawable.tab_unselected);
            tabAll.setTextColor(unselectedColor);

            tabSearch.setBackgroundResource(R.drawable.tab_unselected);
            tabSearch.setTextColor(unselectedColor);
        }
    }

    private void updateCurrentList() {
        currentList.clear();

        if ("all".equals(currentTab)) {
            currentList.addAll(allHistory);

        } else if ("search".equals(currentTab)) {
            for (HistoryItem item : allHistory) {
                if ("search".equals(item.getType())) {
                    currentList.add(item);
                }
            }

        } else if ("doctor".equals(currentTab)) {
            for (HistoryItem item : allHistory) {
                if ("doctor".equals(item.getType())) {
                    currentList.add(item);
                }
            }
        }

        // ✅ safe call
        if (adapter != null) {
            adapter.updateList(currentList);
        }

        if (currentList.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void clearAllHistory() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Clear History")
                .setMessage("Clear all history?")
                .setPositiveButton("Clear", (dialog, which) -> {
                    allHistory.clear();
                    saveHistory();
                    updateCurrentList();
                    Toast.makeText(this, "History cleared", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showItemDetails(HistoryItem item) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(item.getName())
                .setMessage(item.getDescription() + "\n\n" + item.getTimestamp())
                .setPositiveButton("OK", null)
                .show();
    }

    // Static helper to add history
    public static void addToHistory(Context context, String type, String name, String description, String imageUrl) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_HISTORY, "");

        List<HistoryItem> historyList = new ArrayList<>();

        if (!json.isEmpty()) {
            Gson gson = new Gson();
            Type typeToken = new TypeToken<List<HistoryItem>>() {}.getType();
            historyList = gson.fromJson(json, typeToken);

            if (historyList == null) {
                historyList = new ArrayList<>();
            }
        }

        HistoryItem newItem = new HistoryItem(type, name, description, imageUrl, System.currentTimeMillis());
        historyList.add(0, newItem);

        if (historyList.size() > 100) {
            historyList = historyList.subList(0, 100);
        }

        Gson gson = new Gson();
        prefs.edit().putString(KEY_HISTORY, gson.toJson(historyList)).apply();
    }
}
