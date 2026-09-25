-- সমাধান: cancelled/withdrawn/rejected solver যেন নিজের "বাতিল" ট্যাবে এই problem-টা
-- cancelled হিসেবে দেখতে পারে এবং পোস্ট খুললে বেসিক তথ্য দেখতে পারে।
-- realtime broadcast/postgres_changes কিছুই বদলানো হয়নি -- শুধু plain SELECT RLS, তাই
-- এই পোস্টে নতুন কোনো live update আসবে না (problems টেবিল broadcast-এ migrate করা হয়নি,
-- আর normal-user session-এ table-wide postgres_changes listener কখনো চালু হয় না --
-- শুধু periodic throttled bulk-pull-এই এই সলভার এখন এই row-টা fetch করতে পারবে)।

alter policy problems_select on public.problems
using (
  (auth.uid() = user_id)
  OR (auth.uid() = accepted_solver_id)
  OR is_admin(auth.uid())
  OR ((status = 'OPEN'::text) AND (is_public = true) AND (is_user_deleted = false))
  OR EXISTS (
    SELECT 1 FROM public.bids b
    WHERE b.problem_id = problems.id
      AND b.solver_id = auth.uid()
      AND b.status IN ('CANCELLED', 'WITHDRAWN', 'REJECTED')
  )
);
