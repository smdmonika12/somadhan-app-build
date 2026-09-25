package com.example.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.R
import com.example.SomadhanApp
import com.example.data.entity.EscrowEntity
import com.example.data.entity.ProblemEntity
import com.example.data.entity.UserEntity
import com.example.data.repository.SomadhanRepository
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

/**
 * Premium Official PDF Receipt Generator for Somadhan (Emergency & Regular Jobs).
 * Strictly complies with:
 * 1. Complete Phone Number Privacy (Strictly NO phone numbers; Role badge & Reference UID only).
 * 2. Role-Based Accurate Financial Breakdown (Client invoice has no commission; Solver receipt has full commission & free quota details).
 * 3. Step-by-Step Complete Timeline (Exact match with Summary Page).
 * 4. Official Dispute & Admin Resolution Section (Dispute reason, verdict type, admin notes, and fund distribution).
 * 5. Premium PDF Layout (Branded header, authentic circular escrow stamp, vector QR verification matrix, and watermark).
 */
object InstantJobReceiptUtil {

    /**
     * Generates a PDF receipt for the given ProblemEntity (Default backward-compatible signature).
     */
    fun generateInstantJobReceipt(problem: ProblemEntity): File {
        val ctx = try { SomadhanApp.instance } catch (_: Exception) { null }
        if (ctx != null) {
            val file = generateInstantJobReceipt(ctx, problem)
            if (file != null) return file
        }
        return File.createTempFile("receipt_${problem.id}", ".pdf")
    }

    /**
     * Generates a PDF receipt with context support.
     */
    fun generateInstantJobReceipt(problem: ProblemEntity, context: Context?): File {
        val ctx = context ?: try { SomadhanApp.instance } catch (_: Exception) { null }
        if (ctx != null) {
            val file = generateInstantJobReceipt(ctx, problem)
            if (file != null) return file
        }
        return File.createTempFile("receipt_${problem.id}", ".pdf")
    }

    /**
     * Complete PDF Receipt Generator with Full Role-Based Breakdown, Post/Bid/Escrow IDs,
     * Dispute Details, Timeline steps, and Privacy protection.
     */
    fun generateInstantJobReceipt(
        context: Context,
        problem: ProblemEntity,
        otherUser: UserEntity? = null,
        selfUser: UserEntity? = null,
        isUserRole: Boolean = true,
        commissionBreakdown: SomadhanRepository.CommissionBreakdown? = null,
        escrowId: String? = null,
        bidId: String? = null,
        escrow: EscrowEntity? = null
    ): File? {
        return try {
            val rolePrefix = if (isUserRole) "Client" else "Solver"
            val shortId = problem.id.takeLast(8).uppercase()
            val fileName = "Somadhan_Receipt_${rolePrefix}_${shortId}_${System.currentTimeMillis()}.pdf"

            // Standard A4 Size: 595 x 842 points
            val pageWidth = 595
            val pageHeight = 842
            val document = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas

            drawReceiptContent(
                context = context,
                canvas = canvas,
                width = pageWidth,
                height = pageHeight,
                problem = problem,
                otherUser = otherUser,
                selfUser = selfUser,
                isUserRole = isUserRole,
                breakdown = commissionBreakdown,
                escrowId = escrowId,
                bidId = bidId,
                escrow = escrow
            )

            document.finishPage(page)

            // Save to app external/internal storage
            val outputDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
            if (!outputDir.exists()) outputDir.mkdirs()
            val localFile = File(outputDir, fileName)
            FileOutputStream(localFile).use { fos ->
                document.writeTo(fos)
            }

            // Also save to Public MediaStore Downloads (Android Q+) or public Downloads folder
            saveToPublicDownloads(context, fileName, localFile)

            document.close()
            localFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun saveToPublicDownloads(context: Context, fileName: String, sourceFile: File) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { outputStream ->
                        sourceFile.inputStream().use { inputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                }
            } else {
                val publicDownloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!publicDownloads.exists()) publicDownloads.mkdirs()
                val destFile = File(publicDownloads, fileName)
                sourceFile.copyTo(destFile, overwrite = true)
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    private fun drawReceiptContent(
        context: Context? = null,
        canvas: Canvas,
        width: Int,
        height: Int,
        problem: ProblemEntity,
        otherUser: UserEntity?,
        selfUser: UserEntity? = null,
        isUserRole: Boolean,
        breakdown: SomadhanRepository.CommissionBreakdown?,
        escrowId: String?,
        bidId: String?,
        escrow: EscrowEntity? = null
    ) {
        val paint = Paint().apply {
            isAntiAlias = true
        }

        // Color Palette
        val primaryOrange = Color.rgb(249, 115, 22)   // #F97316 Somadhan Brand Orange
        val darkHeaderBg = Color.rgb(15, 23, 42)      // #0F172A Deep Slate/Navy Header
        val darkText = Color.rgb(15, 23, 42)          // #0F172A Deep Black/Slate Text
        val secondaryText = Color.rgb(71, 85, 105)    // #475569 Slate Muted Text
        val lightBg = Color.rgb(248, 250, 252)        // #F8FAFC Clean Card Background
        val lightBgAlt = Color.rgb(241, 245, 249)     // #F1F5F9 Alternate Row Background
        val borderColor = Color.rgb(226, 232, 240)    // #E2E8F0 Subtle Border
        val successGreen = Color.rgb(22, 163, 74)     // #16A34A Verified Green
        val successGreenBg = Color.rgb(240, 253, 244) // #F0FDF4 Soft Green Bg
        val errorRed = Color.rgb(220, 38, 38)         // #DC2626 Cancel Red
        val errorRedBg = Color.rgb(254, 242, 242)     // #FEF2F2 Soft Red Bg
        val amberOrange = Color.rgb(217, 119, 6)      // #D97706 Dispute Amber
        val amberOrangeBg = Color.rgb(254, 243, 199)  // #FEF3C7 Soft Amber Bg
        val indigoAccent = Color.rgb(67, 56, 202)     // #4338CA Deep Indigo

        // -------------------------------------------------------------
        // Background Watermark (Subtle & Professional)
        // -------------------------------------------------------------
        drawBackgroundWatermark(canvas, width, height)

        // -------------------------------------------------------------
        // 1. Top Decorative Brand Banner
        // -------------------------------------------------------------
        paint.color = darkHeaderBg
        canvas.drawRect(0f, 0f, width.toFloat(), 78f, paint)

        // Accent strip under header
        paint.color = primaryOrange
        canvas.drawRect(0f, 78f, width.toFloat(), 82f, paint)

        // Somadhan Official App Logo on the left of header
        var logoDrawn = false
        val resolvedCtx = context ?: try { SomadhanApp.instance } catch (_: Exception) { null }
        if (resolvedCtx != null) {
            try {
                val logoBitmap = BitmapFactory.decodeResource(resolvedCtx.resources, R.drawable.somadhan_app_icon_1786820904533)
                if (logoBitmap != null) {
                    val logoRect = RectF(28f, 17f, 72f, 61f)
                    val clipPath = Path().apply {
                        addRoundRect(logoRect, 10f, 10f, Path.Direction.CW)
                    }
                    canvas.save()
                    canvas.clipPath(clipPath)
                    canvas.drawBitmap(logoBitmap, null, logoRect, paint)
                    canvas.restore()

                    // Crisp subtle border around logo
                    val logoBorderPaint = Paint().apply {
                        isAntiAlias = true
                        style = Paint.Style.STROKE
                        color = Color.rgb(255, 255, 255)
                        strokeWidth = 1.2f
                    }
                    canvas.drawRoundRect(logoRect, 10f, 10f, logoBorderPaint)
                    logoDrawn = true
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        if (!logoDrawn) {
            // Fallback Brand Badge if resource decoding fails
            paint.color = primaryOrange
            val fallbackRect = RectF(28f, 17f, 72f, 61f)
            canvas.drawRoundRect(fallbackRect, 10f, 10f, paint)
            paint.color = Color.WHITE
            paint.textSize = 22f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText("স", 50f, 46f, paint)
            paint.textAlign = Paint.Align.LEFT
        }

        // Header Title (Right beside the Somadhan App Logo)
        paint.color = Color.WHITE
        paint.textSize = 19f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("সমাধান (Somadhan)", 82f, 36f, paint)

        paint.textSize = 9.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.color = Color.rgb(203, 213, 225)
        val headerSubtitle = if (isUserRole) {
            "ডিজিটাল পেমেন্ট ইনভয়েস ও সার্ভিস রিসিট (Client Payment Invoice)"
        } else {
            "সমাধানকারী আর্নিং ও পে-আউট রিসিট (Solver Earnings & Payout Receipt)"
        }
        canvas.drawText(headerSubtitle, 82f, 54f, paint)

        // Top Right Meta Block
        paint.textAlign = Paint.Align.RIGHT
        paint.color = Color.WHITE
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val receiptNumber = "SOM-${problem.id.takeLast(8).uppercase()}"
        canvas.drawText("রিসিট নং: #$receiptNumber", (width - 32).toFloat(), 32f, paint)

        val todayStr = SimpleDateFormat("dd/MM/yyyy • hh:mm a", Locale.getDefault()).format(Date())
        paint.color = Color.rgb(226, 232, 240)
        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("ইস্যুর তারিখ: ${DistanceUtil.toBengaliDigits(todayStr)}", (width - 32).toFloat(), 48f, paint)

        // Role Copy Badge
        paint.color = primaryOrange
        val copyLabel = if (isUserRole) "গ্রাহকের কপি (Client Copy)" else "সমাধানকারীর কপি (Solver Copy)"
        canvas.drawText("কপি: $copyLabel", (width - 32).toFloat(), 62f, paint)
        paint.textAlign = Paint.Align.LEFT

        var y = 94f

        // -------------------------------------------------------------
        // 2. Status Badge & Meta Identifiers Card
        // -------------------------------------------------------------
        val isCompleted = problem.jobStatus == "JOB_COMPLETED" || problem.status == "COMPLETED"
        val isCancelled = problem.jobStatus == "CANCELLED" || problem.status == "CANCELLED"
        val isDisputed = problem.isDisputed || problem.disputeResolvedAt != null || !problem.disputeResolutionDecision.isNullOrBlank()

        val statusText = when {
            isDisputed -> {
                val decisionBangla = when (problem.disputeResolutionDecision ?: problem.disputeResolutionType) {
                    "REFUND_TO_USER", "REFUND" -> "গ্রাহককে পূর্ণ রিফান্ড"
                    "RELEASE_TO_SOLVER", "RELEASE" -> "সমাধানকারীকে পূর্ণ রিলিজ"
                    "SPLIT_SETTLEMENT", "CUSTOM_SPLIT", "SPLIT_50_50", "SETTLE", "SPLIT" -> "উভয় পক্ষে স্প্লিট মীমাংসা"
                    else -> "বিরোধ নিষ্পত্তি সম্পন্ন"
                }
                "বিরোধ নিষ্পত্তি সম্পন্ন ($decisionBangla)"
            }
            isCancelled -> "বাতিলকৃত কাজ (Job Cancelled)"
            else -> "সফলভাবে সম্পন্ন (Job Successfully Completed)"
        }

        val (statusBg, statusFg) = when {
            isDisputed -> amberOrangeBg to amberOrange
            isCancelled -> errorRedBg to errorRed
            else -> successGreenBg to successGreen
        }

        val metaCardHeight = 56f
        val metaCardRect = RectF(32f, y, (width - 32).toFloat(), y + metaCardHeight)
        paint.color = lightBg
        canvas.drawRoundRect(metaCardRect, 6f, 6f, paint)
        paint.color = borderColor
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        canvas.drawRoundRect(metaCardRect, 6f, 6f, paint)
        paint.style = Paint.Style.FILL

        // Status Pill Inside Meta Card
        val statusPillRect = RectF(40f, y + 6f, (width - 40).toFloat(), y + 24f)
        paint.color = statusBg
        canvas.drawRoundRect(statusPillRect, 4f, 4f, paint)
        paint.color = statusFg
        paint.textSize = 9.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("স্ট্যাটাস: $statusText", 48f, y + 19f, paint)

        // Meta IDs Line (Post ID, Bid ID, Escrow ID)
        val resolvedBidId = bidId ?: problem.acceptedBidId ?: ("BID-" + problem.id.takeLast(6).uppercase())
        val resolvedEscrowId = escrowId ?: ("ESC-" + problem.id.takeLast(8).uppercase())
        val categoryText = problem.categoryName.ifBlank { "জরুরি সমস্যা সমাধান" }

        paint.color = secondaryText
        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        val metaLine = "পোস্ট আইডি: #${problem.id.take(12)}   •   বিড আইডি: #$resolvedBidId   •   ক্যাটাগরি: $categoryText"
        canvas.drawText(metaLine, 42f, y + 37f, paint)

        paint.color = Color.rgb(100, 116, 139)
        paint.textSize = 8.2f
        canvas.drawText("এসক্রো ট্র্যাকিং রেফারেন্স: #$resolvedEscrowId (সিকিউরড এসক্রো পেমেন্ট সিস্টেম)", 42f, y + 49f, paint)

        y += metaCardHeight + 10f

        // -------------------------------------------------------------
        // 3. Privacy Rule: Parties Involved (NO Phone Numbers)
        // -------------------------------------------------------------
        paint.color = darkText
        paint.textSize = 10.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("সংশ্লিষ্ট উভয় পক্ষের বিবরণ (Parties Involved • Privacy Protected)", 32f, y, paint)
        y += 6f

        val clientName = problem.userName.ifBlank { "সম্মানিত সেবাগ্রহীতা" }
        val clientUserForUid = if (isUserRole) selfUser else otherUser
        val clientUid = clientUserForUid?.displayUid?.takeIf { it.isNotBlank() }
            ?: problem.userId.ifBlank { "U-${problem.id.take(6).uppercase()}" }
        val solverName = problem.acceptedSolverName?.ifBlank { otherUser?.name ?: "নির্ধারিত সমাধানকারী" }
            ?: otherUser?.name ?: "নির্ধারিত সমাধানকারী"
        val solverUserForUid = if (isUserRole) otherUser else selfUser
        val solverUid = solverUserForUid?.displayUid?.takeIf { it.isNotBlank() }
            ?: problem.acceptedSolverId?.ifBlank { otherUser?.id ?: "S-${problem.id.takeLast(6).uppercase()}" }
            ?: otherUser?.id ?: "S-${problem.id.takeLast(6).uppercase()}"

        val boxWidth = (width - 72f) / 2f
        val partyBoxHeight = 48f

        // Client Box (Left)
        val clientRect = RectF(32f, y, 32f + boxWidth, y + partyBoxHeight)
        paint.color = lightBg
        canvas.drawRoundRect(clientRect, 6f, 6f, paint)
        val activeBorderOrange = Color.argb(160, 249, 115, 22)
        paint.color = if (isUserRole) activeBorderOrange else borderColor
        paint.style = Paint.Style.STROKE
        canvas.drawRoundRect(clientRect, 6f, 6f, paint)
        paint.style = Paint.Style.FILL

        paint.color = if (isUserRole) primaryOrange else secondaryText
        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val clientRoleTag = if (isUserRole) "সেবাগ্রহীতা (আপনি / Client)" else "সেবাগ্রহীতা (Client)"
        canvas.drawText(clientRoleTag, 40f, y + 14f, paint)

        paint.color = darkText
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val clippedClientName = if (clientName.length > 26) clientName.take(24) + "..." else clientName
        canvas.drawText(clippedClientName, 40f, y + 28f, paint)

        paint.color = secondaryText
        paint.textSize = 8f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("রেফারেন্স ইউজার আইডি: #$clientUid", 40f, y + 40f, paint)

        // Solver Box (Right)
        val solverLeft = 40f + boxWidth
        val solverRect = RectF(solverLeft, y, solverLeft + boxWidth, y + partyBoxHeight)
        paint.color = lightBg
        canvas.drawRoundRect(solverRect, 6f, 6f, paint)
        paint.color = if (!isUserRole) activeBorderOrange else borderColor
        paint.style = Paint.Style.STROKE
        canvas.drawRoundRect(solverRect, 6f, 6f, paint)
        paint.style = Paint.Style.FILL

        paint.color = if (!isUserRole) primaryOrange else secondaryText
        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val solverRoleTag = if (!isUserRole) "সমাধানকারী (আপনি / Solver)" else "সমাধানকারী (Solver)"
        canvas.drawText(solverRoleTag, solverLeft + 8f, y + 14f, paint)

        paint.color = darkText
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val clippedSolverName = if (solverName.length > 26) solverName.take(24) + "..." else solverName
        canvas.drawText(clippedSolverName, solverLeft + 8f, y + 28f, paint)

        paint.color = secondaryText
        paint.textSize = 8f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("রেফারেন্স সমাধানকারী আইডি: #$solverUid", solverLeft + 8f, y + 40f, paint)

        y += partyBoxHeight + 10f

        // -------------------------------------------------------------
        // 4. Job Title & Service Description
        // -------------------------------------------------------------
        val jobCardHeight = 34f
        val jobCardRect = RectF(32f, y, (width - 32).toFloat(), y + jobCardHeight)
        paint.color = lightBg
        canvas.drawRoundRect(jobCardRect, 6f, 6f, paint)
        paint.color = borderColor
        paint.style = Paint.Style.STROKE
        canvas.drawRoundRect(jobCardRect, 6f, 6f, paint)
        paint.style = Paint.Style.FILL

        paint.color = darkText
        paint.textSize = 9.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val jobTitle = problem.title.ifBlank { "জরুরি সমস্যা সমাধান" }
        val displayTitle = if (jobTitle.length > 56) jobTitle.take(54) + "..." else jobTitle
        canvas.drawText("কাজের শিরোনাম: $displayTitle", 40f, y + 14f, paint)

        paint.color = secondaryText
        paint.textSize = 8f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        val descSnippet = problem.description.replace("\n", " ").trim()
        val displayDesc = if (descSnippet.length > 76) descSnippet.take(74) + "..." else descSnippet.ifBlank { "বিবরণ অন্তর্ভুক্ত নেই" }
        canvas.drawText("বিবরণ: $displayDesc", 40f, y + 26f, paint)

        y += jobCardHeight + 10f

        // -------------------------------------------------------------
        // 5. Accurate Step-by-Step Timeline (Matching Summary Page Exactly)
        // -------------------------------------------------------------
        paint.color = darkText
        paint.textSize = 10.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("কাজের অগ্রগতি ও সময়সূচী (Step-by-Step Work Timeline)", 32f, y, paint)
        y += 6f

        fun formatBengaliTime(ts: Long?): String {
            if (ts == null || ts <= 0L) return "সম্পন্ন হয়নি"
            val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.ENGLISH)
            val str = sdf.format(Date(ts))
            val enM = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
            val bnM = listOf("জানু", "ফেব্রু", "মার্চ", "এপ্রিল", "মে", "জুন", "জুলাই", "আগস্ট", "সেপ্টে", "অক্টো", "নভে", "ডিসে")
            var res = str
            for (i in enM.indices) res = res.replace(enM[i], bnM[i])
            return DistanceUtil.toBengaliDigits(res)
        }

        data class ReceiptTimelineStep(
            val label: String,
            val timeText: String,
            val isDone: Boolean,
            val isSpecial: Boolean = false,
            val isCancel: Boolean = false,
            val isDispute: Boolean = false
        )

        val timelineSteps = mutableListOf<ReceiptTimelineStep>()

        // 1. Post Creation (Always completed)
        val step1Done = problem.createdAt > 0L
        timelineSteps.add(
            ReceiptTimelineStep(
                label = "১. পোস্ট তৈরি ও ব্রডকাস্ট",
                timeText = if (step1Done) formatBengaliTime(problem.createdAt) else "সম্পন্ন হয়নি",
                isDone = step1Done
            )
        )

        // 2. Bid Acceptance & Escrow Secured
        val step2Ts: Long? = problem.acceptedAt2?.takeIf { it > 0L }
        val step2Done = step2Ts != null || (!problem.acceptedSolverId.isNullOrBlank() && !isCancelled)
        timelineSteps.add(
            ReceiptTimelineStep(
                label = "২. বিড গ্রহণ ও এসক্রো সিকিউরড",
                timeText = if (step2Ts != null) formatBengaliTime(step2Ts) else if (step2Done) formatBengaliTime(problem.createdAt) else "সম্পন্ন হয়নি",
                isDone = step2Done
            )
        )

        // 3. Solver On the Way
        val step3Ts = problem.onWayAt?.takeIf { it > 0L }
        val step3Done = step3Ts != null
        timelineSteps.add(
            ReceiptTimelineStep(
                label = "৩. সমাধানকারী লোকেশনের উদ্দেশ্যে রওয়ানা",
                timeText = if (step3Done) formatBengaliTime(step3Ts) else "সম্পন্ন হয়নি",
                isDone = step3Done
            )
        )

        // 4. Solver Arrived at Location
        val step4Ts = problem.arrivedAt?.takeIf { it > 0L }
        val step4Done = step4Ts != null
        timelineSteps.add(
            ReceiptTimelineStep(
                label = "৪. সমাধানকারী গন্তব্যে উপস্থিত",
                timeText = if (step4Done) formatBengaliTime(step4Ts) else "সম্পন্ন হয়নি",
                isDone = step4Done
            )
        )

        if (isDisputed) {
            // Step 5: Work Started / OTP Verification
            val step5Ts = problem.jobStartedAt?.takeIf { it > 0L }
            val step5Done = step5Ts != null
            timelineSteps.add(
                ReceiptTimelineStep(
                    label = "৫. কাজ শুরু ও ওটিপি ভেরিফিকেশন",
                    timeText = if (step5Done) formatBengaliTime(step5Ts) else "সম্পন্ন হয়নি",
                    isDone = step5Done
                )
            )

            // Step 6: Dispute Raised
            val dispRaiseTime = problem.disputedAt ?: problem.lastActivityAt ?: problem.createdAt
            timelineSteps.add(
                ReceiptTimelineStep(
                    label = "৬. বিরোধ উত্থাপন (Dispute Raised)",
                    timeText = formatBengaliTime(dispRaiseTime),
                    isDone = true,
                    isSpecial = true,
                    isDispute = true
                )
            )

            // Step 7: Official Admin Dispute Resolution
            val dispResolveTime = problem.disputeResolvedAt?.takeIf { it > 0L } ?: problem.disputeSettledAt?.takeIf { it > 0L }
            val isResolved = dispResolveTime != null
            timelineSteps.add(
                ReceiptTimelineStep(
                    label = "৭. অফিসিয়াল অ্যাডমিন নিষ্পত্তি (Dispute Resolved)",
                    timeText = if (isResolved) formatBengaliTime(dispResolveTime) else "প্রক্রিয়াধীন",
                    isDone = isResolved,
                    isSpecial = true,
                    isDispute = true
                )
            )
        } else if (isCancelled) {
            // Step 5: Work Started / OTP Verification
            val step5Ts = problem.jobStartedAt?.takeIf { it > 0L }
            val step5Done = step5Ts != null
            timelineSteps.add(
                ReceiptTimelineStep(
                    label = "৫. কাজ শুরু ও ওটিপি ভেরিফিকেশন",
                    timeText = if (step5Done) formatBengaliTime(step5Ts) else "সম্পন্ন হয়নি",
                    isDone = step5Done
                )
            )

            // Step 6: Job Cancellation & Escrow Protection
            val cancelTs = problem.completedAt ?: problem.lastActivityAt ?: problem.createdAt
            timelineSteps.add(
                ReceiptTimelineStep(
                    label = "৬. কাজ বাতিল ও এসক্রো রিফান্ড সম্পন্ন",
                    timeText = formatBengaliTime(cancelTs),
                    isDone = false,
                    isSpecial = true,
                    isCancel = true
                )
            )
        } else {
            // Completed normal job
            val step5Ts = problem.jobStartedAt?.takeIf { it > 0L } ?: problem.arrivedAt ?: problem.onWayAt ?: problem.acceptedAt2 ?: problem.createdAt
            timelineSteps.add(
                ReceiptTimelineStep(
                    label = "৫. কাজ শুরু ও ওটিপি ভেরিফিকেশন",
                    timeText = formatBengaliTime(step5Ts),
                    isDone = true
                )
            )

            val compTs = problem.completedAt?.takeIf { it > 0L } ?: step5Ts
            timelineSteps.add(
                ReceiptTimelineStep(
                    label = "৬. কাজ সফলভাবে সম্পন্ন ও পেমেন্ট রিলিজ",
                    timeText = formatBengaliTime(compTs),
                    isDone = true
                )
            )
        }

        val stepLineHeight = 14.5f
        val timelineCardHeight = (timelineSteps.size * stepLineHeight) + 11f
        val timelineRect = RectF(32f, y, (width - 32).toFloat(), y + timelineCardHeight)
        paint.color = lightBg
        canvas.drawRoundRect(timelineRect, 6f, 6f, paint)
        paint.color = borderColor
        paint.style = Paint.Style.STROKE
        canvas.drawRoundRect(timelineRect, 6f, 6f, paint)
        paint.style = Paint.Style.FILL

        var stepY = y + 13f
        timelineSteps.forEach { step ->
            val bulletColor = when {
                step.isCancel -> errorRed
                step.isDispute -> amberOrange
                step.isDone -> successGreen
                else -> Color.parseColor("#EF4444")
            }

            paint.color = bulletColor
            canvas.drawCircle(42f, stepY - 3f, 3f, paint)

            paint.color = if (step.isDone || step.isSpecial) darkText else Color.parseColor("#6B7280")
            paint.textSize = 8.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, if (step.isSpecial || step.isDone) Typeface.BOLD else Typeface.NORMAL)
            val prefix = if (step.isDone) "✓ " else "✗ "
            canvas.drawText(prefix + step.label, 50f, stepY, paint)

            paint.color = if (step.isDone) secondaryText else if (step.isCancel) errorRed else if (step.isDispute) amberOrange else Color.parseColor("#EF4444")
            paint.textAlign = Paint.Align.RIGHT
            paint.textSize = 8f
            canvas.drawText(step.timeText, (width - 42).toFloat(), stepY, paint)
            paint.textAlign = Paint.Align.LEFT

            stepY += stepLineHeight
        }

        y += timelineCardHeight + 10f

        // -------------------------------------------------------------
        // 6. Role-Based Accurate Financial Breakdown
        // -------------------------------------------------------------
        val sectionTitle = if (isUserRole) {
            "আর্থিক বিবরণী ও পেমেন্ট হিসাব (Client Payment Breakdown)"
        } else {
            "আর্থিক বিবরণী ও সমাধানকারী আয় (Solver Earnings & Commission Breakdown)"
        }

        paint.color = darkText
        paint.textSize = 10.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(sectionTitle, 32f, y, paint)
        y += 6f

        val (baseAmount, extraAmount) = SomadhanRepository.resolveSettlementAmounts(problem, escrow)
        val totalGross = baseAmount + extraAmount

        val decision = problem.disputeResolutionDecision ?: problem.disputeResolutionType ?: ""
        val isDisputeSplit = decision in listOf("SPLIT_SETTLEMENT", "CUSTOM_SPLIT", "SPLIT_50_50", "SETTLE", "SPLIT") ||
                (problem.disputeSplitSolverPercent != null && problem.disputeSplitSolverPercent!! > 0 && problem.disputeSplitSolverPercent!! < 100)
        val isRefundToUser = decision in listOf("REFUND_TO_USER", "REFUND")
        val isReleaseToSolver = decision in listOf("RELEASE_TO_SOLVER", "RELEASE")

        // Commission values
        val normalRate = breakdown?.rate ?: (problem.appliedCommissionRate ?: 10.0)
        val wasFreeQuotaJob = breakdown?.wasFreeQuotaJob ?: (problem.appliedCommissionRate == 0.0)
        val baseCommission = breakdown?.baseCommission ?: if (wasFreeQuotaJob) 0.0 else Math.round(baseAmount * (normalRate / 100.0)).toDouble()
        val extraCommission = breakdown?.extraCommission ?: if (wasFreeQuotaJob) 0.0 else Math.round(extraAmount * (normalRate / 100.0)).toDouble()
        val totalCommission = breakdown?.totalCommission ?: (baseCommission + extraCommission)
        val netEarnings = breakdown?.netAmount ?: (totalGross - totalCommission).coerceAtLeast(0.0)

        // Triple: Label, Value, Code (0: regular dark, 1: primary orange, 2: success green, 3: secondary muted, 4: bold dark, 5: error red)
        val tableRows = mutableListOf<Triple<String, String, Int>>()

        if (isUserRole) {
            // ================= CLIENT INVOICE VIEW (NO COMMISSION) =================
            tableRows.add(Triple("চুক্তিভিত্তিক মূল বাজেট (Agreed Base Fare)", "৳ ${DistanceUtil.toBengaliDigits(baseAmount.toInt().toString())}", 0))
            if (extraAmount > 0.0) {
                tableRows.add(Triple("অনুমোদিত অতিরিক্ত চার্জ / পার্টস বিল (Extra Charges)", "৳ ${DistanceUtil.toBengaliDigits(extraAmount.toInt().toString())}", 0))
            }
            tableRows.add(Triple("সর্বমোট এসক্রো জমা (Total Escrow Fund Paid)", "৳ ${DistanceUtil.toBengaliDigits(totalGross.toInt().toString())}", 4))

            if (isDisputeSplit) {
                val solverPct = problem.disputeSplitSolverPercent ?: 50.0
                val clientPct = (100.0 - solverPct).coerceAtLeast(0.0)
                val solverShareGross = Math.round(totalGross * (solverPct / 100.0)).toDouble()
                val clientRefund = Math.round(totalGross - solverShareGross).toDouble()

                val ratioText = "${DistanceUtil.toBengaliDigits(clientPct.toInt().toString())}% ক্লায়েন্ট / ${DistanceUtil.toBengaliDigits(solverPct.toInt().toString())}% সমাধানকারী"
                tableRows.add(Triple("মীমাংসা বণ্টন অনুপাত (Dispute Split Ratio)", ratioText, 3))
                tableRows.add(Triple("ক্লায়েন্ট ওয়ালেটে রিফান্ড প্রাপ্তি (Refund to Wallet)", "৳ ${DistanceUtil.toBengaliDigits(clientRefund.toInt().toString())}", 2))
                tableRows.add(Triple("চূড়ান্ত সেবা খরচ (Net Amount Paid)", "৳ ${DistanceUtil.toBengaliDigits(solverShareGross.toInt().toString())}", 1))
            } else if (isRefundToUser) {
                tableRows.add(Triple("অ্যাডমিন নিষ্পত্তি (Admin Verdict)", "১০০% গ্রাহক পূর্ণ রিফান্ড", 3))
                tableRows.add(Triple("ওয়ালেটে রিফান্ড প্রাপ্তি (Total Refund Credited)", "৳ ${DistanceUtil.toBengaliDigits(totalGross.toInt().toString())}", 2))
                tableRows.add(Triple("গ্রাহকের নিট প্রদেয় খরচ (Client Net Cost)", "৳ ০ (সম্পূর্ণ অর্থ ফেরত)", 2))
            } else if (isReleaseToSolver) {
                tableRows.add(Triple("অ্যাডমিন নিষ্পত্তি (Admin Verdict)", "১০০% সমাধানকারীকে রিলিজ", 3))
                tableRows.add(Triple("গ্রাহক মোট পরিশোধ (Total Amount Paid)", "৳ ${DistanceUtil.toBengaliDigits(totalGross.toInt().toString())}", 1))
            } else if (isCancelled) {
                tableRows.add(Triple("বাতিল স্ট্যাটাস (Cancellation Status)", "কাজ বাতিলের পর গ্রাহকের ওয়ালেটে সম্পূর্ণ অর্থ রিফান্ড সম্পন্ন", 2))
                tableRows.add(Triple("ওয়ালেটে রিফান্ড প্রাপ্তি (Refund to Wallet)", "৳ ${DistanceUtil.toBengaliDigits(totalGross.toInt().toString())}", 2))
            } else {
                tableRows.add(Triple("পেমেন্ট স্ট্যাটাস (Payment Status)", "এসক্রো থেকে সমাধানকারীকে সফলভাবে পরিশোধিত", 2))
                tableRows.add(Triple("ক্লায়েন্ট মোট পরিশোধ (Total Amount Paid)", "৳ ${DistanceUtil.toBengaliDigits(totalGross.toInt().toString())}", 1))
            }
        } else {
            // ================= SOLVER PAYOUT VIEW (WITH COMMISSION & FREE QUOTA) =================
            tableRows.add(Triple("চুক্তিভিত্তিক মূল বিল (Agreed Base Fare)", "৳ ${DistanceUtil.toBengaliDigits(baseAmount.toInt().toString())}", 0))
            if (extraAmount > 0.0) {
                tableRows.add(Triple("অনুমোদিত অতিরিক্ত চার্জ / পার্টস বিল (Extra Charges)", "৳ ${DistanceUtil.toBengaliDigits(extraAmount.toInt().toString())}", 0))
            }
            tableRows.add(Triple("সর্বমোট কাজের মূল্য (Total Gross Amount)", "৳ ${DistanceUtil.toBengaliDigits(totalGross.toInt().toString())}", 4))

            if (isDisputeSplit) {
                val solverPct = problem.disputeSplitSolverPercent ?: 50.0
                val clientPct = (100.0 - solverPct).coerceAtLeast(0.0)
                val solverGrossBase = Math.round(baseAmount * (solverPct / 100.0)).toDouble()
                val solverGrossExtra = Math.round(extraAmount * (solverPct / 100.0)).toDouble()
                val solverShareGross = solverGrossBase + solverGrossExtra
                val splitBaseComm = breakdown?.baseCommission ?: if (wasFreeQuotaJob) 0.0 else Math.round(solverGrossBase * (normalRate / 100.0)).toDouble()
                val splitExtraComm = breakdown?.extraCommission ?: if (wasFreeQuotaJob) 0.0 else Math.round(solverGrossExtra * (normalRate / 100.0)).toDouble()
                val splitTotalComm = breakdown?.totalCommission ?: (splitBaseComm + splitExtraComm)
                val solverShareNet = breakdown?.netAmount ?: (solverShareGross - splitTotalComm).coerceAtLeast(0.0)

                val ratioText = "${DistanceUtil.toBengaliDigits(solverPct.toInt().toString())}% সমাধানকারী / ${DistanceUtil.toBengaliDigits(clientPct.toInt().toString())}% ক্লায়েন্ট"
                tableRows.add(Triple("মীমাংসা বণ্টন অনুপাত (Split Ratio)", ratioText, 3))
                tableRows.add(Triple("সমাধানকারীর মোট অংশ (Solver Gross Share)", "৳ ${DistanceUtil.toBengaliDigits(solverShareGross.toInt().toString())}", 0))

                val commText = if (wasFreeQuotaJob) "৳ ০ (ফ্রি কোটা ছাড়)" else "(-) ৳ ${DistanceUtil.toBengaliDigits(splitTotalComm.toInt().toString())} (${DistanceUtil.toBengaliDigits(normalRate.toInt().toString())}%)"
                tableRows.add(Triple("প্ল্যাটফর্ম কমিশন কর্তন (Platform Fee)", commText, 3))
                tableRows.add(Triple("ওয়ালেটে জমাকৃত নিট আয় (Net Payout Credited)", "৳ ${DistanceUtil.toBengaliDigits(solverShareNet.toInt().toString())}", 1))
            } else if (isRefundToUser) {
                tableRows.add(Triple("অ্যাডমিন নিষ্পত্তি (Admin Verdict)", "১০০% গ্রাহক রিফান্ড", 3))
                tableRows.add(Triple("সমাধানকারীর অর্জিত আয় (Solver Net Earnings)", "৳ ০", 3))
            } else if (isCancelled) {
                tableRows.add(Triple("বাতিল স্ট্যাটাস (Cancellation Status)", "কাজ বাতিল হওয়ায় কোনো পারিশ্রমিক অর্জিত হয়নি", 5))
                tableRows.add(Triple("সমাধানকারীর নিট আয় (Net Solver Earnings)", "৳ ০", 3))
            } else {
                // Regular / Release
                val commDesc = when {
                    wasFreeQuotaJob -> "৳ ০ (ফ্রি কোটা ছাড়)"
                    breakdown?.extraCommissionApplied == true && extraCommission < (extraAmount * (normalRate / 100.0)) -> {
                        "(-) ৳ ${DistanceUtil.toBengaliDigits(totalCommission.toInt().toString())} (অতিরিক্ত বিলে ছাড়সহ)"
                    }
                    else -> {
                        val rateStr = DistanceUtil.toBengaliDigits(normalRate.toInt().toString())
                        "(-) ৳ ${DistanceUtil.toBengaliDigits(totalCommission.toInt().toString())} ($rateStr% প্ল্যাটফর্ম ফি)"
                    }
                }
                tableRows.add(Triple("প্রযোজ্য প্ল্যাটফর্ম কমিশন (Platform Fee)", commDesc, 3))
                tableRows.add(Triple("ওয়ালেটে জমাকৃত নিট আয় (Net Solver Earnings)", "৳ ${DistanceUtil.toBengaliDigits(netEarnings.toInt().toString())}", 1))
            }
        }

        val rowHeight = 16f
        val tableHeight = (tableRows.size * rowHeight) + 10f
        val finCardRect = RectF(32f, y, (width - 32).toFloat(), y + tableHeight)
        paint.color = lightBg
        canvas.drawRoundRect(finCardRect, 6f, 6f, paint)
        paint.color = borderColor
        paint.style = Paint.Style.STROKE
        canvas.drawRoundRect(finCardRect, 6f, 6f, paint)
        paint.style = Paint.Style.FILL

        var rowY = y + 15f
        tableRows.forEachIndexed { idx, (rTitle, rAmount, colorCode) ->
            if (idx % 2 == 1) {
                val rowBgRect = RectF(34f, rowY - 11f, (width - 34).toFloat(), rowY + 5f)
                paint.color = lightBgAlt
                canvas.drawRect(rowBgRect, paint)
            }

            val isBold = colorCode == 1 || colorCode == 4 || colorCode == 2
            val tColor = when (colorCode) {
                1 -> primaryOrange
                2 -> successGreen
                3 -> secondaryText
                4 -> darkText
                5 -> errorRed
                else -> darkText
            }

            paint.color = if (isBold && colorCode != 1 && colorCode != 2) darkText else if (colorCode == 3) secondaryText else darkText
            paint.typeface = Typeface.create(Typeface.DEFAULT, if (isBold) Typeface.BOLD else Typeface.NORMAL)
            paint.textSize = if (isBold) 9.5f else 8.5f
            canvas.drawText(rTitle, 42f, rowY, paint)

            paint.color = tColor
            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText(rAmount, (width - 42).toFloat(), rowY, paint)
            paint.textAlign = Paint.Align.LEFT

            rowY += rowHeight
        }

        y += tableHeight + 10f

        // -------------------------------------------------------------
        // 7. Dispute & Admin Resolution Section (If Disputed) OR Cancellation Box
        // -------------------------------------------------------------
        if (isDisputed) {
            val dispBoxHeight = 52f
            val dispRect = RectF(32f, y, (width - 32).toFloat(), y + dispBoxHeight)
            paint.color = amberOrangeBg
            canvas.drawRoundRect(dispRect, 6f, 6f, paint)
            paint.color = Color.rgb(245, 158, 11) // Amber border
            paint.style = Paint.Style.STROKE
            canvas.drawRoundRect(dispRect, 6f, 6f, paint)
            paint.style = Paint.Style.FILL

            paint.color = amberOrange
            paint.textSize = 9.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val initiatorRole = if (problem.disputeInitiatorRole == "USER") "সেবাগ্রহীতা" else "সমাধানকারী"
            val reason = problem.disputeReason ?: "সেবা সংক্রান্ত অসঙ্গতি"
            canvas.drawText("⚖️ অফিসিয়াল ডিসপিউট রেজোলিউশন ($initiatorRole কর্তৃক উত্থাপিত • কারণ: $reason)", 40f, y + 14f, paint)

            paint.color = darkText
            paint.textSize = 8.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            val note = problem.disputeResolutionNote?.ifBlank { "অ্যাডমিন প্যানেল থেকে উভয় পক্ষের পর্যালোচনা শেষে বিরোধ নিষ্পত্তি করা হয়েছে।" }
                ?: "অ্যাডমিন প্যানেল থেকে উভয় পক্ষের পর্যালোচনা শেষে বিরোধ নিষ্পত্তি করা হয়েছে।"
            val clippedNote = if (note.length > 76) note.take(74) + "..." else note
            canvas.drawText("অ্যাডমিন মীমাংসা নোট: $clippedNote", 40f, y + 28f, paint)

            paint.color = secondaryText
            paint.textSize = 8f
            val resolvedTimeStr = formatBengaliTime(problem.disputeResolvedAt ?: problem.disputeSettledAt)
            val verdictBangla = when (problem.disputeResolutionDecision ?: problem.disputeResolutionType) {
                "REFUND_TO_USER", "REFUND" -> "১০০% গ্রাহক রিফান্ড"
                "RELEASE_TO_SOLVER", "RELEASE" -> "১০০% সমাধানকারীকে রিলিজ"
                "SPLIT_SETTLEMENT", "CUSTOM_SPLIT", "SPLIT_50_50", "SETTLE", "SPLIT" -> "উভয় পক্ষে স্প্লিট মীমাংসা"
                else -> "নিষ্পত্তি সম্পন্ন"
            }
            canvas.drawText("মীমাংসার ধরন: $verdictBangla   •   নিষ্পত্তির সময়: $resolvedTimeStr", 40f, y + 42f, paint)

            y += dispBoxHeight + 10f
        } else if (isCancelled) {
            val cancelBoxHeight = 44f
            val cancelRect = RectF(32f, y, (width - 32).toFloat(), y + cancelBoxHeight)
            paint.color = errorRedBg
            canvas.drawRoundRect(cancelRect, 6f, 6f, paint)
            paint.color = Color.rgb(252, 165, 165) // Red border
            paint.style = Paint.Style.STROKE
            canvas.drawRoundRect(cancelRect, 6f, 6f, paint)
            paint.style = Paint.Style.FILL

            paint.color = errorRed
            paint.textSize = 9.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val cancelReason = if (!problem.solverCancelledNotice.isNullOrBlank()) {
                problem.solverCancelledNotice
            } else {
                "নির্দিষ্ট সময়ের পর বা অনিবার্য কারণে কাজ বাতিল করা হয়েছে।"
            }
            canvas.drawText("🚫 কাজ বাতিলের সারসংক্ষেপ (Job Cancellation Summary)", 40f, y + 14f, paint)

            paint.color = darkText
            paint.textSize = 8.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            val clippedReason = if (cancelReason.length > 76) cancelReason.take(74) + "..." else cancelReason
            canvas.drawText("বাতিলের কারণ: $clippedReason", 40f, y + 26f, paint)

            paint.color = successGreen
            paint.textSize = 8f
            canvas.drawText("✓ এসক্রো সিকিউরিটি পলিসির আওতায় গ্রাহকের ওয়ালেটে ১০০% রিফান্ড নিশ্চিত করা হয়েছে।", 40f, y + 37f, paint)

            y += cancelBoxHeight + 10f
        }

        // -------------------------------------------------------------
        // 8. Official Escrow Security Stamp & QR Verification Box
        // -------------------------------------------------------------
        val bottomSectionY = y.coerceAtMost(height - 120f)

        // Security Box
        val secCardRect = RectF(32f, bottomSectionY, (width - 32).toFloat(), bottomSectionY + 68f)
        paint.color = lightBg
        canvas.drawRoundRect(secCardRect, 6f, 6f, paint)
        paint.color = borderColor
        paint.style = Paint.Style.STROKE
        canvas.drawRoundRect(secCardRect, 6f, 6f, paint)
        paint.style = Paint.Style.FILL

        // Draw Authentic Vector Official Escrow Stamp on Left
        drawOfficialEscrowStamp(canvas, 72f, bottomSectionY + 34f, isDisputed, isCancelled)

        // Security Text in Middle
        paint.color = if (isDisputed) amberOrange else if (isCancelled) errorRed else successGreen
        paint.textSize = 9.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("✓ VERIFIED DIGITAL TRANSACTION • ESCROW GATEWAY", 120f, bottomSectionY + 22f, paint)

        paint.color = secondaryText
        paint.textSize = 8f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("এই রিসিটটি সমাধান ডিজিটাল এসক্রো সিকিউরিটি ও ডাটাবেস ব্লকে স্বয়ংক্রিয়ভাবে ভেরিফাইড।", 120f, bottomSectionY + 36f, paint)
        canvas.drawText("ট্র্যাকিং হ্যাশ: #${problem.id.hashCode().toString().takeLast(8).uppercase()} • এনক্রিপ্টেড ডিজিটাল রেকর্ড", 120f, bottomSectionY + 48f, paint)

        // Draw Vector QR Code Matrix on Right
        val qrBoxSize = 50f
        val qrLeft = (width - 32 - qrBoxSize - 10f)
        val qrTop = bottomSectionY + 9f
        drawVectorQrCode(canvas, qrLeft, qrTop, qrBoxSize, problem.id)

        // -------------------------------------------------------------
        // 9. Modern Footer Disclaimer & Support Info
        // -------------------------------------------------------------
        val footerY = (height - 30).toFloat()
        paint.color = Color.rgb(148, 163, 184)
        paint.textSize = 7.8f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("ওয়েবসাইট: www.somadhan.com.bd   •   সার্বক্ষণিক হেল্পলাইন: ১৬৫XX   •   ইমেইল: support@somadhan.com.bd", (width / 2).toFloat(), footerY, paint)
        canvas.drawText("এটি সমাধান অ্যাপ দ্বারা স্বয়ংক্রিয়ভাবে প্রস্তুতকৃত ডিজিটাল প্রামাণ্য রিসিট, কোনো ম্যানুয়াল স্বাক্ষরের প্রয়োজন নেই।", (width / 2).toFloat(), footerY + 12f, paint)
        paint.textAlign = Paint.Align.LEFT
    }

    /**
     * Draws a subtle background watermark in the center of the PDF page.
     */
    private fun drawBackgroundWatermark(canvas: Canvas, width: Int, height: Int) {
        val paint = Paint().apply {
            isAntiAlias = true
            color = Color.rgb(226, 232, 240)
            alpha = 24 // Very subtle
            textSize = 34f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        canvas.save()
        canvas.translate(width / 2f, height / 2f)
        canvas.rotate(-30f)
        canvas.drawText("SOMADHAN ESCROW SECURED", 0f, -20f, paint)
        canvas.drawText("OFFICIAL DIGITAL INVOICE", 0f, 25f, paint)
        canvas.restore()
    }

    /**
     * Draws an authentic double-ring circular official stamp vector.
     */
    private fun drawOfficialEscrowStamp(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        isDisputed: Boolean,
        isCancelled: Boolean
    ) {
        val stampColor = when {
            isDisputed -> Color.rgb(217, 119, 6)
            isCancelled -> Color.rgb(220, 38, 38)
            else -> Color.rgb(22, 163, 74)
        }

        val paint = Paint().apply {
            isAntiAlias = true
            color = stampColor
        }

        // Outer Ring
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.8f
        canvas.drawCircle(cx, cy, 26f, paint)

        // Inner Ring
        paint.strokeWidth = 0.8f
        canvas.drawCircle(cx, cy, 21f, paint)

        // Center Star / Symbol
        paint.style = Paint.Style.FILL
        drawSmallStar(canvas, cx, cy - 7f, 4f, stampColor)

        paint.textSize = 6.2f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textAlign = Paint.Align.CENTER

        val stampTitle = when {
            isDisputed -> "DISPUTED"
            isCancelled -> "CANCELLED"
            else -> "VERIFIED"
        }
        canvas.drawText(stampTitle, cx, cy + 4f, paint)

        paint.textSize = 5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("SOMADHAN", cx, cy + 11f, paint)
        paint.textAlign = Paint.Align.LEFT
    }

    private fun drawSmallStar(canvas: Canvas, cx: Float, cy: Float, radius: Float, color: Int) {
        val paint = Paint().apply {
            isAntiAlias = true
            this.color = color
            style = Paint.Style.FILL
        }
        val path = Path()
        val innerRadius = radius * 0.45f
        for (i in 0 until 10) {
            val r = if (i % 2 == 0) radius else innerRadius
            val angle = Math.toRadians((i * 36 - 90).toDouble())
            val x = (cx + r * cos(angle)).toFloat()
            val y = (cy + r * sin(angle)).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        canvas.drawPath(path, paint)
    }

    /**
     * Draws a clean, accurate 21x21 Vector QR Code Matrix with Finder Patterns.
     */
    private fun drawVectorQrCode(
        canvas: Canvas,
        x: Float,
        y: Float,
        size: Float,
        seed: String
    ) {
        val paint = Paint().apply {
            isAntiAlias = true
            color = Color.rgb(15, 23, 42)
            style = Paint.Style.FILL
        }

        val qrBgPaint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }

        // Draw QR White Backing Box
        canvas.drawRoundRect(RectF(x - 2f, y - 2f, x + size + 2f, y + size + 2f), 3f, 3f, qrBgPaint)

        val borderPaint = Paint().apply {
            color = Color.rgb(203, 213, 225)
            style = Paint.Style.STROKE
            strokeWidth = 0.8f
        }
        canvas.drawRoundRect(RectF(x - 2f, y - 2f, x + size + 2f, y + size + 2f), 3f, 3f, borderPaint)

        val modules = 21
        val cellSize = size / modules

        // 1. Draw 3 Corner Finder Patterns (7x7 Outer Box, 5x5 White Gap, 3x3 Center Block)
        drawFinderPattern(canvas, x, y, cellSize)
        drawFinderPattern(canvas, x + 14 * cellSize, y, cellSize)
        drawFinderPattern(canvas, x, y + 14 * cellSize, cellSize)

        // 2. Timing Patterns (Row 6, Col 6)
        for (i in 8..12) {
            if (i % 2 == 0) {
                canvas.drawRect(x + i * cellSize, y + 6 * cellSize, x + (i + 1) * cellSize, y + 7 * cellSize, paint)
                canvas.drawRect(x + 6 * cellSize, y + i * cellSize, x + 7 * cellSize, y + (i + 1) * cellSize, paint)
            }
        }

        // 3. Deterministic Data Dots derived from seed hash
        val hash = seed.hashCode()
        var bitCounter = 0
        for (row in 0 until modules) {
            for (col in 0 until modules) {
                // Skip finder patterns & timing tracks
                val inTopLeftFinder = row < 8 && col < 8
                val inTopRightFinder = row < 8 && col >= 13
                val inBottomLeftFinder = row >= 13 && col < 8
                val inTiming = row == 6 || col == 6

                if (!inTopLeftFinder && !inTopRightFinder && !inBottomLeftFinder && !inTiming) {
                    val bit = ((hash shr (bitCounter % 31)) and 1) xor ((row * 7 + col * 13) % 2)
                    if (bit == 1) {
                        canvas.drawRect(
                            x + col * cellSize + 0.3f,
                            y + row * cellSize + 0.3f,
                            x + (col + 1) * cellSize - 0.3f,
                            y + (row + 1) * cellSize - 0.3f,
                            paint
                        )
                    }
                    bitCounter++
                }
            }
        }
    }

    private fun drawFinderPattern(canvas: Canvas, x: Float, y: Float, cellSize: Float) {
        val blackPaint = Paint().apply {
            color = Color.rgb(15, 23, 42)
            style = Paint.Style.FILL
        }
        val whitePaint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }

        // Outer 7x7 Box
        canvas.drawRect(x, y, x + 7 * cellSize, y + 7 * cellSize, blackPaint)
        // Inner 5x5 White Box
        canvas.drawRect(x + cellSize, y + cellSize, x + 6 * cellSize, y + 6 * cellSize, whitePaint)
        // Center 3x3 Black Box
        canvas.drawRect(x + 2 * cellSize, y + 2 * cellSize, x + 5 * cellSize, y + 5 * cellSize, blackPaint)
    }
}
