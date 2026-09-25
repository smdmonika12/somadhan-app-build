-- ============================================================================
-- step12_8c_user_confirm_extra_amount_expected_amount_guard   [Step 12.8c — migration-ফাইল-সিঙ্ক, Step 12.12]
-- ============================================================================
-- এটা live Supabase প্রজেক্টে (mghvvpndkxnscwryfkib) ইতিমধ্যে apply-করা migration
-- `20260920225136 step12_8c_user_confirm_extra_amount_expected_amount_guard`-এর repo-কপি। live-ই সত্যের উৎস: নিচের SQL live pg_proc (MCP,
-- ২০২৬-০৯-২১) থেকে টেনে যাচাই করা — প্রতিটা ফাংশন-বডির md5(prosrc) live-এর সাথে বাইট-বাই-বাইট মিলেছে।
-- কী বদলায়: নতুন overload user_confirm_extra_amount(text,numeric) — p_expected_amount গার্ড; পুরনো ১-arg overload ইচ্ছাকৃতভাবে অক্ষত।
-- live md5(prosrc) user_confirm_extra_amount(text,numeric): c0a6e34f2cb89db7765ca2e53c22a5bd
-- নোট: PROPOSED ফাইলের বডিতে বাড়তি ব্যাখ্যা-কমেন্ট ছিল; live-এ apply-করা বডিতে সেগুলো নেই — এই ফাইলে live-এর বডিই আছে।
-- ⚠️ ফাইলের নামের `zz_<live-version>_` prefix ইচ্ছাকৃত: CI/setup script `ls supabase/migrations/*.sql | sort`
--    (অক্ষরক্রম) অনুযায়ী apply করে, timestamp অনুযায়ী না — `step12_*` নাম `step36_*`-এর আগে বসত, আর তখন
--    step36-এর পুরনো বডি এই বদলকে fresh-DB-তে overwrite করত। `zz_` সবার শেষে বসায়, live-এর ক্রম বজায় থাকে।
-- ⚠️ live-এ এটা আবার চালানো নিরাপদ (CREATE OR REPLACE / DROP IF EXISTS) — কিন্তু দরকার নেই, live আগেই apply-করা।
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

  if v_amt is distinct from p_expected_amount then
    return jsonb_build_object('result', 'AMOUNT_CHANGED');
  end if;

  select * into v_user from public.users where id = v_problem.user_id for update;

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
