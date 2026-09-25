-- ধাপ ৪: bids টেবিলের জন্য Realtime Broadcast (topic scoping)
-- Topic: problem:<problem_id>:bids -- messages-এর problem:<problem_id> থেকে ইচ্ছাকৃতভাবে আলাদা
-- (bids_select policy-র visibility messages_select-এর চেয়ে বেশি broad -- OPEN+public problem-এর
-- বিড যে কেউ দেখতে পারে (ধাপ ২৩, Firebase-parity), শুধু owner/accepted_solver না -- এই দুই
-- টেবিলের topic একই রাখলে chat-এর privacy boundary ভেঙে যেত, তাই bids-এর জন্য নিজস্ব topic +
-- নিজস্ব RLS policy। ব্যবহারকারীকে ৩টা অপশন দিয়ে জিজ্ঞেস করা হয়েছিল, "Firebase-এর মতোই সিস্টেম"
-- (broad visibility parity) চাওয়া হয়েছে -- সেই অনুযায়ী এই policy bids_select-এর সাথে হুবহু
-- মিলিয়ে লেখা হয়েছে।

-- 1) Trigger function: bids_<TG_OP> event নামে problem:<id>:bids topic-এ broadcast
create or replace function public.notify_bids_broadcast()
returns trigger
language plpgsql
security definer
set search_path = ''
as $function$
begin
  perform realtime.broadcast_changes(
    'problem:' || coalesce(NEW.problem_id, OLD.problem_id) || ':bids',
    'bids_' || TG_OP, TG_OP, TG_TABLE_NAME, TG_TABLE_SCHEMA, NEW, OLD
  );
  return null;
end;
$function$;

drop trigger if exists broadcast_bids_changes on public.bids;
create trigger broadcast_bids_changes
after insert or update or delete on public.bids
for each row execute function public.notify_bids_broadcast();

-- 2) RLS policy: realtime.messages-এ problem:<id>:bids টপিক-প্যাটার্নের জন্য
-- bids_select-এর সাথে সামঞ্জস্যপূর্ণ (parity বজায় রাখার জন্য, ব্যবহারকারীর সিদ্ধান্ত অনুযায়ী) --
-- admin, নিজের bid থাকা solver, problem owner, অথবা OPEN+public+non-deleted problem হলে যে কেউই।
drop policy if exists "problem bids visibility broadcasts" on realtime.messages;
create policy "problem bids visibility broadcasts"
on realtime.messages
for select
to authenticated
using (
  extension = 'broadcast'
  and (select realtime.topic()) like 'problem:%:bids'
  and (
    is_admin((select auth.uid()))
    or exists (
      select 1 from public.problems p
      where ('problem:' || p.id || ':bids') = (select realtime.topic())
        and (
          p.user_id = (select auth.uid())
          or exists (
            select 1 from public.bids b
            where b.problem_id = p.id and b.solver_id = (select auth.uid())
          )
          or (p.status = 'OPEN' and p.is_public = true and p.is_user_deleted = false)
        )
    )
  )
);
