package com.example.myapplication

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar

class PrivacyActivity : AppCompatActivity() {

    private lateinit var privacyDropdown: LinearLayout
    private lateinit var privacyDropdown2: LinearLayout
    private lateinit var privacyDropdown3: LinearLayout
    private lateinit var expandIcon: ImageView
    private lateinit var expandIcon2: ImageView
    private lateinit var expandIcon3: ImageView
    private lateinit var privacyText: TextView
    private lateinit var privacyText2: TextView
    private lateinit var privacyText3: TextView
    private var isExpandedMap = mutableMapOf<Int, Boolean>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_privacy)
        setupToolbar()
        setupDropdownViews()
        setupDropdownListeners()
    }

    private fun setupToolbar() {
        val toolbar = findViewById<Toolbar>(R.id.top_toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        val backButton = findViewById<ImageView>(R.id.btnBack)
        backButton.setOnClickListener { finish() }
    }

    private fun setupDropdownViews() {
        privacyDropdown = findViewById(R.id.privacyDropdown)
        privacyDropdown2 = findViewById(R.id.privacyDropdown2)
        privacyDropdown3 = findViewById(R.id.privacyDropdown3)
        expandIcon = findViewById(R.id.expandIcon)
        expandIcon2 = findViewById(R.id.expandIcon2)
        expandIcon3 = findViewById(R.id.expandIcon3)
        privacyText = findViewById(R.id.privacy_Text)
        privacyText2 = findViewById(R.id.privacy_Text2)
        privacyText3 = findViewById(R.id.privacy_Text3)
    }

    private fun setupDropdownListeners() {
        findViewById<LinearLayout>(R.id.privacy_Tab).setOnClickListener {
            toggleDropdown(privacyDropdown, expandIcon, 1)
        }
        findViewById<LinearLayout>(R.id.privacy_Tab2).setOnClickListener {
            toggleDropdown(privacyDropdown2, expandIcon2, 2)
        }
        findViewById<LinearLayout>(R.id.privacy_Tab3).setOnClickListener {
            toggleDropdown(privacyDropdown3, expandIcon3, 3)
        }
    }

    private fun toggleDropdown(dropdownView: LinearLayout, expandIconView: ImageView, key: Int) {
        // 다른 드롭다운 닫기
        isExpandedMap.forEach { (k, _) ->
            if (k != key && isExpandedMap[k] == true) {
                val otherDropdownView = when (k) {
                    1 -> privacyDropdown
                    2 -> privacyDropdown2
                    3 -> privacyDropdown3
                    else -> null
                }
                val otherExpandIconView = when (k) {
                    1 -> expandIcon
                    2 -> expandIcon2
                    3 -> expandIcon3
                    else -> null
                }
                otherDropdownView?.visibility = View.GONE
                otherExpandIconView?.setImageResource(R.drawable.baseline_right)
                isExpandedMap[k] = false
            }
        }

        // 현재 드롭다운 토글
        isExpandedMap[key] = !(isExpandedMap[key] ?: false)
        val isExpanded = isExpandedMap[key] ?: false

        if (isExpanded) {
            dropdownView.visibility = View.VISIBLE
            expandIconView.setImageResource(R.drawable.baseline_down) // 드롭다운 아이콘으로 변경

            // 드롭다운별로 다른 파일 읽기
            when (key) {
                1 -> privacyText.text = loadTextFromAsset("privacy_terms1.txt")
                2 -> privacyText2.text = loadTextFromAsset("privacy_terms2.txt")
                3 -> privacyText3.text = loadTextFromAsset("privacy_terms3.txt")
            }

        } else {
            dropdownView.visibility = View.GONE
            expandIconView.setImageResource(R.drawable.baseline_right) // 오른쪽 화살표로 변경
        }
    }

    private fun loadTextFromAsset(fileName: String): String {
        return try {
            val inputStream = assets.open(fileName)
            inputStream.bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            "내용을 불러오는 데 실패했습니다."
        }
    }
}
