-- [Somadhan Bug-Fix — গ্রুপ ৭, আইটেম ৭.১] submit_rating(text,integer,text,text)-এ আগে কোনো
-- idempotency guard ছিল না — একই (problem_id, rater_role)-এর জন্য দ্বিতীয়বার কল করলে (retry-এর
-- কারণে হোক বা ইচ্ছাকৃত দ্বিতীয় submit) নতুন একটা ডুপ্লিকেট `ratings` row তৈরি হয়ে যেত (money-table
-- না, কিন্তু data-integrity ইস্যু — একই problem-এ একই role-এর একাধিক rating থেকে গেলে গড় rating/
-- reputation হিসাব ভুল হতে পারে)।
--
-- ফিক্স: insert-এর আগে (problem_id, rater_role) দিয়ে existing rating খুঁজে দেখা হচ্ছে — পাওয়া গেলে
-- নতুন row তৈরি না করে সেই existing rating-ই ফেরত দেওয়া হচ্ছে (retry-safe, idempotent), আর response-এ
-- একটা `already_rated: true` ফ্ল্যাগ যোগ করা হলো যাতে caller (app) চাইলে আলাদা UX দেখাতে পারে
-- ("আপনি ইতিমধ্যে rating দিয়েছেন")। rule #৩ (ন্যূনতম এডিট) অনুযায়ী বাকি ফাংশন অপরিবর্তিত।

CREATE OR REPLACE FUNCTION public.submit_rating(p_problem_id text, p_stars integer, p_comment text, p_rater_role text)
 RETURNS jsonb
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
declare
  v_problem public.problems%rowtype;
  v_rating_id text := 'RATE_' || replace(gen_random_uuid()::text, '-', '');
  v_existing_rating_id text;
begin
  select * into v_problem from public.problems where id = p_problem_id;
  if not found then raise exception 'PROBLEM_NOT_FOUND'; end if;
  if p_rater_role not in ('USER', 'SOLVER') then raise exception 'INVALID_RATER_ROLE'; end if;
  if p_rater_role = 'USER' and auth.uid() <> v_problem.user_id then raise exception 'NOT_AUTHORIZED'; end if;
  if p_rater_role = 'SOLVER' and auth.uid() <> v_problem.accepted_solver_id then raise exception 'NOT_AUTHORIZED'; end if;

  -- [৭.১ idempotency guard] একই problem-এ একই role আগে থেকেই rating দিয়ে থাকলে নতুন row না বানিয়ে
  -- সেটাই ফেরত দেওয়া হচ্ছে।
  select id into v_existing_rating_id
    from public.ratings
    where problem_id = p_problem_id and rater_role = p_rater_role
    limit 1;

  if v_existing_rating_id is not null then
    return jsonb_build_object('result', 'OK', 'rating_id', v_existing_rating_id, 'already_rated', true);
  end if;

  insert into public.ratings (id, problem_id, problem_title, user_id, solver_id, stars, comment, rater_role, created_at)
  values (v_rating_id, p_problem_id, v_problem.title, v_problem.user_id, v_problem.accepted_solver_id,
    p_stars, p_comment, p_rater_role, now());

  return jsonb_build_object('result', 'OK', 'rating_id', v_rating_id, 'already_rated', false);
end;
$function$
;
GRANT EXECUTE ON FUNCTION submit_rating(text,integer,text,text) TO anon;
GRANT EXECUTE ON FUNCTION submit_rating(text,integer,text,text) TO authenticated;
GRANT EXECUTE ON FUNCTION submit_rating(text,integer,text,text) TO postgres;
GRANT EXECUTE ON FUNCTION submit_rating(text,integer,text,text) TO service_role;
