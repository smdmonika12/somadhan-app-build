@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class
)

package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PostAdd
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.entity.AdditionalChargeEntity
import com.example.data.entity.AdminAuditLogEntity
import com.example.data.entity.BidEntity
import com.example.data.entity.CategoryEntity
import com.example.data.entity.EscrowEntity
import com.example.data.entity.FaqEntity
import com.example.data.entity.MessageEntity
import com.example.data.entity.ProblemEntity
import com.example.data.entity.RatingEntity
import com.example.data.entity.ReputationEventEntity
import com.example.data.entity.TransactionEntity
import com.example.data.entity.UserEntity
import com.example.data.entity.WithdrawalEntity
import com.example.data.repository.AdminDashboardMetrics
import com.example.ui.components.PulsingValue
import com.example.ui.components.RealtimeLocationBar
import com.example.ui.components.ReputationBadge
import com.example.ui.components.RoleBadge
import com.example.ui.components.StatCard
import com.example.ui.components.StatusBadge
import com.example.ui.components.rememberFieldChangePulse
import com.example.ui.navigation.Screen
import com.example.ui.theme.SomadhanBg
import com.example.ui.theme.SomadhanBorder
import com.example.ui.theme.SomadhanCardBg
import com.example.ui.theme.SomadhanDivider
import com.example.ui.theme.SomadhanError
import com.example.ui.theme.SomadhanErrorLight
import com.example.ui.theme.SomadhanInfo
import com.example.ui.theme.SomadhanOrange
import com.example.ui.theme.SomadhanOrangeLight
import com.example.ui.theme.SomadhanSuccess
import com.example.ui.theme.SomadhanSuccessLight
import com.example.ui.theme.SomadhanTextHint
import com.example.ui.theme.SomadhanTextPrimary
import com.example.ui.theme.SomadhanTextSecondary
import com.example.ui.theme.SomadhanYellowVerified
import com.example.ui.theme.SomadhanYellowVerifiedBg
import com.example.ui.viewmodel.SomadhanViewModel
import com.example.util.CsvExportUtil
import com.example.util.DistanceUtil
import com.example.util.FileAttachmentUtil
import com.example.util.Formatters
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.text.SimpleDateFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import com.example.ui.components.BottomSlideAlertDialog

@Composable
fun CategoryFormDialog(
    categoryToEdit: CategoryEntity? = null,
    radiusKm: Double = 10.0,
    onDismiss: () -> Unit,
    onSave: (CategoryEntity) -> Unit
) {
    var nameBangla by remember { mutableStateOf(categoryToEdit?.nameBangla ?: "") }
    var nameEnglish by remember { mutableStateOf(categoryToEdit?.nameEnglish ?: "") }
    var minBudget by remember { mutableStateOf(categoryToEdit?.minBudget?.toInt()?.toString() ?: "300") }
    var maxBudget by remember { mutableStateOf(categoryToEdit?.maxBudget?.toInt()?.toString() ?: "1000") }
    var isPhysical by remember { mutableStateOf(categoryToEdit?.isPhysical ?: true) }
    var isActive by remember { mutableStateOf(categoryToEdit?.isActive ?: true) }
    var instantJobEnabled by remember { mutableStateOf(categoryToEdit?.instantJobEnabled ?: false) }
    var instantJobRadiusKm by remember { mutableStateOf(categoryToEdit?.instantJobRadiusKm?.toInt()?.toString() ?: "5") }

    BottomSlideAlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (categoryToEdit == null) Icons.Default.AddCircleOutline else Icons.Default.Edit,
                    contentDescription = null,
                    tint = SomadhanOrange
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (categoryToEdit == null) "নতুন ক্যাটাগরি যুক্ত করুন" else "ক্যাটাগরি এডিট করুন",
                    fontWeight = FontWeight.Bold,
                    color = SomadhanTextPrimary
                )
            }
        },
        text = {
            Column {
                OutlinedTextField(
                    value = nameBangla,
                    onValueChange = { nameBangla = it },
                    label = { Text("ক্যাটাগরির নাম (বাংলা)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = nameEnglish,
                    onValueChange = { nameEnglish = it },
                    label = { Text("ক্যাটাগরির নাম (English)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = minBudget,
                    onValueChange = { minBudget = it },
                    label = { Text("ডিফল্ট সর্বনিম্ন বাজেট (৳)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = maxBudget,
                    onValueChange = { maxBudget = it },
                    label = { Text("ডিফল্ট সর্বোচ্চ বাজেট (৳)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text("সেবার ধরন:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = SomadhanTextSecondary)
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Button(
                        onClick = { isPhysical = true },
                        colors = ButtonDefaults.buttonColors(containerColor = if (isPhysical) SomadhanOrange else SomadhanCardBg),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("ফিজিক্যাল (${DistanceUtil.toBengaliDigits(radiusKm.toInt().toString())} কিমি)", color = if (isPhysical) Color.White else SomadhanTextPrimary, fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { isPhysical = false },
                        colors = ButtonDefaults.buttonColors(containerColor = if (!isPhysical) SomadhanOrange else SomadhanCardBg),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("ভার্চুয়াল (সারাদেশ)", color = if (!isPhysical) Color.White else SomadhanTextPrimary, fontSize = 12.sp)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("ক্যাটাগরি সক্রিয় অবস্থা", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SomadhanTextPrimary)
                        Text(if (isActive) "গ্রাহক ও সলভাররা দেখতে পাবেন" else "নিষ্ক্রিয় (লুকানো থাকবে)", fontSize = 11.sp, color = SomadhanTextHint)
                    }
                    Switch(
                        checked = isActive,
                        onCheckedChange = { isActive = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = SomadhanOrange,
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = SomadhanDivider
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("জরুরি / ইনস্ট্যান্ট জব সুবিধা", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SomadhanTextPrimary)
                        Text(if (instantJobEnabled) "এই ক্যাটাগরিতে জরুরি অনুরোধ সক্রিয়" else "ইনস্ট্যান্ট অনুরোধ বন্ধ", fontSize = 11.sp, color = SomadhanTextHint)
                    }
                    Switch(
                        checked = instantJobEnabled,
                        onCheckedChange = { instantJobEnabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = SomadhanOrange,
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = SomadhanDivider
                        )
                    )
                }

                if (instantJobEnabled) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = instantJobRadiusKm,
                        onValueChange = { instantJobRadiusKm = it },
                        label = { Text("ইনস্ট্যান্ট ব্রডকাস্ট রেডিয়াস (কিমি)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (nameBangla.isNotBlank()) {
                        val finalEnglish = if (nameEnglish.isNotBlank()) nameEnglish.trim() else nameBangla.trim()
                        val rKm = instantJobRadiusKm.toDoubleOrNull() ?: 5.0
                        val category = categoryToEdit?.copy(
                            nameBangla = nameBangla.trim(),
                            nameEnglish = finalEnglish,
                            isPhysical = isPhysical,
                            minBudget = minBudget.toDoubleOrNull() ?: 300.0,
                            maxBudget = maxBudget.toDoubleOrNull() ?: 1000.0,
                            isActive = isActive,
                            instantJobEnabled = instantJobEnabled,
                            instantJobRadiusKm = rKm
                        ) ?: CategoryEntity(
                            id = "cat_" + System.currentTimeMillis(),
                            nameBangla = nameBangla.trim(),
                            nameEnglish = finalEnglish,
                            isPhysical = isPhysical,
                            iconName = "Build",
                            keywords = "$nameBangla,$finalEnglish",
                            minBudget = minBudget.toDoubleOrNull() ?: 300.0,
                            maxBudget = maxBudget.toDoubleOrNull() ?: 1000.0,
                            isActive = isActive,
                            instantJobEnabled = instantJobEnabled,
                            instantJobRadiusKm = rKm
                        )
                        onSave(category)
                    }
                },
                enabled = nameBangla.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = SomadhanOrange),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(if (categoryToEdit == null) "সংরক্ষণ" else "আপডেট করুন")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("বাতিল", color = SomadhanTextSecondary)
            }
        }
    )
}

@Composable
fun AdminCategoriesView(
    categories: List<CategoryEntity>,
    radiusKm: Double = 10.0,
    isPhysicalWorkEnabled: Boolean = true,
    isVirtualWorkEnabled: Boolean = true,
    onSetPhysicalWorkEnabled: (Boolean) -> Unit = {},
    onSetVirtualWorkEnabled: (Boolean) -> Unit = {},
    onAddClick: () -> Unit,
    onEditCategory: (CategoryEntity) -> Unit,
    onDeleteCategory: (String) -> Unit,
    onToggleActive: (String, Boolean) -> Unit,
    viewModel: SomadhanViewModel? = null,
    isManualRefreshing: Boolean = false
) {
    var categoryToDelete by remember { mutableStateOf<CategoryEntity?>(null) }
    var showDisablePhysicalConfirmDialog by remember { mutableStateOf(false) }
    var showDisableVirtualConfirmDialog by remember { mutableStateOf(false) }

    // Admin Panel Loading fix, সেশন ২.৫ — KYC (২.৩)-এর মতো, শুধু ক্যাটাগরি-লিস্টের কার্ডগুলো
    // pulse করবে (toggle/Add বাটন এই value-র অংশ না, তাই কখনো pulse করবে না)। sessionKey
    // "admin_categories_sync" — AdminPanelScreen.kt-এর ইনডেক্স ৬-এর SyncAwareContent cold-load
    // gate-এর সাথে একই key শেয়ার করে, যাতে re-entry visit-এও একবার সংক্ষিপ্ত pulse হয়, আর
    // pull-to-refresh সম্পন্ন হলেও (isManualRefreshing সত্যি→মিথ্যা) আলাদাভাবে pulse হয়।
    val categoriesListPulse = rememberFieldChangePulse(
        value = categories,
        isManualRefreshing = isManualRefreshing,
        sessionKey = "admin_categories_sync",
        viewModel = viewModel
    )

    if (showDisablePhysicalConfirmDialog) {
        BottomSlideAlertDialog(
            onDismissRequest = { showDisablePhysicalConfirmDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = SomadhanError)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("ফিজিক্যাল কাজ বন্ধ নিশ্চিতকরণ", fontWeight = FontWeight.Bold, color = SomadhanTextPrimary)
                }
            },
            text = {
                Text(
                    text = "আপনি কি নিশ্চিত যে ফিজিক্যাল কাজ পুরো অ্যাপ জুড়ে বন্ধ করতে চান? এতে সকল ফিজিক্যাল ক্যাটাগরি ইউজারদের কাছে অদৃশ্য হয়ে যাবে, কিন্তু ক্যাটাগরি ডাটা মুছে যাবে না।",
                    fontSize = 13.sp,
                    color = SomadhanTextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSetPhysicalWorkEnabled(false)
                        showDisablePhysicalConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SomadhanError),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("বন্ধ করুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDisablePhysicalConfirmDialog = false }) {
                    Text("বাতিল", color = SomadhanTextSecondary)
                }
            }
        )
    }

    if (showDisableVirtualConfirmDialog) {
        BottomSlideAlertDialog(
            onDismissRequest = { showDisableVirtualConfirmDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = SomadhanError)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("ভার্চুয়াল কাজ বন্ধ নিশ্চিতকরণ", fontWeight = FontWeight.Bold, color = SomadhanTextPrimary)
                }
            },
            text = {
                Text(
                    text = "আপনি কি নিশ্চিত যে ভার্চুয়াল কাজ পুরো অ্যাপ জুড়ে বন্ধ করতে চান? এতে সকল ভার্চুয়াল ক্যাটাগরি ইউজারদের কাছে অদৃশ্য হয়ে যাবে, কিন্তু ক্যাটাগরি ডাটা মুছে যাবে না।",
                    fontSize = 13.sp,
                    color = SomadhanTextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSetVirtualWorkEnabled(false)
                        showDisableVirtualConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SomadhanError),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("বন্ধ করুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDisableVirtualConfirmDialog = false }) {
                    Text("বাতিল", color = SomadhanTextSecondary)
                }
            }
        )
    }

    if (categoryToDelete != null) {
        val cat = categoryToDelete!!
        BottomSlideAlertDialog(
            onDismissRequest = { categoryToDelete = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = SomadhanError)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("ক্যাটাগরি মুছে ফেলা", fontWeight = FontWeight.Bold, color = SomadhanTextPrimary)
                }
            },
            text = {
                Text(
                    text = "আপনি কি নিশ্চিত যে '${cat.nameBangla}' ক্যাটাগরিটি মুছে ফেলতে চান? সলভারদের প্রোফাইল থেকেও এটি অপসারিত হবে।",
                    fontSize = 13.sp,
                    color = SomadhanTextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteCategory(cat.id)
                        categoryToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SomadhanError),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("মুছে ফেলুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { categoryToDelete = null }) {
                    Text("বাতিল", color = SomadhanTextSecondary)
                }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Global Master Controls Card
        Card(
            colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, SomadhanDivider, RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        tint = SomadhanOrange,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "গ্লোবাল ক্যাটাগরি নিয়ন্ত্রণ",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = SomadhanTextPrimary
                        )
                        Text(
                            text = "মার্কেটপ্লেসে কাজের টাইপ অনুযায়ী মাস্টার সুইচ",
                            fontSize = 11.sp,
                            color = SomadhanTextHint
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = SomadhanDivider, thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(10.dp))

                // Physical Master Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Navigation,
                            contentDescription = null,
                            tint = if (isPhysicalWorkEnabled) SomadhanOrange else SomadhanTextHint,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "ফিজিক্যাল কাজ চালু আছে",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isPhysicalWorkEnabled) SomadhanTextPrimary else SomadhanTextSecondary
                            )
                            Text(
                                text = "সশরীরে উপস্থিত হয়ে করা কাজ (রেডিয়াসের মধ্যে)",
                                fontSize = 11.sp,
                                color = SomadhanTextHint
                            )
                        }
                    }
                    Switch(
                        checked = isPhysicalWorkEnabled,
                        onCheckedChange = { checked ->
                            if (checked) {
                                onSetPhysicalWorkEnabled(true)
                            } else {
                                showDisablePhysicalConfirmDialog = true
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = SomadhanOrange,
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = SomadhanDivider
                        ),
                        modifier = Modifier
                            .scale(0.85f)
                            .testTag("admin_toggle_physical_master")
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Divider(color = SomadhanDivider, thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(8.dp))

                // Virtual Master Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = if (isVirtualWorkEnabled) SomadhanOrange else SomadhanTextHint,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "ভার্চুয়াল কাজ চালু আছে",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isVirtualWorkEnabled) SomadhanTextPrimary else SomadhanTextSecondary
                            )
                            Text(
                                text = "অনলাইনে দূর থেকে করার মতো কাজ (সারাদেশ)",
                                fontSize = 11.sp,
                                color = SomadhanTextHint
                            )
                        }
                    }
                    Switch(
                        checked = isVirtualWorkEnabled,
                        onCheckedChange = { checked ->
                            if (checked) {
                                onSetVirtualWorkEnabled(true)
                            } else {
                                showDisableVirtualConfirmDialog = true
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = SomadhanOrange,
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = SomadhanDivider
                        ),
                        modifier = Modifier
                            .scale(0.85f)
                            .testTag("admin_toggle_virtual_master")
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Button(
            onClick = onAddClick,
            colors = ButtonDefaults.buttonColors(containerColor = SomadhanOrange),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text("নতুন ক্যাটাগরি যুক্ত করুন")
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (categories.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "কোনো ক্যাটাগরি পাওয়া যায়নি",
                    fontSize = 13.sp,
                    color = SomadhanTextHint
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories, key = { it.id }) { cat ->
                    val isInactive = !cat.isActive

                    PulsingValue(isUpdating = categoriesListPulse) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isInactive) SomadhanCardBg.copy(alpha = 0.5f) else SomadhanCardBg
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = 1.dp,
                                color = if (isInactive) SomadhanDivider.copy(alpha = 0.5f) else SomadhanDivider,
                                shape = RoundedCornerShape(10.dp)
                            )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = cat.nameBangla,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isInactive) SomadhanTextSecondary else SomadhanTextPrimary
                                            )
                                            if (isInactive) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(SomadhanErrorLight)
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = "নিষ্ক্রিয়",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = SomadhanError
                                                    )
                                                }
                                            }
                                        }
                                        if (cat.nameEnglish.isNotBlank() && cat.nameEnglish != cat.nameBangla) {
                                            Text(
                                                text = cat.nameEnglish,
                                                fontSize = 11.sp,
                                                color = SomadhanTextHint
                                            )
                                        }
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Switch(
                                        checked = cat.isActive,
                                        onCheckedChange = { checked ->
                                            onToggleActive(cat.id, checked)
                                        },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = SomadhanOrange,
                                            uncheckedThumbColor = Color.White,
                                            uncheckedTrackColor = SomadhanDivider
                                        ),
                                        modifier = Modifier.scale(0.85f)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    IconButton(
                                        onClick = { onEditCategory(cat) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "এডিট করুন",
                                            tint = SomadhanOrange,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { categoryToDelete = cat },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "মুছুন",
                                            tint = SomadhanError,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Divider(color = SomadhanDivider.copy(alpha = 0.5f), thickness = 0.5.dp)
                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (cat.isPhysical) "📍 ফিজিক্যাল (${DistanceUtil.toBengaliDigits(radiusKm.toInt().toString())} কিমি ব্যাসার্ধ)" else "🌐 ভার্চুয়াল (সারাদেশ)",
                                    fontSize = 11.sp,
                                    color = if (cat.isPhysical) SomadhanOrange else SomadhanInfo,
                                    fontWeight = FontWeight.Medium
                                )

                                Text(
                                    text = "বাজেট: ৳${DistanceUtil.toBengaliDigits(cat.minBudget.toInt().toString())} - ৳${DistanceUtil.toBengaliDigits(cat.maxBudget.toInt().toString())}",
                                    fontSize = 11.sp,
                                    color = SomadhanTextSecondary
                                )
                            }
                        }
                    }
                    }
                }
            }
        }
    }
}
