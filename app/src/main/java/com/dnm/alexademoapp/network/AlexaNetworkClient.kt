package com.dnm.alexademoapp.network

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Header

interface AlexaNetworkClient {

    @GET("/ping")
    fun ping() : Call<String>
}