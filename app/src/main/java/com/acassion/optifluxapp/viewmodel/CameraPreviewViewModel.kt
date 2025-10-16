package com.acassion.optifluxapp.viewmodel

import android.content.Context
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import com.acassion.optifluxapp.model.OpticalFlowModel
import com.acassion.optifluxapp.native.NativeOpticalFlowCalculator
import com.acassion.optifluxapp.utils.OpticalFlowAnalyzer
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.Executors
import kotlin.time.Duration.Companion.milliseconds

class CameraPreviewViewModel : ViewModel() {
    val TAG = "CameraPreviewViewModel"

    private val cameraExecutor = Executors.newSingleThreadExecutor()

    private val _surfaceRequest = MutableStateFlow<SurfaceRequest?>(null)
    val surfaceRequest = _surfaceRequest.asStateFlow()
    private val _opticalFlowModel = MutableStateFlow<OpticalFlowModel>(OpticalFlowModel())
    val opticalFlow = _opticalFlowModel.asStateFlow()
    
    // Camera selector state - false for back camera by default
    private val _isFrontCamera = MutableStateFlow(false)
    val isFrontCamera = _isFrontCamera.asStateFlow()

    private val cameraPreviewUseCase = Preview.Builder().build().apply {
        setSurfaceProvider { newSurfaceRequest ->
            _surfaceRequest.update { newSurfaceRequest }
        }
    }

    private var lastTimeStep = System.nanoTime()
    private var frames = 0

    private val cameraAnalysisUseCase = ImageAnalysis
        .Builder()
        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
        .build().apply {
            setAnalyzer(
                cameraExecutor,
                OpticalFlowAnalyzer(
                    onFlow = { opticalFlow: FloatArray, frameWidth: Int, frameHeight: Int, rotation: Int, durationNano: Long ->
                        frames++
                        val currentTime = System.nanoTime()

                        if (currentTime - lastTimeStep > 100.milliseconds.inWholeNanoseconds) {
                            _opticalFlowModel.update {
                                it.copy(
                                    vectors = opticalFlow,
                                    frameWidth = frameWidth,
                                    frameHeight = frameHeight,
                                    rotation = rotation,
                                    isFrontCamera = _isFrontCamera.value
                                )
                            }
                            lastTimeStep = currentTime
                            
                            if (frames % 30 == 0) {
                                Log.d(TAG, "Updated optical flow: ${opticalFlow.size/5} vectors, rotation: $rotation°, front camera: ${_isFrontCamera.value}")
                            }
                        }
                    }

                )
            )
        }

    suspend fun bindToCamera(applicationContext: Context, lifecycleOwner: LifecycleOwner) {
        val processCameraProvider = ProcessCameraProvider.awaitInstance(applicationContext)
        
        // Select camera based on current state
        val cameraSelector = if (_isFrontCamera.value) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }
        
        processCameraProvider.bindToLifecycle(
            lifecycleOwner = lifecycleOwner,
            cameraSelector = cameraSelector,
            cameraPreviewUseCase, cameraAnalysisUseCase
        )
        _setNativeFlowCalculatorParams()

        try {
            awaitCancellation()
        } finally {
            processCameraProvider.unbindAll()
        }
    }

    fun updateCellSize(cellSize: Int) {
        _opticalFlowModel.update { it.copy(cellSize = cellSize) }
        _setNativeFlowCalculatorParams()
    }

    fun updateWindowSize(windowSize: Int) {
        _opticalFlowModel.update { it.copy(windowSize = windowSize) }
        _setNativeFlowCalculatorParams()
    }
    
    fun toggleCamera() {
        _isFrontCamera.update { !it }
        Log.d(TAG, "Camera orientation: ${if (_isFrontCamera.value) "FRONT" else "BACK"}")
    }

    private fun _setNativeFlowCalculatorParams() {
        val opticalFlowModel: OpticalFlowModel = _opticalFlowModel.value
        NativeOpticalFlowCalculator.setParams(
            frameWidth = opticalFlowModel.frameWidth,
            frameHeight = opticalFlowModel.frameHeight,
            cellSize = opticalFlowModel.cellSize,
            windowSize = opticalFlowModel.windowSize
        )
    }
}

