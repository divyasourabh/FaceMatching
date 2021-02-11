package com.ds.airtelassignment

import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

class MainActivity : AppCompatActivity() {

    var isFirstImageAdded = false
    var isSecondImageAdded = false
    var isFirstImage = false
    var isFirstImageFaceFound=false
    var isSecondImageFaceFound=false
    var firstImageBitmap: Bitmap? = null
    var secondImageBitmap: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }
}