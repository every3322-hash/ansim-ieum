package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

class MainActivity : AppCompatActivity() {

    lateinit var loginbtn: Button
    lateinit var EditTextId: EditText
    lateinit var EditTextPW: EditText
    lateinit var signUpButton: Button
    lateinit var idfindBtn: Button
    lateinit var googleLoginBtn: Button
    lateinit var pwfindBtn: Button

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    lateinit var keepLoginCheckBox: CheckBox
    private val PREFS_NAME = "LoginPrefs"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        loginbtn = findViewById(R.id.loginbtn)
        EditTextId = findViewById(R.id.ID)
        EditTextPW = findViewById(R.id.PW)
        signUpButton = findViewById(R.id.signbtn)
        idfindBtn = findViewById(R.id.IDfind)
        pwfindBtn = findViewById(R.id.PWfind)
        googleLoginBtn = findViewById(R.id.google_login_button) // 구글 로그인 버튼

        firebaseAuth = FirebaseAuth.getInstance()

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        loginbtn.setOnClickListener {
            val user = EditTextId.text.toString().trim()
            val pass = EditTextPW.text.toString().trim()

            if (user.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this@MainActivity, "아이디와 비밀번호를 모두 입력해주세요.", Toast.LENGTH_SHORT).show()
            } else {
                firebaseAuth.signInWithEmailAndPassword(user, pass)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            if (keepLoginCheckBox.isChecked) {
                                val editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit()
                                editor.putString("email", user)
                                editor.putString("password", pass)
                                editor.apply()
                            }

                            Toast.makeText(this@MainActivity, "로그인 성공!", Toast.LENGTH_SHORT).show()
                            val intent = Intent(this, MapActivity::class.java)
                            startActivity(intent)
                        } else {
                            Toast.makeText(this@MainActivity, "로그인 실패: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
            }
        }

        signUpButton.setOnClickListener {
            val intent = Intent(this, SubActivity::class.java)
            startActivity(intent)
        }

        googleLoginBtn.setOnClickListener {
            val signInIntent = googleSignInClient.signInIntent
            googleLoginLauncher.launch(signInIntent)
        }

        idfindBtn.setOnClickListener {
            val intent = Intent(this, IdFindActivity::class.java)
            startActivity(intent)
        }

        pwfindBtn.setOnClickListener {
            val intent = Intent(this, PwFindActivity::class.java)
            startActivity(intent)
        }

        keepLoginCheckBox = findViewById(R.id.checkbox_keep_login)

        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val savedEmail = prefs.getString("email", null)
        val savedPassword = prefs.getString("password", null)

        if (savedEmail != null && savedPassword != null) {
            firebaseAuth.signInWithEmailAndPassword(savedEmail, savedPassword)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "자동 로그인 성공!", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, MapActivity::class.java))
                        finish()
                    }
                }
        }
    }

    private val googleLoginLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data
        val task = GoogleSignIn.getSignedInAccountFromIntent(data)
        try {
            val account = task.getResult(ApiException::class.java)!!
            firebaseAuthWithGoogle(account)
        } catch (e: ApiException) {
            Toast.makeText(this, "구글 로그인 실패: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun firebaseAuthWithGoogle(account: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        firebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "구글 로그인 성공!", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, MapActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this, "구글 로그인 실패: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }
}