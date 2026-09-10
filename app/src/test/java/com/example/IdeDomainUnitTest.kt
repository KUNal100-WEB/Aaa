package com.example

import com.example.data.model.BuildStatus
import com.example.data.repository.ProjectRepository
import com.example.data.service.MockBuildService
import com.example.domain.ai.AICodingAgent
import com.example.domain.validation.PathValidator
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class IdeDomainUnitTest {

    @Test
    fun `path validator prevents path traversal`() {
        val traversalResult = PathValidator.validate("../evil/secret.txt")
        assertFalse(traversalResult.isValid)

        val rootResult = PathValidator.validate("/etc/passwd.kt")
        assertFalse(rootResult.isValid)

        val badExtensionResult = PathValidator.validate("app/script.sh")
        assertFalse(badExtensionResult.isValid)

        val validResult = PathValidator.validate("app/src/main/java/com/example/Test.kt")
        assertTrue(validResult.isValid)
    }

    @Test
    fun `project repository creates and duplicates project properly`() {
        val repo = ProjectRepository()
        val proj = repo.createProject(
            name = "Test Calculator",
            packageName = "com.example.calc",
            description = "A simple calculator"
        )
        assertNotNull(proj)
        assertEquals("Test Calculator", proj.name)

        val files = repo.getFiles(proj.id)
        assertTrue(files.isNotEmpty())

        val dup = repo.duplicateProject(proj.id)
        assertNotNull(dup)
        assertEquals("Test Calculator (Copy)", dup!!.name)
    }

    @Test
    fun `build service produces failed status on syntax error trigger`() = runBlocking {
        val repo = ProjectRepository()
        val proj = repo.createProject("Broken", "com.example.broken", "Broken project")
        repo.saveFile(
            proj.id,
            "app/src/main/java/com/example/broken/MainActivity.kt",
            "val bad = SYNTAX_ERROR_TRIGGER"
        )

        val files = repo.getFiles(proj.id)
        val buildService = MockBuildService()
        val emitted = buildService.executeBuild(proj, files).toList()

        val lastRecord = emitted.last()
        assertEquals(BuildStatus.FAILED, lastRecord.status)
        assertNotNull(lastRecord.errorSummary)
    }

    @Test
    fun `ai agent repairs syntax error and caps attempts`() = runBlocking {
        val repo = ProjectRepository()
        val proj = repo.createProject("Broken2", "com.example.broken2", "Broken project 2")
        val filePath = "app/src/main/java/com/example/broken2/MainActivity.kt"
        repo.saveFile(proj.id, filePath, "val bad = SYNTAX_ERROR_TRIGGER")

        val agent = AICodingAgent(repo)

        // Attempt 1
        val req1 = agent.processPrompt(proj, "Fix build error")
        assertEquals("APPLIED", req1.status)
        val updatedFile = repo.getFile(proj.id, filePath)
        assertFalse(updatedFile!!.content.contains("SYNTAX_ERROR_TRIGGER"))

        // Attempt 2, 3, 4 (triggers 3-attempt limit)
        agent.processPrompt(proj, "Fix error")
        agent.processPrompt(proj, "Fix error")
        val req4 = agent.processPrompt(proj, "Fix error")
        assertEquals("FAILED", req4.status)
        assertTrue(req4.summary.contains("Maximum 3 automatic repair attempts exceeded"))
    }
}
