package com.example.food_select.ui.theme

import android.os.Build
import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.food_select.data.local.MealEntity

// 권한 관련 임포트
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

// -----------------------------------------------------------------
// 1. Stateful HomeScreen (로직 담당)
// -----------------------------------------------------------------
@Composable
fun HomeScreen(viewModel: HomeViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val mealList by viewModel.mealList.collectAsState()
    val totalCalories by viewModel.totalCalories.collectAsState()

    val recommendation by viewModel.recommendation.collectAsState()
    val isRecommendLoading by viewModel.isRecommendLoading.collectAsState()

    val context = LocalContext.current

    // 1. 실제 카메라를 실행하는 런처
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            viewModel.analyzeImage(bitmap)
        } else {
            Toast.makeText(context, "촬영이 취소되었습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    // 2. 다중 권한 요청 런처
    val multiplePermissionsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val isCameraGranted = permissions[Manifest.permission.CAMERA] ?: false
        if (isCameraGranted) {
            cameraLauncher.launch(null)
        } else {
            Toast.makeText(context, "카메라 권한은 필수입니다.", Toast.LENGTH_LONG).show()
        }
    }

    // 3. 권한 체크 및 요청 함수
    fun checkAndLaunchCamera() {
        val permissionsToRequest = mutableListOf(Manifest.permission.CAMERA)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val permissionsNotGranted = permissionsToRequest.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsNotGranted.isEmpty()) {
            cameraLauncher.launch(null)
        } else {
            multiplePermissionsLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    if (uiState is UiState.Success) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            val state = uiState as UiState.Success
            // ★ 수정: 영양소 정보를 전달합니다.
            FoodResultDialog(
                foodName = state.foodName,
                calories = state.calories,
                carbs = state.carbs,
                protein = state.protein,
                fat = state.fat,
                onDismiss = { viewModel.resetState() }
            )
        }
    } else {
        // [2] 메인 홈 화면
        Box(modifier = Modifier.fillMaxSize()) {
            HomeScreenContent(
                isLoading = uiState is UiState.Loading,
                onCameraClick = { checkAndLaunchCamera() },
                currentCalories = totalCalories,
                mealList = mealList,
                onSwitchChanged = { isGemini -> viewModel.switchProvider(isGemini) },
                viewModel = viewModel
            )

            // 로딩 중일 때 (이미지 분석)
            if (uiState is UiState.Loading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }

            // 로딩 중일 때 (추천 분석)
            if (isRecommendLoading) {
                Dialog(onDismissRequest = {}) {
                    Card(modifier = Modifier.padding(16.dp)) {
                        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("AI가 메뉴를 고르는 중...🤔")
                        }
                    }
                }
            }

            // 추천 결과 팝업
            if (recommendation.isNotEmpty()) {
                AlertDialog(
                    onDismissRequest = { viewModel.clearRecommendation() },
                    title = { Text("🥗 AI의 추천 메뉴") },
                    text = { Text(recommendation) },
                    confirmButton = {
                        Button(onClick = { viewModel.clearRecommendation() }) {
                            Text("확인")
                        }
                    }
                )
            }

            // 에러 났을 때 토스트
            if (uiState is UiState.Error) {
                val errorMsg = (uiState as UiState.Error).message
                LaunchedEffect(errorMsg) {
                    Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                    viewModel.resetState()
                }
            }
        }
    }
}

// -----------------------------------------------------------------
// 2. Stateless Content (화면 그리기 담당)
// -----------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreenContent(
    isLoading: Boolean,
    onCameraClick: () -> Unit,
    currentCalories: Int,
    mealList: List<MealEntity>,
    onSwitchChanged: (Boolean) -> Unit,
    viewModel: HomeViewModel
) {
    var isGeminiMode by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Calo Snap", fontWeight = FontWeight.Bold) },
                actions = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 16.dp)
                    ) {
                        Text(
                            text = if (isGeminiMode) "Gemini" else "GPT",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = isGeminiMode,
                            onCheckedChange = { isChecked ->
                                isGeminiMode = isChecked
                                onSwitchChanged(isChecked)
                            },
                            modifier = Modifier.scale(0.8f)
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            if (!isLoading) {
                FloatingActionButton(
                    onClick = onCameraClick,
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "촬영", tint = Color.White)
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // 1. 칼로리 요약
            CalorieSummaryCard(current = currentCalories, total = 2000)

            Spacer(modifier = Modifier.height(24.dp))

            // 2. 주간 통계 차트
            WeeklyChart(viewModel = viewModel)

            Spacer(modifier = Modifier.height(24.dp))

            // 3. 추천 버튼
            Button(
                onClick = { viewModel.requestRecommendation() },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
            ) {
                Text("🤖 AI에게 저녁 메뉴 추천받기", fontSize = 18.sp)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 4. 식단 리스트
            TodayMealList(meals = mealList)
        }
    }
}

// -----------------------------------------------------------------
// 3. 결과 팝업 다이얼로그 (수정됨: 영양소 표시 추가)
// -----------------------------------------------------------------
@Composable
fun FoodResultDialog(
    foodName: String,
    calories: String,
    carbs: String,    // ★ 추가
    protein: String,  // ★ 추가
    fat: String,      // ★ 추가
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("🎉 분석 완료!", style = MaterialTheme.typography.titleMedium, color = Color.Gray)
                Spacer(modifier = Modifier.height(8.dp))

                // 음식 이름
                Text(
                    text = foodName,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )

                // 칼로리 강조
                Text(
                    text = "$calories kcal",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(24.dp))

                // ★ 영양성분 표시 (박스 형태로 예쁘게)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    NutritionBox(label = "탄수화물", value = "${carbs}g", color = Color(0xFFE57373)) // 붉은색
                    NutritionBox(label = "단백질", value = "${protein}g", color = Color(0xFF64B5F6)) // 파란색
                    NutritionBox(label = "지방", value = "${fat}g", color = Color(0xFFFFD54F))   // 노란색
                }

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("기록하기", fontSize = 16.sp, modifier = Modifier.padding(4.dp))
                }
            }
        }
    }
}

// -----------------------------------------------------------------
// 4. 하위 컴포넌트들
// -----------------------------------------------------------------

// ★ 새로 추가된 영양소 박스 컴포넌트
@Composable
fun NutritionBox(label: String, value: String, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(80.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.1f)) // 연한 배경
            .padding(vertical = 12.dp)
    ) {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = Color.Gray)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
fun CalorieSummaryCard(current: Int, total: Int) {
    val progress = if (total > 0) (current.toFloat() / total.toFloat()).coerceIn(0f, 1f) else 0f

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("오늘 섭취 칼로리", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "$current / $total kcal",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(8.dp)
            )
        }
    }
}

@Composable
fun TodayMealList(meals: List<MealEntity>) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("최근 기록", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        if (meals.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                Text("아직 기록된 식단이 없습니다.", color = Color.Gray)
            }
        } else {
            meals.sortedByDescending { it.id }.forEach { meal ->
                MealItemRow(meal)
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }
        }
    }
}

@Composable
fun MealItemRow(meal: MealEntity) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(meal.foodName, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(meal.getFormattedTime(), color = Color.Gray, fontSize = 12.sp)
        }
        Text(
            "${meal.calories} kcal",
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
    }
}