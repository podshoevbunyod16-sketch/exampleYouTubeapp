package com.agon.app.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.agon.app.data.api.GoogleAuthManager
import com.agon.app.data.api.YouTubeApiService
import com.agon.app.data.models.UserProfile
import com.agon.app.data.models.YouTubeVideo
import com.agon.app.data.models.WatchHistoryItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Main ViewModel for the app
 * Manages UI state, authentication, and data
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {
    
    private val context = application.applicationContext
    private val authManager = GoogleAuthManager(context)
    private val youTubeApiService = YouTubeApiService(context)
    
    // UI State
    sealed class UiState {
        object Loading : UiState()
        object Success : UiState()
        data class Error(val message: String) : UiState()
    }
    
    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    
    // User Profile
    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()
    
    // Videos
    private val _videos = MutableStateFlow<List<YouTubeVideo>>(emptyList())
    val videos: StateFlow<List<YouTubeVideo>> = _videos.asStateFlow()
    
    // Search Results
    private val _searchResults = MutableStateFlow<List<YouTubeVideo>>(emptyList())
    val searchResults: StateFlow<List<YouTubeVideo>> = _searchResults.asStateFlow()
    
    // Selected Video for playback
    private val _selectedVideo = MutableStateFlow<YouTubeVideo?>(null)
    val selectedVideo: StateFlow<YouTubeVideo?> = _selectedVideo.asStateFlow()
    
    // Watch History
    private val _watchHistory = MutableStateFlow<List<WatchHistoryItem>>(emptyList())
    val watchHistory: StateFlow<List<WatchHistoryItem>> = _watchHistory.asStateFlow()
    
    // Loading states
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()
    
    // Error
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    // Search query
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    init {
        checkSignInState()
    }
    
    /**
     * Check if user is already signed in
     */
    private fun checkSignInState() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            
            if (authManager.isSignedIn()) {
                val profile = authManager.getCurrentUserProfile()
                _userProfile.value = profile
                
                // Initialize YouTube API
                authManager.getCurrentAccount()?.let { account ->
                    youTubeApiService.initialize(account)
                }
                
                // Load videos
                loadVideos()
            }
            
            _uiState.value = UiState.Success
        }
    }
    
    /**
     * Handle Google Sign-In result
     */
    fun handleSignInResult(data: Intent?) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            val result = authManager.handleSignInResult(data)
            
            result.fold(
                onSuccess = { profile ->
                    _userProfile.value = profile
                    loadVideos()
                },
                onFailure = { error ->
                    _error.value = error.message
                }
            )
            
            _isLoading.value = false
        }
    }
    
    /**
     * Get sign-in intent for Google
     */
    fun getSignInIntent(): Intent {
        return authManager.getSignInIntent()
    }
    
    /**
     * Sign out
     */
    fun signOut() {
        viewModelScope.launch {
            authManager.signOut()
            _userProfile.value = null
            _videos.value = emptyList()
            _watchHistory.value = emptyList()
        }
    }
    
    /**
     * Load popular videos
     */
    fun loadVideos() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            val result = youTubeApiService.fetchPopularVideos()
            
            result.fold(
                onSuccess = { videos ->
                    _videos.value = videos
                },
                onFailure = { error ->
                    _error.value = error.message
                    // Load sample videos as fallback
                    _videos.value = youTubeApiService.getSampleVideos()
                }
            )
            
            _isLoading.value = false
        }
    }
    
    /**
     * Refresh videos
     */
    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            loadVideos()
            _isRefreshing.value = false
        }
    }
    
    /**
     * Search videos
     */
    fun searchVideos(query: String) {
        viewModelScope.launch {
            _searchQuery.value = query
            _isLoading.value = true
            
            if (query.isBlank()) {
                _searchResults.value = emptyList()
            } else {
                val result = youTubeApiService.searchVideos(query)
                
                result.fold(
                    onSuccess = { videos ->
                        _searchResults.value = videos
                    },
                    onFailure = { error ->
                        _error.value = error.message
                    }
                )
            }
            
            _isLoading.value = false
        }
    }
    
    /**
     * Select video for playback
     */
    fun selectVideo(video: YouTubeVideo) {
        _selectedVideo.value = video
        addToHistory(video)
    }
    
    /**
     * Clear selected video
     */
    fun clearSelectedVideo() {
        _selectedVideo.value = null
    }
    
    /**
     * Add to watch history
     */
    private fun addToHistory(video: YouTubeVideo) {
        val history = _watchHistory.value.toMutableList()
        // Remove if already exists
        history.removeAll { it.videoId == video.id }
        // Add to beginning
        history.add(0, WatchHistoryItem(video.id, video))
        // Keep only last 50 items
        _watchHistory.value = history.take(50)
    }
    
    /**
     * Clear watch history
     */
    fun clearHistory() {
        _watchHistory.value = emptyList()
    }
    
    /**
     * Clear error
     */
    fun clearError() {
        _error.value = null
    }
}
