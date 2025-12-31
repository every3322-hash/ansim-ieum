package com.example.myapplication

data class Comment(
    val content: String = "",
    val author: String = "익명",
    val timestamp: Long = System.currentTimeMillis(),
)