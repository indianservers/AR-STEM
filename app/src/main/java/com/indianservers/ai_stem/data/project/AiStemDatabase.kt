package com.indianservers.ai_stem.data.project

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ProjectEntity::class,
        SceneEntity::class,
        SceneObjectEntity::class,
        GraphProjectEntity::class,
        GraphExpressionEntity::class,
        GraphSliderEntity::class,
        DataTableEntity::class,
        GeometryObjectEntity::class,
        ConstructionDependencyEntity::class,
        SceneGroupEntity::class,
        AnnotationEntity::class,
        MeasurementEntity::class,
        AppearanceEntity::class,
        ViewportEntity::class,
        ArPlacementEntity::class,
        ProjectThumbnailEntity::class,
        DeletedProjectEntity::class,
        SchemaMetadataEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AiStemDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao

    companion object {
        @Volatile private var instance: AiStemDatabase? = null

        fun getInstance(context: Context): AiStemDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AiStemDatabase::class.java,
                    "ai-stem.db"
                ).build().also { instance = it }
            }
    }
}
