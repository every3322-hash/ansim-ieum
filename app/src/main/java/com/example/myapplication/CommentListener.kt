package com.example.myapplication

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.firestore.*

object CommentListener {
    private var registration: ListenerRegistration? = null

    fun startListeningToComments(context: Context, postId: String) {
        if (postId.isNullOrBlank()) {
            Log.e("CommentListener", "postId가 null이거나 빈 값입니다. 댓글 리스너 등록을 중단합니다.")
            return
        }

        val firestore = FirebaseFirestore.getInstance()
        registration = firestore.collection("posts")
            .document(postId)
            .collection("comments")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.e("CommentListener", "댓글 리스너 오류: ${error.message}")
                    return@addSnapshotListener
                }

                if (!snapshots!!.isEmpty) {
                    val latestComment = snapshots.documents.firstOrNull()
                    latestComment?.getString("content")?.let { content ->
                        sendNotification(context, "새 댓글", content)
                    }
                }
            }

        Log.d("CommentListener", "댓글 리스너 등록됨")
    }

    fun stopListeningToComments() {
        registration?.remove()
        registration = null
        Log.d("CommentListener", "댓글 리스너 해제됨")
    }

    private fun sendNotification(context: Context, title: String, content: String) {
        val channelId = "comment_channel"
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "댓글 알림 채널",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            manager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_title)
            .setContentTitle(title)
            .setContentText(content)
            .setAutoCancel(true)

        manager.notify(1, builder.build())
    }
}

