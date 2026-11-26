package hei.school;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

class SubmissionDurationTest {

    @Test
    void allowsStartingAnExerciseWithoutTimerEvenAfterDeadline() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        SubmissionController controller = controllerWith(jdbcTemplate);
        Map<String, Object> exercise = new HashMap<>();
        exercise.put("deadline", Timestamp.valueOf(LocalDateTime.now().minusMinutes(1)));
        exercise.put("duration_minutes", null);
        when(jdbcTemplate.queryForList(
                eq("SELECT deadline, duration_minutes FROM exercise WHERE id = ?"), eq(1L)))
                .thenReturn(List.of(exercise));
        when(jdbcTemplate.queryForList(eq("SELECT id FROM student WHERE id = ?"), eq(2L)))
                .thenReturn(List.of(Map.of("id", 2L)));

        ResponseEntity<?> response = controller.startExercise(1, 2);

        assertEquals(200, response.getStatusCode().value());
        verify(jdbcTemplate, org.mockito.Mockito.never())
                .update(anyString(), org.mockito.ArgumentMatchers.<Object[]>any());
    }

    @Test
    void requiresTimedExerciseToBeStartedBeforeSubmission() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        SubmissionController controller = controllerWith(jdbcTemplate);
        stubExerciseAndStudent(jdbcTemplate);
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(1L), eq(2L))).thenReturn(0);
        when(jdbcTemplate.queryForList(anyString(), eq(1L), eq(2L))).thenReturn(List.of());

        ResponseEntity<?> response = controller.submit(1, Map.of(
                "studentId", 2,
                "content", "solution"));

        assertEquals(409, response.getStatusCode().value());
        verify(jdbcTemplate, org.mockito.Mockito.never())
                .update(anyString(), org.mockito.ArgumentMatchers.<Object[]>any());
    }

    @Test
    void rejectsSubmissionAfterTimedDurationWith422() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        SubmissionController controller = controllerWith(jdbcTemplate);
        stubExerciseAndStudent(jdbcTemplate);
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(1L), eq(2L))).thenReturn(0);
        Map<String, Object> start = new HashMap<>();
        start.put("started_at", Timestamp.valueOf(LocalDateTime.now().minusMinutes(31)));
        when(jdbcTemplate.queryForList(anyString(), eq(1L), eq(2L))).thenReturn(List.of(start));

        ResponseEntity<?> response = controller.submit(1, Map.of(
                "studentId", 2,
                "content", "solution"));

        assertEquals(422, response.getStatusCode().value());
        verify(jdbcTemplate, org.mockito.Mockito.never())
                .update(anyString(), org.mockito.ArgumentMatchers.<Object[]>any());
    }

    private static SubmissionController controllerWith(JdbcTemplate jdbcTemplate) {
        SubmissionController controller = new SubmissionController();
        ReflectionTestUtils.setField(controller, "jdbcTemplate", jdbcTemplate);
        return controller;
    }

    private static void stubExerciseAndStudent(JdbcTemplate jdbcTemplate) {
        Map<String, Object> exercise = new HashMap<>();
        exercise.put("deadline", null);
        exercise.put("duration_minutes", 30);
        when(jdbcTemplate.queryForList(eq("SELECT * FROM exercise WHERE id = ?"), eq(1L)))
                .thenReturn(List.of(exercise));
        when(jdbcTemplate.queryForList(eq("SELECT * FROM student WHERE id = ?"), eq(2L)))
                .thenReturn(List.of(Map.of("id", 2L)));
    }
}
