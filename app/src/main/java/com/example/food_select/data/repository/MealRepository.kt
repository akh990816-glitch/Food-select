package com.example.food_select.data.repository

// ★ [중요] AnalysisResult와 FoodInfo의 정확한 위치(패키지)를 import 해야 합니다.
import com.example.food_select.data.model.AnalysisResult
import com.example.food_select.data.model.FoodInfo
import com.example.food_select.data.network.GeminiService
import com.example.food_select.data.network.OpenAIService

// 클라우드 종류 선택지
enum class CloudProvider {
    OPENAI, GEMINI
}

class MealRepository {
    // 각각의 서비스 생성
    private val openAIService = OpenAIService()
    private val geminiService = GeminiService()

    // 현재 선택된 모드 (기본값: OpenAI)
    // 팁: 나중에 설정 화면에서 이 값을 바꾸면 엔진이 즉시 교체됩니다!
    var currentProvider = CloudProvider.OPENAI


    suspend fun getDietRecommendation(currentCarbs: Double, currentProtein: Double, currentFat: Double): String {
        // AI에게 보낼 질문 작성
        val prompt = """
            나 오늘 탄수화물 ${currentCarbs.toInt()}g, 단백질 ${currentProtein.toInt()}g, 지방 ${currentFat.toInt()}g 먹었어.
            영양학적으로 볼 때 저녁 메뉴로 뭘 먹으면 좋을지 3가지만 추천해줘.
            메뉴 이름과 추천 이유를 간단히 한국어로 설명해줘.
            (너무 길지 않게 3줄 이내로 요약해서)
        """.trimIndent()

        
        return if (currentProvider == CloudProvider.GEMINI) {
            geminiService.getRecommendation(prompt)
        } else {
            // OpenAI도 텍스트 전용 함수를 만들어야 하지만, 일단 Gemini로 통일
            "현재 추천 기능은 Gemini 모드에서만 가능합니다."
        }
    }

    // ★ [수정] 반환 타입에 <FoodInfo>를 꼭 붙여주세요!
    suspend fun analyzeImage(base64Image: String): AnalysisResult<FoodInfo> {
        // 선택된 모드에 따라 일을 시킴
        return when (currentProvider) {
            // OpenAIService의 analyze 함수도 <FoodInfo>를 반환하도록 되어 있어야 합니다.
            CloudProvider.OPENAI -> openAIService.analyze(base64Image)
            CloudProvider.GEMINI -> geminiService.analyze(base64Image)
        }
    }
}