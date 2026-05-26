package com.wolza.arduinoapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class SignUpActivity extends AppCompatActivity {

    private TextInputLayout tilName, tilEmail, tilPassword, tilConfirmPassword;
    private EditText etName, etEmail, etPassword, etConfirmPassword;
    private Button btnSignup;
    private TextView tvLoginRedirect;
    private ProgressBar progressBar;
    private View loadingBackground;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initViews();
        setupClickListeners();
    }

    private void initViews() {
        tilName = findViewById(R.id.tilName);
        tilEmail = findViewById(R.id.tilEmail);
        tilPassword = findViewById(R.id.tilPassword);
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword);
        progressBar = findViewById(R.id.progressBar);
        loadingBackground = findViewById(R.id.loadingBackground);

        etName = (EditText) tilName.getEditText();
        etEmail = (EditText) tilEmail.getEditText();
        etPassword = (EditText) tilPassword.getEditText();
        etConfirmPassword = (EditText) tilConfirmPassword.getEditText();

        btnSignup = findViewById(R.id.btnSignup);
        tvLoginRedirect = findViewById(R.id.tvLoginRedirect);
    }

    private void setupClickListeners() {
        btnSignup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                attemptSignUp();
            }
        });

        tvLoginRedirect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(SignUpActivity.this, LoginActivity.class));
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                finish();
            }
        });
    }

    private void attemptSignUp() {
        tilName.setError(null);
        tilEmail.setError(null);
        tilPassword.setError(null);
        tilConfirmPassword.setError(null);

        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        boolean isValid = true;
        View focusView = null;

        // Validate Name
        if (TextUtils.isEmpty(name)) {
            tilName.setError("Full name is required");
            focusView = tilName;
            isValid = false;
        } else if (name.length() < 3) {
            tilName.setError("Name must be at least 3 characters");
            focusView = tilName;
            isValid = false;
        }

        // Validate Email
        if (TextUtils.isEmpty(email)) {
            tilEmail.setError("Email is required");
            if (focusView == null) focusView = tilEmail;
            isValid = false;
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Please enter a valid email address");
            if (focusView == null) focusView = tilEmail;
            isValid = false;
        }

        // Validate Password
        if (TextUtils.isEmpty(password)) {
            tilPassword.setError("Password is required");
            if (focusView == null) focusView = tilPassword;
            isValid = false;
        } else if (password.length() < 6) {
            tilPassword.setError("Password must be at least 6 characters");
            if (focusView == null) focusView = tilPassword;
            isValid = false;
        }

        // Validate Confirm Password
        if (TextUtils.isEmpty(confirmPassword)) {
            tilConfirmPassword.setError("Please confirm your password");
            if (focusView == null) focusView = tilConfirmPassword;
            isValid = false;
        } else if (!password.equals(confirmPassword)) {
            tilConfirmPassword.setError("Passwords do not match");
            if (focusView == null) focusView = tilConfirmPassword;
            isValid = false;
        }

        if (!isValid) {
            if (focusView != null) {
                focusView.requestFocus();
            }
        } else {
            createAccount(name, email, password);
        }
    }

    private void createAccount(String name, String email, String password) {
        showLoading(true);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();

                            if (user != null) {
                                // Update display name in Firebase Auth
                                UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                        .setDisplayName(name)
                                        .build();
                                user.updateProfile(profileUpdates);

                                // Save user data to Firestore
                                saveUserToFirestore(user.getUid(), name, email);
                            }
                        } else {
                            showLoading(false);
                            String errorMessage = task.getException().getMessage();
                            Toast.makeText(SignUpActivity.this,
                                    "Registration failed: " + errorMessage,
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private void saveUserToFirestore(String userId, String name, String email) {
        Map<String, Object> user = new HashMap<>();
        user.put("name", name);
        user.put("surname", "");  // Empty initially
        user.put("email", email);
        user.put("createdAt", System.currentTimeMillis());
        user.put("avatar", "");   // Empty initially

        db.collection("users").document(userId)
                .set(user)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        showLoading(false);
                        if (task.isSuccessful()) {
                            Toast.makeText(SignUpActivity.this,
                                    "Account created successfully!", Toast.LENGTH_LONG).show();

                            // Go to Login
                            Intent intent = new Intent(SignUpActivity.this, LoginActivity.class);
                            intent.putExtra("email", email);
                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            startActivity(intent);
                            finish();
                        } else {
                            Toast.makeText(SignUpActivity.this,
                                    "Account created but failed to save user data", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(SignUpActivity.this, LoginActivity.class);
                            startActivity(intent);
                            finish();
                        }
                    }
                });
    }

    private void showLoading(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        if (loadingBackground != null) {
            loadingBackground.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        btnSignup.setEnabled(!show);
        btnSignup.setText(show ? "Creating Account..." : "Sign Up");
    }
}