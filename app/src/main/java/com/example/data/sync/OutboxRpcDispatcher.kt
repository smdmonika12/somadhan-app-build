package com.example.data.sync

import com.example.data.remote.SupabaseSyncManager
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

/**
 * Outbox/retry ইনফ্রা — ধাপ ৬ (RPC_SYNC_FIX ট্র্যাক)। ডিজাইন রেফারেন্স:
 * `docs/OUTBOX_RETRY_DESIGN.md` সেকশন ৪ ("callRpcByName dispatch টেবিল")।
 *
 * **ডিজাইন ডক থেকে একটা ছোট বিচ্যুতি (deviation), স্পষ্ট করে নোট করা হলো:** ডকের pseudocode-এ
 * `SupabaseSyncManager.callRpcByName(...)` লেখা ছিল (যেন এটা `SupabaseSyncManager` object-এরই
 * একটা মেম্বার)। কিন্তু Step 6-এর কমন রুল অনুযায়ী "entity রেজিস্টার করা আর schema version
 * বাড়ানো ছাড়া বিদ্যমান কোনো ফাইলের ভেতরের বিদ্যমান কোড এডিট করা যাবে না" -- আর
 * `SupabaseSyncManager.kt` (৩২০০+ লাইনের বিদ্যমান ফাইল) এর আওতায় পড়ে। তাই dispatch টেবিলটা
 * এই সম্পূর্ণ নতুন, আলাদা ফাইলে/object-এ রাখা হয়েছে (`OutboxRpcDispatcher`), যেটা
 * `SupabaseSyncManager`-এর বিদ্যমান public suspend fun গুলো শুধু *কল* করে -- সেই ফাইলের একটা
 * লাইনও বদলায় না। আচরণগতভাবে এটা ডিজাইন ডকের বর্ণনার সাথে সমতুল্য (rpcName string থেকে সঠিক
 * ফাংশন resolve করে), শুধু ফাইল-লোকেশন/নাম আলাদা।
 *
 * **প্যারামিটার-কী কনভেনশন (এই ধাপে প্রস্তাবিত, Step 7-এ চূড়ান্ত):** ডিজাইন ডক সেকশন ৩-এর
 * উদাহরণ অনুসরণ করে (`Json.encodeToString(mapOf("escrowId" to escrow.id))`), প্রতিটা RPC-র
 * `paramsJson`-এ camelCase key আশা করা হচ্ছে (নিচে প্রতিটা `when` branch-এ কমেন্টে লেখা আছে)।
 * Step 6-এ কোনো dual-write ফাংশন এখনো outbox insert করছে না, তাই এই কী-নামগুলো এখনো কোনো
 * actual কলের সাথে যাচাই করা হয়নি -- Step 7-এ যখন `payoutEscrowToSolver` ইত্যাদির fail-ব্লকে
 * প্রকৃত insert কোড যোগ হবে, তখন এখানকার key নাম আর insert-এর সময়কার key নাম মিলছে কিনা
 * নিশ্চিত করে নেওয়া জরুরি (নাহলে worker প্রতিটা retry-তে parse error পাবে)।
 *
 * শুধু ডিজাইন ডক সেকশন ৫-এর প্রস্তাবিত ৭টা টাকা-সংক্রান্ত RPC-র জন্য এন্ট্রি আছে -- বাকি ~৮৪টা
 * RPC (non-priority) ধীরে ধীরে ভবিষ্যতে যোগ হবে, যখন সংশ্লিষ্ট dual-write ফাংশন outbox-এ wire
 * হবে (master plan-এর ব্যাকলগ সেকশন দেখুন)।
 */
object OutboxRpcDispatcher {

    /**
     * `rpcName` অনুযায়ী সঠিক `SupabaseSyncManager` ফাংশন কল করে, `paramsJson` থেকে প্যারামিটার
     * পার্স করে। অজানা `rpcName` বা parse ব্যর্থ হলে `Result.failure` (exception ছোড়ে না) --
     * worker-এর `doWork()` তাহলে normally retryCount বাড়িয়ে পরে আবার ট্রাই করবে, worker নিজে
     * crash করবে না।
     */
    suspend fun callRpcByName(rpcName: String, paramsJson: String): Result<JsonElement> {
        return try {
            val params = Json.parseToJsonElement(paramsJson) as? JsonObject
                ?: return Result.failure(IllegalArgumentException("paramsJson is not a JSON object for rpcName=$rpcName"))
            when (rpcName) {
                // paramsJson: {"escrowId": String}
                "release_escrow" -> SupabaseSyncManager.releaseEscrow(
                    escrowId = params.requireString("escrowId")
                )

                // paramsJson: {"escrowId": String, "refundType": String, "refundPercentage": Double}
                "refund_escrow_once" -> SupabaseSyncManager.refundEscrow(
                    escrowId = params.requireString("escrowId"),
                    refundType = params.requireString("refundType"),
                    refundPercentage = params.requireDouble("refundPercentage")
                )

                // paramsJson: {"escrowId": String, "amount": Double} -- addToEscrow()-এর RPC
                "increment_escrow_extra_amount" -> SupabaseSyncManager.incrementEscrowExtraAmount(
                    escrowId = params.requireString("escrowId"),
                    amount = params.requireDouble("amount")
                )

                // paramsJson: {"userId","amount","gateway","gatewayTrxId","senderPhone","note": String/Double}
                "deposit_money_via_gateway" -> SupabaseSyncManager.depositMoneyViaGateway(
                    userId = params.requireString("userId"),
                    amount = params.requireDouble("amount"),
                    gateway = params.requireString("gateway"),
                    gatewayTrxId = params.requireString("gatewayTrxId"),
                    senderPhone = params.requireString("senderPhone"),
                    note = params.requireString("note")
                )

                // paramsJson: {"amount","method","accountNumber","bankName"?,"branchName"?,"accountHolderName"?,"role"?,"clientWithdrawalId"?}
                // [id-mismatch ফিক্স] "clientWithdrawalId" থাকলে RPC-কে পাঠানো হয় (migration
                // step38-এর নতুন p_client_withdrawal_id) যাতে retry-created cloud row local
                // record-এর সাথে একই id শেয়ার করে (আগে এখানে না থাকায় retry নতুন id দিয়ে
                // একটা "এতিম" cloud row বানাতো)। পুরনো (এই ফিল্ড ছাড়া) enqueue হওয়া outbox
                // এন্ট্রি থাকলেও optionalString null দেবে, RPC আগের মতোই server-side id
                // জেনারেট করবে — কোনো crash হবে না।
                "request_withdrawal" -> SupabaseSyncManager.requestWithdrawal(
                    amount = params.requireDouble("amount"),
                    method = params.requireString("method"),
                    accountNumber = params.requireString("accountNumber"),
                    bankName = params.optionalString("bankName"),
                    branchName = params.optionalString("branchName"),
                    accountHolderName = params.optionalString("accountHolderName"),
                    role = params.optionalString("role"),
                    clientWithdrawalId = params.optionalString("clientWithdrawalId")
                )

                // paramsJson: {"withdrawalId","action": String, "trxId"?: String}
                "process_withdrawal" -> SupabaseSyncManager.processWithdrawal(
                    withdrawalId = params.requireString("withdrawalId"),
                    action = params.requireString("action"),
                    trxId = params.optionalString("trxId")
                )

                // paramsJson: {"userId","reason": String, "amount": Double, "isAddition": Boolean, "role"?: String}
                "admin_adjust_balance" -> SupabaseSyncManager.adminAdjustBalance(
                    userId = params.requireString("userId"),
                    amount = params.requireDouble("amount"),
                    isAddition = params.requireBoolean("isAddition"),
                    reason = params.requireString("reason"),
                    role = params.optionalString("role")
                )

                // paramsJson: {"userId": String}
                "admin_approve_kyc" -> SupabaseSyncManager.adminApproveKyc(
                    userId = params.requireString("userId")
                )

                // paramsJson: {"userId","reason": String}
                "admin_reject_kyc" -> SupabaseSyncManager.adminRejectKyc(
                    userId = params.requireString("userId"),
                    reason = params.requireString("reason")
                )

                // paramsJson: {"userId","reason": String}
                "admin_revoke_kyc" -> SupabaseSyncManager.adminRevokeKyc(
                    userId = params.requireString("userId"),
                    reason = params.requireString("reason")
                )

                // paramsJson: {"userId": String, "banned": Boolean, "role"?: String}
                "admin_set_banned" -> SupabaseSyncManager.adminSetBanned(
                    userId = params.requireString("userId"),
                    banned = params.requireBoolean("banned"),
                    role = params.optionalString("role")
                )

                // paramsJson: {"userId": String, "restricted": Boolean, "role"?: String}
                "admin_set_restricted" -> SupabaseSyncManager.adminSetRestricted(
                    userId = params.requireString("userId"),
                    restricted = params.requireBoolean("restricted"),
                    role = params.optionalString("role")
                )

                // paramsJson: {"userId": String, "verified": Boolean, "role"?: String}
                // adminSetVerifiedBadge() রিটার্ন করে Result<Unit> (অন্যগুলোর মতো Result<JsonElement>
                // না) -- callRpcByName()-এর কমন রিটার্ন টাইপের সাথে মেলাতে সফল হলে একটা placeholder
                // JsonPrimitive("OK")-এ ম্যাপ করা হলো (worker শুধু success/failure দেখে, ভেতরের
                // মান ব্যবহার করে না)।
                "admin_set_verified_badge" -> SupabaseSyncManager.adminSetVerifiedBadge(
                    userId = params.requireString("userId"),
                    verified = params.requireBoolean("verified"),
                    role = params.optionalString("role")
                ).map { JsonPrimitive("OK") as JsonElement }

                // paramsJson: {"userId","newRole": String}
                "admin_change_role" -> SupabaseSyncManager.adminChangeRole(
                    userId = params.requireString("userId"),
                    newRole = params.requireString("newRole")
                )

                // [Step 12.3] paramsJson: {"problemId","bidId": String, "gatewayTrxId"?,"gateway"?: String,
                // "gatewayAmount"?: Double} -- SomadhanRepository.acceptBid()-এর enqueueOutboxRetry কলের
                // key-এর সাথে হুবহু মিলছে। "gatewayAmount" এর জন্য optionalDouble হেল্পার নেই (এই ধাপে
                // নতুন হেল্পার অনুমোদিত না), তাই inline: key না থাকলে/null হলে null।
                "accept_bid" -> SupabaseSyncManager.acceptBid(
                    problemId = params.requireString("problemId"),
                    bidId = params.requireString("bidId"),
                    gatewayTrxId = params.optionalString("gatewayTrxId"),
                    gateway = params.optionalString("gateway"),
                    gatewayAmount = params["gatewayAmount"]?.jsonPrimitive?.doubleOrNull,
                    // [Step 12.11] ঐচ্ছিক -- আগে জমা পুরনো entry-তে key নেই → null → সার্ভার আগের মতো "ESC_<uuid>" বানায়
                    escrowId = params.optionalString("escrowId")
                )

                // [Step 12.4] paramsJson: {"problemId","note": String, "extraAmount": Double} --
                // SomadhanRepository.requestJobRelease()-এর enqueueOutboxRetry কলের key-এর সাথে মিলছে।
                "request_job_release" -> SupabaseSyncManager.requestJobRelease(
                    problemId = params.requireString("problemId"),
                    extraAmount = params.requireDouble("extraAmount"),
                    note = params.requireString("note")
                )

                // [Step 12.4] paramsJson: {"problemId": String}
                "cancel_job_release_request" -> SupabaseSyncManager.cancelJobReleaseRequest(
                    problemId = params.requireString("problemId")
                )

                // [Step 12.4] paramsJson: {"problemId","reason": String}
                "reject_job_release_request" -> SupabaseSyncManager.rejectJobReleaseRequest(
                    problemId = params.requireString("problemId"),
                    reason = params.requireString("reason")
                )

                // [Step 12.4] paramsJson: {"problemId": String}
                "withdraw_dispute" -> SupabaseSyncManager.withdrawDispute(
                    problemId = params.requireString("problemId")
                )

                // [Step 12.4] paramsJson: {"problemId": String}
                "settle_dispute" -> SupabaseSyncManager.settleDispute(
                    problemId = params.requireString("problemId")
                )

                // [Step 12.4] paramsJson: {"problemId","escrowId","resolutionDecision","decisionNote": String,
                // "splitSolverPercent","solverGrossAmount","commissionAmount","solverNetAmount",
                // "userRefundAmount": Double, "progressAtSettlement"?: Int} -- SomadhanRepository-এর
                // adminResolveDisputeLocked()-এর resolveDisputeSplit enqueueOutboxRetry কলের key-এর সাথে
                // হুবহু মিলছে। "progressAtSettlement" ঐচ্ছিক Int -- optionalDouble-এর মতোই এই ধাপে নতুন
                // হেল্পার-ফাংশন অনুমোদিত না, তাই inline `intOrNull` (উপরে import যোগ করা হলো, ঠিক
                // gatewayAmount-এর doubleOrNull প্যাটার্নের মতোই)।
                "resolve_dispute_split" -> SupabaseSyncManager.resolveDisputeSplit(
                    problemId = params.requireString("problemId"),
                    escrowId = params.requireString("escrowId"),
                    splitSolverPercent = params.requireDouble("splitSolverPercent"),
                    solverGrossAmount = params.requireDouble("solverGrossAmount"),
                    commissionAmount = params.requireDouble("commissionAmount"),
                    solverNetAmount = params.requireDouble("solverNetAmount"),
                    userRefundAmount = params.requireDouble("userRefundAmount"),
                    resolutionDecision = params.requireString("resolutionDecision"),
                    decisionNote = params.requireString("decisionNote"),
                    progressAtSettlement = params["progressAtSettlement"]?.jsonPrimitive?.intOrNull
                )

                // [Step 12.5] paramsJson: {"dryRun": Boolean}
                "admin_reconcile_escrow_states" -> SupabaseSyncManager.adminReconcileEscrowStates(
                    dryRun = params.requireBoolean("dryRun")
                )

                // [Step 12.5] paramsJson: {"dryRun": Boolean}
                "admin_reconcile_user_balances" -> SupabaseSyncManager.adminReconcileUserBalances(
                    dryRun = params.requireBoolean("dryRun")
                )

                // [Step 12.5] paramsJson: {"dryRun": Boolean}
                "admin_cleanup_duplicate_refunds" -> SupabaseSyncManager.adminCleanupDuplicateRefunds(
                    dryRun = params.requireBoolean("dryRun")
                )

                // [Step 12.5] paramsJson: {"dryRun": Boolean}
                "admin_repair_missing_refunds" -> SupabaseSyncManager.adminRepairMissingRefunds(
                    dryRun = params.requireBoolean("dryRun")
                )

                // [Step 12.5] paramsJson: {"problemId","reason": String, "reopenAsOpen": Boolean} --
                // SomadhanRepository.solverCancelJob()-এর enqueueOutboxRetry কলের key-এর সাথে মিলছে।
                "solver_cancel_job" -> SupabaseSyncManager.solverCancelJob(
                    problemId = params.requireString("problemId"),
                    reason = params.requireString("reason"),
                    reopenAsOpen = params.requireBoolean("reopenAsOpen")
                )

                // [Step 12.5] paramsJson: {"chargeId": String} -- SomadhanRepository.confirmReleaseAndComplete()-এর
                // markAdditionalChargeSettled() enqueueOutboxRetry কলের key-এর সাথে মিলছে (বুক-কিপিং-অনলি,
                // কোনো wallet/escrow টাচ করে না)।
                "mark_additional_charge_settled" -> SupabaseSyncManager.markAdditionalChargeSettled(
                    chargeId = params.requireString("chargeId")
                )

                // [Step 12.6] paramsJson: {"withdrawalId","newTrxId": String} --
                // SomadhanRepository.adminUpdateWithdrawalTrxId()-এর enqueueOutboxRetry কলের key-এর
                // সাথে মিলছে (money-movement ছোঁয় না, শুধু trx_id SET)।
                "admin_update_withdrawal_trx_id" -> SupabaseSyncManager.adminUpdateWithdrawalTrxId(
                    withdrawalId = params.requireString("withdrawalId"),
                    newTrxId = params.requireString("newTrxId")
                )

                // [Step 12.6] paramsJson: {"problemId": String} --
                // SomadhanRepository.adminRefundEscrow()-এর enqueueOutboxRetry কলের key-এর সাথে
                // মিলছে (এই RPC নিজে কোনো balance/escrow ছোঁয় না, শুধু problem/bid reopen)।
                "admin_refund_and_reopen_problem" -> SupabaseSyncManager.adminRefundAndReopenProblem(
                    problemId = params.requireString("problemId")
                )

                // [Step 12.6] paramsJson: {"problemId","reason": String, "amount": Double} --
                // SomadhanRepository.requestAdditionalCharge()-এর enqueueOutboxRetry কলের key-এর
                // সাথে মিলছে।
                "request_additional_charge" -> SupabaseSyncManager.requestAdditionalCharge(
                    problemId = params.requireString("problemId"),
                    reason = params.requireString("reason"),
                    amount = params.requireDouble("amount")
                )

                // [Step 12.6] paramsJson: {"chargeId": String, "accept": Boolean} --
                // SomadhanRepository.respondToAdditionalCharge()-এর enqueueOutboxRetry কলের key-এর
                // সাথে মিলছে।
                "respond_additional_charge" -> SupabaseSyncManager.respondToAdditionalCharge(
                    chargeId = params.requireString("chargeId"),
                    accept = params.requireBoolean("accept")
                )

                // [Step 12.7] paramsJson: {"solverId": String, "newJobCount","newMissCount": Int} --
                // SomadhanRepository.trackExtraPaymentMissCycle()-এর দুটো enqueueOutboxRetry কলের key-এর
                // সাথে হুবহু মিলছে। Int-এর জন্য require-হেল্পার নেই (এই ধাপে নতুন হেল্পার অনুমোদিত না),
                // তাই inline `intOrNull` + throw -- ঠিক progressAtSettlement-এর প্যাটার্নে, শুধু required।
                "system_track_extra_payment_miss" -> SupabaseSyncManager.systemTrackExtraPaymentMiss(
                    solverId = params.requireString("solverId"),
                    newJobCount = params["newJobCount"]?.jsonPrimitive?.intOrNull
                        ?: throw IllegalArgumentException("missing/invalid int param 'newJobCount'"),
                    newMissCount = params["newMissCount"]?.jsonPrimitive?.intOrNull
                        ?: throw IllegalArgumentException("missing/invalid int param 'newMissCount'")
                )

                // [Step 12.7] paramsJson: {"problemId","status","directContractStatus": String} --
                // SomadhanRepository-এর দুটো সাইট (adminUpdateDirectContractStatus-এর নিজের ব্লক এবং
                // adminCancelAndRefundDirectContract-এর ব্লক) একই rpcName ব্যবহার করে, একই key-সেটে।
                "admin_update_direct_contract_status" -> SupabaseSyncManager.adminUpdateDirectContractStatus(
                    problemId = params.requireString("problemId"),
                    status = params.requireString("status"),
                    directContractStatus = params.requireString("directContractStatus")
                )

                // [Step 12.7] paramsJson: {"problemId","note": String, "amount": Double} --
                // SomadhanRepository.requestExtraAmount()-এর enqueueOutboxRetry কলের key-এর সাথে মিলছে।
                "request_extra_amount" -> SupabaseSyncManager.requestExtraAmount(
                    problemId = params.requireString("problemId"),
                    amount = params.requireDouble("amount"),
                    note = params.requireString("note")
                )

                // [Step 12.7] paramsJson: {"problemId": String} --
                // SomadhanRepository.userRejectExtraAmount()-এর enqueueOutboxRetry কলের key-এর সাথে
                // মিলছে (RPC কোনো balance/escrow ছোঁয় না, শুধু pending ফিল্ড null করে)।
                "user_reject_extra_amount" -> SupabaseSyncManager.userRejectExtraAmount(
                    problemId = params.requireString("problemId")
                )

                // [Step 12.8a] paramsJson: {"problemId": String} --
                // SomadhanRepository.acceptDirectContractProposal()-এর enqueueOutboxRetry কলের key-এর
                // সাথে মিলছে (RPC idempotent: ALREADY_PROCESSED গার্ড, direct_contract_status <>
                // 'PENDING_ACCEPTANCE' হলে escrow দ্বিতীয়বার insert হয় না)।
                "accept_direct_contract" -> SupabaseSyncManager.acceptDirectContract(
                    problemId = params.requireString("problemId"),
                    // [Step 12.11] ঐচ্ছিক -- পুরনো entry-তে key নেই → null (আগের আচরণ)
                    escrowId = params.optionalString("escrowId")
                )

                // [Step 12.8a] paramsJson: {"problemId","solverId": String, "usedCount": Int,
                // "monthKey": String} -- SomadhanRepository.resolveCommissionRateForNewJob()-এর দুটো
                // enqueueOutboxRetry কলের key-এর সাথে হুবহু মিলছে (RPC idempotent: absolute value সেট
                // করে, increment না)। Int-এর জন্য require-হেল্পার নেই, inline `intOrNull` + throw --
                // system_track_extra_payment_miss-এর প্যাটার্নে।
                "sync_solver_free_job_quota" -> SupabaseSyncManager.syncSolverFreeJobQuota(
                    problemId = params.requireString("problemId"),
                    solverId = params.requireString("solverId"),
                    usedCount = params["usedCount"]?.jsonPrimitive?.intOrNull
                        ?: throw IllegalArgumentException("missing/invalid int param 'usedCount'"),
                    monthKey = params.requireString("monthKey")
                )

                // [Step 12.8a] paramsJson: {"problemId","reason": String} --
                // SomadhanRepository.adminManuallyFlagDispute()-এর enqueueOutboxRetry কলের key-এর সাথে
                // মিলছে (RPC idempotent: ALREADY_DISPUTED গার্ড; problem-status চেক নেই, কিন্তু
                // ব্যবহারকারীর product-সিদ্ধান্ত অনুযায়ী COMPLETED/CANCELLED কাজেও admin dispute flag
                // করতে পারবেন, তাই retry গ্রহণযোগ্য)।
                "admin_manually_flag_dispute" -> SupabaseSyncManager.adminManuallyFlagDispute(
                    problemId = params.requireString("problemId"),
                    reason = params.requireString("reason")
                )

                // [Step 12.8c] paramsJson: {"problemId": String, "expectedAmount": Double} --
                // SomadhanRepository.userConfirmExtraAmount()-এর enqueueOutboxRetry কলের key-এর সাথে
                // হুবহু মিলছে। (আগে BLOCKED ছিল -- কারণ replay সার্ভারের *বর্তমান* pending_extra_amount
                // কনফার্ম করত, যা owner-এর অজান্তে ভিন্ন অঙ্ক কাটতে পারত; এখন server-side guard
                // (p_expected_amount, নতুন ২-arg overload) apply হয়েছে, তাই retry-উইন্ডোতে pending
                // amount বদলে গেলে RPC non-OK `AMOUNT_CHANGED` দেয়।) expectedAmount বাধ্যতামূলক
                // (requireDouble) -- না থাকলে exception, কখনো গার্ডহীন পুরনো ১-arg overload-এ নেমে যাবে না।
                "user_confirm_extra_amount" -> SupabaseSyncManager.userConfirmExtraAmount(
                    problemId = params.requireString("problemId"),
                    expectedAmount = params.requireDouble("expectedAmount")
                )

                // [Step 12.8c] paramsJson: {"problemId": String, "rate"?: Double} -- RPC না, তাই migration
                // লাগেনি। SupabaseSyncManager.adminUpdateProblemCommissionRate()-এ নতুন
                // status='OPEN'/accepted_solver_id-is-null filter-গার্ড যোগ হয়েছে (replay-এর সময় ততক্ষণে
                // solver accept করে ফেললে ০-row, কিন্তু এই ফাংশন সেটাকে বেনাইন no-op ধরে, exception
                // ছোড়ে না -- worker চিরস্থায়ী retry-loop-এ আটকাবে না)। Result<Unit> -- admin_set_verified_badge-এর
                // প্যাটার্নে placeholder JsonPrimitive("OK")-এ ম্যাপ করা হলো।
                "admin_update_problem_commission_rate" -> SupabaseSyncManager.adminUpdateProblemCommissionRate(
                    problemId = params.requireString("problemId"),
                    rate = params.optionalDouble("rate")
                ).map { JsonPrimitive("OK") as JsonElement }

                // [Step 12.8b] paramsJson: {"amount": Double, "gateway","gatewayTrxId","senderPhone","note",
                // "role","expectedUserId": String} -- SomadhanRepository.depositMoneyViaGateway()-এর
                // enqueueOutboxRetry কলের key-এর সাথে হুবহু মিলছে। (আগে Step 12.6-এ BLOCKED ছিল -- কারণ RPC
                // auth.uid()-কে টাকা দেয়; এখন server-side guard (p_expected_user_id) apply হয়েছে, তাই replay
                // ভিন্ন user-session-এ চললে RPC NOT_AUTHORIZED দেয়, ভুল ওয়ালেটে টাকা যায় না।) expectedUserId
                // বাধ্যতামূলক (requireString) -- না থাকলে exception, কখনো গার্ডহীন পুরনো signature-এ নেমে যাবে না।
                // senderPhone/note ফাঁকা হতে পারে (optionalString blank-কে null করে, তাই ?: "")।
                "request_wallet_deposit" -> SupabaseSyncManager.requestWalletDeposit(
                    amount = params.requireDouble("amount"),
                    gateway = params.requireString("gateway"),
                    gatewayTrxId = params.requireString("gatewayTrxId"),
                    senderPhone = params.optionalString("senderPhone") ?: "",
                    note = params.optionalString("note") ?: "",
                    role = params.optionalString("role"),
                    expectedUserId = params.requireString("expectedUserId")
                )

                else -> Result.failure(IllegalArgumentException("OutboxRpcDispatcher: unknown rpcName=$rpcName (no dispatch entry yet)"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun JsonObject.requireString(key: String): String =
        this[key]?.jsonPrimitive?.contentOrNullSafe()
            ?: throw IllegalArgumentException("missing/invalid string param '$key'")

    private fun JsonObject.optionalString(key: String): String? =
        this[key]?.jsonPrimitive?.contentOrNullSafe()

    private fun JsonObject.requireDouble(key: String): Double =
        this[key]?.jsonPrimitive?.doubleOrNull
            ?: throw IllegalArgumentException("missing/invalid double param '$key'")

    // [Step 12.8c] requireDouble-এর ঐচ্ছিক ভার্সন -- key না থাকলে/JSON null হলে null রিটার্ন করে,
    // exception ছোড়ে না (admin_update_problem_commission_rate-এর ঐচ্ছিক "rate" প্যারামিটারের জন্য)।
    private fun JsonObject.optionalDouble(key: String): Double? =
        this[key]?.jsonPrimitive?.doubleOrNull

    private fun JsonObject.requireBoolean(key: String): Boolean =
        this[key]?.jsonPrimitive?.booleanOrNull
            ?: throw IllegalArgumentException("missing/invalid boolean param '$key'")

    // JsonPrimitive.content ফাংশন JSON null-এ ছোড়ে -- null-safe wrapper, missing/blank হলে null
    private fun JsonPrimitive.contentOrNullSafe(): String? =
        if (this.isString) this.content.ifBlank { null } else null
}
