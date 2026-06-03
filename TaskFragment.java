package com.example.studentassistantappv1.fragments;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
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
import androidx.core.content.ContextCompat;
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

    // AddTaskActivity থেকে ডেটা অ্যাড হয়ে ব্যাক করলে লিস্ট রিফ্রেশ করার লঞ্চার
    private final ActivityResultLauncher<Intent> addTaskLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Log.d(TAG, "New task added, refreshing...");
                    fetchTasksFromSupabase();
                }
            }
    );

    // অ্যান্ড্রয়েড ১৩+ নোটিফিকেশন পারমিশন রিকোয়েস্ট হ্যান্ডেলার
    private final ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    Log.d(TAG, "Notification permission granted");
                    fetchTasksFromSupabase();
                } else {
                    Toast.makeText(getContext(), "Notification permission denied. Deadlines won't be alerted!", Toast.LENGTH_SHORT).show();
                    fetchTasksFromSupabase(); // পারমিশন ডিনাই করলেও ডেটা ক্লাউড থেকে লোড হবে
                }
            }
    );

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // আপনার দেওয়া এক্সএমএল লেআউট ফাইলটি ইনফ্লেট করা হলো
        return inflater.inflate(R.layout.fragment_tasks, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        initRetrofit();
        setupRecyclerView();

        // রানটাইম নোটিফিকেশন পারমিশন চেক (অ্যান্ড্রয়েড ১৩+)
        checkNotificationPermission();

        if (btnAddTask != null) {
            btnAddTask.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), AddTaskActivity.class);
                addTaskLauncher.launch(intent);
            });
        }
    }

    private void initViews(View view) {
        // আপনার দেওয়া এক্সএমএল-এর আইডিগুলোর সাথে নিখুঁতভাবে ম্যাপিং করা হলো
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

    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            } else {
                fetchTasksFromSupabase();
            }
        } else {
            fetchTasksFromSupabase();
        }
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
        List<Task> pendingTasks = new ArrayList<>();

        for (Task t : tasks) {
            if (!t.isCompleted()) {
                pending++;
                pendingTasks.add(t);
                // প্রতিটি পেন্ডিং টাস্কের জন্য অ্যালার্ম নোটিফিকেশন সেটআপ করবে
                scheduleDeadlineNotification(t);
            }
        }

        // এক্সএমএল এর রাইট সাইডের কাউন্টার আপডেট (e.g., "3 Pending")
        if (tvPendingCount != null) {
            tvPendingCount.setText(pending + " Pending");
        }

        // টপ প্রায়োরিটি কার্ড ভিউ আপডেট লজিক
        if (!pendingTasks.isEmpty()) {
            // সবচেয়ে কাছের বা শেষ পেন্ডিং টাস্কটি প্রায়োরিটি কার্ডে দেখাবে
            Task latestPriority = pendingTasks.get(0);
            if (tvPriorityTitle != null) tvPriorityTitle.setText(latestPriority.getTitle());
            if (tvPrioritySub != null) {
                tvPrioritySub.setText(latestPriority.getCategory().toUpperCase() + " • DUE SOON");
            }
            saveTaskForDashboard(latestPriority);
        } else if (!tasks.isEmpty()) {
            // কোনো পেন্ডিং না থাকলে নরমাল শেষ টাস্ক দেখাবে
            Task completedLatest = tasks.get(0);
            if (tvPriorityTitle != null) tvPriorityTitle.setText(completedLatest.getTitle());
            if (tvPrioritySub != null) {
                tvPrioritySub.setText(completedLatest.getCategory().toUpperCase() + " • COMPLETED");
            }
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

            // ⏰ ডেডলাইনের দিন রাত ১২:০০ টা (00:00 AM) সেট করা হলো
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);

            // অ্যালার্মের সময় বর্তমান সময়ের চেয়ে ভবিষ্যতে হলে শিডিউল করবে
            if (calendar.getTimeInMillis() > System.currentTimeMillis()) {
                Intent intent = new Intent(getContext(), DeadlineReceiver.class);
                intent.putExtra("task_title", task.getTitle());

                PendingIntent pendingIntent = PendingIntent.getBroadcast(
                        getContext(),
                        task.hashCode(), // প্রতিটি টাস্কের ইউনিক রিকোয়েস্ট কোড
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
                    Log.d(TAG, "Notification set for: " + task.getTitle() + " at midnight: " + calendar.getTime());
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
        // অন-রিজিউমে অটো-সিঙ্ক ও পারমিশন লেভেল রি-চেক
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED) {
                fetchTasksFromSupabase();
            }
        } else {
            fetchTasksFromSupabase();
        }
    }
}