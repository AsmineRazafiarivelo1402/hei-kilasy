package hei.school;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.http.ResponseEntity;

class SubmissionControllerSmsFailureTest {

    @Test
    void keepsGradePublishedWhenSmsSendingFails() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        JavaMailSender mailSender = mock(JavaMailSender.class);
        SubmissionController controller = new SubmissionController();
        SmsNotificationService smsService = new SmsNotificationService(message -> {
            throw new IllegalStateException("SMS provider unavailable");
        });

        ReflectionTestUtils.setField(controller, "jdbcTemplate", jdbcTemplate);
        ReflectionTestUtils.setField(controller, "mailSender", mailSender);
        ReflectionTestUtils.setField(controller, "smsNotificationService", smsService);

        Map<String, Object> submission = new HashMap<>();
        submission.put("max_points", new BigDecimal("20.00"));
        submission.put("deadline", Timestamp.valueOf(LocalDateTime.of(2026, 11, 10, 23, 59, 59)));
        submission.put("submitted_at", Timestamp.valueOf(LocalDateTime.of(2026, 11, 11, 1, 59, 59)));
        submission.put("full_name", "Alice Example");
        submission.put("title", "Tri à bulles");
        submission.put("email", "alice@example.test");
        submission.put("phone", "+261 34 00 000 01");
        when(jdbcTemplate.queryForList(anyString(), eq(42L))).thenReturn(List.of(submission));

        ResponseEntity<?> response = controller.grade(42, Map.of(
                "points", 16,
                "feedback", "Bon travail"));

        assertEquals(200, response.getStatusCode().value());
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) response.getBody();
        assertEquals(new BigDecimal("14.4"), result.get("finalGrade"));
        assertEquals(10, result.get("penaltyPercent"));
        verify(jdbcTemplate).update(anyString(), eq(16.0), eq("Bon travail"), eq(42L));
    }
}
