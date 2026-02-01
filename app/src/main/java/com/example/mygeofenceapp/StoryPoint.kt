package com.example.mygeofenceapp

import org.osmdroid.util.GeoPoint

data class StoryPoint(
    val id: String,
    val title: String,
    val location: GeoPoint,
    val radiusMeters: Double = 20.0, // 默认20米触发范围
    val content: String,
    val imageResId: Int, // 故事配图资源ID
    var isUnlocked: Boolean = false // 状态标记，防止重复触发
)