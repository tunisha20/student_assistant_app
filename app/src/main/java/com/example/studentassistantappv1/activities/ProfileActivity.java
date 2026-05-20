package com.example.studentassistantappv1.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.studentassistantappv1.activities.MainActivity;
import com.example.studentassistantappv1.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textfield.TextInputEditText;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class ProfileActivity extends AppCompatActivity {

    private TextView tvName, tvEmail;
    private ShapeableImageView profileImage;
    private SharedPreferences sharedPreferences;
    private static final String PREF_NAME = "UserPrefs";

    // ছবি গ্যালারি থেকে সিলেক্ট করার লঞ্চার
    private final ActivityResultLauncher<String> pickImage = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    try {
                        InputStream inputStream = getContentResolver().openInputStream(uri);
                        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                        profileImage.setImageBitmap(bitmap);
                        saveImageToPrefs(bitmap); // লোকালি সেভ করা
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
        MaterialButton editBtn = findViewById(R.id.edit_profile_button);
      //  MaterialButton backBtn = findViewById(R.id.btn_back_profile); // যদি ব্যাক বাটন থাকে

        sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);

        // ১. সেভ করা ডাটা লোড করা (Register/Login থেকে আসা ডাটা)
        loadSavedData();

        // ২. এডিট ডায়ালগ ওপেন করা (আপনার স্ক্রিনশটের লাল দাগ ফিক্স করা হয়েছে এখানে)
        if (editBtn != null) {
            editBtn.setOnClickListener(v -> showEditProfileDialog());
        }

        // সরাসরি ছবিতে ক্লিক করলেও প্রোফাইল পিক চেঞ্জ হবে
        if (profileImage != null) {
            profileImage.setOnClickListener(v -> pickImage.launch("image/*"));
        }

       // if (backBtn != null) {
            //backBtn.setOnClickListener(v -> finish());
      //  }
    }

    // 🔥 এটিই আপনার স্ক্রিনশটে লাল দেখাচ্ছিল। এখন এটি ফিক্সড।
    private void showEditProfileDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_profile, null);
        TextInputEditText nameInput = dialogView.findViewById(R.id.edit_name_input);
        TextInputEditText emailInput = dialogView.findViewById(R.id.edit_email_input);
        MaterialButton btnChangePhoto = dialogView.findViewById(R.id.btn_change_photo);

        // ডায়ালগে বর্তমান ডাটা দেখানো
        if (nameInput != null) nameInput.setText(tvName.getText().toString());
        if (emailInput != null) emailInput.setText(tvEmail.getText().toString());

        if (btnChangePhoto != null) {
            btnChangePhoto.setOnClickListener(v -> pickImage.launch("image/*"));
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle("Update Profile")
                .setView(dialogView)
                .setCancelable(true)
                .setPositiveButton("Save", (dialog, which) -> {
                    String newName = nameInput.getText().toString().trim();
                    String newEmail = emailInput.getText().toString().trim();

                    if (!newName.isEmpty() && !newEmail.isEmpty()) {
                        saveProfileData(newName, newEmail);
                    } else {
                        Toast.makeText(this, "Fields cannot be empty!", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void saveProfileData(String name, String email) {
        sharedPreferences.edit()
                .putString("userName", name)
                .putString("userEmail", email)
                .apply();

        tvName.setText(name);
        tvEmail.setText(email);
        Toast.makeText(this, "Profile Updated!", Toast.LENGTH_SHORT).show();
    }

    private void saveImageToPrefs(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, baos);
        byte[] b = baos.toByteArray();
        String encodedImage = Base64.encodeToString(b, Base64.DEFAULT);
        sharedPreferences.edit().putString("profile_image", encodedImage).apply();
    }

    private void loadSavedData() {
        // লগইন বা রেজিস্ট্রেশন থেকে আসা কী (userName, userEmail) ব্যবহার করা হয়েছে
        String name = sharedPreferences.getString("userName", "Bravo42");
        String email = sharedPreferences.getString("userEmail", "student@baust.edu");

        tvName.setText(name);
        tvEmail.setText(email);

        // প্রোফাইল পিকচার লোড
        String encodedImage = sharedPreferences.getString("profile_image", "");
        if (!encodedImage.isEmpty()) {
            byte[] b = Base64.decode(encodedImage, Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(b, 0, b.length);
            profileImage.setImageBitmap(bitmap);
        }
    }
}