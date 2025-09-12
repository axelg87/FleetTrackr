package com.fleetmanager.data.local.dao

import androidx.room.*
import com.fleetmanager.data.dto.UserDto
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    
    @Query("SELECT * FROM users WHERE uid = :uid")
    suspend fun getUserById(uid: String): UserDto?
    
    @Query("SELECT * FROM users WHERE uid = :uid")
    fun getUserByIdFlow(uid: String): Flow<UserDto?>
    
    @Query("SELECT * FROM users ORDER BY displayName ASC")
    fun getAllUsers(): Flow<List<UserDto>>
    
    @Query("SELECT * FROM users WHERE role = :role ORDER BY displayName ASC")
    fun getUsersByRole(role: String): Flow<List<UserDto>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserDto)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<UserDto>)
    
    @Update
    suspend fun updateUser(user: UserDto)
    
    @Delete
    suspend fun deleteUser(user: UserDto)
    
    @Query("DELETE FROM users WHERE uid = :uid")
    suspend fun deleteUserById(uid: String)
    
    @Query("UPDATE users SET lastSyncedAt = :syncTime WHERE uid = :uid")
    suspend fun updateLastSyncTime(uid: String, syncTime: java.util.Date)
}