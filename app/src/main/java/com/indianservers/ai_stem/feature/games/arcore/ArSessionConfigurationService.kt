package com.indianservers.ai_stem.feature.games.arcore

import com.google.ar.core.Config
import com.google.ar.core.Session

class ArSessionConfigurationService {
    fun configureForMathArena(session: Session, config: Config, matrix: ArGameCapabilityMatrix): Config {
        config.planeFindingMode = Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL
        config.lightEstimationMode = if (matrix.available(ArGameFeature.EnvironmentalHdr)) {
            Config.LightEstimationMode.ENVIRONMENTAL_HDR
        } else {
            Config.LightEstimationMode.AMBIENT_INTENSITY
        }
        config.depthMode = if (matrix.available(ArGameFeature.DepthApi) && session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
            Config.DepthMode.AUTOMATIC
        } else {
            Config.DepthMode.DISABLED
        }
        config.instantPlacementMode = if (matrix.available(ArGameFeature.InstantPlacement)) {
            Config.InstantPlacementMode.LOCAL_Y_UP
        } else {
            Config.InstantPlacementMode.DISABLED
        }
        config.cloudAnchorMode = Config.CloudAnchorMode.DISABLED
        config.geospatialMode = Config.GeospatialMode.DISABLED
        config.streetscapeGeometryMode = Config.StreetscapeGeometryMode.DISABLED
        config.semanticMode = Config.SemanticMode.DISABLED
        return config
    }

    private fun ArGameCapabilityMatrix.available(feature: ArGameFeature): Boolean =
        features.firstOrNull { it.feature == feature }?.available == true
}
