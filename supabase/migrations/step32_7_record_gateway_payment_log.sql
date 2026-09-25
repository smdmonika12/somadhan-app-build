-- ধাপ ৩২.৭ — recordGatewayPayment()-এর bookkeeping/audit-log row (FirebaseSyncManager.
-- syncGatewayPayment() এর সমতুল্য)। লাইভ DB থেকে হুবহু sync করা।

create or replace function public.record_gateway_payment_log(
  p_id text,
  p_gateway_trx_id text,
  p_user_id uuid,
  p_user_name text,
  p_user_phone text,
  p_amount numeric,
  p_gateway text,
  p_purpose text default 'ESCROW_PAYMENT',
  p_problem_id text default '',
  p_problem_title text default '',
  p_status text default 'SUCCESS',
  p_note text default '',
  p_role text default 'USER'
)
returns jsonb
language plpgsql
security definer
set search_path to 'public'
as $$
declare
  v_caller uuid := auth.uid();
begin
  if v_caller is null then
    raise exception 'AUTH_REQUIRED';
  end if;
  if p_id is null or p_gateway_trx_id is null or p_user_id is null or p_amount is null then
    raise exception 'MISSING_PARAMS';
  end if;
  if not (v_caller = p_user_id or is_admin(v_caller)) then
    raise exception 'NOT_AUTHORIZED';
  end if;
  if p_role not in ('USER', 'SOLVER') then
    raise exception 'INVALID_ROLE';
  end if;

  insert into public.gateway_payments (
    id, gateway_trx_id, user_id, user_name, user_phone, amount, gateway,
    purpose, problem_id, problem_title, status, note, role, "timestamp"
  ) values (
    p_id, p_gateway_trx_id, p_user_id, coalesce(p_user_name, ''), coalesce(p_user_phone, ''),
    p_amount, coalesce(p_gateway, ''), coalesce(p_purpose, 'ESCROW_PAYMENT'), coalesce(p_problem_id, ''),
    coalesce(p_problem_title, ''), coalesce(p_status, 'SUCCESS'), coalesce(p_note, ''),
    coalesce(p_role, 'USER'), now()
  )
  on conflict (id) do nothing;

  return jsonb_build_object('result', 'OK', 'id', p_id);
end;
$$;

grant execute on function public.record_gateway_payment_log(text, text, uuid, text, text, numeric, text, text, text, text, text, text, text) to anon, authenticated;
