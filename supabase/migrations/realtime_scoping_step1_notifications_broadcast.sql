-- সমাধান (Somadhan) — Realtime Broadcast/Topic Scoping ফিক্স, ধাপ ১ (স্বতন্ত্র কাজ, মূল
-- Firebase→Supabase migration থেকে আলাদা — দেখুন somadhan-realtime-scoping-fix-master-prompt.md)
--
-- Foundation + notifications pilot: টেবিল-ওয়াইড postgresChangeFlow-এর বদলে user:<user_id>
-- private broadcast topic-এ scoped delivery। postgres_changes subscription অক্ষত থাকছে
-- (dual-run), এটা শুধু একটা সমান্তরাল নতুন পথ যোগ করে।
--
-- নোট: এই ফাইলটা DB-তে migration version 20260913085320 হিসেবে ইতিমধ্যে apply করা আছে (Supabase
-- MCP দিয়ে সরাসরি) — কনটেন্ট supabase_migrations.schema_migrations.statements থেকে হুবহু
-- reconstruct করা হয়েছে (আগের একটা সেশনে এই ফাইল প্রথমবার লেখা হয়েছিল কিন্তু হাতে-লেখা
-- reconstruction-এ কিছু ছোট syntax পার্থক্য ছিল আসল applied কনটেন্টের সাথে -- এই সেশনে DB থেকে
-- সরাসরি পড়ে সেটা ঠিক করে দেওয়া হলো)। **এই ফাইল আবার apply করার দরকার নেই।**

-- ১) RLS policy: realtime.messages -- প্রতিটা authenticated user শুধু নিজের 'user:<uuid>'
--    topic-ই read করতে পারবে, অন্য কারো topic subscribe করার চেষ্টা reject হবে।
create policy "users can receive own user-topic broadcasts"
on "realtime"."messages"
for select
to authenticated
using (
  realtime.messages.extension = 'broadcast'
  and (select realtime.topic()) = 'user:' || (select auth.uid())::text
);

-- ২) Trigger function: notifications টেবিলের প্রতিটা INSERT/UPDATE/DELETE-এ owner-এর
--    'user:<user_id>' topic-এ broadcast করবে। DELETE-ও অন্তর্ভুক্ত করা হলো (শুধু insert/update
--    না) কারণ notifications-এ DELETE বাস্তবেই ব্যবহৃত হয় (admin_delete_notification_group RPC) --
--    পুরনো postgresChangeFlow ইতিমধ্যে DELETE হ্যান্ডল করে, নতুন পথও সমান আচরণ না করলে
--    dual-run-এর সময় নতুন পথ পুরনোটার চেয়ে কম সক্ষম হয়ে যেত।
create or replace function public.notify_notifications_broadcast()
returns trigger
security definer
set search_path = ''
language plpgsql
as $$
begin
  perform realtime.broadcast_changes(
    'user:' || coalesce(NEW.user_id, OLD.user_id)::text,  -- topic
    TG_OP,                                                  -- event
    TG_OP,                                                  -- operation
    TG_TABLE_NAME,                                          -- table
    TG_TABLE_SCHEMA,                                        -- schema
    NEW,                                                     -- new record
    OLD                                                      -- old record
  );
  return null;
end;
$$;

-- ৩) Trigger
drop trigger if exists broadcast_notifications_changes on public.notifications;
create trigger broadcast_notifications_changes
after insert or update or delete on public.notifications
for each row execute function public.notify_notifications_broadcast();

-- টেস্ট-ফলাফল (REALTIME_SCOPING_PROGRESS.md, "ধাপ ১" এন্ট্রি দেখুন): partition-সমস্যা সমাধান হওয়ার
-- পর broadcast সঠিক topic-এই যাচ্ছে (INSERT/UPDATE/DELETE তিনটাই) এবং RLS policy অন্য ইউজারের
-- topic-এ subscribe করার চেষ্টা সঠিকভাবে reject করছে।
