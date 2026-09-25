package com.example

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * TEMPORARY DEBUG HELPER (safe to remove once the crash is fixed).
 * Shown instead of the app closing silently -- displays the full crash text directly on screen
 * so it can be read/copied without any PC, cable, or file manager.
 */
class CrashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val crashText = intent.getStringExtra("crash_text") ?: "কোনো crash তথ্য পাওয়া যায়নি।"
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFFFFF3F3)) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "অ্যাপ ক্র্যাশ হয়েছে — নিচের লেখাটা কপি করে Claude-কে পাঠান",
                            color = Color(0xFFB00020)
                        )
                        Button(onClick = {
                            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("crash_log", crashText))
                            Toast.makeText(this@CrashActivity, "কপি হয়েছে", Toast.LENGTH_SHORT).show()
                        }) {
                            Text("পুরো লেখা কপি করুন")
                        }
                        Text(crashText)
                    }
                }
            }
        }
    }
}
