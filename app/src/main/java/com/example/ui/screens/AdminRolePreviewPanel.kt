package com.example.ui.screens

/**
 * [ADMIN_ROLE_PROFILE সেশন ৩ — অংশ ২.২] লাইভ প্রিভিউ সিমুলেটর — মকআপ `admin-role-management.html`-এর
 * ডানপাশের প্যানেল (`preview-phone` + \"লাইভ স্ক্রিন সিমুলেশন — ইউজারগণ\" কার্ড)-এর প্রোডাকশন সংস্করণ।
 * ✅ কনফার্মড (২০২৬-০৯-২৪, প্রশ্ন ২): **শুধু সুপার অ্যাডমিনের জন্য** — এই কম্পোজেবল শুধু
 * [AdminRoleManagementView] (সুপার-অনলি ট্যাব) থেকেই কল হয়, অন্য কোথাও না।
 *
 * মকআপ থেকে ইচ্ছাকৃত পার্থক্য:
 * - মেনু-তালিকা ও অ্যাকশন-চিপ **`AdminPermissionCatalog.GROUPS` থেকে ডাইনামিক** — মকআপের হার্ডকোড `GROUPS`
 *   (প্রতি স্ক্রিনে ১-২টা প্লেসহোল্ডার অ্যাকশন) ব্যবহার হয়নি।
 * - \"ইউজারগণ\" সিমুলেশনের বাটনগুলোও ক্যাটালগের `users:users` অ্যাকশন থেকে ডাইনামিক (মকআপের ৬টা
 *   হার্ডকোড বাটনের বদলে আসল ৮টা), তাই ক্যাটালগ বাড়লে এখানে হাত দিতে হয় না।
 * - ফোন-স্ক্রিনে পাশাপাশি কলাম নেই — রোল-লিস্ট থেকে আলাদা প্রিভিউ-পেইন, আর এডিটরের ভেতরে
 *   collapsible \"লাইভ প্রিভিউ\" (টিক দিলে সাথে সাথে বদলায়)।
 *
 * **ফ্ল্যাগ-নিয়ম** (মাস্টার প্রম্পট ধাপ ১, ৭.০): ফ্ল্যাগড অ্যাডমিনের `view` অক্ষত, বাকি সব অ্যাকশন ব্লকড —
 * [previewActionAllowed] ঠিক সেটাই প্রয়োগ করে। [isFlagged] এখনো কেউ `true` দেয় না (এডমিন অ্যাকাউন্ট
 * সেশন ৪-এ আসবে; তখন অ্যাকাউন্ট-ভিত্তিক প্রিভিউ এই প্যারামিটারেই ওয়্যার হবে)।
 */

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.security.AdminPermissionCatalog
import com.example.ui.theme.SomadhanBg
import com.example.ui.theme.SomadhanBorder
import com.example.ui.theme.SomadhanCardBg
import com.example.ui.theme.SomadhanDivider
import com.example.ui.theme.SomadhanError
import com.example.ui.theme.SomadhanErrorLight
import com.example.ui.theme.SomadhanOrange
import com.example.ui.theme.SomadhanOrangeLight
import com.example.ui.theme.SomadhanSuccess
import com.example.ui.theme.SomadhanSuccessLight
import com.example.ui.theme.SomadhanTextHint
import com.example.ui.theme.SomadhanTextPrimary
import com.example.ui.theme.SomadhanTextSecondary

/**
 * একটা অ্যাকশন এই (রোল + ফ্ল্যাগ) সংমিশ্রণে সক্রিয় কিনা।
 * সুপার = সব; নাহলে কী পারমিশন-সেটে থাকতে হবে; ফ্ল্যাগড হলে `view` ছাড়া সব ব্লকড।
 */
internal fun previewActionAllowed(
    perms: Set<String>,
    isSuper: Boolean,
    isFlagged: Boolean,
    groupId: String,
    itemId: String,
    actionId: String
): Boolean {
    val eligible = isSuper || "$groupId:$itemId:$actionId" in perms
    return eligible && !(isFlagged && actionId != "view")
}

/** এই রোল/অ্যাকাউন্ট মেনুটা আদৌ দেখবে কিনা (`view` পারমিশন; সুপার সবসময়)। ফ্ল্যাগ এটাকে প্রভাবিত করে না। */
internal fun previewItemVisible(perms: Set<String>, isSuper: Boolean, groupId: String, itemId: String): Boolean =
    isSuper || "$groupId:$itemId:view" in perms

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminRolePreviewPanel(
    name: String,
    permissions: Set<String>,
    isSuper: Boolean,
    modifier: Modifier = Modifier,
    isFlagged: Boolean = false
) {
    val openItems = remember { mutableStateMapOf<String, Boolean>() }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // ---------- ফোন-স্টাইল মেনু প্রিভিউ ----------
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SomadhanBg),
            border = BorderStroke(1.dp, SomadhanBorder),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SomadhanOrange)
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Column {
                        Text("সমাধান অ্যাডমিন প্যানেল", color = SomadhanBg, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("লাইভ প্রিভিউ", color = SomadhanBg.copy(alpha = 0.9f), fontSize = 10.5.sp)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(if (isSuper) SomadhanOrange else SomadhanOrangeLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = name.trim().take(2).ifEmpty { "—" },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSuper) SomadhanBg else SomadhanOrange
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(name.ifBlank { "—" }, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SomadhanTextPrimary)
                        Text("এই রোলের অ্যাডমিন যা দেখবে", fontSize = 11.sp, color = SomadhanTextSecondary)
                    }
                }
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(SomadhanDivider))

                if (isFlagged) {
                    FlagBanner("🚩 ফ্ল্যাগড — মেনু দেখা যাচ্ছে, কিন্তু কোনো অ্যাকশন নেওয়া যাবে না।")
                }

                var anyVisible = false
                AdminPermissionCatalog.GROUPS.forEach { group ->
                    if (group.superOnly && !isSuper) return@forEach
                    val visibleItems = group.items.filter { previewItemVisible(permissions, isSuper, group.id, it.id) }
                    if (visibleItems.isEmpty()) return@forEach
                    anyVisible = true

                    Text(
                        text = group.title,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = SomadhanTextHint,
                        modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 2.dp)
                    )
                    visibleItems.forEach { item ->
                        val key = "${group.id}:${item.id}"
                        val open = openItems[key] == true
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { openItems[key] = !open }
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(SomadhanSuccess))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(item.label, fontSize = 12.5.sp, color = SomadhanTextPrimary, modifier = Modifier.weight(1f))
                                Icon(
                                    imageVector = if (open) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    tint = SomadhanTextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            if (open) {
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth().padding(start = 31.dp, end = 14.dp, bottom = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    item.actions.forEach { action ->
                                        val has = previewActionAllowed(permissions, isSuper, isFlagged, group.id, item.id, action.id)
                                        ActionChip(label = action.label, allowed = has)
                                    }
                                }
                            }
                        }
                    }
                }
                if (!anyVisible) {
                    Text(
                        text = "এই রোলে এখনও কোনো মেনুর \"দেখুন\" পারমিশন নেই — লগইন করলে খালি মেনু দেখাবে।",
                        fontSize = 12.sp,
                        color = SomadhanTextHint,
                        modifier = Modifier.fillMaxWidth().padding(20.dp)
                    )
                }
                Text(
                    text = "মেনুর নামে ট্যাপ করে সেই মেনুর কোন কোন ফাংশন খোলা আছে দেখুন।",
                    fontSize = 10.5.sp,
                    color = SomadhanTextHint,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp)
                )
            }
        }

        // ---------- লাইভ স্ক্রিন সিমুলেশন — ইউজারগণ ----------
        UsersScreenSimulation(permissions = permissions, isSuper = isSuper, isFlagged = isFlagged)
    }
}

@Composable
private fun FlagBanner(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(SomadhanErrorLight)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(text, fontSize = 11.5.sp, color = SomadhanError)
    }
}

@Composable
private fun ActionChip(label: String, allowed: Boolean) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (allowed) SomadhanSuccessLight else SomadhanCardBg)
            .border(
                BorderStroke(1.dp, if (allowed) SomadhanSuccess else SomadhanBorder),
                RoundedCornerShape(50)
            )
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = if (allowed) label else "✕ $label",
            fontSize = 11.sp,
            color = if (allowed) SomadhanTextPrimary else SomadhanTextHint
        )
    }
}

/**
 * মকআপের `renderUsersSim` — \"ইউজারগণ\" স্ক্রিনে ঢুকলে এই রোল ঠিক কোন বাটনগুলো সক্রিয় পাবে।
 * `users:users:view` না থাকলে স্ক্রিনটাই খোলে না (locked কার্ড)। বাটনগুলো ক্যাটালগ থেকে ডাইনামিক,
 * `view` বাদে (view মানেই স্ক্রিন খোলা, আলাদা বাটন না)। বাটনে ক্লিকের কোনো ক্রিয়া নেই — শুধু দৃশ্য।
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun UsersScreenSimulation(permissions: Set<String>, isSuper: Boolean, isFlagged: Boolean) {
    val item = AdminPermissionCatalog.findItem("users", "users") ?: return
    val hasView = previewItemVisible(permissions, isSuper, "users", "users")

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SomadhanBg),
        border = BorderStroke(1.dp, SomadhanBorder),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text("লাইভ স্ক্রিন সিমুলেশন — ইউজারগণ", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = SomadhanTextPrimary)
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                "এই রোলের অ্যাডমিন \"ইউজারগণ\" স্ক্রিনে ঢুকলে ঠিক এই বাটনগুলোই সক্রিয় পাবে।",
                fontSize = 11.sp,
                color = SomadhanTextHint
            )
            Spacer(modifier = Modifier.height(10.dp))

            if (!hasView) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(SomadhanCardBg)
                        .border(BorderStroke(1.dp, SomadhanBorder), RoundedCornerShape(10.dp))
                        .padding(14.dp)
                ) {
                    Text(
                        "এই রোলের \"ইউজারগণ\" মেনুতেই অ্যাক্সেস নেই — স্ক্রিনটা খুলবে না।",
                        fontSize = 12.sp,
                        color = SomadhanTextSecondary
                    )
                }
                return@Column
            }

            if (isFlagged) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(SomadhanErrorLight)
                        .padding(10.dp)
                ) {
                    Text("🚩 ফ্ল্যাগড অ্যাডমিন — স্ক্রিন খুলবে কিন্তু সব অ্যাকশন বাটন ব্লকড থাকবে।", fontSize = 11.5.sp, color = SomadhanError)
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(SomadhanCardBg)
                    .border(BorderStroke(1.dp, SomadhanDivider), RoundedCornerShape(10.dp))
                    .padding(12.dp)
            ) {
                Text("রাকিব করিম", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = SomadhanTextPrimary)
                Text("017XXXXXXXX · সলভার · ব্যালেন্স ৳ ১,২৪০", fontSize = 10.5.sp, color = SomadhanTextHint)
                Spacer(modifier = Modifier.height(10.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    item.actions.filter { it.id != "view" }.forEach { action ->
                        val enabled = previewActionAllowed(permissions, isSuper, isFlagged, "users", "users", action.id)
                        OutlinedButton(
                            onClick = {},
                            enabled = enabled,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text(action.label, fontSize = 11.5.sp)
                        }
                    }
                }
            }
        }
    }
}
