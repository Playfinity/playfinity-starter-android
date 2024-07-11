package com.playfinity.recorder.presentation

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import io.playfinity.sdk.sample.R

@SuppressLint("MissingPermission")
class SmartBallActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_smartball)
    }
}
