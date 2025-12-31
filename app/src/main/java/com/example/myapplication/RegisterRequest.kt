
import android.content.Context
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth

class RegisterRequest(private val context: Context) {

    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()

    fun register(userID: String, userPassword: String) {
        firebaseAuth.createUserWithEmailAndPassword(userID, userPassword)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // 회원가입 성공
                    Toast.makeText(context, "회원가입 성공", Toast.LENGTH_SHORT).show()
                } else {
                    // 회원가입 실패
                    Toast.makeText(context, "회원가입 실패: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }
}