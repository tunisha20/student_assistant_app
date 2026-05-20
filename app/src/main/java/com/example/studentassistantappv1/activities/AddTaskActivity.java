package com.example.studentassistantappv1.activities;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.studentassistantappv1.R;
import com.example.studentassistantappv1.data.SupabaseApi;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class AddTaskActivity extends AppCompatActivity {

    private EditText etTaskTitle, etDescription;
    private TextView tvDueDate, btnAcademic, btnPersonal, btnWork, btnProject;
    private TextView btnLow, btnMed, btnHigh;
    private Button btnSaveTask;
    private ImageView btnBack;

    private String selectedCategory = "Academic";
    private String selectedPriority = "Med";
    private SupabaseApi supabaseApi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_task);

        initViews();
        initRetrofit();

        // ডিফল্ট সিলেকশন
        selectCategory("Academic", btnAcademic);
        selectPriority("Med", btnMed);

        tvDueDate.setOnClickListener(v -> showDatePicker());

        // ক্যাটাগরি ও প্রায়োরিটি লজিক (আগের মতই)
        btnAcademic.setOnClickListener(v -> selectCategory("Academic", btnAcademic));
        btnPersonal.setOnClickListener(v -> selectCategory("Personal", btnPersonal));
        btnWork.setOnClickListener(v -> selectCategory("Work", btnWork));
        if (btnProject != null) btnProject.setOnClickListener(v -> selectCategory("Project", btnProject));

        btnLow.setOnClickListener(v -> selectPriority("Low", btnLow));
        btnMed.setOnClickListener(v -> selectPriority("Med", btnMed));
        btnHigh.setOnClickListener(v -> selectPriority("High", btnHigh));

        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        btnSaveTask.setOnClickListener(v -> postTaskToSupabase());
    }

    private void initViews() {
        etTaskTitle = findViewById(R.id.etTaskTitle);
        etDescription = findViewById(R.id.etNoteDetails);
        tvDueDate = findViewById(R.id.tvDueDate);
        btnSaveTask = findViewById(R.id.btnSaveTask);
        btnBack = findViewById(R.id.btnBack);
        btnAcademic = findViewById(R.id.btnAcademic);
        btnPersonal = findViewById(R.id.btnPersonal);
        btnWork = findViewById(R.id.btnWork);
        btnProject = findViewById(R.id.btnProject);
        btnLow = findViewById(R.id.btnLow);
        btnMed = findViewById(R.id.btnMed);
        btnHigh = findViewById(R.id.btnHigh);
    }

    private void initRetrofit() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://kjanqxiaynuewhqbpdiw.supabase.co/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        supabaseApi = retrofit.create(SupabaseApi.class);
    }

    private void postTaskToSupabase() {
        String title = etTaskTitle.getText().toString().trim();
        String date = tvDueDate.getText().toString();
        String desc = etDescription.getText().toString().trim();

        if (title.isEmpty() || date.equals("mm/dd/yyyy")) {
            Toast.makeText(this, "Please fill Title and Date", Toast.LENGTH_SHORT).show();
            return;
        }

        // SharedPreferences থেকে ইউজার আইডি ও টোকেন নেওয়া
        SharedPreferences prefs = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        String userId = prefs.getString("user_id", "");
        // নিশ্চিত করুন "Bearer " এর পর একটি স্পেস আছে
        String authToken = "Bearer " + prefs.getString("auth_token", "");

        if (userId.isEmpty()) {
            Toast.makeText(this, "User ID not found! Login again.", Toast.LENGTH_SHORT).show();
            return;
        }

        // সুপাবেস টেবিল কলাম অনুযায়ী ম্যাপ তৈরি (আপনার Task মডেলে 'subject' কলাম আছে)
        Map<String, Object> taskData = new HashMap<>();
        taskData.put("user_id", userId);
        taskData.put("title", title);
        taskData.put("description", desc);
        taskData.put("subject", selectedCategory); // আপনার মডেলে subject = category
        taskData.put("due_date", date);
        taskData.put("priority", selectedPriority);
        taskData.put("is_completed", false);

        supabaseApi.insertTask(
                SupabaseApi.apiKey,
                authToken,
                "application/json",
                "return=representation",
                taskData
        ).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    // ড্যাশবোর্ডের জন্য লোকাল সেভ
                    prefs.edit().putString("last_task_title", title).apply();

                    Toast.makeText(AddTaskActivity.this, "Task Added Successfully! ✅", Toast.LENGTH_SHORT).show();
                    setResult(Activity.RESULT_OK); // এটি TaskFragment-কে সিগনাল দিবে ডাটা রিফ্রেশ করতে
                    finish();
                } else {
                    Log.e("SupabaseError", "Code: " + response.code());
                    Toast.makeText(AddTaskActivity.this, "Error: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(AddTaskActivity.this, "Network Error!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showDatePicker() {
        final Calendar c = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, year, monthOfYear, dayOfMonth) ->
                        tvDueDate.setText((monthOfYear + 1) + "/" + dayOfMonth + "/" + year),
                c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.show();
    }

    private void selectCategory(String category, TextView selectedBtn) {
        selectedCategory = category;
        btnAcademic.setBackgroundResource(R.drawable.bg_chip_unselected);
        btnPersonal.setBackgroundResource(R.drawable.bg_chip_unselected);
        btnWork.setBackgroundResource(R.drawable.bg_chip_unselected);
        if (btnProject != null) btnProject.setBackgroundResource(R.drawable.bg_chip_unselected);

        btnAcademic.setTextColor(Color.parseColor("#44474E"));
        btnPersonal.setTextColor(Color.parseColor("#44474E"));
        btnWork.setTextColor(Color.parseColor("#44474E"));
        if (btnProject != null) btnProject.setTextColor(Color.parseColor("#44474E"));

        selectedBtn.setBackgroundResource(R.drawable.bg_chip_selected);
        selectedBtn.setTextColor(Color.WHITE);
    }

    private void selectPriority(String priority, TextView selectedBtn) {
        selectedPriority = priority;
        btnLow.setBackgroundResource(R.drawable.bg_priority_left);
        btnMed.setBackgroundResource(R.drawable.bg_priority_left);
        btnHigh.setBackgroundResource(R.drawable.bg_priority_right);

        btnLow.setTextColor(Color.BLACK);
        btnMed.setTextColor(Color.BLACK);
        btnHigh.setTextColor(Color.BLACK);

        selectedBtn.setBackgroundResource(R.drawable.bg_priority_selected);
        selectedBtn.setTextColor(Color.parseColor("#2962FF"));
    }
}