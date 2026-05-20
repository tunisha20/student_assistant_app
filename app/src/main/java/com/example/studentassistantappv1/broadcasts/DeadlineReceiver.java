package com.example.studentassistantappv1.broadcasts;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import com.example.studentassistantappv1.R;

public class DeadlineReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String taskTitle = intent.getStringExtra("task_title");
        if (taskTitle == null) taskTitle = "Your task is due!";

        Log.d("NotificationDebug", "Receiver triggered for: " + taskTitle);

        // ১. নোটিফিকেশন বিল্ডার সেটআপ
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "class_reminder")
                .setSmallIcon(R.drawable.ic_notification) // নিশ্চিত করুন এই ড্রয়েবলটি আছে
                .setContentTitle("Task Deadline Today! ⚠️")
                .setContentText("Don't forget to complete: " + taskTitle)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setDefaults(NotificationCompat.DEFAULT_ALL) // সাউন্ড এবং ভাইব্রেশন নিশ্চিত করবে
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

        // ২. Android 13+ পারমিশন চেক (সহজ পদ্ধতিতে)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED) {
                notificationManager.notify((int) System.currentTimeMillis(), builder.build());
                Log.d("NotificationDebug", "Notification sent successfully!");
            } else {
                Log.e("NotificationDebug", "Permission missing at the moment of delivery!");
            }
        } else {
            // পুরাতন ভার্সনের জন্য সরাসরি সেন্ড
            notificationManager.notify((int) System.currentTimeMillis(), builder.build());
        }
    }
}