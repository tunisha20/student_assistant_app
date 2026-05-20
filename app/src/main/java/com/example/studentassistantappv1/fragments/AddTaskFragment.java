package com.example.studentassistantappv1.fragments;

import android.app.DatePickerDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.studentassistantappv1.R;
import com.example.studentassistantappv1.data.SupabaseApi; // নিশ্চিত করুন এই ইন্টারফেসটি তৈরি আছে
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class AddTaskFragment extends Fragment {

    private EditText etTaskTitle, etDescription;
    private TextView tvDueDate, tvLow, tvMedium, tvHigh;
    private RelativeLayout layoutDueDate;
    private ChipGroup chipGroupSubjects;
    private MaterialButton btnSaveTask;

    private String selectedPriority = "Medium";
    private String selectedDate = "";

    // Supabase Credentials (আপনার ড্যাশবোর্ড থেকে নিন)
    private final String SUPABASE_URL = "https://kjanqxiaynuewhqbpdiw.supabase.co/rest/v1/";
    private final String ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImtqYW5xeGlheW51ZXdocWJwZGl3Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzgyMDE5NTEsImV4cCI6MjA5Mzc3Nzk1MX0.tuI3MaR4nLxc898NwWo_QbZpjfTxpu8sD42b_c7oSCA";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_add_new_task, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);

        if (layoutDueDate != null) {
            layoutDueDate.setOnClickListener(v -> showDatePicker());
        }

        setupPrioritySelection();

        btnSaveTask.setOnClickListener(v -> {
            String title = etTaskTitle.getText().toString().trim();
            if (title.isEmpty()) {
                etTaskTitle.setError("Title is required");
                return;
            }

            // চিপ থেকে সাবজেক্ট নেওয়া
            int checkedId = chipGroupSubjects.getCheckedChipId();
            String subject = "General";
            if (checkedId != View.NO_ID) {
                Chip chip = chipGroupSubjects.findViewById(checkedId);
                subject = chip.getText().toString();
            }

            saveTaskToJava(title, selectedPriority, subject);
        });
    }

    private void initViews(View view) {
        etTaskTitle = view.findViewById(R.id.et_task_title);
        etDescription = view.findViewById(R.id.et_description);
        //tvDueDate = view.findViewById(R.id.tv_due_date);
        //layoutDueDate = view.findViewById(R.id.layout_due_date);
        chipGroupSubjects = view.findViewById(R.id.chip_group_subjects);
        btnSaveTask = view.findViewById(R.id.btn_save_task);

        tvLow = view.findViewById(R.id.tv_priority_low);
        tvMedium = view.findViewById(R.id.tv_priority_medium);
        tvHigh = view.findViewById(R.id.tv_priority_high);
    }

    private void setupPrioritySelection() {
        View.OnClickListener priorityListener = v -> {
            resetPriorityStyles();
            TextView selected = (TextView) v;
            selected.setBackgroundResource(R.drawable.bg_priority_selected);
            selected.setTextColor(Color.parseColor("#2962FF"));
            selected.setTypeface(null, Typeface.BOLD);
            selectedPriority = selected.getText().toString();
        };

        if (tvLow != null) tvLow.setOnClickListener(priorityListener);
        if (tvMedium != null) tvMedium.setOnClickListener(priorityListener);
        if (tvHigh != null) tvHigh.setOnClickListener(priorityListener);
    }

    private void resetPriorityStyles() {
        TextView[] priorities = {tvLow, tvMedium, tvHigh};
        for (TextView tv : priorities) {
            if (tv != null) {
                tv.setBackground(null);
                tv.setTextColor(Color.parseColor("#64748B"));
                tv.setTypeface(null, Typeface.NORMAL);
            }
        }
    }

    private void showDatePicker() {
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        new DatePickerDialog(requireContext(), (view, y, m, d) -> {
            selectedDate = String.format(Locale.getDefault(), "%02d/%02d/%d", d, (m + 1), y);
            if (tvDueDate != null) {
                tvDueDate.setText(selectedDate);
                tvDueDate.setTextColor(Color.parseColor("#1E293B"));
            }
        }, year, month, day).show();
    }

    private void saveTaskToJava(String title, String priority, String subject) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(SUPABASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        SupabaseApi api = retrofit.create(SupabaseApi.class);

        Map<String, Object> taskData = new HashMap<>();
        taskData.put("title", title);
        taskData.put("priority", priority);
        taskData.put("subject", subject);
        taskData.put("due_date", selectedDate);
        taskData.put("description", etDescription.getText().toString());

        String auth = "Bearer " + ANON_KEY;

        api.insertTask(ANON_KEY, auth, "application/json", "return=minimal", taskData)
                .enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                        if (response.isSuccessful()) {
                            Toast.makeText(getContext(), "Saved successfully to Supabase!", Toast.LENGTH_SHORT).show();

                            // ফোকাস ফ্র্যাগমেন্টে ডাটা পাস করা
                            Bundle result = new Bundle();
                            result.putString("selectedTaskName", title);
                            getParentFragmentManager().setFragmentResult("taskKey", result);

                            getParentFragmentManager().popBackStack();
                        } else {
                            Toast.makeText(getContext(), "Failed: " + response.code(), Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                        Toast.makeText(getContext(), "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}