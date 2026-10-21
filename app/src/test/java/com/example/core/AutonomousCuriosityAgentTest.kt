package com.example.core

import com.example.core.learning.AutonomousCuriosityAgent
import com.example.core.learning.CuriosityCategory
import com.example.core.learning.LearningState
import com.example.core.learning.ProposalStatus
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AutonomousCuriosityAgentTest {

    private lateinit var curiosityAgent: AutonomousCuriosityAgent

    @Before
    fun setUp() {
        curiosityAgent = AutonomousCuriosityAgent()
    }

    @Test
    fun testAutonomousProposalGeneration() {
        val topic = curiosityAgent.getNextPendingTopic()
        assertNotNull("Agent must proactively have a pending learning topic", topic)
        assertEquals(ProposalStatus.PROPOSED_WAITING_USER, topic?.status)
        assertTrue(topic?.agentQuestionToUser?.contains("?") == true)
    }

    @Test
    fun testUserApprovalAndOnlineLearningInteraction() = runBlocking {
        val topic = curiosityAgent.getNextPendingTopic()
        assertNotNull(topic)

        val result = curiosityAgent.approveAndLearnTopic(
            topicId = topic!!.id,
            userGuidance = "Focus on SSMB29 box office numbers and Spirit release window"
        )

        assertEquals(ProposalStatus.MUTUAL_DIALOGUE_ACTIVE, result.status)
        assertTrue(result.agentSummary.contains("Online research completed"))
        assertTrue("Discovered facts must be populated", topic.discoveredFacts.isNotEmpty())
        assertTrue("Synthesized steps must be generated", topic.synthesizedProceduralSteps.isNotEmpty())
        assertTrue("Follow-up dialogue questions must be present", result.questionsForUser.isNotEmpty())
    }

    @Test
    fun testMutualConfirmationCommitsToBrainVault() = runBlocking {
        val topic = curiosityAgent.getNextPendingTopic()
        assertNotNull(topic)

        curiosityAgent.approveAndLearnTopic(topic!!.id)
        val committedSkill = curiosityAgent.commitLearnedSkill(topic.id)

        assertNotNull(committedSkill)
        assertEquals(LearningState.VERIFIED, committedSkill.state)
        assertEquals(ProposalStatus.COMMITTED_TO_VAULT, topic.status)
        assertTrue(committedSkill.evidenceSummary.contains("Collaborative"))
    }

    @Test
    fun testUserDeclineTopic() {
        val topic = curiosityAgent.getNextPendingTopic()
        assertNotNull(topic)

        curiosityAgent.declineTopic(topic!!.id)
        assertEquals(ProposalStatus.DECLINED, topic.status)
    }
}
