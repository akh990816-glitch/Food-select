package com.example.food_select.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ★ 수정 1: 테이블 이름을 DAO와 맞추기 위해 "meal_table"로 변경 (혹은 DAO를 "meals"로 바꿔도 됨)
@Entity(tableName = "meal_table")
data class MealEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val foodName: String,
    val calories: Int,
    val protein: Double,
    val carbs: Double,
    val fat: Double,

    // ★ 수정 2: 그래프용 날짜 컬럼 추가 ("2025-12-17" 형태)
    val date: String = SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).format(Date()),

    val timestamp: Long = System.currentTimeMillis()
) {
    fun getFormattedTime(): String {
        val sdf = SimpleDateFormat("MM월 dd일 a hh:mm", Locale.KOREA)
        return sdf.format(Date(timestamp))
    }
}