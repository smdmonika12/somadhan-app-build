-- RECOVERED from live DB on 2026-09-18, verbatim capture, no logic changes. See
-- RPC_SYNC_FIX_PROGRESS.md Step 2.
-- Functions in this file (Step 1 missing-RPC group): admin_credentials_get_phone, admin_credentials_update, admin_credentials_verify_password

-- signature: admin_credentials_get_phone()
CREATE OR REPLACE FUNCTION public.admin_credentials_get_phone()
 RETURNS text
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
declare
  v_phone text;
begin
  select phone into v_phone from public.admin_credentials where id = 1;
  return v_phone; -- row না থাকলে null -- client fallback করবে local default-এ
end;
$function$
;
GRANT EXECUTE ON FUNCTION admin_credentials_get_phone() TO anon;
GRANT EXECUTE ON FUNCTION admin_credentials_get_phone() TO authenticated;
GRANT EXECUTE ON FUNCTION admin_credentials_get_phone() TO postgres;
GRANT EXECUTE ON FUNCTION admin_credentials_get_phone() TO service_role;

-- signature: admin_credentials_update(text,text,text)
CREATE OR REPLACE FUNCTION public.admin_credentials_update(p_current_password text, p_new_phone text, p_new_password_hash text)
 RETURNS boolean
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public', 'extensions'
AS $function$
declare
  v_row public.admin_credentials%rowtype;
begin
  select * into v_row from public.admin_credentials where id = 1;

  if v_row.id is null then
    insert into public.admin_credentials (id, phone, password_hash)
    values (
      1,
      coalesce(p_new_phone, '01700000000'),
      coalesce(p_new_password_hash, extensions.crypt(p_current_password, extensions.gen_salt('bf')))
    );
    return true;
  end if;

  if v_row.password_hash <> extensions.crypt(p_current_password, v_row.password_hash) then
    return false;
  end if;

  update public.admin_credentials
  set
    phone = coalesce(p_new_phone, phone),
    password_hash = coalesce(p_new_password_hash, password_hash),
    updated_at = now()
  where id = 1;

  return true;
end;
$function$
;
GRANT EXECUTE ON FUNCTION admin_credentials_update(text,text,text) TO anon;
GRANT EXECUTE ON FUNCTION admin_credentials_update(text,text,text) TO authenticated;
GRANT EXECUTE ON FUNCTION admin_credentials_update(text,text,text) TO postgres;
GRANT EXECUTE ON FUNCTION admin_credentials_update(text,text,text) TO service_role;

-- signature: admin_credentials_verify_password(text)
CREATE OR REPLACE FUNCTION public.admin_credentials_verify_password(p_password text)
 RETURNS boolean
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public', 'extensions'
AS $function$
declare
  v_hash text;
begin
  select password_hash into v_hash from public.admin_credentials where id = 1;
  if v_hash is null then
    return false;
  end if;
  return v_hash = extensions.crypt(p_password, v_hash);
end;
$function$
;
GRANT EXECUTE ON FUNCTION admin_credentials_verify_password(text) TO anon;
GRANT EXECUTE ON FUNCTION admin_credentials_verify_password(text) TO authenticated;
GRANT EXECUTE ON FUNCTION admin_credentials_verify_password(text) TO postgres;
GRANT EXECUTE ON FUNCTION admin_credentials_verify_password(text) TO service_role;

