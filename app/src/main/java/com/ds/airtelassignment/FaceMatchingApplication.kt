package com.ds.airtelassignment

import android.app.Application
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.bumptech.glide.request.RequestOptions

class FaceMatchingApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val provideRequestOptions = provideRequestOptions()

        glideInstance = provideGlideInstance(this, provideRequestOptions)
    }

    private fun provideRequestOptions(): RequestOptions {
        return RequestOptions.placeholderOf(R.drawable.image_placeholder)
            .error(R.drawable.image_placeholder)
    }

    private fun provideGlideInstance(application: Application, requestOptions: RequestOptions) =
        Glide.with(application).setDefaultRequestOptions(requestOptions)
    companion object{
        private lateinit var  glideInstance: RequestManager

        fun getGlide()= glideInstance
    }
}