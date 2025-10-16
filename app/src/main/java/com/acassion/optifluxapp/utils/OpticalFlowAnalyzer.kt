package com.acassion.optifluxapp.utils

import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.acassion.optifluxapp.native.NativeOpticalFlowCalculator

class OpticalFlowAnalyzer(
    private val onFlow: (opticalFlow:FloatArray, frameWidth: Int, frameHeight: Int, rotation: Int, durationNano: Long) -> Unit
): ImageAnalysis.Analyzer {

    val TAG = "OpticalFlowAnalyzer"

    var previousImage: ByteArray? = null
    var currentImage: ByteArray? = null
    private var frameCount = 0
    private var imageRotation = 0
    private var paramsSet = false

    override fun analyze(image: ImageProxy) {
        
        if (frameCount == 0) {
            imageRotation = image.imageInfo.rotationDegrees
            currentImage = ByteArray(image.width * image.height)
            
            Log.d(TAG, "image received: ${image.width}x${image.height}, rotation: ${imageRotation}°")
            
            NativeOpticalFlowCalculator.setParams(
                frameWidth = image.width,
                frameHeight = image.height,
                cellSize = Constants.cellSize,
                windowSize = Constants.windowSize
            )
            paramsSet = true
        }
        
        frameCount++

        val yPlane = image.planes[0]
        val yBuffer = yPlane.buffer
        val yRowStride = yPlane.rowStride
        val yPixelStride = yPlane.pixelStride
        
        yBuffer.rewind()
        if (yPixelStride == 1 && yRowStride == image.width) {
            yBuffer.get(currentImage!!)
        } else {
            for (row in 0 until image.height) {
                yBuffer.position(row * yRowStride)
                for (col in 0 until image.width) {
                    currentImage!![row * image.width + col] = yBuffer.get()
                    if (yPixelStride > 1) {
                        yBuffer.position(yBuffer.position() + yPixelStride - 1)
                    }
                }
            }
        }

        val startTime = System.nanoTime()
        val opticalFlow: FloatArray = when(previousImage != null) {
            true -> NativeOpticalFlowCalculator.compute(
                previousImage = previousImage!!,
                currentImage = currentImage!!,
                previousFrameWidth = image.width,
                currentFrameWidth = image.width
            )
            false -> FloatArray(0)
        }
        val endTime = System.nanoTime()

        if (frameCount % 30 == 0) {
            Log.d(TAG, "Frame $frameCount: computed ${opticalFlow.size/5} vectors in ${(endTime-startTime)/1000000}ms")
        }

        if (opticalFlow.isNotEmpty()) {
            onFlow(
                opticalFlow,
                image.width,
                image.height,
                imageRotation,
                endTime - startTime
            )
        }

        if (previousImage == null) {
            previousImage = ByteArray(currentImage!!.size)
        }

        System.arraycopy(
            currentImage!!,
            0,
            previousImage!!,
            0,
            currentImage!!.size
        )
        image.close()
    }
}