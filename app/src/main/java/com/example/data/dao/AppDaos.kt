package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entity.AdditionalChargeEntity
import com.example.data.entity.AdminAuditLogEntity
import com.example.data.entity.BidEntity
import com.example.data.entity.CategoryEntity
import com.example.data.entity.EscrowEntity
import com.example.data.entity.FaqEntity
import com.example.data.entity.GatewayPaymentEntity
import com.example.data.entity.MessageEntity
import com.example.data.entity.NotificationEntity
import com.example.data.entity.PlatformSettingEntity
import com.example.data.entity.ProblemEntity
import com.example.data.entity.RatingEntity
import com.example.data.entity.ReputationEventEntity
import com.example.data.entity.TransactionEntity
import com.example.data.entity.UserEntity
import com.example.data.entity.WithdrawalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE phone = :phone LIMIT 1")
    suspend fun getUserByPhone(phone: String): UserEntity?

    @Query("SELECT * FROM users WHERE displayUid = :displayUid LIMIT 1")
    suspend fun getUserByDisplayUid(displayUid: String): UserEntity?

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users WHERE phone = :phone OR email = :email LIMIT 1")
    suspend fun getUserByPhoneOrEmail(phone: String, email: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    fun getUserByIdFlow(id: String): Flow<UserEntity?>

    @Query("SELECT * FROM users ORDER BY createdAt DESC")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users ORDER BY createdAt DESC LIMIT :limit OFFSET :offset")
    suspend fun getAllUsersPage(limit: Int, offset: Int): List<UserEntity>

    @Query("SELECT * FROM users WHERE (linkedAccountId = :linkedId OR id = :linkedId) AND role = :role LIMIT 1")
    suspend fun getLinkedUserByRole(linkedId: String, role: String): UserEntity?

    @Query("SELECT * FROM users WHERE (phone = :phone OR email = :email) AND role = :role LIMIT 1")
    suspend fun getUserByContactAndRole(phone: String, email: String, role: String): UserEntity?

    @Query("SELECT * FROM users WHERE linkedAccountId = :linkedId OR id = :linkedId")
    suspend fun getLinkedAccounts(linkedId: String): List<UserEntity>

    @Query("SELECT * FROM users WHERE role = :role")
    suspend fun getUsersByRole(role: String): List<UserEntity>

    @Query("SELECT * FROM users WHERE role = :role ORDER BY createdAt DESC LIMIT :limit OFFSET :offset")
    suspend fun getUsersByRolePage(role: String, limit: Int, offset: Int): List<UserEntity>

    @Query("SELECT * FROM users")
    suspend fun getAllUsersList(): List<UserEntity>

    @Query("SELECT * FROM users")
    suspend fun getAllUsersOnce(): List<UserEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    // Batch version used by the Supabase sync path's full pulls (formerly FirebaseSyncManager's,
    // before that class was deleted in the migration). Room's Flow queries (e.g.
    // getAllUsers()) re-run and emit after EVERY write, so inserting a large pulled batch one
    // row at a time made any UI observing that Flow (admin user list/count, etc.) visibly
    // grow 1-by-1 as the sync progressed instead of appearing all at once. Inserting the whole
    // batch in a single call means Room only invalidates/emits once.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<UserEntity>)

    @Update
    suspend fun updateUser(user: UserEntity)

    // Used by the Supabase sync path (formerly FirebaseSyncManager, now deleted) to pull down
    // role changes made remotely (e.g. from the Supabase dashboard) into the local Room
    // database, so that in-app admin access (which is gated on the local `role` field) stays
    // in sync with the cloud.
    @Query("UPDATE users SET role = :role WHERE id = :id")
    suspend fun updateUserRole(id: String, role: String)

    @Query("UPDATE users SET balance = balance + :amount, updatedAt = :timestamp WHERE id = :userId")
    suspend fun addBalance(userId: String, amount: Double, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE users SET balance = balance - :amount, updatedAt = :timestamp WHERE id = :userId")
    suspend fun deductBalance(userId: String, amount: Double, timestamp: Long = System.currentTimeMillis())

    // [ব্যালেন্স ফিক্স — ধাপ ১৪.৫ role-scoped mirror সিঙ্ক] আগের addBalance/deductBalance
    // শুধু legacy plain `balance` কলাম আপডেট করত — `balanceUser`/`balanceSolver` (role-scoped
    // মিরর, যেটা role switch করার সময় UI-এর জন্য ব্যবহার হয়) কখনো আপডেট হতো না, ফলে সেগুলো
    // স্টেল/পুরনো থেকে যেত আর role switch করলে ভুল/পুরনো সংখ্যা দেখাত। নিচের চারটা ফাংশন
    // সবসময় সঠিক role-scoped কলামটা আপডেট করে, ঠিক যেভাবে Supabase RPC-গুলো (accept_bid,
    // release_escrow ইত্যাদি) সার্ভার-সাইডে ইতিমধ্যেই করে — যাতে local mirror সবসময়
    // cloud-এর সাথে সামঞ্জস্যপূর্ণ থাকে।
    //
    // [ব্যালেন্স ফিক্স — cross-role mix বাগ, পরের সেশনে ধরা পড়েছে] শেয়ার্ড `balance`
    // কলামটা "এই মুহূর্তে ডিভাইসে যে role active (`role` কলাম) তার ব্যালেন্স" বোঝানোর কথা
    // (দেখুন UserEntity.activeRoleBalance)। কিন্তু আগে এই ফাংশনগুলো `role` কলাম কী আছে তা না
    // দেখেই সবসময় শেয়ার্ড `balance`-ও আপডেট করত। ফলে: ইউজার এখন User মোডে আছে, ঠিক তখনই
    // তার Solver জবের escrow release হলে (admin/৪৮-ঘণ্টা auto-release) — addBalanceForSolverRole
    // ঠিকভাবে balanceSolver বাড়াত, কিন্তু শেয়ার্ড balance-ও একই সাথে বেড়ে যেত, ফলে UI-তে
    // সাময়িকভাবে দুই role-এর টাকা মিশে দেখাত (পরের role switch-এ switchRoleInPlace() ঠিক করে
    // দিত, কিন্তু ততক্ষণ ভুল দেখাত — কোনো টাকার ক্ষতি হতো না, কারণ withdraw/spend এলিজিবিলিটি
    // সবসময় role-scoped কলাম থেকেই চেক হয়, নিচের setBannedStatusForUserRole/SolverRole
    // ফাংশনগুলোর মতোই — ওগুলো ধাপ ৮ থেকে শেয়ার্ড কলাম একেবারেই টাচ করে না)। ফিক্স: এখন CASE
    // দিয়ে শেয়ার্ড balance তখনই আপডেট হয় যখন row-টার `role` আসলে সেই role-এর সাথে ম্যাচ করে —
    // অন্য role-এর জন্য শেয়ার্ড কলাম অপরিবর্তিত থাকে, তাই মিক্সিং উইন্ডোটাই আর তৈরি হয় না।
    @Query(
        "UPDATE users SET " +
        "balanceUser = balanceUser + :amount, " +
        "balance = CASE WHEN role = 'USER' THEN balance + :amount ELSE balance END, " +
        "updatedAt = :timestamp WHERE id = :userId"
    )
    suspend fun addBalanceForUserRole(userId: String, amount: Double, timestamp: Long = System.currentTimeMillis())

    @Query(
        "UPDATE users SET " +
        "balanceUser = balanceUser - :amount, " +
        "balance = CASE WHEN role = 'USER' THEN balance - :amount ELSE balance END, " +
        "updatedAt = :timestamp WHERE id = :userId"
    )
    suspend fun deductBalanceForUserRole(userId: String, amount: Double, timestamp: Long = System.currentTimeMillis())

    @Query(
        "UPDATE users SET " +
        "balanceSolver = balanceSolver + :amount, " +
        "balance = CASE WHEN role = 'SOLVER' THEN balance + :amount ELSE balance END, " +
        "updatedAt = :timestamp WHERE id = :userId"
    )
    suspend fun addBalanceForSolverRole(userId: String, amount: Double, timestamp: Long = System.currentTimeMillis())

    @Query(
        "UPDATE users SET " +
        "balanceSolver = balanceSolver - :amount, " +
        "balance = CASE WHEN role = 'SOLVER' THEN balance - :amount ELSE balance END, " +
        "updatedAt = :timestamp WHERE id = :userId"
    )
    suspend fun deductBalanceForSolverRole(userId: String, amount: Double, timestamp: Long = System.currentTimeMillis())

    // [BALANCE_REPUTATION_ROLE_SEPARATION - ধাপ ১] balanceUser/balanceSolver-এর role-scoped
    // ফাংশনগুলোর (উপরে) ঠিক একই প্যাটার্নে — reputationScoreUser/reputationScoreSolver-এর জন্য।
    // পার্থক্য: balance ফাংশনগুলো একটা ডেল্টা amount +/- করে, কিন্তু এখানে caller
    // (SomadhanRepository.applyReputationChange()) আগে থেকেই clamp করা (0.0-100.0) একটা
    // absolute নতুন স্কোর (`newScore`) পাঠায় — তাই এখানে +/- না করে সরাসরি SET করা হচ্ছে।
    // শেয়ার্ড plain `reputationScore` কলামও একই cross-role-mix-বিরোধী CASE গার্ড দিয়ে আপডেট
    // হয় (শুধু তখনই, যখন row-টার `role` আসলে এই role-এর সাথে ম্যাচ করে) — addBalanceForUserRole/
    // SolverRole-এর মতোই, অন্য role-এর জন্য শেয়ার্ড কলাম অপরিবর্তিত থাকে।
    @Query(
        "UPDATE users SET " +
        "reputationScoreUser = :newScore, " +
        "reputationScore = CASE WHEN role = 'USER' THEN :newScore ELSE reputationScore END, " +
        "updatedAt = :timestamp WHERE id = :userId"
    )
    suspend fun updateReputationForUserRole(userId: String, newScore: Double, timestamp: Long = System.currentTimeMillis())

    @Query(
        "UPDATE users SET " +
        "reputationScoreSolver = :newScore, " +
        "reputationScore = CASE WHEN role = 'SOLVER' THEN :newScore ELSE reputationScore END, " +
        "updatedAt = :timestamp WHERE id = :userId"
    )
    suspend fun updateReputationForSolverRole(userId: String, newScore: Double, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE users SET isBanned = :banned, updatedAt = :timestamp WHERE id = :userId")
    suspend fun setBannedStatus(userId: String, banned: Boolean, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE users SET isRestricted = :restricted, updatedAt = :timestamp WHERE id = :userId")
    suspend fun setRestrictedStatus(userId: String, restricted: Boolean, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE users SET isVerifiedBadge = :verified WHERE id = :userId")
    suspend fun setVerifiedBadge(userId: String, verified: Boolean)

    // [MONEY_FLOW_AND_ADMIN_BUGS ধাপ ৮ — বাগ D২/D৩] role-scoped ভার্সন — উপরের তিনটা legacy
    // ফাংশন শুধু shared কলাম ছোঁয়; dual-role ইউজারের USER role ব্যান করলে যেন SOLVER role
    // (বা উল্টো) কোনোভাবেই প্রভাবিত না হয়, তাই নিচের ছয়টা ফাংশন শুধু নিজ নিজ role-scoped
    // কলাম আপডেট করে, শেয়ার্ড কলাম টাচ করে না।
    @Query("UPDATE users SET isBannedUser = :banned, updatedAt = :timestamp WHERE id = :userId")
    suspend fun setBannedStatusForUserRole(userId: String, banned: Boolean, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE users SET isBannedSolver = :banned, updatedAt = :timestamp WHERE id = :userId")
    suspend fun setBannedStatusForSolverRole(userId: String, banned: Boolean, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE users SET isRestrictedUser = :restricted, updatedAt = :timestamp WHERE id = :userId")
    suspend fun setRestrictedStatusForUserRole(userId: String, restricted: Boolean, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE users SET isRestrictedSolver = :restricted, updatedAt = :timestamp WHERE id = :userId")
    suspend fun setRestrictedStatusForSolverRole(userId: String, restricted: Boolean, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE users SET verifiedBadgeUser = :verified, updatedAt = :timestamp WHERE id = :userId")
    suspend fun setVerifiedBadgeForUserRole(userId: String, verified: Boolean, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE users SET verifiedBadgeSolver = :verified, updatedAt = :timestamp WHERE id = :userId")
    suspend fun setVerifiedBadgeForSolverRole(userId: String, verified: Boolean, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE users SET password = :hashedPassword, updatedAt = :timestamp WHERE id = :userId")
    suspend fun resetPassword(userId: String, hashedPassword: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE users SET isKycVerified = 0, kycStatus = 'rejected', kycRejectReason = :reason, updatedAt = :timestamp WHERE id = :userId")
    suspend fun revokeKyc(userId: String, reason: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE users SET kycDocumentNumber = :docNumber, kycFirstName = :firstName, kycLastName = :lastName, kycAddress = :address WHERE id = :userId")
    suspend fun updateKycInfo(userId: String, docNumber: String, firstName: String, lastName: String, address: String)

    @Query("UPDATE users SET kycStatus = 'pending', kycRejectReason = '', updatedAt = :timestamp WHERE id = :userId")
    suspend fun resetKycToPending(userId: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE users SET favoriteSolverIds = :favoriteSolverIds, updatedAt = :timestamp WHERE id = :userId")
    suspend fun updateFavoriteSolverIds(userId: String, favoriteSolverIds: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE users SET instantJobNotificationsEnabled = :enabled WHERE id = :userId")
    suspend fun updateInstantJobToggle(userId: String, enabled: Boolean)

    @Query("DELETE FROM users WHERE id = :userId")
    suspend fun deleteUser(userId: String)
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE id = :id LIMIT 1")
    suspend fun getCategoryById(id: String): CategoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<CategoryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity)

    @Update
    suspend fun updateCategory(category: CategoryEntity)

    @Query("UPDATE categories SET isActive = :isActive WHERE id = :id")
    suspend fun setCategoryActive(id: String, isActive: Boolean)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteCategory(id: String)
}

@Dao
interface ProblemDao {
    @Query("SELECT * FROM problems ORDER BY createdAt DESC")
    fun getAllProblems(): Flow<List<ProblemEntity>>

    @Query("SELECT * FROM problems ORDER BY createdAt DESC LIMIT :limit OFFSET :offset")
    suspend fun getAllProblemsPage(limit: Int, offset: Int): List<ProblemEntity>

    @Query("SELECT * FROM problems")
    suspend fun getAllProblemsList(): List<ProblemEntity>

    @Query("SELECT * FROM problems WHERE status = 'OPEN' AND isDirectContract = 0 AND isPublic = 1 AND isUserDeleted = 0 ORDER BY createdAt DESC")
    fun getOpenProblems(): Flow<List<ProblemEntity>>

    @Query("SELECT * FROM problems WHERE status = 'OPEN' AND isDirectContract = 0 AND isPublic = 1 AND isUserDeleted = 0 ORDER BY createdAt DESC LIMIT :limit OFFSET :offset")
    suspend fun getOpenProblemsPage(limit: Int, offset: Int): List<ProblemEntity>

    @Query("SELECT * FROM problems WHERE status = 'OPEN' AND isDirectContract = 0 AND isPublic = 1 AND isUserDeleted = 0 AND categoryId = :categoryId ORDER BY createdAt DESC LIMIT :limit OFFSET :offset")
    suspend fun getOpenProblemsByCategoryPage(categoryId: String, limit: Int, offset: Int): List<ProblemEntity>

    @Query("SELECT * FROM problems WHERE status = 'COMPLETED' AND isDirectContract = 0 AND isPublic = 1 ORDER BY completedAt DESC")
    fun getCompletedProblems(): Flow<List<ProblemEntity>>

    @Query("SELECT * FROM problems WHERE status = 'COMPLETED' AND isDirectContract = 0 AND isPublic = 1 ORDER BY completedAt DESC LIMIT :limit OFFSET :offset")
    suspend fun getCompletedProblemsPage(limit: Int, offset: Int): List<ProblemEntity>

    @Query("SELECT * FROM problems WHERE isDirectContract = 1 AND isUserDeleted = 0 ORDER BY createdAt DESC")
    fun getAllDirectContracts(): Flow<List<ProblemEntity>>

    @Query("SELECT * FROM problems WHERE isDirectContract = 1 AND isUserDeleted = 0 ORDER BY createdAt DESC LIMIT :limit OFFSET :offset")
    suspend fun getAllDirectContractsPage(limit: Int, offset: Int): List<ProblemEntity>

    @Query("SELECT * FROM problems WHERE userId = :userId AND status = 'OPEN' AND isUserDeleted = 0 ORDER BY createdAt DESC")
    fun getActiveProblemsByUserId(userId: String): Flow<List<ProblemEntity>>

    @Query("SELECT * FROM problems WHERE userId = :userId AND status = 'OPEN' AND isUserDeleted = 0 ORDER BY createdAt DESC LIMIT :limit OFFSET :offset")
    suspend fun getActiveProblemsByUserIdPage(userId: String, limit: Int, offset: Int): List<ProblemEntity>

    @Query("SELECT * FROM problems WHERE userId = :userId AND status = 'COMPLETED' ORDER BY completedAt DESC")
    fun getCompletedProblemsByUserId(userId: String): Flow<List<ProblemEntity>>

    @Query("SELECT * FROM problems WHERE userId = :userId AND status = 'COMPLETED' ORDER BY completedAt DESC LIMIT :limit OFFSET :offset")
    suspend fun getCompletedProblemsByUserIdPage(userId: String, limit: Int, offset: Int): List<ProblemEntity>

    @Query("SELECT * FROM problems WHERE userId = :userId AND isUserDeleted = 0 ORDER BY createdAt DESC")
    fun getProblemsByUserId(userId: String): Flow<List<ProblemEntity>>

    @Query("SELECT * FROM problems WHERE userId = :userId AND isUserDeleted = 0 ORDER BY createdAt DESC LIMIT :limit OFFSET :offset")
    suspend fun getProblemsByUserIdPage(userId: String, limit: Int, offset: Int): List<ProblemEntity>

    @Query("SELECT * FROM problems WHERE acceptedSolverId = :solverId AND status = 'COMPLETED' ORDER BY completedAt DESC")
    fun getCompletedProblemsBySolver(solverId: String): Flow<List<ProblemEntity>>

    @Query("SELECT * FROM problems WHERE acceptedSolverId = :solverId AND status = 'COMPLETED' ORDER BY completedAt DESC LIMIT :limit OFFSET :offset")
    suspend fun getCompletedProblemsBySolverPage(solverId: String, limit: Int, offset: Int): List<ProblemEntity>

    @Query("SELECT * FROM problems WHERE acceptedSolverId = :solverId ORDER BY createdAt DESC")
    fun getProblemsBySolver(solverId: String): Flow<List<ProblemEntity>>

    @Query("SELECT * FROM problems WHERE acceptedSolverId = :solverId ORDER BY createdAt DESC LIMIT :limit OFFSET :offset")
    suspend fun getProblemsBySolverPage(solverId: String, limit: Int, offset: Int): List<ProblemEntity>

    @Query("SELECT * FROM problems WHERE id = :id LIMIT 1")
    suspend fun getProblemById(id: String): ProblemEntity?

    @Query("SELECT * FROM problems WHERE id = :id LIMIT 1")
    fun getProblemByIdFlow(id: String): Flow<ProblemEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProblem(problem: ProblemEntity)

    // Batch version -- see insertUsers() above for why this matters (this is the one behind the
    // "সম্পন্ন হওয়া সমস্যা" / completed-problems count visibly counting up 1,2,3... on load).
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProblems(problems: List<ProblemEntity>)

    @Update
    suspend fun updateProblem(problem: ProblemEntity)

    @Query("UPDATE problems SET status = :status, lastActivityAt = :timestamp WHERE id = :problemId")
    suspend fun adminUpdateStatus(problemId: String, status: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE problems SET minBudget = :minBudget, maxBudget = :maxBudget, lastActivityAt = :timestamp WHERE id = :problemId")
    suspend fun adminUpdateBudget(problemId: String, minBudget: Double, maxBudget: Double, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE problems SET acceptedSolverId = :solverId, acceptedSolverName = :solverName, lastActivityAt = :timestamp WHERE id = :problemId")
    suspend fun adminReassignSolver(problemId: String, solverId: String, solverName: String, timestamp: Long = System.currentTimeMillis())

    @Query("SELECT * FROM problems WHERE userId = :userId AND acceptedSolverId = :solverId AND status = 'COMPLETED'")
    suspend fun getCompletedProblemsBetween(userId: String, solverId: String): List<ProblemEntity>

    @Query("UPDATE problems SET solverLastSeenAt = :timestamp WHERE id = :problemId")
    suspend fun updateSolverLastSeenAt(problemId: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE problems SET userLastSeenAt = :timestamp WHERE id = :problemId")
    suspend fun updateUserLastSeenAt(problemId: String, timestamp: Long = System.currentTimeMillis())

    @Query("SELECT * FROM problems WHERE isInstantJob = 1 AND jobStatus = 'BROADCASTING' AND categoryId = :categoryId")
    fun getBroadcastingJobsByCategory(categoryId: String): Flow<List<ProblemEntity>>

    @Query("SELECT * FROM problems WHERE isInstantJob = 1 AND jobStatus IS NOT NULL AND jobStatus != 'BROADCASTING' AND jobStatus != 'JOB_COMPLETED' AND jobStatus != 'CANCELLED' AND acceptedSolverId = :solverId")
    fun getActiveInstantJobForSolver(solverId: String): Flow<List<ProblemEntity>>

    @Query("SELECT * FROM problems WHERE isInstantJob = 1 AND userId = :userId AND jobStatus IS NOT NULL AND jobStatus != 'JOB_COMPLETED' AND jobStatus != 'COMPLETED' AND jobStatus != 'CANCELLED' AND status != 'COMPLETED' AND status != 'CANCELLED'")
    fun getActiveInstantJobForUser(userId: String): Flow<List<ProblemEntity>>

    @Query("SELECT * FROM problems WHERE isInstantJob = 1 AND userId = :userId AND jobStatus IS NOT NULL AND jobStatus != 'JOB_COMPLETED' AND jobStatus != 'COMPLETED' AND jobStatus != 'CANCELLED' AND status != 'COMPLETED' AND status != 'CANCELLED' LIMIT 1")
    suspend fun getActiveInstantJobForUserSync(userId: String): ProblemEntity?

    @Query("UPDATE problems SET solverLiveLat = :lat, solverLiveLng = :lng, solverLiveUpdatedAt = :now, lastActivityAt = :now WHERE id = :problemId")
    suspend fun updateSolverLiveLocation(problemId: String, lat: Double, lng: Double, now: Long = System.currentTimeMillis())

    @Query("DELETE FROM problems WHERE id = :id")
    suspend fun deleteProblem(id: String)
}

@Dao
interface BidDao {
    @Query("SELECT * FROM bids WHERE problemId = :problemId ORDER BY createdAt DESC")
    fun getBidsForProblem(problemId: String): Flow<List<BidEntity>>

    @Query("SELECT * FROM bids WHERE problemId = :problemId ORDER BY createdAt DESC LIMIT :limit OFFSET :offset")
    suspend fun getBidsForProblemPage(problemId: String, limit: Int, offset: Int): List<BidEntity>

    @Query("SELECT * FROM bids WHERE problemId = :problemId AND status != 'CANCELLED' ORDER BY createdAt DESC")
    fun getActiveBidsForProblem(problemId: String): Flow<List<BidEntity>>

    @Query("SELECT * FROM bids WHERE problemId = :problemId AND status != 'CANCELLED' ORDER BY createdAt DESC LIMIT :limit OFFSET :offset")
    suspend fun getActiveBidsForProblemPage(problemId: String, limit: Int, offset: Int): List<BidEntity>

    @Query("SELECT COUNT(*) FROM bids WHERE problemId = :problemId AND solverId = :solverId AND status != 'CANCELLED'")
    suspend fun countSolverBidsForProblem(problemId: String, solverId: String): Int

    @Query("SELECT * FROM bids WHERE solverId = :solverId ORDER BY createdAt DESC")
    fun getBidsBySolver(solverId: String): Flow<List<BidEntity>>

    @Query("SELECT * FROM bids WHERE solverId = :solverId ORDER BY createdAt DESC LIMIT :limit OFFSET :offset")
    suspend fun getBidsBySolverPage(solverId: String, limit: Int, offset: Int): List<BidEntity>

    @Query("SELECT * FROM bids WHERE solverId = :solverId ORDER BY createdAt DESC")
    fun getBidsForSolver(solverId: String): Flow<List<BidEntity>>

    @Query("SELECT * FROM bids WHERE solverId = :solverId ORDER BY createdAt DESC LIMIT :limit OFFSET :offset")
    suspend fun getBidsForSolverPage(solverId: String, limit: Int, offset: Int): List<BidEntity>

    // Used by scoped Supabase sync (pullProblems) to find every problemId this solver has ever
    // bid on locally, so those specific problems can be fetched even if they fall outside the
    // recent-OPEN sync window (see ENGINEERING_NOTES.md §2).
    @Query("SELECT DISTINCT problemId FROM bids WHERE solverId = :solverId")
    suspend fun getDistinctProblemIdsForSolver(solverId: String): List<String>

    @Query("SELECT * FROM bids ORDER BY createdAt DESC")
    fun getAllBids(): Flow<List<BidEntity>>

    @Query("SELECT * FROM bids ORDER BY createdAt DESC LIMIT :limit OFFSET :offset")
    suspend fun getAllBidsPage(limit: Int, offset: Int): List<BidEntity>

    @Query("SELECT * FROM bids WHERE id = :id LIMIT 1")
    suspend fun getBidById(id: String): BidEntity?

    @Query("SELECT * FROM bids WHERE problemId = :problemId")
    suspend fun getBidsForProblemSync(problemId: String): List<BidEntity>

    @Query("SELECT * FROM bids WHERE status = 'CANCELLED' ORDER BY createdAt DESC")
    fun getAllCancelledBids(): Flow<List<BidEntity>>

    @Query("SELECT * FROM bids WHERE status = 'CANCELLED' ORDER BY createdAt DESC LIMIT :limit OFFSET :offset")
    suspend fun getAllCancelledBidsPage(limit: Int, offset: Int): List<BidEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBid(bid: BidEntity)

    // Batch version -- see UserDao.insertUsers() above for why this matters.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBids(bids: List<BidEntity>)

    @Update
    suspend fun updateBid(bid: BidEntity)

    @Query("UPDATE bids SET status = 'REJECTED' WHERE problemId = :problemId AND id != :acceptedBidId")
    suspend fun rejectOtherBids(problemId: String, acceptedBidId: String)

    @Query("UPDATE bids SET status = 'REJECTED' WHERE id = :bidId")
    suspend fun adminRejectBid(bidId: String)

    @Query("DELETE FROM bids WHERE id = :id")
    suspend fun deleteBid(id: String)
}

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE problemId = :problemId ORDER BY timestamp ASC")
    fun getMessagesForProblem(problemId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE problemId = :problemId ORDER BY timestamp DESC LIMIT :limit OFFSET :offset")
    suspend fun getMessagesForProblemPage(problemId: String, limit: Int, offset: Int): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE senderId = :userId OR receiverId = :userId ORDER BY timestamp DESC")
    fun getAllUserMessages(userId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE senderId = :userId OR receiverId = :userId ORDER BY timestamp DESC LIMIT :limit OFFSET :offset")
    suspend fun getAllUserMessagesPage(userId: String, limit: Int, offset: Int): List<MessageEntity>

    @Query("SELECT * FROM messages ORDER BY timestamp DESC")
    fun getAllMessagesFlow(): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages ORDER BY timestamp DESC LIMIT :limit OFFSET :offset")
    suspend fun getAllMessagesFlowPage(limit: Int, offset: Int): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE id = :id LIMIT 1")
    suspend fun getMessageById(id: String): MessageEntity?

    @Query("SELECT COUNT(*) FROM messages WHERE receiverId = :userId AND isRead = 0")
    fun getUnreadMessagesCountForUser(userId: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM messages WHERE problemId = :problemId AND receiverId = :userId AND isRead = 0")
    fun getUnreadMessagesCountForProblem(problemId: String, userId: String): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<MessageEntity>)

    @Query("UPDATE messages SET isRead = 1 WHERE problemId = :problemId AND receiverId = :userId")
    suspend fun markMessagesAsReadForProblem(problemId: String, userId: String)

    @Query("DELETE FROM messages WHERE id = :messageId")
    suspend fun deleteMessage(messageId: String)

    @Query("DELETE FROM messages WHERE problemId = :problemId")
    suspend fun deleteMessagesForProblem(problemId: String)

    // [Offline Action Gating ধাপ ৯] messenger-স্টাইল send-status ট্র্যাকিং -- sendMessage() insert
    // করার পর dual-write সফল/ব্যর্থ অনুযায়ী "SENT"/"FAILED"-এ আপডেট করে, আর ChatScreen-এর ইনলাইন
    // retry বাটন retrySendMessage() দিয়ে আবার "SENT"-এ আপডেট করার চেষ্টা করে।
    @Query("UPDATE messages SET sendStatus = :status WHERE id = :messageId")
    suspend fun updateMessageSendStatus(messageId: String, status: String)

    // অফলাইন→অনলাইন reconnect-এ auto-retry-এর জন্য -- শুধু এই ইউজারের নিজের পাঠানো ব্যর্থ
    // মেসেজগুলো (senderId স্কোপড, কারণ messages_insert RLS-ও শুধু sender নিজেকেই অনুমতি দেয়)।
    @Query("SELECT * FROM messages WHERE senderId = :userId AND sendStatus = 'FAILED' ORDER BY timestamp ASC")
    suspend fun getFailedMessagesForSender(userId: String): List<MessageEntity>
}

@Dao
interface RatingDao {
    @Query("SELECT * FROM ratings WHERE solverId = :solverId ORDER BY createdAt DESC")
    fun getRatingsForSolver(solverId: String): Flow<List<RatingEntity>>

    @Query("SELECT * FROM ratings WHERE solverId = :solverId ORDER BY createdAt DESC LIMIT :limit OFFSET :offset")
    suspend fun getRatingsForSolverPage(solverId: String, limit: Int, offset: Int): List<RatingEntity>

    @Query("SELECT * FROM ratings WHERE userId = :userId ORDER BY createdAt DESC")
    fun getRatingsByUserId(userId: String): Flow<List<RatingEntity>>

    @Query("SELECT * FROM ratings WHERE userId = :userId ORDER BY createdAt DESC LIMIT :limit OFFSET :offset")
    suspend fun getRatingsByUserIdPage(userId: String, limit: Int, offset: Int): List<RatingEntity>

    @Query("SELECT * FROM ratings WHERE solverId = :solverId AND raterRole = 'USER' ORDER BY createdAt DESC")
    fun getRatingsReceivedBySolver(solverId: String): Flow<List<RatingEntity>>

    @Query("SELECT * FROM ratings WHERE solverId = :solverId AND raterRole = 'USER' ORDER BY createdAt DESC LIMIT :limit OFFSET :offset")
    suspend fun getRatingsReceivedBySolverPage(solverId: String, limit: Int, offset: Int): List<RatingEntity>

    @Query("SELECT * FROM ratings WHERE userId = :userId AND raterRole = 'SOLVER' ORDER BY createdAt DESC")
    fun getRatingsReceivedByUser(userId: String): Flow<List<RatingEntity>>

    @Query("SELECT * FROM ratings WHERE userId = :userId AND raterRole = 'SOLVER' ORDER BY createdAt DESC LIMIT :limit OFFSET :offset")
    suspend fun getRatingsReceivedByUserPage(userId: String, limit: Int, offset: Int): List<RatingEntity>

    @Query("SELECT * FROM ratings WHERE userId = :userId AND raterRole = 'USER' ORDER BY createdAt DESC")
    fun getRatingsGivenByUser(userId: String): Flow<List<RatingEntity>>

    @Query("SELECT * FROM ratings WHERE userId = :userId AND raterRole = 'USER' ORDER BY createdAt DESC LIMIT :limit OFFSET :offset")
    suspend fun getRatingsGivenByUserPage(userId: String, limit: Int, offset: Int): List<RatingEntity>

    @Query("SELECT * FROM ratings WHERE solverId = :solverId AND raterRole = 'SOLVER' ORDER BY createdAt DESC")
    fun getRatingsGivenBySolver(solverId: String): Flow<List<RatingEntity>>

    @Query("SELECT * FROM ratings WHERE solverId = :solverId AND raterRole = 'SOLVER' ORDER BY createdAt DESC LIMIT :limit OFFSET :offset")
    suspend fun getRatingsGivenBySolverPage(solverId: String, limit: Int, offset: Int): List<RatingEntity>

    @Query("SELECT * FROM ratings WHERE (solverId = :personId AND raterRole = 'USER') OR (userId = :personId AND raterRole = 'SOLVER') ORDER BY createdAt DESC")
    fun getAllRatingsReceivedByPerson(personId: String): Flow<List<RatingEntity>>

    @Query("SELECT * FROM ratings WHERE (solverId = :personId AND raterRole = 'USER') OR (userId = :personId AND raterRole = 'SOLVER') ORDER BY createdAt DESC LIMIT :limit OFFSET :offset")
    suspend fun getAllRatingsReceivedByPersonPage(personId: String, limit: Int, offset: Int): List<RatingEntity>

    @Query("SELECT * FROM ratings ORDER BY createdAt DESC")
    fun getAllRatings(): Flow<List<RatingEntity>>

    @Query("SELECT * FROM ratings ORDER BY createdAt DESC LIMIT :limit OFFSET :offset")
    suspend fun getAllRatingsPage(limit: Int, offset: Int): List<RatingEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRating(rating: RatingEntity)

    @Query("DELETE FROM ratings WHERE id = :ratingId")
    suspend fun deleteRating(ratingId: String)
}

@Dao
interface NotificationDao {
    // [ROLE_SEPARATION ধাপ ৬, অংশ ক] role-scoped: বর্তমান active role (:currentRole) না মিললে,
    // এবং role blank/neutral না হলে (পুরনো row বা role-নিরপেক্ষ ঘটনা) দেখাবে না। পুরনো caller
    // (currentRole ডিফল্ট "" পাস করলে) সব role-এর notification দেখাবে -- কল-সাইট বদলে বর্তমান
    // role পাস করা এই ধাপেরই কাজ, নিচের caller-গুলো দেখুন।
    @Query("SELECT * FROM notifications WHERE userId = :userId AND (:currentRole = '' OR role = :currentRole OR role = '') AND (scheduledFor IS NULL OR scheduledFor <= :now) ORDER BY timestamp DESC")
    fun getNotificationsForUser(userId: String, currentRole: String = "", now: Long = System.currentTimeMillis()): Flow<List<NotificationEntity>>

    @Query("SELECT * FROM notifications WHERE userId = :userId AND (:currentRole = '' OR role = :currentRole OR role = '') AND (scheduledFor IS NULL OR scheduledFor <= :now) ORDER BY timestamp DESC LIMIT :limit OFFSET :offset")
    suspend fun getNotificationsForUserPage(userId: String, currentRole: String = "", limit: Int, offset: Int, now: Long = System.currentTimeMillis()): List<NotificationEntity>

    @Query("SELECT COUNT(*) FROM notifications WHERE userId = :userId AND (:currentRole = '' OR role = :currentRole OR role = '') AND isRead = 0 AND (scheduledFor IS NULL OR scheduledFor <= :now)")
    fun getUnreadCount(userId: String, currentRole: String = "", now: Long = System.currentTimeMillis()): Flow<Int>

    @Query("SELECT COUNT(*) FROM notifications WHERE userId = :userId AND (:currentRole = '' OR role = :currentRole OR role = '') AND targetType = 'problem' AND isRead = 0 AND (scheduledFor IS NULL OR scheduledFor <= :now)")
    suspend fun getUnreadCountForUser(userId: String, currentRole: String = "", now: Long = System.currentTimeMillis()): Int

    @Query("SELECT COUNT(*) FROM notifications WHERE userId = :userId AND (:currentRole = '' OR role = :currentRole OR role = '') AND targetType = 'problem' AND isRead = 0 AND (scheduledFor IS NULL OR scheduledFor <= :now)")
    fun getUnreadProblemNotificationsFlow(userId: String, currentRole: String = "", now: Long = System.currentTimeMillis()): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotifications(notifications: List<NotificationEntity>)

    @Query("SELECT * FROM notifications ORDER BY timestamp DESC")
    fun getAllNotificationsFlow(): Flow<List<NotificationEntity>>

    @Query("SELECT * FROM notifications ORDER BY timestamp DESC LIMIT :limit OFFSET :offset")
    suspend fun getAllNotificationsFlowPage(limit: Int, offset: Int): List<NotificationEntity>

    @Query("UPDATE notifications SET isRead = 1 WHERE userId = :userId")
    suspend fun markAllAsRead(userId: String)

    @Query("UPDATE notifications SET isRead = 1 WHERE id = :id")
    suspend fun markAsRead(id: String)

    // [SUPABASE-MIGRATED - ধাপ ১২] markNotificationAsRead()-এ Supabase dual-write করার আগে এই
    // notification-এর userId জানা দরকার (RLS guard: শুধু নিজের notification নিজে read মার্ক করতে
    // পারবে) -- আগে userId প্যারামিটার হিসেবে পাস হতো না, তাই এই lookup যোগ করা হলো।
    @Query("SELECT * FROM notifications WHERE id = :id LIMIT 1")
    suspend fun getNotificationById(id: String): NotificationEntity?

    @Query("DELETE FROM notifications WHERE id = :id")
    suspend fun deleteNotification(id: String)

    @Query("DELETE FROM notifications WHERE title = :title AND timestamp = :timestamp")
    suspend fun deleteNotificationGroup(title: String, timestamp: Long)

    @Query("DELETE FROM notifications WHERE title = :title AND scheduledFor = :scheduledFor")
    suspend fun deleteScheduledNotificationGroup(title: String, scheduledFor: Long)
}

@Dao
interface WithdrawalDao {
    @Query("SELECT * FROM withdrawals WHERE solverId = :solverId ORDER BY createdAt DESC")
    fun getWithdrawalsForSolver(solverId: String): Flow<List<WithdrawalEntity>>

    @Query("SELECT * FROM withdrawals WHERE solverId = :solverId ORDER BY createdAt DESC LIMIT :limit OFFSET :offset")
    suspend fun getWithdrawalsForSolverPage(solverId: String, limit: Int, offset: Int): List<WithdrawalEntity>

    @Query("SELECT * FROM withdrawals ORDER BY createdAt DESC")
    fun getAllWithdrawals(): Flow<List<WithdrawalEntity>>

    @Query("SELECT * FROM withdrawals ORDER BY createdAt DESC LIMIT :limit OFFSET :offset")
    suspend fun getAllWithdrawalsPage(limit: Int, offset: Int): List<WithdrawalEntity>

    @Query("SELECT * FROM withdrawals WHERE id = :id LIMIT 1")
    suspend fun getWithdrawalById(id: String): WithdrawalEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWithdrawal(withdrawal: WithdrawalEntity)

    @Update
    suspend fun updateWithdrawal(withdrawal: WithdrawalEntity)

    @Query("DELETE FROM withdrawals WHERE id = :id")
    suspend fun deleteWithdrawal(id: String)
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC LIMIT :limit OFFSET :offset")
    suspend fun getAllTransactionsPage(limit: Int, offset: Int): List<TransactionEntity>

    @Query("SELECT * FROM transactions")
    suspend fun getAllTransactionsList(): List<TransactionEntity>

    // [BALANCE_REPUTATION_ROLE_SEPARATION - ধাপ ৫] আগে এখানে "solverId = :userId AND type !=
    // 'REFUND'" একটা ad-hoc হ্যাক ছিল (userId/solverId কলাম মিলে গেলেই দেখানো, REFUND টাইপ শুধু
    // বাদ দেওয়া)। এখন role-সচেতন ফিল্টারিং caller-সাইডে (TransactionHelper.matchesRoleForHistory,
    // TRANSACTION_ROLE_FIELD_DESIGN.md-এর blank-legacy টাইপ-ভিত্তিক resolution-সহ) হয় বলে এই
    // হ্যাকটা আর দরকার নেই এবং ভুল ছিল -- এটা শুধু "REFUND" টাইপ বাদ দিত, কিন্তু PAYMENT-এর মতো
    // অন্য SOLVER-role টাইপও User-role history-তে ঢুকে যেত (মূল বাগ)। এখন দুটো কলামেই ব্রড ম্যাচ
    // করা হয় -- role-based বাছাই সম্পূর্ণ caller-সাইডে।
    @Query("SELECT * FROM transactions WHERE userId = :userId OR solverId = :userId ORDER BY timestamp DESC")
    fun getTransactionsForUser(userId: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE userId = :userId OR solverId = :userId ORDER BY timestamp DESC LIMIT :limit OFFSET :offset")
    suspend fun getTransactionsForUserPage(userId: String, limit: Int, offset: Int): List<TransactionEntity>

    // আগে শুধু "solverId = :solverId" চেক করতো -- কিন্তু WITHDRAWAL_DEDUCTION/WITHDRAWAL_REFUND
    // (role="SOLVER") আসলে userId কলামে solver-এর id রাখে (solverId কলাম blank থাকে, দেখুন
    // SomadhanRepository.kt-এর withdrawal deduction/refund সাইট), তাই আগের কোয়েরিতে এই দুটো
    // টাইপ কখনো ম্যাচ করতো না (এই ফাংশনটার বর্তমানে কোনো caller নেই বলে বাগটা ধরা পড়েনি) -- এখন
    // ঠিক getTransactionsForUser()-এর মতোই দুটো কলামেই ব্রড ম্যাচ করা হয়।
    @Query("SELECT * FROM transactions WHERE solverId = :solverId OR userId = :solverId ORDER BY timestamp DESC")
    fun getTransactionsForSolver(solverId: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE solverId = :solverId OR userId = :solverId ORDER BY timestamp DESC LIMIT :limit OFFSET :offset")
    suspend fun getTransactionsForSolverPage(solverId: String, limit: Int, offset: Int): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE problemId = :problemId")
    suspend fun getTransactionsForProblem(problemId: String): List<TransactionEntity>

    // Added for deterministic-id idempotency checks on transactions that aren't tied to any
    // problemId (e.g. a withdrawal-rejection refund) -- getTransactionsForProblem() can't be
    // used for those, and pulling getAllTransactionsList() just to check one id is wasteful.
    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getTransactionById(id: String): TransactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity)

    // Batch version -- see UserDao.insertUsers() above for why this matters.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<TransactionEntity>)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransaction(id: String)
}

@Dao
interface PlatformSettingDao {
    @Query("SELECT * FROM platform_settings")
    fun getAllSettings(): Flow<List<PlatformSettingEntity>>

    @Query("SELECT value FROM platform_settings WHERE key = :key LIMIT 1")
    suspend fun getSetting(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetting(setting: PlatformSettingEntity)

    @Query("DELETE FROM platform_settings WHERE key = :key")
    suspend fun deleteSetting(key: String)
}

@Dao
interface EscrowDao {
    @Query("SELECT * FROM escrows WHERE problemId = :problemId")
    suspend fun getAllByProblemId(problemId: String): List<EscrowEntity>

    @Query("SELECT * FROM escrows")
    suspend fun getAllEscrowsSync(): List<EscrowEntity>

    @Query("SELECT * FROM escrows WHERE id = :escrowId LIMIT 1")
    suspend fun getEscrowById(escrowId: String): EscrowEntity?

    @Query("SELECT * FROM escrows WHERE problemId = :problemId ORDER BY createdAt DESC, rowid DESC LIMIT 1")
    suspend fun getByProblemId(problemId: String): EscrowEntity?

    @Query("SELECT * FROM escrows WHERE problemId = :problemId ORDER BY createdAt DESC, rowid DESC LIMIT 1")
    suspend fun getEscrowByProblemId(problemId: String): EscrowEntity?

    @Query("SELECT * FROM escrows WHERE problemId = :problemId ORDER BY createdAt DESC, rowid DESC LIMIT 1")
    fun getEscrowByProblemIdFlow(problemId: String): Flow<EscrowEntity?>

    @Query("SELECT * FROM escrows WHERE userId = :userId OR solverId = :userId ORDER BY createdAt DESC")
    fun getEscrowsForUser(userId: String): Flow<List<EscrowEntity>>

    @Query("SELECT * FROM escrows WHERE solverId = :solverId ORDER BY createdAt DESC")
    fun getEscrowsForSolver(solverId: String): Flow<List<EscrowEntity>>

    @Query("SELECT * FROM escrows ORDER BY createdAt DESC")
    fun getAllEscrows(): Flow<List<EscrowEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(escrow: EscrowEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEscrow(escrow: EscrowEntity)

    @Update
    suspend fun updateEscrow(escrow: EscrowEntity)

    // Bug fix: this used to be scoped by problemId ("WHERE problemId = :problemId") instead of
    // by escrow id. acceptBid() deliberately mints a BRAND-NEW escrow row (new id) for every
    // bid-accept cycle on the same problem -- so over a post's lifetime (solver A accepted then
    // cancelled -> refunded row, solver B accepted afterwards -> new HELD row) multiple escrow
    // rows can share the same problemId. A problemId-scoped UPDATE touches ALL of them at once,
    // so accepting an extra bill on the CURRENT (solver B) escrow was also silently bumping the
    // OLD, already-refunded (solver A) row's extraAmount. Scoping this by escrow id (like
    // refundEscrowOnce()/adminReleaseEscrow() already correctly do) makes it only ever touch the
    // one payment cycle it's actually meant for.
    @Query("UPDATE escrows SET extraAmount = extraAmount + :amount, updatedAt = :updatedAt WHERE id = :escrowId")
    suspend fun addExtraAmountById(escrowId: String, amount: Double, updatedAt: Long)

    @Deprecated("Scoped by problemId -- can silently rewrite the status of OTHER escrow rows sharing the same problemId (see addExtraAmountById's comment). Use releaseEscrow(escrowId, timestamp) / refundEscrow(escrowId, timestamp), which are scoped by escrow id.")
    @Query("UPDATE escrows SET status = :status, releasedAt = :releasedAt WHERE problemId = :problemId")
    suspend fun updateStatus(problemId: String, status: String, releasedAt: Long)

    @Query("UPDATE escrows SET status = 'RELEASED', releasedAt = :timestamp WHERE id = :escrowId")
    suspend fun releaseEscrow(escrowId: String, timestamp: Long)

    @Query("UPDATE escrows SET status = 'REFUNDED', releasedAt = :timestamp WHERE id = :escrowId")
    suspend fun refundEscrow(escrowId: String, timestamp: Long)

    @Query("SELECT * FROM escrows WHERE status = 'HELD' ORDER BY createdAt ASC")
    fun getAllHeldEscrows(): Flow<List<EscrowEntity>>

    @Query("SELECT * FROM escrows WHERE status = 'RELEASED' ORDER BY releasedAt DESC, createdAt DESC")
    fun getAllReleasedEscrows(): Flow<List<EscrowEntity>>

    @Query("SELECT * FROM escrows WHERE status = 'REFUNDED' OR status = 'REFUND_PENDING_SYNC' ORDER BY createdAt DESC")
    fun getAllRefundedEscrows(): Flow<List<EscrowEntity>>

    @Query("SELECT COALESCE(SUM(baseAmount + extraAmount), 0.0) FROM escrows WHERE status = 'HELD'")
    fun getTotalHeldAmount(): Flow<Double>

    @Query("DELETE FROM escrows WHERE id = :id")
    suspend fun deleteEscrow(id: String)
}

@Dao
interface AdditionalChargeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(charge: AdditionalChargeEntity)

    @Query("UPDATE additional_charges SET status = :status, respondedAt = :respondedAt WHERE id = :id")
    suspend fun updateStatus(id: String, status: String, respondedAt: Long)

    @Query("SELECT * FROM additional_charges WHERE problemId = :problemId AND status = 'PENDING' ORDER BY createdAt DESC")
    fun getPendingByProblemId(problemId: String): Flow<List<AdditionalChargeEntity>>

    @Query("SELECT * FROM additional_charges WHERE problemId = :problemId ORDER BY createdAt DESC")
    fun getAllByProblemId(problemId: String): Flow<List<AdditionalChargeEntity>>

    @Query("SELECT * FROM additional_charges ORDER BY createdAt DESC")
    fun getAllCharges(): Flow<List<AdditionalChargeEntity>>

    @Query("SELECT * FROM additional_charges ORDER BY createdAt DESC")
    fun getAllAdditionalCharges(): Flow<List<AdditionalChargeEntity>>

    @Query("SELECT * FROM additional_charges WHERE id = :id LIMIT 1")
    suspend fun getChargeById(id: String): AdditionalChargeEntity?

    @Query("DELETE FROM additional_charges WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface ReputationEventDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: ReputationEventEntity)

    @Query("SELECT * FROM reputation_events WHERE userId = :userId ORDER BY createdAt DESC LIMIT :limit")
    fun getRecentByUserId(userId: String, limit: Int = 20): Flow<List<ReputationEventEntity>>

    @Query("SELECT COALESCE(SUM(scoreChange), 0.0) FROM reputation_events WHERE userId = :userId AND eventType IN (:eventTypes) AND createdAt >= :sinceTimestamp")
    suspend fun getSumScoreChangeSince(userId: String, eventTypes: List<String>, sinceTimestamp: Long): Double

    @Query("SELECT COALESCE(SUM(scoreChange), 0.0) FROM reputation_events WHERE userId = :userId AND eventType IN (:eventTypes) AND problemId = :problemId")
    suspend fun getSumScoreChangeForProblem(userId: String, eventTypes: List<String>, problemId: String): Double

    @Query("DELETE FROM reputation_events WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface AdminAuditLogDao {
    @Query("SELECT * FROM admin_audit_logs ORDER BY timestamp DESC LIMIT 200")
    fun getRecentLogs(): Flow<List<AdminAuditLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: AdminAuditLogEntity)
}

@Dao
interface FaqDao {
    @Query("SELECT * FROM faqs WHERE isActive = 1 ORDER BY displayOrder ASC, createdAt ASC")
    fun getActiveFaqs(): Flow<List<FaqEntity>>

    @Query("SELECT * FROM faqs WHERE isActive = 1 AND (targetAudience = :audience OR targetAudience = 'BOTH') ORDER BY displayOrder ASC, createdAt ASC")
    fun getActiveFaqsForAudience(audience: String): Flow<List<FaqEntity>>

    @Query("SELECT * FROM faqs ORDER BY displayOrder ASC, createdAt ASC")
    fun getAllFaqs(): Flow<List<FaqEntity>>

    @Query("SELECT * FROM faqs WHERE targetAudience = :audience ORDER BY displayOrder ASC, createdAt ASC")
    fun getAllFaqsForAudience(audience: String): Flow<List<FaqEntity>>

    @Query("SELECT * FROM faqs WHERE id = :id LIMIT 1")
    suspend fun getFaqById(id: String): FaqEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFaq(faq: FaqEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFaqs(faqs: List<FaqEntity>)

    @Update
    suspend fun updateFaq(faq: FaqEntity)

    @Delete
    suspend fun deleteFaq(faq: FaqEntity)

    @Query("DELETE FROM faqs WHERE id = :id")
    suspend fun deleteFaqById(id: String)
}

@Dao
interface GatewayPaymentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: GatewayPaymentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayments(payments: List<GatewayPaymentEntity>)

    @Query("SELECT * FROM gateway_payments ORDER BY timestamp DESC")
    fun getAllPaymentsFlow(): Flow<List<GatewayPaymentEntity>>

    @Query("SELECT * FROM gateway_payments ORDER BY timestamp DESC")
    suspend fun getAllPayments(): List<GatewayPaymentEntity>

    @Query("SELECT * FROM gateway_payments WHERE userId = :userId ORDER BY timestamp DESC")
    fun getPaymentsForUserFlow(userId: String): Flow<List<GatewayPaymentEntity>>

    @Query("SELECT * FROM gateway_payments WHERE userId = :userId ORDER BY timestamp DESC")
    suspend fun getPaymentsForUser(userId: String): List<GatewayPaymentEntity>

    @Query("SELECT * FROM gateway_payments WHERE id = :id LIMIT 1")
    suspend fun getPaymentById(id: String): GatewayPaymentEntity?

    @Query("SELECT * FROM gateway_payments WHERE gatewayTrxId = :trxId LIMIT 1")
    suspend fun getPaymentByGatewayTrxId(trxId: String): GatewayPaymentEntity?

    @Query("UPDATE gateway_payments SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)

    @Query("DELETE FROM gateway_payments WHERE id = :id")
    suspend fun deletePayment(id: String)

    @Query("DELETE FROM gateway_payments")
    suspend fun deleteAllPayments()
}


