package com.example.omrscanner;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

public class ResultActivity extends AppCompatActivity {

    private OMRResult omrResult;
    private ResultStorage resultStorage;
    private boolean isSaved = false;

    private TextView tvResExamName;
    private TextView tvResRollNumber;
    private TextView tvResDate;
    private TextView tvResFinalScore;
    private TextView tvResPercentage;
    private TextView tvResCorrect;
    private TextView tvResIncorrect;
    private TextView tvResUnanswered;

    private LinearLayout llAnswersTableContainer;
    private LinearLayout llDebugImageContainer;
    private LinearLayout llQuestionRows;
    private ImageView ivDebugVisual;

    private Button btnToggleViewMode;
    private Button btnReturnHome;
    private Button btnSaveResult;
    private Button btnScanNextSheet;

    private boolean isDebugModeVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        resultStorage = new ResultStorage(this);
        omrResult = ScanSession.getCurrentResult();

        if (omrResult == null) {
            Toast.makeText(this, "No result data found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        populateData();
        setupListeners();
    }

    private void initViews() {
        tvResExamName = findViewById(R.id.tvResExamName);
        tvResRollNumber = findViewById(R.id.tvResRollNumber);
        tvResDate = findViewById(R.id.tvResDate);
        tvResFinalScore = findViewById(R.id.tvResFinalScore);
        tvResPercentage = findViewById(R.id.tvResPercentage);
        tvResCorrect = findViewById(R.id.tvResCorrect);
        tvResIncorrect = findViewById(R.id.tvResIncorrect);
        tvResUnanswered = findViewById(R.id.tvResUnanswered);

        llAnswersTableContainer = findViewById(R.id.llAnswersTableContainer);
        llDebugImageContainer = findViewById(R.id.llDebugImageContainer);
        llQuestionRows = findViewById(R.id.llQuestionRows);
        ivDebugVisual = findViewById(R.id.ivDebugVisual);

        btnToggleViewMode = findViewById(R.id.btnToggleViewMode);
        btnReturnHome = findViewById(R.id.btnReturnHome);
        btnSaveResult = findViewById(R.id.btnSaveResult);
        btnScanNextSheet = findViewById(R.id.btnScanNextSheet);
    }

    private void populateData() {
        tvResExamName.setText(omrResult.getExamName());
        tvResRollNumber.setText(omrResult.getRollNumber());
        tvResDate.setText(omrResult.getTimestamp());

        tvResFinalScore.setText(String.format(Locale.getDefault(), "%.2f / %.1f",
                omrResult.getFinalScore(), omrResult.getMaxScore()));
        tvResPercentage.setText(String.format(Locale.getDefault(), "%.1f%%", omrResult.getPercentage()));

        tvResCorrect.setText(String.valueOf(omrResult.getTotalCorrect()));
        tvResIncorrect.setText(String.valueOf(omrResult.getTotalIncorrect()));
        tvResUnanswered.setText(String.valueOf(omrResult.getTotalUnanswered()));

        // Populate 15-question breakdown rows
        char[] detected = omrResult.getDetectedAnswers();
        char[] correct = omrResult.getCorrectAnswers();
        OMRResult.Status[] statuses = omrResult.getQuestionStatuses();

        llQuestionRows.removeAllViews();
        for (int i = 0; i < OMRConfig.TOTAL_QUESTIONS; i++) {
            // Group dividers
            if (i == 0) addGroupHeader("── Group 1 (Questions 1 - 5) ──");
            else if (i == 5) addGroupHeader("── Group 2 (Questions 6 - 10) ──");
            else if (i == 10) addGroupHeader("── Group 3 (Questions 11 - 15) ──");

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(8, 8, 8, 8);
            row.setBackgroundColor((i % 2 == 0) ? 0xFF1A1A1A : 0xFF141414);
            row.setGravity(Gravity.CENTER_VERTICAL);

            // Question number
            TextView tvQ = new TextView(this);
            tvQ.setText(String.format(Locale.getDefault(), "Q%02d", i + 1));
            tvQ.setTextColor(0xFFFFFFFF);
            tvQ.setTextSize(12f);
            tvQ.setWidth(130);
            row.addView(tvQ);

            // Detected answer
            char dAns = (detected != null && i < detected.length) ? detected[i] : '?';
            TextView tvD = new TextView(this);
            tvD.setText(String.valueOf(dAns));
            tvD.setTextColor(0xFFFFFFFF);
            tvD.setTextSize(13f);
            tvD.setTypeface(null, android.graphics.Typeface.BOLD);
            tvD.setWidth(150);
            tvD.setGravity(Gravity.CENTER);
            row.addView(tvD);

            // Key answer
            char kAns = (correct != null && i < correct.length) ? correct[i] : '?';
            TextView tvK = new TextView(this);
            tvK.setText(String.valueOf(kAns));
            tvK.setTextColor(0xFFB0BEC5);
            tvK.setTextSize(13f);
            tvK.setWidth(130);
            tvK.setGravity(Gravity.CENTER);
            row.addView(tvK);

            // Status badge
            OMRResult.Status status = (statuses != null && i < statuses.length) ? statuses[i] : OMRResult.Status.UNANSWERED;
            TextView tvStatus = new TextView(this);
            tvStatus.setTextSize(11f);
            tvStatus.setTypeface(null, android.graphics.Typeface.BOLD);
            tvStatus.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
            tvStatus.setLayoutParams(lp);

            if (status == OMRResult.Status.CORRECT) {
                tvStatus.setText("✓ CORRECT");
                tvStatus.setTextColor(0xFF00E676);
            } else if (status == OMRResult.Status.INCORRECT) {
                tvStatus.setText("✗ WRONG");
                tvStatus.setTextColor(0xFFFF5252);
            } else if (status == OMRResult.Status.MULTIPLE) {
                tvStatus.setText("⚠ MULTIPLE");
                tvStatus.setTextColor(0xFFFFD700);
            } else {
                tvStatus.setText("○ BLANK");
                tvStatus.setTextColor(0xFF757575);
            }
            row.addView(tvStatus);

            llQuestionRows.addView(row);
        }

        // Set visual debug bitmap if available
        Bitmap debugBmp = ScanSession.getCurrentDebugBitmap();
        if (debugBmp != null) {
            ivDebugVisual.setImageBitmap(debugBmp);
        } else {
            btnToggleViewMode.setVisibility(View.GONE);
        }
    }

    private void addGroupHeader(String title) {
        TextView tv = new TextView(this);
        tv.setText(title);
        tv.setTextColor(0xFF00E676);
        tv.setTextSize(10f);
        tv.setTypeface(null, android.graphics.Typeface.BOLD);
        tv.setPadding(8, 8, 8, 2);
        llQuestionRows.addView(tv);
    }

    private void setupListeners() {
        // Toggle Debug mode view
        btnToggleViewMode.setOnClickListener(v -> {
            isDebugModeVisible = !isDebugModeVisible;
            if (isDebugModeVisible) {
                llAnswersTableContainer.setVisibility(View.GONE);
                llDebugImageContainer.setVisibility(View.VISIBLE);
                btnToggleViewMode.setText("View Table");
            } else {
                llAnswersTableContainer.setVisibility(View.VISIBLE);
                llDebugImageContainer.setVisibility(View.GONE);
                btnToggleViewMode.setText("View Debug Map");
            }
        });

        // Save Result locally
        btnSaveResult.setOnClickListener(v -> {
            if (!isSaved) {
                resultStorage.saveResult(omrResult);
                isSaved = true;
                btnSaveResult.setText("✓ Saved");
                btnSaveResult.setEnabled(false);
                btnSaveResult.setBackgroundColor(0xFF2E7D32);
                Toast.makeText(this, "Result saved for Roll " + omrResult.getRollNumber(), Toast.LENGTH_SHORT).show();
            }
        });

        // Scan Another Sheet (Consecutive scanning without restart)
        btnScanNextSheet.setOnClickListener(v -> {
            // Auto save if user didn't tap save explicitly
            if (!isSaved) {
                resultStorage.saveResult(omrResult);
            }
            // Return to live camera preview in MainActivity
            finish();
        });

        // Return Home
        btnReturnHome.setOnClickListener(v -> {
            if (!isSaved) {
                resultStorage.saveResult(omrResult);
            }
            Intent intent = new Intent(this, HomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });
    }
}
