import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { createClient } from "jsr:@supabase/supabase-js@2";

// এই Edge Function admin panel থেকে কোনো user/solver-এর auth password reset করার জন্য।
// এটা দরকার কারণ public.users টেবিলে কোনো password column নেই -- আসল password
// Supabase Auth (auth.users) এ থাকে, আর auth.admin.updateUserById() শুধু service_role
// key দিয়েই কল করা যায় (client-side anon/authenticated key দিয়ে না) -- তাই এটা একটা
// কাজের জন্যই client সরাসরি RPC/Postgrest দিয়ে করতে পারে না, Edge Function লাগে।
//
// নিরাপত্তা: কলারের নিজের JWT (Authorization header) দিয়ে তার পরিচয় যাচাই করে, তারপর
// `admin_reset_password_precheck(target_user_id)` RPC দিয়ে সিদ্ধান্ত নেয় -- client-পাঠানো
// কোনো flag বিশ্বাস করা হয় না। target_user_id ক্লায়েন্ট থেকে আসে কিন্তু শুধুমাত্র precheck
// allowed=true দিলেই ব্যবহার হয়।
//
// [ধাপ ৩০ নোট] এই ফাইলটা একটা আগের/আনলগড সেশনে লেখা ও deploy করা হয়েছিল (লাইভ প্রজেক্টে
// পাওয়া গেছে, version 2) -- সেই কপিটা শুধু is_admin(caller) চেক করত।
//
// [সেশন ৭.১.১ ফিক্স, ২০২৬-০৯-২৫] নিরাপত্তা-ফাঁক: আগের কোড caller admin কিনা শুধু সেটাই
// চেক করত -- caller *যেকোনো* সক্রিয় admin হলেই *যেকোনো* target-এর (এমনকি অন্য এডমিন/সুপারের)
// পাসওয়ার্ড রিসেট করতে পারত, caller-এর রোলে reset_password পারমিশন আছে কিনা বা caller
// flagged কিনা কিছুই চেক হতো না। এখন `admin_reset_password_precheck` RPC (migration
// zz_20260925130000) একবারেই caller-admin + target-not-admin-unless-super + caller-এর
// `users:users:reset_password` পারমিশন/flagged-অবস্থা সবকিছু সার্ভার-সাইডে verify করে --
// শুধু ক্লায়েন্ট-গেটের ওপর ভরসা না করে, যেহেতু service-role দিয়ে সরাসরি এই Edge Function
// কল করাও টেকনিক্যালি সম্ভব।

Deno.serve(async (req: Request) => {
  try {
    const { target_user_id, new_password } = await req.json();
    if (!target_user_id || typeof target_user_id !== "string") {
      return new Response(JSON.stringify({ error: "INVALID_TARGET_USER_ID" }), { status: 400 });
    }
    if (!new_password || typeof new_password !== "string" || new_password.length < 6) {
      return new Response(JSON.stringify({ error: "INVALID_PASSWORD" }), { status: 400 });
    }

    const authHeader = req.headers.get("Authorization");
    if (!authHeader) {
      return new Response(JSON.stringify({ error: "NO_AUTH_HEADER" }), { status: 401 });
    }

    const supabaseUrl = Deno.env.get("SUPABASE_URL")!;
    const anonKey = Deno.env.get("SUPABASE_ANON_KEY")!;
    const serviceRoleKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;

    // কলারের নিজের JWT দিয়ে তার পরিচয় যাচাই
    const callerClient = createClient(supabaseUrl, anonKey, {
      global: { headers: { Authorization: authHeader } },
    });
    const { data: userData, error: userErr } = await callerClient.auth.getUser();
    if (userErr || !userData?.user) {
      return new Response(JSON.stringify({ error: "NOT_AUTHENTICATED" }), { status: 401 });
    }

    // admin_reset_password_precheck(target) RPC দিয়ে সার্ভার-সাইডে পুরো সিদ্ধান্ত: caller admin
    // কিনা + target নিজে admin হলে caller সুপার কিনা + সাধারণ ইউজার-টার্গেটে caller-এর
    // reset_password পারমিশন/flagged-অবস্থা -- সবকিছু একবারেই (client flag বিশ্বাস করা হয় না)।
    // RPC কলই ব্যর্থ হলে (network/schema সমস্যা) fail-closed: ADMIN_ONLY, fail-open না।
    const { data: precheck, error: precheckErr } = await callerClient.rpc(
      "admin_reset_password_precheck",
      { p_target_user_id: target_user_id },
    );
    if (precheckErr || !precheck || precheck.allowed !== true) {
      const reason = (precheck && precheck.reason) || "ADMIN_ONLY";
      const status = reason === "NOT_AUTHENTICATED" ? 401 : 403;
      return new Response(JSON.stringify({ error: reason }), { status });
    }

    // service_role দিয়ে target user-এর auth password reset
    const adminClient = createClient(supabaseUrl, serviceRoleKey);
    const { error: updateErr } = await adminClient.auth.admin.updateUserById(target_user_id, {
      password: new_password,
    });
    if (updateErr) {
      return new Response(JSON.stringify({ error: updateErr.message }), { status: 400 });
    }

    return new Response(JSON.stringify({ result: "OK" }), {
      headers: { "Content-Type": "application/json" },
    });
  } catch (e) {
    return new Response(JSON.stringify({ error: String(e) }), { status: 500 });
  }
});
