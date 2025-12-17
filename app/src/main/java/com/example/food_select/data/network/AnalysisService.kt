package com.example.food_select.data.network

import com.example.food_select.data.model.AnalysisResult
import com.example.food_select.data.model.FoodInfo

interface AnalysisService {
    // 어떤 AI든 "이미지(Base64)를 주면 -> 결과(FoodInfo)를 내놔라" 라는 약속
    suspend fun analyze(base64Image: String): AnalysisResult<FoodInfo>
}