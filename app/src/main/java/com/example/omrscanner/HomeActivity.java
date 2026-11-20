package com.example.omrscanner;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

public class HomeActivity extends AppCompatActivity {

    private ExamManager examManager;
    private ResultStorage resultStorage;

    private TextView tvActiveExamName;
    private TextView tvActiveExamDetails;
    private TextView tvAnswerKeyPreview;
    private TextView tvChangeExam;

    private TextView tvTotalExams;
    private TextView tvTotalScans;

    private Button btnStartScanning;
    private Button btnManageExams;
    private Button btnViewHistory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        examManager = new ExamManager(this);
        resultStorage = new ResultStorage(this);

        tvActiveExamName = findViewById(R.id.tvActiveExamName);
        tvActiveExamDetails = findViewById(R.id.tvActiveExamDetails);
        tvAnswerKeyPreview = findViewById(R.id.tvAnswerKeyPreview);
        tvChangeExam = findViewById(R.id.tvChangeExam);

        tvTotalExams = findViewById(R.id.tvTotalExams);
        tvTotalScans = findViewById(R.id.tvTotalScans);

        androidx.cardview.widget.CardView cardActiveExam = findViewById(R.id.cardActiveExam);
        if (cardActiveExam != null) {
            cardActiveExam.setOnClickListener(v -> showSelectExamDialog());
        }

        btnStartScanning = findViewById(R.id.btnStartScanning);
        btnManageExams = findViewById(R.id.btnManageExams);
        btnViewHistory = findViewById(R.id.btnViewHistory);

        // Scan button
        btnStartScanning.setOnClickListener(v -> {
            Exam activeExam = examManager.getActiveExam();
            if (activeExam == null) {
                Toast.makeText(this, "Please create or select an examination first", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra("exam_id", activeExam.getId());
            startActivity(intent);
        });

        // Manage exams
        btnManageExams.setOnClickListener(v -> {
            Intent intent = new Intent(this, ExamActivity.class);
            startActivity(intent);
        });

        // View scan history
        btnViewHistory.setOnClickListener(v -> {
            Intent intent = new Intent(this, ResultHistoryActivity.class);
            startActivity(intent);
        });

        // Change active exam dialog
        tvChangeExam.setOnClickListener(v -> showSelectExamDialog());
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshUI();
    }

    private void refreshUI() {
        Exam activeExam = examManager.getActiveExam();
        if (activeExam != null) {
            tvActiveExamName.setText(activeExam.getName());
            tvActiveExamDetails.setText(String.format("15 Questions • +%.1f / -%.2f marks",
                    activeExam.getMarksPerCorrect(), activeExam.getNegativeMarks()));

            StringBuilder sb = new StringBuilder("Key: ");
            char[] key = activeExam.getAnswerKey();
            if (key != null) {
                for (int i = 0; i < key.length; i++) {
                    sb.append(key[i]);
                    if (i < key.length - 1) sb.append(" ");
                }
            }
            tvAnswerKeyPreview.setText(sb.toString());
        } else {
            tvActiveExamName.setText("No Exam Created");
            tvActiveExamDetails.setText("Tap Change to create or select an exam");
            tvAnswerKeyPreview.setText("");
        }

        List<Exam> exams = examManager.getAllExams();
        tvTotalExams.setText(String.valueOf(exams.size()));

        List<OMRResult> results = resultStorage.getAllResults();
        tvTotalScans.setText(String.valueOf(results.size()));
    }

    private void showSelectExamDialog() {
        List<Exam> exams = examManager.getAllExams();
        if (exams.isEmpty()) {
            Toast.makeText(this, "No exams available. Tap Exams & Keys to create one.", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] examNames = new String[exams.size()];
        int selectedIndex = 0;
        Exam currentActive = examManager.getActiveExam();
        for (int i = 0; i < exams.size(); i++) {
            examNames[i] = exams.get(i).getName();
            if (currentActive != null && currentActive.getId().equals(exams.get(i).getId())) {
                selectedIndex = i;
            }
        }

        new AlertDialog.Builder(this)
                .setTitle("Select Active Examination")
                .setSingleChoiceItems(examNames, selectedIndex, (dialog, which) -> {
                    examManager.setActiveExamId(exams.get(which).getId());
                    refreshUI();
                    dialog.dismiss();
                    Toast.makeText(this, "Active Exam: " + exams.get(which).getName(), Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}

