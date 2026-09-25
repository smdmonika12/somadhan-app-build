@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.remote.SupabaseSyncManager
import com.example.ui.theme.SomadhanBg
import com.example.ui.theme.SomadhanBorder
import com.example.ui.theme.SomadhanCardBg
import com.example.ui.theme.SomadhanDivider
import com.example.ui.theme.SomadhanError
import com.example.ui.theme.SomadhanErrorLight
import com.example.ui.theme.SomadhanOrange
import com.example.ui.theme.SomadhanSuccess
import com.example.ui.theme.SomadhanTextHint
import com.example.ui.theme.SomadhanTextPrimary
import com.example.ui.theme.SomadhanTextSecondary
import com.example.ui.viewmodel.SomadhanViewModel
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import com.example.ui.components.BottomSlideAlertDialog

/**
 * সমাধান (Somadhan) — Supabase migration ধাপ ১৯
 *
 * [SUPABASE-MIGRATED - ধাপ ১৯] পুরনো `AdminFirestoreExplorerView.kt` (৮৩টা Firebase reference,
 * ~২৯৬৬ লাইন) এর Supabase-সমতুল্য replacement। সেই ফাইলটা raw Firestore collection/document
 * browse/edit/delete করতো (schemaless, real-time listener দিয়ে); Postgrest-এ কোনো সরাসরি
 * built-in সমতুল্য নেই বলে এটা নতুন করে বানানো হয়েছে।
 *
 * ফিচার প্যারিটি (প্রম্পটে যা চাওয়া হয়েছিল, ঠিক ততটুকু — CSV import/export, Firebase-config
 * ডায়ালগ, বা "factory reset" ড্যাঞ্জার-জোন এই নতুন ফাইলে ইচ্ছাকৃতভাবে বাদ, কারণ প্রম্পটে চাওয়া
 * হয়নি — সেগুলো স্কোপ-বাড়ানো হতো):
 * - টেবিলের তালিকা (নিচের [EXPLORER_TABLES], সবগুলো ১৭টা Supabase টেবিল)
 * - নির্বাচিত টেবিলের row paginated ভাবে দেখানো (Postgrest `range()` — Firestore
 *   `limit()+startAfter()` এর সমতুল্য)
 * - client-side filter/search (লোড হওয়া row গুলোর মধ্যে যেকোনো ফিল্ডে টেক্সট মিলিয়ে)
 * - row-এর field এডিট করা এবং সম্পূর্ণ row ডিলিট করা (admin হওয়ায়)
 * - **sensitive column read-only**: `balance`/`role`/`is_banned`/`is_kyc_verified` — এবং এদের
 *   variant (`balance_user`, `is_banned_solver` ইত্যাদি) — নাম মেলালে (substring match) UI-তে
 *   lock আইকন দেখিয়ে non-editable রাখা হয়। RLS/column-permission এমনিতেই সার্ভার-সাইডে ব্লক
 *   করার কথা (verify করা হয়নি এই ধাপে, নিচে সতর্কতা দেখুন) — এটা শুধু UX পরিষ্কার রাখার জন্য,
 *   যাতে admin "কেন সেভ হচ্ছে না" ভেবে বিভ্রান্ত না হয়।
 *
 * ⚠️ **RLS admin-bypass ধরে নেওয়া হয়েছে** (যেমন ধাপ ১৮-এর `AdminRefundDebugView`-এও) — এই
 * generic explorer কোনো owner/solver-scoped filter ছাড়াই সরাসরি pk দিয়ে read/update/delete/insert
 * করে, ধরে নিয়ে যে admin session `is_admin()` পলিসি satisfy করে। যদি কোনো টেবিলে admin-bypass
 * policy না থাকে, তাহলে সেই টেবিলে explorer খালি/আংশিক ফলাফল দেখাবে বা write silently RLS-এ
 * আটকে যাবে (exception ছাড়াই) — প্রথম manual QA-তে Supabase dashboard-এ RLS একবার cross-check
 * করা উচিত।
 *
 * [আপডেট — ধাপ ৩৩.৩] পুরনো `AdminFirestoreExplorerView.kt` তখন (ধাপ ১৯-এ) navigation থেকে এই
 * নতুন View দিয়ে প্রতিস্থাপিত হয়েছিল কিন্তু ফাইলটা নিজে রেখে দেওয়া হয়েছিল; ধাপ ৩৩.৩-এ (Firebase
 * সম্পূর্ণ অপসারণের সময়) সেই dead ফাইলটা সম্পূর্ণ ডিলিট করা হয়ে গেছে — এখন প্রজেক্টে নেই।
 */

/** এক্সপ্লোরারে দেখানো একটা টেবিলের কনফিগ — নাম, বাংলা লেবেল, আর তার primary key কলাম। */
private data class ExplorerTableConfig(
    val tableName: String,
    val displayLabel: String,
    val pkColumn: String
)

/** সব ১৭টা Supabase টেবিল (ধাপ ২ এর DTO তালিকা অনুযায়ী), যার যার PK কলামসহ। */
private val EXPLORER_TABLES = listOf(
    ExplorerTableConfig("users", "ইউজার/সলভার", "id"),
    ExplorerTableConfig("problems", "সমস্যা", "id"),
    ExplorerTableConfig("bids", "বিড", "id"),
    ExplorerTableConfig("escrows", "এসক্রো", "id"),
    ExplorerTableConfig("transactions", "লেনদেন", "id"),
    ExplorerTableConfig("withdrawals", "উইথড্রয়াল", "id"),
    ExplorerTableConfig("additional_charges", "অতিরিক্ত চার্জ", "id"),
    ExplorerTableConfig("messages", "চ্যাট মেসেজ", "id"),
    ExplorerTableConfig("ratings", "রেটিং", "id"),
    ExplorerTableConfig("reputation_events", "রেপুটেশন ইভেন্ট", "id"),
    ExplorerTableConfig("categories", "ক্যাটাগরি", "id"),
    ExplorerTableConfig("faqs", "FAQ", "id"),
    ExplorerTableConfig("admin_audit_logs", "অ্যাডমিন অডিট লগ", "id"),
    ExplorerTableConfig("platform_settings", "প্ল্যাটফর্ম সেটিংস", "key"),
    ExplorerTableConfig("gateway_payments", "গেটওয়ে পেমেন্ট", "id"),
    ExplorerTableConfig("idempotency_keys", "আইডেম্পোটেন্সি কী", "key"),
    ExplorerTableConfig("notifications", "নোটিফিকেশন", "id")
)

/** এই সাবস্ট্রিং গুলোর যেকোনো একটা কলাম-নামে (case-insensitive) থাকলে সেটা sensitive ধরা হয়। */
private val SENSITIVE_COLUMN_SUBSTRINGS = listOf("balance", "role", "is_banned", "is_kyc_verified")

private fun isSensitiveColumn(columnName: String): Boolean {
    val lower = columnName.lowercase()
    return SENSITIVE_COLUMN_SUBSTRINGS.any { lower.contains(it) }
}

private fun jsonElementToDisplayString(element: JsonElement?): String {
    return when {
        element == null || element is JsonNull -> "null"
        element is JsonPrimitive -> element.content
        else -> element.toString()
    }
}

private fun pkValueOf(row: JsonObject, pkColumn: String): String =
    jsonElementToDisplayString(row[pkColumn])

private fun defaultTypeFor(element: JsonElement?): String {
    return when {
        element == null || element is JsonNull -> "null"
        element is JsonPrimitive && element.booleanOrNull != null -> "Boolean"
        element is JsonPrimitive && !element.isString -> "Number"
        element is JsonPrimitive -> "String"
        else -> "Json"
    }
}

private const val PAGE_SIZE = 25L

@Composable
fun AdminSupabaseExplorerView(
    viewModel: SomadhanViewModel,
    onLogout: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()

    var selectedTable by remember { mutableStateOf(EXPLORER_TABLES.first().tableName) }
    val tableConfig = remember(selectedTable) { EXPLORER_TABLES.first { it.tableName == selectedTable } }

    var rows by remember { mutableStateOf<List<JsonObject>>(emptyList()) }
    var nextOffset by remember { mutableStateOf(0L) }
    var hasMore by remember { mutableStateOf(true) }
    var isLoading by remember { mutableStateOf(false) }
    var isLoadingMore by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var refreshTrigger by remember { mutableStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }

    var editingField by remember { mutableStateOf<EditingFieldInfo?>(null) }
    var rowPendingDelete by remember { mutableStateOf<String?>(null) }
    var showNewRowDialog by remember { mutableStateOf(false) }

    fun loadMore() {
        if (isLoadingMore || !hasMore) return
        isLoadingMore = true
        coroutineScope.launch {
            val result = SupabaseSyncManager.explorerFetchPage(
                table = selectedTable,
                orderByColumn = tableConfig.pkColumn,
                from = nextOffset,
                to = nextOffset + PAGE_SIZE - 1
            )
            isLoadingMore = false
            result.onSuccess { fetched ->
                rows = rows + fetched
                nextOffset += PAGE_SIZE
                hasMore = fetched.size.toLong() == PAGE_SIZE
            }.onFailure { e ->
                viewModel.showToast("আরও ডাটা লোড করতে ত্রুটি: ${e.localizedMessage}")
            }
        }
    }

    // টেবিল বদলালে বা রিফ্রেশ ট্রিগার হলে প্রথম পেজ (নতুন করে) লোড হবে
    LaunchedEffect(selectedTable, refreshTrigger) {
        isLoading = true
        errorMessage = null
        rows = emptyList()
        nextOffset = 0L
        hasMore = true
        val result = SupabaseSyncManager.explorerFetchPage(
            table = selectedTable,
            orderByColumn = tableConfig.pkColumn,
            from = 0L,
            to = PAGE_SIZE - 1
        )
        isLoading = false
        result.onSuccess { fetched ->
            rows = fetched
            nextOffset = PAGE_SIZE
            hasMore = fetched.size.toLong() == PAGE_SIZE
        }.onFailure { e ->
            errorMessage = e.localizedMessage ?: "টেবিল লোড করতে ত্রুটি হয়েছে"
        }
    }

    val dynamicColumns = remember(rows, selectedTable) {
        val allKeys = linkedSetOf<String>()
        rows.forEach { row -> allKeys.addAll(row.keys) }
        val others = (allKeys - tableConfig.pkColumn).sorted()
        listOf(tableConfig.pkColumn) + others
    }

    val filteredRows = remember(rows, searchQuery) {
        if (searchQuery.isBlank()) {
            rows
        } else {
            val q = searchQuery.trim().lowercase()
            rows.filter { row -> row.values.any { jsonElementToDisplayString(it).lowercase().contains(q) } }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SomadhanBg)
            .padding(16.dp)
    ) {
        // Header
        Card(
            colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, SomadhanBorder, RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(SomadhanOrange.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Storage,
                                contentDescription = null,
                                tint = SomadhanOrange,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Supabase ডাটা এক্সপ্লোরার",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = SomadhanTextPrimary
                            )
                            Text(
                                text = "Postgrest টেবিল ব্রাউজার (paginated)",
                                fontSize = 12.sp,
                                color = SomadhanTextSecondary
                            )
                        }
                    }

                    IconButton(
                        onClick = { refreshTrigger++ },
                        modifier = Modifier.testTag("supabase_explorer_refresh_button")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "রিফ্রেশ", tint = SomadhanOrange)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { showNewRowDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = SomadhanOrange),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("supabase_explorer_add_row_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("+ নতুন Row", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Table Selector Chips
        Text(
            text = "টেবিল নির্বাচন করুন (${EXPLORER_TABLES.size}টি):",
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            color = SomadhanTextSecondary,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("supabase_explorer_table_selector")
        ) {
            items(EXPLORER_TABLES.size) { idx ->
                val cfg = EXPLORER_TABLES[idx]
                FilterChip(
                    selected = cfg.tableName == selectedTable,
                    onClick = { selectedTable = cfg.tableName; searchQuery = "" },
                    label = { Text(cfg.displayLabel, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = SomadhanOrange,
                        selectedLabelColor = Color.White
                    ),
                    modifier = Modifier.testTag("supabase_explorer_table_chip_${cfg.tableName}")
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("লোড হওয়া row-এর মধ্যে খুঁজুন...", fontSize = 13.sp, color = SomadhanTextHint) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = SomadhanTextSecondary) },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "মুছুন", tint = SomadhanTextSecondary)
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SomadhanOrange,
                unfocusedBorderColor = SomadhanBorder
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("supabase_explorer_search_input")
        )

        Spacer(modifier = Modifier.height(10.dp))

        if (errorMessage != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SomadhanErrorLight),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = errorMessage ?: "",
                    color = SomadhanError,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(12.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = SomadhanOrange)
            }
        } else {
            Text(
                text = "মোট লোড হয়েছে: ${filteredRows.size}টি row" + if (searchQuery.isNotBlank()) " (ফিল্টার করা)" else "",
                fontSize = 11.sp,
                color = SomadhanTextHint,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("supabase_explorer_row_list"),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredRows.size) { idx ->
                    val row = filteredRows[idx]
                    ExplorerRowCard(
                        row = row,
                        columns = dynamicColumns,
                        pkColumn = tableConfig.pkColumn,
                        onEditField = { fieldName, currentValue ->
                            editingField = EditingFieldInfo(
                                pkValue = pkValueOf(row, tableConfig.pkColumn),
                                fieldName = fieldName,
                                currentValue = currentValue
                            )
                        },
                        onDeleteRow = { rowPendingDelete = pkValueOf(row, tableConfig.pkColumn) }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    if (hasMore && searchQuery.isBlank()) {
                        OutlinedButton(
                            onClick = { loadMore() },
                            enabled = !isLoadingMore,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("supabase_explorer_load_more_button")
                        ) {
                            if (isLoadingMore) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = SomadhanOrange, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("লোড হচ্ছে...", fontSize = 13.sp, color = SomadhanOrange)
                            } else {
                                Text("আরও লোড করুন", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SomadhanOrange)
                            }
                        }
                    } else if (!hasMore && rows.isNotEmpty()) {
                        Text(
                            text = "সব row লোড হয়ে গেছে।",
                            fontSize = 11.sp,
                            color = SomadhanTextHint,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else if (rows.isEmpty()) {
                        Text(
                            text = "এই টেবিলে কোনো row পাওয়া যায়নি।",
                            fontSize = 13.sp,
                            color = SomadhanTextSecondary,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }

    // Edit Field Dialog
    editingField?.let { info ->
        EditFieldDialog(
            info = info,
            onDismiss = { editingField = null },
            onSave = { newValue ->
                coroutineScope.launch {
                    val updates = buildJsonObject { put(info.fieldName, newValue) }
                    val result = SupabaseSyncManager.explorerUpdateRow(
                        table = selectedTable,
                        pkColumn = tableConfig.pkColumn,
                        pkValue = info.pkValue,
                        updates = updates
                    )
                    result.onSuccess {
                        rows = rows.map { row ->
                            if (pkValueOf(row, tableConfig.pkColumn) == info.pkValue) {
                                JsonObject(row.toMutableMap().apply { put(info.fieldName, newValue) })
                            } else row
                        }
                        viewModel.logAdminAction(
                            actionType = "SUPABASE_EXPLORER_UPDATE_FIELD",
                            targetId = info.pkValue,
                            targetName = selectedTable,
                            details = "টেবিল: $selectedTable, ফিল্ড: ${info.fieldName}, নতুন মান: ${jsonElementToDisplayString(newValue).take(50)}"
                        )
                        viewModel.showToast("ফিল্ড '${info.fieldName}' সফলভাবে আপডেট হয়েছে!")
                    }.onFailure { e ->
                        viewModel.showToast("আপডেট ব্যর্থ: ${e.localizedMessage}")
                    }
                    editingField = null
                }
            }
        )
    }

    // Delete Confirmation Dialog
    rowPendingDelete?.let { pkVal ->
        BottomSlideAlertDialog(
            onDismissRequest = { rowPendingDelete = null },
            title = { Text("Row ডিলিট নিশ্চিত করুন", fontWeight = FontWeight.Bold, color = SomadhanError) },
            text = {
                Text(
                    "টেবিল: $selectedTable\n${tableConfig.pkColumn} = $pkVal\n\nএই row-টা স্থায়ীভাবে মুছে যাবে, এটা আর ফিরিয়ে আনা যাবে না।",
                    fontSize = 13.sp,
                    color = SomadhanTextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            val result = SupabaseSyncManager.explorerDeleteRow(
                                table = selectedTable,
                                pkColumn = tableConfig.pkColumn,
                                pkValue = pkVal
                            )
                            result.onSuccess {
                                rows = rows.filter { pkValueOf(it, tableConfig.pkColumn) != pkVal }
                                viewModel.logAdminAction(
                                    actionType = "SUPABASE_EXPLORER_DELETE_ROW",
                                    targetId = pkVal,
                                    targetName = selectedTable,
                                    details = "টেবিল: $selectedTable থেকে row (${tableConfig.pkColumn}=$pkVal) মুছে ফেলা হয়েছে"
                                )
                                viewModel.showToast("Row সফলভাবে মুছে ফেলা হয়েছে!")
                            }.onFailure { e ->
                                viewModel.showToast("মুছে ফেলতে ব্যর্থ: ${e.localizedMessage}")
                            }
                            rowPendingDelete = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SomadhanError),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("supabase_explorer_confirm_delete_button")
                ) {
                    Text("ডিলিট করুন", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { rowPendingDelete = null }) {
                    Text("বাতিল", color = SomadhanTextSecondary)
                }
            },
            containerColor = SomadhanCardBg,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // New Row Dialog
    if (showNewRowDialog) {
        NewRowDialog(
            tableConfig = tableConfig,
            existingColumns = dynamicColumns,
            onDismiss = { showNewRowDialog = false },
            onSave = { values ->
                coroutineScope.launch {
                    val result = SupabaseSyncManager.explorerInsertRow(selectedTable, values)
                    result.onSuccess {
                        showNewRowDialog = false
                        val pkVal = jsonElementToDisplayString(values[tableConfig.pkColumn])
                        viewModel.logAdminAction(
                            actionType = "SUPABASE_EXPLORER_INSERT_ROW",
                            targetId = pkVal,
                            targetName = selectedTable,
                            details = "টেবিল: $selectedTable এ নতুন row (${tableConfig.pkColumn}=$pkVal) যোগ করা হয়েছে"
                        )
                        viewModel.showToast("নতুন row সফলভাবে তৈরি হয়েছে!")
                        refreshTrigger++
                    }.onFailure { e ->
                        viewModel.showToast("Row তৈরি করতে ব্যর্থ: ${e.localizedMessage}")
                    }
                }
            }
        )
    }
}

/** একটা row-কে card আকারে দেখায় — pk + delete বাটন হেডারে, বাকি field গুলো নিচে key:value আকারে। */
@Composable
private fun ExplorerRowCard(
    row: JsonObject,
    columns: List<String>,
    pkColumn: String,
    onEditField: (fieldName: String, currentValue: JsonElement?) -> Unit,
    onDeleteRow: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SomadhanBorder, RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$pkColumn: ${pkValueOf(row, pkColumn)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    color = SomadhanTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDeleteRow, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "ডিলিট", tint = SomadhanError, modifier = Modifier.size(18.dp))
                }
            }
            Divider(color = SomadhanDivider, modifier = Modifier.padding(vertical = 6.dp))

            columns.filter { it != pkColumn }.forEach { col ->
                val value = row[col]
                val sensitive = isSensitiveColumn(col)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (!sensitive) Modifier.clip(RoundedCornerShape(6.dp)).clickable { onEditField(col, value) }
                            else Modifier
                        )
                        .padding(vertical = 5.dp, horizontal = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = col,
                        fontSize = 11.sp,
                        color = SomadhanTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(0.4f)
                    )
                    Row(
                        modifier = Modifier.weight(0.6f),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = jsonElementToDisplayString(value),
                            fontSize = 12.sp,
                            color = SomadhanTextPrimary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        if (sensitive) {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = "সংরক্ষিত ফিল্ড — এডিট করা যাবে না",
                                tint = SomadhanTextHint,
                                modifier = Modifier.size(12.dp)
                            )
                        } else {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "এডিট",
                                tint = SomadhanTextSecondary.copy(alpha = 0.7f),
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class EditingFieldInfo(
    val pkValue: String,
    val fieldName: String,
    val currentValue: JsonElement?
)

@Composable
private fun EditFieldDialog(
    info: EditingFieldInfo,
    onDismiss: () -> Unit,
    onSave: (JsonElement) -> Unit
) {
    val isComplex = info.currentValue is JsonObject || info.currentValue is JsonArray
    var jsonText by remember { mutableStateOf(info.currentValue?.toString() ?: "") }
    var jsonError by remember { mutableStateOf<String?>(null) }
    var selectedType by remember { mutableStateOf(defaultTypeFor(info.currentValue)) }
    var textValue by remember {
        mutableStateOf(jsonElementToDisplayString(info.currentValue).let { if (it == "null") "" else it })
    }
    var boolValue by remember { mutableStateOf((info.currentValue as? JsonPrimitive)?.booleanOrNull ?: false) }

    BottomSlideAlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("ফিল্ড এডিট: ${info.fieldName}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = SomadhanTextPrimary)
                Text("PK: ${info.pkValue}", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = SomadhanTextSecondary)
            }
        },
        text = {
            if (isComplex) {
                Column {
                    Text(
                        "এটা একটা nested JSON object/array — raw JSON হিসেবে এডিট করুন:",
                        fontSize = 12.sp,
                        color = SomadhanTextSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = jsonText,
                        onValueChange = { jsonText = it; jsonError = null },
                        shape = RoundedCornerShape(8.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SomadhanOrange,
                            unfocusedBorderColor = SomadhanBorder
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 100.dp, max = 260.dp)
                    )
                    if (jsonError != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(jsonError ?: "", color = SomadhanError, fontSize = 11.sp)
                    }
                }
            } else {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("String", "Number", "Boolean", "null").forEach { type ->
                            FilterChip(
                                selected = selectedType == type,
                                onClick = { selectedType = type },
                                label = { Text(type, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SomadhanOrange,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    when (selectedType) {
                        "Boolean" -> {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Button(
                                    onClick = { boolValue = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = if (boolValue) SomadhanSuccess else SomadhanCardBg),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.border(1.dp, if (boolValue) SomadhanSuccess else SomadhanBorder, RoundedCornerShape(8.dp))
                                ) { Text("True", color = if (boolValue) Color.White else SomadhanTextPrimary) }

                                Button(
                                    onClick = { boolValue = false },
                                    colors = ButtonDefaults.buttonColors(containerColor = if (!boolValue) SomadhanError else SomadhanCardBg),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.border(1.dp, if (!boolValue) SomadhanError else SomadhanBorder, RoundedCornerShape(8.dp))
                                ) { Text("False", color = if (!boolValue) Color.White else SomadhanTextPrimary) }
                            }
                        }
                        "null" -> {
                            Text("মান মুছে 'null' হিসেবে সেভ হবে।", fontSize = 13.sp, color = SomadhanTextSecondary)
                        }
                        else -> {
                            OutlinedTextField(
                                value = textValue,
                                onValueChange = { textValue = it },
                                label = { Text("নতুন মান ($selectedType)", fontSize = 12.sp) },
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = SomadhanOrange,
                                    unfocusedBorderColor = SomadhanBorder
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isComplex) {
                        try {
                            val parsed = Json.parseToJsonElement(jsonText)
                            onSave(parsed)
                        } catch (e: Exception) {
                            jsonError = "অবৈধ JSON: ${e.localizedMessage}"
                        }
                    } else {
                        val finalVal: JsonElement = when (selectedType) {
                            "Boolean" -> JsonPrimitive(boolValue)
                            "null" -> JsonNull
                            "Number" -> textValue.toLongOrNull()?.let { JsonPrimitive(it) }
                                ?: textValue.toDoubleOrNull()?.let { JsonPrimitive(it) }
                                ?: JsonPrimitive(textValue)
                            else -> JsonPrimitive(textValue)
                        }
                        onSave(finalVal)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = SomadhanOrange),
                shape = RoundedCornerShape(8.dp)
            ) { Text("সংরক্ষণ করুন", color = Color.White, fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("বাতিল", color = SomadhanTextSecondary) }
        },
        containerColor = SomadhanCardBg,
        shape = RoundedCornerShape(16.dp)
    )
}

/** নতুন Row form-এর একটা field entry — key/value/type সবই admin এডিট করতে পারে। */
private data class NewFieldEntry(
    val key: String,
    var value: String = "",
    var type: String = "String"
)

@Composable
private fun NewRowDialog(
    tableConfig: ExplorerTableConfig,
    existingColumns: List<String>,
    onDismiss: () -> Unit,
    onSave: (JsonObject) -> Unit
) {
    val fieldEntries = remember {
        val list = mutableStateListOf<NewFieldEntry>()
        list.add(NewFieldEntry(key = tableConfig.pkColumn))
        existingColumns.forEach { col ->
            if (col != tableConfig.pkColumn && !isSensitiveColumn(col)) {
                list.add(NewFieldEntry(key = col))
            }
        }
        list
    }
    var errorText by remember { mutableStateOf<String?>(null) }

    BottomSlideAlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("+ নতুন Row যোগ (${tableConfig.tableName})", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = SomadhanTextPrimary)
        },
        text = {
            Column {
                Text(
                    "${tableConfig.pkColumn} (PK) অবশ্যই দিতে হবে — বাকি ফিল্ড ফাঁকা রাখলে DB ডিফল্ট প্রযোজ্য হবে। sensitive কলাম (balance/role/is_banned/is_kyc_verified জাতীয়) এখানে দেখানো হয়নি।",
                    fontSize = 11.sp,
                    color = SomadhanTextSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp)) {
                    items(fieldEntries.size) { idx ->
                        val entry = fieldEntries[idx]
                        Column(modifier = Modifier.padding(vertical = 6.dp)) {
                            Text(
                                text = entry.key + if (entry.key == tableConfig.pkColumn) " (PK)" else "",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SomadhanTextPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf("String", "Number", "Boolean", "null").forEach { t ->
                                    FilterChip(
                                        selected = entry.type == t,
                                        onClick = { entry.type = t },
                                        label = { Text(t, fontSize = 10.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = SomadhanOrange,
                                            selectedLabelColor = Color.White
                                        )
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            when (entry.type) {
                                "Boolean" -> {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = { entry.value = "true" },
                                            colors = ButtonDefaults.buttonColors(containerColor = if (entry.value == "true") SomadhanSuccess else SomadhanCardBg),
                                            shape = RoundedCornerShape(6.dp)
                                        ) { Text("True", fontSize = 11.sp, color = if (entry.value == "true") Color.White else SomadhanTextPrimary) }
                                        Button(
                                            onClick = { entry.value = "false" },
                                            colors = ButtonDefaults.buttonColors(containerColor = if (entry.value == "false") SomadhanError else SomadhanCardBg),
                                            shape = RoundedCornerShape(6.dp)
                                        ) { Text("False", fontSize = 11.sp, color = if (entry.value == "false") Color.White else SomadhanTextPrimary) }
                                    }
                                }
                                "null" -> {
                                    Text("null হিসেবে সেভ হবে।", fontSize = 11.sp, color = SomadhanTextHint)
                                }
                                else -> {
                                    OutlinedTextField(
                                        value = entry.value,
                                        onValueChange = { entry.value = it },
                                        placeholder = { Text("মান", fontSize = 11.sp) },
                                        singleLine = true,
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                        Divider(color = SomadhanDivider)
                    }
                }
                if (errorText != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(errorText ?: "", color = SomadhanError, fontSize = 11.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val pkEntry = fieldEntries.first { it.key == tableConfig.pkColumn }
                    if (pkEntry.value.isBlank()) {
                        errorText = "${tableConfig.pkColumn} (PK) ফাঁকা রাখা যাবে না।"
                        return@Button
                    }
                    val obj = buildJsonObject {
                        fieldEntries.forEach { entry ->
                            val isPk = entry.key == tableConfig.pkColumn
                            if (!isPk && entry.value.isBlank() && entry.type != "null") return@forEach
                            val jsonVal: JsonElement = when (entry.type) {
                                "Boolean" -> JsonPrimitive(entry.value == "true")
                                "null" -> JsonNull
                                "Number" -> entry.value.toLongOrNull()?.let { JsonPrimitive(it) }
                                    ?: entry.value.toDoubleOrNull()?.let { JsonPrimitive(it) }
                                    ?: JsonPrimitive(entry.value)
                                else -> JsonPrimitive(entry.value)
                            }
                            put(entry.key, jsonVal)
                        }
                    }
                    onSave(obj)
                },
                colors = ButtonDefaults.buttonColors(containerColor = SomadhanOrange),
                shape = RoundedCornerShape(8.dp)
            ) { Text("Row তৈরি করুন", color = Color.White, fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("বাতিল", color = SomadhanTextSecondary) }
        },
        containerColor = SomadhanCardBg,
        shape = RoundedCornerShape(16.dp)
    )
}
