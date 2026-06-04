package com.agon.app.data.models

import kotlinx.serialization.Serializable

@Serializable
data class YouTubeVideo(
    val id: String,
    val title: String,
    val description: String,
    val thumbnailUrl: String,
    val channelTitle: String,
    val channelId: String,
    val channelAvatarUrl: String = "",
    val viewCount: String,
    val publishedAt: String,
    val duration: String,
    val likeCount: String = "",
    val subscriberCount: String = "",
    val isLive: Boolean = false
)

@Serializable
data class UserProfile(
    val id: String,
    val name: String,
    val email: String,
    val photoUrl: String,
    val accessToken: String = "",
    val isSignedIn: Boolean = false
)

@Serializable
data class WatchHistoryItem(
    val videoId: String,
    val video: YouTubeVideo,
    val watchedAt: Long = System.currentTimeMillis()
)