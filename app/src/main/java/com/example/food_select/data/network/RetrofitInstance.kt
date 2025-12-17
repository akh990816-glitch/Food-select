package com.example.food_select.data.network

import okhttp3.logging.HttpLoggingInterceptor
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

// 1. 토큰을 헤더에 넣어주는 '낚아채기(Interceptor)' 클래스
class AuthInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val user = FirebaseAuth.getInstance().currentUser

        // 사용자가 로그인 안 되어 있으면 그냥 보냄 (어차피 401 에러 날 것임)
        if (user == null) {
            return chain.proceed(chain.request())
        }

        // 사용자의 최신 토큰을 가져옴 (동기 처리)
        // 주의: 네트워크 스레드에서 실행되므로 runBlocking을 써도 안전함
        val token = runBlocking {
            try {
                // forceRefresh = false (기존 토큰 재사용)
                val result = user.getIdToken(false).await()
                result.token
            } catch (e: Exception) {
                // e를 사용해서 에러 내용을 로그창에 남김
                android.util.Log.e("AuthInterceptor", "토큰 갱신 실패", e)
                null
            }
        }

        // 요청을 복제해서 'Authorization' 헤더 추가
        val newRequest = chain.request().newBuilder()
            .addHeader("X-Auth-Token", token ?: "")
            .build()

        return chain.proceed(newRequest)
    }
}

// 2. Retrofit 객체 생성
object RetrofitInstance {
    private const val BASE_URL =
        "https://analyzeimageandgetcalories-rss5guc7sq-uc.a.run.app/" // 본인의 Function URL

    private val client = OkHttpClient.Builder()
        .addInterceptor(AuthInterceptor()) //
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY // 통신 내용을 전부 다 보여줘!
        })
        .connectTimeout(120, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .build()
}