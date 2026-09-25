-- [ধাপ ৩২.৬ — এই ফাইলটা "ডুপ্লিকেট মাইগ্রেশন ফিক্স" সেশনে লাইভ DB থেকে re-synced। প্রথম ভার্সন
--  ঢিলেঢালা id-প্যাটার্ন ম্যাচিং করতো ও শুধু flat balance আপডেট করতো; একই সেশনে দ্বিতীয়বার এই
--  কড়াকড়ি/role-scoped ভার্সন apply হয়েছিল যেটাই এখন লাইভ, কিন্তু ফাইল/রিপোর্ট আপডেট হয়নি।]
--
-- Mirrors Kotlin cleanupDuplicateRefunds(): finds REFUND transactions grouped by escrow_id where
-- more than one exists (historical race-condition duplicates), keeps the earliest, deletes the
-- rest, and deducts the duplicate's net_amount back out of the affected user's balance. Skips any
-- escrow_id starting with the synthetic fallback pattern 'ESC_' (real escrow ids are always
-- 'ESCROW_<uuid>' -- 'ESC_' prefixed ids only ever come from a fallback path when the real escrow
-- couldn't be resolved at write time, see SomadhanRepository.kt lines ~393/3223/3614, so grouping
-- by them is unreliable and flagged for manual review instead of automatic deletion, exactly like
-- the Kotlin version).
create or replace function public.admin_cleanup_duplicate_refunds(p_dry_run boolean default true)
returns jsonb
language plpgsql
security definer
set search_path to 'public'
as $function$
declare
  v_now timestamptz := now();
  v_group record;
  v_dup record;
  v_removed_count int := 0;
  v_skipped_count int := 0;
  v_items jsonb := '[]'::jsonb;
begin
  if not is_admin(auth.uid()) then
    raise exception 'NOT_AUTHORIZED';
  end if;

  for v_group in
    select escrow_id
    from public.transactions
    where type = 'REFUND' and escrow_id is not null and escrow_id <> ''
    group by escrow_id
    having count(*) > 1
  loop
    if v_group.escrow_id like 'ESC\_%' escape '\' then
      v_skipped_count := v_skipped_count + 1;
      v_items := v_items || jsonb_build_object('escrow_id', v_group.escrow_id, 'action', 'skipped_fallback_id_needs_manual_review');
      continue;
    end if;

    for v_dup in
      select * from public.transactions
      where type = 'REFUND' and escrow_id = v_group.escrow_id
      order by "timestamp" asc
      offset 1  -- keep the earliest, process the rest as duplicates
    loop
      v_items := v_items || jsonb_build_object(
        'escrow_id', v_group.escrow_id, 'duplicate_trx_id', v_dup.id,
        'user_id', v_dup.user_id, 'net_amount', v_dup.net_amount, 'action', 'delete_and_deduct'
      );
      if not p_dry_run then
        delete from public.transactions where id = v_dup.id;
        if v_dup.net_amount > 0 and v_dup.user_id is not null then
          update public.users set balance = greatest(0, balance - v_dup.net_amount),
            balance_user = greatest(0, balance_user - v_dup.net_amount), updated_at = v_now
          where id = v_dup.user_id;
          insert into public.transactions (id, problem_id, problem_title, user_id, gross_amount, net_amount, type, "timestamp")
          values ('TRX_DUP_CORRECTION_' || v_dup.id, v_dup.problem_id, 'ডুপ্লিকেট লেনদেন সমন্বয়',
            v_dup.user_id, v_dup.net_amount, -v_dup.net_amount, 'DUPLICATE_CORRECTION', v_now)
          on conflict (id) do nothing;
        end if;
        v_removed_count := v_removed_count + 1;
      end if;
    end loop;
  end loop;

  if not p_dry_run and v_removed_count > 0 then
    insert into public.admin_audit_logs (id, action_type, target_id, target_name, details, "timestamp")
    values ('AUDIT_' || replace(gen_random_uuid()::text, '-', ''), 'CLEANUP_DUPLICATE_REFUNDS', '', '',
      'removed=' || v_removed_count || ', skipped_fallback=' || v_skipped_count, v_now);
  end if;

  return jsonb_build_object('dry_run', p_dry_run, 'removed_count', v_removed_count, 'skipped_count', v_skipped_count, 'items', v_items);
end;
$function$;

revoke all on function public.admin_cleanup_duplicate_refunds(boolean) from public, anon;
grant execute on function public.admin_cleanup_duplicate_refunds(boolean) to authenticated;
