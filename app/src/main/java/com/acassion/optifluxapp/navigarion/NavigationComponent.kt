package com.acassion.optifluxapp.navigarion

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.acassion.optifluxapp.ui.screens.cameraview.CameraViewScreen
import com.acassion.optifluxapp.ui.screens.settings.SettingsScreen


enum class NavRoutes {
    CameraView,
    Settings
}

@Composable
fun NavigationComponent() {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = NavRoutes.CameraView.name
    ) {
        composable(NavRoutes.CameraView.name ) { CameraViewScreen() }
        composable(NavRoutes.Settings.name ) { SettingsScreen() }
    }
}