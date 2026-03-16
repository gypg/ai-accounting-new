package com.example.aiaccounting.data.repository

import com.example.aiaccounting.data.local.dao.CustomButlerDao
import com.example.aiaccounting.data.local.entity.CustomButlerEntity
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import java.io.File
import java.util.Base64
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CustomButlerRepository @Inject constructor(
    private val dao: CustomButlerDao
) {
    private val gson = Gson()

    fun observeAll(): Flow<List<CustomButlerEntity>> = dao.observeAll()

    suspend fun getById(id: String): CustomButlerEntity? = dao.getById(id)

    /**
     * Create or update a custom butler.
     */
    suspend fun upsert(entity: CustomButlerEntity) = dao.upsert(entity)

    /**
     * Explicit update. Kept for API completeness.
     */
    suspend fun update(entity: CustomButlerEntity) = dao.update(entity)

    suspend fun softDelete(id: String, updatedAt: Long) = dao.softDelete(id, updatedAt)

    /**
     * Duplicate an existing butler.
     *
     * - Generates a new UUID id by default
     * - Resets createdAt/updatedAt to [now]
     */
    suspend fun duplicate(
        id: String,
        now: Long = System.currentTimeMillis(),
        newId: String = UUID.randomUUID().toString()
    ): CustomButlerEntity {
        val existing = dao.getById(id) ?: throw IllegalArgumentException("Custom butler not found: $id")
        val duplicated = existing.copy(
            id = newId,
            createdAt = now,
            updatedAt = now,
            isDeleted = false
        )
        dao.upsert(duplicated)
        return duplicated
    }

    /**
     * Export a butler to JSON.
     *
     * If avatarType is [AVATAR_TYPE_LOCAL_PATH] and the referenced file exists, its bytes are embedded as
     * base64 in the JSON.
     */
    suspend fun exportToJson(id: String): String {
        val butler = dao.getById(id) ?: throw IllegalArgumentException("Custom butler not found: $id")

        val avatarImageBase64 = when {
            butler.avatarType == AVATAR_TYPE_LOCAL_PATH -> {
                val file = File(butler.avatarValue)
                if (file.exists() && file.isFile) {
                    Base64.getEncoder().encodeToString(file.readBytes())
                } else {
                    null
                }
            }

            else -> null
        }

        // Never export absolute local file paths.
        val exportedButler = when {
            butler.avatarType == AVATAR_TYPE_LOCAL_PATH -> butler.copy(avatarValue = "")
            else -> butler
        }

        return gson.toJson(
            ExportEnvelope(
                schemaVersion = SCHEMA_VERSION,
                butler = exportedButler,
                avatarImageBase64 = avatarImageBase64
            )
        )
    }

    enum class ImportMode {
        /** Keep the imported id as-is. */
        KEEP_ID,

        /** Create a new id to avoid collisions (default). */
        NEW_ID
    }

    /**
     * Import a butler from JSON.
     *
     * - Validates schemaVersion
     * - If avatarImageBase64 exists, writes it to [avatarStorageDir] and stores the resulting absolute path
     * - Upserts the resulting entity
     */
    suspend fun importFromJson(
        json: String,
        importMode: ImportMode = ImportMode.NEW_ID,
        avatarStorageDir: File,
        now: Long = System.currentTimeMillis(),
        newId: String = UUID.randomUUID().toString()
    ): CustomButlerEntity {
        val envelope = gson.fromJson(json, ExportEnvelope::class.java)
            ?: throw IllegalArgumentException("Invalid JSON")

        if (envelope.schemaVersion != SCHEMA_VERSION) {
            throw IllegalArgumentException(
                "Unsupported schemaVersion: ${envelope.schemaVersion}, expected $SCHEMA_VERSION"
            )
        }

        val normalizedStorageDir = avatarStorageDir.also { dir ->
            if (!dir.exists()) {
                dir.mkdirs()
            }
            if (!dir.exists() || !dir.isDirectory) {
                throw IllegalArgumentException("avatarStorageDir is not a directory: ${dir.absolutePath}")
            }
        }

        val imported = envelope.butler

        val finalId = when (importMode) {
            ImportMode.KEEP_ID -> imported.id
            ImportMode.NEW_ID -> newId
        }

        val avatarPath = envelope.avatarImageBase64
            ?.takeIf { it.isNotBlank() }
            ?.let { base64 ->
                val bytes = Base64.getDecoder().decode(base64)
                val outFile = File(normalizedStorageDir, "butler_${finalId}_avatar_${UUID.randomUUID()}.bin")
                outFile.writeBytes(bytes)
                outFile.absolutePath
            }

        val finalEntity = imported.copy(
            id = finalId,
            createdAt = if (importMode == ImportMode.NEW_ID) now else imported.createdAt,
            updatedAt = if (importMode == ImportMode.NEW_ID) now else imported.updatedAt,
            avatarType = if (avatarPath != null) AVATAR_TYPE_LOCAL_PATH else imported.avatarType,
            avatarValue = avatarPath ?: imported.avatarValue,
            isDeleted = false
        )

        dao.upsert(finalEntity)
        return finalEntity
    }

    private data class ExportEnvelope(
        val schemaVersion: Int,
        val butler: CustomButlerEntity,
        val avatarImageBase64: String?
    )

    companion object {
        const val SCHEMA_VERSION = 1

        /**
         * Avatar is stored as a file path in app-private storage.
         */
        const val AVATAR_TYPE_LOCAL_PATH = "LOCAL_PATH"
    }
}
