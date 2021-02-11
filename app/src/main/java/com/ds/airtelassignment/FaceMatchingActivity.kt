package com.ds.airtelassignment

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
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

class FaceMatchingActivity : AppCompatActivity() {

    var isUserImageAdded = false
    var isIdCardImageAdded = false
    var isUserImage = false
    var isUserImageFaceFound=false
    var isIdCardImageFaceFound=false
    var userImageBitmap: Bitmap? = null
    var idCardImageBitmap: Bitmap? = null

    private val faceMatchinghelper: FaceMatchinghelper by lazy {
        FaceMatchinghelper(this@FaceMatchingActivity)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tv_firstImage.setOnClickListener {
            isUserImage = true
            startCropScreen()   // open camera
        }
        tv_secondImage.setOnClickListener {
            isUserImage = false
            startCropScreen() // open camera
        }

        btn_calculate.setOnClickListener {
            if(!isUserImageAdded){
                Toast.makeText(this@FaceMatchingActivity,"Please add User image.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if(!isIdCardImageAdded){
                Toast.makeText(this@FaceMatchingActivity,"Please add Id Card image.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if(!isUserImageFaceFound){
                Toast.makeText(this@FaceMatchingActivity,"Face Not found in User image", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if(!isIdCardImageFaceFound){
                Toast.makeText(this@FaceMatchingActivity,"Face Not found in Id Card image", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            var highestMatchingPercentage = -1f
            var result: String
            val p = faceMatchinghelper.findNearest(faceMatchinghelper.userImageBorders,faceMatchinghelper.idCardImageBorders)

            result =  if ( p > highestMatchingPercentage && p < 1f) {
                highestMatchingPercentage = p
                "User And Id Card Images Matching"
            }else{
                "User And Id Card Images didn't Matching"
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
                Toast.makeText(this@FaceMatchingActivity,"No face Found.Please try again", Toast.LENGTH_SHORT).show()
                if (isUserImage)
                    isUserImageFaceFound = false
                else
                    isIdCardImageFaceFound = false
            }else{
                Toast.makeText(this@FaceMatchingActivity,"Found ${faces.size} faces ", Toast.LENGTH_SHORT).show()
                if (isUserImage) {
                    faceMatchinghelper.createFirstEmbedding()
                    faceMatchinghelper.userImageBorders =
                        faceMatchinghelper.getFaceEmbedding(userImageBitmap!!,faces[0].boundingBox)
                    isUserImageFaceFound=true
                }else{
                    faceMatchinghelper.createSecondEmbedding()
                    faceMatchinghelper.idCardImageBorders =
                        faceMatchinghelper.getFaceEmbedding(idCardImageBitmap!!,faces[0].boundingBox)
                    isIdCardImageFaceFound=true

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
        Toast.makeText(this@FaceMatchingActivity,"Error while detecting the face", Toast.LENGTH_SHORT).show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            val result = CropImage.getActivityResult(data)
            if (resultCode == Activity.RESULT_OK) {
                val resultUri = result.uri
                val selectedBitmap= MediaStore.Images.Media.getBitmap(this.contentResolver, resultUri)
                if (isUserImage) { // result for the first image
                    // show image to ui
                    FaceMatchingApplication.getGlide().load(resultUri).into(iv_FirstImage)
                    isUserImageAdded = true
                    userImageBitmap =selectedBitmap

                } else {
                    FaceMatchingApplication.getGlide().load(resultUri).into(ivSecondImage)
                    isIdCardImageAdded = true
                    idCardImageBitmap = selectedBitmap
                }

                showProgress(true)
                val image: FirebaseVisionImage
                try {
                    image = FirebaseVisionImage.fromByteArray(bitmapToNV21(selectedBitmap),
                        getVisionMetaData(selectedBitmap))
                    faceMatchinghelper.detectFace(image,onfaceDetected,errorWhileDetectingFace)
                } catch (e: IOException) {
                    showProgress(false)
                    Toast.makeText(this@FaceMatchingActivity,"Exception while getting the Image from file path", Toast.LENGTH_SHORT).show()
                }
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                val error = result.error
            }
        }
    }

}