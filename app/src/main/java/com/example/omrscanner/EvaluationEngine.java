package com.example.omrscanner;

/**
 * Engine that evaluates student answers against an exam answer key.
 */
public class EvaluationEngine {

    public static OMRResult evaluate(Exam exam, BubbleDetectionResult detection) {
        OMRResult result = new OMRResult();

        result.setExamId(exam != null ? exam.getId() : "");
        result.setExamName(exam != null ? exam.getName() : "General Exam");
        result.setRollNumber(detection != null ? detection.getRollNumber() : "Unknown");

        char[] studentAnswers = detection != null ? detection.getAnswers() : new char[OMRConfig.TOTAL_QUESTIONS];
        char[] answerKey = exam != null ? exam.getAnswerKey() : new char[OMRConfig.TOTAL_QUESTIONS];

        result.setDetectedAnswers(studentAnswers);
        result.setCorrectAnswers(answerKey);

        OMRResult.Status[] statuses = new OMRResult.Status[OMRConfig.TOTAL_QUESTIONS];
        int correctCount = 0;
        int incorrectCount = 0;
        int unansweredCount = 0;

        double marksPerCorrect = exam != null ? exam.getMarksPerCorrect() : 1.0;
        double negativeMarks = exam != null ? exam.getNegativeMarks() : 0.0;
        double marksPerUnanswered = exam != null ? exam.getMarksPerUnanswered() : 0.0;

        double totalScore = 0.0;

        for (int i = 0; i < OMRConfig.TOTAL_QUESTIONS; i++) {
            char studentAns = (i < studentAnswers.length) ? studentAnswers[i] : '?';
            char keyAns = (i < answerKey.length) ? answerKey[i] : '?';

            if (studentAns == '?' || studentAns == ' ') {
                statuses[i] = OMRResult.Status.UNANSWERED;
                unansweredCount++;
                totalScore += marksPerUnanswered;
            } else if (studentAns == 'M') {
                statuses[i] = OMRResult.Status.MULTIPLE;
                incorrectCount++;
                totalScore -= negativeMarks;
            } else if (Character.toUpperCase(studentAns) == Character.toUpperCase(keyAns)) {
                statuses[i] = OMRResult.Status.CORRECT;
                correctCount++;
                totalScore += marksPerCorrect;
            } else {
                statuses[i] = OMRResult.Status.INCORRECT;
                incorrectCount++;
                totalScore -= negativeMarks;
            }
        }

        double maxScore = marksPerCorrect * OMRConfig.TOTAL_QUESTIONS;
        double percentage = maxScore > 0 ? Math.max(0.0, (totalScore / maxScore) * 100.0) : 0.0;

        result.setQuestionStatuses(statuses);
        result.setTotalCorrect(correctCount);
        result.setTotalIncorrect(incorrectCount);
        result.setTotalUnanswered(unansweredCount);
        result.setFinalScore(Math.round(totalScore * 100.0) / 100.0);
        result.setMaxScore(Math.round(maxScore * 100.0) / 100.0);
        result.setPercentage(Math.round(percentage * 10.0) / 10.0);

        return result;
    }
}

