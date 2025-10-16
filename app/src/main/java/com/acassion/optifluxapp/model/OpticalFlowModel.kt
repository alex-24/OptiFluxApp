package com.acassion.optifluxapp.model

import com.acassion.optifluxapp.utils.Constants

data class OpticalFlowModel(
    val vectors: FloatArray = FloatArray(0),
    val frameWidth: Int = Constants.frameWidth,
    val frameHeight: Int = Constants.frameHeight,
    val cellSize: Int = Constants.cellSize,
    val windowSize: Int = Constants.windowSize,
    val fps: Int = Constants.fps,
    val rotation: Int = 0,
    val isFrontCamera: Boolean = false,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as OpticalFlowModel

        if (frameWidth != other.frameWidth) return false
        if (frameHeight != other.frameHeight) return false
        if (cellSize != other.cellSize) return false
        if (windowSize != other.windowSize) return false
        if (fps != other.fps) return false
        if (rotation != other.rotation) return false
        if (isFrontCamera != other.isFrontCamera) return false
        if (!vectors.contentEquals(other.vectors)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = frameWidth
        result = 31 * result + frameHeight
        result = 31 * result + cellSize
        result = 31 * result + windowSize
        result = 31 * result + fps
        result = 31 * result + rotation
        result = 31 * result + isFrontCamera.hashCode()
        result = 31 * result + vectors.contentHashCode()
        return result
    }
}
