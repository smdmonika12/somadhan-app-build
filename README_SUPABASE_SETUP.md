# Supabase Setup — URL ও Anon Key কীভাবে বসাবেন

এই ধাপে (ধাপ ১) প্রজেক্টে Supabase SDK যোগ করা হয়েছে এবং `SupabaseClientProvider.kt` নামে একটা
client-setup ক্লাস বানানো হয়েছে। কিন্তু এখনো এতে আপনার নিজের Supabase project এর URL ও anon
key বসানো হয়নি — সেটা আপনাকে নিচের ধাপ অনুসরণ করে বসাতে হবে।

## ১. আপনার Supabase URL ও anon key কোথায় পাবেন

Supabase Dashboard → আপনার project → **Project Settings → API** এ গেলে দুটো জিনিস পাবেন:

- **Project URL** (যেমন `https://xxxxxxxxxxxx.supabase.co`)
- **anon / public key** (একটা লম্বা JWT স্ট্রিং)

## ২. কোন ফাইলে বসাতে হবে

এই প্রজেক্ট আগে থেকেই **Secrets Gradle Plugin** দিয়ে `.env` ফাইল থেকে সিক্রেট মান
(`MAPS_API_KEY`, `SMS_API_KEY`, `SENDGRID_API_KEY` ইত্যাদি) পড়ে — Supabase এর জন্যও ঠিক একই
প্যাটার্ন অনুসরণ করা হয়েছে।

1. প্রজেক্টের root এ (যেখানে `settings.gradle.kts` আছে) একটা `.env` ফাইল বানান — যদি আগে থেকে
   না থাকে। (`.env.example` ফাইলটা দেখলে বুঝবেন ফরম্যাটটা কেমন।)
2. `.env` ফাইলে এই দুটো লাইন যোগ/আপডেট করুন:

   ```
   SUPABASE_URL=https://xxxxxxxxxxxx.supabase.co
   SUPABASE_ANON_KEY=আপনার-anon-key-এখানে-বসান
   ```

3. `.env` ফাইলটা **কখনো git এ commit করবেন না** (আসল key/secret এতে থাকে)। Android Studio তে
   Gradle sync করলে Secrets Gradle Plugin স্বয়ংক্রিয়ভাবে এই মানগুলো
   `BuildConfig.SUPABASE_URL` ও `BuildConfig.SUPABASE_ANON_KEY` হিসেবে কোডে ব্যবহারযোগ্য করে
   দেবে — ঠিক যেভাবে `BuildConfig.MAPS_API_KEY` এখন কাজ করছে।

## ৩. `.env` না থাকলে কী হয়

`.env` ফাইল না থাকলে বা এতে এই দুটো key না থাকলে, Gradle `.env.example` থেকে placeholder মান
(`your_supabase_project_url_here` / `your_supabase_anon_key_here`) ব্যবহার করবে — অর্থাৎ app
build হবে, কিন্তু Supabase এর সাথে আসল সংযোগ কাজ করবে না যতক্ষণ না আপনি real মান বসাবেন।

## ৪. যাচাই করবেন কীভাবে

`SupabaseClientProvider.isConfigured()` ফাংশনটা `true` রিটার্ন করলে বুঝবেন URL/key ঠিকভাবে
বসানো হয়েছে (placeholder মান রয়ে গেলে এটা `false` রিটার্ন করে)।

> **নোট:** এই ধাপে `SupabaseClientProvider` এখনো অ্যাপের কোথাও call করা হয়নি — এটা শুধু ready
> রাখা হয়েছে। পরের ধাপগুলোতে (ধাপ ৩ থেকে) repository/viewmodel এর সাথে এটা সংযুক্ত হবে।

---

## ✅ আপডেট — `.env` তৈরি করা হয়েছে (আসল Supabase project দিয়ে)

Anthropic Supabase connector দিয়ে সরাসরি অ্যাক্সেস নিয়ে root এ `.env` ফাইলটা বানানো হয়েছে, একটা
বিদ্যমান/active Supabase প্রজেক্ট (`somadhan`, region `ap-northeast-2`) এর আসল URL ও legacy
anon (JWT) key দিয়ে। `.env` ফাইলটা `.gitignore`-এ থাকা উচিত (secret এতে আছে), তাই zip-এ থাকলেও
এটা repo-তে commit করবেন না।
