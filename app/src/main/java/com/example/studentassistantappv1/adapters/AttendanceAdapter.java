package com.example.studentassistantappv1.adapters;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.studentassistantappv1.R;
import com.example.studentassistantappv1.data.SupabaseApi;
import com.example.studentassistantappv1.models.Attendance;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AttendanceAdapter extends RecyclerView.Adapter<AttendanceAdapter.ViewHolder> {

    private List<Attendance> attendanceList;
    private Context context;
    private OnAttendanceActionListener actionListener;
    private SupabaseApi supabaseApi;
    private String authToken;

    public interface OnAttendanceActionListener {
        void onAction(boolean shouldAnimate);
    }

    public AttendanceAdapter(List<Attendance> attendanceList, Context context,
                             SupabaseApi api, String token, OnAttendanceActionListener actionListener) {
        this.attendanceList = attendanceList;
        this.context = context;
        this.supabaseApi = api;
        this.authToken = token;
        this.actionListener = actionListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_attendance, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Attendance item = attendanceList.get(position);

        holder.tvSubject.setText(item.getSubjectName());
        holder.tvStats.setText("Present: " + item.getPresentClasses() + " / " + item.getTotalClasses());

        // ক্যালকুলেশন লজিক
        int percentage = (item.getTotalClasses() > 0) ? (item.getPresentClasses() * 100) / item.getTotalClasses() : 0;
        holder.progressBar.setProgress(percentage, true);
        holder.tvPercentage.setText(percentage + "%");

        // কনসিস্টেন্সি লজিক (রঙ পরিবর্তন)
        int statusColor = (percentage >= 75) ? Color.parseColor("#00C2FF") : Color.parseColor("#EF4444");
        holder.progressBar.setIndicatorColor(statusColor);
        holder.tvPercentage.setTextColor(statusColor);
        holder.tvStatus.setText(percentage >= 75 ? "Safe Zone 🛡️" : "Danger Zone ⚠️");
        holder.tvStatus.setTextColor(statusColor);

        // Present বাটনে ক্লিক করলে ডাটাবেজ আপডেট হবে
        holder.btnPresent.setOnClickListener(v -> {
            int newPresent = item.getPresentClasses() + 1;
            int newTotal = item.getTotalClasses() + 1;
            updateAttendanceOnSupabase(item, newPresent, newTotal, position);
        });

        // Absent বাটনে ক্লিক করলে ডাটাবেজ আপডেট হবে
        holder.btnAbsent.setOnClickListener(v -> {
            int newTotal = item.getTotalClasses() + 1;
            updateAttendanceOnSupabase(item, item.getPresentClasses(), newTotal, position);
        });
    }

    private void updateAttendanceOnSupabase(Attendance item, int newPresent, int newTotal, int position) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("present_classes", newPresent);
        updates.put("total_classes", newTotal);

        // Supabase-এ PATCH রিকোয়েস্ট পাঠানো
        supabaseApi.updateAttendance(
                SupabaseApi.apiKey,
                authToken,
                "application/json",
                "eq." + item.getId(),
                updates
        ).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    // সফল হলে লোকাল লিস্ট আপডেট এবং UI রিফ্রেশ
                    item.setPresentClasses(newPresent);
                    item.setTotalClasses(newTotal);
                    notifyItemChanged(position);

                    // ড্যাশবোর্ড বা ওভারভিউ আপডেট করার জন্য লিসেনার কল
                    if (actionListener != null) {
                        actionListener.onAction(true);
                    }
                } else {
                    Log.e("Supabase_Update", "Error Code: " + response.code());
                    Toast.makeText(context, "Update Failed! Please try again.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                Log.e("Supabase_Update", "Network Error: " + t.getMessage());
                Toast.makeText(context, "Check Internet Connection", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return attendanceList != null ? attendanceList.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvSubject, tvStats, tvPercentage, tvStatus;
        CircularProgressIndicator progressBar;
        Button btnPresent, btnAbsent;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSubject = itemView.findViewById(R.id.tv_subject_name);
            tvStats = itemView.findViewById(R.id.tv_class_count);
            tvPercentage = itemView.findViewById(R.id.tv_percent);
            progressBar = itemView.findViewById(R.id.progress_subject);
            tvStatus = itemView.findViewById(R.id.tv_status);
            btnPresent = itemView.findViewById(R.id.btn_present);
            btnAbsent = itemView.findViewById(R.id.btn_absent);
        }
    }
}