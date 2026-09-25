-- [ধাপ ৩২.৬ — এই ফাইলটা "ডুপ্লিকেট মাইগ্রেশন ফিক্স" সেশনে লাইভ DB থেকে re-synced। প্রথম ভার্সন
--  শুধু status='REFUNDED' চেক করতো ও সবসময় fixed 100% রিফান্ড করতো; একই সেশনে দ্বিতীয়বার
--  REFUND_PENDING_SYNC-ও কভার করা এবং split-dispute-aware percentage হিসাবের ভার্সন apply
--  হয়েছিল যেটাই এখন লাইভ, কিন্তু ফাইল/রিপোর্ট আপডেট হয়নি।]
--
-- Mirrors Kotlin repairMissingRefunds(): finds escrows marked REFUNDED (status already says so)
-- but with NO matching REFUND transaction row (money-losing bug -- status says done, ledger says
-- nothing happened). Can't reuse refund_escrow_once() here because it explicitly blocks on
-- status IN (RELEASED, REFUNDED) as an already-terminal guard -- exactly the state this repair
-- starts from -- so the credit + transaction-insert logic is reimplemented directly here,
-- idempotent via a deterministic transaction id (TRX_REFUND_<escrow_id>).
create or replace function public.admin_repair_missing_refunds(p_dry_run boolean default true)
returns jsonb
language plpgsql
security definer
set search_path to 'public'
as $function$
declare
  v_now timestamptz := now();
  v_escrow record;
  v_total_amount numeric;
  v_refund_type text;
  v_refund_pct numeric;
  v_trx_id text;
  v_scanned int := 0;
  v_missing_count int := 0;
  v_repaired_count int := 0;
  v_total_amount_sum numeric := 0;
  v_items jsonb := '[]'::jsonb;
begin
  if not is_admin(auth.uid()) then
    raise exception 'NOT_AUTHORIZED';
  end if;

  select count(*) into v_scanned from public.escrows;

  for v_escrow in
    select e.*, p.title as problem_title2, p.status as problem_status,
      p.dispute_resolution_decision, p.dispute_split_solver_percent
    from public.escrows e
    left join public.problems p on p.id = e.problem_id
    where e.status in ('REFUNDED', 'REFUND_PENDING_SYNC')
  loop
    v_total_amount := v_escrow.base_amount + v_escrow.extra_amount;
    if v_total_amount <= 0 or v_escrow.problem_id is null or v_escrow.problem_id = ''
       or v_escrow.user_id is null then
      continue;
    end if;

    v_trx_id := 'TRX_REFUND_' || v_escrow.id;
    if exists (select 1 from public.transactions where id = v_trx_id)
       or exists (select 1 from public.transactions where type = 'REFUND' and escrow_id = v_escrow.id) then
      continue; -- already has a refund transaction, not actually missing
    end if;

    -- Same eligibility guard as the Kotlin version: skip if the problem looks like a genuine
    -- completed/released payout scenario rather than a refund scenario.
    if v_escrow.problem_status = 'COMPLETED' and v_escrow.released_at is null
       and coalesce(v_escrow.dispute_resolution_decision, '') not in ('REFUND_TO_USER', 'SPLIT_SETTLEMENT', 'CUSTOM_SPLIT') then
      continue;
    end if;

    v_missing_count := v_missing_count + 1;
    v_total_amount_sum := v_total_amount_sum + v_total_amount;

    v_refund_type := case
      when v_escrow.dispute_resolution_decision in ('SPLIT_SETTLEMENT', 'CUSTOM_SPLIT') then 'SPLIT_REFUND'
      when v_escrow.dispute_resolution_decision = 'REFUND_TO_USER' then 'DISPUTE_REFUND'
      else 'SOLVER_CANCEL'
    end;
    v_refund_pct := case
      when v_refund_type = 'SPLIT_REFUND' then greatest(0, least(100, 100 - coalesce(v_escrow.dispute_split_solver_percent, 50)))
      else 100
    end;

    v_items := v_items || jsonb_build_object(
      'escrow_id', v_escrow.id, 'problem_id', v_escrow.problem_id, 'user_id', v_escrow.user_id,
      'total_amount', v_total_amount, 'refund_type', v_refund_type, 'refund_percentage', v_refund_pct
    );

    if not p_dry_run then
      update public.users set balance = balance + (v_total_amount * v_refund_pct / 100.0),
        balance_user = balance_user + (v_total_amount * v_refund_pct / 100.0), updated_at = v_now
      where id = v_escrow.user_id;

      insert into public.transactions (id, problem_id, problem_title, user_id, solver_id, gross_amount,
        net_amount, type, escrow_id, refund_type, refund_percentage, "timestamp")
      values (v_trx_id, v_escrow.problem_id, coalesce(v_escrow.problem_title2, v_escrow.problem_title, ''),
        v_escrow.user_id, v_escrow.solver_id, v_total_amount * v_refund_pct / 100.0,
        v_total_amount * v_refund_pct / 100.0, 'REFUND', v_escrow.id, v_refund_type, v_refund_pct, v_now)
      on conflict (id) do nothing;

      v_repaired_count := v_repaired_count + 1;
    end if;
  end loop;

  if not p_dry_run and v_repaired_count > 0 then
    insert into public.admin_audit_logs (id, action_type, target_id, target_name, details, "timestamp")
    values ('AUDIT_' || replace(gen_random_uuid()::text, '-', ''), 'ADMIN_REPAIR_MISSING_REFUNDS', '', '',
      'repaired=' || v_repaired_count || ', total_amount=' || v_total_amount_sum, v_now);
  end if;

  return jsonb_build_object(
    'dry_run', p_dry_run, 'scanned_escrows', v_scanned, 'missing_count', v_missing_count,
    'repaired_count', v_repaired_count, 'total_amount_sum', v_total_amount_sum, 'items', v_items
  );
end;
$function$;

revoke all on function public.admin_repair_missing_refunds(boolean) from public, anon;
grant execute on function public.admin_repair_missing_refunds(boolean) to authenticated;
