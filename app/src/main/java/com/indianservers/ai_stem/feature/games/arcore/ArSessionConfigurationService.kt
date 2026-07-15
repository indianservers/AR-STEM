package com.indianservers.ai_stem.feature.games.arcore

import com.google.ar.core.Config
import com.google.ar.core.Session

class ArSessionConfigurationService {
    private val commonSurfaceDetector = ArCoreCommonSurfaceDetector()

    fun configureForMathArena(session: Session, config: Config, matrix: ArGameCapabilityMatrix): Config {
        commonSurfaceDetector.configureMarkerlessSurfaceSession(
            session = session,
            config = config,
            instantPreview = matrix.available(ArGameFeature.InstantPlacement),
            preferDepth = matrix.available(ArGameFeature.DepthApi)
        )
        config.lightEstimationMode = if (matrix.available(ArGameFeature.EnvironmentalHdr)) {
            Config.LightEstimationMode.ENVIRONMENTAL_HDR
        } else {
            Config.LightEstimationMode.AMBIENT_INTENSITY
        }
        config.geospatialMode = Config.GeospatialMode.DISABLED
        config.streetscapeGeometryMode = Config.StreetscapeGeometryMode.DISABLED
        config.semanticMode = Config.SemanticMode.DISABLED
        return config
    }

    private fun ArGameCapabilityMatrix.available(feature: ArGameFeature): Boolean =
        features.firstOrNull { it.feature == feature }?.available == true
}
