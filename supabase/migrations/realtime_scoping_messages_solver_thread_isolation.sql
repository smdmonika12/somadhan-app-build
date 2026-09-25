-- Messages: solver থ্রেড আইসোলেশন ফিক্স (ব্যবহারকারীর সরাসরি অনুরোধে, realtime-scoping ধাপ ৪-এর
-- পরে)
--
-- সমস্যা: `messages_select` (table RLS) এবং broadcast RLS policy দুটোতেই "তুমি কি এই problem-এর
-- বর্তমান accepted_solver?" -- এই শর্তটা প্রতিটা মেসেজ-রো নির্বিশেষে (sender/receiver না দেখেই)
-- পুরো problem-এর সব মেসেজ পড়ার অনুমতি দিচ্ছিল। ফলে কোনো solver reassign হলে, নতুন (বর্তমান)
-- accepted_solver আগের (বাতিল হওয়া) solver আর owner-এর মধ্যেকার পুরনো চ্যাটও দেখতে পারতো --
-- যেটা প্রাইভেসি বাগ, ব্যবহারকারী স্পষ্টভাবে ফিক্স করতে বলেছেন। (নোট: এই ফিক্স আগের সেশনের
-- Firebase-parity অডিটে পাওয়া "messages broadcast RLS-এ sender/receiver শর্ত অনুপস্থিত" গ্যাপটাও
-- একই সাথে সমাধান করে দিয়েছে -- সেই সংকীর্ণ ফিক্সের চেয়ে এটা বেশি সঠিক, কারণ এটা table policy-র
-- ভুলটাও (accepted_solver ব্লানকেট এক্সেস) একসাথে ঠিক করেছে।)
--
-- ফিক্স: owner সব সময়ের মতোই সব solver-এর সব থ্রেড দেখবে; admin সবসময়ের মতোই সব দেখবে; কিন্তু
-- "বর্তমান accepted_solver" ভিত্তিক ব্লানকেট এক্সেস বাদ দিয়ে সেটার জায়গায় প্রতিটা solver-এর জন্য
-- sender_id/receiver_id-ভিত্তিক থ্রেড-স্কোপড এক্সেস বসানো হলো -- প্রতিটা solver (বর্তমান বা আগের)
-- শুধু নিজের পাঠানো/পাওয়া মেসেজ-ই দেখবে/লাইভ ব্রডকাস্ট পাবে। messages_insert policy অপরিবর্তিত --
-- মেসেজ পাঠানো এখনো owner ও বর্তমান accepted_solver-এর জন্যই সীমাবদ্ধ, শুধু read/broadcast
-- visibility বদলাচ্ছে।
--
-- SQL-টেস্ট (নিয়ম #৮) পাস করেছে: সিন্থেটিক owner/old-solver/new-solver + দুইটা থ্রেড বানিয়ে
-- (একটা transaction-এ, শেষে rollback) `set local role authenticated; set local
-- request.jwt.claim.sub` দিয়ে সিমুলেশন -- ফিক্সের আগে: old=1, new=2 (bug, নতুন solver দুটোই
-- দেখছিল), owner=2। ফিক্সের পরে: old=1, new=1 (ঠিক), owner=2 (অপরিবর্তিত)।

-- 1) Table-level SELECT RLS: public.messages
drop policy if exists "messages_select" on public.messages;
create policy "messages_select"
on public.messages
for select
to authenticated
using (
  (select auth.uid()) = sender_id
  or (select auth.uid()) = receiver_id
  or is_admin((select auth.uid()))
  or exists (
    select 1 from public.problems p
    where p.id = messages.problem_id and p.user_id = (select auth.uid())
  )
);

-- 2) Broadcast RLS: realtime.messages, 'problem:<id>' topic -- উপরের table policy-র সাথে মিরর
-- করা (আগের broadcast policy-র accepted_solver_id শর্ত সরিয়ে sender/receiver-ভিত্তিক exists
-- বসানো হলো)।
drop policy if exists "problem participants can receive problem-topic broadcasts" on realtime.messages;
create policy "problem participants can receive problem-topic broadcasts"
on realtime.messages
for select
to authenticated
using (
  extension = 'broadcast'
  and (select realtime.topic()) like 'problem:%'
  and (
    is_admin((select auth.uid()))
    or exists (
      select 1 from public.problems p
      where ('problem:' || p.id) = (select realtime.topic())
        and (
          p.user_id = (select auth.uid())
          or exists (
            select 1 from public.messages m
            where m.problem_id = p.id
              and (m.sender_id = (select auth.uid()) or m.receiver_id = (select auth.uid()))
          )
        )
    )
  )
);
