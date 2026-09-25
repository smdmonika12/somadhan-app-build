-- ধাপ ৩২.৯৫ (ব্যবহারকারীর অনুরোধে, ৩৩ শুরুর আগে) — sendSystemEventMessage() গ্যাপ ফিক্স।
--
-- সিস্টেম-জেনারেটেড চ্যাট মেসেজ (dispute opened/withdrawn/settled, admin dispute resolution
-- ইত্যাদি, senderId="SYSTEM" - কোনো real uuid না) সরাসরি client postgrest insert দিয়ে যাওয়া
-- সম্ভব না, কারণ `messages_insert` RLS policy `auth.uid() = sender_id` দাবি করে। তাই একটা
-- SECURITY DEFINER RPC, sender_id NULL রেখে is_system_event=true/system_event_type দিয়ে insert
-- করে (client-এর MessageEntity/MessageDto-তেও sender_id nullable, তাই কোনো schema-সমস্যা নেই)।
--
-- Guard: caller অবশ্যই admin, অথবা problem-এর owner, অথবা accepted_solver হতে হবে -- কোডবেসে
-- এই ফাংশনের ৭টা call-site যাচাই করে দেখা গেছে caller সবসময় এই তিনটার একটা (dispute flow-এ
-- owner/solver উভয়েই ডাকতে পারে, admin dispute-resolution flow-এ admin ডাকে)।
create or replace function public.system_event_message(
  p_problem_id text,
  p_receiver_id text,
  p_event_type text,
  p_content text,
  p_sender_name text default 'সিস্টেম'
)
returns jsonb
language plpgsql
security definer
set search_path to 'public'
as $$
declare
  v_problem public.problems%rowtype;
  v_now timestamptz := now();
  v_msg_id text := 'MSG_' || replace(gen_random_uuid()::text, '-', '');
  v_content text := trim(coalesce(p_content, ''));
  v_receiver_id uuid;
begin
  select * into v_problem from public.problems where id = p_problem_id;
  if not found then
    raise exception 'PROBLEM_NOT_FOUND';
  end if;

  if not (
    public.is_admin(auth.uid())
    or auth.uid() = v_problem.user_id
    or auth.uid() = v_problem.accepted_solver_id
  ) then
    raise exception 'NOT_AUTHORIZED';
  end if;

  if v_content = '' then
    raise exception 'CONTENT_REQUIRED';
  end if;

  begin
    v_receiver_id := nullif(trim(coalesce(p_receiver_id, '')), '')::uuid;
  exception when others then
    v_receiver_id := null;
  end;

  insert into public.messages (
    id, problem_id, sender_id, receiver_id, sender_name, content, "timestamp",
    is_read, is_system_event, system_event_type
  ) values (
    v_msg_id, p_problem_id, null, v_receiver_id, p_sender_name, v_content, v_now,
    false, true, p_event_type
  );

  update public.problems
    set last_activity_at = v_now
    where id = p_problem_id;

  return jsonb_build_object('result', 'OK', 'id', v_msg_id);
end;
$$;

grant execute on function public.system_event_message(text, text, text, text, text) to authenticated;
