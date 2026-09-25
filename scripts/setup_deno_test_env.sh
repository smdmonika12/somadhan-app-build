#!/usr/bin/env bash
# scripts/setup_deno_test_env.sh
#
# ============================================================================
# উদ্দেশ্য
# ============================================================================
# Step 17 (Supabase Edge Function coverage — supabase/functions/*/index.ts,
# Deno runtime)-এর জন্য এই zip-এ এখনো Deno একবারও সেটআপ/verify করা হয়নি।
# এই স্ক্রিপ্ট Deno toolchain প্রস্তুত করে — idempotent (আগে থেকে থাকলে skip),
# আর প্রতিটা ধাপের ফলাফল সত্যিই যাচাই করে দেখায় (assume করে না)।
#
# ইনস্টল পদ্ধতি ইচ্ছাকৃতভাবে npm দিয়ে (deno.land-এর নিজস্ব install script না) —
# কারণ registry.npmjs.org / www.npmjs.com এই sandbox network-এ allow করা আছে
# (আগের সেশনগুলোর network_configuration অনুযায়ী), আর npm-এর "deno" প্যাকেজ
# postinstall-এ GitHub releases (github.com/denoland/deno, যেটাও allow করা)
# থেকে বাইনারি নামায়। deno.land/x বা deno.land/install.sh সরাসরি allow-list-এ
# নেই বলে সেটা এড়ানো হয়েছে।
#
# এই স্ক্রিপ্ট নিজে কোনো টেস্ট চালায় না — শুধু toolchain প্রস্তুত + sanity check।
#
# ব্যবহার: bash scripts/setup_deno_test_env.sh
# ============================================================================

set -uo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$REPO_ROOT"

log()  { printf '\n== %s ==\n' "$1"; }
ok()   { printf '  ✅ %s\n' "$1"; }
skip() { printf '  ⏭  %s (আগে থেকেই ঠিক আছে — skip)\n' "$1"; }
warn() { printf '  ⚠️  %s\n' "$1"; }
die()  { printf '  ‼️  %s\n' "$1"; exit 1; }

# ----------------------------------------------------------------------------
# ধাপ ১ — Node/npm আছে কিনা (deno npm প্যাকেজ নামাতে লাগবে)
# ----------------------------------------------------------------------------
log "ধাপ ১: npm"
if command -v npm >/dev/null 2>&1; then
  skip "npm পাওয়া গেছে ($(npm --version))"
else
  die "npm পাওয়া যায়নি — এই container-এ Node.js নেই। Node/npm ইনস্টল আগে করতে হবে (apt-get install nodejs npm বা nvm), সেটা এই স্ক্রিপ্টের স্কোপে নেই।"
fi

# ----------------------------------------------------------------------------
# ধাপ ২ — Deno (idempotent — আগে থেকে থাকলে skip)
# ----------------------------------------------------------------------------
log "ধাপ ২: Deno"
if command -v deno >/dev/null 2>&1; then
  skip "deno পাওয়া গেছে ($(deno --version | head -n1))"
else
  echo "  npm দিয়ে deno global install চেষ্টা করছি..."
  if npm install -g deno > /tmp/npm_deno_install.log 2>&1; then
    ok "npm install -g deno সম্পন্ন"
  else
    warn "npm install ব্যর্থ — লগ: /tmp/npm_deno_install.log"
    tail -n 20 /tmp/npm_deno_install.log | sed 's/^/    /'
    die "deno install করা গেল না (network gap সম্ভবত — registry.npmjs.org বা github.com/release-assets.githubusercontent.com পৌঁছানো যাচ্ছে কিনা দেখো)। এই সেশনে Step 17 real-run সম্ভব না — কোড-লেভেল রিভিউ দিয়ে এগোও।"
  fi

  if ! command -v deno >/dev/null 2>&1; then
    # কখনো কখনো npm global bin PATH-এ যোগ হয় না — খুঁজে বের করার চেষ্টা
    NPM_BIN=$(npm bin -g 2>/dev/null || npm root -g 2>/dev/null | sed 's/lib\/node_modules$/bin/')
    if [ -x "$NPM_BIN/deno" ]; then
      export PATH="$NPM_BIN:$PATH"
      warn "deno পাওয়া গেছে কিন্তু PATH-এ ছিল না — এই সেশনের জন্য PATH-এ যোগ করা হলো ($NPM_BIN)। স্থায়ীভাবে ~/.bashrc-তেও যোগ করে নিও।"
    else
      die "npm install সফল দেখালেও 'deno' কমান্ড PATH-এ পাওয়া যাচ্ছে না — ম্যানুয়ালি ট্রেস করতে হবে ($NPM_BIN)।"
    fi
  fi
fi

deno --version | sed 's/^/  /'

# ----------------------------------------------------------------------------
# ধাপ ৩ — Edge Function inventory (শুধু একটাই আছে ধরে না নিয়ে, প্রতিবার
# আসলেই স্ক্যান করে — progress doc-এর rule অনুযায়ী)
# ----------------------------------------------------------------------------
log "ধাপ ৩: supabase/functions/ inventory"
if [ ! -d supabase/functions ]; then
  die "supabase/functions/ ফোল্ডার পাওয়া যায়নি — repo root ঠিক আছে তো?"
fi
FN_COUNT=$(find supabase/functions -mindepth 1 -maxdepth 1 -type d | wc -l)
echo "  পাওয়া গেছে ${FN_COUNT}টা function:"
find supabase/functions -mindepth 1 -maxdepth 1 -type d -exec basename {} \; | sed 's/^/    - /'
if [ "$FN_COUNT" -gt 1 ]; then
  warn "একাধিক function পাওয়া গেছে — সব কটার জন্যই Step 17-এর টেস্ট লিখতে হবে, শুধু admin-reset-user-password না (আগের progress doc-এ শুধু ১টাই জানা ছিল)।"
fi

# ----------------------------------------------------------------------------
# ধাপ ৪ — deno lint/check sanity (আসল টেস্ট লেখার আগেই syntax/import ঠিক
# আছে কিনা দ্রুত যাচাই, যাতে টেস্ট লেখার পর একগাদা import-error না আসে)
# ----------------------------------------------------------------------------
log "ধাপ ৪: deno check (sanity)"
CHECK_FAIL=0
while IFS= read -r ts_file; do
  echo "  >> deno check $ts_file"
  if deno check "$ts_file" > /tmp/deno_check.log 2>&1; then
    ok "$ts_file — ঠিক আছে"
  else
    warn "$ts_file — deno check ব্যর্থ (import/network-dependent হতে পারে, remote import হলে network লাগবে):"
    tail -n 10 /tmp/deno_check.log | sed 's/^/    /'
    CHECK_FAIL=1
  fi
done < <(find supabase/functions -name "index.ts")

if [ "$CHECK_FAIL" -eq 1 ]; then
  warn "কিছু ফাইলে deno check ব্যর্থ হয়েছে — remote import (esm.sh/deno.land/x ইত্যাদি) network-নির্ভর হতে পারে, এটা এই স্ক্রিপ্টের ব্লকার না কিন্তু Step 17 শুরুর আগে মাথায় রাখতে হবে।"
fi

echo
echo "================================================================"
echo " Deno toolchain প্রস্তুত ও verify করা হয়েছে (${FN_COUNT}টা function পাওয়া গেছে)।"
echo " Step 17 শুরুর আগে মনে রাখতে হবে (progress doc অনুযায়ী):"
echo "   - is_admin(uid) RPC migrations-এ define করা নেই — inferred/TEMPORARY"
echo "     stub বানাতে হবে (rule #6), আর PROGRESS.md-এ স্পষ্ট লিখতে হবে এই"
echo "     security-check আসল লাইভ DB-র is_admin() বিপরীতে verify হয়নি।"
echo "   - 'deno test' চালাতে CI workflow-এ denoland/setup-deno action লাগবে।"
echo "================================================================"
exit 0
