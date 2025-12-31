package com.example.myapplication

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.net.HttpURLConnection
import java.net.URL

class TrafficWorker(
    context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    override fun doWork(): Result {
        val targetDistrict = inputData.getString("district") ?: return Result.failure()

        return try {
            val url = URL("http://34.10.183.181:3000/proxy")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"

            connection.inputStream.bufferedReader().use { reader ->
                val response = reader.readText()
                parseTrafficDataAndNotify(response, targetDistrict)
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }


    private fun parseTrafficDataAndNotify(xmlData: String, targetDistrict: String) {
        try {
            val factory = XmlPullParserFactory.newInstance()
            val parser = factory.newPullParser()
            parser.setInput(xmlData.reader())

            var eventType = parser.eventType

            var incidentType: String? = null
            var incidentTitle: String? = null
            var startDate: String? = null
            var incidentLocation: String? = null

            while (eventType != XmlPullParser.END_DOCUMENT) {
                val tagName = parser.name
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        try {
                            when (tagName) {
                                "incidenteTypeCd" -> incidentType = parser.nextText()
                                "incidentTitle" -> incidentTitle = parser.nextText()
                                "startDate" -> startDate = parser.nextText()
                                "location" -> incidentLocation = parser.nextText()
                            }
                        } catch (e: Exception) {
                            // 특정 태그 파싱 실패 시 무시
                            e.printStackTrace()
                        }
                    }

                    XmlPullParser.END_TAG -> {
                        if (tagName == "record") {
                            if (
                                incidentType == "1" &&
                                !incidentTitle.isNullOrBlank() &&
                                !incidentLocation.isNullOrBlank() &&
                                (incidentLocation.startsWith(targetDistrict) || incidentLocation.contains(targetDistrict))
                            ) {
                                sendNotification("교통사고 발생", "$incidentTitle ($startDate)")
                            }

                            incidentType = null
                            incidentTitle = null
                            startDate = null
                            incidentLocation = null
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun sendNotification(title: String, message: String) {
        val channelId = "traffic_alert_channel"
        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Oreo 이상은 채널 필요
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "교통 정보 알림",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.drawable.ic_alert) // 알림 아이콘 리소스 설정
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
    }
}
