package com.example.myapplication

data class Post(
    var id: String = "",
    val title: String = "",
    val content: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    var region: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val authorName: String = "익명",
    val profileImage: String = "profile1",
    val userId: String = "",
    val likeCount: Int = 0,             //  좋아요 수
    val commentCount: Int = 0,          //  댓글 수
)


