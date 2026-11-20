package com.example.omrscanner;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ResultHistoryActivity extends AppCompatActivity {

    private ResultStorage resultStorage;
    private ExamManager examManager;

    private EditText etSearchRoll;
    private Spinner spnExamFilter;
    private TextView tvResultsCount;
    private ListView lvResults;

    private ResultAdapter adapter;
    private final List<OMRResult> displayedResults = new ArrayList<>();
    private final List<Exam> examList = new ArrayList<>();

    private String selectedExamId = null; // null means all
    private String currentSearchQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result_history);

        resultStorage = new ResultStorage(this);
        examManager = new ExamManager(this);

        etSearchRoll = findViewById(R.id.etSearchRoll);
        spnExamFilter = findViewById(R.id.spnExamFilter);
        tvResultsCount = findViewById(R.id.tvResultsCount);
        lvResults = findViewById(R.id.lvResults);

        Button btnBack = findViewById(R.id.btnBackFromHistory);
        btnBack.setOnClickListener(v -> finish());

        Button btnExport = findViewById(R.id.btnExportCsv);
        btnExport.setOnClickListener(v -> exportCsv());

        adapter = new ResultAdapter();
        lvResults.setAdapter(adapter);

        setupFilters();
        setupSearch();

        lvResults.setOnItemClickListener((parent, view, position, id) -> {
            OMRResult item = displayedResults.get(position);
            ScanSession.setSession(item, null);
            Intent intent = new Intent(this, ResultActivity.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshResults();
    }

    private void setupFilters() {
        examList.clear();
        examList.addAll(examManager.getAllExams());

        List<String> filterOptions = new ArrayList<>();
        filterOptions.add("All Examinations (" + examList.size() + ")");
        for (Exam e : examList) {
            filterOptions.add(e.getName());
        }

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                filterOptions
        );
        spnExamFilter.setAdapter(spinnerAdapter);

        spnExamFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    selectedExamId = null;
                } else {
                    selectedExamId = examList.get(position - 1).getId();
                }
                refreshResults();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupSearch() {
        etSearchRoll.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s.toString();
                refreshResults();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void refreshResults() {
        List<OMRResult> all;
        if (selectedExamId != null) {
            all = resultStorage.getResultsByExam(selectedExamId);
        } else {
            all = resultStorage.getAllResults();
        }

        displayedResults.clear();
        if (currentSearchQuery.trim().isEmpty()) {
            displayedResults.addAll(all);
        } else {
            String q = currentSearchQuery.trim().toLowerCase(Locale.ROOT);
            for (OMRResult r : all) {
                if (r.getRollNumber() != null && r.getRollNumber().toLowerCase(Locale.ROOT).contains(q)) {
                    displayedResults.add(r);
                }
            }
        }

        tvResultsCount.setText(String.format(Locale.getDefault(), "%d sheet(s) found", displayedResults.size()));
        adapter.notifyDataSetChanged();
    }

    private void exportCsv() {
        try {
            File csvFile = resultStorage.exportToCsv(selectedExamId);
            if (csvFile == null || !csvFile.exists()) {
                Toast.makeText(this, "Failed to create CSV file", Toast.LENGTH_SHORT).show();
                return;
            }

            Uri uri = FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".fileprovider",
                    csvFile
            );

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/csv");
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "OMR Scan Results CSV Export");
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivity(Intent.createChooser(shareIntent, "Share / Export CSV"));
            Toast.makeText(this, "CSV exported: " + csvFile.getName(), Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Export error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private class ResultAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return displayedResults.size();
        }

        @Override
        public OMRResult getItem(int position) {
            return displayedResults.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(ResultHistoryActivity.this).inflate(R.layout.item_result, parent, false);
            }

            OMRResult res = getItem(position);
            TextView tvRoll = convertView.findViewById(R.id.tvItemRoll);
            TextView tvScore = convertView.findViewById(R.id.tvItemScore);
            TextView tvExam = convertView.findViewById(R.id.tvItemExam);
            TextView tvDate = convertView.findViewById(R.id.tvItemDate);
            TextView tvSummary = convertView.findViewById(R.id.tvItemSummary);

            tvRoll.setText("Roll: " + res.getRollNumber());
            tvScore.setText(String.format(Locale.getDefault(), "%.2f / %.1f (%.1f%%)",
                    res.getFinalScore(), res.getMaxScore(), res.getPercentage()));
            tvExam.setText(res.getExamName());
            tvDate.setText(res.getTimestamp());
            tvSummary.setText(String.format(Locale.getDefault(),
                    "✓ %d Correct  •  ✗ %d Wrong  •  ○ %d Blank",
                    res.getTotalCorrect(), res.getTotalIncorrect(), res.getTotalUnanswered()));

            return convertView;
        }
    }
}

