
import android.content.Context
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth

class LoginRequest(private val context: Context) {

    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()

    fun login(userID: String, userPassword: String) {
        firebaseAuth.signInWithEmailAndPassword(userID, userPassword)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // 로그인 성공
                    Toast.makeText(context, "로그인 성공", Toast.LENGTH_SHORT).show()
                } else {
                    // 로그인 실패
                    Toast.makeText(context, "로그인 실패: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }
}