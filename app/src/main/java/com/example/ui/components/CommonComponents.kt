package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.StarHalf
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.Indicator
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlin.math.ceil
import kotlin.math.min
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import android.widget.Toast
import coil.compose.AsyncImage
import com.example.util.ImageStorageUtil
import com.example.data.entity.CategoryEntity
import com.example.data.entity.NotificationEntity
import com.example.data.entity.ProblemEntity
import com.example.data.entity.UserEntity
import com.example.ui.theme.SomadhanBg
import com.example.ui.theme.SomadhanBorder
import com.example.ui.theme.SomadhanCardBg
import com.example.ui.theme.SomadhanDivider
import com.example.ui.theme.SomadhanError
import com.example.ui.theme.SomadhanErrorLight
import com.example.ui.theme.SomadhanInfo
import com.example.ui.theme.SomadhanInfoLight
import com.example.ui.theme.SomadhanOrange
import com.example.ui.theme.SomadhanOrangeLight
import com.example.ui.theme.SomadhanOrangePressed
import com.example.ui.theme.SomadhanSuccess
import com.example.ui.theme.SomadhanSuccessLight
import com.example.ui.theme.SomadhanSurface
import com.example.ui.theme.SomadhanSurfaceVariant
import com.example.ui.theme.SomadhanTextHint
import com.example.ui.theme.SomadhanTextPrimary
import com.example.ui.theme.SomadhanTextSecondary
import com.example.ui.theme.SomadhanYellowVerified
import com.example.ui.theme.SomadhanYellowVerifiedBg
import com.example.util.DistanceUtil
import com.example.util.Formatters
import com.example.util.LocationResult

@Composable
fun ReputationBadge(
    score: Double,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    // Rich, deep background colors with matching high-contrast text and border
    val (bgColor, contentColor, borderColor, icon) = when {
        score >= 80 -> Quadruple(Color(0xFFDCFCE7), Color(0xFF15803D), Color(0xFF86EFAC), Icons.Default.Star)
        score >= 50 -> Quadruple(Color(0xFFE0F2FE), Color(0xFF0369A1), Color(0xFF7DD3FC), Icons.Default.StarHalf)
        else -> Quadruple(Color(0xFFFFEDD5), Color(0xFFC2410C), Color(0xFFFDBA74), Icons.Default.StarBorder)
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .testTag("reputation_badge")
    ) {
        Icon(icon, contentDescription = "রেপুটেশন স্কোর", tint = contentColor, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(3.dp))
        Text(
            text = DistanceUtil.toBengaliDigits(score.toInt().toString()),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = contentColor
        )
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SomadhanTopBar(
    title: String,
    currentUser: UserEntity?,
    unreadCount: Int,
    onNotificationClick: () -> Unit,
    onAdminClick: (() -> Unit)? = null,
    onReputationClick: (() -> Unit)? = null,
    onHistoryClick: (() -> Unit)? = null,
    showReputation: Boolean = true,
    locationAddress: String? = null,
    onLocationRefresh: (() -> Unit)? = null,
    isLocationUpdating: Boolean = false
) {
    Column {
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = SomadhanTextPrimary
                    )
                    if (currentUser != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        RoleBadge(role = currentUser.role)
                    }
                }
            },
            actions = {
                if (onHistoryClick != null) {
                    IconButton(
                        onClick = onHistoryClick,
                        modifier = Modifier.testTag("instant_job_history_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "জরুরি হিস্ট্রি",
                            tint = SomadhanTextPrimary
                        )
                    }
                }

                if (showReputation && currentUser != null) {
                    ReputationBadge(
                        score = currentUser.reputationScore,
                        onClick = onReputationClick
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }

                if (onAdminClick != null) {
                    IconButton(onClick = onAdminClick, modifier = Modifier.testTag("admin_button")) {
                        Icon(
                            imageVector = Icons.Default.AdminPanelSettings,
                            contentDescription = "অ্যাডমিন প্যানেল",
                            tint = SomadhanOrange
                        )
                    }
                }

                IconButton(
                    onClick = onNotificationClick,
                    modifier = Modifier.testTag("notification_button")
                ) {
                    BadgedBox(
                        badge = {
                            if (unreadCount > 0) {
                                Badge(
                                    containerColor = SomadhanOrange,
                                    contentColor = Color.White
                                ) {
                                    Text(DistanceUtil.toBengaliDigits(unreadCount.toString()))
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (unreadCount > 0) Icons.Filled.Notifications else Icons.Outlined.Notifications,
                            contentDescription = "বিজ্ঞপ্তি",
                            tint = SomadhanTextPrimary
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = SomadhanBg
            ),
            modifier = Modifier.border(width = 1.dp, color = SomadhanDivider)
        )

        if (locationAddress != null) {
            RealtimeLocationBar(
                locationAddress = locationAddress,
                onRefresh = onLocationRefresh,
                isUpdating = isLocationUpdating,
                isSolver = (currentUser?.role == "SOLVER")
            )
        }
    }
}

@Composable
fun RoleBadge(role: String) {
    val (label, bg, fg) = when (role) {
        "SOLVER" -> Triple("সমাধানকারী", SomadhanOrangeLight, SomadhanOrange)
        "ADMIN" -> Triple("অ্যাডমিন", SomadhanInfoLight, SomadhanInfo)
        else -> Triple("ব্যবহারকারী", SomadhanSuccessLight, SomadhanSuccess)
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = fg
        )
    }
}

@Composable
fun SomadhanBottomNav(
    currentRoute: String,
    isSolver: Boolean = false,
    isWalletEnabled: Boolean = true,
    isInstantJobEnabled: Boolean = true,
    unreadChatCount: Int = 0,
    onNavigate: (String) -> Unit
) {
    val selectedColor = if (isSolver) SomadhanOrange else Color(0xFF1D4ED8)
    val indicatorColor = if (isSolver) SomadhanOrangeLight else Color(0xFFDBEAFE)

    NavigationBar(
        containerColor = SomadhanBg,
        modifier = Modifier.border(width = 1.dp, color = SomadhanDivider)
    ) {
        val items = if (isSolver) {
            val list = mutableListOf(
                Triple("home", "হোম", Icons.Default.Home),
                Triple("dashboard", "ড্যাশবোর্ড", Icons.Default.Dashboard)
            )
            if (isInstantJobEnabled) {
                list.add(Triple("instant_hub", "জরুরি", Icons.Default.Bolt))
            }
            list.add(Triple("solver_all_posts", "সব পোস্ট", Icons.Default.ListAlt))
            list.add(Triple("profile", "প্রোফাইল", Icons.Default.Person))
            list
        } else {
            val list = mutableListOf(
                Triple("home", "হোম", Icons.Default.Home),
                Triple("dashboard", "ড্যাশবোর্ড", Icons.Default.Dashboard)
            )
            if (isInstantJobEnabled) {
                list.add(Triple("instant_hub", "জরুরি", Icons.Default.Bolt))
            }
            if (isWalletEnabled) {
                list.add(Triple("wallet", "ওয়ালেট", Icons.Default.AccountBalanceWallet))
            }
            list.add(Triple("profile", "প্রোফাইল", Icons.Default.Person))
            list
        }

        items.forEach { (route, label, icon) ->
            val selected = currentRoute == route || (route == "instant_hub" && (currentRoute == "instant_jobs" || currentRoute == "instant_hub" || currentRoute.startsWith("post_problem?instant=true")))
            if (route == "instant_hub") {
                NavigationBarItem(
                    selected = selected,
                    onClick = { onNavigate(route) },
                    icon = {
                        Box(
                            modifier = Modifier
                                .offset(y = (-6).dp)
                                .size(44.dp)
                                .shadow(elevation = if (selected) 8.dp else 5.dp, shape = CircleShape, clip = false)
                                .clip(CircleShape)
                                .background(if (selected) Color(0xFFEA580C) else SomadhanOrange)
                                .border(if (selected) 2.5.dp else 2.dp, Color.White, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = "জরুরি",
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    },
                    label = {
                        Text(
                            text = label,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = SomadhanOrange
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = SomadhanOrange,
                        indicatorColor = Color.Transparent,
                        unselectedIconColor = Color.White,
                        unselectedTextColor = SomadhanOrange
                    ),
                    modifier = Modifier.testTag("nav_$route")
                )
            } else {
                NavigationBarItem(
                    selected = selected,
                    onClick = { onNavigate(route) },
                    icon = {
                        if (route == "messages" && unreadChatCount > 0) {
                            BadgedBox(
                                badge = {
                                    Badge(
                                        containerColor = SomadhanOrange,
                                        contentColor = Color.White
                                    ) {
                                        Text(
                                            text = if (unreadChatCount > 99) "99+" else DistanceUtil.toBengaliDigits(unreadChatCount.toString()),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = label
                                )
                            }
                        } else {
                            Icon(
                                imageVector = icon,
                                contentDescription = label
                            )
                        }
                    },
                    label = {
                        Text(
                            text = label,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = if (isSolver) 11.sp else 12.sp
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = selectedColor,
                        selectedTextColor = selectedColor,
                        indicatorColor = indicatorColor,
                        unselectedIconColor = SomadhanTextSecondary,
                        unselectedTextColor = SomadhanTextSecondary
                    ),
                    modifier = Modifier.testTag("nav_$route")
                )
            }
        }
    }
}

@Composable
fun UserVerificationBadge(
    role: String = "USER",
    isKycVerified: Boolean = false,
    onKycClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    when (role) {
        "SOLVER" -> {
            if (isKycVerified) {
                // KYC সম্পন্ন সলভার: Green টিক মার্ক সহ Verified
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(SomadhanSuccessLight)
                        .then(if (onKycClick != null) Modifier.clickable { onKycClick() } else Modifier)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = "Verified Solver",
                        tint = SomadhanSuccess,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Verified",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = SomadhanSuccess
                    )
                }
            } else {
                // KYC অসম্পূর্ণ সলভার: Yellow টিক মার্ক সহ KYC
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(SomadhanYellowVerifiedBg)
                        .then(if (onKycClick != null) Modifier.clickable { onKycClick() } else Modifier)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = "KYC Pending Solver",
                        tint = SomadhanYellowVerified,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "KYC",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFB45309)
                    )
                }
            }
        }
        else -> {
            // সাধারণ User একাউন্টে লেখা থাকবে না, শুধু Yellow টিক মার্ক
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = "যাচাইকৃত ইউজার",
                tint = SomadhanYellowVerified,
                modifier = modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun YellowVerifiedBadge(
    role: String = "USER",
    isKycVerified: Boolean = false,
    onKycClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    UserVerificationBadge(role = role, isKycVerified = isKycVerified, onKycClick = onKycClick, modifier = modifier)
}

@Composable
fun StatusBadge(status: String) {
    val (text, bg, fg) = when (status) {
        "OPEN" -> Triple("উন্মুক্ত", SomadhanInfoLight, SomadhanInfo)
        "IN_PROGRESS" -> Triple("চলমান", SomadhanOrangeLight, SomadhanOrange)
        "COMPLETED" -> Triple("সম্পন্ন", SomadhanSuccessLight, SomadhanSuccess)
        "CANCELLED" -> Triple("বাতিল", SomadhanErrorLight, SomadhanError)
        "ACCEPTED" -> Triple("গৃহীত", SomadhanSuccessLight, SomadhanSuccess)
        "REJECTED" -> Triple("বাতিল", SomadhanErrorLight, SomadhanError)
        "PENDING" -> Triple("অপেক্ষমাণ", SomadhanSurfaceVariant, SomadhanTextSecondary)
        "DISPUTED" -> Triple("বিতর্কিত", SomadhanErrorLight, SomadhanError)
        else -> Triple(status, SomadhanSurfaceVariant, SomadhanTextSecondary)
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = fg
        )
    }
}

@Composable
fun UrgencyBadge(urgency: String) {
    val (bg, fg) = when (urgency) {
        "খুব জরুরি" -> Pair(SomadhanErrorLight, SomadhanError)
        "জরুরি" -> Pair(SomadhanOrangeLight, SomadhanOrange)
        else -> Pair(SomadhanSurfaceVariant, SomadhanTextSecondary)
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = urgency,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = fg
        )
    }
}

@Composable
fun ProblemCard(
    problem: ProblemEntity,
    posterUser: UserEntity? = null,
    solverLat: Double? = null,
    solverLon: Double? = null,
    accentColor: Color? = null,
    onUserClick: (() -> Unit)? = null,
    onDeleteClick: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val primaryAccent = accentColor ?: SomadhanOrange
    val lightAccent = if (accentColor != null) accentColor.copy(alpha = 0.12f) else SomadhanOrangeLight
    val cardBorderColor = if (accentColor != null) accentColor.copy(alpha = 0.15f) else Color(0xFFEFEFEF)

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp, pressedElevation = 6.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .border(1.dp, cardBorderColor, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .testTag("problem_card_${problem.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category Chip
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(lightAccent)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = problem.categoryName,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryAccent,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (problem.isPhysical) SomadhanSuccessLight else SomadhanInfoLight)
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = if (problem.isPhysical) "ফিজিক্যাল" else "ভার্চুয়াল",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (problem.isPhysical) SomadhanSuccess else SomadhanInfo
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    if (problem.userName.isNotBlank() || posterUser != null) {
                        val displayName = problem.userName.ifBlank { posterUser?.name ?: "গ্রাহক" }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .clip(RoundedCornerShape(4.dp))
                                .background(SomadhanBg.copy(alpha = 0.7f))
                                .border(0.5.dp, SomadhanBorder, RoundedCornerShape(4.dp))
                                .then(
                                    if (onUserClick != null) {
                                        Modifier.clickable { onUserClick() }
                                    } else Modifier
                                )
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            // User Profile Photo
                            val photoUri = posterUser?.profileImageUri
                            if (!photoUri.isNullOrBlank()) {
                                AsyncImage(
                                    model = photoUri,
                                    contentDescription = "প্রোফাইল ছবি",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .border(0.8.dp, primaryAccent.copy(alpha = 0.5f), CircleShape)
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(lightAccent)
                                        .border(0.8.dp, primaryAccent.copy(alpha = 0.4f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = displayName.take(1).uppercase(),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = primaryAccent
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(4.dp))

                            Text(
                                text = displayName,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SomadhanTextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    UrgencyBadge(urgency = problem.urgency)
                    if (onDeleteClick != null) {
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(
                            onClick = onDeleteClick,
                            modifier = Modifier
                                .size(24.dp)
                                .testTag("delete_problem_button_${problem.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "পোস্ট ডিলিট করুন",
                                tint = SomadhanError,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = problem.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = SomadhanTextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = problem.description,
                fontSize = 13.sp,
                color = SomadhanTextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Budget Range
                Column {
                    Text(
                        text = "বাজেট রেঞ্জ",
                        fontSize = 11.sp,
                        color = SomadhanTextHint
                    )
                    Text(
                        text = Formatters.formatTakaRange(problem.minBudget, problem.maxBudget),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryAccent
                    )
                }

                // Distance or Time
                Column(horizontalAlignment = Alignment.End) {
                    if (solverLat != null && solverLon != null && problem.isPhysical) {
                        val distance = DistanceUtil.calculateDistanceKm(
                            lat1 = solverLat,
                            lon1 = solverLon,
                            lat2 = problem.latitude,
                            lon2 = problem.longitude
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.DirectionsWalk,
                                contentDescription = null,
                                tint = SomadhanSuccess,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = DistanceUtil.formatDistance(distance),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = SomadhanSuccess
                            )
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = SomadhanTextHint,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = problem.userAddress,
                                fontSize = 11.sp,
                                color = SomadhanTextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Text(
                        text = Formatters.formatTimeAgo(problem.createdAt),
                        fontSize = 10.sp,
                        color = SomadhanTextHint
                    )
                }
            }
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    subtitle: String? = null,
    icon: ImageVector,
    iconColor: Color = SomadhanOrange,
    modifier: Modifier = Modifier,
    isValuePulsing: Boolean = false
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SomadhanBg),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .border(1.dp, SomadhanDivider, RoundedCornerShape(12.dp))
            .padding(2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    color = SomadhanTextSecondary,
                    fontWeight = FontWeight.Medium
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(iconColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            PulsingValue(isUpdating = isValuePulsing) {
                Text(
                    text = value,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = SomadhanTextPrimary
                )
            }

            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = SomadhanTextHint
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RoleSwitchCategoryDialog(
    allCategories: List<CategoryEntity>,
    isSubmitting: Boolean = false,
    errorMessage: String? = null,
    onDismiss: () -> Unit,
    onConfirm: (List<String>) -> Unit
) {
    var selectedIds by remember { mutableStateOf(setOf<String>()) }
    var validationError by remember { mutableStateOf<String?>(null) }
    val displayError = validationError ?: errorMessage

    BottomSlideAlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "সমাধানকারী স্কিল বাছাই করুন",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = SomadhanTextPrimary
            )
        },
        text = {
            Column {
                Text(
                    text = "সমাধানকারী (Solver) হতে সর্বনিম্ন ১টি এবং সর্বোচ্চ ৩টি ক্যাটাগরি বাছাই করুন:",
                    fontSize = 13.sp,
                    color = SomadhanTextSecondary
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Selected chips display
                if (selectedIds.isNotEmpty()) {
                    Text(
                        text = "বাছাইকৃত ক্যাটাগরি (${DistanceUtil.toBengaliDigits(selectedIds.size.toString())}/৩):",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = SomadhanOrange
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        selectedIds.forEach { id ->
                            val cat = allCategories.find { it.id == id }
                            if (cat != null) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(SomadhanOrange)
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Text(
                                        text = cat.nameBangla,
                                        fontSize = 11.sp,
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "বাদ দিন",
                                        tint = Color.White,
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clickable { selectedIds = selectedIds - id }
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                LazyColumn(modifier = Modifier.height(240.dp)) {
                    items(allCategories) { category ->
                        val isSelected = selectedIds.contains(category.id)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) SomadhanOrangeLight else SomadhanCardBg)
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) SomadhanOrange else SomadhanDivider,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    if (isSelected) {
                                        selectedIds = selectedIds - category.id
                                        validationError = null
                                    } else {
                                        if (selectedIds.size < 3) {
                                            selectedIds = selectedIds + category.id
                                            validationError = null
                                        } else {
                                            validationError = "সর্বোচ্চ ৩টি ক্যাটাগরি বাছাই করা যাবে।"
                                        }
                                    }
                                }
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = CategoryIconHelper.getIcon(category.iconName),
                                contentDescription = null,
                                tint = if (isSelected) SomadhanOrange else SomadhanTextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = category.nameBangla,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = SomadhanTextPrimary
                                )
                                Text(
                                    text = if (category.isPhysical) "ফিজিক্যাল কাজ" else "ভার্চুয়াল কাজ",
                                    fontSize = 11.sp,
                                    color = SomadhanTextHint
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = SomadhanOrange,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                if (displayError != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = displayError,
                        fontSize = 12.sp,
                        color = SomadhanError
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !isSubmitting,
                onClick = {
                    if (selectedIds.isEmpty()) {
                        validationError = "কমপক্ষে ১টি ক্যাটাগরি বাছাই করুন।"
                    } else {
                        validationError = null
                        onConfirm(selectedIds.toList())
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = SomadhanOrange)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("নিশ্চিত হচ্ছে...")
                } else {
                    Text("নিশ্চিত করুন")
                }
            }
        },
        dismissButton = {
            TextButton(enabled = !isSubmitting, onClick = onDismiss) {
                Text("বাতিল", color = SomadhanTextSecondary)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationBottomSheet(
    notifications: List<NotificationEntity>,
    onDismiss: () -> Unit,
    onMarkAllAsRead: () -> Unit,
    onNotificationClick: (targetType: String, targetId: String) -> Unit = { _, _ -> },
    onNotificationItemClick: ((NotificationEntity) -> Unit)? = null,
    onMarkAsRead: ((String) -> Unit)? = null,
    isSolver: Boolean = false,
    hasMore: Boolean = false,
    loadingMore: Boolean = false,
    onLoadMore: (() -> Unit)? = null
) {
    val brandColor = if (isSolver) SomadhanOrange else Color(0xFF1D4ED8)
    val unreadBg = if (isSolver) SomadhanOrangeLight.copy(alpha = 0.45f) else Color(0xFFEFF6FF)
    val unreadBorder = if (isSolver) SomadhanOrange.copy(alpha = 0.4f) else Color(0xFFBFDBFE)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = SomadhanBg
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "সকল বিজ্ঞপ্তি (${DistanceUtil.toBengaliDigits(notifications.size.toString())})",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = SomadhanTextPrimary
                )

                if (notifications.any { !it.isRead }) {
                    TextButton(onClick = onMarkAllAsRead) {
                        Text(
                            text = "সব পড়া হয়েছে",
                            color = brandColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (notifications.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "আপনার কোনো নতুন বিজ্ঞপ্তি নেই।",
                        color = SomadhanTextHint,
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.height(350.dp)) {
                    items(notifications, key = { it.id }) { notif ->
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (notif.isRead) SomadhanCardBg else unreadBg
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .border(
                                    1.dp,
                                    if (notif.isRead) SomadhanDivider else unreadBorder,
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable {
                                    onMarkAsRead?.invoke(notif.id)
                                    onDismiss()
                                    if (onNotificationItemClick != null) {
                                        onNotificationItemClick(notif)
                                    } else {
                                        val resolvedTargetId = notif.relatedProblemId?.takeIf { it.isNotBlank() }
                                            ?: notif.targetId
                                            ?: ""
                                        onNotificationClick(notif.targetType, resolvedTargetId)
                                    }
                                }
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (!notif.isRead) {
                                            Box(
                                                modifier = Modifier
                                                    .size(7.dp)
                                                    .clip(CircleShape)
                                                    .background(brandColor)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                        }
                                        Text(
                                            text = notif.title,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = SomadhanTextPrimary
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = Formatters.formatTimeAgo(notif.timestamp),
                                        fontSize = 10.sp,
                                        color = SomadhanTextHint
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = notif.message,
                                    fontSize = 12.sp,
                                    color = SomadhanTextSecondary,
                                    lineHeight = 17.sp
                                )
                            }
                        }
                    }

                    // Auto-load-more footer + "আরও দেখুন" fallback
                    if (hasMore) {
                        item {
                            LaunchedEffect(Unit) {
                                onLoadMore?.invoke()
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (loadingMore) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = brandColor
                                    )
                                } else if (onLoadMore != null) {
                                    TextButton(
                                        onClick = { onLoadMore() },
                                        modifier = Modifier.testTag("load_more_notifications_button")
                                    ) {
                                        Text(
                                            text = "আরও দেখুন",
                                            color = brandColor,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * Universal safe router for all platform notifications.
 * Accurately connects notifications to their respective functional pages:
 * - Instant emergency jobs -> Solvers to InstantJobs radar/feed, Users to JobTracking
 * - Bid/job cancellations & refunds -> JobTracking
 * - Additional bills / extra charge requests -> JobTracking
 * - Active job progress, live arrivals & completion release -> JobTracking
 * - Disputes & settlements -> JobTracking
 * - New bid proposals for normal posts -> ProblemDetail / onProblemClick
 * - Direct chats / messages -> Chat / Messages
 * - KYC & Verification -> SolverKyc
 * - Wallet balance, deposits & withdrawals -> SolverBalanceWithdraw (Solver) / UserWallet (User)
 * - Profile & account notices -> Profile
 * - Fallbacks -> ProblemDetail or NotificationDetail
 */
fun handleSomadhanNotification(
    notif: NotificationEntity,
    onNavigate: (String) -> Unit,
    onProblemClick: ((String) -> Unit)? = null,
    onChatClick: ((String) -> Unit)? = null,
    isSolver: Boolean = false
) {
    val cleanType = notif.targetType.trim().lowercase()
    val probId = notif.relatedProblemId?.takeIf { it.isNotBlank() }
        ?: notif.targetId?.takeIf { it.startsWith("PROB") }
        ?: ""
    val titleLower = notif.title.lowercase()
    val messageLower = notif.message.lowercase()
    val notifId = notif.id

    // 1. Instant / Emergency Job Broadcast
    val isInstantJobNotif = cleanType == "instant_job" ||
        cleanType == "instant" ||
        notifId.startsWith("NOTIF_INSTANT_") ||
        titleLower.contains("জরুরি জব") ||
        titleLower.contains("নতুন জরুরি") ||
        titleLower.contains("জরুরি কাজ") ||
        titleLower.contains("জরুরি ব্রডকাস্ট") ||
        messageLower.contains("জরুরি কাজ পোস্ট করেছেন") ||
        messageLower.contains("জরুরি জব")

    if (isInstantJobNotif) {
        if (isSolver) {
            onNavigate(com.example.ui.navigation.Screen.InstantJobs.route)
        } else if (probId.isNotBlank()) {
            onNavigate(com.example.ui.navigation.Screen.JobTracking.createRoute(probId))
        } else {
            onNavigate(com.example.ui.navigation.Screen.InstantJobs.route)
        }
        return
    }

    // 2. Job / Bid Cancellation or Refund Notice (Takes client/solver directly to JobTracking)
    val isCancellationOrRefund = cleanType == "tracking" ||
        cleanType == "job_tracking" ||
        titleLower.contains("বিড বাতিল") ||
        titleLower.contains("কাজ বাতিল") ||
        titleLower.contains("পোস্ট বাতিল") ||
        titleLower.contains("বাতিল করেছেন") ||
        titleLower.contains("বাতিল নিশ্চিত") ||
        titleLower.contains("বাতিলকৃত") ||
        titleLower.contains("বাতিল ও রিফান্ড") ||
        messageLower.contains("বিড বাতিল করেছেন") ||
        messageLower.contains("কাজটি বাতিল করা হয়েছে") ||
        messageLower.contains("পোস্টটি বাতিল হয়ে গেছে")

    if (isCancellationOrRefund) {
        if (probId.isNotBlank()) {
            onNavigate(com.example.ui.navigation.Screen.JobTracking.createRoute(probId))
        } else if (cleanType == "balance" || cleanType == "withdrawal" || cleanType == "payment") {
            if (isSolver) {
                onNavigate(com.example.ui.navigation.Screen.SolverBalanceWithdraw.route)
            } else {
                onNavigate(com.example.ui.navigation.Screen.UserWallet.route)
            }
        } else {
            onNavigate(com.example.ui.navigation.Screen.NotificationDetail.createRoute(notif.id))
        }
        return
    }

    // 3. Additional Bill / Extra Charge (Must take directly to JobTracking)
    val isAdditionalCharge = cleanType == "additional_charge" ||
        cleanType == "extra_bill" ||
        cleanType == "extra_charge" ||
        titleLower.contains("অতিরিক্ত বিল") ||
        titleLower.contains("বিল অনুরোধ") ||
        messageLower.contains("অতিরিক্ত বিল") ||
        messageLower.contains("অতিরিক্ত চার্জ")

    if (isAdditionalCharge) {
        if (probId.isNotBlank()) {
            onNavigate(com.example.ui.navigation.Screen.JobTracking.createRoute(probId))
        } else {
            onNavigate(com.example.ui.navigation.Screen.NotificationDetail.createRoute(notif.id))
        }
        return
    }

    // 4. Job Tracking / In-Progress Actions / Completion Release / Disputes
    val isJobTrackingAction = cleanType == "release" ||
        titleLower.contains("রওনা") ||
        titleLower.contains("পৌঁছেছেন") ||
        titleLower.contains("শুরু হয়েছে") ||
        titleLower.contains("রিলিজের অনুরোধ") ||
        titleLower.contains("কাজ সম্পন্ন") ||
        titleLower.contains("বিরোধ") ||
        titleLower.contains("ডিসপিউট") ||
        messageLower.contains("রওনা হয়েছেন") ||
        messageLower.contains("পৌঁছে গেছেন") ||
        messageLower.contains("কাজ শুরু করেছেন") ||
        messageLower.contains("অর্থ রিলিজের অনুরোধ")

    if (isJobTrackingAction && probId.isNotBlank()) {
        onNavigate(com.example.ui.navigation.Screen.JobTracking.createRoute(probId))
        return
    }

    // 5. Escrow Notifications
    if (cleanType == "escrow" || titleLower.contains("escrow")) {
        if (probId.isNotBlank()) {
            onNavigate(com.example.ui.navigation.Screen.JobTracking.createRoute(probId))
        } else if (isSolver) {
            onNavigate(com.example.ui.navigation.Screen.SolverBalanceWithdraw.route)
        } else {
            onNavigate(com.example.ui.navigation.Screen.UserWallet.route)
        }
        return
    }

    // 6. Direct Chat / Messages
    if (cleanType == "chat" || cleanType == "message" || titleLower.contains("বার্তা") || titleLower.contains("মেসেজ")) {
        if (probId.isNotBlank()) {
            if (onChatClick != null) {
                onChatClick(probId)
            } else {
                onNavigate(com.example.ui.navigation.Screen.Chat.createRoute(probId))
            }
        } else {
            onNavigate(com.example.ui.navigation.Screen.Messages.route)
        }
        return
    }

    // 7. KYC Verification
    if (cleanType == "kyc" || titleLower.contains("kyc")) {
        onNavigate(com.example.ui.navigation.Screen.SolverKyc.route)
        return
    }

    // 8. Balance / Wallet / Withdrawal
    if (cleanType == "balance" || cleanType == "withdrawal" || cleanType == "payment" ||
        titleLower.contains("উইথড্র") || titleLower.contains("ব্যালেন্স") || titleLower.contains("টাকা পাঠানো")
    ) {
        if (probId.isNotBlank()) {
            onNavigate(com.example.ui.navigation.Screen.JobTracking.createRoute(probId))
        } else if (isSolver) {
            onNavigate(com.example.ui.navigation.Screen.SolverBalanceWithdraw.route)
        } else {
            onNavigate(com.example.ui.navigation.Screen.UserWallet.route)
        }
        return
    }

    // 9. Profile / Role / Security
    if (cleanType == "role" || cleanType == "profile" || titleLower.contains("অ্যাকাউন্ট") || titleLower.contains("পাসওয়ার্ড") || titleLower.contains("প্রোফাইল")) {
        onNavigate(com.example.ui.navigation.Screen.Profile.route)
        return
    }

    // 10. Solver Skills & Categories
    if (cleanType == "solver_skills" || cleanType == "category" || cleanType == "skills" || titleLower.contains("দক্ষতা") || titleLower.contains("ক্যাটাগরি")) {
        onNavigate(com.example.ui.navigation.Screen.SolverSkills.route)
        return
    }

    // 11. Bids (e.g., New Bid submitted or Bid won)
    if (cleanType == "bid" || titleLower.contains("বিড")) {
        if (titleLower.contains("বিড গৃহীত") && probId.isNotBlank()) {
            onNavigate(com.example.ui.navigation.Screen.JobTracking.createRoute(probId))
        } else {
            val finalProbId = probId.ifBlank {
                notif.targetId?.takeIf { !it.startsWith("USR_") && !it.startsWith("NOTIF_") } ?: ""
            }
            if (finalProbId.isNotBlank()) {
                if (onProblemClick != null) {
                    onProblemClick(finalProbId)
                } else {
                    onNavigate(com.example.ui.navigation.Screen.ProblemDetail.createRoute(finalProbId))
                }
            } else {
                onNavigate(com.example.ui.navigation.Screen.NotificationDetail.createRoute(notif.id))
            }
        }
        return
    }

    // 12. General Problems / Posts
    if (cleanType == "problem" || cleanType == "post" || probId.isNotBlank()) {
        val finalProbId = probId.ifBlank {
            notif.targetId?.takeIf { !it.startsWith("USR_") && !it.startsWith("NOTIF_") } ?: ""
        }
        if (finalProbId.isNotBlank()) {
            if (onProblemClick != null) {
                onProblemClick(finalProbId)
            } else {
                onNavigate(com.example.ui.navigation.Screen.ProblemDetail.createRoute(finalProbId))
            }
        } else {
            onNavigate(com.example.ui.navigation.Screen.NotificationDetail.createRoute(notif.id))
        }
        return
    }

    // 13. General Notifications Fallback
    onNavigate(com.example.ui.navigation.Screen.NotificationDetail.createRoute(notif.id))
}

/**
 * Reusable Pull-to-Refresh container with consistent SomadhanOrange indicator styling.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SomadhanPullToRefresh(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val state = rememberPullToRefreshState()
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        state = state,
        modifier = modifier,
        indicator = {
            Indicator(
                modifier = Modifier.align(Alignment.TopCenter),
                isRefreshing = isRefreshing,
                containerColor = SomadhanSurface,
                color = SomadhanOrange,
                state = state
            )
        }
    ) {
        content()
    }
}

@Composable
fun SomadhanActionBanner(message: String?, onDismiss: () -> Unit) {
    LaunchedEffect(message) {
        if (message != null) {
            delay(1500)
            onDismiss()
        }
    }
    AnimatedVisibility(
        visible = message != null,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(top = 8.dp, start = 12.dp, end = 12.dp)
    ) {
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = SomadhanTextPrimary),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SomadhanSuccess, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text(message ?: "", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
fun ExpandablePaginatedSection(
    allItems: List<ProblemEntity>,
    onProblemClick: (String) -> Unit,
    emptyMessage: String = "কোনো তালিকা পাওয়া যায়নি।",
    listItemContent: @Composable (ProblemEntity) -> Unit
) {
    var isExpanded by remember(allItems) { mutableStateOf(false) }
    var page by remember(allItems) { mutableIntStateOf(0) } // 0-indexed, ব্যবহার হবে এক্সপ্যান্ডেড অবস্থায়

    LaunchedEffect(Unit) {
        page = 0
    }

    val pageSize = 5
    val previewCount = 3

    if (allItems.isEmpty()) {
        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp), contentAlignment = Alignment.Center) {
            Text(emptyMessage, fontSize = 13.sp, color = SomadhanTextHint)
        }
        return
    }

    val displayList = if (!isExpanded) {
        allItems.take(previewCount)
    } else {
        val start = page * pageSize
        val end = min(start + pageSize, allItems.size)
        allItems.subList(start.coerceAtMost(allItems.size), end)
    }

    Column {
        displayList.forEach { problem ->
            listItemContent(problem)
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (!isExpanded && allItems.size > previewCount) {
            // "আরও দেখুন" বাটন
            TextButton(
                onClick = { isExpanded = true; page = 0 },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("আরও দেখুন ↓", color = SomadhanOrange, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        } else if (isExpanded) {
            val totalPages = ceil(allItems.size.toDouble() / pageSize).toInt()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { if (page > 0) page-- },
                    enabled = page > 0,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text("পূর্ববর্তী", fontSize = 12.sp)
                }

                TextButton(onClick = { isExpanded = false; page = 0 }) {
                    Text("কম দেখুন ↑", color = SomadhanTextSecondary, fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = { if (page < totalPages - 1) page++ },
                    enabled = page < totalPages - 1,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text("পরবর্তী", fontSize = 12.sp)
                }
            }
        }
    }
}

/**
 * Extensible Account Status Enum for current and future statuses
 */
enum class AccountStatusType {
    NONE,
    RESTRICTED,
    BANNED,
    CUSTOM
}

/**
 * Compact, tap-able/long-press account status indicator badge with auto-dismissing tooltip.
 * If user is neither banned nor restricted, this Composable renders nothing and takes zero space.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AccountStatusIndicator(
    isBanned: Boolean,
    isRestricted: Boolean,
    modifier: Modifier = Modifier,
    customStatus: String? = null,
    customTooltipText: String? = null
) {
    val statusType = when {
        isBanned -> AccountStatusType.BANNED
        isRestricted -> AccountStatusType.RESTRICTED
        !customStatus.isNullOrBlank() -> AccountStatusType.CUSTOM
        else -> AccountStatusType.NONE
    }

    if (statusType == AccountStatusType.NONE) return

    var showTooltip by remember { mutableStateOf(false) }

    // Auto-dismiss tooltip after ~1.5 seconds
    LaunchedEffect(showTooltip) {
        if (showTooltip) {
            delay(1500)
            showTooltip = false
        }
    }

    val icon: ImageVector
    val iconTint: Color
    val bgTint: Color
    val borderTint: Color
    val tooltipText: String
    val contentDesc: String

    when (statusType) {
        AccountStatusType.BANNED -> {
            icon = Icons.Default.Block
            iconTint = SomadhanError
            bgTint = SomadhanError.copy(alpha = 0.12f)
            borderTint = SomadhanError.copy(alpha = 0.4f)
            tooltipText = customTooltipText ?: "অ্যাকাউন্ট স্থগিত (Banned) — অ্যাডমিনের সাথে যোগাযোগ করুন"
            contentDesc = "অ্যাকাউন্ট স্থগিত (Banned)"
        }
        AccountStatusType.RESTRICTED -> {
            icon = Icons.Default.Warning
            iconTint = Color(0xFFD97706)
            bgTint = Color(0xFFFEF3C7)
            borderTint = Color(0xFFF59E0B).copy(alpha = 0.5f)
            tooltipText = customTooltipText ?: "অ্যাকাউন্ট সীমাবদ্ধ (Restricted) — নতুন কার্যক্রমে সীমাবদ্ধতা আছে"
            contentDesc = "অ্যাকাউন্ট সীমাবদ্ধ (Restricted)"
        }
        AccountStatusType.CUSTOM -> {
            icon = Icons.Default.Warning
            iconTint = SomadhanOrange
            bgTint = SomadhanOrangeLight
            borderTint = SomadhanOrange.copy(alpha = 0.4f)
            tooltipText = customTooltipText ?: (customStatus ?: "অ্যাকাউন্ট স্ট্যাটাস")
            contentDesc = customStatus ?: "Custom Status"
        }
        AccountStatusType.NONE -> return
    }

    Box(
        modifier = modifier.testTag("account_status_indicator")
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(bgTint)
                .border(0.8.dp, borderTint, CircleShape)
                .combinedClickable(
                    onClick = { showTooltip = !showTooltip },
                    onLongClick = { showTooltip = true }
                )
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDesc,
                tint = iconTint,
                modifier = Modifier.size(13.dp)
            )
        }

        if (showTooltip) {
            Popup(
                alignment = Alignment.TopCenter,
                onDismissRequest = { showTooltip = false },
                properties = PopupProperties(focusable = false, dismissOnClickOutside = true)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SomadhanCardBg,
                    shadowElevation = 6.dp,
                    border = androidx.compose.foundation.BorderStroke(1.dp, borderTint),
                    modifier = Modifier
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .clickable { showTooltip = false }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = tooltipText,
                            color = SomadhanTextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

/**
 * Compact, tap-able skill badge indicator for category cards with a 1.5-second auto-dismissing tooltip popup.
 */
@Composable
fun CategorySkillIndicator(
    modifier: Modifier = Modifier,
    tooltipText: String = "আপনার স্কিল",
    testTag: String = "category_skill_badge"
) {
    var showTooltip by remember { mutableStateOf(false) }

    // Auto-dismiss tooltip after 1.5 seconds
    LaunchedEffect(showTooltip) {
        if (showTooltip) {
            delay(1500)
            showTooltip = false
        }
    }

    Box(
        modifier = modifier.testTag(testTag)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(Color(0xFFFFEDD5))
                .border(0.8.dp, SomadhanOrange.copy(alpha = 0.5f), CircleShape)
                .clickable { showTooltip = !showTooltip }
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = "আপনার স্কিল",
                tint = SomadhanOrange,
                modifier = Modifier.size(12.dp)
            )
        }

        if (showTooltip) {
            Popup(
                alignment = Alignment.TopStart,
                onDismissRequest = { showTooltip = false },
                properties = PopupProperties(focusable = false, dismissOnClickOutside = true)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SomadhanCardBg,
                    shadowElevation = 6.dp,
                    border = androidx.compose.foundation.BorderStroke(1.dp, SomadhanOrange.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                        .clickable { showTooltip = false }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = SomadhanOrange,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = tooltipText,
                            color = SomadhanTextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Reusable dynamic profile avatar with fallback initial
 */
@Composable
fun UserAvatar(
    photoUri: String?,
    name: String,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 36.dp,
    borderWidth: androidx.compose.ui.unit.Dp = 1.dp,
    borderColor: Color = SomadhanOrange.copy(alpha = 0.4f),
    backgroundColor: Color = SomadhanOrangeLight,
    textColor: Color = SomadhanOrange,
    fontSize: androidx.compose.ui.unit.TextUnit = 14.sp
) {
    val isPhotoValid = remember(photoUri) { ImageStorageUtil.isValidDisplayUri(photoUri) }
    var loadFailed by remember(photoUri) { mutableStateOf(false) }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(backgroundColor)
            .border(borderWidth, borderColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (isPhotoValid && !loadFailed) {
            AsyncImage(
                model = photoUri,
                contentDescription = "প্রোফাইল ছবি",
                contentScale = ContentScale.Crop,
                onError = { loadFailed = true },
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
            )
        } else {
            val initial = name.trim().take(1).ifBlank { "স" }.uppercase()
            Text(
                text = initial,
                fontSize = fontSize,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        }
    }
}


