package com.example.ui.screens

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.CategoryIconHelper
import com.example.ui.components.RealtimeLocationBar
import com.example.ui.components.SyncAwareContent
import com.example.ui.components.worstSyncPhase
import com.example.ui.theme.SomadhanBg
import com.example.ui.theme.SomadhanBorder
import com.example.ui.theme.SomadhanCardBg
import com.example.ui.theme.SomadhanDivider
import com.example.ui.theme.SomadhanError
import com.example.ui.theme.SomadhanOrange
import com.example.ui.theme.SomadhanOrangeLight
import com.example.ui.theme.SomadhanSuccess
import com.example.ui.theme.SomadhanTextHint
import com.example.ui.theme.SomadhanTextPrimary
import com.example.ui.theme.SomadhanTextSecondary
import com.example.ui.viewmodel.SomadhanViewModel
import com.example.util.DistanceUtil

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SolverSkillsScreen(
    viewModel: SomadhanViewModel,
    onNavigateBack: () -> Unit
) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val liveLocation by viewModel.liveLocation.collectAsStateWithLifecycle()
    val allCategories by viewModel.allCategories.collectAsStateWithLifecycle()
    val physicalCategoryRadiusKm by viewModel.physicalCategoryRadiusKm.collectAsStateWithLifecycle()
    // Loading/Sync Fix Roadmap v2, ধাপ ৪ (Group B) — এই স্ক্রিন currentUser (bulk-pull/
    // initialSyncPhase) আর allCategories (categoriesSyncPhase) দুটোর উপরই নির্ভরশীল, তাই
    // worstSyncPhase দিয়ে combine করা হলো।
    val initialSyncPhase by viewModel.initialSyncPhase.collectAsStateWithLifecycle()
    val categoriesSyncPhase by viewModel.categoriesSyncPhase.collectAsStateWithLifecycle()

    val currentCatIds = remember(currentUser) {
        currentUser?.solverCategories?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() }?.toSet() ?: emptySet()
    }

    var selectedCategoryIds by remember(currentCatIds) { mutableStateOf(currentCatIds) }
    var dropdownExpanded by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSavingSkills by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            text = "সমাধানকারী স্কিল পরিবর্তন",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = SomadhanTextPrimary
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "ফিরে যান",
                                tint = SomadhanTextPrimary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = SomadhanBg),
                    modifier = Modifier.border(1.dp, SomadhanDivider)
                )
                RealtimeLocationBar(
                    locationAddress = liveLocation.address,
                    isSolver = true,
                    onRefresh = { viewModel.refreshLiveLocation() }
                )
            }
        },
        containerColor = SomadhanBg
    ) { paddingValues ->
        SyncAwareContent(
            sessionKey = "solver_skills_sync",
            viewModel = viewModel,
            syncPhase = worstSyncPhase(initialSyncPhase, categoriesSyncPhase),
            onRetry = {
                viewModel.retryInitialSync()
                viewModel.retryCategoriesSync()
            },
            modifier = Modifier.padding(paddingValues)
        ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(SomadhanBg)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(
                text = "আপনার কাজের স্কিল ও ক্যাটাগরি",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = SomadhanTextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "আপনি যে ধরণের সমস্যার সমাধান করতে দক্ষ, সর্বনিম্ন ১টি এবং সর্বোচ্চ ৩টি ক্যাটাগরি বাছাই করুন।",
                fontSize = 13.sp,
                color = SomadhanTextSecondary,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Selected count badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "বাছাইকৃত ক্যাটাগরি:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = SomadhanTextPrimary
                )
                Text(
                    text = "${DistanceUtil.toBengaliDigits(selectedCategoryIds.size.toString())} / ৩ টি সিলেক্টেড",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (selectedCategoryIds.size in 1..3) SomadhanSuccess else SomadhanOrange
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (selectedCategoryIds.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    selectedCategoryIds.forEach { id ->
                        val cat = allCategories.find { it.id == id }
                        if (cat != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(SomadhanOrange)
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = cat.nameBangla,
                                    fontSize = 12.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "রিমুভ",
                                    tint = Color.White,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clickable {
                                            selectedCategoryIds = selectedCategoryIds - id
                                            errorMessage = null
                                        }
                                )
                            }
                        }
                    }
                }
            }

            // Unselected Categories Dropdown
            val availableCategories = allCategories.filter { it.id !in selectedCategoryIds }

            Text(
                text = "নতুন ক্যাটাগরি যুক্ত করুন",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = SomadhanTextPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))

            ExposedDropdownMenuBox(
                expanded = dropdownExpanded,
                onExpandedChange = {
                    if (selectedCategoryIds.size < 3) {
                        dropdownExpanded = !dropdownExpanded
                    } else {
                        errorMessage = "সর্বোচ্চ ৩টি ক্যাটাগরি বাছাই করতে পারবেন।"
                    }
                }
            ) {
                OutlinedTextField(
                    value = if (selectedCategoryIds.size >= 3) "সর্বোচ্চ ৩টি ক্যাটাগরি নির্বাচিত হয়েছে" else "",
                    onValueChange = {},
                    readOnly = true,
                    placeholder = {
                        Text(
                            text = if (selectedCategoryIds.size >= 3) "সর্বোচ্চ ৩টি ক্যাটাগরি নির্বাচিত" else "ক্যাটাগরি বাছাই করতে ট্যাপ করুন...",
                            color = SomadhanTextHint,
                            fontSize = 13.sp
                        )
                    },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded)
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Category,
                            contentDescription = null,
                            tint = SomadhanOrange
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SomadhanOrange,
                        unfocusedBorderColor = SomadhanBorder,
                        focusedContainerColor = SomadhanBg,
                        unfocusedContainerColor = SomadhanCardBg
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )

                ExposedDropdownMenu(
                    expanded = dropdownExpanded,
                    onDismissRequest = { dropdownExpanded = false }
                ) {
                    if (availableCategories.isEmpty()) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "কোনো নতুন ক্যাটাগরি বাকি নেই",
                                    fontSize = 13.sp,
                                    color = SomadhanTextHint
                                )
                            },
                            onClick = { dropdownExpanded = false }
                        )
                    } else {
                        availableCategories.forEach { category ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(
                                            imageVector = CategoryIconHelper.getIcon(category.iconName),
                                            contentDescription = null,
                                            tint = SomadhanOrange,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = category.nameBangla,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = SomadhanTextPrimary
                                            )
                                            Text(
                                                text = if (category.isPhysical) "ফিজিক্যাল সমস্যা (${DistanceUtil.toBengaliDigits(physicalCategoryRadiusKm.toInt().toString())} কিমি ফিল্টার)" else "ভার্চুয়াল সমস্যা (সারাদেশ)",
                                                fontSize = 10.sp,
                                                color = SomadhanTextHint
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    if (selectedCategoryIds.size < 3) {
                                        selectedCategoryIds = selectedCategoryIds + category.id
                                        errorMessage = null
                                    } else {
                                        errorMessage = "সর্বোচ্চ ৩টি ক্যাটাগরি বাছাই করতে পারবেন।"
                                    }
                                    dropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = errorMessage ?: "",
                    fontSize = 12.sp,
                    color = SomadhanError,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                enabled = !isSavingSkills,
                onClick = {
                    if (selectedCategoryIds.isEmpty() || selectedCategoryIds.size > 3) {
                        errorMessage = "সর্বনিম্ন ১টি এবং সর্বোচ্চ ৩টি ক্যাটাগরি বাছাই করুন।"
                        return@Button
                    }
                    isSavingSkills = true
                    viewModel.updateSolverSkills(
                        newCategories = selectedCategoryIds.toList(),
                        onSuccess = {
                            isSavingSkills = false
                            onNavigateBack()
                        },
                        onError = {
                            isSavingSkills = false
                            errorMessage = it
                        }
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = SomadhanOrange),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("save_skills_button")
            ) {
                if (isSavingSkills) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("সংরক্ষণ হচ্ছে...", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                } else {
                    Text("স্কিল সংরক্ষণ করুন", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
        }
    }
}
