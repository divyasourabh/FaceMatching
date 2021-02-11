package com.ds.airtelassignment

import android.graphics.Bitmap

const val MODEL_FACE_SIZE = 112
const val IMAGE_MEAN = 128.0f
const val IMAGE_STD = 128.0f

fun bitmapToNV21(bitmap: Bitmap): ByteArray {
    val argb = IntArray(bitmap.width * bitmap.height )
    bitmap.getPixels(argb, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
    val yuv = ByteArray(bitmap.height * bitmap.width + 2 * Math.ceil(bitmap.height / 2.0).toInt()
            * Math.ceil(bitmap.width / 2.0).toInt())
    encodeYUV420SP( yuv, argb, bitmap.width, bitmap.height)
    return yuv
}

private fun encodeYUV420SP(yuv420sp: ByteArray, argb: IntArray, width: Int, height: Int) {
    val frameSize = width * height
    var yIndex = 0
    var uvIndex = frameSize
    var R: Int
    var G: Int
    var B: Int
    var Y: Int
    var U: Int
    var V: Int
    var index = 0
    for (j in 0 until height) {
        for (i in 0 until width) {
            R = argb[index] and 0xff0000 shr 16
            G = argb[index] and 0xff00 shr 8
            B = argb[index] and 0xff shr 0
            Y = (66 * R + 129 * G + 25 * B + 128 shr 8) + 16
            U = (-38 * R - 74 * G + 112 * B + 128 shr 8) + 128
            V = (112 * R - 94 * G - 18 * B + 128 shr 8) + 128
            yuv420sp[yIndex++] = (if (Y < 0) 0 else if (Y > 255) 255 else Y).toByte()
            if (j % 2 == 0 && index % 2 == 0) {
                yuv420sp[uvIndex++] = (if (V < 0) 0 else if (V > 255) 255 else V).toByte()
                yuv420sp[uvIndex++] = (if (U < 0) 0 else if (U > 255) 255 else U).toByte()
            }
            index++
        }
    }
}
