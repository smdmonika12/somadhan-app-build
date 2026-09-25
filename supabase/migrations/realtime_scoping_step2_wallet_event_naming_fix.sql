-- সমাধান (Somadhan) — Realtime Scoping ফিক্স, ধাপ ২ (wallet/money গ্রুপ)
--
-- প্রেক্ষাপট: transactions/withdrawals/gateway_payments/additional_charges — এই ৪টা টেবিলই
-- notifications-এর মতো একই `user:<uuid>` topic ব্যবহার করে (একাধিক টেবিল, একই topic)। আগের
-- সেশনে যে ৪টা trigger function বসানো হয়েছিল (bare TG_OP, যেমন event='INSERT') সেগুলো
-- notifications-এর trigger-এর bare event নাম থেকে আলাদা করা হলো না -- ফলে ক্লায়েন্ট-সাইডে
-- broadcastFlow(event='INSERT') একই topic-এ ৫টা ভিন্ন টেবিলের payload মিলেমিশে পেত (শুধু
-- accidental DTO-decode ব্যর্থতার উপর নির্ভর করে ভুল টেবিলের row বাদ পড়তো -- এটা silent
-- event-miss/misroute-এর ঝুঁকি, নিয়ম #৮-এর পরিপন্থী)।
--
-- ফিক্স: এই ৪টা wallet টেবিলের event নাম টেবিল-প্রিফিক্সড করা হলো
-- (যেমন 'transactions_INSERT') -- notifications-এর bare 'INSERT'/'UPDATE'/'DELETE' থেকে
-- সম্পূর্ণ আলাদা স্ট্রিং, তাই কোনো কোল্লিশন সম্ভব না। notifications-এর trigger (ধাপ ১,
-- আগে থেকেই টেস্ট-পাস করা) ইচ্ছাকৃতভাবে touch করা হয়নি (নিয়ম #২)।
--
-- নোট: এই ফাইলটা DB-তে migration version 20260913093831 হিসেবে আগের একটা সেশনে ইতিমধ্যে apply
-- করা ছিল, কিন্তু তখন repo-তে .sql ফাইল হিসেবে যোগ করা হয়নি -- এই সেশনে সেই gap পূরণ করা হলো
-- (supabase_migrations.schema_migrations.statements থেকে হুবহু reconstruct করা)। **এই ফাইল আবার
-- apply করার দরকার নেই।**

create or replace function public.notify_transactions_broadcast()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
  if coalesce(new.user_id, old.user_id) is not null then
    perform realtime.broadcast_changes(
      'user:' || coalesce(new.user_id, old.user_id)::text,
      'transactions_' || TG_OP, TG_OP, TG_TABLE_NAME, TG_TABLE_SCHEMA, new, old
    );
  end if;
  if coalesce(new.solver_id, old.solver_id) is not null then
    perform realtime.broadcast_changes(
      'user:' || coalesce(new.solver_id, old.solver_id)::text,
      'transactions_' || TG_OP, TG_OP, TG_TABLE_NAME, TG_TABLE_SCHEMA, new, old
    );
  end if;
  return null;
end;
$$;

create or replace function public.notify_withdrawals_broadcast()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
  perform realtime.broadcast_changes(
    'user:' || coalesce(new.solver_id, old.solver_id)::text,
    'withdrawals_' || TG_OP, TG_OP, TG_TABLE_NAME, TG_TABLE_SCHEMA, new, old
  );
  return null;
end;
$$;

create or replace function public.notify_gateway_payments_broadcast()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
  if coalesce(new.user_id, old.user_id) is not null then
    perform realtime.broadcast_changes(
      'user:' || coalesce(new.user_id, old.user_id)::text,
      'gateway_payments_' || TG_OP, TG_OP, TG_TABLE_NAME, TG_TABLE_SCHEMA, new, old
    );
  end if;
  return null;
end;
$$;

create or replace function public.notify_additional_charges_broadcast()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
  perform realtime.broadcast_changes(
    'user:' || coalesce(new.user_id, old.user_id)::text,
    'additional_charges_' || TG_OP, TG_OP, TG_TABLE_NAME, TG_TABLE_SCHEMA, new, old
  );
  perform realtime.broadcast_changes(
    'user:' || coalesce(new.solver_id, old.solver_id)::text,
    'additional_charges_' || TG_OP, TG_OP, TG_TABLE_NAME, TG_TABLE_SCHEMA, new, old
  );
  return null;
end;
$$;
