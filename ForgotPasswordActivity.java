package com.example.studentassistantappv1.activities;


import android.os.Bundle;
import android.text.TextUtils;
import android.view.View; // View ইমপোর্ট করা প্রয়োজন
import android.widget.ImageView; // ImageView ইমপোর্ট করা প্রয়োজন
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.studentassistantappv1.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class ForgotPasswordActivity extends AppCompatActivity {

    private TextInputEditText etEmail;
    private MaterialButton btnReset;
    private TextView tvBackToLogin;
    private ImageView btnBack; // ব্যাক আইকনের জন্য

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        // UI Initialization
        etEmail = findViewById(R.id.etEmail);
        btnReset = findViewById(R.id.btnReset);
        tvBackToLogin = findViewById(R.id.tvBackToLogin);
        btnBack = findViewById(R.id.btnBack);

        // Reset Button Click
        btnReset.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();

            if (TextUtils.isEmpty(email)) {
                etEmail.setError("Please enter your registered email");
                etEmail.requestFocus();
            } else {
                performPasswordReset(email);
            }
        });

        // Back to Login Click (টেক্সট এ ক্লিক করলে)
        tvBackToLogin.setOnClickListener(v -> finish());

        // Back Icon Click (তীরের আইকনে ক্লিক করলে)
        btnBack.setOnClickListener(v -> finish());
    }

    private void performPasswordReset(String email) {
        Toast.makeText(this, "Reset link sent to " + email, Toast.LENGTH_LONG).show();
        finish();
    }
}