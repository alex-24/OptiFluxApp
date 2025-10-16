package com.acassion.optifluxapp.native

object NativeOpticalFlowCalculator {

    external fun setParams(
        frameWidth: Int,
        frameHeight: Int,
        cellSize: Int,
        windowSize: Int,
    ): Boolean

    /**
     * Compute the Velocity matrix
     *
     * Returns a flat list repeating this pattern for each pixel [x, y, u, v, mag, ...]
     */
    external fun compute(
        previousImage: ByteArray,
        currentImage: ByteArray,
        previousFrameWidth: Int, // number of pixels in a row ow pixels
        currentFrameWidth: Int,
    ) :  FloatArray
}