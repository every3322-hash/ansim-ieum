package com.example.myapplication

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment

class Tab4Fragment : Fragment(){
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
        val view = inflater.inflate(R.layout.fragment_tab4, container, false)

        // 버튼 클릭 이벤트로 해당 카테고리로 이동
        view.findViewById<Button>(R.id.tab4_btn1).setOnClickListener {
            navigateToCategory("04001") // 비상사태 카테고리
        }
        view.findViewById<Button>(R.id.tab4_btn2).setOnClickListener {
            navigateToCategory("04002") // 민방공 카테고리
        }
        view.findViewById<Button>(R.id.tab4_btn3).setOnClickListener {
            navigateToCategory("04003") // 화생방 무기 카테고리
        }
        return view
    }

    private fun navigateToCategory(category: String) {
        val intent = Intent(activity, CivilActivity::class.java)
        intent.putExtra("safetyCategory", category) // 카테고리 값 전달
        startActivity(intent)
    }
}