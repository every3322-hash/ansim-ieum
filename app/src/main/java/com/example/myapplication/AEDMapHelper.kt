package com.example.myapplication

import android.content.Context
import android.location.Location
import android.util.Log
import com.android.volley.NetworkResponse
import com.android.volley.ParseError
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.HttpHeaderParser
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.NaverMap
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.UnsupportedEncodingException


class AEDMapHelper(private val context: Context, // Context 추가
                   private val naverMap: NaverMap,
                   private val currentLocation: LatLng,
                   private val addAEDMarkerCallback: (LatLng, String) -> Unit){

    private val serviceKey =
        "G9sDqmxYftvdX%2FQ%2BAcpSCcnTXGiRhlkSTyw6Wk4BMNDf0XM68KrDSGzRdGtz4YFyN46Chm8USwd29y5QNEfgdA%3D%3D"

    // 위도와 경도를 매개변수로 받아서 사용하도록 수정
    fun fetchAEDLocations(latitude: Double, longitude: Double) {
        val url =
            "https://apis.data.go.kr/B552657/AEDInfoInqireService/getAedLcinfoInqire?serviceKey=$serviceKey&type=xml&WGS84_LON=$longitude&WGS84_LAT=$latitude&pageNo=1&numOfRows=50"

        val request = object : StringRequest(Request.Method.GET, url, { response ->
            parseAEDData(response)
        }, { error ->
            Log.e("AEDMapHelper", "Error fetching AED data: ${error.message}")
        }) {
            // UTF-8 인코딩 설정을 추가하는 부분
            override fun parseNetworkResponse(response: NetworkResponse?): Response<String> {
                return try {
                    val utf8String = String(response!!.data, Charsets.UTF_8)
                    Response.success(utf8String, HttpHeaderParser.parseCacheHeaders(response))
                } catch (e: UnsupportedEncodingException) {
                    Response.error(ParseError(e))
                }
            }
        }

        Volley.newRequestQueue(context).add(request)
    }

    private fun parseAEDData(xmlData: String) {
        try {
            val factory = XmlPullParserFactory.newInstance()
            val parser = factory.newPullParser()
            parser.setInput(xmlData.reader())

            var eventType = parser.eventType
            var lat: Double? = null
            var lng: Double? = null
            var address: String? = null
            var place: String? = null

            while (eventType != XmlPullParser.END_DOCUMENT) {
                val tagName = parser.name
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        when (tagName) {
                            "wgs84Lat" -> lat = parser.nextText().toDoubleOrNull()
                            "wgs84Lon" -> lng = parser.nextText().toDoubleOrNull()
                            "buildAddress" -> address = parser.nextText()
                            "buildPlace" -> place = parser.nextText()
                        }
                    }

                    XmlPullParser.END_TAG -> {
                        if (tagName == "item" && lat != null && lng != null) {
                            val location = LatLng(lat, lng)
                            val distance = calculateDistance(currentLocation, location)

                            // AED 데이터 로그 출력
                            Log.d("AEDMapHelper", "AED 위치: $location, 거리: ${distance}m, 장소: $place")

                            if (distance <= 10000) { // 반경 1km 이내
                                addAEDMarkerCallback(location, address ?: "알 수 없는 주소")
                            }
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun calculateDistance(start: LatLng, end: LatLng): Float {
        val startLocation = Location("").apply {
            latitude = start.latitude
            longitude = start.longitude
        }
        val endLocation = Location("").apply {
            latitude = end.latitude
            longitude = end.longitude
        }
        return startLocation.distanceTo(endLocation)
    }
}