package com.example.myapplication

import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.jsoup.Jsoup
import org.jsoup.parser.Parser
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class EarthquakeActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: EarthquakeAdapter
    private val earthquakeList = mutableListOf<EarthquakeModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_earthquake)

        val backButton = findViewById<ImageView>(R.id.btnBack)
        backButton.setOnClickListener {
            finish()
        }

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = EarthquakeAdapter(earthquakeList)
        recyclerView.adapter = adapter

        fetchEarthquakeData()
    }

    private fun fetchEarthquakeData() {
        val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        val calendar = Calendar.getInstance()

        val toTmFc = dateFormat.format(calendar.time) // 오늘 날짜
        calendar.add(Calendar.DATE, -3) // 3일 전 날짜 계산
        val fromTmFc = dateFormat.format(calendar.time)

        val url = "https://apis.data.go.kr/1360000/EqkInfoService/getEqkMsg" +
                "?serviceKey=G9sDqmxYftvdX%2FQ%2BAcpSCcnTXGiRhlkSTyw6Wk4BMNDf0XM68KrDSGzRdGtz4YFyN46Chm8USwd29y5QNEfgdA%3D%3D" +
                "&pageNo=1&numOfRows=10&dataType=XML&fromTmFc=$fromTmFc&toTmFc=$toTmFc"

        android.util.Log.d("EarthquakeActivity", "Request URL: $url")

        val request = StringRequest(Request.Method.GET, url, { response ->

            android.util.Log.d("EarthquakeActivity", "Response: $response")

            parseXmlResponse(response)
        }, { error ->
            Toast.makeText(this, "데이터를 불러오는데 실패했습니다.", Toast.LENGTH_SHORT).show()
        })

        Volley.newRequestQueue(this).add(request)
    }

    private fun parseXmlResponse(response: String) {
        val doc = Jsoup.parse(response, "", Parser.xmlParser())
        val items = doc.select("item")

        earthquakeList.clear()
        for (item in items) {
            val location = item.select("loc").text()
            val magnitude = item.select("mt").text()
            val depth = item.select("dep").text()
            val timeRaw = item.select("tmEqk").text()
            val timeFormatted = formatDate(timeRaw)
            val imageUrl = item.select("img").text()

            earthquakeList.add(EarthquakeModel(location, magnitude, depth, timeFormatted, imageUrl))
        }
        adapter.notifyDataSetChanged()
    }

    private fun formatDate(dateStr: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault())
            val outputFormat = SimpleDateFormat("yyyy년 MM월 dd일 HH시 mm분 ss초", Locale.getDefault())
            val date = inputFormat.parse(dateStr)
            outputFormat.format(date!!)
        } catch (e: Exception) {
            android.util.Log.e("EarthquakeActivity", "Date format error: ${e.message}")
            dateStr  // 변환 실패 시 원본 값 그대로 반환
        }
    }
}
