package com.example.food_select.data.network // 패키지명 확인!

import com.google.gson.annotations.SerializedName

// 1. 메인 요청 객체
data class AnalysisRequest(
    val model: String = "gpt-4o",
    val messages: List<Message>,
    val max_tokens: Int = 500
)

// 2. 메시지 객체
data class Message(
    val role: String = "user",
    val content: List<Content>
)

// 3. 컨텐츠 (텍스트 + 이미지)
data class Content(
    val type: String, // "text" 또는 "image_url"

    val text: String? = null,

    // ★ 중요: OpenAI는 JSON 키로 "image_url"을 원함
    // Service 코드에서 imageUrl 변수를 쓰고 있으니 매핑이 필요
    @SerializedName("image_url")
    val imageUrl: ImageUrl? = null
)

// 4. 이미지 URL 객체
data class ImageUrl(
    // ★ 중요: OpenAI는 JSON 키로 "url"을 원함
    @SerializedName("url")
    val url: String
)

// 5. 응답 객체 (Response)
data class OpenAIResponse(
    val choices: List<Choice>
)

data class Choice(
    val message: MessageContent
)

data class MessageContent(
    val content: String // 여기에 JSON 결과가 담겨옴
)