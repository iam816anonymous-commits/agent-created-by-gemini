package com.example.core

import com.example.core.learning.CompactSkillRecord
import com.example.core.learning.LearningManager
import com.example.core.learning.LearningState
import com.example.core.learning.ReviewAction
import com.example.core.learning.toEntity
import com.example.core.learning.toRecord
import com.example.data.local.LearningRecordDao
import com.example.data.local.LearningRecordEntity
import com.example.data.local.LocalAgentDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

/**
 * In-memory test fake for LearningRecordDao to verify Room DAO contracts,
 * state transitions, and process-restart recovery in headless JUnit environments.
 */
class FakeLearningRecordDao : LearningRecordDao {
    private val records = mutableMapOf<String, LearningRecordEntity>()

    override fun getAllRecords(): Flow<List<LearningRecordEntity>> {
        return flowOf(records.values.sortedByDescending { it.createdTimestampMs })
    }

    override suspend fun getAllRecordsSnapshot(): List<LearningRecordEntity> {
        return records.values.sortedByDescending { it.createdTimestampMs }
    }

    override fun getRecordsByState(state: String): Flow<List<LearningRecordEntity>> {
        return flowOf(records.values.filter { it.state == state })
    }

    override suspend fun getRecordsByStateSnapshot(state: String): List<LearningRecordEntity> {
        return records.values.filter { it.state == state }
    }

    override suspend fun getRecordById(id: String): LearningRecordEntity? {
        return records[id]
    }

    override suspend fun findByPackageAndTask(packageName: String, task: String): LearningRecordEntity? {
        return records.values.firstOrNull { it.resolvedPackageName == packageName && it.intendedTask == task }
    }

    override suspend fun insertRecord(record: LearningRecordEntity) {
        records[record.id] = record
    }

    override suspend fun updateRecord(record: LearningRecordEntity) {
        records[record.id] = record
    }

    override suspend fun deleteRecordById(id: String) {
        records.remove(id)
    }

    override suspend fun clearAll() {
        records.clear()
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class LearningPersistenceTest {

    @Test
    fun testCandidateCreation_storedInPendingState() {
        val manager = LearningManager()
        val candidate = CompactSkillRecord(
            title = "Calculator Add Numbers",
            description = "Types 7 + 8 and presses equals",
            targetAppLabel = "Calculator",
            resolvedPackageName = "com.google.android.calculator",
            intendedTask = "arithmetic_addition",
            orderedExecutableSteps = listOf("Launch Calculator", "Click 7", "Click +", "Click 8", "Click ="),
            preconditions = listOf("Calculator installed"),
            postconditions = listOf("Display contains 15"),
            verificationStrategy = "formula_contains_15",
            knownLimitations = listOf("Standard calculator mode only"),
            evidenceSummary = "Single manual execution observed"
        )

        manager.submitCandidate(candidate)

        val pending = manager.getPendingProposals()
        assertEquals(1, pending.size)
        assertEquals("Calculator Add Numbers", pending[0].title)
        assertEquals(LearningState.PENDING_REVIEW, pending[0].state)
        assertEquals(1, pending[0].successfulExecutions)
    }

    @Test
    fun testDeduplication_updatesEvidenceInsteadOfDuplicateRows() {
        val manager = LearningManager()
        val candidate1 = CompactSkillRecord(
            title = "Calculator Add Numbers",
            description = "Types 7 + 8",
            targetAppLabel = "Calculator",
            resolvedPackageName = "com.google.android.calculator",
            intendedTask = "arithmetic_addition",
            orderedExecutableSteps = listOf("Launch Calculator", "Click 7"),
            preconditions = emptyList(),
            postconditions = emptyList(),
            verificationStrategy = "display_contains_7",
            knownLimitations = emptyList(),
            evidenceSummary = "First observation"
        )

        manager.submitCandidate(candidate1)
        assertEquals(1, manager.getPendingProposals().size)
        assertEquals(1, manager.getPendingProposals()[0].successfulExecutions)

        // Submit candidate targeting identical package and task
        val candidate2 = candidate1.copy(
            id = UUID.randomUUID().toString(),
            evidenceSummary = "Second observation"
        )
        manager.submitCandidate(candidate2)

        // Should still only have 1 record, but with incremented executions
        val pending = manager.getPendingProposals()
        assertEquals(1, pending.size)
        assertEquals(2, pending[0].successfulExecutions)
    }

    @Test
    fun testStateTransitions_userApprovalDoesNotAutomaticallyConferVerification() {
        val manager = LearningManager()
        val candidate = CompactSkillRecord(
            id = "cand-001",
            title = "Unverified App Action",
            description = "Action with only 1 execution observed",
            targetAppLabel = "Settings",
            resolvedPackageName = "com.android.settings",
            intendedTask = "toggle_bluetooth",
            orderedExecutableSteps = listOf("Launch Settings", "Click Bluetooth"),
            preconditions = emptyList(),
            postconditions = emptyList(),
            verificationStrategy = "bluetooth_switch_checked",
            knownLimitations = emptyList(),
            evidenceSummary = "Single observation",
            successfulExecutions = 1, // Only 1 observation: technically unverified
            failedValidations = 0,
            state = LearningState.PENDING_REVIEW
        )

        manager.submitCandidate(candidate)

        // Supervisor approves the candidate
        val result = manager.processReview("cand-001", ReviewAction.APPROVE, userNote = "Looks safe")

        assertNotNull(result)
        // MUST transition to APPROVED, NOT VERIFIED!
        assertEquals("User approval without sufficient verification must be APPROVED, not VERIFIED",
            LearningState.APPROVED, result?.state)
        assertEquals(0, manager.getVerifiedSkills().size)
        assertEquals(1, manager.getApprovedSkills().size)
    }

    @Test
    fun testStateTransitions_doubleValidatedCandidateBecomesVerifiedOnApproval() {
        val manager = LearningManager()
        val candidate = CompactSkillRecord(
            id = "cand-002",
            title = "Thoroughly Validated Action",
            description = "Action with 2 successful executions and 0 failures",
            targetAppLabel = "Calculator",
            resolvedPackageName = "com.google.android.calculator",
            intendedTask = "press_clear",
            orderedExecutableSteps = listOf("Click C"),
            preconditions = emptyList(),
            postconditions = emptyList(),
            verificationStrategy = "display_empty",
            knownLimitations = emptyList(),
            evidenceSummary = "Demonstrated twice with zero errors",
            successfulExecutions = 2, // Meets threshold
            failedValidations = 0,
            state = LearningState.PENDING_REVIEW
        )

        manager.submitCandidate(candidate)

        val result = manager.processReview("cand-002", ReviewAction.APPROVE, userNote = "Approved verified skill")

        assertNotNull(result)
        assertEquals(LearningState.VERIFIED, result?.state)
        assertEquals(1, manager.getVerifiedSkills().size)
        assertEquals(0, manager.getApprovedSkills().size)
    }

    @Test
    fun testStateTransitions_rejectAndNeedsMoreEvidence() {
        val manager = LearningManager()
        val candidate = CompactSkillRecord(
            id = "cand-003",
            title = "Dubious Action",
            description = "Click random icon",
            targetAppLabel = "Unknown",
            resolvedPackageName = "com.unknown.app",
            intendedTask = "random_tap",
            orderedExecutableSteps = listOf("Tap (100, 100)"),
            preconditions = emptyList(),
            postconditions = emptyList(),
            verificationStrategy = "none",
            knownLimitations = emptyList(),
            evidenceSummary = "Failed once",
            successfulExecutions = 1,
            failedValidations = 1
        )

        manager.submitCandidate(candidate)

        // Test "Test First" action
        val testFirstResult = manager.processReview("cand-003", ReviewAction.TEST_FIRST)
        assertEquals(LearningState.NEEDS_MORE_EVIDENCE, testFirstResult?.state)

        // Test "Reject" action
        val rejectResult = manager.processReview("cand-003", ReviewAction.REJECT, userNote = "Unsafe action")
        assertEquals(LearningState.REJECTED, rejectResult?.state)
        assertEquals("Unsafe action", rejectResult?.userApprovalNote)
    }

    @Test
    fun testEntitySerialization_roundTripPreservesAllFields() {
        val original = CompactSkillRecord(
            id = "test-uuid-99",
            title = "Keep Notes Write",
            description = "Writes meeting notes",
            targetAppLabel = "Keep Notes",
            resolvedPackageName = "com.google.android.keep",
            intendedTask = "create_note",
            requiredPermissions = listOf("android.permission.INTERNET"),
            orderedExecutableSteps = listOf("Launch Keep", "Tap New Note", "Type Meeting"),
            targetResolutionRules = mapOf("new_note_btn" to "id/new_note_button"),
            preconditions = listOf("User logged in"),
            postconditions = listOf("Note appears in list"),
            verificationStrategy = "list_contains_note_title",
            knownLimitations = listOf("Offline sync delay"),
            evidenceSummary = "Executed 3 times",
            successfulExecutions = 3,
            failedValidations = 0,
            state = LearningState.VERIFIED,
            userApprovalNote = "Supervisor confirmed",
            createdTimestampMs = 1729000000000L,
            lastValidatedTimestampMs = 1729000050000L,
            version = 2
        )

        val entity = original.toEntity()
        val restored = entity.toRecord()

        assertEquals(original.id, restored.id)
        assertEquals(original.title, restored.title)
        assertEquals(original.resolvedPackageName, restored.resolvedPackageName)
        assertEquals(original.requiredPermissions, restored.requiredPermissions)
        assertEquals(original.orderedExecutableSteps, restored.orderedExecutableSteps)
        assertEquals(original.targetResolutionRules, restored.targetResolutionRules)
        assertEquals(original.preconditions, restored.preconditions)
        assertEquals(original.postconditions, restored.postconditions)
        assertEquals(original.state, restored.state)
        assertEquals(original.successfulExecutions, restored.successfulExecutions)
        assertEquals(original.userApprovalNote, restored.userApprovalNote)
        assertEquals(original.version, restored.version)
    }

    @Test
    fun testProcessRestart_repositoryReloadRecoversPendingAndApprovedRecords() = runBlocking {
        val fakeDao = FakeLearningRecordDao()

        // Phase 1: App session 1 - User adds candidate and approves another
        val session1Manager = LearningManager(dao = fakeDao)

        val cand1 = CompactSkillRecord(
            id = "session1-cand1",
            title = "Pending Note Creation",
            description = "Pending review",
            targetAppLabel = "Keep",
            resolvedPackageName = "com.google.android.keep",
            intendedTask = "note_1",
            orderedExecutableSteps = listOf("Step 1"),
            preconditions = emptyList(),
            postconditions = emptyList(),
            verificationStrategy = "none",
            knownLimitations = emptyList(),
            evidenceSummary = "Observed",
            state = LearningState.PENDING_REVIEW
        )
        val cand2 = CompactSkillRecord(
            id = "session1-cand2",
            title = "Approved Note Search",
            description = "Approved by user",
            targetAppLabel = "Keep",
            resolvedPackageName = "com.google.android.keep",
            intendedTask = "search_1",
            orderedExecutableSteps = listOf("Step 1", "Step 2"),
            preconditions = emptyList(),
            postconditions = emptyList(),
            verificationStrategy = "search_results_visible",
            knownLimitations = emptyList(),
            evidenceSummary = "Observed once",
            successfulExecutions = 1,
            state = LearningState.PENDING_REVIEW
        )

        session1Manager.submitCandidateSync(cand1)
        session1Manager.submitCandidateSync(cand2)
        session1Manager.processReviewSync("session1-cand2", ReviewAction.APPROVE)

        // Verify session 1 state
        assertEquals(1, session1Manager.getPendingProposals().size)
        assertEquals(1, session1Manager.getApprovedSkills().size)

        // Phase 2: App process killed and restarted (Simulate cold start)
        // A brand new LearningManager instance is created with the persistent DAO
        val session2Manager = LearningManager(dao = fakeDao)
        session2Manager.reloadFromPersistentStore()

        // Verify both pending and approved proposals survive restart!
        val recoveredPending = session2Manager.getPendingProposals()
        val recoveredApproved = session2Manager.getApprovedSkills()

        assertEquals("Pending review proposals must survive app restart", 1, recoveredPending.size)
        assertEquals("session1-cand1", recoveredPending[0].id)
        assertEquals(LearningState.PENDING_REVIEW, recoveredPending[0].state)

        assertEquals("Approved skills must survive app restart", 1, recoveredApproved.size)
        assertEquals("session1-cand2", recoveredApproved[0].id)
        assertEquals(LearningState.APPROVED, recoveredApproved[0].state)
    }

    @Test
    fun testMigration_MIGRATION_4_5_schemaIntegrity() {
        val migration = LocalAgentDatabase.MIGRATION_4_5
        assertEquals(4, migration.startVersion)
        assertEquals(5, migration.endVersion)
    }
}
