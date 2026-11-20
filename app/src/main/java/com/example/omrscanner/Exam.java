package com.example.omrscanner;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.UUID;

/**
 * Model representing an Examination and its Answer Key.
 * Supports configurable marking scheme (marks per correct, negative marking, unanswered).
 */
public class Exam implements Serializable {

    private String id;
    private String name;
    private String date;
    private char[] answerKey; // Exactly 15 answers: 'A', 'B', 'C', or 'D'
    private double marksPerCorrect;
    private double negativeMarks;
    private double marksPerUnanswered;

    public Exam() {
        this.id = UUID.randomUUID().toString();
        this.name = "Untitled Exam";
        this.date = "";
        this.answerKey = new char[OMRConfig.TOTAL_QUESTIONS];
        for (int i = 0; i < answerKey.length; i++) {
            answerKey[i] = 'A';
        }
        this.marksPerCorrect = 1.0;
        this.negativeMarks = 0.0;
        this.marksPerUnanswered = 0.0;
    }

    public Exam(String name, String date, char[] answerKey, double marksPerCorrect, double negativeMarks, double marksPerUnanswered) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.date = date;
        this.answerKey = answerKey;
        this.marksPerCorrect = marksPerCorrect;
        this.negativeMarks = negativeMarks;
        this.marksPerUnanswered = marksPerUnanswered;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public char[] getAnswerKey() {
        return answerKey;
    }

    public void setAnswerKey(char[] answerKey) {
        this.answerKey = answerKey;
    }

    public char getCorrectAnswer(int questionIndex0Based) {
        if (questionIndex0Based >= 0 && questionIndex0Based < answerKey.length) {
            return answerKey[questionIndex0Based];
        }
        return '?';
    }

    public void setCorrectAnswer(int questionIndex0Based, char option) {
        if (questionIndex0Based >= 0 && questionIndex0Based < answerKey.length) {
            answerKey[questionIndex0Based] = Character.toUpperCase(option);
        }
    }

    public double getMarksPerCorrect() {
        return marksPerCorrect;
    }

    public void setMarksPerCorrect(double marksPerCorrect) {
        this.marksPerCorrect = marksPerCorrect;
    }

    public double getNegativeMarks() {
        return negativeMarks;
    }

    public void setNegativeMarks(double negativeMarks) {
        this.negativeMarks = negativeMarks;
    }

    public double getMarksPerUnanswered() {
        return marksPerUnanswered;
    }

    public void setMarksPerUnanswered(double marksPerUnanswered) {
        this.marksPerUnanswered = marksPerUnanswered;
    }

    public double getMaxPossibleMarks() {
        return marksPerCorrect * OMRConfig.TOTAL_QUESTIONS;
    }

    // JSON Serialization
    public JSONObject toJson() throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("id", id);
        obj.put("name", name);
        obj.put("date", date);

        JSONArray keyArr = new JSONArray();
        if (answerKey != null) {
            for (char c : answerKey) {
                keyArr.put(String.valueOf(c));
            }
        }
        obj.put("answerKey", keyArr);
        obj.put("marksPerCorrect", marksPerCorrect);
        obj.put("negativeMarks", negativeMarks);
        obj.put("marksPerUnanswered", marksPerUnanswered);
        return obj;
    }

    public static Exam fromJson(JSONObject obj) throws JSONException {
        Exam exam = new Exam();
        exam.setId(obj.optString("id", UUID.randomUUID().toString()));
        exam.setName(obj.optString("name", "Untitled Exam"));
        exam.setDate(obj.optString("date", ""));
        exam.setMarksPerCorrect(obj.optDouble("marksPerCorrect", 1.0));
        exam.setNegativeMarks(obj.optDouble("negativeMarks", 0.0));
        exam.setMarksPerUnanswered(obj.optDouble("marksPerUnanswered", 0.0));

        JSONArray keyArr = obj.optJSONArray("answerKey");
        char[] key = new char[OMRConfig.TOTAL_QUESTIONS];
        if (keyArr != null) {
            for (int i = 0; i < key.length && i < keyArr.length(); i++) {
                String s = keyArr.optString(i, "A");
                key[i] = s.length() > 0 ? s.charAt(0) : 'A';
            }
        } else {
            for (int i = 0; i < key.length; i++) key[i] = 'A';
        }
        exam.setAnswerKey(key);
        return exam;
    }
}

