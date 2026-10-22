package com.example.kairo.data.repository

import com.example.kairo.data.PreferencesManager
import com.example.kairo.data.SupabaseClient
import com.example.kairo.data.remote.RemoteUserSettings
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.user.UserInfo

class AuthRepository(private val preferencesManager: PreferencesManager) {

    val currentUser: UserInfo?
        get() = try {
            SupabaseClient.auth.currentUserOrNull()
        } catch (e: Exception) {
            null
        }

    val sessionStatus = SupabaseClient.auth.sessionStatus

    suspend fun signUp(emailInput: String, passwordInput: String): Result<Unit> {
        return try {
            SupabaseClient.auth.signUpWith(Email) {
                email = emailInput
                password = passwordInput
            }
            currentUser?.id?.let { userId ->
                syncLocalToRemote(userId)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signIn(emailInput: String, passwordInput: String): Result<Unit> {
        return try {
            SupabaseClient.auth.signInWith(Email) {
                email = emailInput
                password = passwordInput
            }
            currentUser?.id?.let { userId ->
                syncRemoteToLocalOrPush(userId)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signOut(): Result<Unit> {
        return try {
            SupabaseClient.auth.signOut()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchRemoteSettings(userId: String): RemoteUserSettings? {
        return try {
            SupabaseClient.postgrest["user_settings"]
                .select {
                    filter {
                        eq("id", userId)
                    }
                }
                .decodeSingleOrNull<RemoteUserSettings>()
        } catch (e: Exception) {
            null
        }
    }

    suspend fun pushLocalToRemote(userId: String): Result<Unit> {
        return try {
            val localSettings = preferencesManager.exportRemoteUserSettings(userId)
            SupabaseClient.postgrest["user_settings"].upsert(localSettings)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun syncRemoteToLocalOrPush(userId: String): Result<Unit> {
        return try {
            val remote = fetchRemoteSettings(userId)
            if (remote != null) {
                preferencesManager.importRemoteUserSettings(remote)
            } else {
                pushLocalToRemote(userId)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun syncLocalToRemote(userId: String): Result<Unit> {
        return pushLocalToRemote(userId)
    }
}
