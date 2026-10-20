package com.example.core.slm

import org.json.JSONArray
import org.json.JSONObject

/**
 * GBNF (GGML BNF) Grammar Definitions for Structured Model Output.
 * Studied from ggml-org/llama.cpp & ffmpegkit-maintained/llama-android.
 *
 * Tiny models (SmolLM2-360M, Qwen2.5-0.5B) lack the parameter capacity for
 * long conversational reasoning. By constraining decoding to a strict GBNF grammar,
 * we force the model to output 100% syntactically valid JSON tool invocation arrays,
 * preventing hallucinated conversational chatter, markdown wrappers, or invalid syntax.
 */
object LocalGbnfGrammar {

    /**
     * Formal GBNF grammar for LocalAgent mobile tool dispatch.
     */
    val TOOL_CALL_GBNF_GRAMMAR: String = """
        root ::= "{" ws "\"intent\":" ws string "," ws "\"confidence\":" ws number "," ws "\"plan\":" ws action_list "}"
        action_list ::= "[" (ws action (ws "," ws action)*)? ws "]"
        action ::= "{" ws "\"step\":" ws number "," ws "\"action\":" ws string "," ws "\"target\":" ws string (ws "," ws "\"text\":" ws string)? (ws "," ws "\"pkg\":" ws string)? "}"
        string ::= "\"" [^\"]* "\""
        number ::= [0-9]+ ("." [0-9]+)?
        ws ::= [ \t\n\r]*
    """.trimIndent()

    /**
     * Validates whether a raw LLM output string conforms to the required
     * structured plan JSON schema.
     */
    fun validateStructuredPlanJson(rawJson: String): Boolean {
        return try {
            val trimmed = rawJson.trim()
            if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) return false
            val root = JSONObject(trimmed)
            if (!root.has("intent") || !root.has("plan")) return false
            val plan = root.getJSONArray("plan")
            for (i in 0 until plan.length()) {
                val actionObj = plan.getJSONObject(i)
                if (!actionObj.has("action") || !actionObj.has("target")) {
                    return false
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Generates a deterministic JSON payload conforming to GBNF schema
     * for fast-path regex parsing or fallback situations.
     */
    fun buildStructuredJson(
        intent: String,
        confidence: Float,
        actions: List<Map<String, String>>
    ): String {
        val root = JSONObject()
        root.put("intent", intent)
        root.put("confidence", confidence)
        val array = JSONArray()
        actions.forEachIndexed { idx, act ->
            val obj = JSONObject()
            obj.put("step", idx + 1)
            obj.put("action", act["action"] ?: "CLICK")
            obj.put("target", act["target"] ?: "")
            if (act.containsKey("text")) obj.put("text", act["text"])
            if (act.containsKey("pkg")) obj.put("pkg", act["pkg"])
            array.put(obj)
        }
        root.put("plan", array)
        return root.toString(2)
    }
}
