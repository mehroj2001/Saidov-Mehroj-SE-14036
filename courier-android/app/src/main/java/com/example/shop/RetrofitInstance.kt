package com.example.shop

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitInstance {

    val api: ShopAPI by lazy {
        Retrofit.Builder().baseUrl("https://shopproject-ten.vercel.app")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ShopAPI::class.java)
    }
}