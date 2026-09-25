package com.example.ui.screens

/**
 * [ADMIN_ROLE_PROFILE সেশন ৬] `AdminProfileView` — টপ-বারের অবতারে ট্যাপ করলে খোলা প্রোফাইল ওভারলে।
 *
 * রেফারেন্স: `admin-profile.html` মকআপ (লেআউট/কপি — লজিক না)। কনভেনশন: `AdminAccountsView.kt`
 * (হেল্পার পুনর্ব্যবহার: [adminInitials], [formatAdminTimestamp]), `BottomSlideAlertDialog`।
 *
 * - সাধারণ এডমিন: শুধু নিজের প্রোফাইল দেখে/এডিট করে।
 * - সুপার অ্যাডমিন: বাম কলামে সব এডমিনের সার্চেবল তালিকা (মকআপের মতো), যেকোনো একজন বেছে দেখতে/এডিট করতে পারে।
 * - রোল/ফোন এখান থেকে বদলানো যায় না (সার্ভারেও লক); পারমিশন-তালিকা শুধু-পড়ার (রোল-এডিটর থেকে নির্ধারিত)।
 * - পাসওয়ার্ড বদল শুধু নিজের জন্য (সেশন ২-এর `adminUpdateCredentials`, বর্তমান পাসওয়ার্ড re-verify করে) —
 *   সুপার অন্যের পাসওয়ার্ড বদলাতে চাইলে বিদ্যমান "এডমিন অ্যাকাউন্ট" ট্যাবের রিসেট-পাসওয়ার্ড ব্যবহার করবে।
 *
 * ⚠️ কম্পাইল/রান করা হয়নি (এই এনভায়রনমেন্টে Gradle/SDK নেই)।
 */

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import com.example.data.security.AdminAccountInfo
import com.example.data.security.AdminSessionRecord
import com.example.data.security.adminPermissionSummary
import com.example.ui.components.AdminAvatar
import com.example.ui.components.BottomSlideAlertDialog
import com.example.ui.theme.SomadhanBg
import com.example.ui.theme.SomadhanBorder
import com.example.ui.theme.SomadhanCardBg
import com.example.ui.theme.SomadhanError
import com.example.ui.theme.SomadhanErrorLight
import com.example.ui.theme.SomadhanOrange
import com.example.ui.theme.SomadhanOrangeContainer
import com.example.ui.theme.SomadhanOrangeLight
import com.example.ui.theme.SomadhanOrangePressed
import com.example.ui.theme.SomadhanSuccess
import com.example.ui.theme.SomadhanTextHint
import com.example.ui.theme.SomadhanTextPrimary
import com.example.ui.theme.SomadhanTextSecondary
import com.example.ui.viewmodel.SomadhanViewModel

/** সার্ভারের raise-করা কোড → বাংলা বার্তা (প্রোফাইল RPC-র নিজস্ব কোডগুলো + সাধারণগুলো `accountErrorMessage`-এর মতো)। */
fun profileErrorMessage(raw: String): String = when {
    raw.contains("INVALID_NAME") -> "নাম দিন (সর্বোচ্চ ৮০ অক্ষর)।"
    raw.contains("INVALID_DESIGNATION") -> "পদবি সর্বোচ্চ ৮০ অক্ষরের হতে পারবে।"
    raw.contains("INVALID_EMAIL") -> "ইমেইল ঠিকানাটা সঠিক নয়।"
    raw.contains("INVALID_BIO") -> "সংক্ষিপ্ত পরিচিতি সর্বোচ্চ ৫০০ অক্ষরের হতে পারবে।"
    raw.contains("ACCOUNT_NOT_FOUND") -> "অ্যাকাউন্টটা খুঁজে পাওয়া যায়নি।"
    raw.contains("ACCOUNT_INACTIVE") -> "এই অ্যাকাউন্ট নিষ্ক্রিয়।"
    raw.contains("SUPER_ADMIN_REQUIRED") || raw.contains("AUTH_REQUIRED") -> "শুধু সুপার অ্যাডমিন অন্যের প্রোফাইল বদলাতে পারবেন।"
    else -> "প্রোফাইল সংরক্ষণ করা যায়নি। ইন্টারনেট দেখে আবার চেষ্টা করুন।"
}

/** ছবি-আপলোড ব্যর্থতার কোড ([AdminProfilePhotoUploader]) → বাংলা বার্তা। */
fun photoUploadErrorMessage(raw: String): String = when {
    raw.contains("PHOTO_TOO_LARGE") -> "ছবিটা অনেক বড় (সর্বোচ্চ ২MB)। অন্য ছবি বাছুন।"
    raw.contains("PHOTO_READ_FAILED") -> "ছবিটা পড়া যায়নি। অন্য ছবি বাছুন।"
    raw.contains("PHOTO_UPLOAD_TIMEOUT") -> "ছবি আপলোড সময়মতো শেষ হয়নি। ইন্টারনেট দেখে আবার চেষ্টা করুন।"
    raw.contains("AUTH_REQUIRED") -> "সেশন পাওয়া যায়নি — আবার লগইন করুন।"
    else -> "ছবি আপলোড করা যায়নি। ইন্টারনেট দেখে আবার চেষ্টা করুন।"
}

@Composable
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
fun AdminProfileView(
    viewModel: SomadhanViewModel,
    currentAdminId: String,
    isSuper: Boolean,
    allAccounts: List<AdminAccountInfo>,
    onBack: () -> Unit
) {
    var selectedId by remember { mutableStateOf(currentAdminId) }
    var query by remember { mutableStateOf("") }

    // সুপার হলে: হেডলাইন-অ্যাকাউন্ট [allAccounts] থেকে (রোল-এডিট/ফ্ল্যাগ তাৎক্ষণিক প্রতিফলিত)। সুপার না হলে
    // শুধু নিজেরটাই দেখানো হবে — [allAccounts] তখন ফাঁকা থাকতে পারে (সুপার-অনলি RPC), তাই সেই কেসে ব্যবহার হয় না।
    val selected = if (isSuper) {
        allAccounts.find { it.id == selectedId } ?: allAccounts.find { it.id == currentAdminId }
    } else {
        allAccounts.find { it.id == currentAdminId }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("এডমিন প্রোফাইল", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = SomadhanTextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "পেছনে যান", tint = SomadhanTextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SomadhanBg)
            )
        },
        containerColor = SomadhanBg
    ) { padding ->
        if (isSuper) {
            Row(modifier = Modifier.fillMaxSize().padding(padding)) {
                // ---------- বাম কলাম: সব এডমিন ----------
                Column(modifier = Modifier.width(220.dp).fillMaxSize().padding(8.dp)) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text("নাম দিয়ে খুঁজুন", fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    val filtered = filterAdminAccounts(allAccounts, query)
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(filtered, key = { it.id }) { a ->
                            AdminMiniListItem(
                                account = a,
                                selected = a.id == selectedId,
                                onClick = { selectedId = a.id }
                            )
                        }
                    }
                }
                if (selected != null) {
                    Box(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp).verticalScroll(rememberScrollState())) {
                        AdminProfileCard(
                            viewModel = viewModel,
                            account = selected,
                            isSelf = selected.id == currentAdminId,
                            canEdit = true
                        )
                    }
                }
            }
        } else if (selected != null) {
            Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 12.dp).verticalScroll(rememberScrollState())) {
                AdminProfileCard(viewModel = viewModel, account = selected, isSelf = true, canEdit = true)
            }
        } else {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = SomadhanOrange)
            }
        }
    }
}

@Composable
private fun AdminMiniListItem(account: AdminAccountInfo, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) SomadhanOrangeLight else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AdminAvatar(name = account.name, photo = account.photoUrl, isSuper = account.isSuper, size = 32.dp)
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(account.name, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = SomadhanTextPrimary, maxLines = 1)
                if (account.flagged) Text(" 🚩", fontSize = 11.sp)
            }
            Text(account.designation.ifBlank { account.roleName }, fontSize = 11.sp, color = SomadhanTextHint, maxLines = 1)
        }
    }
}

@Composable
private fun AdminProfileCard(
    viewModel: SomadhanViewModel,
    account: AdminAccountInfo,
    isSelf: Boolean,
    canEdit: Boolean
) {
    val context = LocalContext.current

    // ড্রাফট ফিল্ড — [account.id] বদলালে (সুপার অন্য কাউকে বেছে নিলে) রিসেট হয়, সংরক্ষণের আগ পর্যন্ত টাইপ করা মান ধরে রাখে।
    var name by remember(account.id) { mutableStateOf(account.name) }
    var designation by remember(account.id) { mutableStateOf(account.designation) }
    var email by remember(account.id) { mutableStateOf(account.email.orEmpty()) }
    var bio by remember(account.id) { mutableStateOf(account.bio) }
    var pickedPhotoUri by remember(account.id) { mutableStateOf<Uri?>(null) }
    var uploadedPhotoUrl by remember(account.id) { mutableStateOf<String?>(null) }
    var removePhoto by remember(account.id) { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var sessions by remember(account.id) { mutableStateOf<List<AdminSessionRecord>?>(null) }

    LaunchedEffect(account.id) {
        sessions = null
        viewModel.fetchAdminSessions(account.id) { sessions = it }
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            pickedPhotoUri = uri
            uploadedPhotoUrl = null
            removePhoto = false
        }
    }

    val displayPhoto: Any? = when {
        pickedPhotoUri != null -> pickedPhotoUri
        removePhoto -> null
        else -> account.photoUrl
    }
    val dirty = name.trim() != account.name || designation.trim() != account.designation ||
        email.trim() != account.email.orEmpty() || bio.trim() != account.bio ||
        pickedPhotoUri != null || removePhoto

    fun resetDraft() {
        name = account.name; designation = account.designation
        email = account.email.orEmpty(); bio = account.bio
        pickedPhotoUri = null; uploadedPhotoUrl = null; removePhoto = false
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = SomadhanCardBg),
        border = BorderStroke(1.dp, if (account.isSuper) SomadhanOrange else SomadhanBorder),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // ---------- প্রিমিয়াম হেডার ব্যান্ড: ফুল-ব্লিড গ্র্যাডিয়েন্ট, অ্যাভাটার/নাম/পদবি/রোল-ব্যাজ কেন্দ্রে
            // (আগের Row(SpaceBetween) ভার্সনে নাম-কলাম আর রোল-ব্যাজ পাশাপাশি বসানো হতো — লম্বা নাম/ব্যাজ একসাথে
            // থাকলে নাম-কলাম জায়গা না পেয়ে অক্ষর-বাই-অক্ষর ভেঙে যেত; সবকিছু উলম্বভাবে কেন্দ্রে সাজিয়ে এই সমস্যা
            // পুরোপুরি এড়ানো হলো, সাথে maxLines=1 + ellipsis সেফটি-নেট হিসেবে রইল) ----------
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(
                                if (account.isSuper) SomadhanOrangeContainer else SomadhanOrangeLight,
                                SomadhanCardBg
                            )
                        )
                    )
                    .padding(top = 28.dp, bottom = 18.dp, start = 20.dp, end = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box {
                        AdminAvatar(name = account.name, photo = displayPhoto, isSuper = account.isSuper, size = 92.dp, ring = true)
                        if (canEdit) {
                            IconButton(
                                onClick = { galleryLauncher.launch("image/*") },
                                modifier = Modifier
                                    .size(32.dp)
                                    .align(Alignment.BottomEnd)
                                    .clip(CircleShape)
                                    .background(SomadhanOrange)
                                    .border(2.dp, SomadhanCardBg, CircleShape)
                            ) {
                                Icon(Icons.Default.CameraAlt, contentDescription = "ছবি বদলান", tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    if (canEdit && displayPhoto != null) {
                        TextButton(
                            onClick = { pickedPhotoUri = null; uploadedPhotoUrl = null; removePhoto = true },
                            modifier = Modifier.height(28.dp)
                        ) { Text("ছবি সরান", color = SomadhanError, fontSize = 12.sp) }
                    } else {
                        Text("JPG/PNG, সর্বোচ্চ ২MB", fontSize = 11.sp, color = SomadhanTextHint)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        account.name,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = SomadhanTextPrimary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    if (account.designation.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            account.designation,
                            fontSize = 13.sp,
                            color = SomadhanTextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(if (account.isSuper) SomadhanOrangeContainer else SomadhanOrangeLight)
                            .border(
                                1.dp,
                                if (account.isSuper) SomadhanOrangePressed else SomadhanOrange,
                                RoundedCornerShape(50)
                            )
                            .padding(horizontal = 12.dp, vertical = 5.dp)
                    ) {
                        Text(
                            (if (account.isSuper) "🔒 " else "") + account.roleName,
                            fontSize = 12.sp, fontWeight = FontWeight.Bold,
                            color = if (account.isSuper) SomadhanOrangePressed else SomadhanOrange
                        )
                    }
                }
            }

            if (account.flagged) {
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(SomadhanErrorLight)
                        .padding(10.dp)
                ) {
                    Text(
                        "🚩 এই অ্যাকাউন্টটি সুপার অ্যাডমিন কর্তৃক ফ্ল্যাগড — সব মেনু দেখা যাচ্ছে কিন্তু কোনো অ্যাকশন নেওয়া যাবে না, যতক্ষণ না ফ্ল্যাগ তুলে নেওয়া হয়।",
                        fontSize = 12.sp, color = SomadhanError
                    )
                }
            }

            Column(modifier = Modifier.padding(horizontal = 16.dp).padding(top = 16.dp, bottom = 16.dp)) {
            SectionTitle("মূল তথ্য")
            OutlinedTextField(
                value = name, onValueChange = { name = it },
                label = { Text("পূর্ণ নাম *") }, enabled = canEdit, singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = designation, onValueChange = { designation = it },
                label = { Text("পদবি") }, enabled = canEdit, singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = account.phone, onValueChange = {}, enabled = false,
                label = { Text("ফোন নম্বর") },
                supportingText = { Text("লগইন-আইডি — পরিবর্তনের জন্য সুপার অ্যাডমিনের সাথে যোগাযোগ করুন।", fontSize = 11.sp) },
                singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = email, onValueChange = { email = it },
                label = { Text("ইমেইল") }, enabled = canEdit, singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))
            SectionTitle("সম্পর্কে")
            OutlinedTextField(
                value = bio, onValueChange = { bio = it },
                label = { Text("সংক্ষিপ্ত পরিচিতি") }, enabled = canEdit,
                placeholder = { Text("নিজের দায়িত্ব সম্পর্কে ২-৩ লাইন লিখুন...") },
                minLines = 2, maxLines = 4,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))
            SectionTitle("পদবি অনুযায়ী ফাংশন এক্সেস")
            Text(
                "এই তালিকা রোল ম্যানেজমেন্ট থেকে নির্ধারিত — এখান থেকে পরিবর্তন করা যাবে না।",
                fontSize = 11.sp, color = SomadhanTextHint, modifier = Modifier.padding(bottom = 8.dp)
            )
            val permChips = adminPermissionSummary(account.isSuper, account.permissions)
            if (permChips.isEmpty()) {
                Text("কোনো ফাংশন-এক্সেস নেই।", fontSize = 12.sp, color = SomadhanTextHint)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    permChips.forEach { chip ->
                        Text("✓ $chip", fontSize = 12.sp, color = SomadhanTextSecondary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            SectionTitle("লগইন ও সেশন তথ্য")
            val online = account.isOnline && account.active
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(9.dp).clip(CircleShape).background(if (online) SomadhanSuccess else SomadhanTextHint))
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (online) "এই মুহূর্তে অনলাইন" else "অফলাইন", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SomadhanTextPrimary)
            }
            Text("সর্বশেষ লগইন: ${formatAdminTimestamp(account.lastLoginAt)}", fontSize = 12.sp, color = SomadhanTextSecondary)
            if (account.lastLoginDevice?.isNotBlank() == true) {
                Text("ডিভাইস: ${account.lastLoginDevice}", fontSize = 12.sp, color = SomadhanTextSecondary)
            }
            if (account.lastLoginIp?.isNotBlank() == true) {
                Text("আইপি অ্যাড্রেস: ${account.lastLoginIp}", fontSize = 12.sp, color = SomadhanTextSecondary)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("সাম্প্রতিক সেশন", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = SomadhanTextSecondary)
            val sessionList = sessions
            when {
                sessionList == null -> Text("লোড হচ্ছে…", fontSize = 12.sp, color = SomadhanTextHint)
                sessionList.isEmpty() -> Text("কোনো সেশন রেকর্ড নেই।", fontSize = 12.sp, color = SomadhanTextHint)
                else -> Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    sessionList.take(5).forEach { s ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("📱 ", fontSize = 12.sp)
                            Column {
                                Text(s.device.ifBlank { "অজানা ডিভাইস" }, fontSize = 12.sp, color = SomadhanTextPrimary)
                                val loc = s.location.ifBlank { s.ip.ifBlank { "—" } }
                                val time = if (s.endedAt == null) "এখন সক্রিয়" else formatAdminTimestamp(s.lastSeenAt)
                                Text("$loc · $time", fontSize = 11.sp, color = SomadhanTextHint)
                            }
                        }
                    }
                }
            }

            if (isSelf) {
                Spacer(modifier = Modifier.height(14.dp))
                SectionTitle("সিকিউরিটি")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = SomadhanTextSecondary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("পাসওয়ার্ড", fontSize = 13.sp, color = SomadhanTextPrimary)
                    }
                    OutlinedButton(onClick = { showPasswordDialog = true }) { Text("পরিবর্তন করুন", fontSize = 12.sp) }
                }
            }

            if (canEdit) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { resetDraft() },
                        enabled = dirty && !isSaving,
                        modifier = Modifier.weight(1f)
                    ) { Text("বাতিল") }
                    Button(
                        onClick = {
                            isSaving = true
                            viewModel.adminSaveProfile(
                                context = context,
                                target = account,
                                name = name,
                                designation = designation,
                                email = email,
                                bio = bio,
                                newPhotoUri = if (uploadedPhotoUrl == null) pickedPhotoUri else null,
                                uploadedPhotoUrl = uploadedPhotoUrl,
                                removePhoto = removePhoto,
                                onPhotoUploaded = { url -> uploadedPhotoUrl = url },
                                onDone = { ok ->
                                    isSaving = false
                                    if (ok) {
                                        pickedPhotoUri = null; uploadedPhotoUrl = null; removePhoto = false
                                    }
                                }
                            )
                        },
                        enabled = dirty && !isSaving,
                        colors = ButtonDefaults.buttonColors(containerColor = SomadhanOrange),
                        modifier = Modifier.weight(1f)
                    ) { Text(if (isSaving) "সংরক্ষণ হচ্ছে…" else "সংরক্ষণ করুন") }
                }
            }
            } // ---------- মূল তথ্য-থেকে-বাটন পর্যন্ত ভেতরের প্যাডেড কলাম শেষ ----------
        }
    }

    if (showPasswordDialog) {
        AdminSelfPasswordDialog(
            viewModel = viewModel,
            onDismiss = { showPasswordDialog = false }
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SomadhanTextPrimary, modifier = Modifier.padding(bottom = 8.dp))
}

/** নিজের পাসওয়ার্ড বদল — বর্তমান পাসওয়ার্ড re-verify করে (সেশন ২-এর `adminUpdateCredentials`)। */
@Composable
private fun AdminSelfPasswordDialog(viewModel: SomadhanViewModel, onDismiss: () -> Unit) {
    var current by remember { mutableStateOf("") }
    var newPw by remember { mutableStateOf("") }
    var visible by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    BottomSlideAlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text("পাসওয়ার্ড পরিবর্তন", fontWeight = FontWeight.Bold, color = SomadhanTextPrimary) },
        text = {
            Column {
                OutlinedTextField(
                    value = current, onValueChange = { current = it; error = null },
                    label = { Text("বর্তমান পাসওয়ার্ড") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = newPw, onValueChange = { newPw = it; error = null },
                    label = { Text("নতুন পাসওয়ার্ড (কমপক্ষে ৮ অক্ষর)") },
                    isError = error != null,
                    supportingText = { error?.let { Text(it, color = SomadhanError) } },
                    visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { visible = !visible }) {
                            Icon(if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = null)
                        }
                    },
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = !busy,
                onClick = {
                    if (current.isBlank()) {
                        error = "বর্তমান পাসওয়ার্ড লিখুন।"
                        return@TextButton
                    }
                    if (newPw.trim().length < 8) {
                        error = "নতুন পাসওয়ার্ড কমপক্ষে ৮ অক্ষরের হতে হবে।"
                        return@TextButton
                    }
                    busy = true
                    viewModel.adminUpdateCredentials(
                        newPhone = "",
                        currentPassword = current,
                        newPassword = newPw
                    ) { ok, message ->
                        busy = false
                        if (ok) onDismiss() else error = message
                    }
                }
            ) { Text(if (busy) "অপেক্ষা করুন…" else "বদলান", color = SomadhanOrange) }
        },
        dismissButton = { TextButton(enabled = !busy, onClick = onDismiss) { Text("বাতিল") } }
    )
}
