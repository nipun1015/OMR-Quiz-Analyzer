package com.example.omrscanner;

import org.junit.Test;

import static org.junit.Assert.*;

public class OMRSystemTest {

    @Test
    public void testOMRConfigDotCounts() {
        // Roll number: 10 columns x 10 rows = 100 bubbles
        assertEquals(10, OMRConfig.ROLL_COLS);
        assertEquals(10, OMRConfig.ROLL_ROWS);
        assertEquals(100, OMRConfig.ROLL_COLS * OMRConfig.ROLL_ROWS);

        // Answers: 3 groups of 5 questions x 4 options = 60 bubbles
        assertEquals(3, OMRConfig.NUM_GROUPS);
        assertEquals(5, OMRConfig.QUESTIONS_PER_GROUP);
        assertEquals(4, OMRConfig.OPTIONS_PER_Q);
        assertEquals(15, OMRConfig.TOTAL_QUESTIONS);
        assertEquals(60, OMRConfig.NUM_GROUPS * OMRConfig.QUESTIONS_PER_GROUP * OMRConfig.OPTIONS_PER_Q);

        // Total dots
        assertEquals(160, (10 * 10) + (3 * 5 * 4));
    }

    @Test
    public void testEvaluationEngineAllCorrect() {
        Exam exam = new Exam();
        exam.setName("Test Exam");
        exam.setMarksPerCorrect(2.0);
        exam.setNegativeMarks(0.5);
        char[] key = {'A', 'B', 'C', 'D', 'A', 'B', 'C', 'D', 'A', 'B', 'C', 'D', 'A', 'B', 'C'};
        exam.setAnswerKey(key);

        char[] studentAnswers = {'A', 'B', 'C', 'D', 'A', 'B', 'C', 'D', 'A', 'B', 'C', 'D', 'A', 'B', 'C'};
        BubbleDetectionResult detection = new BubbleDetectionResult(
                "1021034567",
                studentAnswers,
                new float[10][10],
                new float[15][4],
                null
        );

        OMRResult result = EvaluationEngine.evaluate(exam, detection);

        assertEquals("1021034567", result.getRollNumber());
        assertEquals(15, result.getTotalCorrect());
        assertEquals(0, result.getTotalIncorrect());
        assertEquals(0, result.getTotalUnanswered());
        assertEquals(30.0, result.getFinalScore(), 0.001);
        assertEquals(30.0, result.getMaxScore(), 0.001);
        assertEquals(100.0, result.getPercentage(), 0.001);
    }

    @Test
    public void testEvaluationEngineWithNegativeMarksAndUnanswered() {
        Exam exam = new Exam();
        exam.setName("Test Exam");
        exam.setMarksPerCorrect(4.0);
        exam.setNegativeMarks(1.0);
        exam.setMarksPerUnanswered(0.0);

        char[] key = {
                'A', 'B', 'C', 'D', 'A', // Q1-5
                'B', 'C', 'D', 'A', 'B', // Q6-10
                'C', 'D', 'A', 'B', 'C'  // Q11-15
        };
        exam.setAnswerKey(key);

        // 10 correct, 4 incorrect (3 wrong + 1 multiple), 1 unanswered
        char[] studentAnswers = {
                'A', 'B', 'C', 'D', 'A', // Q1-5: all 5 correct (+20)
                'A', 'B', 'C', 'A', 'B', // Q6-8 wrong (-3), Q9-10 correct (+8)
                'C', 'D', '?', 'M', 'C'  // Q11-12 correct (+8), Q13 blank (0), Q14 multiple (-1), Q15 correct (+4)
        };
        // Total correct = 5 + 2 + 3 = 10 -> +40
        // Total incorrect = 3 wrong + 1 multiple = 4 -> -4
        // Unanswered = 1 -> 0
        // Net score = 36 / 60 = 60.0%

        BubbleDetectionResult detection = new BubbleDetectionResult(
                "2024105983",
                studentAnswers,
                new float[10][10],
                new float[15][4],
                null
        );

        OMRResult result = EvaluationEngine.evaluate(exam, detection);

        assertEquals("2024105983", result.getRollNumber());
        assertEquals(10, result.getTotalCorrect());
        assertEquals(4, result.getTotalIncorrect());
        assertEquals(1, result.getTotalUnanswered());
        assertEquals(36.0, result.getFinalScore(), 0.001);
        assertEquals(60.0, result.getMaxScore(), 0.001);
        assertEquals(60.0, result.getPercentage(), 0.001);
    }

    @Test
    public void testExamJsonSerialization() throws Exception {
        Exam exam = new Exam();
        exam.setName("Midterm Evaluation");
        exam.setDate("2026-11-11");
        exam.setMarksPerCorrect(3.0);
        exam.setNegativeMarks(0.75);
        exam.setMarksPerUnanswered(-0.2);

        char[] key = {'B', 'C', 'D', 'A', 'B', 'C', 'D', 'A', 'B', 'C', 'D', 'A', 'B', 'C', 'D'};
        exam.setAnswerKey(key);

        String json = exam.toJson().toString();
        Exam restored = Exam.fromJson(new org.json.JSONObject(json));

        assertEquals(exam.getId(), restored.getId());
        assertEquals(exam.getName(), restored.getName());
        assertEquals(exam.getDate(), restored.getDate());
        assertEquals(exam.getMarksPerCorrect(), restored.getMarksPerCorrect(), 0.001);
        assertEquals(exam.getNegativeMarks(), restored.getNegativeMarks(), 0.001);
        assertEquals(exam.getMarksPerUnanswered(), restored.getMarksPerUnanswered(), 0.001);

        for (int i = 0; i < 15; i++) {
            assertEquals(key[i], restored.getAnswerKey()[i]);
        }
    }
}
