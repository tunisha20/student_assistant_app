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

public class SemesterAdapter extends RecyclerView.Adapter<SemesterAdapter.ViewHolder> {
    private List<SemesterRecord> records;

    public SemesterAdapter(List<SemesterRecord> records) {
        this.records = records;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_semester_record, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SemesterRecord record = records.get(position);
        holder.tvName.setText(record.getName());
        holder.tvDetails.setText(record.getDetails());
        holder.tvGpa.setText(record.getGpa());
        holder.tvGrade.setText(record.getGradeLabel());
    }

    @Override
    public int getItemCount() { return records.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvDetails, tvGpa, tvGrade;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvSemesterName);
            tvDetails = itemView.findViewById(R.id.tvSemesterDetails);
            tvGpa = itemView.findViewById(R.id.tvSemesterGPA);
            tvGrade = itemView.findViewById(R.id.tvGradeLabel);
        }
    }

}