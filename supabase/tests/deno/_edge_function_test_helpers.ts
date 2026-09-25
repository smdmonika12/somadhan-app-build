// supabase/tests/deno/_edge_function_test_helpers.ts
//
// Step 17.1 — Supabase Edge Function (Deno runtime) কভারেজের জন্য shared test helper।
// এই ফাইলটাই CI_TEST_SUITE_MASTER_PROMPT.md-এর Step 17.1-এর "scaffold/mock-infrastructure"
// অংশ — 17.2/17.3-এর আসল ফাংশনাল টেস্টগুলো (401/403/200/400) এই দুটো export ব্যবহার করবে।
//
// ============================================================================
// আর্কিটেকচারাল সিদ্ধান্ত (এই ধাপেই নেওয়া হলো, rule ৬-এর "প্রতিটা নতুন ধাপে যা লাগবে ঠিক
// করবে" অনুযায়ী) — কেন standalone Deno test, Postgres service লাগবে না:
// ============================================================================
// supabase/functions/admin-reset-user-password/index.ts একটা single, self-contained
// Deno.serve() handler — কোনো local Postgres দরকার নেই কারণ এটা কখনো সরাসরি DB-তে কানেক্ট
// করে না, সবসময় supabase-js ক্লায়েন্ট দিয়ে Supabase-এর HTTP API (auth/rest endpoint)
// কল করে। তাই "local emulator + Step 0-এর ephemeral Postgres" রুটের বদলে এই রুট বেছে
// নেওয়া হলো: globalThis.fetch override করে ওই HTTP কলগুলো মক করা (rule ২ পুরোপুরি মানা হয় —
// কোনো real/staging Supabase project ছোঁয়া হয় না, positive দিক থেকে local Postgres
// bootstrap/migration-apply করারও দরকার নেই এই layer-এর জন্য)।
//
// ============================================================================
// 🔴 sandbox-এ verify করা যায়নি — network ব্লকার (এই ধাপেই আবিষ্কৃত ও কনফার্মড)
// ============================================================================
// index.ts মডিউল-লেভেলে `jsr:@supabase/functions-js/edge-runtime.d.ts` ও
// `jsr:@supabase/supabase-js@2` import করে। এই sandbox-এর network egress allowlist-এ
// `jsr.io` নেই — সরাসরি curl দিয়ে কনফার্মড:
//   curl -sI https://jsr.io/@supabase/supabase-js/meta.json  →  HTTP/2 403,
//   x-deny-reason: host_not_allowed
// ফলে `deno check`/`deno test` দিয়ে আসল index.ts import করার চেষ্টা এই sandbox-এ সবসময়
// এই একই 403-এ ব্যর্থ হবে (deno check দিয়ে সরাসরি reproduce করা হয়েছে progress doc-এ)।
// এটা DualWriteGapTest.kt/OfflineGatingSyncTest.kt-এর "sandbox-এ real Gradle/Robolectric
// রান সম্ভব না" সীমাবদ্ধতার একই ধরনের একটা সমতুল্য — কোড/টেস্টের বাগ না, sandbox-এর
// network-allowlist সীমাবদ্ধতা। GitHub Actions ubuntu-latest runner-এ (unrestricted
// egress) এবং ব্যবহারকারীর Windows মেশিনে (স্বাভাবিক ইন্টারনেট) এটা কাজ করার কথা।
//
// ✅ যেটা এই sandbox-এই সত্যিই চালিয়ে verify করা হয়েছে (POC, শুধু এই ধাপের জন্য, repo-তে
// রাখা হয়নি): নিচের `captureHandler()`-এর মূল কৌশলটা (Deno.serve override করে handler
// capture করা, কোনো real listener bind না করে) একটা jsr:-import-ছাড়া স্ট্যান্ডইন ফাইল
// দিয়ে যাচাই করা হয়েছে — mechanism নিজে কাজ করে, শুধু real index.ts-এর jsr: import-টাই
// এই sandbox-এ ব্লকড।
//
// verify command (Windows/CI, রিয়েল ইন্টারনেট থাকা অবস্থায়):
//   deno test --allow-net --allow-env --allow-read supabase/tests/deno/
// ============================================================================

export type EdgeHandler = (req: Request) => Response | Promise<Response>;

/**
 * supabase-js ক্লায়েন্ট createClient()/auth কলের জন্য প্রয়োজনীয় env var বসিয়ে দেয় —
 * index.ts-এর `Deno.env.get("...")!`-এ non-null assertion আছে, তাই এগুলো না থাকলে
 * handler-এর ভেতরে undefined non-null-assert crash হবে। এগুলো সম্পূর্ণ dummy মান —
 * কোনো real credential/URL না (rule 2a — `.env`-এর real value কখনো committed ফাইলে
 * যাবে না, আর এমনিতেও rule ২ অনুযায়ী real project ছোঁয়ার কথাই না)।
 */
export function setDummySupabaseEnv(): void {
  Deno.env.set("SUPABASE_URL", "http://127.0.0.1:54321");
  Deno.env.set("SUPABASE_ANON_KEY", "dummy-anon-key-for-tests-only");
  Deno.env.set("SUPABASE_SERVICE_ROLE_KEY", "dummy-service-role-key-for-tests-only");
}

/**
 * `modulePath`-এ থাকা Edge Function ফাইলটা import করে, module-load-টাইমে করা তার
 * `Deno.serve(handler)` কল থেকে ভেতরের request-handler ফাংশনটা বের করে আনে — index.ts
 * এডিট না করেই (rule ১), এবং কোনো real network listener bind না করেই (Deno.serve
 * সাময়িকভাবে override করা হয়, import শেষে আসলটা ফিরিয়ে দেওয়া হয়)।
 *
 * cache-bust query-string ব্যবহার করা হয়েছে যাতে একই `deno test` প্রসেসে একাধিক টেস্ট
 * ফাইল থেকে একই module বারবার capture করতে চাইলে Deno-র module-cache-এর কারণে
 * দ্বিতীয়বার আর top-level Deno.serve(...) কলটা re-run না হয়ে যাওয়ার সমস্যা এড়ানো যায়।
 */
export async function captureHandler(modulePath: string): Promise<EdgeHandler> {
  const originalServe = Deno.serve;
  let captured: EdgeHandler | undefined;

  // @ts-ignore — শুধু handler-ক্যাপচারের জন্য সাময়িক override; আসল Deno.serve-এর পূর্ণ
  // overload সিগনেচারের (options variant ইত্যাদি) সাথে হুবহু মেলে না, ইচ্ছাকৃতভাবে।
  Deno.serve = ((handler: EdgeHandler) => {
    captured = handler;
    return { finished: Promise.resolve(undefined) } as unknown as ReturnType<typeof Deno.serve>;
  }) as typeof Deno.serve;

  try {
    await import(`${modulePath}?t=${Date.now()}-${Math.random()}`);
  } finally {
    Deno.serve = originalServe;
  }

  if (!captured) {
    throw new Error(
      `captureHandler: ${modulePath} import করার পর কোনো Deno.serve(...) কল ধরা পড়েনি — ` +
        `ফাইলটা কি সত্যিই module-load-এ সরাসরি Deno.serve কল করে?`,
    );
  }
  return captured;
}

/** installMockSupabaseFetch()-এর কনফিগারেশন — 17.2/17.3-এর প্রতিটা টেস্ট-কেস নিজের মতো
 *  ভ্যালু বসাবে (৪০১/৪০৩/২০০/৪০০ প্রতিটার জন্য আলাদা কম্বিনেশন)।
 *
 * [সেশন ৭.১.১, ২০২৬-০৯-২৫] `is_admin` RPC মক আগে ছিল এখানে — index.ts এখন এক-কলে সবকিছু
 * সিদ্ধান্ত নেওয়া `admin_reset_password_precheck(p_target_user_id)` RPC ডাকে (migration
 * zz_20260925130000), তাই মক-ও সেই RPC-র শেপে বদলানো হলো: `{allowed, reason}` jsonb। পুরনো
 * `isAdmin`/`isAdminRpcFails` ফিল্ড দুটো `precheckAllowed`/`precheckReason`/`precheckRpcFails`
 * দিয়ে প্রতিস্থাপিত।
 */
export interface MockFetchOptions {
  /** `/rest/v1/rpc/admin_reset_password_precheck` কলের `allowed` মান। */
  precheckAllowed: boolean;
  /** `precheckAllowed: false` হলে সাথের `reason` (যেমন "ADMIN_ONLY"/"TARGET_IS_ADMIN"/
   *  "PERMISSION_DENIED")। `precheckAllowed: true` হলে ব্যবহৃত হয় না। */
  precheckReason?: string;
  /** `/auth/v1/user`-এর জন্য মক করা authenticated user — null মানে "টোকেন invalid/expired,
   *  ৪০১ প্রত্যাশিত"। */
  authUser: { id: string } | null;
  /** `/auth/v1/admin/users/:id` (PUT, password update)-এর ফলাফল — false দিলে mock একটা
   *  error response রিটার্ন করবে (৪০০ কেস)। */
  updateUserSucceeds: boolean;
  /**
   * true হলে `/rest/v1/rpc/admin_reset_password_precheck` কলটাই একটা PostgREST-style error
   * (৪০৪, `PGRST202`) রিটার্ন করবে — উপরের `precheckAllowed`/`precheckReason` তখন আর ব্যবহার
   * হয় না। index.ts-এর `precheckErr || !precheck || precheck.allowed !== true` চেক অনুযায়ী
   * এই কেসেও ৪০৩ ADMIN_ONLY (fail-closed) হওয়ার কথা, exception silently pass/fail-open হওয়ার
   * কথা না। ঐচ্ছিক (default: false/unset)।
   */
  precheckRpcFails?: boolean;
}

/**
 * globalThis.fetch অস্থায়ীভাবে override করে supabase-js@2-এর তিনটা network কল মক করে —
 * `/auth/v1/user` (auth.getUser()), `/rest/v1/rpc/is_admin` (rpc()), এবং
 * `/auth/v1/admin/users/*` (auth.admin.updateUserById())। কোনো real Supabase
 * project/network ছোঁয়া হয় না (rule ২)।
 *
 * ⚠️ এই তিনটা URL-প্যাটার্ন supabase-js@2-এর auth-js/postgrest-js প্যাকেজের সাধারণভাবে
 * পরিচিত পাবলিক route-শেপ থেকে লেখা — কিন্তু jsr.io ব্লকড থাকায় (উপরের নোট) এই সেশনে
 * সত্যিকারের network কল করে exact path/method যাচাই করা সম্ভব হয়নি। তাই এটাও
 * TEMPORARY/INFERRED — 17.2-এ real (CI/Windows) রান-এ যদি এই প্যাটার্ন না মেলে
 * (যেমন mock-এ কোনো URL অধরা থেকে যায়, নিচের catch-all error দিয়ে সেটা ধরা পড়বে),
 * সেটা ঠিক করে এই কমেন্টও আপডেট করতে হবে।
 */
export function installMockSupabaseFetch(opts: MockFetchOptions): () => void {
  const original = globalThis.fetch;

  globalThis.fetch = (async (input: RequestInfo | URL, _init?: RequestInit) => {
    const url = typeof input === "string" ? input : input instanceof URL ? input.href : input.url;

    if (url.includes("/auth/v1/user")) {
      if (!opts.authUser) {
        return new Response(JSON.stringify({ error: "invalid_token", message: "Invalid JWT" }), {
          status: 401,
        });
      }
      return new Response(
        JSON.stringify({ id: opts.authUser.id, aud: "authenticated", role: "authenticated" }),
        { status: 200 },
      );
    }

    if (url.includes("/rest/v1/rpc/admin_reset_password_precheck")) {
      if (opts.precheckRpcFails) {
        // PostgREST-এর "function not found for these argument names" শেপ — schema-cache
        // stale/মিসম্যাচ হলে লাইভে ঠিক এরকম response আসতে পারে (আগের is_admin-এর জন্য
        // Step 17.1-এ যেমন ফ্ল্যাগ করা হয়েছিল, একই ক্লাসের ব্যর্থতা)।
        return new Response(
          JSON.stringify({
            code: "PGRST202",
            message:
              "Could not find the function public.admin_reset_password_precheck(p_target_user_id) in the schema cache",
          }),
          { status: 404 },
        );
      }
      return new Response(
        JSON.stringify({ allowed: opts.precheckAllowed, reason: opts.precheckReason ?? null }),
        { status: 200 },
      );
    }

    if (url.includes("/auth/v1/admin/users/")) {
      if (!opts.updateUserSucceeds) {
        return new Response(JSON.stringify({ msg: "mock update failure" }), { status: 400 });
      }
      return new Response(JSON.stringify({ id: opts.authUser?.id ?? "mock-target-user-id" }), {
        status: 200,
      });
    }

    throw new Error(
      `installMockSupabaseFetch: unhandled mocked URL (17.2/17.3-এ নতুন কোনো supabase-js ` +
        `network কল পাওয়া গেলে এখানে নতুন case যোগ করতে হবে): ${url}`,
    );
  }) as typeof fetch;

  return () => {
    globalThis.fetch = original;
  };
}
