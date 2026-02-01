package com.example.mygeofenceapp

import org.osmdroid.tileprovider.tilesource.XYTileSource

object RetroTileSourceFactory {

    // 创建一个自定义的瓦片源定义
    fun createRetroSource(): XYTileSource {
        return XYTileSource(
            "RetroCustom", // 瓦片源名称
            10,            // 最小缩放级别
            18,            // 最大缩放级别
            256,           // 瓦片像素尺寸
            ".png",        // 文件后缀
            arrayOf("")    // 在线URL（由于是离线自定义地图，此处留空或设为本地服务器）
        )
    }
}