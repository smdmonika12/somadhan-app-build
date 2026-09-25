-- ধাপ ৩২.৮ — deleteUser() (admin) এর জন্য soft-delete সমতুল্য।
--
-- এই session-এ Supabase MCP দিয়ে verify করা হয়েছে (information_schema.columns):
--   `users` টেবিলে কোনো is_deleted কলাম আগে থেকে ছিল না।
-- আর FK constraint যাচাই করে দেখা গেছে (information_schema.referential_constraints):
--   bids/escrows/transactions/withdrawals/gateway_payments/messages/ratings/notifications/
--   reputation_events/problems — এই সবকটা টেবিলেই users(id)-এর ওপর FK আছে delete_rule='NO ACTION'
--   দিয়ে। অর্থাৎ Kotlin/Firestore পাশের মতো সরাসরি hard-DELETE করলে বাস্তবে প্রায় প্রতিটা
--   active user-এর জন্যই FK ভায়োলেশন এরর দেবে (Firestore-এ কোনো FK নেই বলে ওই পাশে এই সমস্যা
--   কখনো ধরা পড়েনি)।
--
-- তাই এখানে ইচ্ছাকৃতভাবে hard-delete-এর বদলে soft-delete flag যোগ করা হলো — এটা literal
-- Firebase-parity থেকে একটা সচেতন, নথিভুক্ত বিচ্যুতি, কারণ literal hard-delete এই Postgres
-- schema-তে নিরাপদ/সম্ভব না।
--
-- এই flag আপাতত শুধু "মার্ক করা" পর্যন্তই সীমাবদ্ধ:
--   - লগইন ব্লক করা (is_deleted=true হলে auth flow-এ reject করা) — এই migration-এর scope-এ নেই।
--   - auth.users থেকে আসল রেকর্ড সরানো — client থেকে সম্ভব না (service_role লাগবে), ধাপ ৩০-এর
--     admin-reset-user-password Edge Function প্যাটার্নে ভবিষ্যতে করা যেতে পারে।
-- এই দুটো সিদ্ধান্তই ইচ্ছাকৃতভাবে খোলা রাখা হলো — পরবর্তী ব্যবহারকারী/session-এর সুনির্দিষ্ট
-- সিদ্ধান্তের অপেক্ষায় (MIGRATION_PROGRESS.md-এ নথিভুক্ত)।
--
-- [Supabase MCP দিয়ে সরাসরি apply করা হয়েছে এই session-এ, rule #১৩ অনুযায়ী — এই ফাইলটা
--  ইতিহাস/রেকর্ডের জন্য রাখা হলো।]
alter table public.users add column if not exists is_deleted boolean not null default false;

create or replace function public.admin_soft_delete_user(p_user_id uuid)
returns jsonb
language plpgsql
security definer
set search_path to 'public'
as $$
begin
  if not public.is_admin(auth.uid()) then
    raise exception 'NOT_AUTHORIZED';
  end if;

  update public.users
  set is_deleted = true, updated_at = now()
  where id = p_user_id;

  if not found then
    return jsonb_build_object('result', 'USER_NOT_FOUND');
  end if;

  return jsonb_build_object('result', 'OK');
end;
$$;
