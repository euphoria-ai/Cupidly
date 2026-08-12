-- Cupidly / Hook — allowance schema.
-- Run this ONCE in the Supabase SQL editor (Dashboard > SQL Editor > New query),
-- then set SUPABASE_URL and SUPABASE_SERVICE_ROLE_KEY on the server.
-- Safe to re-run: everything here is idempotent.

-- One row per install. This is the ONLY thing the server persists about a user:
-- an opaque per-install id and a counter. No screenshots, no messages, no replies.
create table if not exists public.allowance (
    app_user_id text primary key,
    used        integer     not null default 0,
    updated_at  timestamptz not null default now()
);

-- Lock the table down. RLS on with NO policies means anon/authenticated keys
-- can read and write exactly nothing; only the service-role key (which bypasses
-- RLS, and lives only on the server) can touch it.
alter table public.allowance enable row level security;

revoke all on table public.allowance from anon, authenticated;

-- Atomic spend-one-generation.
-- PostgREST has no read-modify-write, so a select-then-update from the app
-- would race under concurrent requests and leak extra free generations.
-- This does it in a single statement and returns the new total.
create or replace function public.increment_allowance(p_app_user_id text)
returns integer
language plpgsql
security definer
set search_path = public
as $$
declare
    new_used integer;
begin
    insert into public.allowance (app_user_id, used, updated_at)
    values (p_app_user_id, 1, now())
    on conflict (app_user_id) do update
        set used = allowance.used + 1,
            updated_at = now()
    returning used into new_used;

    return new_used;
end;
$$;

-- Only the server (service role) may call it.
revoke all on function public.increment_allowance(text) from public, anon, authenticated;
grant execute on function public.increment_allowance(text) to service_role;
