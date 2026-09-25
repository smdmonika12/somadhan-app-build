-- ধাপ ৫ (ঐচ্ছিক) — users/escrows Broadcast + Topic Scoping
-- realtime.messages-এ আগে থেকেই একটা generic "users can receive own user-topic broadcasts"
-- policy আছে (topic = 'user:' || auth.uid()) -- notifications/transactions/withdrawals/
-- gateway_payments/additional_charges সবাই এটাই ব্যবহার করছে, তাই users/escrows-এর জন্য নতুন
-- RLS policy লাগছে না, শুধু trigger।

-- users: single-owner, topic user:<id> (নিজের id-ই owner)
create or replace function public.notify_users_broadcast()
returns trigger
language plpgsql
security definer
set search_path to ''
as $function$
begin
  perform realtime.broadcast_changes(
    'user:' || coalesce(new.id, old.id)::text,
    TG_OP, TG_OP, TG_TABLE_NAME, TG_TABLE_SCHEMA, new, old
  );
  return null;
end;
$function$;

create trigger broadcast_users_changes
after insert or update or delete on public.users
for each row execute function public.notify_users_broadcast();

-- escrows: dual-owner (user_id ও solver_id দুটো কলামই) -- transactions-এর প্যাটার্ন অনুসরণ করে
-- দুই topic-এই broadcast
create or replace function public.notify_escrows_broadcast()
returns trigger
language plpgsql
security definer
set search_path to ''
as $function$
begin
  if coalesce(new.user_id, old.user_id) is not null then
    perform realtime.broadcast_changes(
      'user:' || coalesce(new.user_id, old.user_id)::text,
      'escrows_' || TG_OP, TG_OP, TG_TABLE_NAME, TG_TABLE_SCHEMA, new, old
    );
  end if;
  if coalesce(new.solver_id, old.solver_id) is not null then
    perform realtime.broadcast_changes(
      'user:' || coalesce(new.solver_id, old.solver_id)::text,
      'escrows_' || TG_OP, TG_OP, TG_TABLE_NAME, TG_TABLE_SCHEMA, new, old
    );
  end if;
  return null;
end;
$function$;

create trigger broadcast_escrows_changes
after insert or update or delete on public.escrows
for each row execute function public.notify_escrows_broadcast();
