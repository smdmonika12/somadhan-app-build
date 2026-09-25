-- ============================================================================
-- Step 12.11 — escrow id mismatch: READ-ONLY diagnostic (কোনো ডেটা বদলায় না)
-- ============================================================================
-- Supabase SQL editor-এ চালাও (production বা staging, শুধু select)। ফল Step 12.11 সেশনের শুরুতে দেবে।
--
-- পটভূমি (কোড পড়ে): অ্যাপ local-এ escrow বানায় id = "ESCROW_<৮অক্ষর>" (openEscrow, acceptDirectContractProposal),
-- কিন্তু cloud RPC (accept_bid, accept_direct_contract) বানায় id = "ESC_<uuid>"। অ্যাপ `release_escrow(escrow.id)`
-- পাঠায় local id দিয়ে, আর RPC সেটা `escrows.id = p_escrow_id` দিয়ে খোঁজে। realtime `escrows` টেবিল থেকে cloud
-- row-কে local-এ নতুন id-তে insert করে (কোনো id-reconcile নেই)। এর ফলে (ক) local-এ একই problem-এর দুটো escrow row
-- থাকতে পারে, (খ) release/refund-এর dual-write ESCROW_NOT_FOUND পেতে পারে (log-only, "local flow unaffected")।
-- এটা **কোড পড়ে অনুমান** — নিচের কুয়েরিগুলো সেটা সত্যি কিনা live ডেটা দিয়ে দেখাবে।

-- Q1: cloud escrows-এ id-এর ধরন কেমন? ('ESCROW_' থাকলে client-created id কোনো না কোনো পথে cloud-এ পৌঁছেছে)
select case
         when id like 'ESC\_%'    then 'ESC_  (cloud RPC-created)'
         when id like 'ESCROW\_%' then 'ESCROW_ (client-created)'
         else 'other'
       end as id_kind,
       count(*) as n
from public.escrows
group by 1
order by n desc;

-- Q2: একই problem-এ একাধিক escrow row (id-mismatch-জনিত duplicate-এর লক্ষণ; refund/re-accept cycle-ও এটা বানাতে পারে)
select problem_id, count(*) as n,
       array_agg(id order by created_at)     as ids,
       array_agg(status order by created_at) as statuses
from public.escrows
group by problem_id
having count(*) > 1
order by n desc
limit 50;

-- Q3: RELEASED PAYMENT transaction-এর escrow_id কোন ধরনের? (release_escrow সফল হলে transactions.escrow_id = সেই id)
--     ESCROW_ ধরনের সংখ্যা ০ আর ESC_ ধরনের সংখ্যা বেশি হলে বোঝা যায় client-id দিয়ে release কার্যত সফল হচ্ছে না।
select count(*) filter (where escrow_id like 'ESC\_%')    as payment_trx_with_cloud_style_escrow_id,
       count(*) filter (where escrow_id like 'ESCROW\_%') as payment_trx_with_client_style_escrow_id,
       count(*)                                           as total_payment_trx
from public.transactions
where type = 'PAYMENT';

-- Q4: HELD অবস্থায় আটকে থাকা escrow, যার problem ইতিমধ্যে COMPLETED (release cloud-এ পৌঁছায়নি এমন লক্ষণ)
select e.id, e.problem_id, e.status as escrow_status, p.status as problem_status, e.base_amount, e.extra_amount
from public.escrows e
join public.problems p on p.id = e.problem_id
where e.status = 'HELD' and p.status = 'COMPLETED'
order by e.created_at desc
limit 50;
