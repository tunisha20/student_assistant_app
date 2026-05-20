package com.example.studentassistantappv1.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.studentassistantappv1.R;
import com.example.studentassistantappv1.activities.PdfViewerActivity;
import com.example.studentassistantappv1.adapters.NotesAdapter;
import com.example.studentassistantappv1.data.SupabaseApi;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
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

public class NotesFragment extends Fragment {

    private RecyclerView recyclerSubjects;
    private ExtendedFloatingActionButton fabAddNote;
    private EditText etSearchSubjects;
    private NotesAdapter adapter;

    private List<SubjectNote> subjectList = new ArrayList<>();
    private List<SubjectNote> filteredList = new ArrayList<>();

    private String selectedPdfUri = "";
    private TextView tvPdfStatusNote, tvScanStatusNote;
    private SupabaseApi supabaseApi;

    // PDF Picker Launcher
    private final ActivityResultLauncher<String[]> pdfPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(),
            uri -> {
                if (uri != null) {
                    try {
                        requireContext().getContentResolver().takePersistableUriPermission(
                                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        selectedPdfUri = uri.toString();
                        if (tvPdfStatusNote != null) {
                            tvPdfStatusNote.setText("PDF Attached ✅");
                            tvPdfStatusNote.setTextColor(Color.parseColor("#4CAF50"));
                        }
                    } catch (Exception e) { e.printStackTrace(); }
                }
            }
    );

    // Image Picker Launcher
    private final ActivityResultLauncher<String> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetMultipleContents(),
            uris -> { if (uris != null && !uris.isEmpty()) createPdfFromImages(uris); }
    );

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_notes, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
        initRetrofit();
        setupRecyclerView();
        fetchNotesFromSupabase();

        // রিয়েল-টাইম সার্চ লজিক
        etSearchSubjects.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filter(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        fabAddNote.setOnClickListener(v -> showCreateNoteDialog());
    }

    private void initViews(View view) {
        recyclerSubjects = view.findViewById(R.id.recyclerSubjects);
        fabAddNote = view.findViewById(R.id.fabAddNote);
        etSearchSubjects = view.findViewById(R.id.etSearchSubjects);
    }

    private void initRetrofit() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://kjanqxiaynuewhqbpdiw.supabase.co/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        supabaseApi = retrofit.create(SupabaseApi.class);
    }

    private void setupRecyclerView() {
        recyclerSubjects.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new NotesAdapter(filteredList);
        adapter.setOnItemClickListener(new NotesAdapter.OnItemClickListener() {
            @Override public void onItemClick(SubjectNote note) { openPdfFile(note); }
            @Override public void onShareClick(SubjectNote note) { shareNote(note); }
            @Override public void onExportClick(SubjectNote note) {
                Toast.makeText(getContext(), "Exporting " + note.getTitle(), Toast.LENGTH_SHORT).show();
            }
            @Override public void onDeleteClick(SubjectNote note) {
                deleteNoteFromSupabase(note);
            }
        });
        recyclerSubjects.setAdapter(adapter);
    }

    private void fetchNotesFromSupabase() {
        SharedPreferences prefs = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        String userId = prefs.getString("user_id", "");
        String authToken = "Bearer " + prefs.getString("auth_token", "");

        if (userId.isEmpty()) return;

        supabaseApi.getNotes(SupabaseApi.apiKey, authToken, "*", "eq." + userId)
                .enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                        if (response.isSuccessful() && response.body() != null && isAdded()) {
                            try {
                                String json = response.body().string();
                                List<SubjectNote> cloudNotes = new Gson().fromJson(json, new TypeToken<List<SubjectNote>>(){}.getType());
                                if (cloudNotes != null) {
                                    subjectList.clear();
                                    subjectList.addAll(cloudNotes);
                                    updateDisplayList();
                                }
                            } catch (Exception e) { e.printStackTrace(); }
                        }
                    }
                    @Override public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                        if(isAdded()) Toast.makeText(getContext(), "Sync Failed!", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveNoteToSupabase(String title, String pdfUri) {
        SharedPreferences prefs = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        String userId = prefs.getString("user_id", "");
        String authToken = "Bearer " + prefs.getString("auth_token", "");

        Map<String, Object> noteData = new HashMap<>();
        noteData.put("user_id", userId);
        noteData.put("subject_name", title);
        noteData.put("pdf_uri", pdfUri);
        noteData.put("note_count", "1 note");

        supabaseApi.insertNote(SupabaseApi.apiKey, authToken, "application/json", "return=representation", noteData)
                .enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                        if (response.isSuccessful() && isAdded()) {
                            Toast.makeText(getContext(), "Note Saved! ☁️", Toast.LENGTH_SHORT).show();
                            fetchNotesFromSupabase();
                        }
                    }
                    @Override public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {}
                });
    }

    private void deleteNoteFromSupabase(SubjectNote note) {
        SharedPreferences prefs = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        String authToken = "Bearer " + prefs.getString("auth_token", "");

        supabaseApi.deleteNote(SupabaseApi.apiKey, authToken, "eq." + note.getId())
                .enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                        if (response.isSuccessful() && isAdded()) {
                            Toast.makeText(getContext(), "Note Deleted! 🗑️", Toast.LENGTH_SHORT).show();
                            fetchNotesFromSupabase();
                        }
                    }
                    @Override public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                        if (isAdded()) Toast.makeText(getContext(), "Delete Failed!", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showCreateNoteDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_create_note, null);
        dialog.setContentView(view);

        EditText etSubject = view.findViewById(R.id.etSubjectNameNote);
        Button btnSave = view.findViewById(R.id.btnSaveNote);
        LinearLayout layoutAttach = view.findViewById(R.id.layoutAttachPdf);
        LinearLayout layoutScan = view.findViewById(R.id.layoutScanImages);
        tvPdfStatusNote = view.findViewById(R.id.tvPdfStatusNote);
        tvScanStatusNote = view.findViewById(R.id.tvScanStatusNote);

        selectedPdfUri = "";

        layoutAttach.setOnClickListener(v -> pdfPickerLauncher.launch(new String[]{"application/pdf"}));
        layoutScan.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));

        btnSave.setOnClickListener(v -> {
            String title = etSubject.getText().toString().trim();
            if (!title.isEmpty()) {
                saveNoteToSupabase(title, selectedPdfUri);
                dialog.dismiss();
            } else {
                etSubject.setError("Title required");
            }
        });
        dialog.show();
    }

    private void updateDisplayList() {
        filteredList.clear();
        filteredList.addAll(subjectList);
        adapter.notifyDataSetChanged();
    }

    private void filter(String text) {
        filteredList.clear();
        for (SubjectNote item : subjectList) {
            if (item.getTitle().toLowerCase().contains(text.toLowerCase())) {
                filteredList.add(item);
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void openPdfFile(SubjectNote note) {
        if (note.getPdfUri() != null && !note.getPdfUri().isEmpty()) {
            Intent intent = new Intent(getContext(), PdfViewerActivity.class);
            intent.putExtra("pdf_uri", note.getPdfUri());
            startActivity(intent);
        } else {
            Toast.makeText(getContext(), "No PDF attached!", Toast.LENGTH_SHORT).show();
        }
    }

    private void shareNote(SubjectNote note) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        String shareBody = "Shared via StudyBuddy: " + note.getTitle();
        if (note.getPdfUri() != null && !note.getPdfUri().isEmpty()) {
            Uri pdfUri;
            if (note.getPdfUri().startsWith("content://")) {
                pdfUri = Uri.parse(note.getPdfUri());
            } else {
                File file = new File(Uri.parse(note.getPdfUri()).getPath());
                pdfUri = FileProvider.getUriForFile(requireContext(), requireContext().getPackageName() + ".fileprovider", file);
            }
            intent.setType("application/pdf");
            intent.putExtra(Intent.EXTRA_STREAM, pdfUri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            shareBody += "\n(PDF Attached)";
        } else { intent.setType("text/plain"); }
        intent.putExtra(Intent.EXTRA_TEXT, shareBody);
        startActivity(Intent.createChooser(intent, "Share Note"));
    }

    private void createPdfFromImages(List<Uri> uris) {
        PdfDocument document = new PdfDocument();
        try {
            for (int i = 0; i < uris.size(); i++) {
                InputStream inputStream = requireContext().getContentResolver().openInputStream(uris.get(i));
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(bitmap.getWidth(), bitmap.getHeight(), i + 1).create();
                PdfDocument.Page page = document.startPage(pageInfo);
                page.getCanvas().drawBitmap(bitmap, 0, 0, null);
                document.finishPage(page);
                if (inputStream != null) inputStream.close();
            }
            File file = new File(requireContext().getExternalFilesDir(null), "Scan_" + System.currentTimeMillis() + ".pdf");
            document.writeTo(new FileOutputStream(file));
            selectedPdfUri = Uri.fromFile(file).toString();
            if (tvScanStatusNote != null) {
                tvScanStatusNote.setText("Scan Ready ✅");
                tvScanStatusNote.setTextColor(Color.parseColor("#4CAF50"));
            }
        } catch (IOException e) { e.printStackTrace(); } finally { document.close(); }
    }

    public static class SubjectNote {
        @SerializedName("id") private long id;
        @SerializedName("subject_name") private String title;
        @SerializedName("note_count") private String count;
        @SerializedName("pdf_uri") private String pdfUri;

        public SubjectNote(long id, String title, String count, String pdfUri) {
            this.id = id;
            this.title = title;
            this.count = count;
            this.pdfUri = pdfUri;
        }
        public long getId() { return id; }
        public String getTitle() { return title != null ? title : "Untitled"; }
        public String getCount() { return count != null ? count : "1 note"; }
        public String getPdfUri() { return pdfUri != null ? pdfUri : ""; }
    }
}