@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.AppDatabase
import com.example.data.remote.SupabaseSyncManager
import com.example.ui.theme.SomadhanBg
import com.example.ui.theme.SomadhanBorder
import com.example.ui.theme.SomadhanCardBg
import com.example.ui.theme.SomadhanDivider
import com.example.ui.theme.SomadhanError
import com.example.ui.theme.SomadhanInfo
import com.example.ui.theme.SomadhanOrange
import com.example.ui.theme.SomadhanSuccess
import com.example.ui.theme.SomadhanTextHint
import com.example.ui.theme.SomadhanTextPrimary
import com.example.ui.theme.SomadhanTextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * READ-ONLY Refund Diagnostic Debug Viewer
 * Fetches and displays Room and Supabase (cloud) records for a specific problemId without
 * modifying any data.
 *
 * [SUPABASE-MIGRATED - ধাপ ১৮] আগে এই ফাইল সরাসরি `FirebaseSyncManager.requireDb()` দিয়ে
 * Firestore `escrows`/`transactions` collection থেকে raw document পড়তো (একমাত্র active
 * Firebase ব্যবহার, বাকি ২৩টা Admin screen ফাইলে যা শুধু dead import ছিল)। এখন
 * `SupabaseSyncManager.getEscrowById()`/`getTransactionById()` দিয়ে Postgrest row lookup করে।
 */
@Composable
fun AdminRefundDebugView() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    var problemIdInput by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var diagnosticResultJson by remember { mutableStateOf<String?>(null) }
    var diagnosticSummary by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US) }

    fun runDiagnostic() {
        val pid = problemIdInput.trim()
        if (pid.isBlank()) {
            errorMessage = "অনুগ্রহ করে একটি Problem ID লিখুন।"
            return
        }

        errorMessage = null
        isLoading = true
        diagnosticResultJson = null
        diagnosticSummary = null

        coroutineScope.launch {
            try {
                val reportJson = withContext(Dispatchers.IO) {
                    val db = AppDatabase.getDatabase(context)

                    val rootObj = JSONObject()
                    rootObj.put("queryProblemId", pid)
                    rootObj.put("queryTimestampMs", System.currentTimeMillis())
                    rootObj.put("queryTimestampFormatted", dateFormat.format(Date(System.currentTimeMillis())))

                    // 1. (ক) Room: EscrowEntity rows for problemId
                    val allProblemEscrows = db.escrowDao().getAllByProblemId(pid).sortedBy { it.createdAt }
                    val roomEscrowsArray = JSONArray()
                    for (esc in allProblemEscrows) {
                        val escObj = JSONObject()
                        escObj.put("id", esc.id)
                        escObj.put("problemId", esc.problemId)
                        escObj.put("problemTitle", esc.problemTitle)
                        escObj.put("userId", esc.userId)
                        escObj.put("solverId", esc.solverId)
                        escObj.put("status", esc.status)
                        escObj.put("baseAmount", esc.baseAmount)
                        escObj.put("extraAmount", esc.extraAmount)
                        escObj.put("totalAmount", esc.baseAmount + esc.extraAmount)
                        escObj.put("createdAtMs", esc.createdAt)
                        escObj.put("createdAtFormatted", if (esc.createdAt > 0) dateFormat.format(Date(esc.createdAt)) else "N/A")
                        escObj.put("releasedAtMs", esc.releasedAt ?: JSONObject.NULL)
                        val relAt = esc.releasedAt
                        escObj.put("releasedAtFormatted", if (relAt != null && relAt > 0) dateFormat.format(Date(relAt)) else "N/A")
                        roomEscrowsArray.put(escObj)
                    }
                    rootObj.put("1_room_escrows_count", allProblemEscrows.size)
                    rootObj.put("1_room_escrows", roomEscrowsArray)

                    // 2. (খ) Room: REFUND transactions for problemId
                    val problemTransactions = db.transactionDao().getTransactionsForProblem(pid)
                    val allTransactions = db.transactionDao().getAllTransactionsList()
                    val refundTransactions = (problemTransactions + allTransactions.filter { 
                        it.problemId == pid || (it.escrowId.isNotBlank() && allProblemEscrows.any { esc -> esc.id == it.escrowId })
                    }).distinctBy { it.id }.filter { trx ->
                        trx.type.equals("REFUND", ignoreCase = true) || trx.id.contains("REFUND", ignoreCase = true)
                    }.sortedBy { it.timestamp }

                    val roomRefundsArray = JSONArray()
                    for (trx in refundTransactions) {
                        val trxObj = JSONObject()
                        trxObj.put("id", trx.id)
                        trxObj.put("problemId", trx.problemId)
                        trxObj.put("problemTitle", trx.problemTitle)
                        trxObj.put("escrowId", trx.escrowId)
                        trxObj.put("userId", trx.userId)
                        trxObj.put("solverId", trx.solverId)
                        trxObj.put("type", trx.type)
                        trxObj.put("grossAmount", trx.grossAmount)
                        trxObj.put("netAmount", trx.netAmount)
                        trxObj.put("commissionAmount", trx.commissionAmount)
                        trxObj.put("baseAmount", trx.baseAmount)
                        trxObj.put("extraAmount", trx.extraAmount)
                        trxObj.put("timestampMs", trx.timestamp)
                        trxObj.put("timestampFormatted", if (trx.timestamp > 0) dateFormat.format(Date(trx.timestamp)) else "N/A")
                        trxObj.put("pendingCloudSync", trx.pendingCloudSync)
                        trxObj.put("cloudBalanceSynced", trx.cloudBalanceSynced)
                        roomRefundsArray.put(trxObj)
                    }
                    rootObj.put("2_room_refund_transactions_count", refundTransactions.size)
                    rootObj.put("2_room_refund_transactions", roomRefundsArray)

                    // 3. (গ) Supabase: escrows টেবিলের row for found escrow IDs + fallback
                    // [SUPABASE-MIGRATED - ধাপ ১৮] আগে Firestore `escrows` collection থেকে raw
                    // document পড়া হতো; এখন SupabaseSyncManager.getEscrowById() দিয়ে Postgrest row।
                    val cloudEscrowsArray = JSONArray()
                    val escrowIdsToCheck = mutableSetOf<String>()
                    allProblemEscrows.forEach { escrowIdsToCheck.add(it.id) }
                    escrowIdsToCheck.add(pid) // also check if the row is keyed by problemId

                    for (escId in escrowIdsToCheck) {
                        val escDocObj = JSONObject()
                        escDocObj.put("escrowId", escId)
                        try {
                            val result = SupabaseSyncManager.getEscrowById(escId)
                            val cloudEscrow = result.getOrNull()
                            escDocObj.put("existsInSupabase", cloudEscrow != null)
                            if (cloudEscrow != null) {
                                escDocObj.put("status", cloudEscrow.status)
                                escDocObj.put("releasedAt_raw", cloudEscrow.releasedAt ?: "null")
                                escDocObj.put("createdAt_raw", cloudEscrow.createdAt ?: "null")
                                escDocObj.put("baseAmount", cloudEscrow.baseAmount.toString())
                                escDocObj.put("extraAmount", cloudEscrow.extraAmount.toString())
                                escDocObj.put("userId", cloudEscrow.userId)
                                escDocObj.put("solverId", cloudEscrow.solverId)
                                escDocObj.put("problemId", cloudEscrow.problemId)

                                // Add all other raw fields (full DTO dump)
                                val fullDataObj = JSONObject()
                                fullDataObj.put("id", cloudEscrow.id)
                                fullDataObj.put("problem_id", cloudEscrow.problemId)
                                fullDataObj.put("problem_title", cloudEscrow.problemTitle)
                                fullDataObj.put("user_id", cloudEscrow.userId)
                                fullDataObj.put("solver_id", cloudEscrow.solverId)
                                fullDataObj.put("base_amount", cloudEscrow.baseAmount)
                                fullDataObj.put("extra_amount", cloudEscrow.extraAmount)
                                fullDataObj.put("status", cloudEscrow.status)
                                fullDataObj.put("created_at", cloudEscrow.createdAt ?: "null")
                                fullDataObj.put("released_at", cloudEscrow.releasedAt ?: "null")
                                fullDataObj.put("updated_at", cloudEscrow.updatedAt ?: "null")
                                escDocObj.put("all_row_fields", fullDataObj)
                            } else {
                                result.exceptionOrNull()?.let { e ->
                                    escDocObj.put("fetchError", e.message ?: "Unknown error")
                                }
                            }
                        } catch (e: Exception) {
                            escDocObj.put("existsInSupabase", false)
                            escDocObj.put("fetchError", e.message ?: "Unknown error")
                        }
                        cloudEscrowsArray.put(escDocObj)
                    }
                    rootObj.put("3_supabase_escrows", cloudEscrowsArray)

                    // 4. (ঘ) Supabase: transactions টেবিলের row for REFUND transaction IDs
                    // [SUPABASE-MIGRATED - ধাপ ১৮] আগে Firestore `transactions` collection থেকে raw
                    // document পড়া হতো (document id দিয়ে); এখন SupabaseSyncManager.getTransactionById()
                    // দিয়ে Postgrest row lookup — `id` কলামটা Room/Firestore/Supabase তিন জায়গাতেই
                    // একই deterministic transaction id হিসেবে ব্যবহৃত হয়, তাই lookup key অপরিবর্তিত।
                    val cloudRefundsArray = JSONArray()
                    val refundIdsToCheck = mutableSetOf<String>()
                    refundTransactions.forEach { refundIdsToCheck.add(it.id) }
                    refundIdsToCheck.add("TRX_REFUND_$pid")
                    allProblemEscrows.forEach { refundIdsToCheck.add("TRX_REFUND_${it.id}") }

                    for (trxId in refundIdsToCheck) {
                        val trxDocObj = JSONObject()
                        trxDocObj.put("transactionId", trxId)
                        try {
                            val result = SupabaseSyncManager.getTransactionById(trxId)
                            val cloudTrx = result.getOrNull()
                            trxDocObj.put("existsInSupabase", cloudTrx != null)
                            if (cloudTrx != null) {
                                trxDocObj.put("type", cloudTrx.type)
                                trxDocObj.put("netAmount", cloudTrx.netAmount.toString())
                                trxDocObj.put("problemId", cloudTrx.problemId)
                                trxDocObj.put("escrowId", cloudTrx.escrowId ?: "")
                                trxDocObj.put("userId", cloudTrx.userId ?: "")
                                trxDocObj.put("timestamp_raw", cloudTrx.timestamp ?: "null")

                                // Add all other raw fields (full DTO dump)
                                val fullDataObj = JSONObject()
                                fullDataObj.put("id", cloudTrx.id)
                                fullDataObj.put("problem_id", cloudTrx.problemId)
                                fullDataObj.put("problem_title", cloudTrx.problemTitle)
                                fullDataObj.put("user_id", cloudTrx.userId ?: "null")
                                fullDataObj.put("solver_id", cloudTrx.solverId ?: "null")
                                fullDataObj.put("gross_amount", cloudTrx.grossAmount)
                                fullDataObj.put("commission_percent", cloudTrx.commissionPercent)
                                fullDataObj.put("commission_amount", cloudTrx.commissionAmount)
                                fullDataObj.put("net_amount", cloudTrx.netAmount)
                                fullDataObj.put("timestamp", cloudTrx.timestamp ?: "null")
                                fullDataObj.put("base_amount", cloudTrx.baseAmount)
                                fullDataObj.put("extra_amount", cloudTrx.extraAmount)
                                fullDataObj.put("type", cloudTrx.type)
                                fullDataObj.put("escrow_id", cloudTrx.escrowId ?: "null")
                                fullDataObj.put("refund_type", cloudTrx.refundType)
                                fullDataObj.put("refund_percentage", cloudTrx.refundPercentage)
                                fullDataObj.put("release_type", cloudTrx.releaseType)
                                trxDocObj.put("all_row_fields", fullDataObj)
                            } else {
                                result.exceptionOrNull()?.let { e ->
                                    trxDocObj.put("fetchError", e.message ?: "Unknown error")
                                }
                            }
                        } catch (e: Exception) {
                            trxDocObj.put("existsInSupabase", false)
                            trxDocObj.put("fetchError", e.message ?: "Unknown error")
                        }
                        cloudRefundsArray.put(trxDocObj)
                    }
                    rootObj.put("4_supabase_refund_transactions", cloudRefundsArray)

                    rootObj.toString(2)
                }

                diagnosticResultJson = reportJson
                diagnosticSummary = "ডায়াগনস্টিক সফল হয়েছে। নিচে Room ও Supabase-এর সমস্ত Raw ডেটা দেখতে পাচ্ছেন।"
            } catch (e: Exception) {
                errorMessage = "ডায়াগনস্টিক ব্যর্থ হয়েছে: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SomadhanBg)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Card(
            colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
            border = androidx.compose.foundation.BorderStroke(1.dp, SomadhanBorder),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.BugReport,
                        contentDescription = null,
                        tint = SomadhanOrange,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Refund & Escrow Diagnostic Viewer (Read-Only)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = SomadhanTextPrimary
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "এই টুলটি সম্পূর্ণ READ-ONLY। কোনো ডেটাবেসে পরিবর্তন না করে সরাসরি Room এবং Supabase থেকে নির্দিষ্ট problemId-র সব Escrow ও Refund Transaction-এর লাইভ ভ্যালু প্রদর্শন করে।",
                    fontSize = 12.sp,
                    color = SomadhanTextSecondary,
                    lineHeight = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Input Section
        Card(
            colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
            border = androidx.compose.foundation.BorderStroke(1.dp, SomadhanBorder),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Problem ID দিন:",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = SomadhanTextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = problemIdInput,
                        onValueChange = { problemIdInput = it },
                        placeholder = { Text("যেমন: PROB_1700000000000_abcd বা 123", fontSize = 13.sp, color = SomadhanTextHint) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("refund_debug_problem_id_input"),
                        singleLine = true,
                        trailingIcon = {
                            if (problemIdInput.isNotBlank()) {
                                IconButton(onClick = { problemIdInput = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear", tint = SomadhanTextSecondary)
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { runDiagnostic() }),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SomadhanOrange,
                            unfocusedBorderColor = SomadhanBorder
                        )
                    )

                    Button(
                        onClick = { runDiagnostic() },
                        enabled = !isLoading && problemIdInput.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = SomadhanOrange),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .height(52.dp)
                            .testTag("refund_debug_run_button")
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                Text("চেক করুন", fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage ?: "",
                        color = SomadhanError,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Diagnostic Results
        if (diagnosticResultJson != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
                border = androidx.compose.foundation.BorderStroke(1.dp, SomadhanSuccess.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = SomadhanSuccess, modifier = Modifier.size(20.dp))
                            Text(
                                text = "ডায়াগনস্টিক রিপোর্ট (JSON)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = SomadhanTextPrimary
                            )
                        }

                        Button(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(diagnosticResultJson ?: ""))
                                Toast.makeText(context, "রিপোর্ট ক্লিপবোর্ডে কপি করা হয়েছে!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SomadhanOrange),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.testTag("refund_debug_copy_button")
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("কপি করুন", fontSize = 12.sp, color = Color.White)
                        }
                    }

                    if (diagnosticSummary != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = diagnosticSummary ?: "",
                            fontSize = 12.sp,
                            color = SomadhanSuccess,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(color = SomadhanDivider, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Text display box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF1E1E1E), shape = RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0xFF333333), shape = RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        SelectionContainer {
                            Text(
                                text = diagnosticResultJson ?: "",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = Color(0xFFD4D4D4),
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
