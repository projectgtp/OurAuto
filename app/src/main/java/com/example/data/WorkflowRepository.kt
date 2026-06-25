package com.example.data

import kotlinx.coroutines.flow.Flow

class WorkflowRepository(private val workflowDao: WorkflowDao) {
    val allWorkflows: Flow<List<Workflow>> = workflowDao.getAllWorkflows()

    suspend fun getWorkflowById(id: Long): Workflow? {
        return workflowDao.getWorkflowById(id)
    }

    suspend fun insertWorkflow(workflow: Workflow): Long {
        return workflowDao.insertWorkflow(workflow)
    }

    suspend fun updateWorkflow(workflow: Workflow) {
        workflowDao.updateWorkflow(workflow)
    }

    suspend fun deleteWorkflow(workflow: Workflow) {
        workflowDao.deleteWorkflow(workflow)
    }

    suspend fun deleteById(id: Long) {
        workflowDao.deleteById(id)
    }
}
