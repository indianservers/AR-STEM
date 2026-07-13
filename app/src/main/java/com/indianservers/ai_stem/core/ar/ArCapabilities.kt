package com.indianservers.ai_stem.core.ar

import android.content.Context
import com.google.ar.core.Config
import com.google.ar.core.Session

data class ArCameraProfile(
    val width: Int,
    val height: Int,
    val fpsRange: String
)

data class ArDeviceCapabilities(
    val arCoreSupported: Boolean,
    val depthSupported: Boolean,
    val rawDepthSupported: Boolean,
    val instantPlacementSupported: Boolean,
    val environmentalHdrSupported: Boolean,
    val geospatialSupported: Boolean,
    val streetscapeGeometrySupported: Boolean,
    val cloudAnchorsSupported: Boolean,
    val augmentedImagesSupported: Boolean,
    val recordingPlaybackSupported: Boolean,
    val supportedCameraConfigs: List<ArCameraProfile>
)

object ArCapabilityInspector {
    fun inspect(context: Context, session: Session?): ArDeviceCapabilities {
        if (session == null) {
            return ArDeviceCapabilities(
                arCoreSupported = false,
                depthSupported = false,
                rawDepthSupported = false,
                instantPlacementSupported = false,
                environmentalHdrSupported = false,
                geospatialSupported = false,
                streetscapeGeometrySupported = false,
                cloudAnchorsSupported = false,
                augmentedImagesSupported = false,
                recordingPlaybackSupported = false,
                supportedCameraConfigs = emptyList()
            )
        }
        val configs = runCatching {
            session.supportedCameraConfigs.map {
                ArCameraProfile(
                    width = it.imageSize.width,
                    height = it.imageSize.height,
                    fpsRange = it.fpsRange.toString()
                )
            }
        }.getOrDefault(emptyList())
        return ArDeviceCapabilities(
            arCoreSupported = true,
            depthSupported = session.isDepthModeSupported(Config.DepthMode.AUTOMATIC),
            rawDepthSupported = session.isDepthModeSupported(Config.DepthMode.RAW_DEPTH_ONLY),
            instantPlacementSupported = true,
            environmentalHdrSupported = true,
            geospatialSupported = runCatching { session.isGeospatialModeSupported(Config.GeospatialMode.ENABLED) }.getOrDefault(false),
            streetscapeGeometrySupported = runCatching { session.isGeospatialModeSupported(Config.GeospatialMode.ENABLED) }.getOrDefault(false),
            cloudAnchorsSupported = false,
            augmentedImagesSupported = true,
            recordingPlaybackSupported = true,
            supportedCameraConfigs = configs
        )
    }
}
