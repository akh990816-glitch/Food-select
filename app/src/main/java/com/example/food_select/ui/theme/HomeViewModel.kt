package com.example.food_select.ui.theme

import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.food_select.worker.OfflineWorker
import android.app.Application
import android.graphics.Bitmap
import android.util.Base64
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.food_select.data.local.AppDatabase
import com.example.food_select.data.local.MealEntity
import com.example.food_select.data.local.OfflineRequest // ★ 추가
import com.example.food_select.data.model.AnalysisResult
import com.example.food_select.data.model.FoodInfo
import com.example.food_select.data.repository.CloudProvider
import com.example.food_select.data.repository.MealRepository
import com.example.food_select.until.FoodClassifier
import com.example.food_select.until.NetworkHelper // ★ 추가
import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer
import com.patrykandpatrick.vico.core.entry.FloatEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// UI 상태 정의
sealed class UiState {
    object Idle : UiState()
    object Loading : UiState()
    data class Success(
        val foodName: String,
        val calories: String,
        val carbs: String,
        val protein: String,
        val fat: String
    ) : UiState()
    data class Error(val message: String) : UiState()
}

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val mealDao = AppDatabase.getDatabase(application).mealDao()
    private val repository = MealRepository()
    private val foodClassifier = FoodClassifier(application)
    private val networkHelper = NetworkHelper(application) // ★ 네트워크 감지기 추가

    // DB 데이터 구독
    val mealList = mealDao.getAllMeals()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalCalories = mealList.map { list -> list.sumOf { it.calories } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // 추천 기능 관련 상태
    private val _recommendation = MutableStateFlow<String>("")
    val recommendation: StateFlow<String> = _recommendation.asStateFlow()

    private val _isRecommendLoading = MutableStateFlow(false)
    val isRecommendLoading: StateFlow<Boolean> = _isRecommendLoading.asStateFlow()

    fun switchProvider(isGemini: Boolean) {
        repository.currentProvider = if (isGemini) CloudProvider.GEMINI else CloudProvider.OPENAI
    }

    fun resetState() {
        _uiState.value = UiState.Idle
    }

    // ★ [핵심 수정] 이미지 분석 요청 (오프라인 로직 추가)
    fun analyzeImage(bitmap: Bitmap) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading

            // 1. 공통: TFLite로 음식 여부 먼저 검사 (API 비용 절약 및 큐 오염 방지)
            val isFood = foodClassifier.isFood(bitmap)
            if (!isFood) {
                _uiState.value = UiState.Error("음식이 아닌 것 같아요! 😅")
                return@launch
            }

            val base64String = encodeBitmapToBase64(bitmap)

            // 2. 네트워크 상태 확인
            if (networkHelper.isNetworkAvailable()) {
                // ------------------------------------------------
                // [CASE A] 온라인: 바로 서버로 분석 요청 (기존 로직)
                // ------------------------------------------------
                try {
                    val result = repository.analyzeImage(base64String)
                    when (result) {
                        is AnalysisResult.Success -> {
                            val info = result.data
                            saveMealToDb(info)
                            _uiState.value = UiState.Success(
                                foodName = info.foodName,
                                calories = info.calories.toString(),
                                carbs = info.carbs.toString(),
                                protein = info.protein.toString(),
                                fat = info.fat.toString()
                            )
                        }
                        is AnalysisResult.Error -> {
                            _uiState.value = UiState.Error(result.message)
                        }
                        is AnalysisResult.Loading -> {
                            _uiState.value = UiState.Loading
                        }
                    }
                } catch (e: Exception) {
                    _uiState.value = UiState.Error("오류 발생: ${e.localizedMessage}")
                }

            } else {
                // ------------------------------------------------
                // [CASE B] 오프라인: 대기열(DB)에 저장 + WorkManager 예약
                // ------------------------------------------------
                try {
                    // 1. DB에 임시 저장
                    mealDao.insertOfflineRequest(
                        OfflineRequest(imageBase64 = base64String)
                    )

                    // 2. ★ 일꾼 예약 (핵심)
                    // "인터넷이 연결되면(CONNECTED) 실행해라"라는 조건 설정
                    val constraints = Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()

                    val uploadWorkRequest = OneTimeWorkRequestBuilder<OfflineWorker>()
                        .setConstraints(constraints)
                        .build()

                    // 예약 큐에 등록 (인터넷 연결되는 순간 실행됨)
                    WorkManager.getInstance(getApplication()).enqueue(uploadWorkRequest)

                    // 사용자에게 알림
                    _uiState.value = UiState.Error("오프라인 상태입니다. 🌐\n인터넷이 연결되면 자동으로 분석해서 알려드릴게요!")
                } catch (e: Exception) {
                    _uiState.value = UiState.Error("저장 실패: ${e.localizedMessage}")
                }
            }
        }
    }

    // 추천 받기 함수
    fun requestRecommendation() {
        viewModelScope.launch {
            if (!networkHelper.isNetworkAvailable()) {
                _recommendation.value = "인터넷 연결이 필요해요! 😅"
                return@launch
            }

            _isRecommendLoading.value = true

            val today = LocalDate.now().toString()
            val nutrients = mealDao.getTodayNutrients(today)

            val carbs = nutrients?.carb ?: 0.0
            val protein = nutrients?.prot ?: 0.0
            val fat = nutrients?.fat ?: 0.0

            val result = repository.getDietRecommendation(carbs, protein, fat)

            _recommendation.value = result
            _isRecommendLoading.value = false
        }
    }

    fun clearRecommendation() {
        _recommendation.value = ""
    }

    private suspend fun saveMealToDb(info: FoodInfo) {
        mealDao.insertMeal(
            MealEntity(
                foodName = info.foodName,
                calories = info.calories,
                protein = info.protein,
                carbs = info.carbs,
                fat = info.fat
            )
        )
    }

    // 차트 관련 변수
    private val _chartEntryModel = MutableStateFlow<ChartEntryModelProducer?>(null)
    val chartEntryModel: StateFlow<ChartEntryModelProducer?> = _chartEntryModel.asStateFlow()

    private val _bottomAxisLabels = MutableStateFlow<List<String>>(emptyList())
    val bottomAxisLabels: StateFlow<List<String>> = _bottomAxisLabels.asStateFlow()

    init {
        loadWeeklyStatistics()
    }

    private fun loadWeeklyStatistics() {
        viewModelScope.launch {
            val today = LocalDate.now()
            val last7Days = (0..6).map { today.minusDays(it.toLong()) } // 최신 -> 과거

            val formatter = DateTimeFormatter.ofPattern("MM.dd")
            _bottomAxisLabels.value = last7Days.map { it.format(formatter) }

            mealDao.getLast7DaysCalories().collect { dbData ->
                val entries = last7Days.mapIndexed { index, date ->
                    val dateString = date.toString()
                    val calorie = dbData.find { it.date == dateString }?.totalCalories ?: 0

                    FloatEntry(x = index.toFloat(), y = calorie.toFloat())
                }
                _chartEntryModel.value = ChartEntryModelProducer(entries)
            }
        }
    }

    private fun encodeBitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }

    override fun onCleared() {
        super.onCleared()
        foodClassifier.close()
    }
}