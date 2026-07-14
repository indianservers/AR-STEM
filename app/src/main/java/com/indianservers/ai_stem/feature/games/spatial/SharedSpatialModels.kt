package com.indianservers.ai_stem.feature.games.spatial

import kotlin.math.acos
import kotlin.math.max
import kotlin.math.sqrt

const val SHARED_COORDINATE_VERSION = 1
const val AR_MATH_ARENA_MARKER_NAME = "AR Math Arena Origin"
const val AR_MATH_ARENA_MARKER_WIDTH_METRES = 0.18f

data class Vector3Dto(
    val x: Float,
    val y: Float,
    val z: Float
) {
    fun isFinite(): Boolean = x.isFinite() && y.isFinite() && z.isFinite()
    fun magnitude(): Float = sqrt(x * x + y * y + z * z)
    operator fun minus(other: Vector3Dto): Vector3Dto = Vector3Dto(x - other.x, y - other.y, z - other.z)
    operator fun plus(other: Vector3Dto): Vector3Dto = Vector3Dto(x + other.x, y + other.y, z + other.z)
    operator fun times(value: Float): Vector3Dto = Vector3Dto(x * value, y * value, z * value)
}

data class QuaternionDto(
    val x: Float,
    val y: Float,
    val z: Float,
    val w: Float
) {
    fun isFinite(): Boolean = x.isFinite() && y.isFinite() && z.isFinite() && w.isFinite()
}

data class SharedTransform(
    val coordinateVersion: Int = SHARED_COORDINATE_VERSION,
    val originVersion: Long = 0,
    val positionMetres: Vector3Dto,
    val rotation: QuaternionDto = QuaternionDto(0f, 0f, 0f, 1f),
    val scale: Vector3Dto = Vector3Dto(1f, 1f, 1f)
)

data class LocalPoseDto(
    val translationMetres: Vector3Dto,
    val rotation: QuaternionDto
)

enum class SharedOriginMode { PrintedMarkerOrigin, HostSurfacePlacement }
enum class SharedOriginState { Undefined, Searching, Stabilizing, Ready, Weak, Lost, RecalibrationRequired }

data class SharedOriginDefinition(
    val originId: String,
    val originVersion: Long,
    val mode: SharedOriginMode,
    val coordinateVersion: Int = SHARED_COORDINATE_VERSION,
    val markerName: String? = AR_MATH_ARENA_MARKER_NAME,
    val markerWidthMetres: Float? = AR_MATH_ARENA_MARKER_WIDTH_METRES,
    val hostTransform: SharedTransform,
    val createdByPlayerId: String,
    val createdAtEpochMs: Long = System.currentTimeMillis()
)

data class SpatialSnapshot(
    val originVersion: Long,
    val transforms: List<SharedObjectTransform>
)

data class SharedObjectTransform(
    val objectId: String,
    val transform: SharedTransform,
    val sequence: Long,
    val updatedAtEpochMs: Long = System.currentTimeMillis()
)

object SharedTransformMath {
    fun validate(transform: SharedTransform): Result<Unit> = runCatching {
        require(transform.coordinateVersion == SHARED_COORDINATE_VERSION) { "Unsupported coordinate version." }
        require(transform.positionMetres.isFinite()) { "Position must be finite." }
        require(transform.rotation.isFinite()) { "Rotation must be finite." }
        require(transform.scale.isFinite()) { "Scale must be finite." }
        require(transform.scale.x in 0.001f..100f && transform.scale.y in 0.001f..100f && transform.scale.z in 0.001f..100f) {
            "Scale is outside safe bounds."
        }
        normalize(transform.rotation)
    }

    fun normalize(q: QuaternionDto): QuaternionDto {
        require(q.isFinite()) { "Quaternion contains invalid values." }
        val length = sqrt(q.x * q.x + q.y * q.y + q.z * q.z + q.w * q.w)
        require(length > 0.00001f) { "Quaternion length is too small." }
        return QuaternionDto(q.x / length, q.y / length, q.z / length, q.w / length)
    }

    fun localToMarkerRelative(localPose: LocalPoseDto, markerPose: LocalPoseDto, originVersion: Long): SharedTransform {
        val inverseOrigin = invert(markerPose)
        val relative = multiply(inverseOrigin, localPose)
        return SharedTransform(
            originVersion = originVersion,
            positionMetres = relative.translationMetres,
            rotation = normalize(relative.rotation)
        )
    }

    fun markerRelativeToLocal(transform: SharedTransform, markerPose: LocalPoseDto): LocalPoseDto {
        validate(transform).getOrThrow()
        val relative = LocalPoseDto(transform.positionMetres, normalize(transform.rotation))
        return multiply(markerPose, relative)
    }

    fun interpolate(from: SharedTransform, to: SharedTransform, t: Float): SharedTransform {
        require(from.coordinateVersion == to.coordinateVersion) { "Coordinate version mismatch." }
        require(from.originVersion == to.originVersion) { "Origin version mismatch." }
        val clamped = t.coerceIn(0f, 1f)
        return SharedTransform(
            coordinateVersion = from.coordinateVersion,
            originVersion = from.originVersion,
            positionMetres = lerp(from.positionMetres, to.positionMetres, clamped),
            rotation = slerp(normalize(from.rotation), normalize(to.rotation), clamped),
            scale = lerp(from.scale, to.scale, clamped)
        )
    }

    fun reconcile(local: SharedObjectTransform, authoritative: SharedObjectTransform): SharedObjectTransform {
        require(local.objectId == authoritative.objectId) { "Cannot reconcile different objects." }
        require(local.transform.originVersion == authoritative.transform.originVersion) { "Origin version mismatch." }
        if (authoritative.sequence <= local.sequence) return local
        return authoritative.copy(
            transform = interpolate(local.transform, authoritative.transform, 0.35f)
        )
    }

    private fun invert(pose: LocalPoseDto): LocalPoseDto {
        val invRotation = conjugate(normalize(pose.rotation))
        val invTranslation = rotate(Vector3Dto(-pose.translationMetres.x, -pose.translationMetres.y, -pose.translationMetres.z), invRotation)
        return LocalPoseDto(invTranslation, invRotation)
    }

    private fun multiply(a: LocalPoseDto, b: LocalPoseDto): LocalPoseDto =
        LocalPoseDto(
            translationMetres = a.translationMetres + rotate(b.translationMetres, a.rotation),
            rotation = normalize(multiply(a.rotation, b.rotation))
        )

    private fun rotate(v: Vector3Dto, qRaw: QuaternionDto): Vector3Dto {
        val q = normalize(qRaw)
        val u = Vector3Dto(q.x, q.y, q.z)
        val s = q.w
        val dot = u.x * v.x + u.y * v.y + u.z * v.z
        val cross = Vector3Dto(
            u.y * v.z - u.z * v.y,
            u.z * v.x - u.x * v.z,
            u.x * v.y - u.y * v.x
        )
        return u * (2f * dot) + v * (s * s - (u.x * u.x + u.y * u.y + u.z * u.z)) + cross * (2f * s)
    }

    private fun multiply(a: QuaternionDto, b: QuaternionDto): QuaternionDto =
        QuaternionDto(
            w = a.w * b.w - a.x * b.x - a.y * b.y - a.z * b.z,
            x = a.w * b.x + a.x * b.w + a.y * b.z - a.z * b.y,
            y = a.w * b.y - a.x * b.z + a.y * b.w + a.z * b.x,
            z = a.w * b.z + a.x * b.y - a.y * b.x + a.z * b.w
        )

    private fun conjugate(q: QuaternionDto): QuaternionDto = QuaternionDto(-q.x, -q.y, -q.z, q.w)
    private fun lerp(a: Vector3Dto, b: Vector3Dto, t: Float): Vector3Dto = a * (1f - t) + b * t

    private fun slerp(aRaw: QuaternionDto, bRaw: QuaternionDto, t: Float): QuaternionDto {
        var b = bRaw
        var dot = aRaw.x * b.x + aRaw.y * b.y + aRaw.z * b.z + aRaw.w * b.w
        if (dot < 0f) {
            dot = -dot
            b = QuaternionDto(-b.x, -b.y, -b.z, -b.w)
        }
        if (dot > 0.9995f) {
            return normalize(
                QuaternionDto(
                    x = aRaw.x + t * (b.x - aRaw.x),
                    y = aRaw.y + t * (b.y - aRaw.y),
                    z = aRaw.z + t * (b.z - aRaw.z),
                    w = aRaw.w + t * (b.w - aRaw.w)
                )
            )
        }
        val theta0 = acos(dot.coerceIn(-1f, 1f))
        val theta = theta0 * t
        val sinTheta = kotlin.math.sin(theta)
        val sinTheta0 = max(kotlin.math.sin(theta0), 0.00001f)
        val s0 = kotlin.math.cos(theta) - dot * sinTheta / sinTheta0
        val s1 = sinTheta / sinTheta0
        return normalize(
            QuaternionDto(
                x = s0 * aRaw.x + s1 * b.x,
                y = s0 * aRaw.y + s1 * b.y,
                z = s0 * aRaw.z + s1 * b.z,
                w = s0 * aRaw.w + s1 * b.w
            )
        )
    }
}
