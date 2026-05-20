package com.example.studentassistantappv1.fragments;

import android.animation.ValueAnimator;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.studentassistantappv1.R;

public class StatsFragment extends Fragment {

    private View barMon, barTue, barWed, barThu, barFri, barSat, barSun;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_stats, container, false);

        // ১. বারগুলো খুঁজে বের করা
        barMon = view.findViewById(R.id.bar_mon);
        barTue = view.findViewById(R.id.bar_tue);
        barWed = view.findViewById(R.id.bar_wed);
        barThu = view.findViewById(R.id.bar_thu);
        barFri = view.findViewById(R.id.bar_fri);
        barSat = view.findViewById(R.id.bar_sat);
        barSun = view.findViewById(R.id.bar_sun);

        // ২. প্রফেশনাল স্ট্যাগার্ড অ্যানিমেশন কল করা (Height, Delay)
        // প্রতি ১০০ মিলি-সেকেন্ড পর পর একেকটি বার অ্যানিমেশন শুরু করবে
        animateBarGrowth(barMon, 180, 100);
        animateBarGrowth(barTue, 120, 200);
        animateBarGrowth(barWed, 250, 300);
        animateBarGrowth(barThu, 200, 400);
        animateBarGrowth(barFri, 150, 500);
        animateBarGrowth(barSat, 90, 600);
        animateBarGrowth(barSun, 210, 700);

        return view;
    }

    // 🔥 তোমার দেওয়া প্রফেশনাল অ্যানিমেশন মেথড
    private void animateBarGrowth(View barView, int targetHeightDp, int delay) {
        float scale = getResources().getDisplayMetrics().density;
        int targetHeightPx = (int) (targetHeightDp * scale + 0.5f);

        barView.setAlpha(0f); // শুরুতে ইনভিজিবল থাকবে

        ValueAnimator anim = ValueAnimator.ofInt(0, targetHeightPx);
        anim.setDuration(800);
        anim.setStartDelay(delay); // একটির পর একটি শুরু হওয়ার জন্য ডিলে

        // AnticipateOvershootInterpolator দিলে বারটি উপরে উঠে সামান্য বাউন্স করবে (Premium Feel)
        anim.setInterpolator(new android.view.animation.AnticipateOvershootInterpolator());

        anim.addUpdateListener(valueAnimator -> {
            int val = (Integer) valueAnimator.getAnimatedValue();
            ViewGroup.LayoutParams params = barView.getLayoutParams();
            params.height = val;
            barView.setLayoutParams(params);
            barView.setAlpha(1f); // অ্যানিমেশন শুরু হলে ভিজিবল হবে
        });

        anim.start();
    }
}