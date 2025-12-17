package com.example.food_select.data.model

// ★★★ [중요] <out T>가 반드시 있어야 합니다! ★★★
sealed class AnalysisResult<out T> {

    // 로딩 중
    object Loading : AnalysisResult<Nothing>()

    // 성공 (데이터 T를 가짐)
    data class Success<T>(val data: T) : AnalysisResult<T>()

    // 실패 (에러 메시지 가짐)
    data class Error(val message: String) : AnalysisResult<Nothing>()
}