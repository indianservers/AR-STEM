package com.indianservers.ai_stem.feature.games.arcore

enum class ArAvailabilityResult {
    Checking,
    SupportedReady,
    SupportedInstallRequired,
    SupportedUpdateRequired,
    CameraPermissionRequired,
    Unsupported,
    TemporarilyUnavailable,
    Failure
}

enum class ArGameCapabilityRole {
    RequiredForBaseGameplay,
    OptionalEnhancement,
    Unsupported,
    FutureOutdoorFeature,
    CloudDependentFeature
}

enum class ArGameFeature {
    MotionTracking,
    HorizontalPlaneDetection,
    VerticalPlaneDetection,
    Anchors,
    HitTesting,
    InstantPlacement,
    AugmentedImages,
    DepthApi,
    RawDepth,
    DepthOcclusion,
    EnvironmentalHdr,
    CameraFlash,
    RecordingPlayback,
    CloudAnchors,
    Geospatial,
    StreetscapeGeometry,
    SceneSemantics
}

data class ArFeatureCapability(
    val feature: ArGameFeature,
    val role: ArGameCapabilityRole,
    val available: Boolean,
    val note: String
)

data class ArGameCapabilityMatrix(
    val availability: ArAvailabilityResult,
    val features: List<ArFeatureCapability>
) {
    val baseGameplayReady: Boolean
        get() = availability == ArAvailabilityResult.SupportedReady &&
            features.filter { it.role == ArGameCapabilityRole.RequiredForBaseGameplay }.all { it.available }
}

object ArMathArenaCapabilityPolicy {
    fun matrix(
        availability: ArAvailabilityResult,
        depthSupported: Boolean = false,
        rawDepthSupported: Boolean = false,
        environmentalHdrSupported: Boolean = true,
        instantPlacementSupported: Boolean = true,
        flashSupported: Boolean = false,
        recordingPlaybackSupported: Boolean = true,
        augmentedImagesSupported: Boolean = true,
        sceneSemanticsSupported: Boolean = false
    ): ArGameCapabilityMatrix {
        val arReady = availability == ArAvailabilityResult.SupportedReady
        return ArGameCapabilityMatrix(
            availability = availability,
            features = listOf(
                required(ArGameFeature.MotionTracking, arReady, "Tracks device movement for arena placement."),
                required(ArGameFeature.HorizontalPlaneDetection, arReady, "Finds floors and tables for the shared base."),
                required(ArGameFeature.VerticalPlaneDetection, arReady, "Useful for walls and classroom boards."),
                required(ArGameFeature.Anchors, arReady, "Keeps game objects fixed in space."),
                required(ArGameFeature.HitTesting, arReady, "Lets players tap surfaces and markers."),
                required(ArGameFeature.AugmentedImages, arReady && augmentedImagesSupported, "Shared marker tracking for local teams."),
                optional(ArGameFeature.InstantPlacement, arReady && instantPlacementSupported, "Speeds up first placement but is not forced."),
                optional(ArGameFeature.DepthApi, arReady && depthSupported, "Improves realism on supported devices."),
                optional(ArGameFeature.RawDepth, arReady && rawDepthSupported, "Advanced depth data when available."),
                optional(ArGameFeature.DepthOcclusion, arReady && depthSupported, "Lets arena objects sit behind real surfaces."),
                optional(ArGameFeature.EnvironmentalHdr, arReady && environmentalHdrSupported, "Better lighting for AR objects."),
                optional(ArGameFeature.CameraFlash, flashSupported, "Optional low-light classroom helper."),
                optional(ArGameFeature.RecordingPlayback, arReady && recordingPlaybackSupported, "Useful for debugging matches."),
                cloud(ArGameFeature.CloudAnchors, false, "Disabled by default for classroom LAN gameplay."),
                outdoor(ArGameFeature.Geospatial, false, "Reserved for a future outdoor game mode."),
                outdoor(ArGameFeature.StreetscapeGeometry, false, "Reserved for future outdoor geometry games."),
                unsupported(ArGameFeature.SceneSemantics, sceneSemanticsSupported, "Not required for standard arena matches.")
            )
        )
    }

    private fun required(feature: ArGameFeature, available: Boolean, note: String) =
        ArFeatureCapability(feature, ArGameCapabilityRole.RequiredForBaseGameplay, available, note)

    private fun optional(feature: ArGameFeature, available: Boolean, note: String) =
        ArFeatureCapability(feature, ArGameCapabilityRole.OptionalEnhancement, available, note)

    private fun cloud(feature: ArGameFeature, available: Boolean, note: String) =
        ArFeatureCapability(feature, ArGameCapabilityRole.CloudDependentFeature, available, note)

    private fun outdoor(feature: ArGameFeature, available: Boolean, note: String) =
        ArFeatureCapability(feature, ArGameCapabilityRole.FutureOutdoorFeature, available, note)

    private fun unsupported(feature: ArGameFeature, available: Boolean, note: String) =
        ArFeatureCapability(feature, if (available) ArGameCapabilityRole.OptionalEnhancement else ArGameCapabilityRole.Unsupported, available, note)
}
