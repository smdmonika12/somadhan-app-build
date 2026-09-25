-- Realtime Scoping ফিক্স, ধাপ ৩ — Chat (`messages`)। Topic: 'problem:<problem_id>' -- resource-
-- based (user-based না), তাই এই problem-এ যুক্ত owner+accepted_solver উভয়েই একই topic থেকে পাবে,
-- dual-broadcast লাগে না (wallet গ্রুপের মতো না)।
--
-- Event নাম শুরু থেকেই table-prefixed ('messages_' || TG_OP) রাখা হলো -- কারণ ধাপ ৪-এ bids-ও এই
-- একই 'problem:<problem_id>' topic ব্যবহার করবে (roadmap অনুযায়ী), তাই bare event নাম হলে ঠিক
-- ধাপ ২-তে যে collision বাগ পাওয়া গিয়েছিল সেটাই আবার ঘটতো। এবার শুরু থেকেই সঠিকভাবে করা হলো, পরে
-- আলাদা "fix" migration লাগবে না।

-- ১) RLS policy: realtime.messages -- 'problem:<id>' প্যাটার্নের topic-এ শুধু সেই problem-এর
--    owner/accepted_solver/admin subscribe করতে পারবে -- ঠিক public.messages টেবিলের বিদ্যমান
--    messages_select RLS policy-র owner/accepted_solver লজিকের সাথে সামঞ্জস্যপূর্ণ (সাধারণ bidder-
--    রা, accepted হওয়ার আগে, আসল chat message-ও দেখতে পায় না -- তাই broadcast topic-ও তাদের জন্য
--    খুলে দেওয়া হলো না, over-authorization এড়াতে)।
-- (২০২৬-০৯-২২ ফিক্স, Step 13.8 সেশনে ধরা পড়া CI-order বাগ — ব্যবহারকারীর সরাসরি
-- অনুমোদনে যোগ করা হলো) এই policy-নামটা `realtime_scoping_messages_solver_thread_isolation.sql`-এও
-- আছে (পরবর্তী একটা রিফাইনমেন্ট হিসেবে, chronologically এই ফাইলের পরে লেখা), কিন্তু
-- সেই ফাইলের নাম alphabetically এই ফাইলের **আগে** পড়ে ("m" < "s") — তাই CI/সহ যেকোনো
-- alphabetical-sort migration-runner-এ (যেমন `full-test.yml`, `local_pgtap_bootstrap.sh`)
-- ওই ফাইলটা আগে চলে policy-টা তৈরি করে ফেলে, তারপর এই ফাইলের নিচের `create policy`
-- (guard ছাড়া) "already exists" error দিয়ে পুরো migration-ই ভেঙে দেয় — ফলে এর পরের
-- trigger/function statement-গুলো (`notify_messages_broadcast()`, `broadcast_messages_changes`)
-- CI-তে কখনো তৈরিই হয় না। `drop policy if exists` গার্ড যোগ করে idempotent করা হলো,
-- ঠিক সেই অন্য ফাইলের নিজস্ব প্যাটার্নের মতোই — এখন apply-order নির্বিশেষে (এই ফাইল
-- আগে বা পরে যে ক্রমেই চলুক) নিরাপদে re-create হবে। live Supabase-এ কোনো প্রভাব নেই
-- (policy আগে থেকেই একইভাবে সংজ্ঞায়িত আছে, শুধু drop+recreate একই জিনিস তৈরি করে)।
drop policy if exists "problem participants can receive problem-topic broadcasts" on "realtime"."messages";
create policy "problem participants can receive problem-topic broadcasts"
on "realtime"."messages"
for select
to authenticated
using (
  realtime.messages.extension = 'broadcast'
  and (select realtime.topic()) like 'problem:%'
  and (
    public.is_admin((select auth.uid()))
    or exists (
      select 1
      from public.problems p
      where 'problem:' || p.id = (select realtime.topic())
        and (p.user_id = (select auth.uid()) or p.accepted_solver_id = (select auth.uid()))
    )
  )
);

-- ২) Trigger function: messages টেবিলের প্রতিটা INSERT/UPDATE/DELETE-এ সংশ্লিষ্ট
--    'problem:<problem_id>' topic-এ broadcast করে (শুধু একবার call -- topic resource-based)।
create or replace function public.notify_messages_broadcast()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
  perform realtime.broadcast_changes(
    'problem:' || coalesce(NEW.problem_id, OLD.problem_id),
    'messages_' || TG_OP, TG_OP, TG_TABLE_NAME, TG_TABLE_SCHEMA, NEW, OLD
  );
  return null;
end;
$$;

-- ৩) Trigger
drop trigger if exists broadcast_messages_changes on public.messages;
create trigger broadcast_messages_changes
after insert or update or delete on public.messages
for each row execute function public.notify_messages_broadcast();
