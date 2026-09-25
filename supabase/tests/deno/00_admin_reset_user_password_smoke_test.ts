// supabase/tests/deno/00_admin_reset_user_password_smoke_test.ts
//
// Step 17.1 — শুধু infra + একটা ট্রিভিয়াল smoke-test। কোনো ফাংশনাল (৪০১/৪০৩/২০০/৪০০)
// assertion এখানে ইচ্ছাকৃতভাবে নেই — ওগুলো Step 17.2 (authorization) ও Step 17.3
// (success/validation)-এ আলাদা ফাইলে লেখা হবে। এই ফাইলের একমাত্র উদ্দেশ্য: প্রমাণ করা যে
// `captureHandler()` দিয়ে আসল index.ts সত্যিই import + invoke করা যায় — অর্থাৎ Deno test
// pipeline structurally আসলেই কাজ করে, module-load ব্যর্থ হয়ে পুরো ফাইলটাই crash করে না।
//
// 🔴 এই sandbox-এ চালিয়ে verify করা যায়নি — `_edge_function_test_helpers.ts`-এর উপরের
// কমেন্টে বিস্তারিত: jsr.io এই sandbox-এর network allowlist-এ নেই (403 host_not_allowed,
// curl দিয়ে কনফার্মড), আর index.ts মডিউল-লেভেলে `jsr:@supabase/supabase-js@2` import করে।
// capture-mechanism-টা নিজে (Deno.serve override) একটা jsr-import-ছাড়া POC ফাইল দিয়ে এই
// sandbox-এই সত্যিই চালিয়ে verify করা হয়েছে (progress doc-এর "Step 17.1" সেকশন দ্রষ্টব্য) —
// শুধু real index.ts-এর jsr: import resolve করাটাই এখানে সম্ভব হয়নি।
//
// verify command (Windows/CI, স্বাভাবিক ইন্টারনেট থাকা অবস্থায়):
//   deno test --allow-net --allow-env --allow-read supabase/tests/deno/
// প্রত্যাশা: "ok | 1 passed | 0 failed"।

import { captureHandler, setDummySupabaseEnv } from "./_edge_function_test_helpers.ts";

const MODULE_PATH = "../../functions/admin-reset-user-password/index.ts";

Deno.test("admin-reset-user-password: handler capture + malformed-request smoke test", async () => {
  setDummySupabaseEnv();

  const handler = await captureHandler(MODULE_PATH);
  if (typeof handler !== "function") {
    throw new Error(`captureHandler ফাংশন রিটার্ন করেনি, পেয়েছে: ${typeof handler}`);
  }

  // ইচ্ছাকৃতভাবে অবৈধ JSON body — index.ts-এর `await req.json()` এখানেই throw করার কথা,
  // যেটা outer try/catch ধরে ৫০০ রিটার্ন করে। এই পথে auth-header check বা কোনো network
  // কল-ই এখনো হয় না (সেটা req.json()-এর পরের লাইনে), তাই fetch mock ছাড়াই নিরাপদে চালানো
  // যায় — শুধু pipeline plumbing-টা প্রমাণ করাই এই স্মোক-টেস্টের কাজ, business logic না।
  const badRequest = new Request("http://localhost/admin-reset-user-password", {
    method: "POST",
    body: "not valid json {{{",
  });

  const response = await handler(badRequest);

  if (!(response instanceof Response)) {
    throw new Error(`handler একটা Response রিটার্ন করেনি, পেয়েছে: ${typeof response}`);
  }
  if (response.status !== 500) {
    throw new Error(`প্রত্যাশা ছিল status 500 (malformed JSON → outer catch), পাওয়া গেছে: ${response.status}`);
  }

  const body = await response.json();
  if (!body || typeof body.error !== "string") {
    throw new Error(`প্রত্যাশা ছিল JSON body-তে string "error" ফিল্ড, পাওয়া গেছে: ${JSON.stringify(body)}`);
  }

  console.log(`[smoke test OK] captured handler invoked, got expected 500 + error="${body.error}"`);
});
