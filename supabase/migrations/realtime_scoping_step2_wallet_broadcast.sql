-- Realtime Scoping ফিক্স, ধাপ ২ — Wallet/Money গ্রুপ (transactions/withdrawals/gateway_payments/
-- additional_charges) broadcast triggers। সবগুলোই 'user:<uuid>' topic-প্যাটার্ন ব্যবহার করে, যেটার
-- জন্য RLS policy ধাপ ১-এই বসানো হয়েছে ('users can receive own user-topic broadcasts',
-- realtime.messages টেবিলে) — সেটা টেবিল-নির্দিষ্ট না, শুধু topic-প্যাটার্নভিত্তিক, তাই পুনর্ব্যবহার
-- করা হচ্ছে, নতুন policy লাগছে না।
--
-- নোট: এই ফাইলটা DB-তে migration version 20260913093147 হিসেবে আগের একটা সেশনে ইতিমধ্যে apply
-- করা ছিল, কিন্তু তখন repo-তে .sql ফাইল হিসেবে যোগ করা হয়নি -- এই সেশনে সেই gap পূরণ করা হলো
-- (supabase_migrations.schema_migrations.statements থেকে হুবহু reconstruct করা)। **এই ফাইল আবার
-- apply করার দরকার নেই।** (এই migration-এর bare-TG_OP event নাম পরের migration
-- (realtime_scoping_step2_wallet_event_naming_fix.sql, version 20260913093831)-এ table-prefixed
-- করে ঠিক করা হয়েছে -- এই ফাইলটা শুধু ঐতিহাসিক রেকর্ডের জন্য অপরিবর্তিত রাখা হলো।)

-- ১. transactions — dual-owner (user_id ও solver_id দুটোই nullable) — দুই topic-এই broadcast,
--    যেটা null না সেটাতেই শুধু (null topic string তৈরি এড়াতে)।
create or replace function public.notify_transactions_broadcast()
returns trigger
language plpgsql
security definer
set search_path = ''
as $function$
begin
  if coalesce(NEW.user_id, OLD.user_id) is not null then
    perform realtime.broadcast_changes(
      'user:' || coalesce(NEW.user_id, OLD.user_id)::text,
      TG_OP, TG_OP, TG_TABLE_NAME, TG_TABLE_SCHEMA, NEW, OLD
    );
  end if;
  if coalesce(NEW.solver_id, OLD.solver_id) is not null then
    perform realtime.broadcast_changes(
      'user:' || coalesce(NEW.solver_id, OLD.solver_id)::text,
      TG_OP, TG_OP, TG_TABLE_NAME, TG_TABLE_SCHEMA, NEW, OLD
    );
  end if;
  return null;
end;
$function$;

drop trigger if exists broadcast_transactions_changes on public.transactions;
create trigger broadcast_transactions_changes
after insert or delete or update on public.transactions
for each row execute function public.notify_transactions_broadcast();

-- ২. withdrawals — single-owner (solver_id NOT NULL)।
create or replace function public.notify_withdrawals_broadcast()
returns trigger
language plpgsql
security definer
set search_path = ''
as $function$
begin
  perform realtime.broadcast_changes(
    'user:' || coalesce(NEW.solver_id, OLD.solver_id)::text,
    TG_OP, TG_OP, TG_TABLE_NAME, TG_TABLE_SCHEMA, NEW, OLD
  );
  return null;
end;
$function$;

drop trigger if exists broadcast_withdrawals_changes on public.withdrawals;
create trigger broadcast_withdrawals_changes
after insert or delete or update on public.withdrawals
for each row execute function public.notify_withdrawals_broadcast();

-- ৩. gateway_payments — single-owner (user_id nullable)।
create or replace function public.notify_gateway_payments_broadcast()
returns trigger
language plpgsql
security definer
set search_path = ''
as $function$
begin
  if coalesce(NEW.user_id, OLD.user_id) is not null then
    perform realtime.broadcast_changes(
      'user:' || coalesce(NEW.user_id, OLD.user_id)::text,
      TG_OP, TG_OP, TG_TABLE_NAME, TG_TABLE_SCHEMA, NEW, OLD
    );
  end if;
  return null;
end;
$function$;

drop trigger if exists broadcast_gateway_payments_changes on public.gateway_payments;
create trigger broadcast_gateway_payments_changes
after insert or delete or update on public.gateway_payments
for each row execute function public.notify_gateway_payments_broadcast();

-- ৪. additional_charges — dual-party (user_id ও solver_id দুটোই NOT NULL সরাসরি টেবিলেই আছে,
--    problems টেবিল থেকে lookup করার দরকার নেই — মাস্টার-প্রম্পটের অনুমানের চেয়ে সহজ)।
create or replace function public.notify_additional_charges_broadcast()
returns trigger
language plpgsql
security definer
set search_path = ''
as $function$
begin
  perform realtime.broadcast_changes(
    'user:' || coalesce(NEW.user_id, OLD.user_id)::text,
    TG_OP, TG_OP, TG_TABLE_NAME, TG_TABLE_SCHEMA, NEW, OLD
  );
  perform realtime.broadcast_changes(
    'user:' || coalesce(NEW.solver_id, OLD.solver_id)::text,
    TG_OP, TG_OP, TG_TABLE_NAME, TG_TABLE_SCHEMA, NEW, OLD
  );
  return null;
end;
$function$;

drop trigger if exists broadcast_additional_charges_changes on public.additional_charges;
create trigger broadcast_additional_charges_changes
after insert or delete or update on public.additional_charges
for each row execute function public.notify_additional_charges_broadcast();
