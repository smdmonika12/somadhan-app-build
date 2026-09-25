-- ============================================================================
-- 🟡 PROPOSED (Step 12.8c) — user_confirm_extra_amount: expected-amount replay guard (নতুন overload)
-- ============================================================================
-- এখনো APPLY করা হয়নি। ব্যবহারকারীর review + সম্মতির পর Supabase MCP দিয়ে (rule #13) অথবা
-- আপনার নিজের migration flow দিয়ে apply করুন, তারপর Claude-কে জানালে retry/dispatcher-branch যোগ
-- করা হবে (12.8b-র হুবহু ক্রম)।
--
-- 🔍 পূর্ব-যাচাই (Supabase MCP, read-only, ২০২৬-০৯-২১, প্রজেক্ট mghvvpndkxnscwryfkib) — গুরুত্বপূর্ণ পার্থক্য:
--   (১) live বডি zip-এর supabase/migrations/step36_...sql-এর ভার্সনের সাথে মেলেনি (ঠিক deposit-এর
--       12.8b-তে যা ঘটেছিল, সেরকমই) — live-এ একটা পরের "ধাপ ৩৯" ফিক্স আছে যা deduction হিসাব করে
--       role-scoped `balance_user` থেকে (`least(greatest(v_user.balance_user, 0), v_amt)`), legacy
--       shared `balance` থেকে না। নিচের বডি **live prosrc থেকে** নেওয়া, live-এর সাথে diff = শুধু
--       GUARD (২ নতুন লাইন) + নতুন প্যারামিটার p_expected_amount।
--   (২) বর্তমান signature একটাই: `user_confirm_extra_amount(p_problem_id text)`। কোনো ambiguous
--       overload নেই (Step 12.9-এর ৭টা ফাংশনের তালিকায় এটা নেই)।
--   (৩) বর্তমান grants: PUBLIC, postgres, anon, authenticated, service_role (সবাই EXECUTE) — RPC
--       নিজেই `auth.uid() <> v_problem.user_id` চেক করে, তাই anon grant থাকলেও ঝুঁকি নেই। নতুন
--       overload-এ (নিচে) ইচ্ছাকৃতভাবে **আরো টাইট** grants (শুধু authenticated + service_role) —
--       12.8b-র request_wallet_deposit-এর নতুন overload-এর সাথে সঙ্গতিপূর্ণ, যেহেতু এই overload
--       শুধু outbox-replay path-এর জন্য বানানো হচ্ছে।
--
-- ডিজাইন: 12.8b-এর হুবহু প্যাটার্ন — পুরনো ১-প্যারামিটার signature **DROP করা হয়নি** (Step 12.9-এর
-- duplicate-overload কাজের সাথে সংঘর্ষ এড়াতে); বদলে p_expected_amount সহ একটা **নতুন, distinct-arity
-- overload** যোগ হলো। p_expected_amount-এর কোনো DEFAULT নেই, তাই:
--   - normal (online) path client শুধু p_problem_id পাঠালে পুরনো ১-প্যারামিটার overload-ই resolve
--     হবে (আজকের আচরণ অপরিবর্তিত, কোনো ambiguity নেই — deposit-এর মতোই)।
--   - retry path (OutboxRpcDispatcher) দুটোই পাঠালে এই নতুন overload resolve হবে, guard সক্রিয় হবে।
--
-- GUARD উদ্দেশ্য: retry উইন্ডোর মধ্যে যদি সলভার নতুন/ভিন্ন অঙ্কের additional charge রিকোয়েস্ট করে
-- (পুরনো pending_extra_amount confirm হওয়ার পর, নতুন একটা raise হয়ে যায়), তাহলে stale replay যেন
-- owner-এর অজান্তে ভিন্ন (সাধারণত ভুল) অঙ্ক ওয়ালেট থেকে কেটে না নেয় — non-OK result (`AMOUNT_CHANGED`),
-- exception না, যাতে `enqueueOutboxRetry`-র "non-OK enqueue করি না" নিয়মের সাথে মেলে।
--
-- STAGING যাচাই (apply-এর পর, সুপারিশ — এখনো করা হয়নি):
--   ১. একটা টেস্ট problem-এ pending_extra_amount সেট করে সঠিক amount দিয়ে কল করুন → `result: OK`।
--   ২. একই problem-এ (বা নতুন pending সহ অন্য একটাতে) ভুল/পুরনো amount দিয়ে কল করুন → `result: AMOUNT_CHANGED`,
--      কোনো balance/escrow/transaction পরিবর্তন হয়নি সেটা যাচাই করুন।
--   ৩. পুরনো ১-arg কল (p_expected_amount ছাড়া) এখনো আগের মতোই কাজ করছে সেটা যাচাই করুন (regression)।
--
-- ROLLBACK:
--   drop function if exists public.user_confirm_extra_amount(text, numeric);
--   (পুরনো ১-arg overload অস্পৃশ্য থাকবে, rollback-এ কিছু করার দরকার নেই)
-- ============================================================================

CREATE OR REPLACE FUNCTION public.user_confirm_extra_amount(p_problem_id text, p_expected_amount numeric)
 RETURNS jsonb
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
declare
  v_problem public.problems%rowtype;
  v_user public.users%rowtype;
  v_escrow public.escrows%rowtype;
  v_now timestamptz := now();
  v_amt numeric;
  v_wallet_deduction numeric(12,2) := 0;
begin
  select * into v_problem from public.problems where id = p_problem_id for update;
  if not found then raise exception 'PROBLEM_NOT_FOUND'; end if;
  if auth.uid() <> v_problem.user_id then raise exception 'NOT_AUTHORIZED'; end if;
  if v_problem.pending_extra_amount is null then
    return jsonb_build_object('result', 'NOT_PENDING');
  end if;
  v_amt := v_problem.pending_extra_amount;

  -- [GUARD, Step 12.8c] outbox-replay safety: retry উইন্ডোর মধ্যে pending amount বদলে গেলে
  -- (নতুন additional-charge request) replay যেন ভিন্ন/ভুল অঙ্ক ওয়ালেট থেকে না কাটে।
  -- Non-OK result (exception না) — enqueueOutboxRetry-র non-OK-enqueue-করি-না নিয়মের সাথে মেলে।
  if v_amt is distinct from p_expected_amount then
    return jsonb_build_object('result', 'AMOUNT_CHANGED');
  end if;

  select * into v_user from public.users where id = v_problem.user_id for update;

  -- [ধাপ ৩৯ ফিক্স, live prosrc থেকে অপরিবর্তিত রাখা হলো] deduction হিসাব role-scoped
  -- balance_user থেকে, legacy shared balance থেকে না।
  v_wallet_deduction := least(greatest(v_user.balance_user, 0), v_amt);
  if v_wallet_deduction > 0 then
    update public.users set balance = balance - v_wallet_deduction, balance_user = balance_user - v_wallet_deduction, updated_at = v_now where id = v_user.id;
    insert into public.transactions (id, problem_id, problem_title, user_id, gross_amount, net_amount, type, role, "timestamp")
    values ('TRX_CONFIRM_EXTRA_' || replace(gen_random_uuid()::text, '-', ''), v_problem.id, v_problem.title, v_user.id,
      v_wallet_deduction, -v_wallet_deduction, 'EXTRA_CHARGE_DEDUCTION', 'USER', v_now);
  end if;

  select * into v_escrow from public.escrows where problem_id = v_problem.id and status = 'HELD'
    order by created_at desc limit 1 for update;
  if found then
    update public.escrows set extra_amount = extra_amount + v_amt, updated_at = v_now where id = v_escrow.id;
  end if;

  update public.problems set
    confirmed_extra_amount_total = confirmed_extra_amount_total + v_amt,
    pending_extra_amount = null,
    pending_extra_amount_note = null,
    pending_extra_amount_requested_at = null,
    last_activity_at = v_now
  where id = p_problem_id;

  if v_problem.accepted_solver_id is not null then
    insert into public.notifications (id, user_id, title, message, target_type, target_id, related_problem_id, "timestamp")
    values ('NOTIF_' || replace(gen_random_uuid()::text, '-', ''), v_problem.accepted_solver_id,
      'অতিরিক্ত বিল অনুমোদিত ✅',
      'ক্লায়েন্ট আপনার ৳' || trim(to_char(v_amt, 'FM999999999')) || ' অতিরিক্ত বিলের অনুরোধ অনুমোদন করেছেন।',
      'problem', p_problem_id, p_problem_id, v_now);
  end if;

  return jsonb_build_object('result', 'OK', 'confirmed_amount', v_amt, 'wallet_deduction', v_wallet_deduction);
end;
$function$;

REVOKE ALL ON FUNCTION public.user_confirm_extra_amount(text, numeric) FROM PUBLIC;
REVOKE ALL ON FUNCTION public.user_confirm_extra_amount(text, numeric) FROM anon;
GRANT EXECUTE ON FUNCTION public.user_confirm_extra_amount(text, numeric) TO authenticated;
GRANT EXECUTE ON FUNCTION public.user_confirm_extra_amount(text, numeric) TO service_role;
