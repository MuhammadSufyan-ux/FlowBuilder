package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FlowProjectDao {
    @Query("SELECT * FROM flow_projects ORDER BY updatedAt DESC")
    fun observeAllProjects(): Flow<List<FlowProjectEntity>>

    @Query("SELECT * FROM flow_projects WHERE id = :id LIMIT 1")
    suspend fun getProjectById(id: String): FlowProjectEntity?

    @Query("SELECT COUNT(*) FROM flow_projects")
    suspend fun getProjectCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProject(project: FlowProjectEntity)

    @Query("UPDATE flow_projects SET title = :title, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateTitle(id: String, title: String, updatedAt: Long)

    @Query("UPDATE flow_projects SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavorite(id: String, isFavorite: Boolean)

    @Query("DELETE FROM flow_projects WHERE id = :id")
    suspend fun deleteProjectById(id: String)
}
