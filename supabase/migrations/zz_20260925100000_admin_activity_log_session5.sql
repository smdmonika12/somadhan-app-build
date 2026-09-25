-- [ADMIN_ROLE_PROFILE সেশন ৫] অ্যাক্টিভিটি লগ ট্যাবের ডেটা-সোর্স — সুপার-অনলি, সার্ভার-সাইড ফিল্টার + পেজিনেশন।
--
-- কেন নতুন RPC: বিদ্যমান `AdminAuditLogView` (ট্যাব ১২) শুধু ওই ডিভাইসের নিজের Room-লগ দেখায় (admin_audit_logs
-- কখনো cloud→Room pull হয় না)। মাল্টি-এডমিনে সুপারের দরকার *সব* এডমিনের লগ, তাই সরাসরি ক্লাউড থেকে পড়া।
--
-- `log_admin_action`-এর কল-সাইট বদলানোর দরকার নেই: সেশন ১-এ সেটা auth.uid() থেকে admin_id/admin_name/
-- admin_role_name নিজেই বসায়, আর সেশন ২-এর পর প্রতিটা এডমিন নিজের Supabase Auth সেশনে কাজ করে — লাইভ ডেটায়
-- যাচাই হয়েছে (২০২৬-০৯-২৪-এর পর UPDATE_SETTING/ROLE_CREATED/ADMIN_LOGIN ইত্যাদিতে admin_id বসে আছে)।
--
-- ⚠️ পেজিনেশন কার্সর = (timestamp, id) জোড়া। শুধু timestamp যথেষ্ট না: একই ট্রানজ্যাকশনের অনেক লগ একই
-- `now()` পায় (যেমন reconcile-এর ব্যাচ) — পেজের সীমায় পড়লে সারি বাদ/ডুপ্লিকেট হতো। ক্লায়েন্ট কার্সরের timestamp
-- সার্ভারের ISO স্ট্রিং হুবহু ফেরত পাঠায় (মাইক্রোসেকেন্ড অক্ষত রাখতে; millis-এ পার্স করলে তুলনা ভাঙে)।

create index if not exists admin_audit_logs_ts_idx on public.admin_audit_logs ("timestamp" desc, id desc);

create or replace function public.admin_activity_logs_list(
  p_admin_id uuid default null,
  p_name_query text default ''::text,
  p_unattributed_only boolean default false,
  p_before timestamptz default null,
  p_before_id text default null,
  p_limit integer default 30)
returns jsonb language plpgsql stable security definer set search_path to 'public' as $$
declare
  v_limit integer := least(greatest(coalesce(p_limit, 30), 1), 100);
  v_q text := btrim(coalesce(p_name_query, ''));
  v_pattern text;
  v_rows jsonb;
  v_count integer;
begin
  perform public._admin_require_super();

  -- ILIKE-র বিশেষ অক্ষর (\ % _) escape — নামে "50%" লিখলে যেন সব মেলে না যায়
  v_pattern := '%' || replace(replace(replace(v_q, '\', '\\'), '%', '\%'), '_', '\_') || '%';

  -- limit+1 আনি: বাড়তি একটা সারি আছে মানেই আরও পেজ আছে
  select coalesce(jsonb_agg(jsonb_build_object(
           'id', x.id, 'action_type', x.action_type, 'target_id', x.target_id, 'target_name', x.target_name,
           'details', x.details, 'role', x.role, 'timestamp', x."timestamp",
           'admin_id', x.admin_id, 'admin_name', x.admin_name, 'admin_role_name', x.admin_role_name
         ) order by x."timestamp" desc, x.id desc), '[]'::jsonb),
         count(*)
    into v_rows, v_count
  from (
    select l.id, l.action_type, l.target_id, l.target_name, l.details, l.role, l."timestamp",
           l.admin_id, l.admin_name, l.admin_role_name
    from public.admin_audit_logs l
    where (p_admin_id is null or l.admin_id = p_admin_id)
      and (v_q = '' or l.admin_name ilike v_pattern)
      -- "সিস্টেম / লিগ্যাসি": এডমিন-পরিচয় ছাড়া লগ (পুরনো shared-admin যুগ, সিস্টেম/ইউজার-ট্রিগার্ড ইভেন্ট)
      and (not coalesce(p_unattributed_only, false) or l.admin_name = '')
      and (p_before is null or (l."timestamp", l.id) < (p_before, coalesce(p_before_id, chr(1114111))))
    order by l."timestamp" desc, l.id desc
    limit v_limit + 1
  ) x;

  if v_count > v_limit then
    -- বাড়তি (সবচেয়ে পুরনো) সারিটা বাদ দিয়ে has_more = true
    return jsonb_build_object(
      'rows', (select coalesce(jsonb_agg(e.elem order by e.ord), '[]'::jsonb)
                 from jsonb_array_elements(v_rows) with ordinality as e(elem, ord)
                where e.ord <= v_limit),
      'has_more', true);
  end if;
  return jsonb_build_object('rows', v_rows, 'has_more', false);
end $$;

revoke execute on function public.admin_activity_logs_list(uuid, text, boolean, timestamptz, text, integer) from public, anon;
grant execute on function public.admin_activity_logs_list(uuid, text, boolean, timestamptz, text, integer) to authenticated, service_role;
