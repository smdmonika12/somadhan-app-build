-- ধাপ ২৩ (ফলো-আপ, ব্যবহারকারীর স্পষ্ট নির্দেশে) — bids_select RLS policy বিস্তৃতকরণ
--
-- প্রেক্ষাপট: এই session-এ Supabase MCP দিয়ে যাচাই করে পাওয়া গিয়েছিল যে ProblemDetailScreen.kt-এর
-- বিড-লিস্ট (যেটা Firebase আমলে যেকোনো visitor -- শুধু owner/bidder না -- দেখতে পেতো) RLS-এর
-- কারণে Supabase পাশে third-party visitor-দের জন্য কাজ করতো না (bids_select আগে শুধু নিজের বিড/
-- admin/পোস্টদাতাকে অনুমতি দিতো)। ব্যবহারকারী স্পষ্টভাবে জানিয়েছেন Firebase-এর মতোই সম্পূর্ণ
-- ফাংশনালিটি চান -- তাই এই মাইগ্রেশন সেই gap বন্ধ করে।
--
-- এই session-এ এটা ইতিমধ্যে Supabase:apply_migration দিয়ে সরাসরি apply করা হয়েছে এবং
-- apply-পরবর্তী verify-query (pg_policies) দিয়ে নিশ্চিত করা হয়েছে। এই ফাইলটা ইতিহাস/
-- রোলব্যাক-রেফারেন্সের জন্য (নিয়ম #১৩)।
--
-- নতুন শর্ত: problems_select policy-র "যেকোনো ভিজিটর" ক্লজের (status='OPEN' AND is_public=true
-- AND is_user_deleted=false) সাথে হুবহু সঙ্গতিপূর্ণ একটা ৪র্থ OR-শর্ত bids_select-এ যোগ করা
-- হলো -- ফলে কোনো সমস্যা এখনো OPEN+public+non-deleted থাকা অবস্থায় তার সব বিড (টাকার অংক,
-- সলভার পরিচয়সহ) যেকোনো authenticated/anon ভিজিটরের কাছে দৃশ্যমান, ঠিক Firebase-আমলের মতোই।
-- এর ফলে SupabaseRealtimeManager-এর গ্লোবাল bids channel (ধাপ ২০-২২, ইতিমধ্যে প্রতিটা
-- session-এ চালু) এখন থেকে এই ভিজিটরদের জন্যও লাইভ বিড Room-এ upsert করবে -- কোনো নতুন
-- Kotlin কোড ছাড়াই (ProblemDetailScreen.kt-এর `viewModel.problemBids` ইতিমধ্যে সেই একই Room
-- টেবিল থেকে read করে)।
alter policy bids_select on public.bids
  using (
    (auth.uid() = solver_id)
    or is_admin(auth.uid())
    or (exists (select 1 from public.problems p where p.id = bids.problem_id and p.user_id = auth.uid()))
    or (exists (
      select 1 from public.problems p
      where p.id = bids.problem_id
        and p.status = 'OPEN'
        and p.is_public = true
        and p.is_user_deleted = false
    ))
  );
