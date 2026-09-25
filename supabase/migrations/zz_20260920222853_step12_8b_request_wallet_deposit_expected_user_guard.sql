-- ============================================================================
-- step12_8b_request_wallet_deposit_expected_user_guard   [Step 12.8b — migration-ফাইল-সিঙ্ক, Step 12.12]
-- ============================================================================
-- এটা live Supabase প্রজেক্টে (mghvvpndkxnscwryfkib) ইতিমধ্যে apply-করা migration
-- `20260920222853 step12_8b_request_wallet_deposit_expected_user_guard`-এর repo-কপি। live-ই সত্যের উৎস: নিচের SQL live pg_proc (MCP,
-- ২০২৬-০৯-২১) থেকে টেনে যাচাই করা — প্রতিটা ফাংশন-বডির md5(prosrc) live-এর সাথে বাইট-বাই-বাইট মিলেছে।
-- কী বদলায়: নতুন overload request_wallet_deposit(numeric,text,text,uuid,text,text,text) — p_expected_user_id গার্ড (replay-session ভিন্ন হলে NOT_AUTHORIZED); পুরনো ৬-arg overload ইচ্ছাকৃতভাবে অক্ষত।
-- live md5(prosrc) request_wallet_deposit(…uuid…): fb385c76163a3db45533ae1d1332f7d6
-- ⚠️ ফাইলের নামের `zz_<live-version>_` prefix ইচ্ছাকৃত: CI/setup script `ls supabase/migrations/*.sql | sort`
--    (অক্ষরক্রম) অনুযায়ী apply করে, timestamp অনুযায়ী না — `step12_*` নাম `step36_*`-এর আগে বসত, আর তখন
--    step36-এর পুরনো বডি এই বদলকে fresh-DB-তে overwrite করত। `zz_` সবার শেষে বসায়, live-এর ক্রম বজায় থাকে।
-- ⚠️ live-এ এটা আবার চালানো নিরাপদ (CREATE OR REPLACE / DROP IF EXISTS) — কিন্তু দরকার নেই, live আগেই apply-করা।
-- ============================================================================
CREATE OR REPLACE FUNCTION public.request_wallet_deposit(p_amount numeric, p_gateway text, p_gateway_trx_id text, p_expected_user_id uuid, p_sender_phone text DEFAULT ''::text, p_note text DEFAULT ''::text, p_role text DEFAULT 'USER'::text)
 RETURNS jsonb
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
declare
    v_acct public.users%rowtype;
    v_role_active boolean;
    v_payment_id text := 'GWPAY_' || replace(gen_random_uuid()::text, '-', '');
    v_now timestamptz := now();
    v_auto_approve boolean := coalesce(
      (select value = 'true' from public.platform_settings where key = 'gateway_auto_approve_deposits'), true
    );
begin
    -- [GUARD-A, Step 12.8b] outbox-replay safety: caller session must be the original depositor.
    -- Non-OK result (not an exception) so the client's "do not enqueue non-OK" rule matches.
    if auth.uid() is null or p_expected_user_id is null or auth.uid() <> p_expected_user_id then
        return jsonb_build_object('result', 'NOT_AUTHORIZED');
    end if;

    if p_role not in ('USER', 'SOLVER') then
        raise exception 'INVALID_ROLE';
    end if;
    if p_amount <= 0 then
        raise exception 'INVALID_AMOUNT';
    end if;
    if p_gateway not in ('BKASH','NAGAD','ROCKET','CARD') then
        raise exception 'INVALID_GATEWAY';
    end if;
    if p_gateway_trx_id is null or p_gateway_trx_id = '' then
        raise exception 'TRX_ID_REQUIRED';
    end if;
    -- [GUARD-B, Step 12.8b] serialize concurrent calls with the same gateway_trx_id so the exists-guard below
    -- returns ALREADY_SUBMITTED instead of a unique_violation (constraint gateway_payments_gateway_trx_id_key is the backstop).
    perform pg_advisory_xact_lock(hashtextextended('request_wallet_deposit:' || p_gateway_trx_id, 0));
    if exists (select 1 from public.gateway_payments where gateway_trx_id = p_gateway_trx_id) then
        return jsonb_build_object('result', 'ALREADY_SUBMITTED');
    end if;

    select * into v_acct from public.users where id = auth.uid();
    if not found then raise exception 'USER_NOT_FOUND'; end if;

    if p_role = 'SOLVER' then
      v_role_active := v_acct.has_solver_role;
    else
      v_role_active := v_acct.has_user_role;
    end if;
    if not coalesce(v_role_active, false) then
      raise exception 'ROLE_INACTIVE';
    end if;

    if v_auto_approve then
        insert into public.gateway_payments (id, gateway_trx_id, user_id, amount, gateway, purpose, status, note, role, "timestamp")
        values (v_payment_id, p_gateway_trx_id, auth.uid(), p_amount, p_gateway, 'WALLET_DEPOSIT', 'SUCCESS',
            coalesce(nullif(p_note, ''), 'ওয়ালেট ব্যালেন্স রিচার্জ (টপ-আপ)') ||
            case when p_sender_phone <> '' then ' | প্রেরকের নম্বর: ' || p_sender_phone else '' end,
            p_role, v_now);

        if p_role = 'SOLVER' then
          update public.users set balance = balance + p_amount, balance_solver = balance_solver + p_amount, updated_at = v_now where id = auth.uid();
        else
          update public.users set balance = balance + p_amount, balance_user = balance_user + p_amount, updated_at = v_now where id = auth.uid();
        end if;

        insert into public.transactions (id, problem_id, problem_title, user_id, gross_amount, net_amount,
            base_amount, type, escrow_id, role, "timestamp")
        values ('TRX_DEP_' || v_payment_id, '', 'ওয়ালেট রিচার্জ (' || p_gateway || ')',
            auth.uid(), p_amount, p_amount, p_amount, 'WALLET_DEPOSIT', v_payment_id, p_role, v_now);

        insert into public.notifications (id, user_id, title, message, target_type, target_id, role, "timestamp")
        values ('NOTIF_' || replace(gen_random_uuid()::text, '-', ''), auth.uid(),
            'রিচার্জ সফল', '৳' || p_amount::text || ' আপনার ব্যালেন্সে যোগ হয়েছে।', 'balance', v_payment_id, p_role, v_now);

        return jsonb_build_object('result', 'OK', 'payment_id', v_payment_id);
    else
        insert into public.gateway_payments (id, gateway_trx_id, user_id, amount, gateway, purpose, status, note, role, "timestamp")
        values (v_payment_id, p_gateway_trx_id, auth.uid(), p_amount, p_gateway, 'WALLET_DEPOSIT', 'PENDING',
            coalesce(nullif(p_note, ''), 'ওয়ালেট রিচার্জ অনুরোধ — অ্যাডমিন যাচাইয়ের অপেক্ষায়') ||
            case when p_sender_phone <> '' then ' | প্রেরকের নম্বর: ' || p_sender_phone else '' end,
            p_role, v_now);

        insert into public.notifications (id, user_id, title, message, target_type, target_id, role, "timestamp")
        values ('NOTIF_' || replace(gen_random_uuid()::text, '-', ''), auth.uid(),
            'রিচার্জ অনুরোধ জমা হয়েছে',
            '৳' || p_amount::text || ' রিচার্জের অনুরোধ জমা হয়েছে, অ্যাডমিন যাচাই করার পর ব্যালেন্সে যোগ হবে।',
            'balance', v_payment_id, p_role, v_now);

        return jsonb_build_object('result', 'PENDING_APPROVAL', 'payment_id', v_payment_id);
    end if;
end;
$function$;

REVOKE ALL ON FUNCTION public.request_wallet_deposit(numeric, text, text, uuid, text, text, text) FROM PUBLIC;
REVOKE ALL ON FUNCTION public.request_wallet_deposit(numeric, text, text, uuid, text, text, text) FROM anon;
GRANT EXECUTE ON FUNCTION public.request_wallet_deposit(numeric, text, text, uuid, text, text, text) TO authenticated;
GRANT EXECUTE ON FUNCTION public.request_wallet_deposit(numeric, text, text, uuid, text, text, text) TO service_role;
