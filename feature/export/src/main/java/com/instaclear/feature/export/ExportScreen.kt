package com.instaclear.feature.export

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.instaclear.core.ui.components.glassmorphism
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape

@Composable
fun ExportScreen(
    outputUriString: String,
    onBackToStart: () -> Unit
) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Tayyor! ✨", style = MaterialTheme.typography.displayMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Fayl ijtimoiy tarmoqlar uchun to'liq optimallashtirildi.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(64.dp))
            
            Button(
                onClick = {
                    val uris = outputUriString.split(",").map { Uri.parse(it) }
                    val shareIntent = if (uris.size > 1) {
                        Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                            type = "*/*"
                            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            // setPackage("com.instagram.android")
                        }
                    } else {
                        Intent(Intent.ACTION_SEND).apply {
                            type = "*/*"
                            putExtra(Intent.EXTRA_STREAM, uris.firstOrNull())
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            // setPackage("com.instagram.android")
                        }
                    }

                    try {
                        // Removing setPackage makes it open a native Share Sheet (Apple style "It just works")
                        // Which allows sharing to Instagram, TikTok, Facebook etc automatically.
                        val chooser = Intent.createChooser(shareIntent, "Ulashish")
                        context.startActivity(chooser)
                    } catch (e: Exception) {
                        // Fallback
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .glassmorphism(cornerRadius = 32f),
                shape = CircleShape
            ) {
                Text("Ulashish (Share)", style = MaterialTheme.typography.titleMedium)
            }

            Spacer(modifier = Modifier.height(24.dp))
            androidx.compose.material3.OutlinedButton(
                onClick = onBackToStart,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape = CircleShape,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Text("Boshqa fayl tanlash", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}
