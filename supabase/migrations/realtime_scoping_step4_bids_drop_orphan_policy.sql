-- ধাপ ৪-এর একটা আগের সেশনে প্রথম খসড়ায় "bid participants can receive problem-bids-topic
-- broadcasts" নামে একটা RLS policy তৈরি হয়েছিল। পরে সেই একই লজিক পুনর্লিখে "problem bids
-- visibility broadcasts" নামে নতুন policy তৈরি করা হয় (চূড়ান্ত, Firebase-parity সিদ্ধান্ত
-- অনুযায়ী), কিন্তু পুরনো নামের policy-টা কখনো drop করা হয়নি -- ফলে দুইটা কার্যত-অভিন্ন policy
-- একসাথে ছিল (SELECT policy-গুলো OR হয় বলে এটা নিরাপত্তা-বাগ ছিল না, শুধু duplicate/messy)।
drop policy if exists "bid participants can receive problem-bids-topic broadcasts" on realtime.messages;
