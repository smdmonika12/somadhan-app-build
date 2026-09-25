-- [ধাপ ৩২.৬ — এই ফাইলটা "ডুপ্লিকেট মাইগ্রেশন ফিক্স" সেশনে লাইভ DB থেকে re-synced। মূল ৩২.৬
--  সেশনে এই RPC দুইবার apply হয়েছিল: প্রথমবার case-2 তে refund_escrow_once(id, 'FULL', null)
--  কল করা হয়েছিল -- p_refund_percentage=null হওয়ায় amount NULL হয়ে যেত (money bug, কখনো
--  ব্যবহারকারীর হাতে পৌঁছায়নি কারণ সেই সময় DB খালি ছিল)। একই সেশনে দ্বিতীয়বার সংশোধিত ভার্সন
--  apply হয় (percentage=100), কিন্তু .sql ফাইল/রিপোর্ট আপডেট হয়নি, আর migration history-তে
--  দুইটা entry-ই থেকে যায়। এই ফাইলটা এখন সেই সংশোধিত/লাইভ ভার্সনের সাথে হুবহু মিলিয়ে রাখা হলো।]
create or replace function public.admin_reconcile_escrow_states(p_dry_run boolean default true)
returns jsonb
language plpgsql
security definer
set search_path to 'public'
as $function$
declare
  v_now timestamptz := now();
  v_escrow record;
  v_refund_count int;
  v_case1_synced int := 0;
  v_case2_repaired int := 0;
  v_case3_repaired int := 0;
  v_repairs_this_run int := 0;
  v_max_repairs_per_run int := 5;
  v_items jsonb := '[]'::jsonb;
  v_result jsonb;
begin
  if not is_admin(auth.uid()) then
    raise exception 'NOT_AUTHORIZED';
  end if;

  for v_escrow in
    select e.*, p.status as problem_status, p.accepted_solver_id, p.title as problem_title2
    from public.escrows e
    left join public.problems p on p.id = e.problem_id
    where e.status = 'HELD'
    order by e.created_at
  loop
    select count(*) into v_refund_count
    from public.transactions t
    where t.escrow_id = v_escrow.id and t.type = 'REFUND';

    if v_refund_count > 0 then
      -- Case 1: a verified refund transaction already exists -> safe pure status sync, no money movement
      v_case1_synced := v_case1_synced + 1;
      v_items := v_items || jsonb_build_object(
        'case', 1, 'escrow_id', v_escrow.id, 'problem_id', v_escrow.problem_id,
        'action', 'sync_status_to_refunded'
      );
      if not p_dry_run then
        update public.escrows set status = 'REFUNDED', released_at = coalesce(released_at, v_now), updated_at = v_now
        where id = v_escrow.id;
      end if;

    elsif v_escrow.problem_status = 'CANCELLED'
       or (v_escrow.problem_status = 'OPEN' and v_escrow.accepted_solver_id is null) then
      -- Case 2: cancelled/unassigned-open problem, HELD escrow, no refund tx -> should be refunded
      if v_repairs_this_run < v_max_repairs_per_run then
        v_case2_repaired := v_case2_repaired + 1;
        v_items := v_items || jsonb_build_object(
          'case', 2, 'escrow_id', v_escrow.id, 'problem_id', v_escrow.problem_id,
          'action', 'refund_escrow_once'
        );
        if not p_dry_run then
          v_repairs_this_run := v_repairs_this_run + 1;
          perform public.refund_escrow_once(v_escrow.id, 'SOLVER_CANCEL', 100);
        end if;
      end if;

    elsif v_escrow.problem_status = 'COMPLETED' then
      -- Case 3: problem completed but solver never actually got paid out -> should be released
      if v_repairs_this_run < v_max_repairs_per_run then
        v_case3_repaired := v_case3_repaired + 1;
        v_items := v_items || jsonb_build_object(
          'case', 3, 'escrow_id', v_escrow.id, 'problem_id', v_escrow.problem_id,
          'action', 'release_escrow'
        );
        if not p_dry_run then
          v_repairs_this_run := v_repairs_this_run + 1;
          perform public.release_escrow(v_escrow.id);
        end if;
      end if;
    end if;
  end loop;

  v_result := jsonb_build_object(
    'dry_run', p_dry_run,
    'case1_synced', v_case1_synced,
    'case2_repaired', v_case2_repaired,
    'case3_repaired', v_case3_repaired,
    'items', v_items
  );

  if not p_dry_run then
    insert into public.admin_audit_logs (id, action_type, target_id, target_name, details, "timestamp")
    values ('AUDIT_' || replace(gen_random_uuid()::text, '-', ''), 'ADMIN_RECONCILE_ESCROW_STATES', '', '',
      'case1_synced=' || v_case1_synced || ', case2_repaired=' || v_case2_repaired || ', case3_repaired=' || v_case3_repaired, v_now);
  end if;

  return v_result;
end;
$function$;

revoke all on function public.admin_reconcile_escrow_states(boolean) from public, anon;
grant execute on function public.admin_reconcile_escrow_states(boolean) to authenticated;
