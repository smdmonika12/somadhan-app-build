-- সমাধান (Somadhan) — ধাপ ৩৫ (ব্যবহারকারীর অনুরোধে)
--
-- সমস্যা: `notifications` আর `withdrawals` -- এই দুইটা টেবিল এতদিন `supabase_realtime`
-- publication-এর বাইরে ছিল (SupabaseRealtimeManager.kt-এর কমেন্টে আগে থেকেই লেখা ছিল: "এই
-- ৮টাই এখন পর্যন্ত realtime channel পেয়েছে... notifications/withdrawals... সেগুলোর কোনোটারই
-- এখনো Supabase Realtime channel নেই")। ফলাফল: notification পাঠানো/মুছে ফেলা হলে বা withdrawal
-- request/approve/reject হলে সেটা অন্য ডিভাইসে (বা এমনকি একই ডিভাইসের অন্য স্ক্রিনে) app
-- restart বা manual pull-to-refresh ছাড়া লাইভ দেখা যেত না -- users/problems/bids/messages/
-- transactions/escrows/gateway_payments/additional_charges-এর মতো বাকি ৮টা টেবিলের বিপরীতে।
--
-- সমাধান (Kotlin-সাইড পরিবর্তন আলাদা -- SupabaseRealtimeManager.kt/MessageTransactionMappers.kt,
-- ঠিক ধাপ ৩৪-এর additional_charges প্যাটার্ন অনুসরণ করে):
--   ১. `notifications` টেবিলকে `supabase_realtime` publication-এ যোগ করা।
--   ২. `withdrawals` টেবিলকে `supabase_realtime` publication-এ যোগ করা।
--
-- নোট: RLS policy (SELECT/UPDATE) দুই টেবিলেই আগে থেকেই ঠিক আছে -- এই migration শুধু
-- publication-ভিত্তিক realtime broadcast চালু করছে, কোনো RLS policy বদলাচ্ছে না। INSERT/DELETE
-- (notifications-এর জন্য `admin_broadcast_notification`/`create_notification`/
-- `admin_delete_notification_group`, withdrawals-এর জন্য `request_withdrawal`/
-- `process_withdrawal`) সবই আগে থেকেই SECURITY DEFINER RPC দিয়ে হয় (client-side সরাসরি
-- INSERT/DELETE policy লাগে না, RPC নিজেই RLS বাইপাস করে) -- এই অংশটা ইতিমধ্যে ঠিক আছে,
-- SomadhanRepository.kt-এর `sendManualNotification`/`deleteScheduledNotification`/
-- `deleteNotificationGroup` কমেন্টে ("[SUPABASE-MIGRATED - ধাপ ১২]") verify করা।

ALTER PUBLICATION supabase_realtime ADD TABLE public.notifications;
ALTER PUBLICATION supabase_realtime ADD TABLE public.withdrawals;
