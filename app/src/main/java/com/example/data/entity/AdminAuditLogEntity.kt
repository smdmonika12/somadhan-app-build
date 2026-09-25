package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "admin_audit_logs")
data class AdminAuditLogEntity(
    @PrimaryKey val id: String,
    val actionType: String, // "BAN_USER", "DELETE_PROBLEM", "APPROVE_KYC", ইত্যাদি
    val targetId: String,   // যে ইউজার/সমস্যা/আইটেমের উপর অ্যাকশন নেওয়া হয়েছে তার id
    val targetName: String, // readable নাম, যেমন ইউজারের নাম বা সমস্যার শিরোনাম
    val details: String,    // অতিরিক্ত বিবরণ (যেমন reason, amount, ইত্যাদি)
    val timestamp: Long = System.currentTimeMillis(),
    // [ROLE_SEPARATION ধাপ ৬, অংশ খ] role-scoped action-এর ক্ষেত্রে কোন role-এ action হয়েছে —
    // "USER"/"SOLVER"/"" (blank = role-নিরপেক্ষ action, যেমন DELETE_PROBLEM, APPROVE_KYC-ছাড়া
    // অন্য profile-level কাজ)। পুরনো row-এ ডিফল্ট "" থাকবে।
    val role: String = ""
)
