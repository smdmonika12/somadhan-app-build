package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.remote.SupabaseRealtimeManager
import com.example.ui.theme.SomadhanBorder
import com.example.ui.theme.SomadhanBg
import com.example.ui.theme.SomadhanError
import com.example.ui.theme.SomadhanOrange
import com.example.ui.theme.SomadhanTextPrimary
import com.example.ui.theme.SomadhanTextSecondary
import com.example.ui.viewmodel.SomadhanViewModel
import kotlinx.coroutines.delay

/**
 * ===== Somadhan Motion Toolkit =====
 * Reusable building blocks for "Uber-style" smooth loading:
 * shimmer skeleton placeholders + loading-aware crossfade wrapper.
 *
 * This file is additive only - it does not modify any existing component.
 * Screens opt in by wrapping their content with [LoadingAwareContent].
 */

/**
 * An animated shimmer [Brush] that sweeps left-to-right, used to paint
 * skeleton placeholder shapes so they feel "alive" instead of static grey boxes.
 *
 * Unlike Uber's neutral grey shimmer (which suits their black/white brand),
 * this blends in a soft hint of [tint] - Somadhan's orange by default - so
 * the loading state still feels like *this* app, not a borrowed style.
 * Pass a different [tint] on screens with a different accent (e.g. an
 * admin/blue context) to keep the shimmer matched to that page.
 */
@Composable
fun rememberShimmerBrush(tint: Color = SomadhanOrange): Brush {
    val transition = rememberInfiniteTransition(label = "shimmer_transition")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )
    // Base tone: a genuinely visible light-grey (not near-white), warmed
    // slightly toward the accent - reads as "Somadhan surface", not generic
    // neutral grey. Fixed at 2025-09 review: the old base (SomadhanSurfaceVariant,
    // ~#F8F9FA) was almost the same brightness as the highlight, so the sweep
    // was invisible on real devices despite animating correctly.
    val baseTone = lerp(Color(0xFFD9D7CE), tint, 0.08f)
    // Highlight: bright near-white with a touch more of the accent, for the
    // sweeping glint - the brightness gap from baseTone is what makes the
    // "light sliding across" effect actually visible.
    val highlightTone = lerp(Color.White, tint, 0.12f)
    val shimmerColors = listOf(baseTone, highlightTone, baseTone)
    return Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnim - 300f, 0f),
        end = Offset(translateAnim, 0f)
    )
}

/**
 * A single shimmering placeholder block. Use this to mimic the shape
 * (width/height/corner radius) of the real content it stands in for.
 */
@Composable
fun ShimmerBlock(
    modifier: Modifier = Modifier,
    cornerRadius: androidx.compose.ui.unit.Dp = 6.dp,
    tint: Color = SomadhanOrange
) {
    val brush = rememberShimmerBrush(tint)
    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(brush)
    )
}

/**
 * Skeleton placeholder shaped like [com.example.ui.components.ProblemCard] /
 * the feed cards on HomeScreen - header chip row, title, two description
 * lines, and a footer row. Used while real feed data hasn't arrived yet.
 */
@Composable
fun ProblemCardSkeleton(modifier: Modifier = Modifier, tint: Color = SomadhanOrange) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .border(1.dp, SomadhanBorder, RoundedCornerShape(14.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ShimmerBlock(modifier = Modifier.width(90.dp).height(20.dp), cornerRadius = 6.dp, tint = tint)
                ShimmerBlock(modifier = Modifier.width(60.dp).height(20.dp), cornerRadius = 6.dp, tint = tint)
            }
            Spacer(modifier = Modifier.height(10.dp))
            ShimmerBlock(modifier = Modifier.fillMaxWidth(0.8f).height(16.dp), tint = tint)
            Spacer(modifier = Modifier.height(6.dp))
            ShimmerBlock(modifier = Modifier.fillMaxWidth().height(13.dp), tint = tint)
            Spacer(modifier = Modifier.height(4.dp))
            ShimmerBlock(modifier = Modifier.fillMaxWidth(0.6f).height(13.dp), tint = tint)
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ShimmerBlock(modifier = Modifier.width(70.dp).height(18.dp), tint = tint)
                ShimmerBlock(modifier = Modifier.width(50.dp).height(18.dp), tint = tint)
            }
        }
    }
}

/**
 * Full HomeScreen feed skeleton: a status-card placeholder followed by
 * a few [ProblemCardSkeleton]s, so the very first frame of Home already
 * looks like "content coming" instead of an empty white screen.
 */
@Composable
fun HomeFeedSkeleton(modifier: Modifier = Modifier, tint: Color = SomadhanOrange) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(SomadhanBg)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        ShimmerBlock(
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp),
            cornerRadius = 16.dp,
            tint = tint
        )
        Spacer(modifier = Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            repeat(3) {
                ShimmerBlock(
                    modifier = Modifier.width(84.dp).height(84.dp),
                    cornerRadius = 12.dp,
                    tint = tint
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        repeat(3) {
            ProblemCardSkeleton(tint = tint)
        }
    }
}

/**
 * Generic list-screen skeleton: a header block followed by N card
 * placeholders. Reusable across any screen whose content is primarily
 * a list/feed (Dashboard, Chat list, UserProblems, SolverMyBids, etc.)
 * so we don't need a bespoke skeleton per screen.
 */
@Composable
fun ListScreenSkeleton(
    modifier: Modifier = Modifier,
    itemCount: Int = 4,
    showHeader: Boolean = true,
    tint: Color = SomadhanOrange
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(SomadhanBg)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        if (showHeader) {
            ShimmerBlock(
                modifier = Modifier.fillMaxWidth().height(64.dp),
                cornerRadius = 14.dp,
                tint = tint
            )
            Spacer(modifier = Modifier.height(14.dp))
        }
        repeat(itemCount) {
            ProblemCardSkeleton(tint = tint)
        }
    }
}

/**
 * Generic detail-screen skeleton: a hero block (image/title area) followed
 * by a few text-line placeholders of varying width, then a couple of
 * action-row blocks. Reusable for single-item detail screens
 * (ProblemDetail, PublicProfile, ReputationDetail, etc.)
 */
@Composable
fun DetailScreenSkeleton(
    modifier: Modifier = Modifier,
    tint: Color = SomadhanOrange
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(SomadhanBg)
            .padding(16.dp)
    ) {
        ShimmerBlock(
            modifier = Modifier.fillMaxWidth().height(140.dp),
            cornerRadius = 16.dp,
            tint = tint
        )
        Spacer(modifier = Modifier.height(16.dp))
        ShimmerBlock(modifier = Modifier.fillMaxWidth(0.7f).height(20.dp), tint = tint)
        Spacer(modifier = Modifier.height(10.dp))
        ShimmerBlock(modifier = Modifier.fillMaxWidth().height(14.dp), tint = tint)
        Spacer(modifier = Modifier.height(6.dp))
        ShimmerBlock(modifier = Modifier.fillMaxWidth().height(14.dp), tint = tint)
        Spacer(modifier = Modifier.height(6.dp))
        ShimmerBlock(modifier = Modifier.fillMaxWidth(0.5f).height(14.dp), tint = tint)
        Spacer(modifier = Modifier.height(20.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ShimmerBlock(modifier = Modifier.weight(1f).height(48.dp), cornerRadius = 12.dp, tint = tint)
            ShimmerBlock(modifier = Modifier.weight(1f).height(48.dp), cornerRadius = 12.dp, tint = tint)
        }
    }
}

/**
 * Skeleton for JobTrackingScreen's live map view: a large map-area block,
 * a floating status card placeholder, and a bottom action-bar placeholder -
 * matching that screen's actual map + status-card + action-bar structure
 * (distinct from the plain list/detail skeletons above).
 */
@Composable
fun JobTrackingSkeleton(
    modifier: Modifier = Modifier,
    tint: Color = SomadhanOrange
) {
    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .fillMaxWidth()
            .background(SomadhanBg)
    ) {
        // Map area
        ShimmerBlock(
            modifier = Modifier
                .fillMaxWidth()
                .height(340.dp),
            cornerRadius = 0.dp,
            tint = tint
        )
        // Floating status card, overlaid near the top like the real screen
        Column(
            modifier = Modifier
                .align(androidx.compose.ui.Alignment.TopCenter)
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(14.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    ShimmerBlock(modifier = Modifier.fillMaxWidth(0.6f).height(16.dp), tint = tint)
                    Spacer(modifier = Modifier.height(8.dp))
                    ShimmerBlock(modifier = Modifier.fillMaxWidth(0.4f).height(13.dp), tint = tint)
                }
            }
        }
        // Bottom action bar
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            modifier = Modifier
                .align(androidx.compose.ui.Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                ShimmerBlock(modifier = Modifier.fillMaxWidth(0.5f).height(15.dp), tint = tint)
                Spacer(modifier = Modifier.height(12.dp))
                ShimmerBlock(modifier = Modifier.fillMaxWidth().height(48.dp), cornerRadius = 12.dp, tint = tint)
            }
        }
    }
}

/**
 * Skeleton for ChatScreen: a header-bar placeholder followed by alternating
 * left/right message-bubble placeholders, matching a chat thread's real
 * shape (distinct from list/detail/map skeletons above).
 */
@Composable
fun ChatSkeleton(
    modifier: Modifier = Modifier,
    tint: Color = SomadhanOrange
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(SomadhanBg)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            ShimmerBlock(
                modifier = Modifier.width(36.dp).height(36.dp),
                cornerRadius = 18.dp,
                tint = tint
            )
            Spacer(modifier = Modifier.width(10.dp))
            ShimmerBlock(modifier = Modifier.width(120.dp).height(16.dp), tint = tint)
        }
        Spacer(modifier = Modifier.height(24.dp))
        val bubbleWidths = listOf(0.5f, 0.65f, 0.4f, 0.6f, 0.45f)
        bubbleWidths.forEachIndexed { index, widthFraction ->
            val fromMe = index % 2 == 0
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = if (fromMe) Arrangement.End else Arrangement.Start
            ) {
                ShimmerBlock(
                    modifier = Modifier
                        .fillMaxWidth(widthFraction)
                        .height(38.dp),
                    cornerRadius = 14.dp,
                    tint = tint
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

/**
 * For screens that use an early-return loading pattern (`if (x == null) { skeleton; return }`)
 * rather than [LoadingAwareContent]: returns true for [minimumDurationMs] after first
 * composition, then false forever after. Combine with the real null-check
 * (`if (x == null || rememberMinimumSkeletonGate())`) so a guaranteed brief skeleton
 * flash still happens even when the data was already cached and arrived instantly -
 * matching how [LoadingAwareContent] behaves on Home/Dashboard.
 */
@Composable
fun rememberMinimumSkeletonGate(minimumDurationMs: Long = 220L): Boolean {
    var minimumElapsed by remember { mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        delay(minimumDurationMs)
        minimumElapsed = true
    }
    return !minimumElapsed
}

/**
 * Loading-aware wrapper: shows [skeleton] while [isLoading] is true, then
 * crossfades into [content] once data is ready. Guarantees the skeleton is
 * visible for at least [minimumDurationMs] so fast (cached/local) loads
 * don't "flash" - and stays visible as long as needed on slow connections,
 * since it's driven by the real [isLoading] flag rather than a fixed timer.
 *
 * Pass [isRefreshing] whenever this sits inside a `SomadhanPullToRefresh` -
 * while true, the skeleton is force-suppressed (real content always shown)
 * so this shimmer can never run alongside the native pull-to-refresh spinner.
 * That spinner is already tied 1:1 to the real refresh duration, so the two
 * loading indicators can never end up out of sync or double-animating.
 */
@Composable
fun LoadingAwareContent(
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    minimumDurationMs: Long = 220L,
    isRefreshing: Boolean = false,
    skeleton: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    var minimumTimeElapsed by remember { mutableStateOf(false) }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        delay(minimumDurationMs)
        minimumTimeElapsed = true
    }

    val showSkeleton = !isRefreshing && (isLoading || !minimumTimeElapsed)

    Crossfade(
        targetState = showSkeleton,
        animationSpec = tween(durationMillis = 260),
        modifier = modifier,
        label = "loading_aware_crossfade"
    ) { loading ->
        if (loading) skeleton() else content()
    }
}

// Loading/Sync Fix Roadmap v2, ধাপ ৪ (শেষ ধাপ) — dead-code cleanup। এখানে আগে
// `SessionAwareLoadingContent`, `rememberAdminTabReady`, আর `rememberPageDataReady`
// (timeout-ভিত্তিক পুরনো readiness হেল্পার) সংজ্ঞায়িত ছিল। ৩৩টা স্ক্রিনই এখন `SyncAwareContent`
// (উপরে) ব্যবহার করে বলে এই তিনটার আর কোনো call-site অবশিষ্ট নেই — তাই মুছে ফেলা হলো। ইতিহাসের
// জন্য: এই হেল্পারগুলো readiness নির্ধারণ করতো শুধু `list.isNotEmpty()` চেক করে + একটা fixed
// timeout (৬ সেকেন্ড) fallback দিয়ে, যেটা প্রকৃত fetch-সাফল্য/ব্যর্থতা থেকে সম্পূর্ণ আলাদা ছিল —
// ঠিক এই কারণেই `SyncAwareContent`-এ migrate করা হয়েছে (দেখো `SyncAwareContent`-এর উপরের কমেন্ট)।

/**
 * Session-aware version of [rememberMinimumSkeletonGate], for screens that use
 * the early-return loading pattern (`if (x == null || gate) { skeleton; return }`)
 * instead of `SyncAwareContent`. Returns true only while this screen's
 * skeleton should still show: never once [sessionKey] has been marked loaded in
 * this app session, otherwise the same minimum-duration-guaranteed gate as
 * [rememberMinimumSkeletonGate].
 *
 * বাগ-ফিক্স (রিপোর্ট: `PublicProfileScreen`-এ re-entry-তে কোনো শিমার/ফ্ল্যাশই হচ্ছে না) —
 * root cause: [sessionKey] একবার লোড হয়ে গেলে (re-entry visit) এই গেট আগে সবসময়ই সরাসরি
 * `false` রিটার্ন করত, কোনো ব্যতিক্রম ছাড়াই। যেসব caller [SyncAwareRefreshableContent] বা
 * [rememberFieldChangePulse] ব্যবহার করে তাদের নিজস্ব আলাদা "flashOnReentry" ব্যবস্থা আছে,
 * কিন্তু যেসব caller (যেমন `PublicProfileScreen`) সরাসরি এই লো-লেভেল গেটটাই একা ব্যবহার করে,
 * তাদের re-entry-তে কোনো visual feedback-ই কখনো ছিল না। ঐচ্ছিক [flashOnReentry] (ডিফল্ট
 * `false` — তাই `ChatScreen`/`JobTrackingScreen`-এর ইচ্ছাকৃত "re-entry-তে পুরো-পেজ শিমার
 * কখনোই না" আচরণ-সহ বাকি সব পুরনো কল-সাইট অক্ষত থাকে) `true` দিলে re-entry visit-এও
 * [reentryFlashMs] সময়ের জন্য সংক্ষিপ্ত flash হবে — ঠিক [SyncAwareRefreshableContent]-এর
 * "পথ B" re-entry-flash-এর মতোই, শুধু generic `data`-diffing ছাড়া সরল single-flash সংস্করণ।
 * এই flash-টা [reentryFlashElapsed]-এর initial state-এই সরাসরি `false` (মানে গেট সরাসরি
 * `true়`) দিয়ে শুরু হয় — কোনো coroutine/delay-এর জন্য অপেক্ষা করতে হয় না, তাই প্রথম ফ্রেম থেকেই
 * flash দেখা যায় (দ্বিতীয় বাগের মতো "আগে content, পরে শিমার" ক্রম এখানে ঘটে না)।
 *
 * Caller is still responsible for combining this with its own real null-check and for
 * calling [SomadhanViewModel.markLoadedOnce] once its data has actually arrived (see
 * call sites for the exact pattern).
 */
@Composable
fun rememberSessionAwareSkeletonGate(
    sessionKey: String,
    viewModel: SomadhanViewModel,
    minimumDurationMs: Long = 220L,
    flashOnReentry: Boolean = false,
    reentryFlashMs: Long = 350L
): Boolean {
    // mount-মুহূর্তেই ক্যাপচার করা হচ্ছে (recomposition-এ আর বদলাবে না), [SyncAwareRefreshableContent]-এর
    // isReentryVisit-এর মতোই — এই sessionKey এই app session-এ আগে থেকেই লোড হয়ে গেছে কিনা।
    val isReentryVisit = remember(sessionKey) { viewModel.hasLoadedOnce(sessionKey) }
    if (isReentryVisit) {
        if (!flashOnReentry) return false
        var reentryFlashElapsed by remember(sessionKey) { mutableStateOf(false) }
        LaunchedEffect(sessionKey) {
            delay(reentryFlashMs)
            reentryFlashElapsed = true
        }
        return !reentryFlashElapsed
    }
    return rememberMinimumSkeletonGate(minimumDurationMs)
}


/**
 * Localized/targeted loading indicator for a single realtime-updating value
 * (wallet balance, unread-notification count, live bid amount, etc.) - NOT a
 * full-page skeleton. While [isUpdating] is true, [content] is shown at reduced
 * opacity with a subtle pulse, so only the specific number/text that's actually
 * changing gives feedback; the rest of the page stays completely still. This is
 * the pattern professional apps use for values that update in place - Uber's
 * live fare/ETA, a bank app's balance refresh - rather than re-skeletoning
 * something the user can already see.
 */
@Composable
fun PulsingValue(
    isUpdating: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val transition = rememberInfiniteTransition(label = "pulsing_value_transition")
    val pulseAlpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 550, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulsing_value_alpha"
    )
    androidx.compose.foundation.layout.Box(
        modifier = modifier.alpha(if (isUpdating) pulseAlpha else 1f)
    ) {
        content()
    }
}

/**
 * Loading Pattern Master Prompt — AdminPanelScreen Overview ট্যাবের progressive/lazy
 * scroll-reveal শিমার (ব্যাচ ১৫)। [PulsingValue] realtime value-change-এর জন্য, এটা তার সাথী —
 * প্রথমবার কোনো zone viewport-এ scroll করে দেখা দেওয়ার মুহূর্তে সংক্ষিপ্ত শিমার-ওভারলে দেখায়,
 * তারপর নিচের real content reveal করে (crossfade)। ডেটা ইতিমধ্যে সব parameter হিসেবেই এসে গেছে
 * (কোনো lazy fetch/compute না) — তাই এটা প্রকৃত ডেটা-লোডিং লুকায় না, শুধু reveal-এর *টাইমিং*টাই
 * scroll-এর সাথে সামঞ্জস্যপূর্ণ করে (professional অ্যাপে যেমন হয়, যেমন LinkedIn/Instagram ফিড)।
 *
 * [content] শুরু থেকেই আসল layout-এ compose হয় (তাই সাইজ/লেআউট আগে থেকেই সঠিক, কোনো jump নেই) —
 * শুধু তার উপর একটা [ShimmerBlock] ওভারলে বসে ছোট [revealDelayMs] সময়ের জন্য, তারপর fade-out হয়ে
 * সরে যায়। [zoneKey] দিয়ে প্রতিটা zone আলাদা শনাক্ত হয়; [revealedKeys] (caller-এর composable-এ
 * `remember { mutableStateOf(setOf<String>()) }` দিয়ে রাখা) একবার কোনো key reveal হয়ে গেলে সেটা
 * মনে রাখে — তাই একই বসার (visit/composition) মধ্যে উপরে-নিচে আবার scroll করলে দ্বিতীয়বার শিমার
 * রিপ্লে হয় না, সাথে সাথেই দেখা যায়। [revealedKeys] caller-এর composable instance-এর সাথে bound
 * (তাই স্ক্রিন থেকে বেরিয়ে আবার ঢুকলে/নতুন visit-এ আবার প্রথমবারের মতো শিমার হবে, যা "শুধু
 * প্রথমবার" রুলের সাথে সংগতিপূর্ণ)।
 */
@Composable
fun ScrollRevealShimmer(
    zoneKey: String,
    revealedKeys: androidx.compose.runtime.MutableState<Set<String>>,
    tint: Color = SomadhanOrange,
    revealDelayMs: Long = 260L,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val alreadyRevealed = zoneKey in revealedKeys.value
    var showShimmer by remember(zoneKey) { mutableStateOf(!alreadyRevealed) }

    LaunchedEffect(zoneKey) {
        if (!alreadyRevealed) {
            delay(revealDelayMs)
            showShimmer = false
            revealedKeys.value = revealedKeys.value + zoneKey
        }
    }

    androidx.compose.foundation.layout.Box(modifier = modifier) {
        content()
        androidx.compose.animation.AnimatedVisibility(
            visible = showShimmer,
            exit = fadeOut(animationSpec = tween(durationMillis = 180))
        ) {
            ShimmerBlock(
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(14.dp)),
                tint = tint
            )
        }
    }
}

/**
 * Realtime-Aware, Structure-Preserving Refresh — ধাপ ৪ (আর ধাপ ৫-এর "extra feature", আর ধাপ
 * ৬-এর "re-entry-ও রিফ্রেশ" সংযোজন)। একক মান-ভিত্তিক স্ক্রিনে (ওয়ালেট ব্যালেন্স, এসক্রো টোটাল
 * ইত্যাদি) [PulsingValue]-কে চালানোর জন্য এই হেল্পার — তিনভাবে pulse ট্রিগার হতে পারে, তিনটাই
 * স্বাধীন:
 *   ১. [value] আগের রেকর্ড করা মানের থেকে সত্যিই ভিন্ন হলে (automatic realtime update-সহ যেকোনো
 *      কারণে) — সাধারণ Kotlin `==` দিয়ে তুলনা, ঠিক [SyncAwareRefreshableContent]-এর মতোই।
 *   ২. [isManualRefreshing] সত্যি থেকে মিথ্যায় ফিরলে (মানে ব্যবহারকারীর pull-to-refresh সাইকেল
 *      সবেমাত্র শেষ হলো) — এক্ষেত্রে [value] সত্যিই বদলেছে কিনা তা না দেখেই pulse হবে, যাতে
 *      ম্যানুয়াল রিফ্রেশে সবসময় একটা visual feedback থাকে ("রিফ্রেশ সম্পন্ন হয়েছে")। এই কারণটা
 *      ঐচ্ছিক — ডিফল্ট `false` মানে পুরনো কল-সাইট (যাদের pull-to-refresh নেই, যেমন
 *      `ActiveJobsPopupScreen`) কোনো পরিবর্তন ছাড়াই আগের আচরণেই থাকে।
 *   ৩. **(ধাপ ৬, নতুন)** এই কম্পোজেবল ইনস্ট্যান্স mount হওয়ার মুহূর্তে যদি [sessionKey] +
 *      [viewModel] দেওয়া থাকে আর [viewModel]-এ ওই [sessionKey] আগে থেকেই "লোড হয়ে গেছে" ধরে
 *      রাখা থাকে (মানে এটা এই app session-এ প্রথমবার এই স্ক্রিনে ঢোকা না, বরং স্ক্রিন থেকে বেরিয়ে
 *      আবার ঢোকা/re-entry) — তাহলে mount হওয়ার সাথে সাথেই একবার সংক্ষিপ্ত pulse হবে, [value]
 *      সত্যিই বদলেছে কিনা তা না দেখেই। এটা ব্যবহারকারীর সিদ্ধান্ত "পথ B": স্ক্রিন বন্ধ থাকা অবস্থায়
 *      realtime-এ ডেটা সত্যিই বদলে থাকতে পারে, তাই re-entry-কেও এক অর্থে "রিফ্রেশ" ধরে নিয়ে
 *      "সর্বশেষ ডেটা আনা হচ্ছে" এই ফিডব্যাক দেওয়া — ম্যানুয়াল pull-to-refresh-এর extra
 *      feature-এর মতোই। ঐচ্ছিক [flashOnReentry] দিয়ে কোনো নির্দিষ্ট কল-সাইটে এটা বন্ধ রাখা
 *      যাবে (ডিফল্ট `true`)। [sessionKey]/[viewModel] না দিলে (ডিফল্ট `null`) এই তৃতীয় কারণটা
 *      সম্পূর্ণ নিষ্ক্রিয় থাকে — পুরনো কল-সাইট (parameter না দেওয়া) আগের আচরণেই থাকে।
 * তিনটা কারণ একই সময়ে ট্রিগার হলেও একে অপরকে অকালে বন্ধ করে না — একটা active-reason কাউন্টার
 * ব্যবহার করা হয়েছে (শুধু বুলিয়ান না), যতক্ষণ অন্তত একটা কারণ সক্রিয় ততক্ষণ pulse চলতে থাকবে।
 * [durationMs] ডিফল্ট ৩৫০ms (ধাপ ১-এর সিদ্ধান্ত অনুযায়ী)। প্রথমবার composition-এ (আগের কোনো মান
 * নেই, initial value-ই "previous" ধরা হয়) [value]-বদলের কারণে pulse হয় না — শুধু পরবর্তী প্রকৃত
 * পরিবর্তনে, ম্যানুয়াল-রিফ্রেশ-সমাপ্তিতে, বা (নতুন) re-entry mount-এ।
 */
@Composable
fun <T> rememberFieldChangePulse(
    value: T,
    isManualRefreshing: Boolean = false,
    durationMs: Long = 350L,
    sessionKey: String? = null,
    viewModel: SomadhanViewModel? = null,
    flashOnReentry: Boolean = true
): Boolean {
    var previousValue by remember { mutableStateOf(value) }
    var wasManualRefreshing by remember { mutableStateOf(isManualRefreshing) }
    // mount-মুহূর্তেই ক্যাপচার করা হচ্ছে (recomposition-এ আর বদলাবে না) — sessionKey/viewModel
    // দুটোই দেওয়া থাকলে আর সেই key আগে থেকে লোড-হয়ে-গেছে চিহ্নিত থাকলে, এটাই re-entry বোঝাবে।
    val isReentryVisit = remember {
        sessionKey != null && viewModel != null && viewModel.hasLoadedOnce(sessionKey)
    }
    // রুট-কজ ফিক্স (একই ক্লাসের বাগ যা [SyncAwareRefreshableContent]-এও ছিল): আগে
    // activePulseReasons সবসময় 0 দিয়ে শুরু হতো আর re-entry pulse ট্রিগার হতো নিচের
    // LaunchedEffect(Unit)-এর ভেতরে (প্রথম composition commit-এর *পরে* asynchronously) —
    // ফলে প্রথম ফ্রেমে pulse বন্ধ থাকতো, effect রান হওয়ার পরই "খুলতো"। এখন সিদ্ধান্তটা
    // synchronously নেওয়া হচ্ছে (isReentryVisit ইতিমধ্যে জানা), তাই দরকার হলে
    // activePulseReasons-এর initial value-ই সরাসরি ১।
    val needsReentryPulseOnMount = flashOnReentry && isReentryVisit
    var activePulseReasons by remember { mutableStateOf(if (needsReentryPulseOnMount) 1 else 0) }

    // ধাপ ৬ — re-entry mount pulse: composition instance-টা নতুন তৈরি হওয়ার সাথে সাথে (screen
    // থেকে বেরিয়ে আবার ঢোকার ফলে) একবার চলে, [value]/[isManualRefreshing]-এর effect দুটো থেকে
    // সম্পূর্ণ স্বাধীন।
    // সেশন ২.৩ (batch 31, fix2_3 half1) — ধাপ ১ ডায়াগনসিসে পাওয়া রুট-কজ (দেখো
    // SHIMMER_REGRESSION_BATCH31_PROGRESS.md, বাগ ১): নিচের তিনটা LaunchedEffect-এই
    // `activePulseReasons--` আগে `delay(durationMs)`-এর ঠিক পরে (কোনো try/finally ছাড়া)
    // ছিল। [value]/[isManualRefreshing] খুব দ্রুত (৩৫০ms উইন্ডোর মধ্যে) দ্বিতীয়বার বদলালে
    // Compose পুরনো coroutine-টা delay চলাকালীনই cancel করে দিত, তাই decrement-লাইনটা
    // কখনো রান হতো না — `activePulseReasons` স্থায়ীভাবে বেশি থেকে যেত আর pulse/skeleton
    // কখনো বন্ধ হতো না (ঠিক ওয়ালেট-রিচার্জের পরে দেখা "স্টাক শিমার" বাগের মতোই, যেহেতু এটাও
    // একই কাউন্টার-প্যাটার্ন)। এখন `finally { activePulseReasons-- }` দিয়ে cancellation
    // হলেও decrement গ্যারান্টিড।
    LaunchedEffect(Unit) {
        if (needsReentryPulseOnMount) {
            // activePulseReasons ইতিমধ্যে উপরে synchronously ১ দিয়ে শুরু হয়ে গেছে (ডাবল-কাউন্ট
            // এড়াতে এখানে আবার ++ করা হচ্ছে না) — শুধু duration অপেক্ষা করে ঠিক একবার --।
            try {
                delay(durationMs)
            } finally {
                activePulseReasons--
            }
        }
    }

    LaunchedEffect(value) {
        if (previousValue != value) {
            previousValue = value
            activePulseReasons++
            try {
                delay(durationMs)
            } finally {
                activePulseReasons--
            }
        }
    }

    LaunchedEffect(isManualRefreshing) {
        if (wasManualRefreshing && !isManualRefreshing) {
            activePulseReasons++
            try {
                delay(durationMs)
            } finally {
                activePulseReasons--
            }
        }
        wasManualRefreshing = isManualRefreshing
    }

    return activePulseReasons > 0
}

/**
 * Loading/Sync Fix Roadmap v2, ধাপ ৫ — pertains only to [SyncAwareContent] below. প্রতিটা
 * `sessionKey`-এর সর্বশেষ automatic (ON_RESUME-ট্রিগারড) retry-এর timestamp রাখে, যাতে ৫
 * সেকেন্ডের মধ্যে একই sessionKey-এর জন্য দ্বিতীয়বার automatic retry না চলে (দ্রুত বারবার
 * ট্যাব সুইচ করলে retry-storm এড়াতে)। এটা module-level/session-scoped (app process বেঁচে
 * থাকা পর্যন্ত) — ম্যানুয়াল "আবার চেষ্টা করুন" বাটন এই cooldown-এর আওতার বাইরে, সবসময় সাথে
 * সাথে কাজ করে (দেখো নিচে [SyncAwareContent]-এর `onRetry` সরাসরি কল, cooldown শুধু
 * ON_RESUME-পথে)।
 */
private object SyncResumeRetryCooldown {
    private val lastRetryAtMs = mutableMapOf<String, Long>()
    private const val COOLDOWN_MS = 5_000L

    /** true হলে retry চালানো নিরাপদ (আর সাথে সাথে timestamp আপডেট করে দেয়)। */
    fun tryConsume(sessionKey: String, nowMs: Long): Boolean {
        val last = lastRetryAtMs[sessionKey]
        if (last != null && nowMs - last < COOLDOWN_MS) return false
        lastRetryAtMs[sessionKey] = nowMs
        return true
    }
}

/**
 * একটা [SupabaseRealtimeManager.SyncPhase]
 * নিয়ে সরাসরি তিনটা অবস্থাতেই সঠিক UI দেখায়:
 *   - `LOADING` -> [skeleton]
 *   - `LOADED`  -> [content]
 *   - `ERROR`   -> স্পষ্ট বাংলা এরর মেসেজ + "আবার চেষ্টা করুন" বাটন, ট্যাপ করলে [onRetry] কল হয়।
 *     [onRetry] caller-নির্ভর — কোন sync ফাংশন আবার কল হবে (`attachDatabase()`/
 *     `pullBulkDataFromSupabase()`, বা ধাপ ২-এর নির্দিষ্ট per-domain ফাংশন) তা কল-সাইট ঠিক করে,
 *     এই কম্পোনেন্ট নিজে কোনো sync ফাংশন জানে না/কল করে না।
 *
 * API-টা (sessionKey, viewModel, ... , skeleton, content) প্যাটার্নে **স্থিতিশীল রাখা প্রয়োজন**
 * — ধাপ ৫-এ এই একই কম্পোনেন্টের ভেতরে একটা ছোট নতুন behavior (NavBackStackEntry-এর
 * ON_RESUME lifecycle event শুনে, [syncPhase] তখনও `ERROR` থাকলে বাটনে চাপ না দিয়েই স্বয়ংক্রিয়ভাবে
 * [onRetry] কল করা) যোগ হবে — কোনো কল-সাইটের প্যারামিটার/সিগনেচার তখন বদলাবে না। [sessionKey] আর
 * [viewModel] তখন per-screen resume-retry guard/cooldown রাখতে ব্যবহার হবে; এখন এই ধাপে শুধু
 * `LOADED`-এ পৌঁছালে [SomadhanViewModel.markLoadedOnce] কল করে বাকি সব session-aware
 * কম্পোনেন্টের (এই ফাইলেরই [rememberSessionAwareSkeletonGate]) সাথে
 * consistency রাখা হচ্ছে। (ধাপ ৪ শেষে: এই কমেন্টে আগে `SessionAwareLoadingContent`/
 * `rememberAdminTabReady`-র রেফারেন্স ছিল — ৩৩টা স্ক্রিন কনভার্টের পর সেগুলো dead code হিসেবে
 * মুছে ফেলা হয়েছে বলে রেফারেন্স দুটোও সরানো হলো, নিচের নতুন নোট দ্রষ্টব্য।)
 */
@Composable
fun SyncAwareContent(
    sessionKey: String,
    viewModel: SomadhanViewModel,
    syncPhase: SupabaseRealtimeManager.SyncPhase,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    skeleton: @Composable () -> Unit = { ListScreenSkeleton() },
    content: @Composable () -> Unit
) {
    // Loading/Sync Fix Roadmap v2, ধাপ ৮ (regression fix) — [syncPhase] পুরো অ্যাপ-জুড়ে একটাই
    // শেয়ার্ড সিগন্যাল (শুরুর bulk-pull সফল হলে একবার LOADED হয়ে সারা session-এ আর কখনো
    // LOADING-এ ফেরে না)। শুধু এটার উপর ভিত্তি করে skeleton/content ঠিক করলে সমস্যা হচ্ছিল: bulk-pull
    // ইতিমধ্যে দ্রুত (বা cache থেকে) LOADED হয়ে গেলে, পরে যেকোনো স্ক্রিনে *প্রথমবার* ঢুকলেও কোনো
    // skeleton flash ছাড়াই সরাসরি content() দেখানো হতো — প্রতিটা স্ক্রিনের নিজস্ব প্রথম-ভিজিটের
    // "cold load" অনুভূতিটাই হারিয়ে যাচ্ছিল। এখন এখানে [rememberSessionAwareSkeletonGate] দিয়ে
    // প্রতিটা sessionKey-এর জন্য আলাদাভাবে একটা ন্যূনতম skeleton-উইন্ডো guarantee করা হচ্ছে: এই
    // sessionKey এই app session-এ প্রথমবার এখানে এলে (viewModel.hasLoadedOnce(sessionKey) == false)
    // অন্তত ~220ms স্কেলিটন-ই দেখাবে, global syncPhase ততক্ষণে LOADED হয়ে গেলেও — তারপর একবার
    // markLoadedOnce হয়ে গেলে (নিচে) একই sessionKey-তে পরের ভিজিটে আর কখনো flash হবে না।
    // list/object খালি না ভরা তা দিয়ে বিচার করা হয়নি ইচ্ছাকৃতভাবে — খালি লিস্ট (যেমন "কোনো কাজ
    // নেই") একটা বৈধ LOADED অবস্থা, ওটাকে "ডেটা এখনো আসেনি" ধরে নিলে সেই স্ক্রিন চিরকাল skeleton-এ
    // আটকে থাকতো।
    val withinFirstVisitWindow = rememberSessionAwareSkeletonGate(sessionKey, viewModel)
    val effectivePhase = if (syncPhase == SupabaseRealtimeManager.SyncPhase.LOADED && withinFirstVisitWindow) {
        SupabaseRealtimeManager.SyncPhase.LOADING
    } else {
        syncPhase
    }

    if (effectivePhase == SupabaseRealtimeManager.SyncPhase.LOADED) {
        LaunchedEffect(sessionKey) { viewModel.markLoadedOnce(sessionKey) }
    }

    // Loading/Sync Fix Roadmap v2, ধাপ ৫ — পেজ থেকে বেরিয়ে আবার ঢুকলে (ON_RESUME) স্বয়ংক্রিয়
    // retry। rememberUpdatedState দিয়ে সবসময় সর্বশেষ syncPhase/onRetry ধরে রাখা হচ্ছে যাতে
    // observer-এর ভেতরের lambda স্টেল ক্লোজারে পুরনো মান না দেখে (DisposableEffect শুধু
    // sessionKey বদলালেই পুনরায় রেজিস্টার হয়, প্রতিটা recomposition-এ না)।
    //
    // রুট-কজ ফিক্স (রিপোর্ট: withdrawal-সহ যেকোনো ট্যাবে মাঝে মাঝে re-entry-তে শিমার আটকে
    // যাচ্ছিল, পেজ খুলছিল না, অন্য পেজ ঘুরে re-entry করলে ঠিক হয়ে যাচ্ছিল — শুধু withdrawal না,
    // সব পেজেই) — AndroidX Lifecycle-এর নিজস্ব আচরণ অনুযায়ী, lifecycle ইতিমধ্যে RESUMED অবস্থায়
    // থাকা অবস্থায় একটা নতুন LifecycleEventObserver addObserver() করা হলে, সেই lifecycle
    // observer-টাকে বর্তমান state পর্যন্ত "catch up" করাতে ON_CREATE/ON_START/ON_RESUME
    // synchronously রিপ্লে করে দেয় — even যদি ব্যবহারকারী আদৌ backgrounded/foregrounded না করে
    // থাকেন। যেহেতু এই DisposableEffect প্রতিবার sessionKey-এর composable ফ্রেশ mount হলেই
    // (tab switch করে আবার সেই ট্যাবে ফেরা সহ, ঠিক এটাই "re-entry") নতুন করে রেজিস্টার হয়,
    // প্রতিটা re-entry-তেই এই synthetic ON_RESUME ফায়ার হতো — [syncPhase] তখন ERROR থাকলে
    // (যেকোনো আগের transient network hiccup-এর কারণে) সাথে সাথে [onRetry] কল হয়ে যেত, যেটা
    // [initialSyncPhase]-এর মতো **সম্পূর্ণ app-wide শেয়ার্ড** phase-কে আবার LOADING-এ ফিরিয়ে
    // দিত (দেখুন SupabaseRealtimeManager.performInitialSync) — ফলে সেই মুহূর্তে composed থাকা
    // *প্রতিটা* SyncAwareContent/SyncAwareRefreshableContent স্ক্রিনই skeleton-এ আটকে যেত,
    // শুধু যেই ট্যাবে re-entry হয়েছিল সেটাই না। নিচে প্রথম ON_RESUME (registration-এর সাথে
    // সাথেই আসা synthetic replay) স্কিপ করা হচ্ছে — শুধু তার *পরের* ON_RESUME (যেটার আগে সত্যিকার
    // ON_PAUSE ঘটেছে, অর্থাৎ প্রকৃত ব্যাকগ্রাউন্ড/ফোরগ্রাউন্ড বা genuine navigation pause/resume)
    // থেকেই auto-retry ট্রিগার হবে।
    val latestSyncPhase by rememberUpdatedState(syncPhase)
    val latestOnRetry by rememberUpdatedState(onRetry)
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(sessionKey, lifecycleOwner) {
        var hasSkippedSynthenticInitialResume = false
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (!hasSkippedSynthenticInitialResume) {
                    hasSkippedSynthenticInitialResume = true
                } else if (latestSyncPhase == SupabaseRealtimeManager.SyncPhase.ERROR) {
                    if (SyncResumeRetryCooldown.tryConsume(sessionKey, System.currentTimeMillis())) {
                        latestOnRetry()
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val showCachedOnError = rememberShowCachedOnSyncError(viewModel)
    Crossfade(
        targetState = effectivePhase,
        animationSpec = tween(durationMillis = 260),
        modifier = modifier,
        label = "sync_aware_crossfade"
    ) { phase ->
        when (phase) {
            SupabaseRealtimeManager.SyncPhase.LOADING -> skeleton()
            SupabaseRealtimeManager.SyncPhase.LOADED -> content()
            SupabaseRealtimeManager.SyncPhase.ERROR ->
                if (showCachedOnError) content() else SyncErrorState(onRetry = onRetry)
        }
    }
}

/**
 * Realtime-Aware, Structure-Preserving Refresh — ধাপ ২ (নতুন, সংযোজন-মাত্র)।
 *
 * সমস্যা: উপরের [SyncAwareContent] প্রথমবার স্ক্রিনে ঢোকার (cold load) অভিজ্ঞতা খুব ভালোভাবে
 * হ্যান্ডল করে — প্রথমবার পুরো skeleton, তারপর content, একবার [SomadhanViewModel.markLoadedOnce]
 * হয়ে গেলে সেশনে আর কখনো flash না। কিন্তু স্ক্রিন খোলা অবস্থায় realtime-এ আসল ডেটা সত্যিই
 * বদলে/নতুন যোগ হলে (বা স্ক্রিন থেকে বেরিয়ে আবার ঢুকলে, ততক্ষণে ডেটা বদলে থাকলে), সেটা
 * [SyncAwareContent]-এর স্কোপের বাইরে ছিল — ও শুধু [SupabaseRealtimeManager.SyncPhase] দেখে,
 * `LOADED`-এর ভেতরে আসল ডেটা কনটেন্ট বদলেছে কিনা তা দেখে না। ফলে ইতিমধ্যে লোড হওয়া কোনো স্ক্রিনে
 * realtime আপডেট এলে পুরো পেজ আবার re-skeleton হয়ে যেত (বা কিছুই বদলাতো না, ডেটা স্থির থাকা
 * সত্ত্বেও অহেতুক flash হতো) — কাঠামো (header/filter/tab) ঠিক থেকে শুধু তালিকা/মানের জায়গাটুকু
 * সংক্ষিপ্ত সময়ের জন্য shimmer করে নতুন ডেটা আনার ব্যবস্থা ছিল না।
 *
 * সমাধান: এই নতুন generic কম্পোনেন্ট [SyncAwareContent]-এর প্রথম-ভিজিট আচরণ হুবহু অক্ষত রেখে
 * (নিচে একই [rememberSessionAwareSkeletonGate] reuse করা হয়েছে, আলাদা কিছু বানানো হয়নি) তার
 * উপরে একটা [data]: T প্যারামিটার যোগ করছে। যেই সেশনে এই [sessionKey]-এর জন্য প্রথমবার `LOADED`
 * অবস্থায় পৌঁছায়, সেই মুহূর্তের [data]-কেই কোনো flash ছাড়া "সবশেষ দেখানো" ধরে নেওয়া হয় (প্রথম
 * ভিজিটের skeleton ইতিমধ্যেই উপরের গেট দিয়ে দেখানো হয়ে গেছে, এখানে দ্বিতীয়বার দরকার নেই)। তারপর
 * থেকে প্রতিবার নতুন [data] এলে সাধারণ Kotlin `==` (data class হলে structural equality এমনিতেই
 * কাজ করে) দিয়ে আগের রাখা মানের সাথে তুলনা হয় — অমিল পেলে সংক্ষিপ্ত [refreshFlashMs] সময়ের জন্য
 * (ডিফল্ট ৩৫০ms, প্রয়োজনে override-যোগ্য) [skeleton] দেখিয়ে তারপর নতুন [data] দিয়ে [content]
 * দেখানো হয়; মিলে গেলে (ডেটা সত্যিই অপরিবর্তিত) কিচ্ছু হয় না — নীরবে একই [content] থেকে যায়,
 * কোনো flash/shimmer ছাড়াই।
 *
 * পুরনো [SyncAwareContent] (non-generic, [data] প্যারামিটার নেই, ৪২টা বিদ্যমান কল-সাইট) এই ধাপে
 * **কোনোভাবেই বদলানো/মোছা হয়নি** — এই কম্পোনেন্ট তার পাশে নতুন যোগ হলো, প্রতিস্থাপন না। বিদ্যমান
 * কোনো স্ক্রিন এখনো এটা ব্যবহার করছে না; স্ক্রিনগুলো পরের ধাপগুলোতে একে একে, স্বাধীনভাবে migrate
 * হবে। caller নিজেই header/filter/tab-এর মতো বাইরের কাঠামো এই কম্পোনেন্টের *বাইরে* রাখবে — ভেতরে
 * শুধু আসল তালিকা/মান অংশটুকু যাবে, যাতে realtime রিফ্রেশে পুরো পেজ আর re-skeleton না হয়ে শুধু
 * এই অংশটুকুই সংক্ষিপ্ত সময়ের জন্য shimmer করে।
 *
 * ERROR অবস্থা ও ON_RESUME auto-retry [SyncAwareContent]-এর হুবহু একই প্যাটার্নে, consistency-এর
 * জন্য (আলাদা আচরণ বানানোর দরকার নেই — এই দুই কম্পোনেন্টের মধ্যে caller-রা যেকোনো একটা বেছে নিলেও
 * error/retry-এর অভিজ্ঞতা একই থাকা উচিত)।
 *
 * **"extra feature" (ধাপ ৫-এ সংযোজন):** ঐচ্ছিক [isManualRefreshing] প্যারামিটার — যে স্ক্রিনে
 * `SomadhanPullToRefresh` আছে সেখানে caller তার `isRefreshing` state-টাই এখানে পাস করবে। এটা
 * সত্যি থেকে মিথ্যায় ফিরলে (ব্যবহারকারীর pull-to-refresh সাইকেল সবেমাত্র শেষ হলো) — তালিকা সত্যিই
 * বদলেছে কিনা তা না দেখেই সংক্ষিপ্ত সময়ের জন্য flash হবে, যাতে ম্যানুয়াল রিফ্রেশে সবসময় একটা
 * visual feedback থাকে। ডিফল্ট `false` — যে স্ক্রিনে pull-to-refresh নেই (যেমন
 * `ActiveJobsPopupScreen`) তাদের কোনো পরিবর্তন ছাড়াই আগের আচরণ (শুধু data সত্যিই বদলালে flash)
 * অক্ষত থাকে।
 *
 * **"re-entry-ও রিফ্রেশ" (ধাপ ৬-এ সংযোজন, ব্যবহারকারীর সিদ্ধান্ত "পথ B"):** এই sessionKey এই app
 * session-এ *এর আগেই* একবার লোড হয়ে গিয়ে থাকলে (অর্থাৎ এটা প্রথম-ভিজিট না, বরং স্ক্রিন থেকে
 * বেরিয়ে আবার ঢোকা/re-entry) — তাহলে mount হওয়ার সাথে সাথেই (data সত্যিই বদলেছে কিনা তা না
 * দেখেই) একবার সংক্ষিপ্ত [refreshFlashMs] flash হবে, তারপর content দেখানো হবে। যুক্তি: স্ক্রিনটা
 * বন্ধ/অদৃশ্য থাকা অবস্থায় realtime-এ ডেটা সত্যিই বদলে থাকতে পারে, তাই re-entry-কেও একধরনের
 * "রিফ্রেশ" ধরে "সর্বশেষ ডেটা আনা হচ্ছে" এই অনুভূতি দেওয়া — অনেকটা [isManualRefreshing]
 * extra feature-এরই মতো, ম্যানুয়াল বাটনের বদলে ট্রিগার এখানে "স্ক্রিনে আবার ঢোকা"। এটা সত্যিকারের
 * প্রথম cold visit-কে প্রভাবিত করে না (তখন উপরের skeleton gate ইতিমধ্যেই পুরো skeleton দেখিয়ে
 * দিচ্ছে, এই flash আলাদাভাবে সেখানে যোগ হয় না)। ঐচ্ছিক [flashOnReentry] দিয়ে কোনো নির্দিষ্ট
 * স্ক্রিনে এটা বন্ধ রাখা যাবে (ডিফল্ট `true`, যেহেতু এটাই এখন থেকে সব migrate-হওয়া স্ক্রিনের
 * standard আচরণ)।
 */
@Composable
fun <T> SyncAwareRefreshableContent(
    sessionKey: String,
    viewModel: SomadhanViewModel,
    syncPhase: SupabaseRealtimeManager.SyncPhase,
    data: T,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    refreshFlashMs: Long = 350L,
    isManualRefreshing: Boolean = false,
    flashOnReentry: Boolean = true,
    skeleton: @Composable () -> Unit = { ListScreenSkeleton() },
    content: @Composable (T) -> Unit
) {
    // প্রথম-ভিজিট আচরণ: [SyncAwareContent]-এর মতোই হুবহু, একই গেট reuse করে (নতুন কিছু বানানো
    // হয়নি) — এই sessionKey এই app session-এ প্রথমবার এখানে এলে অন্তত ন্যূনতম সময় পুরো skeleton
    // দেখাবে, global syncPhase ততক্ষণে LOADED হয়ে গেলেও।
    // ধাপ ৬: এই কম্পোজেবল ইনস্ট্যান্স mount হওয়ার মুহূর্তেই ক্যাপচার করা হচ্ছে viewModel-এ এই
    // sessionKey আগে থেকেই "লোড হয়ে গেছে" চিহ্নিত ছিল কিনা (মানে এটা re-entry, প্রথম visit না)।
    // [rememberSessionAwareSkeletonGate]-এর নিচের কলটার আগেই ক্যাপচার করা হচ্ছে যাতে তার ভেতরের
    // hasLoadedOnce-চেকের সাথে consistent থাকে (দুটোই একই মুহূর্তের অবস্থা দেখছে)।
    val isReentryVisit = remember(sessionKey) { viewModel.hasLoadedOnce(sessionKey) }
    val withinFirstVisitWindow = rememberSessionAwareSkeletonGate(sessionKey, viewModel)
    val effectivePhase = if (syncPhase == SupabaseRealtimeManager.SyncPhase.LOADED && withinFirstVisitWindow) {
        SupabaseRealtimeManager.SyncPhase.LOADING
    } else {
        syncPhase
    }

    if (effectivePhase == SupabaseRealtimeManager.SyncPhase.LOADED) {
        LaunchedEffect(sessionKey) { viewModel.markLoadedOnce(sessionKey) }
    }

    // ON_RESUME auto-retry — [SyncAwareContent]-এর ধাপ ৫ প্যাটার্ন হুবহু, একই cooldown অবজেক্ট
    // ([SyncResumeRetryCooldown]) sessionKey-ভিত্তিক শেয়ার করা হচ্ছে যাতে দুই কম্পোনেন্ট মিলিয়ে
    // ব্যবহার করলেও (বা ভবিষ্যতে migrate করলেও) retry-storm প্রতিরোধ একই জায়গা থেকে হয়।
    // রুট-কজ ফিক্স (একই কারণে, দেখো SyncAwareContent-এর উপরের বিস্তারিত কমেন্ট) — প্রথম,
    // registration-মুহূর্তের synthetic ON_RESUME replay স্কিপ করা হচ্ছে, শুধু তার পরেরটা থেকেই
    // auto-retry ট্রিগার হবে।
    val latestSyncPhase by rememberUpdatedState(syncPhase)
    val latestOnRetry by rememberUpdatedState(onRetry)
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(sessionKey, lifecycleOwner) {
        var hasSkippedSynthenticInitialResume = false
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (!hasSkippedSynthenticInitialResume) {
                    hasSkippedSynthenticInitialResume = true
                } else if (latestSyncPhase == SupabaseRealtimeManager.SyncPhase.ERROR) {
                    if (SyncResumeRetryCooldown.tryConsume(sessionKey, System.currentTimeMillis())) {
                        latestOnRetry()
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // --- নতুন অংশ: প্রথম-ভিজিটের পরে আসল ডেটা সত্যিই বদলেছে কিনা ট্র্যাক করা ---
    // lastShownData: সবশেষ যে data দিয়ে content() দেখানো হয়েছে। [SyncAwareRefreshableUninitialized]
    // দিয়ে শুরু (এখনো কিছু দেখানো হয়নি) — T নিজে nullable নাও হতে পারে বলে Any?-টাইপ sentinel।
    // flashRequestCount: বুলিয়ানের বদলে কাউন্টার — কারণ দুইটা স্বাধীন কারণে flash ট্রিগার হতে পারে
    // (ডেটা-বদল আর ম্যানুয়াল-রিফ্রেশ-সমাপ্তি, নিচে দেখুন), একই সময়ে দুটো ওভারল্যাপ করলে যেন একটা
    // আরেকটাকে অকালে বন্ধ করে না দেয়।
    var lastShownData by remember(sessionKey) { mutableStateOf<Any?>(SyncAwareRefreshableUninitialized) }
    // রুট-কজ ফিক্স (রিপোর্ট: re-entry flash হওয়ার ঠিক *আগে* এক ফ্রেমের জন্য পুরনো/আসল data
    // দেখা যাচ্ছিল, তারপর শিমার খুলছিল — শুধু re-entry-তে না, এই কম্পোনেন্ট ব্যবহারকারী সব
    // স্ক্রিনেই একই কাঠামো বলে একই বাগ)। root cause: flashRequestCount আগে সবসময় 0 দিয়ে শুরু
    // হতো, আর re-entry-তে flash ট্রিগার করার সিদ্ধান্তটা নেওয়া হতো নিচের LaunchedEffect(data)-এর
    // ভেতরে — যেটা প্রথম composition commit হওয়ার *পরে* asynchronously রান হয়। ফলে প্রথম
    // ফ্রেমে isRefreshFlashing=false-ই থাকতো (effect তখনো রান হয়নি), তাই lastShownData
    // ===Uninitialized দেখে নিচের Crossfade সরাসরি আসল data-ই দেখিয়ে দিত — effect রান হওয়ার
    // পরই flashRequestCount++ হয়ে শিমার "খুলতো", যা "আগে data, পরে শিমার" এই উল্টো ক্রম তৈরি
    // করছিল। ফিক্স: এই মুহূর্তে re-entry-flash লাগবে কিনা তা synchronously, প্রথম composition-এর
    // মধ্যেই ঠিক করা হচ্ছে ([effectivePhase]/[isReentryVisit]/[flashOnReentry] — তিনটাই এই
    // মুহূর্তে ইতিমধ্যে জানা, কোনো coroutine/delay লাগে না) — তাই flashRequestCount-এর initial
    // value-ই সরাসরি ১ (দরকার হলে) দিয়ে শুরু হয়, প্রথম ফ্রেম থেকেই শিমার দেখাবে।
    val needsReentryFlashOnMount = remember(sessionKey) {
        effectivePhase == SupabaseRealtimeManager.SyncPhase.LOADED && flashOnReentry && isReentryVisit
    }
    var flashRequestCount by remember(sessionKey) { mutableStateOf(if (needsReentryFlashOnMount) 1 else 0) }
    val isRefreshFlashing = flashRequestCount > 0

    if (effectivePhase == SupabaseRealtimeManager.SyncPhase.LOADED) {
        LaunchedEffect(data) {
            val previous = lastShownData
            if (previous === SyncAwareRefreshableUninitialized) {
                if (needsReentryFlashOnMount) {
                    // flashRequestCount ইতিমধ্যে উপরে synchronously ১ দিয়ে শুরু হয়ে গেছে (তাই
                    // এখানে আবার ++ করা হচ্ছে না, ডাবল-কাউন্ট এড়াতে) — শুধু flash-duration
                    // অপেক্ষা করে data-কে "সবশেষ দেখানো" হিসেবে রেকর্ড করে, শেষে ঠিক একবার --।
                    try {
                        delay(refreshFlashMs)
                        lastShownData = data
                    } finally {
                        flashRequestCount--
                    }
                } else {
                    // সত্যিকারের প্রথম cold visit (বা এই স্ক্রিনে flashOnReentry বন্ধ করা আছে) —
                    // উপরের গেট ইতিমধ্যে পুরো skeleton দেখিয়ে দিয়েছে, এখানে দ্বিতীয়বার flash না
                    // করে সরাসরি এই data-কেই "সবশেষ দেখানো" হিসেবে রেকর্ড করা হচ্ছে।
                    lastShownData = data
                }
            } else if (previous != data) {
                // আসল ডেটা সত্যিই বদলেছে (structural `!=`) — সংক্ষিপ্ত [refreshFlashMs] সময়ের
                // জন্য skeleton দেখিয়ে তারপর নতুন data-কে "সবশেষ দেখানো" হিসেবে আপডেট করা। এই
                // LaunchedEffect [data]-এর উপর keyed, তাই flash চলাকালীন আবার data বদলে গেলে
                // পুরনো delay বাতিল হয়ে নতুন করে শুরু হয় (স্টেল ডেটা দেখানো হয় না)।
                flashRequestCount++
                try {
                    delay(refreshFlashMs)
                    lastShownData = data
                } finally {
                    flashRequestCount--
                }
            }
            // previous == data (সত্যিই অপরিবর্তিত) হলে ইচ্ছাকৃতভাবে কিছুই করা হয় না — নীরবে
            // একই content থেকে যাবে, কোনো flash/shimmer ছাড়াই।
        }

        // "extra feature" — [isManualRefreshing] সত্যি থেকে মিথ্যায় ফিরলে (pull-to-refresh
        // সাইকেল শেষ হলো), তালিকা সত্যিই বদলেছে কিনা তা না দেখেই সংক্ষিপ্ত flash — উপরের
        // data-change effect থেকে সম্পূর্ণ স্বাধীন, তাই দুটো একসাথে ঘটলেও flashRequestCount
        // ঠিকভাবে সামলে নেয় (একটা শেষ হলেও অন্যটা চলতে থাকলে flash বন্ধ হবে না)।
        var wasManualRefreshing by remember(sessionKey) { mutableStateOf(isManualRefreshing) }
        LaunchedEffect(isManualRefreshing) {
            if (wasManualRefreshing && !isManualRefreshing) {
                flashRequestCount++
                try {
                    delay(refreshFlashMs)
                    lastShownData = data
                } finally {
                    flashRequestCount--
                }
            }
            wasManualRefreshing = isManualRefreshing
        }
    }

    val showCachedOnError = rememberShowCachedOnSyncError(viewModel)
    Crossfade(
        targetState = effectivePhase,
        animationSpec = tween(durationMillis = 260),
        modifier = modifier,
        label = "sync_aware_refreshable_crossfade"
    ) { phase ->
        when (phase) {
            SupabaseRealtimeManager.SyncPhase.LOADING -> skeleton()
            SupabaseRealtimeManager.SyncPhase.ERROR ->
                if (showCachedOnError) content(data) else SyncErrorState(onRetry = onRetry)
            SupabaseRealtimeManager.SyncPhase.LOADED -> {
                // ভেতরের ছোট Crossfade: শুধু flash windowটুকুর জন্য skeleton <-> content, বাইরের
                // পুরো-পেজ ক্রসফেডের (উপরে) সাথে না গুলিয়ে। এতে caller-এর header/filter/tab
                // (এই কম্পোনেন্টের বাইরে থাকা) কখনোই re-animate হয় না, শুধু এই ভেতরের অংশটুকু।
                Crossfade(
                    targetState = isRefreshFlashing,
                    animationSpec = tween(durationMillis = 200),
                    label = "sync_aware_refreshable_flash_crossfade"
                ) { flashing ->
                    if (flashing) {
                        skeleton()
                    } else {
                        @Suppress("UNCHECKED_CAST")
                        val shown = (if (lastShownData === SyncAwareRefreshableUninitialized) data else lastShownData) as T
                        content(shown)
                    }
                }
            }
        }
    }
}

/**
 * [SyncAwareRefreshableContent]-এর জন্য sentinel — "এখনো কোনো data দেখানো হয়নি" বোঝাতে, যেহেতু
 * generic `T` নিজে nullable নাও হতে পারে (তাই `null`-কে sentinel হিসেবে ব্যবহার করা যায় না)।
 */
private object SyncAwareRefreshableUninitialized

/**
 * [Offline Action Gating ধাপ ১৩ - ফলো-আপ] ERROR অবস্থায় SyncErrorState-এর বদলে cached কনটেন্ট
 * দেখানো হবে কিনা। শর্ত: (১) Strict Offline Block টগল OFF (Strict মোডের আচরণ অপরিবর্তিত) এবং
 * (২) এই ইউজারের জন্য আগে অন্তত একবার সম্পূর্ণ সফল bulk-pull হয়েছে (নইলে লোকাল Room প্রায় খালি,
 * তখন retry-সহ ERROR স্ক্রিনই সঠিক)। মূল কারণ: "wifi আছে কিন্তু ইন্টারনেট নেই"-তে bulk-pull
 * ২০s timeout খেলে phase ERROR হয়, অথচ Room-এ দেখানোর মতো আসল ডেটা আছে। retry পথগুলো (ON_RESUME
 * auto-retry, reconnect retry, pull-to-refresh-এর retry-if-ERROR) অপরিবর্তিত থাকে।
 */
@Composable
private fun rememberShowCachedOnSyncError(viewModel: SomadhanViewModel): Boolean {
    val strictBlock by viewModel.isStrictOfflineBlockEnabled.collectAsStateWithLifecycle()
    return !strictBlock && viewModel.hasCleanSyncHistory()
}

/**
 * Loading/Sync Fix Roadmap v2, ধাপ ৪ — একাধিক [SupabaseRealtimeManager.SyncPhase]-নির্ভর স্ক্রিনের
 * (যেমন `SolverReviewsScreen`/`UserReviewsScreen`, যেগুলো `users` টেবিলের `initialSyncPhase` আর
 * ratings-এর `ratingsSyncPhase` — দুটোর উপরই নির্ভরশীল) জন্য ছোট combine helper। যেকোনো একটা
 * `ERROR` হলে `ERROR`, নাহলে যেকোনো একটা `LOADING` হলে `LOADING`, সবগুলো `LOADED` হলেই `LOADED` —
 * roadmap-এর ধাপ ৪ নির্দেশনার "সবচেয়ে খারাপ অবস্থা" নিয়মটাই এখানে প্রতিফলিত।
 */
fun worstSyncPhase(
    vararg phases: SupabaseRealtimeManager.SyncPhase
): SupabaseRealtimeManager.SyncPhase = when {
    phases.any { it == SupabaseRealtimeManager.SyncPhase.ERROR } -> SupabaseRealtimeManager.SyncPhase.ERROR
    phases.any { it == SupabaseRealtimeManager.SyncPhase.LOADING } -> SupabaseRealtimeManager.SyncPhase.LOADING
    else -> SupabaseRealtimeManager.SyncPhase.LOADED
}

/**
 * Loading/Sync Fix Roadmap v2, ধাপ ৪ (Group B সম্প্রসারণ) — এমন স্ক্রিনের জন্য যেগুলো
 * [SyncAwareContent]-এর content-lambda-ভিত্তিক প্যাটার্নে না গিয়ে নিজস্ব
 * `rememberSessionAwareSkeletonGate`-ভিত্তিক early-return loading প্যাটার্ন ব্যবহার করে
 * (যেমন `JobTrackingScreen`/`ReputationDetailScreen` — এদের সম্পূর্ণ ফাংশন-বডি একটা
 * content-lambda-তে মুড়ে ফেলা খুবই ঝুঁকিপূর্ণ, কারণ ফাইলগুলো অনেক বড় ও জটিল)। যখন প্রকৃত
 * ডাটা (problem/user ইত্যাদি, যেটার জন্য early-return গেট বসানো আছে) এখনো `null`/absent
 * থাকে *এবং* প্রাসঙ্গিক [SupabaseRealtimeManager.SyncPhase] ইতিমধ্যে `ERROR`-এ চলে গেছে
 * (অর্থাৎ ডাটা আসার আর কোনো সম্ভাবনা নেই যতক্ষণ না retry হয়), তখন সেই একই early-return-এর
 * জায়গায় এটা কল করলে চিরকাল স্কেলিটনে আটকে থাকার বদলে [SyncErrorState]-এর মতোই এরর+রিট্রাই
 * UI দেখাবে। কল-সাইট নিজে থেকেই ঠিক করে কখন এটা দেখাবে (এই কম্পোনেন্ট নিজে কোনো condition
 * চেক করে না) — তাই বিদ্যমান LOADING স্কেলিটন/minimumSkeletonActive যুক্তি অক্ষত থাকে।
 */
@Composable
fun SyncBlockedRetryState(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    SyncErrorState(onRetry = onRetry, modifier = modifier)
}

/**
 * [SyncAwareContent]-এর `ERROR` অবস্থার UI — একটা ওয়াইফাই-অফ আইকন, স্পষ্ট বাংলা মেসেজ, আর
 * একটা "আবার চেষ্টা করুন" বাটন। [NoInternetOverlay]-এর মতো ডিভাইস-লেভেল ফুল-স্ক্রিন ইন্টারসেপ্ট
 * না — এটা একটা নির্দিষ্ট স্ক্রিন/সেকশনের কন্টেন্ট-এরিয়ার ভেতরেই বসে, যাতে ৩৩টা স্ক্রিনের
 * বিদ্যমান TopAppBar/bottom-nav/scaffold স্পর্শ না করেই ধাপ ৪-এ বসানো যায়।
 */
@Composable
private fun SyncErrorState(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(SomadhanBg)
            .padding(horizontal = 24.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.WifiOff,
            contentDescription = "ডাটা লোড করা যায়নি",
            tint = SomadhanError,
            modifier = Modifier.size(40.dp)
        )
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = "ডাটা লোড করা যায়নি, ইন্টারনেট চেক করে আবার চেষ্টা করুন",
            fontSize = 14.sp,
            color = SomadhanTextSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(18.dp))
        Button(
            onClick = onRetry,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SomadhanOrange)
        ) {
            Text(
                text = "আবার চেষ্টা করুন",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }
    }
}
