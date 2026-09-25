-- ধাপ ২৯ — Additional Charges ডাবল-ডিডাকশন বাগ ফিক্স — 🔴 টাকা-সংক্রান্ত
--
-- বুক-কিপিং-অনলি RPC: confirmReleaseAndComplete()-এর extra-amount settlement অংশে একটা বিদ্যমান
-- PENDING additional_charges row-কে ACCEPTED হিসেবে চিহ্নিত করার জন্য।
--
-- কেন আলাদা RPC দরকার (respond_additional_charge() ব্যবহার করা যাবে না):
-- respond_additional_charge() হলো OTHER flow-এর জন্য (SomadhanRepository.respondToAdditionalCharge()
-- -- গ্রাহক app-এ Accept/Reject বাটন চাপলে) -- সেই flow-এ টাকা তখনো user-এর wallet থেকে কাটা হয়নি,
-- তাই RPC নিজেই wallet deduct করে escrows.extra_amount-এ যোগ করে।
--
-- কিন্তু confirmReleaseAndComplete()-এ ততক্ষণে টাকা ইতিমধ্যে নড়ে গেছে দুইভাবে:
--   ১) walletDeduction প্যারামিটার দিয়ে সরাসরি user-এর wallet থেকে deduct হয় (confirmReleaseAndComplete
--      ফাংশনের একদম শুরুতে), এবং/অথবা
--   ২) payoutEscrowToSolver() -> release_escrow RPC দিয়ে escrow-এর পুরো (base+extra) amount
--      solver-কে payout হয়ে যায়।
-- তাই এখানে additional_charges.status -> ACCEPTED করাটা নিছক bookkeeping/label আপডেট -- আবার
-- respond_additional_charge() কল করলে সেই একই টাকা দ্বিতীয়বার wallet থেকে কাটা হতো আর escrow-এর
-- extra_amount-এ দ্বিতীয়বার যোগ হতো (রিপোর্ট হওয়া ডাবল-ডিডাকশন বাগ, ঠিক এটাই)।
--
-- এই ফাংশন **শুধু** additional_charges.status/responded_at আপডেট করে -- users.balance,
-- escrows.extra_amount, বা problems.confirmed_extra_amount_total-এর কোনোটাই স্পর্শ করে না।
-- ভবিষ্যতে কেউ যেন ভুল করে এখানে money-movement side-effect যোগ না করে, সেই সতর্কতা এই কমেন্টে
-- স্পষ্ট করে লেখা হলো।
--
-- Note: টেবিলে `settled_at` নামে কোনো কলাম নেই (শুধু `responded_at` আছে -- Supabase MCP দিয়ে
-- সরাসরি চেক করা হয়েছে; request_additional_charge/respond_additional_charge RPC দুটোই এই একই
-- কলাম ব্যবহার করে) -- তাই এখানেও responded_at reuse করা হলো, নতুন কলাম যোগ করা হয়নি
-- (Kotlin AdditionalChargeEntity.respondedAt এর সাথেও মিলে যায়)।
--
-- Authorization: problem owner (auth.uid() = charge.user_id), solver (auth.uid() = charge.solver_id),
-- অথবা admin -- confirmReleaseAndComplete()-এর তিনটা caller (ব্যবহারকারীর নিজের "Confirm & Complete"
-- ট্যাপ, admin dispute-resolution RELEASE_TO_SOLVER শাখা, এবং 48-ঘণ্টা auto-release sweep) এর মধ্যে
-- প্রথম দুইটা এই চেক pass করবে। 48hr sweep-এর caller problem owner/solver/admin নাও হতে পারে (যেই
-- ডিভাইসে app খোলা আছে সে-ই ট্রিগার করে) -- সেক্ষেত্রে NOT_AUTHORIZED রিটার্ন হবে, যা Kotlin-সাইডে
-- শুধু log হয়ে best-effort হিসেবে থেমে যাবে (system_notify_48hour_auto_release-এর মতো একটা
-- আলাদা, narrowly-scoped SECURITY DEFINER variant দরকার হলে সেটা পরবর্তী কোনো cleanup ধাপে করা
-- যেতে পারে -- এই ধাপের স্কোপের বাইরে, রিপোর্টে ফ্ল্যাগ করা হয়েছে)।
create or replace function public.mark_additional_charge_settled(p_charge_id text)
returns jsonb
language plpgsql
security definer
set search_path to 'public'
as $function$
declare
  v_charge public.additional_charges%rowtype;
  v_now timestamptz := now();
begin
  select * into v_charge from public.additional_charges where id = p_charge_id for update;
  if not found then
    raise exception 'CHARGE_NOT_FOUND';
  end if;

  if auth.uid() <> v_charge.user_id
     and auth.uid() <> v_charge.solver_id
     and not public.is_admin(auth.uid()) then
    raise exception 'NOT_AUTHORIZED';
  end if;

  if v_charge.status <> 'PENDING' then
    return jsonb_build_object('result', 'ALREADY_RESPONDED', 'status', v_charge.status);
  end if;

  -- Bookkeeping-only: NO wallet/escrow/problem money-movement here on purpose. See the function
  -- comment above -- that money already moved through confirmReleaseAndComplete()'s own
  -- walletDeduction param + payoutEscrowToSolver()/release_escrow RPC call.
  update public.additional_charges
    set status = 'ACCEPTED', responded_at = v_now
  where id = p_charge_id;

  return jsonb_build_object('result', 'OK', 'status', 'ACCEPTED');
end;
$function$;

revoke all on function public.mark_additional_charge_settled(text) from public;
grant execute on function public.mark_additional_charge_settled(text) to authenticated;
