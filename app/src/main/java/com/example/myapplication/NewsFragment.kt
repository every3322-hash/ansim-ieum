package com.example.myapplication

import NewsAdapter
import UTF8StringRequest
import android.app.DatePickerDialog
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import java.net.URLDecoder
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class NewsFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: NewsAdapter
    private val newsList = mutableListOf<NewsItem>()
    private lateinit var queue: RequestQueue
    private val allNews = mutableListOf<NewsItem>()

    private var currentPage = 1 // 현재 페이지
    private val displayCount = 10 // 한 페이지에 표시할 데이터 수
    private var startDate: String = "" // 시작일
    private var endDate: String = "" // 종료일

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_news, container, false)

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val calendar = Calendar.getInstance()
        endDate = dateFormat.format(calendar.time) // 오늘 날짜
        calendar.add(Calendar.DAY_OF_MONTH, -30) // 30일 전 날짜
        startDate = dateFormat.format(calendar.time)

        val etKeyword = view.findViewById<EditText>(R.id.etKeyword)
        val btnSearch = view.findViewById<Button>(R.id.btnSearch)
        val btnStartDate: Button = view.findViewById(R.id.btnStartDate)
        val btnEndDate: Button = view.findViewById(R.id.btnEndDate)
        val btnNext = view.findViewById<Button>(R.id.btnNext)
        val btnPrev = view.findViewById<Button>(R.id.btnPrev)
        recyclerView = view.findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = NewsAdapter(requireContext(), newsList)
        recyclerView.adapter = adapter

        // Volley RequestQueue 초기화
        queue = Volley.newRequestQueue(requireContext())

        // 초기 날짜 버튼 텍스트 설정
        btnStartDate.text = startDate
        btnEndDate.text = endDate

        // 초기 화면에서 최신 뉴스 표시
        searchNews("")

        btnSearch.setOnClickListener {
            val keyword = etKeyword.text.toString()
            currentPage = 1 // 검색할 때마다 페이지 초기화
            searchNews(keyword) // keyword를 searchNews()에 전달
        }

        btnStartDate.setOnClickListener {
            showDatePicker { date ->
                startDate = date
                btnStartDate.text = startDate
            }
        }

        btnEndDate.setOnClickListener {
            showDatePicker { date ->
                endDate = date
                btnEndDate.text = endDate
            }
        }

        btnPrev.setOnClickListener {
            if (currentPage > 1) {
                currentPage--
                showCurrentPage()
            }
        }

        btnNext.setOnClickListener {
            val totalPages = (allNews.size + displayCount - 1) / displayCount
            if (currentPage < totalPages) {
                currentPage++
                showCurrentPage()
            }
        }

        return view
    }

    private fun showDatePicker(onDateSelected: (String) -> Unit) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePicker =
            DatePickerDialog(requireContext(), { _, selectedYear, selectedMonth, selectedDay ->
                val formattedDate = "$selectedYear-${
                    (selectedMonth + 1).toString().padStart(2, '0')
                }-${selectedDay.toString().padStart(2, '0')}"
                onDateSelected(formattedDate)
            }, year, month, day)

        datePicker.show()
    }

    private fun handleAllRequestsFinished() {
        allNews.sortByDescending { it.date }
        showCurrentPage()
    }

    private fun searchNews(keyword: String = "") {
        newsList.clear()
        allNews.clear()
        adapter.notifyDataSetChanged()

        val serviceKey = "G2762UK6YJO5S02S"
        val returnType = "xml"
        val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())

        val calendar = Calendar.getInstance()
        val today = calendar.time

        val seenIds = mutableSetOf<String>()
        var finishedRequests = 0

        for (i in 0 until 30) {
            val inqCalendar = Calendar.getInstance()
            inqCalendar.time = today
            inqCalendar.add(Calendar.DAY_OF_MONTH, -i)
            val inqDt = dateFormat.format(inqCalendar.time)

            val urlBuilder = StringBuilder(
                "https://www.safetydata.go.kr/V2/api/DSSP-IF-00051" +
                        "?serviceKey=$serviceKey" +
                        "&numOfRows=100" +
                        "&pageNo=1" +
                        "&returnType=$returnType" +
                        "&inqDt=$inqDt"
            )
            if (keyword.isNotEmpty()) {
                urlBuilder.append("&keyword=$keyword")
            }

            val request = UTF8StringRequest(
                Request.Method.GET,
                urlBuilder.toString(),
                { response ->
                    val items = parseNewsItems(response)
                    val uniqueItems = items.filter { seenIds.add(it.newsId) }
                    allNews.addAll(uniqueItems)
                    finishedRequests++
                    if (finishedRequests == 30) {
                        handleAllRequestsFinished()
                    }
                },
                { error ->
                    Log.e("API Error", "날짜 $inqDt 요청 실패: ${error.message}")
                    finishedRequests++
                    if (finishedRequests == 30) {
                        handleAllRequestsFinished()
                    }
                }
            )

            queue.add(request)
        }
    }

    private fun showCurrentPage() {
        val totalPages = (allNews.size + displayCount - 1) / displayCount
        val startIdx = (currentPage - 1) * displayCount
        val endIdx = (startIdx + displayCount).coerceAtMost(allNews.size)

        newsList.clear()
        newsList.addAll(allNews.subList(startIdx, endIdx))
        adapter.notifyDataSetChanged()

        view?.findViewById<TextView>(R.id.tvPagination)?.text = "$currentPage / $totalPages"
    }


    private fun parseNewsItems(response: String): List<NewsItem> {
        val result = mutableListOf<NewsItem>()
        try {
            val factory = XmlPullParserFactory.newInstance()
            val parser = factory.newPullParser()
            parser.setInput(StringReader(response))

            var eventType = parser.eventType
            var currentTag: String? = null
            var newsId = ""
            var title = ""
            var content = ""
            var writer = ""
            var date = ""

            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> currentTag = parser.name
                    XmlPullParser.TEXT -> {
                        val text = parser.text.trim()
                        when (currentTag) {
                            "YNA_NO" -> newsId = text
                            "YNA_TTL" -> title = decode(text)
                            "YNA_CN" -> content = decode(text)
                            "YNA_WRTR_NM" -> writer = decode(text)
                            "YNA_YMD" -> date = text
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (parser.name == "item") {
                            val encodedTitle = Uri.encode(title)
                            val link = "https://search.naver.com/search.naver?query=$encodedTitle"
                            result.add(NewsItem(newsId, title, content, date, writer, link))
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            Log.e("XML Parse", "Error: ${e.message}")
        }
        return result
    }

    companion object {
        fun decode(text: String): String {
            return try {
                URLDecoder.decode(text, "UTF-8")
            } catch (e: Exception) {
                Log.e("Decode Error", "Error decoding text: ${e.message}")
                text // 디코딩 실패 시 원본 반환
            }
        }
    }
}
