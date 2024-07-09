package com.example.shop

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ShopAPI {
    @GET("/api/getShops")
    suspend fun getShops(): Response<ShopResponse>

    @POST("/api/addShop")
    suspend fun addShop(@Body shop: Shop): Response<ShopResponse>
}