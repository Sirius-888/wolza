package com.wolza.arduinoapp;

import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.UUID;

public class AddPostActivity extends AppCompatActivity {

    private EditText etTitle, etContent;
    private ImageView ivSelectedImage, btnBack;
    private Button btnSelectImage, btnPost;
    private ProgressBar progressBar;

    private Uri imageUri;
    private String groupId;
    
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;

    private String currentUserName = "Anonymous";
    private String currentUserAvatar = "";

    private final ActivityResultLauncher<String> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    imageUri = uri;
                    ivSelectedImage.setVisibility(View.VISIBLE);
                    Glide.with(this).load(imageUri).into(ivSelectedImage);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_post);

        groupId = getIntent().getStringExtra("groupId");
        if (groupId == null) {
            Toast.makeText(this, "Error: Group not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        initViews();
        loadUserData();
        setupClickListeners();
    }

    private void initViews() {
        etTitle = findViewById(R.id.etTitle);
        etContent = findViewById(R.id.etContent);
        ivSelectedImage = findViewById(R.id.ivSelectedImage);
        btnBack = findViewById(R.id.btnBack);
        btnSelectImage = findViewById(R.id.btnSelectImage);
        btnPost = findViewById(R.id.btnPost);
        progressBar = findViewById(R.id.progressBar);
    }

    private void loadUserData() {
        if (currentUser == null) return;
        db.collection("users").document(currentUser.getUid())
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        String name = document.getString("name");
                        currentUserName = (name != null && !name.isEmpty()) ? name : 
                                (currentUser.getEmail() != null ? currentUser.getEmail().split("@")[0] : "User");
                        String avatar = document.getString("avatar");
                        if (avatar != null) currentUserAvatar = avatar;
                    }
                });
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnSelectImage.setOnClickListener(v -> {
            imagePickerLauncher.launch("image/*");
        });

        btnPost.setOnClickListener(v -> uploadPost());
    }

    private void uploadPost() {
        String title = etTitle.getText().toString().trim();
        String content = etContent.getText().toString().trim();

        if (TextUtils.isEmpty(title)) {
            etTitle.setError("Title is required");
            return;
        }

        if (currentUser == null) {
            Toast.makeText(this, "Please login to post", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true);

        if (imageUri != null) {
            try {
                // Resize to max 500px to keep Firestore payload size within limits (1MB limit)
                android.graphics.Bitmap resizedBitmap = ImageUtils.handleImageUri(this, imageUri, 500);
                if (resizedBitmap != null) {
                    String base64Image = ImageUtils.bitmapToBase64(resizedBitmap, 60);
                    String dataUrl = "data:image/jpeg;base64," + base64Image;
                    savePostToFirestore(title, content, dataUrl);
                } else {
                    showLoading(false);
                    Toast.makeText(this, "Cannot process selected image", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                showLoading(false);
                Toast.makeText(this, "Error processing image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        } else {
            savePostToFirestore(title, content, "");
        }
    }

    private void savePostToFirestore(String title, String content, String imageUrl) {
        CommunityPost post = new CommunityPost(
                groupId,
                currentUser.getUid(),
                currentUserName,
                currentUserAvatar,
                title,
                content,
                imageUrl,
                System.currentTimeMillis()
        );

        db.collection("community_posts")
                .add(post)
                .addOnSuccessListener(documentReference -> {
                    showLoading(false);
                    Toast.makeText(AddPostActivity.this, "Post created!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(AddPostActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void showLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnPost.setEnabled(!loading);
        btnSelectImage.setEnabled(!loading);
        etTitle.setEnabled(!loading);
        etContent.setEnabled(!loading);
    }
}
