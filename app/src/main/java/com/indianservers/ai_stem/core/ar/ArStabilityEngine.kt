package com.indianservers.ai_stem.core.ar

import com.indianservers.ai_stem.feature.arviewer.ArRecoveryMode
import com.indianservers.ai_stem.feature.arviewer.PlacementHitKind
import com.indianservers.ai_stem.feature.arviewer.PlacementQuality
import com.indianservers.ai_stem.feature.arviewer.TrackingStatus

data class ArSensorState(
    val cameraTracking: TrackingStatus,
    val anchorTracking: TrackingStatus,
    val placementHitKind: PlacementHitKind,
    val hasPlacedObject: Boolean,
    val trackingMessage: String
)

data class ArStabilityState(
    val recoveryMode: ArRecoveryMode,
    val placementQuality: PlacementQuality,
    val canPlace: Boolean,
    val canRePlace: Boolean,
    val guidance: String
)

class ArStabilityEngine {
    fun evaluate(sensor: ArSensorState): ArStabilityState {
        if (sensor.cameraTracking == TrackingStatus.Paused) {
            return ArStabilityState(
                recoveryMode = ArRecoveryMode.RePlacementRequired,
                placementQuality = PlacementQuality.Lost,
                canPlace = false,
                canRePlace = sensor.hasPlacedObject,
                guidance = sensor.trackingMessage
            )
        }

        if (sensor.hasPlacedObject && sensor.anchorTracking == TrackingStatus.Paused) {
            return ArStabilityState(
                recoveryMode = ArRecoveryMode.RePlacementRequired,
                placementQuality = PlacementQuality.Lost,
                canPlace = false,
                canRePlace = true,
                guidance = "Object anchor was lost. Tap Re-place, then choose a visible surface."
            )
        }

        if (sensor.hasPlacedObject && sensor.anchorTracking == TrackingStatus.Limited) {
            return ArStabilityState(
                recoveryMode = ArRecoveryMode.AnchorRecovering,
                placementQuality = PlacementQuality.Recovering,
                canPlace = false,
                canRePlace = true,
                guidance = "Object anchor is recovering. Look back at the placed area and move slowly; use Re-place only if it does not return."
            )
        }

        if (sensor.cameraTracking == TrackingStatus.Limited) {
            return ArStabilityState(
                recoveryMode = ArRecoveryMode.TrackingLimited,
                placementQuality = PlacementQuality.Weak,
                canPlace = sensor.placementHitKind != PlacementHitKind.None,
                canRePlace = sensor.hasPlacedObject,
                guidance = sensor.trackingMessage
            )
        }

        return when (sensor.placementHitKind) {
            PlacementHitKind.StreetscapeGeometry -> ArStabilityState(
                recoveryMode = ArRecoveryMode.Normal,
                placementQuality = PlacementQuality.Excellent,
                canPlace = true,
                canRePlace = sensor.hasPlacedObject,
                guidance = "Outdoor building or terrain mesh tracking is strong."
            )
            PlacementHitKind.Plane -> ArStabilityState(
                recoveryMode = ArRecoveryMode.Normal,
                placementQuality = PlacementQuality.Excellent,
                canPlace = true,
                canRePlace = sensor.hasPlacedObject,
                guidance = "Surface tracking is strong."
            )
            PlacementHitKind.DepthPoint -> ArStabilityState(
                recoveryMode = ArRecoveryMode.Normal,
                placementQuality = PlacementQuality.Good,
                canPlace = true,
                canRePlace = sensor.hasPlacedObject,
                guidance = "Depth tracking is available on this real-world surface."
            )
            PlacementHitKind.FeaturePoint -> ArStabilityState(
                recoveryMode = ArRecoveryMode.Normal,
                placementQuality = PlacementQuality.Good,
                canPlace = true,
                canRePlace = sensor.hasPlacedObject,
                guidance = "Feature tracking is available."
            )
            PlacementHitKind.Instant -> ArStabilityState(
                recoveryMode = ArRecoveryMode.Normal,
                placementQuality = PlacementQuality.Weak,
                canPlace = true,
                canRePlace = sensor.hasPlacedObject,
                guidance = "Approximate placement is available. Move slowly to refine it."
            )
            PlacementHitKind.None -> ArStabilityState(
                recoveryMode = ArRecoveryMode.Normal,
                placementQuality = PlacementQuality.Unknown,
                canPlace = false,
                canRePlace = sensor.hasPlacedObject,
                guidance = sensor.trackingMessage
            )
        }
    }
}
