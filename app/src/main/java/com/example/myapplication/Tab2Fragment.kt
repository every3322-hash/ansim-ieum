package com.example.myapplication

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment

class Tab2Fragment : Fragment(){

    companion object {
        fun newInstance() : Tab2Fragment = Tab2Fragment()
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
        val view = inflater.inflate(R.layout.fragment_tab2, container, false)

        // 버튼 클릭 이벤트로 해당 카테고리로 이동
        view.findViewById<Button>(R.id.tab2_btn1).setOnClickListener {
            navigateToCategory("02001") // 해양오염 카테고리
        }
        view.findViewById<Button>(R.id.tab2_btn2).setOnClickListener {
            navigateToCategory("02002") // 수질오염 카테고리
        }
        view.findViewById<Button>(R.id.tab2_btn3).setOnClickListener {
            navigateToCategory("02003") // 식용수 카테고리
        }
        view.findViewById<Button>(R.id.tab2_btn4).setOnClickListener {
            navigateToCategory("02004") // 공동구 재난 카테고리
        }
        view.findViewById<Button>(R.id.tab2_btn5).setOnClickListener {
            navigateToCategory("02005") // 가축질병 카테고리
        }
        view.findViewById<Button>(R.id.tab2_btn6).setOnClickListener {
            navigateToCategory("02006") // 감염병 예방 카테고리
        }
        view.findViewById<Button>(R.id.tab2_btn7).setOnClickListener {
            navigateToCategory("02007") // 철도사고 카테고리
        }
        view.findViewById<Button>(R.id.tab2_btn8).setOnClickListener {
            navigateToCategory("02008") // 금융전산 카테고리
        }
        view.findViewById<Button>(R.id.tab2_btn9).setOnClickListener {
            navigateToCategory("02009") // 원전사고 카테고리
        }
        view.findViewById<Button>(R.id.tab2_btn10).setOnClickListener {
            navigateToCategory("02010") // 화학물질 카테고리
        }
        view.findViewById<Button>(R.id.tab2_btn11).setOnClickListener {
            navigateToCategory("02011") // 화재 카테고리
        }
        view.findViewById<Button>(R.id.tab2_btn12).setOnClickListener {
            navigateToCategory("02012") // 산불 카테고리
        }
        view.findViewById<Button>(R.id.tab2_btn13).setOnClickListener {
            navigateToCategory("02013") // 건축물 붕괴 카테고리
        }
        view.findViewById<Button>(R.id.tab2_btn14).setOnClickListener {
            navigateToCategory("02014") // 댐 붕괴 카테고리
        }
        view.findViewById<Button>(R.id.tab2_btn15).setOnClickListener {
            navigateToCategory("02015") // 폭발 카테고리
        }
        view.findViewById<Button>(R.id.tab2_btn16).setOnClickListener {
            navigateToCategory("02016") // 항공기 사고 카테고리
        }
        view.findViewById<Button>(R.id.tab2_btn17).setOnClickListener {
            navigateToCategory("02017") // 화생방 사고 카테고리
        }
        view.findViewById<Button>(R.id.tab2_btn18).setOnClickListener {
            navigateToCategory("02018") // 정전 카테고리
        }
        view.findViewById<Button>(R.id.tab2_btn19).setOnClickListener {
            navigateToCategory("02019") // 전기, 가스 카테고리
        }
        view.findViewById<Button>(R.id.tab2_btn20).setOnClickListener {
            navigateToCategory("02020") // 유도선 사고 카테고리
        }
        view.findViewById<Button>(R.id.tab2_btn21).setOnClickListener {
            navigateToCategory("02021") // 철도사고 카테고리
        }
        view.findViewById<Button>(R.id.tab2_btn22).setOnClickListener {
            navigateToCategory("02022") // 금융전산 카테고리
        }
        view.findViewById<Button>(R.id.tab2_btn23).setOnClickListener {
            navigateToCategory("02023") // 원전사고 카테고리
        }
        return view
    }

    private fun navigateToCategory(category: String) {
        val intent = Intent(requireActivity(), SocialActivity::class.java)
        intent.putExtra("safetyCategory", category) // 카테고리 값 전달
        startActivity(intent)
    }
}