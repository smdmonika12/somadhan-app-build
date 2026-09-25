package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.AdditionalChargeDao
import com.example.data.dao.AdminAuditLogDao
import com.example.data.dao.BidDao
import com.example.data.dao.CategoryDao
import com.example.data.dao.EscrowDao
import com.example.data.dao.FaqDao
import com.example.data.dao.GatewayPaymentDao
import com.example.data.dao.MessageDao
import com.example.data.dao.NotificationDao
import com.example.data.dao.PendingSyncOutboxDao
import com.example.data.dao.PlatformSettingDao
import com.example.data.dao.ProblemDao
import com.example.data.dao.RatingDao
import com.example.data.dao.ReputationEventDao
import com.example.data.dao.TransactionDao
import com.example.data.dao.UserDao
import com.example.data.dao.WithdrawalDao
import com.example.data.entity.AdditionalChargeEntity
import com.example.data.entity.AdminAuditLogEntity
import com.example.data.entity.BidEntity
import com.example.data.entity.CategoryEntity
import com.example.data.entity.EscrowEntity
import com.example.data.entity.FaqEntity
import com.example.data.entity.GatewayPaymentEntity
import com.example.data.entity.MessageEntity
import com.example.data.entity.NotificationEntity
import com.example.data.entity.PendingSyncOutboxEntity
import com.example.data.entity.PlatformSettingEntity
import com.example.data.entity.ProblemEntity
import com.example.data.entity.RatingEntity
import com.example.data.entity.ReputationEventEntity
import com.example.data.entity.TransactionEntity
import com.example.data.entity.UserEntity
import com.example.data.entity.WithdrawalEntity
import com.example.data.security.PasswordHasher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        UserEntity::class,
        CategoryEntity::class,
        ProblemEntity::class,
        BidEntity::class,
        MessageEntity::class,
        RatingEntity::class,
        NotificationEntity::class,
        WithdrawalEntity::class,
        TransactionEntity::class,
        PlatformSettingEntity::class,
        EscrowEntity::class,
        AdditionalChargeEntity::class,
        ReputationEventEntity::class,
        AdminAuditLogEntity::class,
        FaqEntity::class,
        GatewayPaymentEntity::class,
        PendingSyncOutboxEntity::class
    ],
    // ধাপ ৮ (MONEY_FLOW_AND_ADMIN_BUGS, বাগ D৩) — UserEntity-তে verifiedBadgeUser/
    // verifiedBadgeSolver কলাম যোগ হওয়ায় ৪৯ থেকে ৫০-এ বাড়ানো হলো।
    // [RPC_SYNC_FIX ট্র্যাক, ধাপ ৬-এর পর গ্যাপ-ফিক্স] আগে এখানে MIGRATION_49_50 লেখা/রেজিস্টার
    // করা হয়নি (fallbackToDestructiveMigration সামলাতো) — এখন সেই গ্যাপ বন্ধ করে নিচে
    // MIGRATION_49_50 explicitly লেখা ও রেজিস্টার করা হয়েছে (৪৫→৪৬/৫০→৫১-এর মতোই rule #৪
    // অনুযায়ী, destructive fallback এড়িয়ে), যাতে schema 49-এ আটকে থাকা পুরনো ইনস্টলে আপডেটের
    // সময় local DB (balance/job history/message/notification) মুছে না যায়।
    // [BALANCE_REPUTATION_ROLE_SEPARATION, ধাপ ৩] TransactionEntity-তে নতুন `role` কলাম
    // যোগ হওয়ায় ৫০ থেকে ৫১-এ বাড়ানো হলো — কিন্তু এবার (৪৯→৫০-এর মতো ফাঁকা fallback না রেখে)
    // মাস্টার প্রম্পট rule #৪ (কোনো destructive migration/data loss না, বিশেষ করে transaction
    // history) অনুযায়ী নিচে MIGRATION_50_51 explicitly লেখা ও রেজিস্টার করা হয়েছে, যাতে এই
    // version bump-এ fallbackToDestructiveMigration(dropAllTables = true) ট্রিগার হয়ে পুরনো
    // ব্যবহারকারীদের সব transaction/balance/reputation ডেটা মুছে না যায় (৪৫→৪৬-এর মতো, যেখানে
    // পরে MIGRATION_45_46 বসিয়ে সেই আগের গ্যাপ বন্ধ করা হয়েছিল)।
    // [BALANCE_REPUTATION_ROLE_SEPARATION, ধাপ ৬] NotificationEntity ও AdminAuditLogEntity-তে
    // নতুন `role` কলাম যোগ হওয়ায় ৫১ থেকে ৫২-এ বাড়ানো হলো — একই rule #৪ অনুযায়ী নিচে
    // MIGRATION_51_52 explicitly লেখা হয়েছে (destructive fallback এড়িয়ে, কোনো notification/
    // audit-log ইতিহাস মোছা হবে না)।
    // [RPC_SYNC_FIX ট্র্যাক, ধাপ ৬] নতুন PendingSyncOutboxEntity (outbox/retry ইনফ্রা, দেখুন
    // docs/OUTBOX_RETRY_DESIGN.md) যোগ হওয়ায় ৫২ থেকে ৫৩-এ বাড়ানো হলো — নিচে MIGRATION_52_53
    // explicitly লেখা হয়েছে (শুধু নতুন টেবিল CREATE, destructive fallback এড়িয়ে)। এই টেবিল
    // সম্পূর্ণ নতুন ও এখনো কোনো dual-write ফাংশন এতে insert করছে না (Step 7-এর কাজ) — তাই এই
    // version bump বিদ্যমান কোনো ব্যবহারকারীর ডেটা স্পর্শ করে না।
    // [Offline Action Gating ধাপ ৯] MessageEntity-তে নতুন `sendStatus` কলাম (messenger-স্টাইল
    // পাঠানো/ব্যর্থ/রিট্রাই ট্র্যাকিং, ChatScreen-এর ইনলাইন retry UI-এর জন্য) যোগ হওয়ায় ৫৩ থেকে
    // ৫৪-এ বাড়ানো হলো — নিচে MIGRATION_53_54 explicitly লেখা হয়েছে (শুধু নতুন কলাম, ডিফল্ট
    // 'SENT', destructive fallback এড়িয়ে, rule #৪ অনুযায়ী কোনো পুরনো মেসেজ/চ্যাট-ইতিহাস মুছে
    // যাবে না)।
    version = 54,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun categoryDao(): CategoryDao
    abstract fun problemDao(): ProblemDao
    abstract fun bidDao(): BidDao
    abstract fun messageDao(): MessageDao
    abstract fun ratingDao(): RatingDao
    abstract fun notificationDao(): NotificationDao
    abstract fun withdrawalDao(): WithdrawalDao
    abstract fun transactionDao(): TransactionDao
    abstract fun platformSettingDao(): PlatformSettingDao
    abstract fun escrowDao(): EscrowDao
    abstract fun additionalChargeDao(): AdditionalChargeDao
    abstract fun reputationEventDao(): ReputationEventDao
    abstract fun adminAuditLogDao(): AdminAuditLogDao
    abstract fun faqDao(): FaqDao
    abstract fun gatewayPaymentDao(): GatewayPaymentDao
    abstract fun pendingSyncOutboxDao(): PendingSyncOutboxDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE users ADD COLUMN lastReputationDecayCheckAt INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE withdrawals ADD COLUMN rejectionReason TEXT")
            }
        }

        val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `faqs` (
                        `id` TEXT NOT NULL,
                        `question` TEXT NOT NULL,
                        `answer` TEXT NOT NULL,
                        `displayOrder` INTEGER NOT NULL DEFAULT 0,
                        `isActive` INTEGER NOT NULL DEFAULT 1,
                        `createdAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE users ADD COLUMN favoriteSolverIds TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE problems ADD COLUMN hasReleaseRequest INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE problems ADD COLUMN releaseRequestExtraAmount REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE problems ADD COLUMN releaseRequestNote TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE problems ADD COLUMN releaseRequestedAt INTEGER")
            }
        }

        val MIGRATION_17_18 = object : Migration(17, 18) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE messages ADD COLUMN fileUrl TEXT")
                db.execSQL("ALTER TABLE messages ADD COLUMN fileName TEXT")
                db.execSQL("ALTER TABLE messages ADD COLUMN fileType TEXT")
                db.execSQL("ALTER TABLE messages ADD COLUMN isDirectContractProposal INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE messages ADD COLUMN directContractBudget REAL")
                db.execSQL("ALTER TABLE messages ADD COLUMN directContractDuration TEXT")

                db.execSQL("ALTER TABLE problems ADD COLUMN isDirectContract INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE problems ADD COLUMN isPublic INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE problems ADD COLUMN directContractStatus TEXT")
                db.execSQL("ALTER TABLE problems ADD COLUMN deadline TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_18_19 = object : Migration(18, 19) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE problems ADD COLUMN lastActivityAt INTEGER")
                db.execSQL("ALTER TABLE problems ADD COLUMN solverLastSeenAt INTEGER")
                db.execSQL("ALTER TABLE problems ADD COLUMN userLastSeenAt INTEGER")
            }
        }

        val MIGRATION_19_20 = object : Migration(19, 20) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE problems ADD COLUMN isDisputed INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE problems ADD COLUMN disputeReason TEXT")
                db.execSQL("ALTER TABLE problems ADD COLUMN disputeInitiatorId TEXT")
                db.execSQL("ALTER TABLE problems ADD COLUMN disputeInitiatorRole TEXT")
                db.execSQL("ALTER TABLE problems ADD COLUMN disputedAt INTEGER")
                db.execSQL("ALTER TABLE problems ADD COLUMN isAdminInvolvedInChat INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE problems ADD COLUMN adminAssistanceRequestedBy TEXT")
                db.execSQL("ALTER TABLE problems ADD COLUMN adminAssistanceRequestedAt INTEGER")

                db.execSQL("ALTER TABLE messages ADD COLUMN isAdminMessage INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE messages ADD COLUMN isDisputeNotice INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_20_21 = object : Migration(20, 21) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE problems ADD COLUMN isUserDeleted INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_21_22 = object : Migration(21, 22) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE problems ADD COLUMN appliedCommissionRate REAL")
            }
        }

        val MIGRATION_22_23 = object : Migration(22, 23) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE faqs ADD COLUMN targetAudience TEXT NOT NULL DEFAULT 'USER'")
            }
        }

        val MIGRATION_23_24 = object : Migration(23, 24) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE users ADD COLUMN freeJobsUsedThisMonth INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE users ADD COLUMN freeJobsMonthKey TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE users ADD COLUMN cycleJobCount INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE users ADD COLUMN cycleMissCount INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_24_25 = object : Migration(24, 25) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN baseAmount REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE transactions ADD COLUMN extraAmount REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE transactions ADD COLUMN baseCommissionAmount REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE transactions ADD COLUMN extraCommissionAmount REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE transactions ADD COLUMN extraCommissionApplied INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE transactions ADD COLUMN wasFreeQuotaJob INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_25_26 = object : Migration(25, 26) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE categories ADD COLUMN instantJobEnabled INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE categories ADD COLUMN instantJobRadiusKm REAL NOT NULL DEFAULT 5.0")
                db.execSQL("ALTER TABLE users ADD COLUMN instantJobNotificationsEnabled INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE problems ADD COLUMN isInstantJob INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE problems ADD COLUMN jobStatus TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE problems ADD COLUMN broadcastRadiusKm REAL DEFAULT NULL")
                db.execSQL("ALTER TABLE problems ADD COLUMN acceptedAt2 INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE problems ADD COLUMN onWayAt INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE problems ADD COLUMN arrivedAt INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE problems ADD COLUMN jobStartedAt INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE problems ADD COLUMN solverLiveLat REAL DEFAULT NULL")
                db.execSQL("ALTER TABLE problems ADD COLUMN solverLiveLng REAL DEFAULT NULL")
                db.execSQL("ALTER TABLE problems ADD COLUMN solverLiveUpdatedAt INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE problems ADD COLUMN userLiveLat REAL DEFAULT NULL")
                db.execSQL("ALTER TABLE problems ADD COLUMN userLiveLng REAL DEFAULT NULL")
            }
        }

        val MIGRATION_26_27 = object : Migration(26, 27) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE problems ADD COLUMN pendingExtraAmount REAL DEFAULT NULL")
                db.execSQL("ALTER TABLE problems ADD COLUMN pendingExtraAmountNote TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE problems ADD COLUMN pendingExtraAmountRequestedAt INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE problems ADD COLUMN confirmedExtraAmountTotal REAL NOT NULL DEFAULT 0.0")
            }
        }

        val MIGRATION_27_28 = object : Migration(27, 28) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE problems ADD COLUMN broadcastTimerStartedAt INTEGER DEFAULT NULL")
            }
        }

        val MIGRATION_28_29 = object : Migration(28, 29) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE problems ADD COLUMN disputeResolutionDecision TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE problems ADD COLUMN disputeResolutionNote TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE problems ADD COLUMN disputeResolvedAt INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE problems ADD COLUMN disputeProgressAtSettlement INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE problems ADD COLUMN solverCancelledNotice TEXT DEFAULT NULL")
            }
        }

        val MIGRATION_29_30 = object : Migration(29, 30) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE messages ADD COLUMN isSystemEvent INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE messages ADD COLUMN systemEventType TEXT DEFAULT NULL")
            }
        }

        val MIGRATION_30_31 = object : Migration(30, 31) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE problems ADD COLUMN disputeSettledAt INTEGER DEFAULT NULL")
            }
        }

        val MIGRATION_31_32 = object : Migration(31, 32) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE problems ADD COLUMN disputeResolutionType TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE problems ADD COLUMN disputeResultSeenByUser INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE problems ADD COLUMN disputeResultSeenBySolver INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_32_33 = object : Migration(32, 33) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE problems ADD COLUMN disputeSplitSolverPercent REAL DEFAULT NULL")
            }
        }

        val MIGRATION_33_34 = object : Migration(33, 34) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE problems ADD COLUMN disputeProgressAtRaise INTEGER DEFAULT NULL")
            }
        }

        val MIGRATION_34_35 = object : Migration(34, 35) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE problems ADD COLUMN completionResultSeenByUser INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE problems ADD COLUMN completionResultSeenBySolver INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_35_36 = object : Migration(35, 36) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE bids ADD COLUMN progressAtCancel INTEGER DEFAULT NULL")
            }
        }

        val MIGRATION_36_37 = object : Migration(36, 37) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN type TEXT NOT NULL DEFAULT 'PAYMENT'")
            }
        }

        val MIGRATION_37_38 = object : Migration(37, 38) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Problems indices
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_problems_userId` ON `problems` (`userId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_problems_status` ON `problems` (`status`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_problems_categoryId` ON `problems` (`categoryId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_problems_acceptedSolverId` ON `problems` (`acceptedSolverId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_problems_createdAt` ON `problems` (`createdAt`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_problems_isInstantJob` ON `problems` (`isInstantJob`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_problems_isDirectContract` ON `problems` (`isDirectContract`)")

                // Bids indices
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_bids_problemId` ON `bids` (`problemId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_bids_solverId` ON `bids` (`solverId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_bids_status` ON `bids` (`status`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_bids_createdAt` ON `bids` (`createdAt`)")

                // Users indices
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_users_phone` ON `users` (`phone`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_users_email` ON `users` (`email`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_users_role` ON `users` (`role`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_users_linkedAccountId` ON `users` (`linkedAccountId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_users_createdAt` ON `users` (`createdAt`)")

                // Messages indices
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_messages_problemId` ON `messages` (`problemId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_messages_senderId` ON `messages` (`senderId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_messages_receiverId` ON `messages` (`receiverId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_messages_timestamp` ON `messages` (`timestamp`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_messages_isRead` ON `messages` (`isRead`)")

                // Ratings indices
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_ratings_problemId` ON `ratings` (`problemId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_ratings_solverId` ON `ratings` (`solverId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_ratings_userId` ON `ratings` (`userId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_ratings_raterRole` ON `ratings` (`raterRole`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_ratings_createdAt` ON `ratings` (`createdAt`)")

                // Notifications indices
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_notifications_userId` ON `notifications` (`userId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_notifications_isRead` ON `notifications` (`isRead`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_notifications_timestamp` ON `notifications` (`timestamp`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_notifications_targetType` ON `notifications` (`targetType`)")

                // Withdrawals indices
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_withdrawals_solverId` ON `withdrawals` (`solverId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_withdrawals_status` ON `withdrawals` (`status`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_withdrawals_createdAt` ON `withdrawals` (`createdAt`)")

                // Transactions indices
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_userId` ON `transactions` (`userId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_solverId` ON `transactions` (`solverId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_problemId` ON `transactions` (`problemId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_timestamp` ON `transactions` (`timestamp`)")

                // Escrows indices
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_escrows_problemId` ON `escrows` (`problemId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_escrows_userId` ON `escrows` (`userId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_escrows_solverId` ON `escrows` (`solverId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_escrows_status` ON `escrows` (`status`)")

                // Additional Charges indices
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_additional_charges_problemId` ON `additional_charges` (`problemId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_additional_charges_solverId` ON `additional_charges` (`solverId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_additional_charges_userId` ON `additional_charges` (`userId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_additional_charges_status` ON `additional_charges` (`status`)")

                // Reputation Events indices
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_reputation_events_userId` ON `reputation_events` (`userId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_reputation_events_problemId` ON `reputation_events` (`problemId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_reputation_events_createdAt` ON `reputation_events` (`createdAt`)")
            }
        }

        val MIGRATION_38_39 = object : Migration(38, 39) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `transactions` ADD COLUMN `escrowId` TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_39_40 = object : Migration(39, 40) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `gateway_payments` (
                        `id` TEXT NOT NULL,
                        `gatewayTrxId` TEXT NOT NULL,
                        `userId` TEXT NOT NULL,
                        `userName` TEXT NOT NULL,
                        `userPhone` TEXT NOT NULL,
                        `amount` REAL NOT NULL,
                        `gateway` TEXT NOT NULL,
                        `purpose` TEXT NOT NULL,
                        `problemId` TEXT NOT NULL DEFAULT '',
                        `problemTitle` TEXT NOT NULL DEFAULT '',
                        `status` TEXT NOT NULL DEFAULT 'SUCCESS',
                        `note` TEXT NOT NULL DEFAULT '',
                        `timestamp` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_gateway_payments_userId` ON `gateway_payments` (`userId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_gateway_payments_gatewayTrxId` ON `gateway_payments` (`gatewayTrxId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_gateway_payments_gateway` ON `gateway_payments` (`gateway`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_gateway_payments_status` ON `gateway_payments` (`status`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_gateway_payments_purpose` ON `gateway_payments` (`purpose`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_gateway_payments_timestamp` ON `gateway_payments` (`timestamp`)")
            }
        }

        val MIGRATION_40_41 = object : Migration(40, 41) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN pendingCloudSync INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_41_42 = object : Migration(41, 42) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN refundType TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE transactions ADD COLUMN refundPercentage REAL NOT NULL DEFAULT 100.0")
            }
        }

        val MIGRATION_42_43 = object : Migration(42, 43) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN cloudBalanceSynced INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_43_44 = object : Migration(43, 44) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Stamp how each bid's cycle actually ended (SOLVER_CANCEL / ADMIN_SPLIT /
                // ADMIN_REFUND_TO_USER / ADMIN_RELEASE_TO_SOLVER) directly on the bid, instead of
                // relying solely on the shared, per-cycle-overwritten dispute fields on the
                // problem. See BidEntity.kt for the full rationale.
                db.execSQL("ALTER TABLE bids ADD COLUMN resolutionType TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE bids ADD COLUMN resolvedAt INTEGER DEFAULT NULL")
            }
        }

        val MIGRATION_44_45 = object : Migration(44, 45) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Mirror of Bug 2/3's refundType fix, but for the solver's PAYMENT side: tags a
                // solver's earning transaction as coming from a dispute resolution (full release
                // or split), separate from refundType so isRefundTrx() never mistakes a payment
                // for a refund. See TransactionEntity.releaseType.
                db.execSQL("ALTER TABLE transactions ADD COLUMN releaseType TEXT NOT NULL DEFAULT ''")
            }
        }

        /**
         * মানি-ফ্লো ফিক্স, ধাপ ২ — অনুপস্থিত `MIGRATION_45_46` যোগ করা হলো।
         *
         * `version = 46` আগে থেকেই কোডে ছিল (এই সেশনের আগে থেকে, MIGRATION_PROGRESS.md-এর
         * "পূর্ব-বিদ্যমান গ্যাপ" নোট দেখুন) কিন্তু `MIGRATION_45_46` কখনো ডিফাইন/রেজিস্টার করা হয়নি
         * — শেষ রেজিস্টার করা মাইগ্রেশন ছিল `MIGRATION_44_45`। ফলে কোনো ডিভাইস যদি version 45-এ
         * থাকা অবস্থায় আপগ্রেড করতো, `fallbackToDestructiveMigration(dropAllTables = true)`
         * ট্রিগার হয়ে পুরো local DB মুছে যেত।
         *
         * এই gap-টা explain করার মতো কোনো রেকর্ড (কমিট নোট/entity field diff) কোথাও পাওয়া যায়নি —
         * `MIGRATION_44_45` (transactions.releaseType) আর `MIGRATION_46_47` (৮টা role-scoped users
         * কলাম)-এর মাঝে UserEntity/অন্য কোনো entity-তে unaccounted কোনো ফিল্ড নেই। তাই ধরে নেওয়া
         * হচ্ছে version 45→46 বাড়ানোর সময় আসলে কোনো schema পরিবর্তনই দরকার ছিল না (সম্ভবত ভুলবশত
         * version number আগেই বাড়ানো হয়েছিল) — এটা একটা **no-op bridge migration**, শুধু গ্যাপ বন্ধ
         * করে destructive fallback আটকানোর জন্য। ভবিষ্যতে যদি প্রমাণ পাওয়া যায় যে 45→46-এ আসলে কোনো
         * নির্দিষ্ট কলাম/টেবিল বদল হওয়ার কথা ছিল, তাহলে এই migration-টা আপডেট করে সেটা যোগ করতে হবে।
         */
        val MIGRATION_45_46 = object : Migration(45, 46) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // ইচ্ছাকৃতভাবে no-op — উপরের কমেন্ট দেখুন।
            }
        }

        val MIGRATION_46_47 = object : Migration(46, 47) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // ধাপ ১৪.৫ (Role-Profile Redesign) Kotlin-wiring প্রস্তুতি: Supabase `users`
                // টেবিলে (migration role_profile_14_5a...14_5d) আগেই যোগ হওয়া role-scoped
                // কলামগুলোর local Room cache সমতুল্য। পুরনো shared কলাম (balance/isBanned/
                // isRestricted/reputationScore) অক্ষত রাখা হয়েছে — এগুলো নতুন কলাম, কোনো
                // বিদ্যমান ডেটা মোছা/বদলানো হয়নি।
                db.execSQL("ALTER TABLE users ADD COLUMN balanceUser REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE users ADD COLUMN balanceSolver REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE users ADD COLUMN reputationScoreUser REAL NOT NULL DEFAULT 50.0")
                db.execSQL("ALTER TABLE users ADD COLUMN reputationScoreSolver REAL NOT NULL DEFAULT 50.0")
                db.execSQL("ALTER TABLE users ADD COLUMN isBannedUser INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE users ADD COLUMN isBannedSolver INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE users ADD COLUMN isRestrictedUser INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE users ADD COLUMN isRestrictedSolver INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_47_48 = object : Migration(47, 48) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // ধাপ ১৪.৫ (গ) — Legacy dual-row device-data migrate/merge। এই কলামটা
                // সম্পূর্ণ local/device-only (Supabase-এ এর কোনো সমতুল্য নেই, password-এর
                // মতোই) -- প্রতিটা ডিভাইসে একবারই legacy dual-row (per-role আলাদা UserEntity
                // row) local balance/reputation/ban/restrict ডেটা cloud role-scoped কলাম দিয়ে
                // মার্জ/ওভাররাইট হয়েছে কিনা তার guard। ডিফল্ট 0 (এখনো merge হয়নি)। দেখুন
                // SomadhanRepository.mergeLegacyDualRoleDataFromCloud()।
                db.execSQL("ALTER TABLE users ADD COLUMN legacyDualRoleMergeDoneAt INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_48_49 = object : Migration(48, 49) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // [ROLE_UID ফিক্স - ধাপ ৩] পুরনো dual-row (SOLVER_xxx/USER_xxx) row root-এ
                // merge হওয়ার পর সেটাকে "migrated" হিসেবে চিহ্নিত করার local-only মার্কার।
                // row মোছা হয় না (rollback সম্ভব রাখতে) — শুধু এই ফ্ল্যাগ 1 হয়। Supabase-এ
                // এর সমতুল্য কলাম নেই। ডিফল্ট 0 = এখনো merge হয়নি। শুধু নতুন কলাম যোগ,
                // কোনো বিদ্যমান ডেটা মোছা/বদলানো হয়নি (rule #৫)।
                db.execSQL("ALTER TABLE users ADD COLUMN localDualRowArchived INTEGER NOT NULL DEFAULT 0")
            }
        }

        /**
         * [RPC_SYNC_FIX ট্র্যাক, ধাপ ৬-এর পর গ্যাপ-ফিক্স] অনুপস্থিত `MIGRATION_49_50` যোগ করা হলো।
         *
         * `UserEntity.verifiedBadgeUser`/`verifiedBadgeSolver` (ডিফল্ট `true`, উপরে entity-তে
         * দেখুন) — এই দুই কলাম যোগ হওয়ার কারণেই version ৪৯→৫০ বাড়ানো হয়েছিল (কমেন্ট: "বাগ D৩"),
         * কিন্তু তখন `MIGRATION_49_50` কখনো ডিফাইন/রেজিস্টার করা হয়নি। ফলে schema version ঠিক
         * ৪৯-এ আটকে থাকা কোনো পুরনো ইনস্টল আপডেট করলে `fallbackToDestructiveMigration
         * (dropAllTables = true)` ট্রিগার হয়ে পুরো local DB মুছে যেত (নতুন ইউজার বা যারা
         * ইতিমধ্যে ৫০+-এ আছে, তাদের কোনো প্রভাব নেই)।
         *
         * ফিক্স: বিদ্যমান `MIGRATION_46_47`-এর ঠিক একই কলামগুলোর ডিফল্টের convention অনুসরণ করে
         * `Boolean = true` → `INTEGER NOT NULL DEFAULT 1`। শুধু ২টা নতুন কলাম যোগ (rule #৪ —
         * কোনো বিদ্যমান row/কলাম মোছা/ওভাররাইট হয়নি)।
         */
        val MIGRATION_49_50 = object : Migration(49, 50) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE users ADD COLUMN verifiedBadgeUser INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE users ADD COLUMN verifiedBadgeSolver INTEGER NOT NULL DEFAULT 1")
            }
        }

        val MIGRATION_50_51 = object : Migration(50, 51) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // [BALANCE_REPUTATION_ROLE_SEPARATION, ধাপ ৩] TransactionEntity.role — এই
                // transaction-টা কোন role-এর ledger-এ গোনা হবে (balanceUser/balanceSolver),
                // userId/solverId কলাম কোনটায় টাকা গেছে সেটা না। বিদ্যমান সব row-এর জন্য
                // ডিফল্ট '' (blank/legacy, TRANSACTION_ROLE_FIELD_DESIGN.md-এর অপশন B অনুযায়ী
                // reconcileUserBalances()-এ ধাপ ৪-এ read-time type-ভিত্তিক resolve হবে) —
                // কোনো বিদ্যমান row মোছা/ওভাররাইট হয়নি, শুধু নতুন কলাম যোগ (rule #৪)।
                db.execSQL("ALTER TABLE transactions ADD COLUMN role TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_51_52 = object : Migration(51, 52) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // [BALANCE_REPUTATION_ROLE_SEPARATION, ধাপ ৬] NotificationEntity.role ও
                // AdminAuditLogEntity.role — কোন role-এর ঘটনার জন্য নোটিফিকেশন/audit-log
                // এন্ট্রিটা, তা ট্যাগ করার জন্য। বিদ্যমান সব row-এর জন্য ডিফল্ট '' (role-
                // নিরপেক্ষ/legacy — পুরনো row দুই role-এই আগের মতো দেখাতে থাকবে, filter করে
                // বাদ পড়বে না)। শুধু নতুন কলাম যোগ, কোনো বিদ্যমান row মোছা/ওভাররাইট হয়নি
                // (rule #৪)।
                db.execSQL("ALTER TABLE notifications ADD COLUMN role TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE admin_audit_logs ADD COLUMN role TEXT NOT NULL DEFAULT ''")
            }
        }

        /**
         * [RPC_SYNC_FIX ট্র্যাক, ধাপ ৬] নতুন `pending_sync_outbox` টেবিল -- Outbox/retry ইনফ্রা,
         * দেখুন `docs/OUTBOX_RETRY_DESIGN.md` সেকশন ২ ও ৭। সম্পূর্ণ additive (`CREATE TABLE`
         * ছাড়া আর কিছু না) -- বিদ্যমান কোনো টেবিল/কলাম/row স্পর্শ করে না, তাই কোনো বিদ্যমান
         * ব্যবহারকারীর ডেটা হারানোর ঝুঁকি নেই।
         */
        val MIGRATION_52_53 = object : Migration(52, 53) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `pending_sync_outbox` (
                        `id` TEXT NOT NULL,
                        `rpcName` TEXT NOT NULL,
                        `paramsJson` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `retryCount` INTEGER NOT NULL DEFAULT 0,
                        `lastError` TEXT,
                        `lastAttemptAt` INTEGER,
                        `status` TEXT NOT NULL DEFAULT 'PENDING',
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
            }
        }

        /**
         * [Offline Action Gating ধাপ ৯] `messages` টেবিলে নতুন `sendStatus` কলাম (TEXT, ডিফল্ট
         * 'SENT') -- বিদ্যমান সব মেসেজ ইতিমধ্যে পাঠানো/synced ধরে নেওয়া নিরাপদ বলে ডিফল্ট 'SENT',
         * নতুন sendMessage() কলগুলোই শুধু explicitly 'PENDING' দিয়ে insert করবে। সম্পূর্ণ additive
         * (`ALTER TABLE ... ADD COLUMN` ছাড়া আর কিছু না) -- কোনো বিদ্যমান row/কলাম স্পর্শ করে না।
         */
        val MIGRATION_53_54 = object : Migration(53, 54) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE messages ADD COLUMN sendStatus TEXT NOT NULL DEFAULT 'SENT'")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "somadhan_database"
                )
                    .addMigrations(
                        MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16,
                        MIGRATION_16_17, MIGRATION_17_18, MIGRATION_18_19, MIGRATION_19_20,
                        MIGRATION_20_21, MIGRATION_21_22, MIGRATION_22_23, MIGRATION_23_24,
                        MIGRATION_24_25, MIGRATION_25_26, MIGRATION_26_27, MIGRATION_27_28,
                        MIGRATION_28_29, MIGRATION_29_30, MIGRATION_30_31, MIGRATION_31_32,
                        MIGRATION_32_33, MIGRATION_33_34, MIGRATION_34_35, MIGRATION_35_36,
                        MIGRATION_36_37, MIGRATION_37_38, MIGRATION_38_39, MIGRATION_39_40,
                        MIGRATION_40_41, MIGRATION_41_42, MIGRATION_42_43, MIGRATION_43_44, MIGRATION_44_45,
                        MIGRATION_45_46, MIGRATION_46_47, MIGRATION_47_48, MIGRATION_48_49,
                        MIGRATION_49_50, MIGRATION_50_51, MIGRATION_51_52, MIGRATION_52_53,
                        MIGRATION_53_54
                    )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            CoroutineScope(Dispatchers.IO).launch {
                                getDatabase(context).seedInitialData()
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    suspend fun seedInitialData() {
        val categories = listOf(
            CategoryEntity(
                id = "CAT_ELEC",
                nameBangla = "ইলেকট্রিশিয়ান ও ওয়্যারিং",
                nameEnglish = "Electrician & Wiring",
                isPhysical = true,
                iconName = "Bolt",
                keywords = "কারেন্ট,ফ্যান,সুইচ,ওয়্যারিং,শর্ট সার্কিট,বাল্ব,বিদ্যুৎ,wiring,switch,fan,circuit,ac,light,fuse,meter,voltage",
                minBudget = 200.0,
                maxBudget = 2000.0
            ),
            CategoryEntity(
                id = "CAT_PLUMB",
                nameBangla = "প্লাম্বিং ও পাইপফিটিং",
                nameEnglish = "Plumbing & Pipefitting",
                isPhysical = true,
                iconName = "Plumbing",
                keywords = "পানি,ট্যাপ,পাইপ,বেসিন,প্লাম্বার,লিক,লাইল,pipe,leak,tap,basin,commode,faucet,drainage,water",
                minBudget = 250.0,
                maxBudget = 2500.0
            ),
            CategoryEntity(
                id = "CAT_TECH",
                nameBangla = "কম্পিউটার ও আইটি সাপোর্ট",
                nameEnglish = "Computer & IT Support",
                isPhysical = false,
                iconName = "Computer",
                keywords = "উইন্ডোজ,ল্যাপটপ,কম্পিউটার,পিসি,সফটওয়্যার,ইন্সটল,প্রিন্টার,windows,laptop,computer,pc,software,install,slow,ram,ssd,display,printer",
                minBudget = 300.0,
                maxBudget = 3000.0
            ),
            CategoryEntity(
                id = "CAT_AC",
                nameBangla = "এসি ও রেফ্রিজারেটর সার্ভিসিং",
                nameEnglish = "AC & Refrigerator Servicing",
                isPhysical = true,
                iconName = "AcUnit",
                keywords = "এসি,ফ্রিজ,গ্যাস,ঠান্ডা,কুলিং,সার্ভিসিং,ac,fridge,cooling,gas,repair,inverter",
                minBudget = 500.0,
                maxBudget = 5000.0
            ),
            CategoryEntity(
                id = "CAT_HOME",
                nameBangla = "গৃহস্থালি মেরামত ও শিফটিং",
                nameEnglish = "Home Repair & Shifting",
                isPhysical = true,
                iconName = "HomeRepairService",
                keywords = "শিফটিং,রং,ফার্নিচার,কাঠমিস্ত্রি,মেরামত,carpenter,paint,furniture,drill,shifting,door,lock",
                minBudget = 400.0,
                maxBudget = 4000.0
            ),
            CategoryEntity(
                id = "CAT_GRAPHIC",
                nameBangla = "গ্রাফিক ডিজাইন ও ফটো এডিট",
                nameEnglish = "Graphic Design & Photo Edit",
                isPhysical = false,
                iconName = "DesignServices",
                keywords = "লোগো,ব্যানার,পোস্টার,ছবি,এডিটিং,ডিজাইন,logo,banner,poster,photo,photoshop,illustrator,canva,thumbnail",
                minBudget = 200.0,
                maxBudget = 2500.0
            ),
            CategoryEntity(
                id = "CAT_WEB",
                nameBangla = "ওয়েব ডেভেলপমেন্ট ও কোডিং",
                nameEnglish = "Web Development & Coding",
                isPhysical = false,
                iconName = "Code",
                keywords = "ওয়েবসাইট,কোডিং,প্রোগ্রামিং,ওয়েব,এইচটিএমএল,website,web,html,css,javascript,react,wordpress,backend,database,app",
                minBudget = 500.0,
                maxBudget = 10000.0
            ),
            CategoryEntity(
                id = "CAT_TUTOR",
                nameBangla = "হোম ও অনলাইন টিউশন",
                nameEnglish = "Home & Online Tuition",
                isPhysical = true,
                iconName = "School",
                keywords = "টিউশন,পড়ানো,টিচার,ইংরেজি,গণিত,টিউটর,tuition,teacher,math,english,science,physics,study",
                minBudget = 500.0,
                maxBudget = 6000.0
            )
        )
        categoryDao().insertCategories(categories)

        // Seed default platform settings
        platformSettingDao().insertSetting(PlatformSettingEntity(key = "commission_percent", value = "10.0"))
        platformSettingDao().insertSetting(PlatformSettingEntity(key = "min_withdrawal", value = "100.0"))
        platformSettingDao().insertSetting(PlatformSettingEntity(key = "urgency_levels", value = "সাধারণ,জরুরি,খুব জরুরি"))
        platformSettingDao().insertSetting(PlatformSettingEntity(key = "maintenance_mode", value = "false"))
        platformSettingDao().insertSetting(PlatformSettingEntity(key = "maintenance_message", value = "অ্যাপটি বর্তমানে রক্ষণাবেক্ষণের জন্য বন্ধ আছে। কিছুক্ষণ পর আবার চেষ্টা করুন।"))
        platformSettingDao().insertSetting(PlatformSettingEntity(key = "min_app_version", value = "1"))
        platformSettingDao().insertSetting(PlatformSettingEntity(key = "physical_category_radius_km", value = "10"))
        platformSettingDao().insertSetting(PlatformSettingEntity(key = "extra_amount_commission_enabled", value = "true"))
        platformSettingDao().insertSetting(PlatformSettingEntity(key = "extra_amount_commission_discount_percent", value = "50.0"))
        platformSettingDao().insertSetting(PlatformSettingEntity(key = "extra_amount_commission_percent", value = "50.0"))
        platformSettingDao().insertSetting(PlatformSettingEntity(key = "free_quota_enabled", value = "true"))
        platformSettingDao().insertSetting(PlatformSettingEntity(key = "free_quota_reputation_threshold", value = "80.0"))
        platformSettingDao().insertSetting(PlatformSettingEntity(key = "free_quota_job_count", value = "10"))
        platformSettingDao().insertSetting(PlatformSettingEntity(key = "extra_bill_reputation_cap_per_problem", value = "10.0"))
        platformSettingDao().insertSetting(PlatformSettingEntity(key = "extra_payment_miss_rule_enabled", value = "true"))
        platformSettingDao().insertSetting(PlatformSettingEntity(key = "extra_payment_miss_cycle_size", value = "10"))
        platformSettingDao().insertSetting(PlatformSettingEntity(key = "extra_payment_miss_threshold", value = "3"))
        platformSettingDao().insertSetting(PlatformSettingEntity(key = "extra_payment_miss_penalty", value = "5.0"))
        // Dynamic Reputation Engine Defaults
        platformSettingDao().insertSetting(PlatformSettingEntity(key = "rep_score_bid_won", value = "0.5"))
        platformSettingDao().insertSetting(PlatformSettingEntity(key = "rep_cap_daily_bid_won", value = "2.0"))
        platformSettingDao().insertSetting(PlatformSettingEntity(key = "rep_score_job_completed", value = "0.5"))
        platformSettingDao().insertSetting(PlatformSettingEntity(key = "rep_cap_daily_job_completed", value = "2.0"))
        platformSettingDao().insertSetting(PlatformSettingEntity(key = "rep_rate_withdrawal_per_100", value = "0.1"))
        platformSettingDao().insertSetting(PlatformSettingEntity(key = "rep_cap_daily_withdrawal", value = "2.0"))
        platformSettingDao().insertSetting(PlatformSettingEntity(key = "rep_score_problem_posted", value = "0.2"))
        platformSettingDao().insertSetting(PlatformSettingEntity(key = "rep_cap_daily_problem_posted", value = "2.0"))
        platformSettingDao().insertSetting(PlatformSettingEntity(key = "rep_score_rating_5_star", value = "1.5"))
        platformSettingDao().insertSetting(PlatformSettingEntity(key = "rep_score_rating_4_star", value = "0.5"))
        platformSettingDao().insertSetting(PlatformSettingEntity(key = "rep_cap_daily_rating", value = "2.0"))
        platformSettingDao().insertSetting(PlatformSettingEntity(key = "rep_score_kyc_verified", value = "5.0"))
        platformSettingDao().insertSetting(PlatformSettingEntity(key = "rep_penalty_rating_bad", value = "1.0"))
        platformSettingDao().insertSetting(PlatformSettingEntity(key = "rep_penalty_job_cancelled", value = "3.0"))
        platformSettingDao().insertSetting(PlatformSettingEntity(key = "rep_penalty_release_timeout", value = "10.0"))
        platformSettingDao().insertSetting(PlatformSettingEntity(key = "rep_penalty_inactive_7d", value = "2.0"))
        platformSettingDao().insertSetting(PlatformSettingEntity(key = "rep_penalty_inactive_30d", value = "5.0"))
        platformSettingDao().insertSetting(PlatformSettingEntity(key = "rep_penalty_restricted", value = "10.0"))
        platformSettingDao().insertSetting(PlatformSettingEntity(key = "rep_penalty_banned", value = "25.0"))
        // Instant Job Defaults
        platformSettingDao().insertSetting(PlatformSettingEntity(key = "instant_job_feature_enabled", value = "true"))
        platformSettingDao().insertSetting(PlatformSettingEntity(key = "instant_job_default_radius_km", value = "5.0"))
        platformSettingDao().insertSetting(PlatformSettingEntity(key = "instant_job_broadcast_timeout_seconds", value = "300"))
        platformSettingDao().insertSetting(PlatformSettingEntity(key = "instant_job_arrival_radius_meters", value = "200"))
        platformSettingDao().insertSetting(PlatformSettingEntity(key = "instant_job_max_active_per_solver", value = "1"))

        // [ধাপ ১৪ ফলো-আপ — ✅ সরানো হয়েছে] এখানে আগে demo user/solver (USER_DEMO_01,
        // SOLVER_DEMO_01) + তাদের sample problem/bid seed করা হতো "instant out-of-the-box
        // readiness"-এর জন্য। এগুলোর একমাত্র ব্যবহার ছিল এখন-অপসারিত demo quick-login বাটন
        // (LoginScreen.kt) দিয়ে ঢোকা -- normal login বহু আগে থেকেই real Supabase
        // phone+password দিয়ে হয়, তাই এই local Room seed data আর কোনো login path থেকে
        // reachable ছিল না। ব্যবহারকারীর স্পষ্ট সিদ্ধান্তে (rule #4 exception) সম্পূর্ণ
        // সরানো হয়েছে।

        // Seed initial FAQ data
        val defaultFaqs = listOf(
            // User FAQs
            FaqEntity(
                id = "FAQ_USER_01",
                question = "কীভাবে সমস্যা পোস্ট করব?",
                answer = "হোম স্ক্রিনের নিচে 'সমস্যা পোস্ট' বাটনে চাপ দিন। আপনার সমস্যার ক্যাটাগরি, বিস্তারিত বিবরণ, এলাকা/লোকেশন ও আনুমানিক বাজেট দিয়ে পোস্ট নিশ্চিত করুন।",
                targetAudience = "USER",
                displayOrder = 1,
                isActive = true
            ),
            FaqEntity(
                id = "FAQ_USER_02",
                question = "টাকা কখন সলভারকে দেওয়া হয়?",
                answer = "বিড গ্রহণের পর পেমেন্ট এসক্রোতে নিরাপদে জমা থাকে। কাজ সম্পূর্ণ সন্তোষজনকভাবে শেষ হওয়ার পর আপনার নিশ্চিতকরণ সাপেক্ষে সলভারের একাউন্টে টাকা পৌঁছায়।",
                targetAudience = "USER",
                displayOrder = 2,
                isActive = true
            ),
            FaqEntity(
                id = "FAQ_USER_03",
                question = "বিড কীভাবে অ্যাকসেপ্ট করব?",
                answer = "আপনার পোস্টের বিস্তারিত স্ক্রিনে গিয়ে সলভারদের দেওয়া অফার ও রেটিং দেখুন। পছন্দের বিডের পাশে 'অ্যাকসেপ্ট করুন' বোতামে চাপুন।",
                targetAudience = "USER",
                displayOrder = 3,
                isActive = true
            ),
            FaqEntity(
                id = "FAQ_USER_04",
                question = "রিফান্ড কীভাবে পাব?",
                answer = "সলভার কাজ না করলে বা কোনো অনাকাঙ্ক্ষিত জটিলতা তৈরি হলে সাপোর্ট সেন্টারে যোগাযোগ করে অথবা বিরোধ (Dispute) নিষ্পত্তির মাধ্যমে সরাসরি ওয়ালেটে রিফান্ড পেতে পারেন।",
                targetAudience = "USER",
                displayOrder = 4,
                isActive = true
            ),
            // Solver FAQs
            FaqEntity(
                id = "FAQ_SOLVER_01",
                question = "সলভার হিসেবে কীভাবে কাজে বিড করব?",
                answer = "হোম ফিডে উন্মুক্ত সমস্যাগুলো দেখুন। আপনার দক্ষতা অনুযায়ী কাজের বিস্তারিত পেজে গিয়ে প্রস্তাবিত মূল্য ও আনুমানিক সময় দিয়ে বিড সাবমিট করুন।",
                targetAudience = "SOLVER",
                displayOrder = 1,
                isActive = true
            ),
            FaqEntity(
                id = "FAQ_SOLVER_02",
                question = "কাজের পেমেন্ট কীভাবে উইথড্র করব?",
                answer = "কাজ সম্পন্ন হলে এবং গ্রাহক অনুমোদন দিলে কমিশন বাদে নিট অর্থ আপনার ওয়ালেটে জমা হবে। ওয়ালেট পেজ থেকে বিকাশ, নগদ বা রকেটের মাধ্যমে টাকা তোলার রিকোয়েস্ট পাঠাতে পারবেন।",
                targetAudience = "SOLVER",
                displayOrder = 2,
                isActive = true
            ),
            FaqEntity(
                id = "FAQ_SOLVER_03",
                question = "বিড করার পর কি বাতিল করা যায়?",
                answer = "গ্রাহক বিড গ্রহণ করার পূর্ব পর্যন্ত বিড প্রত্যাহার করা যাবে না। তবে গ্রাহক বিড গ্রহণ করে কাজ শুরু করার পর কোনো অনিবার্য কারণে সমস্যা হলে শর্তসাপেক্ষে কাজ বাতিলের ব্যবস্থা রয়েছে।",
                targetAudience = "SOLVER",
                displayOrder = 3,
                isActive = true
            ),
            FaqEntity(
                id = "FAQ_SOLVER_04",
                question = "কমিশন কীভাবে কাটা হয়?",
                answer = "প্ল্যাটফর্মের নির্ধারিত ফি কাজের মোট বাজেট থেকে স্বয়ংক্রিয়ভাবে কর্তন করে অবশিষ্ট সম্পূর্ণ টাকা সলভারের ওয়ালেটে জমা হয়।",
                targetAudience = "SOLVER",
                displayOrder = 4,
                isActive = true
            )
        )
        faqDao().insertFaqs(defaultFaqs)
    }
}
