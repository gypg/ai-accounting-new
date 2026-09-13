package com.example.aiaccounting.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.aiaccounting.data.local.entity.CustomButlerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomButlerDao {

    @Query("SELECT * FROM custom_butlers WHERE isDeleted = 0 ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<CustomButlerEntity>>

    @Query("SELECT * FROM custom_butlers WHERE id = :id AND isDeleted = 0 LIMIT 1")
    suspend fun getById(id: String): CustomButlerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: CustomButlerEntity)

    @Update
    suspend fun update(entity: CustomButlerEntity)

    @Query("UPDATE custom_butlers SET isDeleted = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun softDelete(id: String, updatedAt: Long)
}
