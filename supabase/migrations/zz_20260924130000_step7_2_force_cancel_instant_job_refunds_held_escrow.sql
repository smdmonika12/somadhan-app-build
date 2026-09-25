-- ============================================================================
-- step7_2_force_cancel_instant_job_refunds_held_escrow   [Somadhan Bug-Fix Step 7.2]
-- ============================================================================
-- (১) admin_force_cancel_instant_job(text,text,text,integer): accepted solver থাকলে সেই problem-এর HELD escrow(গুলো)
--     refund_escrow_once(id,'ADMIN_FORCE_ACTION',100) দিয়ে refund করা হয় (আগে RPC escrows ছুঁতো না)।
--     owner-এর USER role নিষ্ক্রিয় (USER_ROLE_INACTIVE) হলে cancel থেমে যায় না — cancel সম্পন্ন হয়, escrow HELD থাকে,
--     আর ফলাফলে 'refund_pending': true ফেরে। অন্য যেকোনো refund error-এ আগের মতো পুরো cancel rollback হয়।
-- (২) নতুন trigger users.has_user_role false→true হলে সেই user-এর HELD escrow যেগুলোর problem CANCELLED অথবা
--     (OPEN + accepted_solver_id null) (admin_reconcile_escrow_states-এর Case 2-এর একই শর্ত) সেগুলো
--     refund_escrow_once(id,'ROLE_REACTIVATION_REFUND',100) দিয়ে স্বয়ংক্রিয় refund করে। trigger কখনো role-activation
--     আটকায় না — refund ব্যর্থ হলে WARNING দিয়ে এগোয়, escrow HELD-ই থাকে (পরে reconcile ধরবে)।
-- ফাংশন-বডি recovered_admin_moderation.sql:551-এর সাথে বাকি সব হুবহু এক। grants CREATE OR REPLACE-এ অপরিবর্তিত।
-- ⚠️ ফাইলের নামে `zz_<version>_` prefix ইচ্ছাকৃত (CI অক্ষরক্রমে apply করে)।
-- ✅ live Supabase প্রজেক্টে (mghvvpndkxnscwryfkib) Supabase MCP `apply_migration` দিয়ে apply করা হয়েছে (২০২৬-০৯-২৪, ব্যবহারকারীর অনুমতিতে);
--    apply-এর পর function-বডি/trigger-def/grants cloud থেকে পড়ে যাচাই করা হয়েছে। (নোট: live migration-history-র নামে MCP নিজে timestamp prefix বসায়।)
-- ============================================================================
CREATE OR REPLACE FUNCTION public.admin_force_cancel_instant_job(p_problem_id text, p_reason text, p_target_action text DEFAULT 'CANCEL'::text, p_progress_step integer DEFAULT 1)
 RETURNS jsonb
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
declare
  v_problem public.problems%rowtype;
  v_now timestamptz := now();
  v_step int := greatest(coalesce(p_progress_step, 1), 1);
  v_had_solver boolean;
  v_escrow_id text;
  v_refund_pending boolean := false;
begin
  if not public.is_admin(auth.uid()) then
    raise exception 'NOT_AUTHORIZED';
  end if;
  select * into v_problem from public.problems where id = p_problem_id for update;
  if not found then raise exception 'PROBLEM_NOT_FOUND'; end if;
  if v_problem.status in ('COMPLETED', 'CANCELLED') then
    return jsonb_build_object('result', 'ALREADY_TERMINAL');
  end if;

  v_had_solver := v_problem.accepted_solver_id is not null;

  if v_had_solver then
    update public.bids set
      status = 'CANCELLED', progress_at_cancel = v_step, resolution_type = 'ADMIN_FORCE_ACTION', resolved_at = v_now
    where problem_id = p_problem_id
      and (id = v_problem.accepted_bid_id or solver_id = v_problem.accepted_solver_id)
      and status <> 'CANCELLED';
  end if;

  -- [Step 7.2] accepted solver থাকলে সেই problem-এর HELD escrow-ও এই RPC-র ভেতরেই refund করা হয়
  -- (তিনটা target_action-এই, ঠিক Kotlin `adminForceCancelInstantJob()`-এর লোকাল আচরণের মতো)।
  -- refund_escrow_once() নিজেই idempotent — app আগে refund করলে double-refund হয় না।
  -- owner-এর USER role নিষ্ক্রিয় হলে refund_escrow_once 'USER_ROLE_INACTIVE' ছোঁড়ে: শুধু সেই ক্ষেত্রে cancel চালিয়ে যাওয়া হয়,
  -- escrow HELD থাকে (refund_pending=true), role আবার active হলে নিচের trigger refund করে দেবে। অন্য error → rollback।
  if v_had_solver then
    for v_escrow_id in
      select id from public.escrows where problem_id = p_problem_id and status = 'HELD' order by id
    loop
      begin
        perform public.refund_escrow_once(v_escrow_id, 'ADMIN_FORCE_ACTION', 100);
      exception when others then
        if sqlerrm = 'USER_ROLE_INACTIVE' then
          v_refund_pending := true;
        else
          raise;
        end if;
      end;
    end loop;
  end if;

  if p_target_action = 'REBROADCAST' then
    update public.problems set
      is_instant_job = true, job_status = 'BROADCASTING', status = 'OPEN',
      accepted_bid_id = null, accepted_solver_id = null, accepted_solver_name = null, accepted_amount = null,
      solver_live_lat = null, solver_live_lng = null, solver_live_updated_at = null,
      arrived_at = null, job_started_at = null,
      has_release_request = false, release_request_extra_amount = 0, release_request_note = '', release_requested_at = null,
      confirmed_extra_amount_total = 0, broadcast_timer_started_at = v_now, last_activity_at = v_now
    where id = p_problem_id;

    insert into public.notifications (id, user_id, title, message, target_type, target_id, related_problem_id, "timestamp")
    values ('NOTIF_' || replace(gen_random_uuid()::text, '-', ''), v_problem.user_id,
      'জরুরি জব পুনরায় ব্রডকাস্ট করা হয়েছে ⚡',
      'অ্যাডমিন কর্তৃক আপনার জরুরি জবটি নতুন করে ব্রডকাস্ট করা হয়েছে। কারণ: ' || coalesce(p_reason, ''),
      'problem', p_problem_id, p_problem_id, v_now);

    if v_problem.accepted_solver_id is not null then
      insert into public.notifications (id, user_id, title, message, target_type, target_id, related_problem_id, "timestamp")
      values ('NOTIF_' || replace(gen_random_uuid()::text, '-', ''), v_problem.accepted_solver_id,
        'জরুরি কাজ পুনরায় ব্রডকাস্ট করা হয়েছে (অ্যাডমিন) ⚡',
        'অ্যাডমিন কর্তৃক "' || v_problem.title || '" কাজটি পুনরায় ব্রডকাস্ট করা হয়েছে। কারণ: ' || coalesce(p_reason, ''),
        'problem', p_problem_id, p_problem_id, v_now);
    end if;

  elsif p_target_action = 'TO_NORMAL_BIDDING' then
    update public.problems set
      is_instant_job = false, job_status = null, status = 'OPEN',
      accepted_bid_id = null, accepted_solver_id = null, accepted_solver_name = null, accepted_amount = null,
      solver_live_lat = null, solver_live_lng = null, solver_live_updated_at = null,
      arrived_at = null, job_started_at = null,
      has_release_request = false, release_request_extra_amount = 0, release_request_note = '', release_requested_at = null,
      confirmed_extra_amount_total = 0, last_activity_at = v_now
    where id = p_problem_id;

    insert into public.notifications (id, user_id, title, message, target_type, target_id, related_problem_id, "timestamp")
    values ('NOTIF_' || replace(gen_random_uuid()::text, '-', ''), v_problem.user_id,
      'কাজটি সাধারণ বিডিংয়ে রূপান্তর করা হয়েছে 📋',
      'জরুরি জবটি সাধারণ বিডিং তালিকায় পাঠানো হয়েছে। কারণ: ' || coalesce(p_reason, ''),
      'problem', p_problem_id, p_problem_id, v_now);

    if v_problem.accepted_solver_id is not null then
      insert into public.notifications (id, user_id, title, message, target_type, target_id, related_problem_id, "timestamp")
      values ('NOTIF_' || replace(gen_random_uuid()::text, '-', ''), v_problem.accepted_solver_id,
        'কাজটি সাধারণ বিডিংয়ে রূপান্তর করা হয়েছে (অ্যাডমিন) 📋',
        'অ্যাডমিন কর্তৃক "' || v_problem.title || '" কাজটি সাধারণ বিডিং তালিকায় পাঠানো হয়েছে। কারণ: ' || coalesce(p_reason, ''),
        'problem', p_problem_id, p_problem_id, v_now);
    end if;

  else -- CANCEL
    update public.problems set
      status = 'CANCELLED', job_status = 'CANCELLED',
      accepted_bid_id = null,
      has_release_request = false, release_request_extra_amount = 0, release_request_note = '', release_requested_at = null,
      last_activity_at = v_now
    where id = p_problem_id;

    insert into public.notifications (id, user_id, title, message, target_type, target_id, related_problem_id, "timestamp")
    values ('NOTIF_' || replace(gen_random_uuid()::text, '-', ''), v_problem.user_id,
      'জরুরি কাজ বাতিল (অ্যাডমিন) ❌',
      'অ্যাডমিন কর্তৃক "' || v_problem.title || '" কাজটি বাতিল করা হয়েছে। কারণ: ' || coalesce(p_reason, ''),
      'problem', p_problem_id, p_problem_id, v_now);

    if v_problem.accepted_solver_id is not null then
      insert into public.notifications (id, user_id, title, message, target_type, target_id, related_problem_id, "timestamp")
      values ('NOTIF_' || replace(gen_random_uuid()::text, '-', ''), v_problem.accepted_solver_id,
        'জরুরি কাজ বাতিল (অ্যাডমিন) ❌',
        'অ্যাডমিন কর্তৃক "' || v_problem.title || '" কাজটি বাতিল করা হয়েছে। কারণ: ' || coalesce(p_reason, ''),
        'problem', p_problem_id, p_problem_id, v_now);
    end if;
  end if;

  return jsonb_build_object('result', 'OK', 'refund_pending', v_refund_pending);
end;
$function$;

-- ----------------------------------------------------------------------------
-- (২) users.has_user_role false→true হলে pending (HELD) escrow refund
-- ----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.refund_pending_escrows_on_user_role_activation()
 RETURNS trigger
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
declare
  v_escrow_id text;
begin
  for v_escrow_id in
    select e.id
    from public.escrows e
    join public.problems p on p.id = e.problem_id
    where e.user_id = new.id
      and e.status = 'HELD'
      and (p.status = 'CANCELLED' or (p.status = 'OPEN' and p.accepted_solver_id is null))
    order by e.id
  loop
    begin
      perform public.refund_escrow_once(v_escrow_id, 'ROLE_REACTIVATION_REFUND', 100);
    exception when others then
      raise warning 'refund_pending_escrows_on_user_role_activation: escrow % refund ব্যর্থ (%), HELD-ই থাকল', v_escrow_id, sqlerrm;
    end;
  end loop;
  return null;
end;
$function$;

DROP TRIGGER IF EXISTS trg_refund_pending_escrows_on_user_role_activation ON public.users;
CREATE TRIGGER trg_refund_pending_escrows_on_user_role_activation
  AFTER UPDATE OF has_user_role ON public.users
  FOR EACH ROW
  WHEN (OLD.has_user_role IS DISTINCT FROM TRUE AND NEW.has_user_role IS TRUE)
  EXECUTE FUNCTION public.refund_pending_escrows_on_user_role_activation();
