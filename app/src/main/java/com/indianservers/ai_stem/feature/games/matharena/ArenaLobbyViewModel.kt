package com.indianservers.ai_stem.feature.games.matharena

import android.app.Application
import android.net.nsd.NsdManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.indianservers.ai_stem.feature.games.diagnostics.ArDiagnosticsReport
import com.indianservers.ai_stem.feature.games.diagnostics.ArDiagnosticsService
import com.indianservers.ai_stem.feature.games.basedefense.BaseDefenseCommand
import com.indianservers.ai_stem.feature.games.basedefense.BaseDefenseConfig
import com.indianservers.ai_stem.feature.games.basedefense.BaseDefenseMatchState
import com.indianservers.ai_stem.feature.games.basedefense.BaseDefenseReducer
import com.indianservers.ai_stem.feature.games.basedefense.BasePlacementCandidate
import com.indianservers.ai_stem.feature.games.basedefense.DefenseType
import com.indianservers.ai_stem.feature.games.basedefense.MatchPace
import com.indianservers.ai_stem.feature.games.basedefense.skill
import com.indianservers.ai_stem.feature.games.classroom.AccessibilitySettings
import com.indianservers.ai_stem.feature.games.classroom.AdaptiveDifficultyDecision
import com.indianservers.ai_stem.feature.games.classroom.AdaptiveDifficultyEngine
import com.indianservers.ai_stem.feature.games.classroom.BoundaryPolicy
import com.indianservers.ai_stem.feature.games.classroom.BoundarySafetyEngine
import com.indianservers.ai_stem.feature.games.classroom.BoundaryWarning
import com.indianservers.ai_stem.feature.games.classroom.HostIntervention
import com.indianservers.ai_stem.feature.games.classroom.HostInterventionType
import com.indianservers.ai_stem.feature.games.classroom.MatchAnalyticsEngine
import com.indianservers.ai_stem.feature.games.classroom.MatchReportExporter
import com.indianservers.ai_stem.feature.games.classroom.MatchSummaryRecord
import com.indianservers.ai_stem.feature.games.classroom.MissionResultRecord
import com.indianservers.ai_stem.feature.games.classroom.PlayerParticipationRecord
import com.indianservers.ai_stem.feature.games.classroom.RecentMissionPerformance
import com.indianservers.ai_stem.feature.games.classroom.TeacherDashboardReducer
import com.indianservers.ai_stem.feature.games.classroom.TeacherMatchSettings
import com.indianservers.ai_stem.feature.games.multiplayer.ARENA_PROTOCOL_VERSION
import com.indianservers.ai_stem.feature.games.multiplayer.ArenaCodeGenerator
import com.indianservers.ai_stem.feature.games.multiplayer.ArenaConnectionState
import com.indianservers.ai_stem.feature.games.multiplayer.ArenaLobbyCommand
import com.indianservers.ai_stem.feature.games.multiplayer.ArenaLobbyReducer
import com.indianservers.ai_stem.feature.games.multiplayer.ArenaPlayer
import com.indianservers.ai_stem.feature.games.multiplayer.ArenaProtocolCodec
import com.indianservers.ai_stem.feature.games.multiplayer.ArenaQrJoinCodec
import com.indianservers.ai_stem.feature.games.multiplayer.ArenaQrJoinPayload
import com.indianservers.ai_stem.feature.games.multiplayer.ArenaRole
import com.indianservers.ai_stem.feature.games.multiplayer.ArenaRoomSettings
import com.indianservers.ai_stem.feature.games.multiplayer.ArenaRoomState
import com.indianservers.ai_stem.feature.games.multiplayer.ArenaTeamAssignmentMode
import com.indianservers.ai_stem.feature.games.multiplayer.ArenaTextValidator
import com.indianservers.ai_stem.feature.games.multiplayer.GameMessage
import com.indianservers.ai_stem.feature.games.multiplayer.GamePayload
import com.indianservers.ai_stem.feature.games.multiplayer.LocalPlayerProfile
import com.indianservers.ai_stem.feature.games.multiplayer.LocalPlayerProfileStore
import com.indianservers.ai_stem.feature.games.multiplayer.defaultArenaTeams
import com.indianservers.ai_stem.feature.games.multiplayer.network.AndroidNsdArenaDiscovery
import com.indianservers.ai_stem.feature.games.multiplayer.network.ArenaNetworkHandle
import com.indianservers.ai_stem.feature.games.multiplayer.network.ArenaTcpTransport
import com.indianservers.ai_stem.feature.games.multiplayer.network.ArenaTransportEvent
import com.indianservers.ai_stem.feature.games.multiplayer.network.DiscoveredArenaRoom
import com.indianservers.ai_stem.feature.games.multiplayer.network.LocalNetworkDiagnostics
import com.indianservers.ai_stem.feature.games.multiplayer.network.LocalNetworkInspector
import com.indianservers.ai_stem.feature.games.mission.AnswerValidationEngine
import com.indianservers.ai_stem.feature.games.mission.AnswerDefinition
import com.indianservers.ai_stem.feature.games.mission.DifficultyLevel
import com.indianservers.ai_stem.feature.games.mission.GradeBand
import com.indianservers.ai_stem.feature.games.mission.MathMission
import com.indianservers.ai_stem.feature.games.mission.MathMissionGenerator
import com.indianservers.ai_stem.feature.games.mission.MathSkill
import com.indianservers.ai_stem.feature.games.mission.MissionAttempt
import com.indianservers.ai_stem.feature.games.mission.MissionEvent
import com.indianservers.ai_stem.feature.games.mission.MissionProgress
import com.indianservers.ai_stem.feature.games.mission.MissionScoringEngine
import com.indianservers.ai_stem.feature.games.mission.MissionStateMachine
import com.indianservers.ai_stem.feature.games.mission.PlayerParticipation
import com.indianservers.ai_stem.feature.games.mission.RoleClue
import com.indianservers.ai_stem.feature.games.mission.RoleClueDistributor
import com.indianservers.ai_stem.feature.games.mission.ValidationResult
import com.indianservers.ai_stem.feature.games.spatial.CalibrationState
import com.indianservers.ai_stem.feature.games.spatial.PlayerCalibrationStatus
import com.indianservers.ai_stem.feature.games.spatial.SharedTransform
import com.indianservers.ai_stem.feature.games.spatial.SharedOriginMode
import com.indianservers.ai_stem.feature.games.spatial.SpatialCommand
import com.indianservers.ai_stem.feature.games.spatial.SpatialSessionReducer
import com.indianservers.ai_stem.feature.games.spatial.SpatialSessionState
import com.indianservers.ai_stem.feature.games.spatial.Vector3Dto
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ArenaShellMode {
    Landing, HostSetup, JoinGame, HostLobby, PlayerLobby, CalibrationLobby, MissionTest, BaseDefense, TeacherDashboard, HowToPlay, DeviceCheck, NetworkTest, TeacherMode, Accessibility, Settings
}

data class ArenaLobbyUiState(
    val mode: ArenaShellMode = ArenaShellMode.Landing,
    val profile: LocalPlayerProfile = LocalPlayerProfile(),
    val hostSettings: ArenaRoomSettings = ArenaRoomSettings(),
    val roomState: ArenaRoomState? = null,
    val spatialSession: SpatialSessionState? = null,
    val discoveredRooms: List<DiscoveredArenaRoom> = emptyList(),
    val diagnostics: ArDiagnosticsReport? = null,
    val networkDiagnostics: LocalNetworkDiagnostics? = null,
    val qrJoinText: String? = null,
    val joinInput: String = "",
    val status: String = "Ready",
    val isHosting: Boolean = false,
    val connectedPort: Int? = null,
    val allowNonArAnalyst: Boolean = true,
    val testMission: MathMission? = null,
    val testProgress: MissionProgress? = null,
    val testRoleClues: List<RoleClue> = emptyList(),
    val testAnswer: String = "",
    val testValidation: ValidationResult? = null,
    val testScore: Int? = null,
    val baseDefense: BaseDefenseMatchState? = null,
    val teacherSettings: TeacherMatchSettings = TeacherMatchSettings(),
    val accessibility: AccessibilitySettings = AccessibilitySettings(),
    val interventions: List<HostIntervention> = emptyList(),
    val matchSummary: MatchSummaryRecord? = null,
    val adaptiveDecision: AdaptiveDifficultyDecision? = null,
    val boundaryWarning: BoundaryWarning = BoundaryWarning.Safe,
    val exportedReportPreview: String? = null
)

class ArenaLobbyViewModel(application: Application) : AndroidViewModel(application) {
    private val profileStore = LocalPlayerProfileStore(application)
    private val diagnosticsService = ArDiagnosticsService()
    private val networkInspector = LocalNetworkInspector()
    private val discovery = AndroidNsdArenaDiscovery(application)
    private var discoveryJob: Job? = null
    private var transportJob: Job? = null
    private var nsdRegistration: NsdManager.RegistrationListener? = null
    private var networkHandle: ArenaNetworkHandle? = null
    private var transport: ArenaTcpTransport? = null
    private var sequence = 0L
    private val missionStateMachine = MissionStateMachine()
    private val roleClueDistributor = RoleClueDistributor()
    private val scoringEngine = MissionScoringEngine()
    private val baseDefenseReducer = BaseDefenseReducer()
    private val teacherReducer = TeacherDashboardReducer()
    private val analyticsEngine = MatchAnalyticsEngine()
    private val adaptiveEngine = AdaptiveDifficultyEngine()
    private val boundaryEngine = BoundarySafetyEngine()

    private val _uiState = MutableStateFlow(ArenaLobbyUiState(profile = profileStore.load()))
    val uiState: StateFlow<ArenaLobbyUiState> = _uiState.asStateFlow()

    init {
        refreshNetwork()
        startDiscovery()
    }

    fun setMode(mode: ArenaShellMode) {
        _uiState.update { it.copy(mode = mode, status = "Ready") }
        if (mode == ArenaShellMode.DeviceCheck) refreshDiagnostics()
        if (mode == ArenaShellMode.NetworkTest || mode == ArenaShellMode.JoinGame) refreshNetwork()
    }

    fun updateDisplayName(value: String) {
        val safe = value.take(24)
        _uiState.update { it.copy(profile = it.profile.copy(displayName = safe)) }
    }

    fun saveProfile() {
        runCatching {
            val clean = ArenaTextValidator.cleanDisplayName(_uiState.value.profile.displayName)
            val profile = _uiState.value.profile.copy(displayName = clean)
            profileStore.save(profile)
            _uiState.update { it.copy(profile = profile, status = "Profile saved") }
        }.onFailure { error(it.message ?: "Could not save profile.") }
    }

    fun setJoinInput(value: String) {
        _uiState.update { it.copy(joinInput = value.take(512)) }
    }

    fun setMissionTestAnswer(value: String) {
        _uiState.update { it.copy(testAnswer = value.take(120)) }
    }

    fun generateMissionTest(skill: MathSkill = MathSkill.entries.random()) {
        val seed = System.currentTimeMillis() % 100_000
        val mission = MathMissionGenerator.generate(skill, seed, GradeBand.Mixed, DifficultyLevel.Medium)
        val progress = MissionProgress(mission)
        _uiState.update {
            it.copy(
                mode = ArenaShellMode.MissionTest,
                testMission = mission,
                testProgress = missionStateMachine.transition(progress, MissionEvent.StartBriefing, hostAuthorized = true),
                testRoleClues = roleClueDistributor.cluesFor(mission),
                testAnswer = "",
                testValidation = null,
                testScore = null,
                status = "Generated ${mission.skill}"
            )
        }
    }

    fun startMissionTest() {
        val progress = _uiState.value.testProgress ?: return
        _uiState.update { it.copy(testProgress = missionStateMachine.transition(progress, MissionEvent.StartMission, hostAuthorized = true), status = "Mission active") }
    }

    fun submitMissionTestAnswer() {
        val progress = _uiState.value.testProgress ?: return
        val answer = _uiState.value.testAnswer
        val attempt = MissionAttempt(_uiState.value.profile.playerId, _uiState.value.profile.preferredRole, answer, System.currentTimeMillis(), elapsedSeconds = 20)
        val next = missionStateMachine.transition(progress, MissionEvent.SubmitAnswer(attempt), hostAuthorized = true)
        val score = scoringEngine.score(
            teamId = "test",
            progress = next,
            participation = listOf(PlayerParticipation(_uiState.value.profile.playerId, _uiState.value.profile.preferredRole, actionsTaken = 1, submissions = 1)),
            arAccurate = false,
            explanationCorrect = false
        )
        _uiState.update { it.copy(testProgress = next, testValidation = next.validationResult, testScore = score.total, status = next.validationResult?.message ?: "Validated") }
    }

    fun startBaseDefense(shortMatch: Boolean = false) {
        val room = _uiState.value.roomState
        val hostId = room?.hostPlayerId ?: _uiState.value.profile.playerId
        val state = BaseDefenseMatchState(
            matchId = "base-defense-${System.currentTimeMillis()}",
            hostPlayerId = hostId,
            config = BaseDefenseConfig(pace = if (shortMatch) MatchPace.Short else MatchPace.Standard)
        )
        _uiState.update { it.copy(mode = ArenaShellMode.BaseDefense, baseDefense = state, status = "Base Defence started") }
    }

    fun placeDemoBase() {
        val state = _uiState.value.baseDefense ?: return startBaseDefense(shortMatch = true)
        val index = state.bases.size
        val candidate = BasePlacementCandidate(
            teamId = if (index == 0) "red" else "blue",
            transform = SharedTransform(originVersion = _uiState.value.spatialSession?.originVersion ?: 1, positionMetres = Vector3Dto(if (index == 0) -1.6f else 1.6f, 0f, 1.2f)),
            planeWidthMetres = 2f,
            planeDepthMetres = 2f
        )
        applyBaseDefense(BaseDefenseCommand.PlaceBase(state.hostPlayerId, candidate, if (index == 0) "Red Team" else "Blue Team"))
    }

    fun advanceBaseDefenseStage() {
        val state = _uiState.value.baseDefense ?: return
        applyBaseDefense(BaseDefenseCommand.AdvanceStage(state.hostPlayerId))
    }

    fun spawnBaseDefenseResources() {
        val state = _uiState.value.baseDefense ?: return
        applyBaseDefense(BaseDefenseCommand.SpawnResources(state.hostPlayerId, seed = state.sequence + 1200L, nowMs = System.currentTimeMillis()))
    }

    fun solveFirstResource() {
        val state = _uiState.value.baseDefense ?: return
        val resource = state.resources.firstOrNull { it.lifecycle == com.indianservers.ai_stem.feature.games.basedefense.ObjectLifecycle.Active } ?: return
        applyBaseDefense(BaseDefenseCommand.SubmitResourceAnswer(state.hostPlayerId, resource.resourceId, _uiState.value.profile.playerId, resource.mission.expectedAnswer.answerString()))
    }

    fun buildDemoDefense() {
        val state = _uiState.value.baseDefense ?: return
        val base = state.bases.firstOrNull() ?: return
        val slot = base.defenseSlots.firstOrNull { it.occupiedDefenseId == null } ?: return
        val type = DefenseType.NumberCannon
        val mission = MathMissionGenerator.generate(type.skill(), state.sequence + type.ordinal, GradeBand.Mixed, DifficultyLevel.Medium)
        applyBaseDefense(BaseDefenseCommand.BuildDefense(state.hostPlayerId, base.teamId, slot.slotId, type, mission.expectedAnswer.answerString()))
    }

    fun spawnWaveOne() {
        val state = _uiState.value.baseDefense ?: return
        applyBaseDefense(BaseDefenseCommand.SpawnWave(state.hostPlayerId, if (state.currentWave == 0) 1 else state.currentWave + 1, state.sequence + 5000L))
    }

    fun tickBaseDefense() {
        val state = _uiState.value.baseDefense ?: return
        applyBaseDefense(BaseDefenseCommand.Tick(state.hostPlayerId, 5f))
    }

    fun startBossBattle() {
        val state = _uiState.value.baseDefense ?: return
        applyBaseDefense(BaseDefenseCommand.StartBoss(state.hostPlayerId, state.sequence + 9000L))
    }

    fun pauseOrResumeBaseDefense() {
        val state = _uiState.value.baseDefense ?: return
        applyBaseDefense(if (state.paused) BaseDefenseCommand.Resume(state.hostPlayerId) else BaseDefenseCommand.Pause(state.hostPlayerId))
    }

    fun openTeacherDashboard() {
        _uiState.update { it.copy(mode = ArenaShellMode.TeacherDashboard, status = "Teacher dashboard") }
    }

    fun toggleLargeText() {
        _uiState.update { state ->
            val next = state.accessibility.copy(largeText = !state.accessibility.largeText)
            state.copy(accessibility = next, baseDefense = state.baseDefense?.copy(config = next.applyTo(state.baseDefense.config)), status = "Large text ${if (next.largeText) "on" else "off"}")
        }
    }

    fun toggleSeatedPlay() {
        _uiState.update { state ->
            val next = state.accessibility.copy(seatedPlayMode = !state.accessibility.seatedPlayMode, simplifiedArGuidance = !state.accessibility.seatedPlayMode)
            state.copy(accessibility = next, baseDefense = state.baseDefense?.copy(config = next.applyTo(state.baseDefense.config)), status = "Seated play ${if (next.seatedPlayMode) "on" else "off"}")
        }
    }

    fun teacherPauseResume() {
        val match = _uiState.value.baseDefense ?: return
        val type = if (match.paused) HostInterventionType.Resume else HostInterventionType.Pause
        logIntervention(type, null, "Teacher ${type.name.lowercase()}")
        pauseOrResumeBaseDefense()
    }

    fun teacherReduceDifficulty() {
        logIntervention(HostInterventionType.ReduceDifficulty, null, "Reduced difficulty for next mission")
        val history = samplePerformance()
        _uiState.update { it.copy(adaptiveDecision = adaptiveEngine.decide(it.teacherSettings.difficulty, history, enabled = true), status = "Adaptive decision updated") }
    }

    fun teacherEndMatch() {
        logIntervention(HostInterventionType.EndMatch, null, "Teacher ended match")
        val match = _uiState.value.baseDefense ?: return
        applyBaseDefense(BaseDefenseCommand.EndMatch(match.hostPlayerId))
        generateLocalSummary()
    }

    fun generateLocalSummary() {
        val match = _uiState.value.baseDefense ?: return
        val missionResults = match.activeMissions.map {
            MissionResultRecord(
                missionId = it.missionId,
                teamId = it.teamId,
                topic = it.mission.topic,
                skill = it.mission.skill,
                status = it.validation?.status ?: com.indianservers.ai_stem.feature.games.mission.ValidationStatus.Incorrect,
                responseSeconds = 30,
                hintCount = 0,
                retryCount = 0,
                commonMisconception = it.mission.misconceptions.firstOrNull()?.feedback
            )
        }
        val participation = _uiState.value.roomState?.players.orEmpty().mapIndexed { index, player ->
            PlayerParticipationRecord(
                playerId = player.playerId,
                localAlias = "Player ${index + 1}",
                roleCounts = player.role?.let { mapOf(it to 1) }.orEmpty(),
                actionCount = 1 + index,
                answerContributionCount = if (player.role == ArenaRole.Solver) 1 else 0,
                hintUse = 0,
                accuracy = if (index == 0) 1.0 else 0.75
            )
        }
        val summary = analyticsEngine.summarize(match, missionResults, participation, _uiState.value.interventions, individualHistoryEnabled = false)
        _uiState.update { it.copy(matchSummary = summary, exportedReportPreview = MatchReportExporter.toCsv(summary, includeIndividuals = false), status = "Local summary generated") }
    }

    fun evaluateBoundaryAtBase() {
        val position = _uiState.value.baseDefense?.bases?.firstOrNull()?.transform?.positionMetres ?: Vector3Dto(0f, 0f, 0f)
        _uiState.update { it.copy(boundaryWarning = boundaryEngine.evaluate(position, BoundaryPolicy(maxRadiusMetres = it.teacherSettings.accessibilityDefaults.let { _ -> 5f }))) }
    }

    fun hostGame() {
        val profile = _uiState.value.profile.copy(displayName = ArenaTextValidator.cleanDisplayName(_uiState.value.profile.displayName))
        profileStore.save(profile)
        val settings = _uiState.value.hostSettings.sanitized()
        val roomToken = ArenaCodeGenerator.token()
        val host = ArenaPlayer(
            playerId = profile.playerId,
            displayName = profile.displayName,
            avatarSeed = profile.avatarSeed,
            teamId = defaultArenaTeams(settings.teamCount).first().id,
            role = profile.preferredRole,
            reconnectToken = ArenaCodeGenerator.token(),
            lastSeenEpochMs = System.currentTimeMillis()
        )
        val state = ArenaRoomState(
            protocolVersion = ARENA_PROTOCOL_VERSION,
            roomCode = settings.roomCode,
            roomToken = roomToken,
            hostPlayerId = profile.playerId,
            settings = settings,
            players = listOf(host)
        )
        val spatial = SpatialSessionState(hostPlayerId = profile.playerId)
        closeNetwork()
        val tcp = ArenaTcpTransport()
        val handle = tcp.host()
        transport = tcp
        networkHandle = handle
        observeTransport(tcp)
        nsdRegistration = discovery.registerRoom(
            settings = settings,
            port = handle.port,
            onRegistered = { service -> _uiState.update { it.copy(status = "Hosting as $service") } },
            onError = { error(it) }
        )
        val qr = ArenaQrJoinCodec.encode(
            ArenaQrJoinPayload(
                roomCode = settings.roomCode,
                hostServiceName = "AI STEM ${settings.roomCode}",
                hostAddress = null,
                port = handle.port,
                roomToken = roomToken,
                expiresAtEpochMs = System.currentTimeMillis() + 5 * 60 * 1000L
            )
        )
        _uiState.update {
            it.copy(
                profile = profile,
                roomState = state,
                spatialSession = spatial,
                mode = ArenaShellMode.HostLobby,
                qrJoinText = qr,
                isHosting = true,
                connectedPort = handle.port,
                status = "Room ${settings.roomCode} open on local Wi-Fi"
            )
        }
    }

    fun joinSelectedRoom(room: DiscoveredArenaRoom) {
        val current = _uiState.value
        val host = room.hostAddress
        if (host.isNullOrBlank()) {
            error("Host address is still resolving. Try again in a moment.")
            return
        }
        closeNetwork()
        runCatching {
            val tcp = ArenaTcpTransport()
            val handle = tcp.connect(host, room.port)
            transport = tcp
            networkHandle = handle
            observeTransport(tcp)
            viewModelScope.launch {
                handle.broadcast(
                    message(GamePayload.JoinRequest(current.profile, room.roomCode, current.joinInput.ifBlank { "discovery-token-required" }, null))
                )
            }
            _uiState.update { it.copy(mode = ArenaShellMode.PlayerLobby, status = "Join request sent to ${room.roomName}", isHosting = false) }
        }.onFailure { error("Join failed: ${it.message}") }
    }

    fun joinFromQr() {
        ArenaQrJoinCodec.decode(_uiState.value.joinInput)
            .onSuccess { payload ->
                if (payload.hostAddress.isNullOrBlank()) {
                    error("QR code needs a host address or local discovery match.")
                    return
                }
                closeNetwork()
                runCatching {
                    val tcp = ArenaTcpTransport()
                    val handle = tcp.connect(payload.hostAddress, payload.port)
                    transport = tcp
                    networkHandle = handle
                    observeTransport(tcp)
                    viewModelScope.launch {
                        handle.broadcast(message(GamePayload.JoinRequest(_uiState.value.profile, payload.roomCode, payload.roomToken, null)))
                    }
                    _uiState.update { it.copy(mode = ArenaShellMode.PlayerLobby, status = "Join request sent for ${payload.roomCode}", isHosting = false) }
                }.onFailure { error("QR join failed: ${it.message}") }
            }
            .onFailure { error(it.message ?: "Invalid QR join code.") }
    }

    fun toggleReady(playerId: String = _uiState.value.profile.playerId) {
        val room = _uiState.value.roomState ?: return
        val player = room.players.firstOrNull { it.playerId == playerId } ?: return
        applyHostCommand(ArenaLobbyCommand.SetReady(playerId, !player.ready))
    }

    fun autoAssign() = applyHostCommand(ArenaLobbyCommand.AutoAssign(_uiState.value.profile.playerId, ArenaTeamAssignmentMode.Balanced))
    fun lockRoom() = applyHostCommand(ArenaLobbyCommand.LockRoom(_uiState.value.profile.playerId, _uiState.value.roomState?.locked != true))
    fun startMatch() {
        val room = _uiState.value.roomState ?: return
        val spatial = _uiState.value.spatialSession
        val required = room.players
            .filter { it.connectionState == ArenaConnectionState.Connected }
            .map { it.playerId }
            .toSet()
        if (spatial == null || !spatial.requiredPlayersReady(required, _uiState.value.allowNonArAnalyst)) {
            error("Calibrate required players before starting the shared AR match.")
            _uiState.update { it.copy(mode = ArenaShellMode.CalibrationLobby) }
            return
        }
        applyHostCommand(ArenaLobbyCommand.StartMatch(_uiState.value.profile.playerId))
    }
    fun chooseRole(role: ArenaRole) = applyHostCommand(ArenaLobbyCommand.AssignRole(_uiState.value.profile.playerId, _uiState.value.profile.playerId, role))

    fun leaveRoom() {
        closeNetwork()
        _uiState.update { it.copy(mode = ArenaShellMode.Landing, roomState = null, spatialSession = null, qrJoinText = null, isHosting = false, status = "Left room") }
        startDiscovery()
    }

    fun openCalibrationLobby() {
        val room = _uiState.value.roomState
        val spatial = _uiState.value.spatialSession ?: room?.let { SpatialSessionState(hostPlayerId = it.hostPlayerId) }
        _uiState.update { it.copy(spatialSession = spatial, mode = ArenaShellMode.CalibrationLobby, status = "Shared origin calibration") }
    }

    fun startMarkerCalibration() = applySpatialCommand(SpatialCommand.StartCalibration(_uiState.value.profile.playerId, SharedOriginMode.PrintedMarkerOrigin))

    fun startSurfaceFallbackCalibration() = applySpatialCommand(SpatialCommand.StartCalibration(_uiState.value.profile.playerId, SharedOriginMode.HostSurfacePlacement))

    fun reportCalibrationQuality(status: PlayerCalibrationStatus) = applySpatialCommand(SpatialCommand.UpdatePlayerCalibration(status))

    fun markCurrentPlayerAsNonArAnalyst() {
        val profile = _uiState.value.profile
        reportCalibrationQuality(
            PlayerCalibrationStatus(
                playerId = profile.playerId,
                displayName = profile.displayName,
                calibrationState = CalibrationState.RecalibrationRequired,
                qualityScore = 0,
                instruction = "Player assigned as non-AR analyst.",
                originVersion = _uiState.value.spatialSession?.originVersion,
                nonArAnalyst = true
            )
        )
    }

    fun refreshDiagnostics() {
        _uiState.update { it.copy(status = "Checking AR device...") }
        _uiState.update { it.copy(diagnostics = diagnosticsService.inspect(getApplication()), status = "AR device check complete") }
    }

    fun refreshNetwork() {
        _uiState.update { it.copy(networkDiagnostics = networkInspector.inspect(getApplication())) }
    }

    private fun observeTransport(tcp: ArenaTcpTransport) {
        transportJob?.cancel()
        transportJob = viewModelScope.launch {
            tcp.events.collect { event ->
                when (event) {
                    is ArenaTransportEvent.ClientConnected -> _uiState.update { it.copy(status = "Player connected") }
                    is ArenaTransportEvent.ClientDisconnected -> _uiState.update { it.copy(status = "Player disconnected: ${event.reason ?: "closed"}") }
                    is ArenaTransportEvent.TransportError -> error(event.reason)
                    is ArenaTransportEvent.MessageReceived -> handleInbound(event.inbound.message)
                }
            }
        }
    }

    private fun handleInbound(message: GameMessage) {
        if (handleSpatialPayload(message.payload)) return
        val state = _uiState.value.roomState ?: return
        val result = ArenaLobbyReducer.reduceMessage(state, message)
        _uiState.update { it.copy(roomState = result.state, status = result.reason ?: "Lobby updated") }
        if (result.accepted && _uiState.value.isHosting) {
            viewModelScope.launch {
                networkHandle?.broadcast(message(GamePayload.LobbySnapshot(result.state)))
            }
        }
    }

    private fun handleSpatialPayload(payload: GamePayload): Boolean {
        val spatialCommand = when (payload) {
            is GamePayload.CalibrationStarted -> SpatialCommand.StartCalibration(_uiState.value.roomState?.hostPlayerId ?: _uiState.value.profile.playerId, payload.mode)
            is GamePayload.CalibrationQualityUpdated -> SpatialCommand.UpdatePlayerCalibration(payload.status)
            is GamePayload.PlayerCalibrationState -> SpatialCommand.UpdatePlayerCalibration(payload.status)
            is GamePayload.RecalibrationRequested -> SpatialCommand.RequestRecalibration(_uiState.value.roomState?.hostPlayerId ?: _uiState.value.profile.playerId, payload.playerId, payload.reason)
            is GamePayload.SharedOriginDefined -> SpatialCommand.DefineOrigin(_uiState.value.roomState?.hostPlayerId ?: _uiState.value.profile.playerId, payload.definition)
            is GamePayload.AnchorDefinition -> SpatialCommand.UpsertAnchor(_uiState.value.roomState?.hostPlayerId ?: _uiState.value.profile.playerId, payload.anchor)
            is GamePayload.AnchorRemoved -> SpatialCommand.RemoveAnchor(_uiState.value.roomState?.hostPlayerId ?: _uiState.value.profile.playerId, payload.anchorId)
            else -> null
        } ?: return false
        val spatial = _uiState.value.spatialSession ?: SpatialSessionState(hostPlayerId = _uiState.value.roomState?.hostPlayerId ?: _uiState.value.profile.playerId)
        val result = SpatialSessionReducer.reduce(spatial, spatialCommand)
        _uiState.update { it.copy(spatialSession = result.state, status = result.reason ?: "Spatial state synchronized") }
        return true
    }

    private fun applyHostCommand(command: ArenaLobbyCommand) {
        val state = _uiState.value.roomState ?: return
        val result = ArenaLobbyReducer.reduce(state, command)
        _uiState.update { it.copy(roomState = result.state, status = result.reason ?: "Lobby updated") }
        if (result.accepted && _uiState.value.isHosting) {
            viewModelScope.launch { networkHandle?.broadcast(message(GamePayload.LobbySnapshot(result.state))) }
        }
    }

    private fun applySpatialCommand(command: SpatialCommand) {
        val spatial = _uiState.value.spatialSession ?: SpatialSessionState(hostPlayerId = _uiState.value.roomState?.hostPlayerId ?: _uiState.value.profile.playerId)
        val result = SpatialSessionReducer.reduce(spatial, command)
        _uiState.update { it.copy(spatialSession = result.state, mode = ArenaShellMode.CalibrationLobby, status = result.reason ?: "Spatial calibration updated") }
        if (result.accepted && _uiState.value.isHosting) {
            val payload = when (command) {
                is SpatialCommand.StartCalibration -> GamePayload.CalibrationStarted(command.mode, result.state.originVersion)
                is SpatialCommand.UpdatePlayerCalibration -> GamePayload.PlayerCalibrationState(command.status)
                is SpatialCommand.RequestRecalibration -> GamePayload.RecalibrationRequested(command.playerId, command.reason)
                else -> null
            }
            if (payload != null) viewModelScope.launch { networkHandle?.broadcast(message(payload)) }
        }
    }

    private fun applyBaseDefense(command: BaseDefenseCommand) {
        val state = _uiState.value.baseDefense ?: return
        val result = baseDefenseReducer.reduce(state, command)
        _uiState.update { it.copy(baseDefense = result.state, mode = ArenaShellMode.BaseDefense, status = result.reason ?: "Base Defence updated") }
        if (result.accepted && _uiState.value.isHosting) {
            val payload = GamePayload.MatchRecoverySnapshot(result.state.matchId, result.state.stage, result.state.sequence)
            viewModelScope.launch { networkHandle?.broadcast(message(payload)) }
        }
    }

    private fun startDiscovery() {
        discoveryJob?.cancel()
        discoveryJob = viewModelScope.launch {
            discovery.discoverRooms().collect { room ->
                _uiState.update { state ->
                    val updated = (state.discoveredRooms.filterNot { it.serviceName == room.serviceName } + room)
                        .sortedBy { it.roomName }
                    state.copy(discoveredRooms = updated)
                }
            }
        }
    }

    private fun message(payload: GamePayload): GameMessage {
        val state = _uiState.value.roomState
        return GameMessage(
            roomId = state?.roomId ?: _uiState.value.hostSettings.roomCode,
            senderPlayerId = _uiState.value.profile.playerId,
            sequence = ++sequence,
            payload = payload
        )
    }

    private fun error(text: String) {
        _uiState.update { it.copy(status = text) }
    }

    private fun logIntervention(type: HostInterventionType, targetId: String?, reason: String) {
        val match = _uiState.value.baseDefense ?: return
        val intervention = HostIntervention("int-${System.currentTimeMillis()}", match.matchId, type, targetId, reason)
        val updated = teacherReducer.logIntervention(_uiState.value.interventions, intervention, hostAuthorized = true)
        _uiState.update { it.copy(interventions = updated) }
    }

    private fun samplePerformance(): List<RecentMissionPerformance> =
        _uiState.value.baseDefense?.activeMissions.orEmpty().takeLast(5).map {
            RecentMissionPerformance(it.mission.topic, it.mission.skill, it.validation?.status ?: com.indianservers.ai_stem.feature.games.mission.ValidationStatus.Incorrect, 35, 1, 1)
        }.ifEmpty {
            listOf(
                RecentMissionPerformance(com.indianservers.ai_stem.feature.games.mission.MathTopic.Fractions, MathSkill.FractionAddSubtract, com.indianservers.ai_stem.feature.games.mission.ValidationStatus.Incorrect, 70, 2, 2),
                RecentMissionPerformance(com.indianservers.ai_stem.feature.games.mission.MathTopic.Fractions, MathSkill.FractionAddSubtract, com.indianservers.ai_stem.feature.games.mission.ValidationStatus.Correct, 50, 1, 1),
                RecentMissionPerformance(com.indianservers.ai_stem.feature.games.mission.MathTopic.Algebra, MathSkill.TwoStepEquation, com.indianservers.ai_stem.feature.games.mission.ValidationStatus.Incorrect, 80, 2, 2)
            )
        }

    private fun AnswerDefinition.answerString(): String = when (this) {
        is AnswerDefinition.IntegerAnswer -> value.toString()
        is AnswerDefinition.DecimalAnswer -> value.toString()
        is AnswerDefinition.FractionAnswer -> value.toString()
        is AnswerDefinition.MultipleChoiceAnswer -> correctChoiceIds.joinToString(",")
        is AnswerDefinition.ExpressionAnswer -> "${expression.coefficient}${expression.variable}+${expression.constant}"
        is AnswerDefinition.EquationAnswer -> "$variable=$value"
        is AnswerDefinition.CoordinateAnswer -> "($x,$y)"
        is AnswerDefinition.AngleAnswer -> "$degrees degrees"
        is AnswerDefinition.MeasurementAnswer -> "$value ${if (unit == com.indianservers.ai_stem.feature.games.mission.UnitKind.SquareMetres) "m2" else unit.name}"
        is AnswerDefinition.SequenceAnswer -> orderedIds.joinToString(",")
        is AnswerDefinition.MultiStepAnswer -> parts.joinToString("|") { it.answerString() }
    }

    private fun closeNetwork() {
        discovery.unregister(nsdRegistration)
        nsdRegistration = null
        networkHandle?.close()
        networkHandle = null
        transportJob?.cancel()
        transportJob = null
    }

    override fun onCleared() {
        closeNetwork()
        discoveryJob?.cancel()
        super.onCleared()
    }
}
