package com.example.studentassistantappv1.adapters;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.studentassistantappv1.R;
import com.example.studentassistantappv1.models.CourseEntry;
import java.util.List;

public class CourseAdapter extends RecyclerView.Adapter<CourseAdapter.ViewHolder> {
    private List<CourseEntry> courseList;
    private String[] gradeLabels;

    public CourseAdapter(List<CourseEntry> courseList, String[] gradeLabels) {
        this.courseList = courseList;
        this.gradeLabels = gradeLabels;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_course_entry, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CourseEntry course = courseList.get(position);

        // গ্রেড স্পিনার সেটআপ
        ArrayAdapter<String> adapter = new ArrayAdapter<>(holder.itemView.getContext(), R.layout.spinner_item, gradeLabels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        holder.spinner.setAdapter(adapter);
        holder.spinner.setSelection(course.gradeIndex);

        // ক্রেডিট ইনপুট
        holder.etCredit.setText(course.credits > 0 ? String.valueOf(course.credits) : "");

        // ডাটা আপডেট লজিক
        holder.etCredit.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                try { course.credits = Double.parseDouble(s.toString()); } catch (Exception e) { course.credits = 0; }
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        holder.spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) { course.gradeIndex = position; }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        holder.btnRemove.setOnClickListener(v -> {
            courseList.remove(position);
            notifyDataSetChanged();
        });
    }

    @Override public int getItemCount() { return courseList.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        EditText etCredit;
        Spinner spinner;
        ImageView btnRemove;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            etCredit = itemView.findViewById(R.id.et_credit);
            spinner = itemView.findViewById(R.id.spinner_grade);
            btnRemove = itemView.findViewById(R.id.btn_remove);
        }
    }
}