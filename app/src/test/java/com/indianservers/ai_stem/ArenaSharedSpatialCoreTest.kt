package com.indianservers.ai_stem

import com.indianservers.ai_stem.feature.games.multiplayer.ArenaProtocolCodec
import com.indianservers.ai_stem.feature.games.multiplayer.GameMessage
import com.indianservers.ai_stem.feature.games.multiplayer.GamePayload
import com.indianservers.ai_stem.feature.games.spatial.ArCameraTrackingQuality
import com.indianservers.ai_stem.feature.games.spatial.CalibrationReadinessEvaluator
import com.indianservers.ai_stem.feature.games.spatial.CalibrationSample
import com.indianservers.ai_stem.feature.games.spatial.CalibrationState
import com.indianservers.ai_stem.feature.games.spatial.LocalPoseDto
import com.indianservers.ai_stem.feature.games.spatial.MarkerTrackingQuality
import com.indianservers.ai_stem.feature.games.spatial.PlayerCalibrationStatus
import com.indianservers.ai_stem.feature.games.spatial.QuaternionDto
import com.indianservers.ai_stem.feature.games.spatial.SHARED_COORDINATE_VERSION
import com.indianservers.ai_stem.feature.games.spatial.SharedAnchorRecord
import com.indianservers.ai_stem.feature.games.spatial.SharedAnchorRegistry
import com.indianservers.ai_stem.feature.games.spatial.SharedAnchorType
import com.indianservers.ai_stem.feature.games.spatial.SharedObjectTransform
import com.indianservers.ai_stem.feature.games.spatial.SharedOriginDefinition
import com.indianservers.ai_stem.feature.games.spatial.SharedOriginMode
import com.indianservers.ai_stem.feature.games.spatial.SharedTransform
import com.indianservers.ai_stem.feature.games.spatial.SharedTransformMath
import com.indianservers.ai_stem.feature.games.spatial.SpatialCommand
import com.indianservers.ai_stem.feature.games.spatial.SpatialSessionReducer
import com.indianservers.ai_stem.feature.games.spatial.SpatialSessionState
import com.indianservers.ai_stem.feature.games.spatial.SurfaceOrientation
import com.indianservers.ai_stem.feature.games.spatial.SurfacePlacementCandidate
import com.indianservers.ai_stem.feature.games.spatial.SurfacePlacementPolicy
import com.indianservers.ai_stem.feature.games.spatial.SurfacePlacementState
import com.indianservers.ai_stem.feature.games.spatial.Vector3Dto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ArenaSharedSpatialCoreTest {
    @Test
    fun transformConversionRoundTripsThroughMarkerOrigin() {
        val marker = LocalPoseDto(Vector3Dto(1f, 0f, 2f), QuaternionDto(0f, 0f, 0f, 1f))
        val local = LocalPoseDto(Vector3Dto(1.5f, 0.2f, 2.3f), QuaternionDto(0f, 0f, 0f, 1f))

        val shared = SharedTransformMath.localToMarkerRelative(local, marker, originVersion = 3)
        val restored = SharedTransformMath.markerRelativeToLocal(shared, marker)

        assertEquals(3, shared.originVersion)
        assertClose(0.5f, shared.positionMetres.x)
        assertClose(0.2f, shared.positionMetres.y)
        assertClose(0.3f, shared.positionMetres.z)
        assertClose(local.translationMetres.x, restored.translationMetres.x)
        assertClose(local.translationMetres.z, restored.translationMetres.z)
    }

    @Test
    fun quaternionNormalizesAndInvalidTransformRejects() {
        val normalized = SharedTransformMath.normalize(QuaternionDto(0f, 0f, 0f, 2f))
        assertEquals(1f, normalized.w, 0.0001f)

        val invalid = SharedTransform(
            coordinateVersion = SHARED_COORDINATE_VERSION,
            positionMetres = Vector3Dto(Float.NaN, 0f, 0f)
        )
        assertTrue(SharedTransformMath.validate(invalid).isFailure)
    }

    @Test
    fun interpolationRejectsOriginVersionMismatchAndReconcilesNewerSnapshots() {
        val local = SharedObjectTransform("obj", transform(1, 0f), sequence = 1)
        val remote = SharedObjectTransform("obj", transform(1, 2f), sequence = 2)
        val reconciled = SharedTransformMath.reconcile(local, remote)

        assertEquals(2, reconciled.sequence)
        assertTrue(reconciled.transform.positionMetres.x > 0f)
        assertTrue(
            runCatching { SharedTransformMath.interpolate(transform(1, 0f), transform(2, 1f), 0.5f) }.isFailure
        )
    }

    @Test
    fun calibrationReadinessRequiresStableFullTracking() {
        val evaluator = CalibrationReadinessEvaluator(requiredSamples = 4)
        val unstable = evaluator.evaluate(listOf(sample(MarkerTrackingQuality.Detected, 0, 0.1f)))
        assertFalse(unstable.canAccept)

        val ready = evaluator.evaluate((0 until 4).map { sample(MarkerTrackingQuality.FullTracking, it, 0.001f) })
        assertEquals(CalibrationState.Ready, ready.state)
        assertTrue(ready.canAccept)
    }

    @Test
    fun anchorRegistryCleansUpOriginChangesAndSessionClose() {
        val registry = SharedAnchorRegistry<String>()
        registry.upsert(anchor("a", 1), localAnchor = "local-a")
        registry.upsert(anchor("b", 2), localAnchor = "local-b")

        val removed = registry.cleanupForOriginVersion(2)
        assertEquals(listOf("a"), removed.map { it.anchorId })
        assertEquals(null, registry.localAnchor("a"))

        val all = registry.cleanupAll()
        assertTrue(all.any { it.anchorId == "b" })
        assertTrue(registry.records().none { it.active })
    }

    @Test
    fun instantPlacementPreviewCannotFinalizeSurfaceOrigin() {
        val preview = SurfacePlacementCandidate(
            pose = LocalPoseDto(Vector3Dto(0f, 0f, 0f), QuaternionDto(0f, 0f, 0f, 1f)),
            planeWidthMetres = 2f,
            planeHeightMetres = 2f,
            distanceMetres = 1f,
            orientation = SurfaceOrientation.HorizontalUp,
            polygonContainsHit = true,
            instantPlacementPreview = true
        )
        val result = SurfacePlacementPolicy.evaluate(preview)
        assertEquals(SurfacePlacementState.PreviewApproximate, result.first)
        assertFalse(preview.reliable)
    }

    @Test
    fun spatialProtocolRoundTripsSharedOriginAndCalibrationStatus() {
        val definition = SharedOriginDefinition(
            originId = "origin-1",
            originVersion = 4,
            mode = SharedOriginMode.PrintedMarkerOrigin,
            hostTransform = transform(4, 0f),
            createdByPlayerId = "host"
        )
        val message = GameMessage("1".toInt(), "m", "room", "host", 3, payload = GamePayload.SharedOriginDefined(definition))
        val decoded = ArenaProtocolCodec.decode(ArenaProtocolCodec.encode(message)).getOrThrow()

        assertEquals(GamePayload.SharedOriginDefined::class, decoded.payload::class)
        assertEquals(4, (decoded.payload as GamePayload.SharedOriginDefined).definition.originVersion)

        val statusMessage = GameMessage(
            roomId = "room",
            senderPlayerId = "p2",
            sequence = 4,
            payload = GamePayload.PlayerCalibrationState(PlayerCalibrationStatus("p2", "Dev", CalibrationState.Ready, 96, "Ready", 4))
        )
        assertEquals(GamePayload.PlayerCalibrationState::class, ArenaProtocolCodec.decode(ArenaProtocolCodec.encode(statusMessage)).getOrThrow().payload::class)
    }

    @Test
    fun spatialReducerEnforcesHostAuthority() {
        val state = SpatialSessionState(hostPlayerId = "host")
        val rejected = SpatialSessionReducer.reduce(state, SpatialCommand.StartCalibration("guest", SharedOriginMode.PrintedMarkerOrigin))
        assertFalse(rejected.accepted)

        val started = SpatialSessionReducer.reduce(state, SpatialCommand.StartCalibration("host", SharedOriginMode.PrintedMarkerOrigin)).state
        val defined = SpatialSessionReducer.reduce(
            started,
            SpatialCommand.DefineOrigin(
                "host",
                SharedOriginDefinition("origin", started.originVersion, SharedOriginMode.PrintedMarkerOrigin, hostTransform = transform(started.originVersion, 0f), createdByPlayerId = "host")
            )
        )
        assertTrue(defined.accepted)
    }

    private fun sample(markerTracking: MarkerTrackingQuality, index: Int, jitter: Float): CalibrationSample =
        CalibrationSample(
            cameraTracking = ArCameraTrackingQuality.Tracking,
            markerTracking = markerTracking,
            markerExtentXMetres = 0.18f + jitter,
            markerExtentZMetres = 0.18f + jitter,
            markerPose = LocalPoseDto(Vector3Dto(jitter * index, 0f, 0.8f), QuaternionDto(0f, 0f, 0f, 1f)),
            distanceFromMarkerMetres = 0.8f,
            viewingAngleDegrees = 12f,
            lightEstimate = 0.8f,
            deviceMotionMetresPerSecond = 0.02f,
            timestampMs = index.toLong()
        )

    private fun transform(originVersion: Long, x: Float): SharedTransform =
        SharedTransform(originVersion = originVersion, positionMetres = Vector3Dto(x, 0f, 0f))

    private fun anchor(id: String, originVersion: Long): SharedAnchorRecord =
        SharedAnchorRecord(
            anchorId = id,
            sharedTransform = transform(originVersion, 0f),
            anchorType = SharedAnchorType.GameObject,
            ownerPlayerId = "host",
            creationSequence = originVersion,
            originVersion = originVersion
        )

    private fun assertClose(expected: Float, actual: Float) {
        assertEquals(expected, actual, 0.0001f)
    }
}
