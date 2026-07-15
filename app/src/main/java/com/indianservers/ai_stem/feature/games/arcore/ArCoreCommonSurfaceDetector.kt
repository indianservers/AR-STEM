package com.indianservers.ai_stem.feature.games.arcore

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
import com.indianservers.ai_stem.feature.games.spatial.CommonSurfaceDetectionState
import com.indianservers.ai_stem.feature.games.spatial.CommonSurfaceHitKind
import com.indianservers.ai_stem.feature.games.spatial.CommonSurfaceObservation
import com.indianservers.ai_stem.feature.games.spatial.CommonSurfacePlacementEngine
import com.indianservers.ai_stem.feature.games.spatial.CommonSurfaceTracking
import com.indianservers.ai_stem.feature.games.spatial.LocalPoseDto
import com.indianservers.ai_stem.feature.games.spatial.QuaternionDto
import com.indianservers.ai_stem.feature.games.spatial.SurfaceOrientation
import com.indianservers.ai_stem.feature.games.spatial.Vector3Dto
import kotlin.math.sqrt

class ArCoreCommonSurfaceDetector {
    fun configureMarkerlessSurfaceSession(session: Session, config: Config, instantPreview: Boolean, preferDepth: Boolean): Config {
        config.planeFindingMode = Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL
        config.focusMode = Config.FocusMode.AUTO
        config.updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
        config.lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
        config.depthMode = if (preferDepth && session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
            Config.DepthMode.AUTOMATIC
        } else {
            Config.DepthMode.DISABLED
        }
        config.instantPlacementMode = if (instantPreview) Config.InstantPlacementMode.LOCAL_Y_UP else Config.InstantPlacementMode.DISABLED
        config.cloudAnchorMode = Config.CloudAnchorMode.DISABLED
        return config
    }

    fun observe(frame: Frame, screenX: Float, screenY: Float): CommonSurfaceDetectionState {
        val tracking = frame.camera.trackingState.toCommonTracking()
        val hits = runCatching { frame.hitTest(screenX, screenY) }.getOrDefault(emptyList())
        val hitObservations = hits.mapNotNull { it.toObservation(frame.camera.pose, tracking) }
        val planeObservations = frame.getUpdatedTrackables(Plane::class.java)
            .filter { it.trackingState == TrackingState.TRACKING && it.subsumedBy == null }
            .map {
                CommonSurfaceObservation(
                    hitKind = CommonSurfaceHitKind.Plane,
                    pose = it.centerPose.toLocalPose(),
                    planeWidthMetres = it.extentX,
                    planeHeightMetres = it.extentZ,
                    distanceMetres = distance(frame.camera.pose, it.centerPose),
                    orientation = it.type.toOrientation(),
                    polygonContainsHit = true,
                    tracking = tracking,
                    subsumedByAnotherPlane = it.subsumedBy != null
                )
            }
        return CommonSurfacePlacementEngine.evaluate(hitObservations + planeObservations)
    }

    private fun HitResult.toObservation(cameraPose: Pose, tracking: CommonSurfaceTracking): CommonSurfaceObservation? {
        val trackable = trackable
        return when (trackable) {
            is Plane -> CommonSurfaceObservation(
                hitKind = CommonSurfaceHitKind.Plane,
                pose = hitPose.toLocalPose(),
                planeWidthMetres = trackable.extentX,
                planeHeightMetres = trackable.extentZ,
                distanceMetres = distance(cameraPose, hitPose),
                orientation = trackable.type.toOrientation(),
                polygonContainsHit = trackable.isPoseInPolygon(hitPose),
                tracking = tracking,
                subsumedByAnotherPlane = trackable.subsumedBy != null
            )
            is DepthPoint -> CommonSurfaceObservation(
                hitKind = CommonSurfaceHitKind.DepthPoint,
                pose = hitPose.toLocalPose(),
                distanceMetres = distance(cameraPose, hitPose),
                polygonContainsHit = true,
                tracking = tracking
            )
            is Point -> CommonSurfaceObservation(
                hitKind = CommonSurfaceHitKind.FeaturePoint,
                pose = hitPose.toLocalPose(),
                distanceMetres = distance(cameraPose, hitPose),
                polygonContainsHit = true,
                tracking = tracking
            )
            is InstantPlacementPoint -> CommonSurfaceObservation(
                hitKind = CommonSurfaceHitKind.InstantPreview,
                pose = hitPose.toLocalPose(),
                distanceMetres = distance(cameraPose, hitPose),
                polygonContainsHit = true,
                tracking = tracking
            )
            else -> null
        }
    }

    private fun TrackingState.toCommonTracking(): CommonSurfaceTracking = when (this) {
        TrackingState.TRACKING -> CommonSurfaceTracking.Tracking
        TrackingState.PAUSED -> CommonSurfaceTracking.Paused
        TrackingState.STOPPED -> CommonSurfaceTracking.Stopped
    }

    private fun Plane.Type.toOrientation(): SurfaceOrientation = when (this) {
        Plane.Type.HORIZONTAL_UPWARD_FACING -> SurfaceOrientation.HorizontalUp
        Plane.Type.HORIZONTAL_DOWNWARD_FACING -> SurfaceOrientation.HorizontalDown
        Plane.Type.VERTICAL -> SurfaceOrientation.Vertical
    }

    private fun Pose.toLocalPose(): LocalPoseDto {
        val t = translation
        val q = rotationQuaternion
        return LocalPoseDto(
            translationMetres = Vector3Dto(t[0], t[1], t[2]),
            rotation = QuaternionDto(q[0], q[1], q[2], q[3])
        )
    }

    private fun distance(a: Pose, b: Pose): Float {
        val at = a.translation
        val bt = b.translation
        val dx = at[0] - bt[0]
        val dy = at[1] - bt[1]
        val dz = at[2] - bt[2]
        return sqrt(dx * dx + dy * dy + dz * dz)
    }
}
