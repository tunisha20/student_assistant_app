package com.example.studentassistantappv1.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.studentassistantappv1.R;

public class AddCourseActivity extends AppCompatActivity {
    private EditText etCourseName;
    private Spinner spinnerCredits, spinnerGrade;
    private Button btnSave, btnCancel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_course);

        // ১. ভিউ ইনিশিয়ালাইজেশন
        etCourseName = findViewById(R.id.etCourseName);
        spinnerCredits = findViewById(R.id.spinnerCredits);
        spinnerGrade = findViewById(R.id.spinnerGrade);
        btnSave = findViewById(R.id.btnSaveCourse);
        btnCancel = findViewById(R.id.btnCancel);

        // ২. স্পিনার সেটআপ
        setupSpinners();

        // ৩. সেভ বাটন লজিক
        btnSave.setOnClickListener(v -> {
            String name = etCourseName.getText().toString().trim();
            String credits = spinnerCredits.getSelectedItem().toString();
            String grade = spinnerGrade.getSelectedItem().toString();

            if (!name.isEmpty()) {
                Intent resultIntent = new Intent();
                resultIntent.putExtra("name", name);
                resultIntent.putExtra("credits", credits);
                resultIntent.putExtra("grade", grade);
                setResult(RESULT_OK, resultIntent);
                finish();
            } else {
                Toast.makeText(this, "Please enter course name", Toast.LENGTH_SHORT).show();
            }
        });

        // ৪. ক্যান্সেল বাটন লজিক
        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> finish());
        }
    }

    private void setupSpinners() {
        // ক্রেডিটের জন্য ডাটা
        String[] creditList = {"1.0", "2.0", "3.0", "4.0"};

        // কাস্টম spinner_item ব্যবহার করে কালার ফিক্স
        ArrayAdapter<String> creditAdapter = new ArrayAdapter<>(this,
                R.layout.spinner_item, creditList);
        creditAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCredits.setAdapter(creditAdapter);

        // গ্রেডের জন্য ডাটা
        String[] grades = {"A+", "A", "A-", "B+", "B", "B-", "C+", "C", "D", "F"};

        ArrayAdapter<String> gradeAdapter = new ArrayAdapter<>(this,
                R.layout.spinner_item, grades);
        gradeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerGrade.setAdapter(gradeAdapter);
    }
}