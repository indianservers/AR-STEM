package com.indianservers.ai_stem

import com.indianservers.ai_stem.feature.games.api.GameNavigationRegistry
import com.indianservers.ai_stem.feature.games.catalog.GamesCatalog
import com.indianservers.ai_stem.feature.games.multiplayer.ARENA_PROTOCOL_VERSION
import com.indianservers.ai_stem.feature.games.multiplayer.ArenaProtocolCodec
import com.indianservers.ai_stem.feature.games.multiplayer.ArenaProtocolGuard
import com.indianservers.ai_stem.feature.games.multiplayer.ArenaProtocolGuardConfig
import com.indianservers.ai_stem.feature.games.multiplayer.GameMessage
import com.indianservers.ai_stem.feature.games.multiplayer.GamePayload
import com.indianservers.ai_stem.feature.games.multiplayer.requiresHostAuthority
import com.indianservers.ai_stem.feature.games.performance.ArenaPerformancePolicy
import com.indianservers.ai_stem.feature.games.performance.ArenaPerformanceTier
import com.indianservers.ai_stem.feature.games.recovery.ArenaRecoverySnapshot
import com.indianservers.ai_stem.feature.games.recovery.ArenaSnapshotCodec
import com.indianservers.ai_stem.feature.games.spatial.SharedOriginMode
import com.indianservers.ai_stem.feature.games.basedefense.BaseDefenseMatchStage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ArenaProductionHardeningTest {
    @Test
    fun gamePluginsExposeRuntimeContractsForFutureGames() {
        val registry = GameNavigationRegistry(GamesCatalog.registeredPlugins.plugins)
        val arena = registry.byId("ar-math-arena")

        assertNotNull(arena)
        assertEquals(6, registry.games.size)
        assertTrue(arena!!.runtimeContract.hostAuthoritative)
        assertTrue(arena.runtimeContract.requiresSharedOrigin)
        assertTrue(arena.definition.capabilities.any { it.name == "HostAuthoritative" })
    }

    @Test
    fun protocolGuardRejectsHostOnlyMessagesFromPlayers() {
        val guard = ArenaProtocolGuard(hostPlayerId = "host")
        val playerMessage = GameMessage(
            roomId = "room",
            senderPlayerId = "player",
            sequence = 1,
            payload = GamePayload.MatchStarting(matchSeed = 42L)
        )

        val decision = guard.inspect(playerMessage)

        assertFalse(decision.accepted)
        assertTrue(playerMessage.payload.requiresHostAuthority())
    }

    @Test
    fun protocolGuardRejectsDuplicatesOutOfOrderMalformedAndRateBursts() {
        val guard = ArenaProtocolGuard(
            hostPlayerId = "host",
            config = ArenaProtocolGuardConfig(maxMessagesPerWindow = 4, windowMs = 1_000L)
        )
        val first = GameMessage(roomId = "room", senderPlayerId = "host", sequence = 1, payload = GamePayload.Heartbeat(1))
        val duplicate = first.copy()
        val oldSequence = first.copy(messageId = "m2", sequence = 1)
        val second = first.copy(messageId = "m3", sequence = 2)
        val burst = first.copy(messageId = "m4", sequence = 3)

        assertTrue(guard.inspect(first, now = 10_000L).accepted)
        assertFalse(guard.inspect(duplicate, now = 10_010L).accepted)
        assertFalse(guard.inspect(oldSequence, now = 10_020L).accepted)
        assertTrue(guard.inspect(second, now = 10_030L).accepted)
        assertFalse(guard.inspect(burst, now = 10_040L).accepted)
        assertFalse(ArenaProtocolGuard(hostPlayerId = "host").decodeAndInspect("bad-packet", now = 20_000L).accepted)
    }

    @Test
    fun scoreUpdatedRoundTripsThroughProtocolCodec() {
        val score = com.indianservers.ai_stem.feature.games.mission.MissionScore(
            teamId = "team-a",
            missionId = "mission-1",
            total = 180,
            answerPoints = 100,
            processPoints = 20,
            arPoints = 30,
            participationPoints = 20,
            hintBonus = 10,
            speedBonus = 0,
            explanationPoints = 0,
            penalties = 0,
            contributionByPlayer = emptyMap(),
            topicMasteryDelta = emptyMap()
        )
        val encoded = ArenaProtocolCodec.encode(
            GameMessage(
                protocolVersion = ARENA_PROTOCOL_VERSION,
                roomId = "room",
                senderPlayerId = "host",
                sequence = 9,
                payload = GamePayload.ScoreUpdated(score)
            )
        )

        val decoded = ArenaProtocolCodec.decode(encoded).getOrThrow().payload

        assertTrue(decoded is GamePayload.ScoreUpdated)
        assertEquals(180, (decoded as GamePayload.ScoreUpdated).score.total)
    }

    @Test
    fun performanceTiersKeepSimulationFairWhileReducingVisualLoad() {
        val budgets = ArenaPerformanceTier.entries.map(ArenaPerformancePolicy::budgetFor)

        assertTrue(ArenaPerformancePolicy.simulationFairnessInvariant(budgets))
        assertTrue(budgets.first { it.tier == ArenaPerformanceTier.Low }.maxParticles < budgets.first { it.tier == ArenaPerformanceTier.High }.maxParticles)
        assertFalse(budgets.first { it.tier == ArenaPerformanceTier.Low }.enableDepthOcclusion)
        assertTrue(budgets.first { it.tier == ArenaPerformanceTier.High }.enableDepthOcclusion)
    }

    @Test
    fun recoverySnapshotRoundTripsWithoutNetworkOrCameraIdentifiers() {
        val snapshot = ArenaRecoverySnapshot(
            roomId = "room-1",
            roomCode = "ABC234",
            hostPlayerId = "host",
            matchId = "match-1",
            protocolVersion = ARENA_PROTOCOL_VERSION,
            roomVersion = 12L,
            originVersion = 3L,
            originMode = SharedOriginMode.PrintedMarkerOrigin,
            matchStage = BaseDefenseMatchStage.EnemyWaveOne,
            hostSequence = 99L,
            savedAtEpochMs = 123_456L
        )

        val encoded = ArenaSnapshotCodec.encode(snapshot)
        val decoded = ArenaSnapshotCodec.decode(encoded).getOrThrow()

        assertEquals(snapshot, decoded)
        assertFalse(encoded.contains("192.168"))
        assertFalse(encoded.contains("android_id", ignoreCase = true))
    }
}
