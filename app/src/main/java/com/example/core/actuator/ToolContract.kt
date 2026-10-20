package com.example.core.actuator

/**
 * Studied from DroidRun (mobilerun):
 * Standardized, model-agnostic Tool Contract.
 * Every actuator, scraper, or system bridge implements this contract,
 * producing structured outputs and execution traces for failure diagnostics.
 */
data class ToolExecutionResult(
    val isSuccess: Boolean,
    val message: String,
    val outputData: Map<String, Any?> = emptyMap(),
    val executionTrace: List<String> = emptyList(),
    val timestampMs: Long = System.currentTimeMillis()
)

interface AgentTool {
    val name: String
    val description: String
    val parameterSchemaJson: String

    suspend fun execute(parameters: Map<String, Any?>): ToolExecutionResult
}

/**
 * Registry holding all available tools in the LocalAgent system.
 */
class ToolRegistry {
    private val tools = mutableMapOf<String, AgentTool>()

    fun register(tool: AgentTool) {
        tools[tool.name.lowercase()] = tool
    }

    fun get(name: String): AgentTool? = tools[name.lowercase()]

    val allTools: List<AgentTool> get() = tools.values.toList()
}
