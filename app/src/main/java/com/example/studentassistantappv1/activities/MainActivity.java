package com.example.studentassistantappv1.activities;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.example.studentassistantappv1.R;
import com.example.studentassistantappv1.fragments.DashboardFragment;
import com.example.studentassistantappv1.fragments.NotesFragment;
import com.example.studentassistantappv1.fragments.RoutineFragment;
import com.example.studentassistantappv1.fragments.SyllabusFragment;
import com.example.studentassistantappv1.fragments.TaskFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.navigation.NavigationView;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private BottomNavigationView bottomNav;
    private static final int NOTIFICATION_PERMISSION_CODE = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // ১. টুলবার সেটআপ
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // ২. ভিউ ইনিশিয়ালাইজেশন
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        bottomNav = findViewById(R.id.bottom_navigation);

        // ৩. ড্রয়ার টগল সেটআপ
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // ৪. লিসেনার ও চ্যানেল সেটআপ
        navigationView.setNavigationItemSelectedListener(this);
        createNotificationChannel();
        requestNotificationPermission(); // পারমিশন রিকোয়েস্ট

        // ৫. ডাটা ও ফ্র্যাগমেন্ট লোড
        loadUserDataToDrawer();
        if (savedInstanceState == null) {
            loadFragment(new DashboardFragment(), "DASHBOARD");
        }
        setupBottomNavigation();
    }

    // --- পারমিশন হ্যান্ডলিং ---
    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_CODE);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == NOTIFICATION_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Notification Permission Granted! ✅", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Permission Denied! You will miss reminders.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // --- নোটিফিকেশন চ্যানেল ---
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("class_reminder", "Student Assistant Reminders", NotificationManager.IMPORTANCE_HIGH);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    // --- ফ্র্যাগমেন্ট ও ড্রয়ার লজিক ---
    private void loadUserDataToDrawer() {
        SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String name = sharedPreferences.getString("userName", "Bravo42");
        String email = sharedPreferences.getString("userEmail", "student@baust.edu");

        View headerView = navigationView.getHeaderView(0);
        TextView tvHeaderName = headerView.findViewById(R.id.nav_header_name);
        TextView tvHeaderEmail = headerView.findViewById(R.id.nav_header_email);

        if (tvHeaderName != null) tvHeaderName.setText(name);
        if (tvHeaderEmail != null) tvHeaderEmail.setText(email);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.nav_profile) {
            // startActivity(new Intent(MainActivity.this, ProfileActivity.class));
        } else if (id == R.id.nav_about) {
            showAboutDialog();
        } else if (id == R.id.nav_logout) {
            performLogout();
        }
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void showAboutDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("About StudyBuddy")
                .setMessage("Developed by Bravo42\nYour personal student companion.")
                .setPositiveButton("OK", null)
                .show();
    }

    private void performLogout() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Logout")
                .setMessage("Are you sure?")
                .setPositiveButton("Logout", (dialog, which) -> {
                    getSharedPreferences("UserPrefs", MODE_PRIVATE).edit().clear().apply();
                    finish();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void setupBottomNavigation() {
        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) selectedFragment = new DashboardFragment();
            else if (itemId == R.id.nav_routine) selectedFragment = new RoutineFragment();
            else if (itemId == R.id.nav_notes) selectedFragment = new NotesFragment();
            else if (itemId == R.id.nav_tasks) selectedFragment = new TaskFragment();
            else if (itemId == R.id.nav_syllabus) selectedFragment = new SyllabusFragment();

            if (selectedFragment != null) {
                loadFragment(selectedFragment, null);
                return true;
            }
            return false;
        });
    }

    public void loadFragment(Fragment fragment, String tag) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment, tag)
                .commit();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUserDataToDrawer();
    }
}