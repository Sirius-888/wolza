package com.wolza.arduinoapp;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.IOException;
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

    private static final int PICK_IMAGE_REQUEST = 1;

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

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnSelectImage.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
        });

        btnPost.setOnClickListener(v -> uploadPost());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                ivSelectedImage.setImageBitmap(bitmap);
                ivSelectedImage.setVisibility(View.VISIBLE);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
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
            uploadImageAndPost(title, content);
        } else {
            savePostToFirestore(title, content, "");
        }
    }

    private void uploadImageAndPost(String title, String content) {
        String fileName = UUID.randomUUID().toString();
        StorageReference ref = storage.getReference().child("post_images/" + fileName);

        ref.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> ref.getDownloadUrl().addOnSuccessListener(uri -> {
                    savePostToFirestore(title, content, uri.toString());
                }))
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(AddPostActivity.this, "Image upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void savePostToFirestore(String title, String content, String imageUrl) {
        String userName = currentUser.getDisplayName() != null ? currentUser.getDisplayName() : "Anonymous";
        String userAvatar = currentUser.getPhotoUrl() != null ? currentUser.getPhotoUrl().toString() : "";

        CommunityPost post = new CommunityPost(
                groupId,
                currentUser.getUid(),
                userName,
                userAvatar,
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
