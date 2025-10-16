package com.acassion.optifluxapp.ui.screens.cameraview

import androidx.camera.compose.CameraXViewfinder
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
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
    val opticalFlowState = viewModel.opticalFlow.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(lifecycleOwner) {
        viewModel.bindToCamera(
            applicationContext = context.applicationContext,
            lifecycleOwner = lifecycleOwner
        )
    }

    surfaceRequest.value?.let { request ->
        Box {
            CameraXViewfinder(
                surfaceRequest = surfaceRequest.value!!,
                modifier = modifier
            )
            Canvas(modifier = Modifier.fillMaxSize().pointerInput(Unit) {}) {
                val w = size.width
                val h = size.height
                val fx = opticalFlowState.value.frameWidth.toFloat()
                val fy = opticalFlowState.value.frameHeight.toFloat()
                val rotation = opticalFlowState.value.rotation
                val isFrontCamera = opticalFlowState.value.isFrontCamera

                opticalFlowState.value.vectors.toList().chunked(5).forEach { vector ->
                    var frameX = vector[0]
                    var frameY = vector[1]
                    var frameU = vector[2]
                    var frameV = vector[3]
                    val magnitude = vector[4]

                    // Apply rotation transformation
                    // For front camera, we need to account for mirroring in the rotation
                    val (screenX, screenY, screenU, screenV) = when {
                        isFrontCamera && rotation == 270 -> {
                            // Front camera 270°: mirror then rotate
                            // Effective transformation: (x,y) -> (width-y, width-x) 
                            val scaleX = w / fy
                            val scaleY = h / fx
                            listOf((fy - frameY) * scaleX, (fx - frameX) * scaleY, -frameV * scaleX, -frameU * scaleY)
                        }
                        isFrontCamera && rotation == 90 -> {
                            // Front camera 90°
                            val scaleX = w / fy
                            val scaleY = h / fx
                            listOf(frameY * scaleX, frameX * scaleY, frameV * scaleX, frameU * scaleY)
                        }
                        isFrontCamera && rotation == 0 -> {
                            // Front camera no rotation - just mirror
                            val scaleX = w / fx
                            val scaleY = h / fy
                            listOf((fx - frameX) * scaleX, frameY * scaleY, -frameU * scaleX, frameV * scaleY)
                        }
                        rotation == 0 -> {
                            // Back camera, no rotation
                            val scaleX = w / fx
                            val scaleY = h / fy
                            listOf(frameX * scaleX, frameY * scaleY, frameU * scaleX, frameV * scaleY)
                        }
                        rotation == 90 -> {
                            // Back camera 90° clockwise
                            val scaleX = w / fy
                            val scaleY = h / fx
                            listOf((fy - frameY) * scaleX, frameX * scaleY, -frameV * scaleX, frameU * scaleY)
                        }
                        rotation == 270 -> {
                            // Back camera 270° clockwise
                            val scaleX = w / fy
                            val scaleY = h / fx
                            listOf(frameY * scaleX, (fx - frameX) * scaleY, frameV * scaleX, -frameU * scaleY)
                        }
                        else -> {
                            // Fallback
                            val scaleX = w / fx
                            val scaleY = h / fy
                            listOf(frameX * scaleX, frameY * scaleY, frameU * scaleX, frameV * scaleY)
                        }
                    }

                    val color = if (magnitude < 0.5f) {
                        Color.White.copy(alpha = 0.2f)
                    } else if (magnitude < 2f) {
                        Color.White.copy(alpha = 0.6f)
                    } else {
                        Color.White
                    }
                    drawLine(
                        color = color,
                        start = Offset(
                            x = screenX,
                            y = screenY
                        ),
                        end = Offset(
                            x = screenX+screenU*6f,
                            y = screenY+screenV*6f
                        ),
                        strokeWidth = 2f
                    )
                    // small arrow head
                    drawCircle(
                        color = color,
                        radius = 2f,
                        center = Offset(
                            x = screenX,
                            y = screenY
                        )
                    )
                }
            }
        }
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