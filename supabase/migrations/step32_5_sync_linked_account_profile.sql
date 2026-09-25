-- [ধাপ ৩২.৫] Category-৩ গ্যাপ #১ ফিক্স — linked-account (User<->Solver dual-role) profile-sync loop।
--
-- প্রেক্ষাপট: `SomadhanRepository.updateUser()`-এর linked-profile-sync loop এতদিন শুধু
-- FirebaseSyncManager.syncUser(syncedLinked) কল করত, কোনো Supabase কল ছিল না -- কারণ পুরনো
-- ধারণা ছিল RLS-এ ভিন্ন id-এর (অন্য auth.uid()) row লেখার কোনো অনুমতি নেই। কিন্তু DB-তে গিয়ে
-- (rule #11) `is_same_account_family()` ফাংশন ও `users_update_own` policy পরীক্ষা করে দেখা গেছে
-- linked family-র মধ্যে UPDATE আসলে RLS দিয়েই অনুমোদিত -- শুধু ক্লায়েন্ট কোডে সেই কলটা কখনো
-- লেখা হয়নি। তাও, RLS পুরোপুরি bypass না করে (defense-in-depth) এবং pre-link duplicate
-- (একই ফোন/ইমেইল কিন্তু linked_account_id এখনো সেট হয়নি এমন) রো-ও কভার করার জন্য এই RPC
-- (SECURITY DEFINER) বানানো হলো -- caller ও target সত্যিই একই পরিবার/duplicate কিনা সার্ভার-সাইডে
-- নিজে verify করে, তারপর non-sensitive column গুলো sync করে (password/balance/role/is_banned/
-- kyc_* কিছুই ছোঁয় না -- ঠিক `update_own_profile`-এর RLS-permitted column সেটের মতোই সংকীর্ণ)।

create or replace function public.sync_linked_account_profile(
  p_target_user_id uuid,
  p_name text default null,
  p_phone text default null,
  p_email text default null,
  p_address text default null,
  p_latitude double precision default null,
  p_longitude double precision default null,
  p_profile_image_uri text default null,
  p_is_verified_badge boolean default null
) returns jsonb
language plpgsql
security definer
set search_path = public
as $$
declare
  v_caller uuid := auth.uid();
  v_root_id uuid;
  v_authorized boolean;
begin
  if v_caller is null then
    raise exception 'not authenticated';
  end if;

  if p_target_user_id = v_caller then
    -- নিজের row -- এটা এই RPC-এর কাজ না, update_own_profile ব্যবহার করা উচিত। তবু নিরাপদে allow।
    v_authorized := true;
  else
    select (
      public.is_same_account_family(v_caller, p_target_user_id)
      or exists (
        select 1
        from public.users u_caller, public.users u_target
        where u_caller.id = v_caller
          and u_target.id = p_target_user_id
          and (
            (u_caller.phone = u_target.phone and coalesce(u_caller.phone, '') <> '')
            or (u_caller.email = u_target.email and coalesce(u_caller.email, '') <> '')
          )
      )
    ) into v_authorized;
  end if;

  if not v_authorized then
    raise exception 'not authorized to sync this linked account';
  end if;

  select coalesce(linked_account_id, id) into v_root_id from public.users where id = v_caller;

  update public.users
  set
    name = coalesce(p_name, name),
    phone = coalesce(p_phone, phone),
    email = coalesce(p_email, email),
    address = coalesce(p_address, address),
    latitude = coalesce(p_latitude, latitude),
    longitude = coalesce(p_longitude, longitude),
    profile_image_uri = coalesce(p_profile_image_uri, profile_image_uri),
    is_verified_badge = coalesce(p_is_verified_badge, is_verified_badge),
    linked_account_id = coalesce(v_root_id, linked_account_id),
    updated_at = now()
  where id = p_target_user_id;

  return jsonb_build_object('success', true, 'target_id', p_target_user_id);
end;
$$;

grant execute on function public.sync_linked_account_profile(
  uuid, text, text, text, text, double precision, double precision, text, boolean
) to authenticated;
