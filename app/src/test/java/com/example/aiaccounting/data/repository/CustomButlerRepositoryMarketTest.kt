package com.example.aiaccounting.data.repository

import com.example.aiaccounting.data.local.dao.CustomButlerDao
import com.example.aiaccounting.data.local.entity.CustomButlerEntity
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class CustomButlerRepositoryMarketTest {

    private data class ExportEnvelope(
        val schemaVersion: Int,
        val butler: CustomButlerEntity,
        val avatarImageBase64: String?
    )

    private class FakeCustomButlerDao : CustomButlerDao {
        private val byId = linkedMapOf<String, CustomButlerEntity>()
        private val allFlow = MutableStateFlow<List<CustomButlerEntity>>(emptyList())

        override fun observeAll(): Flow<List<CustomButlerEntity>> = allFlow

        override suspend fun getById(id: String): CustomButlerEntity? {
            return byId[id]?.takeIf { !it.isDeleted }
        }

        override suspend fun upsert(entity: CustomButlerEntity) {
            byId[entity.id] = entity
            refreshFlow()
        }

        override suspend fun update(entity: CustomButlerEntity) {
            byId[entity.id] = entity
            refreshFlow()
        }

        override suspend fun softDelete(id: String, updatedAt: Long) {
            val existing = byId[id] ?: return
            byId[id] = existing.copy(isDeleted = true, updatedAt = updatedAt)
            refreshFlow()
        }

        private fun refreshFlow() {
            allFlow.update {
                byId.values
                    .filter { !it.isDeleted }
                    .sortedByDescending { it.updatedAt }
            }
        }

        fun getRawById(id: String): CustomButlerEntity? = byId[id]
    }

    private fun sampleEntity(
        id: String,
        avatarType: String,
        avatarValue: String,
        createdAt: Long = 100L,
        updatedAt: Long = 200L,
        isDeleted: Boolean = false
    ): CustomButlerEntity {
        return CustomButlerEntity(
            id = id,
            name = "小管家",
            title = "标题",
            description = "描述",
            avatarType = avatarType,
            avatarValue = avatarValue,
            userCallName = "老板",
            butlerSelfName = "我",
            communicationStyle = 50,
            emotionIntensity = 50,
            professionalism = 50,
            humor = 50,
            proactivity = 50,
            featureFlagsJson = "{}",
            priorityJson = "[]",
            systemPrompt = "prompt",
            promptVersion = 1,
            createdAt = createdAt,
            updatedAt = updatedAt,
            isDeleted = isDeleted
        )
    }

    @Test
    fun exportImport_roundtrip_embedsAvatarBase64_andWritesToDir() = runTest {
        val avatarBytes = byteArrayOf(1, 2, 3, 4, 5, 6, 7)
        val sourceDir = Files.createTempDirectory("butler_export_src").toFile()
        val sourceAvatar = File(sourceDir, "avatar.bin").apply { writeBytes(avatarBytes) }

        val dao = FakeCustomButlerDao()
        val repo = CustomButlerRepository(dao)
        val entity = sampleEntity(
            id = "butler_1",
            avatarType = CustomButlerRepository.AVATAR_TYPE_LOCAL_PATH,
            avatarValue = sourceAvatar.absolutePath
        )
        dao.upsert(entity)

        val json = repo.exportToJson(id = entity.id)

        val importDir = Files.createTempDirectory("butler_import_dst").toFile()
        val imported = repo.importFromJson(
            json = json,
            importMode = CustomButlerRepository.ImportMode.KEEP_ID,
            avatarStorageDir = importDir
        )

        assertEquals(entity.id, imported.id)
        assertEquals(entity.createdAt, imported.createdAt)
        assertEquals(entity.updatedAt, imported.updatedAt)
        assertEquals(CustomButlerRepository.AVATAR_TYPE_LOCAL_PATH, imported.avatarType)
        assertNotNull(imported.avatarValue)

        val importedAvatarFile = File(imported.avatarValue)
        assertTrue(importedAvatarFile.exists())
        assertArrayEquals(avatarBytes, importedAvatarFile.readBytes())

        sourceDir.deleteRecursively()
        importDir.deleteRecursively()
    }

    @Test(expected = IllegalArgumentException::class)
    fun importFromJson_schemaVersionMismatch_throws() = runTest {
        val dao = FakeCustomButlerDao()
        val repo = CustomButlerRepository(dao)

        val json = """
            {
              "schemaVersion": 999,
              "butler": {
                "id": "x",
                "name": "n",
                "title": "t",
                "description": "d",
                "avatarType": "RESOURCE",
                "avatarValue": "ic_avatar",
                "userCallName": "u",
                "butlerSelfName": "b",
                "communicationStyle": 0,
                "emotionIntensity": 0,
                "professionalism": 0,
                "humor": 0,
                "proactivity": 0,
                "featureFlagsJson": "{}",
                "priorityJson": "[]",
                "systemPrompt": "p",
                "promptVersion": 1,
                "createdAt": 1,
                "updatedAt": 1,
                "isDeleted": false
              }
            }
        """.trimIndent()

        val dir = Files.createTempDirectory("butler_import").toFile()
        try {
            repo.importFromJson(json = json, avatarStorageDir = dir)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun importFromJson_newId_changesIdAndTimestamps() = runTest {
        val dao = FakeCustomButlerDao()
        val repo = CustomButlerRepository(dao)

        val src = sampleEntity(
            id = "old_id",
            avatarType = "RESOURCE",
            avatarValue = "ic_avatar",
            createdAt = 10L,
            updatedAt = 20L
        )

        val json = Gson().toJson(
            ExportEnvelope(
                schemaVersion = CustomButlerRepository.SCHEMA_VERSION,
                butler = src,
                avatarImageBase64 = null
            )
        )

        val dir = Files.createTempDirectory("butler_import_newid").toFile()
        try {
            val imported = repo.importFromJson(
                json = json,
                importMode = CustomButlerRepository.ImportMode.NEW_ID,
                avatarStorageDir = dir,
                now = 9999L,
                newId = "new_id"
            )

            assertEquals("new_id", imported.id)
            assertNotEquals("old_id", imported.id)
            assertEquals(9999L, imported.createdAt)
            assertEquals(9999L, imported.updatedAt)
            assertEquals(false, imported.isDeleted)

            val persisted = dao.getRawById("new_id")
            assertNotNull(persisted)
            assertEquals("new_id", persisted!!.id)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun duplicate_copiesAllFieldsButChangesIdAndTimestamps() = runTest {
        val dao = FakeCustomButlerDao()
        val repo = CustomButlerRepository(dao)

        val original = sampleEntity(
            id = "a",
            avatarType = "RESOURCE",
            avatarValue = "ic_avatar",
            createdAt = 1L,
            updatedAt = 2L
        )
        dao.upsert(original)

        val duplicated = repo.duplicate(id = "a", newId = "b", now = 5000L)

        assertEquals("b", duplicated.id)
        assertNotEquals(original.id, duplicated.id)
        assertEquals(5000L, duplicated.createdAt)
        assertEquals(5000L, duplicated.updatedAt)
        assertEquals(original.name, duplicated.name)
        assertEquals(original.systemPrompt, duplicated.systemPrompt)

        val persistedOriginal = dao.getRawById("a")
        val persistedCopy = dao.getRawById("b")
        assertNotNull(persistedOriginal)
        assertNotNull(persistedCopy)
    }
}
