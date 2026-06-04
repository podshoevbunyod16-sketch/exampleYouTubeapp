package com.agon.app.data.api

import android.content.Context
import android.util.Log
import com.agon.app.data.models.YouTubeVideo
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.youtube.YouTube
import com.google.api.services.youtube.model.VideoSnippet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.math.BigInteger

/**
 * Real YouTube API Service with Google OAuth
 */
class YouTubeApiService(private val context: Context) {
    
    companion object {
        private const val TAG = "YouTubeAPI"
        const val YOUTUBE_API_KEY = "AIzaSyDl8wl2HVYlYvVrrbkVylvsd9QmO" // Replace with real key
    }
    
    private var youTube: YouTube? = null
    private var currentAccount: GoogleSignInAccount? = null
    
    /**
     * Initialize YouTube service with Google account
     */
    fun initialize(account: GoogleSignInAccount) {
        currentAccount = account
        try {
            val credential = GoogleAccountCredential.usingOAuth2(
                context,
                listOf(
                    "https://www.googleapis.com/auth/youtube.readonly",
                    "https://www.googleapis.com/auth/youtube",
                    "https://www.googleapis.com/auth/userinfo.profile",
                    "https://www.googleapis.com/auth/userinfo.email"
                )
            ).apply {
                selectedAccount = account.account
            }
            
            youTube = YouTube.Builder(
                NetHttpTransport(),
                GsonFactory.getDefaultInstance(),
                credential
            ).setApplicationName("NeonTube").build()
            
            Log.d(TAG, "YouTube API initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize YouTube API: ${e.message}")
        }
    }
    
    /**
     * Fetch popular videos from YouTube
     */
    suspend fun fetchPopularVideos(regionCode: String = "RU", maxResults: Long = 20): Result<List<YouTubeVideo>> = withContext(Dispatchers.IO) {
        try {
            val videos = mutableListOf<YouTubeVideo>()
            
            // Use YouTube Data API
            youTube?.let { yt ->
                // Get most popular video IDs
                val popularRequest = yt.videos().list(listOf("snippet", "contentDetails", "statistics"))
                    .setChart("mostPopular")
                    .setRegionCode(regionCode)
                    .setMaxResults(maxResults)
                    .setKey(YOUTUBE_API_KEY)
                
                val popularResponse = popularRequest.execute()
                
                popularResponse.items.forEach { item ->
                    videos.add(
                        YouTubeVideo(
                            id = item.id,
                            title = item.snippet.title,
                            description = item.snippet.description,
                            thumbnailUrl = item.snippet.thumbnails.maxres?.url 
                                ?: item.snippet.thumbnails.high?.url 
                                ?: item.snippet.thumbnails.default?.url 
                                ?: "",
                            channelTitle = item.snippet.channelTitle,
                            channelId = item.snippet.channelId,
                            viewCount = formatViewCount(item.statistics.viewCount),
                            publishedAt = formatPublishedDate(item.snippet.publishedAt.toString()),
                            duration = formatDuration(item.contentDetails.duration),
                            likeCount = item.statistics.likeCount?.toString() ?: "0"
                        )
                    )
                }
            }
            
            if (videos.isEmpty()) {
                // Fallback to sample data if API fails
                Result.success(getSampleVideos())
            } else {
                Result.success(videos)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching popular videos: ${e.message}")
            // Return sample data as fallback
            Result.success(getSampleVideos())
        }
    }
    
    /**
     * Search videos on YouTube
     */
    suspend fun searchVideos(query: String, maxResults: Long = 20): Result<List<YouTubeVideo>> = withContext(Dispatchers.IO) {
        try {
            val videos = mutableListOf<YouTubeVideo>()
            
            youTube?.let { yt ->
                val searchRequest = yt.search().list(listOf("snippet"))
                    .setQ(query)
                    .setType(listOf("video"))
                    .setMaxResults(maxResults)
                    .setKey(YOUTUBE_API_KEY)
                
                val searchResponse = searchRequest.execute()
                
                val videoIds = searchResponse.items.map { it.id.videoId }
                
                if (videoIds.isNotEmpty()) {
                    val videosRequest = yt.videos().list(listOf("snippet", "contentDetails", "statistics"))
                        .setId(videoIds)
                        .setKey(YOUTUBE_API_KEY)
                    
                    val videosResponse = videosRequest.execute()
                    
                    videosResponse.items.forEach { item ->
                        videos.add(
                            YouTubeVideo(
                                id = item.id,
                                title = item.snippet.title,
                                description = item.snippet.description,
                                thumbnailUrl = item.snippet.thumbnails.maxres?.url 
                                    ?: item.snippet.thumbnails.high?.url 
                                    ?: item.snippet.thumbnails.default?.url 
                                    ?: "",
                                channelTitle = item.snippet.channelTitle,
                                channelId = item.snippet.channelId,
                                viewCount = formatViewCount(item.statistics.viewCount),
                                publishedAt = formatPublishedDate(item.snippet.publishedAt.toString()),
                                duration = formatDuration(item.contentDetails.duration)
                            )
                        )
                    }
                }
            }
            
            Result.success(videos)
        } catch (e: Exception) {
            Log.e(TAG, "Error searching videos: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Get channel info
     */
    suspend fun getChannelInfo(channelId: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            var avatarUrl = ""
            
            youTube?.let { yt ->
                val channelRequest = yt.channels().list(listOf("snippet", "statistics"))
                    .setId(listOf(channelId))
                    .setKey(YOUTUBE_API_KEY)
                
                val channelResponse = channelRequest.execute()
                
                channelResponse.items.firstOrNull()?.let { channel ->
                    avatarUrl = channel.snippet.thumbnails.default?.url ?: ""
                }
            }
            
            Result.success(avatarUrl)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting channel info: ${e.message}")
            Result.failure(e)
        }
    }
    
    private fun formatViewCount(count: BigInteger?): String {
        if (count == null) return "0"
        val num = count.toLong()
        return when {
            num >= 1_000_000 -> "%.1f млн".
                format(num / 1_000_000.0)
            num >= 1_000 -> "%.0f тыс.".format(num / 1_000.0)
            else -> num.toString()
        }
    }
    
    private fun formatDuration(duration: String?): String {
        if (duration.isNullOrBlank()) return "0:00"
        
        // Parse ISO 8601 duration (PT1H2M10S)
        val regex = "PT(?:(\\d+)H)?(?:(\\d+)M)?(?:(\\d+)S)?".toRegex()
        val match = regex.find(duration) ?: return "0:00"
        
        val hours = match.groupValues[1].toIntOrNull() ?: 0
        val minutes = match.groupValues[2].toIntOrNull() ?: 0
        val seconds = match.groupValues[3].toIntOrNull() ?: 0
        
        return when {
            hours > 0 -> "%d:%02d:%02d".format(hours, minutes, seconds)
            else -> "%d:%02d".format(minutes, seconds)
        }
    }
    
    private fun formatPublishedDate(publishedAt: String): String {
        try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            val date = inputFormat.parse(publishedAt) ?: return ""
            
            val now = Date()
            val diffMs = now.time - date.time
            val diffDays = diffMs / (1000 * 60 * 60 * 24)
            
            return when {
                diffDays == 0L -> "сегодня"
                diffDays == 1L -> "вчера"
                diffDays < 7 -> "$diffDays дн. назад"
                diffDays < 30 -> "${diffDays / 7} нед. назад"
                diffDays < 365 -> "${diffDays / 30} мес. назад"
                else -> "${diffDays / 365} г. назад"
            }
        } catch (e: Exception) {
            return ""
        }
    }
    
    /**
     * Sample videos for fallback when API is not available
     */
    fun getSampleVideos(): List<YouTubeVideo> {
        return listOf(
            YouTubeVideo(
                id = "dQw4w9WgXcQ",
                title = "Rick Astley - Never Gonna Give You Up (Official Music Video)",
                description = "The official video for 'Never Gonna Give You Up' by Rick Astley",
                thumbnailUrl = "https://i.ytimg.com/vi/dQw4w9WgXcQ/maxresdefault.jpg",
                channelTitle = "Rick Astley",
                channelId = "UCuAXFkgsw1L7xaCfnd5JJOw",
                viewCount = "1.4 млрд",
                publishedAt = "15 лет назад",
                duration = "3:33"
            ),
            YouTubeVideo(
                id = "jNQXAC9IVRw",
                title = "Me at the zoo",
                description = "The first video on YouTube",
                thumbnailUrl = "https://i.ytimg.com/vi/jNQXAC9IVRw/maxresdefault.jpg",
                channelTitle = "jawed",
                channelId = "UC4QobU6STFB0P51PM8OGM5w",
                viewCount = "280 млн",
                publishedAt = "19 лет назад",
                duration = "0:19"
            ),
            YouTubeVideo(
                id = "9bZkp7q19f0",
                title = "PSY - GANGNAM STYLE(강남스타일) M/V",
                description = "PSY - GANGNAM STYLE",
                thumbnailUrl = "https://i.ytimg.com/vi/9bZkp7q19f0/maxresdefault.jpg",
                channelTitle = "officialpsy",
                channelId = "UCrDkAvwZum-UTjHmzDI2iIw",
                viewCount = "5 млрд",
                publishedAt = "12 лет назад",
                duration = "4:13"
            ),
            YouTubeVideo(
                id = "kJQP7kiw5Fk",
                title = "Luis Fonsi - Despacito ft. Daddy Yankee",
                description = "Despacito - Luis Fonsi ft. Daddy Yankee",
                thumbnailUrl = "https://i.ytimg.com/vi/kJQP7kiw5Fk/maxresdefault.jpg",
                channelTitle = "Luis Fonsi",
                channelId = "UCxoq60PAO9fdO3aQ8DPQj4w",
                viewCount = "8.3 млрд",
                publishedAt = "8 лет назад",
                duration = "4:42"
            ),
            YouTubeVideo(
                id = "JGwWNGJdvx8",
                title = "Ed Sheeran - Shape of You (Official Music Video)",
                description = "Shape of You by Ed Sheeran",
                thumbnailUrl = "https://i.ytimg.com/vi/JGwWNGJdvx8/maxresdefault.jpg",
                channelTitle = "Ed Sheeran",
                channelId = "UC0C-w0YjGpqDXGB8IHb662A",
                viewCount = "6.2 млрд",
                publishedAt = "7 лет назад",
                duration = "4:24"
            ),
            YouTubeVideo(
                id = "RgKAFK5djSk",
                title = "Wiz Khalifa - See You Again ft. Charlie Puth",
                description = "See You Again - Furious 7 Soundtrack",
                thumbnailUrl = "https://i.ytimg.com/vi/RgKAFK5djSk/maxresdefault.jpg",
                channelTitle = "Wiz Khalifa",
                channelId = "UCaWm6DEfYDvsvGDLhN1FqBw",
                viewCount = "5.9 млрд",
                publishedAt = "9 лет назад",
                duration = "3:58"
            ),
            YouTubeVideo(
                id = "fJ9rUzIMcZQ",
                title = "Queen – Bohemian Rhapsody (Official Video)",
                description = "Bohemian Rhapsody by Queen",
                thumbnailUrl = "https://i.ytimg.com/vi/fJ9rUzIMcZQ/maxresdefault.jpg",
                channelTitle = "Queen Official",
                channelId = "UCiMhD4jzUqG-IgPzUmmytRQ",
                viewCount = "1.8 млрд",
                publishedAt = "14 лет назад",
                duration = "5:55"
            ),
            YouTubeVideo(
                id = "CevxZvSJLk8",
                title = "Katy Perry - Roar (Official)",
                description = "Roar by Katy Perry",
                thumbnailUrl = "https://i.ytimg.com/vi/CevxZvSJLk8/maxresdefault.jpg",
                channelTitle = "Katy Perry",
                channelId = "UC-ODQ-HfPP8FPv45Xwg3uiA",
                viewCount = "3.8 млрд",
                publishedAt = "11 лет назад",
                duration = "4:30"
            ),
            YouTubeVideo(
                id = "hT_nvWreIhg",
                title = "OneRepublic - Counting Stars",
                description = "Counting Stars by OneRepublic",
                thumbnailUrl = "https://i.ytimg.com/vi/hT_nvWreIhg/maxresdefault.jpg",
                channelTitle = "OneRepublic",
                channelId = "UCnCp4oXp9kGhYKFhBz5tZew",
                viewCount = "4 млрд",
                publishedAt = "11 лет назад",
                duration = "4:44"
            ),
            YouTubeVideo(
                id = "YQHsXMglC9A",
                title = "Adele - Hello (Official Music Video)",
                description = "Hello by Adele",
                thumbnailUrl = "https://i.ytimg.com/vi/YQHsXMglC9A/maxresdefault.jpg",
                channelTitle = "Adele",
                channelId = "UComPepzeQI4mM4s_8Jc3I_g",
                viewCount = "3.2 млрд",
                publishedAt = "9 лет назад",
                duration = "6:07"
            )
        )
    }
}
