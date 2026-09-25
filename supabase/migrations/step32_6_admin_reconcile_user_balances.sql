-- [ধাপ ৩২.৬ — এই ফাইলটা "ডুপ্লিকেট মাইগ্রেশন ফিক্স" সেশনে লাইভ DB থেকে re-synced। মূল সেশনে
--  প্রথম ভার্সন শুধু flat "users.balance" রিকনসাইল করতো; একই সেশনে দ্বিতীয়বার role-scoped
--  (balance_user/balance_solver, দুইটা আলাদা ledger) ভার্সন apply হয়েছিল যেটাই এখন লাইভ, কিন্তু
--  ফাইল/রিপোর্ট আপডেট হয়নি ও migration history-তে দুইটা entry থেকে গিয়েছিল।]
--
-- Role-scoped ledger reconciliation. Unlike the Kotlin client (which recomputes a single flat
-- ledger against Room's legacy merged "balance" field), this recomputes TWO ledgers directly
-- from the source-of-truth transactions table:
--   ledger_user[user_id]   = sum(net_amount) of every txn where user_id is set, EXCEPT type
--                            PAYMENT (that's solver earnings, not user-side) and EXCEPT type
--                            BALANCE_RECONCILIATION (a correction record, not new activity --
--                            counting it would make the mismatch reappear every run, see the
--                            identical exclusion in the Kotlin reconcileUserBalances()).
--   ledger_solver[solver_id] = sum(net_amount) of every PAYMENT txn where solver_id is set.
-- This maps naturally onto the DB's real role-scoped columns (balance_user / balance_solver),
-- which is more precise than the Kotlin app's single merged "balance" comparison -- a mismatch
-- against this RPC's report is not necessarily the same mismatch the Kotlin tool would report,
-- and that's expected/documented, not a bug.
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
    select u.id, u.name, u.balance_user, u.balance_solver,
      coalesce(lu.ledger, 0) as ledger_user, coalesce(ls.ledger, 0) as ledger_solver
    from public.users u
    left join tmp_ledger_user lu on lu.id = u.id
    left join tmp_ledger_solver ls on ls.id = u.id
  loop
    v_scanned := v_scanned + 1;

    v_diff := v_row.balance_user - v_row.ledger_user;
    if abs(v_diff) > v_tolerance then
      v_mismatch_count := v_mismatch_count + 1;
      v_total_abs_diff := v_total_abs_diff + abs(v_diff);
      v_items := v_items || jsonb_build_object(
        'user_id', v_row.id, 'name', v_row.name, 'role', 'USER',
        'stored', v_row.balance_user, 'ledger', v_row.ledger_user, 'difference', v_diff
      );
      if not p_dry_run then
        update public.users set balance_user = v_row.ledger_user, updated_at = v_now where id = v_row.id;
        insert into public.transactions (id, problem_id, problem_title, user_id, gross_amount, net_amount, type, "timestamp")
        values ('TRX_RECONCILE_U_' || v_row.id || '_' || extract(epoch from v_now)::bigint, '',
          'ব্যালেন্স সংশোধন (লেজার অডিট, USER)', v_row.id, abs(v_diff), (v_row.ledger_user - v_row.balance_user),
          'BALANCE_RECONCILIATION', v_now);
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
      if not p_dry_run then
        update public.users set balance_solver = v_row.ledger_solver, updated_at = v_now where id = v_row.id;
        insert into public.transactions (id, problem_id, problem_title, solver_id, gross_amount, net_amount, type, "timestamp")
        values ('TRX_RECONCILE_S_' || v_row.id || '_' || extract(epoch from v_now)::bigint, '',
          'ব্যালেন্স সংশোধন (লেজার অডিট, SOLVER)', v_row.id, abs(v_diff), (v_row.ledger_solver - v_row.balance_solver),
          'BALANCE_RECONCILIATION', v_now);
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
