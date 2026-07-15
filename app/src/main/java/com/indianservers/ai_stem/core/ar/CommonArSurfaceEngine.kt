package com.indianservers.ai_stem.core.ar

import com.google.ar.core.Config
import com.google.ar.core.DepthPoint
import com.google.ar.core.Frame
import com.google.ar.core.HitResult
import com.google.ar.core.InstantPlacementPoint
import com.google.ar.core.Plane
import com.google.ar.core.Point
import com.google.ar.core.Pose
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import kotlin.math.sqrt

enum class CommonArSurfaceMode { Markerless, SurfaceOnly, AirPlacement, PaperGraph, OutdoorGeospatial }
enum class CommonArSurfaceTracking { Tracking, Paused, Stopped }
enum class CommonArSurfaceHitKind { Plane, DepthPoint, FeaturePoint, InstantPreview, StreetscapeGeometry, None }
enum class CommonArSurfaceOrientation { HorizontalUp, HorizontalDown, Vertical, Unknown }
enum class CommonArSurfaceQuality { Searching, PreviewOnly, Weak, Good, Excellent, Lost }
enum class CommonArHudTone { Neutral, Success, Warning, Error }

data class CommonArSurfacePose(val x: Float, val y: Float, val z: Float)

data class CommonArSurfaceObservation(
    val hitKind: CommonArSurfaceHitKind,
    val pose: CommonArSurfacePose?,
    val planeWidthMetres: Float = 0f,
    val planeHeightMetres: Float = 0f,
    val distanceMetres: Float = Float.MAX_VALUE,
    val orientation: CommonArSurfaceOrientation = CommonArSurfaceOrientation.Unknown,
    val polygonContainsHit: Boolean = false,
    val tracking: CommonArSurfaceTracking = CommonArSurfaceTracking.Tracking,
    val subsumedByAnotherPlane: Boolean = false
)

data class CommonArSurfaceState(
    val quality: CommonArSurfaceQuality,
    val hitKind: CommonArSurfaceHitKind,
    val canPlace: Boolean,
    val compactStatus: String,
    val primaryAction: String,
    val pose: CommonArSurfacePose?,
    val diagnostics: List<String> = emptyList()
)

data class CommonArHudState(
    val title: String,
    val chips: List<String>,
    val primaryAction: String,
    val tone: CommonArHudTone,
    val hiddenDetailCount: Int
)

object CommonArSurfaceEngine {
    fun configureSession(
        session: Session,
        config: Config,
        mode: CommonArSurfaceMode,
        preferDepth: Boolean,
        instantPreview: Boolean
    ): Config {
        config.focusMode = Config.FocusMode.AUTO
        config.updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
        config.lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
        config.planeFindingMode = when (mode) {
            CommonArSurfaceMode.SurfaceOnly,
            CommonArSurfaceMode.Markerless,
            CommonArSurfaceMode.AirPlacement -> Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL
            CommonArSurfaceMode.PaperGraph -> Config.PlaneFindingMode.DISABLED
            CommonArSurfaceMode.OutdoorGeospatial -> Config.PlaneFindingMode.DISABLED
        }
        config.instantPlacementMode = if (instantPreview && mode == CommonArSurfaceMode.AirPlacement) {
            Config.InstantPlacementMode.LOCAL_Y_UP
        } else {
            Config.InstantPlacementMode.DISABLED
        }
        config.depthMode = if (mode != CommonArSurfaceMode.PaperGraph && preferDepth && session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
            Config.DepthMode.AUTOMATIC
        } else {
            Config.DepthMode.DISABLED
        }
        config.cloudAnchorMode = Config.CloudAnchorMode.DISABLED
        return config
    }

    fun evaluate(observations: List<CommonArSurfaceObservation>, mode: CommonArSurfaceMode): CommonArSurfaceState {
        val latestTracking = observations.lastOrNull()?.tracking ?: CommonArSurfaceTracking.Tracking
        if (latestTracking != CommonArSurfaceTracking.Tracking) {
            return CommonArSurfaceState(CommonArSurfaceQuality.Lost, CommonArSurfaceHitKind.None, false, "Tracking lost", "Hold still", null)
        }
        val minPlaneExtent = minPlaneExtent(mode)
        val plane = observations
            .filter { it.hitKind == CommonArSurfaceHitKind.Plane && !it.subsumedByAnotherPlane }
            .filter { it.polygonContainsHit && it.orientation in setOf(CommonArSurfaceOrientation.HorizontalUp, CommonArSurfaceOrientation.Vertical) }
            .filter { it.planeWidthMetres >= minPlaneExtent && it.planeHeightMetres >= minPlaneExtent && it.distanceMetres <= MAX_PLANE_DISTANCE_METRES }
            .minByOrNull { it.distanceMetres }
        if (plane != null) {
            val excellent = plane.planeWidthMetres >= 0.9f && plane.planeHeightMetres >= 0.9f && plane.distanceMetres <= 2.5f
            return CommonArSurfaceState(
                quality = if (excellent) CommonArSurfaceQuality.Excellent else CommonArSurfaceQuality.Good,
                hitKind = CommonArSurfaceHitKind.Plane,
                canPlace = true,
                compactStatus = if (excellent) "Surface locked" else "Surface ready",
                primaryAction = "Tap to place",
                pose = plane.pose,
                diagnostics = listOf("plane=${plane.planeWidthMetres}x${plane.planeHeightMetres}", "distance=${"%.2f".format(plane.distanceMetres)}m")
            )
        }
        val depth = observations.firstOrNull { it.hitKind == CommonArSurfaceHitKind.DepthPoint }
        if (depth != null && mode !in setOf(CommonArSurfaceMode.SurfaceOnly, CommonArSurfaceMode.Markerless)) {
            return CommonArSurfaceState(CommonArSurfaceQuality.Weak, CommonArSurfaceHitKind.DepthPoint, false, "Need plane", "Move slowly", depth.pose)
        }
        val feature = observations.firstOrNull { it.hitKind == CommonArSurfaceHitKind.FeaturePoint }
        if (feature != null && mode == CommonArSurfaceMode.AirPlacement) {
            return CommonArSurfaceState(CommonArSurfaceQuality.Weak, CommonArSurfaceHitKind.FeaturePoint, true, "Air point", "Tap to place", feature.pose)
        }
        val instant = observations.firstOrNull { it.hitKind == CommonArSurfaceHitKind.InstantPreview }
        if (instant != null && mode == CommonArSurfaceMode.AirPlacement) {
            return CommonArSurfaceState(CommonArSurfaceQuality.PreviewOnly, CommonArSurfaceHitKind.InstantPreview, false, "Preview only", "Scan more", instant.pose)
        }
        return CommonArSurfaceState(CommonArSurfaceQuality.Searching, CommonArSurfaceHitKind.None, false, "Scanning", "Find surface", null)
    }

    fun hud(state: CommonArSurfaceState, trackingLabel: String, expanded: Boolean = false): CommonArHudState {
        val tone = when (state.quality) {
            CommonArSurfaceQuality.Excellent, CommonArSurfaceQuality.Good -> CommonArHudTone.Success
            CommonArSurfaceQuality.PreviewOnly, CommonArSurfaceQuality.Weak -> CommonArHudTone.Warning
            CommonArSurfaceQuality.Lost -> CommonArHudTone.Error
            CommonArSurfaceQuality.Searching -> CommonArHudTone.Neutral
        }
        val chips = buildList {
            add(state.compactStatus)
            add(trackingLabel)
            if (expanded) addAll(state.diagnostics.take(3))
        }.distinct().take(if (expanded) 5 else 2)
        return CommonArHudState(
            title = when (tone) {
                CommonArHudTone.Success -> "AR Ready"
                CommonArHudTone.Warning -> "Refine AR"
                CommonArHudTone.Error -> "AR Paused"
                CommonArHudTone.Neutral -> "Scanning"
            },
            chips = chips,
            primaryAction = state.primaryAction,
            tone = tone,
            hiddenDetailCount = (state.diagnostics.size - if (expanded) 3 else 0).coerceAtLeast(0)
        )
    }

    private fun minPlaneExtent(mode: CommonArSurfaceMode): Float = when (mode) {
        CommonArSurfaceMode.Markerless -> 0.18f
        CommonArSurfaceMode.SurfaceOnly -> 0.25f
        CommonArSurfaceMode.PaperGraph -> 0.12f
        CommonArSurfaceMode.AirPlacement -> 0.18f
        CommonArSurfaceMode.OutdoorGeospatial -> 0.45f
    }
    private const val MAX_PLANE_DISTANCE_METRES = 4.0f
}

class CommonArCoreSurfaceDetector {
    fun observe(frame: Frame, screenX: Float, screenY: Float, mode: CommonArSurfaceMode): CommonArSurfaceState {
        if (mode == CommonArSurfaceMode.OutdoorGeospatial) {
            return CommonArSurfaceState(CommonArSurfaceQuality.Searching, CommonArSurfaceHitKind.StreetscapeGeometry, false, "Scan outdoor", "Find mesh", null)
        }
        if (mode == CommonArSurfaceMode.PaperGraph) {
            return CommonArSurfaceState(CommonArSurfaceQuality.Searching, CommonArSurfaceHitKind.None, false, "Scan marker", "Find marker", null)
        }
        val tracking = frame.camera.trackingState.toCommonTracking()
        val hits = runCatching { frame.hitTest(screenX, screenY) }.getOrDefault(emptyList())
        val observations = hits.mapNotNull { it.toObservation(frame.camera.pose, tracking) }
        val instant = if (mode == CommonArSurfaceMode.AirPlacement && frame.camera.trackingState == TrackingState.TRACKING) {
            runCatching {
                frame.hitTestInstantPlacement(screenX, screenY, 0.8f)
                    .mapNotNull { it.toObservation(frame.camera.pose, tracking) }
            }.getOrDefault(emptyList())
        } else {
            emptyList()
        }
        return CommonArSurfaceEngine.evaluate(observations + instant, mode)
    }

    private fun HitResult.toObservation(cameraPose: Pose, tracking: CommonArSurfaceTracking): CommonArSurfaceObservation? {
        val trackable = trackable
        return when (trackable) {
            is Plane -> CommonArSurfaceObservation(
                hitKind = CommonArSurfaceHitKind.Plane,
                pose = hitPose.toCommonPose(),
                planeWidthMetres = trackable.extentX,
                planeHeightMetres = trackable.extentZ,
                distanceMetres = distance(cameraPose, hitPose),
                orientation = trackable.type.toCommonOrientation(),
                polygonContainsHit = trackable.isPoseInPolygon(hitPose),
                tracking = tracking,
                subsumedByAnotherPlane = trackable.subsumedBy != null
            )
            is DepthPoint -> CommonArSurfaceObservation(CommonArSurfaceHitKind.DepthPoint, hitPose.toCommonPose(), distanceMetres = distance(cameraPose, hitPose), polygonContainsHit = true, tracking = tracking)
            is Point -> CommonArSurfaceObservation(CommonArSurfaceHitKind.FeaturePoint, hitPose.toCommonPose(), distanceMetres = distance(cameraPose, hitPose), polygonContainsHit = true, tracking = tracking)
            is InstantPlacementPoint -> CommonArSurfaceObservation(CommonArSurfaceHitKind.InstantPreview, hitPose.toCommonPose(), distanceMetres = distance(cameraPose, hitPose), polygonContainsHit = true, tracking = tracking)
            else -> null
        }
    }

    private fun TrackingState.toCommonTracking(): CommonArSurfaceTracking = when (this) {
        TrackingState.TRACKING -> CommonArSurfaceTracking.Tracking
        TrackingState.PAUSED -> CommonArSurfaceTracking.Paused
        TrackingState.STOPPED -> CommonArSurfaceTracking.Stopped
    }

    private fun Plane.Type.toCommonOrientation(): CommonArSurfaceOrientation = when (this) {
        Plane.Type.HORIZONTAL_UPWARD_FACING -> CommonArSurfaceOrientation.HorizontalUp
        Plane.Type.HORIZONTAL_DOWNWARD_FACING -> CommonArSurfaceOrientation.HorizontalDown
        Plane.Type.VERTICAL -> CommonArSurfaceOrientation.Vertical
    }

    private fun Pose.toCommonPose(): CommonArSurfacePose = CommonArSurfacePose(tx(), ty(), tz())

    private fun distance(a: Pose, b: Pose): Float {
        val dx = a.tx() - b.tx()
        val dy = a.ty() - b.ty()
        val dz = a.tz() - b.tz()
        return sqrt(dx * dx + dy * dy + dz * dz)
    }
}
