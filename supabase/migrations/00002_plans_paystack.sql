-- Teacher's Companion — Migration 00002: Subscription plans & Paystack
-- Migrates FREE→BASIC, STANDARD→ADVANCE, adds payment columns.

UPDATE public.user_accounts
SET subscription_plan = 'BASIC'
WHERE subscription_plan = 'FREE';

UPDATE public.user_accounts
SET subscription_plan = 'ADVANCE'
WHERE subscription_plan = 'STANDARD';

ALTER TABLE public.user_accounts
DROP CONSTRAINT IF EXISTS user_accounts_subscription_plan_check;

ALTER TABLE public.user_accounts
ADD CONSTRAINT user_accounts_subscription_plan_check
CHECK (subscription_plan IN ('BASIC', 'ADVANCE', 'PREMIUM'));

ALTER TABLE public.user_accounts
ADD COLUMN IF NOT EXISTS payment_email TEXT NOT NULL DEFAULT '';

ALTER TABLE public.user_accounts
ADD COLUMN IF NOT EXISTS plan_expires_at TIMESTAMPTZ;

ALTER TABLE public.user_accounts
ADD COLUMN IF NOT EXISTS last_payment_reference TEXT NOT NULL DEFAULT '';

ALTER TABLE public.user_accounts
ADD COLUMN IF NOT EXISTS last_payment_date TIMESTAMPTZ;
