package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VerifiedUser
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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.ui.components.RealtimeLocationBar
import com.example.ui.components.SyncAwareContent
import com.example.ui.theme.SomadhanBg
import com.example.ui.theme.SomadhanBorder
import com.example.ui.theme.SomadhanCardBg
import com.example.ui.theme.SomadhanDivider
import com.example.ui.theme.SomadhanError
import com.example.ui.theme.SomadhanInfo
import com.example.ui.theme.SomadhanOrange
import com.example.ui.theme.SomadhanOrangeLight
import com.example.ui.theme.SomadhanSuccess
import com.example.ui.theme.SomadhanSuccessLight
import com.example.ui.theme.SomadhanTextHint
import com.example.ui.theme.SomadhanTextPrimary
import com.example.ui.theme.SomadhanTextSecondary
import com.example.ui.viewmodel.SomadhanViewModel
import com.example.util.Formatters
import com.example.util.KycUploadManager
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SolverKycScreen(
    viewModel: SomadhanViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val liveLocation by viewModel.liveLocation.collectAsStateWithLifecycle()
    // Loading/Sync Fix Roadmap v2, ধাপ ৪ (Group B) — currentUser (KYC ফিল্ড সহ) bulk-pull-এর
    // অংশ, তাই initialSyncPhase প্রাসঙ্গিক।
    val initialSyncPhase by viewModel.initialSyncPhase.collectAsStateWithLifecycle()

    val documentTypes = listOf(
        "জাতীয় পরিচয়পত্র / স্মার্টকার্ড",
        "পাসপোর্ট",
        "ড্রাইভিং লাইসেন্স",
        "জন্ম নিবন্ধন সনদ"
    )

    var selectedDocType by remember { mutableStateOf(currentUser?.kycDocumentType ?: documentTypes.first()) }
    var docTypeExpanded by remember { mutableStateOf(false) }
    var documentNumber by remember { mutableStateOf(currentUser?.kycDocumentNumber ?: "") }

    var docFrontUri by remember { mutableStateOf<Uri?>(null) }
    var docBackUri by remember { mutableStateOf<Uri?>(null) }
    var selfieUri by remember { mutableStateOf<Uri?>(null) }

    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }
    var isUploading by remember { mutableStateOf(false) }
    var uploadProgressText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Activity Result Launchers
    val frontPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            docFrontUri = uri
            errorMessage = null
        }
    }

    val backPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            docBackUri = uri
            errorMessage = null
        }
    }

    val selfieCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success && tempCameraUri != null) {
            selfieUri = tempCameraUri
            errorMessage = null
        }
    }

    // সেশন ২.১১ — আসল ক্যামেরা ওপেন করার লজিক আলাদা ফাংশনে রাখা হলো, যাতে পারমিশন
    // আগে থেকেই থাকলে (checkSelfPermission) সরাসরি এটা কল করা যায়, আর পারমিশন
    // popup-এ "Allow" করার পরও (নিচে cameraPermissionLauncher-এর callback থেকে) একই
    // ফাংশন কল করে সাথে সাথে ক্যামেরা খোলা যায় — কোনো ডুপ্লিকেট কোড ছাড়াই।
    fun openSelfieCamera() {
        try {
            val cacheFile = File(context.cacheDir, "kyc_selfie_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                cacheFile
            )
            tempCameraUri = uri
            selfieCameraLauncher.launch(uri)
        } catch (e: Exception) {
            errorMessage = "ক্যামেরা খুলতে সমস্যা হয়েছে: ${e.message}"
        }
    }

    // সেশন ২.১১ ফিক্স — CAMERA রানটাইম পারমিশন না থাকলে standard Android
    // permission-request popup ট্রিগার করে। Allow করলে openSelfieCamera() কল হয়ে
    // সাথে সাথে ক্যামেরা খোলে; Deny করলে rationale-সহ নির্দিষ্ট মেসেজ দেখানো হয়।
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            openSelfieCamera()
        } else {
            errorMessage = "সেলফি তোলার জন্য ক্যামেরা পারমিশন প্রয়োজন। অনুগ্রহ করে অ্যাপের সেটিংসে " +
                "গিয়ে ক্যামেরা পারমিশন অনুমোদন করুন, তারপর আবার চেষ্টা করুন।"
        }
    }

    fun launchCameraForSelfie() {
        val hasCameraPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (hasCameraPermission) {
            openSelfieCamera()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // [MONEY_FLOW_AND_ADMIN_BUGS ধাপ ৯ — বাগ E] আগে isKycVerified/isKycPending
    // kycDocumentNumber-এর উপস্থিতি থেকে অনুমান করা হতো (isKycVerified == false &&
    // documentNumber ফাঁকা না) — এটা "rejected" অবস্থাকেও ভুলভাবে "pending" দেখাতো, কারণ
    // reject করার পরও kycDocumentNumber ফাঁকা হয় না। এখন সরাসরি user.kycStatus
    // ("none"/"pending"/"verified"/"rejected") থেকে source-of-truth নেওয়া হচ্ছে — এই কলামটাই
    // submitKyc()/adminApproveKyc()/adminRejectKyc() (Repository) সেট করে।
    val kycStatus = currentUser?.kycStatus ?: "none"
    val isKycVerified = kycStatus == "verified"

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            text = "KYC ভেরিফিকেশন",
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
            sessionKey = "solver_kyc_sync",
            viewModel = viewModel,
            syncPhase = initialSyncPhase,
            onRetry = { viewModel.retryInitialSync() },
            modifier = Modifier.padding(paddingValues)
        ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(SomadhanBg)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            if (isKycVerified) {
                // Verified Status Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = SomadhanSuccessLight),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SomadhanSuccess.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = "যাচাইকৃত",
                            tint = SomadhanSuccess,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "আপনার KYC ভেরিফিকেশন সম্পন্ন হয়েছে!",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = SomadhanSuccess
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "আপনার পরিচয়পত্র সফলভাবে অনুমোদিত। এখন আপনি সব ধরণের কাজে নির্দ্বিধায় বিড করতে পারেন।",
                            fontSize = 12.sp,
                            color = SomadhanTextSecondary,
                            lineHeight = 17.sp
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        if (currentUser?.kycDocumentType != null) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = SomadhanBg),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "ডকুমেন্টের ধরন: ${currentUser?.kycDocumentType ?: ""}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = SomadhanTextPrimary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "ডকুমেন্ট নম্বর: ${currentUser?.kycDocumentNumber ?: ""}",
                                        fontSize = 12.sp,
                                        color = SomadhanTextSecondary
                                    )
                                    if (currentUser?.kycSubmissionDate != null) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "অনুমোদনের তারিখ: ${Formatters.formatDateBengali(currentUser?.kycSubmissionDate ?: 0L)}",
                                            fontSize = 11.sp,
                                            color = SomadhanTextHint
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else if (kycStatus == "pending") {
                // [ধাপ ৯ — বাগ E] Pending অবস্থায় ফর্ম/আপলোড UI সম্পূর্ণ হাইড — শুধু এই স্ট্যাটাস
                // কার্ডটাই দেখানো হয়, নিচে আর কোনো ইনপুট বক্স/আপলোড বাটন/সাবমিট বাটন থাকবে না
                // (আগে এই ব্লকের পরেও KYC Form Container আনকন্ডিশনালি রেন্ডার হতো, যেটাই এই
                // বাগের root cause ছিল — pending অবস্থায়ও রিসাবমিট করা যেত)।
                Card(
                    colors = CardDefaults.cardColors(containerColor = SomadhanOrangeLight),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SomadhanOrange.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(SomadhanOrange),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.HourglassTop,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "আপনার KYC আবেদন যাচাইয়ের অপেক্ষায় আছে",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = SomadhanOrange,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "আপনার পূর্বে জমা দেওয়া ডকুমেন্ট ও ছবি অ্যাডমিন টিম যাচাই করছে। ফলাফল না আসা পর্যন্ত নতুন করে জমা দেওয়ার প্রয়োজন নেই — অনুমোদন বা বাতিল হলে আপনাকে নোটিফিকেশনে জানানো হবে।",
                            fontSize = 12.sp,
                            color = SomadhanTextSecondary,
                            lineHeight = 17.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )

                        if (currentUser?.kycSubmissionDate != null) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Card(
                                colors = CardDefaults.cardColors(containerColor = SomadhanBg),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    if (currentUser?.kycDocumentType != null) {
                                        Text(
                                            text = "ডকুমেন্টের ধরন: ${currentUser?.kycDocumentType ?: ""}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = SomadhanTextPrimary
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                    }
                                    Text(
                                        text = "জমা দেওয়ার তারিখ: ${Formatters.formatDateBengali(currentUser?.kycSubmissionDate ?: 0L)}",
                                        fontSize = 11.sp,
                                        color = SomadhanTextHint
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                if (kycStatus == "rejected") {
                    // [ধাপ ৯ — বাগ E] Rejected অবস্থায় কারণ (kycRejectReason) স্পষ্টভাবে দেখানো, এর
                    // নিচেই ফর্ম আবার খোলে (রিসাবমিট করা যাবে) — নিচের "KYC Form Container" ব্লক
                    // অপরিবর্তিত থাকছে।
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SomadhanError.copy(alpha = 0.08f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, SomadhanError.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(SomadhanError),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "আপনার KYC আবেদন প্রত্যাখ্যাত হয়েছে",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SomadhanError
                                )
                                if (!currentUser?.kycRejectReason.isNullOrBlank()) {
                                    Text(
                                        text = "কারণ: ${currentUser?.kycRejectReason}",
                                        fontSize = 11.sp,
                                        color = SomadhanTextSecondary,
                                        lineHeight = 15.sp
                                    )
                                }
                                Text(
                                    text = "নিচে সঠিক তথ্য ও ছবি দিয়ে আবার জমা দিন।",
                                    fontSize = 11.sp,
                                    color = SomadhanTextSecondary,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                } else {
                    // KYC Info Banner (kycStatus == "none" / খালি — প্রথমবার জমা দেওয়ার ফর্ম)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SomadhanOrangeLight),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = SomadhanOrange,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "সমাধানকারী KYC ভেরিফিকেশন",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SomadhanOrange
                                )
                                Text(
                                    text = "কাজে বিড করার জন্য KYC যাচাইকরণ বাধ্যতামূলক।",
                                    fontSize = 11.sp,
                                    color = SomadhanTextSecondary
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // KYC Form Container
                Card(
                    colors = CardDefaults.cardColors(containerColor = SomadhanBg),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SomadhanDivider, RoundedCornerShape(14.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "ডকুমেন্টের ধরন নির্বাচন করুন *",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SomadhanTextPrimary
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        ExposedDropdownMenuBox(
                            expanded = docTypeExpanded,
                            onExpandedChange = { docTypeExpanded = !docTypeExpanded }
                        ) {
                            OutlinedTextField(
                                value = selectedDocType,
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = docTypeExpanded) },
                                leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null, tint = SomadhanOrange) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = SomadhanOrange,
                                    unfocusedBorderColor = SomadhanBorder,
                                    focusedContainerColor = SomadhanBg,
                                    unfocusedContainerColor = SomadhanCardBg
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor()
                            )

                            ExposedDropdownMenu(
                                expanded = docTypeExpanded,
                                onDismissRequest = { docTypeExpanded = false }
                            ) {
                                documentTypes.forEach { type ->
                                    DropdownMenuItem(
                                        text = { Text(type, fontSize = 13.sp) },
                                        onClick = {
                                            selectedDocType = type
                                            docTypeExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "ডকুমেন্ট / আইডি নম্বর *",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SomadhanTextPrimary
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        OutlinedTextField(
                            value = documentNumber,
                            onValueChange = {
                                documentNumber = it
                                errorMessage = null
                            },
                            placeholder = { Text("ডকুমেন্ট নম্বর প্রবেশ করান", fontSize = 13.sp, color = SomadhanTextHint) },
                            leadingIcon = { Icon(Icons.Default.Numbers, contentDescription = null, tint = SomadhanOrange) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SomadhanOrange,
                                unfocusedBorderColor = SomadhanBorder,
                                focusedContainerColor = SomadhanBg,
                                unfocusedContainerColor = SomadhanCardBg
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("kyc_number_input")
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        // ================= 1. FRONT IMAGE SECTION =================
                        Text(
                            text = "১. ডকুমেন্টের সামনের ছবি (Front Side) *",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = SomadhanTextPrimary
                        )
                        Text(
                            text = "গ্যালারি থেকে আপনার ডকুমেন্টের স্পষ্ট সামনের ছবি বাছাই করুন",
                            fontSize = 11.sp,
                            color = SomadhanTextSecondary
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        if (docFrontUri != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(150.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(1.5.dp, SomadhanSuccess, RoundedCornerShape(10.dp))
                                    .background(SomadhanSuccessLight)
                            ) {
                                AsyncImage(
                                    model = docFrontUri,
                                    contentDescription = "সামনের ছবি",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                Row(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(8.dp)
                                        .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(6.dp))
                                        .clickable { frontPickerLauncher.launch("image/*") }
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("ছবি পরিবর্তন", fontSize = 11.sp, color = Color.White)
                                }
                                Row(
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(8.dp)
                                        .background(SomadhanSuccess, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("সামনের ছবি সংগৃহীত", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(SomadhanCardBg)
                                    .border(1.dp, SomadhanDivider, RoundedCornerShape(10.dp))
                                    .clickable { frontPickerLauncher.launch("image/*") }
                                    .padding(12.dp)
                                    .testTag("upload_front_image_box"),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.PhotoLibrary,
                                        contentDescription = null,
                                        tint = SomadhanOrange,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "গ্যালারি থেকে সামনের ছবি বাছাই করুন",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = SomadhanTextPrimary
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // ================= 2. BACK IMAGE SECTION =================
                        Text(
                            text = "২. ডকুমেন্টের পেছনের ছবি (Back Side) *",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = SomadhanTextPrimary
                        )
                        Text(
                            text = "গ্যালারি থেকে আপনার ডকুমেন্টের স্পষ্ট পেছনের ছবি বাছাই করুন",
                            fontSize = 11.sp,
                            color = SomadhanTextSecondary
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        if (docBackUri != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(150.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(1.5.dp, SomadhanSuccess, RoundedCornerShape(10.dp))
                                    .background(SomadhanSuccessLight)
                            ) {
                                AsyncImage(
                                    model = docBackUri,
                                    contentDescription = "পেছনের ছবি",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                Row(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(8.dp)
                                        .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(6.dp))
                                        .clickable { backPickerLauncher.launch("image/*") }
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("ছবি পরিবর্তন", fontSize = 11.sp, color = Color.White)
                                }
                                Row(
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(8.dp)
                                        .background(SomadhanSuccess, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("পেছনের ছবি সংগৃহীত", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(SomadhanCardBg)
                                    .border(1.dp, SomadhanDivider, RoundedCornerShape(10.dp))
                                    .clickable { backPickerLauncher.launch("image/*") }
                                    .padding(12.dp)
                                    .testTag("upload_back_image_box"),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.PhotoLibrary,
                                        contentDescription = null,
                                        tint = SomadhanOrange,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "গ্যালারি থেকে পেছনের ছবি বাছাই করুন",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = SomadhanTextPrimary
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // ================= 3. LIVE SELFIE CAMERA SECTION =================
                        Text(
                            text = "৩. লাইভ সেলফি (Live Camera Selfie) *",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = SomadhanTextPrimary
                        )
                        Text(
                            text = "সরাসরি ক্যামেরা ওপেন করে আপনার স্পষ্ট ও সোজা মুখের সেলফি তুলুন",
                            fontSize = 11.sp,
                            color = SomadhanTextSecondary
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        if (selfieUri != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(170.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(1.5.dp, SomadhanSuccess, RoundedCornerShape(10.dp))
                                    .background(SomadhanSuccessLight)
                            ) {
                                AsyncImage(
                                    model = selfieUri,
                                    contentDescription = "সেলফি",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                Row(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(8.dp)
                                        .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(6.dp))
                                        .clickable { launchCameraForSelfie() }
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("আবার ছবি তুলুন", fontSize = 11.sp, color = Color.White)
                                }
                                Row(
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(8.dp)
                                        .background(SomadhanSuccess, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("সেলফি তোলা সম্পন্ন", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(110.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(SomadhanOrangeLight)
                                    .border(1.dp, SomadhanOrange.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                    .clickable { launchCameraForSelfie() }
                                    .padding(12.dp)
                                    .testTag("take_selfie_box"),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.CameraAlt,
                                        contentDescription = null,
                                        tint = SomadhanOrange,
                                        modifier = Modifier.size(30.dp)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "ক্যামেরা ওপেন করে সেলফি তুলুন",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SomadhanOrange
                                    )
                                    Text(
                                        text = "(গ্যালারি থেকে নয়, সরাসরি লাইভ ক্যামেরা চালু হবে)",
                                        fontSize = 10.sp,
                                        color = SomadhanTextSecondary
                                    )
                                }
                            }
                        }

                        if (errorMessage != null) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Card(
                                colors = CardDefaults.cardColors(containerColor = SomadhanError.copy(alpha = 0.1f)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth().border(1.dp, SomadhanError.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            ) {
                                Text(
                                    text = errorMessage ?: "",
                                    fontSize = 12.sp,
                                    color = SomadhanError,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = {
                                if (isUploading) return@Button

                                if (documentNumber.isBlank()) {
                                    errorMessage = "অনুগ্রহ করে ডকুমেন্ট / আইডি নম্বর লিখুন।"
                                    return@Button
                                }
                                if (docFrontUri == null) {
                                    errorMessage = "অনুগ্রহ করে ডকুমেন্টের সামনের ছবি (Front Side) নির্বাচন করুন।"
                                    return@Button
                                }
                                if (docBackUri == null) {
                                    errorMessage = "অনুগ্রহ করে ডকুমেন্টের পেছনের ছবি (Back Side) নির্বাচন করুন।"
                                    return@Button
                                }
                                if (selfieUri == null) {
                                    errorMessage = "অনুগ্রহ করে ক্যামেরা দিয়ে আপনার সেলফি তুলুন।"
                                    return@Button
                                }

                                val user = currentUser
                                if (user == null) {
                                    errorMessage = "ব্যবহারকারী সনাক্ত করা যায়নি। অনুগ্রহ করে পুনরায় লগইন করুন।"
                                    return@Button
                                }

                                isUploading = true
                                errorMessage = null
                                uploadProgressText = "সামনের ছবি আপলোড হচ্ছে..."

                                coroutineScope.launch {
                                    try {
                                        // 1. Upload Front Image
                                        uploadProgressText = "সামনের ছবি আপলোড হচ্ছে..."
                                        val frontResult = KycUploadManager.uploadKycImage(context, user.id, "front", docFrontUri!!)
                                        if (frontResult.isFailure) {
                                            errorMessage = frontResult.exceptionOrNull()?.message ?: "সামনের ছবি আপলোড ব্যর্থ হয়েছে।"
                                            isUploading = false
                                            return@launch
                                        }
                                        val frontUrl = frontResult.getOrThrow()

                                        // 2. Upload Back Image
                                        uploadProgressText = "পেছনের ছবি আপলোড হচ্ছে..."
                                        val backResult = KycUploadManager.uploadKycImage(context, user.id, "back", docBackUri!!)
                                        if (backResult.isFailure) {
                                            errorMessage = backResult.exceptionOrNull()?.message ?: "পেছনের ছবি আপলোড ব্যর্থ হয়েছে।"
                                            isUploading = false
                                            return@launch
                                        }
                                        val backUrl = backResult.getOrThrow()

                                        // 3. Upload Selfie
                                        uploadProgressText = "সেলফি আপলোড হচ্ছে..."
                                        val selfieResult = KycUploadManager.uploadKycImage(context, user.id, "selfie", selfieUri!!)
                                        if (selfieResult.isFailure) {
                                            errorMessage = selfieResult.exceptionOrNull()?.message ?: "সেলফি আপলোড ব্যর্থ হয়েছে।"
                                            isUploading = false
                                            return@launch
                                        }
                                        val selfieUrl = selfieResult.getOrThrow()

                                        // 4. Submit KYC to ViewModel & Database
                                        uploadProgressText = "KYC আবেদন জমা দেওয়া হচ্ছে..."
                                        viewModel.submitKyc(
                                            documentType = selectedDocType,
                                            documentNumber = documentNumber.trim(),
                                            docFrontUri = frontUrl,
                                            docBackUri = backUrl,
                                            selfieUri = selfieUrl,
                                            onSuccess = {
                                                isUploading = false
                                                onNavigateBack()
                                            },
                                            onError = { err ->
                                                isUploading = false
                                                errorMessage = err
                                            }
                                        )
                                    } catch (e: Exception) {
                                        isUploading = false
                                        errorMessage = "আপলোড ব্যর্থ হয়েছে: ${e.localizedMessage ?: e.message}"
                                    }
                                }
                            },
                            enabled = !isUploading,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SomadhanOrange,
                                disabledContainerColor = SomadhanOrange.copy(alpha = 0.6f)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("submit_kyc_btn")
                        ) {
                            if (isUploading) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = uploadProgressText.ifBlank { "আপলোড হচ্ছে..." },
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Shield,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "KYC জমা দিন",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
        }
    }
}
