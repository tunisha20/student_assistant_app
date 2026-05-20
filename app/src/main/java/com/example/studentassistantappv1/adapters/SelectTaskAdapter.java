package com.example.studentassistantappv1.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.studentassistantappv1.R;
import com.example.studentassistantappv1.models.Task;
import com.google.android.material.card.MaterialCardView;
import java.util.List;

public class SelectTaskAdapter extends RecyclerView.Adapter<SelectTaskAdapter.ViewHolder> {

    private List<Task> taskList;
    private int selectedPosition = -1;
    private OnTaskSelectedListener listener;

    public interface OnTaskSelectedListener {
        void onTaskSelected(Task task);
    }

    public SelectTaskAdapter(List<Task> taskList, OnTaskSelectedListener listener) {
        this.taskList = taskList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_selectable_task, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Task task = taskList.get(position);
        holder.tvTitle.setText(task.getTitle());
        holder.tvSub.setText(task.getCategory() + " • " + task.getDate());
        
        holder.radioButton.setChecked(position == selectedPosition);

        holder.cardTask.setOnClickListener(v -> {
            selectedPosition = holder.getAdapterPosition();
            notifyDataSetChanged();
            if (listener != null) {
                listener.onTaskSelected(task);
            }
        });
    }

    @Override
    public int getItemCount() {
        return taskList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvSub;
        RadioButton radioButton;
        MaterialCardView cardTask;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTaskTitle);
            tvSub = itemView.findViewById(R.id.tvTaskSub);
            radioButton = itemView.findViewById(R.id.radioBtn);
            cardTask = itemView.findViewById(R.id.cardTask);
        }
    }
}
