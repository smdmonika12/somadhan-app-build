-- ছোট, ইউজার-facing UID সিস্টেম: র‍্যান্ডম কিন্তু ইউনিক, ৬-সংখ্যা দিয়ে শুরু, পূর্ণ হয়ে
-- গেলে স্বয়ংক্রিয়ভাবে ৭, ৮... সংখ্যায় বাড়বে (কখনো কমবে না)।
-- [গুরুত্বপূর্ণ] এই মাইগ্রেশন ইতিমধ্যে সরাসরি Supabase প্রজেক্ট mghvvpndkxnscwryfkib-এ
-- apply করা হয়ে গেছে (MCP দিয়ে)। এই ফাইলটা শুধু প্রজেক্টের migrations history-তে
-- রেকর্ড রাখার জন্য যোগ করা হলো, আবার চালানোর দরকার নেই।

create table if not exists public.display_uid_state (
  id smallint primary key default 1 check (id = 1), -- single-row lock table
  current_digits smallint not null default 6
);
insert into public.display_uid_state (id, current_digits) values (1, 6)
  on conflict (id) do nothing;

alter table public.users add column if not exists display_uid bigint;
create unique index if not exists users_display_uid_key on public.users (display_uid);

create or replace function public.generate_unique_display_uid()
returns bigint
language plpgsql
security definer
set search_path to 'public'
as $function$
declare
  v_digits smallint;
  v_low bigint;
  v_high bigint;
  v_range bigint;
  v_used bigint;
  v_candidate bigint;
  v_attempt int := 0;
begin
  loop
    -- বর্তমান digit-length window লক করে পড়া হচ্ছে যাতে দুইজন একসাথে সাইনআপ করলেও
    -- window বাড়ানোর সিদ্ধান্তে race condition না হয়।
    select current_digits into v_digits from public.display_uid_state where id = 1 for update;

    v_low := power(10, v_digits - 1)::bigint;
    v_high := power(10, v_digits)::bigint - 1;
    v_range := v_high - v_low + 1;

    select count(*) into v_used from public.users
      where display_uid between v_low and v_high;

    -- এই window ৯০% ভরে গেলে পরের digit-length এ স্থায়ীভাবে সরে যাওয়া হয় (কখনো পেছনে
    -- ফেরে না), যাতে বাকি ১০% এর জন্য random collision retry ধীর/অনির্দিষ্ট না হয়ে যায়।
    if v_used >= (v_range * 0.9)::bigint then
      update public.display_uid_state set current_digits = v_digits + 1 where id = 1;
      continue;
    end if;

    exit;
  end loop;

  loop
    v_attempt := v_attempt + 1;
    v_candidate := v_low + floor(random() * v_range)::bigint;

    if not exists (select 1 from public.users where display_uid = v_candidate) then
      return v_candidate;
    end if;

    if v_attempt > 50 then
      -- অত্যন্ত বিরল fallback: window প্রায় পূর্ণ ধরে পরের চেষ্টায় জোর করে digit বাড়ানো
      update public.display_uid_state set current_digits = v_digits + 1 where id = 1;
      return public.generate_unique_display_uid();
    end if;
  end loop;
end;
$function$;

create or replace function public.set_display_uid_on_insert()
returns trigger
language plpgsql
security definer
set search_path to 'public'
as $function$
begin
  if new.display_uid is null then
    new.display_uid := public.generate_unique_display_uid();
  end if;
  return new;
end;
$function$;

drop trigger if exists trg_set_display_uid on public.users;
create trigger trg_set_display_uid
  before insert on public.users
  for each row
  execute function public.set_display_uid_on_insert();

-- বিদ্যমান ইউজারদের জন্য backfill (created_at অনুযায়ী ক্রমানুসারে, যাতে পুরনো
-- অ্যাকাউন্টগুলো ছোট সংখ্যা পায়)
do $$
declare
  r record;
begin
  for r in select id from public.users where display_uid is null order by created_at asc loop
    update public.users set display_uid = public.generate_unique_display_uid() where id = r.id;
  end loop;
end $$;

alter table public.users alter column display_uid set not null;
