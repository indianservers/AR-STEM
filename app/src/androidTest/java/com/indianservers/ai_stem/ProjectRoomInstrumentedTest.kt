package com.indianservers.ai_stem

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.indianservers.ai_stem.data.project.AiStemDatabase
import com.indianservers.ai_stem.data.project.ProjectSort
import com.indianservers.ai_stem.data.project.ProjectType
import com.indianservers.ai_stem.data.project.RoomProjectRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ProjectRoomInstrumentedTest {
    private lateinit var database: AiStemDatabase
    private lateinit var repository: RoomProjectRepository

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AiStemDatabase::class.java
        ).build()
        repository = RoomProjectRepository(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun createSearchFavouriteDeleteAndRestoreProject() = runBlocking {
        val project = repository.create("Unit Circle", ProjectType.Graph)
        repository.favourite(project.id, true)

        val searched = repository.search("Circle").first()
        assertEquals(1, searched.size)
        assertTrue(searched.single().isFavourite)

        repository.delete(project.id)
        assertTrue(repository.observeRecent().first().isEmpty())
        assertEquals(1, repository.observeDeleted().first().size)

        repository.restore(project.id)
        assertEquals(project.id, repository.observeRecent().first().single().id)
        assertTrue(repository.observeDeleted().first().isEmpty())
    }

    @Test
    fun saveAsCreatesIndependentProjectCopy() = runBlocking {
        val project = repository.create("Original", ProjectType.Mixed)
        val copy = repository.saveAs(project.id, "Copied Project")

        assertNotNull(copy)
        assertNotEquals(project.id, copy!!.id)
        assertEquals("Copied Project", copy.name)
        assertEquals(2, repository.sort(ProjectSort.Name).first().size)
    }
}
