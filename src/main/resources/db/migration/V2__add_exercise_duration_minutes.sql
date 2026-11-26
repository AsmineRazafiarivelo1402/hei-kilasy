ALTER TABLE exercise
    ADD COLUMN duration_minutes INTEGER
        CHECK (duration_minutes > 0);
