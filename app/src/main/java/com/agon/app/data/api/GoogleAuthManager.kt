package com.agon.app.data.api

import android.content.Context
import android.content.Intent
import android.util.Log
import com.agon.app.data.models.UserProfile
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.tasks.await

/**
 * Google Authentication Manager
 * Handles real Google Sign-In OAuth flow
 */
class GoogleAuthManager(private val context: Context) {
    
    companion object {
        private const val TAG = "GoogleAuth"
        const val RC_SIGN_IN = 9001
        
        // YouTube API Scopes
        val YOUTUBE_SCOPES = listOf(
            "https://www.googleapis.com/auth/youtube.readonly",
            "https://www.googleapis.com/auth/youtube",
            "https://www.googleapis.com/auth/userinfo.profile",
            "https://www.googleapis.com/auth/userinfo.email"
        )
    }
    
    private val googleSignInOptions: GoogleSignInOptions = GoogleSignInOptions.Builder()
        .requestEmail()
        .requestProfile()
        .requestId()
        .requestIdToken("67897869870-3sqegjvs0u8efjt74cd7s5pe0nh71t00.apps.googleusercontent.com")
        .requestScopes(
            Scope(YOUTUBE_SCOPES[0]),
            Scope(YOUTUBE_SCOPES[1]),
            Scope(YOUTUBE_SCOPES[2]),
            Scope(YOUTUBE_SCOPES[3])
        )
        .build()
    
    private val googleSignInClient: GoogleSignInClient = 
        GoogleSignIn.getClient(context, googleSignInOptions)
    
    private var youTubeApiService: YouTubeApiService? = null
    
    /**
     * Get the sign-in intent for launching Google Sign-In
     */
    fun getSignInIntent(): Intent {
        return googleSignInClient.signInIntent
    }
    
    /**
     * Get the Google Sign-In client
     */
    fun getSignInClient(): GoogleSignInClient = googleSignInClient
    
    /**
     * Check if user is currently signed in
     */
    fun isSignedIn(): Boolean {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        return account != null
    }
    
    /**
     * Get current signed in account
     */
    fun getCurrentAccount(): GoogleSignInAccount? {
        return GoogleSignIn.getLastSignedInAccount(context)
    }
    
    /**
     * Get current user profile
     */
    fun getCurrentUserProfile(): UserProfile? {
        val account = getCurrentAccount() ?: return null
        return UserProfile(
            id = account.id ?: "",
            name = account.displayName ?: "User",
            email = account.email ?: "",
            photoUrl = account.photoUrl?.toString() ?: "",
            isSignedIn = true
        )
    }
    
    /**
     * Handle sign-in result from Google
     */
    suspend fun handleSignInResult(data: Intent?): Result<UserProfile> {
        return try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.await()
            
            Log.d(TAG, "Sign-in successful: ${account.displayName}")
            
            // Initialize YouTube API with the account
            youTubeApiService = YouTubeApiService(context)
            youTubeApiService?.initialize(account)
            
            Result.success(
                UserProfile(
                    id = account.id ?: "",
                    name = account.displayName ?: "User",
                    email = account.email ?: "",
                    photoUrl = account.photoUrl?.toString() ?: "",
                    isSignedIn = true
                )
            )
        } catch (e: ApiException) {
            Log.e(TAG, "Sign-in failed with status: ${e.statusCode}")
            Result.failure(Exception(getErrorMessage(e.statusCode)))
        } catch (e: Exception) {
            Log.e(TAG, "Sign-in failed: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Sign out the current user
     */
    suspend fun signOut(): Result<Unit> {
        return try {
            googleSignInClient.signOut().await()
            youTubeApiService = null
            Log.d(TAG, "Sign-out successful")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Sign-out failed: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Revoke access and sign out
     */
    suspend fun revokeAccess(): Result<Unit> {
        return try {
            googleSignInClient.revokeAccess().await()
            googleSignInClient.signOut().await()
            youTubeApiService = null
            Log.d(TAG, "Access revoked")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Revoke access failed: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Get YouTube API service
     */
    fun getYouTubeApiService(): YouTubeApiService? = youTubeApiService
    
    /**
     * Get error message from status code
     */
    private fun getErrorMessage(statusCode: Int): String {
        return when (statusCode) {
            12501 -> "Вход отменён пользователем"
            12502 -> "Вход в процессе"
            12500 -> "Ошибка аутентификации"
            7 -> "Нет подключения к сети"
            4 -> "Ошибка сети"
            5 -> "Неверный аккаунт"
            8 -> "Внутренняя ошибка"
            13 -> "Ошибка API"
            else -> "Ошибка входа (код: $statusCode)"
        }
    }
}
