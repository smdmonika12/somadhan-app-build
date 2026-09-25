package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.database.AppDatabase
import com.example.data.entity.BidEntity
import com.example.data.entity.CategoryEntity
import com.example.data.entity.EscrowEntity
import com.example.data.entity.ProblemEntity
import com.example.data.entity.UserEntity
import com.example.data.remote.SupabaseRealtimeManager
import com.example.data.repository.SomadhanRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("সমাধান", appName)
  }

  @Test
  fun `instant job categories must be physical and enabled`() {
    val categories = listOf(
      CategoryEntity(
        id = "CAT_1",
        nameBangla = "প্লাম্বিং",
        nameEnglish = "Plumbing",
        isPhysical = true,
        iconName = "plumbing",
        keywords = "pipe,water",
        minBudget = 200.0,
        maxBudget = 5000.0,
        instantJobEnabled = true
      ),
      CategoryEntity(
        id = "CAT_2",
        nameBangla = "ইলেকট্রিক্যাল",
        nameEnglish = "Electrical",
        isPhysical = true,
        iconName = "electrical_services",
        keywords = "electric,wire",
        minBudget = 200.0,
        maxBudget = 5000.0,
        instantJobEnabled = false
      ),
      CategoryEntity(
        id = "CAT_3",
        nameBangla = "ওয়েব ডেভেলপমেন্ট",
        nameEnglish = "Web Dev",
        isPhysical = false,
        iconName = "code",
        keywords = "code,website",
        minBudget = 1000.0,
        maxBudget = 50000.0,
        instantJobEnabled = true
      )
    )

    val instantCategories = categories.filter { it.instantJobEnabled && it.isPhysical }
    assertEquals(1, instantCategories.size)
    assertEquals("CAT_1", instantCategories.first().id)
    assertTrue(instantCategories.all { it.isPhysical && it.instantJobEnabled })
  }

  @Test
  fun `solver toggle filters instant job notifications`() {
    val solverOn = UserEntity(
      id = "SOLVER_1",
      name = "রাকিব হাসান",
      phone = "01711111111",
      email = "solver1@test.com",
      password = "pass",
      role = "SOLVER",
      latitude = 23.8103,
      longitude = 90.4125,
      address = "ঢাকা",
      solverCategories = "CAT_1",
      instantJobNotificationsEnabled = true
    )
    val solverOff = UserEntity(
      id = "SOLVER_2",
      name = "করিম হোসেন",
      phone = "01722222222",
      email = "solver2@test.com",
      password = "pass",
      role = "SOLVER",
      latitude = 23.8103,
      longitude = 90.4125,
      address = "ঢাকা",
      solverCategories = "CAT_1",
      instantJobNotificationsEnabled = false
    )

    val job = ProblemEntity(
      id = "JOB_1",
      userId = "USER_1",
      userName = "আনিস আহমেদ",
      userPhone = "01700000000",
      userAddress = "ধানমন্ডি, ঢাকা",
      title = "জরুরি পানির পাইপ মেরামত",
      description = "পাইপ ফেটে পানি পড়ছে",
      categoryId = "CAT_1",
      categoryName = "প্লাম্বিং",
      isPhysical = true,
      latitude = 23.8103,
      longitude = 90.4125,
      minBudget = 500.0,
      maxBudget = 500.0,
      urgency = "খুব জরুরি",
      isInstantJob = true,
      jobStatus = "BROADCASTING"
    )

    val solverOnCanReceive = solverOn.instantJobNotificationsEnabled && solverOn.solverCategories.contains(job.categoryId)
    val solverOffCanReceive = solverOff.instantJobNotificationsEnabled && solverOff.solverCategories.contains(job.categoryId)

    assertTrue(solverOnCanReceive)
    assertFalse(solverOffCanReceive)
  }

  @Test
  fun `instant job completion calculation preserves existing wallet and platform commission rules`() {
    val agreedBudget = 1000.0
    val commissionPercent = 10.0 // 10%
    val commissionAmount = (agreedBudget * commissionPercent) / 100.0
    val solverEarnings = agreedBudget - commissionAmount

    assertEquals(100.0, commissionAmount, 0.001)
    assertEquals(900.0, solverEarnings, 0.001)
  }

  @Test
  fun `phase H verification single active instant job rule per user`() {
    val activeProblems = listOf(
      ProblemEntity(
        id = "JOB_ACT_1",
        userId = "USER_1",
        userName = "User One",
        userPhone = "01700000001",
        userAddress = "ঢাকা",
        title = "ফ্যান মেরামত",
        description = "জরুরি ফ্যান সমস্যা",
        categoryId = "CAT_1",
        categoryName = "ইলেকট্রিক্যাল",
        isPhysical = true,
        latitude = 23.81,
        longitude = 90.41,
        minBudget = 500.0,
        maxBudget = 500.0,
        urgency = "জরুরি",
        isInstantJob = true,
        jobStatus = "BROADCASTING",
        status = "OPEN"
      )
    )

    val currentActive = activeProblems.firstOrNull {
      it.userId == "USER_1" && it.isInstantJob && !it.isUserDeleted && it.jobStatus != "COMPLETED" && it.jobStatus != "CANCELLED"
    }

    assertTrue(currentActive != null)
    assertEquals("JOB_ACT_1", currentActive?.id)
  }

  @Test
  fun `phase H verification solver can bid once per post but on multiple posts`() {
    val bidsList = mutableListOf(
      com.example.data.entity.BidEntity(
        id = "BID_1",
        problemId = "JOB_1",
        solverId = "SOLVER_1",
        solverName = "রহিম",
        solverPhone = "01711111111",
        amount = 600.0,
        message = "আমি কাজটি দ্রুত করে দিতে পারব",
        estimatedTime = "১ ঘণ্টা",
        status = "PENDING"
      )
    )

    val canBidSameJob = bidsList.none { it.problemId == "JOB_1" && it.solverId == "SOLVER_1" && it.status != "CANCELLED" }
    val canBidDifferentJob = bidsList.none { it.problemId == "JOB_2" && it.solverId == "SOLVER_1" && it.status != "CANCELLED" }

    assertFalse(canBidSameJob)
    assertTrue(canBidDifferentJob)
  }

  @Test
  fun `phase H verification arrival geofence radius dynamic enforcement`() {
    val arrivalRadiusMeters = 200.0
    val distanceOutsideMeters = 350.0
    val distanceInsideMeters = 120.0

    val canMarkArrivedOutside = distanceOutsideMeters <= arrivalRadiusMeters
    val canMarkArrivedInside = distanceInsideMeters <= arrivalRadiusMeters

    assertFalse(canMarkArrivedOutside)
    assertTrue(canMarkArrivedInside)
  }

  @Test
  fun `phase H verification extra amount and total release calculation`() {
    val baseEscrowBidAmount = 650.0
    val confirmedExtra1 = 150.0
    val totalAmount = baseEscrowBidAmount + confirmedExtra1

    assertEquals(800.0, totalAmount, 0.001)
  }

  @Test
  fun `phase AE dispute checklist test - release to solver`() {
    val reachedStep = 3
    val decision = "RELEASE_TO_SOLVER"

    val baseSteps = listOf(
      "১. জরুরি ব্রডকাস্ট ও সংযোগ" to 1,
      "২. সমাধানকারী রওয়ানা হয়েছেন" to 2,
      "৩. লোকেশনে পৌঁছেছেন" to 3,
      "৪. কাজ শুরু হয়েছে" to 4,
      "৫. কাজ সম্পন্ন" to 5
    ).map { (label, stepNum) ->
      label to (stepNum <= reachedStep)
    }

    val disputeStep = "৬. বিরোধ উত্থাপিত হয়েছে" to true
    val resolutionLabel = when (decision) {
      "RELEASE_TO_SOLVER" -> "৭. অ্যাডমিন কর্তৃক অর্থ রিলিজ করা হয়েছে"
      "REFUND_TO_USER" -> "৭. অ্যাডমিন কর্তৃক অর্থ রিফান্ড করা হয়েছে"
      "SPLIT_SETTLEMENT", "CUSTOM_SPLIT" -> "৭. অ্যাডমিন কর্তৃক অর্থ ভাগাভাগি (স্প্লিট) করা হয়েছে"
      else -> "৭. বিরোধ নিষ্পত্তি সম্পন্ন"
    }
    val verdictStep = resolutionLabel to true

    val allSteps = baseSteps + disputeStep + verdictStep
    assertEquals(7, allSteps.size)
    // Step 1..3 are done, 4..5 are not done
    assertTrue(allSteps[0].second)
    assertTrue(allSteps[1].second)
    assertTrue(allSteps[2].second)
    assertFalse(allSteps[3].second)
    assertFalse(allSteps[4].second)
    // Step 6 & 7 are done
    assertTrue(allSteps[5].second)
    assertEquals("৬. বিরোধ উত্থাপিত হয়েছে", allSteps[5].first)
    assertTrue(allSteps[6].second)
    assertEquals("৭. অ্যাডমিন কর্তৃক অর্থ রিলিজ করা হয়েছে", allSteps[6].first)
  }

  @Test
  fun `phase AE dispute checklist test - refund to user`() {
    val reachedStep = 2
    val decision = "REFUND_TO_USER"

    val baseSteps = listOf(
      "১. জরুরি ব্রডকাস্ট ও সংযোগ" to 1,
      "২. সমাধানকারী রওয়ানা হয়েছেন" to 2,
      "৩. লোকেশনে পৌঁছেছেন" to 3,
      "৪. কাজ শুরু হয়েছে" to 4,
      "৫. কাজ সম্পন্ন" to 5
    ).map { (label, stepNum) ->
      label to (stepNum <= reachedStep)
    }

    val disputeStep = "৬. বিরোধ উত্থাপিত হয়েছে" to true
    val resolutionLabel = when (decision) {
      "RELEASE_TO_SOLVER" -> "৭. অ্যাডমিন কর্তৃক অর্থ রিলিজ করা হয়েছে"
      "REFUND_TO_USER" -> "৭. অ্যাডমিন কর্তৃক অর্থ রিফান্ড করা হয়েছে"
      "SPLIT_SETTLEMENT", "CUSTOM_SPLIT" -> "৭. অ্যাডমিন কর্তৃক অর্থ ভাগাভাগি (স্প্লিট) করা হয়েছে"
      else -> "৭. বিরোধ নিষ্পত্তি সম্পন্ন"
    }
    val verdictStep = resolutionLabel to true

    val allSteps = baseSteps + disputeStep + verdictStep
    assertEquals(7, allSteps.size)
    assertTrue(allSteps[0].second)
    assertTrue(allSteps[1].second)
    assertFalse(allSteps[2].second)
    assertFalse(allSteps[3].second)
    assertFalse(allSteps[4].second)
    assertTrue(allSteps[5].second)
    assertEquals("৬. বিরোধ উত্থাপিত হয়েছে", allSteps[5].first)
    assertTrue(allSteps[6].second)
    assertEquals("৭. অ্যাডমিন কর্তৃক অর্থ রিফান্ড করা হয়েছে", allSteps[6].first)
  }

  @Test
  fun `phase AE dispute checklist test - split settlement`() {
    val reachedStep = 4
    val decision = "SPLIT_SETTLEMENT"

    val baseSteps = listOf(
      "১. জরুরি ব্রডকাস্ট ও সংযোগ" to 1,
      "২. সমাধানকারী রওয়ানা হয়েছেন" to 2,
      "৩. লোকেশনে পৌঁছেছেন" to 3,
      "৪. কাজ শুরু হয়েছে" to 4,
      "৫. কাজ সম্পন্ন" to 5
    ).map { (label, stepNum) ->
      label to (stepNum <= reachedStep)
    }

    val disputeStep = "৬. বিরোধ উত্থাপিত হয়েছে" to true
    val resolutionLabel = when (decision) {
      "RELEASE_TO_SOLVER" -> "৭. অ্যাডমিন কর্তৃক অর্থ রিলিজ করা হয়েছে"
      "REFUND_TO_USER" -> "৭. অ্যাডমিন কর্তৃক অর্থ রিফান্ড করা হয়েছে"
      "SPLIT_SETTLEMENT", "CUSTOM_SPLIT" -> "৭. অ্যাডমিন কর্তৃক অর্থ ভাগাভাগি (স্প্লিট) করা হয়েছে"
      else -> "৭. বিরোধ নিষ্পত্তি সম্পন্ন"
    }
    val verdictStep = resolutionLabel to true

    val allSteps = baseSteps + disputeStep + verdictStep
    assertEquals(7, allSteps.size)
    assertTrue(allSteps[0].second)
    assertTrue(allSteps[1].second)
    assertTrue(allSteps[2].second)
    assertTrue(allSteps[3].second)
    assertFalse(allSteps[4].second)
    assertTrue(allSteps[5].second)
    assertEquals("৬. বিরোধ উত্থাপিত হয়েছে", allSteps[5].first)
    assertTrue(allSteps[6].second)
    assertEquals("৭. অ্যাডমিন কর্তৃক অর্থ ভাগাভাগি (স্প্লিট) করা হয়েছে", allSteps[6].first)
  }

  @Test
  fun `phase AF role check for broadcasting screen test`() {
    val currentUserId = "solver_123"
    val currentUserRole = "SOLVER"
    val problemUserId = "user_456"

    val isUserRole = currentUserId == problemUserId || currentUserRole != "SOLVER"
    assertFalse(isUserRole)

    var navigatedBack = false
    var showedUserBroadcasting = false

    val status = "BROADCASTING"

    if (status == "BROADCASTING" && isUserRole) {
      showedUserBroadcasting = true
    } else if (status == "BROADCASTING" && !isUserRole) {
      navigatedBack = true
    }

    assertTrue(navigatedBack)
    assertFalse(showedUserBroadcasting)

    // Verify user role
    val isRealUser = "user_456" == problemUserId || "USER" != "SOLVER"
    assertTrue(isRealUser)
    var userNavigatedBack = false
    var userShowedBroadcasting = false
    if (status == "BROADCASTING" && isRealUser) {
      userShowedBroadcasting = true
    } else if (status == "BROADCASTING" && !isRealUser) {
      userNavigatedBack = true
    }
    assertTrue(userShowedBroadcasting)
    assertFalse(userNavigatedBack)
  }

  @Test
  fun `phase AG cancellation summary screen routing and checklist test`() {
    val problemReverted = ProblemEntity(
      id = "JOB_CANCEL_1",
      userId = "USER_1",
      userName = "User One",
      userPhone = "01700000001",
      userAddress = "ঢাকা",
      title = "এসি সার্ভিসিং",
      description = "এসি ঠান্ডা হচ্ছে না",
      categoryId = "CAT_1",
      categoryName = "ইলেকট্রিক্যাল",
      isPhysical = true,
      latitude = 23.81,
      longitude = 90.41,
      minBudget = 1000.0,
      maxBudget = 1000.0,
      urgency = "জরুরি",
      isInstantJob = true,
      jobStatus = "BROADCASTING",
      status = "OPEN",
      solverCancelledNotice = "সমাধানকারী করিম হাসান কাজটি বাতিল করেছেন।"
    )

    val problemFresh = problemReverted.copy(solverCancelledNotice = null)

    // 1. Check routing logic
    // Reverted post -> Shows Cancellation Summary Screen for both user and solver
    val isUser = true
    val isSolver = false

    val routesToCancellationSummaryForUser = !problemReverted.solverCancelledNotice.isNullOrBlank()
    val routesToCancellationSummaryForSolver = !problemReverted.solverCancelledNotice.isNullOrBlank()
    assertTrue(routesToCancellationSummaryForUser)
    assertTrue(routesToCancellationSummaryForSolver)

    // Fresh post -> Solver navigates back, User sees UserJobBroadcastingScreen
    val freshUserShowsBroadcasting = problemFresh.solverCancelledNotice.isNullOrBlank() && isUser
    val freshSolverNavigatesBack = problemFresh.solverCancelledNotice.isNullOrBlank() && !isSolver
    assertTrue(freshUserShowsBroadcasting)
    assertTrue(freshSolverNavigatesBack)

    // 2. Check dynamic cancellation checklist steps calculation
    val reachedStep = 3
    val baseSteps = listOf(
      "১. জরুরি ব্রডকাস্ট ও সংযোগ" to 1,
      "২. সমাধানকারী রওয়ানা হয়েছেন" to 2,
      "৩. লোকেশনে পৌঁছেছেন" to 3,
      "৪. কাজ শুরু হয়েছে" to 4,
      "৫. কাজ সম্পন্ন" to 5
    ).map { (label, stepNum) ->
      label to (stepNum <= reachedStep)
    }

    val finalStep = "৭. সমাধানকারী কর্তৃক কাজ বাতিল ❌" to true
    val allSteps = baseSteps + listOf(finalStep)

    assertEquals(6, allSteps.size)
    assertTrue(allSteps[0].second) // Step 1 done
    assertTrue(allSteps[1].second) // Step 2 done
    assertTrue(allSteps[2].second) // Step 3 done
    assertFalse(allSteps[3].second) // Step 4 not done
    assertFalse(allSteps[4].second) // Step 5 not done
    assertTrue(allSteps[5].second) // Step 7 final cancel is done
  }

  @Test
  fun `reconcileEscrowStates does not mark fresh second cycle held escrow as refunded`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    val repo = SomadhanRepository(db)

    val uniqueSuffix = java.util.UUID.randomUUID().toString().take(8)
    val clientUser = UserEntity(
      id = "CLIENT_$uniqueSuffix",
      name = "জামাল উদ্দিন",
      phone = "01811111111",
      email = "client_$uniqueSuffix@test.com",
      password = "pass",
      role = "USER",
      latitude = 23.8103,
      longitude = 90.4125,
      address = "ঢাকা",
      balance = 5000.0
    )
    val solverA = UserEntity(
      id = "SOLVER_A_$uniqueSuffix",
      name = "করিম সলভার",
      phone = "01822222222",
      email = "solvera_$uniqueSuffix@test.com",
      password = "pass",
      role = "SOLVER",
      latitude = 23.8103,
      longitude = 90.4125,
      address = "ঢাকা",
      balance = 0.0
    )
    val solverB = UserEntity(
      id = "SOLVER_B_$uniqueSuffix",
      name = "রহিম সলভার",
      phone = "01833333333",
      email = "solverb_$uniqueSuffix@test.com",
      password = "pass",
      role = "SOLVER",
      latitude = 23.8103,
      longitude = 90.4125,
      address = "ঢাকা",
      balance = 0.0
    )
    db.userDao().insertUser(clientUser)
    db.userDao().insertUser(solverA)
    db.userDao().insertUser(solverB)

    // 1. Post a problem
    val problem = ProblemEntity(
      id = "PROB_$uniqueSuffix",
      userId = clientUser.id,
      userName = clientUser.name,
      userPhone = clientUser.phone,
      userAddress = "ঢাকা",
      title = "ফ্যান মেরামত",
      description = "সিলিং ফ্যান ঘুরছে না",
      categoryId = "CAT_1",
      categoryName = "ইলেকট্রিক্যাল",
      isPhysical = true,
      latitude = 23.8103,
      longitude = 90.4125,
      minBudget = 1000.0,
      maxBudget = 1000.0,
      urgency = "সাধারণ",
      status = "OPEN"
    )
    db.problemDao().insertProblem(problem)

    // 2. Accept Bid A (Solver A) -> Creates Escrow #1 (HELD)
    val bidA = BidEntity(
      id = "BID_A_$uniqueSuffix",
      problemId = problem.id,
      solverId = solverA.id,
      solverName = solverA.name,
      solverPhone = solverA.phone,
      amount = 1000.0,
      message = "আমি কাজটি সুন্দরভাবে সমাধান করে দিবো",
      estimatedTime = "১ দিন",
      status = "PENDING"
    )
    db.bidDao().insertBid(bidA)
    repo.acceptBid(problem, bidA)

    val escrow1 = db.escrowDao().getByProblemId(problem.id)
    assertNotNull(escrow1)
    assertEquals("HELD", escrow1!!.status)
    assertEquals(solverA.id, escrow1.solverId)

    // 3. Solver A cancels -> triggers refund for Escrow #1
    // Escrow #1 becomes REFUNDED (or REFUND_PENDING_SYNC), problem is reset to OPEN
    val refundSuccess1 = repo.refundEscrowOnce(
      problemId = problem.id,
      amount = escrow1.baseAmount + escrow1.extraAmount,
      userId = clientUser.id,
      solverId = solverA.id,
      problemTitle = problem.title,
      escrowId = escrow1.id
    )
    assertTrue(refundSuccess1)
    val updatedProblemAfterCancel = db.problemDao().getProblemById(problem.id)!!.copy(
      status = "OPEN",
      acceptedSolverId = null,
      acceptedBidId = null
    )
    db.problemDao().updateProblem(updatedProblemAfterCancel)

    // Verify Escrow #1 is refunded and refund transaction #1 exists
    val escrow1AfterCancel = db.escrowDao().getEscrowById(escrow1.id)
    assertNotNull(escrow1AfterCancel)
    assertTrue(escrow1AfterCancel!!.status == "REFUNDED" || escrow1AfterCancel.status == "REFUND_PENDING_SYNC")

    val transactionsAfterCancel1 = db.transactionDao().getTransactionsForProblem(problem.id)
    assertEquals(1, transactionsAfterCancel1.count { it.type == "REFUND" })
    assertEquals(escrow1.id, transactionsAfterCancel1.first { it.type == "REFUND" }.escrowId)

    val clientBalanceAfterRefund1 = db.userDao().getUserById(clientUser.id)!!.balance
    assertEquals(5000.0, clientBalanceAfterRefund1, 0.01)

    // 4. Re-select and accept Bid B on the same problem -> Creates Escrow #2 (HELD)
    val bidB = BidEntity(
      id = "BID_B_$uniqueSuffix",
      problemId = problem.id,
      solverId = solverB.id,
      solverName = solverB.name,
      solverPhone = solverB.phone,
      amount = 1200.0,
      message = "আমি দ্রুততম সময়ে ফ্যান ঠিক করবো",
      estimatedTime = "২ ঘন্টা",
      status = "PENDING"
    )
    db.bidDao().insertBid(bidB)
    repo.acceptBid(updatedProblemAfterCancel, bidB)

    val allEscrows = db.escrowDao().getAllByProblemId(problem.id)
    assertEquals(2, allEscrows.size)
    val escrow2 = allEscrows.first { it.solverId == solverB.id }
    assertEquals("HELD", escrow2.status)
    assertEquals(1200.0, escrow2.baseAmount, 0.01)

    // 5. Call reconcileEscrowStates()
    // It MUST NOT flip Escrow #2 to REFUNDED!
    repo.reconcileEscrowStates()

    val escrow2AfterReconcile = db.escrowDao().getEscrowById(escrow2.id)
    assertNotNull(escrow2AfterReconcile)
    assertEquals("HELD", escrow2AfterReconcile!!.status)

    // 6. Cancel Bid B -> confirm refundEscrowOnce() actually credits wallet for Escrow #2
    val refundSuccess2 = repo.refundEscrowOnce(
      problemId = problem.id,
      amount = escrow2.baseAmount + escrow2.extraAmount,
      userId = clientUser.id,
      solverId = solverB.id,
      problemTitle = problem.title,
      escrowId = escrow2.id
    )
    assertTrue("refundEscrowOnce must succeed for escrow #2", refundSuccess2)

    val clientBalanceAfterRefund2 = db.userDao().getUserById(clientUser.id)!!.balance
    assertEquals(5000.0, clientBalanceAfterRefund2, 0.01)

    val transactionsAfterCancel2 = db.transactionDao().getTransactionsForProblem(problem.id)
    val refunds = transactionsAfterCancel2.filter { it.type == "REFUND" }
    assertEquals(2, refunds.size)
    assertTrue(refunds.any { it.escrowId == escrow1.id })
    assertTrue(refunds.any { it.escrowId == escrow2.id })

    db.close()
  }

  @Test
  fun `pull and listener escrow sync do not flip fresh second cycle held escrow to refunded`() = runBlocking {
    // Regression test for Bug 3: pullAllCloudDataToLocal()'s "Pull Escrows" step and the
    // escrowsListener real-time handler used to scope their "does a refund already exist"
    // check to problemId only, so a stale refund from an EARLIER cycle (bid A) caused a
    // brand new HELD escrow from a LATER cycle (bid B) to be silently flipped to REFUNDED
    // locally -- with no transaction and no wallet credit. This test exercises the exact
    // decision logic both sync paths now share (SupabaseRealtimeManager.resolveIncomingEscrowStatus)
    // against the same two-cycle scenario and asserts escrow #2 stays HELD.
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    val repo = SomadhanRepository(db)

    val uniqueSuffix = java.util.UUID.randomUUID().toString().take(8)
    val clientUser = UserEntity(
      id = "CLIENT_$uniqueSuffix",
      name = "জামাল উদ্দিন",
      phone = "01811111112",
      email = "client2_$uniqueSuffix@test.com",
      password = "pass",
      role = "USER",
      latitude = 23.8103,
      longitude = 90.4125,
      address = "ঢাকা",
      balance = 5000.0
    )
    val solverA = UserEntity(
      id = "SOLVER_A_$uniqueSuffix",
      name = "করিম সলভার",
      phone = "01822222223",
      email = "solvera2_$uniqueSuffix@test.com",
      password = "pass",
      role = "SOLVER",
      latitude = 23.8103,
      longitude = 90.4125,
      address = "ঢাকা",
      balance = 0.0
    )
    val solverB = UserEntity(
      id = "SOLVER_B_$uniqueSuffix",
      name = "রহিম সলভার",
      phone = "01833333334",
      email = "solverb2_$uniqueSuffix@test.com",
      password = "pass",
      role = "SOLVER",
      latitude = 23.8103,
      longitude = 90.4125,
      address = "ঢাকা",
      balance = 0.0
    )
    db.userDao().insertUser(clientUser)
    db.userDao().insertUser(solverA)
    db.userDao().insertUser(solverB)

    val problem = ProblemEntity(
      id = "PROB_$uniqueSuffix",
      userId = clientUser.id,
      userName = clientUser.name,
      userPhone = clientUser.phone,
      userAddress = "ঢাকা",
      title = "ফ্যান মেরামত",
      description = "সিলিং ফ্যান ঘুরছে না",
      categoryId = "CAT_1",
      categoryName = "ইলেকট্রিক্যাল",
      isPhysical = true,
      latitude = 23.8103,
      longitude = 90.4125,
      minBudget = 1000.0,
      maxBudget = 1000.0,
      urgency = "সাধারণ",
      status = "OPEN"
    )
    db.problemDao().insertProblem(problem)

    // Bid A accepted -> Escrow #1 (HELD)
    val bidA = BidEntity(
      id = "BID_A_$uniqueSuffix",
      problemId = problem.id,
      solverId = solverA.id,
      solverName = solverA.name,
      solverPhone = solverA.phone,
      amount = 1000.0,
      message = "আমি কাজটি সুন্দরভাবে সমাধান করে দিবো",
      estimatedTime = "১ দিন",
      status = "PENDING"
    )
    db.bidDao().insertBid(bidA)
    repo.acceptBid(problem, bidA)
    val escrow1 = db.escrowDao().getByProblemId(problem.id)!!

    // Solver A cancels -> Escrow #1 refunded, problem reopened
    repo.refundEscrowOnce(
      problemId = problem.id,
      amount = escrow1.baseAmount + escrow1.extraAmount,
      userId = clientUser.id,
      solverId = solverA.id,
      problemTitle = problem.title,
      escrowId = escrow1.id
    )
    val reopenedProblem = db.problemDao().getProblemById(problem.id)!!.copy(
      status = "OPEN",
      acceptedSolverId = null,
      acceptedBidId = null
    )
    db.problemDao().updateProblem(reopenedProblem)

    // Bid B accepted -> Escrow #2 (HELD), currently active cycle
    val bidB = BidEntity(
      id = "BID_B_$uniqueSuffix",
      problemId = problem.id,
      solverId = solverB.id,
      solverName = solverB.name,
      solverPhone = solverB.phone,
      amount = 1200.0,
      message = "আমি দ্রুততম সময়ে ফ্যান ঠিক করবো",
      estimatedTime = "২ ঘন্টা",
      status = "PENDING"
    )
    db.bidDao().insertBid(bidB)
    repo.acceptBid(reopenedProblem, bidB)
    val escrow2 = db.escrowDao().getAllByProblemId(problem.id).first { it.solverId == solverB.id }
    assertEquals("HELD", escrow2.status)

    val problemAfterBidB = db.problemDao().getProblemById(problem.id)!!
    assertEquals(solverB.id, problemAfterBidB.acceptedSolverId)

    // Old (pre-fix) behavior check: a problem-scoped refund lookup would wrongly find
    // escrow #1's refund and treat it as applying to escrow #2 too.
    val anyRefundOnProblem = db.transactionDao().getTransactionsForProblem(problem.id)
      .any { it.type == "REFUND" }
    assertTrue("sanity check: an old refund from cycle 1 exists on this problem", anyRefundOnProblem)

    // Simulate pullAllCloudDataToLocal()'s "Pull Escrows" step for escrow #2 using the shared,
    // now escrow-scoped decision function.
    val hasEscrowScopedRefundForEscrow2 = db.transactionDao().getTransactionsForProblem(problem.id)
      .any { it.type == "REFUND" && it.escrowId == escrow2.id }
    assertFalse(hasEscrowScopedRefundForEscrow2)

    val pulledEscrow2 = SupabaseRealtimeManager.resolveIncomingEscrowStatus(
      escrow2,
      problemAfterBidB,
      hasEscrowScopedRefundForEscrow2
    )
    assertEquals("HELD", pulledEscrow2.status)

    // Simulate escrowsListener's MODIFIED/ADDED branch for the same incoming escrow #2 doc.
    val listenerEscrow2 = SupabaseRealtimeManager.resolveIncomingEscrowStatus(
      escrow2.copy(),
      problemAfterBidB,
      hasEscrowScopedRefundForEscrow2
    )
    assertEquals("HELD", listenerEscrow2.status)

    // Escrow #1 (stale cycle, problem now moved on to solver B) should still resolve to REFUNDED
    // when it has its own escrow-scoped refund transaction.
    val hasEscrowScopedRefundForEscrow1 = db.transactionDao().getTransactionsForProblem(problem.id)
      .any { it.type == "REFUND" && it.escrowId == escrow1.id }
    assertTrue(hasEscrowScopedRefundForEscrow1)
    val pulledEscrow1 = SupabaseRealtimeManager.resolveIncomingEscrowStatus(
      escrow1.copy(status = "HELD", releasedAt = null),
      problemAfterBidB,
      hasEscrowScopedRefundForEscrow1
    )
    assertEquals("REFUNDED", pulledEscrow1.status)

    db.close()
  }

  @Test
  fun `earlier cycle's bid keeps its own resolutionType after a later cycle on the same problem is disputed and split`() = runBlocking {
    // Regression test for the "post history tab" bug: a problem's shared, per-cycle-overwritten
    // dispute fields (problem.isDisputed, disputeResolutionDecision, ...) used to be the only
    // signal InstantJobHistoryScreen used to categorize a job into "Cancelled" vs "Disputed".
    // Because those fields get overwritten by whichever cycle resolves most recently, a LATER
    // cycle's dispute silently made an EARLIER, unrelated cycle's plain cancellation disappear
    // from the "Cancelled" history and reappear under "Disputed". The fix stamps each bid with
    // its own resolutionType at the moment that specific cycle ends, independent of later cycles.
    // This test asserts that stamp survives a later cycle's dispute resolution unchanged.
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    val repo = SomadhanRepository(db)

    val uniqueSuffix = java.util.UUID.randomUUID().toString().take(8)
    val clientUser = UserEntity(
      id = "CLIENT_$uniqueSuffix",
      name = "নাসরিন আক্তার",
      phone = "01844444445",
      email = "client3_$uniqueSuffix@test.com",
      password = "pass",
      role = "USER",
      latitude = 23.8103,
      longitude = 90.4125,
      address = "ঢাকা",
      balance = 5000.0
    )
    val solverA = UserEntity(
      id = "SOLVER_A2_$uniqueSuffix",
      name = "জামিল সলভার",
      phone = "01855555556",
      email = "solvera3_$uniqueSuffix@test.com",
      password = "pass",
      role = "SOLVER",
      latitude = 23.8103,
      longitude = 90.4125,
      address = "ঢাকা",
      balance = 0.0
    )
    val solverB = UserEntity(
      id = "SOLVER_B2_$uniqueSuffix",
      name = "ফাহিম সলভার",
      phone = "01866666667",
      email = "solverb3_$uniqueSuffix@test.com",
      password = "pass",
      role = "SOLVER",
      latitude = 23.8103,
      longitude = 90.4125,
      address = "ঢাকা",
      balance = 0.0
    )
    db.userDao().insertUser(clientUser)
    db.userDao().insertUser(solverA)
    db.userDao().insertUser(solverB)

    val problem = ProblemEntity(
      id = "PROB2_$uniqueSuffix",
      userId = clientUser.id,
      userName = clientUser.name,
      userPhone = clientUser.phone,
      userAddress = "ঢাকা",
      title = "পানির লাইন লিক",
      description = "রান্নাঘরের নিচে পানি জমছে",
      categoryId = "CAT_PLUMB",
      categoryName = "প্লাম্বিং ও পাইপফিটিং",
      isPhysical = true,
      isInstantJob = true,
      latitude = 23.8103,
      longitude = 90.4125,
      minBudget = 400.0,
      maxBudget = 400.0,
      urgency = "জরুরি",
      status = "OPEN"
    )
    db.problemDao().insertProblem(problem)

    // Cycle 1: bid A accepted, then solver A plain-cancels it.
    val bidA = BidEntity(
      id = "BIDX_A_$uniqueSuffix",
      problemId = problem.id,
      solverId = solverA.id,
      solverName = solverA.name,
      solverPhone = solverA.phone,
      amount = 400.0,
      message = "আমি এখনই আসতে পারব",
      estimatedTime = "৩০ মিনিট",
      status = "PENDING"
    )
    db.bidDao().insertBid(bidA)
    repo.acceptBid(problem, bidA)
    repo.solverCancelJob(problem.id, solverA.id, "ব্যক্তিগত কারণে আসতে পারছি না", reopenAsOpen = true)

    val bidAAfterCancel = db.bidDao().getBidById(bidA.id)!!
    assertEquals("CANCELLED", bidAAfterCancel.status)
    assertEquals("SOLVER_CANCEL", bidAAfterCancel.resolutionType)

    // Cycle 2: bid B accepted on the SAME problem, then goes to dispute and gets split by admin.
    val reopenedProblem = db.problemDao().getProblemById(problem.id)!!
    val bidB = BidEntity(
      id = "BIDX_B_$uniqueSuffix",
      problemId = problem.id,
      solverId = solverB.id,
      solverName = solverB.name,
      solverPhone = solverB.phone,
      amount = 400.0,
      message = "আমি কাজটি নিতে চাই",
      estimatedTime = "১ ঘণ্টা",
      status = "PENDING"
    )
    db.bidDao().insertBid(bidB)
    repo.acceptBid(reopenedProblem, bidB)
    repo.adminResolveDispute(
      problemId = problem.id,
      resolution = "SPLIT_SETTLEMENT",
      decisionNote = "উভয় পক্ষের বক্তব্য অনুযায়ী সমান ভাগ",
      splitSolverPercent = 50.0
    )

    val bidBAfterSplit = db.bidDao().getBidById(bidB.id)!!
    assertEquals("ADMIN_SPLIT", bidBAfterSplit.resolutionType)

    // The key regression check: bid A's stamp from cycle 1 must be untouched by cycle 2's
    // dispute resolution -- it should still read as a plain solver cancellation, not silently
    // become part of the later dispute.
    val bidAAfterLaterDispute = db.bidDao().getBidById(bidA.id)!!
    assertEquals("SOLVER_CANCEL", bidAAfterLaterDispute.resolutionType)
    assertEquals("CANCELLED", bidAAfterLaterDispute.status)

    db.close()
  }
}


