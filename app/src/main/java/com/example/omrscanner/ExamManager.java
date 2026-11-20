package com.example.omrscanner;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Manages Examinations and Answer Keys in local storage.
 * Completely offline using JSON file storage.
 */
public class ExamManager {

    private static final String EXAMS_FILE = "omr_exams.json";
    private static final String PREFS_NAME = "omr_app_prefs";
    private static final String KEY_ACTIVE_EXAM_ID = "active_exam_id";

    private final Context context;
    private final List<Exam> exams = new ArrayList<>();

    public ExamManager(Context context) {
        this.context = context.getApplicationContext();
        loadExams();
    }

    public synchronized List<Exam> getAllExams() {
        return new ArrayList<>(exams);
    }

    public synchronized Exam getExamById(String id) {
        if (id == null) return null;
        for (Exam e : exams) {
            if (id.equals(e.getId())) return e;
        }
        return null;
    }

    public synchronized Exam getActiveExam() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String activeId = prefs.getString(KEY_ACTIVE_EXAM_ID, null);
        Exam exam = getExamById(activeId);
        if (exam == null && !exams.isEmpty()) {
            exam = exams.get(0);
            setActiveExamId(exam.getId());
        }
        return exam;
    }

    public synchronized void setActiveExamId(String id) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_ACTIVE_EXAM_ID, id).apply();
    }

    public synchronized void saveExam(Exam exam) {
        if (exam == null) return;
        boolean found = false;
        for (int i = 0; i < exams.size(); i++) {
            if (exams.get(i).getId().equals(exam.getId())) {
                exams.set(i, exam);
                found = true;
                break;
            }
        }
        if (!found) {
            exams.add(0, exam);
        }
        persistExams();
        setActiveExamId(exam.getId());
    }

    public synchronized void deleteExam(String id) {
        for (int i = 0; i < exams.size(); i++) {
            if (exams.get(i).getId().equals(id)) {
                exams.remove(i);
                break;
            }
        }
        persistExams();
    }

    private void loadExams() {
        exams.clear();
        File file = new File(context.getFilesDir(), EXAMS_FILE);
        if (!file.exists()) {
            // Seed a default exam matching OMR2 sheet (e.g. Cognitive Computing Quiz)
            Exam defaultExam = createDefaultExam();
            exams.add(defaultExam);
            persistExams();
            setActiveExamId(defaultExam.getId());
            return;
        }

        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] bytes = new byte[(int) file.length()];
            fis.read(bytes);
            String jsonStr = new String(bytes, StandardCharsets.UTF_8);
            JSONArray arr = new JSONArray(jsonStr);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                exams.add(Exam.fromJson(obj));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (exams.isEmpty()) {
            Exam defaultExam = createDefaultExam();
            exams.add(defaultExam);
            persistExams();
            setActiveExamId(defaultExam.getId());
        }
    }

    private void persistExams() {
        try {
            JSONArray arr = new JSONArray();
            for (Exam e : exams) {
                arr.put(e.toJson());
            }
            File file = new File(context.getFilesDir(), EXAMS_FILE);
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(arr.toString(2).getBytes(StandardCharsets.UTF_8));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Exam createDefaultExam() {
        Exam exam = new Exam();
        exam.setName("UCS420 Cognitive Computing Quiz");
        exam.setDate(new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date()));
        // Default answer key for 15 questions: A, B, C, D, A, B, C, D, A, B, C, D, A, B, C
        char[] defaultKey = {'A', 'B', 'C', 'D', 'A', 'B', 'C', 'D', 'A', 'B', 'C', 'D', 'A', 'B', 'C'};
        exam.setAnswerKey(defaultKey);
        exam.setMarksPerCorrect(1.0);
        exam.setNegativeMarks(0.25);
        exam.setMarksPerUnanswered(0.0);
        return exam;
    }
}

