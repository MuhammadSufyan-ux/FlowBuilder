package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "flow_projects")
data class FlowProjectEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val category: String,
    val graphJson: String,
    val createdAt: Long,
    val updatedAt: Long,
    val isFavorite: Boolean = false,
)
