package com.example.myapplication

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.myapplication.databinding.FragmentCallBinding

class CallFragment : Fragment() {
    private lateinit var binding: FragmentCallBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentCallBinding.inflate(inflater, container, false)

        //각 긴급전화 버튼 클릭시 해당 번호로 전화걸기
        binding.Btn112.setOnClickListener{
            val intentDial = Intent(Intent.ACTION_DIAL, Uri.parse("tel:112"))
            startActivity(intentDial)
        }
        binding.Btn119.setOnClickListener{
            val intentDial = Intent(Intent.ACTION_DIAL, Uri.parse("tel:119"))
            startActivity(intentDial)
        }
        binding.Btn110.setOnClickListener{
            val intentDial = Intent(Intent.ACTION_DIAL, Uri.parse("tel:110"))
            startActivity(intentDial)
        }
        //119로 문자 보내기
        binding.Btn119m.setOnClickListener{
            val smsUri = Uri.parse("smsto:119")
            val intent = Intent(Intent.ACTION_SENDTO)
            intent.data = smsUri
            intent.putExtra("sms_body","")
            startActivity(intent)
        }
        //앱 신고 버튼 클릭시 구글스토어로 이동해서 설치
        binding.Btnapp1.setOnClickListener{
            val storeUri = Uri.parse("market://details?id=com.winitech.mm119t&hl=ko")
            val sIntent = Intent(Intent.ACTION_VIEW, storeUri)
            startActivity(sIntent)
        }
        binding.Btnapp2.setOnClickListener{
            val storeUri = Uri.parse("market://details?id=kr.go.safepeople&hl=ko")
            val sIntent = Intent(Intent.ACTION_VIEW, storeUri)
            startActivity(sIntent)
        }
        return binding.root
    }
}