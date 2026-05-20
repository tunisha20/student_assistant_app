package com.example.studentassistantappv1.adapters;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.studentassistantappv1.R;
import com.example.studentassistantappv1.data.SupabaseApi; // ✅ এপিআই ইমপোর্ট
import com.example.studentassistantappv1.models.Routine;
import com.google.android.material.button.MaterialButton;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RoutineAdapter extends RecyclerView.Adapter<RoutineAdapter.RoutineViewHolder> {

    private List<Routine> routineList;
    private SupabaseApi supabaseApi; // ✅ সুপাবেস এপিআই রেফারেন্স

    // ১. কনস্ট্রাক্টরে এপিআই যুক্ত করা হয়েছে
    public RoutineAdapter(List<Routine> routineList, SupabaseApi supabaseApi) {
        this.routineList = routineList;
        this.supabaseApi = supabaseApi;
    }

    @NonNull
    @Override
    public RoutineViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_routine, parent, false);
        return new RoutineViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RoutineViewHolder holder, int position) {
        Routine routine = routineList.get(position);

        holder.tvSubject.setText(routine.getSubject());
        holder.tvStartTime.setText(routine.getStartTime());
        holder.tvEndTime.setText(routine.getEndTime());
        holder.tvRoom.setText("Room: " + routine.getRoom() + " • " + routine.getType());

        // ক্যানসেলড স্ট্যাটাস অনুযায়ী UI আপডেট
        updateCancelUI(holder, routine.isCancelled());

        holder.btnCancel.setOnClickListener(v -> {
            // ২. লোকাল আপডেট ও ক্লাউড সিঙ্ক কল করা
            updateRoutineStatusInCloud(holder.itemView.getContext(), routine, position);
        });
    }

    // ✅ সুপাবেসে স্ট্যাটাস (is_cancelled) আপডেট করার মেথড
    private void updateRoutineStatusInCloud(Context context, Routine routine, int position) {
        SharedPreferences prefs = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        String authToken = "Bearer " + prefs.getString("auth_token", "");

        Map<String, Object> body = new HashMap<>();
        body.put("is_cancelled", true); // সুপাবেসে কলামের নাম অনুযায়ী

        supabaseApi.updateRoutine(SupabaseApi.apiKey, authToken, "eq." + routine.getId(), body)
                .enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                        if (response.isSuccessful()) {
                            routine.setCancelled(true);
                            notifyItemChanged(position);
                        }
                    }

                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                        // এরর হ্যান্ডলিং
                    }
                });
    }

    private void updateCancelUI(RoutineViewHolder holder, boolean isCancelled) {
        if (isCancelled) {
            holder.itemView.setAlpha(0.4f);
            holder.tvSubject.setPaintFlags(holder.tvSubject.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            holder.btnCancel.setText("Cancelled");
            holder.btnCancel.setEnabled(false);
        } else {
            holder.itemView.setAlpha(1.0f);
            holder.tvSubject.setPaintFlags(holder.tvSubject.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            holder.btnCancel.setText("Cancel");
            holder.btnCancel.setEnabled(true);
        }
    }

    @Override
    public int getItemCount() { return routineList.size(); }

    public static class RoutineViewHolder extends RecyclerView.ViewHolder {
        TextView tvSubject, tvStartTime, tvEndTime, tvRoom;
        MaterialButton btnCancel;

        public RoutineViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSubject = itemView.findViewById(R.id.tvCardSubject);
            tvStartTime = itemView.findViewById(R.id.tvCardStartTime);
            tvEndTime = itemView.findViewById(R.id.tvCardEndTime);
            tvRoom = itemView.findViewById(R.id.tvCardRoom);
            btnCancel = itemView.findViewById(R.id.btnCancelClass);
        }
    }
}