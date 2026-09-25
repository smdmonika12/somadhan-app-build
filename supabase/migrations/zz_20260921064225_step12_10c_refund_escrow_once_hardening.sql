-- ============================================================================
-- step12_10c_refund_escrow_once_hardening   [Step 12.10c — migration-ফাইল-সিঙ্ক, Step 12.12]
-- ============================================================================
-- এটা live Supabase প্রজেক্টে (mghvvpndkxnscwryfkib) ইতিমধ্যে apply-করা migration
-- `20260921064225 step12_10c_refund_escrow_once_hardening`-এর repo-কপি। live-ই সত্যের উৎস: নিচের SQL live pg_proc (MCP,
-- ২০২৬-০৯-২১) থেকে টেনে যাচাই করা — প্রতিটা ফাংশন-বডির md5(prosrc) live-এর সাথে বাইট-বাই-বাইট মিলেছে।
-- কী বদলায়: refund_escrow_once(text,text,numeric): (B) auth.uid() NULL হলে NOT_AUTHORIZED, (C) refund-শতাংশ ০–১০০-র বাইরে হলে INVALID_REFUND_PERCENTAGE।
-- live md5(prosrc) refund_escrow_once(text,text,numeric): 69e5dd400a26d63c6423343ad399a882
-- CREATE OR REPLACE-এ grants অপরিবর্তিত থাকে (live ACL: postgres/anon/authenticated/service_role EXECUTE)।
-- ⚠️ ফাইলের নামের `zz_<live-version>_` prefix ইচ্ছাকৃত: CI/setup script `ls supabase/migrations/*.sql | sort`
--    (অক্ষরক্রম) অনুযায়ী apply করে, timestamp অনুযায়ী না — `step12_*` নাম `step36_*`-এর আগে বসত, আর তখন
--    step36-এর পুরনো বডি এই বদলকে fresh-DB-তে overwrite করত। `zz_` সবার শেষে বসায়, live-এর ক্রম বজায় থাকে।
-- ⚠️ live-এ এটা আবার চালানো নিরাপদ (CREATE OR REPLACE / DROP IF EXISTS) — কিন্তু দরকার নেই, live আগেই apply-করা।
-- ============================================================================
CREATE OR REPLACE FUNCTION public.refund_escrow_once(p_escrow_id text, p_refund_type text DEFAULT 'SOLVER_CANCEL'::text, p_refund_percentage numeric DEFAULT 100)
 RETURNS jsonb
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
declare
  v_escrow public.escrows%rowtype;
  v_amount numeric(12,2);
  v_trx_id text := 'TRX_REFUND_' || p_escrow_id;
  v_now timestamptz := now();
  v_user_has_role boolean;
begin
  -- [Step 12.10c — B] JWT ছাড়া (anon) কলার আটকানো
  if auth.uid() is null then
    raise exception 'NOT_AUTHORIZED';
  end if;
  -- [Step 12.10c — C] refund-শতাংশ ০–১০০-র বাইরে হলে বাতিল
  if p_refund_percentage is null or p_refund_percentage < 0 or p_refund_percentage > 100 then
    raise exception 'INVALID_REFUND_PERCENTAGE';
  end if;

  select * into v_escrow from public.escrows where id = p_escrow_id for update;
  if not found then
    raise exception 'ESCROW_NOT_FOUND';
  end if;

  if not (auth.uid() = v_escrow.user_id or auth.uid() = v_escrow.solver_id or public.is_admin(auth.uid())) then
    raise exception 'NOT_AUTHORIZED';
  end if;

  if v_escrow.status in ('RELEASED', 'REFUNDED') then
    return jsonb_build_object('result', 'ALREADY_TERMINAL', 'status', v_escrow.status);
  end if;

  if exists (select 1 from public.transactions where id = v_trx_id) then
    return jsonb_build_object('result', 'ALREADY_REFUNDED');
  end if;

  select has_user_role into v_user_has_role from public.users where id = v_escrow.user_id for update;
  if v_user_has_role is null then
    raise exception 'USER_NOT_FOUND';
  end if;
  if not v_user_has_role then
    raise exception 'USER_ROLE_INACTIVE';
  end if;

  v_amount := round((v_escrow.base_amount + v_escrow.extra_amount) * (p_refund_percentage / 100.0), 2);

  update public.users set balance = balance + v_amount, balance_user = balance_user + v_amount, updated_at = v_now where id = v_escrow.user_id;

  insert into public.transactions (id, problem_id, problem_title, user_id, solver_id, gross_amount,
    net_amount, type, escrow_id, refund_type, refund_percentage, role, "timestamp")
  values (v_trx_id, v_escrow.problem_id, v_escrow.problem_title, v_escrow.user_id, v_escrow.solver_id,
    v_amount, v_amount, 'REFUND', p_escrow_id, p_refund_type, p_refund_percentage, 'USER', v_now);

  update public.escrows set status = 'REFUNDED', released_at = v_now, updated_at = v_now where id = p_escrow_id;

  insert into public.notifications (id, user_id, title, message, target_type, target_id, role, "timestamp")
  values ('NOTIF_' || replace(gen_random_uuid()::text, '-', ''), v_escrow.user_id,
    'রিফান্ড সম্পন্ন', 'আপনার ৳' || v_amount::text || ' রিফান্ড করা হয়েছে।', 'balance', v_escrow.user_id, 'USER', v_now);

  return jsonb_build_object('result', 'OK', 'amount', v_amount);
end;
$function$;
