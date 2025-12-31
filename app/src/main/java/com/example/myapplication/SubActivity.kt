package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.regex.Pattern

class SubActivity : AppCompatActivity() {
    private lateinit var EditTextId: EditText
    private lateinit var EditTextPassword: EditText
    private lateinit var EditTextRePassword: EditText
    private lateinit var EditTextnickname: EditText
    private lateinit var sign_up_btn: Button

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign)

        // Firebase 인스턴스 초기화
        firebaseAuth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // 뷰 연결
        EditTextId = findViewById(R.id.email)
        EditTextPassword = findViewById(R.id.password)
        EditTextRePassword = findViewById(R.id.repassword)
        EditTextnickname = findViewById(R.id.nickname)
        sign_up_btn = findViewById(R.id.sign_up_btn)

        // 회원가입 완료 버튼
        sign_up_btn.setOnClickListener {
            val userId = EditTextId.text.toString()
            val userPassword = EditTextPassword.text.toString()
            val rePassword = EditTextRePassword.text.toString()
            val userName = EditTextnickname.text.toString()
            val passwordPattern = "^(?=.*[A-Za-z])(?=.*[0-9])(?=.*[!@#\$%^&*])[A-Za-z0-9!@#\$%^&*]{8,15}$"

            Log.d("RegisterRequest", "userId: @$userId, userPassword: @$userPassword, userName: @$userName")

            // 유효성 검사
            when {
                userId.isEmpty() || userPassword.isEmpty() || rePassword.isEmpty() || userName.isEmpty() ->
                    showToast("회원정보를 모두 입력해주세요.")
                !Pattern.matches(passwordPattern, userPassword) ->
                    showToast("비밀번호 형식이 옳지 않습니다.")
                userPassword != rePassword ->
                    showToast("비밀번호가 일치하지 않습니다.")
                else -> {
                    // Firebase를 통해 회원가입 요청
                    firebaseAuth.createUserWithEmailAndPassword(userId, userPassword)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                // Firestore에 사용자 정보 저장
                                saveUserToFirestore(userName)
                            } else {
                                // 회원가입 실패 시
                                showToast("회원가입 실패: ${task.exception?.message}")
                            }
                        }
                }
            }
        }
    }

    // Firestore에 사용자 정보 저장
    private fun saveUserToFirestore(userName: String) {
        val userId = firebaseAuth.currentUser?.uid

        if (userId != null) {
            val user = hashMapOf(
                "name" to userName,
                "email" to firebaseAuth.currentUser?.email,
                "profileImage" to "profile1" // 기본 프로필 이미지 설정
            )

            firestore.collection("users").document(userId)
                .set(user)
                .addOnSuccessListener {
                    showToast("회원가입이 완료되었습니다.")
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    finish() // 현재 액티비티 종료
                }
                .addOnFailureListener { exception ->
                    showToast("사용자 정보 저장 실패: ${exception.message}")
                }
        } else {
            showToast("회원가입에 문제가 발생했습니다.")
        }
    }

    // 간단한 Toast 메시지 출력 함수
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
