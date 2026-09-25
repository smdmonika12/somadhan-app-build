-- RECOVERED from live DB on 2026-09-18, verbatim capture, no logic changes. See
-- RPC_SYNC_FIX_PROGRESS.md Step 2.
-- Functions in this file (Step 1 missing-RPC group): cancel_job_release_request, reject_job_release_request, request_job_release

-- signature: cancel_job_release_request(text)
CREATE OR REPLACE FUNCTION public.cancel_job_release_request(p_problem_id text)
 RETURNS jsonb
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
declare
  v_problem public.problems%rowtype;
  v_was_disputed boolean;
  v_now timestamptz := now();
begin
  select * into v_problem from public.problems where id = p_problem_id for update;
  if not found then raise exception 'PROBLEM_NOT_FOUND'; end if;
  if auth.uid() <> v_problem.accepted_solver_id then
    raise exception 'NOT_AUTHORIZED';
  end if;

  v_was_disputed := v_problem.is_disputed;

  if v_was_disputed then
    update public.problems set
      has_release_request = false,
      release_request_extra_amount = 0,
      release_request_note = '',
      release_requested_at = null,
      is_disputed = false,
      dispute_reason = null,
      dispute_initiator_id = null,
      dispute_initiator_role = null,
      disputed_at = null,
      is_admin_involved_in_chat = false,
      admin_assistance_requested_by = null,
      admin_assistance_requested_at = null,
      last_activity_at = v_now
    where id = p_problem_id;
  else
    update public.problems set
      has_release_request = false,
      release_request_extra_amount = 0,
      release_request_note = '',
      release_requested_at = null,
      last_activity_at = v_now
    where id = p_problem_id;
  end if;

  insert into public.notifications (id, user_id, title, message, target_type, target_id, related_problem_id, "timestamp")
  values ('NOTIF_' || replace(gen_random_uuid()::text, '-', ''), v_problem.user_id,
    case when v_was_disputed then 'বিরোধ বন্ধ ও রিলিজ অনুরোধ প্রত্যাহার' else 'রিলিজের অনুরোধ প্রত্যাহার করা হয়েছে' end,
    case when v_was_disputed
      then coalesce(v_problem.accepted_solver_name, 'সমাধানকারী') || ' "' || v_problem.title || '" কাজের বিরোধ বন্ধ করে রিলিজ অনুরোধ প্রত্যাহার করেছেন।'
      else coalesce(v_problem.accepted_solver_name, 'সমাধানকারী') || ' "' || v_problem.title || '" কাজের রিলিজ অনুরোধ সাময়িকভাবে প্রত্যাহার করেছেন।'
    end,
    'problem', p_problem_id, p_problem_id, v_now);

  return jsonb_build_object('result', 'OK', 'was_disputed', v_was_disputed);
end;
$function$
;
GRANT EXECUTE ON FUNCTION cancel_job_release_request(text) TO "-";
GRANT EXECUTE ON FUNCTION cancel_job_release_request(text) TO anon;
GRANT EXECUTE ON FUNCTION cancel_job_release_request(text) TO authenticated;
GRANT EXECUTE ON FUNCTION cancel_job_release_request(text) TO postgres;
GRANT EXECUTE ON FUNCTION cancel_job_release_request(text) TO service_role;

-- signature: reject_job_release_request(text,text)
CREATE OR REPLACE FUNCTION public.reject_job_release_request(p_problem_id text, p_reason text)
 RETURNS jsonb
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
declare
  v_problem public.problems%rowtype;
  v_escrow public.escrows%rowtype;
  v_total_accepted_extra numeric(12,2);
  v_now timestamptz := now();
  v_reason_text text := trim(coalesce(p_reason, ''));
begin
  select * into v_problem from public.problems where id = p_problem_id for update;
  if not found then raise exception 'PROBLEM_NOT_FOUND'; end if;
  if auth.uid() <> v_problem.user_id then
    raise exception 'NOT_AUTHORIZED';
  end if;

  update public.problems set
    has_release_request = false,
    release_request_extra_amount = 0,
    release_request_note = '',
    release_requested_at = null,
    last_activity_at = v_now
  where id = p_problem_id;

  select * into v_escrow from public.escrows where problem_id = p_problem_id order by created_at desc limit 1 for update;
  if found and v_escrow.extra_amount > 0 then
    select coalesce(sum(amount), 0) into v_total_accepted_extra
    from public.additional_charges where problem_id = p_problem_id and status = 'ACCEPTED';
    update public.escrows set extra_amount = v_total_accepted_extra, updated_at = v_now where id = v_escrow.id;
  end if;

  if v_problem.accepted_solver_id is not null then
    insert into public.notifications (id, user_id, title, message, target_type, target_id, related_problem_id, "timestamp")
    values ('NOTIF_' || replace(gen_random_uuid()::text, '-', ''), v_problem.accepted_solver_id,
      'রিলিজের অনুরোধ প্রত্যাখ্যাত ❌',
      v_problem.user_name || ' আপনার "' || v_problem.title || '" কাজের রিলিজ অনুরোধ প্রত্যাখ্যান করেছেন' ||
        (case when v_reason_text <> '' then ' (কারণ: ' || v_reason_text || ')' else '' end) ||
        '। কাজটি সম্পন্ন বা সংশোধন করে পুনরায় অনুরোধ পাঠাতে পারবেন।',
      'problem', p_problem_id, p_problem_id, v_now);
  end if;

  return jsonb_build_object('result', 'OK');
end;
$function$
;
GRANT EXECUTE ON FUNCTION reject_job_release_request(text,text) TO "-";
GRANT EXECUTE ON FUNCTION reject_job_release_request(text,text) TO anon;
GRANT EXECUTE ON FUNCTION reject_job_release_request(text,text) TO authenticated;
GRANT EXECUTE ON FUNCTION reject_job_release_request(text,text) TO postgres;
GRANT EXECUTE ON FUNCTION reject_job_release_request(text,text) TO service_role;

-- signature: request_job_release(text,numeric,text)
CREATE OR REPLACE FUNCTION public.request_job_release(p_problem_id text, p_extra_amount numeric, p_note text)
 RETURNS jsonb
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
declare
  v_problem public.problems%rowtype;
  v_safe_extra numeric(12,2) := greatest(coalesce(p_extra_amount, 0), 0);
  v_note_text text := trim(coalesce(p_note, ''));
  v_now timestamptz := now();
  v_charge_id text;
  v_total numeric(12,2);
begin
  select * into v_problem from public.problems where id = p_problem_id for update;
  if not found then raise exception 'PROBLEM_NOT_FOUND'; end if;
  if auth.uid() <> v_problem.accepted_solver_id then
    raise exception 'NOT_AUTHORIZED';
  end if;

  update public.problems set
    has_release_request = true,
    release_request_extra_amount = v_safe_extra,
    release_request_note = v_note_text,
    release_requested_at = v_now,
    last_activity_at = v_now
  where id = p_problem_id;

  if v_safe_extra > 0 then
    v_charge_id := 'EXTRA_' || replace(gen_random_uuid()::text, '-', '');
    insert into public.additional_charges (id, problem_id, solver_id, user_id, reason, amount, status, created_at)
    values (v_charge_id, p_problem_id, v_problem.accepted_solver_id, v_problem.user_id,
      case when v_note_text = '' then 'কাজের অতিরিক্ত বিল রিকোয়েস্ট' else v_note_text end,
      v_safe_extra, 'PENDING', v_now);
  end if;

  v_total := coalesce(v_problem.accepted_amount, 0) + v_safe_extra;

  insert into public.notifications (id, user_id, title, message, target_type, target_id, related_problem_id, "timestamp")
  values ('NOTIF_' || replace(gen_random_uuid()::text, '-', ''), v_problem.user_id,
    'কাজ সম্পন্ন ও অর্থ রিলিজের অনুরোধ এসেছে! 🔔',
    coalesce(v_problem.accepted_solver_name, 'সমাধানকারী') || ' "' || v_problem.title || '" কাজটি সম্পন্ন করেছেন এবং ৳' || v_total::text || ' রিলিজের অনুরোধ পাঠিয়েছেন। অনুগ্রহ করে ৪৮ ঘণ্টার মধ্যে যাচাই করে পেমেন্ট রিলিজ করুন।',
    'problem', p_problem_id, p_problem_id, v_now);

  return jsonb_build_object('result', 'OK', 'charge_id', v_charge_id);
end;
$function$
;
GRANT EXECUTE ON FUNCTION request_job_release(text,numeric,text) TO "-";
GRANT EXECUTE ON FUNCTION request_job_release(text,numeric,text) TO anon;
GRANT EXECUTE ON FUNCTION request_job_release(text,numeric,text) TO authenticated;
GRANT EXECUTE ON FUNCTION request_job_release(text,numeric,text) TO postgres;
GRANT EXECUTE ON FUNCTION request_job_release(text,numeric,text) TO service_role;

