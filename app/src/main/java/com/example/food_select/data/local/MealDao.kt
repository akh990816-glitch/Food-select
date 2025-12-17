package com.example.food_select.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MealDao {
    // 1. 식단 추가 (저장)
    @Insert
    suspend fun insertMeal(meal: MealEntity)

    // 2. 모든 식단 가져오기 (최신순)
    // ★ 수정: 테이블 이름을 'meal_table'로 통일하고 중복 제거함
    @Query("SELECT * FROM meal_table ORDER BY timestamp DESC")
    fun getAllMeals(): Flow<List<MealEntity>>

    // 3. 오늘 섭취한 총 칼로리 계산
    // ★ 수정: 여기도 'meals' -> 'meal_table'로 변경
    @Query("SELECT SUM(calories) FROM meal_table")
    fun getTotalCalories(): Flow<Int?>

    // 4. 최근 7일 통계 가져오기 (그래프용)
    @Query("SELECT date, SUM(calories) as totalCalories FROM meal_table GROUP BY date ORDER BY date DESC LIMIT 7")
    fun getLast7DaysCalories(): Flow<List<DailyCalorieTuple>>

    // ★ [추가] 오늘 섭취한 영양소 합계 (추천 로직용)
    // substr(date, 1, 10)은 YYYY-MM-DD 형식의 날짜 비교를 위함
    @Query("SELECT SUM(calories) as cal, SUM(carbs) as carb, SUM(protein) as prot, SUM(fat) as fat FROM meal_table WHERE date = :todayDate")
    suspend fun getTodayNutrients(todayDate: String): NutrientTuple?

    @Insert
    suspend fun insertOfflineRequest(request: OfflineRequest)

    @Query("SELECT * FROM offline_requests ORDER BY timestamp ASC")
    suspend fun getAllOfflineRequests(): List<OfflineRequest>

    @Query("DELETE FROM offline_requests WHERE id = :id")
    suspend fun deleteOfflineRequest(id: Int)

}

data class NutrientTuple(
    val cal: Int?,
    val carb: Double?,
    val prot: Double?,
    val fat: Double?
)

// 쿼리 결과 담는 그릇
data class DailyCalorieTuple(
    val date: String,
    val totalCalories: Int
)