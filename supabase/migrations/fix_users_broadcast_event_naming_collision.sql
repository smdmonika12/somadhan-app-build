-- বাগ ফিক্স: notify_users_broadcast() bare TG_OP ("INSERT"/"UPDATE"/"DELETE") event নাম
-- ব্যবহার করছিল, যেটা একই "user:<id>" channel-এ থাকা notifications-এর bare event-নামের
-- সাথে collide করতো (ধাপ ২-এ wallet group-এ ঠিক এই একই ভুল হয়েছিল ও সেখানে
-- realtime_scoping_step2_wallet_event_naming_fix.sql দিয়ে table-prefixed করা হয়েছিল)।
-- escrows-এর trigger শুরু থেকেই ঠিকমতো 'escrows_' || TG_OP প্রিফিক্স ব্যবহার করছিল, তাই সেটায়
-- হাত দেওয়া হয়নি।

create or replace function public.notify_users_broadcast()
returns trigger
language plpgsql
security definer
set search_path to ''
as $function$
begin
  perform realtime.broadcast_changes(
    'user:' || coalesce(new.id, old.id)::text,
    'users_' || TG_OP, TG_OP, TG_TABLE_NAME, TG_TABLE_SCHEMA, new, old
  );
  return null;
end;
$function$;
