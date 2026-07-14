package com.indianservers.ai_stem.feature.games.multiplayer

import com.indianservers.ai_stem.feature.games.spatial.CalibrationState
import com.indianservers.ai_stem.feature.games.spatial.PlayerCalibrationStatus
import com.indianservers.ai_stem.feature.games.spatial.QuaternionDto
import com.indianservers.ai_stem.feature.games.spatial.SHARED_COORDINATE_VERSION
import com.indianservers.ai_stem.feature.games.spatial.SharedAnchorRecord
import com.indianservers.ai_stem.feature.games.spatial.SharedAnchorType
import com.indianservers.ai_stem.feature.games.spatial.SharedObjectTransform
import com.indianservers.ai_stem.feature.games.spatial.SharedOriginDefinition
import com.indianservers.ai_stem.feature.games.spatial.SharedOriginMode
import com.indianservers.ai_stem.feature.games.spatial.SharedTransform
import com.indianservers.ai_stem.feature.games.spatial.SpatialSnapshot
import com.indianservers.ai_stem.feature.games.spatial.Vector3Dto
import com.indianservers.ai_stem.feature.games.mission.MathSkill
import com.indianservers.ai_stem.feature.games.mission.MissionScore
import com.indianservers.ai_stem.feature.games.mission.MissionState
import com.indianservers.ai_stem.feature.games.mission.RoleClue
import com.indianservers.ai_stem.feature.games.mission.ValidationStatus
import com.indianservers.ai_stem.feature.games.basedefense.BaseDefenseMatchStage
import com.indianservers.ai_stem.feature.games.basedefense.DefenseType
import com.indianservers.ai_stem.feature.games.basedefense.ResourceType
import java.util.UUID

data class GameMessage(
    val protocolVersion: Int = ARENA_PROTOCOL_VERSION,
    val messageId: String = UUID.randomUUID().toString(),
    val roomId: String,
    val senderPlayerId: String?,
    val sequence: Long,
    val timestampEpochMs: Long = System.currentTimeMillis(),
    val payload: GamePayload
)

sealed interface GamePayload {
    data class HostHello(val roomCode: String, val roomName: String, val port: Int) : GamePayload
    data class JoinRequest(val profile: LocalPlayerProfile, val roomCode: String, val roomToken: String, val reconnectToken: String?) : GamePayload
    data class JoinAccepted(val playerId: String, val reconnectToken: String, val snapshot: LobbySnapshot) : GamePayload
    data class JoinRejected(val reason: String) : GamePayload
    data class PlayerJoined(val player: ArenaPlayer) : GamePayload
    data class PlayerDisconnected(val playerId: String, val graceUntilEpochMs: Long) : GamePayload
    data class PlayerRemoved(val playerId: String, val reason: String) : GamePayload
    data class LobbySnapshot(val state: ArenaRoomState) : GamePayload
    data class ReadyChanged(val playerId: String, val ready: Boolean) : GamePayload
    data class TeamAssignmentRequested(val playerId: String, val teamId: String?) : GamePayload
    data class TeamAssigned(val playerId: String, val teamId: String) : GamePayload
    data class RoleSelected(val playerId: String, val role: ArenaRole) : GamePayload
    data class RoomLocked(val locked: Boolean) : GamePayload
    data class MatchStarting(val matchSeed: Long) : GamePayload
    data class Heartbeat(val lastKnownRoomVersion: Long) : GamePayload
    data class Acknowledgement(val acknowledgedMessageId: String, val acknowledgedSequence: Long) : GamePayload
    data class ReconnectRequest(val playerId: String, val reconnectToken: String) : GamePayload
    data class ReconnectAccepted(val playerId: String, val snapshot: LobbySnapshot) : GamePayload
    data class StateSnapshot(val snapshot: LobbySnapshot) : GamePayload
    data class ProtocolError(val reason: String) : GamePayload
    data class CalibrationStarted(val mode: SharedOriginMode, val originVersion: Long) : GamePayload
    data class MarkerDetected(val playerId: String, val markerName: String, val extentXMetres: Float, val extentZMetres: Float) : GamePayload
    data class CalibrationQualityUpdated(val status: PlayerCalibrationStatus) : GamePayload
    data class CalibrationAccepted(val playerId: String, val originVersion: Long) : GamePayload
    data class CalibrationRejected(val playerId: String, val reason: String) : GamePayload
    data class SharedOriginDefined(val definition: SharedOriginDefinition) : GamePayload
    data class SharedOriginVersionChanged(val originVersion: Long, val reason: String) : GamePayload
    data class PlayerCalibrationState(val status: PlayerCalibrationStatus) : GamePayload
    data class SpatialSnapshotMessage(val snapshot: SpatialSnapshot) : GamePayload
    data class AnchorDefinition(val anchor: SharedAnchorRecord) : GamePayload
    data class AnchorRemoved(val anchorId: String, val originVersion: Long) : GamePayload
    data class RecalibrationRequested(val playerId: String, val reason: String) : GamePayload
    data class MissionCreated(val missionId: String, val skill: MathSkill, val seed: Long) : GamePayload
    data class MissionAssigned(val missionId: String, val teamId: String) : GamePayload
    data class RoleClueAssigned(val missionId: String, val playerId: String, val clue: RoleClue) : GamePayload
    data class MissionStarted(val missionId: String, val startedAtEpochMs: Long) : GamePayload
    data class HintRequested(val missionId: String, val playerId: String) : GamePayload
    data class HintApproved(val missionId: String, val hintIndex: Int) : GamePayload
    data class AnswerSubmitted(val missionId: String, val playerId: String, val answerHash: String) : GamePayload
    data class AnswerValidated(val missionId: String, val playerId: String, val status: ValidationStatus) : GamePayload
    data class MissionStateChanged(val missionId: String, val state: MissionState) : GamePayload
    data class ScoreUpdated(val score: MissionScore) : GamePayload
    data class ParticipationUpdated(val missionId: String, val playerId: String, val actionCount: Int) : GamePayload
    data class MissionCompleted(val missionId: String, val teamId: String, val score: Int) : GamePayload
    data class BaseDefenseStageChanged(val matchId: String, val stage: BaseDefenseMatchStage) : GamePayload
    data class TeamBaseDefined(val matchId: String, val teamId: String, val transformField: String) : GamePayload
    data class ResourceSpawned(val matchId: String, val resourceId: String, val type: ResourceType, val missionId: String) : GamePayload
    data class ResourceResolved(val matchId: String, val resourceId: String, val teamId: String, val rewardEnergy: Int) : GamePayload
    data class DefenseBuilt(val matchId: String, val defenseId: String, val teamId: String, val type: DefenseType) : GamePayload
    data class EnemyWaveStarted(val matchId: String, val wave: Int, val enemyCount: Int) : GamePayload
    data class EnemyStateUpdated(val matchId: String, val compactState: String) : GamePayload
    data class BossPhaseChanged(val matchId: String, val phase: String, val health: Int) : GamePayload
    data class MatchPaused(val matchId: String, val paused: Boolean) : GamePayload
    data class MatchRecoverySnapshot(val matchId: String, val stage: BaseDefenseMatchStage, val sequence: Long) : GamePayload
}

object ArenaProtocolCodec {
    fun encode(message: GameMessage): String {
        val body = when (val p = message.payload) {
            is GamePayload.HostHello -> listOf("HostHello", p.roomCode, p.roomName, p.port)
            is GamePayload.JoinRequest -> listOf("JoinRequest", p.profile.playerId, p.profile.displayName, p.profile.avatarSeed, p.profile.preferredRole.name, p.roomCode, p.roomToken, p.reconnectToken.orEmpty())
            is GamePayload.JoinAccepted -> listOf("JoinAccepted", p.playerId, p.reconnectToken, p.snapshot.state.roomVersion)
            is GamePayload.JoinRejected -> listOf("JoinRejected", p.reason)
            is GamePayload.PlayerJoined -> listOf("PlayerJoined", p.player.playerId, p.player.displayName)
            is GamePayload.PlayerDisconnected -> listOf("PlayerDisconnected", p.playerId, p.graceUntilEpochMs)
            is GamePayload.PlayerRemoved -> listOf("PlayerRemoved", p.playerId, p.reason)
            is GamePayload.LobbySnapshot -> listOf("LobbySnapshot", p.state.roomVersion, p.state.players.joinToString(",") { it.playerId })
            is GamePayload.ReadyChanged -> listOf("ReadyChanged", p.playerId, p.ready)
            is GamePayload.TeamAssignmentRequested -> listOf("TeamAssignmentRequested", p.playerId, p.teamId.orEmpty())
            is GamePayload.TeamAssigned -> listOf("TeamAssigned", p.playerId, p.teamId)
            is GamePayload.RoleSelected -> listOf("RoleSelected", p.playerId, p.role.name)
            is GamePayload.RoomLocked -> listOf("RoomLocked", p.locked)
            is GamePayload.MatchStarting -> listOf("MatchStarting", p.matchSeed)
            is GamePayload.Heartbeat -> listOf("Heartbeat", p.lastKnownRoomVersion)
            is GamePayload.Acknowledgement -> listOf("Acknowledgement", p.acknowledgedMessageId, p.acknowledgedSequence)
            is GamePayload.ReconnectRequest -> listOf("ReconnectRequest", p.playerId, p.reconnectToken)
            is GamePayload.ReconnectAccepted -> listOf("ReconnectAccepted", p.playerId, p.snapshot.state.roomVersion)
            is GamePayload.StateSnapshot -> listOf("StateSnapshot", p.snapshot.state.roomVersion)
            is GamePayload.ProtocolError -> listOf("ProtocolError", p.reason)
            is GamePayload.CalibrationStarted -> listOf("CalibrationStarted", p.mode.name, p.originVersion)
            is GamePayload.MarkerDetected -> listOf("MarkerDetected", p.playerId, p.markerName, p.extentXMetres, p.extentZMetres)
            is GamePayload.CalibrationQualityUpdated -> listOf("CalibrationQualityUpdated") + p.status.toFields()
            is GamePayload.CalibrationAccepted -> listOf("CalibrationAccepted", p.playerId, p.originVersion)
            is GamePayload.CalibrationRejected -> listOf("CalibrationRejected", p.playerId, p.reason)
            is GamePayload.SharedOriginDefined -> listOf("SharedOriginDefined") + p.definition.toFields()
            is GamePayload.SharedOriginVersionChanged -> listOf("SharedOriginVersionChanged", p.originVersion, p.reason)
            is GamePayload.PlayerCalibrationState -> listOf("PlayerCalibrationState") + p.status.toFields()
            is GamePayload.SpatialSnapshotMessage -> listOf("SpatialSnapshot", p.snapshot.originVersion, p.snapshot.transforms.joinToString(";") { it.toField() })
            is GamePayload.AnchorDefinition -> listOf("AnchorDefinition") + p.anchor.toFields()
            is GamePayload.AnchorRemoved -> listOf("AnchorRemoved", p.anchorId, p.originVersion)
            is GamePayload.RecalibrationRequested -> listOf("RecalibrationRequested", p.playerId, p.reason)
            is GamePayload.MissionCreated -> listOf("MissionCreated", p.missionId, p.skill.name, p.seed)
            is GamePayload.MissionAssigned -> listOf("MissionAssigned", p.missionId, p.teamId)
            is GamePayload.RoleClueAssigned -> listOf("RoleClueAssigned", p.missionId, p.playerId, p.clue.role.name, p.clue.clue, p.clue.canSubmit, p.clue.availableActions.joinToString(","))
            is GamePayload.MissionStarted -> listOf("MissionStarted", p.missionId, p.startedAtEpochMs)
            is GamePayload.HintRequested -> listOf("HintRequested", p.missionId, p.playerId)
            is GamePayload.HintApproved -> listOf("HintApproved", p.missionId, p.hintIndex)
            is GamePayload.AnswerSubmitted -> listOf("AnswerSubmitted", p.missionId, p.playerId, p.answerHash)
            is GamePayload.AnswerValidated -> listOf("AnswerValidated", p.missionId, p.playerId, p.status.name)
            is GamePayload.MissionStateChanged -> listOf("MissionStateChanged", p.missionId, p.state.name)
            is GamePayload.ScoreUpdated -> listOf("ScoreUpdated", p.score.teamId, p.score.missionId, p.score.total)
            is GamePayload.ParticipationUpdated -> listOf("ParticipationUpdated", p.missionId, p.playerId, p.actionCount)
            is GamePayload.MissionCompleted -> listOf("MissionCompleted", p.missionId, p.teamId, p.score)
            is GamePayload.BaseDefenseStageChanged -> listOf("BaseDefenseStageChanged", p.matchId, p.stage.name)
            is GamePayload.TeamBaseDefined -> listOf("TeamBaseDefined", p.matchId, p.teamId, p.transformField)
            is GamePayload.ResourceSpawned -> listOf("ResourceSpawned", p.matchId, p.resourceId, p.type.name, p.missionId)
            is GamePayload.ResourceResolved -> listOf("ResourceResolved", p.matchId, p.resourceId, p.teamId, p.rewardEnergy)
            is GamePayload.DefenseBuilt -> listOf("DefenseBuilt", p.matchId, p.defenseId, p.teamId, p.type.name)
            is GamePayload.EnemyWaveStarted -> listOf("EnemyWaveStarted", p.matchId, p.wave, p.enemyCount)
            is GamePayload.EnemyStateUpdated -> listOf("EnemyStateUpdated", p.matchId, p.compactState)
            is GamePayload.BossPhaseChanged -> listOf("BossPhaseChanged", p.matchId, p.phase, p.health)
            is GamePayload.MatchPaused -> listOf("MatchPaused", p.matchId, p.paused)
            is GamePayload.MatchRecoverySnapshot -> listOf("MatchRecoverySnapshot", p.matchId, p.stage.name, p.sequence)
        }.joinToString("|") { it.toString().escape() }
        return listOf(
            message.protocolVersion,
            message.messageId,
            message.roomId,
            message.senderPlayerId.orEmpty(),
            message.sequence,
            message.timestampEpochMs,
            body
        ).joinToString("\t") { it.toString().escape() }
    }

    fun decode(raw: String): Result<GameMessage> = runCatching {
        require(raw.toByteArray().size <= MAX_ARENA_MESSAGE_BYTES) { "Message too large." }
        val parts = raw.split("\t").map { it.unescape() }
        require(parts.size == 7) { "Invalid message envelope." }
        val body = parts[6].split("|").map { it.unescape() }
        require(body.isNotEmpty()) { "Missing payload." }
        val payload = when (body[0]) {
            "HostHello" -> GamePayload.HostHello(body[1], body[2], body[3].toInt())
            "JoinRequest" -> GamePayload.JoinRequest(
                profile = LocalPlayerProfile(body[1], body[2], body[3].toInt(), ArenaRole.valueOf(body[4])),
                roomCode = body[5],
                roomToken = body[6],
                reconnectToken = body.getOrNull(7)?.ifBlank { null }
            )
            "JoinRejected" -> GamePayload.JoinRejected(body[1])
            "ReadyChanged" -> GamePayload.ReadyChanged(body[1], body[2].toBoolean())
            "TeamAssignmentRequested" -> GamePayload.TeamAssignmentRequested(body[1], body.getOrNull(2)?.ifBlank { null })
            "TeamAssigned" -> GamePayload.TeamAssigned(body[1], body[2])
            "RoleSelected" -> GamePayload.RoleSelected(body[1], ArenaRole.valueOf(body[2]))
            "RoomLocked" -> GamePayload.RoomLocked(body[1].toBoolean())
            "Heartbeat" -> GamePayload.Heartbeat(body[1].toLong())
            "Acknowledgement" -> GamePayload.Acknowledgement(body[1], body[2].toLong())
            "ReconnectRequest" -> GamePayload.ReconnectRequest(body[1], body[2])
            "CalibrationStarted" -> GamePayload.CalibrationStarted(SharedOriginMode.valueOf(body[1]), body[2].toLong())
            "MarkerDetected" -> GamePayload.MarkerDetected(body[1], body[2], body[3].toFloat(), body[4].toFloat())
            "CalibrationQualityUpdated" -> GamePayload.CalibrationQualityUpdated(body.statusFromFields(1))
            "CalibrationAccepted" -> GamePayload.CalibrationAccepted(body[1], body[2].toLong())
            "CalibrationRejected" -> GamePayload.CalibrationRejected(body[1], body[2])
            "SharedOriginDefined" -> GamePayload.SharedOriginDefined(body.originFromFields(1))
            "SharedOriginVersionChanged" -> GamePayload.SharedOriginVersionChanged(body[1].toLong(), body[2])
            "PlayerCalibrationState" -> GamePayload.PlayerCalibrationState(body.statusFromFields(1))
            "SpatialSnapshot" -> GamePayload.SpatialSnapshotMessage(
                SpatialSnapshot(
                    originVersion = body[1].toLong(),
                    transforms = body.getOrNull(2)?.takeIf { it.isNotBlank() }?.split(";")?.map { it.objectTransformFromField() }.orEmpty()
                )
            )
            "AnchorDefinition" -> GamePayload.AnchorDefinition(body.anchorFromFields(1))
            "AnchorRemoved" -> GamePayload.AnchorRemoved(body[1], body[2].toLong())
            "RecalibrationRequested" -> GamePayload.RecalibrationRequested(body[1], body[2])
            "MissionCreated" -> GamePayload.MissionCreated(body[1], MathSkill.valueOf(body[2]), body[3].toLong())
            "MissionAssigned" -> GamePayload.MissionAssigned(body[1], body[2])
            "RoleClueAssigned" -> GamePayload.RoleClueAssigned(
                body[1],
                body[2],
                RoleClue(com.indianservers.ai_stem.feature.games.multiplayer.ArenaRole.valueOf(body[3]), body[4], body[5].toBoolean(), body.getOrNull(6)?.split(",")?.filter { it.isNotBlank() }.orEmpty())
            )
            "MissionStarted" -> GamePayload.MissionStarted(body[1], body[2].toLong())
            "HintRequested" -> GamePayload.HintRequested(body[1], body[2])
            "HintApproved" -> GamePayload.HintApproved(body[1], body[2].toInt())
            "AnswerSubmitted" -> GamePayload.AnswerSubmitted(body[1], body[2], body[3])
            "AnswerValidated" -> GamePayload.AnswerValidated(body[1], body[2], ValidationStatus.valueOf(body[3]))
            "MissionStateChanged" -> GamePayload.MissionStateChanged(body[1], MissionState.valueOf(body[2]))
            "ScoreUpdated" -> GamePayload.ScoreUpdated(
                MissionScore(
                    teamId = body[1],
                    missionId = body[2],
                    total = body[3].toInt(),
                    answerPoints = body.getOrNull(4)?.toIntOrNull() ?: 0,
                    processPoints = body.getOrNull(5)?.toIntOrNull() ?: 0,
                    arPoints = body.getOrNull(6)?.toIntOrNull() ?: 0,
                    participationPoints = body.getOrNull(7)?.toIntOrNull() ?: 0,
                    hintBonus = body.getOrNull(8)?.toIntOrNull() ?: 0,
                    speedBonus = body.getOrNull(9)?.toIntOrNull() ?: 0,
                    explanationPoints = body.getOrNull(10)?.toIntOrNull() ?: 0,
                    penalties = body.getOrNull(11)?.toIntOrNull() ?: 0,
                    contributionByPlayer = emptyMap(),
                    topicMasteryDelta = emptyMap()
                )
            )
            "MissionCompleted" -> GamePayload.MissionCompleted(body[1], body[2], body[3].toInt())
            "BaseDefenseStageChanged" -> GamePayload.BaseDefenseStageChanged(body[1], BaseDefenseMatchStage.valueOf(body[2]))
            "TeamBaseDefined" -> GamePayload.TeamBaseDefined(body[1], body[2], body[3])
            "ResourceSpawned" -> GamePayload.ResourceSpawned(body[1], body[2], ResourceType.valueOf(body[3]), body[4])
            "ResourceResolved" -> GamePayload.ResourceResolved(body[1], body[2], body[3], body[4].toInt())
            "DefenseBuilt" -> GamePayload.DefenseBuilt(body[1], body[2], body[3], DefenseType.valueOf(body[4]))
            "EnemyWaveStarted" -> GamePayload.EnemyWaveStarted(body[1], body[2].toInt(), body[3].toInt())
            "EnemyStateUpdated" -> GamePayload.EnemyStateUpdated(body[1], body[2])
            "BossPhaseChanged" -> GamePayload.BossPhaseChanged(body[1], body[2], body[3].toInt())
            "MatchPaused" -> GamePayload.MatchPaused(body[1], body[2].toBoolean())
            "MatchRecoverySnapshot" -> GamePayload.MatchRecoverySnapshot(body[1], BaseDefenseMatchStage.valueOf(body[2]), body[3].toLong())
            "ProtocolError" -> GamePayload.ProtocolError(body[1])
            else -> GamePayload.ProtocolError("Unsupported payload: ${body[0]}")
        }
        GameMessage(
            protocolVersion = parts[0].toInt(),
            messageId = parts[1],
            roomId = parts[2],
            senderPlayerId = parts[3].ifBlank { null },
            sequence = parts[4].toLong(),
            timestampEpochMs = parts[5].toLong(),
            payload = payload
        )
    }

    private fun String.escape(): String = replace("\\", "\\\\").replace("\t", "\\t").replace("|", "\\p").replace("\n", "\\n")
    private fun String.unescape(): String = replace("\\n", "\n").replace("\\p", "|").replace("\\t", "\t").replace("\\\\", "\\")

    private fun PlayerCalibrationStatus.toFields(): List<Any> = listOf(
        playerId,
        displayName,
        calibrationState.name,
        qualityScore,
        instruction,
        originVersion ?: "",
        nonArAnalyst,
        updatedAtEpochMs
    )

    private fun List<String>.statusFromFields(offset: Int): PlayerCalibrationStatus =
        PlayerCalibrationStatus(
            playerId = this[offset],
            displayName = this[offset + 1],
            calibrationState = CalibrationState.valueOf(this[offset + 2]),
            qualityScore = this[offset + 3].toInt(),
            instruction = this[offset + 4],
            originVersion = this[offset + 5].ifBlank { null }?.toLong(),
            nonArAnalyst = this[offset + 6].toBoolean(),
            updatedAtEpochMs = this[offset + 7].toLong()
        )

    private fun SharedOriginDefinition.toFields(): List<Any> = listOf(
        originId,
        originVersion,
        mode.name,
        coordinateVersion,
        markerName.orEmpty(),
        markerWidthMetres ?: "",
        createdByPlayerId,
        createdAtEpochMs,
        hostTransform.toField()
    )

    private fun List<String>.originFromFields(offset: Int): SharedOriginDefinition =
        SharedOriginDefinition(
            originId = this[offset],
            originVersion = this[offset + 1].toLong(),
            mode = SharedOriginMode.valueOf(this[offset + 2]),
            coordinateVersion = this[offset + 3].toInt(),
            markerName = this[offset + 4].ifBlank { null },
            markerWidthMetres = this[offset + 5].ifBlank { null }?.toFloat(),
            createdByPlayerId = this[offset + 6],
            createdAtEpochMs = this[offset + 7].toLong(),
            hostTransform = this[offset + 8].transformFromField()
        )

    private fun SharedAnchorRecord.toFields(): List<Any> = listOf(
        anchorId,
        anchorType.name,
        ownerPlayerId,
        creationSequence,
        active,
        originVersion,
        lastSynchronizationEpochMs,
        sharedTransform.toField()
    )

    private fun List<String>.anchorFromFields(offset: Int): SharedAnchorRecord =
        SharedAnchorRecord(
            anchorId = this[offset],
            anchorType = SharedAnchorType.valueOf(this[offset + 1]),
            ownerPlayerId = this[offset + 2],
            creationSequence = this[offset + 3].toLong(),
            active = this[offset + 4].toBoolean(),
            originVersion = this[offset + 5].toLong(),
            lastSynchronizationEpochMs = this[offset + 6].toLong(),
            sharedTransform = this[offset + 7].transformFromField()
        )

    private fun SharedObjectTransform.toField(): String =
        listOf(objectId, sequence, updatedAtEpochMs, transform.toField()).joinToString(",")

    private fun String.objectTransformFromField(): SharedObjectTransform {
        val parts = split(",")
        return SharedObjectTransform(
            objectId = parts[0],
            sequence = parts[1].toLong(),
            updatedAtEpochMs = parts[2].toLong(),
            transform = parts.drop(3).joinToString(",").transformFromField()
        )
    }

    private fun SharedTransform.toField(): String = listOf(
        coordinateVersion,
        originVersion,
        positionMetres.x,
        positionMetres.y,
        positionMetres.z,
        rotation.x,
        rotation.y,
        rotation.z,
        rotation.w,
        scale.x,
        scale.y,
        scale.z
    ).joinToString(",")

    private fun String.transformFromField(): SharedTransform {
        val p = split(",")
        return SharedTransform(
            coordinateVersion = p.getOrNull(0)?.toIntOrNull() ?: SHARED_COORDINATE_VERSION,
            originVersion = p[1].toLong(),
            positionMetres = Vector3Dto(p[2].toFloat(), p[3].toFloat(), p[4].toFloat()),
            rotation = QuaternionDto(p[5].toFloat(), p[6].toFloat(), p[7].toFloat(), p[8].toFloat()),
            scale = Vector3Dto(p[9].toFloat(), p[10].toFloat(), p[11].toFloat())
        )
    }
}
