package com.example.food_select.ui.theme

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.food_select.data.model.FoodInfo

@Composable
fun AnalysisResultScreen(
    viewModel: HomeViewModel,
    onRetry: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when (val state = uiState) {
            is UiState.Loading -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("AI가 맛있는 음식을 분석 중입니다... 🔍")
                }
            }
            is UiState.Success -> {
                // ★ [수정 핵심] 0.0 대신 state에 있는 진짜 데이터를 넣습니다!
                // 문자열(String)로 온 데이터를 숫자(Double)로 변환합니다.
                val realInfo = FoodInfo(
                    foodName = state.foodName,
                    calories = state.calories.toIntOrNull() ?: 0,
                    carbs = state.carbs.toDoubleOrNull() ?: 0.0,     // 진짜 탄수화물
                    protein = state.protein.toDoubleOrNull() ?: 0.0, // 진짜 단백질
                    fat = state.fat.toDoubleOrNull() ?: 0.0          // 진짜 지방
                )
                FoodResultCard(foodInfo = realInfo, onRetry = onRetry)
            }
            is UiState.Error -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("오류 발생 😢", color = Color.Red, fontSize = 18.sp)
                    Text(state.message)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onRetry) {
                        Text("다시 시도")
                    }
                }
            }
            else -> {
                // Idle 상태
            }
        }
    }
}

@Composable
fun FoodResultCard(foodInfo: FoodInfo, onRetry: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        elevation = CardDefaults.cardElevation(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("🍱 분석 결과", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "음식명: ${foodInfo.foodName}",
                fontSize = 24.sp,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(modifier = Modifier.height(8.dp))

            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            // ★ 여기는 건드릴 필요 없이 데이터가 들어오면 자동으로 숫자가 바뀝니다.
            NutritionRow("칼로리", "${foodInfo.calories} kcal")
            NutritionRow("탄수화물", "${foodInfo.carbs} g")
            NutritionRow("단백질", "${foodInfo.protein} g")
            NutritionRow("지방", "${foodInfo.fat} g")

            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onRetry) {
                Text("확인")
            }
        }
    }
}

@Composable
fun NutritionRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color.Gray)
        Text(value, fontWeight = FontWeight.Bold)
    }
}