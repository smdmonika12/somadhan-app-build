-- RECOVERED from live DB on 2026-09-18, verbatim capture, no logic changes. See
-- RPC_SYNC_FIX_PROGRESS.md Step 2.
-- Functions in this file (Step 1 missing-RPC group): submit_kyc, submit_rating, submit_reputation_event

-- signature: submit_kyc(text,text,text,text,text,text,text,text)
CREATE OR REPLACE FUNCTION public.submit_kyc(p_first_name text, p_last_name text, p_address text, p_document_type text, p_document_number text, p_doc_front_url text, p_doc_back_url text, p_selfie_url text)
 RETURNS jsonb
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
declare
  v_uid uuid := auth.uid();
  v_now timestamptz := now();
  v_updated_rows int;
begin
  if v_uid is null then
    raise exception 'NOT_AUTHENTICATED';
  end if;

  update public.users set
    kyc_first_name = p_first_name,
    kyc_last_name = p_last_name,
    kyc_address = p_address,
    kyc_document_type = p_document_type,
    kyc_document_number = p_document_number,
    kyc_document_front_image = p_doc_front_url,
    kyc_document_back_image = p_doc_back_url,
    kyc_selfie_image = p_selfie_url,
    kyc_status = 'PENDING',
    is_kyc_verified = false,
    kyc_submission_date = v_now,
    updated_at = v_now
  where id = v_uid;

  get diagnostics v_updated_rows = row_count;
  if v_updated_rows = 0 then
    raise exception 'USER_ROW_NOT_FOUND_FOR_UID: %', v_uid;
  end if;

  insert into public.notifications (id, user_id, title, message, target_type, target_id, role, "timestamp")
  values (
    'NOTIF_' || replace(gen_random_uuid()::text, '-', ''),
    v_uid,
    'KYC আবেদন গৃহীত হয়েছে',
    'আপনার KYC ভেরিফিকেশন আবেদন অ্যাডমিন পর্যালোচনার জন্য জমা হয়েছে। শীঘ্রই স্ট্যাটাস জানানো হবে।',
    'kyc',
    v_uid,
    'SOLVER',
    v_now
  );

  return jsonb_build_object('result', 'OK', 'user_id', v_uid, 'kyc_status', 'PENDING');
end;
$function$
;
GRANT EXECUTE ON FUNCTION submit_kyc(text,text,text,text,text,text,text,text) TO "-";
GRANT EXECUTE ON FUNCTION submit_kyc(text,text,text,text,text,text,text,text) TO anon;
GRANT EXECUTE ON FUNCTION submit_kyc(text,text,text,text,text,text,text,text) TO authenticated;
GRANT EXECUTE ON FUNCTION submit_kyc(text,text,text,text,text,text,text,text) TO postgres;
GRANT EXECUTE ON FUNCTION submit_kyc(text,text,text,text,text,text,text,text) TO service_role;

-- signature: submit_rating(text,integer,text,text)
CREATE OR REPLACE FUNCTION public.submit_rating(p_problem_id text, p_stars integer, p_comment text, p_rater_role text)
 RETURNS jsonb
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
declare
  v_problem public.problems%rowtype;
  v_rating_id text := 'RATE_' || replace(gen_random_uuid()::text, '-', '');
begin
  select * into v_problem from public.problems where id = p_problem_id;
  if not found then raise exception 'PROBLEM_NOT_FOUND'; end if;
  if p_rater_role not in ('USER', 'SOLVER') then raise exception 'INVALID_RATER_ROLE'; end if;
  if p_rater_role = 'USER' and auth.uid() <> v_problem.user_id then raise exception 'NOT_AUTHORIZED'; end if;
  if p_rater_role = 'SOLVER' and auth.uid() <> v_problem.accepted_solver_id then raise exception 'NOT_AUTHORIZED'; end if;

  insert into public.ratings (id, problem_id, problem_title, user_id, solver_id, stars, comment, rater_role, created_at)
  values (v_rating_id, p_problem_id, v_problem.title, v_problem.user_id, v_problem.accepted_solver_id,
    p_stars, p_comment, p_rater_role, now());

  return jsonb_build_object('result', 'OK', 'rating_id', v_rating_id);
end;
$function$
;
GRANT EXECUTE ON FUNCTION submit_rating(text,integer,text,text) TO anon;
GRANT EXECUTE ON FUNCTION submit_rating(text,integer,text,text) TO authenticated;
GRANT EXECUTE ON FUNCTION submit_rating(text,integer,text,text) TO postgres;
GRANT EXECUTE ON FUNCTION submit_rating(text,integer,text,text) TO service_role;

-- signature: submit_reputation_event(uuid,text,text,numeric,text)
CREATE OR REPLACE FUNCTION public.submit_reputation_event(p_user_id uuid, p_event_type text, p_ref_id text DEFAULT NULL::text, p_score_change numeric DEFAULT NULL::numeric, p_note text DEFAULT NULL::text)
 RETURNS jsonb
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
declare
  v_caller uuid := auth.uid();
  v_event text := upper(coalesce(p_event_type, ''));
  v_score numeric;
  v_cap numeric;
  v_score_key text;
  v_cap_key text;
  v_today_gain numeric;
  v_note text;
  v_id text := 'REP_' || replace(gen_random_uuid()::text, '-', '');
  v_already boolean;
  v_user record;
  v_new_score numeric;
  v_rating record;
  v_withdrawal record;
  v_charge record;
  v_already_gained numeric;
  v_cap_per_problem numeric;
  v_problem record;
  v_bid record;
  v_authorized boolean;
begin
  if v_caller is null then
    raise exception 'AUTH_REQUIRED';
  end if;
  if p_user_id is null then
    raise exception 'USER_ID_REQUIRED';
  end if;

  ---------------------------------------------------------------------------
  -- 1) ADMIN_ADJUSTMENT: admin-only, uncapped, direct amount from admin
  ---------------------------------------------------------------------------
  if v_event = 'ADMIN_ADJUSTMENT' then
    if not is_admin(v_caller) then
      raise exception 'ADMIN_ONLY';
    end if;
    if p_score_change is null then
      raise exception 'SCORE_CHANGE_REQUIRED';
    end if;

    select * into v_user from public.users where id = p_user_id for update;
    if not found then raise exception 'USER_NOT_FOUND'; end if;

    v_new_score := least(greatest(v_user.reputation_score + p_score_change, 0), 100);
    update public.users set reputation_score = v_new_score, updated_at = now() where id = p_user_id;

    insert into public.reputation_events (id, user_id, event_type, problem_id, score_change, score_after, note, created_at)
    values (v_id, p_user_id, v_event, null, p_score_change, v_new_score,
            coalesce(p_note, 'অ্যাডমিন কর্তৃক রেপুটেশন সমন্বয়'), now());

    return jsonb_build_object('result', 'OK', 'id', v_id, 'score_change', p_score_change, 'new_score', v_new_score);
  end if;

  ---------------------------------------------------------------------------
  -- 2) EXTRA_CHARGE_VIA_APP / EXTRA_CHARGE_ACCEPTED: per-problem cap (not daily)
  --    p_ref_id = problem_id. Mirrors applyCappedPerProblemReputation() exactly:
  --    shared cap across both event types, for this user, on this problem.
  --    No separate replay-guard here -- same as the original Kotlin code, the
  --    cap itself bounds repeat calls (a replay just re-hits the same ceiling).
  ---------------------------------------------------------------------------
  if v_event in ('EXTRA_CHARGE_VIA_APP', 'EXTRA_CHARGE_ACCEPTED') then
    if p_ref_id is null then raise exception 'REF_ID_REQUIRED'; end if;
    if p_user_id <> v_caller then raise exception 'NOT_ELIGIBLE'; end if;

    if v_event = 'EXTRA_CHARGE_VIA_APP' then
      select * into v_charge from public.additional_charges
        where problem_id = p_ref_id and solver_id = v_caller and status = 'ACCEPTED'
        order by responded_at desc nulls last limit 1;
    else
      select * into v_charge from public.additional_charges
        where problem_id = p_ref_id and user_id = v_caller and status = 'ACCEPTED'
        order by responded_at desc nulls last limit 1;
    end if;
    if not found then raise exception 'NOT_ELIGIBLE'; end if;

    v_score := case when v_event = 'EXTRA_CHARGE_VIA_APP'
      then least(1.0 + (v_charge.amount / 500.0 * 0.5), 3.0)
      else 0.5
    end;

    select coalesce(value::numeric, 10.0) into v_cap_per_problem
      from public.platform_settings where key = 'extra_bill_reputation_cap_per_problem';
    if v_cap_per_problem is null then v_cap_per_problem := 10.0; end if;

    select coalesce(sum(score_change), 0) into v_already_gained
      from public.reputation_events
      where user_id = p_user_id and problem_id = p_ref_id
        and event_type in ('EXTRA_CHARGE_VIA_APP', 'EXTRA_CHARGE_ACCEPTED');

    v_score := least(greatest(v_cap_per_problem - v_already_gained, 0), greatest(v_score, 0));
    if v_score <= 0 then
      return jsonb_build_object('result', 'DAILY_CAP_REACHED');
    end if;
    v_note := coalesce(p_note, v_event);

    select * into v_user from public.users where id = p_user_id for update;
    if not found then raise exception 'USER_NOT_FOUND'; end if;
    v_new_score := least(greatest(v_user.reputation_score + v_score, 0), 100);
    update public.users set reputation_score = v_new_score, updated_at = now() where id = p_user_id;

    insert into public.reputation_events (id, user_id, event_type, problem_id, score_change, score_after, note, created_at)
    values (v_id, p_user_id, v_event, p_ref_id, v_score, v_new_score, v_note, now());

    return jsonb_build_object('result', 'OK', 'id', v_id, 'score_change', v_score, 'new_score', v_new_score);
  end if;

  ---------------------------------------------------------------------------
  -- 3) Remaining event types: daily-capped, one-time-per-ref guard
  --    এখানে eligibility p_user_id (টার্গেট)-ভিত্তিক, authorization আলাদাভাবে চেক করা হয়
  ---------------------------------------------------------------------------
  if v_event not in ('BID_WON', 'JOB_COMPLETED', 'PROBLEM_POSTED', 'RATING_BONUS', 'WITHDRAWAL_COMPLETED') then
    raise exception 'UNSUPPORTED_EVENT_TYPE';
  end if;

  if v_event = 'BID_WON' then
    if p_ref_id is null then raise exception 'REF_ID_REQUIRED'; end if;
    select * into v_bid from public.bids where problem_id = p_ref_id and solver_id = p_user_id and status = 'ACCEPTED';
    if not found then raise exception 'NOT_ELIGIBLE'; end if;
    select * into v_problem from public.problems where id = p_ref_id;
    v_authorized := (v_caller = p_user_id) or (v_problem.user_id = v_caller) or is_admin(v_caller);
    if not v_authorized then raise exception 'NOT_ELIGIBLE'; end if;
    v_score_key := 'rep_score_bid_won'; v_cap_key := 'rep_cap_daily_bid_won';
    select coalesce((select value::numeric from public.platform_settings where key = v_score_key), 0.5) into v_score;
    v_note := coalesce(p_note, 'বিড জিতে কাজ পেয়েছেন');

  elsif v_event = 'JOB_COMPLETED' then
    if p_ref_id is null then raise exception 'REF_ID_REQUIRED'; end if;
    select * into v_problem from public.problems
      where id = p_ref_id and status = 'COMPLETED' and (user_id = p_user_id or accepted_solver_id = p_user_id);
    if not found then raise exception 'NOT_ELIGIBLE'; end if;
    v_authorized := (v_caller = v_problem.user_id) or (v_caller = v_problem.accepted_solver_id) or is_admin(v_caller);
    if not v_authorized then raise exception 'NOT_ELIGIBLE'; end if;
    v_score_key := 'rep_score_job_completed'; v_cap_key := 'rep_cap_daily_job_completed';
    select coalesce((select value::numeric from public.platform_settings where key = v_score_key), 0.5) into v_score;
    v_note := coalesce(p_note, 'কাজ সম্পন্ন করেছেন');

  elsif v_event = 'PROBLEM_POSTED' then
    if p_ref_id is null then raise exception 'REF_ID_REQUIRED'; end if;
    if not exists (select 1 from public.problems where id = p_ref_id and user_id = p_user_id) then
      raise exception 'NOT_ELIGIBLE';
    end if;
    v_authorized := (v_caller = p_user_id) or is_admin(v_caller);
    if not v_authorized then raise exception 'NOT_ELIGIBLE'; end if;
    v_score_key := 'rep_score_problem_posted'; v_cap_key := 'rep_cap_daily_problem_posted';
    select coalesce((select value::numeric from public.platform_settings where key = v_score_key), 0.2) into v_score;
    v_note := coalesce(p_note, 'নতুন সমস্যা পোস্ট করেছেন');

  elsif v_event = 'RATING_BONUS' then
    if p_ref_id is null then raise exception 'REF_ID_REQUIRED'; end if;
    select * into v_rating from public.ratings
      where problem_id = p_ref_id and solver_id = p_user_id and stars >= 4
      limit 1;
    if not found then raise exception 'NOT_ELIGIBLE'; end if;
    v_authorized := (v_caller = p_user_id) or (v_caller = v_rating.user_id) or is_admin(v_caller);
    if not v_authorized then raise exception 'NOT_ELIGIBLE'; end if;
    v_cap_key := 'rep_cap_daily_rating';
    if v_rating.stars = 5 then
      select coalesce((select value::numeric from public.platform_settings where key = 'rep_score_rating_5_star'), 1.5) into v_score;
    else
      select coalesce((select value::numeric from public.platform_settings where key = 'rep_score_rating_4_star'), 0.5) into v_score;
    end if;
    v_note := coalesce(p_note, v_rating.stars || ' স্টার রেটিং পেয়েছেন');

  elsif v_event = 'WITHDRAWAL_COMPLETED' then
    if p_ref_id is null then raise exception 'REF_ID_REQUIRED'; end if;
    select * into v_withdrawal from public.withdrawals
      where id = p_ref_id and solver_id = p_user_id and status = 'COMPLETED';
    if not found then raise exception 'NOT_ELIGIBLE'; end if;
    v_authorized := (v_caller = p_user_id) or is_admin(v_caller);
    if not v_authorized then raise exception 'NOT_ELIGIBLE'; end if;
    v_cap_key := 'rep_cap_daily_withdrawal';
    select coalesce((select value::numeric from public.platform_settings where key = 'rep_rate_withdrawal_per_100'), 0.1)
      into v_score;
    v_score := (v_withdrawal.amount / 100.0) * v_score;
    v_note := coalesce(p_note, 'সফলভাবে উইথড্র সম্পন্ন করেছেন');
  end if;

  -- one-time-per-ref guard (each problem/withdrawal can only award this event type once)
  select exists(
    select 1 from public.reputation_events
    where user_id = p_user_id and event_type = v_event and problem_id = p_ref_id
  ) into v_already;
  if v_already then
    return jsonb_build_object('result', 'ALREADY_CLAIMED');
  end if;

  if v_score <= 0 then
    return jsonb_build_object('result', 'SKIPPED_ZERO_SCORE');
  end if;

  select coalesce((select value::numeric from public.platform_settings where key = v_cap_key), 2.0) into v_cap;

  select coalesce(sum(score_change), 0) into v_today_gain
    from public.reputation_events
    where user_id = p_user_id and event_type = v_event and created_at >= now() - interval '24 hours';

  if v_today_gain >= v_cap then
    return jsonb_build_object('result', 'DAILY_CAP_REACHED');
  end if;

  v_score := least(v_score, greatest(v_cap - v_today_gain, 0));
  if v_score <= 0 then
    return jsonb_build_object('result', 'DAILY_CAP_REACHED');
  end if;

  select * into v_user from public.users where id = p_user_id for update;
  if not found then raise exception 'USER_NOT_FOUND'; end if;
  v_new_score := least(greatest(v_user.reputation_score + v_score, 0), 100);
  update public.users set reputation_score = v_new_score, updated_at = now() where id = p_user_id;

  insert into public.reputation_events (id, user_id, event_type, problem_id, score_change, score_after, note, created_at)
  values (v_id, p_user_id, v_event, p_ref_id, v_score, v_new_score, v_note, now());

  return jsonb_build_object('result', 'OK', 'id', v_id, 'score_change', v_score, 'new_score', v_new_score);
end;
$function$
;
GRANT EXECUTE ON FUNCTION submit_reputation_event(uuid,text,text,numeric,text) TO "-";
GRANT EXECUTE ON FUNCTION submit_reputation_event(uuid,text,text,numeric,text) TO anon;
GRANT EXECUTE ON FUNCTION submit_reputation_event(uuid,text,text,numeric,text) TO authenticated;
GRANT EXECUTE ON FUNCTION submit_reputation_event(uuid,text,text,numeric,text) TO postgres;
GRANT EXECUTE ON FUNCTION submit_reputation_event(uuid,text,text,numeric,text) TO service_role;

-- signature: submit_reputation_event(uuid,text,text,numeric,text,text)
CREATE OR REPLACE FUNCTION public.submit_reputation_event(p_user_id uuid, p_event_type text, p_ref_id text DEFAULT NULL::text, p_score_change numeric DEFAULT NULL::numeric, p_note text DEFAULT NULL::text, p_role text DEFAULT NULL::text)
 RETURNS jsonb
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
declare
  v_caller uuid := auth.uid();
  v_event text := upper(coalesce(p_event_type, ''));
  v_score numeric;
  v_cap numeric;
  v_score_key text;
  v_cap_key text;
  v_today_gain numeric;
  v_note text;
  v_id text := 'REP_' || replace(gen_random_uuid()::text, '-', '');
  v_already boolean;
  v_user record;
  v_new_score numeric;
  v_rating record;
  v_withdrawal record;
  v_charge record;
  v_already_gained numeric;
  v_cap_per_problem numeric;
  v_problem record;
  v_bid record;
  v_authorized boolean;
  v_role text;
  v_role_active boolean;
  v_penalty numeric;
  v_min_inactive_days int;
begin
  if v_caller is null then
    raise exception 'AUTH_REQUIRED';
  end if;
  if p_user_id is null then
    raise exception 'USER_ID_REQUIRED';
  end if;

  ---------------------------------------------------------------------------
  -- 1) ADMIN_ADJUSTMENT: admin-only, uncapped, direct amount from admin
  ---------------------------------------------------------------------------
  if v_event = 'ADMIN_ADJUSTMENT' then
    if not is_admin(v_caller) then
      raise exception 'ADMIN_ONLY';
    end if;
    if p_score_change is null then
      raise exception 'SCORE_CHANGE_REQUIRED';
    end if;

    if p_role is null or p_role not in ('USER', 'SOLVER') then
      raise exception 'ROLE_REQUIRED_FOR_ADMIN_ADJUSTMENT';
    end if;
    v_role := p_role;

    select * into v_user from public.users where id = p_user_id for update;
    if not found then raise exception 'USER_NOT_FOUND'; end if;

    v_role_active := case when v_role = 'SOLVER' then v_user.has_solver_role else v_user.has_user_role end;
    if not coalesce(v_role_active, false) then
      raise exception 'ROLE_INACTIVE';
    end if;

    v_new_score := least(greatest(v_user.reputation_score + p_score_change, 0), 100);

    if v_role = 'SOLVER' then
      update public.users set reputation_score = v_new_score,
        reputation_score_solver = least(greatest(reputation_score_solver + p_score_change, 0), 100),
        updated_at = now() where id = p_user_id;
    else
      update public.users set reputation_score = v_new_score,
        reputation_score_user = least(greatest(reputation_score_user + p_score_change, 0), 100),
        updated_at = now() where id = p_user_id;
    end if;

    insert into public.reputation_events (id, user_id, event_type, problem_id, score_change, score_after, note, role, created_at)
    values (v_id, p_user_id, v_event, null, p_score_change, v_new_score,
            coalesce(p_note, 'অ্যাডমিন কর্তৃক রেপুটেশন সমন্বয়'), v_role, now());

    return jsonb_build_object('result', 'OK', 'id', v_id, 'score_change', p_score_change, 'new_score', v_new_score);
  end if;

  ---------------------------------------------------------------------------
  -- 1.5) [ধাপ ৩৩.১, ব্যবহারকারীর সিদ্ধান্তে যোগ] INACTIVE_7_DAYS / INACTIVE_30_DAYS:
  --      নিষ্ক্রিয়তা-জনিত reputation decay। কোনো ref_id নেই (নির্দিষ্ট problem/bid-এর সাথে
  --      সম্পর্কিত না) — তাই সাধারণ one-time-per-ref গার্ড এখানে প্রযোজ্য না (সেটা ব্যবহার করলে
  --      problem_id=null মিলিয়ে প্রথম কলের পরেই চিরতরে ALREADY_CLAIMED হয়ে যেত)। বরং এখানে
  --      users.last_reputation_decay_check_at কলাম দিয়ে rate-limit করা হয় (client-side
  --      ৭-দিনের debounce-এর সার্ভার-সাইড আয়না)। penalty amount client থেকে বিশ্বাস না করে
  --      platform_settings থেকে নিজেই independently গণনা করা হয় (বাকি সব supported event
  --      type-এর মতো একই ডিজাইন-দর্শন)।
  ---------------------------------------------------------------------------
  if v_event in ('INACTIVE_7_DAYS', 'INACTIVE_30_DAYS') then
    v_authorized := (v_caller = p_user_id) or is_admin(v_caller)
      or exists (select 1 from public.users where id = v_caller and linked_account_id = p_user_id)
      or exists (select 1 from public.users where id = p_user_id and linked_account_id = v_caller);
    if not v_authorized then raise exception 'NOT_ELIGIBLE'; end if;

    if p_role is null or p_role not in ('USER', 'SOLVER') then
      raise exception 'ROLE_REQUIRED_FOR_INACTIVE_DECAY';
    end if;
    v_role := p_role;

    select * into v_user from public.users where id = p_user_id for update;
    if not found then raise exception 'USER_NOT_FOUND'; end if;

    v_role_active := case when v_role = 'SOLVER' then v_user.has_solver_role else v_user.has_user_role end;
    if not coalesce(v_role_active, false) then
      raise exception 'ROLE_INACTIVE';
    end if;

    -- rate-limit: client-side একই ৭-দিনের debounce সার্ভার-সাইডে আয়না করা
    if v_user.last_reputation_decay_check_at is not null
       and now() - v_user.last_reputation_decay_check_at < interval '7 days' then
      return jsonb_build_object('result', 'ALREADY_CHECKED_RECENTLY');
    end if;

    -- server-side নিজেই যাচাই করে নেয় user সত্যিই যথেষ্ট নিষ্ক্রিয় কিনা (client দাবির ওপর ভরসা না করে)
    v_min_inactive_days := case when v_event = 'INACTIVE_30_DAYS' then 30 else 7 end;
    if now() - v_user.updated_at < (v_min_inactive_days || ' days')::interval then
      update public.users set last_reputation_decay_check_at = now() where id = p_user_id;
      return jsonb_build_object('result', 'NOT_YET_INACTIVE');
    end if;

    v_score_key := case when v_event = 'INACTIVE_30_DAYS' then 'rep_penalty_inactive_30d' else 'rep_penalty_inactive_7d' end;
    select coalesce((select value::numeric from public.platform_settings where key = v_score_key),
                     case when v_event = 'INACTIVE_30_DAYS' then 5.0 else 2.0 end) into v_penalty;
    v_score := -1 * abs(v_penalty);
    v_note := coalesce(p_note, case when v_event = 'INACTIVE_30_DAYS'
      then '৩০ দিন নিষ্ক্রিয় থাকার কারণে রেপুটেশন হ্রাস' else '৭ দিন নিষ্ক্রিয় থাকার কারণে রেপুটেশন হ্রাস' end);

    v_new_score := least(greatest(v_user.reputation_score + v_score, 0), 100);

    if v_role = 'SOLVER' then
      update public.users set reputation_score = v_new_score,
        reputation_score_solver = least(greatest(reputation_score_solver + v_score, 0), 100),
        last_reputation_decay_check_at = now(), updated_at = now() where id = p_user_id;
    else
      update public.users set reputation_score = v_new_score,
        reputation_score_user = least(greatest(reputation_score_user + v_score, 0), 100),
        last_reputation_decay_check_at = now(), updated_at = now() where id = p_user_id;
    end if;

    insert into public.reputation_events (id, user_id, event_type, problem_id, score_change, score_after, note, role, created_at)
    values (v_id, p_user_id, v_event, null, v_score, v_new_score, v_note, v_role, now());

    return jsonb_build_object('result', 'OK', 'id', v_id, 'score_change', v_score, 'new_score', v_new_score);
  end if;

  ---------------------------------------------------------------------------
  -- 2) EXTRA_CHARGE_VIA_APP / EXTRA_CHARGE_ACCEPTED: per-problem cap (not daily)
  --    p_ref_id = problem_id. Mirrors applyCappedPerProblemReputation() exactly:
  --    shared cap across both event types, for this user, on this problem.
  --    No separate replay-guard here -- same as the original Kotlin code, the
  --    cap itself bounds repeat calls (a replay just re-hits the same ceiling).
  ---------------------------------------------------------------------------
  if v_event in ('EXTRA_CHARGE_VIA_APP', 'EXTRA_CHARGE_ACCEPTED') then
    if p_ref_id is null then raise exception 'REF_ID_REQUIRED'; end if;
    if p_user_id <> v_caller then raise exception 'NOT_ELIGIBLE'; end if;

    v_role := case when v_event = 'EXTRA_CHARGE_VIA_APP' then 'SOLVER' else 'USER' end;

    if v_event = 'EXTRA_CHARGE_VIA_APP' then
      select * into v_charge from public.additional_charges
        where problem_id = p_ref_id and solver_id = v_caller and status = 'ACCEPTED'
        order by responded_at desc nulls last limit 1;
    else
      select * into v_charge from public.additional_charges
        where problem_id = p_ref_id and user_id = v_caller and status = 'ACCEPTED'
        order by responded_at desc nulls last limit 1;
    end if;
    if not found then raise exception 'NOT_ELIGIBLE'; end if;

    v_score := case when v_event = 'EXTRA_CHARGE_VIA_APP'
      then least(1.0 + (v_charge.amount / 500.0 * 0.5), 3.0)
      else 0.5
    end;

    select coalesce(value::numeric, 10.0) into v_cap_per_problem
      from public.platform_settings where key = 'extra_bill_reputation_cap_per_problem';
    if v_cap_per_problem is null then v_cap_per_problem := 10.0; end if;

    select coalesce(sum(score_change), 0) into v_already_gained
      from public.reputation_events
      where user_id = p_user_id and problem_id = p_ref_id
        and event_type in ('EXTRA_CHARGE_VIA_APP', 'EXTRA_CHARGE_ACCEPTED');

    v_score := least(greatest(v_cap_per_problem - v_already_gained, 0), greatest(v_score, 0));
    if v_score <= 0 then
      return jsonb_build_object('result', 'DAILY_CAP_REACHED');
    end if;
    v_note := coalesce(p_note, v_event);

    select * into v_user from public.users where id = p_user_id for update;
    if not found then raise exception 'USER_NOT_FOUND'; end if;

    v_role_active := case when v_role = 'SOLVER' then v_user.has_solver_role else v_user.has_user_role end;
    if not coalesce(v_role_active, false) then
      raise exception 'ROLE_INACTIVE';
    end if;

    v_new_score := least(greatest(v_user.reputation_score + v_score, 0), 100);

    if v_role = 'SOLVER' then
      update public.users set reputation_score = v_new_score,
        reputation_score_solver = least(greatest(reputation_score_solver + v_score, 0), 100),
        updated_at = now() where id = p_user_id;
    else
      update public.users set reputation_score = v_new_score,
        reputation_score_user = least(greatest(reputation_score_user + v_score, 0), 100),
        updated_at = now() where id = p_user_id;
    end if;

    insert into public.reputation_events (id, user_id, event_type, problem_id, score_change, score_after, note, role, created_at)
    values (v_id, p_user_id, v_event, p_ref_id, v_score, v_new_score, v_note, v_role, now());

    return jsonb_build_object('result', 'OK', 'id', v_id, 'score_change', v_score, 'new_score', v_new_score);
  end if;

  ---------------------------------------------------------------------------
  -- 3) Remaining event types: daily-capped, one-time-per-ref guard
  --    এখানে eligibility p_user_id (টার্গেট)-ভিত্তিক, authorization আলাদাভাবে চেক করা হয়
  ---------------------------------------------------------------------------
  if v_event not in ('BID_WON', 'JOB_COMPLETED', 'PROBLEM_POSTED', 'RATING_BONUS', 'WITHDRAWAL_COMPLETED') then
    raise exception 'UNSUPPORTED_EVENT_TYPE';
  end if;

  if v_event = 'BID_WON' then
    if p_ref_id is null then raise exception 'REF_ID_REQUIRED'; end if;
    select * into v_bid from public.bids where problem_id = p_ref_id and solver_id = p_user_id and status = 'ACCEPTED';
    if not found then raise exception 'NOT_ELIGIBLE'; end if;
    select * into v_problem from public.problems where id = p_ref_id;
    v_authorized := (v_caller = p_user_id) or (v_problem.user_id = v_caller) or is_admin(v_caller);
    if not v_authorized then raise exception 'NOT_ELIGIBLE'; end if;
    v_score_key := 'rep_score_bid_won'; v_cap_key := 'rep_cap_daily_bid_won';
    select coalesce((select value::numeric from public.platform_settings where key = v_score_key), 0.5) into v_score;
    v_note := coalesce(p_note, 'বিড জিতে কাজ পেয়েছেন');
    v_role := 'SOLVER';

  elsif v_event = 'JOB_COMPLETED' then
    if p_ref_id is null then raise exception 'REF_ID_REQUIRED'; end if;
    select * into v_problem from public.problems
      where id = p_ref_id and status = 'COMPLETED' and (user_id = p_user_id or accepted_solver_id = p_user_id);
    if not found then raise exception 'NOT_ELIGIBLE'; end if;
    v_authorized := (v_caller = v_problem.user_id) or (v_caller = v_problem.accepted_solver_id) or is_admin(v_caller);
    if not v_authorized then raise exception 'NOT_ELIGIBLE'; end if;
    v_score_key := 'rep_score_job_completed'; v_cap_key := 'rep_cap_daily_job_completed';
    select coalesce((select value::numeric from public.platform_settings where key = v_score_key), 0.5) into v_score;
    v_note := coalesce(p_note, 'কাজ সম্পন্ন করেছেন');
    v_role := case when p_user_id = v_problem.user_id then 'USER' else 'SOLVER' end;

  elsif v_event = 'PROBLEM_POSTED' then
    if p_ref_id is null then raise exception 'REF_ID_REQUIRED'; end if;
    if not exists (select 1 from public.problems where id = p_ref_id and user_id = p_user_id) then
      raise exception 'NOT_ELIGIBLE';
    end if;
    v_authorized := (v_caller = p_user_id) or is_admin(v_caller);
    if not v_authorized then raise exception 'NOT_ELIGIBLE'; end if;
    v_score_key := 'rep_score_problem_posted'; v_cap_key := 'rep_cap_daily_problem_posted';
    select coalesce((select value::numeric from public.platform_settings where key = v_score_key), 0.2) into v_score;
    v_note := coalesce(p_note, 'নতুন সমস্যা পোস্ট করেছেন');
    v_role := 'USER';

  elsif v_event = 'RATING_BONUS' then
    if p_ref_id is null then raise exception 'REF_ID_REQUIRED'; end if;
    select * into v_rating from public.ratings
      where problem_id = p_ref_id and solver_id = p_user_id and stars >= 4
      limit 1;
    if not found then raise exception 'NOT_ELIGIBLE'; end if;
    v_authorized := (v_caller = p_user_id) or (v_caller = v_rating.user_id) or is_admin(v_caller);
    if not v_authorized then raise exception 'NOT_ELIGIBLE'; end if;
    v_cap_key := 'rep_cap_daily_rating';
    if v_rating.stars = 5 then
      select coalesce((select value::numeric from public.platform_settings where key = 'rep_score_rating_5_star'), 1.5) into v_score;
    else
      select coalesce((select value::numeric from public.platform_settings where key = 'rep_score_rating_4_star'), 0.5) into v_score;
    end if;
    v_note := coalesce(p_note, v_rating.stars || ' স্টার রেটিং পেয়েছেন');
    v_role := 'SOLVER';

  elsif v_event = 'WITHDRAWAL_COMPLETED' then
    if p_ref_id is null then raise exception 'REF_ID_REQUIRED'; end if;
    select * into v_withdrawal from public.withdrawals
      where id = p_ref_id and solver_id = p_user_id and status = 'COMPLETED';
    if not found then raise exception 'NOT_ELIGIBLE'; end if;
    v_authorized := (v_caller = p_user_id) or is_admin(v_caller);
    if not v_authorized then raise exception 'NOT_ELIGIBLE'; end if;
    v_cap_key := 'rep_cap_daily_withdrawal';
    select coalesce((select value::numeric from public.platform_settings where key = 'rep_rate_withdrawal_per_100'), 0.1)
      into v_score;
    v_score := (v_withdrawal.amount / 100.0) * v_score;
    v_note := coalesce(p_note, 'সফলভাবে উইথড্র সম্পন্ন করেছেন');
    v_role := 'SOLVER';
  end if;

  select exists(
    select 1 from public.reputation_events
    where user_id = p_user_id and event_type = v_event and problem_id = p_ref_id
  ) into v_already;
  if v_already then
    return jsonb_build_object('result', 'ALREADY_CLAIMED');
  end if;

  if v_score <= 0 then
    return jsonb_build_object('result', 'SKIPPED_ZERO_SCORE');
  end if;

  select coalesce((select value::numeric from public.platform_settings where key = v_cap_key), 2.0) into v_cap;

  select coalesce(sum(score_change), 0) into v_today_gain
    from public.reputation_events
    where user_id = p_user_id and event_type = v_event and created_at >= now() - interval '24 hours';

  if v_today_gain >= v_cap then
    return jsonb_build_object('result', 'DAILY_CAP_REACHED');
  end if;

  v_score := least(v_score, greatest(v_cap - v_today_gain, 0));
  if v_score <= 0 then
    return jsonb_build_object('result', 'DAILY_CAP_REACHED');
  end if;

  select * into v_user from public.users where id = p_user_id for update;
  if not found then raise exception 'USER_NOT_FOUND'; end if;

  v_role_active := case when v_role = 'SOLVER' then v_user.has_solver_role else v_user.has_user_role end;
  if not coalesce(v_role_active, false) then
    raise exception 'ROLE_INACTIVE';
  end if;

  v_new_score := least(greatest(v_user.reputation_score + v_score, 0), 100);

  if v_role = 'SOLVER' then
    update public.users set reputation_score = v_new_score,
      reputation_score_solver = least(greatest(reputation_score_solver + v_score, 0), 100),
      updated_at = now() where id = p_user_id;
  else
    update public.users set reputation_score = v_new_score,
      reputation_score_user = least(greatest(reputation_score_user + v_score, 0), 100),
      updated_at = now() where id = p_user_id;
  end if;

  insert into public.reputation_events (id, user_id, event_type, problem_id, score_change, score_after, note, role, created_at)
  values (v_id, p_user_id, v_event, p_ref_id, v_score, v_new_score, v_note, v_role, now());

  return jsonb_build_object('result', 'OK', 'id', v_id, 'score_change', v_score, 'new_score', v_new_score);
end;
$function$
;
GRANT EXECUTE ON FUNCTION submit_reputation_event(uuid,text,text,numeric,text,text) TO "-";
GRANT EXECUTE ON FUNCTION submit_reputation_event(uuid,text,text,numeric,text,text) TO anon;
GRANT EXECUTE ON FUNCTION submit_reputation_event(uuid,text,text,numeric,text,text) TO authenticated;
GRANT EXECUTE ON FUNCTION submit_reputation_event(uuid,text,text,numeric,text,text) TO postgres;
GRANT EXECUTE ON FUNCTION submit_reputation_event(uuid,text,text,numeric,text,text) TO service_role;

