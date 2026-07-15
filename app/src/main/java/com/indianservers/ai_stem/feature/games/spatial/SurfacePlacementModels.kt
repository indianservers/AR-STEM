package com.indianservers.ai_stem.feature.games.spatial

enum class SurfacePlacementState { Scanning, PreviewApproximate, ValidPlaneFound, InvalidSurface, Confirmed, LostTracking }
enum class SurfaceOrientation { HorizontalUp, HorizontalDown, Vertical, Unknown }
enum class CommonSurfaceTracking { Tracking, Paused, Stopped }
enum class CommonSurfaceHitKind { Plane, DepthPoint, FeaturePoint, InstantPreview, None }
enum class CommonSurfaceQuality { Searching, PreviewOnly, Weak, Good, Excellent }

data class SurfacePlacementCandidate(
    val pose: LocalPoseDto,
    val planeWidthMetres: Float,
    val planeHeightMetres: Float,
    val distanceMetres: Float,
    val orientation: SurfaceOrientation,
    val polygonContainsHit: Boolean,
    val instantPlacementPreview: Boolean
) {
    val reliable: Boolean
        get() = !instantPlacementPreview &&
            polygonContainsHit &&
            planeWidthMetres >= 0.45f &&
            planeHeightMetres >= 0.45f &&
            distanceMetres <= 4.0f &&
            orientation in setOf(SurfaceOrientation.HorizontalUp, SurfaceOrientation.Vertical)
}

object SurfacePlacementPolicy {
    fun evaluate(candidate: SurfacePlacementCandidate?): Pair<SurfacePlacementState, String> =
        when {
            candidate == null -> SurfacePlacementState.Scanning to "Scan a table, floor, wall, or board."
            candidate.instantPlacementPreview -> SurfacePlacementState.PreviewApproximate to "Preview only. Wait for a real tracked plane before confirming."
            !candidate.polygonContainsHit -> SurfacePlacementState.InvalidSurface to "Aim inside the detected surface boundary."
            candidate.planeWidthMetres < 0.45f || candidate.planeHeightMetres < 0.45f -> SurfacePlacementState.InvalidSurface to "Find a larger flat surface."
            candidate.distanceMetres > 4.0f -> SurfacePlacementState.InvalidSurface to "Move closer before placing the origin."
            candidate.orientation == SurfaceOrientation.Unknown -> SurfacePlacementState.InvalidSurface to "Use a clearer horizontal or vertical surface."
            else -> SurfacePlacementState.ValidPlaneFound to "Surface ready. Confirm orientation before broadcasting."
        }
}

data class CommonSurfaceObservation(
    val hitKind: CommonSurfaceHitKind,
    val pose: LocalPoseDto?,
    val planeWidthMetres: Float = 0f,
    val planeHeightMetres: Float = 0f,
    val distanceMetres: Float = Float.MAX_VALUE,
    val orientation: SurfaceOrientation = SurfaceOrientation.Unknown,
    val polygonContainsHit: Boolean = false,
    val tracking: CommonSurfaceTracking = CommonSurfaceTracking.Tracking,
    val subsumedByAnotherPlane: Boolean = false
)

data class CommonSurfaceDetectionState(
    val quality: CommonSurfaceQuality,
    val placementState: SurfacePlacementState,
    val candidate: SurfacePlacementCandidate?,
    val canPlace: Boolean,
    val compactStatus: String,
    val primaryAction: String,
    val debugFacts: List<String> = emptyList()
)

object CommonSurfacePlacementEngine {
    fun evaluate(observations: List<CommonSurfaceObservation>): CommonSurfaceDetectionState {
        val latestTracking = observations.lastOrNull()?.tracking ?: CommonSurfaceTracking.Tracking
        if (latestTracking != CommonSurfaceTracking.Tracking) {
            return CommonSurfaceDetectionState(
                quality = CommonSurfaceQuality.Searching,
                placementState = SurfacePlacementState.LostTracking,
                candidate = null,
                canPlace = false,
                compactStatus = "Tracking lost",
                primaryAction = "Hold still"
            )
        }

        val bestPlane = observations
            .asSequence()
            .filter { it.hitKind == CommonSurfaceHitKind.Plane && !it.subsumedByAnotherPlane }
            .mapNotNull(::candidateFrom)
            .sortedWith(compareByDescending<SurfacePlacementCandidate> { it.reliable }.thenBy { it.distanceMetres })
            .firstOrNull()
        val planeResult = SurfacePlacementPolicy.evaluate(bestPlane)
        if (bestPlane != null && planeResult.first == SurfacePlacementState.ValidPlaneFound) {
            val quality = if (bestPlane.planeWidthMetres >= 0.9f && bestPlane.planeHeightMetres >= 0.9f && bestPlane.distanceMetres <= 2.5f) {
                CommonSurfaceQuality.Excellent
            } else {
                CommonSurfaceQuality.Good
            }
            return CommonSurfaceDetectionState(
                quality = quality,
                placementState = planeResult.first,
                candidate = bestPlane,
                canPlace = true,
                compactStatus = if (quality == CommonSurfaceQuality.Excellent) "Surface locked" else "Surface ready",
                primaryAction = "Tap to place",
                debugFacts = listOf("plane=${bestPlane.planeWidthMetres}x${bestPlane.planeHeightMetres}", "distance=${bestPlane.distanceMetres}")
            )
        }

        val instant = observations.firstOrNull { it.hitKind == CommonSurfaceHitKind.InstantPreview }
        if (instant != null) {
            return CommonSurfaceDetectionState(
                quality = CommonSurfaceQuality.PreviewOnly,
                placementState = SurfacePlacementState.PreviewApproximate,
                candidate = candidateFrom(instant),
                canPlace = false,
                compactStatus = "Preview only",
                primaryAction = "Scan more"
            )
        }

        val fallback = observations.firstOrNull { it.hitKind == CommonSurfaceHitKind.DepthPoint || it.hitKind == CommonSurfaceHitKind.FeaturePoint }
        if (fallback != null) {
            return CommonSurfaceDetectionState(
                quality = CommonSurfaceQuality.Weak,
                placementState = SurfacePlacementState.Scanning,
                candidate = candidateFrom(fallback),
                canPlace = false,
                compactStatus = "Need plane",
                primaryAction = "Move slowly"
            )
        }

        return CommonSurfaceDetectionState(
            quality = CommonSurfaceQuality.Searching,
            placementState = planeResult.first,
            candidate = bestPlane,
            canPlace = false,
            compactStatus = "Scanning",
            primaryAction = "Find surface"
        )
    }

    private fun candidateFrom(observation: CommonSurfaceObservation): SurfacePlacementCandidate? {
        val pose = observation.pose ?: return null
        return SurfacePlacementCandidate(
            pose = pose,
            planeWidthMetres = observation.planeWidthMetres,
            planeHeightMetres = observation.planeHeightMetres,
            distanceMetres = observation.distanceMetres,
            orientation = observation.orientation,
            polygonContainsHit = observation.polygonContainsHit,
            instantPlacementPreview = observation.hitKind == CommonSurfaceHitKind.InstantPreview
        )
    }
}
