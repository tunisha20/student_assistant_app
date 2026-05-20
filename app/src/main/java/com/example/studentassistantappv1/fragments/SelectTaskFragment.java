package com.example.studentassistantappv1.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.studentassistantappv1.R;
import com.example.studentassistantappv1.adapters.SelectTaskAdapter;
import com.example.studentassistantappv1.data.SupabaseApi;
import com.example.studentassistantappv1.models.Task;
import com.google.android.material.button.MaterialButton;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class SelectTaskFragment extends Fragment {

    private RecyclerView recyclerSelectableTasks;
    private SelectTaskAdapter adapter;
    private List<Task> taskList = new ArrayList<>();
    private Task selectedTask;
    private MaterialButton btnConfirm;
    private TextView tvAddNewTask;
    private SupabaseApi supabaseApi;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_select_task, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        initRetrofit();
        setupRecyclerView();
        fetchTasksFromSupabase(); // ক্লাউড থেকে ডাটা লোড

        // কনফার্ম বাটন লজিক
        btnConfirm.setOnClickListener(v -> {
            if (selectedTask != null) {
                Bundle result = new Bundle();
                result.putString("selectedTaskName", selectedTask.getTitle());
                getParentFragmentManager().setFragmentResult("taskKey", result);
                getParentFragmentManager().popBackStack();
            }
        });

        // Add New Task লজিক
        tvAddNewTask.setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new AddTaskFragment())
                    .addToBackStack(null)
                    .commit();
        });
    }

    private void initViews(View view) {
        recyclerSelectableTasks = view.findViewById(R.id.recyclerSelectableTasks);
        btnConfirm = view.findViewById(R.id.btn_confirm_selection);
        tvAddNewTask = view.findViewById(R.id.tvAddNewTask);

        btnConfirm.setEnabled(false);
        btnConfirm.setAlpha(0.5f);
    }

    private void initRetrofit() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://kjanqxiaynuewhqbpdiw.supabase.co/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        supabaseApi = retrofit.create(SupabaseApi.class);
    }

    private void setupRecyclerView() {
        recyclerSelectableTasks.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new SelectTaskAdapter(taskList, task -> {
            selectedTask = task;
            btnConfirm.setEnabled(true);
            btnConfirm.setAlpha(1.0f);
        });
        recyclerSelectableTasks.setAdapter(adapter);
    }

    private void fetchTasksFromSupabase() {
        SharedPreferences prefs = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        String userId = prefs.getString("user_id", "");
        String authToken = "Bearer " + prefs.getString("auth_token", "");

        if (userId.isEmpty()) return;

        supabaseApi.getTasks(SupabaseApi.apiKey, authToken, "*", "eq." + userId)
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
                                    adapter.notifyDataSetChanged();
                                }
                            } catch (Exception e) { e.printStackTrace(); }
                        }
                    }
                    @Override public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                        if (isAdded()) Toast.makeText(getContext(), "Failed to load tasks!", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}