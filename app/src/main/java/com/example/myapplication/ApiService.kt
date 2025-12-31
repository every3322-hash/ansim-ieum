package com.example.myapplication

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {
    @GET("{endpoint}")
    fun getSafetyInfo(
        @Path("endpoint") endpoint: String,
        @Query("serviceKey") serviceKey: String,
        @Query("pageNo") pageNo: Int,
        @Query("numOfRows") numOfRows: Int,
        @Query("returnType") returnType: String,
        @Query("safety_cate") safetyCate: String? = null
    ): Call<ApiResponse>
}

