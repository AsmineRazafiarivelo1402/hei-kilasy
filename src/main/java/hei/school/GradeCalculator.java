package hei.school;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;

public final class GradeCalculator {

    private static final BigDecimal MAX_GRADE = new BigDecimal("20");
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private GradeCalculator() {
    }

    public static int penaltyPercent(LocalDateTime deadline, LocalDateTime submittedAt) {
        if (!submittedAt.isAfter(deadline)) {
            return 0;
        }

        Duration lateness = Duration.between(deadline, submittedAt);
        long fullDaysLate = lateness.toDays();
        long startedDaysLate = fullDaysLate + (lateness.minusDays(fullDaysLate).isZero() ? 0 : 1);
        return startedDaysLate >= 5 ? 50 : (int) startedDaysLate * 10;
    }

    public static GradeResult calculate(
            BigDecimal rawPoints,
            BigDecimal maxPoints,
            LocalDateTime deadline,
            LocalDateTime submittedAt) {
        if (maxPoints.signum() <= 0) {
            throw new IllegalArgumentException("Le barème doit être positif");
        }

        BigDecimal grade = rawPoints.multiply(MAX_GRADE)
                .divide(maxPoints, 1, RoundingMode.HALF_UP);
        int penaltyPercent = penaltyPercent(deadline, submittedAt);
        BigDecimal penaltyPoints = grade.multiply(BigDecimal.valueOf(penaltyPercent))
                .divide(HUNDRED, 1, RoundingMode.HALF_UP);
        BigDecimal finalGrade = grade.subtract(penaltyPoints)
                .max(BigDecimal.ZERO)
                .setScale(1, RoundingMode.HALF_UP);

        return new GradeResult(grade, penaltyPercent, penaltyPoints, finalGrade);
    }

    public record GradeResult(
            BigDecimal grade,
            int penaltyPercent,
            BigDecimal penaltyPoints,
            BigDecimal finalGrade) {
    }
}
