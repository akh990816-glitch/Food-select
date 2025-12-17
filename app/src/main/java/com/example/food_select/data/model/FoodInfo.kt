package com.example.food_select.data.model

import com.google.gson.annotations.SerializedName

data class FoodInfo(
    // JSON 키값("food_name")을 Kotlin 변수명(foodName)으로 매핑
    @SerializedName("food_name")
    val foodName: String = "알 수 없음",

    @SerializedName("calories")
    val calories: Int = 0,

    // 영양성분은 소수점일 수 있으므로 Double 추천
    @SerializedName("carbs")
    val carbs: Double = 0.0,   // 탄수화물

    @SerializedName("protein")
    val protein: Double = 0.0, // 단백질

    @SerializedName("fat")
    val fat: Double = 0.0      // 지방
)