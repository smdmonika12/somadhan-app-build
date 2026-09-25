-- সমাধান (Somadhan) — ধাপ ৩৪ (ব্যবহারকারীর অনুরোধে)
--
-- সমস্যা: solver যখন `request_additional_charge()` RPC কল করে extra bill request করত, সেটা
-- শুধু `additional_charges` টেবিলে insert হতো (+ `notifications` টেবিলে একটা entry) -- এই দুটো
-- টেবিলের কোনোটাই `supabase_realtime` publication-এ ছিল না, আর কোনো RPC-ই `messages` টেবিলে
-- কিছু লিখত না। ফলে customer-এর app খোলা অবস্থায় এই request কোথাও লাইভ দেখা যেত না -- না
-- job-tracking/problem-detail স্ক্রিনে, না চ্যাটে -- পরের app restart/pull-to-refresh পর্যন্ত
-- অপেক্ষা করতে হতো (বিস্তারিত বিশ্লেষণ চ্যাট হিস্টোরিতে আছে)।
--
-- সমাধান (দুই ভাগে, ব্যবহারকারীর অনুরোধ অনুযায়ী "1+2 combination"):
--   ১. `additional_charges` টেবিলকে `supabase_realtime` publication-এ যোগ করা -- এতে
--      ProblemDetailScreen.kt-এর `getPendingAdditionalCharges(problemId)` (Room Flow, ইতিমধ্যে
--      reactive) সাথে সাথে নতুন pending charge দেখাবে, যদি অ্যাপ কোনো কোডে এই টেবিলের জন্য
--      realtime channel subscribe করে (Kotlin-সাইড পরিবর্তন আলাদা -- SupabaseRealtimeManager.kt)।
--   ২. `request_additional_charge()` RPC-তে বিদ্যমান `system_event_message()` RPC (ধাপ ৩২.৯৫,
--      dispute flow-এ ইতিমধ্যে ব্যবহৃত) কল করে একটা system-event চ্যাট মেসেজও পাঠানো -- `messages`
--      টেবিল আগে থেকেই realtime, তাই এটা customer-এর ChatScreen-এ সাথে সাথেই (ইতিমধ্যে বিদ্যমান
--      `message.isSystemEvent` রেন্ডারিং দিয়ে, কোনো নতুন UI কোড ছাড়াই) দেখা যাবে।
--
-- `respond_additional_charge()` (accept/reject) এই migration-এ ছোঁয়া হয়নি -- ব্যবহারকারীর
-- অনুরোধ শুধু request-আসার দিকটা নিয়ে ছিল। চাইলে পরে একই প্যাটার্নে (system_event_message কল)
-- accept/reject-এর জন্যও solver-কে live জানানো যোগ করা যায় (আলাদা migration হিসেবে)।

-- ১. Realtime publication
ALTER PUBLICATION supabase_realtime ADD TABLE public.additional_charges;

-- ২. RPC আপডেট -- মূল বডি অবিকল রাখা হয়েছে, শুধু শেষে system_event_message() কল যোগ হলো
CREATE OR REPLACE FUNCTION public.request_additional_charge(p_problem_id text, p_reason text, p_amount numeric)
 RETURNS jsonb
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
declare
  v_problem public.problems%rowtype;
  v_charge_id text := 'EXTRA_' || replace(gen_random_uuid()::text, '-', '');
  v_now timestamptz := now();
begin
  select * into v_problem from public.problems where id = p_problem_id;
  if not found then raise exception 'PROBLEM_NOT_FOUND'; end if;
  if auth.uid() <> v_problem.accepted_solver_id then raise exception 'NOT_AUTHORIZED'; end if;
  if exists (select 1 from public.additional_charges where problem_id = p_problem_id and solver_id = auth.uid() and status = 'PENDING') then
    raise exception 'PENDING_CHARGE_EXISTS';
  end if;

  insert into public.additional_charges (id, problem_id, solver_id, user_id, reason, amount, status, created_at)
  values (v_charge_id, p_problem_id, auth.uid(), v_problem.user_id, p_reason, p_amount, 'PENDING', v_now);

  update public.problems set last_activity_at = v_now where id = p_problem_id;

  insert into public.notifications (id, user_id, title, message, target_type, target_id, related_problem_id, "timestamp")
  values ('NOTIF_' || replace(gen_random_uuid()::text, '-', ''), v_problem.user_id, 'অতিরিক্ত বিল অনুরোধ',
    v_problem.title || '-এর জন্য ৳' || p_amount::text || ' অতিরিক্ত বিল অনুরোধ করা হয়েছে। কারণ: ' || p_reason,
    'additional_charge', v_charge_id, p_problem_id, v_now);

  -- [ধাপ ৩৪] chat-এ live system-event মেসেজ -- messages realtime channel দিয়ে customer সাথে সাথেই
  -- এটা দেখবে (ChatScreen.kt-এর isSystemEvent bubble রেন্ডারিং আগে থেকেই আছে, কোনো নতুন UI লাগেনি)।
  -- system_event_message() নিজেই caller = admin/owner/accepted_solver যাচাই করে -- এখানে caller
  -- ইতিমধ্যে উপরে accepted_solver হিসেবে verify হয়ে গেছে, তাই এই নেস্টেড কল সবসময় pass করবে।
  perform public.system_event_message(
    p_problem_id,
    v_problem.user_id::text,
    'ADDITIONAL_CHARGE_REQUESTED',
    'অতিরিক্ত বিল অনুরোধ করা হয়েছে: ৳' || p_amount::text || ' — কারণ: ' || p_reason
  );

  return jsonb_build_object('result', 'OK', 'charge_id', v_charge_id);
end;
$function$;
