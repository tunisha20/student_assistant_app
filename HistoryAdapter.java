package com.example.studentassistantappv1.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.studentassistantappv1.R;
import com.example.studentassistantappv1.models.SemesterRecord;
import java.util.List;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

    private final List<SemesterRecord> recordList;

    public HistoryAdapter(List<SemesterRecord> recordList) {
        this.recordList = recordList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_semester_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SemesterRecord record = recordList.get(position);

        if (record != null) {
            // সুপাবেসের কলাম অনুযায়ী ডেটা সেট করা হচ্ছে
            holder.name.setText(record.getName());          // সেমিস্টার নাম (e.g., Level 3 Term 1)
            holder.stats.setText(record.getDetails());      // ক্রেডিট ও সেমিস্টার জিপিএ ডিটেইলস
            holder.gpa.setText(record.getGpa());            // ক্যালকুলেটেড জিপিএ ভ্যালু
            holder.grade.setText(record.getGradeLabel());    // গ্রেড লেটার (e.g., A+)
        }
    }

    @Override
    public int getItemCount() {
        return recordList != null ? recordList.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name, stats, gpa, grade;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.tvSemesterName);
            stats = itemView.findViewById(R.id.tvSemesterStats);
            gpa = itemView.findViewById(R.id.tvSemesterGpa);
            grade = itemView.findViewById(R.id.tvGradeLabel);
        }
    }
}