-- ধাপ ৩২.৭ — addToEscrow()-এর atomic increment (FirebaseSyncManager.incrementEscrowExtraAmount()
-- এর সমতুল্য)। এই ফাইলটা লাইভ DB-তে apply করা statement থেকে হুবহু sync করা (Supabase MCP দিয়ে
-- schema_migrations.statements পড়ে)।

create or replace function public.increment_escrow_extra_amount(p_escrow_id text, p_amount numeric)
returns jsonb
language plpgsql
security definer
set search_path to 'public'
as $$
declare
  v_caller uuid := auth.uid();
  v_escrow record;
begin
  if v_caller is null then
    raise exception 'AUTH_REQUIRED';
  end if;
  if p_amount is null then
    raise exception 'MISSING_PARAMS';
  end if;

  select id, user_id, solver_id, extra_amount into v_escrow
  from public.escrows
  where id = p_escrow_id
  for update;

  if v_escrow.id is null then
    raise exception 'ESCROW_NOT_FOUND';
  end if;

  if not (v_caller = v_escrow.user_id or v_caller = v_escrow.solver_id or is_admin(v_caller)) then
    raise exception 'NOT_AUTHORIZED';
  end if;

  update public.escrows
  set extra_amount = coalesce(extra_amount, 0) + p_amount,
      updated_at = now()
  where id = p_escrow_id;

  return jsonb_build_object('result', 'OK', 'escrow_id', p_escrow_id);
end;
$$;

grant execute on function public.increment_escrow_extra_amount(text, numeric) to anon, authenticated;
