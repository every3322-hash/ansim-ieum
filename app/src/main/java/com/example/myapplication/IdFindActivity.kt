package com.example.myapplication

import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class IdFindActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_idfind)

        val backButton = findViewById<ImageView>(R.id.btnBack)

        backButton.setOnClickListener{
            finish()
        }
    }
}
