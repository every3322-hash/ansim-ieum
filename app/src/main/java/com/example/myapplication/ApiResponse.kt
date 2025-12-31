package com.example.myapplication

data class ApiResponse(
    val header: Header,
    val numOfRows: Int,
    val pageNo: Int,
    val totalCount: Int,
    val body: List<SafetyItem>
)

data class Header(
    val resultMsg: String,
    val resultCode: String,
    val errorMsg: String?
)

data class SafetyItem(
    val actRmks: String?,
    val contentsUrl: String?,
    val safety_cate1: String?,
    val safety_cate2: String?,
    val safety_cate3: String?,
    val safety_cate4: String?,
    val safety_cate_nm1: String?,
    val safety_cate_nm2: String?,
    val safety_cate_nm3: String?
)
