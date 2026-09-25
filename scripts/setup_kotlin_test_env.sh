#!/usr/bin/env bash
# scripts/setup_kotlin_test_env.sh
#
# ============================================================================
# উদ্দেশ্য
# ============================================================================
# Step 12 (Kotlin dual-write coverage) আর Step 16 (offline-action-gating)-এর
# জন্য JVM/Robolectric unit test চালানো লাগবে — এটা Postgres-নির্ভর না,
# সম্পূর্ণ আলাদা toolchain (JDK + Gradle + Android SDK stub)। এই zip-এ এখনো
# একবারও এই toolchain সেটআপ/verify করা হয়নি (progress doc অনুযায়ী), তাই এটা
# প্রথমবার ধীর হতেই পারে — এই স্ক্রিপ্টের কাজ সেই প্রথমবারের "কোথায় আটকায়"
# ডিসকভারিটা এক জায়গায় বেঁধে রাখা, যাতে পরের প্রতিটা সেশনে আবার নতুন করে
# ভাবতে না হয়।
#
# ⚠️ known blocker (আগে থেকেই সতর্ক করে রাখা হলো, setup_test_env.sh-এর
# apt-get network-gap-এর মতোই): Gradle-এর নিজের dependency resolve করতে
# Google Maven (dl.google.com), Maven Central (repo.maven.apache.org বা
# repo1.maven.org), আর Gradle distribution (services.gradle.org) — এই
# ডোমেইনগুলো অনেক sandbox network policy-তে allow করা নাও থাকতে পারে (যেমন
# পোস্টগ্রেস apt-get আগের সেশনগুলোয় ছিল)। network না থাকলে এই স্ক্রিপ্ট
# প্রথম ধাপেই স্পষ্ট বলে দেবে, আন্দাজে এগোবে না।
#
# এই স্ক্রিপ্ট নিজে কোনো টেস্ট চালায় না — শুধু toolchain প্রস্তুত + একটা
# ছোট dependency-resolve sanity check করে, যাতে আসল টেস্ট লেখা/চালানোর
# সময় "build tool ঠিকমতো বসছে কিনা" এটা নিয়ে সময় নষ্ট না হয়।
#
# ব্যবহার: bash scripts/setup_kotlin_test_env.sh
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
# ধাপ ১ — JDK (Gradle/Kotlin-এর জন্য JDK 17+ লাগে সাধারণত — gradle.properties/
# build.gradle.kts-এ exact ভার্সন লেখা থাকলে সেটাই source of truth)
# ----------------------------------------------------------------------------
log "ধাপ ১: JDK"
if command -v java >/dev/null 2>&1; then
  JAVA_VER=$(java -version 2>&1 | head -n1)
  skip "java পাওয়া গেছে ($JAVA_VER)"
else
  echo "  JDK ইনস্টল করার চেষ্টা করছি (openjdk-17-jdk)..."
  if ! apt-get update -y >/tmp/apt_update_jdk.log 2>&1; then
    die "apt-get update ব্যর্থ (network বন্ধ সম্ভবত) — লগ: /tmp/apt_update_jdk.log"
  fi
  if ! apt-get install -y openjdk-17-jdk-headless >/tmp/apt_install_jdk.log 2>&1; then
    die "openjdk-17-jdk install ব্যর্থ — লগ: /tmp/apt_install_jdk.log"
  fi
  ok "JDK install সম্পন্ন"
fi

# gradle.properties / build.gradle.kts থেকে expected JDK ভার্সন বের করার চেষ্টা
# (শুধু তথ্যের জন্য — mismatch হলে Gradle নিজেই স্পষ্ট error দেবে)
if [ -f gradle.properties ]; then
  grep -i "jdk\|java" gradle.properties 2>/dev/null | sed 's/^/    note: /' || true
fi

# ----------------------------------------------------------------------------
# ধাপ ২ — gradlew executable আছে কিনা, permission ঠিক আছে কিনা
# ----------------------------------------------------------------------------
log "ধাপ ২: gradlew"
[ -f gradlew ] || die "./gradlew পাওয়া যায়নি — repo root-এ আছি তো? ($REPO_ROOT)"
if [ -x gradlew ]; then
  skip "gradlew executable"
else
  chmod +x gradlew
  ok "chmod +x gradlew করা হলো"
fi

# ----------------------------------------------------------------------------
# ধাপ ৩ — Gradle wrapper jar cache-এ আছে কিনা (থাকলে network লাগবে না এই ধাপে)
# ----------------------------------------------------------------------------
log "ধাপ ৩: Gradle wrapper + dependency cache"
GRADLE_USER_HOME="${GRADLE_USER_HOME:-$HOME/.gradle}"
if [ -d "$GRADLE_USER_HOME/wrapper/dists" ] && find "$GRADLE_USER_HOME/wrapper/dists" -name "gradle-*-bin.zip" 2>/dev/null | grep -q .; then
  skip "Gradle distribution আগে থেকেই cache-এ আছে ($GRADLE_USER_HOME)"
else
  echo "  প্রথমবার — Gradle distribution ডাউনলোড হবে (services.gradle.org)।"
  echo "  network এই ডোমেইনে না পৌঁছালে এখানেই আটকাবে।"
fi

# --offline flag ছাড়া একবার --version চালিয়ে দেখি wrapper+distribution ঠিকমতো
# resolve হয় কিনা — এটাই আসল "network পৌঁছাচ্ছে কিনা" চেক, আন্দাজ না করে
if timeout 300 ./gradlew --version > /tmp/gradle_version.log 2>&1; then
  ok "Gradle wrapper কাজ করছে"
  grep -A1 "^Gradle" /tmp/gradle_version.log | sed 's/^/    /'
else
  warn "gradlew --version ব্যর্থ/timeout — লগ: /tmp/gradle_version.log"
  tail -n 15 /tmp/gradle_version.log | sed 's/^/    /'
  die "Gradle distribution/dependency নামানো যাচ্ছে না (network gap সম্ভবত — dl.google.com/repo.maven.apache.org/services.gradle.org allow করা আছে কিনা দেখো)। এই সেশনে Step 12/16-এর real build/test চালানো যাবে না — শুধু কোড-লেভেল inventory/static review করে এগোও।"
fi

# ----------------------------------------------------------------------------
# ধাপ ৪ — dependency-resolve sanity: শুধু dependencies resolve করে দেখি,
# পুরো build/test না চালিয়ে (দ্রুত fail করার জন্য, ভুল জায়গায় সময় না যাক)
# ----------------------------------------------------------------------------
log "ধাপ ৪: dependency resolve sanity check"
if timeout 600 ./gradlew :app:dependencies --configuration debugRuntimeClasspath -q > /tmp/gradle_deps.log 2>&1; then
  ok "app module-এর dependency resolve সফল (Google Maven/Maven Central পৌঁছানো যাচ্ছে)"
else
  warn "dependency resolve ব্যর্থ — লগ: /tmp/gradle_deps.log"
  tail -n 20 /tmp/gradle_deps.log | sed 's/^/    /'
  die "dependency নামানো যাচ্ছে না — network/repository-access সমস্যা। Step 12/16 real-run এই সেশনে সম্ভব না।"
fi

# ----------------------------------------------------------------------------
# ধাপ ৫ — Robolectric আগে থেকে dependency হিসেবে আছে কিনা (Step 12/16 স্পেক
# অনুযায়ী লাগবে — না থাকলে test module-এ যোগ করা প্রথম কাজ হবে, script
# নিজে build.gradle.kts এডিট করে না, শুধু জানিয়ে দেয়)
# ----------------------------------------------------------------------------
log "ধাপ ৫: Robolectric dependency চেক (app/build.gradle.kts)"
if grep -qi "robolectric" app/build.gradle.kts 2>/dev/null; then
  ok "app/build.gradle.kts-এ robolectric dependency পাওয়া গেছে"
else
  warn "app/build.gradle.kts-এ robolectric পাওয়া যায়নি — Step 12/16 শুরুর আগে"
  warn "testImplementation(\"org.robolectric:robolectric:<ভার্সন>\") যোগ করতে হবে (rule #1 অনুযায়ী মানুষের review-সাপেক্ষ কিনা নিশ্চিত করে নেবে, শুধু test-dependency তাই সাধারণত নিরাপদ)।"
fi

echo
echo "================================================================"
echo " Gradle/JVM toolchain প্রস্তুত ও verify করা হয়েছে।"
echo " এখন existing unit test চালাতে (baseline নেওয়ার জন্য):"
echo "   ./gradlew :app:testDebugUnitTest"
echo " Step 12/16-এর নতুন টেস্ট ফাইল লেখার পর এই একই কমান্ডেই ধরা পড়বে"
echo " (progress doc-এ বলা আছে আলাদা job/wiring লাগবে না)।"
echo "================================================================"
exit 0
