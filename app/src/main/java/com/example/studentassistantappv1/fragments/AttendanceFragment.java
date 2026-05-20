package com.example.studentassistantappv1.fragments;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.AnticipateOvershootInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.studentassistantappv1.R;
import com.example.studentassistantappv1.adapters.AttendanceAdapter;
import com.example.studentassistantappv1.data.SupabaseApi;
import com.example.studentassistantappv1.models.Attendance;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class AttendanceFragment extends Fragment {

    private CircularProgressIndicator progressAttendance;
    private TextView tvPercentage, tvStatusMessage, tvTotalClasses, tvClassesMissed, tvBunkAdvice;
    private MaterialCardView cardSummary, cardStatusPill;
    private RecyclerView recyclerView;
    private ExtendedFloatingActionButton fabAdd;
    private LinearLayout layoutEmptyState;

    private List<Attendance> attendanceList;
    private AttendanceAdapter adapter;
    private SupabaseApi supabaseApi;
    private String userId, authToken;

    private static final String PREF_NAME = "UserPrefs";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_attendance, container, false);

        initViews(view);
        initRetrofit();
        setupRecyclerView();

        fabAdd.setOnClickListener(v -> showAddSubjectDialog());
        fetchAttendanceFromSupabase();

        return view;
    }

    private void initViews(View view) {
        progressAttendance = view.findViewById(R.id.progress_attendance);
        tvPercentage = view.findViewById(R.id.tv_percentage);
        tvStatusMessage = view.findViewById(R.id.tv_status_message);
        cardSummary = view.findViewById(R.id.card_summary);
        cardStatusPill = view.findViewById(R.id.card_status_pill);
        tvTotalClasses = view.findViewById(R.id.tv_total_classes);
        tvClassesMissed = view.findViewById(R.id.tv_classes_missed);
        recyclerView = view.findViewById(R.id.recycler_view_attendance);
        fabAdd = view.findViewById(R.id.fab_add_attendance);
        layoutEmptyState = view.findViewById(R.id.layout_empty_state);
        tvBunkAdvice = view.findViewById(R.id.tvBunkAdvice);
    }

    private void initRetrofit() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://kjanqxiaynuewhqbpdiw.supabase.co/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        supabaseApi = retrofit.create(SupabaseApi.class);

        SharedPreferences prefs = requireActivity().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        userId = prefs.getString("user_id", "");
        authToken = "Bearer " + prefs.getString("auth_token", "");
    }

    private void setupRecyclerView() {
        attendanceList = new ArrayList<>();
        adapter = new AttendanceAdapter(
                attendanceList,
                requireContext(),
                supabaseApi,
                authToken,
                (shouldAnimate) -> calculateAndRefreshOverview(shouldAnimate)
        );

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setNestedScrollingEnabled(false);
        recyclerView.setAdapter(adapter);
    }

    private void fetchAttendanceFromSupabase() {
        if (userId.isEmpty()) return;

        supabaseApi.getAttendanceData(SupabaseApi.apiKey, authToken, "*", "eq." + userId)
                .enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                        if (response.isSuccessful() && response.body() != null && isAdded()) {
                            try {
                                String json = response.body().string();
                                List<Attendance> list = new Gson().fromJson(json, new TypeToken<List<Attendance>>(){}.getType());
                                attendanceList.clear();
                                attendanceList.addAll(list);
                                adapter.notifyDataSetChanged();
                                calculateAndRefreshOverview(false);
                            } catch (Exception e) { e.printStackTrace(); }
                        }
                    }
                    @Override public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                        Log.e("Attendance", "Fetch error: " + t.getMessage());
                    }
                });
    }

    private void saveSubjectToDatabase(String subjectName) {
        Map<String, Object> data = new HashMap<>();
        data.put("user_id", userId);
        data.put("subject_name", subjectName);
        data.put("present_classes", 0);
        data.put("total_classes", 0);

        // হেডার হিসেবে "return=representation" পাঠানো হয়েছে
        supabaseApi.insertAttendance(SupabaseApi.apiKey, authToken,
                        "application/json", "return=representation", data)
                .enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                        if (response.isSuccessful()) {
                            // ✅ নতুন সাবজেক্ট সেভ হওয়ার পর লিস্ট রিফ্রেশ করা
                            fetchAttendanceFromSupabase();
                            Toast.makeText(getContext(), "Subject Added!", Toast.LENGTH_SHORT).show();
                        } else {
                            Log.e("Attendance", "Error Code: " + response.code());
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                        Toast.makeText(getContext(), "Network Error!", Toast.LENGTH_SHORT).show();
                    }
                });
    }
    private void calculateAndRefreshOverview(boolean shouldAnimate) {
        int totalClassesAll = 0, totalPresentAll = 0;

        for (Attendance a : attendanceList) {
            totalClassesAll += a.getTotalClasses();
            totalPresentAll += a.getPresentClasses();
        }

        int overallPercentage = (totalClassesAll > 0) ? (totalPresentAll * 100) / totalClassesAll : 0;

        SharedPreferences mainPrefs = requireActivity().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        mainPrefs.edit().putString("user_attendance", String.valueOf(overallPercentage)).apply();

        updateStatsUI(overallPercentage, totalClassesAll, totalClassesAll - totalPresentAll);
        calculateBunkAdvice(totalPresentAll, totalClassesAll); // ✅ আপনার লজিকটি কল করা হয়েছে

        if (shouldAnimate) triggerPremiumFeedbackEffects();
        checkEmptyState();
    }

    private void updateStatsUI(int percent, int total, int missed) {
        progressAttendance.setProgress(percent, true);
        tvPercentage.setText(percent + "%");
        tvTotalClasses.setText(String.valueOf(total));
        tvClassesMissed.setText(String.valueOf(missed));

        if (percent >= 75) {
            int color = Color.parseColor("#00C2FF");
            progressAttendance.setIndicatorColor(color);
            tvStatusMessage.setText("Excellent Consistency! 🛡️");
            tvStatusMessage.setTextColor(color);
            cardStatusPill.setCardBackgroundColor(Color.parseColor("#F0F9FF"));
        } else if (percent > 0) {
            int color = Color.parseColor("#EF4444");
            progressAttendance.setIndicatorColor(color);
            tvStatusMessage.setText("Danger Zone! ⚠️");
            tvStatusMessage.setTextColor(color);
            cardStatusPill.setCardBackgroundColor(Color.parseColor("#FFF1F2"));
        } else {
            tvStatusMessage.setText("No Data Available");
            tvStatusMessage.setTextColor(Color.GRAY);
            cardStatusPill.setCardBackgroundColor(Color.parseColor("#F8FAFC"));
        }
    }

    private void calculateBunkAdvice(int present, int total) {
        if (tvBunkAdvice == null || total == 0) return;
        double target = 0.75;
        double current = (double) present / total;

        if (current >= target) {
            int safeToBunk = 0;
            int tempTotal = total;
            while (((double) present / (tempTotal + 1)) >= target) {
                tempTotal++;
                safeToBunk++;
            }
            tvBunkAdvice.setText("Safe! You can miss next " + safeToBunk + " classes.");
            tvBunkAdvice.setTextColor(Color.parseColor("#10B981"));
        } else {
            int requiredClasses = 0;
            int tempPresent = present;
            int tempTotal = total;
            while (((double) tempPresent / tempTotal) < target) {
                tempPresent++;
                tempTotal++;
                requiredClasses++;
            }
            tvBunkAdvice.setText("Critical! Attend next " + requiredClasses + " classes continuously.");
            tvBunkAdvice.setTextColor(Color.parseColor("#EF4444"));
        }
    }

    private void triggerPremiumFeedbackEffects() {
        if (cardSummary != null) {
            cardSummary.animate().translationY(-25f).setDuration(200).setInterpolator(new DecelerateInterpolator())
                    .withEndAction(() -> cardSummary.animate().translationY(0f).setDuration(300).setInterpolator(new AnticipateOvershootInterpolator()).start())
                    .start();
        }
    }

    private void checkEmptyState() {
        if (attendanceList.isEmpty()) {
            layoutEmptyState.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            layoutEmptyState.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void showAddSubjectDialog() {
        Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_update_attendance);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        TextInputEditText etName = dialog.findViewById(R.id.et_subject_name);
        MaterialButton btnSave = dialog.findViewById(R.id.btn_save_attendance);

        if (btnSave != null) {
            btnSave.setOnClickListener(v -> {
                String name = etName.getText().toString().trim();
                if (!name.isEmpty()) {
                    saveSubjectToDatabase(name);
                    dialog.dismiss();
                } else {
                    etName.setError("Subject name cannot be empty");
                }
            });
        }
        dialog.show();
    }
}