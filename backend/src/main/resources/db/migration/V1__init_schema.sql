-- LockedIn schema. Ported from the original Prisma schema, with UUID ids.
-- Flyway owns the schema; JPA entities should map onto these tables (ddl-auto=validate).

CREATE TABLE users (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    name          VARCHAR(255),
    image         VARCHAR(1024),
    xp            INTEGER      NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE body_weights (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID             NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    weight     DOUBLE PRECISION NOT NULL,
    created_at TIMESTAMPTZ      NOT NULL DEFAULT now()
);
CREATE INDEX idx_body_weights_user_created ON body_weights (user_id, created_at);

CREATE TABLE exercises (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    name         VARCHAR(255) NOT NULL,
    description  TEXT         NOT NULL,
    muscle_group VARCHAR(64),
    equipment    VARCHAR(64),
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_exercises_user ON exercises (user_id);

CREATE TABLE logs (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID             NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    exercise_id UUID             NOT NULL REFERENCES exercises (id) ON DELETE CASCADE,
    weight      DOUBLE PRECISION NOT NULL,
    reps        INTEGER          NOT NULL,
    one_rep_max INTEGER          NOT NULL,
    created_at  TIMESTAMPTZ      NOT NULL DEFAULT now()
);
CREATE INDEX idx_logs_exercise_created ON logs (exercise_id, created_at);
CREATE INDEX idx_logs_user_created ON logs (user_id, created_at);

CREATE TABLE workouts (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    name        VARCHAR(255) NOT NULL,
    description TEXT         NOT NULL,
    -- Public program templates (PPL, StrongLifts, ...) are owned by the system user
    -- and can be copied by anyone.
    is_template BOOLEAN      NOT NULL DEFAULT FALSE,
    is_public   BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_workouts_user ON workouts (user_id);

CREATE TABLE workout_exercises (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workout_id  UUID        NOT NULL REFERENCES workouts (id) ON DELETE CASCADE,
    exercise_id UUID        NOT NULL REFERENCES exercises (id) ON DELETE CASCADE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_workout_exercise UNIQUE (workout_id, exercise_id)
);

CREATE TABLE nutrition_entries (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID             NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    calories   INTEGER          NOT NULL,
    protein    DOUBLE PRECISION NOT NULL DEFAULT 0,
    carbs      DOUBLE PRECISION NOT NULL DEFAULT 0,
    fat        DOUBLE PRECISION NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ      NOT NULL DEFAULT now()
);
CREATE INDEX idx_nutrition_entries_user_created ON nutrition_entries (user_id, created_at);

-- One daily target per user.
CREATE TABLE nutrition_goals (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID             NOT NULL UNIQUE REFERENCES users (id) ON DELETE CASCADE,
    calories   INTEGER          NOT NULL,
    protein    DOUBLE PRECISION NOT NULL DEFAULT 0,
    carbs      DOUBLE PRECISION NOT NULL DEFAULT 0,
    fat        DOUBLE PRECISION NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ      NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ      NOT NULL DEFAULT now()
);

-- Global, read-only library users pick from when creating their own exercise.
CREATE TABLE exercise_catalog (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name         VARCHAR(255) NOT NULL UNIQUE,
    muscle_group VARCHAR(64)  NOT NULL,
    equipment    VARCHAR(64)  NOT NULL,
    instructions TEXT         NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);
