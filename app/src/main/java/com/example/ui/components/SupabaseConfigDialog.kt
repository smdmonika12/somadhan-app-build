package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.SomadhanBorder
import com.example.ui.theme.SomadhanCardBg
import com.example.ui.theme.SomadhanError
import com.example.ui.theme.SomadhanInfo
import com.example.ui.theme.SomadhanOrange
import com.example.ui.theme.SomadhanSuccess
import com.example.ui.theme.SomadhanTextPrimary
import com.example.ui.theme.SomadhanTextSecondary
import com.example.ui.viewmodel.SomadhanViewModel
import com.example.util.SupabaseConfigHelper
import kotlinx.coroutines.launch

/**
 * সমাধান (Somadhan) — ধাপ ৩৩.৩ কাজ ৪
 *
 * পুরনো `FirebaseConfigDialog`-এর Supabase-সমতুল্য — অ্যাডমিন-প্যানেলের "ক্লাউড প্রজেক্ট
 * রিকনফিগার" টুলটা এখন Supabase Project URL/anon key দিয়ে কাজ করে (Firebase project id/API
 * key এর বদলে)। পুরনো `FirebaseConfigDialog.kt`/`FirebaseConfigHelper.kt` ডিলিট করা হয়নি
 * (অনুরোধ অনুযায়ী), শুধু এই নতুন ফাইল/ফাংশনটা call-site গুলোতে তার জায়গা নিয়েছে।
 */
@Composable
fun SupabaseConfigDialog(
    viewModel: SomadhanViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var url by remember { mutableStateOf(SupabaseConfigHelper.getSavedUrl(context)) }
    var anonKey by remember { mutableStateOf(SupabaseConfigHelper.getSavedAnonKey(context)) }

    var isSaving by remember { mutableStateOf(false) }
    var testResultText by remember { mutableStateOf<String?>(null) }
    var isTestSuccess by remember { mutableStateOf<Boolean?>(null) }
    var isTesting by remember { mutableStateOf(false) }

    BottomSlideAlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        title = null,
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .background(SomadhanOrange.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudSync,
                                contentDescription = null,
                                tint = SomadhanOrange,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Supabase ক্লাউড প্রজেক্ট কনফিগ",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = SomadhanTextPrimary
                            )
                            Text(
                                text = "আপনার নিজস্ব Supabase প্রজেক্ট সংযোগ করুন",
                                fontSize = 11.sp,
                                color = SomadhanTextSecondary
                            )
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = SomadhanTextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = SomadhanBorder)
                Spacer(modifier = Modifier.height(14.dp))

                Surface(
                    color = SomadhanInfo.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = SomadhanInfo,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "আপনার Supabase প্রজেক্টের Project URL এবং anon (public) key প্রদান করে সেভ করলেই রিয়েল-টাইম সিঙ্ক নতুন প্রজেক্টে চালু হবে।",
                            fontSize = 11.5.sp,
                            color = SomadhanTextSecondary,
                            lineHeight = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text("Supabase Project URL *", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = SomadhanTextPrimary)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    placeholder = { Text("https://xxxxxxxx.supabase.co", fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("supabase_config_url_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SomadhanOrange,
                        unfocusedBorderColor = SomadhanBorder,
                        focusedTextColor = SomadhanTextPrimary,
                        unfocusedTextColor = SomadhanTextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text("Anon (public) Key *", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = SomadhanTextPrimary)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = anonKey,
                    onValueChange = { anonKey = it },
                    placeholder = { Text("eyJhbGciOi...", fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("supabase_config_anon_key_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SomadhanOrange,
                        unfocusedBorderColor = SomadhanBorder,
                        focusedTextColor = SomadhanTextPrimary,
                        unfocusedTextColor = SomadhanTextPrimary
                    )
                )

                if (testResultText != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        color = if (isTestSuccess == true) SomadhanSuccess.copy(alpha = 0.12f) else SomadhanError.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                1.dp,
                                if (isTestSuccess == true) SomadhanSuccess.copy(alpha = 0.4f) else SomadhanError.copy(alpha = 0.4f),
                                RoundedCornerShape(8.dp)
                            )
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = if (isTestSuccess == true) Icons.Default.CheckCircle else Icons.Default.CloudOff,
                                contentDescription = null,
                                tint = if (isTestSuccess == true) SomadhanSuccess else SomadhanError,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = testResultText ?: "",
                                fontSize = 11.5.sp,
                                color = if (isTestSuccess == true) SomadhanSuccess else SomadhanError,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (url.isBlank() || anonKey.isBlank()) {
                        Toast.makeText(context, "অনুগ্রহ করে URL ও anon key দুটোই দিন", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    isSaving = true
                    viewModel.reconfigureSupabaseAndSync(
                        context = context,
                        url = url.trim(),
                        anonKey = anonKey.trim()
                    ) { success, msg ->
                        isSaving = false
                        isTestSuccess = success
                        testResultText = msg
                    }
                },
                enabled = !isSaving && !isTesting,
                colors = ButtonDefaults.buttonColors(containerColor = SomadhanOrange),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("supabase_config_save_button")
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(14.dp), color = Color.White, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("সিঙ্ক হচ্ছে...", fontSize = 12.sp, color = Color.White)
                } else {
                    Icon(Icons.Default.CloudDone, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("সংরক্ষণ ও লাইভ সিঙ্ক", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = {
                    isTesting = true
                    coroutineScope.launch {
                        val res = SupabaseConfigHelper.testConnection(context)
                        isTesting = false
                        isTestSuccess = res.first
                        testResultText = res.second
                    }
                },
                enabled = !isSaving && !isTesting,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = SomadhanInfo)
            ) {
                if (isTesting) {
                    CircularProgressIndicator(modifier = Modifier.size(14.dp), color = SomadhanInfo, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("কানেকশন টেস্ট", fontSize = 11.5.sp)
                }
            }
        },
        containerColor = SomadhanCardBg,
        shape = RoundedCornerShape(16.dp)
    )
}
