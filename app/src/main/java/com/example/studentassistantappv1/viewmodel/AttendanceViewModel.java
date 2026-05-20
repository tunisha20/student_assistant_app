package com.example.studentassistantappv1.viewmodel;


import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.studentassistantappv1.models.Attendance;
import java.util.ArrayList;
import java.util.List;

public class AttendanceViewModel extends ViewModel {

    // MutableLiveData ব্যবহার করা হয় যাতে ডাটা পরিবর্তন করা যায়
    private final MutableLiveData<List<Attendance>> attendanceList = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();

    // বাইরের ক্লাস (Fragment) শুধু LiveData পাবে, যা পরিবর্তন করা যাবে না (Read-only)
    public LiveData<List<Attendance>> getAttendanceList() {
        return attendanceList;
    }

    public LiveData<String> getError() {
        return error;
    }

    // এই মেথডটি কল করলে ডাটা লোড হবে
    public void loadAttendanceData() {
        try {
            // আপাতত ডামি ডাটা (পরে এখানে ডাটাবেস কোড বসবে)
            List<Attendance> dummyList = new ArrayList<>();
            dummyList.add(new Attendance("Bangla", 20, 18));
            dummyList.add(new Attendance("English", 15, 12));
            dummyList.add(new Attendance("Math", 30, 25));
            dummyList.add(new Attendance("Physics", 12, 5)); // Warning কালার চেক করার জন্য

            attendanceList.setValue(dummyList);
        } catch (Exception e) {
            error.setValue("Failed to load data");
        }
    }
}