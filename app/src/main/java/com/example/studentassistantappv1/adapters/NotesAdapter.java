package com.example.studentassistantappv1.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.studentassistantappv1.R;
import com.example.studentassistantappv1.fragments.NotesFragment;
import java.util.List;

public class NotesAdapter extends RecyclerView.Adapter<NotesAdapter.ViewHolder> {

    private List<NotesFragment.SubjectNote> subjectList;
    private OnItemClickListener listener;

    // ১. ইন্টারফেসে সব প্রয়োজনীয় মেথড
    public interface OnItemClickListener {
        void onItemClick(NotesFragment.SubjectNote note);
        void onShareClick(NotesFragment.SubjectNote note);
        void onExportClick(NotesFragment.SubjectNote note);
        void onDeleteClick(NotesFragment.SubjectNote note);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public NotesAdapter(List<NotesFragment.SubjectNote> subjectList) {
        this.subjectList = subjectList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_note, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        NotesFragment.SubjectNote note = subjectList.get(position);

        holder.tvSubject.setText(note.getTitle());
        holder.tvTitle.setText("Lecture: " + note.getTitle());
        holder.tvDate.setText(note.getCount());

        // PDF ইন্ডিকেটর লজিক
        if (note.getPdfUri() != null && !note.getPdfUri().isEmpty()) {
            holder.chipPdfIndicator.setVisibility(View.VISIBLE);
            // যদি এটি টেক্সটভিউ বা বাটন হয় তবেই কালার সেট হবে
            if (holder.chipPdfIndicator instanceof TextView) {
                ((TextView) holder.chipPdfIndicator).setTextColor(Color.WHITE);
            }
            holder.chipPdfIndicator.setOnClickListener(v -> {
                if (listener != null) listener.onItemClick(note);
            });
        } else {
            holder.chipPdfIndicator.setVisibility(View.GONE);
        }

        // পুরো কার্ড ক্লিক
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(note);
        });

        // শেয়ার বাটন
        holder.btnShare.setOnClickListener(v -> {
            if (listener != null) listener.onShareClick(note);
        });

        // ডিলিট বাটন লজিক উইথ কনফার্মেশন ডায়ালগ
        holder.btnDelete.setOnClickListener(v -> {
            new androidx.appcompat.app.AlertDialog.Builder(v.getContext())
                    .setTitle("Delete Note")
                    .setMessage("Are you sure you want to delete this note?")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        if (listener != null) listener.onDeleteClick(note);
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }

    @Override
    public int getItemCount() { return subjectList.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvSubject, tvTitle, tvDate;
        View chipPdfIndicator;
        ImageView btnShare, btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSubject = itemView.findViewById(R.id.tv_note_subject);
            tvTitle = itemView.findViewById(R.id.tv_note_title);
            tvDate = itemView.findViewById(R.id.tv_note_date);
            btnShare = itemView.findViewById(R.id.btn_share_note);
            chipPdfIndicator = itemView.findViewById(R.id.chip_pdf_attachment);

            // আপনার item_note.xml ফাইলের আইডি অনুযায়ী সেট করা হয়েছে
            btnDelete = itemView.findViewById(R.id.btn_delete_note);
        }
    }
}