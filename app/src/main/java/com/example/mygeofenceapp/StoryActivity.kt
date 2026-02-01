package com.example.mygeofenceapp

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.mygeofenceapp.R

class StoryActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_story)

        // 获取传递的数据
        val title = intent.getStringExtra("EXTRA_TITLE")
        val content = intent.getStringExtra("EXTRA_CONTENT")
        val imageResId = intent.getIntExtra("EXTRA_IMAGE", 0)

        // 绑定视图
        findViewById<TextView>(R.id.tvStoryTitle).text = title
        findViewById<TextView>(R.id.tvStoryContent).text = content

        if (imageResId!= 0) {
            findViewById<ImageView>(R.id.ivStoryImage).setImageResource(imageResId)
        }
    }
}