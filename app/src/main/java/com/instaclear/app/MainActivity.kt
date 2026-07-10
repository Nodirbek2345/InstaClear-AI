package com.instaclear.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import com.instaclear.core.ui.theme.InstaClearTheme
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.instaclear.feature.picker.PickerScreen
import com.instaclear.feature.processing.ProcessingScreen
import com.instaclear.feature.export.ExportScreen
import com.instaclear.feature.camera.CameraScreen
import dagger.hilt.android.AndroidEntryPoint
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            InstaClearTheme {
                Surface {
                    val navController = rememberNavController()
                    NavHost(navController = navController, startDestination = "picker") {
                        composable("picker") {
                            PickerScreen(
                                onMediaSelected = { uris ->
                                    val encodedUris = uris.joinToString(",") { 
                                        URLEncoder.encode(it, StandardCharsets.UTF_8.toString()) 
                                    }
                                    navController.navigate("processing/$encodedUris")
                                },
                                onNavigateToCamera = {
                                    navController.navigate("camera")
                                }
                            )
                        }
                        composable("camera") {
                            CameraScreen(
                                onPhotoCaptured = { uri ->
                                    val encodedUri = URLEncoder.encode(uri, StandardCharsets.UTF_8.toString())
                                    navController.navigate("processing/$encodedUri")
                                },
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable("processing/{uris}") { backStackEntry ->
                            val urisString = backStackEntry.arguments?.getString("uris") ?: return@composable
                            ProcessingScreen(
                                urisString = urisString,
                                onFinished = { outputUri ->
                                    val encodedOutUri = URLEncoder.encode(outputUri, StandardCharsets.UTF_8.toString())
                                    navController.navigate("export/$encodedOutUri") {
                                        popUpTo("picker") { inclusive = false }
                                    }
                                }
                            )
                        }
                        composable("export/{uri}") { backStackEntry ->
                            val uri = backStackEntry.arguments?.getString("uri") ?: return@composable
                            ExportScreen(
                                outputUriString = uri,
                                onBackToStart = {
                                    navController.navigate("picker") {
                                        popUpTo("picker") { inclusive = true }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
