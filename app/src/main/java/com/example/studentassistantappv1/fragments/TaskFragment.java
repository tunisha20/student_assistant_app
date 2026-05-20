package com.example.studentassistantappv1.fragments;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.studentassistantappv1.R;
import com.example.studentassistantappv1.activities.AddTaskActivity;
import com.example.studentassistantappv1.adapters.TaskAdapter;
import com.example.studentassistantappv1.broadcasts.DeadlineReceiver;
import com.example.studentassistantappv1.data.SupabaseApi;
import com.example.studentassistantappv1.models.Task;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class TaskFragment extends Fragment {

    private static final String TAG = "SupabaseDebug";
    private RecyclerView recyclerUpcomingTasks;
    private ExtendedFloatingActionButton btnAddTask;
    private List<Task> taskList = new ArrayList<>();
    private TaskAdapter adapter;
    private SupabaseApi supabaseApi;

    private TextView tvPriorityTitle, tvPrioritySub, tvPendingCount;

    private final ActivityResultLauncher<Intent> addTaskLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Log.d(TAG, "New task added, refreshing...");
                    fetchTasksFromSupabase();
                }
            }
    );

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_tasks, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        initRetrofit();
        setupRecyclerView();
        fetchTasksFromSupabase();

        if (btnAddTask != null) {
            btnAddTask.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), AddTaskActivity.class);
                addTaskLauncher.launch(intent);
            });
        }
    }

    private void initViews(View view) {
        recyclerUpcomingTasks = view.findViewById(R.id.recyclerUpcomingTasks);
        btnAddTask = view.findViewById(R.id.btnAddTasks);
        tvPriorityTitle = view.findViewById(R.id.tvPriorityTitle);
        tvPrioritySub = view.findViewById(R.id.tvPrioritySub);
        tvPendingCount = view.findViewById(R.id.tvPendingCount);
    }

    private void initRetrofit() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://kjanqxiaynuewhqbpdiw.supabase.co/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        supabaseApi = retrofit.create(SupabaseApi.class);
    }

    private void setupRecyclerView() {
        recyclerUpcomingTasks.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new TaskAdapter(taskList, supabaseApi);
        recyclerUpcomingTasks.setAdapter(adapter);
    }

    private void fetchTasksFromSupabase() {
        SharedPreferences prefs = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        String userId = prefs.getString("user_id", "");
        String authToken = "Bearer " + prefs.getString("auth_token", "");

        if (userId.isEmpty()) {
            Log.e(TAG, "User ID is empty! Login again.");
            return;
        }

        String userIdFilter = "eq." + userId;
        String order = "created_at.desc";

        supabaseApi.getTasks(SupabaseApi.apiKey, authToken, "*", userIdFilter, order)
                .enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                        if (response.isSuccessful() && response.body() != null && isAdded()) {
                            try {
                                String json = response.body().string();
                                List<Task> cloudTasks = new Gson().fromJson(json, new TypeToken<List<Task>>(){}.getType());

                                if (cloudTasks != null) {
                                    taskList.clear();
                                    taskList.addAll(cloudTasks);

                                    requireActivity().runOnUiThread(() -> {
                                        adapter.notifyDataSetChanged();
                                        updateUIElements(cloudTasks);
                                    });
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing JSON: " + e.getMessage());
                            }
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                        if (isAdded()) Log.e(TAG, "Network Failure: " + t.getMessage());
                    }
                });
    }

    private void updateUIElements(List<Task> tasks) {
        int pending = 0;
        for (Task t : tasks) {
            if (!t.isCompleted()) {
                pending++;
                // প্রতিটা পেন্ডিং টাস্কের জন্য নোটিফিকেশন শিডিউল করবে
                scheduleDeadlineNotification(t);
            }
        }

        if (tvPendingCount != null) tvPendingCount.setText(pending + " Pending");

        if (!tasks.isEmpty()) {
            Task latest = tasks.get(0);
            if (tvPriorityTitle != null) tvPriorityTitle.setText(latest.getTitle());
            if (tvPrioritySub != null) {
                tvPrioritySub.setText(latest.getCategory() + " • " + latest.getDate());
            }
            saveTaskForDashboard(latest);
        }
    }

    private void scheduleDeadlineNotification(Task task) {
        if (task.getDate() == null || task.getDate().isEmpty()) return;

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date deadlineDate = sdf.parse(task.getDate());

            if (deadlineDate == null) return;

            Calendar calendar = Calendar.getInstance();
            calendar.setTime(deadlineDate);
            calendar.set(Calendar.HOUR_OF_DAY, 9); // সকাল ৯ টায়
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);

            if (calendar.getTimeInMillis() > System.currentTimeMillis()) {
                Intent intent = new Intent(getContext(), DeadlineReceiver.class);
                intent.putExtra("task_title", task.getTitle());

                PendingIntent pendingIntent = PendingIntent.getBroadcast(
                        getContext(),
                        task.hashCode(),
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                );

                AlarmManager alarmManager = (AlarmManager) requireContext().getSystemService(Context.ALARM_SERVICE);

                if (alarmManager != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        if (alarmManager.canScheduleExactAlarms()) {
                            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
                        } else {
                            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
                        }
                    } else {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
                    }
                    Log.d(TAG, "Notification set for: " + task.getTitle() + " at " + calendar.getTime());
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Alarm Error for " + task.getTitle() + ": " + e.getMessage());
        }
    }

    private void saveTaskForDashboard(Task task) {
        SharedPreferences prefs = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        prefs.edit().putString("last_task_title", task.getTitle()).apply();
    }

    @Override
    public void onResume() {
        super.onResume();
        fetchTasksFromSupabase();
    }
}