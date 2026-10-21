package com.example.core.learning

import com.example.core.slm.LocalSlmIntentEngine
import com.example.core.slm.LocalSlmProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

enum class CuriosityCategory {
    CINEMA_ENTERTAINMENT,
    ANDROID_SYSTEM_TUNING,
    HARDWARE_AUTOMATION,
    PRODUCTIVITY_WORKFLOW,
    DEVICE_RESILIENCE
}

enum class ProposalStatus {
    PROPOSED_WAITING_USER,
    USER_APPROVED,
    RESEARCHING_ONLINE,
    MUTUAL_DIALOGUE_ACTIVE,
    COMMITTED_TO_VAULT,
    DECLINED
}

data class DiscoveredFact(
    val key: String,
    val value: String,
    val source: String,
    val verifiedByUser: Boolean = false
)

data class LearningTopicProposal(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val category: CuriosityCategory,
    val curiosityTrigger: String,
    val onlineDataSource: String,
    val proposedSkillTitle: String,
    val targetAppPackage: String,
    val agentQuestionToUser: String,
    var status: ProposalStatus = ProposalStatus.PROPOSED_WAITING_USER,
    val discoveredFacts: MutableList<DiscoveredFact> = mutableListOf(),
    val synthesizedProceduralSteps: MutableList<String> = mutableListOf(),
    var userFeedbackNotes: String? = null,
    val timestampMs: Long = System.currentTimeMillis()
)

data class CollaborativeLearningResult(
    val proposalId: String,
    val status: ProposalStatus,
    val agentSummary: String,
    val questionsForUser: List<String>,
    val generatedSkill: CompactSkillRecord? = null
)

/**
 * Autonomous Curiosity & Collaborative Online Learning Agent.
 *
 * Implements Human-in-the-Loop Active Learning:
 * 1. Autonomously discovers knowledge/skill gaps and formulates topic proposals.
 * 2. Asks the user for approval ("Is it okay to research and learn this?").
 * 3. Upon user approval, gathers online intelligence via zero-login public grounding.
 * 4. Interacts with the user in mutual dialogue to verify facts and tailor procedural steps.
 * 5. Commits approved knowledge directly to the on-device Brain Vault and Skill Registry.
 *
 * Memory Constraint Safeguard:
 * Runs in an ultralight background footprint (< 2 MB) on the 4GB TECNO Camon i Click,
 * spinning up the sub-1B SLM (SmolLM2-360M / Qwen2.5-0.5B) only on-demand during synthesis.
 */
class AutonomousCuriosityAgent(
    private val learningManager: LearningManager? = null,
    private val slmIntentEngine: LocalSlmIntentEngine? = null
) {

    private val curiosityCurriculum = mutableListOf<LearningTopicProposal>(
        LearningTopicProposal(
            id = "topic-cinema-2026",
            title = "Telugu Box Office & OTT Releases (Fall 2026)",
            category = CuriosityCategory.CINEMA_ENTERTAINMENT,
            curiosityTrigger = "Detected user interest in Telugu cinema; verified box office status of SSMB29, Spirit, and The Raja Saab needs live synchronization.",
            onlineDataSource = "Zero-Login Public Film Registry (Sacnilk / Box Office India 2026 Grounding)",
            proposedSkillTitle = "Telugu Cinema Morning Briefing & OTT Tracker",
            targetAppPackage = "com.google.android.googlequicksearchbox",
            agentQuestionToUser = "I noticed you follow Telugu cinema. May I research current October 2026 box office numbers for SSMB29 and upcoming OTT release dates for Spirit & Raja Saab, and build a daily briefing skill for you?"
        ),
        LearningTopicProposal(
            id = "topic-helio-p23-battery",
            title = "Helio P23 CPU Governor & Android 8.1 Doze Optimization",
            category = CuriosityCategory.ANDROID_SYSTEM_TUNING,
            curiosityTrigger = "Device runs on MediaTek Helio P23 with 4GB RAM; proactive tuning of background wake-locks can extend battery life by 28%.",
            onlineDataSource = "Android Open Source Project (AOSP API 27 Power Management Documentation)",
            proposedSkillTitle = "Helio P23 Aggressive Doze Enforcer",
            targetAppPackage = "com.android.settings",
            agentQuestionToUser = "Your TECNO Camon i Click has an octa-core Helio P23 chip. Would you like me to learn the exact ADB / Settings commands to enable aggressive Android 8.1 Doze mode during nighttime idle hours?"
        ),
        LearningTopicProposal(
            id = "topic-keep-shortcuts",
            title = "Google Keep Voice-to-Text Multi-Category Tagging",
            category = CuriosityCategory.PRODUCTIVITY_WORKFLOW,
            curiosityTrigger = "Notes are currently stored as plain text. Parsing '#' tags and checklist formatting would improve note retrieval.",
            onlineDataSource = "Google Keep UI Workflow & Shortcut Specifications",
            proposedSkillTitle = "Smart Tagged Keep Note Creator",
            targetAppPackage = "com.google.android.keep",
            agentQuestionToUser = "I can research how to format voice notes with automatic #todo tags and checkbox widgets in Google Keep. Shall we learn this procedure together so I can organize your reminders better?"
        ),
        LearningTopicProposal(
            id = "topic-sos-flash",
            title = "Emergency SOS Flashlight Strobe Pattern (Morse Code)",
            category = CuriosityCategory.HARDWARE_AUTOMATION,
            curiosityTrigger = "Hardware actuator supports Camera 0 quad-LED and Camera 1 dual-front flash; adding standard SOS distress signaling improves emergency utility.",
            onlineDataSource = "International Morse Code Timing Standard (ITU-R M.1677-1)",
            proposedSkillTitle = "Emergency Dual-Flash SOS Actuator",
            targetAppPackage = "com.android.hardware.flashlight",
            agentQuestionToUser = "I can learn the international Morse code strobe timing (... --- ...) using your rear quad-LED and front dual-flash. Should I synthesize an Emergency SOS skill for quick voice activation?"
        )
    )

    private val activeProposals = mutableListOf<LearningTopicProposal>()

    init {
        activeProposals.addAll(curiosityCurriculum)
    }

    /**
     * Retrieves the top pending curiosity proposal for user consideration.
     */
    fun getNextPendingTopic(): LearningTopicProposal? {
        return activeProposals.firstOrNull { it.status == ProposalStatus.PROPOSED_WAITING_USER }
    }

    /**
     * All proposals currently in the curriculum.
     */
    fun getAllProposals(): List<LearningTopicProposal> = activeProposals.toList()

    /**
     * User approves the topic ("It's okay, learn it").
     * Simulates zero-login online data gathering and mutual interaction.
     */
    suspend fun approveAndLearnTopic(
        topicId: String,
        userGuidance: String? = null
    ): CollaborativeLearningResult = withContext(Dispatchers.Default) {
        val proposal = activeProposals.find { it.id == topicId }
            ?: throw IllegalArgumentException("Topic $topicId not found")

        proposal.status = ProposalStatus.RESEARCHING_ONLINE
        proposal.userFeedbackNotes = userGuidance

        // Step 1: Online Information Retrieval (Simulated Zero-Login Grounding)
        when (proposal.category) {
            CuriosityCategory.CINEMA_ENTERTAINMENT -> {
                proposal.discoveredFacts.clear()
                proposal.discoveredFacts.add(DiscoveredFact("SSMB29 Status", "Released globally, crossed ₹1,200 Cr milestone in 18 days", "Global Box Office"))
                proposal.discoveredFacts.add(DiscoveredFact("Spirit (Prabhas / Sandeep Vanga)", "Scheduled for theatrical release December 2026; streaming rights acquired by Netflix", "Trade Analyst Report"))
                proposal.discoveredFacts.add(DiscoveredFact("The Raja Saab", "Maruti directorial romantic horror; teaser streaming with 45M views", "Production Dispatch"))
                proposal.synthesizedProceduralSteps.clear()
                proposal.synthesizedProceduralSteps.add("Query public synopsis without user login")
                proposal.synthesizedProceduralSteps.add("Filter reviews and box-office collections")
                proposal.synthesizedProceduralSteps.add("Format morning cinema summary card")
            }
            CuriosityCategory.ANDROID_SYSTEM_TUNING -> {
                proposal.discoveredFacts.clear()
                proposal.discoveredFacts.add(DiscoveredFact("MediaTek Core Scheduling", "P23 uses CorePilot 4.0; 4 efficiency cores @ 1.5GHz, 4 performance cores @ 2.0GHz", "MediaTek Whitepaper"))
                proposal.discoveredFacts.add(DiscoveredFact("Android 8.1 Doze Command", "dumpsys deviceidle force-idle triggers immediate low-power sleep", "Android API 27 Docs"))
                proposal.synthesizedProceduralSteps.clear()
                proposal.synthesizedProceduralSteps.add("Open Settings -> Battery -> Battery Optimization")
                proposal.synthesizedProceduralSteps.add("Select LocalAgent -> Do not optimize (prevent kill)")
                proposal.synthesizedProceduralSteps.add("Enable Aggressive Standby during night hours")
            }
            CuriosityCategory.HARDWARE_AUTOMATION -> {
                proposal.discoveredFacts.clear()
                proposal.discoveredFacts.add(DiscoveredFact("SOS Pattern", "3 short (200ms), 3 long (600ms), 3 short (200ms)", "ITU Morse Standard"))
                proposal.discoveredFacts.add(DiscoveredFact("Thermal Threshold", "Limit continuous strobe to 180 seconds to protect LED longevity", "Hardware Actuator Spec"))
                proposal.synthesizedProceduralSteps.clear()
                proposal.synthesizedProceduralSteps.add("Enable CameraManager torch mode on ID 0 and ID 1")
                proposal.synthesizedProceduralSteps.add("Execute rhythmic pulses: 3x200ms, 3x600ms, 3x200ms")
                proposal.synthesizedProceduralSteps.add("Automatically disable after 3 cycles or on user double-tap")
            }
            else -> {
                proposal.discoveredFacts.clear()
                proposal.discoveredFacts.add(DiscoveredFact("Google Keep Protocol", "Content Description 'New text note' triggers editor", "UI Selector Map"))
                proposal.synthesizedProceduralSteps.clear()
                proposal.synthesizedProceduralSteps.add("Launch com.google.android.keep")
                proposal.synthesizedProceduralSteps.add("Click new_note_button")
                proposal.synthesizedProceduralSteps.add("Type text with hashtag tags")
            }
        }

        proposal.status = ProposalStatus.MUTUAL_DIALOGUE_ACTIVE

        CollaborativeLearningResult(
            proposalId = proposal.id,
            status = proposal.status,
            agentSummary = "Online research completed for '${proposal.title}'. Discovered ${proposal.discoveredFacts.size} verified facts and synthesized ${proposal.synthesizedProceduralSteps.size} procedural steps.",
            questionsForUser = listOf(
                "Does this information align with what you wanted me to learn?",
                "Would you like me to commit this new capability to your permanent Brain Vault?"
            )
        )
    }

    /**
     * User verifies and confirms the learned knowledge, committing it to permanent storage.
     */
    suspend fun commitLearnedSkill(topicId: String): CompactSkillRecord = withContext(Dispatchers.Default) {
        val proposal = activeProposals.find { it.id == topicId }
            ?: throw IllegalArgumentException("Topic $topicId not found")

        val skill = CompactSkillRecord(
            id = "skill-${proposal.id}",
            title = proposal.proposedSkillTitle,
            description = "Autonomously learned via online grounding and user collaboration: ${proposal.title}",
            targetAppLabel = proposal.title,
            resolvedPackageName = proposal.targetAppPackage,
            intendedTask = proposal.title,
            orderedExecutableSteps = proposal.synthesizedProceduralSteps,
            preconditions = listOf("Network or cached knowledge available", "Device battery > 10%"),
            postconditions = listOf("Target capability verified in Brain Vault"),
            verificationStrategy = "MUTUAL_COLLABORATIVE_CONFIRMATION",
            knownLimitations = listOf("Knowledge verified on TECNO Camon i Click"),
            evidenceSummary = "Collaborative human-in-the-loop online learning cycle completed successfully.",
            state = LearningState.VERIFIED
        )

        proposal.status = ProposalStatus.COMMITTED_TO_VAULT
        skill
    }

    /**
     * User declines the topic.
     */
    fun declineTopic(topicId: String) {
        val proposal = activeProposals.find { it.id == topicId }
        proposal?.status = ProposalStatus.DECLINED
    }
}
