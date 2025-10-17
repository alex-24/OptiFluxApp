package com.acassion.optifluxapp.ui.screens.cameraview

import androidx.camera.compose.CameraXViewfinder
import androidx.camera.viewfinder.core.ImplementationMode
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
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
                modifier = modifier,
                implementationMode = ImplementationMode.EMBEDDED
            )
            /*Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color.Black
            )  {  }*/
            Canvas(modifier = Modifier.fillMaxSize().pointerInput(Unit) {}) {
                val w = size.width
                val h = size.height
                val fx = opticalFlowState.value.frameWidth.toFloat()
                val fy = opticalFlowState.value.frameHeight.toFloat()
                val rotation = opticalFlowState.value.rotation
                val isFrontCamera = opticalFlowState.value.isFrontCamera
                
                // Calculate the actual viewport dimensions considering rotation
                // When rotated 270° or 90°, frame dimensions are swapped relative to screen
                val isRotated = rotation == 90 || rotation == 270
                val effectiveFrameWidth = if (isRotated) fy else fx
                val effectiveFrameHeight = if (isRotated) fx else fy
                
                // FILL_CENTER mode: scale to fill entire canvas, crop what doesn't fit
                val scaleToFillWidth = w / effectiveFrameWidth
                val scaleToFillHeight = h / effectiveFrameHeight
                val scale = maxOf(scaleToFillWidth, scaleToFillHeight)  // Use MAX to fill
                
                // Calculate which portion of the frame is visible after cropping
                val scaledFrameWidth = effectiveFrameWidth * scale
                val scaledFrameHeight = effectiveFrameHeight * scale
                
                // Calculate crop offsets (what's cut off from the frame)
                val frameCropX = (scaledFrameWidth - w) / 2f
                val frameCropY = (scaledFrameHeight - h) / 2f
                
                if (opticalFlowState.value.vectors.isNotEmpty()) {
                    android.util.Log.d("CameraViewScreen", 
                        "Canvas: ${w}x${h}, Frame: ${fx}x${fy}, Rotation: $rotation°, " +
                        "EffectiveFrame: ${effectiveFrameWidth}x${effectiveFrameHeight}, " +
                        "Scale: $scale, ScaledFrame: ${scaledFrameWidth}x${scaledFrameHeight}, " +
                        "FrameCrop: (${frameCropX}, ${frameCropY})")
                }

                opticalFlowState.value.vectors.toList().chunked(5).forEach { vector ->
                    var frameX = vector[0]
                    var frameY = vector[1]
                    var frameU = vector[2]
                    var frameV = vector[3]
                    val magnitude = vector[4]

                    // Apply rotation transformation with proper scaling and crop offsets
                    val (screenX, screenY, screenU, screenV) = when {
                        isFrontCamera && rotation == 270 -> {
                            // Front camera 270°: mirror then rotate
                            // Scale to fill, then subtract the crop offset
                            val sx = (fy - frameY) * scale - frameCropX
                            val sy = (fx - frameX) * scale - frameCropY
                            listOf(
                                sx,
                                sy,
                                frameV * scale,
                                -frameU * scale
                            )
                        }
                        isFrontCamera && rotation == 90 -> {
                            // Front camera 90°
                            val sx = frameY * scale - frameCropX
                            val sy = frameX * scale - frameCropY
                            listOf(
                                sx,
                                sy,
                                frameV * scale,
                                frameU * scale
                            )
                        }
                        isFrontCamera && rotation == 0 -> {
                            // Front camera no rotation - just mirror
                            val sx = (fx - frameX) * scale - frameCropX
                            val sy = frameY * scale - frameCropY
                            listOf(
                                sx,
                                sy,
                                frameU * scale,
                                frameV * scale
                            )
                        }
                        rotation == 0 -> {
                            // Back camera, no rotation
                            val sx = frameX * scale - frameCropX
                            val sy = frameY * scale - frameCropY
                            listOf(
                                sx,
                                sy,
                                -frameU * scale,
                                frameV * scale
                            )
                        }
                        rotation == 90 -> {
                            // Back camera 90° clockwise
                            val sx = (fy - frameY) * scale - frameCropX
                            val sy = frameX * scale - frameCropY
                            listOf(
                                sx,
                                sy,
                                frameV * scale,
                                frameU * scale
                            )
                        }
                        rotation == 270 -> {
                            // Back camera 270° clockwise
                            val sx = frameY * scale - frameCropX
                            val sy = (fx - frameX) * scale - frameCropY
                            listOf(
                                sx,
                                sy,
                                frameV * scale,
                                -frameU * scale
                            )
                        }
                        else -> {
                            // Fallback
                            val sx = frameX * scale - frameCropX
                            val sy = frameY * scale - frameCropY
                            listOf(
                                sx,
                                sy,
                                frameU * scale,
                                frameV * scale
                            )
                        }
                    }

                    val strokeColor: Color
                    val strokeWidth: Float
                    if (magnitude < 1f || magnitude > 6f) {
                        //Color.White.copy(alpha = 0.2f)
                        strokeColor = Color.Transparent
                        strokeWidth = 0f
                    } else if (magnitude < 3f) {
                        strokeColor = Color.White.copy(alpha = 0.6f)
                        //strokeColor = Color.Yellow
                        strokeWidth = 2f
                    } else {
                        strokeColor = Color.White
                        strokeWidth = 4f
                    }
                    drawLine(
                        color = strokeColor,
                        start = Offset(
                            x = screenX,
                            y = screenY
                        ),
                        end = Offset(
                            x = screenX+screenU*6f,
                            y = screenY+screenV*6f
                        ),
                        strokeWidth = strokeWidth
                    )
                    // small arrow head
                    drawCircle(
                        color = strokeColor,
                        radius = strokeWidth,
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