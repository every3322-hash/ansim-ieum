package com.example.myapplication

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment

class Tab3Fragment : Fragment(){
    companion object {
        fun newInstance() : MemoFragment = MemoFragment()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_tab3, container, false)

        // 버튼 클릭 이벤트로 해당 카테고리로 이동
        view.findViewById<Button>(R.id.tab3_btn1).setOnClickListener {
            navigateToCategory("03002") // 응급처치 카테고리
        }
        view.findViewById<Button>(R.id.tab3_btn2).setOnClickListener {
            navigateToCategory("03003") // 심폐소생술 카테고리
        }
        view.findViewById<Button>(R.id.tab3_btn3).setOnClickListener {
            navigateToCategory("03004") // 소화기 카테고리
        }
        view.findViewById<Button>(R.id.tab3_btn4).setOnClickListener {
            navigateToCategory("03005") // 식중독 카테고리
        }
        view.findViewById<Button>(R.id.tab3_btn5).setOnClickListener {
            navigateToCategory("03006") // 산행안전 카테고리
        }
        view.findViewById<Button>(R.id.tab3_btn6).setOnClickListener {
            navigateToCategory("03007") // 놀이시설 카테고리
        }
        view.findViewById<Button>(R.id.tab3_btn7).setOnClickListener {
            navigateToCategory("03008") // 실종유괴 카테고리
        }
        view.findViewById<Button>(R.id.tab3_btn8).setOnClickListener {
            navigateToCategory("03009") // 성폭력 카테고리
        }
        view.findViewById<Button>(R.id.tab3_btn9).setOnClickListener {
            navigateToCategory("03010") // 학교폭력 카테고리
        }
        view.findViewById<Button>(R.id.tab3_btn10).setOnClickListener {
            navigateToCategory("03011") // 가정폭력 카테고리
        }
        view.findViewById<Button>(R.id.tab3_btn11).setOnClickListener {
            navigateToCategory("03012") // 억류, 납치 카테고리
        }
        view.findViewById<Button>(R.id.tab3_btn12).setOnClickListener {
            navigateToCategory("03013") // 교통사고 카테고리
        }
        view.findViewById<Button>(R.id.tab3_btn13).setOnClickListener {
            navigateToCategory("03014") // 승강기 안전 카테고리
        }
        view.findViewById<Button>(R.id.tab3_btn14).setOnClickListener {
            navigateToCategory("03015") // 미세먼지 카테고리
        }
        view.findViewById<Button>(R.id.tab3_btn15).setOnClickListener {
            navigateToCategory("03016") // 소화전 카테고리
        }
        view.findViewById<Button>(R.id.tab3_btn16).setOnClickListener {
            navigateToCategory("03017") // 가정안전 카테고리
        }
        return view
    }

    private fun navigateToCategory(category: String) {
        Log.d("Tab3Fragment", "Navigating to category: $category")
        val intent = Intent(activity, LifeActivity::class.java)
        intent.putExtra("safetyCategory", category) // 카테고리 값 전달
        startActivity(intent)
    }
}