package hei.school;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DeadlineMigration implements ApplicationRunner {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        String deadlineType = jdbcTemplate.queryForObject(
                "SELECT data_type FROM information_schema.columns "
                        + "WHERE table_schema = current_schema() AND table_name = 'exercise' AND column_name = 'deadline'",
                String.class);

        if ("date".equals(deadlineType)) {
            jdbcTemplate.execute(
                    "ALTER TABLE exercise ALTER COLUMN deadline TYPE TIMESTAMP "
                            + "USING deadline + TIME '23:59:59'");
        } else if (!"timestamp without time zone".equals(deadlineType)) {
            throw new IllegalStateException("Type inattendu pour exercise.deadline : " + deadlineType);
        }
    }
}
