-- ============================================================================
-- step7_8_admin_reconcile_escrow_states_per_escrow_exception_handling   [Somadhan Bug-Fix Step 7.8]
-- ============================================================================
-- বাগ (৭.২-এর সময় ধরা পড়েছে): Case 2 (`refund_escrow_once`) ও Case 3 (`release_escrow`)-এর লুপে
-- `perform ...`-এর কোনো exception-handling ছিল না। একটা escrow-এর owner-এর USER role নিষ্ক্রিয়
-- (`USER_ROLE_INACTIVE`) বা solver-এর SOLVER role নিষ্ক্রিয় (`SOLVER_ROLE_INACTIVE`), অথবা অন্য
-- যেকোনো কারণে একটা escrow-এ `perform` exception ছুঁড়লে, পুরো ফাংশন-কল (পুরো loop, সব escrow)
-- rollback হয়ে যেত — বাকি সব ভালো escrow-ও সেই রানে repair হতো না। ৭.২-এর ফিক্সের পর
-- USER_ROLE_INACTIVE-এর কারণে HELD-pending escrow বাড়তে পারে বলে এই সমস্যাটা আরও বাস্তব হয়ে ওঠে।
--
-- ফিক্স: `admin_force_cancel_instant_job` (Step 7.2)-এর precedent অনুযায়ী প্রতিটা escrow-এর
-- `perform` কল একটা নেস্টেড `begin ... exception when others ... end;` ব্লকে মোড়ানো হলো —
-- plpgsql-এ নেস্টেড ব্লক নিজে একটা implicit savepoint হওয়ায়, একটা escrow ব্যর্থ হলে শুধু সেই
-- escrow-এর কাজ (এবং তার ভেতরের savepoint-এর পরের যেকোনো আংশিক লেখা) undo হয়, লুপের বাকি
-- escrow-গুলোর আগের/পরের কাজ অক্ষত থাকে। ব্যর্থ escrow-কে `v_items`-এ 'status':'failed' + আসল
-- error message (`sqlerrm`)-সহ যোগ করা হয় (`case2_failed`/`case3_failed` কাউন্টার-সহ রিপোর্ট করা
-- হয়), যাতে admin/caller বুঝতে পারে কোনটা কেন বাদ পড়েছে। `case2_repaired`/`case3_repaired`
-- কাউন্টার শুধু `perform` সফল হলেই increment হয় (আগের মতো কল করার আগে না), তাই ব্যর্থ escrow
-- আর repaired-count-এ ধরা পড়ে না।
--
-- অপরিবর্তিত: dry-run path (আগের মতোই কোনো `perform` কল হয় না, তাই exception-এর প্রশ্নই নেই),
-- ৫-repair-per-run cap লজিক (attempt-ভিত্তিক, আগের মতোই), audit-log-এর `details` স্ট্রিং ফরম্যাট
-- (বিদ্যমান pgTAP-এর exact-string assertion না ভাঙার জন্য অপরিবর্তিত রাখা হলো — case2_failed/
-- case3_failed শুধু রিটার্ন-জেসনে আছে, audit-log টেক্সটে না)। Case 1 (pure status-sync, কোনো
-- money-moving RPC কল করে না) অপরিবর্তিত।
-- ============================================================================
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
  v_case2_failed int := 0;
  v_case3_failed int := 0;
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
        if p_dry_run then
          v_case2_repaired := v_case2_repaired + 1;
          v_items := v_items || jsonb_build_object(
            'case', 2, 'escrow_id', v_escrow.id, 'problem_id', v_escrow.problem_id,
            'action', 'refund_escrow_once'
          );
        else
          v_repairs_this_run := v_repairs_this_run + 1;
          -- [Step 7.8] per-escrow exception handling: এই escrow ব্যর্থ হলে (যেমন owner-এর
          -- USER_ROLE_INACTIVE) শুধু এটাই বাদ পড়বে, নেস্টেড ব্লক-এর implicit savepoint-এর কারণে
          -- বাকি লুপ (আগের/পরের সফল escrow) rollback হয় না।
          begin
            perform public.refund_escrow_once(v_escrow.id, 'SOLVER_CANCEL', 100);
            v_case2_repaired := v_case2_repaired + 1;
            v_items := v_items || jsonb_build_object(
              'case', 2, 'escrow_id', v_escrow.id, 'problem_id', v_escrow.problem_id,
              'action', 'refund_escrow_once'
            );
          exception when others then
            v_case2_failed := v_case2_failed + 1;
            v_items := v_items || jsonb_build_object(
              'case', 2, 'escrow_id', v_escrow.id, 'problem_id', v_escrow.problem_id,
              'action', 'refund_escrow_once', 'status', 'failed', 'error', sqlerrm
            );
          end;
        end if;
      end if;

    elsif v_escrow.problem_status = 'COMPLETED' then
      -- Case 3: problem completed but solver never actually got paid out -> should be released
      if v_repairs_this_run < v_max_repairs_per_run then
        if p_dry_run then
          v_case3_repaired := v_case3_repaired + 1;
          v_items := v_items || jsonb_build_object(
            'case', 3, 'escrow_id', v_escrow.id, 'problem_id', v_escrow.problem_id,
            'action', 'release_escrow'
          );
        else
          v_repairs_this_run := v_repairs_this_run + 1;
          -- [Step 7.8] per-escrow exception handling: যেমন solver-এর SOLVER_ROLE_INACTIVE বা
          -- PROBLEM_DISPUTED — শুধু এই escrow বাদ পড়বে, নেস্টেড ব্লক-এর implicit savepoint-এর
          -- কারণে বাকি লুপ চলতে থাকে।
          begin
            perform public.release_escrow(v_escrow.id);
            v_case3_repaired := v_case3_repaired + 1;
            v_items := v_items || jsonb_build_object(
              'case', 3, 'escrow_id', v_escrow.id, 'problem_id', v_escrow.problem_id,
              'action', 'release_escrow'
            );
          exception when others then
            v_case3_failed := v_case3_failed + 1;
            v_items := v_items || jsonb_build_object(
              'case', 3, 'escrow_id', v_escrow.id, 'problem_id', v_escrow.problem_id,
              'action', 'release_escrow', 'status', 'failed', 'error', sqlerrm
            );
          end;
        end if;
      end if;
    end if;
  end loop;

  v_result := jsonb_build_object(
    'dry_run', p_dry_run,
    'case1_synced', v_case1_synced,
    'case2_repaired', v_case2_repaired,
    'case3_repaired', v_case3_repaired,
    'case2_failed', v_case2_failed,
    'case3_failed', v_case3_failed,
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
