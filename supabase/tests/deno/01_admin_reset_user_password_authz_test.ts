// supabase/tests/deno/01_admin_reset_user_password_authz_test.ts
//
// Step 17.2 — Authorization টেস্ট (৪০১/৪০৩), master prompt অনুযায়ী সবচেয়ে বেশি অগ্রাধিকার।
// admin-reset-user-password Edge Function-এর security-critical অংশ verify করে:
// (ক) কোনো Authorization header ছাড়া কল → ৪০১ (NO_AUTH_HEADER)
// (খ) authenticated কিন্তু non-admin user কল করলে → ৪০৩ (ADMIN_ONLY)
//
// প্লাস তিনটা অতিরিক্ত risk-focused কেস — কোডে সত্যিই যা আছে সেটাই assert করা হচ্ছে, নতুন কোনো
// অনুমান না (rule ৫):
// (গ) Authorization header আছে কিন্তু token invalid/expired (auth.getUser() ব্যর্থ) → ৪০১
//     (NOT_AUTHENTICATED) — এই ব্র্যাঞ্চটাও (ক)-এর মতোই "না-authenticated" পরিবারের, কিন্তু
//     ভিন্ন কোড-পাথ (আলাদাভাবে টেস্ট না করলে missed থেকে যেতে পারত)।
// (ঘ) precheck RPC কলই ব্যর্থ হলে (schema-cache মিসম্যাচ/network সমস্যা হলে বাস্তবে যা ঘটতে
//     পারে তার একটা realistic সিমুলেশন, PGRST202) — index.ts-এর
//     `precheckErr || !precheck || precheck.allowed !== true` চেক অনুযায়ী এটাও ৪০৩
//     (fail-closed, ADMIN_ONLY) হওয়ার কথা, silently pass/fail-open হওয়ার কথা না।
//
// [সেশন ৭.১.১, ২০২৬-০৯-২৫] নিরাপত্তা-ফিক্সের সাথে যোগ হওয়া নতুন দুটো কেস (আগে test-ই
// লেখা হয়নি কারণ ফিচারটাই ছিল না):
// (ঙ) caller admin কিন্তু target নিজেও admin (caller সুপার না) → ৪০৩ TARGET_IS_ADMIN
// (চ) caller admin, target সাধারণ ইউজার, কিন্তু precheck বলছে PERMISSION_DENIED
//     (রোলে reset_password পারমিশন নেই বা flagged) → ৪০৩ PERMISSION_DENIED
//
// 🔴 sandbox network ব্লকার (Step 17.1-এ কনফার্মড, অপরিবর্তিত) — jsr.io host_not_allowed,
// তাই এই ফাইলও এই sandbox-এ real-run করা যায়নি। capture/mock mechanism-এর নিজের লজিক
// আগের ধাপেই (jsr-import-ছাড়া POC দিয়ে) sandbox-এ verify হয়ে গেছে — এখানে শুধু নতুন
// assertion-গুলো যোগ হয়েছে, mechanism অপরিবর্তিত।
//
// verify command (Windows/CI, স্বাভাবিক ইন্টারনেট থাকা অবস্থায়):
//   deno test --allow-net --allow-env --allow-read supabase/tests/deno/
// প্রত্যাশা: "ok | 7 passed | 0 failed" (এই ফাইলের ৬টা + 00_...smoke_test.ts-এর ১টা)।

import {
  captureHandler,
  installMockSupabaseFetch,
  setDummySupabaseEnv,
} from "./_edge_function_test_helpers.ts";

const MODULE_PATH = "../../functions/admin-reset-user-password/index.ts";

// index.ts-এর validation (target_user_id + new_password >= 6 char) পাস করার মতো একটা
// ন্যূনতম বৈধ body — যাতে টেস্টগুলো ঠিক authorization ব্র্যাঞ্চেই পৌঁছায়, ৪০০ ভ্যালিডেশন
// এররে আটকে না যায় (ওটা Step 17.3-এর স্কোপ)।
const VALID_BODY = JSON.stringify({
  target_user_id: "11111111-1111-1111-1111-111111111111",
  new_password: "a-valid-password-123",
});

function makeRequest(withAuthHeader: boolean): Request {
  const headers: HeadersInit = withAuthHeader
    ? { Authorization: "Bearer fake-jwt-for-test" }
    : {};
  return new Request("http://localhost/admin-reset-user-password", {
    method: "POST",
    headers,
    body: VALID_BODY,
  });
}

Deno.test(
  "admin-reset-user-password: (ক) Authorization header ছাড়া কল → 401 NO_AUTH_HEADER",
  async () => {
    setDummySupabaseEnv();
    const handler = await captureHandler(MODULE_PATH);

    // এই কেসে fetch mock ইনস্টল করার দরকারই নেই — authHeader চেক (req.json()-এর ঠিক পরেই,
    // কোনো network কলের আগেই) ফেইল করার কথা।
    const response = await handler(makeRequest(false));

    if (response.status !== 401) {
      throw new Error(`প্রত্যাশা ছিল status 401, পাওয়া গেছে: ${response.status}`);
    }
    const body = await response.json();
    if (body.error !== "NO_AUTH_HEADER") {
      throw new Error(
        `প্রত্যাশা ছিল error="NO_AUTH_HEADER", পাওয়া গেছে: ${JSON.stringify(body)}`,
      );
    }
  },
);

Deno.test(
  "admin-reset-user-password: (গ) invalid/expired token → 401 NOT_AUTHENTICATED",
  async () => {
    setDummySupabaseEnv();
    const restoreFetch = installMockSupabaseFetch({
      authUser: null, // auth.getUser() ব্যর্থ হবে — token invalid/expired সিমুলেট করে
      precheckAllowed: false, // এই পাথে কখনো পৌঁছানোর কথা না, শুধু interface পূরণের জন্য
      updateUserSucceeds: true,
    });
    try {
      const handler = await captureHandler(MODULE_PATH);
      const response = await handler(makeRequest(true));

      if (response.status !== 401) {
        throw new Error(`প্রত্যাশা ছিল status 401, পাওয়া গেছে: ${response.status}`);
      }
      const body = await response.json();
      if (body.error !== "NOT_AUTHENTICATED") {
        throw new Error(
          `প্রত্যাশা ছিল error="NOT_AUTHENTICATED", পাওয়া গেছে: ${JSON.stringify(body)}`,
        );
      }
    } finally {
      restoreFetch();
    }
  },
);

Deno.test(
  "admin-reset-user-password: (খ) authenticated non-admin user → 403 ADMIN_ONLY",
  async () => {
    setDummySupabaseEnv();
    const restoreFetch = installMockSupabaseFetch({
      authUser: { id: "22222222-2222-2222-2222-222222222222" },
      // real non-admin ব্যবহারকারীর জন্য precheck RPC allowed:false, reason ADMIN_ONLY দেওয়ার কথা
      precheckAllowed: false,
      precheckReason: "ADMIN_ONLY",
      updateUserSucceeds: true,
    });
    try {
      const handler = await captureHandler(MODULE_PATH);
      const response = await handler(makeRequest(true));

      if (response.status !== 403) {
        throw new Error(
          `প্রত্যাশা ছিল status 403 — বাগ থাকলে (যেমন is_admin() ফলাফল ঠিকমতো চেক না হওয়া) ` +
            `এখানেই ধরা পড়ার কথা। পাওয়া গেছে: ${response.status}`,
        );
      }
      const body = await response.json();
      if (body.error !== "ADMIN_ONLY") {
        throw new Error(
          `প্রত্যাশা ছিল error="ADMIN_ONLY", পাওয়া গেছে: ${JSON.stringify(body)}`,
        );
      }
    } finally {
      restoreFetch();
    }
  },
);

Deno.test(
  "admin-reset-user-password: (ঘ) precheck RPC ব্যর্থ হলে (simulated PGRST202) → fail-closed 403, fail-open না",
  async () => {
    setDummySupabaseEnv();
    const restoreFetch = installMockSupabaseFetch({
      authUser: { id: "33333333-3333-3333-3333-333333333333" },
      // ইচ্ছাকৃতভাবে true — যাতে প্রমাণ হয় ফলাফল RPC নিজেই ব্যর্থ হওয়ার কারণে override
      // হচ্ছে (precheckRpcFails: true থাকলে এই মান আসলে ব্যবহারই হয় না — helper-এর কমেন্ট
      // ও কোড দ্রষ্টব্য), precheckAllowed মান নিজে থেকে না।
      precheckAllowed: true,
      updateUserSucceeds: true,
      precheckRpcFails: true,
    });
    try {
      const handler = await captureHandler(MODULE_PATH);
      const response = await handler(makeRequest(true));

      if (response.status !== 403) {
        throw new Error(
          `প্রত্যাশা ছিল status 403 (fail-closed) — precheck RPC নিজেই ব্যর্থ হলে index.ts-এর ` +
            `"precheckErr || !precheck || precheck.allowed !== true" চেক অনুযায়ী ৪০৩ হওয়ার কথা, ` +
            `exception silently pass হয়ে fail-open হওয়ার কথা না। পাওয়া গেছে: ${response.status}`,
        );
      }
      const body = await response.json();
      if (body.error !== "ADMIN_ONLY") {
        throw new Error(
          `প্রত্যাশা ছিল error="ADMIN_ONLY", পাওয়া গেছে: ${JSON.stringify(body)}`,
        );
      }
    } finally {
      restoreFetch();
    }
  },
);

Deno.test(
  "admin-reset-user-password: (ঙ) caller admin কিন্তু target নিজেও admin (caller সুপার না) → 403 TARGET_IS_ADMIN",
  async () => {
    setDummySupabaseEnv();
    const restoreFetch = installMockSupabaseFetch({
      authUser: { id: "66666666-6666-6666-6666-666666666666" },
      precheckAllowed: false,
      precheckReason: "TARGET_IS_ADMIN",
      updateUserSucceeds: true,
    });
    try {
      const handler = await captureHandler(MODULE_PATH);
      const response = await handler(makeRequest(true));

      if (response.status !== 403) {
        throw new Error(
          `প্রত্যাশা ছিল status 403 — সাধারণ (নন-সুপার) এডমিন অন্য এডমিনের পাসওয়ার্ড রিসেট ` +
            `করতে পারার কথা না। পাওয়া গেছে: ${response.status}`,
        );
      }
      const body = await response.json();
      if (body.error !== "TARGET_IS_ADMIN") {
        throw new Error(
          `প্রত্যাশা ছিল error="TARGET_IS_ADMIN", পাওয়া গেছে: ${JSON.stringify(body)}`,
        );
      }
    } finally {
      restoreFetch();
    }
  },
);

Deno.test(
  "admin-reset-user-password: (চ) caller admin, target সাধারণ ইউজার, কিন্তু পারমিশন/flagged-এ আটকায় → 403 PERMISSION_DENIED",
  async () => {
    setDummySupabaseEnv();
    const restoreFetch = installMockSupabaseFetch({
      authUser: { id: "77777777-7777-7777-7777-777777777777" },
      precheckAllowed: false,
      precheckReason: "PERMISSION_DENIED",
      updateUserSucceeds: true,
    });
    try {
      const handler = await captureHandler(MODULE_PATH);
      const response = await handler(makeRequest(true));

      if (response.status !== 403) {
        throw new Error(
          `প্রত্যাশা ছিল status 403 — caller-এর রোলে reset_password পারমিশন না থাকলে বা ` +
            `caller flagged হলে ব্লক হওয়ার কথা। পাওয়া গেছে: ${response.status}`,
        );
      }
      const body = await response.json();
      if (body.error !== "PERMISSION_DENIED") {
        throw new Error(
          `প্রত্যাশা ছিল error="PERMISSION_DENIED", পাওয়া গেছে: ${JSON.stringify(body)}`,
        );
      }
    } finally {
      restoreFetch();
    }
  },
);
