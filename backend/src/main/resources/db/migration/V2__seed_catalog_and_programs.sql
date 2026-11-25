-- Seed data: the global exercise catalog and the public program templates.
-- Ported from the original prisma/seed.mjs.

INSERT INTO exercise_catalog (name, muscle_group, equipment, instructions) VALUES
    ('Barbell Bench Press', 'Chest', 'Barbell', 'Lie on a flat bench, lower the bar to mid-chest, press up to lockout.'),
    ('Incline Dumbbell Press', 'Chest', 'Dumbbell', 'On a 30-45° incline, press dumbbells from chest to lockout.'),
    ('Dumbbell Fly', 'Chest', 'Dumbbell', 'With a slight elbow bend, open arms wide then squeeze the chest to bring them together.'),
    ('Push Up', 'Chest', 'Bodyweight', 'Keep a straight line head to heels, lower chest to floor, press back up.'),
    ('Cable Crossover', 'Chest', 'Cable', 'Pull both cable handles down and across the body, squeezing the chest.'),
    ('Deadlift', 'Back', 'Barbell', 'Hinge at the hips with a flat back, drive through the floor to stand tall.'),
    ('Pull Up', 'Back', 'Bodyweight', 'Hang from a bar, pull until chin clears the bar, lower under control.'),
    ('Bent Over Row', 'Back', 'Barbell', 'Hinge forward, row the bar to the lower ribs, squeeze the shoulder blades.'),
    ('Lat Pulldown', 'Back', 'Cable', 'Pull the bar to the upper chest, driving elbows down and back.'),
    ('Seated Cable Row', 'Back', 'Cable', 'Pull the handle to the abdomen with a tall chest, squeeze the back.'),
    ('Back Squat', 'Legs', 'Barbell', 'Bar on upper traps, squat to depth keeping the chest up, drive up through mid-foot.'),
    ('Front Squat', 'Legs', 'Barbell', 'Bar racked on front delts, squat upright, drive up keeping elbows high.'),
    ('Romanian Deadlift', 'Legs', 'Barbell', 'Hinge at the hips with soft knees, lower the bar along the legs, feel the hamstrings.'),
    ('Leg Press', 'Legs', 'Machine', 'Press the platform away until legs are nearly straight, lower under control.'),
    ('Walking Lunge', 'Legs', 'Dumbbell', 'Step forward into a lunge, drive through the front heel, alternate legs.'),
    ('Leg Curl', 'Legs', 'Machine', 'Curl the pad toward the glutes, squeeze the hamstrings, lower slowly.'),
    ('Calf Raise', 'Legs', 'Machine', 'Rise onto the toes through a full range, pause, lower under control.'),
    ('Overhead Press', 'Shoulders', 'Barbell', 'Press the bar from shoulders to overhead lockout, keep the core braced.'),
    ('Dumbbell Shoulder Press', 'Shoulders', 'Dumbbell', 'Press dumbbells from shoulder height to overhead, lower under control.'),
    ('Lateral Raise', 'Shoulders', 'Dumbbell', 'Raise dumbbells out to the sides to shoulder height, lower slowly.'),
    ('Face Pull', 'Shoulders', 'Cable', 'Pull the rope toward the face, leading with the elbows, squeeze rear delts.'),
    ('Barbell Curl', 'Arms', 'Barbell', 'Curl the bar with elbows pinned to the sides, squeeze the biceps at the top.'),
    ('Dumbbell Curl', 'Arms', 'Dumbbell', 'Curl dumbbells one or both at a time, supinating the wrists.'),
    ('Hammer Curl', 'Arms', 'Dumbbell', 'Curl with a neutral (thumbs-up) grip to target the brachialis.'),
    ('Tricep Pushdown', 'Arms', 'Cable', 'Push the bar down to full extension, keep elbows tucked.'),
    ('Skullcrusher', 'Arms', 'Barbell', 'Lying down, lower the bar toward the forehead then extend the elbows.'),
    ('Dip', 'Arms', 'Bodyweight', 'Lower until elbows reach ~90°, press back to lockout.'),
    ('Plank', 'Core', 'Bodyweight', 'Hold a straight line on forearms and toes, brace the core.'),
    ('Hanging Leg Raise', 'Core', 'Bodyweight', 'Hang from a bar and raise the legs toward parallel or higher.'),
    ('Cable Crunch', 'Core', 'Cable', 'Kneel and crunch the torso down, flexing the abs against the cable.'),
    ('Russian Twist', 'Core', 'Bodyweight', 'Seated with feet up, rotate the torso side to side.'),
    ('Running', 'Cardio', 'Bodyweight', 'Steady-state or interval running for cardiovascular conditioning.'),
    ('Rowing Machine', 'Cardio', 'Machine', 'Drive with the legs, swing the torso, finish with the arms; reverse to recover.'),
    ('Cycling', 'Cardio', 'Machine', 'Maintain a steady cadence; adjust resistance for intervals.');

-- The system user owns the program templates. Its password hash is not a valid
-- bcrypt hash, so nobody can ever log in as it.
INSERT INTO users (email, name, password_hash) VALUES ('system@lockedin.internal', 'LockedIn', '!');

-- System-owned copies of every catalog exercise used by a program.
INSERT INTO exercises (user_id, name, description, muscle_group, equipment)
SELECT u.id, c.name, c.instructions, c.muscle_group, c.equipment
FROM exercise_catalog c CROSS JOIN users u
WHERE u.email = 'system@lockedin.internal'
  AND c.name IN ('Back Squat', 'Barbell Bench Press', 'Barbell Curl', 'Bent Over Row', 'Calf Raise', 'Deadlift', 'Face Pull', 'Incline Dumbbell Press', 'Lateral Raise', 'Leg Curl', 'Leg Press', 'Overhead Press', 'Pull Up', 'Romanian Deadlift', 'Tricep Pushdown');

INSERT INTO workouts (user_id, name, description, is_template, is_public)
SELECT u.id, p.name, p.description, TRUE, TRUE
FROM users u CROSS JOIN (VALUES
    ('PPL Push', 'Push day: chest, shoulders, triceps.'),
    ('PPL Pull', 'Pull day: back and biceps.'),
    ('PPL Legs', 'Leg day: quads, hamstrings, calves.'),
    ('StrongLifts A', 'StrongLifts 5x5 workout A.'),
    ('StrongLifts B', 'StrongLifts 5x5 workout B.'),
    ('Starting Strength A', 'Starting Strength workout A.'),
    ('Starting Strength B', 'Starting Strength workout B.')
) AS p (name, description)
WHERE u.email = 'system@lockedin.internal';

INSERT INTO workout_exercises (workout_id, exercise_id, created_at)
SELECT w.id, e.id, now() + make_interval(secs => pe.position)
FROM (VALUES
    ('PPL Push', 'Barbell Bench Press', 0),
    ('PPL Push', 'Overhead Press', 1),
    ('PPL Push', 'Incline Dumbbell Press', 2),
    ('PPL Push', 'Lateral Raise', 3),
    ('PPL Push', 'Tricep Pushdown', 4),
    ('PPL Pull', 'Deadlift', 0),
    ('PPL Pull', 'Pull Up', 1),
    ('PPL Pull', 'Bent Over Row', 2),
    ('PPL Pull', 'Face Pull', 3),
    ('PPL Pull', 'Barbell Curl', 4),
    ('PPL Legs', 'Back Squat', 0),
    ('PPL Legs', 'Romanian Deadlift', 1),
    ('PPL Legs', 'Leg Press', 2),
    ('PPL Legs', 'Leg Curl', 3),
    ('PPL Legs', 'Calf Raise', 4),
    ('StrongLifts A', 'Back Squat', 0),
    ('StrongLifts A', 'Barbell Bench Press', 1),
    ('StrongLifts A', 'Bent Over Row', 2),
    ('StrongLifts B', 'Back Squat', 0),
    ('StrongLifts B', 'Overhead Press', 1),
    ('StrongLifts B', 'Deadlift', 2),
    ('Starting Strength A', 'Back Squat', 0),
    ('Starting Strength A', 'Barbell Bench Press', 1),
    ('Starting Strength A', 'Deadlift', 2),
    ('Starting Strength B', 'Back Squat', 0),
    ('Starting Strength B', 'Overhead Press', 1),
    ('Starting Strength B', 'Deadlift', 2)
) AS pe (workout_name, exercise_name, position)
JOIN users u ON u.email = 'system@lockedin.internal'
JOIN workouts w ON w.user_id = u.id AND w.name = pe.workout_name AND w.is_template
JOIN exercises e ON e.user_id = u.id AND e.name = pe.exercise_name;
