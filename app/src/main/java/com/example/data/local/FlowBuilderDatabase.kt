package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [FlowProjectEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class FlowBuilderDatabase : RoomDatabase() {
    abstract fun flowProjectDao(): FlowProjectDao

    companion object {
        @Volatile
        private var INSTANCE: FlowBuilderDatabase? = null

        fun getInstance(context: Context): FlowBuilderDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    FlowBuilderDatabase::class.java,
                    "flowbuilder_offline.db",
                ).fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
