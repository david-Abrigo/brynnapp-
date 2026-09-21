package com.example.data.supabase

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SupabaseTransactionDto(
    @Json(name = "id") val id: String? = null,
    @Json(name = "store_code") val storeCode: String,
    @Json(name = "sender_name") val senderName: String,
    @Json(name = "amount") val amount: Double,
    @Json(name = "timestamp") val timestamp: Long,
    @Json(name = "transaction_type") val transactionType: String = "RECEIVED",
    @Json(name = "raw_notification") val rawNotification: String = "",
    @Json(name = "branch_name") val branchName: String? = null,
    @Json(name = "claimed_by") val claimedBy: String? = null,
    @Json(name = "claimed_by_name") val claimedByName: String? = null,
    @Json(name = "claimed_at") val claimedAt: String? = null,
    @Json(name = "note") val note: String? = null,
    @Json(name = "created_at") val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class SupabaseStoreDto(
    @Json(name = "store_code") val storeCode: String,
    @Json(name = "store_name") val storeName: String,
    @Json(name = "last_active") val lastActive: Long = System.currentTimeMillis(),
    @Json(name = "owner_id") val ownerId: String? = null,
    @Json(name = "branches") val branches: String = "Principal,Sucursal 2",
    @Json(name = "plan") val plan: String = "FREE",
    @Json(name = "allow_worker_history") val allowWorkerHistory: Boolean = true
)

@JsonClass(generateAdapter = true)
data class SupabaseProfileDto(
    @Json(name = "id") val id: String,
    @Json(name = "email") val email: String? = null,
    @Json(name = "display_name") val displayName: String? = null,
    @Json(name = "avatar_url") val avatarUrl: String? = null,
    @Json(name = "plan") val plan: String = "FREE"
)

enum class ConnectionStatus {
    UNCONFIGURED,
    CONNECTING,
    CONNECTED,
    SYNCING,
    OFFLINE,
    ERROR
}

@JsonClass(generateAdapter = true)
data class SupabaseRedeemResponseDto(
    @Json(name = "success") val success: Boolean = false,
    @Json(name = "message") val message: String? = null,
    @Json(name = "store_code") val storeCode: String? = null,
    @Json(name = "store_name") val storeName: String? = null,
    @Json(name = "plan") val plan: String? = null
)

@JsonClass(generateAdapter = true)
data class SupabaseReceiverDto(
    @Json(name = "id") val id: String? = null,
    @Json(name = "store_code") val storeCode: String = "",
    @Json(name = "user_id") val userId: String? = null,
    @Json(name = "custom_name") val customName: String = "Trabajador",
    @Json(name = "user_email") val userEmail: String? = null,
    @Json(name = "role") val role: String = "RECEIVER",
    @Json(name = "status") val status: String = "ACTIVE",
    @Json(name = "notifications_enabled") val notificationsEnabled: Boolean = true,
    @Json(name = "schedule_enabled") val scheduleEnabled: Boolean = false,
    @Json(name = "schedule_start_time") val scheduleStartTime: String = "08:00",
    @Json(name = "schedule_end_time") val scheduleEndTime: String = "20:00",
    @Json(name = "schedule_days") val scheduleDays: String = "ALL",
    @Json(name = "branch_name") val branchName: String = "Principal",
    @Json(name = "joined_at") val joinedAt: String? = null,
    @Json(name = "last_active") val lastActive: String? = null,
    @Json(name = "pairing_code_used") val pairingCodeUsed: String? = null
)

@JsonClass(generateAdapter = true)
data class SupabaseClaimResponseDto(
    @Json(name = "success") val success: Boolean = false,
    @Json(name = "message") val message: String? = null,
    @Json(name = "branch_name") val branchName: String? = null,
    @Json(name = "current_branch") val currentBranch: String? = null
)

@JsonClass(generateAdapter = true)
data class SupabaseLinkedStoreDto(
    @Json(name = "store_code") val storeCode: String = "",
    @Json(name = "store_name") val storeName: String = "",
    @Json(name = "branch_name") val branchName: String = "Principal",
    @Json(name = "custom_name") val customName: String = "Trabajador",
    @Json(name = "status") val status: String = "ACTIVE",
    @Json(name = "store_branches") val storeBranches: String = "Principal,Sucursal 2"
)


