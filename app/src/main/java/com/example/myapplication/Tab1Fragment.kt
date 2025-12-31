package com.example.myapplication

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment

class Tab1Fragment : Fragment(){

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
        val view = inflater.inflate(R.layout.fragment_tab1, container, false)

        // 버튼 클릭 이벤트로 해당 카테고리로 이동
        view.findViewById<Button>(R.id.tab1_btn1).setOnClickListener {
            navigateToCategory("01001") // 태풍 카테고리
        }
        view.findViewById<Button>(R.id.tab1_btn2).setOnClickListener {
            navigateToCategory("01002") // 홍수 카테고리
        }
        view.findViewById<Button>(R.id.tab1_btn3).setOnClickListener {
            navigateToCategory("01003") // 호우 카테고리
        }
        view.findViewById<Button>(R.id.tab1_btn4).setOnClickListener {
            navigateToCategory("01004") // 강풍 카테고리
        }
        view.findViewById<Button>(R.id.tab1_btn5).setOnClickListener {
            navigateToCategory("01005") // 대설 카테고리
        }
        view.findViewById<Button>(R.id.tab1_btn6).setOnClickListener {
            navigateToCategory("01006") // 한파 카테고리
        }
        view.findViewById<Button>(R.id.tab1_btn7).setOnClickListener {
            navigateToCategory("01007") // 풍랑 카테고리
        }
        view.findViewById<Button>(R.id.tab1_btn8).setOnClickListener {
            navigateToCategory("01008") // 황사 카테고리
        }
        view.findViewById<Button>(R.id.tab1_btn9).setOnClickListener {
            navigateToCategory("01009") // 폭염 카테고리
        }
        view.findViewById<Button>(R.id.tab1_btn10).setOnClickListener {
            navigateToCategory("01010") // 가뭄 카테고리
        }
        view.findViewById<Button>(R.id.tab1_btn11).setOnClickListener {
            navigateToCategory("01011") // 지진 카테고리
        }
        view.findViewById<Button>(R.id.tab1_btn12).setOnClickListener {
            navigateToCategory("01013") // 해일 카테고리
        }
        view.findViewById<Button>(R.id.tab1_btn13).setOnClickListener {
            navigateToCategory("01014") // 산사태 카테고리
        }
        view.findViewById<Button>(R.id.tab1_btn14).setOnClickListener {
            navigateToCategory("01015") // 화산폭발 카테고리
        }
        return view
    }

    private fun navigateToCategory(category: String) {
        val intent = Intent(activity, NaturalActivity::class.java)
        intent.putExtra("safetyCategory", category) // 카테고리 값 전달
        startActivity(intent)
    }
}

