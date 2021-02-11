package com.ds.airtelassignment

import android.content.Context
import android.graphics.*
import android.util.Log
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.face.FirebaseVisionFace
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sqrt

class FaceMatchinghelper(context: Context) {

    lateinit var userImageBorders: FloatArray
    lateinit var idCardImageBorders: FloatArray
    private val intValues: IntArray = IntArray(MODEL_FACE_SIZE * MODEL_FACE_SIZE)
    private lateinit var firebaseVisionFaceDetectorOptions: FirebaseVisionFaceDetectorOptions
    private val tensorflowInterpreter: Interpreter

    init {
        val interpreterOptions = Interpreter.Options().apply {
            setNumThreads(4)
        }
        tensorflowInterpreter = Interpreter(
            FileUtil.loadMappedFile(context, "face_matching_model.tflite"),
            interpreterOptions
        )
    }
    
    fun createFirstEmbedding() {
        userImageBorders = FloatArray(128)
    }

    fun createSecondEmbedding() {
        idCardImageBorders = FloatArray(128)
    }


    fun getFaceEmbedding(firstImageBitmap: Bitmap, boundingBox: Rect): FloatArray {
        return runFaceNet(
            convertBitmapToBuffer(
                createAndDrawCanvas(firstImageBitmap, MODEL_FACE_SIZE, boundingBox)
            )
        )[0]
    }

    private fun createAndDrawCanvas(
        imageBitmap: Bitmap,
        tfOdApiInputSize: Int,
        boundingBox: Rect
    ): Bitmap {
        var faceBmp = Bitmap.createBitmap(
            tfOdApiInputSize, tfOdApiInputSize,
            Bitmap.Config.ARGB_8888
        )
        val cvFace = Canvas(faceBmp)
        // maps original coordinates to portrait coordinates
        val faceBB = RectF(boundingBox)
        val sx: Float =
            (tfOdApiInputSize.toFloat()) / faceBB.width()
        val sy: Float =
            (tfOdApiInputSize.toFloat()) / faceBB.height()
        val matrix = Matrix()
        matrix.postTranslate(-faceBB.left, -faceBB.top)
        matrix.postScale(sx, sy)
        cvFace.drawBitmap(imageBitmap, matrix, null)
        return faceBmp
    }

    private fun convertBitmapToBuffer(bitmap: Bitmap): ByteBuffer {
        bitmap.getPixels(
            intValues,
            0,
            bitmap.width,
            0,
            0,
            bitmap.width,
            bitmap.height
        )
        var byteArray =
            ByteBuffer.allocateDirect(1 * MODEL_FACE_SIZE * MODEL_FACE_SIZE * 3 * 4)
                .apply {
                    order(ByteOrder.nativeOrder())
                }
        byteArray.rewind()
        for (i in 0 until MODEL_FACE_SIZE) {
            for (j in 0 until MODEL_FACE_SIZE) {
                val pixelValue = intValues[i * MODEL_FACE_SIZE + j]

                byteArray.putFloat(((pixelValue shr 16 and 0xFF) - IMAGE_MEAN) / IMAGE_STD)
                byteArray.putFloat(((pixelValue shr 8 and 0xFF) - IMAGE_MEAN) / IMAGE_STD)
                byteArray.putFloat(((pixelValue and 0xFF) - IMAGE_MEAN) / IMAGE_STD)
            }
        }
        return byteArray
    }

    private fun runFaceNet(inputs: ByteBuffer): Array<FloatArray> {

        val t1 = System.currentTimeMillis()
        val outputs = Array(1) { FloatArray(192) }
        tensorflowInterpreter.run(inputs, outputs)
        Log.i("Performance", "FaceNet Inference Speed in ms : ${System.currentTimeMillis() - t1}")
        return outputs
    }

    fun findNearest(emb1: FloatArray, emb2: FloatArray): Float {
        var distance = 0f
        for (i in emb1.indices) {
            val diff = emb1[i] - emb2[i]
            distance += diff * diff
        }
        distance = sqrt(distance.toDouble()).toFloat()
        return distance

    }

    fun setupDetectorOption() {
        firebaseVisionFaceDetectorOptions =
            FirebaseVisionFaceDetectorOptions.Builder()
                .setPerformanceMode(FirebaseVisionFaceDetectorOptions.ACCURATE)
                .setLandmarkMode(FirebaseVisionFaceDetectorOptions.NO_LANDMARKS)
                .setClassificationMode(FirebaseVisionFaceDetectorOptions.NO_CLASSIFICATIONS)
                .build()
    }

    fun detectFace(
        image: FirebaseVisionImage, success: (MutableList<FirebaseVisionFace>?) -> Unit,
        failure: (Exception) -> Unit
    ) {
        FirebaseVision.getInstance()
            .getVisionFaceDetector(firebaseVisionFaceDetectorOptions).detectInImage(image)
            .addOnSuccessListener {
                success(it)
            }
            .addOnFailureListener {
                failure(it)
            }
    }
}