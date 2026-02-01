package com.example.mygeofenceapp

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.TilesOverlay

object RetroStyler {

    /**
     * 将地图视图转换为复古棕褐色调
     */
    fun applySepiaFilter(mapView: MapView) {
        val sepiaMatrix = ColorMatrix()

        // 步骤1：去色（转换为黑白）
        sepiaMatrix.setSaturation(0f)

        // 步骤2：应用棕色色阶缩放
        // R=1.0, G=0.95, B=0.82 是一种模拟旧纸张的经典比例
        val scaleMatrix = ColorMatrix()
        scaleMatrix.setScale(1.0f, 0.95f, 0.82f, 1.0f)

        // 合并矩阵
        sepiaMatrix.postConcat(scaleMatrix)

        val colorFilter = ColorMatrixColorFilter(sepiaMatrix)

        // 获取地图的瓦片覆盖层并应用滤镜
        // 通常底图瓦片是覆盖层列表中的第一个
        val tileOverlay = mapView.overlays.firstOrNull { it is TilesOverlay } as? TilesOverlay
        tileOverlay?.setColorFilter(colorFilter)

        // 也可以设置加载时的背景色为米色，避免黑屏
        tileOverlay?.loadingBackgroundColor = 0xFFFDF5E6.toInt()
        tileOverlay?.loadingLineColor = 0x00000000
    }
}