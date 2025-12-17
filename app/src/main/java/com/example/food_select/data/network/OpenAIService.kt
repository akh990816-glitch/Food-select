package com.example.food_select.data.network

import com.example.food_select.BuildConfig
import com.example.food_select.data.model.AnalysisResult
import com.example.food_select.data.model.FoodInfo
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class OpenAIService : AnalysisService {

    // OpenAI 전용 Retrofit 객체 생성 (기존 RetrofitInstance와 BaseURL이 다르므로 별도 생성 추천)
    private val api: OpenAIApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.openai.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OpenAIApi::class.java)
    }

    private val apiKey = BuildConfig.OPENAI_API_KEY // local.properties에 키 저장 필수!

    override suspend fun analyze(base64Image: String): AnalysisResult<FoodInfo> {
        return withContext(Dispatchers.IO) {
            try {
                // 1. 요청 데이터(Payload) 만들기
                // 이미지 url은 "data:image/jpeg;base64,{BASE64_STRING}" 형식이어야 함
                val imageUrl = "data:image/jpeg;base64,$base64Image"

                val request = AnalysisRequest(
                    model = "gpt-4o", // 혹은 "gpt-4-turbo"
                    messages = listOf(
                        Message(
                            role = "user",
                            content = listOf(
                                Content(type = "text", text = "이 음식 사진을 분석해서 다음 JSON 형식으로만 답해줘. 마크다운 기호 없이 순수 JSON만 줘.\n" +
                                        "{\n" +
                                        "  \"food_name\": \"음식이름(한국어)\",\n" +
                                        "  \"calories\": 000,\n" +
                                        "  \"carbs\": 00.0,\n" +
                                        "  \"protein\": 00.0,\n" +
                                        "  \"fat\": 00.0\n" +
                                        "}"),
                                Content(type = "image_url", imageUrl = ImageUrl(imageUrl))
                            )
                        )
                    ),
                    max_tokens = 500
                )

                // 2. API 호출
                val response = api.analyzeImage("Bearer $apiKey", request)

                // 3. 응답 텍스트 추출
                // OpenAI는 choices[0].message.content에 답이 들어있음
                var jsonText = response.choices.firstOrNull()?.message?.content ?: ""

                // 4. 마크다운(```json) 제거 (Gemini 때와 동일한 방어 로직)
                if (jsonText.contains("```")) {
                    jsonText = jsonText.replace("```json", "").replace("```", "").trim()
                }

                // 5. JSON -> FoodInfo 변환
                val gson = Gson()
                val foodInfo = gson.fromJson(jsonText, FoodInfo::class.java)

                AnalysisResult.Success(foodInfo)

            } catch (e: Exception) {
                e.printStackTrace()
                AnalysisResult.Error("OpenAI 분석 실패: ${e.localizedMessage}")
            }
        }
    }
}