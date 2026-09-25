// supabase/tests/deno/02_admin_reset_user_password_success_validation_test.ts
//
// Step 17.3 — Success/validation টেস্ট (Step 17-এর শেষ উপ-ধাপ)। master prompt-এর চাহিদা
// অনুযায়ী:
// (গ) admin হিসেবে সঠিক call করলে password আসলেই reset হয় → 200 {result: "OK"}
// (ঘ) খুব ছোট/invalid password দিলে → 400 INVALID_PASSWORD
// প্লাস (HANDOFF-এ নোট করা অতিরিক্ত কেসগুলো, একই validation-block/success-path-এর
// failure-branch, "happy path + edge case" কনভেনশন অনুযায়ী একই সেশনের স্কোপে):
// - target_user_id missing/non-string → 400 INVALID_TARGET_USER_ID
// - admin, auth সব ঠিক কিন্তু target user-এর auth password আসলে update করতে গিয়ে ব্যর্থ
//   (adminClient.auth.admin.updateUserById নিজে error দেয়) → 400, error = আসল updateErr.message
//
// ⚠️ index.ts-এর validation-order অনুযায়ী target_user_id ও new_password উভয়ই যাচাই হয়
// **auth header check-এরও আগে** (সবার প্রথম দুটো if-block) — তাই এই দুটো validation-কেসে
// কোনো Authorization header বা fetch-mock লাগে না, network-এ পৌঁছানোরই কথা না।
//
// 🔵 এই সেশনে নতুন যাচাই (Step 17.1/17.2-এ flag করা uncertainty আংশিক কমেছে): `npm view` দিয়ে
// আসল `@supabase/auth-js@2.117.0` সোর্স (registry.npmjs.org থেকে, jsr.io না — এই sandbox-এ
// npm allow করা আছে) টেনে পড়া হয়েছে। দুটো জিনিস কনফার্মড হলো:
//   1. `GoTrueAdminApi.updateUserById(uid, attrs)` → `PUT ${url}/admin/users/${uid}` কল করে,
//      response.ok হলে error null, xform সহ success data রিটার্ন করে — helper-এর
//      `updateUserSucceeds: true` মক-শেপ (`{id: ...}`, স্ট্যাটাস ২০০) এর সাথে সামঞ্জস্যপূর্ণ।
//   2. response ok না হলে (`handleError`), error-message resolve হয় `_getErrorMessage()` দিয়ে —
//      যেটা priority অনুযায়ী প্রথমে `.msg`, তারপর `.message`, `.error_description`, `.error`
//      ফিল্ড চেক করে। helper-এর existing mock (`{ msg: "mock update failure" }`, ১৭.১-এ লেখা)
//      তাই সঠিক শেপ — `updateErr.message` আসলেই "mock update failure" হবে, যেটা নিচের
//      (ঘ)-টেস্টে সরাসরি assert করা হচ্ছে (আগে এই ধারণা untested/inferred ছিল)।
//   (এছাড়া `updateUserById` নিজে `uid` UUID-format কিনা validate করে — এই ফাইলের সব টেস্টেই
//   `VALID_BODY`-র মতো একটা সঠিক-ফরম্যাট UUID ব্যবহার করা হয়েছে, তাই এটা কোনো টেস্টেই ব্লকার না।)
//
// 🔴 sandbox network ব্লকার (Step 17.1-এ কনফার্মড, অপরিবর্তিত) — jsr.io host_not_allowed,
// তাই এই ফাইলও এই sandbox-এ real index.ts-এর বিপরীতে চালানো যায়নি। কিন্তু এই সেশনেও (17.1/17.2-এর
// মতোই) একটা disposable, jsr-import-ছাড়া POC দিয়ে এই ফাইলের পুরো লজিক (৫টা assertion-ই) সত্যিই
// চালিয়ে verify করা হয়েছে — নিচে progress doc-এ বিস্তারিত।
//
// verify command (Windows/CI, স্বাভাবিক ইন্টারনেট থাকা অবস্থায়):
//   deno test --allow-net --allow-env --allow-read supabase/tests/deno/
// প্রত্যাশা: "ok | 12 passed | 0 failed" (00_...smoke ১টা + 01_...authz ৬টা [সেশন ৭.১.১-এ
// TARGET_IS_ADMIN/PERMISSION_DENIED নতুন দুটো কেস যোগ হয়েছে] + এই ফাইলের ৫টা)।

import {
  captureHandler,
  installMockSupabaseFetch,
  setDummySupabaseEnv,
} from "./_edge_function_test_helpers.ts";

const MODULE_PATH = "../../functions/admin-reset-user-password/index.ts";

const VALID_UUID = "11111111-1111-1111-1111-111111111111";
const VALID_PASSWORD = "a-valid-password-123";

function makeRequest(body: unknown, withAuthHeader = true): Request {
  const headers: HeadersInit = withAuthHeader
    ? { Authorization: "Bearer fake-jwt-for-test" }
    : {};
  return new Request("http://localhost/admin-reset-user-password", {
    method: "POST",
    headers,
    body: JSON.stringify(body),
  });
}

Deno.test(
  "admin-reset-user-password: (গ) admin সঠিক call → 200, password reset হয়",
  async () => {
    setDummySupabaseEnv();
    const restoreFetch = installMockSupabaseFetch({
      authUser: { id: "44444444-4444-4444-4444-444444444444" },
      precheckAllowed: true,
      updateUserSucceeds: true,
    });
    try {
      const handler = await captureHandler(MODULE_PATH);
      const response = await handler(
        makeRequest({ target_user_id: VALID_UUID, new_password: VALID_PASSWORD }),
      );

      if (response.status !== 200) {
        throw new Error(`প্রত্যাশা ছিল status 200, পাওয়া গেছে: ${response.status}`);
      }
      const body = await response.json();
      if (body.result !== "OK") {
        throw new Error(`প্রত্যাশা ছিল {result: "OK"}, পাওয়া গেছে: ${JSON.stringify(body)}`);
      }
    } finally {
      restoreFetch();
    }
  },
);

Deno.test(
  "admin-reset-user-password: (ঘ) new_password < 6 chars → 400 INVALID_PASSWORD (auth check-এরও আগে, mock ছাড়াই)",
  async () => {
    setDummySupabaseEnv();
    const handler = await captureHandler(MODULE_PATH);
    // ইচ্ছাকৃতভাবে কোনো Authorization header/fetch-mock নেই — এই ভ্যালিডেশন
    // index.ts-এর একদম প্রথম দিকে, network-এ পৌঁছানোরই আগে ঘটার কথা।
    const response = await handler(
      makeRequest({ target_user_id: VALID_UUID, new_password: "abc" }, false),
    );

    if (response.status !== 400) {
      throw new Error(`প্রত্যাশা ছিল status 400, পাওয়া গেছে: ${response.status}`);
    }
    const body = await response.json();
    if (body.error !== "INVALID_PASSWORD") {
      throw new Error(`প্রত্যাশা ছিল error="INVALID_PASSWORD", পাওয়া গেছে: ${JSON.stringify(body)}`);
    }
  },
);

Deno.test(
  "admin-reset-user-password: target_user_id missing → 400 INVALID_TARGET_USER_ID (mock ছাড়াই)",
  async () => {
    setDummySupabaseEnv();
    const handler = await captureHandler(MODULE_PATH);
    const response = await handler(makeRequest({ new_password: VALID_PASSWORD }, false));

    if (response.status !== 400) {
      throw new Error(`প্রত্যাশা ছিল status 400, পাওয়া গেছে: ${response.status}`);
    }
    const body = await response.json();
    if (body.error !== "INVALID_TARGET_USER_ID") {
      throw new Error(
        `প্রত্যাশা ছিল error="INVALID_TARGET_USER_ID", পাওয়া গেছে: ${JSON.stringify(body)}`,
      );
    }
  },
);

Deno.test(
  "admin-reset-user-password: target_user_id non-string (number) → 400 INVALID_TARGET_USER_ID (mock ছাড়াই)",
  async () => {
    setDummySupabaseEnv();
    const handler = await captureHandler(MODULE_PATH);
    const response = await handler(
      makeRequest({ target_user_id: 12345, new_password: VALID_PASSWORD }, false),
    );

    if (response.status !== 400) {
      throw new Error(`প্রত্যাশা ছিল status 400, পাওয়া গেছে: ${response.status}`);
    }
    const body = await response.json();
    if (body.error !== "INVALID_TARGET_USER_ID") {
      throw new Error(
        `প্রত্যাশা ছিল error="INVALID_TARGET_USER_ID", পাওয়া গেছে: ${JSON.stringify(body)}`,
      );
    }
  },
);

Deno.test(
  "admin-reset-user-password: admin+auth ঠিক কিন্তু auth.admin.updateUserById ব্যর্থ → 400, real updateErr.message",
  async () => {
    setDummySupabaseEnv();
    const restoreFetch = installMockSupabaseFetch({
      authUser: { id: "55555555-5555-5555-5555-555555555555" },
      precheckAllowed: true,
      updateUserSucceeds: false, // mock: /auth/v1/admin/users/* → 400 { msg: "mock update failure" }
    });
    try {
      const handler = await captureHandler(MODULE_PATH);
      const response = await handler(
        makeRequest({ target_user_id: VALID_UUID, new_password: VALID_PASSWORD }),
      );

      if (response.status !== 400) {
        throw new Error(`প্রত্যাশা ছিল status 400, পাওয়া গেছে: ${response.status}`);
      }
      const body = await response.json();
      // @supabase/auth-js-এর _getErrorMessage() মক-এর "msg" ফিল্ড থেকে সরাসরি error.message
      // বানায় (এই ফাইলের হেডার-কমেন্টে npm সোর্স-verification দ্রষ্টব্য) — তাই এখানে exact
      // মান assert করা যাচ্ছে, শুধু "কোনো string আছে কিনা" না।
      if (body.error !== "mock update failure") {
        throw new Error(
          `প্রত্যাশা ছিল error="mock update failure" (auth-js _getErrorMessage()-এর "msg"-ফিল্ড ` +
            `priority অনুযায়ী), পাওয়া গেছে: ${JSON.stringify(body)}`,
        );
      }
    } finally {
      restoreFetch();
    }
  },
);
