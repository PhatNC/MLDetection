package com.example.mldetectionproject

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.example.mldetectionproject.helpers.ImageHelperActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    fun onGotoImageActivity(view: View) {
        val intent = Intent(view.context, ImageHelperActivity::class.java)
        view.context.startActivity(intent)
    }
}