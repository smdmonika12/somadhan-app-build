-- ============================================================================
-- PROPOSED (Step 12.10) — release_escrow-এ server-side dispute guard  [পুনর্লিখিত: LIVE বডির ওপর, ২০২৬-০৯-২১]
-- ============================================================================
-- ⚠️ এটা **প্রস্তাব**। `supabase/migrations/`-এ নেই; কেউ এটা স্বয়ংক্রিয়ভাবে apply করেনি (এই সেশনেও না)।
--    live টাকার ফাংশন বদলায় — নিচের "APPLY-এর আগে" ও "APPLY-এর পরে" কুয়েরি দুটো চালিয়ে নাও।
--
-- 🔴 কেন পুনর্লিখন (আগের সংস্করণ step36-এর বডির ওপর ছিল): এই সেশনে Supabase MCP (read-only SELECT) দিয়ে
--    live `release_escrow(text)`-এর আসল `prosrc` টানা হয়েছে (length 2909, md5 443500c96c61b79e528606841f221593)
--    ও step36-এর বডির (length 3513) সাথে diff করা হয়েছে। live ≠ step36। পার্থক্য (পুরোটাই):
--      (১) step36-এর কমেন্টগুলো live-এ নেই (শুধু কমেন্ট, আচরণ একই);
--      (২) ⚠️ live-এর `notifications` insert-এ অতিরিক্ত `role` কলাম + মান `'SOLVER'` আছে — step36-এ নেই।
--    আগের (step36-ভিত্তিক) প্রস্তাব apply করলে (২) নিঃশব্দে হারিয়ে যেত: solver-এর "পেমেন্ট প্রকাশিত হয়েছে"
--    notification-এর role '' হয়ে যেত (role-scoped notification-ফিল্টারিং ভাঙত)। এই সংস্করণ live বডি
--    *হুবহু* রেখে তার ওপর শুধু নিচের A (ও ঐচ্ছিক B) *যোগ* করে — কিছু বাদ/বদল নেই (প্রোগ্রাম দিয়ে যাচাই:
--    যোগ-করা ব্লকগুলো সরালে বডি live-এর সাথে বাইট-বাই-বাইট মেলে)।
--
-- 🅰️ A — dispute guard (Step 12.10-এর মূল কাজ): problem `is_disputed = true` থাকলে non-admin কলার
--    `PROBLEM_DISPUTED` exception পায়। admin আগের মতোই পারে। idempotent রিটার্ন (`ALREADY_TERMINAL`,
--    `ALREADY_RELEASED`) guard-এর আগে — retry-তে অপ্রভাবিত। `resolve_dispute_split` disputed-flag `true` রেখে
--    escrow সরাসরি RELEASED করে (terminal) — তাই পরে আসা কোনো release `ALREADY_TERMINAL` পায়, guard-এ পৌঁছায় না।
--
-- 🅱️ B — ঐচ্ছিক, আলাদা সিদ্ধান্ত (নতুন আবিষ্কার, এই সেশনে): live-এ `release_escrow`-এর auth-চেক
--    `if not (auth.uid() = v_escrow.user_id or public.is_admin(auth.uid()))` — auth.uid() NULL (anon, JWT ছাড়া)
--    হলে এটা NULL হয়, `if NULL` raise করে না, তাই চেক পাশ হয়ে যায়; আর `anon`-এর EXECUTE grant আছে।
--    (যাচাই: `select not (null::uuid = gen_random_uuid() or public.is_admin(null::uuid))` → NULL। `release_escrow`
--    নিজে কল করা হয়নি।) মানে anon key দিয়ে কেউ escrow-id জানলে টাকা ছাড়াতে পারার সম্ভাবনা। B সেটা বন্ধ করে।
--    আচরণ-পরিবর্তন: JWT-ছাড়া কল (SQL editor/`postgres` role/service_role থেকে সরাসরি) এখন NOT_AUTHORIZED পাবে।
--    live-এ যাচাইকৃত: cron job নেই, non-public ফাংশন কলার নেই, 3টা SQL কলারই admin-চেক করে (auth.uid() non-null),
--    Edge Function (`admin-reset-user-password`) এটা কল করে না। B না চাইলে ▼▼[B]…▲▲ ব্লক মুছে apply করো।
--    (একই NULL-প্রবণ প্যাটার্ন `refund_escrow_once` [anon EXECUTE ✅] ও `deposit_money_via_gateway` [anon ❌]-তেও
--    আছে — এই ফাইলের scope-এর বাইরে, progress doc-এ আলাদা করে নথিভুক্ত।)
--
-- ⚠️ CREATE OR REPLACE-এ grants অপরিবর্তিত থাকে (live: postgres/anon/authenticated/service_role EXECUTE)।
-- ⚠️ Outbox প্রভাব (বিস্তারিত progress doc): client `payoutEscrowToSolver` RPC-র *আগেই* local-এ solver-কে credit
--    ও escrow RELEASED করে; RPC exception হলে শুধু log + `enqueueOutboxRetry`। PROBLEM_DISPUTED/NOT_AUTHORIZED
--    deterministic — retry কখনো সফল হবে না; worker `MAX_RETRY_COUNT = 10`-এর পর FAILED_PERMANENT করে (অসীম loop নয়)।
--    client-সাইড অবস্থা (যাচাইকৃত): repository-র choke point `confirmReleaseAndComplete()`-এ নিজস্ব `isDisputed`
--    চেক নেই (শুধু problem.status == COMPLETED ও escrow-status গার্ড); 48-ঘণ্টা auto-release sweep `!prob.isDisputed`
--    ফিল্টার করে (SomadhanRepository.kt:5897); UI-তে `isDisputeActive` গণনা হয় (SomadhanViewModel.kt:770, 1904) —
--    confirm-বাটন সেটার ওপর নির্ভর করে কিনা যাচাই করা হয়নি। অর্থাৎ আজ আটকানোটা UI/sweep-স্তরে, repository-স্তরে না।
--
-- ROLLBACK: নিচের `CREATE OR REPLACE`-এর বডিতে A ও B ব্লক মুছে আবার `create or replace` করো
--   (= live আগের বডি; md5 443500c96c61b79e528606841f221593), অথবা এই ফাইলের ভেতরের বডি থেকে ব্লকগুলো বাদ দাও।
--
-- ---------------------------------------------------------------------------
-- APPLY-এর আগে (read-only) — live বডি এখনো ওই বডিই কিনা (মাঝখানে কেউ বদলালে এই ফাইল আর মিলবে না):
--   select md5(prosrc) as md5, length(prosrc) as len, (prosrc ilike '%PROBLEM_DISPUTED%') as already_guarded
--   from pg_proc where oid = 'public.release_escrow(text)'::regprocedure;
--   -- আশা: md5 = 443500c96c61b79e528606841f221593, len = 2909, already_guarded = false
--   -- না মিললে apply করো না — বডি আবার টেনে diff করে এই ফাইল নতুন বডির ওপর বসাও।
--
-- APPLY-এর পরে (read-only):
--   select (prosrc ilike '%PROBLEM_DISPUTED%') as guard_a, (prosrc ilike '%auth.uid() is null%') as guard_b,
--          (prosrc ilike '%target_id, role, "timestamp")%') as notif_role_kept
--   from pg_proc where oid = 'public.release_escrow(text)'::regprocedure;
--   -- আশা: guard_a = true, guard_b = true (B রাখলে), notif_role_kept = true
--
-- বিহেভিয়ার-যাচাই — একমাত্র Supabase প্রজেক্ট `somadhan` (mghvvpndkxnscwryfkib) = production; আলাদা staging নেই।
-- তাই নিচের প্রতিটা টেস্ট DO-ব্লকে, যেটা *সবসময়* শেষে exception ছুঁড়ে পুরো ট্রানজ্যাকশন rollback করে —
-- ফাংশন যা-ই করুক (এমনকি guard না থাকলে টাকা ছেড়ে দিলেও) কিছুই commit হয় না; ফল দেখা যায় error-বার্তায়।
-- <...> জায়গাগুলো একটা আসল disputed problem ও তার HELD escrow-র id/owner-uuid দিয়ে বদলাও:
--
--   -- টেস্ট ১: owner + disputed → আশা: EXCEPTION: PROBLEM_DISPUTED
--   do $t$ declare v text; begin
--     perform set_config('request.jwt.claims', json_build_object('sub','<owner-uuid>')::text, true);
--     begin v := public.release_escrow('<escrow-id>')::text; exception when others then v := 'EXCEPTION: '||sqlerrm; end;
--     raise exception 'TEST1 (সব rollback হয়েছে): %', v;
--   end $t$;
--
--   -- টেস্ট ২: admin + একই escrow → আশা: {"result": "OK", ...} (বা ALREADY_TERMINAL) — rollback হবে, টাকা নড়বে না
--   do $t$ declare v text; begin
--     perform set_config('request.jwt.claims', json_build_object('sub','<admin-uuid>')::text, true);
--     begin v := public.release_escrow('<escrow-id>')::text; exception when others then v := 'EXCEPTION: '||sqlerrm; end;
--     raise exception 'TEST2 (সব rollback হয়েছে): %', v;
--   end $t$;
--
--   -- টেস্ট ৩: owner + disputed নয় এমন problem-র HELD escrow → আশা: {"result": "OK", ...} (rollback হবে)
--   -- (একই DO-ব্লক, <owner-uuid>/<escrow-id> বদলে)
--
--   -- টেস্ট ৪ (শুধু B রাখলে): JWT ছাড়া → আশা: EXCEPTION: NOT_AUTHORIZED
--   do $t$ declare v text; begin
--     perform set_config('request.jwt.claims', '', true);
--     begin v := public.release_escrow('<escrow-id>')::text; exception when others then v := 'EXCEPTION: '||sqlerrm; end;
--     raise exception 'TEST4 (সব rollback হয়েছে): %', v;
--   end $t$;
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
  -- ▼▼ [Step 12.10 — B, ঐচ্ছিক নিরাপত্তা-হার্ডেনিং] anon/unauthenticated কলার আটকানো ▼▼
  -- live-এ যাচাইকৃত: নিচের `not (auth.uid() = ... or is_admin(auth.uid()))` চেকটা auth.uid() NULL হলে
  -- NULL হয় (is_admin(NULL) = false, NULL = uuid = NULL → NULL or false = NULL → not NULL = NULL),
  -- আর PL/pgSQL `if NULL` raise করে না — মানে anon (JWT ছাড়া) কলার এই চেক পাশ কাটিয়ে যায়।
  -- `anon` role-এর EXECUTE grant live-এ আছে। এই ৩ লাইন সেটা বন্ধ করে। বাদ দিতে চাইলে ▼▼…▲▲ পুরো ব্লক মুছুন;
  -- A (dispute guard)-এর সাথে এর কোনো নির্ভরতা নেই।
  if auth.uid() is null then
    raise exception 'NOT_AUTHORIZED';
  end if;
  -- ▲▲ [B শেষ] ▲▲

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

  -- [Step 12.10 — A] server-side dispute guard।
  -- আগে এই RPC `problems.is_disputed` একেবারেই দেখত না: admin কোনো কাজ বিরোধে (dispute) ফেললেও owner-এর
  -- ডিভাইস থেকে `release_escrow` কল করলে টাকা solver-কে চলে যেত (আটকানো ছিল শুধু client-নির্ভর)।
  -- এখন: problem disputed থাকলে শুধু admin release করতে পারবে। live SQL callers
  -- (`resolve_dispute`, `admin_update_direct_contract_status`, `admin_reconcile_escrow_states`) সবাই
  -- আগেই `is_admin(auth.uid())` চেক করে (live pg_proc-এ যাচাইকৃত), তাই অপ্রভাবিত।
  -- ALREADY_TERMINAL/ALREADY_RELEASED (idempotent) রিটার্ন ওপরে আগের মতোই আগে চলে।
  -- `problems.is_disputed` live-এ `boolean NOT NULL default false` — coalesce শুধু নিরাপত্তার জন্য।
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
$function$
;
