package com.example.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Supabase টেবিল `public.problems` এর DTO। column নাম/টাইপ Supabase MCP schema থেকে verify করা।
 */
@Serializable
data class ProblemDto(
    val id: String, // text, PK
    @SerialName("user_id") val userId: String, // uuid, FK -> users.id
    @SerialName("user_name") val userName: String = "",
    @SerialName("user_phone") val userPhone: String = "",
    @SerialName("user_address") val userAddress: String = "",
    val title: String,
    val description: String = "",
    @SerialName("category_id") val categoryId: String? = null, // nullable, FK -> categories.id
    @SerialName("category_name") val categoryName: String = "",
    @SerialName("is_physical") val isPhysical: Boolean = false,
    val latitude: Double? = null,
    val longitude: Double? = null,
    @SerialName("min_budget") val minBudget: Double = 0.0,
    @SerialName("max_budget") val maxBudget: Double = 0.0,
    val urgency: String = "সাধারণ",
    val status: String = "OPEN", // check: OPEN | IN_PROGRESS | COMPLETED | CANCELLED
    @SerialName("bids_count") val bidsCount: Int = 0,
    @SerialName("accepted_bid_id") val acceptedBidId: String? = null,
    @SerialName("accepted_solver_id") val acceptedSolverId: String? = null, // uuid, nullable, FK -> users.id
    @SerialName("accepted_solver_name") val acceptedSolverName: String? = null,
    @SerialName("accepted_amount") val acceptedAmount: Double? = null,
    @SerialName("has_release_request") val hasReleaseRequest: Boolean = false,
    @SerialName("release_request_extra_amount") val releaseRequestExtraAmount: Double = 0.0,
    @SerialName("release_request_note") val releaseRequestNote: String = "",
    @SerialName("release_requested_at") val releaseRequestedAt: String? = null, // timestamptz
    @SerialName("created_at") val createdAt: String? = null, // timestamptz
    @SerialName("completed_at") val completedAt: String? = null, // timestamptz
    @SerialName("is_direct_contract") val isDirectContract: Boolean = false,
    @SerialName("is_public") val isPublic: Boolean = true,
    @SerialName("direct_contract_status") val directContractStatus: String? = null, // check: PENDING_ACCEPTANCE | ACCEPTED | DECLINED
    val deadline: String = "",
    @SerialName("is_disputed") val isDisputed: Boolean = false,
    @SerialName("dispute_reason") val disputeReason: String? = null,
    @SerialName("dispute_initiator_id") val disputeInitiatorId: String? = null, // uuid, nullable, FK -> users.id
    @SerialName("dispute_initiator_role") val disputeInitiatorRole: String? = null,
    @SerialName("disputed_at") val disputedAt: String? = null, // timestamptz
    @SerialName("is_admin_involved_in_chat") val isAdminInvolvedInChat: Boolean = false,
    @SerialName("admin_assistance_requested_by") val adminAssistanceRequestedBy: String? = null, // check: USER | SOLVER
    @SerialName("admin_assistance_requested_at") val adminAssistanceRequestedAt: String? = null, // timestamptz
    @SerialName("is_user_deleted") val isUserDeleted: Boolean = false,
    @SerialName("applied_commission_rate") val appliedCommissionRate: Double? = null,
    @SerialName("last_activity_at") val lastActivityAt: String? = null, // timestamptz
    @SerialName("solver_last_seen_at") val solverLastSeenAt: String? = null, // timestamptz
    @SerialName("user_last_seen_at") val userLastSeenAt: String? = null, // timestamptz
    @SerialName("is_instant_job") val isInstantJob: Boolean = false,
    @SerialName("job_status") val jobStatus: String? = null,
    @SerialName("broadcast_radius_km") val broadcastRadiusKm: Double? = null,
    @SerialName("accepted_at2") val acceptedAt2: String? = null, // timestamptz (note: এই নাম-ই আসল schema তে আছে)
    @SerialName("on_way_at") val onWayAt: String? = null, // timestamptz
    @SerialName("arrived_at") val arrivedAt: String? = null, // timestamptz
    @SerialName("job_started_at") val jobStartedAt: String? = null, // timestamptz
    @SerialName("solver_live_lat") val solverLiveLat: Double? = null,
    @SerialName("solver_live_lng") val solverLiveLng: Double? = null,
    @SerialName("solver_live_updated_at") val solverLiveUpdatedAt: String? = null, // timestamptz
    @SerialName("user_live_lat") val userLiveLat: Double? = null,
    @SerialName("user_live_lng") val userLiveLng: Double? = null,
    @SerialName("pending_extra_amount") val pendingExtraAmount: Double? = null,
    @SerialName("pending_extra_amount_note") val pendingExtraAmountNote: String? = null,
    @SerialName("pending_extra_amount_requested_at") val pendingExtraAmountRequestedAt: String? = null, // timestamptz
    @SerialName("confirmed_extra_amount_total") val confirmedExtraAmountTotal: Double = 0.0,
    @SerialName("broadcast_timer_started_at") val broadcastTimerStartedAt: String? = null, // timestamptz
    @SerialName("dispute_resolution_decision") val disputeResolutionDecision: String? = null,
    @SerialName("dispute_resolution_type") val disputeResolutionType: String? = null,
    @SerialName("dispute_resolution_note") val disputeResolutionNote: String? = null,
    @SerialName("dispute_resolved_at") val disputeResolvedAt: String? = null, // timestamptz
    @SerialName("dispute_result_seen_by_user") val disputeResultSeenByUser: Boolean = false,
    @SerialName("dispute_result_seen_by_solver") val disputeResultSeenBySolver: Boolean = false,
    @SerialName("dispute_progress_at_settlement") val disputeProgressAtSettlement: Int? = null,
    @SerialName("solver_cancelled_notice") val solverCancelledNotice: String? = null,
    @SerialName("dispute_settled_at") val disputeSettledAt: String? = null, // timestamptz
    @SerialName("dispute_split_solver_percent") val disputeSplitSolverPercent: Double? = null,
    @SerialName("dispute_progress_at_raise") val disputeProgressAtRaise: Int? = null,
    @SerialName("completion_result_seen_by_user") val completionResultSeenByUser: Boolean = false,
    @SerialName("completion_result_seen_by_solver") val completionResultSeenBySolver: Boolean = false
)
