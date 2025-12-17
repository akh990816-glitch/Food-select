package com.example.food_select.data.network

import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface OpenAIApi {
    // OpenAI 채팅 API 엔드포인트
    @POST("v1/chat/completions")
    suspend fun analyzeImage(
        @Header("Authorization") authHeader: String, // "Bearer sk-..." 토큰
        @Body request: AnalysisRequest               // 아까 만든 Request 모델
    ): OpenAIResponse                                // 아까 만든 Response 모델
}

