package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkflowDao {
    @Query("SELECT * FROM workflows ORDER BY createdAt DESC")
    fun getAllWorkflows(): Flow<List<Workflow>>

    @Query("SELECT * FROM workflows WHERE id = :id LIMIT 1")
    suspend fun getWorkflowById(id: Long): Workflow?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkflow(workflow: Workflow): Long

    @Update
    suspend fun updateWorkflow(workflow: Workflow)

    @Delete
    suspend fun deleteWorkflow(workflow: Workflow)

    @Query("DELETE FROM workflows WHERE id = :id")
    suspend fun deleteById(id: Long)
}
