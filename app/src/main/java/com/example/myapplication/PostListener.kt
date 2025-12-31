package com.example.myapplication

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

object PostListener {
    private var registration: ListenerRegistration? = null
    private var lastPostId: String? = null

    fun startListening(context: Context) {
        val db = FirebaseFirestore.getInstance()

        registration = db.collection("posts")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(1)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.e("PostListener", "Firestore 오류", error)
                    return@addSnapshotListener
                }

                val doc = snapshots?.documents?.firstOrNull() ?: return@addSnapshotListener
                val newPostId = doc.id
                val title = doc.getString("title") ?: "새 게시글이 있습니다"

                if (lastPostId != null && lastPostId != newPostId) {
                    sendNotification(context, title)
                }

                lastPostId = newPostId
            }
    }

    fun stopListening() {
        registration?.remove()
        registration = null
    }

    private fun sendNotification(context: Context, title: String) {
        val channelId = "post_channel"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // 게시글 알림 채널
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "게시글 알림",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_title)
            .setContentTitle("새 게시글 도착!")
            .setContentText(title)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        Log.d("PostListener", "즉시 알림 전송: $title")
        notificationManager.notify(1001, notification)
    }
}
