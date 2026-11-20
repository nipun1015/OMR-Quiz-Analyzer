package com.example.omrscanner;

import android.content.Context;
import android.os.Environment;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Handles persistent storage of student scan results locally in JSON format,
 * and handles CSV export without any cloud dependencies.
 */
public class ResultStorage {

    private static final String RESULTS_FILE = "omr_scan_results.json";
    private final Context context;
    private final List<OMRResult> results = new ArrayList<>();

    public ResultStorage(Context context) {
        this.context = context.getApplicationContext();
        loadResults();
    }

    public synchronized List<OMRResult> getAllResults() {
        return new ArrayList<>(results);
    }

    public synchronized List<OMRResult> getResultsByExam(String examId) {
        List<OMRResult> filtered = new ArrayList<>();
        if (examId == null) return filtered;
        for (OMRResult r : results) {
            if (examId.equals(r.getExamId())) {
                filtered.add(r);
            }
        }
        return filtered;
    }

    public synchronized List<OMRResult> searchByRollNumber(String query) {
        List<OMRResult> filtered = new ArrayList<>();
        if (query == null || query.trim().isEmpty()) return getAllResults();
        String q = query.trim().toLowerCase(Locale.ROOT);
        for (OMRResult r : results) {
            if (r.getRollNumber() != null && r.getRollNumber().toLowerCase(Locale.ROOT).contains(q)) {
                filtered.add(r);
            }
        }
        return filtered;
    }

    public synchronized void saveResult(OMRResult result) {
        if (result == null) return;
        // Check if existing result with same id
        boolean found = false;
        for (int i = 0; i < results.size(); i++) {
            if (results.get(i).getId().equals(result.getId())) {
                results.set(i, result);
                found = true;
                break;
            }
        }
        if (!found) {
            results.add(0, result); // Add newest first
        }
        persistResults();
    }

    public synchronized void deleteResult(String id) {
        for (int i = 0; i < results.size(); i++) {
            if (results.get(i).getId().equals(id)) {
                results.remove(i);
                break;
            }
        }
        persistResults();
    }

    private void loadResults() {
        results.clear();
        File file = new File(context.getFilesDir(), RESULTS_FILE);
        if (!file.exists()) return;

        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] bytes = new byte[(int) file.length()];
            fis.read(bytes);
            String jsonStr = new String(bytes, StandardCharsets.UTF_8);
            JSONArray arr = new JSONArray(jsonStr);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                results.add(OMRResult.fromJson(obj));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void persistResults() {
        try {
            JSONArray arr = new JSONArray();
            for (OMRResult r : results) {
                arr.put(r.toJson());
            }
            File file = new File(context.getFilesDir(), RESULTS_FILE);
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(arr.toString(2).getBytes(StandardCharsets.UTF_8));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Exports results to a CSV file in app documents / downloads directory.
     * @param examId Optional exam filter (null for all exams)
     * @return Generated CSV File
     */
    public File exportToCsv(String examId) throws IOException {
        List<OMRResult> dataToExport = (examId != null && !examId.isEmpty()) ?
                getResultsByExam(examId) : getAllResults();

        File exportDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        if (exportDir == null) {
            exportDir = context.getFilesDir();
        }
        if (!exportDir.exists()) {
            exportDir.mkdirs();
        }

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        File csvFile = new File(exportDir, "OMR_Results_" + timeStamp + ".csv");

        try (FileWriter writer = new FileWriter(csvFile)) {
            // Write CSV Header
            StringBuilder header = new StringBuilder();
            header.append("Roll Number,Exam Name,Date/Time,");
            for (int i = 1; i <= OMRConfig.TOTAL_QUESTIONS; i++) {
                header.append("Q").append(i).append(",");
            }
            header.append("Total Correct,Total Incorrect,Total Unanswered,Final Score,Max Score,Percentage\n");
            writer.write(header.toString());

            // Write Data Rows
            for (OMRResult r : dataToExport) {
                StringBuilder row = new StringBuilder();
                row.append(escapeCsv(r.getRollNumber())).append(",");
                row.append(escapeCsv(r.getExamName())).append(",");
                row.append(escapeCsv(r.getTimestamp())).append(",");

                char[] answers = r.getDetectedAnswers();
                for (int i = 0; i < OMRConfig.TOTAL_QUESTIONS; i++) {
                    char ans = (answers != null && i < answers.length) ? answers[i] : '?';
                    row.append(ans).append(",");
                }

                row.append(r.getTotalCorrect()).append(",");
                row.append(r.getTotalIncorrect()).append(",");
                row.append(r.getTotalUnanswered()).append(",");
                row.append(r.getFinalScore()).append(",");
                row.append(r.getMaxScore()).append(",");
                row.append(r.getPercentage()).append("%\n");

                writer.write(row.toString());
            }
            writer.flush();
        }

        return csvFile;
    }

    private String escapeCsv(String val) {
        if (val == null) return "";
        if (val.contains(",") || val.contains("\"") || val.contains("\n")) {
            return "\"" + val.replace("\"", "\"\"") + "\"";
        }
        return val;
    }
}

