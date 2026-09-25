-- Step 13.3/13.4 (CI Test Suite session, ২০২৬-০৯-২২) — notifications-broadcast event-naming
-- collision ফিক্স, ব্যবহারকারীর স্পষ্ট অনুমতিতে ("Tumi ei fix suru koro")।
--
-- প্রেক্ষাপট: `realtime_scoping_step1_notifications_broadcast.sql`-এর
-- `notify_notifications_broadcast()` bare TG_OP ('INSERT'/'UPDATE'/'DELETE') event-নামে
-- broadcast করত 'user:<user_id>' topic-এ — এই একই topic ৬টা আরও টেবিল (users, escrows,
-- transactions, withdrawals, gateway_payments, additional_charges) শেয়ার করে, আর তাদের সবারই
-- নিজস্ব "fix" migration আছে যা table-prefixed event-নাম ('users_'||TG_OP ইত্যাদি) ব্যবহার
-- করে — শুধু notifications-ই বাকি ছিল। users-এর ক্ষেত্রে ঠিক এই একই bare-TG_OP প্যাটার্নটাই
-- আগে একবার real bug ছিল (fix_users_broadcast_event_naming_collision.sql)। notifications-এর
-- জন্য এখনো কোনো active collision ঘটছিল না (client-side কোডও bare event-ই subscribe করছিল,
-- তাই দুই পাশ সামঞ্জস্যপূর্ণ ছিল) — কিন্তু ভবিষ্যতে এই topic-এ নতুন কোনো bare-event trigger
-- যোগ হলে notifications-এর সাথে গুলিয়ে যাওয়ার ঝুঁকি ছিল। এই migration সেই ঝুঁকি বন্ধ করে,
-- বাকি ৬টার সাথে consistency আনে।
--
-- ⚠️ এই fix শুধু DB-সাইড না — client-side Kotlin (SupabaseRealtimeManager.kt,
-- `startUserTopicBroadcastSubscription()`)-ও একই সাথে "notifications_"+op event সাবস্ক্রাইব
-- করতে আপডেট করা হয়েছে (একই commit/zip-এ) — একটা বাদ দিলে notification real-time broadcast
-- ভেঙে যেত (যদিও পুরনো table-wide postgresChangeFlow dual-run fallback হিসেবে থেকেই যেত,
-- broadcast পথ কাজ না করলেও)।
--
-- `zz_` প্রিফিক্স — CI/sandbox-এর alphabetical migration-apply-order (`ls supabase/migrations/*.sql
-- | sort`)-এ এই ফাইলটা সবার শেষে পড়ে, যাতে মূল `realtime_scoping_step1_notifications_broadcast.sql`
-- (যেটা 'r'... দিয়ে শুরু, এই ফাইলের 'z'... এর আগে পড়ে) প্রথমে bare-TG_OP দিয়ে function বানানোর
-- পরে, এই ফাইলটাই শেষে re-define করে জেতে (users-এর 13.2-ফিক্সের একই প্যাটার্ন)।
--
-- **live-এ এই সেশনেই সরাসরি apply করা হয়েছে** (mcp__Supabase__apply_migration, project
-- mghvvpndkxnscwryfkib) — users-এর CI-only ফিক্সের থেকে ভিন্ন, কারণ এখানে বাগটা live-এও ছিল।

create or replace function public.notify_notifications_broadcast()
returns trigger
security definer
set search_path = ''
language plpgsql
as $$
begin
  perform realtime.broadcast_changes(
    'user:' || coalesce(NEW.user_id, OLD.user_id)::text,  -- topic
    'notifications_' || TG_OP,                              -- event (আগে বেয়ার TG_OP ছিল)
    TG_OP,                                                  -- operation
    TG_TABLE_NAME,                                          -- table
    TG_TABLE_SCHEMA,                                        -- schema
    NEW,                                                     -- new record
    OLD                                                      -- old record
  );
  return null;
end;
$$;
