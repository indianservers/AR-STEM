package com.indianservers.ai_stem

import com.indianservers.ai_stem.data.project.ProjectArchiveCodec
import com.indianservers.ai_stem.data.project.ProjectArchiveManifest
import com.indianservers.ai_stem.data.project.ProjectArchivePayload
import com.indianservers.ai_stem.data.project.ProjectArchiveResult
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProjectArchiveCodecTest {
    @Test
    fun archiveRoundTripPreservesManifestAndSections() {
        val codec = ProjectArchiveCodec()
        val exported = codec.export(
            ProjectArchivePayload(
                manifest = ProjectArchiveManifest(1, "project-1", "Unit Circle", setOf("graph", "tables")),
                projectJson = """{"id":"project-1","name":"Unit Circle"}""",
                graphJson = """{"expressions":["x^2+y^2=1"]}""",
                tables = mapOf("points" to "x,y\n0,1")
            )
        )

        val imported = codec.import(exported) as ProjectArchiveResult.Success

        assertEquals("project-1", imported.payload.manifest.projectId)
        assertEquals("Unit Circle", imported.payload.manifest.projectName)
        assertEquals("""{"expressions":["x^2+y^2=1"]}""", imported.payload.graphJson)
        assertEquals("x,y\n0,1", imported.payload.tables["points"])
    }

    @Test
    fun importRejectsZipTraversal() {
        val bytes = ByteArrayOutputStream().use { bytes ->
            ZipOutputStream(bytes).use { zip ->
                zip.putNextEntry(ZipEntry("../project.json"))
                zip.write("{}".encodeToByteArray())
                zip.closeEntry()
            }
            bytes.toByteArray()
        }

        val result = ProjectArchiveCodec().import(bytes)

        assertTrue(result is ProjectArchiveResult.Failure)
    }
}
