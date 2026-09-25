-- scripts/ci_pre_migration_fixups.sql
-- শুধু CI workflow-র "migration apply"-এর ঠিক আগে চলে (supabase/tests/-এর বাইরে রাখা হয়েছে ইচ্ছাকৃতভাবে,
-- যাতে scripts/run_tests.sh এটা পরে আবার apply না করে — নিচের কারণ দেখুন)।
--
-- কেন: `drop_transactions_escrow_id_strict_fk.sql` migration আসল live DB-র একটা FK
-- (`transactions_escrow_id_fkey`) DROP করে। আমাদের inferred schema stub-এ সেই FK নেই (rule #6),
-- তাই stub আগে বসিয়ে migration চালালে "constraint does not exist" error দিত। এখানে আসল DB-র
-- অবস্থা মিমিক করে FK-টা আগে বসিয়ে রাখা হচ্ছে; migration সেটা drop করবে → চূড়ান্ত অবস্থা
-- আসল DB-র মতোই (FK নেই), আর wallet-deposit টেস্টের `escrow_id = GWPAY_…` insert ঠিকই চলবে।
-- run_tests.sh আবার এটা apply করলে FK ফিরে এসে ওই টেস্টগুলো ভাঙত — তাই এই ফাইল supabase/tests/-এ নেই।
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'transactions_escrow_id_fkey') THEN
    ALTER TABLE public.transactions
      ADD CONSTRAINT transactions_escrow_id_fkey FOREIGN KEY (escrow_id) REFERENCES public.escrows(id);
  END IF;
END $$;
