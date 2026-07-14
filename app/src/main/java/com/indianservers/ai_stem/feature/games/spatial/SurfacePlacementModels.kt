package com.indianservers.ai_stem.feature.games.spatial

enum class SurfacePlacementState { Scanning, PreviewApproximate, ValidPlaneFound, InvalidSurface, Confirmed, LostTracking }
enum class SurfaceOrientation { HorizontalUp, HorizontalDown, Vertical, Unknown }

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
