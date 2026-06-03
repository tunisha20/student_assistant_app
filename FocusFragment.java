package com.example.studentassistantappv1.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.Fragment;

import com.example.studentassistantappv1.R;
import com.example.studentassistantappv1.data.SupabaseApi;

import java.util.HashMap;
import java.util.Locale;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class FocusFragment extends Fragment {

    private TextView tvTimer, tvCoins, tvStreak;
    private ProgressBar progressBar;
    private AppCompatButton btnStart, btn25, btn45, btn60;
    private EditText etCustomTime;
    private ImageView btnBack;

    private CountDownTimer countDownTimer;
    private long totalTimeInMillis = 1500000; // ডিফল্ট ২৫ মিনিট
    private long timeLeftInMillis = 1500000;
    private boolean timerRunning = false;

    private SharedPreferences prefs;
    private SupabaseApi supabaseApi;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_focus, container, false);

        initViews(view);
        prefs = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        initRetrofit();
        loadLocalStats();

        // ১. ব্যাক বাটন লজিক
        btnBack.setOnClickListener(v -> {
            if (getActivity() != null) getActivity().onBackPressed();
        });

        // ২. ডিউরেশন বাটন লজিক
        btn25.setOnClickListener(v -> setTimer(25));
        btn45.setOnClickListener(v -> setTimer(45));
        btn60.setOnClickListener(v -> setTimer(60));

        // ৩. স্টার্ট বাটন লজিক (কাস্টম টাইমসহ)
        btnStart.setOnClickListener(v -> {
            if (timerRunning) {
                stopTimer();
            } else {
                handleStartLogic();
            }
        });

        return view;
    }

    private void initViews(View v) {
        btnBack = v.findViewById(R.id.btnBack);
        tvTimer = v.findViewById(R.id.tv_timer);
        tvCoins = v.findViewById(R.id.tvCoins);
        tvStreak = v.findViewById(R.id.tvStreak);
        progressBar = v.findViewById(R.id.focus_progress);
        btnStart = v.findViewById(R.id.btn_start);
        btn25 = v.findViewById(R.id.btn25m);
        btn45 = v.findViewById(R.id.btn45m);
        btn60 = v.findViewById(R.id.btn60m);
        etCustomTime = v.findViewById(R.id.et_custom_time);
    }

    private void handleStartLogic() {
        String customTimeInput = etCustomTime.getText().toString().trim();

        if (!customTimeInput.isEmpty()) {
            try {
                int totalMins;
                if (customTimeInput.contains(":")) {
                    String[] parts = customTimeInput.split(":");
                    int hours = Integer.parseInt(parts[0]);
                    int mins = Integer.parseInt(parts[1]);
                    totalMins = (hours * 60) + mins;
                } else {
                    totalMins = Integer.parseInt(customTimeInput);
                }

                if (totalMins > 0) {
                    setTimer(totalMins);
                } else {
                    Toast.makeText(getContext(), "Enter a valid time", Toast.LENGTH_SHORT).show();
                    return;
                }
            } catch (Exception e) {
                Toast.makeText(getContext(), "Use MM or HH:MM format", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        startTimer();
    }

    private void setTimer(int minutes) {
        if (timerRunning) stopTimer();
        totalTimeInMillis = minutes * 60 * 1000L;
        timeLeftInMillis = totalTimeInMillis;
        updateCountDownText();
        progressBar.setProgress(100);
    }

    private void startTimer() {
        countDownTimer = new CountDownTimer(timeLeftInMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeLeftInMillis = millisUntilFinished;
                updateCountDownText();
                int progress = (int) (timeLeftInMillis * 100 / totalTimeInMillis);
                progressBar.setProgress(progress);
            }

            @Override
            public void onFinish() {
                timerRunning = false;
                btnStart.setText("Start Session");
                progressBar.setProgress(0);
                handleSessionComplete();
            }
        }.start();

        timerRunning = true;
        btnStart.setText("Stop Session");
        etCustomTime.clearFocus(); // কী-বোর্ড হাইড করার জন্য
    }

    private void stopTimer() {
        if (countDownTimer != null) countDownTimer.cancel();
        timerRunning = false;
        btnStart.setText("Start Session");
    }

    private void updateCountDownText() {
        int minutes = (int) (timeLeftInMillis / 1000) / 60;
        int seconds = (int) (timeLeftInMillis / 1000) % 60;
        tvTimer.setText(String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds));
    }

    private void handleSessionComplete() {
        int sessionMinutes = (int) (totalTimeInMillis / 60000);
        int currentFocusMinutes = prefs.getInt("total_focus_minutes", 0);
        int totalUpdatedMinutes = currentFocusMinutes + sessionMinutes;

        // ১. লোকাল SharedPreferences-এ মিনিট আপডেট
        prefs.edit().putInt("total_focus_minutes", totalUpdatedMinutes).apply();

        int earnedCoins = sessionMinutes / 2;
        if (earnedCoins < 5) earnedCoins = 5;

        int currentCoins = Integer.parseInt(prefs.getString("user_coins", "0"));
        int newTotalCoins = currentCoins + earnedCoins;

        // লোকাল SharedPreferences-এ Coins আপডেট
        prefs.edit().putString("user_coins", String.valueOf(newTotalCoins)).apply();
        tvCoins.setText(newTotalCoins + " Coins");

        // লোকাল SharedPreferences থেকে বর্তমান Streak (Days) পড়া
        String currentStreak = prefs.getString("user_streak", "0");

        // ২. ফোকাস সেশন লগ ট্র্যাকিং টেবিলে সিঙ্ক (পৃথক রেকর্ড রাখার জন্য)
        syncWithSupabase(sessionMinutes, earnedCoins);

        // ৩. ড্যাশবোর্ড এবং প্রোফাইলের সব স্ট্যাটস (Focus Hour, Coins, Streak) একসাথে Supabase-এ পাঠানো
        updateProfileStatsToSupabase(totalUpdatedMinutes, String.valueOf(newTotalCoins), currentStreak);
    }

    private void loadLocalStats() {
        tvCoins.setText(prefs.getString("user_coins", "0") + " Coins");
        tvStreak.setText(prefs.getString("user_streak", "0") + " Days");
    }

    private void initRetrofit() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://kjanqxiaynuewhqbpdiw.supabase.co/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        supabaseApi = retrofit.create(SupabaseApi.class);
    }

    private void syncWithSupabase(int duration, int coins) {
        String userId = prefs.getString("user_id", "");
        String token = "Bearer " + prefs.getString("auth_token", "");

        HashMap<String, Object> sessionMap = new HashMap<>();
        sessionMap.put("user_id", userId);
        sessionMap.put("duration_minutes", duration);
        sessionMap.put("coins_earned", coins);

        supabaseApi.saveFocusSession(SupabaseApi.apiKey, token, sessionMap)
                .enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                        if (response.isSuccessful() && isAdded()) {
                            Toast.makeText(getContext(), "Progress Synced! 🎉", Toast.LENGTH_SHORT).show();
                        }
                    }
                    @Override public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {}
                });
    }

    // ফোকাস আওয়ার, কয়েন এবং স্ট্রিক (Days) একসাথে সুপাবেস প্রোফাইলে আপডেট করার ফ্রেশ মেথড
    private void updateProfileStatsToSupabase(int totalMinutes, String totalCoins, String currentStreak) {
        String userId = prefs.getString("user_id", "");
        String rawToken = prefs.getString("auth_token", "");

        if (userId.isEmpty()) return;

        // মিনিটকে ঘন্টায় কনভার্ট করা (ড্যাশবোর্ড ফরম্যাট "1.3h")
        double hours = totalMinutes / 60.0;
        String focusTimeStr = String.format(Locale.US, "%.1f", hours);

        // প্রোফাইল আপডেটের জন্য ম্যাপ তৈরি
        HashMap<String, Object> profileUpdateMap = new HashMap<>();
        profileUpdateMap.put("focusTime", focusTimeStr);   // মোট ফোকাস টাইম ঘন্টা আকারে
        profileUpdateMap.put("coins", totalCoins);         // লেটেস্ট কয়েন কাউন্ট
        profileUpdateMap.put("streak", currentStreak);     // বর্তমান স্ট্রিক বা দিন সংখ্যা

        // আপনার SupabaseApi ইন্টারফেসের ৫টি প্যারামিটার সিরিয়াল অনুযায়ী কল:
        supabaseApi.updateProfile(
                SupabaseApi.apiKey,                      // ১. apiKey Header
                "Bearer " + rawToken,                    // ২. Authorization Header
                "application/json",                      // ৩. Content-Type Header
                "eq." + userId,                          // ৪. id Query Filter
                profileUpdateMap                         // ৫. Body Map
        ).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    Log.d("FocusSync", "Successfully synced all stats (Focus, Coins, Streak) to profile!");
                } else {
                    Log.e("FocusSync", "Profile Stats Update failed. Code: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                Log.e("FocusSync", "Failed to sync profile stats: " + t.getMessage());
            }
        });
    }
}