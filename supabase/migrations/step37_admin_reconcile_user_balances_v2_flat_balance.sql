-- [ধাপ ৪ — RPC_SYNC_FIX_PROGRESS.md দেখুন] flat "users.balance" কলামও এখন
-- admin_reconcile_user_balances()-এর আওতায় আনা হলো। বিদ্যমান step32_6/step36 ফাইল এডিট করা
-- হয়নি (ইতিহাস অক্ষত রাখতে) — এই ফাইলটা শুধু একটা নতুন CREATE OR REPLACE ভার্সন, যেটা লাইভে
-- বর্তমানে যা আছে (step36-এর ভার্সন, transactions.role কলাম-সহ) তার উপর ভিত্তি করে বানানো।
--
-- কী নতুন যোগ হলো (existing balance_user/balance_solver লজিক অপরিবর্তিত):
--   flat `balance` কলামের "সঠিক" মান = balance_user + balance_solver (এই ফর্মুলা নতুন বানানো
--   হয়নি — MIGRATION_PROGRESS.md-এ আগে থেকেই ডকুমেন্টেড ও ব্যবহৃত, দেখুন "MONEY_FLOW_AND_ADMIN_BUGS
--   ধাপ ৬ ফলো-আপ" এন্ট্রি, যেখানে একটা stale legacy row একই ফর্মুলা দিয়ে ম্যানুয়ালি sync করা
--   হয়েছিল)। যদি এই RPC-এর একই রান-এ balance_user/balance_solver-ও correct হয় (dry_run=false
--   হলে), flat balance-এর টার্গেট effective (post-correction) মান দিয়ে হিসাব হয় — অর্থাৎ একই
--   পাসে দুই স্তরের mismatch থাকলেও শেষে balance == balance_user + balance_solver ধরে রাখা হয়।
--
-- dry_run প্যাটার্ন অপরিবর্তিত: p_dry_run DEFAULT true, তাই ভুলে সরাসরি কল করলে কোনো টাকার
-- মান বদলাবে না, শুধু রিপোর্ট রিটার্ন করবে।
--
-- ⚠️ টেস্ট করার নিয়ম (এই migration লাইভে apply করার পর):
--   ১. প্রথমে `select admin_reconcile_user_balances(true);` (dry_run=true, ডিফল্ট) চালাও।
--   ২. রিটার্ন করা `items` array manual review করো — বিশেষভাবে নতুন `"role": "FLAT"` এন্ট্রিগুলো,
--      যেগুলো flat balance mismatch নির্দেশ করে।
--   ৩. রিপোর্ট ঠিক মনে হলে তবেই `select admin_reconcile_user_balances(false);` চালাও।
--   এই migration ফাইল যোগ করা মানেই লাইভ DB-তে apply হয়ে গেছে এমন না — শুধু repo-তে ফাইল
--   হিসেবে যোগ হয়েছে; user নিজে সিদ্ধান্ত নিয়ে apply করবে।
create or replace function public.admin_reconcile_user_balances(p_dry_run boolean default true)
returns jsonb
language plpgsql
security definer
set search_path to 'public'
as $function$
declare
  v_now timestamptz := now();
  v_tolerance numeric := 1.0;
  v_row record;
  v_scanned int := 0;
  v_mismatch_count int := 0;
  v_corrected_count int := 0;
  v_total_abs_diff numeric := 0;
  v_items jsonb := '[]'::jsonb;
  v_diff numeric;
  v_effective_balance_user numeric;
  v_effective_balance_solver numeric;
  v_flat_diff numeric;
begin
  if not is_admin(auth.uid()) then
    raise exception 'NOT_AUTHORIZED';
  end if;

  create temp table tmp_ledger_user on commit drop as
    select user_id as id, sum(net_amount) as ledger
    from public.transactions
    where user_id is not null
      and type <> 'PAYMENT' and type <> 'BALANCE_RECONCILIATION'
    group by user_id;

  create temp table tmp_ledger_solver on commit drop as
    select solver_id as id, sum(net_amount) as ledger
    from public.transactions
    where solver_id is not null and type = 'PAYMENT'
    group by solver_id;

  for v_row in
    select u.id, u.name, u.balance, u.balance_user, u.balance_solver,
      coalesce(lu.ledger, 0) as ledger_user, coalesce(ls.ledger, 0) as ledger_solver
    from public.users u
    left join tmp_ledger_user lu on lu.id = u.id
    left join tmp_ledger_solver ls on ls.id = u.id
  loop
    v_scanned := v_scanned + 1;
    v_effective_balance_user := v_row.balance_user;
    v_effective_balance_solver := v_row.balance_solver;

    v_diff := v_row.balance_user - v_row.ledger_user;
    if abs(v_diff) > v_tolerance then
      v_mismatch_count := v_mismatch_count + 1;
      v_total_abs_diff := v_total_abs_diff + abs(v_diff);
      v_items := v_items || jsonb_build_object(
        'user_id', v_row.id, 'name', v_row.name, 'role', 'USER',
        'stored', v_row.balance_user, 'ledger', v_row.ledger_user, 'difference', v_diff
      );
      v_effective_balance_user := v_row.ledger_user;
      if not p_dry_run then
        update public.users set balance_user = v_row.ledger_user, updated_at = v_now where id = v_row.id;
        insert into public.transactions (id, problem_id, problem_title, user_id, gross_amount, net_amount, type, role, "timestamp")
        values ('TRX_RECONCILE_U_' || v_row.id || '_' || extract(epoch from v_now)::bigint, '',
          'ব্যালেন্স সংশোধন (লেজার অডিট, USER)', v_row.id, abs(v_diff), (v_row.ledger_user - v_row.balance_user),
          'BALANCE_RECONCILIATION', 'USER', v_now);
        v_corrected_count := v_corrected_count + 1;
      end if;
    end if;

    v_diff := v_row.balance_solver - v_row.ledger_solver;
    if abs(v_diff) > v_tolerance then
      v_mismatch_count := v_mismatch_count + 1;
      v_total_abs_diff := v_total_abs_diff + abs(v_diff);
      v_items := v_items || jsonb_build_object(
        'user_id', v_row.id, 'name', v_row.name, 'role', 'SOLVER',
        'stored', v_row.balance_solver, 'ledger', v_row.ledger_solver, 'difference', v_diff
      );
      v_effective_balance_solver := v_row.ledger_solver;
      if not p_dry_run then
        update public.users set balance_solver = v_row.ledger_solver, updated_at = v_now where id = v_row.id;
        insert into public.transactions (id, problem_id, problem_title, solver_id, gross_amount, net_amount, type, role, "timestamp")
        values ('TRX_RECONCILE_S_' || v_row.id || '_' || extract(epoch from v_now)::bigint, '',
          'ব্যালেন্স সংশোধন (লেজার অডিট, SOLVER)', v_row.id, abs(v_diff), (v_row.ledger_solver - v_row.balance_solver),
          'BALANCE_RECONCILIATION', 'SOLVER', v_now);
        v_corrected_count := v_corrected_count + 1;
      end if;
    end if;

    -- [ধাপ ৪] flat balance কলাম চেক — target = effective (এই একই পাসে হয়তো just-corrected)
    -- balance_user + balance_solver। রিপোর্টে role='FLAT' হিসেবে দেখানো হয়।
    v_flat_diff := v_row.balance - (v_effective_balance_user + v_effective_balance_solver);
    if abs(v_flat_diff) > v_tolerance then
      v_mismatch_count := v_mismatch_count + 1;
      v_total_abs_diff := v_total_abs_diff + abs(v_flat_diff);
      v_items := v_items || jsonb_build_object(
        'user_id', v_row.id, 'name', v_row.name, 'role', 'FLAT',
        'stored', v_row.balance, 'ledger', (v_effective_balance_user + v_effective_balance_solver),
        'difference', v_flat_diff
      );
      if not p_dry_run then
        update public.users set balance = (v_effective_balance_user + v_effective_balance_solver), updated_at = v_now where id = v_row.id;
        insert into public.transactions (id, problem_id, problem_title, user_id, gross_amount, net_amount, type, role, "timestamp")
        values ('TRX_RECONCILE_F_' || v_row.id || '_' || extract(epoch from v_now)::bigint, '',
          'ব্যালেন্স সংশোধন (লেজার অডিট, FLAT)', v_row.id, abs(v_flat_diff),
          ((v_effective_balance_user + v_effective_balance_solver) - v_row.balance),
          'BALANCE_RECONCILIATION', '', v_now);
        v_corrected_count := v_corrected_count + 1;
      end if;
    end if;
  end loop;

  if not p_dry_run and v_corrected_count > 0 then
    insert into public.admin_audit_logs (id, action_type, target_id, target_name, details, "timestamp")
    values ('AUDIT_' || replace(gen_random_uuid()::text, '-', ''), 'ADMIN_RECONCILE_USER_BALANCES', '', '',
      'corrected=' || v_corrected_count || ', total_abs_diff=' || v_total_abs_diff, v_now);
  end if;

  return jsonb_build_object(
    'dry_run', p_dry_run, 'scanned_users', v_scanned, 'mismatch_count', v_mismatch_count,
    'corrected_count', v_corrected_count, 'total_absolute_difference', v_total_abs_diff, 'items', v_items
  );
end;
$function$;

revoke all on function public.admin_reconcile_user_balances(boolean) from public, anon;
grant execute on function public.admin_reconcile_user_balances(boolean) to authenticated;
