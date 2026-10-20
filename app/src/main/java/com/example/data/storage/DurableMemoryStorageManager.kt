package com.example.data.storage

import android.content.Context
import android.net.Uri
import android.os.Environment
import com.example.core.model.AgentSkill
import com.example.core.model.SynthesizedTask
import com.example.core.model.TaskCommand
import com.example.core.recorder.TaskSynthesizer
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.io.InputStream
import java.io.OutputStream

data class BrainVaultPackage(
    val version: Int = 3,
    val exportedAtMs: Long = System.currentTimeMillis(),
    val agentName: String = "LocalAgent",
    val deviceModel: String = android.os.Build.MODEL,
    val tasks: List<SynthesizedTask> = emptyList(),
    val skills: List<AgentSkill> = emptyList()
)

data class DurableBackupStatus(
    val exists: Boolean,
    val filePath: String,
    val taskCount: Int,
    val lastModifiedMs: Long,
    val message: String
)

class DurableMemoryStorageManager(private val context: Context) {

    private val synthesizer = TaskSynthesizer()

    // We target public Downloads directory first which survives app uninstall on Android
    private fun getPublicBackupFile(): File {
        val publicDownloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (!publicDownloads.exists()) {
            publicDownloads.mkdirs()
        }
        return File(publicDownloads, "localagent_durable_learning.json")
    }

    // Fallback directory in external media
    private fun getExternalMediaBackupFile(): File {
        val externalDirs = context.getExternalFilesDirs(null)
        val baseDir = externalDirs.firstOrNull()?.parentFile?.parentFile ?: context.filesDir
        val backupDir = File(baseDir, "LocalAgentPersistent")
        if (!backupDir.exists()) {
            backupDir.mkdirs()
        }
        return File(backupDir, "localagent_durable_learning.json")
    }

    private fun getTargetFile(): File {
        return try {
            val pub = getPublicBackupFile()
            if (pub.canWrite() || pub.parentFile?.canWrite() == true) pub else getExternalMediaBackupFile()
        } catch (_: Exception) {
            getExternalMediaBackupFile()
        }
    }

    fun exportDurableMemory(tasks: List<SynthesizedTask>): Result<String> {
        return try {
            val file = getTargetFile()
            val root = JSONObject()
            root.put("version", 2)
            root.put("exportedAtMs", System.currentTimeMillis())
            root.put("agentName", "LocalAgent")
            root.put("deviceModel", android.os.Build.MODEL)
            root.put("totalTasks", tasks.size)

            val tasksArray = JSONArray()
            for (task in tasks) {
                val taskObj = JSONObject()
                taskObj.put("id", task.id)
                taskObj.put("name", task.name)
                taskObj.put("description", task.description)
                taskObj.put("rawInteractionsCount", task.rawInteractionsCount)
                taskObj.put("createdAt", task.createdAt)
                taskObj.put("commandsPayload", synthesizer.encodeCommands(task.commands))
                tasksArray.put(taskObj)
            }
            root.put("tasks", tasksArray)

            FileWriter(file).use { writer ->
                writer.write(root.toString(2))
            }

            Result.success(file.absolutePath)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun importDurableMemory(): Result<List<SynthesizedTask>> {
        return try {
            val file = getTargetFile()
            if (!file.exists()) {
                val fallback = getExternalMediaBackupFile()
                if (!fallback.exists()) {
                    return Result.failure(IllegalStateException("No durable backup file found"))
                }
            }

            val target = if (file.exists()) file else getExternalMediaBackupFile()
            val content = FileReader(target).use { it.readText() }
            val root = JSONObject(content)
            val tasksArray = root.getJSONArray("tasks")
            val result = mutableListOf<SynthesizedTask>()

            for (i in 0 until tasksArray.length()) {
                val taskObj = tasksArray.getJSONObject(i)
                val id = taskObj.optString("id", java.util.UUID.randomUUID().toString())
                val name = taskObj.getString("name")
                val desc = taskObj.optString("description", "")
                val rawCount = taskObj.optInt("rawInteractionsCount", 1)
                val createdAt = taskObj.optLong("createdAt", System.currentTimeMillis())
                val payload = taskObj.optString("commandsPayload", "")

                val commands = synthesizer.decodeCommands(payload)
                result.add(
                    SynthesizedTask(
                        id = id,
                        name = name,
                        description = desc,
                        commands = commands,
                        rawInteractionsCount = rawCount,
                        createdAt = createdAt
                    )
                )
            }

            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Storage Access Framework (SAF) export: Streams backup directly to user-selected destination.
     * Complies with Android scoped storage and survives uninstallation.
     */
    fun exportToOutputStream(
        outputStream: OutputStream,
        tasks: List<SynthesizedTask>,
        skills: List<AgentSkill> = emptyList()
    ): Result<Int> {
        return try {
            val root = JSONObject()
            root.put("version", 3)
            root.put("exportedAtMs", System.currentTimeMillis())
            root.put("agentName", "LocalAgent")
            root.put("deviceModel", android.os.Build.MODEL)
            root.put("totalTasks", tasks.size)
            root.put("totalSkills", skills.size)

            val tasksArray = JSONArray()
            for (task in tasks) {
                val taskObj = JSONObject()
                taskObj.put("id", task.id)
                taskObj.put("name", task.name)
                taskObj.put("description", task.description)
                taskObj.put("rawInteractionsCount", task.rawInteractionsCount)
                taskObj.put("createdAt", task.createdAt)
                taskObj.put("commandsPayload", synthesizer.encodeCommands(task.commands))
                tasksArray.put(taskObj)
            }
            root.put("tasks", tasksArray)

            val skillsArray = JSONArray()
            for (skill in skills) {
                val skillObj = JSONObject()
                skillObj.put("id", skill.id)
                skillObj.put("name", skill.name)
                skillObj.put("description", skill.description)
                skillObj.put("category", skill.category)
                skillObj.put("triggers", JSONArray(skill.triggerPhrases))
                skillObj.put("commandsPayload", synthesizer.encodeCommands(skill.commands))
                skillObj.put("createdAt", skill.createdAt)
                skillsArray.put(skillObj)
            }
            root.put("skills", skillsArray)

            outputStream.bufferedWriter().use { writer ->
                writer.write(root.toString(2))
            }
            Result.success(tasks.size + skills.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun exportDurableMemoryToUri(
        uri: Uri,
        tasks: List<SynthesizedTask>,
        skills: List<AgentSkill> = emptyList()
    ): Result<Int> {
        return try {
            val stream = context.contentResolver.openOutputStream(uri)
                ?: return Result.failure(IllegalStateException("Cannot open output stream for URI: $uri"))
            exportToOutputStream(stream, tasks, skills)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun importFromInputStream(inputStream: InputStream): Result<BrainVaultPackage> {
        return try {
            val content = inputStream.bufferedReader().use { it.readText() }
            val root = JSONObject(content)
            val tasksArray = root.optJSONArray("tasks") ?: JSONArray()
            val tasks = mutableListOf<SynthesizedTask>()

            for (i in 0 until tasksArray.length()) {
                val taskObj = tasksArray.getJSONObject(i)
                val id = taskObj.optString("id", java.util.UUID.randomUUID().toString())
                val name = taskObj.getString("name")
                val desc = taskObj.optString("description", "")
                val rawCount = taskObj.optInt("rawInteractionsCount", 1)
                val createdAt = taskObj.optLong("createdAt", System.currentTimeMillis())
                val payload = taskObj.optString("commandsPayload", "")
                val commands = synthesizer.decodeCommands(payload)
                tasks.add(
                    SynthesizedTask(
                        id = id,
                        name = name,
                        description = desc,
                        commands = commands,
                        rawInteractionsCount = rawCount,
                        createdAt = createdAt
                    )
                )
            }

            val skillsArray = root.optJSONArray("skills") ?: JSONArray()
            val skills = mutableListOf<AgentSkill>()
            for (i in 0 until skillsArray.length()) {
                val skillObj = skillsArray.getJSONObject(i)
                val id = skillObj.optString("id", java.util.UUID.randomUUID().toString())
                val name = skillObj.getString("name")
                val desc = skillObj.optString("description", "")
                val category = skillObj.optString("category", "General")
                val triggersArray = skillObj.optJSONArray("triggers") ?: JSONArray()
                val triggers = mutableListOf<String>()
                for (j in 0 until triggersArray.length()) {
                    triggers.add(triggersArray.getString(j))
                }
                val payload = skillObj.optString("commandsPayload", "")
                val commands = synthesizer.decodeCommands(payload)
                val createdAt = skillObj.optLong("createdAt", System.currentTimeMillis())

                skills.add(
                    AgentSkill(
                        id = id,
                        name = name,
                        description = desc,
                        category = category,
                        triggerPhrases = triggers,
                        commands = commands,
                        createdAt = createdAt
                    )
                )
            }

            Result.success(
                BrainVaultPackage(
                    version = root.optInt("version", 3),
                    exportedAtMs = root.optLong("exportedAtMs", System.currentTimeMillis()),
                    agentName = root.optString("agentName", "LocalAgent"),
                    deviceModel = root.optString("deviceModel", android.os.Build.MODEL),
                    tasks = tasks,
                    skills = skills
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun importDurableMemoryFromUri(uri: Uri): Result<BrainVaultPackage> {
        return try {
            val stream = context.contentResolver.openInputStream(uri)
                ?: return Result.failure(IllegalStateException("Cannot open input stream for URI: $uri"))
            importFromInputStream(stream)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getBackupStatus(): DurableBackupStatus {
        val file = getTargetFile()
        val exists = file.exists()
        return if (exists) {
            try {
                val content = FileReader(file).use { it.readText() }
                val root = JSONObject(content)
                val count = root.optInt("totalTasks", 0)
                DurableBackupStatus(
                    exists = true,
                    filePath = file.absolutePath,
                    taskCount = count,
                    lastModifiedMs = file.lastModified(),
                    message = "Persistent knowledge found ($count learned workflows, survives uninstalls)"
                )
            } catch (_: Exception) {
                DurableBackupStatus(
                    exists = true,
                    filePath = file.absolutePath,
                    taskCount = 0,
                    lastModifiedMs = file.lastModified(),
                    message = "Backup file exists on external storage"
                )
            }
        } else {
            DurableBackupStatus(
                exists = false,
                filePath = file.absolutePath,
                taskCount = 0,
                lastModifiedMs = 0L,
                message = "Ready to persist to public storage ($file.name)"
            )
        }
    }
}
