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
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;

import java.util.concurrent.TimeUnit;

public class LoginActivity extends AppCompatActivity {

    private TextInputLayout tilEmail, tilPassword;
    private EditText etEmail, etPassword, etPhone;
    private Button btnLogin, btnPhone;
    private TextView tvSignupRedirect, tvForgotPassword, tvPhoneTab, tvEmailTab;
    private ProgressBar progressBar;
    private LinearLayout emailLoginLayout, phoneLoginLayout;
    private View loadingBackground;

    private FirebaseAuth mAuth;

    private String verificationId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Initialize views
        initViews();

        // Set click listeners
        setupClickListeners();

        // Check if already logged in
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

        tvSignupRedirect = findViewById(R.id.tvSignupRedirect);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        tvPhoneTab = findViewById(R.id.tvPhoneTab);
        tvEmailTab = findViewById(R.id.tvEmailTab);

        progressBar = findViewById(R.id.progressBar);
        loadingBackground = findViewById(R.id.loadingBackground);
        emailLoginLayout = findViewById(R.id.emailLoginLayout);
        phoneLoginLayout = findViewById(R.id.phoneLoginLayout);

        // Start with email tab
        showEmailTab();
    }

    private void setupClickListeners() {
        // Email/Password Login
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loginWithEmail();
            }
        });

        // Phone Sign-In
        btnPhone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendPhoneVerification();
            }
        });

        // Tabs
        tvEmailTab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showEmailTab();
            }
        });

        tvPhoneTab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPhoneTab();
            }
        });

        tvSignupRedirect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LoginActivity.this, SignUpActivity.class));
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            }
        });

        tvForgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetPassword();
            }
        });
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

    private void loginWithEmail() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            tilEmail.setError("Email required");
            return;
        }
        if (TextUtils.isEmpty(password)) {
            tilPassword.setError("Password required");
            return;
        }

        showLoading(true);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        showLoading(false);
                        if (task.isSuccessful()) {
                            Toast.makeText(LoginActivity.this, "Login Successful!", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(LoginActivity.this, MainActivity.class));
                            finish();
                        } else {
                            Toast.makeText(LoginActivity.this, "Error: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private void sendPhoneVerification() {
        String phone = etPhone.getText().toString().trim();
        if (TextUtils.isEmpty(phone)) {
            Toast.makeText(this, "Enter phone number", Toast.LENGTH_SHORT).show();
            return;
        }

        // Format Armenian phone number
        if (!phone.startsWith("+")) {
            if (phone.startsWith("0")) {
                phone = "+374" + phone.substring(1);
            } else {
                phone = "+374" + phone;
            }
        }

        // Show loading
        showLoading(true);
        Toast.makeText(this, "Sending code to " + phone, Toast.LENGTH_LONG).show();

        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(mAuth)
                .setPhoneNumber(phone)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(this)
                .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    @Override
                    public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                        // Auto-retrieval or instant verification
                        showLoading(false);
                        signInWithPhoneCredential(credential);
                    }

                    @Override
                    public void onVerificationFailed(@NonNull FirebaseException e) {
                        showLoading(false);
                        String error = e.getMessage();
                        Toast.makeText(LoginActivity.this,
                                "Failed: " + error, Toast.LENGTH_LONG).show();

                        // Suggest using test numbers
                        if (error.contains("QUOTA_EXCEEDED")) {
                            new AlertDialog.Builder(LoginActivity.this)
                                    .setTitle("SMS Quota Exceeded")
                                    .setMessage("Use test numbers in Firebase Console:\n+37477123456 (code: 123456)")
                                    .setPositiveButton("OK", null)
                                    .show();
                        }
                    }

                    @Override
                    public void onCodeSent(@NonNull String verificationId,
                                           @NonNull PhoneAuthProvider.ForceResendingToken token) {
                        showLoading(false);
                        // Save verification ID
                        LoginActivity.this.verificationId = verificationId;

                        // Show OTP dialog
                        showOTPDialog();

                        Toast.makeText(LoginActivity.this,
                                "Code sent!", Toast.LENGTH_SHORT).show();
                    }
                })
                .build();

        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    private void showOTPDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter Code");
        builder.setMessage("Enter 6-digit code sent to your phone");

        final EditText input = new EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        input.setHint("123456");
        builder.setView(input);

        builder.setPositiveButton("Verify", (dialog, which) -> {
            String code = input.getText().toString().trim();
            if (code.length() == 6) {
                PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, code);
                signInWithPhoneCredential(credential);
            } else {
                Toast.makeText(this, "Enter 6-digit code", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void signInWithPhoneCredential(PhoneAuthCredential credential) {
        showLoading(true);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    showLoading(false);
                    if (task.isSuccessful()) {
                        Toast.makeText(LoginActivity.this,
                                "Phone login successful!", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(LoginActivity.this, MainActivity.class));
                        finish();
                    } else {
                        Toast.makeText(LoginActivity.this,
                                "Error: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void resetPassword() {
        String email = etEmail.getText().toString().trim();
        if (TextUtils.isEmpty(email)) {
            tilEmail.setError("Enter your email");
            return;
        }

        showLoading(true);
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    showLoading(false);
                    if (task.isSuccessful()) {
                        Toast.makeText(LoginActivity.this,
                                "Password reset email sent to " + email, Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(LoginActivity.this,
                                "Error: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
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
        btnLogin.setEnabled(!show);
        btnPhone.setEnabled(!show);
    }
}