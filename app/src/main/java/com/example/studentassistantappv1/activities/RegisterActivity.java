package com.example.studentassistantappv1.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.studentassistantappv1.R;
import com.example.studentassistantappv1.data.SupabaseApi;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.HashMap;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RegisterActivity extends AppCompatActivity {

    private EditText etName, etEmail, etPassword, etConfirmPassword;
    private Button btnRegister;
    private TextView tvBackToLogin;
    private SupabaseApi supabaseApi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        initViews();
        initRetrofit();

        btnRegister.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String pass = etPassword.getText().toString().trim();
            String confirmPass = etConfirmPassword.getText().toString().trim();

            if (name.isEmpty() || email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            } else if (!pass.equals(confirmPass)) {
                Toast.makeText(this, "Passwords do not match!", Toast.LENGTH_SHORT).show();
            } else if (pass.length() < 6) {
                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            } else {
                performRegistration(name, email, pass);
            }
        });

        tvBackToLogin.setOnClickListener(v -> finish());
    }

    private void initViews() {
        etName = findViewById(R.id.etRegName);
        etEmail = findViewById(R.id.etRegEmail);
        etPassword = findViewById(R.id.etRegPassword);
        etConfirmPassword = findViewById(R.id.etRegConfirmPassword);
        btnRegister = findViewById(R.id.btnRegister);
        tvBackToLogin = findViewById(R.id.tvBackToLogin);
    }

    private void initRetrofit() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://kjanqxiaynuewhqbpdiw.supabase.co/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        supabaseApi = retrofit.create(SupabaseApi.class);
    }

    private void performRegistration(String name, String email, String password) {
        Map<String, Object> body = new HashMap<>();
        body.put("email", email);
        body.put("password", password);

        // Supabase user metadata তে নাম পাঠানো হচ্ছে
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("full_name", name);
        body.put("data", metadata);

        supabaseApi.signUp(SupabaseApi.apiKey, "application/json", body)
                .enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            try {
                                String responseData = response.body().string();
                                JsonObject json = JsonParser.parseString(responseData).getAsJsonObject();

                                // সফল হলে ইউজার আইডি (UUID) পাওয়া যাবে
                                String userId = json.get("id").getAsString();

                                // ✅ নাম, ইমেইল এবং আইডি সেভ করা যাতে ড্যাশবোর্ড ও প্রোফাইলে দেখা যায়
                                saveUserData(name, email, userId);

                                Toast.makeText(RegisterActivity.this, "Account Created! Please Login.", Toast.LENGTH_LONG).show();

                                // সরাসরি লগইন স্ক্রিনে পাঠিয়ে দেওয়া
                                Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                                startActivity(intent);
                                finish();

                            } catch (Exception e) {
                                Log.e("RegisterError", "Parsing error: " + e.getMessage());
                            }
                        } else {
                            Toast.makeText(RegisterActivity.this, "Registration Failed! Email might already exist.", Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                        Toast.makeText(RegisterActivity.this, "Network error!", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveUserData(String name, String email, String userId) {
        SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        // কি-নামগুলো ড্যাশবোর্ড ও প্রোফাইলের সাথে মিল রাখা হয়েছে
        editor.putString("userName", name);
        editor.putString("userEmail", email);
        editor.putString("user_id", userId);
        editor.putBoolean("is_logged_in", false); // রেজিস্ট্রেশন এর পর লগইন করতে হবে তাই false

        editor.apply();
    }
}