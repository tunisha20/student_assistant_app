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

import com.example.studentassistantappv1.activities.MainActivity;
import com.example.studentassistantappv1.R;
import com.example.studentassistantappv1.data.SupabaseApi;

import org.json.JSONObject;
import java.util.HashMap;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class LoginActivity extends AppCompatActivity {

    private Button btnLogin;
    private TextView tvSignUp;
    private EditText etEmail, etPassword;
    private SupabaseApi supabaseApi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        initViews();
        initRetrofit();

        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Email and Password are required", Toast.LENGTH_SHORT).show();
            } else {
                performLogin(email, password);
            }
        });

        tvSignUp.setOnClickListener(v -> startActivity(new Intent(this, RegisterActivity.class)));
    }

    private void initViews() {
        btnLogin = findViewById(R.id.btnLogin);
        tvSignUp = findViewById(R.id.btnSignUp);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
    }

    private void initRetrofit() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://kjanqxiaynuewhqbpdiw.supabase.co/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        supabaseApi = retrofit.create(SupabaseApi.class);
    }

    private void performLogin(String email, String password) {
        Map<String, Object> credentials = new HashMap<>();
        credentials.put("email", email);
        credentials.put("password", password);

        supabaseApi.login(SupabaseApi.apiKey, credentials).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String jsonResponse = response.body().string();
                        JSONObject jsonObject = new JSONObject(jsonResponse);

                        // ১. আইডি এবং টোকেন সংগ্রহ
                        String token = jsonObject.getString("access_token");
                        String userId = jsonObject.getJSONObject("user").getString("id");
                        String userEmail = jsonObject.getJSONObject("user").getString("email");

                        // ২. ডাটা সেভ করা (সেশন ম্যানেজমেন্ট)
                        // রেজিস্ট্রেশন থেকে আসা নামটি SharedPreferences-এ অলরেডি থাকলে সেটিই ড্যাশবোর্ডে যাবে
                        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                        String existingName = prefs.getString("userName", "Bravo42");

                        saveUserData(existingName, userEmail, userId, token);

                        Toast.makeText(LoginActivity.this, "Welcome Back! 🚀", Toast.LENGTH_SHORT).show();

                        // ৩. MainActivity-তে যাওয়া
                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        startActivity(intent);
                        finish();

                    } catch (Exception e) {
                        Log.e("LoginError", "Parsing fail: " + e.getMessage());
                    }
                } else {
                    Toast.makeText(LoginActivity.this, "Invalid Email or Password", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                Toast.makeText(LoginActivity.this, "Network Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveUserData(String name, String email, String userId, String token) {
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        // কি-নামগুলো SplashActivity এবং Dashboard এর সাথে মিলিয়ে রাখা হয়েছে
        editor.putBoolean("is_logged_in", true); // অটো লগইন এনাবল
        editor.putString("user_id", userId);
        editor.putString("auth_token", token);
        editor.putString("userName", name);
        editor.putString("userEmail", email);

        editor.apply();
    }
}