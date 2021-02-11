package com.ds.airtelassignment

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata
import com.google.firebase.ml.vision.face.FirebaseVisionFace
import com.theartofdev.edmodo.cropper.CropImage
import com.theartofdev.edmodo.cropper.CropImageView
import kotlinx.android.synthetic.main.activity_main.*
import java.io.IOException

class MainActivity : AppCompatActivity() {

    var isFirstImageAdded = false
    var isSecondImageAdded = false
    var isFirstImage = false
    var isFirstImageFaceFound=false
    var isSecondImageFaceFound=false
    var firstImageBitmap: Bitmap? = null
    var secondImageBitmap: Bitmap? = null

    private val faceMatchinghelper: FaceMatchinghelper by lazy {
        FaceMatchinghelper(this@MainActivity)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tv_firstImage.setOnClickListener {
            isFirstImage = true
            startCropScreen()   // open camera
        }
        tv_secondImage.setOnClickListener {
            isFirstImage = false
            startCropScreen() // open camera
        }

        btn_calculate.setOnClickListener {
            if(!isFirstImageAdded){
                Toast.makeText(this@MainActivity,"Please add first image.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if(!isSecondImageAdded){
                Toast.makeText(this@MainActivity,"Please add second image.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if(!isFirstImageFaceFound){
                Toast.makeText(this@MainActivity,"Face Not found in First image", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if(!isSecondImageFaceFound){
                Toast.makeText(this@MainActivity,"Face Not found in Second image", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            var highestMatchingPercentage = -1f
            var result: String
            val p = faceMatchinghelper.findNearest(faceMatchinghelper.firstImageBorders,faceMatchinghelper.secondImageBorders)

            result =  if ( p > highestMatchingPercentage && p < 1f) {
                highestMatchingPercentage = p
                "First And Second Images Matching"
            }else{
                "First And Second Images didn't Matching"
            }

            val format = String.format("%.2f", highestMatchingPercentage)
            var percentage=(100 - (format.toFloat() * 100f))
            if(percentage>100)
                percentage=0.0f

            AlertDialog.Builder(this).setTitle(result).setMessage("Matching score : $percentage out of 100")
                .setPositiveButton("ok") { _, _-> }.show()
        }

        faceMatchinghelper.setupDetectorOption()
    }

    private fun getVisionMetaData(selectedBitmap: Bitmap)=
        FirebaseVisionImageMetadata.Builder()
            .setWidth(selectedBitmap.width)
            .setHeight(selectedBitmap.height)
            .setFormat(FirebaseVisionImageMetadata.IMAGE_FORMAT_NV21)
            .setRotation(FirebaseVisionImageMetadata.ROTATION_0)
            .build()


    var onfaceDetected :(MutableList<FirebaseVisionFace>?)->Unit={
        showProgress(false)
        it?.let { faces ->
            if (faces.isEmpty()) {
                Toast.makeText(this@MainActivity,"No face Found.Please try again", Toast.LENGTH_SHORT).show()
                if (isFirstImage)
                    isFirstImageFaceFound = false
                else
                    isSecondImageFaceFound = false
            }else{
                Toast.makeText(this@MainActivity,"Found ${faces.size} faces ", Toast.LENGTH_SHORT).show()
                if (isFirstImage) {
                    faceMatchinghelper.createFirstEmbedding()
                    faceMatchinghelper.firstImageBorders =
                        faceMatchinghelper.getFaceEmbedding(firstImageBitmap!!,faces[0].boundingBox)
                    isFirstImageFaceFound=true
                }else{
                    faceMatchinghelper.createSecondEmbedding()
                    faceMatchinghelper.secondImageBorders =
                        faceMatchinghelper.getFaceEmbedding(secondImageBitmap!!,faces[0].boundingBox)
                    isSecondImageFaceFound=true

                }
            }
        }
    }

    private fun showProgress(flag:Boolean){
        if(flag){
            window.setFlags(
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
            progressBar.visibility= View.VISIBLE

        }else{
            window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
            progressBar.visibility= View.GONE
        }

    }

    private fun startCropScreen() {
        CropImage.activity()
            .setGuidelines(CropImageView.Guidelines.ON)
            .start(this)
    }

    var errorWhileDetectingFace :(Exception)->Unit={
        showProgress(false)
        Toast.makeText(this@MainActivity,"Error while detecting the face", Toast.LENGTH_SHORT).show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            val result = CropImage.getActivityResult(data)
            if (resultCode == Activity.RESULT_OK) {
                val resultUri = result.uri
                val selectedBitmap= MediaStore.Images.Media.getBitmap(this.contentResolver, resultUri)
                if (isFirstImage) { // result for the first image
                    // show image to ui
                    FaceMatchApplication.getGlide().load(resultUri).into(iv_FirstImage)
                    isFirstImageAdded = true
                    firstImageBitmap =selectedBitmap

                } else {
                    FaceMatchApplication.getGlide().load(resultUri).into(ivSecondImage)
                    isSecondImageAdded = true
                    secondImageBitmap = selectedBitmap
                }

                showProgress(true)
                val image: FirebaseVisionImage
                try {
                    image = FirebaseVisionImage.fromByteArray(bitmapToNV21(selectedBitmap),
                        getVisionMetaData(selectedBitmap))
                    faceMatchinghelper.detectFace(image,onfaceDetected,errorWhileDetectingFace)
                } catch (e: IOException) {
                    showProgress(false)
                    Toast.makeText(this@MainActivity,"Exception while getting the Image from file path", Toast.LENGTH_SHORT).show()
                }
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                val error = result.error
            }
        }
    }

}