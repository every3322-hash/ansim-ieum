package com.example.myapplication

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.android.volley.NetworkResponse
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.HttpHeaderParser
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.myapplication.databinding.FragmentMapBinding
import com.getkeepsafe.taptargetview.TapTarget
import com.getkeepsafe.taptargetview.TapTargetSequence
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.CameraPosition
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.LocationTrackingMode
import com.naver.maps.map.MapView
import com.naver.maps.map.NaverMap
import com.naver.maps.map.overlay.Marker
import com.naver.maps.map.overlay.OverlayImage
import com.naver.maps.map.util.FusedLocationSource
import org.json.JSONObject
import org.jsoup.Jsoup
import org.w3c.dom.Element
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.xml.parsers.DocumentBuilderFactory

class MapFragment : BaseMapFragment<FragmentMapBinding>(R.layout.fragment_map) {

    private lateinit var naverMap: NaverMap
    private lateinit var locationSource: FusedLocationSource // 현재 위치
    private lateinit var aedMapHelper: AEDMapHelper
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var weatherContainer: LinearLayout
    private lateinit var weatherIcon: ImageView
    private lateinit var weatherTemperature: TextView
    private lateinit var weatherDialog: BottomSheetDialog
    private lateinit var weatherData: WeatherModel
    private val selectedFilters = mutableSetOf<Pair<String, String>>()
    private val markers = mutableListOf<Marker>()
    private val clientId = "RjctVkF7hxDduKBBMW79"
    private val clientSecret = "vxNRYDZDTn"
    private val suggestionList = mutableListOf<String>()
    private lateinit var suggestionAdapter: ArrayAdapter<String>
    private val requestQueue by lazy { Volley.newRequestQueue(requireContext()) }
    private lateinit var sharedPreferences: SharedPreferences
    private val deniedPermissions = mutableSetOf<String>()
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<Array<String>>
    private var toast: Toast? = null
    // 모든 필터 버튼을 담을 리스트
    private lateinit var filterButtons: List<View>
    private var lastSelectedButton: View? = null // 마지막으로 선택된 버튼을 추적

    // 각 버튼에 대한 참조 (ViewBinding을 사용하면 더 좋습니다)
    private lateinit var trafficButton: View
    private lateinit var constructionButton: View
    private lateinit var eventButton: View
    private lateinit var controlButton: View
    private lateinit var disasterButton: View
    private lateinit var otherButton: View
    private lateinit var schoolzoneButton: View
    private lateinit var aedButton: View
    private lateinit var firestationButton: View
    private lateinit var policestationButton: View
    private lateinit var hospitalButton: View
    private lateinit var shelterButton: View

    override var mapView: MapView? = null

    override fun initOnCreateView() {
        initMapView()

        // 권한 런처 등록
        requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            handlePermissionsResult(permissions)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        weatherContainer = view.findViewById(R.id.weatherContainer)
        weatherIcon = view.findViewById(R.id.weatherIcon)
        weatherTemperature = view.findViewById(R.id.weatherTemperature)
        sharedPreferences = requireContext().getSharedPreferences("PermissionPrefs", Context.MODE_PRIVATE)

        if (!isGuideShown) {
            view.post {
                showGuideSequence()
            }
            isGuideShown = true
        }

        checkAndRequestPermissions() //권한 확인
        fetchWeatherData()

        // 날씨 클릭 시 팝업 띄우기
        weatherContainer.setOnClickListener {
            showWeatherPopup(weatherData)
        }
        startEmergencyAlertWorker()
    }

    override fun onMapReady(naverMap: NaverMap) {
        this.naverMap = naverMap
        initNaverMap(naverMap)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())

        // 위치 권한 확인
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // 권한이 허용된 경우에만 lastLocation을 사용
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    val currentLatLng = LatLng(location.latitude, location.longitude)

                    // AEDMapHelper 초기화 및 콜백 설정
                    aedMapHelper = AEDMapHelper(
                        context = requireContext(),
                        naverMap = naverMap,
                        currentLocation = currentLatLng,
                        addAEDMarkerCallback = { location, address -> addAEDMarker(location, address) }
                    )

                    // 현재 위치를 기준으로 AED 데이터 요청
                    aedMapHelper.fetchAEDLocations(location.latitude, location.longitude)
                } ?: run {
                    Log.e("MapFragment", "현재 위치를 얻을 수 없습니다.")
                }
            }
        }
    }

    override fun initOnMapReady(naverMap: NaverMap) {
        initNaverMap(naverMap)
        setCameraChangeListener() // 카메라 변화 리스너 추가
        this.naverMap = naverMap
        initializeFilters() // 초기 필터 설정
        applyFiltersToMap() // 선택된 필터에 맞게 초기 데이터 로드
    }

    override fun initViewCreated() {
        super.initViewCreated()
        clickAlarmButton()
        observeClusterMarkerClick()
        clickLocationButton()
        initializeFilters()
        initializeButtons(requireView()) // 버튼 초기화 함수 호출
        setupFilterButtonListeners() // 리스너 설정 함수 호출
        setupSearchView()
        // 내정보 버튼 클릭 시 MyInfoActivity로 이동
        binding.root.findViewById<View>(R.id.etc_menu)?.setOnClickListener {
            val activity = requireActivity()
            if (activity is MapActivity) { // HomeActivity 대신 MapActivity로 변경
                // MapActivity가 실행 중이라면, 그냥 Drawer만 열어줌
                if (!activity.drawerLayout.isDrawerOpen(Gravity.LEFT)) {
                    activity.drawerLayout.openDrawer(Gravity.LEFT)
                }
            }
        }
        // 웹뷰 버튼 클릭 시 PopulationActivity로 이동
        binding.root.findViewById<View>(R.id.btnWebView).setOnClickListener {
            val intent = Intent(requireContext(), PopulationActivity::class.java)
            startActivity(intent)
        }
    }

    override fun initOnResume() {
        // 필요한 경우 구현
    }

    // 위치,알림,전화 권한
    private fun checkAndRequestPermissions() {
        val permissionsToRequest = getDeniedPermissions()
        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            // SharedPreferences를 통해 메시지를 한 번만 띄우도록 설정
            val prefs = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            val hasShownToast = prefs.getBoolean("has_shown_permission_toast", false)

            if (!hasShownToast) {
                Toast.makeText(requireContext(), "모든 권한이 허용되었습니다.", Toast.LENGTH_SHORT).show()
                prefs.edit().putBoolean("has_shown_permission_toast", true).apply()
            }
            toast?.cancel()
        }
    }

    private fun getDeniedPermissions(): List<String> {
        val permissions = mutableListOf<String>()

        if (!isPermissionGranted(Manifest.permission.ACCESS_FINE_LOCATION) &&
            !deniedPermissions.contains(Manifest.permission.ACCESS_FINE_LOCATION)) {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !isPermissionGranted(Manifest.permission.POST_NOTIFICATIONS) &&
            !deniedPermissions.contains(Manifest.permission.POST_NOTIFICATIONS)) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        if (!isPermissionGranted(Manifest.permission.READ_PHONE_STATE) &&
            !deniedPermissions.contains(Manifest.permission.READ_PHONE_STATE)) {
            permissions.add(Manifest.permission.READ_PHONE_STATE)
        }
        return permissions
    }

    private fun isPermissionGranted(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED
    }

    private fun handlePermissionsResult(permissions: Map<String, Boolean>) {
        val messageList = mutableListOf<String>()

        permissions.forEach { (permission, isGranted) ->
            if (isGranted) {
                savePermissionState(permission, true)
            } else {
                deniedPermissions.add(permission)
            }

            val permissionName = when (permission) {
                Manifest.permission.ACCESS_FINE_LOCATION -> "위치"
                Manifest.permission.POST_NOTIFICATIONS -> "알림"
                Manifest.permission.READ_PHONE_STATE -> "전화 상태"
                else -> "알 수 없음"
            }

            val status = if (isGranted) "허용됨" else "거부됨"
            messageList.add("$permissionName 권한: $status")
        }

        val message = messageList.joinToString("\n")
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }

    private fun showGuideSequence() {
        val overlay = requireView().findViewById<View>(R.id.guide_overlay)
        overlay.visibility = View.VISIBLE
        val etcMenu = requireView().findViewById<View>(R.id.etc_menu)
        val searchView = requireView().findViewById<View>(R.id.search_view)
        val alarmMenu = requireView().findViewById<View>(R.id.alarm_menu)
        val btnWebView = requireView().findViewById<View>(R.id.btnWebView)
        val filterButtons = requireView().findViewById<View>(R.id.filter_buttons_scroll)
        val floatBtn1 = requireView().findViewById<View>(R.id.floatBtn1)
        val weatherContainer = requireView().findViewById<View>(R.id.weatherContainer)
        val bottomNav = requireActivity().findViewById<BottomNavigationView>(R.id.bottom_navigation)

        if (etcMenu == null || searchView == null || bottomNav == null) {
            Log.e("Guide", "View not found")
            return
        }

        TapTargetSequence(requireActivity())
            .targets(
                TapTarget.forView(etcMenu, "계정설정", "계정설정, 앱설정, 재난조회, 즐겨찾기 등을 볼 수 있음")
                    .titleTextColor(R.color.black)
                    .descriptionTextColor(R.color.black)
                    .targetCircleColor(R.color.guide_target_circle_transparent)
                    .cancelable(false),
                TapTarget.forView(searchView, "장소 검색", "장소 검색시 위치 이동")
                    .titleTextColor(R.color.black)
                    .descriptionTextColor(R.color.black)
                    .targetCircleColor(R.color.guide_target_circle_transparent)
                    .cancelable(false),
                TapTarget.forView(alarmMenu, "긴급재난문자", "긴급재난문자 리스트를 볼 수 있음")
                    .titleTextColor(R.color.black)
                    .descriptionTextColor(R.color.black)
                    .targetCircleColor(R.color.guide_target_circle_transparent)
                    .cancelable(false),
                TapTarget.forView(btnWebView, "인구밀집도", "인구밀집도를 볼 수 있음")
                    .titleTextColor(R.color.black)
                    .descriptionTextColor(R.color.black)
                    .targetCircleColor(R.color.guide_target_circle_transparent)
                    .cancelable(false),
                TapTarget.forView(filterButtons, "마커 필터", "각 필터 버튼 클릭 시 지도에 마커 생성")
                    .titleTextColor(R.color.black)
                    .descriptionTextColor(R.color.black)
                    .targetCircleColor(R.color.guide_target_circle_transparent)
                    .cancelable(false),
                TapTarget.forView(floatBtn1, "내 위치", "클릭 시 내 위치로 이동")
                    .titleTextColor(R.color.black)
                    .descriptionTextColor(R.color.black)
                    .targetCircleColor(R.color.guide_target_circle_transparent)
                    .cancelable(false),
                TapTarget.forView(weatherContainer, "날씨 정보", "내 위치 기반 날씨")
                    .titleTextColor(R.color.black)
                    .descriptionTextColor(R.color.black)
                    .targetCircleColor(R.color.guide_target_circle_transparent)
                    .cancelable(false),
                TapTarget.forView(bottomNav, "하단 메뉴", "홈, 신고, 행동요령 등 기능 사용 가능")
                    .titleTextColor(R.color.black)
                    .descriptionTextColor(R.color.black)
                    .targetCircleColor(R.color.guide_target_circle_transparent)
                    .cancelable(false)
            )
            .listener(object : TapTargetSequence.Listener {
                override fun onSequenceFinish() {
                    overlay.visibility = View.GONE
                    Toast.makeText(requireContext(), "가이드가 종료되었습니다!", Toast.LENGTH_SHORT).show()
                }

                override fun onSequenceCanceled(tapTarget: TapTarget) {
                    Toast.makeText(requireContext(), "가이드가 취소되었습니다.", Toast.LENGTH_SHORT).show()
                }

                override fun onSequenceStep(tapTarget: TapTarget, targetClicked: Boolean) {}
            })
            .start()
    }

    private fun savePermissionState(permission: String, isGranted: Boolean) {
        val key = when (permission) {
            Manifest.permission.ACCESS_FINE_LOCATION -> "location_permission"
            Manifest.permission.POST_NOTIFICATIONS -> "notification_permission"
            Manifest.permission.READ_PHONE_STATE -> "phone_state_permission"
            else -> permission
        }
        sharedPreferences.edit().putBoolean(key, isGranted).apply()
    }

    private fun showWeatherPopup(weatherData: WeatherModel) {
        weatherDialog = BottomSheetDialog(requireContext())
        val popupView = layoutInflater.inflate(R.layout.weather_popup, null)

        // 팝업 내부 TextView 연결
        val tempDetail = popupView.findViewById<TextView>(R.id.weatherTemperatureDetail)
        val precipitation = popupView.findViewById<TextView>(R.id.weatherPrecipitation)
        val humidity = popupView.findViewById<TextView>(R.id.weatherHumidity)
        val windSpeed = popupView.findViewById<TextView>(R.id.weatherWindSpeed)

        // API 데이터 넣기
        tempDetail.text = "기온: ${weatherData.temperature}°C"
        precipitation.text = "날씨: ${weatherData.weatherType}"
        humidity.text = "습도: ${weatherData.humidity}%"
        windSpeed.text = "풍속: ${weatherData.windSpeed}m/s"

        // 팝업 띄우기
        weatherDialog.setContentView(popupView)
        weatherDialog.show()
    }

    private fun updateWeatherUI(weatherData: WeatherModel) {
        weatherTemperature.text = "${weatherData.temperature}°C"

        // 아이콘 변경
        val weatherIconRes = when {
            weatherData.precipitation == "비" -> R.drawable.rain
            weatherData.precipitation == "비/눈" -> R.drawable.rainorsnow
            weatherData.precipitation == "눈" -> R.drawable.snow
            weatherData.precipitation == "소나기" -> R.drawable.shower
            weatherData.weatherType == "맑음" -> R.drawable.sunny
            weatherData.weatherType == "구름많음" -> R.drawable.cloudy
            weatherData.weatherType == "흐림" -> R.drawable.cloudy_night
            else -> R.drawable.unknown
        }
        weatherIcon.setImageResource(weatherIconRes)
        // 날씨 컨테이너 클릭 시 팝업 띄우기
        weatherContainer.setOnClickListener {
            showWeatherPopup(weatherData)
        }
    }

    private fun fetchWeatherData() {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())

        // 위치 권한 확인 및 요청
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                val (nx, ny) = if (location != null) {
                    convertGpsToGrid(location.latitude, location.longitude) // GPS 위치 기반 좌표 변환
                } else {
                    Pair(60, 127) // GPS 실패 시 기본 좌표 (양주시)
                }

                // 날짜 (yyyyMMdd 형식)
                val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
                val currentDate = dateFormat.format(Date())

                // 현재 시간 기준으로 base_time 설정 (3시간 간격)
                val currentTime = Calendar.getInstance()
                val hour = currentTime.get(Calendar.HOUR_OF_DAY)
                val baseTime = when {
                    hour < 2 -> "2300"
                    hour < 5 -> "0200"
                    hour < 8 -> "0500"
                    hour < 11 -> "0800"
                    hour < 14 -> "1100"
                    hour < 17 -> "1400"
                    hour < 20 -> "1700"
                    hour < 23 -> "2000"
                    else -> "2300"
                }

                // 기상청 API 요청
                val url = "https://apis.data.go.kr/1360000/VilageFcstInfoService_2.0/getVilageFcst"
                val params = mapOf(
                    "serviceKey" to "G9sDqmxYftvdX%2FQ%2BAcpSCcnTXGiRhlkSTyw6Wk4BMNDf0XM68KrDSGzRdGtz4YFyN46Chm8USwd29y5QNEfgdA%3D%3D",
                    "numOfRows" to "100",
                    "pageNo" to "1",
                    "dataType" to "XML",
                    "base_date" to currentDate,
                    "base_time" to baseTime,
                    "nx" to nx.toString(),
                    "ny" to ny.toString()
                )

                val requestUrl = "$url?${params.entries.joinToString("&") { "${it.key}=${it.value}" }}"

                val stringRequest = StringRequest(
                    Request.Method.GET, requestUrl,
                    { response ->
                        try {
                            // XML 응답 파싱
                            val weatherData = parseWeatherResponse(response)
                            Log.d("WeatherAPI", "Request URL: $requestUrl")

                            if (weatherData != null) {
                                updateWeatherUI(weatherData)
                            } else {
                                Log.e("WeatherAPI", "날씨 데이터 파싱 실패")
                            }
                        } catch (e: Exception) {
                            Log.e("WeatherParse", "Parsing error: ${e.message}")
                        }
                    },
                    { error ->
                        Log.e("WeatherAPI", "Error: ${error.message}")
                    })

                Volley.newRequestQueue(requireContext()).add(stringRequest)
            }
        } else {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1001
            )
        }
    }

    private fun parseWeatherResponse(response: String): WeatherModel {
        try {
            val doc = Jsoup.parse(response)
            val items = doc.select("item")

            var temperature = "정보 없음"
            var weatherType = "알 수 없음"
            var precipitation = "정보 없음"
            var humidity = "정보 없음"
            var windSpeed = "정보 없음"

            for (item in items) {
                val category = item.select("category").text()
                val value = item.select("fcstValue").text()

                when (category) {
                    "T1H" -> temperature = value
                    "TMP" -> if (temperature == "정보 없음") temperature = value
                    "SKY" -> weatherType = when (value) {
                        "1" -> "맑음"
                        "3" -> "구름많음"
                        "4" -> "흐림"
                        else -> "알 수 없음"
                    }
                    "PTY" -> precipitation = when (value) {
                        "0" -> "없음"
                        "1" -> "비"
                        "2" -> "비/눈"
                        "3" -> "눈"
                        "4" -> "소나기"
                        else -> "정보 없음"
                    }
                    "REH" -> humidity = value
                    "WSD" -> windSpeed = value
                }
            }

            return WeatherModel(
                temperature = temperature,
                weatherType = weatherType,
                precipitation = precipitation,
                humidity = humidity,
                windSpeed = windSpeed
            )

        } catch (e: Exception) {
            Log.e("WeatherParse", "Parsing error: ${e.message}")
            return WeatherModel("N/A", "N/A", "N/A", "N/A", "N/A")
        }
    }


    // GPS -> 기상청 격자 좌표 변환 함수
    private fun convertGpsToGrid(lat: Double, lon: Double): Pair<Int, Int> {
        val RE = 6371.00877 // 지구 반경(km)
        val GRID = 5.0 // 격자 간격(km)
        val SLAT1 = 30.0 // 투영 위도1(degree)
        val SLAT2 = 60.0 // 투영 위도2(degree)
        val OLON = 126.0 // 기준점 경도(degree)
        val OLAT = 38.0 // 기준점 위도(degree)
        val XO = 43 // 기준점 X좌표(GRID)
        val YO = 136 // 기준점 Y좌표(GRID)

        val DEGRAD = Math.PI / 180.0
        val re = RE / GRID
        val slat1 = SLAT1 * DEGRAD
        val slat2 = SLAT2 * DEGRAD
        val olon = OLON * DEGRAD
        val olat = OLAT * DEGRAD

        val sn = Math.tan(Math.PI * 0.25 + slat2 * 0.5) / Math.tan(Math.PI * 0.25 + slat1 * 0.5)
        val sf = Math.pow(Math.tan(Math.PI * 0.25 + slat1 * 0.5), sn) / Math.pow(
            Math.tan(Math.PI * 0.25 + olat * 0.5),
            sn
        )
        val ro = re * sf / Math.pow(Math.tan(Math.PI * 0.25 + olat * 0.5), sn)
        val ra = re * sf / Math.pow(Math.tan(Math.PI * 0.25 + lat * DEGRAD * 0.5), sn)
        val theta = lon * DEGRAD - olon
        val x = (ra * Math.sin(theta * sn) + XO + 0.5).toInt()
        val y = (ro - ra * Math.cos(theta * sn) + YO + 0.5).toInt()

        return Pair(x, y)
    }


    private fun clickAlarmButton() {
        binding.alarmMenu.setOnClickListener {
            fetchLatestEmergencyAlerts(false)  // 최신 재난 문자 바로 가져오기
        }
    }

    private fun startEmergencyAlertWorker() {
        val workRequest = PeriodicWorkRequestBuilder<EmergencyAlertWorker>(15, TimeUnit.MINUTES)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        WorkManager.getInstance(requireContext()).enqueueUniquePeriodicWork(
            "EmergencyAlertWork",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }


    // 앱 실행 시 WorkManager 시작
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startEmergencyAlertWorker()
    }


    private fun showEmergencyAlertsDialog(messages: List<String>) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("긴급재난문자 목록")

        val messageText = messages.joinToString("\n\n") { "• $it" }
        builder.setMessage(messageText)

        builder.setPositiveButton("확인") { dialog, _ ->
            dialog.dismiss()
        }

        val dialog = builder.create()
        dialog.show()
    }

    private fun showEmergencyAlertsBottomSheet(messages: List<String>) {
        val dialog = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.dialog_emergency_alerts, null)
        val listView: ListView = view.findViewById(R.id.listViewAlerts)

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, messages)
        listView.adapter = adapter

        dialog.setContentView(view)
        dialog.show()
    }

    private fun fetchLatestEmergencyAlerts(showNotification: Boolean) {
        val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        val currentDate = dateFormat.format(Date())

        val url = "https://www.safetydata.go.kr/V2/api/DSSP-IF-00247?" +
                "serviceKey=90G0IWH0N9WKL8M9" +
                "&numOfRows=10" +  // 최신 데이터 10개만 요청
                "&pageNo=1" +  // 최신 데이터는 1페이지에 있음
                "&returnType=xml" +
                "&crtDt=$currentDate"

        Log.d("MapFragment", "최종 API 요청 URL: $url")

        val request = object : StringRequest(Method.GET, url, { response ->
            Log.d("MapFragment", "API 응답 전체:\n$response")
            parseEmergencyAlert(response, showNotification)
        }, { error ->
            Log.e("MapFragment", "API 요청 실패: ${error.message}")
        }) {
            override fun parseNetworkResponse(response: NetworkResponse?): Response<String> {
                val parsed = String(response?.data ?: ByteArray(0), Charsets.UTF_8)
                return Response.success(parsed, HttpHeaderParser.parseCacheHeaders(response))
            }
        }
        requestQueue.add(request)
    }

    private fun parseEmergencyAlert(response: String, showNotification: Boolean) {
        try {
            val factory = DocumentBuilderFactory.newInstance()
            val builder = factory.newDocumentBuilder()
            val inputStream = response.byteInputStream()
            val doc = builder.parse(inputStream)

            val nodeList = doc.getElementsByTagName("item")
            val messages = mutableListOf<String>()

            for (i in 0 until nodeList.length) {
                val item = nodeList.item(i) as Element
                val msgCnNode = item.getElementsByTagName("MSG_CN")
                if (msgCnNode.length > 0) {
                    val message = msgCnNode.item(0).textContent.trim()
                    messages.add(message)
                }
            }

            if (messages.isNotEmpty()) {
                showEmergencyAlertsDialog(messages)  // 목록은 항상 보여줌
                if (showNotification) {
                    showNotification(messages.first())  // 최신 문자 알림은 조건부
                }
            } else {
                Toast.makeText(requireContext(), "긴급재난문자가 없습니다.", Toast.LENGTH_SHORT).show()
            }

        } catch (e: Exception) {
            Log.e("MapFragment", "XML 파싱 오류: ${e.message}")
        }
    }

    private fun showNotification(message: String) {
        val channelId = "emergency_alerts"
        val notificationManager = requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "긴급재난문자", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "긴급 재난 문자 알림"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(requireContext(), channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("긴급재난문자")
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        Log.d("MapFragment", "최신 재난 문자 내용: $message")
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    // 장소 검색 함수
    private fun searchLocation(query: String) {
        val url = "https://openapi.naver.com/v1/search/local.json?query=$query&display=1&start=1&sort=random"

        val request = object : JsonObjectRequest(Method.GET, url, null, { response ->
            parseSearchResult(response)
        }, { error ->
            Toast.makeText(requireContext(), "검색 실패: ${error.message}", Toast.LENGTH_SHORT).show()
            Log.e("MapFragment", "Error fetching location data: ${error.message}")
        }) {
            override fun getHeaders(): Map<String, String> {
                val headers = HashMap<String, String>()
                headers["X-Naver-Client-Id"] = clientId
                headers["X-Naver-Client-Secret"] = clientSecret
                return headers
            }
        }

        Volley.newRequestQueue(requireContext()).add(request)
    }

    // 검색 결과 파싱 및 지도 이동
    private fun parseSearchResult(response: JSONObject) {
        val items = response.getJSONArray("items")
        if (items.length() > 0) {
            val firstItem = items.getJSONObject(0)
            val title = firstItem.getString("title").replace("<b>", "").replace("</b>", "")

            val longitude = firstItem.getString("mapx").toDouble() / 1e7
            val latitude = firstItem.getString("mapy").toDouble() / 1e7

            val location = LatLng(latitude, longitude)
            moveCameraToLocation(location)
            Toast.makeText(requireContext(), "$title 위치로 이동합니다.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), "검색 결과가 없습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    // 검색 뷰 설정 (자동완성 기능 포함)
    private fun setupSearchView() {
        // 자동완성 리스트 어댑터 설정
        suggestionAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, suggestionList)
        binding.searchSuggestionsList.adapter = suggestionAdapter

        binding.searchSuggestionsList.bringToFront()

        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let { searchLocation(it) }
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (!newText.isNullOrEmpty()) {
                    fetchSearchSuggestions(newText) // 자동완성 검색어 가져오기
                } else {
                    binding.searchSuggestionsList.visibility = View.GONE
                }
                return false
            }
        })

        // 자동완성 리스트 클릭 이벤트 처리
        binding.searchSuggestionsList.setOnItemClickListener { _, _, position, _ ->
            val selectedQuery = suggestionList[position]
            binding.searchView.setQuery(selectedQuery, true)
            binding.searchSuggestionsList.visibility = View.GONE
        }
    }

    // 네이버 API를 이용한 검색어 추천 기능
    private fun fetchSearchSuggestions(query: String) {
        val url = "https://openapi.naver.com/v1/search/local.json?query=$query&display=5&start=1&sort=random"

        val request = object : JsonObjectRequest(Method.GET, url, null, { response ->
            val items = response.getJSONArray("items")
            suggestionList.clear()

            for (i in 0 until items.length()) {
                val title = items.getJSONObject(i).getString("title")
                    .replace("<b>", "").replace("</b>", "")
                suggestionList.add(title)
            }

            if (suggestionList.isNotEmpty()) {
                suggestionAdapter.notifyDataSetChanged()
                binding.searchSuggestionsList.visibility = View.VISIBLE
            } else {
                binding.searchSuggestionsList.visibility = View.GONE
            }
        }, { error ->
            Log.e("MapFragment", "Error fetching search suggestions: ${error.message}")
        }) {
            override fun getHeaders(): Map<String, String> {
                return mapOf(
                    "X-Naver-Client-Id" to clientId,
                    "X-Naver-Client-Secret" to clientSecret
                )
            }
        }

        Volley.newRequestQueue(requireContext()).add(request)
    }

    // 지도의 시점을 이동하는 함수
    private fun moveCameraToLocation(location: LatLng) {
        val cameraUpdate = CameraUpdate.scrollTo(location)
        naverMap.moveCamera(cameraUpdate)
    }

    private fun initMapView() { // mapView 초기화
        mapView = binding.mapView
        mapView?.getMapAsync(this)
        locationSource = FusedLocationSource(this, LOCATION_PERMISSION_REQUEST_CODE)
    }

    private fun initNaverMap(naverMap: NaverMap) { // 위치 및 naverMap 세팅
        this.naverMap = naverMap
        naverMap.locationSource = locationSource

        val locationOverlay = naverMap.locationOverlay
        locationOverlay.isVisible = true

        naverMap.locationTrackingMode = LocationTrackingMode.Follow
    }

    private fun getLastLocation(map: NaverMap) {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())

        checkLocationPermission(requireActivity() as AppCompatActivity)

        requestMapPermission {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val loc = LatLng(location.latitude, location.longitude)
                    Log.d("MapFragment", "Current location: $loc") // 현재 위치 로그 추가
                    map.cameraPosition = CameraPosition(loc, DEFAULT_ZOOM)
                    map.locationTrackingMode = LocationTrackingMode.Follow
                } else {
                    Log.d("MapFragment", "Location is null") // 위치가 null일 때 로그 추가
                }
            }
        }
    }

    private fun requestCurrentLocation() {
        if (!::aedMapHelper.isInitialized) return // AEDMapHelper가 초기화되지 않은 경우 반환

        // 위치 권한 확인
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // 권한이 허용된 경우에만 lastLocation 사용
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                location?.let {
                    val currentLat = it.latitude
                    val currentLng = it.longitude
                    aedMapHelper.fetchAEDLocations(currentLat, currentLng) // 현재 위치 기반으로 AED 데이터 가져오기
                }
            }
        }
    }


    private fun clickLocationButton() {
        binding.floatBtn1.setOnClickListener {
            getLastLocation(naverMap) // 현재 위치로 카메라 이동
            naverMap.locationTrackingMode = LocationTrackingMode.Follow // 카메라가 사용자를 따라 이동

            // 현재 위치 기준으로 AED 데이터 요청
            if (selectedFilters.contains("aed" to "all") && ::aedMapHelper.isInitialized) { // AED필터가 선택된 경우에만
                val currentLat = naverMap.locationOverlay.position.latitude
                val currentLng = naverMap.locationOverlay.position.longitude
                aedMapHelper.fetchAEDLocations(currentLat, currentLng) // 현재 위치 기반으로 AED 데이터 가져오기

                addRadiusCircle(LatLng(currentLat, currentLng)) // 반경 1km 원 표시
            }
        }
    }

    private fun addRadiusCircle(center: LatLng) {
        val circleOverlay = com.naver.maps.map.overlay.CircleOverlay()
        circleOverlay.center = center
        circleOverlay.radius = 1000.0 // 반경 1km
        circleOverlay.color = 0x4087CEEB // 반투명 하늘색 (ARGB)
        circleOverlay.map = naverMap
    }

    private fun setCameraChangeListener() {
        // 카메라 변화 리스너를 설정하는 로직
        // naverMap.addOnCameraChangeListener { ... }
    }

    private fun observeClusterMarkerClick() {
        // 클러스터 마커 클릭 리스너를 설정하는 로직
    }

    private fun checkLocationPermission(activity: AppCompatActivity) {
        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE)
        }
    }

    private fun requestMapPermission(onPermissionGranted: () -> Unit) {
        if (ActivityCompat.checkSelfPermission(requireActivity(), Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            onPermissionGranted() // 권한이 이미 허용되면
        } else {
            ActivityCompat.requestPermissions(requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE) // 권한 요청
        }
    }

    //지진옥외대피소 api
    private fun fetchEarthquakeShelterData() {
        val baseUrl =
            "https://www.safetydata.go.kr/V2/api/DSSP-IF-10943?serviceKey=6QBKRUEJHQKEYY8O&numOfRows=100&pageNo="
        val startLot = 126.734086
        val endLot = 127.269311
        val startLat = 37.413294
        val endLat = 37.715133
        var pageNo = 1
        var totalFetched = 0

        fun fetchPage(page: Int) {
            val url = "$baseUrl$page&returnType=xml&startLot=$startLot&endLot=$endLot&startLat=$startLat&endLat=$endLat"

            val request = object : StringRequest(Request.Method.GET, url, { response ->
                val decodedResponse = String(response.toByteArray(Charsets.ISO_8859_1), Charsets.UTF_8)
                val parsedCount = parseEarthquakeShelterData(decodedResponse) // 현재 페이지 데이터 파싱

                if (parsedCount > 0) {
                    totalFetched += parsedCount
                    fetchPage(page + 1) // 다음 페이지 요청
                } else {
                    Log.i("EarthquakeShelter", "모든 데이터를 가져왔습니다. 총 개수: $totalFetched")
                }
            }, { error ->
                Log.e("EarthquakeShelter", "Error fetching shelter data: ${error.message}")
            }) {
                override fun getHeaders(): Map<String, String> {
                    return mapOf("Accept-Charset" to "UTF-8")
                }
            }

            Volley.newRequestQueue(requireContext()).add(request)
        }

        fetchPage(pageNo)
    }

    private fun parseEarthquakeShelterData(xmlData: String): Int {
        var itemCount = 0

        try {
            val factory = XmlPullParserFactory.newInstance()
            val parser = factory.newPullParser()
            parser.setInput(xmlData.reader())

            var eventType = parser.eventType
            var name: String? = null
            var address: String? = null
            var latitude: Double? = null
            var longitude: Double? = null

            while (eventType != XmlPullParser.END_DOCUMENT) {
                val tagName = parser.name
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        when (tagName) {
                            "VT_ACMDFCLTY_NM" -> name = parser.nextText().trim()
                            "RN_DTL_ADRES" -> address = parser.nextText().trim()
                            "LA" -> latitude = parser.nextText().toDoubleOrNull()
                            "LO" -> longitude = parser.nextText().toDoubleOrNull()
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (tagName == "item" && name != null && address != null && latitude != null && longitude != null) {
                            addEarthquakeShelterMarker(
                                LatLng(latitude, longitude),
                                name,
                                address
                            )
                            itemCount++ // 데이터 개수 증가
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("EarthquakeShelter", "Error parsing shelter data: ${e.message}")
        }

        return itemCount
    }

    //병원 api
    private fun fetchGeneralHospitalData() {
        val baseUrl =
            "https://www.safemap.go.kr/openApiService/data/getGenralHospitalData.do?serviceKey=QFWHCH6M-QFWH-QFWH-QFWH-QFWHCH6MOD"
        val dutyDiv = "A" // DutyDiv=A는 종합병원 필터
        val numOfRows = 30
        var pageNo = 1
        var totalFetched = 0

        fun fetchPage(page: Int) {
            val url = "$baseUrl&pageNo=$page&numOfRows=$numOfRows&type=xml&DutyDiv=$dutyDiv"

            val request = StringRequest(Request.Method.GET, url, { response ->
                val parsedCount = parseGeneralHospitalData(response) // 현재 페이지 데이터를 파싱

                if (parsedCount > 0) {
                    totalFetched += parsedCount
                    fetchPage(page + 1) // 다음 페이지 요청
                } else {
                    Log.i("GeneralHospital", "모든 데이터를 가져왔습니다. 총 개수: $totalFetched")
                }
            }, { error ->
                Log.e("GeneralHospital", "Error fetching hospital data: ${error.message}")
            })

            Volley.newRequestQueue(requireContext()).add(request)
        }

        fetchPage(pageNo)
    }

    private fun parseGeneralHospitalData(xmlData: String): Int {
        var itemCount = 0

        try {
            val factory = XmlPullParserFactory.newInstance()
            val parser = factory.newPullParser()
            parser.setInput(xmlData.reader())

            var eventType = parser.eventType
            var name: String? = null
            var address: String? = null
            var phoneNumber: String? = null
            var latitude: Double? = null
            var longitude: Double? = null

            while (eventType != XmlPullParser.END_DOCUMENT) {
                val tagName = parser.name
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        when (tagName) {
                            "DUTYNAME" -> name = parser.nextText() // 병원 이름
                            "DUTYADDR" -> address = parser.nextText() // 병원 주소
                            "DUTYTEL1" -> phoneNumber = parser.nextText() // 전화번호
                            "LAT" -> latitude = parser.nextText().toDoubleOrNull() // 위도
                            "LON" -> longitude = parser.nextText().toDoubleOrNull() // 경도
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (tagName == "item" && name != null && address != null && latitude != null && longitude != null) {
                            // 마커 추가
                            addHospitalMarker(
                                LatLng(latitude, longitude),
                                name,
                                address,
                                phoneNumber ?: "정보 없음"
                            )
                            itemCount++ // 데이터 개수 증가
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("GeneralHospital", "Error parsing XML data: ${e.message}")
        }

        return itemCount
    }

    //경찰관서 api
    private fun fetchPoliceStationData() {
        val baseUrl = "https://api.odcloud.kr/api/15054711/v1/uddi:4e87ae4e-3171-47ba-9f9a-c4b28098dac2"
        val serviceKey = "G9sDqmxYftvdX%2FQ%2BAcpSCcnTXGiRhlkSTyw6Wk4BMNDf0XM68KrDSGzRdGtz4YFyN46Chm8USwd29y5QNEfgdA%3D%3D"
        val perPage = 100
        var pageNo = 1
        var totalFetched = 0

        fun fetchPage(page: Int) {
            val url = "$baseUrl?page=$page&perPage=$perPage&returnType=JSON&serviceKey=$serviceKey"

            val request = JsonObjectRequest(Request.Method.GET, url, null, { response ->
                try {
                    val data = response.getJSONArray("data")
                    val totalCount = response.optInt("totalCount", 0) // 전체 데이터 수

                    for (i in 0 until data.length()) {
                        val item = data.getJSONObject(i)

                        // 필요한 데이터 추출
                        val policeStation = item.optString("경찰서")
                        val name = item.optString("관서명")
                        val phoneNumber = item.optString("전화번호")
                        val address = item.optString("주소")
                        val province = item.optString("시도청")

                        // 서울청 또는 경기청 데이터만 처리
                        if (province == "서울청" || province == "경기남부청" || province == "경기북부청") {
                            geocodeAndAddPoliceStationMarker(
                                address = address,
                                title = policeStation,
                                stationName = name,
                                phoneNumber = phoneNumber
                            )
                        }
                    }

                    totalFetched += data.length()

                    // 다음 페이지 요청
                    if (totalFetched < totalCount) {
                        fetchPage(page + 1)
                    } else {
                        Log.i("PoliceStation", "모든 데이터를 가져왔습니다. 총 개수: $totalFetched")
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Log.e("PoliceStation", "Error parsing police station data: ${e.message}")
                }
            }, { error ->
                Log.e("PoliceStation", "Error fetching police station data: ${error.message}")
            })

            Volley.newRequestQueue(requireContext()).add(request)
        }

        fetchPage(pageNo)
    }    //소방서 api
    private fun fetchFireStationData() {
        val url = "https://api.odcloud.kr/api/15048243/v1/uddi:a7630967-737e-4f06-84bc-f3e7b131f4a9?page=1&perPage=100&returnType=JSON&serviceKey=G9sDqmxYftvdX%2FQ%2BAcpSCcnTXGiRhlkSTyw6Wk4BMNDf0XM68KrDSGzRdGtz4YFyN46Chm8USwd29y5QNEfgdA%3D%3D"

        val request = JsonObjectRequest(Request.Method.GET, url, null, { response ->
            parseFireStationData(response) // JSON 파서로 처리
        }, { error ->
            Log.e("MapFragment", "Error fetching fire station data: ${error.message}")
        })

        Volley.newRequestQueue(requireContext()).add(request)
    }

    private fun parseFireStationData(jsonData: JSONObject) {
        try {
            // "data" 배열 가져오기
            val dataArray = jsonData.getJSONArray("data")

            for (i in 0 until dataArray.length()) {
                val item = dataArray.getJSONObject(i)

                val name = item.optString("소방서")
                val address = item.optString("주소")
                val phoneNumber = item.optString("전화번호")

                if (name.isNotEmpty() && address.isNotEmpty() && phoneNumber.isNotEmpty()) {
                    geocodeAndAddFireStationMarker(address, name, phoneNumber)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("MapFragment", "Error parsing fire station JSON data: ${e.message}")
        }
    }

    private fun geocodeAddress(
        address: String,
        onGeocodingSuccess: (LatLng) -> Unit,
        onGeocodingFailure: (String) -> Unit = { error ->
            Log.e("Geocode", "Geocoding failed for address: $error")
        }
    ) {
        val encodedAddress = URLEncoder.encode(address, "UTF-8")
        val url = "https://maps.apigw.ntruss.com/map-geocode/v2/geocode?query=$encodedAddress"

        val request = object : JsonObjectRequest(Request.Method.GET, url, null, { response ->
            val addresses = response.optJSONArray("addresses")
            if (addresses != null && addresses.length() > 0) {
                val firstResult = addresses.getJSONObject(0)
                val latitude = firstResult.getDouble("y")
                val longitude = firstResult.getDouble("x")

                // 성공 콜백 호출
                onGeocodingSuccess(LatLng(latitude, longitude))
            } else {
                onGeocodingFailure(address)
            }
        }, { error ->
            onGeocodingFailure("Error during geocoding: ${error.message}")
        }) {
            override fun getHeaders(): Map<String, String> {
                return mapOf(
                    "x-ncp-apigw-api-key-id" to "jv40jxc6a8",
                    "x-ncp-apigw-api-key" to "ddjRlhl1zePYUcq90z3pk8oIBWdQvB5dhxKnOSVh",
                    "Accept" to "application/json"
                )
            }
        }

        Volley.newRequestQueue(requireContext()).add(request)
    }

    private fun geocodeAndAddFireStationMarker(address: String, name: String, phoneNumber: String) {
        geocodeAddress(
            address,
            onGeocodingSuccess = { latLng ->
                addFireStationMarker(latLng, name, address, phoneNumber)
            },
            onGeocodingFailure = { error ->
                Log.e("FireStation", "Geocoding failed for address: $error")
            }
        )
    }

    private fun geocodeAndAddPoliceStationMarker(address: String, title: String, stationName: String, phoneNumber: String = "정보 없음") {
        geocodeAddress(
            address,
            onGeocodingSuccess = { latLng ->
                addPoliceStationMarker(
                    position = latLng,
                    stationName = stationName, // 관서명
                    title = title,       // 경찰서 이름
                    address = address,  // 주소
                    phoneNumber = phoneNumber // 전화번호
                )
            },
            onGeocodingFailure = { error ->
                Log.e("PoliceStation", "Geocoding failed for address: $error")
            }
        )
    }
    //사고다발지역 api
    private fun fetchSchoolZoneData(year: Int, siDo: Int, guGun: Int) {
        val url = "http://apis.data.go.kr/B552061/schoolzoneChild/getRestSchoolzoneChild" +
                "?ServiceKey=G9sDqmxYftvdX%2FQ%2BAcpSCcnTXGiRhlkSTyw6Wk4BMNDf0XM68KrDSGzRdGtz4YFyN46Chm8USwd29y5QNEfgdA%3D%3D" +
                "&searchYearCd=$year" +
                "&siDo=$siDo" +
                "&guGun=$guGun" +
                "&type=xml" +
                "&numOfRows=100" +
                "&pageNo=1"

        val request = StringRequest(Request.Method.GET, url, { response ->
            parseSchoolZoneData(response)
        }, { error ->
            Log.e("MapFragment", "Error fetching school zone data: ${error.message}")
        })

        Volley.newRequestQueue(requireContext()).add(request)
    }

    private fun parseSchoolZoneData(xmlData: String) {
        try {
            val factory = XmlPullParserFactory.newInstance()
            val parser = factory.newPullParser()
            parser.setInput(xmlData.reader())

            var eventType = parser.eventType
            var lat: Double? = null
            var lng: Double? = null
            var title: String? = null
            var occrrncCnt: String? = null
            var casltCnt: String? = null
            var dthDnvCnt: String? = null
            var seDnvCnt: String? = null
            var slDnvCnt: String? = null
            var wndDnvCnt: String? = null

            while (eventType != XmlPullParser.END_DOCUMENT) {
                val tagName = parser.name
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        when (tagName) {
                            "la_crd" -> lat = parser.nextText().toDoubleOrNull()
                            "lo_crd" -> lng = parser.nextText().toDoubleOrNull()
                            "spot_nm" -> title = parser.nextText()
                            "occrrnc_cnt" -> occrrncCnt = parser.nextText()
                            "caslt_cnt" -> casltCnt = parser.nextText()
                            "dth_dnv_cnt" -> dthDnvCnt = parser.nextText()
                            "se_dnv_cnt" -> seDnvCnt = parser.nextText()
                            "sl_dnv_cnt" -> slDnvCnt = parser.nextText()
                            "wnd_dnv_cnt" -> wndDnvCnt = parser.nextText()
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (tagName == "item" && lat != null && lng != null) {
                            addSchoolZoneMarker(
                                position = LatLng(lat, lng),
                                title = title ?: "스쿨존 사고 다발 지역",
                                location = title ?: "스쿨존 사고 다발 지역",
                                occrrncCnt = occrrncCnt ?: "정보 없음",
                                casltCnt = casltCnt ?: "정보 없음",
                                dthDnvCnt = dthDnvCnt ?: "정보 없음",
                                seDnvCnt = seDnvCnt ?: "정보 없음",
                                slDnvCnt = slDnvCnt ?: "정보 없음",
                                wndDnvCnt = wndDnvCnt ?: "정보 없음"
                            )
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun fetchAllSchoolZoneData() {
        val years = 2020..2024
        val seoulGuGunCodes = listOf(110, 140, 170, 200, 215, 230, 260, 290, 305, 320, 350, 380, 410, 440,
            470, 500, 530, 545, 560, 590, 620, 650, 680, 710, 740) // 서울 구군 코드 리스트
        val gyeonggiGuGunCodes = listOf(410, 420, 430, 441, 450, 460, 470, 480, 500, 510, 530, 550, 570, 590,
            610, 620, 650, 660, 670, 680, 690, 700, 710, 730, 750, 760, 770,
            790, 800, 810, 820, 830, 840, 860, 870, 890, 900, 910, 920, 940,
            950, 960) // 경기도 구군 코드 리스트

        for (year in years) {
            // 서울특별시 (siDo = 11)
            for (guGun in seoulGuGunCodes) {
                fetchSchoolZoneData(year, 11, guGun) // siDo 11: 서울
            }
            // 경기도 (siDo = 41)
            for (guGun in gyeonggiGuGunCodes) {
                fetchSchoolZoneData(year, 41, guGun) // siDo 41: 경기
            }
        }
    }

    // 경찰청 API 중계 서버 URL
    private val policeApiUrl = "http://34.10.183.181:3000/proxy"

    fun fetchTrafficData() {
        val request = StringRequest(Request.Method.GET, policeApiUrl, { response ->
            Log.d("MapFragment", "Response: $response")  // 응답 데이터 확인
            parseTrafficData(response)
        }, { error ->
            error.printStackTrace()
            Log.e("MapFragment", "Error fetching traffic data: ${error.message}")
        })

        Volley.newRequestQueue(requireContext()).add(request)
    }

    // 필터 초기화 함수
    private fun initializeFilters() {
        selectedFilters.clear() // 기본적으로 모든 필터 해제 상태
    }

    private fun initializeButtons(rootView: View) {
        trafficButton = rootView.findViewById(R.id.btn_traffic_accident)
        constructionButton = rootView.findViewById(R.id.btn_construction)
        eventButton = rootView.findViewById(R.id.btn_event)
        controlButton = rootView.findViewById(R.id.btn_control)
        disasterButton = rootView.findViewById(R.id.btn_disaster)
        otherButton = rootView.findViewById(R.id.btn_other)
        schoolzoneButton = rootView.findViewById(R.id.btn_schoolzone)
        aedButton = rootView.findViewById(R.id.btn_aed)
        firestationButton = rootView.findViewById(R.id.btn_fire_station)
        policestationButton = rootView.findViewById(R.id.btn_police_station)
        hospitalButton = rootView.findViewById(R.id.btn_hospital)
        shelterButton = rootView.findViewById(R.id.btn_shelter)

        // 모든 버튼을 리스트에 추가
        filterButtons = listOf(
            trafficButton, constructionButton, eventButton, controlButton, disasterButton,
            otherButton, schoolzoneButton, aedButton, firestationButton, policestationButton,
            hospitalButton, shelterButton
        )
    }

    // 각 버튼에 대한 공통 클릭 로직을 처리하는 함수
    private fun handleFilterButtonClick(
        clickedButton: View,
        filterKey: String,
        filterValue: String,
        toastMessage: String
    ) {
        if (lastSelectedButton == clickedButton) {
            // 이미 선택된 버튼을 다시 클릭한 경우: 선택 해제
            clickedButton.isSelected = false
            selectedFilters.clear()
            Toast.makeText(requireContext(), "필터 해제됨", Toast.LENGTH_SHORT).show()
            lastSelectedButton = null
        } else {
            // 새로운 버튼을 클릭한 경우 또는 다른 버튼을 클릭한 경우
            // 1. 이전에 선택된 버튼이 있다면 선택 해제
            lastSelectedButton?.isSelected = false

            // 2. 현재 클릭된 버튼 선택
            clickedButton.isSelected = true
            selectedFilters.clear()
            selectedFilters.add(filterKey to filterValue)
            Toast.makeText(requireContext(), toastMessage, Toast.LENGTH_SHORT).show()

            // 3. 마지막으로 선택된 버튼 업데이트
            lastSelectedButton = clickedButton
        }

        applyFiltersToMap() // 필터에 따른 마커 업데이트
    }

    // 필터 버튼 클릭 리스너 설정 함수
    private fun setupFilterButtonListeners() {
        trafficButton.setOnClickListener {
            handleFilterButtonClick(it, "1", "255", "교통사고 필터 적용")
        }

        constructionButton.setOnClickListener {
            handleFilterButtonClick(it, "2", "all", "공사 필터 적용")
        }

        eventButton.setOnClickListener {
            handleFilterButtonClick(it, "3", "all", "행사 필터 적용")
        }

        controlButton.setOnClickListener {
            handleFilterButtonClick(it, "5", "all", "통제 필터 적용")
        }

        disasterButton.setOnClickListener {
            handleFilterButtonClick(it, "6", "all", "재난 필터 적용")
        }

        otherButton.setOnClickListener {
            handleFilterButtonClick(it, "9", "all", "기타 필터 적용")
        }

        schoolzoneButton.setOnClickListener {
            handleFilterButtonClick(it, "schoolzone", "accident", "스쿨존 사고다발지역 필터 적용")
        }

        aedButton.setOnClickListener {
            handleFilterButtonClick(it, "aed", "all", "자동제세동기(AED) 필터 적용")
        }

        firestationButton.setOnClickListener {
            handleFilterButtonClick(it, "firestation", "all", "소방서 필터 적용")
        }

        policestationButton.setOnClickListener {
            handleFilterButtonClick(it, "PoliceStation", "all", "경찰관서 필터 적용")
        }

        hospitalButton.setOnClickListener {
            handleFilterButtonClick(it, "GeneralHospital", "all", "종합병원 필터 적용")
        }

        shelterButton.setOnClickListener {
            handleFilterButtonClick(it, "EarthquakeShelter", "all", "지진옥외대피소 필터 적용")
        }
    }

    // 필터 적용 및 마커 업데이트 함수
    private fun applyFiltersToMap() {
        // 기존 마커를 모두 제거
        markers.forEach { it.map = null }
        markers.clear()

        // 선택된 필터에 따라 데이터를 불러옴

        if (selectedFilters.contains("EarthquakeShelter" to "all")) {
            fetchEarthquakeShelterData() //지진옥외대피소 데이터 로드
        }
        if (selectedFilters.contains("GeneralHospital" to "all")) {
            fetchGeneralHospitalData() //병원 데이터 로드
        }
        if (selectedFilters.contains("PoliceStation" to "all")) {
            fetchPoliceStationData() // 경찰관서 데이터 로드
        }
        if (selectedFilters.contains("firestation" to "all")) {
            fetchFireStationData() // 소방서 데이터 로드
        }
        if (selectedFilters.contains("schoolzone" to "accident")) {
            fetchAllSchoolZoneData() // 교통사고 다발지역 데이터 로드
        }
        if (selectedFilters.contains("1" to "255")) {
            fetchTrafficData() // 교통사고 데이터 로드
        }
        if (selectedFilters.contains("2" to "all")) {
            fetchTrafficData() // 공사 데이터 로드
        }
        if (selectedFilters.contains("3" to "all")) {
            fetchTrafficData() // 행사 데이터 로드
        }
        if (selectedFilters.contains("5" to "all")) {
            fetchTrafficData() // 통제 데이터 로드
        }
        if (selectedFilters.contains("6" to "all")) {
            fetchTrafficData() // 재난 데이터 로드
        }
        if (selectedFilters.contains("9" to "all")) {
            fetchTrafficData() // 기타 데이터 로드
        }
        if (selectedFilters.contains("aed" to "all")) {
            requestCurrentLocation() // AED 데이터 로드
        }
    }

    private fun applyFilter(incidentType: String, incidentSubType: String): Boolean {
        return selectedFilters.contains(incidentType to "all") ||
                selectedFilters.contains(incidentType to incidentSubType) ||
                (incidentType == "schoolzone" && incidentSubType == "accident" && selectedFilters.contains("schoolzone" to "accident"))
    }

    fun parseTrafficData(xmlData: String) {
        try {
            val factory = XmlPullParserFactory.newInstance()
            val parser = factory.newPullParser()
            parser.setInput(xmlData.reader())

            var eventType = parser.eventType
            var incidentType: String? = null
            var incidentSubType: String? = null
            var x: Double? = null
            var y: Double? = null
            var incidentTitle: String? = null
            var startDate: String? = null

            while (eventType != XmlPullParser.END_DOCUMENT) {
                val tagName = parser.name
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        when (tagName) {
                            "incidenteTypeCd" -> incidentType = parser.nextText()
                            "incidenteSubTypeCd" -> incidentSubType = parser.nextText()
                            "locationDataX" -> x = parser.nextText().toDoubleOrNull()
                            "locationDataY" -> y = parser.nextText().toDoubleOrNull()
                            "incidentTitle" -> incidentTitle = parser.nextText()
                            "startDate" -> startDate = parser.nextText()
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (tagName == "record" && x != null && y != null) {
                            // 필터 조건을 만족하는 경우만 마커를 추가
                            if (incidentType != null && incidentSubType != null && applyFilter(
                                    incidentType,
                                    incidentSubType
                                )
                            ) {
                                addTrafficMarker(
                                    LatLng(y, x),
                                    incidentTitle ?: "돌발 사고",
                                    incidentType,
                                    startDate
                                )
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

    //지진옥외대피소 마커 추가
    private fun addEarthquakeShelterMarker(position: LatLng, name: String, address: String) {
        val marker = Marker()
        marker.position = position
        marker.map = naverMap
        marker.icon = OverlayImage.fromResource(R.drawable.shelter_icon)

        marker.setOnClickListener {
            showInfoPanel(
                title = name,
                location = address,
                isEarthquakeShelter = true
            )
            true
        }

        markers.add(marker)
    }

    //병원 마커 추가
    private fun addHospitalMarker(position: LatLng, name: String, address: String, phoneNumber: String) {
        val marker = Marker()
        marker.position = position
        marker.map = naverMap
        marker.icon = OverlayImage.fromResource(R.drawable.hospital_icon) // 병원 아이콘

        marker.setOnClickListener {
            // 정보 패널 표시
            showInfoPanel(
                title = name,
                location = address,
                phoneNumber = phoneNumber,
                isHospital = true
            )
            true
        }

        markers.add(marker)
    }

    //경찰관서 마커 추가
    private fun addPoliceStationMarker(
        position: LatLng,
        stationName: String,
        title: String,
        address: String,
        phoneNumber: String = "정보 없음"
    ) {
        val marker = Marker()
        marker.position = position
        marker.map = naverMap
        marker.icon = OverlayImage.fromResource(R.drawable.police_station_icon)

        marker.setOnClickListener {
            showInfoPanel(
                title = title,
                location = address,
                phoneNumber = phoneNumber,
                stationName = stationName,
                isPoliceStation = true
            )
            true
        }

        markers.add(marker)
    }

    //소방서 마커 추가
    private fun addFireStationMarker(position: LatLng, title: String, address: String, phoneNumber: String) {
        val marker = Marker()
        marker.position = position
        marker.map = naverMap
        marker.icon = OverlayImage.fromResource(R.drawable.fire_station_icon) // 소방서 아이콘 설정

        // 마커 클릭 리스너
        marker.setOnClickListener {
            // 정보 패널 표시
            showInfoPanel(
                title = title,
                location = address,
                phoneNumber = phoneNumber,
                isFireStation = true // 소방서 마커임을 명시
            )
            true
        }

        markers.add(marker)
    }

    //경찰청 마커 추가
    fun addTrafficMarker(position: LatLng, title: String, type: String?, incidentDate: String?) {
        val marker = Marker()
        marker.position = position
        marker.map = naverMap

        marker.icon = when (type) {
            "1" -> OverlayImage.fromResource(R.drawable.accident_icon) // 사고 아이콘
            "2" -> OverlayImage.fromResource(R.drawable.construction_icon) // 공사 아이콘
            "3" -> OverlayImage.fromResource(R.drawable.event_icon) // 행사 아이콘
            "5" -> OverlayImage.fromResource(R.drawable.control_icon) // 통제 아이콘
            "6" -> OverlayImage.fromResource(R.drawable.disaster_icon) // 재난 아이콘
            else -> OverlayImage.fromResource(R.drawable.other_icon) // 기타 아이콘
        }

        marker.setOnClickListener {
            showInfoPanel(
                title = title,
                location = "교통사고 발생 위치",
                occrrncCnt = incidentDate,
                isTrafficIncident = true
            )
            true
        }

        markers.add(marker)
    }

    // 스쿨존 마커 추가
    private fun addSchoolZoneMarker(
        position: LatLng,
        title: String,
        location: String,
        occrrncCnt: String,
        casltCnt: String,
        dthDnvCnt: String,
        seDnvCnt: String,
        slDnvCnt: String,
        wndDnvCnt: String
    ) {
        val marker = Marker()
        marker.position = position
        marker.map = naverMap
        marker.icon = OverlayImage.fromResource(R.drawable.schoolzone_icon) // 스쿨존 아이콘 설정

        marker.setOnClickListener {
            showInfoPanel(
                title = title,
                location = location,
                occrrncCnt = occrrncCnt,
                casltCnt = casltCnt,
                dthDnvCnt = dthDnvCnt,
                seDnvCnt = seDnvCnt,
                slDnvCnt = slDnvCnt,
                wndDnvCnt = wndDnvCnt,
                isSchoolZone = true
            )
            true
        }

        markers.add(marker)
    }

    private fun addAEDMarker(location: LatLng, address: String) {
        val marker = Marker()
        marker.position = location
        marker.icon = OverlayImage.fromResource(R.drawable.aed_icon) // AED 마커 아이콘 설정
        marker.map = naverMap

        marker.setOnClickListener {
            showInfoPanel(
                title = "자동제세동기(AED)",
                location = address,
                isAED = true
            )
            true
        }

        markers.add(marker)
    }

    // 정보 패널 표시하기
    private fun showInfoPanel(
        title: String,
        location: String,
        phoneNumber: String? = null,
        occrrncCnt: String? = null,
        casltCnt: String? = null,
        dthDnvCnt: String? = null,
        seDnvCnt: String? = null,
        slDnvCnt: String? = null,
        wndDnvCnt: String? = null,
        isSchoolZone: Boolean = false,
        isTrafficIncident: Boolean = false,
        isAED: Boolean = false,
        isFireStation: Boolean = false,
        isPoliceStation: Boolean = false,
        stationName: String? = null,
        isHospital: Boolean = false,
        isEarthquakeShelter: Boolean = false
    ) {
        binding.infoPanel.visibility = View.VISIBLE
        binding.infoTitle.text = title

        binding.viewDetailsButton.setOnClickListener {
            when {
                isSchoolZone -> showDetailedSchoolZoneInfo(location, occrrncCnt, casltCnt, dthDnvCnt, seDnvCnt, slDnvCnt, wndDnvCnt)
                isTrafficIncident -> showDetailedTrafficInfo(title, location, occrrncCnt)
                isAED -> showDetailedAEDInfo(location)
                isFireStation -> showFireStationDetails(title, phoneNumber ?: "정보 없음", location)
                isPoliceStation -> showPoliceStationDetails(stationName = stationName ?: "정보 없음",
                    name = title,
                    phoneNumber = phoneNumber ?: "정보 없음",
                    address = location)
                isHospital -> showHospitalDetails(
                    name = title,
                    phoneNumber = phoneNumber ?: "정보 없음",
                    address = location)
                isEarthquakeShelter -> showEarthquakeShelterDetails(
                    name = title,
                    address = location)
            }
        }

        binding.closeButton.setOnClickListener {
            binding.infoPanel.visibility = View.GONE
        }
    }

    // 발생 일시 포맷팅 함수
    private fun formatIncidentDate(dateString: String?): String {
        if (dateString.isNullOrEmpty()) {
            return "날짜 정보 없음" // 날짜가 없는 경우 기본 메시지
        }

        // API에서 제공된 "yyyy년 MM월 dd일 HH시 mm분" 형식에 맞춰 포맷 지정
        val inputFormat = SimpleDateFormat("yyyy년 MM월 dd일 HH시 mm분", Locale.getDefault())
        val outputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

        return try {
            val date = inputFormat.parse(dateString)
            outputFormat.format(date ?: Date())
        } catch (e: Exception) {
            Log.e("MapFragment", "Date format error: ${e.message}")
            "날짜 형식 오류"
        }
    }

    //지진옥외대피소 상세 정보 표시
    private fun showEarthquakeShelterDetails(name: String, address: String) {
        val detailMessage = """
        시설명: $name
        주소: $address

        [출처] 행정안전부
    """.trimIndent()

        AlertDialog.Builder(requireContext())
            .setTitle("지진옥외대피장소 상세 정보")
            .setMessage(detailMessage)
            .setPositiveButton("닫기", null)
            .show()
    }

    // 병원 상세 정보 표시
    private fun showHospitalDetails(name: String, phoneNumber: String, address: String) {
        val detailMessage = """
        병원명: $name
        전화번호: $phoneNumber
        주소: $address
        
        [출처] 국립중앙의료원
    """.trimIndent()

        AlertDialog.Builder(requireContext())
            .setTitle("병원 상세 정보")
            .setMessage(detailMessage)
            .setPositiveButton("닫기", null)
            .show()
    }

    // 스쿨존 상세 정보 표시
    private fun showDetailedSchoolZoneInfo(
        location: String,
        occrrncCnt: String?,
        casltCnt: String?,
        dthDnvCnt: String?,
        seDnvCnt: String?,
        slDnvCnt: String?,
        wndDnvCnt: String?
    ) {
        val detailMessage = """
            교통사고 다발지역
            위치: $location
            발생건수: $occrrncCnt
            사상자수: $casltCnt
            사망자수: $dthDnvCnt
            중상자수: $seDnvCnt
            경상자수: $slDnvCnt
            부상자신고자수: $wndDnvCnt
            [출처] 한국도로교통공단
        """.trimIndent()

        AlertDialog.Builder(requireContext())
            .setTitle("스쿨존 사고 상세 정보")
            .setMessage(detailMessage)
            .setPositiveButton("닫기", null)
            .show()
    }

    // 교통 사고 상세 정보 표시
    private fun showDetailedTrafficInfo(title: String, location: String, incidentDate: String?) {
        val detailMessage = """
            $title
            발생일시: ${formatIncidentDate(incidentDate)}
            [출처] 경찰청 도시교통정보센터
        """.trimIndent()

        AlertDialog.Builder(requireContext())
            .setTitle("교통 사고 상세 정보")
            .setMessage(detailMessage)
            .setPositiveButton("닫기", null)
            .show()
    }

    // AED 상세 정보 표시
    private fun showDetailedAEDInfo(address: String) {
        val detailMessage = """
            자동제세동기(AED)
            주소: $address
            [출처] 국립중앙의료원
        """.trimIndent()

        AlertDialog.Builder(requireContext())
            .setTitle("AED 상세 정보")
            .setMessage(detailMessage)
            .setPositiveButton("닫기", null)
            .show()
    }

    private fun showFireStationDetails(name: String, phoneNumber: String, address: String) {
        val detailMessage = """
        기관명: $name
        전화번호: $phoneNumber
        주소: $address
        
        [출처] 소방청
    """.trimIndent()

        AlertDialog.Builder(requireContext())
            .setTitle("소방서 상세 정보")
            .setMessage(detailMessage)
            .setPositiveButton("닫기", null) // 닫기 버튼
            .show()
    }

    private fun showPoliceStationDetails(
        stationName: String, // 관서명
        name: String,        // 경찰서 이름
        phoneNumber: String,
        address: String
    ) {
        val detailMessage = """
        경찰서: $name
        관서명: $stationName
        전화번호: $phoneNumber
        주소: $address
        
        [출처] 경찰청
    """.trimIndent()

        AlertDialog.Builder(requireContext())
            .setTitle("경찰관서 상세 정보")
            .setMessage(detailMessage)
            .setPositiveButton("닫기", null)
            .show()
    }

    companion object {
        const val DEFAULT_ZOOM = 15.0
        const val LOCATION_PERMISSION_REQUEST_CODE = 1000
        private var isGuideShown = false
    }
}