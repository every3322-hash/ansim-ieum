package com.example.myapplication

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.HttpHeaderParser
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.w3c.dom.Element
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.xml.parsers.DocumentBuilderFactory

class EmergencyAlertWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {

    override fun doWork(): Result {
        fetchLatestEmergencyAlerts()
        return Result.success()
    }

    private fun fetchLatestEmergencyAlerts() {
        val sharedPreferences = applicationContext.getSharedPreferences("EmergencyPrefs", Context.MODE_PRIVATE)
        val lastMessage = sharedPreferences.getString("last_alert", "")

        val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        val currentDate = dateFormat.format(Date())

        val url = "https://www.safetydata.go.kr/V2/api/DSSP-IF-00247?" +
                "serviceKey=90G0IWH0N9WKL8M9" +
                "&numOfRows=1" + // 최신 1개만 요청
                "&pageNo=1" +
                "&returnType=xml" +
                "&crtDt=$currentDate"

        val request = object : StringRequest(Request.Method.GET, url, { response ->
            val newMessage = parseEmergencyAlert(response)

            if (newMessage.isNotEmpty() && newMessage != lastMessage) {
                sendNotification(newMessage)
                sharedPreferences.edit().putString("last_alert", newMessage).apply()
            }
        }, { error ->
            Log.e("EmergencyAlertWorker", "API 요청 실패: ${error.message}")
        }) {
            override fun parseNetworkResponse(response: com.android.volley.NetworkResponse?): Response<String> {

                val parsed = String(response?.data ?: ByteArray(0), Charsets.UTF_8)
                return Response.success(parsed, HttpHeaderParser.parseCacheHeaders(response))
            }
        }

        Volley.newRequestQueue(applicationContext).add(request)
    }

    private fun parseEmergencyAlert(response: String): String {
        return try {
            val factory = DocumentBuilderFactory.newInstance()
            val builder = factory.newDocumentBuilder()
            val doc = builder.parse(response.byteInputStream())

            val nodeList = doc.getElementsByTagName("item")
            if (nodeList.length > 0) {
                val item = nodeList.item(0) as Element
                val msgCnNode = item.getElementsByTagName("MSG_CN")
                if (msgCnNode.length > 0) {
                    return msgCnNode.item(0).textContent.trim()
                }
            }
            ""
        } catch (e: Exception) {
            Log.e("EmergencyAlertWorker", "XML 파싱 오류: ${e.message}")
            ""
        }
    }

    private fun sendNotification(message: String) {
        val channelId = "emergency_alerts"
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "긴급재난문자", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "긴급 재난 문자 알림"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("긴급재난문자")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}

