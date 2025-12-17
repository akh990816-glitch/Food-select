package com.example.food_select

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.food_select.ui.theme.FoodSelectTheme
import com.example.food_select.ui.theme.HomeScreen
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 1. Firebase 익명 로그인 실행 (앱 켜자마자)
        signInAnonymously()

        // 2. UI 설정
        setContent {
            FoodSelectTheme {
                // 우리가 만든 홈 화면 표시
                HomeScreen()
            }
        }
    }

    // Firebase 익명 로그인 함수
    private fun signInAnonymously() {
        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser == null) {
            auth.signInAnonymously()
                .addOnSuccessListener {
                    Log.d("Auth", "로그인 성공! UID: ${it.user?.uid}")
                }
                .addOnFailureListener {
                    Log.e("Auth", "로그인 실패", it)
                }
        } else {
            Log.d("Auth", "이미 로그인 되어있음. UID: ${auth.currentUser?.uid}")
        }
    }
}