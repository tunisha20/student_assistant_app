package com.example.studentassistantappv1.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.studentassistantappv1.R;
import com.example.studentassistantappv1.data.SupabaseApi;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class SyllabusFragment extends Fragment {

    private RecyclerView recyclerSyllabus;
    private FloatingActionButton fabAddSubject;
    private SyllabusAdapter adapter;
    private List<Subject> subjectList = new ArrayList<>();
    private String selectedPdfUri = "";
    private TextView tvPdfStatusGlobal;
    private SupabaseApi supabaseApi;

    private final String PRIMARY_BLUE = "#2563EB";

    private final ActivityResultLauncher<String> pdfPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    try {
                        requireContext().getContentResolver().takePersistableUriPermission(
                                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        selectedPdfUri = uri.toString();
                        if (tvPdfStatusGlobal != null) {
                            tvPdfStatusGlobal.setText("PDF Attached ✅");
                            tvPdfStatusGlobal.setTextColor(Color.parseColor(PRIMARY_BLUE));
                        }
                    } catch (Exception e) { e.printStackTrace(); }
                }
            }
    );

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_syllabus, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        recyclerSyllabus = view.findViewById(R.id.recyclerSyllabus);
        fabAddSubject = view.findViewById(R.id.fabAddSubject);
        initRetrofit();
        setupRecyclerView();
        fetchSyllabusFromSupabase();
        fabAddSubject.setOnClickListener(v -> showAddSubjectDialog());
    }

    private void initRetrofit() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://kjanqxiaynuewhqbpdiw.supabase.co/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        supabaseApi = retrofit.create(SupabaseApi.class);
    }

    private void setupRecyclerView() {
        recyclerSyllabus.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new SyllabusAdapter(subjectList);
        recyclerSyllabus.setAdapter(adapter);
    }

    private void fetchSyllabusFromSupabase() {
        SharedPreferences prefs = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        String userId = prefs.getString("user_id", "");
        String authToken = "Bearer " + prefs.getString("auth_token", "");

        if (userId.isEmpty()) return;

        supabaseApi.getSyllabus(SupabaseApi.apiKey, authToken, "*", "eq." + userId)
                .enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                        if (response.isSuccessful() && response.body() != null && isAdded()) {
                            try {
                                String json = response.body().string();
                                List<Subject> cloudSyllabus = new Gson().fromJson(json, new TypeToken<List<Subject>>(){}.getType());
                                if (cloudSyllabus != null) {
                                    subjectList.clear();
                                    subjectList.addAll(cloudSyllabus);
                                    adapter.notifyDataSetChanged();
                                }
                            } catch (Exception e) { e.printStackTrace(); }
                        }
                    }
                    @Override public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {}
                });
    }

    private void saveSyllabusToCloud(String name, List<Topic> topicList, String uri) {
        SharedPreferences prefs = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        String userId = prefs.getString("user_id", "");
        String authToken = "Bearer " + prefs.getString("auth_token", "");

        Map<String, Object> body = new HashMap<>();
        body.put("user_id", userId);
        body.put("subject_name", name);
        body.put("pdf_uri", uri);
        body.put("topics_json", topicList);

        supabaseApi.insertSyllabus(SupabaseApi.apiKey, authToken, "application/json", "return=representation", body)
                .enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                        if (response.isSuccessful() && isAdded()) {
                            Toast.makeText(getContext(), "Syllabus Synced! ☁️", Toast.LENGTH_SHORT).show();
                            fetchSyllabusFromSupabase();
                        }
                    }
                    @Override public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {}
                });
    }

    // ✅ টপিক কমপ্লিট বা স্ট্যাটাস চেঞ্জ হলে সুপাবেসে রিয়েল-টাইম আপডেট করার মেথড
    private void updateSyllabusTopicsInCloud(long subjectId, List<Topic> updatedTopics) {
        SharedPreferences prefs = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        String authToken = "Bearer " + prefs.getString("auth_token", "");

        Map<String, Object> updates = new HashMap<>();
        updates.put("topics_json", updatedTopics); // সুপাবেসের JSON কলামে পুরো অ্যারে সেভ হবে

        // updateSyllabus এপিআই কল করা হচ্ছে আইডি ফিল্টার দিয়ে
        supabaseApi.updateSyllabus(SupabaseApi.apiKey, authToken, "application/json", "eq." + subjectId, updates)
                .enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                        if (response.isSuccessful()) {
                            Log.d("SupabaseSyllabus", "Progress Synced to Cloud!");
                        } else {
                            Log.e("SupabaseSyllabus", "Sync Failed. Code: " + response.code());
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                        Log.e("SupabaseSyllabus", "Network Error: " + t.getMessage());
                    }
                });
    }

    private void showAddSubjectDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_subject, null);
        dialog.setContentView(view);

        EditText etSubjectName = view.findViewById(R.id.etSubjectName);
        LinearLayout layoutTopicList = view.findViewById(R.id.layoutTopicList);
        EditText etTopicInput = view.findViewById(R.id.etTopicInput);
        View btnAddTopic = view.findViewById(R.id.btnAddTopic);
        Button btnSaveSubject = view.findViewById(R.id.btnSaveSubject);
        Button btnAttachPdf = view.findViewById(R.id.btnAttachPdf);
        tvPdfStatusGlobal = view.findViewById(R.id.tvPdfStatus);

        selectedPdfUri = "";
        List<Topic> tempTopics = new ArrayList<>();

        btnAttachPdf.setOnClickListener(v -> pdfPickerLauncher.launch("application/pdf"));

        btnAddTopic.setOnClickListener(v -> {
            String topicName = etTopicInput.getText().toString().trim();
            if (!topicName.isEmpty()) {
                Topic newTopic = new Topic(topicName, "In Progress", 1);
                tempTopics.add(newTopic);
                addTopicViewToLayout(newTopic, layoutTopicList, tempTopics);
                etTopicInput.setText("");
            }
        });

        btnSaveSubject.setOnClickListener(v -> {
            String subName = etSubjectName.getText().toString().trim();
            if (!subName.isEmpty() && !tempTopics.isEmpty()) {
                saveSyllabusToCloud(subName, tempTopics, selectedPdfUri);
                dialog.dismiss();
            }
        });
        dialog.show();
    }

    private void addTopicViewToLayout(Topic topic, LinearLayout parent, List<Topic> list) {
        View topicView = LayoutInflater.from(getContext()).inflate(R.layout.item_added_topic, null);
        TextView tvName = topicView.findViewById(R.id.tvAddedTopicName);
        ImageView btnDelete = topicView.findViewById(R.id.btnDeleteTopic);
        tvName.setText(topic.name);
        btnDelete.setOnClickListener(v -> { parent.removeView(topicView); list.remove(topic); });
        parent.addView(topicView);
    }

    public static class Subject {
        @SerializedName("id") long id;
        @SerializedName("subject_name") String name;
        @SerializedName("pdf_uri") String pdfUri;
        @SerializedName("topics_json") List<Topic> topics;

        public int getProgressPercentage() {
            if (topics == null || topics.isEmpty()) return 0;
            int count = 0; for (Topic t : topics) if (t.status == 0) count++;
            return (count * 100) / topics.size();
        }
    }

    public static class Topic {
        String name, subtitle; int status;
        public Topic(String name, String subtitle, int status) { this.name = name; this.subtitle = subtitle; this.status = status; }
    }

    public class SyllabusAdapter extends RecyclerView.Adapter<SyllabusAdapter.ViewHolder> {
        private List<Subject> list;
        public SyllabusAdapter(List<Subject> list) { this.list = list; }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_syllabus_topic, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Subject subject = list.get(position);
            holder.tvSubjectName.setText(subject.name);

            if (holder.tvSubjectSubtitle != null) holder.tvSubjectSubtitle.setVisibility(View.GONE);

            int total = (subject.topics != null) ? subject.topics.size() : 0;
            int completed = 0;
            if (subject.topics != null) {
                for (Topic t : subject.topics) if (t.status == 0) completed++;
            }
            holder.tvTopicsCompletedText.setText(completed + " of " + total + " topics completed");

            int progress = subject.getProgressPercentage();
            holder.progressBarSyllabus.setProgress(progress);
            holder.tvProgressPercent.setText(progress + "%");

            holder.llTopicsContainer.removeAllViews();
            if (subject.topics != null) {
                for (Topic topic : subject.topics) {
                    View topicView = LayoutInflater.from(holder.itemView.getContext()).inflate(R.layout.item_topic_modern, null);
                    TextView tvTitle = topicView.findViewById(R.id.tvTopicTitle);
                    TextView tvSub = topicView.findViewById(R.id.tvTopicSubtitle);
                    MaterialCardView card = topicView.findViewById(R.id.cardTopicItem);
                    tvTitle.setText(topic.name);
                    tvSub.setText(topic.subtitle);

                    if (topic.status == 0) {
                        tvTitle.setPaintFlags(tvTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                    } else {
                        tvTitle.setPaintFlags(tvTitle.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
                    }

                    // ✅ কার্ড ক্লিকে লোকাল UI পরিবর্তনের পাশাপাশি ক্লাউড আপডেট ফায়ার করা হলো
                    card.setOnClickListener(v -> {
                        topic.status = (topic.status == 1) ? 0 : 1;
                        topic.subtitle = (topic.status == 0) ? "Completed" : "In Progress";

                        // লোকাল ইউআই আপডেট
                        notifyItemChanged(position);

                        // সুপাবেসে নতুন JSON ডেটা পুশ মেথড ট্রিগার
                        updateSyllabusTopicsInCloud(subject.id, subject.topics);
                    });
                    holder.llTopicsContainer.addView(topicView);
                }
            }
        }

        @Override public int getItemCount() { return list.size(); }

        public class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvSubjectName, tvProgressPercent, tvTopicsCompletedText, tvSubjectSubtitle;
            LinearProgressIndicator progressBarSyllabus;
            LinearLayout llTopicsContainer;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvSubjectName = itemView.findViewById(R.id.tvSubjectName);
                tvProgressPercent = itemView.findViewById(R.id.tvProgressPercent);
                tvTopicsCompletedText = itemView.findViewById(R.id.tvTopicsCompletedText);
                tvSubjectSubtitle = itemView.findViewById(R.id.tvSyllabusTitle);
                progressBarSyllabus = itemView.findViewById(R.id.progressBarSyllabus);
                llTopicsContainer = itemView.findViewById(R.id.llTopicsContainer);
            }
        }
    }
}