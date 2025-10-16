package com.acassion.optifluxapp.ui.screens.cameraview

import androidx.camera.compose.CameraXViewfinder
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.acassion.optifluxapp.viewmodel.CameraPreviewViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraViewScreen() {
    val cameraPermissionState = rememberPermissionState(permission = android.Manifest.permission.CAMERA)

    when (cameraPermissionState.status.isGranted) {
        true -> CameraAccessGranted()
        false -> CameraAccessDenied(
            cameraPermissionState = cameraPermissionState
        )
    }
}

@Composable
@Preview
fun CameraAccessGranted(
    modifier: Modifier = Modifier,
    viewModel: CameraPreviewViewModel = viewModel(),
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current
) {
    val surfaceRequest =  viewModel.surfaceRequest.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(lifecycleOwner) {
        viewModel.bindToCamera(
            applicationContext = context.applicationContext,
            lifecycleOwner = lifecycleOwner
        )
    }

    surfaceRequest.value?.let { request ->
        CameraXViewfinder(
            surfaceRequest = surfaceRequest.value!!,
            modifier = modifier
        )
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Preview
@Composable
fun CameraAccessDenied(
    modifier: Modifier = Modifier,
    shouldShowRationale: Boolean = false,
    cameraPermissionState: PermissionState? = null
) {
    val message = when (shouldShowRationale) {
        // here the user denied the permission but the rational can be shown
        true -> "This apps needs the camera permission in order to get the feed and calculate the optical flux."
        false -> "This apps needs the camera permission in order to get the feed and calculate the optical flux."
    }

    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = message)
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = { cameraPermissionState?.launchPermissionRequest() }
        ) { Text(text = "Grant permission") }
    }
}