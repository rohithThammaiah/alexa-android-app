package com.dnm.alexademoapp

import android.app.Application
import com.dnm.alexademoapp.network.AlexaNetworkClient
import com.dnm.alexademoapp.utils.Constants
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory

class AlexaApplication : Application() {

    private var alexaNetworkClient: AlexaNetworkClient? = null

    companion object {
        var globalAccessToken = ""
    }


    private var alexaApplication: AlexaApplication? = null

    fun getApplicationInstance(): AlexaApplication {

        if (alexaApplication == null) {
            alexaApplication = this
        }

        return alexaApplication as AlexaApplication
    }


    override fun onCreate() {
        super.onCreate()


    }


    fun getAlexaNetworkClient(): AlexaNetworkClient? {

        if (alexaNetworkClient == null) {
            val httpLoggingInterceptor = HttpLoggingInterceptor()
            httpLoggingInterceptor.level = HttpLoggingInterceptor.Level.BODY
            val okHttpClient = OkHttpClient
                    .Builder()
                    .addInterceptor(httpLoggingInterceptor)
                    .addInterceptor {
                        val request = it.request().newBuilder()
                                .addHeader("Authorization", "Bearer ${globalAccessToken}")
                                .build()
                        return@addInterceptor it.proceed(request)
                    }
            val retrofit = Retrofit.Builder()
                    .baseUrl(Constants.alexaApiEndPointAsia)
                    .addConverterFactory(ScalarsConverterFactory.create())
                    .client(okHttpClient.build())
                    .build()

            alexaNetworkClient = retrofit.create(AlexaNetworkClient::class.java)
        }

        return alexaNetworkClient
    }
}