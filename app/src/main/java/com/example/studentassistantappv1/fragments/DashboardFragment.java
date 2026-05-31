package com.example.studentassistantappv1.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.studentassistantappv1.R;
import com.example.studentassistantappv1.activities.MainActivity;
import com.example.studentassistantappv1.data.SupabaseApi;
import com.example.studentassistantappv1.models.Attendance;
import com.example.studentassistantappv1.models.Task;
import com.example.studentassistantappv1.models.UserProfile;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class DashboardFragment extends Fragment {

    private TextView tvGreeting, tvGreetingLabel, tvAttendance, tvFocusTime, tvCgpaValue;
    private TextView tvSubject, tvClassTime, tvStatusBadge;
    private TextView tvDashboardTaskTitle, tvDashboardTaskSub, tvNoTasks;
    private com.google.android.material.card.MaterialCardView cardCurrentClass;
    private SharedPreferences sharedPreferences;
    private SupabaseApi supabaseApi;

    private static final String PREF_NAME = "UserPrefs";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);
        initViews(view);
        sharedPreferences = requireActivity().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        initRetrofit();
        loadLocalData();
        setupClickListeners(view);
        return view;
    }

    private void initViews(View view) {
        tvGreetingLabel = view.findViewById(R.id.tvGreetingLabel);
        tvGreeting = view.findViewById(R.id.tvGreeting);
        tvAttendance = view.findViewById(R.id.tvAttendancePercent);
        tvFocusTime = view.findViewById(R.id.tvFocusTime);
        tvCgpaValue = view.findViewById(R.id.tvCgpaValue);

        // Class Card (Purple Card)
        tvSubject = view.findViewById(R.id.tvSubject);
        tvClassTime = view.findViewById(R.id.tvClassTime);
        tvStatusBadge = view.findViewById(R.id.tvStatusBadge);
        cardCurrentClass = view.findViewById(R.id.cardCurrentClass);

        // Task Card (Bottom Card)
        tvDashboardTaskTitle = view.findViewById(R.id.tvDashboardTaskTitle);
        tvDashboardTaskSub = view.findViewById(R.id.tvDashboardTaskSub);
        tvNoTasks = view.findViewById(R.id.tvNoTasks);
    }

    private void initRetrofit() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://kjanqxiaynuewhqbpdiw.supabase.co/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        supabaseApi = retrofit.create(SupabaseApi.class);
    }

    private void fetchLiveStatsFromSupabase() {
        String userId = sharedPreferences.getString("user_id", "");
        String rawToken = sharedPreferences.getString("auth_token", "");

        if (userId.isEmpty() || rawToken.isEmpty()) return;

        String authToken = "Bearer " + rawToken;
        String apiKey = SupabaseApi.apiKey;

        fetchProfileData(apiKey, authToken, userId);
        fetchAttendanceData(apiKey, authToken, userId);
        fetchLiveRoutine(apiKey, authToken, userId);
        fetchLatestTask(apiKey, authToken, userId);
    }

    private void fetchProfileData(String apiKey, String authToken, String userId) {
        supabaseApi.getProfileData(apiKey, authToken, "*", "eq." + userId)
                .enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                        if (response.isSuccessful() && response.body() != null && isAdded()) {
                            try {
                                List<UserProfile> profiles = new Gson().fromJson(response.body().string(), new TypeToken<List<UserProfile>>(){}.getType());
                                if (profiles != null && !profiles.isEmpty()) {
                                    UserProfile user = profiles.get(0);
                                    tvCgpaValue.setText(user.getCgpa());

                                    // Supabase থেকে টোটাল ফোকাস টাইম রিড করে ড্যাশবোর্ডে সেট করা
                                    String focusTimeStr = user.getFocusTime();
                                    tvFocusTime.setText(focusTimeStr + "h");

                                    // ডাটাটি লোকালেও আপডেট করে রাখা যাতে ইন্টারনেট ছাড়াও পরবর্তীতে কাজ করে
                                    try {
                                        float cloudHours = Float.parseFloat(focusTimeStr);
                                        int cloudMinutes = (int) (cloudHours * 60);
                                        sharedPreferences.edit().putInt("total_focus_minutes", cloudMinutes).apply();
                                    } catch (Exception ignored) {}
                                }
                            } catch (Exception e) { e.printStackTrace(); }
                        }
                    }
                    @Override public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {}
                });
    }

    private void fetchAttendanceData(String apiKey, String authToken, String userId) {
        supabaseApi.getAttendanceData(apiKey, authToken, "*", "eq." + userId)
                .enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                        if (response.isSuccessful() && response.body() != null && isAdded()) {
                            try {
                                List<Attendance> list = new Gson().fromJson(response.body().string(), new TypeToken<List<Attendance>>(){}.getType());
                                int total = 0, present = 0;
                                for (Attendance a : list) {
                                    total += a.getTotalClasses();
                                    present += a.getPresentClasses();
                                }
                                updateAttendanceUI((total > 0) ? (present * 100) / total : 0);
                            } catch (Exception e) { e.printStackTrace(); }
                        }
                    }
                    @Override public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {}
                });
    }

    private void fetchLiveRoutine(String apiKey, String authToken, String userId) {
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE", Locale.ENGLISH);
        String currentDay = sdf.format(new Date());
        Calendar now = Calendar.getInstance();
        int nowMin = (now.get(Calendar.HOUR_OF_DAY) * 60) + now.get(Calendar.MINUTE);

        supabaseApi.getRoutines(apiKey, authToken, "*", "eq." + currentDay, "eq." + userId)
                .enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                        if (response.isSuccessful() && response.body() != null && isAdded()) {
                            try {
                                JsonArray routineArray = new Gson().fromJson(response.body().string(), JsonArray.class);
                                RoutineInfo session = findLiveOrNextSession(routineArray, nowMin);
                                requireActivity().runOnUiThread(() -> updateRoutineUI(session));
                            } catch (Exception e) { e.printStackTrace(); }
                        }
                    }
                    @Override public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {}
                });
    }

    private void fetchLatestTask(String apiKey, String authToken, String userId) {
        supabaseApi.getTasks(apiKey, authToken, "*", "eq." + userId, "created_at.desc")
                .enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                        if (response.isSuccessful() && response.body() != null && isAdded()) {
                            try {
                                List<Task> tasks = new Gson().fromJson(response.body().string(), new TypeToken<List<Task>>(){}.getType());
                                if (tasks != null) {
                                    Task latestPending = null;
                                    for (Task t : tasks) {
                                        if (!t.isCompleted()) { latestPending = t; break; }
                                    }
                                    Task finalTask = latestPending;
                                    requireActivity().runOnUiThread(() -> updateTaskUI(finalTask));
                                }
                            } catch (Exception e) { e.printStackTrace(); }
                        }
                    }
                    @Override public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {}
                });
    }

    private void updateRoutineUI(RoutineInfo session) {
        if (session.isLive) {
            tvStatusBadge.setText("🔴 LIVE SESSION");
            tvSubject.setText(session.subject);
            tvClassTime.setText("Ends: " + session.endTime + " | Room: " + session.room);
            cardCurrentClass.setCardBackgroundColor(Color.parseColor("#6366F1"));
        } else if (session.startsInMinutes > 0) {
            tvStatusBadge.setText("⏳ UPCOMING CLASS");
            tvSubject.setText(session.subject);
            tvClassTime.setText("Starts in " + session.startsInMinutes + "m | Room: " + session.room);
            cardCurrentClass.setCardBackgroundColor(Color.parseColor("#10B981"));
        } else {
            tvStatusBadge.setText("FREE TIME");
            tvSubject.setText("No More Classes");
            tvClassTime.setText("Relax! You're all caught up. 😊");
            cardCurrentClass.setCardBackgroundColor(Color.parseColor("#94A3B8"));
        }
    }

    private void updateTaskUI(Task task) {
        if (task != null) {
            tvNoTasks.setVisibility(View.GONE);
            tvDashboardTaskTitle.setVisibility(View.VISIBLE);
            tvDashboardTaskSub.setVisibility(View.VISIBLE);
            tvDashboardTaskTitle.setText(task.getTitle());
            tvDashboardTaskSub.setText(task.getCategory().toUpperCase() + " • UPCOMING");
        } else {
            tvNoTasks.setVisibility(View.VISIBLE);
            tvDashboardTaskTitle.setVisibility(View.GONE);
            tvDashboardTaskSub.setVisibility(View.GONE);
        }
    }

    private RoutineInfo findLiveOrNextSession(JsonArray array, int nowMin) {
        RoutineInfo next = null;
        int earliest = Integer.MAX_VALUE;
        for (int i = 0; i < array.size(); i++) {
            JsonObject o = array.get(i).getAsJsonObject();
            try {
                String sub = o.get("subject").getAsString();
                String start = o.get("start_time").getAsString();
                String end = o.get("end_time").getAsString();
                String rm = o.has("room") && !o.get("room").isJsonNull() ? o.get("room").getAsString() : "N/A";
                int sMin = (Integer.parseInt(start.split(":")[0].trim()) * 60) + Integer.parseInt(start.split(":")[1].trim());
                int eMin = (Integer.parseInt(end.split(":")[0].trim()) * 60) + Integer.parseInt(end.split(":")[1].trim());
                if (nowMin >= sMin && nowMin <= eMin) return new RoutineInfo(sub, start, end, rm, true, 0);
                if (sMin > nowMin && sMin < earliest) { earliest = sMin; next = new RoutineInfo(sub, start, end, rm, false, (sMin - nowMin)); }
            } catch (Exception ignored) {}
        }
        return next != null ? next : new RoutineInfo("", "", "", "", false, -1);
    }

    private static class RoutineInfo {
        String subject, startTime, endTime, room;
        boolean isLive; int startsInMinutes;
        RoutineInfo(String s, String st, String et, String r, boolean l, int min) {
            this.subject = s; this.startTime = st; this.endTime = et; this.room = r; this.isLive = l; this.startsInMinutes = min;
        }
    }

    private void updateAttendanceUI(int percent) {
        tvAttendance.setText(percent + "%");
        tvAttendance.setTextColor(percent >= 75 ? Color.WHITE : Color.parseColor("#EF4444"));
    }

    private void loadLocalData() {
        setDynamicGreeting();
        tvGreeting.setText(sharedPreferences.getString("userName", "Student") + "!");
        tvCgpaValue.setText("--");
        tvAttendance.setText("--%");

        // লোকাল SharedPreferences থেকে ফোকাস আওয়ার লোড করার জন্য ডেডিকেটেড মেথড কল
        fetchFocusHour();
    }

    // লোকাল ডাটা থেকে ফোকাস মিনিটকে ঘন্টায় কনভার্ট করে UI-তে সেট করার মেথড
    private void fetchFocusHour() {
        int totalFocusMinutes = sharedPreferences.getInt("total_focus_minutes", 0);
        if (totalFocusMinutes > 0) {
            double hours = totalFocusMinutes / 60.0;
            tvFocusTime.setText(String.format(Locale.getDefault(), "%.1fh", hours));
        } else {
            tvFocusTime.setText("0.0h");
        }
    }

    private void setupClickListeners(View view) {
        view.findViewById(R.id.cardAttendance).setOnClickListener(v -> openFragment(new AttendanceFragment()));
        view.findViewById(R.id.cardTasks).setOnClickListener(v -> navigateToTab(R.id.nav_tasks));
        view.findViewById(R.id.cardFocusStats).setOnClickListener(v -> openFragment(new FocusFragment()));
        view.findViewById(R.id.cardCgpa).setOnClickListener(v -> openFragment(new ResultFragment()));
        cardCurrentClass.setOnClickListener(v -> openFragment(new RoutineFragment()));
    }

    private void openFragment(Fragment fragment) {
        if (isAdded()) {
            getParentFragmentManager().beginTransaction()
                    .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(null)
                    .commit();
        }
    }

    private void navigateToTab(int menuId) {
        if (getActivity() instanceof MainActivity) {
            BottomNavigationView bottomNav = getActivity().findViewById(R.id.bottom_navigation);
            if (bottomNav != null) {
                bottomNav.setSelectedItemId(menuId);
            }
        }
    }

    @Override public void onResume() {
        super.onResume();
        // ব্যাক করে ফিরে আসার সাথে সাথেই ডাটা আপডেট করার জন্য ওয়ান-টাইম লোকাল রিলোড
        loadLocalData();
        fetchLiveStatsFromSupabase();
    }

    private void setDynamicGreeting() {
        int hr = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        tvGreetingLabel.setText(hr < 12 ? "Good Morning," : hr < 16 ? "Good Afternoon," : hr < 21 ? "Good Evening," : "Good Night,");
    }
}