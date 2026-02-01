package com.example.mygeofenceapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
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

class MainActivity : AppCompatActivity(), LocationListener {

    private lateinit var mapWebView: WebView
    private lateinit var tvCoordinates: TextView
    private lateinit var btnRecenter: ImageButton
    private lateinit var locationManager: LocationManager
    private var mapReady = false
    private var pendingLocation: Location? = null
    private var lastLocation: Location? = null
    private var pendingRecenter = false
    private val mapAssetUrl = "https://appassets.androidplatform.net/assets/map.html"
    private lateinit var assetLoader: WebViewAssetLoader


    // 预设的宝藏点数据（示例）
    private val storyPoints = listOf(
        StoryPoint("1", "古老的钟楼", GeoPoint(39.9042, 116.4074), 50.0,
            "你站在古老的钟楼下，听到了来自百年前的钟声回荡...", R.drawable.img_story_clock),
        StoryPoint("2", "遗忘的图书馆", GeoPoint(39.9050, 116.4080), 30.0,
            "尘封的书卷散发着霉味，这里藏着城市的秘密。", R.drawable.img_story_library)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        mapWebView = findViewById(R.id.mapView)
        tvCoordinates = findViewById(R.id.tvCoordinates)
        btnRecenter = findViewById(R.id.btnRecenter)

        setupMap()
        setupRecenter()
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

        // 2. 检查电子围栏逻辑 [25, 26]
        checkGeofences(GeoPoint(location.latitude, location.longitude))

        updateMapLocation(location)
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
        mapWebView.evaluateJavascript("centerOnUser();", null)
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
    }

    override fun onPause() {
        super.onPause()
        if (::locationManager.isInitialized) {
            locationManager.removeUpdates(this)
        }
        // 注意：实际生产中应考虑在此处暂停位置更新以省电，但为了游戏连续性需谨慎处理
    }
}