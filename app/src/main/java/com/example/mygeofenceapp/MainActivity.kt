package com.example.mygeofenceapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import android.widget.ImageButton
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.webkit.WebViewAssetLoader
import com.example.mygeofenceapp.R
import com.example.mygeofenceapp.StoryPoint
import org.osmdroid.util.GeoPoint
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity(), LocationListener {

    private lateinit var mapWebView: WebView
    private lateinit var tvCoordinates: TextView
    private lateinit var btnRecenter: ImageButton
    private lateinit var btnTrack: ImageButton
    private lateinit var btnMemory: Button
    private lateinit var locationManager: LocationManager
    private var mapReady = false
    private var pendingLocation: Location? = null
    private var lastLocation: Location? = null
    private var pendingRecenter = false
    private var pendingTrackStart = false
    private val mapAssetUrl = "https://appassets.androidplatform.net/assets/map.html"
    private lateinit var assetLoader: WebViewAssetLoader
    private var isTracking = false
    private val trackPoints = mutableListOf<Location>()
    private var trackStartTime: Long? = null
    private var storyAreasRendered = false
    private var activeStoryPoint: StoryPoint? = null


    // 预设的宝藏点数据（示例）
    private val storyPoints = listOf(
        StoryPoint(
            "1",
            "回忆区域",
            listOf(
                GeoPoint(55.947018, -3.187909),
                GeoPoint(55.947276, -3.186252),
                GeoPoint(55.947276, -3.186252),
                GeoPoint(55.947668, -3.188315)
            ),
            "你走进了记忆深处，仿佛能听到这片土地曾经的呢喃。",
            R.drawable.img_story_clock
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        mapWebView = findViewById(R.id.mapView)
        tvCoordinates = findViewById(R.id.tvCoordinates)
        btnRecenter = findViewById(R.id.btnRecenter)
        btnTrack = findViewById(R.id.btnTrack)
        btnMemory = findViewById(R.id.btnMemory)
        setupMap()
        setupRecenter()
        setupTrackToggle()
        hideLegacyMemoryButton()
        checkPermissions()
    }

    private fun setupMap() {
        assetLoader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .addPathHandler("/res/", WebViewAssetLoader.ResourcesPathHandler(this))
            .build()
        mapWebView.settings.javaScriptEnabled = true
        mapWebView.settings.domStorageEnabled = true
        mapWebView.settings.allowFileAccess = true
        mapWebView.settings.allowContentAccess = true
        mapWebView.settings.allowFileAccessFromFileURLs = true
        mapWebView.settings.allowUniversalAccessFromFileURLs = true
        mapWebView.settings.cacheMode = WebSettings.LOAD_NO_CACHE
        mapWebView.clearCache(true)
        mapWebView.addJavascriptInterface(MapBridge(), "Android")
        mapWebView.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(
                view: WebView,
                request: WebResourceRequest
            ): WebResourceResponse? {
                return assetLoader.shouldInterceptRequest(request.url)
            }

            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                mapReady = true
                pendingLocation?.let { updateMapLocation(it) }
                pendingLocation = null
                if (pendingRecenter) {
                    centerMapOnUser()
                    pendingRecenter = false
                }
                if (pendingTrackStart) {
                    startTrackLineOnMap()
                    pendingTrackStart = false
                }
                if (isTracking) {
                    syncTrackPointsToMap()
                }
                if (!storyAreasRendered) {
                    renderStoryAreasOnMap()
                }
            }
        }
        mapWebView.loadUrl(mapAssetUrl)
    }

    private fun setupRecenter() {
        btnRecenter.setOnClickListener {
            if (lastLocation == null) {
                Toast.makeText(this, "尚未获取定位信息", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            centerMapOnUser()
        }
    }

    private fun setupTrackToggle() {
        btnTrack.setOnClickListener {
            if (isTracking) {
                stopTracking()
            } else {
                startTracking()
            }
        }
    }

    private fun hideLegacyMemoryButton() {
        btnMemory.visibility = View.GONE
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
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                2000L,
                5f,
                this
            )
        }
        if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            locationManager.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER,
                5000L,
                10f,
                this
            )
        }
    }

    override fun onLocationChanged(location: Location) {
        // 1. 更新UI显示的坐标
        val latStr = String.format("%.4f", location.latitude)
        val lonStr = String.format("%.4f", location.longitude)
        lastLocation = location
        runOnUiThread {
            tvCoordinates.text = "LAT: $latStr  LON: $lonStr"
        }

        updateMapLocation(location)
        if (isTracking) {
            appendTrackingPoint(location)
        }
    }

    private fun updateMapLocation(location: Location) {
        if (!mapReady) {
            pendingLocation = location
            return
        }
        val script = "updateLocation(${location.latitude}, ${location.longitude});"
        mapWebView.evaluateJavascript(script, null)
    }

    private fun centerMapOnUser() {
        if (!mapReady) {
            pendingRecenter = true
            return
        }
        if (lastLocation == null) {
            return
        }
        mapWebView.evaluateJavascript("centerOnUser(17);", null)
    }

    private fun startTracking() {
        isTracking = true
        trackStartTime = System.currentTimeMillis()
        trackPoints.clear()
        btnTrack.setImageResource(R.drawable.ic_track_stop)
        if (mapReady) {
            startTrackLineOnMap()
        } else {
            pendingTrackStart = true
        }
        lastLocation?.let { appendTrackingPoint(it) }
        Toast.makeText(this, "开始记录行走轨迹", Toast.LENGTH_SHORT).show()
    }

    private fun stopTracking() {
        isTracking = false
        pendingTrackStart = false
        trackStartTime = trackStartTime ?: System.currentTimeMillis()
        btnTrack.setImageResource(R.drawable.ic_track_play)
        if (mapReady) {
            mapWebView.evaluateJavascript("finishTrackLine();", null)
        }
        val geoJsonFile = writeTrackGeoJson()
        val message = if (geoJsonFile != null) {
            "轨迹已保存：${geoJsonFile.name}"
        } else {
            "轨迹记录已停止"
        }
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun appendTrackingPoint(location: Location) {
        trackPoints.add(Location(location))
        if (mapReady) {
            val script = "addTrackPoint(${location.latitude}, ${location.longitude});"
            mapWebView.evaluateJavascript(script, null)
        }
    }

    private fun startTrackLineOnMap() {
        mapWebView.evaluateJavascript("startTrackLine();", null)
    }

    private fun syncTrackPointsToMap() {
        mapWebView.evaluateJavascript("startTrackLine();", null)
        trackPoints.forEach { point ->
            val script = "addTrackPoint(${point.latitude}, ${point.longitude});"
            mapWebView.evaluateJavascript(script, null)
        }
    }

    private fun writeTrackGeoJson(): File? {
        val startTime = trackStartTime ?: return null
        val formatter = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        val fileName = "${formatter.format(Date(startTime))}.geojson"
        val file = File(filesDir, fileName)
        val startTimeText = formatter.format(Date(startTime))
        val endTimeText = formatter.format(Date())
        val coordinates = buildString {
            append("[")
            trackPoints.forEachIndexed { index, point ->
                if (index > 0) {
                    append(",")
                }
                append("[${point.longitude},${point.latitude}]")
            }
            append("]")
        }
        val geoJson = """
            {
              "type": "FeatureCollection",
              "features": [
                {
                  "type": "Feature",
                  "properties": {
                    "startTime": "$startTimeText",
                    "endTime": "$endTimeText"
                  },
                  "geometry": {
                    "type": "LineString",
                    "coordinates": $coordinates
                  }
                }
              ]
            }
        """.trimIndent()
        file.writeText(geoJson)
        return file
    }

    private fun renderStoryAreasOnMap() {
        if (!mapReady) {
            return
        }
        storyPoints.forEach { point ->
            val coords = point.polygon.joinToString(prefix = "[", postfix = "]") {
                "[${it.latitude}, ${it.longitude}]"
            }
            val title = point.title.replace("\"", "\\\"")
            val id = point.id.replace("\\", "\\\\").replace("\"", "\\\"")
            mapWebView.evaluateJavascript(
                "renderStoryArea(\"$id\", $coords, \"$title\");",
                null
            )
        }
        storyAreasRendered = true
    }


    private inner class MapBridge {
        @android.webkit.JavascriptInterface
        fun onMemoryClick(storyId: String) {
            val point = storyPoints.firstOrNull { it.id == storyId } ?: return
            runOnUiThread {
                triggerStoryEvent(point)
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
    }

    override fun onPause() {
        super.onPause()
        if (::locationManager.isInitialized) {
            locationManager.removeUpdates(this)
        }
        // 注意：实际生产中应考虑在此处暂停位置更新以省电，但为了游戏连续性需谨慎处理
    }
}
