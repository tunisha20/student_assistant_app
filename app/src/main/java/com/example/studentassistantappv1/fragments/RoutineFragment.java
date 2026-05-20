package com.example.studentassistantappv1.fragments;

import android.Manifest;
import android.app.Dialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.studentassistantappv1.R;
import com.example.studentassistantappv1.adapters.RoutineAdapter;
import com.example.studentassistantappv1.data.SupabaseApi;
import com.example.studentassistantappv1.models.Routine;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RoutineFragment extends Fragment {

    private RecyclerView rvRoutine;
    private FloatingActionButton fabAdd;
    private List<Routine> routineList = new ArrayList<>();
    private RoutineAdapter adapter;
    private SupabaseApi supabaseApi;
    private MaterialCardView[] dayCards = new MaterialCardView[6];

    private final String[] days = {"Saturday", "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday"};
    private String selectedDay = "Saturday";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_routine, container, false);

        initViews(view);
        initRetrofit();
        setupRecyclerView();
        setupDaySelection(view);
        createNotificationChannel();

        fetchRoutinesFromCloud();
        fabAdd.setOnClickListener(v -> showAddRoutineDialog());

        return view;
    }

    private void initViews(View view) {
        rvRoutine = view.findViewById(R.id.rvRoutine);
        fabAdd = view.findViewById(R.id.fabAddRoutine);
    }

    private void initRetrofit() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://kjanqxiaynuewhqbpdiw.supabase.co/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        supabaseApi = retrofit.create(SupabaseApi.class);
    }

    private void setupRecyclerView() {
        adapter = new RoutineAdapter(routineList, supabaseApi);
        rvRoutine.setLayoutManager(new LinearLayoutManager(getContext()));
        rvRoutine.setAdapter(adapter);
    }

    private void setupDaySelection(View view) {
        int[] cardIds = {R.id.cardSat, R.id.cardSun, R.id.cardMon, R.id.cardTue, R.id.cardWed, R.id.cardThu};
        for (int i = 0; i < cardIds.length; i++) {
            final int index = i;
            dayCards[i] = view.findViewById(cardIds[i]);
            dayCards[i].setOnClickListener(v -> {
                selectedDay = days[index];
                updateDayUI(index);
                fetchRoutinesFromCloud();
            });
        }
        updateDayUI(0);
    }

    private void updateDayUI(int activeIndex) {
        for (int i = 0; i < dayCards.length; i++) {
            dayCards[i].setCardBackgroundColor(i == activeIndex ? Color.parseColor("#6366F1") : Color.WHITE);
        }
    }

    private void fetchRoutinesFromCloud() {
        SharedPreferences prefs = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        String userId = prefs.getString("user_id", "");
        String authToken = "Bearer " + prefs.getString("auth_token", "");

        if (userId.isEmpty()) return;

        supabaseApi.getRoutines(SupabaseApi.apiKey, authToken, "*", "eq." + selectedDay, "eq." + userId)
                .enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                        if (response.isSuccessful() && response.body() != null && isAdded()) {
                            try {
                                String json = response.body().string();
                                List<Routine> cloudData = new Gson().fromJson(json, new TypeToken<List<Routine>>(){}.getType());
                                if (cloudData != null) {
                                    routineList.clear();
                                    routineList.addAll(cloudData);
                                    adapter.notifyDataSetChanged();
                                    checkAndAutoSilentAndNotify(routineList);
                                }
                            } catch (Exception e) { Log.e("Routine", e.getMessage()); }
                        }
                    }
                    @Override public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {}
                });
    }

    public void checkAndAutoSilentAndNotify(List<Routine> todayRoutine) {
        if (!isAdded()) return;

        NotificationManager nm = (NotificationManager) requireContext().getSystemService(Context.NOTIFICATION_SERVICE);
        AudioManager am = (AudioManager) requireContext().getSystemService(Context.AUDIO_SERVICE);
        Calendar now = Calendar.getInstance();
        int nowMin = (now.get(Calendar.HOUR_OF_DAY) * 60) + now.get(Calendar.MINUTE);

        boolean inClass = false;

        for (Routine r : todayRoutine) {
            try {
                String[] s = r.getStartTime().split(":");
                int sMin = (Integer.parseInt(s[0].trim()) * 60) + Integer.parseInt(s[1].trim());
                String[] e = r.getEndTime().split(":");
                int eMin = (Integer.parseInt(e[0].trim()) * 60) + Integer.parseInt(e[1].trim());

                if (nowMin >= sMin && nowMin <= eMin && !r.isCancelled()) {
                    inClass = true;
                }

                if (sMin - nowMin == 15) {
                    showClassNotification(r.getSubject(), r.getRoom());
                }
            } catch (Exception ignored) {}
        }

        if (nm.isNotificationPolicyAccessGranted()) {
            am.setRingerMode(inClass ? AudioManager.RINGER_MODE_SILENT : AudioManager.RINGER_MODE_NORMAL);
        }
    }

    private void showClassNotification(String subject, String room) {
        // ১. আইকন অবশ্যই সেট করতে হবে (ic_notification আপনার ড্রয়েবল ফোল্ডারে থাকতে হবে)
        NotificationCompat.Builder builder = new NotificationCompat.Builder(requireContext(), "class_reminder")
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Upcoming Class! 📖")
                .setContentText(subject + " starts in 15 mins at room " + room)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(requireContext());

        // ২. Android 13+ পারমিশন চেক
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }

        int notificationId = (int) (System.currentTimeMillis() % 10000);
        notificationManager.notify(notificationId, builder.build());
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("class_reminder", "Class Reminders", NotificationManager.IMPORTANCE_HIGH);
            NotificationManager manager = requireContext().getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    private void uploadRoutineToCloud(Routine routine) {
        SharedPreferences prefs = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        String userId = prefs.getString("user_id", "");
        String authToken = "Bearer " + prefs.getString("auth_token", "");

        Map<String, Object> body = new HashMap<>();
        body.put("user_id", userId);
        body.put("subject", routine.getSubject());
        body.put("teacher", routine.getTeacher());
        body.put("start_time", routine.getStartTime());
        body.put("end_time", routine.getEndTime());
        body.put("room", routine.getRoom());
        body.put("class_type", routine.getType());
        body.put("day_of_week", selectedDay);
        body.put("is_cancelled", false);

        supabaseApi.insertRoutine(SupabaseApi.apiKey, authToken, "application/json", "return=representation", body)
                .enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                        if (response.isSuccessful() && isAdded()) {
                            fetchRoutinesFromCloud();
                            Toast.makeText(getContext(), "Routine Saved! ☁️", Toast.LENGTH_SHORT).show();
                        }
                    }
                    @Override public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {}
                });
    }

    private void showAddRoutineDialog() {
        Dialog dialog = new Dialog(requireContext());
        dialog.setContentView(R.layout.dialog_add_routine);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        TextInputEditText etSub = dialog.findViewById(R.id.etSubject);
        TextInputEditText etRoom = dialog.findViewById(R.id.etRoom);
        Button btnStart = dialog.findViewById(R.id.btnStartTime);
        Button btnEnd = dialog.findViewById(R.id.btnEndTime);
        MaterialButton btnSave = dialog.findViewById(R.id.btnSaveRoutine);

        btnStart.setOnClickListener(v -> showTimePicker(btnStart));
        btnEnd.setOnClickListener(v -> showTimePicker(btnEnd));

        btnSave.setOnClickListener(v -> {
            if (!etSub.getText().toString().isEmpty() && !btnStart.getText().toString().contains("Time")) {
                Routine r = new Routine(etSub.getText().toString(), "", btnStart.getText().toString(), btnEnd.getText().toString(), etRoom.getText().toString(), "Class", selectedDay);
                uploadRoutineToCloud(r);
                dialog.dismiss();
            }
        });
        dialog.show();
    }

    private void showTimePicker(Button btn) {
        Calendar c = Calendar.getInstance();
        new TimePickerDialog(getContext(), (view, h, m) ->
                btn.setText(String.format(Locale.getDefault(), "%02d:%02d", h, m)),
                c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true).show();
    }
}