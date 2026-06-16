package com.example.studentassistantappv1.activities;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.studentassistantappv1.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textfield.TextInputEditText;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class ProfileActivity extends AppCompatActivity {

    private TextView tvName, tvEmail;
    private ShapeableImageView profileImage;
    private FloatingActionButton btnChangePhotoFab;
    private SharedPreferences sharedPreferences;
    private static final String PREF_NAME = "UserPrefs";

    // গ্যালারি থেকে ইমেজ সিলেক্ট করার লঞ্চার
    private final ActivityResultLauncher<String> pickImage = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    try {
                        InputStream inputStream = getContentResolver().openInputStream(uri);
                        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                        if (bitmap != null) {
                            profileImage.setImageBitmap(bitmap);
                            saveImageToPrefs(bitmap); // অপ্টিমাইজড উপায়ে লোকালে সংরক্ষণ
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // ভিউ ইনিশিয়ালাইজেশন
        tvName = findViewById(R.id.tvProfileName);
        tvEmail = findViewById(R.id.tvProfileEmail);
        profileImage = findViewById(R.id.profile_img);
        btnChangePhotoFab = findViewById(R.id.btn_change_photo_fab);
        MaterialButton editBtn = findViewById(R.id.edit_profile_button);

        sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);

        // লোকাল ডাটা লোড করা
        loadSavedData();

        // এডিট প্রোফাইল ডায়ালগ ওপেন করা
        if (editBtn != null) {
            editBtn.setOnClickListener(v -> showEditProfileDialog());
        }

        // গোল প্রোফাইল ছবিতে ক্লিক করলে ইমেজ চেঞ্জ হবে
        if (profileImage != null) {
            profileImage.setOnClickListener(v -> pickImage.launch("image/*"));
        }

        // ছোট ক্যামেরা বাটনে ক্লিক করলেও ইমেজ充ঞ্জ হবে
        if (btnChangePhotoFab != null) {
            btnChangePhotoFab.setOnClickListener(v -> pickImage.launch("image/*"));
        }
    }

    private void showEditProfileDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_profile, null);
        if (dialogView == null) return;

        TextInputEditText nameInput = dialogView.findViewById(R.id.edit_name_input);
        TextInputEditText emailInput = dialogView.findViewById(R.id.edit_email_input);
        MaterialButton btnDialogChangePhoto = dialogView.findViewById(R.id.btn_change_photo);

        // ডায়ালগের ইনপুট বক্সে বর্তমান নাম ও ইমেইল সেট করা
        if (nameInput != null && tvName != null) nameInput.setText(tvName.getText().toString());
        if (emailInput != null && tvEmail != null) emailInput.setText(tvEmail.getText().toString());

        // ডায়ালগের ভেতরের চেঞ্জ ফটো বাটন অ্যাকশন
        if (btnDialogChangePhoto != null) {
            btnDialogChangePhoto.setOnClickListener(v -> pickImage.launch("image/*"));
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle("Update Profile")
                .setView(dialogView)
                .setCancelable(true)
                .setPositiveButton("Save", (dialog, which) -> {
                    if (nameInput != null && emailInput != null) {
                        String newName = nameInput.getText().toString().trim();
                        String newEmail = emailInput.getText().toString().trim();

                        if (!newName.isEmpty() && !newEmail.isEmpty()) {
                            saveProfileData(newName, newEmail);
                        } else {
                            Toast.makeText(this, "Fields cannot be empty!", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void saveProfileData(String name, String email) {
        // ১. লোকালে আপডেট
        sharedPreferences.edit()
                .putString("userName", name)
                .putString("userEmail", email)
                .apply();

        if (tvName != null) tvName.setText(name);
        if (tvEmail != null) tvEmail.setText(email);

        // ২. ক্লাউডে (Supabase) আপডেট করার জন্য মেথড কল
        String userId = sharedPreferences.getString("user_id", "");
        if (!userId.isEmpty()) {
            updateProfileInSupabase(userId, name, email);
        } else {
            Toast.makeText(this, "Profile Updated Locally!", Toast.LENGTH_SHORT).show();
        }
    }

    // ✅ আপনার দেওয়া নতুন এবং কার্যকরী সঠিক Supabase Auth মেথডটি এখানে যুক্ত করা হলো
    private void updateProfileInSupabase(String userId, String name, String email) {
        String supabaseUrl = "https://kjanqxiaynuewhqbpdiw.supabase.co/auth/v1/user";
        String supabaseApiKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImtqYW5xeGlheW51ZXdocWJwZGl3Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzgyMDE5NTEsImV4cCI6MjA5Mzc3Nzk1MX0.tuI3MaR4nLxc898NwWo_QbZpjfTxpu8sD42b_c7oSCA";

        String jsonBody = "{"
                + "\"email\":\"" + email + "\","
                + "\"user_metadata\":{"
                + "\"full_name\":\"" + name + "\""
                + "}"
                + "}";

        okhttp3.RequestBody body = okhttp3.RequestBody.create(jsonBody, okhttp3.MediaType.parse("application/json"));
        okhttp3.Request request = new okhttp3.Request.Builder()
                .url(supabaseUrl)
                .addHeader("apikey", supabaseApiKey)
                .addHeader("Authorization", "Bearer " + supabaseApiKey)
                .put(body)
                .build();

        new okhttp3.OkHttpClient().newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(okhttp3.Call call, java.io.IOException e) {
                Log.e("SupabaseError", "Network failed: " + e.getMessage());
            }

            @Override
            public void onResponse(okhttp3.Call call, okhttp3.Response response) throws java.io.IOException {
                if (response.isSuccessful()) {
                    runOnUiThread(() -> Toast.makeText(ProfileActivity.this, "Cloud Sync Done! ✅", Toast.LENGTH_SHORT).show());
                } else {
                    Log.e("SupabaseError", "Failed with code: " + response.code() + " body: " + response.body().string());
                }
            }
        });
    }

    // 🛠️ মেমোরি লিক এবং ক্র্যাশ এড়াতে ইমেজ রিসাইজ ও অপ্টিমাইজেশন যুক্ত করা হয়েছে
    private void saveImageToPrefs(Bitmap bitmap) {
        // ইমেজকে ৫০০x৫০০ সাইজে স্কেল ডাউন করা হলো
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, 500, 500, true);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos); // ৭০% কোয়ালিটিতে কম্প্রেস
        byte[] b = baos.toByteArray();
        String encodedImage = Base64.encodeToString(b, Base64.DEFAULT);
        sharedPreferences.edit().putString("profile_image", encodedImage).apply();
    }

    private void loadSavedData() {
        String name = sharedPreferences.getString("userName", "Bravo42");
        String email = sharedPreferences.getString("userEmail", "student@baust.edu");

        if (tvName != null) tvName.setText(name);
        if (tvEmail != null) tvEmail.setText(email);

        String encodedImage = sharedPreferences.getString("profile_image", "");
        if (!encodedImage.isEmpty() && profileImage != null) {
            try {
                byte[] b = Base64.decode(encodedImage, Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(b, 0, b.length);
                if (bitmap != null) {
                    profileImage.setImageBitmap(bitmap);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}