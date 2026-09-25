#!/usr/bin/env bash
#
# scan_duplicate_overloads.sh
#
# কী করে:
#   supabase/migrations/*.sql-এর সব `CREATE [OR REPLACE] FUNCTION public.<name>(<args>)`
#   বের করে (case-insensitive নাম-ম্যাচ, rule #5a), প্রতিটার আর্গুমেন্ট-টাইপ সিগনেচার
#   (parameter নাম/DEFAULT বাদ দিয়ে শুধু টাইপগুলোর ক্রম) বের করে, একই নামের অধীনে কতগুলো
#   **আলাদা** টাইপ-সিগনেচার পাওয়া গেছে গোনে।
#
#   কেন টাইপ-সিগনেচার দিয়ে (শুধু নাম না): Postgres একই নামের ফাংশনকে ভিন্ন ফাংশন হিসেবে
#   চেনে যদি input-parameter টাইপের ক্রম আলাদা হয় (overload)। parameter-এর নাম বা DEFAULT
#   মান আলাদা হলেও টাইপ-ক্রম এক হলে সেটা `CREATE OR REPLACE` — একই ফাংশনের নতুন ভার্সন,
#   overload না। উল্টোদিকে টাইপ-ক্রম আলাদা হলে (যেমন ৪-আর্গ বনাম ৫-আর্গ, বা ভিন্ন টাইপ) সেটাই
#   আসল overload — আর PostgREST-এর HTTP/JSON RPC layer-এ ambiguous overload resolution-এর
#   (PGRST203 "Could not choose the best candidate function") ঝুঁকি তৈরি করে, যেটা কোনো
#   pgTAP টেস্ট (সরাসরি SQL দিয়ে explicit cast-সহ কল করে বলে) কখনো ধরে না।
#
# কীভাবে চালাতে হয় (repo root থেকে):
#   bash scripts/scan_duplicate_overloads.sh
#   bash scripts/scan_duplicate_overloads.sh /path/to/other/migrations/dir   # ঐচ্ছিক override
#
# Exit code:
#   0  — কোনো ফাংশনের একাধিক আলাদা টাইপ-সিগনেচার নেই (duplicate overload নেই)
#   1  — এক বা একাধিক ফাংশনে duplicate/ambiguous overload পাওয়া গেছে (নিচে তালিকা প্রিন্ট হবে)
#   2  — ব্যবহারে ভুল (migrations dir পাওয়া যায়নি ইত্যাদি)
#
# সীমাবদ্ধতা:
#   - শুধু `supabase/migrations/*.sql`-এ ডিফাইন করা ফাংশন স্ক্যান করে (`supabase/tests/`-এর
#     schema-stub ফাইলগুলো ইচ্ছাকৃতভাবে বাদ — ওখানে test-only stub function থাকতে পারে যেগুলো
#     আসল PostgREST schema cache-এ কখনো এক্সপোজড হয় না)।
#   - parameter টাইপ বের করার heuristic সরল: প্রতিটা top-level-comma-separated param entry-তে
#     প্রথম identifier-কে parameter-নাম ধরে বাদ দেওয়া হয়, তারপর `DEFAULT ...` অংশ (থাকলে) কেটে
#     বাকিটা টাইপ ধরা হয়। `OUT`/`INOUT`/`VARIADIC` কীওয়ার্ড থাকলে সেটাও param-নামের আগের
#     token হিসেবে বাদ পড়ে যাবে (এই codebase-এ কোনো migration-এ OUT param ব্যবহার হয়নি বলে
#     এই সীমাবদ্ধতা এখন পর্যন্ত প্রভাব ফেলেনি — নতুন migration OUT param ব্যবহার করলে এই
#     স্ক্রিপ্ট আবার review করে দেখা উচিত)।
#   - `DROP FUNCTION [IF EXISTS] public.<name>(<types>);` স্টেটমেন্ট ট্র্যাক করা হয় — migration
#     ফাইলগুলো alphabetical ক্রমে (`full-test.yml`-এর `ls ... | sort` আচরণ অনুসরণ করে) প্রসেস
#     করে, কোনো signature পরে কোনো migration-এ explicit DROP হলে সেটা "still-live duplicate"
#     তালিকা থেকে বাদ দেওয়া হয় (যেমন `request_withdrawal`-এর পুরনো ৭-আর্গ ওভারলোড,
#     `step38b_drop_old_request_withdrawal_overload.sql`-এ DROP হয়ে গেছে বলে সেটা আর সত্যিকারের
#     ambiguity না)। রিপোর্টে তাই তিনটা ভাগ: "🔴 STILL-LIVE" (এখনো effective DB-স্টেটে ambiguous,
#     exit code non-zero করে), "🟡 ALLOWLISTED" (নিচের ALLOWLIST_NAMES-এ ইচ্ছাকৃতভাবে তালিকাভুক্ত
#     — জানা, ট্রানজিশনাল overload; দেখানো হয় কিন্তু exit code non-zero করে না), আর "⚪ HISTORICAL
#     (DROP করা হয়েছে)" (এক সময় ambiguous ছিল কিন্তু migration দিয়ে ঠিক করা হয়ে গেছে)।
#
#   - **ALLOWLIST_NAMES (২০২৬-০৯-২১, Step 12.12, ব্যবহারকারীর সম্মতিতে যোগ করা):**
#     `request_wallet_deposit` (12.8b — নতুন 7-arg `p_expected_user_id` guard overload, পুরনো
#     6-arg এখনো DROP হয়নি, transitional) আর `user_confirm_extra_amount` (12.8c — নতুন 2-arg
#     `p_expected_amount` guard overload, পুরনো 1-arg এখনো DROP হয়নি, transitional) — দুটোই
#     ইচ্ছাকৃতভাবে সাময়িকভাবে ২-signature অবস্থায় আছে (client app উভয় সিগনেচার সমর্থন করে পুরনো
#     APK চলমান থাকা অবস্থায় নতুন guard রোলআউট করার জন্য), তাই এখানে allowlist করা হলো যাতে
#     `rpc-overload-scan` CI job সত্যিকারের নতুন ambiguity ধরে কিন্তু এই দুটো জানা/ইচ্ছাকৃত
#     transitional overload-এ false-positive fail না দেয়। **⚠️ এই allowlist স্থায়ী না** — যখন
#     পুরনো APK-support আর দরকার হবে না এবং পুরনো signature দুটো DROP করা হবে, তখন এই
#     allowlist entry দুটোও সরিয়ে ফেলা উচিত (নাহলে scanner ভবিষ্যতে আসলেই নতুন ambiguous overload
#     যোগ হলেও ধরতে পারবে না যদি ভুলবশত এই একই নামে হয়)।

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

MIGRATIONS_DIR="${1:-$REPO_ROOT/supabase/migrations}"

if [ ! -d "$MIGRATIONS_DIR" ]; then
  echo "❌ ERROR: migrations ফোল্ডার পাওয়া যায়নি: $MIGRATIONS_DIR" >&2
  exit 2
fi

SQL_COUNT=$(find "$MIGRATIONS_DIR" -maxdepth 1 -name '*.sql' | wc -l)
if [ "$SQL_COUNT" -eq 0 ]; then
  echo "❌ ERROR: $MIGRATIONS_DIR-এ কোনো .sql ফাইল পাওয়া যায়নি।" >&2
  exit 2
fi

python3 - "$MIGRATIONS_DIR" <<'PYEOF'
import re
import sys
import glob
import os

mdir = sys.argv[1]

FUNC_START_RE = re.compile(
    r'CREATE\s+(?:OR\s+REPLACE\s+)?FUNCTION\s+public\.([A-Za-z_][A-Za-z0-9_]*)\s*\(',
    re.IGNORECASE,
)
DROP_FUNC_START_RE = re.compile(
    r'DROP\s+FUNCTION\s+(?:IF\s+EXISTS\s+)?public\.([A-Za-z_][A-Za-z0-9_]*)\s*\(',
    re.IGNORECASE,
)


def find_balanced_args(content, open_paren_idx):
    """open_paren_idx-এ '(' ধরে নিয়ে, ব্যালান্সড বন্ধনী মিলিয়ে ভেতরের raw args-টেক্সট আর
    যেখানে ')' মেলে সেই index রিটার্ন করে। quote-এর ভেতরের প্যারেন গোনা হয় না।"""
    depth = 0
    i = open_paren_idx
    n = len(content)
    in_squote = False
    in_dquote = False
    start = open_paren_idx + 1
    while i < n:
        ch = content[i]
        if in_squote:
            if ch == "'":
                # SQL-এ '' হলো escaped quote, পরের char-ও quote হলে স্কিপ করি
                if i + 1 < n and content[i + 1] == "'":
                    i += 2
                    continue
                in_squote = False
        elif in_dquote:
            if ch == '"':
                in_dquote = False
        else:
            if ch == "'":
                in_squote = True
            elif ch == '"':
                in_dquote = True
            elif ch == '(':
                depth += 1
            elif ch == ')':
                depth -= 1
                if depth == 0:
                    return content[start:i], i
        i += 1
    return None, None  # unbalanced — parse করা গেল না


def split_top_level_commas(args_text):
    """args_text-কে top-level কমা দিয়ে ভাগ করে (নেস্টেড প্যারেন/কোট-এর ভেতরের কমা বাদ)।"""
    parts = []
    depth = 0
    in_squote = False
    in_dquote = False
    buf = []
    i = 0
    n = len(args_text)
    while i < n:
        ch = args_text[i]
        if in_squote:
            buf.append(ch)
            if ch == "'":
                if i + 1 < n and args_text[i + 1] == "'":
                    buf.append(args_text[i + 1])
                    i += 2
                    continue
                in_squote = False
        elif in_dquote:
            buf.append(ch)
            if ch == '"':
                in_dquote = False
        else:
            if ch == "'":
                in_squote = True
                buf.append(ch)
            elif ch == '"':
                in_dquote = True
                buf.append(ch)
            elif ch == '(':
                depth += 1
                buf.append(ch)
            elif ch == ')':
                depth -= 1
                buf.append(ch)
            elif ch == ',' and depth == 0:
                parts.append(''.join(buf))
                buf = []
            else:
                buf.append(ch)
        i += 1
    if buf:
        parts.append(''.join(buf))
    return [p.strip() for p in parts if p.strip()]


DEFAULT_SPLIT_RE = re.compile(r'\bDEFAULT\b', re.IGNORECASE)
LEADING_MODE_RE = re.compile(r'^(IN|OUT|INOUT|VARIADIC)\s+', re.IGNORECASE)
LEADING_IDENT_RE = re.compile(r'^([A-Za-z_][A-Za-z0-9_]*)\s+')


def param_type_of(param_text):
    """একটা single param-entry (যেমন 'p_amount numeric DEFAULT NULL') থেকে শুধু টাইপ-অংশ
    বের করে, param-নাম আর DEFAULT ছেঁটে (উপরের ফাইল-হেডারের heuristic-সীমাবদ্ধতা প্রযোজ্য)।"""
    text = param_text.strip()
    # DEFAULT-এর আগের অংশটাই দরকার
    m = DEFAULT_SPLIT_RE.search(text)
    if m:
        text = text[:m.start()].strip()
    # IN/OUT/INOUT/VARIADIC মোড-কীওয়ার্ড থাকলে বাদ
    mm = LEADING_MODE_RE.match(text)
    if mm:
        text = text[mm.end():].strip()
    # প্রথম identifier-টা param-নাম ধরে বাদ দেওয়া — কিন্তু শুধু যদি তার পরেও কিছু টোকেন
    # (আসল টাইপ) থাকে; নাহলে (যেমন শুধু 'text' লেখা, কোনো নাম ছাড়াই) পুরোটাই টাইপ।
    im = LEADING_IDENT_RE.match(text)
    if im:
        rest = text[im.end():].strip()
        if rest:
            text = rest
    # whitespace normalize (একাধিক স্পেস/newline → single space) যাতে টাইপ তুলনা স্থিতিশীল হয়
    text = re.sub(r'\s+', ' ', text).strip()
    return text.lower()


def signature_of(args_text):
    if not args_text.strip():
        return tuple()
    parts = split_top_level_commas(args_text)
    return tuple(param_type_of(p) for p in parts)


# প্রতিটা (function_name_lower) -> { type_signature_tuple: [ (file, line, raw_args), ... ] }
# (ঐতিহাসিক — কোনো migration-এ যা যা signature কখনো CREATE হয়েছে, DROP হয়েছে কিনা তা না দেখে)
found = {}

# প্রতিটা (function_name_lower) -> set(type_signature_tuple) যেগুলো এখনো DROP হয়নি (live state)
live = {}

# migration চেইন alphabetical ক্রমে apply হয় (full-test.yml-এর `ls ... | sort` আচরণ) —
# তাই ফাইলগুলো সেই একই ক্রমে প্রসেস করা হচ্ছে যাতে CREATE-এর পরে DROP এলে সঠিকভাবে বাতিল হয়।
sql_files = sorted(glob.glob(os.path.join(mdir, "*.sql")))

for path in sql_files:
    with open(path, encoding="utf-8", errors="replace") as f:
        content = f.read()
    fname = os.path.basename(path)

    # একই ফাইলে CREATE আর DROP দুটোই থাকতে পারে (যদিও এখন পর্যন্ত এই কোডবেসে হয়নি) —
    # তাই দুটোকেই তাদের নিজ নিজ position অনুযায়ী একটা মিলিত, ক্রমানুসারে ইভেন্ট-লিস্টে ফেলা হচ্ছে।
    events = []  # (position, kind, match) — kind: 'create' | 'drop'
    for m in FUNC_START_RE.finditer(content):
        events.append((m.start(), 'create', m))
    for m in DROP_FUNC_START_RE.finditer(content):
        events.append((m.start(), 'drop', m))
    events.sort(key=lambda e: e[0])

    for _, kind, m in events:
        name = m.group(1).lower()
        open_paren_idx = m.end() - 1  # regex-এর শেষ char-ই '('
        args_text, close_idx = find_balanced_args(content, open_paren_idx)
        line_no = content.count("\n", 0, m.start()) + 1
        if args_text is None:
            print(
                f"⚠️  WARNING: {fname}:{line_no} — "
                f"'{name}'-এর args balanced-parens দিয়ে parse করা যায়নি, স্কিপ করা হলো।",
                file=sys.stderr,
            )
            continue

        sig = signature_of(args_text)

        if kind == 'create':
            found.setdefault(name, {}).setdefault(sig, []).append(
                (fname, line_no, args_text.strip())
            )
            live.setdefault(name, set()).add(sig)
        else:  # drop
            live.setdefault(name, set()).discard(sig)

# --- রিপোর্ট ---
# ⚠️ ২০২৬-০৯-২১ (Step 12.12, ব্যবহারকারীর সম্মতিতে যোগ করা) — জানা/ইচ্ছাকৃত ট্রানজিশনাল
# overload (উপরে ফাইল-হেডারে বিস্তারিত কারণ) — এখানে থাকলে সেটা STILL-LIVE-এ গণ্য হবে না
# (exit code প্রভাবিত করবে না), কিন্তু রিপোর্টে আলাদা "🟡 ALLOWLISTED" ভাগে দেখানো হবে।
ALLOWLIST_NAMES = {"request_wallet_deposit", "user_confirm_extra_amount"}

all_duplicates = {name: sigs for name, sigs in found.items() if len(sigs) > 1}
allowlisted_duplicates = {
    name: sigs for name, sigs in all_duplicates.items()
    if name in ALLOWLIST_NAMES and len(live.get(name, set())) > 1
}
still_live_duplicates = {
    name: sigs for name, sigs in all_duplicates.items()
    if len(live.get(name, set())) > 1 and name not in ALLOWLIST_NAMES
}
resolved_duplicates = {
    name: sigs for name, sigs in all_duplicates.items()
    if name not in still_live_duplicates and name not in allowlisted_duplicates
}

total_functions = len(found)
total_definitions = sum(len(locs) for sigs in found.values() for locs in sigs.values())
print(f"স্ক্যান করা migration ফাইল: {len(sql_files)}")
print(f"পাওয়া মোট distinct ফাংশন-নাম: {total_functions}")
print(f"পাওয়া মোট CREATE FUNCTION স্টেটমেন্ট: {total_definitions}")
print()


def print_group(name, sigs, only_sigs=None):
    """sigs: {signature_tuple: [(file, line, raw_args), ...]}; only_sigs দিলে শুধু সেই
    সিগনেচারগুলোই দেখানো হয় (resolved-duplicate গ্রুপে যেগুলো DROP হয়ে গেছে সেগুলো বাদ)।"""
    shown = {s: locs for s, locs in sigs.items() if only_sigs is None or s in only_sigs}
    print(f"  ⚠️  public.{name}  —  {len(shown)}টা আলাদা signature:")
    for sig, locs in shown.items():
        arity = len(sig)
        raw_args = sorted(locs)[-1][2]
        file_list = ", ".join(f"{f}:{ln}" for f, ln, _ in sorted(locs))
        dropped_note = "" if sig in live.get(name, set()) else "  [DROP হয়ে গেছে]"
        print(f"      - {arity}-arg: ({raw_args}){dropped_note}")
        print(f"        পাওয়া গেছে: {file_list}")
    print()


if resolved_duplicates:
    print(
        f"⚪ HISTORICAL (এক সময় ambiguous ছিল, migration দিয়ে DROP করে ঠিক করা হয়ে গেছে — "
        f"{len(resolved_duplicates)}টা, exit code-এ ধরা হয়নি):\n"
    )
    for name in sorted(resolved_duplicates):
        print_group(name, resolved_duplicates[name])

if allowlisted_duplicates:
    print(
        f"🟡 ALLOWLISTED (জানা/ইচ্ছাকৃত ট্রানজিশনাল overload, ALLOWLIST_NAMES-এ তালিকাভুক্ত — "
        f"{len(allowlisted_duplicates)}টা, exit code-এ ধরা হয়নি, কিন্তু স্থায়ী সমাধান না —  "
        f"উপরে ফাইল-হেডারের নোট দ্রষ্টব্য):\n"
    )
    for name in sorted(allowlisted_duplicates):
        print_group(name, allowlisted_duplicates[name], only_sigs=live.get(name))

if not still_live_duplicates:
    print("✅ effective (এখনকার live) DB-স্টেটে কোনো ফাংশনে duplicate/ambiguous argument-signature নেই (allowlisted-ছাড়া)।")
    sys.exit(0)

print(
    f"🔴 STILL-LIVE — {len(still_live_duplicates)}টা ফাংশনে এখনো একাধিক আলাদা "
    f"argument-signature (সম্ভাব্য ambiguous overload) আছে:\n"
)

for name in sorted(still_live_duplicates):
    print_group(name, all_duplicates[name], only_sigs=live.get(name))

print(
    "এই ফাংশনগুলোর একাধিক argument-signature (overload) থাকার মানে PostgREST-এর "
    "HTTP/JSON RPC layer-এ ambiguous-overload resolution (PGRST203) ঝুঁকি থাকতে পারে — "
    "শুধু SQL/pgTAP দিয়ে explicit cast করে কল করলে এই ঝুঁকি ধরা পড়ে না।"
)
sys.exit(1)
PYEOF
