package com.example.data.storage

import android.content.Context
import android.os.Environment
import com.example.core.model.SynthesizedTask
import com.example.core.model.TaskCommand
import com.example.core.recorder.TaskSynthesizer
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileReader
import java.io.FileWriter

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
