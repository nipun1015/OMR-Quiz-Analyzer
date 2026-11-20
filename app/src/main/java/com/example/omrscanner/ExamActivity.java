package com.example.omrscanner;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ExamActivity extends AppCompatActivity {

    private ExamManager examManager;
    private ListView lvExams;
    private ExamAdapter adapter;
    private final List<Exam> examList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exam);

        examManager = new ExamManager(this);
        lvExams = findViewById(R.id.lvExams);

        Button btnBack = findViewById(R.id.btnBackFromExams);
        btnBack.setOnClickListener(v -> finish());

        Button btnCreate = findViewById(R.id.btnCreateExam);
        btnCreate.setOnClickListener(v -> showEditExamDialog(null));

        adapter = new ExamAdapter();
        lvExams.setAdapter(adapter);

        refreshList();
    }

    private void refreshList() {
        examList.clear();
        examList.addAll(examManager.getAllExams());
        adapter.notifyDataSetChanged();
    }

    private void showEditExamDialog(Exam existingExam) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_edit_exam);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        TextView tvTitle = dialog.findViewById(R.id.tvDialogTitle);
        EditText etName = dialog.findViewById(R.id.etExamName);
        EditText etCorrect = dialog.findViewById(R.id.etMarksCorrect);
        EditText etNegative = dialog.findViewById(R.id.etNegativeMarks);
        LinearLayout container = dialog.findViewById(R.id.llQuestionsContainer);
        Button btnCancel = dialog.findViewById(R.id.btnCancelEdit);
        Button btnSave = dialog.findViewById(R.id.btnSaveExam);

        boolean isNew = (existingExam == null);
        Exam targetExam = isNew ? new Exam() : existingExam;

        tvTitle.setText(isNew ? "Create New Examination" : "Edit Answer Key");
        etName.setText(isNew ? "Examination " + (examList.size() + 1) : targetExam.getName());
        etCorrect.setText(String.valueOf(targetExam.getMarksPerCorrect()));
        etNegative.setText(String.valueOf(targetExam.getNegativeMarks()));

        // Dynamically build 15 question RadioGroups
        RadioGroup[] radioGroups = new RadioGroup[OMRConfig.TOTAL_QUESTIONS];
        char[] currentKey = targetExam.getAnswerKey();

        for (int q = 0; q < OMRConfig.TOTAL_QUESTIONS; q++) {
            // Group divider headers
            if (q == 0) {
                addSectionHeader(container, "── Group 1 (Questions 1 - 5) ──");
            } else if (q == 5) {
                addSectionHeader(container, "── Group 2 (Questions 6 - 10) ──");
            } else if (q == 10) {
                addSectionHeader(container, "── Group 3 (Questions 11 - 15) ──");
            }

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(4, 6, 4, 6);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);

            TextView qNum = new TextView(this);
            qNum.setText(String.format(Locale.getDefault(), "Q%02d:", q + 1));
            qNum.setTextColor(0xFFFFFFFF);
            qNum.setTextSize(13f);
            qNum.setTypeface(null, android.graphics.Typeface.BOLD);
            qNum.setWidth(100);
            row.addView(qNum);

            RadioGroup rg = new RadioGroup(this);
            rg.setOrientation(RadioGroup.HORIZONTAL);
            char[] options = {'A', 'B', 'C', 'D'};
            char selected = (currentKey != null && q < currentKey.length) ? currentKey[q] : 'A';

            for (int opt = 0; opt < 4; opt++) {
                RadioButton rb = new RadioButton(this);
                rb.setId(View.generateViewId());
                rb.setText(String.valueOf(options[opt]));
                rb.setTextColor(0xFFE0E0E0);
                rb.setTextSize(12f);
                rb.setPadding(2, 0, 16, 0);
                if (options[opt] == selected) {
                    rb.setChecked(true);
                }
                rg.addView(rb);
            }

            row.addView(rg);
            container.addView(row);
            radioGroups[q] = rg;
        }

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            if (name.isEmpty()) {
                etName.setError("Name is required");
                return;
            }

            double correctMarks = 1.0;
            double negMarks = 0.0;
            try {
                correctMarks = Double.parseDouble(etCorrect.getText().toString().trim());
                negMarks = Double.parseDouble(etNegative.getText().toString().trim());
            } catch (Exception ignored) {}

            char[] newKey = new char[OMRConfig.TOTAL_QUESTIONS];
            char[] options = {'A', 'B', 'C', 'D'};
            for (int q = 0; q < OMRConfig.TOTAL_QUESTIONS; q++) {
                RadioGroup rg = radioGroups[q];
                int checkedId = rg.getCheckedRadioButtonId();
                char chosen = 'A';
                for (int opt = 0; opt < 4; opt++) {
                    if (rg.getChildAt(opt).getId() == checkedId) {
                        chosen = options[opt];
                        break;
                    }
                }
                newKey[q] = chosen;
            }

            targetExam.setName(name);
            targetExam.setMarksPerCorrect(correctMarks);
            targetExam.setNegativeMarks(negMarks);
            targetExam.setAnswerKey(newKey);
            targetExam.setDate(new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date()));

            examManager.saveExam(targetExam);
            refreshList();
            dialog.dismiss();
            Toast.makeText(this, "Exam '" + name + "' saved successfully", Toast.LENGTH_SHORT).show();
        });

        dialog.show();
    }

    private void addSectionHeader(LinearLayout container, String title) {
        TextView tv = new TextView(this);
        tv.setText(title);
        tv.setTextColor(0xFF00E676);
        tv.setTextSize(11f);
        tv.setTypeface(null, android.graphics.Typeface.BOLD);
        tv.setPadding(0, 12, 0, 4);
        container.addView(tv);
    }

    private class ExamAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return examList.size();
        }

        @Override
        public Exam getItem(int position) {
            return examList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(ExamActivity.this).inflate(R.layout.item_exam, parent, false);
            }

            Exam exam = getItem(position);
            TextView tvTitle = convertView.findViewById(R.id.tvExamTitle);
            TextView tvBadge = convertView.findViewById(R.id.tvActiveBadge);
            TextView tvMarking = convertView.findViewById(R.id.tvExamMarking);
            TextView tvKey = convertView.findViewById(R.id.tvExamKeySummary);

            Button btnSetActive = convertView.findViewById(R.id.btnSetActive);
            Button btnEdit = convertView.findViewById(R.id.btnEditExam);
            Button btnDelete = convertView.findViewById(R.id.btnDeleteExam);

            tvTitle.setText(exam.getName());
            tvMarking.setText(String.format(Locale.getDefault(), "Correct: +%.1f  |  Negative: -%.2f  |  Max: %.1f",
                    exam.getMarksPerCorrect(), exam.getNegativeMarks(), exam.getMaxPossibleMarks()));

            StringBuilder sb = new StringBuilder("Key: ");
            char[] key = exam.getAnswerKey();
            if (key != null) {
                for (int i = 0; i < key.length; i++) {
                    sb.append(key[i]);
                    if (i < key.length - 1) sb.append(" ");
                }
            }
            tvKey.setText(sb.toString());

            Exam activeExam = examManager.getActiveExam();
            boolean isActive = activeExam != null && activeExam.getId().equals(exam.getId());
            tvBadge.setVisibility(isActive ? View.VISIBLE : View.GONE);
            btnSetActive.setEnabled(!isActive);
            btnSetActive.setText(isActive ? "Active" : "Set Active");

            btnSetActive.setOnClickListener(v -> {
                examManager.setActiveExamId(exam.getId());
                notifyDataSetChanged();
                Toast.makeText(ExamActivity.this, "Active Exam set to: " + exam.getName(), Toast.LENGTH_SHORT).show();
            });

            btnEdit.setOnClickListener(v -> showEditExamDialog(exam));

            btnDelete.setOnClickListener(v -> {
                if (examList.size() <= 1) {
                    Toast.makeText(ExamActivity.this, "Cannot delete the only examination", Toast.LENGTH_SHORT).show();
                    return;
                }
                new AlertDialog.Builder(ExamActivity.this)
                        .setTitle("Delete Examination")
                        .setMessage("Delete '" + exam.getName() + "' and its answer key?")
                        .setPositiveButton("Delete", (d, w) -> {
                            examManager.deleteExam(exam.getId());
                            refreshList();
                            Toast.makeText(ExamActivity.this, "Exam deleted", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });

            return convertView;
        }
    }
}

