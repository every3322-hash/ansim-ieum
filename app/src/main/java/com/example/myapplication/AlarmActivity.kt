package com.example.myapplication

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.appcompat.widget.Toolbar

class AlarmActivity : AppCompatActivity() {
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var switchLocation: SwitchCompat
    private lateinit var switchVibration: SwitchCompat
    private lateinit var switchEmergency: SwitchCompat
    private lateinit var switchPost: SwitchCompat
    private lateinit var switchComment: SwitchCompat

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alarm)

        val toolbar = findViewById<Toolbar>(R.id.top_toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayShowTitleEnabled(false)

            val backButton = findViewById<ImageView>(R.id.btnBack)
            backButton.setOnClickListener {
                finish()
            }
        }

        sharedPreferences = getSharedPreferences("PermissionPrefs", MODE_PRIVATE) // 스위치 초기화
        switchLocation = findViewById(R.id.switchView)
        switchVibration = findViewById(R.id.switchView2)
        switchEmergency = findViewById(R.id.switchAlarm)
        switchPost = findViewById(R.id.switchAlarm2)
        switchComment = findViewById(R.id.switchAlarm3)

        switchLocation.setOnClickListener {
            showSettingsDialog("위치")
        }

        // 알림 권한 관련 스위치 클릭 시 다이얼로그
        val clickListener = { _: Any ->
            showSettingsDialog("알림")
        }
        switchEmergency.setOnClickListener(clickListener)
        switchPost.setOnClickListener(clickListener)
        switchComment.setOnClickListener(clickListener)

        // 진동 스위치 클릭 시
        switchVibration.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit().putBoolean("vibration_enabled", isChecked).apply()
            val message = if (isChecked) "진동이 활성화되었습니다." else "진동이 비활성화되었습니다."
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            // 진동 시작
            if (isChecked) {
                vibrateDevice(this)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateSwitchStatesFromPermissions()
        setSwitchStates()
    }

    private fun setSwitchStates() {
        switchLocation.isChecked = sharedPreferences.getBoolean("location_permission", false)
        val notificationPermission = sharedPreferences.getBoolean("notification_permission", false)

        // 알림 권한이 있으면 3개 모두 ON
        if (notificationPermission) {
            switchEmergency.isChecked = true
            switchPost.isChecked = true
            switchComment.isChecked = true
        } else {
            switchEmergency.isChecked = false
            switchPost.isChecked = false
            switchComment.isChecked = false
        }

        // 진동 스위치 상태 설정
        val vibrationEnabled = sharedPreferences.getBoolean("vibration_enabled", false)
        switchVibration.isChecked = vibrationEnabled
    }

    private fun updateSwitchStatesFromPermissions() {
        val locationGranted = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val notificationGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Android 12 이하에서는 자동 허용
        }

        switchLocation.isChecked = locationGranted
        switchPost.isChecked = notificationGranted
        switchComment.isChecked = notificationGranted

        // SharedPreferences에도 반영
        sharedPreferences.edit().apply {
            putBoolean("location_permission", locationGranted)
            putBoolean("notification_permission", notificationGranted)
            apply()
        }
    }

    private fun showSettingsDialog(permissionName: String) {
        AlertDialog.Builder(this)
            .setTitle("$permissionName 권한 설정")
            .setMessage("이 앱을 사용하려면 $permissionName 권한이 필요합니다. 설정 화면으로 이동하시겠습니까?")
            .setPositiveButton("예") { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", packageName, null)
                }
                startActivity(intent)
            }
            .setNegativeButton("아니요", null)
            .show()
    }

    // 진동 시작 함수
    private fun vibrateDevice(context: Context) {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val vibrationEffect = VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE)
            vibrator.vibrate(vibrationEffect)
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(500)
        }
    }
}
