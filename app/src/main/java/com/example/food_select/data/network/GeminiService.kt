package com.example.food_select.data.network
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import com.example.food_select.BuildConfig
import com.example.food_select.data.model.AnalysisResult
import com.example.food_select.data.model.FoodInfo
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext



class GeminiService : AnalysisService {

    private val apiKey = BuildConfig.GEMINI_API_KEY

    // ★ 2.0-flash-exp 모델 선택 아주 좋습니다!
    private val generativeModel = GenerativeModel(
        modelName = "gemini-2.0-flash-exp",
        apiKey = apiKey
    )

    override suspend fun analyze(base64Image: String): AnalysisResult<FoodInfo>{
        // 네트워크와 이미지 처리는 IO 스레드에서 실행 (앱 버벅임 방지)
        return withContext(Dispatchers.IO) {
            try {
                // 1. 이미지 변환 (만들어두신 함수 활용)
                val bitmap = decodeBase64ToBitmap(base64Image)

                // 이미지가 깨졌을 경우 방어 코드
                if (bitmap == null) {
                    return@withContext AnalysisResult.Error("이미지 형식이 잘못되어 분석할 수 없습니다.")
                }

                // 2. 프롬프트 준비
                val prompt = """
                    이 음식 사진을 분석해서 다음 JSON 형식으로만 답해줘. 마크다운 기호 없이 순수 JSON만 줘.
                    {
                      "food_name": "음식이름(한국어)",
                      "calories": 000,
                      "carbs": 00,
                      "protein": 00,
                      "fat": 00
                    }
                """.trimIndent()

                // 3. Gemini에게 요청 (이미지 + 텍스트)
                val inputContent = content {
                    image(bitmap)
                    text(prompt)
                }

                val response = generativeModel.generateContent(inputContent)
                var jsonText = response.text ?: ""

                // 4. 마크다운(```json) 껍질 벗기기 (아주 잘하셨습니다!)
                if (jsonText.contains("```")) {
                    jsonText = jsonText.replace("```json", "").replace("```", "").trim()
                }

                // 5. JSON -> Kotlin 객체 변환
                val gson = Gson()
                val foodInfo = gson.fromJson(jsonText, FoodInfo::class.java)

                // 성공!
                AnalysisResult.Success(foodInfo)

            } catch (e: Exception) {
                // 실패 시 에러 반환
                e.printStackTrace() // 로그캣에서 에러 확인용
                AnalysisResult.Error("분석 실패: ${e.localizedMessage}")
            }
        }
    }

    suspend fun getRecommendation(prompt: String): String {
        return withContext(Dispatchers.IO) {
            try {
                val response = generativeModel.generateContent(prompt)
                response.text?.trim() ?: "죄송해요, 추천을 가져오지 못했어요."
            } catch (e: Exception) {
                e.printStackTrace()
                "추천 서비스 연결 실패: ${e.localizedMessage}"
            }
        }
    }
}

    // Base64 -> Bitmap 변환 헬퍼 함수 (이제 사용됩니다!)
    private fun decodeBase64ToBitmap(base64Str: String): Bitmap? {
        return try {
            val decodedBytes = Base64.decode(base64Str, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
