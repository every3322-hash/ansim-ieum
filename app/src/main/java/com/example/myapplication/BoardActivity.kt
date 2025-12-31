package com.example.myapplication

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.myapplication.databinding.ActivityBoardBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.MapFragment
import com.naver.maps.map.NaverMap
import com.naver.maps.map.OnMapReadyCallback
import com.naver.maps.map.overlay.Marker
import org.json.JSONObject
import java.io.IOException

class BoardActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var binding: ActivityBoardBinding
    private lateinit var firestore: FirebaseFirestore
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var naverMap: NaverMap? = null
    private var selectedLatLng: LatLng? = null
    private var selectedImageUri: Uri? = null
    private var authorName: String = "익명"
    private var profileImage: String = "profile1" // 기본 프로필 이미지
    private var currentMarker: Marker? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBoardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Firebase 초기화
        firestore = FirebaseFirestore.getInstance()
        firebaseAuth = FirebaseAuth.getInstance()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // 사용자 정보 로드
        loadUserData()

        // 네이버 지도 초기화 (위치 추가 버튼 클릭 시 활성화)
        val mapFragment = supportFragmentManager.findFragmentById(R.id.mapContainer) as MapFragment?
            ?: MapFragment.newInstance().also {
                supportFragmentManager.beginTransaction().add(R.id.mapContainer, it).commit()
            }
        mapFragment.getMapAsync(this)
        binding.mapContainer.visibility = View.GONE  // 초기 상태에서 숨김

        // 위치 추가 버튼 클릭 시 지도 표시
        binding.locationButton.setOnClickListener {
            binding.mapContainer.visibility = View.VISIBLE
        }

        // 사진 추가 버튼 클릭 리스너
        binding.photoButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            photoResultLauncher.launch(intent)
        }

        binding.backButton.setOnClickListener { finish() }

        // 글 작성 버튼 클릭 시 Firestore에 데이터 저장
        binding.inputBtn.setOnClickListener {
            val title = binding.titleArea.text.toString().trim()
            val content = binding.contentArea.text.toString().trim()

            if (title.isEmpty() || content.isEmpty()) {
                Toast.makeText(this, "제목과 내용을 입력해주세요.", Toast.LENGTH_SHORT).show()
            } else {
                binding.inputBtn.setOnClickListener {
                    val title = binding.titleArea.text.toString().trim()
                    val content = binding.contentArea.text.toString().trim()

                    if (title.isEmpty() || content.isEmpty()) {
                        Toast.makeText(this, "제목과 내용을 입력해주세요.", Toast.LENGTH_SHORT).show()
                    } else {
                        if (selectedLatLng != null) {
                            val lat = selectedLatLng!!.latitude
                            val lng = selectedLatLng!!.longitude

                            // 여기서 지역 이름 가져와서 저장하도록 수정
                            fetchRegionNameFromLocation(lat, lng) { regionName ->
                                if (regionName != null) {
                                    savePostToFirestore(title, content, lat, lng, regionName)
                                } else {
                                    Toast.makeText(this, "지역 정보를 가져오지 못했습니다.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } else {
                            Toast.makeText(this, "지도를 클릭해 위치를 선택해주세요.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }

        getFcmToken()
    }

    private fun loadUserData() {
        val userId = firebaseAuth.currentUser?.uid ?: return
        firestore.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    authorName = document.getString("name") ?: "익명"
                    profileImage = document.getString("profileImage") ?: "profile1"
                } else {
                    Toast.makeText(this, "사용자 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "사용자 정보를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show()
            }
    }

    // 사진 선택 결과 처리
    private val photoResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                selectedImageUri = result.data?.data
                binding.photoButton.text = "사진이 선택되었습니다."
            }
        }

    override fun onMapReady(map: NaverMap) {
        naverMap = map
        getCurrentLocation()

        // 지도 클릭 시 선택한 위치 저장
        naverMap?.setOnMapClickListener { _, latLng ->
            selectedLatLng = latLng
            Toast.makeText(this, "선택된 위치: ${latLng.latitude}, ${latLng.longitude}", Toast.LENGTH_SHORT).show()

            // 이전 마커 제거
            currentMarker?.map = null

            // 새 마커 생성 및 표시
            val marker = Marker()
            marker.position = latLng
            marker.map = naverMap
            currentMarker = marker

            // 카메라 이동
            naverMap?.moveCamera(CameraUpdate.scrollTo(latLng))
        }
    }

    private fun savePostToFirestore(title: String, content: String, latitude: Double, longitude: Double, region: String) {
        val post = hashMapOf(
            "title" to title,
            "content" to content,
            "latitude" to latitude,
            "longitude" to longitude,
            "region" to region,
            "authorName" to authorName,
            "profileImage" to profileImage,
            "timestamp" to System.currentTimeMillis(),
            "userId" to firebaseAuth.currentUser?.uid
        )

        firestore.collection("posts")
            .add(post)
            .addOnSuccessListener {
                Toast.makeText(this, "글이 저장되었습니다!", Toast.LENGTH_SHORT).show()
                sendNotificationToAllUsers(title, content) // 푸시 알림 전송
                finish() // 저장 후 현재 Activity 종료
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "글 저장에 실패했습니다: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun getCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    val currentLatLng = LatLng(it.latitude, it.longitude)
                    val marker = Marker()
                    marker.position = currentLatLng
                    marker.map = naverMap

                    val cameraUpdate = CameraUpdate.scrollTo(currentLatLng)
                    naverMap?.moveCamera(cameraUpdate)
                }
            }
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
        }
    }

    private fun sendNotificationToAllUsers(title: String, content: String) {
        val notificationData = hashMapOf(
            "title" to title,
            "content" to content
        )

        firestore.collection("users")
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    val token = document.getString("fcmToken")
                    token?.let {
                        sendPushNotification(it, title, content)
                    }
                }
            }
    }

    private fun sendPushNotification(token: String, title: String, content: String) {
        val notificationData = hashMapOf(
            "to" to token,
            "data" to mapOf(
                "title" to title,
                "content" to content
            )
        )

        // HTTP POST 요청을 사용하여 FCM 서버에 알림 전송
        // Firebase Cloud Function 또는 서버에서 처리하는 것이 보안에 유리하다
    }

    private fun getFcmToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                Log.d("FCM Token", "Token: $token")
            }
        }
    }

    private fun fetchRegionNameFromLocation(lat: Double, lng: Double, callback: (String?) -> Unit) {
        val url = "https://maps.apigw.ntruss.com/map-reversegeocode/v2/gc" +
                "?coords=$lng,$lat&orders=admcode&output=json"

        val request = okhttp3.Request.Builder()
            .url(url)
            .addHeader("X-NCP-APIGW-API-KEY-ID", "jv40jxc6a8")
            .addHeader("X-NCP-APIGW-API-KEY", "ddjRlhl1zePYUcq90z3pk8oIBWdQvB5dhxKnOSVh")
            .build()

        val client = okhttp3.OkHttpClient()
        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                Log.e("Region", "주소 정보 요청 실패", e)
                callback(null)
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                val responseBody = response.body?.string() ?: return callback(null)
                try {
                    val json = JSONObject(responseBody)
                    val results = json.getJSONArray("results")

                    if (results.length() == 0) {
                        runOnUiThread {
                            Toast.makeText(this@BoardActivity, "선택한 위치의 주소를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
                        }
                        callback(null)
                        return
                    }

                    val region = results.getJSONObject(0).getJSONObject("region")
                    val area1 = region.getJSONObject("area1").getString("name")
                    val area2 = region.getJSONObject("area2").getString("name")
                    val regionName = "$area1 $area2"
                    callback(regionName)
                } catch (e: Exception) {
                    Log.e("Region", "주소 파싱 실패", e)
                    callback(null)
                }
            }
        })
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getCurrentLocation()
        } else {
            Toast.makeText(this, "위치 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
        }
    }


    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }
}
