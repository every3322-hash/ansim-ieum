package com.example.myapplication

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.databinding.ActivityChangePwBinding
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth

class ChangePasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChangePwBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChangePwBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        binding.savePasswordButton.setOnClickListener {
            val currentPassword = binding.editCurrentPassword.text.toString().trim()
            val newPassword = binding.editNewPassword.text.toString().trim()

            if (validateInputs(currentPassword, newPassword)) {
                reauthenticateAndChangePassword(currentPassword, newPassword)
            }
        }
        binding.btnBack.setOnClickListener {
            finish()
        }
    }

    private fun validateInputs(currentPassword: String, newPassword: String): Boolean {
        return when {
            currentPassword.isEmpty() -> {
                Toast.makeText(this, "현재 비밀번호를 입력하세요.", Toast.LENGTH_SHORT).show()
                false
            }
            newPassword.isEmpty() || newPassword.length < 6 -> {
                Toast.makeText(this, "새 비밀번호는 6자리 이상이어야 합니다.", Toast.LENGTH_SHORT).show()
                false
            }
            else -> true
        }
    }

    private fun reauthenticateAndChangePassword(currentPassword: String, newPassword: String) {
        val user = auth.currentUser ?: return
        val credential = EmailAuthProvider.getCredential(user.email!!, currentPassword)

        user.reauthenticate(credential)
            .addOnSuccessListener {
                user.updatePassword(newPassword)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this, "비밀번호가 변경되었습니다.", Toast.LENGTH_SHORT).show()
                            finish()
                        } else {
                            Toast.makeText(this, "비밀번호 변경에 실패했습니다.", Toast.LENGTH_SHORT).show()
                        }
                    }
            }
            .addOnFailureListener {
                Toast.makeText(this, " 비밀번호가 올바르지 않습니다.", Toast.LENGTH_SHORT).show()
            }
    }
}
