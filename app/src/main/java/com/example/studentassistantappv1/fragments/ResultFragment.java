package com.example.studentassistantappv1.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.studentassistantappv1.R;
import com.example.studentassistantappv1.adapters.CourseAdapter;
import com.example.studentassistantappv1.data.SupabaseApi;
import com.example.studentassistantappv1.models.CourseEntry;

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

public class ResultFragment extends Fragment {

    // UI Elements
    private RecyclerView recyclerCourses;
    private View btnAddCourse, btnCalculate;
    private TextView tvViewHistory, tvSemesterTitle, tvStandingLabel;
    private CardView cardResult;
    private TextView tvGpaResult, tvCgpaResult, tvCgpaBig;
    private ProgressBar progressCgpa;
    private EditText etPrevCgpa, etPrevCredits;

    // Data & Helpers
    private CourseAdapter courseAdapter;
    private List<CourseEntry> courseDataList = new ArrayList<>();
    private SupabaseApi supabaseApi;

    private final String[] gradeLabels = {"A+ (4.00)", "A (3.75)", "A- (3.50)", "B+ (3.25)", "B (3.00)", "B- (2.75)", "C+ (2.50)", "C (2.25)", "D (2.00)", "F (0.00)"};
    private final double[] gradePoints = {4.00, 3.75, 3.50, 3.25, 3.00, 2.75, 2.50, 2.25, 2.00, 0.00};

    // SharedPreferences Names
    private static final String PREF_NAME = "UserProfilePrefs"; // Result history storage
    private static final String USER_AUTH_PREF = "UserPrefs"; // Auth data storage

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_result, container, false);

        initViews(view);
        initRetrofit();
        setupSemesterTitle();

        // View History Click
        if (tvViewHistory != null) {
            tvViewHistory.setPaintFlags(tvViewHistory.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
            tvViewHistory.setOnClickListener(v -> openHistory());
        }

        // RecyclerView Setup
        courseAdapter = new CourseAdapter(courseDataList, gradeLabels);
        recyclerCourses.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerCourses.setAdapter(courseAdapter);

        // Initial Row
        if (courseDataList.isEmpty()) courseDataList.add(new CourseEntry());

        // Button Click Listeners
        btnAddCourse.setOnClickListener(v -> addCourseRow());
        btnCalculate.setOnClickListener(v -> calculateResult());

        return view;
    }

    private void initViews(View view) {
        recyclerCourses = view.findViewById(R.id.recycler_courses);
        btnAddCourse = view.findViewById(R.id.btn_add_course);
        btnCalculate = view.findViewById(R.id.btn_calculate);
        tvViewHistory = view.findViewById(R.id.btn_view_history);
        tvSemesterTitle = view.findViewById(R.id.tvSemesterTitle);
        tvStandingLabel = view.findViewById(R.id.tvStandingLabel);
        etPrevCgpa = view.findViewById(R.id.et_prev_cgpa);
        etPrevCredits = view.findViewById(R.id.et_prev_credits);
        cardResult = view.findViewById(R.id.card_result);
        tvGpaResult = view.findViewById(R.id.tv_gpa_result);
        tvCgpaResult = view.findViewById(R.id.tv_cgpa_result);
        tvCgpaBig = view.findViewById(R.id.tvCgpaBig);
        progressCgpa = view.findViewById(R.id.progressCgpa);
    }

    private void initRetrofit() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://kjanqxiaynuewhqbpdiw.supabase.co/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        supabaseApi = retrofit.create(SupabaseApi.class);
    }

    private void calculateResult() {
        double totalWeightedPoints = 0;
        double totalCredits = 0;

        for (CourseEntry course : courseDataList) {
            if (course.credits > 0) {
                totalWeightedPoints += (course.credits * gradePoints[course.gradeIndex]);
                totalCredits += course.credits;
            }
        }

        if (totalCredits == 0) {
            Toast.makeText(requireContext(), "Please add credits!", Toast.LENGTH_SHORT).show();
            return;
        }

        double semesterGpa = totalWeightedPoints / totalCredits;
        double finalCgpa = semesterGpa;

        String prevCgpaStr = etPrevCgpa.getText().toString().trim();
        String prevCreditStr = etPrevCredits.getText().toString().trim();

        if (!prevCgpaStr.isEmpty() && !prevCreditStr.isEmpty()) {
            try {
                double prevCgpa = Double.parseDouble(prevCgpaStr);
                double prevCredits = Double.parseDouble(prevCreditStr);
                finalCgpa = ((prevCgpa * prevCredits) + totalWeightedPoints) / (prevCredits + totalCredits);
            } catch (Exception ignored) {}
        }

        String formattedResult = String.format("%.2f", finalCgpa);
        updateUI(semesterGpa, finalCgpa, formattedResult);

        // ✅ Cloud Sync (Profiles & History)
        saveCgpaToSupabase(formattedResult, tvSemesterTitle.getText().toString(), semesterGpa, (int)totalCredits);
    }

    private void saveCgpaToSupabase(String formattedCgpa, String semesterName, double semesterGpa, int totalCredits) {
        SharedPreferences authPrefs = requireActivity().getSharedPreferences(USER_AUTH_PREF, Context.MODE_PRIVATE);
        String userId = authPrefs.getString("user_id", "");
        String authToken = "Bearer " + authPrefs.getString("auth_token", "");

        if (userId.isEmpty()) return;

        // 1. Update Profile (Overall CGPA)
        Map<String, Object> profileUpdates = new HashMap<>();
        profileUpdates.put("user_cgpa", formattedCgpa);

        supabaseApi.updateProfile(SupabaseApi.apiKey, authToken, "application/json", "eq." + userId, profileUpdates)
                .enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                        if (response.isSuccessful()) Log.d("Supabase", "Overall CGPA synced");
                    }
                    @Override public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {}
                });

        // 2. Insert into History (Graph Data)
        Map<String, Object> historyData = new HashMap<>();
        historyData.put("user_id", userId);
        historyData.put("semester_name", semesterName);
        historyData.put("gpa", semesterGpa);
        historyData.put("credits", totalCredits);

        supabaseApi.insertCgpaHistory(SupabaseApi.apiKey, authToken, "application/json", historyData)
                .enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                        if (response.isSuccessful()) {
                            Toast.makeText(getContext(), "Result synced with Cloud! ☁️", Toast.LENGTH_SHORT).show();
                            saveResultToLocalHistory(formattedCgpa);
                        }
                    }
                    @Override public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {}
                });
    }

    private void updateUI(double gpa, double cgpa, String formatted) {
        tvGpaResult.setText(String.format("Semester GPA: %.2f", gpa));
        tvCgpaResult.setText("Cumulative CGPA: " + formatted);
        cardResult.setVisibility(View.VISIBLE);
        tvCgpaBig.setText(formatted);
        progressCgpa.setProgress((int) (cgpa * 100));
        updateStanding(cgpa);
    }

    private void setupSemesterTitle() {
        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String savedSemester = sharedPreferences.getString("current_semester_name", "Winter 2026");
        tvSemesterTitle.setText(savedSemester);

        tvSemesterTitle.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle("Enter Semester Name");
            final EditText input = new EditText(getContext());
            input.setInputType(InputType.TYPE_CLASS_TEXT);
            input.setText(tvSemesterTitle.getText().toString());
            builder.setView(input);

            builder.setPositiveButton("Save", (dialog, which) -> {
                String name = input.getText().toString().trim();
                if (!name.isEmpty()) {
                    tvSemesterTitle.setText(name);
                    sharedPreferences.edit().putString("current_semester_name", name).apply();
                }
            });
            builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
            builder.show();
        });
    }

    private void addCourseRow() {
        courseDataList.add(new CourseEntry());
        courseAdapter.notifyItemInserted(courseDataList.size() - 1);
        recyclerCourses.smoothScrollToPosition(courseDataList.size() - 1);
    }

    private void openHistory() {
        getParentFragmentManager().beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.fragment_container, new HistoryFragment())
                .addToBackStack(null)
                .commit();
    }

    private void updateStanding(double cgpa) {
        if (tvStandingLabel == null) return;
        String status;
        int color;

        if (cgpa >= 3.75) {
            status = "Excellent Standing";
            color = Color.parseColor("#10B981");
        } else if (cgpa >= 3.00) {
            status = "Good Standing";
            color = Color.parseColor("#3B82F6");
        } else if (cgpa >= 2.20) {
            status = "Average Standing";
            color = Color.parseColor("#F59E0B");
        } else if (cgpa == 0.0) {
            status = "No Result Calculated";
            color = Color.GRAY;
        } else {
            status = "Probation Risk";
            color = Color.parseColor("#EF4444");
        }

        tvStandingLabel.setText(status);
        if (tvStandingLabel.getBackground() != null) {
            tvStandingLabel.getBackground().setTint(color);
        }
    }

    private void saveResultToLocalHistory(String cgpa) {
        SharedPreferences prefs = requireActivity().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String history = prefs.getString("cgpa_history_list", "");

        if (history.isEmpty()) {
            history = cgpa;
        } else {
            history = history + "," + cgpa;
        }
        prefs.edit().putString("cgpa_history_list", history).apply();
    }
}