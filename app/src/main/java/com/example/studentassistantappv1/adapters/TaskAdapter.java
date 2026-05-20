package com.example.studentassistantappv1.adapters;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.studentassistantappv1.R;
import com.example.studentassistantappv1.data.SupabaseApi;
import com.example.studentassistantappv1.models.Task;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.ViewHolder> {

    private List<Task> taskList;
    private SupabaseApi supabaseApi;

    public TaskAdapter(List<Task> taskList, SupabaseApi supabaseApi) {
        this.taskList = taskList;
        this.supabaseApi = supabaseApi;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_task, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Task task = taskList.get(position);

        // ১. লিসেনার নাল করা (স্ক্রল এরর এড়াতে এটি মাস্ট)
        holder.checkTask.setOnCheckedChangeListener(null);

        holder.tvTaskTitle.setText(task.getTitle());
        holder.tvSubInfo.setText(task.getCategory() + " • " + task.getDate());

        // ২. স্ট্যাটাস অনুযায়ী UI সেট করা
        holder.checkTask.setChecked(task.isCompleted());
        updateTextStyle(holder, task.isCompleted());

        // ৩. নতুন লিসেনার সেট করা
        holder.checkTask.setOnCheckedChangeListener((buttonView, isChecked) -> {
            task.setCompleted(isChecked);
            updateTextStyle(holder, isChecked);

            // ক্লাউডে আপডেট পাঠানো
            updateStatusInSupabase(holder.itemView.getContext(), task);
        });
    }

    private void updateStatusInSupabase(Context context, Task task) {
        SharedPreferences prefs = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        String authToken = "Bearer " + prefs.getString("auth_token", "");

        Map<String, Object> body = new HashMap<>();
        body.put("is_completed", task.isCompleted());

        supabaseApi.updateTask(SupabaseApi.apiKey, authToken, "eq." + task.getId(), body)
                .enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                        // Success - No action needed
                    }

                    @Override
                    public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                        // Error handling could be added here
                    }
                });
    }

    private void updateTextStyle(ViewHolder holder, boolean isCompleted) {
        if (isCompleted) {
            holder.tvTaskTitle.setPaintFlags(holder.tvTaskTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            holder.tvTaskTitle.setTextColor(Color.GRAY);
        } else {
            holder.tvTaskTitle.setPaintFlags(holder.tvTaskTitle.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            holder.tvTaskTitle.setTextColor(Color.parseColor("#1A1C1E"));
        }
    }

    @Override
    public int getItemCount() { return taskList.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        CheckBox checkTask;
        TextView tvTaskTitle, tvSubInfo;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            checkTask = itemView.findViewById(R.id.checkTask);
            tvTaskTitle = itemView.findViewById(R.id.tvTaskTitle);
            tvSubInfo = itemView.findViewById(R.id.tvSubInfo);
        }
    }
}