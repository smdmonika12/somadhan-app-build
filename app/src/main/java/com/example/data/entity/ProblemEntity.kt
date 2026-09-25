package com.example.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "problems",
    indices = [
        Index("userId"),
        Index("status"),
        Index("categoryId"),
        Index("acceptedSolverId"),
        Index("createdAt"),
        Index("isInstantJob"),
        Index("isDirectContract")
    ]
)
data class ProblemEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val userName: String,
    val userPhone: String,
    val userAddress: String,
    val title: String,
    val description: String,
    val categoryId: String,
    val categoryName: String,
    val isPhysical: Boolean,
    val latitude: Double,
    val longitude: Double,
    val minBudget: Double,
    val maxBudget: Double,
    val urgency: String, // "সাধারণ", "জরুরি", "খুব জরুরি"
    val status: String = "OPEN", // "OPEN", "IN_PROGRESS", "COMPLETED", "CANCELLED"
    val bidsCount: Int = 0,
    val acceptedBidId: String? = null,
    val acceptedSolverId: String? = null,
    val acceptedSolverName: String? = null,
    val acceptedAmount: Double? = null,
    val hasReleaseRequest: Boolean = false,
    val releaseRequestExtraAmount: Double = 0.0,
    val releaseRequestNote: String = "",
    val releaseRequestedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val isDirectContract: Boolean = false,
    val isPublic: Boolean = true,
    val directContractStatus: String? = null, // "PENDING_ACCEPTANCE", "ACCEPTED", "DECLINED"
    val deadline: String = "",
    val isDisputed: Boolean = false,
    val disputeReason: String? = null,
    val disputeInitiatorId: String? = null,
    val disputeInitiatorRole: String? = null, // "USER"
    val disputedAt: Long? = null,
    val isAdminInvolvedInChat: Boolean = false,
    val adminAssistanceRequestedBy: String? = null, // "USER", "SOLVER"
    val adminAssistanceRequestedAt: Long? = null,
    val isUserDeleted: Boolean = false,
    val appliedCommissionRate: Double? = null,
    val lastActivityAt: Long? = null,
    val solverLastSeenAt: Long? = null,
    val userLastSeenAt: Long? = null,
    val isInstantJob: Boolean = false,
    val jobStatus: String? = null,
    val broadcastRadiusKm: Double? = null,
    val acceptedAt2: Long? = null,
    val onWayAt: Long? = null,
    val arrivedAt: Long? = null,
    val jobStartedAt: Long? = null,
    val solverLiveLat: Double? = null,
    val solverLiveLng: Double? = null,
    val solverLiveUpdatedAt: Long? = null,
    val userLiveLat: Double? = null,
    val userLiveLng: Double? = null,
    val pendingExtraAmount: Double? = null,
    val pendingExtraAmountNote: String? = null,
    val pendingExtraAmountRequestedAt: Long? = null,
    val confirmedExtraAmountTotal: Double = 0.0,
    val broadcastTimerStartedAt: Long? = null,
    val disputeResolutionDecision: String? = null,
    val disputeResolutionType: String? = null,
    val disputeResolutionNote: String? = null,
    val disputeResolvedAt: Long? = null,
    val disputeResultSeenByUser: Boolean = false,
    val disputeResultSeenBySolver: Boolean = false,
    val disputeProgressAtSettlement: Int? = null,
    val solverCancelledNotice: String? = null,
    val disputeSettledAt: Long? = null,
    val disputeSplitSolverPercent: Double? = null,
    val disputeProgressAtRaise: Int? = null,
    val completionResultSeenByUser: Boolean = false,
    val completionResultSeenBySolver: Boolean = false
) {
    fun calculateProgressStep(): Int {
        return when {
            completedAt != null -> 5
            jobStartedAt != null || jobStatus == "STARTED" || jobStatus == "IN_PROGRESS" || jobStatus == "WORK_IN_PROGRESS" -> 4
            arrivedAt != null || jobStatus == "ARRIVED" -> 3
            onWayAt != null || acceptedAt2 != null || acceptedSolverId != null || jobStatus in listOf("ACCEPTED", "ON_THE_WAY", "ON_WAY", "SOLVER_ACCEPTED", "SOLVER_EN_ROUTE") -> 2
            else -> 1
        }
    }
}

