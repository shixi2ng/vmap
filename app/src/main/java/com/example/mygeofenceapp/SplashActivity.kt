package com.example.mygeofenceapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import com.example.mygeofenceapp.R

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 隐藏系统UI以实现全屏沉浸感
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)

        setContentView(R.layout.activity_splash)

        val videoView = findViewById<VideoView>(R.id.videoViewSplash)

        // 构建视频资源的URI
        val videoPath = "android.resource://$packageName/${R.raw.intro_video}"
        videoView.setVideoURI(Uri.parse(videoPath))

        // 监听视频播放结束
        videoView.setOnCompletionListener {
            navigateToMain()
        }

        // 允许用户点击跳过视频
        videoView.setOnClickListener {
            navigateToMain()
        }

        videoView.start()
    }

    private fun navigateToMain() {
        if (isFinishing) return

        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)

        // 核心需求：淡出效果 [4, 12]
        // 第一个参数是进入Activity的动画（淡入），第二个是退出Activity的动画（淡出）
        overridePendingTransition(android.R.anim.fade_in, R.anim.fade_out)

        finish()
    }
}