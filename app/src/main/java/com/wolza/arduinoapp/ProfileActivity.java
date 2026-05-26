package com.wolza.arduinoapp;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileActivity extends AppCompatActivity {

    private CircleImageView ivAvatar;
    private TextView tvEmail, tvChangePassword;
    private TextInputEditText etName, etSurname;
    private TextInputLayout tilName, tilSurname;
    private Button btnSave, btnCancel;
    private ImageView  btnBack;
    private Button btnEdit;
    private ProgressBar progressBar;
    private LinearLayout editLayout, viewLayout;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    private boolean isEditMode = false;
    private String currentAvatarBase64 = "";
    private static final int REQUEST_IMAGE_PICK = 100;
    private static final int REQUEST_IMAGE_CAPTURE = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        initViews();
        setupToolbar();
        setupFirebase();
        loadUserData();
        setupClickListeners();
    }

    private void initViews() {
        ivAvatar = findViewById(R.id.ivAvatar);
        tvEmail = findViewById(R.id.tvEmail);
        tvChangePassword = findViewById(R.id.tvChangePassword);
        etName = findViewById(R.id.etName);
        etSurname = findViewById(R.id.etSurname);
        tilName = findViewById(R.id.tilName);
        tilSurname = findViewById(R.id.tilSurname);
        btnSave = findViewById(R.id.btnSave);
        btnCancel = findViewById(R.id.btnCancel);
        btnEdit = findViewById(R.id.btnEdit);
        btnBack = findViewById(R.id.btnBack);
        progressBar = findViewById(R.id.progressBar);
        editLayout = findViewById(R.id.editLayout);
        viewLayout = findViewById(R.id.viewLayout);

        // Initially show view mode
        setEditMode(false);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("My Profile");
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        }
    }

    private void setupFirebase() {
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            tvEmail.setText(currentUser.getEmail());
        }
    }

    private void loadUserData() {
        showLoading(true);

        if (currentUser == null) {
            showLoading(false);
            return;
        }

        // Set email from Firebase Auth
        tvEmail.setText(currentUser.getEmail());

        DocumentReference userRef = db.collection("users").document(currentUser.getUid());
        userRef.get().addOnCompleteListener(task -> {
            showLoading(false);
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document.exists()) {
                    String name = document.getString("name");
                    String surname = document.getString("surname");
                    String avatarBase64 = document.getString("avatar");

                    // Find the TextView in view mode
                    TextView tvName = findViewById(R.id.tvName);
                    TextView tvSurname = findViewById(R.id.tvSurname);

                    if (tvName != null) {
                        tvName.setText(name != null ? name : "--");
                    }
                    if (tvSurname != null) {
                        tvSurname.setText(surname != null ? surname : "--");
                    }

                    // Set edit text fields
                    if (etName != null) etName.setText(name != null ? name : "");
                    if (etSurname != null) etSurname.setText(surname != null ? surname : "");

                    if (avatarBase64 != null && !avatarBase64.isEmpty()) {
                        currentAvatarBase64 = avatarBase64;
                        setAvatarImage(avatarBase64);
                    }
                } else {
                    // Create new user document if doesn't exist
                    createUserDocument();
                }
            }
        });
    }

    private void createUserDocument() {
        if (currentUser == null) return;

        Map<String, Object> userData = new HashMap<>();
        userData.put("name", "");
        userData.put("surname", "");
        userData.put("email", currentUser.getEmail());
        userData.put("createdAt", System.currentTimeMillis());
        userData.put("avatar", "");

        db.collection("users").document(currentUser.getUid())
                .set(userData);
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnEdit.setOnClickListener(v -> {
            setEditMode(true);
        });

        btnCancel.setOnClickListener(v -> {
            setEditMode(false);
            loadUserData(); // Reload original data
        });

        btnSave.setOnClickListener(v -> saveProfile());

        ivAvatar.setOnClickListener(v -> showAvatarPickerDialog());

        tvChangePassword.setOnClickListener(v -> changePassword());
    }

    private void setEditMode(boolean edit) {
        isEditMode = edit;

        if (edit) {
            viewLayout.setVisibility(View.GONE);
            editLayout.setVisibility(View.VISIBLE);
            btnEdit.setVisibility(View.GONE);
        } else {
            viewLayout.setVisibility(View.VISIBLE);
            editLayout.setVisibility(View.GONE);
            btnEdit.setVisibility(View.VISIBLE);
        }
    }

    private void showAvatarPickerDialog() {
        String[] options = {"Take Photo", "Choose from Gallery", "Cancel"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Change Avatar");
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {
                // Take Photo
                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                }
            } else if (which == 1) {
                // Choose from Gallery
                Intent pickPhotoIntent = new Intent(Intent.ACTION_PICK,
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(pickPhotoIntent, REQUEST_IMAGE_PICK);
            }
        });
        builder.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && data != null) {
            Bitmap bitmap = null;

            try {
                if (requestCode == REQUEST_IMAGE_CAPTURE) {
                    // Camera photo
                    Bundle extras = data.getExtras();
                    bitmap = (Bitmap) extras.get("data");
                } else if (requestCode == REQUEST_IMAGE_PICK) {
                    // Gallery photo
                    Uri selectedImage = data.getData();
                    bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedImage);
                }

                if (bitmap != null) {
                    // Compress and convert to Base64
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
                    byte[] imageBytes = baos.toByteArray();
                    currentAvatarBase64 = Base64.encodeToString(imageBytes, Base64.DEFAULT);

                    // Update avatar preview
                    ivAvatar.setImageBitmap(bitmap);

                    // Auto-save if in edit mode
                    if (isEditMode) {
                        saveAvatarOnly();
                    } else {
                        // If not in edit mode, auto-save the avatar
                        saveAvatarOnly();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Error loading image", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void saveAvatarOnly() {
        if (currentUser == null) return;

        showLoading(true);
        Map<String, Object> updates = new HashMap<>();
        updates.put("avatar", currentAvatarBase64);

        db.collection("users").document(currentUser.getUid())
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    showLoading(false);
                    Toast.makeText(this, "Avatar updated", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Failed to update avatar", Toast.LENGTH_SHORT).show();
                });
    }

    private void saveProfile() {
        String name = etName.getText().toString().trim();
        String surname = etSurname.getText().toString().trim();

        if (name.isEmpty()) {
            tilName.setError("Name is required");
            return;
        }

        showLoading(true);

        Map<String, Object> userData = new HashMap<>();
        userData.put("name", name);
        userData.put("surname", surname);
        userData.put("email", currentUser.getEmail());

        if (!currentAvatarBase64.isEmpty()) {
            userData.put("avatar", currentAvatarBase64);
        }

        db.collection("users").document(currentUser.getUid())
                .set(userData)
                .addOnSuccessListener(aVoid -> {
                    showLoading(false);
                    Toast.makeText(ProfileActivity.this, "Profile updated!", Toast.LENGTH_SHORT).show();
                    setEditMode(false);

                    // Update display name in Firebase Auth
                    String fullName = name + " " + surname;
                    currentUser.updateProfile(
                            new com.google.firebase.auth.UserProfileChangeRequest.Builder()
                                    .setDisplayName(fullName)
                                    .build()
                    );
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(ProfileActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void setAvatarImage(String base64Image) {
        try {
            byte[] decodedString = Base64.decode(base64Image, Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
            ivAvatar.setImageBitmap(bitmap);
        } catch (Exception e) {
            ivAvatar.setImageResource(R.drawable.ic_profile);
        }
    }

    private void changePassword() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Change Password");

        final EditText input = new EditText(this);
        input.setHint("Enter new password (min 6 characters)");
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT |
                android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        builder.setView(input);

        builder.setPositiveButton("Update", (dialog, which) -> {
            String newPassword = input.getText().toString().trim();
            if (newPassword.length() < 6) {
                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                return;
            }

            showLoading(true);
            currentUser.updatePassword(newPassword)
                    .addOnCompleteListener(task -> {
                        showLoading(false);
                        if (task.isSuccessful()) {
                            Toast.makeText(ProfileActivity.this,
                                    "Password changed successfully!", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(ProfileActivity.this,
                                    "Error: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnSave.setEnabled(!show);
        btnCancel.setEnabled(!show);
        btnEdit.setEnabled(!show);
    }
}