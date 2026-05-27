-- Teacher's Companion — Supabase PostgreSQL Schema
-- Run this in the Supabase SQL Editor or via `supabase migration up`

-- ============================================================
-- EXTENSIONS
-- ============================================================
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ============================================================
-- USERS TABLE (extends Supabase auth.users)
-- ============================================================
CREATE TABLE IF NOT EXISTS public.user_accounts (
  id            UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  auth_uid      UUID UNIQUE REFERENCES auth.users(id) ON DELETE CASCADE,
  email         TEXT UNIQUE NOT NULL,
  password_hash TEXT NOT NULL,
  full_name     TEXT NOT NULL DEFAULT '',
  gender        TEXT NOT NULL DEFAULT '',
  dob           TEXT NOT NULL DEFAULT '',
  address       TEXT NOT NULL DEFAULT '',
  phone         TEXT NOT NULL DEFAULT '',
  teaching_status TEXT NOT NULL DEFAULT 'FULL_TIME' CHECK (teaching_status IN ('FULL_TIME', 'PART_TIME')),
  is_onboarding_completed BOOLEAN NOT NULL DEFAULT false,
  subscription_plan TEXT NOT NULL DEFAULT 'FREE' CHECK (subscription_plan IN ('FREE', 'STANDARD', 'PREMIUM')),
  schools       JSONB NOT NULL DEFAULT '[]'::jsonb,
  created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_user_accounts_email ON public.user_accounts(email);
CREATE INDEX idx_user_accounts_auth_uid ON public.user_accounts(auth_uid);

-- ============================================================
-- LESSON NOTES
-- ============================================================
CREATE TABLE IF NOT EXISTS public.lesson_notes (
  id                     UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  user_id                UUID NOT NULL REFERENCES public.user_accounts(id) ON DELETE CASCADE,
  title                  TEXT NOT NULL,
  subject                TEXT NOT NULL,
  grade_class            TEXT NOT NULL,
  topic                  TEXT NOT NULL,
  duration               TEXT NOT NULL DEFAULT '',
  behavioral_objectives  TEXT NOT NULL DEFAULT '',
  entry_behavior         TEXT NOT NULL DEFAULT '',
  instructional_materials TEXT NOT NULL DEFAULT '',
  introduction           TEXT NOT NULL DEFAULT '',
  presentation           TEXT NOT NULL DEFAULT '',
  evaluation_questions   TEXT NOT NULL DEFAULT '',
  conclusion             TEXT NOT NULL DEFAULT '',
  assignment             TEXT NOT NULL DEFAULT '',
  syllabus_item_id       UUID,
  created_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at             TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_lesson_notes_user ON public.lesson_notes(user_id);
CREATE INDEX idx_lesson_notes_subject ON public.lesson_notes(subject);
CREATE INDEX idx_lesson_notes_created ON public.lesson_notes(created_at DESC);

-- ============================================================
-- MCQ SETS
-- ============================================================
CREATE TABLE IF NOT EXISTS public.mcq_sets (
  id            UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  user_id       UUID NOT NULL REFERENCES public.user_accounts(id) ON DELETE CASCADE,
  title         TEXT NOT NULL,
  subject       TEXT NOT NULL,
  grade_class   TEXT NOT NULL,
  topic         TEXT NOT NULL,
  difficulty    TEXT NOT NULL DEFAULT 'Medium' CHECK (difficulty IN ('Easy', 'Medium', 'Hard')),
  questions_json JSONB NOT NULL DEFAULT '[]'::jsonb,
  created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_mcq_sets_user ON public.mcq_sets(user_id);
CREATE INDEX idx_mcq_sets_subject ON public.mcq_sets(subject);

-- ============================================================
-- THEORY SETS
-- ============================================================
CREATE TABLE IF NOT EXISTS public.theory_sets (
  id            UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  user_id       UUID NOT NULL REFERENCES public.user_accounts(id) ON DELETE CASCADE,
  title         TEXT NOT NULL,
  subject       TEXT NOT NULL,
  grade_class   TEXT NOT NULL,
  topic         TEXT NOT NULL,
  questions_json JSONB NOT NULL DEFAULT '[]'::jsonb,
  created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_theory_sets_user ON public.theory_sets(user_id);
CREATE INDEX idx_theory_sets_subject ON public.theory_sets(subject);

-- ============================================================
-- TIMETABLE ITEMS
-- ============================================================
CREATE TABLE IF NOT EXISTS public.timetable_items (
  id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  user_id     UUID NOT NULL REFERENCES public.user_accounts(id) ON DELETE CASCADE,
  day_of_week TEXT NOT NULL CHECK (day_of_week IN ('Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday')),
  start_time  TEXT NOT NULL,
  end_time    TEXT NOT NULL,
  subject     TEXT NOT NULL,
  grade_class TEXT NOT NULL,
  school_name TEXT NOT NULL DEFAULT '',
  color_hex   TEXT NOT NULL DEFAULT '#4F81BD',
  is_completed BOOLEAN NOT NULL DEFAULT false,
  created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_timetable_user ON public.timetable_items(user_id);
CREATE INDEX idx_timetable_day ON public.timetable_items(day_of_week);

-- ============================================================
-- SYLLABUS ITEMS
-- ============================================================
CREATE TABLE IF NOT EXISTS public.syllabus_items (
  id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  user_id         UUID NOT NULL REFERENCES public.user_accounts(id) ON DELETE CASCADE,
  school_name     TEXT NOT NULL DEFAULT '',
  grade_class     TEXT NOT NULL DEFAULT '',
  subject         TEXT NOT NULL DEFAULT '',
  term            TEXT NOT NULL DEFAULT 'Term 1',
  week            INT NOT NULL DEFAULT 1,
  theme           TEXT NOT NULL DEFAULT '',
  topic           TEXT NOT NULL DEFAULT '',
  content         TEXT NOT NULL DEFAULT '',
  objectives      TEXT NOT NULL DEFAULT '',
  is_completed    BOOLEAN NOT NULL DEFAULT false,
  completion_date TIMESTAMPTZ,
  created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_syllabus_user ON public.syllabus_items(user_id);
CREATE INDEX idx_syllabus_subject ON public.syllabus_items(subject);
CREATE INDEX idx_syllabus_grade ON public.syllabus_items(grade_class);

-- ============================================================
-- SCHOOL CLASSES
-- ============================================================
CREATE TABLE IF NOT EXISTS public.school_classes (
  id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  user_id     UUID NOT NULL REFERENCES public.user_accounts(id) ON DELETE CASCADE,
  class_name  TEXT NOT NULL,
  school_name TEXT NOT NULL,
  subject     TEXT NOT NULL,
  created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_school_classes_user ON public.school_classes(user_id);
CREATE UNIQUE INDEX idx_school_classes_unique ON public.school_classes(user_id, class_name, school_name, subject);

-- ============================================================
-- STUDENTS
-- ============================================================
CREATE TABLE IF NOT EXISTS public.students (
  id                UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  user_id           UUID NOT NULL REFERENCES public.user_accounts(id) ON DELETE CASCADE,
  class_id          UUID NOT NULL REFERENCES public.school_classes(id) ON DELETE CASCADE,
  full_name         TEXT NOT NULL,
  performance_notes TEXT NOT NULL DEFAULT '',
  attendance_count  INT NOT NULL DEFAULT 0,
  total_sessions    INT NOT NULL DEFAULT 0,
  created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_students_user ON public.students(user_id);
CREATE INDEX idx_students_class ON public.students(class_id);

-- ============================================================
-- USER PREFERENCES
-- ============================================================
CREATE TABLE IF NOT EXISTS public.user_preferences (
  id         UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  user_id    UUID NOT NULL REFERENCES public.user_accounts(id) ON DELETE CASCADE,
  key        TEXT NOT NULL,
  value      TEXT NOT NULL DEFAULT '',
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_preferences_user ON public.user_preferences(user_id);
CREATE UNIQUE INDEX idx_preferences_user_key ON public.user_preferences(user_id, key);

-- ============================================================
-- USAGE LIMITS (for FREE tier tracking)
-- ============================================================
CREATE TABLE IF NOT EXISTS public.usage_limits (
  id         UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  user_id    UUID NOT NULL REFERENCES public.user_accounts(id) ON DELETE CASCADE,
  key        TEXT NOT NULL,
  value      INT NOT NULL DEFAULT 0,
  month      TEXT NOT NULL DEFAULT to_char(now(), 'YYYY-MM'),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_usage_limits_user ON public.usage_limits(user_id);
CREATE UNIQUE INDEX idx_usage_limits_user_month_key ON public.usage_limits(user_id, key, month);

-- ============================================================
-- AUTO-UPDATE updated_at TRIGGER
-- ============================================================
CREATE OR REPLACE FUNCTION public.set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = now();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DO $$
DECLARE
  tbl TEXT;
BEGIN
  FOR tbl IN
    SELECT unnest(ARRAY[
      'user_accounts', 'lesson_notes', 'mcq_sets', 'theory_sets',
      'timetable_items', 'syllabus_items', 'students', 'user_preferences', 'usage_limits'
    ])
  LOOP
    EXECUTE format(
      'CREATE TRIGGER set_%s_updated_at BEFORE UPDATE ON public.%I FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();',
      tbl, tbl
    );
  END LOOP;
END;
$$;

-- ============================================================
-- ROW LEVEL SECURITY
-- ============================================================
ALTER TABLE public.user_accounts ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.lesson_notes ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.mcq_sets ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.theory_sets ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.timetable_items ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.syllabus_items ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.school_classes ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.students ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.user_preferences ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.usage_limits ENABLE ROW LEVEL SECURITY;

-- Users can only access their own data
CREATE POLICY user_accounts_policy ON public.user_accounts
  USING (id = (SELECT id FROM public.user_accounts WHERE auth_uid = auth.uid()));

CREATE POLICY owner_access ON public.lesson_notes
  FOR ALL USING (user_id = (SELECT id FROM public.user_accounts WHERE auth_uid = auth.uid()));

CREATE POLICY owner_access ON public.mcq_sets
  FOR ALL USING (user_id = (SELECT id FROM public.user_accounts WHERE auth_uid = auth.uid()));

CREATE POLICY owner_access ON public.theory_sets
  FOR ALL USING (user_id = (SELECT id FROM public.user_accounts WHERE auth_uid = auth.uid()));

CREATE POLICY owner_access ON public.timetable_items
  FOR ALL USING (user_id = (SELECT id FROM public.user_accounts WHERE auth_uid = auth.uid()));

CREATE POLICY owner_access ON public.syllabus_items
  FOR ALL USING (user_id = (SELECT id FROM public.user_accounts WHERE auth_uid = auth.uid()));

CREATE POLICY owner_access ON public.school_classes
  FOR ALL USING (user_id = (SELECT id FROM public.user_accounts WHERE auth_uid = auth.uid()));

CREATE POLICY owner_access ON public.students
  FOR ALL USING (user_id = (SELECT id FROM public.user_accounts WHERE auth_uid = auth.uid()));

CREATE POLICY owner_access ON public.user_preferences
  FOR ALL USING (user_id = (SELECT id FROM public.user_accounts WHERE auth_uid = auth.uid()));

CREATE POLICY owner_access ON public.usage_limits
  FOR ALL USING (user_id = (SELECT id FROM public.user_accounts WHERE auth_uid = auth.uid()));

-- ============================================================
-- HELPER: Create user_account on sign-up (trigger)
-- ============================================================
CREATE OR REPLACE FUNCTION public.handle_new_auth_user()
RETURNS TRIGGER AS $$
BEGIN
  INSERT INTO public.user_accounts (auth_uid, email, password_hash)
  VALUES (NEW.id, NEW.email, '');
  RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;
CREATE TRIGGER on_auth_user_created
  AFTER INSERT ON auth.users
  FOR EACH ROW EXECUTE FUNCTION public.handle_new_auth_user();
