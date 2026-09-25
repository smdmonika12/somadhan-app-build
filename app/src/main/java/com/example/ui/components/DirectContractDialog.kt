package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.entity.CategoryEntity
import com.example.data.entity.UserEntity
import com.example.ui.theme.SomadhanBg
import com.example.ui.theme.SomadhanBorder
import com.example.ui.theme.SomadhanCardBg
import com.example.ui.theme.SomadhanTextPrimary
import com.example.ui.theme.SomadhanTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DirectContractDialog(
    solver: UserEntity,
    categories: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onSubmit: (title: String, description: String, category: CategoryEntity, budget: Double, durationDays: Int) -> Unit,
    isSubmitting: Boolean = false,
    errorMessage: String? = null
) {
    // Only virtual categories matching the solver's registered profile categories are allowed
    val solverCategoryIds = remember(solver.solverCategories) {
        solver.solverCategories.split(",").map { it.trim() }.filter { it.isNotBlank() }
    }
    val matchingVirtualCategories = remember(categories, solverCategoryIds) {
        categories.filter { !it.isPhysical && solverCategoryIds.contains(it.id) }
    }

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var budgetText by remember { mutableStateOf("") }
    var durationText by remember { mutableStateOf("3") }
    var selectedCategory by remember(matchingVirtualCategories) { mutableStateOf(matchingVirtualCategories.firstOrNull()) }
    var isCategoryExpanded by remember { mutableStateOf(false) }

    BottomSlideDialog(onDismissRequest = { if (!isSubmitting) onDismiss() }) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .testTag("direct_contract_dialog"),
            color = SomadhanBg
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "🎯 সরাসরি কাজের চুক্তি প্রস্তাব",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = SomadhanTextPrimary
                        )
                        Text(
                            text = "সমাধানকারী: ${solver.name}",
                            fontSize = 12.sp,
                            color = SomadhanTextSecondary
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        enabled = !isSubmitting,
                        modifier = Modifier.size(28.dp).testTag("close_direct_contract_dialog")
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "বন্ধ করুন")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (matchingVirtualCategories.isEmpty()) {
                    // Category Mismatch Warning Banner
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFEF2F2),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFECACA))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "⚠️ এই সমাধানকারীর প্রোফাইলে কোনো সরাসরি অ্যাসাইনযোগ্য ভার্চুয়াল ক্যাটাগরি যুক্ত নেই। ক্যাটাগরি মিল না থাকলে সরাসরি কাজের প্রস্তাব দেওয়া যাবে না।",
                                fontSize = 11.sp,
                                color = Color(0xFFDC2626),
                                lineHeight = 16.sp
                            )
                        }
                    }
                } else {
                    // Virtual & Category Match Banner
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFEFF6FF),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBFDBFE))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🌐 সমাধানকারীর অনুমোদিত ভার্চুয়াল কাজের ক্যাটাগরি প্রযোজ্য",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1D4ED8)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Title field
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("কাজের শিরোনাম *") },
                    placeholder = { Text("যেমন: লোগো ডিজাইন, কনটেন্ট রাইটিং") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF1D4ED8),
                        unfocusedBorderColor = SomadhanBorder,
                        focusedContainerColor = SomadhanCardBg,
                        unfocusedContainerColor = SomadhanCardBg
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("direct_contract_title_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Category selector (Matching Virtual categories only)
                ExposedDropdownMenuBox(
                    expanded = isCategoryExpanded && matchingVirtualCategories.isNotEmpty(),
                    onExpandedChange = { if (matchingVirtualCategories.isNotEmpty()) isCategoryExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedCategory?.let { "🌐 ${it.nameBangla}" } ?: if (matchingVirtualCategories.isEmpty()) "কোনো ভার্চুয়াল ক্যাটাগরি মেলেনি" else "ভার্চুয়াল ক্যাটাগরি বাছাই করুন",
                        onValueChange = {},
                        readOnly = true,
                        enabled = matchingVirtualCategories.isNotEmpty(),
                        label = { Text("দক্ষতার সাথে সামঞ্জস্যপূর্ণ ক্যাটাগরি *") },
                        trailingIcon = {
                            if (matchingVirtualCategories.isNotEmpty()) {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCategoryExpanded)
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF1D4ED8),
                            unfocusedBorderColor = SomadhanBorder,
                            focusedContainerColor = SomadhanCardBg,
                            unfocusedContainerColor = SomadhanCardBg
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                            .testTag("direct_contract_category_dropdown")
                    )

                    ExposedDropdownMenu(
                        expanded = isCategoryExpanded && matchingVirtualCategories.isNotEmpty(),
                        onDismissRequest = { isCategoryExpanded = false }
                    ) {
                        matchingVirtualCategories.forEach { cat ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "🌐 ${cat.nameBangla}",
                                            fontWeight = if (selectedCategory?.id == cat.id) FontWeight.Bold else FontWeight.Normal,
                                            color = SomadhanTextPrimary
                                        )
                                        Text(
                                            text = "দক্ষতা মিলছে ✓",
                                            fontSize = 11.sp,
                                            color = Color(0xFF059669),
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                },
                                onClick = {
                                    selectedCategory = cat
                                    isCategoryExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Budget & Duration Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = budgetText,
                        onValueChange = { budgetText = it },
                        label = { Text("বাজেট (৳) *") },
                        placeholder = { Text("৫০০") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF1D4ED8),
                            unfocusedBorderColor = SomadhanBorder,
                            focusedContainerColor = SomadhanCardBg,
                            unfocusedContainerColor = SomadhanCardBg
                        ),
                        modifier = Modifier.weight(1f).testTag("direct_contract_budget_input")
                    )

                    OutlinedTextField(
                        value = durationText,
                        onValueChange = { durationText = it },
                        label = { Text("মেয়াদ (দিন) *") },
                        placeholder = { Text("৩") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF1D4ED8),
                            unfocusedBorderColor = SomadhanBorder,
                            focusedContainerColor = SomadhanCardBg,
                            unfocusedContainerColor = SomadhanCardBg
                        ),
                        modifier = Modifier.weight(1f).testTag("direct_contract_duration_input")
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Description field
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("কাজের বিস্তারিত বিবরণ *") },
                    placeholder = { Text("কাজের সুনির্দিষ্ট শর্তাবলী ও প্রয়োজনীয় বিবরণ লিখুন...") },
                    maxLines = 4,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF1D4ED8),
                        unfocusedBorderColor = SomadhanBorder,
                        focusedContainerColor = SomadhanCardBg,
                        unfocusedContainerColor = SomadhanCardBg
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .testTag("direct_contract_desc_input")
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Submit button
                Button(
                    onClick = {
                        val budget = budgetText.toDoubleOrNull() ?: 0.0
                        val duration = durationText.toIntOrNull() ?: 3
                        val cat = selectedCategory
                        if (title.isNotBlank() && description.isNotBlank() && cat != null && budget > 0.0) {
                            onSubmit(title, description, cat, budget, duration)
                        }
                    },
                    enabled = !isSubmitting && title.isNotBlank() && description.isNotBlank() && selectedCategory != null && (budgetText.toDoubleOrNull() ?: 0.0) > 0.0,
                    modifier = Modifier.fillMaxWidth().testTag("submit_direct_contract_proposal_btn"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D4ED8))
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "পাঠানো হচ্ছে...",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    } else {
                        Text(
                            text = "চুক্তি প্রস্তাব পাঠান 🚀",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage,
                        fontSize = 12.sp,
                        color = Color(0xFFDC2626)
                    )
                }
            }
        }
    }
}
