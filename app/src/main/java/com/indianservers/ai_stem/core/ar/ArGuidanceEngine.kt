package com.indianservers.ai_stem.core.ar

import com.indianservers.ai_stem.feature.arviewer.ArRecoveryMode
import com.indianservers.ai_stem.feature.arviewer.PlacementHitKind
import com.indianservers.ai_stem.feature.arviewer.PlacementQuality
import com.indianservers.ai_stem.feature.arviewer.TrackingStatus

enum class ArPlacementMode { Auto, Floor, Wall, Table, Depth, Instant, FeaturePoint, OutdoorGeospatial }
enum class ArGuidanceSeverity { Info, Warning, ActionRequired }

data class ArGuidanceInput(
    val cameraTracking: TrackingStatus,
    val anchorTracking: TrackingStatus,
    val recoveryMode: ArRecoveryMode,
    val placementQuality: PlacementQuality,
    val placementHitKind: PlacementHitKind,
    val placementScore: Int,
    val motionStable: Boolean,
    val lightStable: Boolean,
    val shouldPreferPlane: Boolean,
    val hasPlacedObject: Boolean,
    val hasValidPlacementHit: Boolean
)

data class ArGuidanceResult(
    val headline: String,
    val instruction: String,
    val severity: ArGuidanceSeverity,
    val recommendedMode: ArPlacementMode,
    val diagnostics: List<String>,
    val canAutoRecover: Boolean
)

class ArGuidanceEngine {
    fun guide(input: ArGuidanceInput): ArGuidanceResult {
        val diagnostics = buildList {
            add("Camera: ${input.cameraTracking}")
            add("Anchor: ${input.anchorTracking}")
            add("Hit: ${input.placementHitKind}")
            add("Quality: ${input.placementQuality} (${input.placementScore})")
            add(if (input.motionStable) "Motion: steady" else "Motion: too fast")
            add(if (input.lightStable) "Light: usable" else "Light: low")
        }

        if (input.recoveryMode == ArRecoveryMode.RePlacementRequired) {
            return ArGuidanceResult(
                headline = "Re-place the object",
                instruction = "The previous anchor is no longer reliable. Tap Re-place, aim at a textured surface, then tap again.",
                severity = ArGuidanceSeverity.ActionRequired,
                recommendedMode = recommendMode(input),
                diagnostics = diagnostics,
                canAutoRecover = false
            )
        }

        if (input.recoveryMode == ArRecoveryMode.AnchorRecovering) {
            return ArGuidanceResult(
                headline = "Anchor recovering",
                instruction = "Keep the original surface in view. If the object does not return, use Re-place Object.",
                severity = ArGuidanceSeverity.Warning,
                recommendedMode = recommendMode(input),
                diagnostics = diagnostics,
                canAutoRecover = true
            )
        }

        if (!input.lightStable) {
            return ArGuidanceResult(
                headline = "More light needed",
                instruction = "Add light or face a brighter area before placing. ARCore needs visible surface details.",
                severity = ArGuidanceSeverity.Warning,
                recommendedMode = recommendMode(input),
                diagnostics = diagnostics,
                canAutoRecover = true
            )
        }

        if (!input.motionStable) {
            return ArGuidanceResult(
                headline = "Hold steady",
                instruction = "Pause for a moment and move slower. Placement will become stronger when motion settles.",
                severity = ArGuidanceSeverity.Warning,
                recommendedMode = recommendMode(input),
                diagnostics = diagnostics,
                canAutoRecover = true
            )
        }

        if (!input.hasValidPlacementHit) {
            return ArGuidanceResult(
                headline = "Find a target",
                instruction = "Aim at a textured floor, table, wall, corner, or printed surface.",
                severity = ArGuidanceSeverity.Info,
                recommendedMode = ArPlacementMode.Auto,
                diagnostics = diagnostics,
                canAutoRecover = true
            )
        }

        if (input.placementQuality == PlacementQuality.Excellent) {
            return ArGuidanceResult(
                headline = "Excellent placement",
                instruction = "Tap to place. This surface should stay stable while you move around.",
                severity = ArGuidanceSeverity.Info,
                recommendedMode = recommendMode(input),
                diagnostics = diagnostics,
                canAutoRecover = true
            )
        }

        if (input.shouldPreferPlane) {
            return ArGuidanceResult(
                headline = "Approximate placement",
                instruction = "You can tap now, but a detected plane will be steadier. Move slowly across the surface to refine.",
                severity = ArGuidanceSeverity.Info,
                recommendedMode = ArPlacementMode.Instant,
                diagnostics = diagnostics,
                canAutoRecover = true
            )
        }

        return ArGuidanceResult(
            headline = "Ready",
            instruction = "Tap to place, then keep the object area in view for a few seconds.",
            severity = ArGuidanceSeverity.Info,
            recommendedMode = recommendMode(input),
            diagnostics = diagnostics,
            canAutoRecover = true
        )
    }

    private fun recommendMode(input: ArGuidanceInput): ArPlacementMode =
        when (input.placementHitKind) {
            PlacementHitKind.Plane -> ArPlacementMode.Floor
            PlacementHitKind.DepthPoint -> ArPlacementMode.Depth
            PlacementHitKind.FeaturePoint -> ArPlacementMode.FeaturePoint
            PlacementHitKind.Instant -> ArPlacementMode.Instant
            PlacementHitKind.StreetscapeGeometry -> ArPlacementMode.OutdoorGeospatial
            PlacementHitKind.None -> ArPlacementMode.Auto
        }
}
