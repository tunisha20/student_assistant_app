package com.example.studentassistantappv1.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.studentassistantappv1.activities.MainActivity;
import com.example.studentassistantappv1.R;
import com.google.android.material.progressindicator.LinearProgressIndicator;

public class SplashActivity extends AppCompatActivity {

    private LinearProgressIndicator progressBar;
    private TextView tvPercent;
    private int progressStatus = 0;
    private final Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        progressBar = findViewById(R.id.splashProgress);
        tvPercent = findViewById(R.id.tvProgressPercent);

        new Thread(() -> {
            while (progressStatus < 100) {
                progressStatus += 1;
                handler.post(() -> {
                    if (progressBar != null) progressBar.setProgress(progressStatus);
                    if (tvPercent != null) tvPercent.setText(progressStatus + "%");
                });
                try { Thread.sleep(30); } catch (InterruptedException e) { e.printStackTrace(); }
            }
            runOnUiThread(this::navigateToNext);
        }).start();
    }

    private void navigateToNext() {
        // ১. সেশন চেক করা
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        boolean isLoggedIn = prefs.getBoolean("is_logged_in", false); // Key matching ensured

        Intent intent;
        if (isLoggedIn) {
            intent = new Intent(SplashActivity.this, MainActivity.class);
        } else {
            intent = new Intent(SplashActivity.this, LoginActivity.class);
        }
        startActivity(intent);
        finish();
    }
}