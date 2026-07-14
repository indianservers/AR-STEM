package com.indianservers.ai_stem.domain.graph

enum class ArAdvancedToolKind {
    ParametricCurve,
    ImplicitRelation,
    InequalityRegion,
    LocusTrace,
    MacroConstruction,
    ProofCheck,
    CasCommand,
    MultiGraphIntersection
}

data class ArAdvancedMathTool(
    val id: String,
    val title: String,
    val kind: ArAdvancedToolKind,
    val equationInsert: String,
    val arHint: String,
    val evidenceLabel: String
)

data class ArAdvancedCapabilityReport(
    val supportedTools: List<ArAdvancedMathTool>,
    val missingWorkflowHints: List<String>,
    val strengthScore: Int
)

object ArAdvancedMathTools {
    val tools: List<ArAdvancedMathTool> = listOf(
        ArAdvancedMathTool(
            id = "parametric-orbit",
            title = "Parametric Curve",
            kind = ArAdvancedToolKind.ParametricCurve,
            equationInsert = "x = a*cos(t), y = b*sin(t)",
            arHint = "Place a traced parametric curve in AR and animate t.",
            evidenceLabel = "Parametric AR trace"
        ),
        ArAdvancedMathTool(
            id = "implicit-circle",
            title = "Implicit Relation",
            kind = ArAdvancedToolKind.ImplicitRelation,
            equationInsert = "x^2 + y^2 = a^2",
            arHint = "Approximate an implicit curve as sampled AR points.",
            evidenceLabel = "Implicit sample cloud"
        ),
        ArAdvancedMathTool(
            id = "inequality-region",
            title = "Inequality Region",
            kind = ArAdvancedToolKind.InequalityRegion,
            equationInsert = "y <= a*x + b",
            arHint = "Shade the feasible region on a surface or paper graph.",
            evidenceLabel = "AR inequality region"
        ),
        ArAdvancedMathTool(
            id = "locus-trace",
            title = "Locus / Trace",
            kind = ArAdvancedToolKind.LocusTrace,
            equationInsert = "y = a*sin(b*x + c)",
            arHint = "Record moving points as an AR trace while sliders animate.",
            evidenceLabel = "Trace path recorded"
        ),
        ArAdvancedMathTool(
            id = "macro-plane",
            title = "Macro Construction",
            kind = ArAdvancedToolKind.MacroConstruction,
            equationInsert = "z = x + y",
            arHint = "Create a reusable AR construction from points, vectors and planes.",
            evidenceLabel = "Custom AR tool"
        ),
        ArAdvancedMathTool(
            id = "proof-check",
            title = "Proof / Check",
            kind = ArAdvancedToolKind.ProofCheck,
            equationInsert = "y = x^2",
            arHint = "Check roots, tangent, area and construction dependencies as evidence.",
            evidenceLabel = "AR proof evidence"
        ),
        ArAdvancedMathTool(
            id = "cas-derivative",
            title = "CAS Command",
            kind = ArAdvancedToolKind.CasCommand,
            equationInsert = "y = a*x^3 + b*x^2 + c*x + d",
            arHint = "Use numerical derivative, integral and extrema cards in AR.",
            evidenceLabel = "CAS-style analysis card"
        ),
        ArAdvancedMathTool(
            id = "multi-intersection",
            title = "Multi-Graph Intersections",
            kind = ArAdvancedToolKind.MultiGraphIntersection,
            equationInsert = "y = sin(x)",
            arHint = "Compare two live AR graphs and mark intersections.",
            evidenceLabel = "Intersection markers"
        )
    )

    fun evaluate(
        equationKind: GraphExpressionKind,
        hasAnalysis: Boolean,
        constructionCount: Int,
        pickedPointCount: Int,
        exported: Boolean
    ): ArAdvancedCapabilityReport {
        val supported = tools.filter { tool ->
            when (tool.kind) {
                ArAdvancedToolKind.ParametricCurve -> equationKind == GraphExpressionKind.Parametric2D
                ArAdvancedToolKind.ImplicitRelation -> equationKind == GraphExpressionKind.Implicit2D
                ArAdvancedToolKind.InequalityRegion -> equationKind == GraphExpressionKind.Inequality2D
                ArAdvancedToolKind.LocusTrace -> pickedPointCount > 0 || hasAnalysis
                ArAdvancedToolKind.MacroConstruction -> constructionCount > 0
                ArAdvancedToolKind.ProofCheck -> hasAnalysis && constructionCount > 0
                ArAdvancedToolKind.CasCommand -> hasAnalysis
                ArAdvancedToolKind.MultiGraphIntersection -> hasAnalysis
            }
        }
        val hints = buildList {
            if (equationKind !in setOf(GraphExpressionKind.Parametric2D, GraphExpressionKind.Implicit2D, GraphExpressionKind.Inequality2D)) {
                add("Add parametric, implicit or inequality relations for stronger GeoGebra parity.")
            }
            if (constructionCount == 0) add("Create construction objects to unlock macro/proof workflows.")
            if (!hasAnalysis) add("Run graph analysis to unlock CAS-style evidence cards.")
            if (!exported) add("Export an activity pack to make the AR work shareable.")
        }
        val score = (supported.size * 100 / tools.size + if (exported) 10 else 0).coerceIn(0, 100)
        return ArAdvancedCapabilityReport(supported, hints, score)
    }
}
