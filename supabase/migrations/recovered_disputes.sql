-- RECOVERED from live DB on 2026-09-18, verbatim capture, no logic changes. See
-- RPC_SYNC_FIX_PROGRESS.md Step 2.
-- Functions in this file (Step 1 missing-RPC group): raise_dispute, resolve_dispute, settle_dispute, withdraw_dispute, request_admin_assistance

-- signature: raise_dispute(text,text)
CREATE OR REPLACE FUNCTION public.raise_dispute(p_problem_id text, p_reason text)
 RETURNS jsonb
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
declare
  v_problem public.problems%rowtype;
  v_is_solver boolean;
  v_target_user_id uuid;
  v_now timestamptz := now();
begin
  select * into v_problem from public.problems where id = p_problem_id for update;
  if not found then raise exception 'PROBLEM_NOT_FOUND'; end if;
  if auth.uid() <> v_problem.user_id and auth.uid() <> v_problem.accepted_solver_id then
    raise exception 'NOT_AUTHORIZED';
  end if;

  v_is_solver := auth.uid() = v_problem.accepted_solver_id;

  update public.problems set
    is_disputed = true,
    dispute_reason = trim(p_reason),
    dispute_initiator_id = auth.uid(),
    dispute_initiator_role = case when v_is_solver then 'SOLVER' else 'USER' end,
    disputed_at = v_now,
    dispute_settled_at = null,
    last_activity_at = v_now
  where id = p_problem_id;

  v_target_user_id := case when v_is_solver then v_problem.user_id else v_problem.accepted_solver_id end;
  if v_target_user_id is not null then
    insert into public.notifications (id, user_id, title, message, target_type, target_id, related_problem_id, role, "timestamp")
    values ('NOTIF_' || replace(gen_random_uuid()::text, '-', ''), v_target_user_id,
      case when v_is_solver then '⚠️ সমাধানকারী বিরোধ (Dispute) উত্থাপন করেছেন' else '⚠️ কাজের রিলিজ নিয়ে বিরোধ/ডিসপিউট তোলা হয়েছে' end,
      v_problem.title || ' কাজের বিষয়ে বিরোধ (Dispute) তুলেছেন। কারণ: ' || trim(p_reason) || '। দয়া করে চ্যাটে আলোচনা করুন।',
      'problem', p_problem_id, p_problem_id, case when v_is_solver then 'USER' else 'SOLVER' end, v_now);
  end if;

  return jsonb_build_object('result', 'OK');
end;
$function$
;
GRANT EXECUTE ON FUNCTION raise_dispute(text,text) TO anon;
GRANT EXECUTE ON FUNCTION raise_dispute(text,text) TO authenticated;
GRANT EXECUTE ON FUNCTION raise_dispute(text,text) TO postgres;
GRANT EXECUTE ON FUNCTION raise_dispute(text,text) TO service_role;

-- signature: resolve_dispute(text,text,text,numeric)
CREATE OR REPLACE FUNCTION public.resolve_dispute(p_problem_id text, p_resolution text, p_decision_note text, p_solver_percent numeric DEFAULT 50)
 RETURNS jsonb
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
declare
  v_problem public.problems%rowtype;
  v_escrow public.escrows%rowtype;
  v_now timestamptz := now();
  v_total numeric(12,2);
  v_solver_gross numeric(12,2);
  v_user_refund numeric(12,2);
  v_commission_rate numeric(6,2);
  v_commission numeric(12,2);
  v_solver_net numeric(12,2);
  v_ratio numeric;
  v_release_result jsonb;
  v_refund_result jsonb;
begin
  if not public.is_admin(auth.uid()) then raise exception 'NOT_AUTHORIZED'; end if;

  select * into v_problem from public.problems where id = p_problem_id for update;
  if not found then raise exception 'PROBLEM_NOT_FOUND'; end if;

  if v_problem.dispute_resolved_at is not null then
    raise exception 'ALREADY_RESOLVED: dispute for % was already resolved at %', p_problem_id, v_problem.dispute_resolved_at;
  end if;

  select * into v_escrow from public.escrows where problem_id = p_problem_id and status = 'HELD'
    order by created_at desc limit 1 for update;

  if p_resolution = 'RELEASE_TO_SOLVER' then
    if found then
      v_release_result := public.release_escrow(v_escrow.id);
    end if;
    update public.problems set
      status = 'COMPLETED', job_status = 'JOB_COMPLETED',
      dispute_resolution_decision = 'RELEASE_TO_SOLVER', dispute_resolution_type = 'RELEASE_TO_SOLVER',
      dispute_resolution_note = p_decision_note, dispute_resolved_at = v_now, dispute_settled_at = v_now,
      dispute_result_seen_by_user = false, dispute_result_seen_by_solver = false, last_activity_at = v_now
    where id = p_problem_id;
    return jsonb_build_object('result', 'OK', 'resolution', 'RELEASE_TO_SOLVER', 'release', v_release_result);

  elsif p_resolution = 'REFUND_TO_USER' then
    if found then
      v_refund_result := public.refund_escrow_once(v_escrow.id, 'DISPUTE_REFUND', 100);
    end if;
    update public.problems set
      status = 'CANCELLED',
      dispute_resolution_decision = 'REFUND_TO_USER', dispute_resolution_type = 'REFUND_TO_USER',
      dispute_resolution_note = p_decision_note, dispute_resolved_at = v_now, dispute_settled_at = v_now,
      dispute_result_seen_by_user = false, dispute_result_seen_by_solver = false, last_activity_at = v_now
    where id = p_problem_id;
    return jsonb_build_object('result', 'OK', 'resolution', 'REFUND_TO_USER', 'refund', v_refund_result);

  elsif p_resolution in ('SPLIT_SETTLEMENT', 'SPLIT_50_50', 'CUSTOM_SPLIT', 'SETTLE') then
    if not found then
      raise exception 'NO_ESCROW_TO_SPLIT';
    end if;
    v_ratio := greatest(least(coalesce(p_solver_percent, 50), 100), 0) / 100.0;
    v_total := v_escrow.base_amount + v_escrow.extra_amount;
    v_solver_gross := round(v_total * v_ratio, 2);
    v_user_refund := v_total - v_solver_gross;

    v_commission_rate := public.resolve_commission_rate(v_escrow.solver_id);
    v_commission := round(v_solver_gross * (v_commission_rate / 100.0), 2);
    v_solver_net := v_solver_gross - v_commission;

    if v_solver_net > 0 then
      update public.users set balance = balance + v_solver_net, updated_at = v_now where id = v_escrow.solver_id;
      insert into public.transactions (id, problem_id, problem_title, user_id, solver_id, gross_amount,
        commission_percent, commission_amount, net_amount, type, escrow_id, "timestamp")
      values ('TRX_SPLIT_SOLVER_' || v_escrow.id, v_problem.id, v_problem.title, v_problem.user_id, v_escrow.solver_id,
        v_solver_gross, v_commission_rate, v_commission, v_solver_net, 'DISPUTE_SPLIT', v_escrow.id, v_now)
      on conflict (id) do nothing;
    end if;

    if v_user_refund > 0 then
      update public.users set balance = balance + v_user_refund, updated_at = v_now where id = v_escrow.user_id;
      insert into public.transactions (id, problem_id, problem_title, user_id, gross_amount, net_amount, type, escrow_id, refund_type, "timestamp")
      values ('TRX_SPLIT_USER_' || v_escrow.id, v_problem.id, v_problem.title, v_escrow.user_id, v_user_refund, v_user_refund,
        'DISPUTE_SPLIT_REFUND', v_escrow.id, 'DISPUTE_SPLIT', v_now)
      on conflict (id) do nothing;
    end if;

    update public.escrows set status = 'RELEASED', released_at = v_now, updated_at = v_now where id = v_escrow.id;

    update public.problems set
      status = 'COMPLETED',
      dispute_resolution_decision = p_resolution, dispute_resolution_type = 'SPLIT_SETTLEMENT',
      dispute_resolution_note = p_decision_note, dispute_split_solver_percent = p_solver_percent,
      dispute_resolved_at = v_now, dispute_settled_at = v_now, completed_at = v_now,
      dispute_result_seen_by_user = false, dispute_result_seen_by_solver = false, last_activity_at = v_now
    where id = p_problem_id;

    return jsonb_build_object('result', 'OK', 'resolution', 'SPLIT_SETTLEMENT', 'solver_net', v_solver_net, 'user_refund', v_user_refund);
  else
    raise exception 'UNKNOWN_RESOLUTION: %', p_resolution;
  end if;
end;
$function$
;
GRANT EXECUTE ON FUNCTION resolve_dispute(text,text,text,numeric) TO anon;
GRANT EXECUTE ON FUNCTION resolve_dispute(text,text,text,numeric) TO authenticated;
GRANT EXECUTE ON FUNCTION resolve_dispute(text,text,text,numeric) TO postgres;
GRANT EXECUTE ON FUNCTION resolve_dispute(text,text,text,numeric) TO service_role;

-- signature: settle_dispute(text)
CREATE OR REPLACE FUNCTION public.settle_dispute(p_problem_id text)
 RETURNS jsonb
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
declare
  v_problem public.problems%rowtype;
  v_is_solver boolean;
  v_actor_name text;
  v_other_party_id uuid;
  v_now timestamptz := now();
begin
  select * into v_problem from public.problems where id = p_problem_id for update;
  if not found then raise exception 'PROBLEM_NOT_FOUND'; end if;
  if auth.uid() <> v_problem.user_id and auth.uid() <> v_problem.accepted_solver_id then
    raise exception 'NOT_AUTHORIZED';
  end if;
  if not v_problem.is_disputed and v_problem.dispute_settled_at is not null then
    return jsonb_build_object('result', 'ALREADY_SETTLED');
  end if;

  v_is_solver := auth.uid() = v_problem.accepted_solver_id;
  v_actor_name := case when v_is_solver then coalesce(v_problem.accepted_solver_name, 'সমাধানকারী') else v_problem.user_name end;
  v_other_party_id := case when v_is_solver then v_problem.user_id else v_problem.accepted_solver_id end;

  update public.problems set
    is_disputed = false,
    dispute_settled_at = v_now,
    dispute_reason = null,
    dispute_initiator_id = null,
    dispute_initiator_role = null,
    last_activity_at = v_now
  where id = p_problem_id;

  if v_other_party_id is not null then
    insert into public.notifications (id, user_id, title, message, target_type, target_id, related_problem_id, role, "timestamp")
    values ('NOTIF_' || replace(gen_random_uuid()::text, '-', ''), v_other_party_id,
      'সমঝোতা সম্পন্ন হয়েছে 🤝',
      v_actor_name || ' \"' || v_problem.title || '\" কাজের বিরোধে সমঝোতা নিশ্চিত করেছেন। কাজের পরবর্তী ধাপগুলো পুনরায় সক্রিয় হয়েছে।',
      'problem', p_problem_id, p_problem_id, case when v_is_solver then 'USER' else 'SOLVER' end, v_now);
  end if;

  return jsonb_build_object('result', 'OK');
end;
$function$
;
GRANT EXECUTE ON FUNCTION settle_dispute(text) TO "-";
GRANT EXECUTE ON FUNCTION settle_dispute(text) TO anon;
GRANT EXECUTE ON FUNCTION settle_dispute(text) TO authenticated;
GRANT EXECUTE ON FUNCTION settle_dispute(text) TO postgres;
GRANT EXECUTE ON FUNCTION settle_dispute(text) TO service_role;

-- signature: withdraw_dispute(text)
CREATE OR REPLACE FUNCTION public.withdraw_dispute(p_problem_id text)
 RETURNS jsonb
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
declare
  v_problem public.problems%rowtype;
  v_is_solver boolean;
  v_initiator_label text;
  v_other_party_id uuid;
  v_now timestamptz := now();
begin
  select * into v_problem from public.problems where id = p_problem_id for update;
  if not found then raise exception 'PROBLEM_NOT_FOUND'; end if;
  if not v_problem.is_disputed then
    return jsonb_build_object('result', 'NOT_DISPUTED');
  end if;
  if v_problem.dispute_initiator_id is null or auth.uid() <> v_problem.dispute_initiator_id then
    raise exception 'NOT_AUTHORIZED';
  end if;

  v_is_solver := auth.uid() = v_problem.accepted_solver_id;
  v_initiator_label := case when v_is_solver then 'সমাধানকারী' else 'গ্রাহক' end;
  v_other_party_id := case when v_is_solver then v_problem.user_id else v_problem.accepted_solver_id end;

  update public.problems set
    is_disputed = false,
    dispute_reason = null,
    dispute_initiator_id = null,
    dispute_initiator_role = null,
    disputed_at = null,
    last_activity_at = v_now
  where id = p_problem_id;

  if v_other_party_id is not null then
    insert into public.notifications (id, user_id, title, message, target_type, target_id, related_problem_id, role, "timestamp")
    values ('NOTIF_' || replace(gen_random_uuid()::text, '-', ''), v_other_party_id,
      'বিরোধ প্রত্যাহার হয়েছে ✅',
      v_initiator_label || ' \"' || v_problem.title || '\" কাজের বিরোধ প্রত্যাহার করে নিয়েছেন এবং কাজটি পুনরায় স্বাভাবিকভাবে চলছে।',
      'problem', p_problem_id, p_problem_id, case when v_is_solver then 'USER' else 'SOLVER' end, v_now);
  end if;

  return jsonb_build_object('result', 'OK');
end;
$function$
;
GRANT EXECUTE ON FUNCTION withdraw_dispute(text) TO "-";
GRANT EXECUTE ON FUNCTION withdraw_dispute(text) TO anon;
GRANT EXECUTE ON FUNCTION withdraw_dispute(text) TO authenticated;
GRANT EXECUTE ON FUNCTION withdraw_dispute(text) TO postgres;
GRANT EXECUTE ON FUNCTION withdraw_dispute(text) TO service_role;

-- signature: request_admin_assistance(text,text)
CREATE OR REPLACE FUNCTION public.request_admin_assistance(p_problem_id text, p_requester_role text)
 RETURNS jsonb
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
declare
  v_problem public.problems%rowtype;
  v_now timestamptz := now();
  v_role_bangla text;
  v_receiver_id uuid;
  v_msg_id text := 'MSG_' || replace(gen_random_uuid()::text, '-', '');
begin
  select * into v_problem from public.problems where id = p_problem_id for update;
  if not found then raise exception 'PROBLEM_NOT_FOUND'; end if;
  if auth.uid() <> v_problem.user_id and auth.uid() <> v_problem.accepted_solver_id then
    raise exception 'NOT_AUTHORIZED';
  end if;

  v_role_bangla := case when upper(p_requester_role) = 'USER' then 'গ্রাহক' else 'সমাধানকারী' end;
  v_receiver_id := case when upper(p_requester_role) = 'USER' then v_problem.accepted_solver_id else v_problem.user_id end;

  update public.problems set
    is_admin_involved_in_chat = true,
    admin_assistance_requested_by = p_requester_role,
    admin_assistance_requested_at = v_now,
    last_activity_at = v_now
  where id = p_problem_id;

  insert into public.messages (id, problem_id, sender_id, receiver_id, sender_name, content, "timestamp", is_read, is_admin_message)
  values (v_msg_id, p_problem_id, auth.uid(), v_receiver_id, 'অ্যাডমিন সাপোর্ট ডেস্ক 🛡️',
    '🛡️ ' || v_role_bangla || ' অ্যাডমিন সহায়তা চেয়েছেন। অ্যাডমিন টিম দ্রুত এই বিরোধ/চ্যাট পর্যবেক্ষণ করে সহায়তা প্রদান করবেন।',
    v_now, false, true);

  return jsonb_build_object('result', 'OK');
end;
$function$
;
GRANT EXECUTE ON FUNCTION request_admin_assistance(text,text) TO "-";
GRANT EXECUTE ON FUNCTION request_admin_assistance(text,text) TO anon;
GRANT EXECUTE ON FUNCTION request_admin_assistance(text,text) TO authenticated;
GRANT EXECUTE ON FUNCTION request_admin_assistance(text,text) TO postgres;
GRANT EXECUTE ON FUNCTION request_admin_assistance(text,text) TO service_role;

