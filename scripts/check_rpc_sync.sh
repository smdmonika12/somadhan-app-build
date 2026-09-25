#!/usr/bin/env bash
#
# check_rpc_sync.sh
#
# কী করে:
#   Kotlin কোডে (SupabaseSyncManager.kt) client.postgrest.rpc("...") দিয়ে যত RPC কল হয়,
#   আর supabase/migrations/*.sql-এ CREATE (OR REPLACE) FUNCTION public.<name> দিয়ে যত RPC
#   ডিফাইন করা আছে — এই দুই লিস্টের ডিফ নেয়। কোনো RPC Kotlin-এ কল হয় কিন্তু কোনো migration
#   ফাইলে ডিফাইন নেই দেখলে, সেটাকে "মিসিং" হিসেবে রিপোর্ট করে এবং non-zero exit code দেয়।
#
# কীভাবে চালাতে হয় (repo root থেকে):
#   bash scripts/check_rpc_sync.sh
#
# উদ্দেশ্য:
#   ভবিষ্যতে কেউ যদি সরাসরি লাইভ Supabase DB-তে নতুন RPC বানায় (dashboard/SQL editor দিয়ে)
#   কিন্তু migration ফাইলে সেটা যোগ করতে ভুলে যায়, অথবা Kotlin কোডে নতুন RPC কল যোগ করে কিন্তু
#   migration/RPC এখনো বানায়নি — এই স্ক্রিপ্ট সেই গ্যাপ ধরবে। CI pipeline-এ বা manual
#   pre-commit/pre-release চেক হিসেবে চালানো যায়।
#
# Exit code:
#   0  — কোনো মিসিং RPC নেই (sync ঠিক আছে)
#   1  — এক বা একাধিক মিসিং RPC পাওয়া গেছে (নিচে তালিকা প্রিন্ট হবে)
#
# সীমাবদ্ধতা:
#   - এটা শুধু "Kotlin-এ কল হয় কিন্তু migration-এ নেই" দিক থেকে চেক করে — উল্টো দিকে (migration-এ
#     আছে কিন্তু Kotlin থেকে কখনো কল হয় না, dead/unused RPC) চেক করে না, সেটা এই স্ক্রিপ্টের
#     উদ্দেশ্যের বাইরে।
#   - RPC নাম স্ট্রিং লিটারেল হিসেবে সরাসরি `client.postgrest.rpc("...")` কলের প্রথম আর্গুমেন্টে
#     থাকতে হবে (ভ্যারিয়েবল/ডাইনামিক নাম দিয়ে কল করলে এই স্ক্রিপ্ট সেটা ধরতে পারবে না)।

set -euo pipefail

# repo root ধরে নেওয়া হচ্ছে এই স্ক্রিপ্টের parent-এর parent directory (scripts/../)
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$REPO_ROOT"

SYNC_MANAGER_FILE="app/src/main/java/com/example/data/remote/SupabaseSyncManager.kt"
MIGRATIONS_DIR="supabase/migrations"

if [ ! -f "$SYNC_MANAGER_FILE" ]; then
  echo "❌ ERROR: $SYNC_MANAGER_FILE পাওয়া যায়নি। এই স্ক্রিপ্ট repo root থেকে চালান।" >&2
  exit 2
fi

if [ ! -d "$MIGRATIONS_DIR" ]; then
  echo "❌ ERROR: $MIGRATIONS_DIR ফোল্ডার পাওয়া যায়নি।" >&2
  exit 2
fi

# --- ধাপ ১: Kotlin-এ কল হওয়া সব RPC নাম বের করা ---
# প্যাটার্ন: postgrest.rpc( তারপর (হয়তো নতুন লাইনে) একটা কোটেড স্ট্রিং লিটারেল। এই extraction
# python দিয়ে করা হচ্ছে (venv/extra dependency ছাড়াই, শুধু stdlib) কারণ grep দিয়ে multi-line
# reliable extract করা কঠিন।
KOTLIN_RPCS=$(python3 - "$SYNC_MANAGER_FILE" <<'PYEOF'
import re, sys
path = sys.argv[1]
with open(path, encoding="utf-8", errors="replace") as f:
    content = f.read()
names = re.findall(r'postgrest\.rpc\(\s*"([a-z_][a-z0-9_]*)"', content)
for n in sorted(set(names)):
    print(n)
PYEOF
)

# --- ধাপ ২: migration ফাইলে ডিফাইন করা সব RPC নাম বের করা ---
MIGRATION_RPCS=$(python3 - "$MIGRATIONS_DIR" <<'PYEOF'
import re, sys, glob, os
mdir = sys.argv[1]
names = set()
for path in sorted(glob.glob(os.path.join(mdir, "*.sql"))):
    with open(path, encoding="utf-8", errors="replace") as f:
        content = f.read()
    for m in re.finditer(
        r'CREATE\s+(?:OR\s+REPLACE\s+)?FUNCTION\s+public\.([a-z_][a-z0-9_]*)',
        content, re.IGNORECASE
    ):
        names.add(m.group(1))
for n in sorted(names):
    print(n)
PYEOF
)

# --- ধাপ ৩: ডিফ নেওয়া (Kotlin-এ আছে, migration-এ নেই) ---
MISSING=$(comm -23 <(echo "$KOTLIN_RPCS") <(echo "$MIGRATION_RPCS"))

KOTLIN_COUNT=$(echo "$KOTLIN_RPCS" | grep -c . || true)
MIGRATION_COUNT=$(echo "$MIGRATION_RPCS" | grep -c . || true)

echo "RPC Sync Check"
echo "=============="
echo "Kotlin-এ কল হওয়া ইউনিক RPC সংখ্যা:      $KOTLIN_COUNT"
echo "migration ফাইলে ডিফাইন করা ইউনিক RPC সংখ্যা: $MIGRATION_COUNT"
echo ""

if [ -z "$MISSING" ]; then
  echo "✅ কোনো মিসিং RPC নেই — সব sync আছে।"
  exit 0
else
  MISSING_COUNT=$(echo "$MISSING" | grep -c .)
  echo "❌ $MISSING_COUNT টা RPC Kotlin-এ কল হয় কিন্তু কোনো migration ফাইলে ডিফাইন নেই:"
  echo ""
  echo "$MISSING" | sed 's/^/  - /'
  echo ""
  echo "এগুলোর জন্য একটা নতুন migration ফাইল (recovered_<name_or_group>.sql প্যাটার্নে) যোগ করুন,"
  echo "অথবা docs/RPC_INVENTORY_REPORT.md-তে ব্লকার হিসেবে নোট করুন যদি ইচ্ছাকৃতভাবে বাদ দেওয়া হয়।"
  exit 1
fi
