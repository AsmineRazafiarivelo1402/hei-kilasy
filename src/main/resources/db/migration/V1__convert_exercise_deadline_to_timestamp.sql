DO $$
DECLARE
    deadline_type TEXT;
BEGIN
    SELECT data_type
    INTO deadline_type
    FROM information_schema.columns
    WHERE table_schema = current_schema()
      AND table_name = 'exercise'
      AND column_name = 'deadline';

    IF deadline_type = 'date' THEN
        ALTER TABLE exercise
            ALTER COLUMN deadline TYPE TIMESTAMP
            USING deadline + TIME '23:59:59';
    ELSIF deadline_type IS NOT NULL
          AND deadline_type <> 'timestamp without time zone' THEN
        RAISE EXCEPTION 'Unexpected type for exercise.deadline: %', deadline_type;
    END IF;
END
$$;
