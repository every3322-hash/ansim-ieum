package com.example.myapplication

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.jsoup.Jsoup
import org.jsoup.parser.Parser
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class TyphoonActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: TyphoonAdapter
    private val typhoonList = mutableListOf<TyphoonModel>()
    private lateinit var emptyTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_typhoon)

        recyclerView = findViewById(R.id.recyclerView)
        emptyTextView = findViewById(R.id.emptyTextView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = TyphoonAdapter(typhoonList)
        recyclerView.adapter = adapter

        findViewById<View>(R.id.btnBack).setOnClickListener {
            finish()
        }

        fetchTyphoonData()
    }

    private fun fetchTyphoonData() {
        val today = LocalDate.now()
        val threeDaysAgo = today.minusDays(3)
        val formatter = DateTimeFormatter.ofPattern("yyyyMMdd")

        val url = "https://apis.data.go.kr/1360000/TyphoonInfoService/getTyphoonInfo" +
                "?serviceKey=G9sDqmxYftvdX%2FQ%2BAcpSCcnTXGiRhlkSTyw6Wk4BMNDf0XM68KrDSGzRdGtz4YFyN46Chm8USwd29y5QNEfgdA%3D%3D" +
                "&pageNo=1&numOfRows=10&dataType=XML" +
                "&fromTmFc=${threeDaysAgo.format(formatter)}&toTmFc=${today.format(formatter)}"

        val request = StringRequest(Request.Method.GET, url, { response ->
            parseXmlResponse(response)
        }, {
            Toast.makeText(this, "태풍 데이터를 불러오는데 실패했습니다.", Toast.LENGTH_SHORT).show()
        })

        Volley.newRequestQueue(this).add(request)
    }

    private fun parseXmlResponse(response: String) {
        val doc = Jsoup.parse(response, "", Parser.xmlParser())
        val items = doc.select("item")

        typhoonList.clear()
        for (item in items) {
            val name = item.select("typName").text()
            val time = item.select("typTm").text()
            val loc = item.select("typLoc").text()
            val pressure = item.select("typPs").text()
            val windSpeed = item.select("typWs").text()

            val formattedTime = formatTime(time)

            typhoonList.add(TyphoonModel(name, formattedTime, loc, pressure, windSpeed))
        }

        if (typhoonList.isEmpty()) {
            emptyTextView.visibility = View.VISIBLE
        } else {
            emptyTextView.visibility = View.GONE
        }

        adapter.notifyDataSetChanged()
    }

    private fun formatTime(input: String): String {
        return try {
            val formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmm")
            val dateTime = LocalDate.parse(input.substring(0, 8), DateTimeFormatter.BASIC_ISO_DATE)
            val hour = input.substring(8, 10)
            val minute = input.substring(10, 12)
            "${dateTime.year}년 ${dateTime.monthValue}월 ${dateTime.dayOfMonth}일 ${hour}시 ${minute}분"
        } catch (e: Exception) {
            input
        }
    }
}