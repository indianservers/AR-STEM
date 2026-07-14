package com.indianservers.ai_stem.feature.games.catalog

import com.indianservers.ai_stem.feature.games.api.GameArRequirement
import com.indianservers.ai_stem.feature.games.api.GameAvailability
import com.indianservers.ai_stem.feature.games.api.GameCapability
import com.indianservers.ai_stem.feature.games.api.GameDefinition
import com.indianservers.ai_stem.feature.games.api.GameDestination
import com.indianservers.ai_stem.feature.games.api.GameEnvironment
import com.indianservers.ai_stem.feature.games.api.GameHeroArtwork
import com.indianservers.ai_stem.feature.games.api.GameHowToPlay
import com.indianservers.ai_stem.feature.games.api.GameIcon
import com.indianservers.ai_stem.feature.games.api.GameNavigationRegistry
import com.indianservers.ai_stem.feature.games.api.GamePlayerMode
import com.indianservers.ai_stem.feature.games.api.GamePlugin
import com.indianservers.ai_stem.feature.games.api.GameRuntimeContract

object GamesCatalog {
    val registeredPlugins: GameNavigationRegistry = GameNavigationRegistry(
        listOf(
            MathFortressPlugin,
            ComingSoonGamePlugin(
                id = "equation_escape_ar",
                title = "Equation Escape AR",
                tagline = "Solve the Room. Unlock the World.",
                description = "A puzzle-room AR game concept where equations, logic clues and coordinate locks become spatial challenges.",
                icon = GameIcon.Escape,
                heroArtwork = GameHeroArtwork.EscapeRoom,
                topics = listOf("Arithmetic", "Algebra", "Patterns", "Equations", "Logic", "Coordinate geometry"),
                loop = listOf("Scan the room", "Find equation locks", "Solve clue chains", "Unlock the next chamber"),
                outcomes = listOf("Solve multi-step equations", "Recognize patterns", "Use coordinates to locate clues"),
                availability = GameAvailability.Available,
                destination = GameDestination.EquationEscape
            ),
            ComingSoonGamePlugin(
                id = "geometry_architect_ar",
                title = "Geometry Architect AR",
                tagline = "Measure. Design. Construct.",
                description = "A spatial design game concept for measuring real surfaces and constructing scale-aware geometry.",
                icon = GameIcon.Architect,
                heroArtwork = GameHeroArtwork.GeometryStudio,
                topics = listOf("Geometry", "Mensuration", "Transformations", "Scale", "Surface area", "Volume", "Spatial reasoning"),
                loop = listOf("Measure a site", "Choose construction constraints", "Build a model", "Compare area and volume tradeoffs"),
                outcomes = listOf("Estimate and measure dimensions", "Apply scale factors", "Reason about surface area and volume"),
                availability = GameAvailability.Available,
                destination = GameDestination.GeometryArchitect
            ),
            ComingSoonGamePlugin(
                id = "fraction_factory_ar",
                title = "Fraction Factory AR",
                tagline = "Combine. Balance. Produce.",
                description = "A production-line AR game concept where fraction, decimal and ratio machines must be balanced.",
                icon = GameIcon.Factory,
                heroArtwork = GameHeroArtwork.FactoryLine,
                topics = listOf("Fractions", "Decimals", "Ratios", "Percentages", "Proportion", "Unit conversion"),
                loop = listOf("Feed number parts", "Balance equivalent values", "Convert units", "Ship a correct batch"),
                outcomes = listOf("Build equivalent fractions", "Convert between forms", "Use proportional reasoning"),
                availability = GameAvailability.Available,
                destination = GameDestination.FractionFactory
            ),
            ComingSoonGamePlugin(
                id = "coordinate_conquest_ar",
                title = "Coordinate Conquest AR",
                tagline = "Plot. Navigate. Capture.",
                description = "A coordinate-grid AR strategy concept for plotting points, vectors and transformations in shared space.",
                icon = GameIcon.Coordinates,
                heroArtwork = GameHeroArtwork.CoordinateField,
                topics = listOf("Coordinates", "Graphs", "Vectors", "Slope", "Distance", "Transformations", "Functions"),
                loop = listOf("Plot the target", "Move along vectors", "Capture graph zones", "Transform objects to defend territory"),
                outcomes = listOf("Plot coordinates accurately", "Calculate slope and distance", "Connect functions to movement"),
                availability = GameAvailability.Available,
                destination = GameDestination.CoordinateConquest
            ),
            ComingSoonGamePlugin(
                id = "math_expedition_ar",
                title = "Math Expedition AR",
                tagline = "Explore the World Through Mathematics.",
                description = "An outdoor map-game concept for route mathematics, estimation and statistics. Open-map rendering will be added later.",
                icon = GameIcon.Expedition,
                heroArtwork = GameHeroArtwork.ExpeditionMap,
                topics = listOf("Distance", "Scale", "Bearings", "Coordinates", "Speed", "Time", "Area", "Route optimisation", "Estimation", "Statistics"),
                loop = listOf("Choose a route", "Estimate distance and time", "Solve location challenges", "Compare route efficiency"),
                outcomes = listOf("Use map scale", "Estimate real-world distances", "Analyze route and travel data"),
                outdoorRequired = true,
                openMapRequired = true,
                modes = setOf(GamePlayerMode.SinglePlayer, GamePlayerMode.Team, GamePlayerMode.OutdoorMap),
                environment = GameEnvironment.Outdoor,
                availability = GameAvailability.Available,
                destination = GameDestination.MathExpedition
            )
        )
    )

    val games: List<GameDefinition> = registeredPlugins.games
    val firstPlayableGame: GameDefinition get() = registeredPlugins.playableGames().first()

    fun requireGame(gameId: String): GameDefinition =
        registeredPlugins.byId(gameId)?.definition ?: error("Unknown game ID: $gameId")

    fun gameOrNull(gameId: String): GameDefinition? =
        registeredPlugins.byId(gameId)?.definition

    private data object MathFortressPlugin : GamePlugin {
        override val definition: GameDefinition = GameDefinition(
            id = "ar-math-arena",
            title = "Math Fortress AR",
            tagline = "Solve. Build. Defend Together.",
            description = "A local Wi-Fi team mathematics strategy game where players solve missions, build defences and protect a shared AR fortress.",
            icon = GameIcon.Fortress,
            heroArtwork = GameHeroArtwork.Fortress,
            playerModes = setOf(GamePlayerMode.Team),
            arRequirement = GameArRequirement.Required,
            localWifiSupported = true,
            outdoorRequired = false,
            openMapRequired = false,
            supportedTopics = listOf("Arithmetic", "Fractions", "Algebra", "Geometry", "Graphing", "Measurement"),
            recommendedGrade = "Grades 6-10",
            capabilities = setOf(
                GameCapability.AugmentedReality,
                GameCapability.LocalWifi,
                GameCapability.TeamGame,
                GameCapability.Mathematics,
                GameCapability.OfflineMatch,
                GameCapability.Diagnostics,
                GameCapability.SharedSpatialOrigin,
                GameCapability.HostAuthoritative,
                GameCapability.SingleDeviceHost,
                GameCapability.OptionalDepth,
                GameCapability.OptionalEnvironmentalHdr
            ),
            availability = GameAvailability.Available,
            destination = GameDestination.ArMathArena,
            recommendedPlayers = "2-6 local players",
            deviceRequirements = listOf("ARCore-capable Android device for active AR roles", "Camera permission", "Same local Wi-Fi for multiplayer"),
            accessibilitySupport = listOf("Large text", "High contrast", "Reduced motion", "Seated play", "Non-AR analyst role"),
            learningOutcomes = listOf("Solve grade-aligned math missions", "Collaborate through team roles", "Connect calculations to AR placement and defence strategy"),
            mainGameLoop = listOf("Host a local room", "Calibrate a shared AR origin", "Solve missions for resources", "Build defences", "Survive waves and boss phases"),
            howToPlay = GameHowToPlay(
                overview = "Work as a team to solve math missions, earn resources and defend your AR fortress.",
                estimatedReadingMinutes = 4,
                quickStartSteps = listOf(
                    "Join a local Wi-Fi room.",
                    "Select a team and role.",
                    "Scan the shared marker.",
                    "Solve missions and build defences.",
                    "Defeat the final boss."
                ),
                learningObjectives = listOf(
                    "Use integers, fractions, algebra, geometry, coordinates and configured trigonometry in live missions.",
                    "Connect correct answers to energy, construction, aiming, damage and shield strength.",
                    "Collaborate through separate roles instead of one player doing everything."
                ),
                playerModes = listOf("Team-based local Wi-Fi match", "Non-AR analyst participation where needed"),
                setupSteps = listOf("Use the same local Wi-Fi", "Host a room", "Join with QR/local discovery", "Choose roles", "Calibrate the shared marker or surface origin"),
                playSteps = listOf(
                    "Host or join a same-Wi-Fi match.",
                    "Select teams and one of the available roles.",
                    "Scan the shared AR marker and calibrate the origin.",
                    "Place or prepare the team fortress.",
                    "Find AR resources and solve mathematics missions.",
                    "Collect energy from correct work.",
                    "Build defensive structures and activate them through correct mathematics.",
                    "Defend against enemy waves and complete the boss battle.",
                    "Reconnect from network loss through the lobby flow when needed.",
                    "Use teacher pause and safety controls if the room becomes unsafe."
                ),
                controls = listOf(
                    "Use Host Game or Join Game from the landing page.",
                    "Use role chips to choose Navigator, Solver, Builder, Analyst or Commander.",
                    "Use Calibrate AR to scan the marker or choose surface fallback.",
                    "Use teacher pause/accessibility controls from the dashboard when supervising."
                ),
                roles = listOf(
                    "Navigator: tracks spatial clues and guides teammates.",
                    "Solver: submits math answers.",
                    "Builder: places and upgrades defences.",
                    "Analyst: checks patterns, hints and non-AR support.",
                    "Commander: coordinates team strategy and timing."
                ),
                scoring = listOf(
                    "Correct answers award energy and score.",
                    "Process, AR accuracy, participation, speed and hint use affect mission scoring.",
                    "Mathematics can control construction, defence activation, aiming, damage and shield strength."
                ),
                winCondition = "Protect the fortress through the match and complete the final boss phase with the highest learning score.",
                safetyNotes = listOf("Keep the play area clear.", "Walk only.", "Use seated or non-AR analyst mode when movement is not safe.", "Teacher can pause or end a match."),
                deviceRequirements = listOf("ARCore-capable device for active AR roles.", "Camera permission.", "Same local Wi-Fi for multiplayer.", "Printed shared marker recommended."),
                accessibilityNotes = listOf("Large text, high contrast, reduced motion, seated play and non-AR analyst support are available in the game module."),
                tutorialAvailability = "Practice is available through Mission Test and Base Defence test controls; the full game flow remains the playable local match."
            ),
            availabilityMessage = "Available now. Existing route and game ID are preserved for saved navigation and protocol compatibility."
        )

        override val runtimeContract: GameRuntimeContract = GameRuntimeContract(
            gameId = definition.id,
            protocolNamespace = "arena.v1.math",
            minPlayers = 2,
            maxPlayers = 6,
            requiresSharedOrigin = true,
            hostAuthoritative = true,
            supportsNonArParticipant = true,
            privacyNotes = listOf(
                "Uses local Wi-Fi sockets only.",
                "Camera frames stay on device.",
                "Room token is temporary and scoped to the LAN match."
            )
        )
    }

    private data class ComingSoonGamePlugin(
        val id: String,
        val title: String,
        val tagline: String,
        val description: String,
        val icon: GameIcon,
        val heroArtwork: GameHeroArtwork,
        val topics: List<String>,
        val loop: List<String>,
        val outcomes: List<String>,
        val outdoorRequired: Boolean = false,
        val openMapRequired: Boolean = false,
        val modes: Set<GamePlayerMode> = setOf(GamePlayerMode.SinglePlayer, GamePlayerMode.Team),
        val environment: GameEnvironment = GameEnvironment.Indoor,
        val availability: GameAvailability = GameAvailability.ComingSoon,
        val destination: GameDestination = GameDestination.ComingSoon
    ) : GamePlugin {
        override val definition: GameDefinition = GameDefinition(
            id = id,
            title = title,
            tagline = tagline,
            description = description,
            icon = icon,
            heroArtwork = heroArtwork,
            playerModes = modes,
            arRequirement = GameArRequirement.Required,
            localWifiSupported = modes.contains(GamePlayerMode.Team),
            outdoorRequired = outdoorRequired,
            openMapRequired = openMapRequired,
            supportedTopics = topics,
            recommendedGrade = "Grades 6-10",
            capabilities = futureCapabilities(id, modes, outdoorRequired, openMapRequired),
            availability = availability,
            destination = destination,
            recommendedPlayers = if (modes.contains(GamePlayerMode.Team)) "Solo or team" else "Solo",
            deviceRequirements = buildList {
                add(if (availability == GameAvailability.Available) "ARCore-capable Android device recommended" else "ARCore-capable Android device planned")
                add(if (availability == GameAvailability.Available) "Camera permission" else "Camera permission planned")
                if (modes.contains(GamePlayerMode.Team)) add(if (availability == GameAvailability.Available) "Single-device team play active; same local Wi-Fi sync is reserved for the next multiplayer pass" else "Same local Wi-Fi planned for team mode")
                if (outdoorRequired) add("Outdoor-safe location and device location support planned")
                if (openMapRequired) add("OpenStreetMap-compatible map rendering planned")
            },
            accessibilitySupport = if (availability == GameAvailability.Available) {
                listOf("Large text", "High contrast surfaces", "Reduced motion friendly controls", "Seated tabletop play")
            } else {
                listOf("Large text planned", "High contrast planned", "Reduced motion planned", "Seated alternatives planned")
            },
            learningOutcomes = outcomes,
            mainGameLoop = loop,
            howToPlay = futureHowToPlay(id, title, loop, outcomes, topics, outdoorRequired, openMapRequired, availability),
            availabilityMessage = if (availability == GameAvailability.Available) {
                "Available now as a playable indoor AR maths game module."
            } else {
                "Coming soon. Details and How to Play are available; gameplay is not implemented yet."
            }
        )

        override val runtimeContract: GameRuntimeContract = GameRuntimeContract(
            gameId = id,
            protocolNamespace = "future.${id.replace("_", ".")}",
            minPlayers = 1,
            maxPlayers = 6,
            requiresSharedOrigin = true,
            hostAuthoritative = false,
            supportsNonArParticipant = true,
            privacyNotes = listOf("Catalog and information page only; no runtime transport is active.")
        )
    }

    private fun futureHowToPlay(
        id: String,
        title: String,
        loop: List<String>,
        outcomes: List<String>,
        topics: List<String>,
        outdoorRequired: Boolean,
        openMapRequired: Boolean,
        availability: GameAvailability
    ): GameHowToPlay = when (id) {
        "equation_escape_ar" -> GameHowToPlay(
            overview = "The physical room becomes an augmented-reality escape room containing mathematical locks, hidden clues, number machines and geometry puzzles.",
            estimatedReadingMinutes = 3,
            quickStartSteps = listOf("Scan the room.", "Find an AR clue.", "Solve the lock.", "Follow the next clue.", "Complete the final escape puzzle."),
            learningObjectives = listOf("Solve arithmetic and algebra challenges.", "Use patterns and logic to connect clues.", "Apply coordinate geometry to locate hidden information."),
            playerModes = listOf("Single Player", "Cooperative Team", "Timed Classroom Challenge"),
            setupSteps = listOf("Choose a safe room.", "Clear the walking path.", "Start the escape scenario when released.", "Share clue information across devices in team mode."),
            playSteps = listOf("Scan the room.", "Locate an AR clue.", "Solve the mathematical challenge.", "Unlock a door, key or mechanism.", "Follow the new clue.", "Complete the final escape puzzle."),
            controls = listOf("Tap clues to inspect them.", "Enter answers into lock panels.", "Use hint controls when enabled.", "Move slowly while scanning."),
            roles = listOf("Clue Finder: searches the room.", "Equation Solver: solves locks.", "Pattern Analyst: connects clue sequences.", "Coordinator: manages time."),
            scoring = listOf("Correct answers unlock progress.", "Fast, accurate solutions improve the escape rating.", "Hints may reduce bonus points."),
            winCondition = if (availability == GameAvailability.Available) "Solve the final escape puzzle and open every room lock with the highest score." else "Coming soon. The intended win condition is to solve the final escape puzzle before time runs out.",
            safetyNotes = listOf("Walk only.", "Keep the room clear.", "Do not crawl under furniture or move objects unless an adult approves."),
            deviceRequirements = if (availability == GameAvailability.Available) listOf("ARCore-capable Android device recommended.", "Camera permission.", "Playable local content with AR placement labels.") else listOf("ARCore-capable Android device planned.", "Camera permission planned.", "Local-only content planned."),
            accessibilityNotes = if (availability == GameAvailability.Available) listOf("Large text, high contrast clue cards and untimed play are active.") else listOf("Large text, high contrast clues and untimed practice are planned."),
            tutorialAvailability = if (availability == GameAvailability.Available) "Playable now. Use hints and Load Room progression to practice clue chains." else "Coming soon. This build includes instructions only; no active start action is provided."
        )
        "geometry_architect_ar" -> GameHowToPlay(
            overview = "Students design mathematically valid AR structures on a desk, floor or classroom surface.",
            estimatedReadingMinutes = 3,
            quickStartSteps = listOf("Read the design brief.", "Scan a surface.", "Place vertices.", "Construct a shape.", "Verify the measurements."),
            learningObjectives = listOf("Apply area, perimeter, surface area and volume.", "Use scale and transformations.", "Improve spatial reasoning through construction constraints."),
            playerModes = listOf("Single Designer", "Collaborative Team", "Classroom Design Competition"),
            setupSteps = listOf("Choose a desk, floor or classroom surface.", "Review the project brief.", "Prepare measuring tools when released.", "Choose a project such as bridge, tower, park, classroom layout or geometric city."),
            playSteps = listOf("Read the design brief.", "Scan and select a building surface.", "Measure or enter dimensions.", "Place vertices and shapes.", "Construct the structure.", "Verify mathematical conditions.", "Improve efficiency and stability.", "Submit the design."),
            controls = listOf("Tap to place vertices.", "Drag handles to resize.", "Use measurement cards to check dimensions.", "Use verify to test constraints."),
            roles = listOf("Designer: plans the structure.", "Measurer: checks dimensions.", "Builder: places parts.", "Reviewer: verifies conditions."),
            scoring = listOf("Designs score for correctness, efficiency and meeting constraints.", "Better scale and measurement accuracy improves results."),
            winCondition = if (availability == GameAvailability.Available) "Submit valid designs that satisfy area, volume and material constraints across the brief set." else "Coming soon. The intended goal is to submit a valid, efficient design that satisfies the brief.",
            safetyNotes = listOf("Use a stable surface.", "Keep devices away from table edges.", "Do not walk backward while viewing AR."),
            deviceRequirements = if (availability == GameAvailability.Available) listOf("ARCore-capable Android device recommended.", "Camera permission.", "Surface/grid placement flow in the game module.") else listOf("ARCore-capable Android device planned.", "Camera permission planned.", "Surface tracking planned."),
            accessibilityNotes = if (availability == GameAvailability.Available) listOf("Large labels, high contrast cards and seated tabletop play are active.") else listOf("Large labels, high contrast edges and seated play are planned."),
            tutorialAvailability = if (availability == GameAvailability.Available) "Playable now. Load Blueprint can be used as a guided sample before manual construction." else "Coming soon. Sample project instructions are included; construction gameplay is not active."
        )
        "fraction_factory_ar" -> GameHowToPlay(
            overview = "An AR factory appears on a table. Players combine quantities, operate machines and complete production orders using fractions, decimals, ratios and percentages.",
            estimatedReadingMinutes = 3,
            quickStartSteps = listOf("Receive an order.", "Choose ingredients.", "Calculate quantities.", "Run the machine.", "Inspect the product."),
            learningObjectives = listOf("Convert between fractions, decimals and percentages.", "Use ratios and proportions.", "Apply unit conversion to production orders."),
            playerModes = listOf("Single Factory Operator", "Team Production Line", "Timed Order Challenge"),
            setupSteps = listOf("Choose a clear table.", "Review the production order.", "Assign team jobs when released.", "Prepare to compare equivalent values."),
            playSteps = listOf("Receive a production order.", "Select containers or ingredients.", "Calculate the required quantity.", "Operate the correct machine.", "Inspect the result.", "Correct errors where necessary.", "Package the final product."),
            controls = listOf("Tap containers to select quantities.", "Use machine controls to combine or convert.", "Inspect output cards for errors.", "Package only verified results."),
            roles = listOf("Ingredient Collector: chooses quantities.", "Calculator: computes conversions.", "Machine Operator: runs tools.", "Quality Inspector: checks results.", "Production Manager: watches time."),
            scoring = listOf("Accurate products earn order points.", "Equivalent forms and unit conversions must match.", "Corrections may reduce bonus points but improve learning feedback."),
            winCondition = if (availability == GameAvailability.Available) "Complete as many production orders as possible with correct equivalent quantities and units." else "Coming soon. The intended goal is to complete production orders accurately before time expires.",
            safetyNotes = listOf("Use a clear tabletop.", "Keep hands and devices steady.", "No real liquids or materials are required."),
            deviceRequirements = if (availability == GameAvailability.Available) listOf("ARCore-capable Android device recommended.", "Camera permission.", "Tabletop factory placement flow in the game module.") else listOf("ARCore-capable Android device planned.", "Camera permission planned.", "Table surface tracking planned."),
            accessibilityNotes = if (availability == GameAvailability.Available) listOf("Large numeric labels, color-independent quantities and untimed practice are active.") else listOf("Large numeric labels, color-independent ingredient symbols and untimed practice are planned."),
            tutorialAvailability = if (availability == GameAvailability.Available) "Playable now. Load Sample teaches the inspection criteria before manual orders." else "Coming soon. Instructions explain the factory loop; gameplay is not active."
        )
            "coordinate_conquest_ar" -> GameHowToPlay(
            overview = "An AR coordinate grid is placed across a classroom floor, hall, playground or tabletop. Players plot points, create lines, transform shapes and capture mathematical territories.",
            estimatedReadingMinutes = 3,
            quickStartSteps = listOf("Calibrate the shared marker.", "Confirm the safe boundary.", "Choose physical or tabletop mode.", "Complete coordinate missions.", "Capture zones with correct math."),
            learningObjectives = listOf("Use number lines, quadrants and ordered pairs.", "Calculate distance, midpoint, slope, line equations and intersections.", "Apply vectors, translations, reflections, rotations and enlargements."),
            playerModes = listOf("Solo Coordinate Training", "Solo Mission Campaign", "Team Territory Capture", "Cooperative Grid Defence", "Vector Relay", "Transformation Battle", "Teacher Challenge Mode", "Seated Tabletop Mode"),
            setupSteps = listOf("Choose a clear floor, hall, playground or table.", "Scan the shared marker.", "Select grid size and unit scale.", "Confirm axis orientation.", "Confirm the physical boundary before movement play starts."),
            playSteps = listOf("Receive a coordinate mission.", "Move physically or place a tabletop beacon.", "Submit point, line or transformation results.", "Host validates math and stable placement.", "Capture or defend a zone.", "Rotate roles between rounds.", "Complete the solo chapter or team match."),
            controls = listOf("Use mode chips for training, campaign, territory, defence, relay, battle, teacher or seated play.", "Toggle physical/tabletop interaction.", "Enter coordinates, slope/intercept or use worked solution practice.", "Use host pause whenever the area becomes unsafe."),
            roles = listOf("Navigator: sees regions and safe movement cues.", "Plotter: places beacons.", "Analyst: calculates coordinates/formulas.", "Vector Controller: applies vector steps.", "Strategist: chooses capture zones.", "Verifier: confirms mathematical results."),
            scoring = listOf("Scores combine coordinate accuracy, math correctness, time, hint use, participation, stable placement, transformations, territory difficulty and explanation quality.", "Tabletop mode receives the same academic score as physical movement.", "Clients cannot assign ownership locally; host validation controls capture."),
            winCondition = "Complete the solo chapter, finish a vector/transformation mission chain, or win a timed host-authoritative territory match by score.",
            safetyNotes = listOf("Walk only.", "Confirm the safe boundary before physical play.", "Use tabletop mode for seated or low-movement play.", "Stop if the marker is lost, tracking weakens, or a player approaches the boundary.", "Human supervision is still required."),
            deviceRequirements = listOf("ARCore-capable Android device recommended.", "Camera permission.", "Printed shared marker recommended.", "Same local Wi-Fi can carry authoritative definitions for team play."),
            accessibilityNotes = listOf("Tabletop mode, seated mode, non-AR Analyst role, large labels, high contrast, colour-independent teams, reduced motion, audio coordinate announcements, haptic axis crossing, extended time and one-handed placement are supported in the game model."),
            tutorialAvailability = "Playable now. Five solo chapters and all eight Coordinate Conquest modes are available from the game screen."
        )
        "math_expedition_ar" -> GameHowToPlay(
            overview = "A teacher-approved open-map route becomes a mathematics expedition. Players navigate safe checkpoints, solve map mathematics, complete optional AR tasks and review local analytics.",
            estimatedReadingMinutes = 4,
            quickStartSteps = listOf("Open or author a teacher-approved route.", "Review boundaries and no-go zones.", "Start foreground location only for real play.", "Solve checkpoint mathematics.", "Complete the final expedition analysis."),
            learningObjectives = listOf("Use distance, scale, bearing and coordinates.", "Connect speed, time, route optimisation and percentages.", "Collect and analyze real expedition data safely."),
            playerModes = listOf("Solo Explorer", "Team Expedition", "Campus Survey", "Mathematics Treasure Route", "Route Optimisation Challenge", "Statistics Field Mission", "Teacher-Led Class Expedition", "Indoor Campus Map Mode"),
            setupSteps = listOf("Choose an OpenStreetMap-compatible provider or teacher campus map.", "Author/check a route.", "Validate boundaries and no-go zones.", "Approve the route for play.", "Confirm adult supervision and safe weather/road conditions."),
            playSteps = listOf("Open the route.", "Review attribution, boundary and offline status.", "Request foreground location only when starting real play.", "Navigate to a checkpoint.", "Solve the map-based mission.", "Verify checkpoint with location accuracy, radius and dwell time.", "Use optional AR mission or non-AR fallback.", "Finish with analytics."),
            controls = listOf("Use mode chips for solo, team, survey, treasure, route, statistics, teacher or indoor play.", "Use permission explanation before location.", "Use checkpoint verification only with live location or teacher indoor confirmation.", "Use emergency stop and return-to-start if unsafe."),
            roles = listOf("Navigator: follows the route.", "Distance Analyst: checks distances and scale.", "Bearing Specialist: checks directions.", "Data Collector: records observations.", "AR Solver: handles optional AR tasks.", "Safety Captain: watches safety prompts without replacing adult supervision."),
            scoring = listOf("Accurate missions, verified checkpoints, estimates, AR/non-AR tasks and final analysis build score.", "Location speed or boundary warnings pause progression.", "Analytics summarize learning without public ranking by speed or location."),
            winCondition = "Complete the approved route checkpoints and final analysis while staying inside safety rules.",
            safetyNotes = listOf("Teacher-approved routes only.", "Walking mode only.", "No gameplay while crossing roads.", "Respect no-go zones.", "Stay inside the safe boundary.", "Adult supervision required.", "Stop when surroundings become unsafe.", "Use emergency stop and return-to-start when needed."),
            deviceRequirements = listOf("Foreground location permission for real outdoor play.", "OpenStreetMap-compatible provider configuration.", "Camera permission only for optional AR checkpoint missions.", "Offline route content works locally; offline map availability depends on provider support.", "ARCore Geospatial is optional and not required."),
            accessibilityNotes = listOf("Indoor campus mode, teacher confirmation, non-walking roles, large cards and non-AR equivalents for optional AR missions are supported."),
            tutorialAvailability = "Playable now. Five adaptable expedition templates and eight expedition modes are available from the game screen."
        )
        else -> GameHowToPlay(
            overview = "$title is a planned AR math game. This page explains the intended learning loop without claiming gameplay is available.",
            estimatedReadingMinutes = 3,
            quickStartSteps = loop.take(5),
            learningObjectives = outcomes,
            playerModes = listOf("Single Player", "Team Game"),
            setupSteps = listOf("Review device requirements.", "Prepare a clear play area.", "Wait for gameplay implementation."),
            playSteps = loop,
            controls = listOf("Controls will be verified when gameplay is implemented."),
            roles = listOf("Roles will be finalized during implementation."),
            scoring = listOf("Scoring will be verified when gameplay is implemented."),
            winCondition = "Coming soon. Final win conditions will be verified when gameplay is implemented.",
            safetyNotes = buildList {
                add("No live gameplay is available yet.")
                if (outdoorRequired) add("Outdoor play requires approved safe routes.")
                if (openMapRequired) add("Open-map features are not implemented in this build.")
            },
            deviceRequirements = listOf("ARCore-capable Android device planned."),
            accessibilityNotes = listOf("Accessible instructions are available; gameplay accessibility will be verified during implementation."),
            tutorialAvailability = "Coming soon."
        )
    }

    private fun futureCapabilities(
        id: String,
        modes: Set<GamePlayerMode>,
        outdoorRequired: Boolean,
        openMapRequired: Boolean
    ): Set<GameCapability> = buildSet {
        add(GameCapability.Mathematics)
        add(GameCapability.SinglePlayer)
        if (modes.contains(GamePlayerMode.Team)) {
            add(GameCapability.TeamGame)
            add(GameCapability.LocalWifi)
        }
        when (id) {
            "equation_escape_ar" -> {
                add(GameCapability.AugmentedReality)
                add(GameCapability.MarkerInteractions)
                add(GameCapability.PuzzleSequencing)
            }
            "geometry_architect_ar" -> {
                add(GameCapability.AugmentedReality)
                add(GameCapability.PlaneDetection)
                add(GameCapability.Measurement)
                add(GameCapability.OptionalDepth)
            }
            "fraction_factory_ar" -> {
                add(GameCapability.AugmentedReality)
                add(GameCapability.PlaneDetection)
                add(GameCapability.ObjectManipulation)
            }
            "coordinate_conquest_ar" -> {
                add(GameCapability.AugmentedReality)
                add(GameCapability.SharedSpatialOrigin)
                add(GameCapability.FloorPlane)
                add(GameCapability.SafeMovementArea)
            }
            "math_expedition_ar" -> {
                add(GameCapability.Outdoor)
                add(GameCapability.OpenMap)
                add(GameCapability.FutureLocationPermission)
                add(GameCapability.OfflineMaps)
                add(GameCapability.TeacherApprovedRoute)
                add(GameCapability.AugmentedReality)
            }
        }
        if (outdoorRequired) add(GameCapability.Outdoor)
        if (openMapRequired) add(GameCapability.OpenMap)
    }
}
