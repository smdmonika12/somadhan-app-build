-- ============================================================================
-- step12_10_release_escrow_dispute_guard   [Step 12.10 — migration-ফাইল-সিঙ্ক, Step 12.12]
-- ============================================================================
-- এটা live Supabase প্রজেক্টে (mghvvpndkxnscwryfkib) ইতিমধ্যে apply-করা migration
-- `20260921062234 step12_10_release_escrow_dispute_guard`-এর repo-কপি। live-ই সত্যের উৎস: নিচের SQL live pg_proc (MCP,
-- ২০২৬-০৯-২১) থেকে টেনে যাচাই করা — প্রতিটা ফাংশন-বডির md5(prosrc) live-এর সাথে বাইট-বাই-বাইট মিলেছে।
-- কী বদলায়: release_escrow(text): (B) auth.uid() NULL হলে NOT_AUTHORIZED, (A) disputed problem-এ non-admin release হলে PROBLEM_DISPUTED।
-- live md5(prosrc) release_escrow(text): 23bcf6014e39b3ef9f749718d09e43e5
-- CREATE OR REPLACE-এ grants অপরিবর্তিত থাকে (live ACL: postgres/anon/authenticated/service_role EXECUTE)।
-- ⚠️ ফাইলের নামের `zz_<live-version>_` prefix ইচ্ছাকৃত: CI/setup script `ls supabase/migrations/*.sql | sort`
--    (অক্ষরক্রম) অনুযায়ী apply করে, timestamp অনুযায়ী না — `step12_*` নাম `step36_*`-এর আগে বসত, আর তখন
--    step36-এর পুরনো বডি এই বদলকে fresh-DB-তে overwrite করত। `zz_` সবার শেষে বসায়, live-এর ক্রম বজায় থাকে।
-- ⚠️ live-এ এটা আবার চালানো নিরাপদ (CREATE OR REPLACE / DROP IF EXISTS) — কিন্তু দরকার নেই, live আগেই apply-করা।
-- ============================================================================
CREATE OR REPLACE FUNCTION public.release_escrow(p_escrow_id text)
 RETURNS jsonb
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
declare
  v_escrow public.escrows%rowtype;
  v_problem public.problems%rowtype;
  v_commission_rate numeric(6,2);
  v_gross numeric(12,2);
  v_commission numeric(12,2);
  v_net numeric(12,2);
  v_trx_id text := 'TRX_RELEASE_' || p_escrow_id;
  v_now timestamptz := now();
  v_solver_has_role boolean;
begin
  -- [Step 12.10 — B] anon/unauthenticated (JWT ছাড়া) কলার আটকানো: নিচের auth-চেক auth.uid() NULL হলে NULL হয়ে পাশ হয়ে যেত।
  if auth.uid() is null then
    raise exception 'NOT_AUTHORIZED';
  end if;

  select * into v_escrow from public.escrows where id = p_escrow_id for update;
  if not found then
    raise exception 'ESCROW_NOT_FOUND';
  end if;

  select * into v_problem from public.problems where id = v_escrow.problem_id for update;

  if not (auth.uid() = v_escrow.user_id or public.is_admin(auth.uid())) then
    raise exception 'NOT_AUTHORIZED';
  end if;

  if v_escrow.status in ('RELEASED', 'REFUNDED') then
    return jsonb_build_object('result', 'ALREADY_TERMINAL', 'status', v_escrow.status);
  end if;

  if exists (select 1 from public.transactions where id = v_trx_id) then
    return jsonb_build_object('result', 'ALREADY_RELEASED');
  end if;

  -- [Step 12.10 — A] server-side dispute guard: problem disputed থাকলে শুধু admin release করতে পারবে।
  -- ALREADY_TERMINAL/ALREADY_RELEASED (idempotent) রিটার্ন ওপরে আগের মতোই আগে চলে।
  if coalesce(v_problem.is_disputed, false) and not public.is_admin(auth.uid()) then
    raise exception 'PROBLEM_DISPUTED';
  end if;

  select has_solver_role into v_solver_has_role from public.users where id = v_escrow.solver_id for update;
  if v_solver_has_role is null then
    raise exception 'SOLVER_NOT_FOUND';
  end if;
  if not v_solver_has_role then
    raise exception 'SOLVER_ROLE_INACTIVE';
  end if;

  v_gross := v_escrow.base_amount + v_escrow.extra_amount;
  v_commission_rate := coalesce(v_problem.applied_commission_rate,
    (select value::numeric from public.platform_settings where key = 'commission_percent'), 10.0);
  v_commission := round(v_gross * (v_commission_rate / 100.0), 2);
  v_net := v_gross - v_commission;

  update public.users set balance = balance + v_net, balance_solver = balance_solver + v_net, updated_at = v_now where id = v_escrow.solver_id;

  insert into public.transactions (id, problem_id, problem_title, user_id, solver_id, gross_amount,
    commission_percent, commission_amount, net_amount, base_amount, extra_amount, type, escrow_id,
    release_type, role, "timestamp")
  values (v_trx_id, v_escrow.problem_id, v_escrow.problem_title, v_escrow.user_id, v_escrow.solver_id,
    v_gross, v_commission_rate, v_commission, v_net, v_escrow.base_amount, v_escrow.extra_amount,
    'PAYMENT', p_escrow_id, 'FULL', 'SOLVER', v_now);

  update public.escrows set status = 'RELEASED', released_at = v_now, updated_at = v_now where id = p_escrow_id;

  update public.problems set status = 'COMPLETED', completed_at = v_now, last_activity_at = v_now
  where id = v_escrow.problem_id;

  insert into public.notifications (id, user_id, title, message, target_type, target_id, role, "timestamp")
  values ('NOTIF_' || replace(gen_random_uuid()::text, '-', ''), v_escrow.solver_id,
    'পেমেন্ট প্রকাশিত হয়েছে', 'আপনার ৳' || v_net::text || ' পেমেন্ট আপনার ব্যালেন্সে যোগ হয়েছে।', 'balance', v_escrow.solver_id, 'SOLVER', v_now);

  return jsonb_build_object('result', 'OK', 'net_amount', v_net, 'commission', v_commission);
end;
$function$;
