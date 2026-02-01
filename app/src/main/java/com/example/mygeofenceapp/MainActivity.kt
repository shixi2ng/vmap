package com.example.mygeofenceapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.example.mygeofenceapp.R
import com.example.mygeofenceapp.RetroTileSourceFactory
import com.example.mygeofenceapp.StoryPoint
import com.example.mygeofenceapp.RetroStyler
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.IMyLocationConsumer
import org.osmdroid.views.overlay.mylocation.IMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.io.File

val customTileSource = object : org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase(
    "OpenStreetMap",
    0, 18, 256, ".png",
    arrayOf("https://tile.openstreetmap.org/") // 这里只写根路径
) {
    override fun getTileURLString(pTileIndex: Long): String {
        // 使用 osmdroid 自带的方法提取索引，确保坐标正确
        val z = org.osmdroid.util.MapTileIndex.getZoom(pTileIndex)
        val x = org.osmdroid.util.MapTileIndex.getX(pTileIndex)
        val y = org.osmdroid.util.MapTileIndex.getY(pTileIndex)

        // 返回完整的拼接 URL
        return "$baseUrl$z/$x/$y.png"
    }
}

class MainActivity : AppCompatActivity(), IMyLocationConsumer {

    private lateinit var mapView: MapView
    private lateinit var locationOverlay: MyLocationNewOverlay
    private lateinit var tvCoordinates: TextView
    private lateinit var locationProvider: GpsMyLocationProvider

    // 预设的宝藏点数据（示例）
    private val storyPoints = listOf(
        StoryPoint("1", "古老的钟楼", GeoPoint(39.9042, 116.4074), 50.0,
            "你站在古老的钟楼下，听到了来自百年前的钟声回荡...", R.drawable.img_story_clock),
        StoryPoint("2", "遗忘的图书馆", GeoPoint(39.9050, 116.4080), 30.0,
            "尘封的书卷散发着霉味，这里藏着城市的秘密。", R.drawable.img_story_library)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. 初始化 Osmdroid 配置
        // 必须在 setContentView 之前调用，加载 User-Agent 防止被服务器屏蔽
        Configuration.getInstance().load(
            applicationContext,
            androidx.preference.PreferenceManager.getDefaultSharedPreferences(applicationContext)
        )


        setContentView(R.layout.activity_main)

        mapView = findViewById(R.id.mapView)
        tvCoordinates = findViewById(R.id.tvCoordinates)

        setupMap()
        checkPermissions()
    }

    private fun setupMap() {
        // 设置多点触控缩放
        mapView.setMultiTouchControls(true)
        mapView.controller.setZoom(18.0)


        // 如果离线文件不存在，回退到默认在线地图，但应用复古滤镜
        mapView.setTileSource(customTileSource)
        RetroStyler.applySepiaFilter(mapView) // [2]


        // 在地图上绘制电子围栏的范围（可选，便于调试）
        drawGeofences()
    }

    private fun drawGeofences() {
        storyPoints.forEach { point ->
            val circle = Polygon()
            circle.points = Polygon.pointsAsCircle(point.location, point.radiusMeters)
            circle.fillPaint.color = 0x225D4037 // 半透明棕色
            circle.outlinePaint.color = 0xFF5D4037.toInt()
            circle.strokeWidth = 2f
            mapView.overlays.add(circle)
        }
    }

    private fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            initLocation()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                100
            )
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            initLocation()
        } else {
            Toast.makeText(this, "需要定位权限才能进行寻宝！", Toast.LENGTH_LONG).show()
        }
    }

    private fun initLocation() {
        locationProvider = GpsMyLocationProvider(this)
        // 添加自定义的位置更新监听器
        locationProvider.startLocationProvider(this)

        locationOverlay = MyLocationNewOverlay(locationProvider, mapView)
        locationOverlay.enableMyLocation()
        locationOverlay.enableFollowLocation() // 跟随模式

        // 更换为复古风格的玩家图标 [21, 22, 23]
        // 需准备 ic_retro_player.png 资源
        val iconDrawable = ResourcesCompat.getDrawable(resources, R.drawable.ic_retro_player, null)
        val iconBitmap = (iconDrawable as BitmapDrawable).bitmap
        locationOverlay.setPersonIcon(iconBitmap)
        locationOverlay.setDirectionArrow(iconBitmap, iconBitmap)

        mapView.overlays.add(locationOverlay)
    }

    // 实现 IMyLocationConsumer 接口，获取实时位置回调 [24]
    override fun onLocationChanged(location: android.location.Location?, source: IMyLocationProvider?) {
        location?.let { loc ->
            // 1. 更新UI显示的坐标
            val latStr = String.format("%.4f", loc.latitude)
            val lonStr = String.format("%.4f", loc.longitude)
            runOnUiThread {
                tvCoordinates.text = "LAT: $latStr  LON: $lonStr"
            }

            // 2. 检查电子围栏逻辑 [25, 26]
            checkGeofences(GeoPoint(loc.latitude, loc.longitude))
        }
    }

    private fun checkGeofences(userPos: GeoPoint) {
        storyPoints.forEach { point ->
            if (!point.isUnlocked) { // 仅检查未解锁的点
                val distance = userPos.distanceToAsDouble(point.location)

                if (distance <= point.radiusMeters) {
                    // 触发！
                    triggerStoryEvent(point)
                }
            }
        }
    }

    private fun triggerStoryEvent(point: StoryPoint) {
        point.isUnlocked = true // 标记为已触发，避免重复弹窗

        runOnUiThread {
            // 使用复古风格的 Dialog 询问用户
            AlertDialog.Builder(this, R.style.RetroDialogTheme)
                .setTitle("发现线索：${point.title}")
                .setMessage("你已到达关键位置。是否查看这里发生过的故事？")
                .setPositiveButton("查看") { _, _ ->
                    // 跳转到故事界面 [4]
                    val intent = Intent(this, StoryActivity::class.java).apply {
                        putExtra("EXTRA_TITLE", point.title)
                        putExtra("EXTRA_CONTENT", point.content)
                        putExtra("EXTRA_IMAGE", point.imageResId)
                    }
                    startActivity(intent)
                }
                .setNegativeButton("忽略", null)
                .show()
        }
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
        // 注意：实际生产中应考虑在此处暂停位置更新以省电，但为了游戏连续性需谨慎处理
    }
}