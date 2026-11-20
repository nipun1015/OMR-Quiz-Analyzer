package com.example.omrscanner;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

/**
 * Evaluated scan result for a student sheet.
 */
public class OMRResult implements Serializable {

    public enum Status {
        CORRECT,
        INCORRECT,
        UNANSWERED,
        MULTIPLE
    }

    private String id;
    private String examId;
    private String examName;
    private String rollNumber;
    private String timestamp;
    private char[] detectedAnswers;
    private char[] correctAnswers;
    private Status[] questionStatuses;
    private int totalCorrect;
    private int totalIncorrect;
    private int totalUnanswered;
    private double finalScore;
    private double maxScore;
    private double percentage;

    public OMRResult() {
        this.id = UUID.randomUUID().toString();
        this.timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getExamId() {
        return examId;
    }

    public void setExamId(String examId) {
        this.examId = examId;
    }

    public String getExamName() {
        return examName;
    }

    public void setExamName(String examName) {
        this.examName = examName;
    }

    public String getRollNumber() {
        return rollNumber;
    }

    public void setRollNumber(String rollNumber) {
        this.rollNumber = rollNumber;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public char[] getDetectedAnswers() {
        return detectedAnswers;
    }

    public void setDetectedAnswers(char[] detectedAnswers) {
        this.detectedAnswers = detectedAnswers;
    }

    public char[] getCorrectAnswers() {
        return correctAnswers;
    }

    public void setCorrectAnswers(char[] correctAnswers) {
        this.correctAnswers = correctAnswers;
    }

    public Status[] getQuestionStatuses() {
        return questionStatuses;
    }

    public void setQuestionStatuses(Status[] questionStatuses) {
        this.questionStatuses = questionStatuses;
    }

    public int getTotalCorrect() {
        return totalCorrect;
    }

    public void setTotalCorrect(int totalCorrect) {
        this.totalCorrect = totalCorrect;
    }

    public int getTotalIncorrect() {
        return totalIncorrect;
    }

    public void setTotalIncorrect(int totalIncorrect) {
        this.totalIncorrect = totalIncorrect;
    }

    public int getTotalUnanswered() {
        return totalUnanswered;
    }

    public void setTotalUnanswered(int totalUnanswered) {
        this.totalUnanswered = totalUnanswered;
    }

    public double getFinalScore() {
        return finalScore;
    }

    public void setFinalScore(double finalScore) {
        this.finalScore = finalScore;
    }

    public double getMaxScore() {
        return maxScore;
    }

    public void setMaxScore(double maxScore) {
        this.maxScore = maxScore;
    }

    public double getPercentage() {
        return percentage;
    }

    public void setPercentage(double percentage) {
        this.percentage = percentage;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("id", id);
        obj.put("examId", examId);
        obj.put("examName", examName);
        obj.put("rollNumber", rollNumber);
        obj.put("timestamp", timestamp);

        JSONArray detArr = new JSONArray();
        if (detectedAnswers != null) {
            for (char c : detectedAnswers) detArr.put(String.valueOf(c));
        }
        obj.put("detectedAnswers", detArr);

        JSONArray corrArr = new JSONArray();
        if (correctAnswers != null) {
            for (char c : correctAnswers) corrArr.put(String.valueOf(c));
        }
        obj.put("correctAnswers", corrArr);

        JSONArray statArr = new JSONArray();
        if (questionStatuses != null) {
            for (Status s : questionStatuses) statArr.put(s.name());
        }
        obj.put("questionStatuses", statArr);

        obj.put("totalCorrect", totalCorrect);
        obj.put("totalIncorrect", totalIncorrect);
        obj.put("totalUnanswered", totalUnanswered);
        obj.put("finalScore", finalScore);
        obj.put("maxScore", maxScore);
        obj.put("percentage", percentage);

        return obj;
    }

    public static OMRResult fromJson(JSONObject obj) throws JSONException {
        OMRResult res = new OMRResult();
        res.setId(obj.optString("id", UUID.randomUUID().toString()));
        res.setExamId(obj.optString("examId", ""));
        res.setExamName(obj.optString("examName", ""));
        res.setRollNumber(obj.optString("rollNumber", ""));
        res.setTimestamp(obj.optString("timestamp", ""));

        JSONArray detArr = obj.optJSONArray("detectedAnswers");
        if (detArr != null) {
            char[] arr = new char[detArr.length()];
            for (int i = 0; i < detArr.length(); i++) {
                String s = detArr.optString(i, "?");
                arr[i] = s.length() > 0 ? s.charAt(0) : '?';
            }
            res.setDetectedAnswers(arr);
        }

        JSONArray corrArr = obj.optJSONArray("correctAnswers");
        if (corrArr != null) {
            char[] arr = new char[corrArr.length()];
            for (int i = 0; i < corrArr.length(); i++) {
                String s = corrArr.optString(i, "?");
                arr[i] = s.length() > 0 ? s.charAt(0) : '?';
            }
            res.setCorrectAnswers(arr);
        }

        JSONArray statArr = obj.optJSONArray("questionStatuses");
        if (statArr != null) {
            Status[] statuses = new Status[statArr.length()];
            for (int i = 0; i < statArr.length(); i++) {
                try {
                    statuses[i] = Status.valueOf(statArr.getString(i));
                } catch (Exception e) {
                    statuses[i] = Status.UNANSWERED;
                }
            }
            res.setQuestionStatuses(statuses);
        }

        res.setTotalCorrect(obj.optInt("totalCorrect", 0));
        res.setTotalIncorrect(obj.optInt("totalIncorrect", 0));
        res.setTotalUnanswered(obj.optInt("totalUnanswered", 0));
        res.setFinalScore(obj.optDouble("finalScore", 0.0));
        res.setMaxScore(obj.optDouble("maxScore", 0.0));
        res.setPercentage(obj.optDouble("percentage", 0.0));

        return res;
    }
}

