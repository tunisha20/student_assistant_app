package com.example.studentassistantappv1.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.studentassistantappv1.R;
import com.example.studentassistantappv1.adapters.HistoryAdapter;
import com.example.studentassistantappv1.models.SemesterRecord;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.ArrayList;
import java.util.List;

public class HistoryFragment extends Fragment {

    private LineChart historyChart;
    private RecyclerView recyclerHistory;
    private TextView tvOverallCgpa, tvImprovementMsg, tvCgpaDiff;
    private List<SemesterRecord> recordList = new ArrayList<>();
    private static final String PREF_NAME = "UserProfilePrefs";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history, container, false);

        // ১. আইডি লিঙ্কিং
        historyChart = view.findViewById(R.id.historyChart);
        recyclerHistory = view.findViewById(R.id.recyclerSemesterHistory);
        tvOverallCgpa = view.findViewById(R.id.tvOverallCgpa);
        tvImprovementMsg = view.findViewById(R.id.tvImprovementMsg);
        tvCgpaDiff = view.findViewById(R.id.tvCgpaDiff); // পাশের ছোট টেক্সট আইডি

        historyChart.setNoDataText("Please calculate a result first to see the graph!");
        historyChart.setNoDataTextColor(Color.GRAY);

        // ২. লোড ডাটা
        setupChart();
        setupRecyclerView();
        loadOverallCgpa();

        return view;
    }

    private void setupChart() {
        SharedPreferences prefs = requireActivity().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String historyStr = prefs.getString("cgpa_history_list", "");

        if (historyStr.isEmpty()) {
            historyChart.clear();
            historyChart.invalidate();
            return;
        }

        String[] historyArray = historyStr.split(",");
        List<Entry> entries = new ArrayList<>();

        for (int i = 0; i < historyArray.length; i++) {
            try {
                float val = Float.parseFloat(historyArray[i]);
                entries.add(new Entry(i, val));
            } catch (Exception ignored) {}
        }

        if (entries.isEmpty()) return;

        LineDataSet dataSet = new LineDataSet(entries, "CGPA Progress");
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setColor(Color.parseColor("#2962FF"));
        dataSet.setCircleColor(Color.parseColor("#2962FF"));
        dataSet.setLineWidth(3f);
        dataSet.setCircleRadius(5f);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(Color.parseColor("#E3F2FD"));
        dataSet.setDrawValues(true);
        dataSet.setValueTextColor(Color.parseColor("#1E293B"));
        dataSet.setValueTextSize(10f);

        LineData lineData = new LineData(dataSet);
        historyChart.setData(lineData);

        XAxis xAxis = historyChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setLabelCount(entries.size());

        historyChart.getAxisRight().setEnabled(false);
        historyChart.getDescription().setEnabled(false);
        historyChart.getLegend().setEnabled(false);
        historyChart.animateX(800);
        historyChart.invalidate();
    }

    private void setupRecyclerView() {
        recyclerHistory.setHasFixedSize(true);
        recyclerHistory.setNestedScrollingEnabled(false);
        recordList.clear();

        SharedPreferences prefs = requireActivity().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String fullHistory = prefs.getString("full_semester_history", "");

        if (!fullHistory.isEmpty()) {
            String[] semesters = fullHistory.split(";");
            for (String s : semesters) {
                String[] data = s.split("\\|");
                if (data.length == 4) {
                    try {
                        recordList.add(new SemesterRecord(
                                data[0],
                                Integer.parseInt(data[1]),
                                Integer.parseInt(data[2]),
                                Double.parseDouble(data[3]),
                                "COMPLETED"
                        ));
                    } catch (Exception e) { e.printStackTrace(); }
                }
            }
        }

        recyclerHistory.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerHistory.setAdapter(new HistoryAdapter(recordList));
    }

    private void loadOverallCgpa() {
        SharedPreferences prefs = requireActivity().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String currentCgpa = prefs.getString("user_cgpa", "0.00");
        tvOverallCgpa.setText(currentCgpa);

        // ৩. ডাইনামিক ডিফারেন্স এবং মেসেজ কার্ড আপডেট
        updateImprovementCard();
    }

    private void updateImprovementCard() {
        if (tvImprovementMsg == null) return;

        SharedPreferences prefs = requireActivity().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String historyStr = prefs.getString("cgpa_history_list", "");

        if (historyStr.isEmpty()) return;

        String[] historyArray = historyStr.split(",");

        // বর্তমান সিজিপিএ এবং আগের সিজিপিএ তুলনা
        try {
            double current = Double.parseDouble(historyArray[historyArray.length - 1]);

            if (historyArray.length >= 2) {
                double previous = Double.parseDouble(historyArray[historyArray.length - 2]);
                double diff = current - previous;
                double percentage = (diff / previous) * 100;

                // ছোট টেক্সট (+0.12) আপডেট
                if (tvCgpaDiff != null) {
                    String symbol = (diff >= 0) ? "↗+" : "↘";
                    tvCgpaDiff.setText(String.format("%s%.2f", symbol, Math.abs(diff)));
                    tvCgpaDiff.setTextColor((diff >= 0) ? Color.parseColor("#10B981") : Color.RED);
                }

                // মেইন কার্ডের মেসেজ আপডেট
                String message;
                if (diff > 0) {
                    message = String.format("Your CGPA has improved by %.1f%% since last semester. Keep up the excellent work!", percentage);
                } else if (diff < 0) {
                    message = String.format("Your CGPA decreased by %.1f%%. Focus more on the next one!", Math.abs(percentage));
                } else {
                    message = "Your CGPA is steady. Aim higher in the next semester!";
                }
                tvImprovementMsg.setText(message);
            } else {
                tvImprovementMsg.setText("Keep calculating to track your progress!");
                if (tvCgpaDiff != null) tvCgpaDiff.setText("");
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    @Override
    public void onResume() {
        super.onResume();
        setupChart();
        setupRecyclerView();
        loadOverallCgpa();
    }
}