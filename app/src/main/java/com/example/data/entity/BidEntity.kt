package com.example.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "bids",
    indices = [
        Index("problemId"),
        Index("solverId"),
        Index("status"),
        Index("createdAt")
    ]
)
data class BidEntity(
    @PrimaryKey val id: String,
    val problemId: String,
    val solverId: String,
    val solverName: String,
    val solverPhone: String,
    val solverRating: Double = 5.0,
    val amount: Double,
    val message: String,
    val estimatedTime: String,
    val status: String = "PENDING", // "PENDING", "ACCEPTED", "REJECTED", "CANCELLED"
    val progressAtCancel: Int? = null,
    val createdAt: Long = System.currentTimeMillis(),
    // How THIS specific bid's cycle ended, stamped at the moment it was cancelled/resolved:
    // "SOLVER_CANCEL", "ADMIN_SPLIT", "ADMIN_REFUND_TO_USER", "ADMIN_RELEASE_TO_SOLVER".
    // Null for bids that were never cancelled/resolved (still PENDING/ACCEPTED/REJECTED/WITHDRAWN),
    // or for older bids saved before this field existed.
    //
    // WHY THIS EXISTS: a single problem/post can go through several bid cycles (bid A accepted ->
    // cancelled -> bid B accepted -> disputed -> resolved). The ProblemEntity only has ONE shared
    // set of dispute fields (isDisputed, disputeResolutionDecision, ...), which get overwritten by
    // whichever cycle resolves most recently. Reading THOSE fields to categorize an OLDER bid's
    // history entry silently reclassifies it as part of a LATER, unrelated dispute. Stamping the
    // outcome on the bid itself, at the moment that specific cycle ends, keeps each cycle's history
    // accurate regardless of what happens in later cycles on the same problem.
    val resolutionType: String? = null,
    val resolvedAt: Long? = null
)
