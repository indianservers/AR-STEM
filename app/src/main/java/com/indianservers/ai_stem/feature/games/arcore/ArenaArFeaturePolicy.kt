package com.indianservers.ai_stem.feature.games.arcore

import com.google.ar.core.Config
import com.google.ar.core.Session

data class ArenaArOptionalFeatureState(
    val depthEnabled: Boolean,
    val occlusionAvailable: Boolean,
    val environmentalHdrEnabled: Boolean,
    val instantPlacementPreviewEnabled: Boolean,
    val recordingPlaybackSupported: Boolean,
    val fallbackLighting: Boolean
)

class ArenaArFeaturePolicy {
    fun configureSharedOriginSession(
        session: Session,
        config: Config,
        mode: SharedOriginModeName,
        enableInstantPreview: Boolean,
        preferQuality: Boolean
    ): ArenaArOptionalFeatureState {
        config.cloudAnchorMode = Config.CloudAnchorMode.DISABLED
        config.planeFindingMode = Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL
        config.focusMode = Config.FocusMode.AUTO
        val depth = session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)
        config.depthMode = if (depth && preferQuality) Config.DepthMode.AUTOMATIC else Config.DepthMode.DISABLED
        config.lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
        config.instantPlacementMode = if (enableInstantPreview && mode == SharedOriginModeName.SurfaceFallback) {
            Config.InstantPlacementMode.LOCAL_Y_UP
        } else {
            Config.InstantPlacementMode.DISABLED
        }
        return ArenaArOptionalFeatureState(
            depthEnabled = config.depthMode == Config.DepthMode.AUTOMATIC,
            occlusionAvailable = depth,
            environmentalHdrEnabled = true,
            instantPlacementPreviewEnabled = config.instantPlacementMode == Config.InstantPlacementMode.LOCAL_Y_UP,
            recordingPlaybackSupported = runCatching { session.recordingStatus != null && session.playbackStatus != null }.getOrDefault(false),
            fallbackLighting = false
        )
    }
}

enum class SharedOriginModeName { MarkerOrigin, SurfaceFallback }
