-- [ADMIN_ROLE_PROFILE সেশন ৬] এডমিন-প্রোফাইল ছবির জন্য `admin-profile-photos` Storage bucket + RLS।
--
-- কেন আলাদা bucket (বিদ্যমান `profile-photos` না): ওটা সাধারণ ইউজার/সলভারের; এডমিনের ছবি আলাদা রাখলে
-- (ক) লেখার অধিকার শুধু সক্রিয় এডমিনের (`is_admin(auth.uid())` — নিষ্ক্রিয় এডমিনের জন্য সেশন ১ থেকেই false),
-- (খ) ফাইল-সাইজ/টাইপ সীমা এখানেই আরোপ করা যায়, (গ) মাস্টার প্ল্যানের ধাপ ০ (৪ নং) অনুযায়ী।
--
-- পাথ-নিয়ম: `{uploader auth.uid()}/photo_{timestamp}.jpg` — প্রথম ফোল্ডার = যে আপলোড করছে তার নিজের auth.uid()
-- (বিদ্যমান `profile_photos_owner_write`-এর হুবহু প্যাটার্ন)। সুপার অ্যাডমিন অন্য এডমিনের ছবি বদলালেও ফাইল
-- সুপারের নিজের ফোল্ডারে যায় — ছবির URL `admin_profile_update(p_photo_url)`-এ ওই এডমিনের রো-তে বসে।
-- bucket public: পড়তে লগইন লাগে না, তাই AsyncImage-এ সরাসরি URL চলে (এডমিন-তালিকা/টপ-বার সব জায়গায়)।
--
-- ⚠️ CI-safe: CI-র plain Postgres-এ `storage` স্কিমা নেই (বিদ্যমান bucket-গুলোও রিপোর migration-এ নেই, লাইভে
-- Supabase Dashboard/MCP থেকে বানানো)। তাই পুরো ব্লক `to_regclass` গার্ডের ভেতরে — CI-তে নিঃশব্দে skip,
-- লাইভে apply হয়। কোনো ফাংশন/টেবিল বদলায় না (তাই `scan_duplicate_overloads.sh`-এ প্রভাব নেই)।
-- idempotent: বারবার চালালে একই ফল।

do $$
begin
  if to_regclass('storage.buckets') is null or to_regclass('storage.objects') is null then
    raise notice 'storage schema নেই (CI?) — admin-profile-photos bucket skip করা হলো';
    return;
  end if;

  -- bucket: public, ২MB সীমা (ক্লায়েন্ট আগেই ১০২৪px JPEG-এ কম্প্রেস করে, ~১৫০-৩০০KB), শুধু ছবির টাইপ
  execute $q$
    insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
    values ('admin-profile-photos', 'admin-profile-photos', true, 2097152,
            array['image/jpeg', 'image/png', 'image/webp'])
    on conflict (id) do update
      set public = excluded.public,
          file_size_limit = excluded.file_size_limit,
          allowed_mime_types = excluded.allowed_mime_types
  $q$;

  execute 'drop policy if exists admin_profile_photos_public_read on storage.objects';
  execute $q$
    create policy admin_profile_photos_public_read on storage.objects
      for select to public
      using (bucket_id = 'admin-profile-photos')
  $q$;

  execute 'drop policy if exists admin_profile_photos_admin_write on storage.objects';
  execute $q$
    create policy admin_profile_photos_admin_write on storage.objects
      for insert to authenticated
      with check (bucket_id = 'admin-profile-photos'
                  and public.is_admin(auth.uid())
                  and (storage.foldername(name))[1] = auth.uid()::text)
  $q$;

  execute 'drop policy if exists admin_profile_photos_admin_update on storage.objects';
  execute $q$
    create policy admin_profile_photos_admin_update on storage.objects
      for update to authenticated
      using (bucket_id = 'admin-profile-photos'
             and public.is_admin(auth.uid())
             and (storage.foldername(name))[1] = auth.uid()::text)
  $q$;

  execute 'drop policy if exists admin_profile_photos_admin_delete on storage.objects';
  execute $q$
    create policy admin_profile_photos_admin_delete on storage.objects
      for delete to authenticated
      using (bucket_id = 'admin-profile-photos'
             and public.is_admin(auth.uid())
             and (storage.foldername(name))[1] = auth.uid()::text)
  $q$;
end $$;
