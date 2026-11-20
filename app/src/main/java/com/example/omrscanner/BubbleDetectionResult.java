package com.example.omrscanner;

import android.graphics.Bitmap;

/**
 * Result of OpenCV bubble processing on an OMR sheet.
 * Contains detected roll number, answers, fill ratios, and optional debug visualization.
 */
public class BubbleDetectionResult {

    private final String rollNumber;          // 10 characters (digits 0-9, '?' for missing, 'M' for multiple)
    private final char[] answers;             // 15 characters ('A', 'B', 'C', 'D', '?' for unanswered, 'M' for multiple)
    private final float[][] rollDarkness;     // 10 cols x 10 rows darkness ratios (0.0 to 1.0)
    private final float[][] answerDarkness;   // 15 questions x 4 options darkness ratios (0.0 to 1.0)
    private final Bitmap debugBitmap;         // Visual debugging image with sampled regions and metrics

    public BubbleDetectionResult(
            String rollNumber,
            char[] answers,
            float[][] rollDarkness,
            float[][] answerDarkness,
            Bitmap debugBitmap) {
        this.rollNumber = rollNumber;
        this.answers = answers;
        this.rollDarkness = rollDarkness;
        this.answerDarkness = answerDarkness;
        this.debugBitmap = debugBitmap;
    }

    public String getRollNumber() {
        return rollNumber;
    }

    public char[] getAnswers() {
        return answers;
    }

    public char getAnswer(int questionIndex0Based) {
        if (questionIndex0Based >= 0 && questionIndex0Based < answers.length) {
            return answers[questionIndex0Based];
        }
        return '?';
    }

    public float[][] getRollDarkness() {
        return rollDarkness;
    }

    public float[][] getAnswerDarkness() {
        return answerDarkness;
    }

    public Bitmap getDebugBitmap() {
        return debugBitmap;
    }

    public boolean isRollComplete() {
        if (rollNumber == null || rollNumber.length() != 10) return false;
        for (int i = 0; i < 10; i++) {
            char c = rollNumber.charAt(i);
            if (c < '0' || c > '9') return false;
        }
        return true;
    }
}

