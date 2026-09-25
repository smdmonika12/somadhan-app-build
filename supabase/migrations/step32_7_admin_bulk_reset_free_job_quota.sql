-- ধাপ ৩২.৭ — runMonthlyFreeQuotaReset()-এর সমতুল্য: cron/admin-triggered bulk reset, client-side
-- loop না করে একটা কলেই সব ইউজারের ফ্রি-কোটা রিসেট করে। লাইভ DB থেকে হুবহু sync করা।

create or replace function public.admin_bulk_reset_free_job_quota(p_month_key text)
returns jsonb
language plpgsql
security definer
set search_path to 'public'
as $$
declare
  v_caller uuid := auth.uid();
  v_month_key text := coalesce(p_month_key, to_char(now(), 'YYYY-MM'));
  v_rows int;
begin
  if v_caller is null then
    raise exception 'AUTH_REQUIRED';
  end if;
  if not is_admin(v_caller) then
    raise exception 'NOT_AUTHORIZED';
  end if;

  update public.users
  set free_jobs_used_this_month = 0,
      free_jobs_month_key = v_month_key,
      updated_at = now();

  get diagnostics v_rows = row_count;

  return jsonb_build_object('result', 'OK', 'month_key', v_month_key, 'rows_affected', v_rows);
end;
$$;

grant execute on function public.admin_bulk_reset_free_job_quota(text) to anon, authenticated;
