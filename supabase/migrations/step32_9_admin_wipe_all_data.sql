-- ধাপ ৩২.৯ (খ): clearAllDatabaseAndReset()-এর Supabase-সমতুল্য।
-- Firebase clearAllCloudData()-এর প্যারিটি অনুসরণ করে: users/problems/bids/categories/
-- withdrawals/transactions/escrows/ratings/reputation_events/messages/notifications/
-- platform_settings/additional_charges/admin_audit_logs/faqs টেবিলের সব row মুছে দেয়।
-- admin_credentials ও idempotency_keys ইচ্ছাকৃতভাবে বাদ (এগুলোর কোনো Firebase-সমতুল্য
-- collection নেই -- Supabase-নির্দিষ্ট, wipe করলে admin lockout/internal breakage হতে পারে)।
-- auth.users/auth.identities টাচ করা হয় না (ঠিক Firebase Auth users-এর মতোই, শুধু profile
-- ডেটা মোছে -- Firebase Auth account persist করে, Supabase Auth account-ও তাই করবে)।
-- FK নির্ভরতা অনুযায়ী child-to-parent ক্রমে delete করা হয়েছে (pg_constraint দিয়ে verify করা)।
--
-- এই migration সরাসরি লাইভ প্রজেক্টে (mghvvpndkxnscwryfkib) Supabase MCP দিয়ে apply করা হয়েছে
-- (migration নাম: step32_9_admin_wipe_all_data)। এই ফাইলটা শুধু ইতিহাস/রোলব্যাক-রেফারেন্সের
-- জন্য zip-এ রাখা হলো।

create or replace function public.admin_wipe_all_data()
returns jsonb
language plpgsql
security definer
set search_path = public
as $$
begin
  if not is_admin(auth.uid()) then
    raise exception 'NOT_AUTHORIZED' using errcode = '42501';
  end if;

  delete from public.transactions;
  delete from public.messages;
  delete from public.ratings;
  delete from public.reputation_events;
  delete from public.notifications;
  delete from public.gateway_payments;
  delete from public.additional_charges;
  delete from public.escrows;
  delete from public.bids;
  delete from public.problems;
  delete from public.withdrawals;
  delete from public.admin_audit_logs;
  delete from public.users;
  delete from public.categories;
  delete from public.faqs;
  delete from public.platform_settings;

  return jsonb_build_object('result', 'OK');
end;
$$;

grant execute on function public.admin_wipe_all_data() to anon, authenticated;
