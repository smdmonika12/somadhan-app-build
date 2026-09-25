-- ধাপ ৩২.৮ — adminUpdateWithdrawalTrxId() এর জন্য RPC।
-- `withdrawals` টেবিলে কোনো admin UPDATE RLS policy নেই (শুধু withdrawals_select আছে, verify করা
-- হয়েছে pg_policies দিয়ে) -- categories/faqs-এর মতো সরাসরি Postgrest update() করা যাবে না,
-- তাই SECURITY DEFINER RPC লাগবে। এটা process_withdrawal() থেকে আলাদা -- শুধু trx_id সংশোধন করে,
-- withdrawal status/money-movement স্পর্শ করে না।
--
-- [Supabase MCP দিয়ে সরাসরি apply করা হয়েছে এই session-এ, rule #১৩ অনুযায়ী — এই ফাইলটা
--  ইতিহাস/রেকর্ডের জন্য রাখা হলো।]
create or replace function public.admin_update_withdrawal_trx_id(p_withdrawal_id text, p_new_trx_id text)
returns jsonb
language plpgsql
security definer
set search_path to 'public'
as $$
begin
  if not public.is_admin(auth.uid()) then
    raise exception 'NOT_AUTHORIZED';
  end if;

  update public.withdrawals
  set trx_id = p_new_trx_id
  where id = p_withdrawal_id;

  if not found then
    return jsonb_build_object('result', 'WITHDRAWAL_NOT_FOUND');
  end if;

  return jsonb_build_object('result', 'OK');
end;
$$;
