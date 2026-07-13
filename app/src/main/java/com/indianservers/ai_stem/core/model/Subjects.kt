package com.indianservers.ai_stem.core.model

enum class StemSubject(
    val title: String,
    val description: String,
    val enabled: Boolean
) {
    Mathematics("Mathematics", "Graphs, shapes, solids and mathematical models", true),
    Physics("Physics", "Forces, motion, electricity and physical systems", false),
    Chemistry("Chemistry", "Atoms, molecules, reactions and laboratory models", false),
    Biology("Biology", "Cells, anatomy, ecosystems and biological structures", false)
}

fun StemSubject.disabledMessage(): String =
    if (enabled) "" else "This STEM subject will be added in a future phase."
