package com.wolza.arduinoapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class LoginActivity extends AppCompatActivity {

    private TextInputLayout tilEmail, tilPassword;
    private EditText etEmail, etPassword, etPhone;
    private Button btnLogin, btnPhone, btnGuest;
    private TextView tvSignupRedirect, tvForgotPassword, tvPhoneTab, tvEmailTab;
    private ProgressBar progressBar;
    private LinearLayout emailLoginLayout, phoneLoginLayout;
    private View loadingBackground;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private String verificationId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initViews();
        setupClickListeners();

        if (mAuth.getCurrentUser() != null) {
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish();
        }
    }

    private void initViews() {
        tilEmail = findViewById(R.id.tilEmail);
        tilPassword = findViewById(R.id.tilPassword);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etPhone = findViewById(R.id.etPhone);
        btnLogin = findViewById(R.id.btnLogin);
        btnPhone = findViewById(R.id.btnPhone);
        btnGuest = findViewById(R.id.btnGuest);
        tvSignupRedirect = findViewById(R.id.tvSignupRedirect);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        tvPhoneTab = findViewById(R.id.tvPhoneTab);
        tvEmailTab = findViewById(R.id.tvEmailTab);
        progressBar = findViewById(R.id.progressBar);
        loadingBackground = findViewById(R.id.loadingBackground);
        emailLoginLayout = findViewById(R.id.emailLoginLayout);
        phoneLoginLayout = findViewById(R.id.phoneLoginLayout);

        showEmailTab();

        etEmail.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { tilEmail.setError(null); }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });
        etPassword.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { tilPassword.setError(null); }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });
    }

    private void setupClickListeners() {
        btnLogin.setOnClickListener(v -> loginWithEmail());
        btnPhone.setOnClickListener(v -> sendPhoneVerification());
        btnGuest.setOnClickListener(v -> loginAsGuest());
        tvEmailTab.setOnClickListener(v -> showEmailTab());
        tvPhoneTab.setOnClickListener(v -> showPhoneTab());
        tvSignupRedirect.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, SignUpActivity.class));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });
        tvForgotPassword.setOnClickListener(v -> resetPassword());
    }

    private void loginAsGuest() {
        showLoading(true);
        // Anonymous Auth: each guest gets their OWN unique Firebase UID
        // → unlimited simultaneous guests, no data conflicts
        mAuth.signInAnonymously()
                .addOnCompleteListener(this, task -> {
                    showLoading(false);
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        // Store guest flag in Firestore so the app knows it's a guest
                        if (user != null) {
                            Map<String, Object> guestData = new HashMap<>();
                            guestData.put("name", "Guest");
                            guestData.put("email", "innovationcampus26@gmail.com");
                            guestData.put("isGuest", true);
                            guestData.put("createdAt", System.currentTimeMillis());
                            db.collection("users").document(user.getUid())
                                    .set(guestData)
                                    .addOnCompleteListener(t -> handleGuestSuccess());
                        } else {
                            handleGuestSuccess();
                        }
                    } else {
                        // Anonymous Auth not enabled — show instructions dialog
                        String msg = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                        new androidx.appcompat.app.AlertDialog.Builder(this)
                                .setTitle("⚙️ One-Time Setup Required")
                                .setMessage(
                                        "To allow Guest Mode, please enable Anonymous Auth in Firebase:\n\n" +
                                        "1. Go to console.firebase.google.com\n" +
                                        "2. Select your project\n" +
                                        "3. Authentication → Sign-in method\n" +
                                        "4. Enable 'Anonymous'\n" +
                                        "5. Save and try again\n\n" +
                                        "(Error: " + msg + ")")
                                .setPositiveButton("OK", null)
                                .show();
                    }
                });
    }

    private void handleGuestSuccess() {
        Toast.makeText(LoginActivity.this, "Welcome, Guest! 👋", Toast.LENGTH_SHORT).show();
        startActivity(new Intent(LoginActivity.this, MainActivity.class));
        finish();
    }


    private void loginWithEmail() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        if (TextUtils.isEmpty(email)) { tilEmail.setError("Email required"); return; }
        if (TextUtils.isEmpty(password)) { tilPassword.setError("Password required"); return; }
        showLoading(true);
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    showLoading(false);
                    if (task.isSuccessful()) {
                        startActivity(new Intent(LoginActivity.this, MainActivity.class));
                        finish();
                    } else {
                        Toast.makeText(LoginActivity.this, "Login Error: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void sendPhoneVerification() {
        String phone = etPhone.getText().toString().trim();
        if (TextUtils.isEmpty(phone)) { Toast.makeText(this, "Enter phone number", Toast.LENGTH_SHORT).show(); return; }
        if (!phone.startsWith("+")) phone = phone.startsWith("0") ? "+374" + phone.substring(1) : "+374" + phone;
        showLoading(true);
        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(mAuth)
                .setPhoneNumber(phone).setTimeout(60L, TimeUnit.SECONDS).setActivity(this)
                .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    @Override public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) { showLoading(false); signInWithPhoneCredential(credential); }
                    @Override public void onVerificationFailed(@NonNull FirebaseException e) { showLoading(false); Toast.makeText(LoginActivity.this, "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show(); }
                    @Override public void onCodeSent(@NonNull String vId, @NonNull PhoneAuthProvider.ForceResendingToken token) {
                        showLoading(false); LoginActivity.this.verificationId = vId; showOTPDialog();
                    }
                }).build();
        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    private void showOTPDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter Code");
        final EditText input = new EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        builder.setView(input);
        builder.setPositiveButton("Verify", (dialog, which) -> {
            String code = input.getText().toString().trim();
            if (code.length() == 6) signInWithPhoneCredential(PhoneAuthProvider.getCredential(verificationId, code));
        });
        builder.setNegativeButton("Cancel", null).show();
    }

    private void signInWithPhoneCredential(PhoneAuthCredential credential) {
        showLoading(true);
        mAuth.signInWithCredential(credential).addOnCompleteListener(this, task -> {
            showLoading(false);
            if (task.isSuccessful()) { startActivity(new Intent(LoginActivity.this, MainActivity.class)); finish(); }
            else Toast.makeText(LoginActivity.this, "Error: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
        });
    }

    private void resetPassword() {
        String currentEmail = etEmail.getText().toString().trim();
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Reset Password");
        final EditText input = new EditText(this);
        input.setText(currentEmail);
        builder.setView(input);
        builder.setPositiveButton("Send", (dialog, which) -> {
            String email = input.getText().toString().trim();
            if (!TextUtils.isEmpty(email)) {
                showLoading(true);
                mAuth.sendPasswordResetEmail(email).addOnCompleteListener(task -> {
                    showLoading(false);
                    Toast.makeText(LoginActivity.this, task.isSuccessful() ? "Email sent" : "Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
        builder.setNegativeButton("Cancel", null).show();
    }

    private void showEmailTab() {
        tvEmailTab.setBackgroundResource(R.drawable.tab_selected);
        tvEmailTab.setTextColor(getColor(android.R.color.white));
        tvPhoneTab.setBackgroundResource(R.drawable.tab_unselected);
        tvPhoneTab.setTextColor(getColor(R.color.text_secondary));
        emailLoginLayout.setVisibility(View.VISIBLE);
        phoneLoginLayout.setVisibility(View.GONE);
    }

    private void showPhoneTab() {
        tvPhoneTab.setBackgroundResource(R.drawable.tab_selected);
        tvPhoneTab.setTextColor(getColor(android.R.color.white));
        tvEmailTab.setBackgroundResource(R.drawable.tab_unselected);
        tvEmailTab.setTextColor(getColor(R.color.text_secondary));
        emailLoginLayout.setVisibility(View.GONE);
        phoneLoginLayout.setVisibility(View.VISIBLE);
    }

    private void showLoading(boolean show) {
        if (progressBar != null) progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        if (loadingBackground != null) loadingBackground.setVisibility(show ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!show);
        btnPhone.setEnabled(!show);
        btnGuest.setEnabled(!show);
    }
}