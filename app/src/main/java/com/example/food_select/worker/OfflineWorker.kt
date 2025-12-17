package com.example.food_select.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.food_select.R
import com.example.food_select.data.local.AppDatabase
import com.example.food_select.data.local.MealEntity
import com.example.food_select.data.model.AnalysisResult
import com.example.food_select.data.repository.MealRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class OfflineWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    // 1. 해야 할 일 정의 (백그라운드에서 실행됨)
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val dao = AppDatabase.getDatabase(applicationContext).mealDao()
        val repository = MealRepository() // AI 서버 통신 도구

        // 2. 대기열(Queue)에 쌓인 요청들 가져오기
        val requests = dao.getAllOfflineRequests()

        if (requests.isEmpty()) {
            return@withContext Result.success()
        }

        try {
            // 3. 하나씩 꺼내서 서버로 전송
            for (request in requests) {
                // 이미지 분석 요청 (이미지 데이터는 request.imageBase64에 있음)
                val result = repository.analyzeImage(request.imageBase64)

                if (result is AnalysisResult.Success) {
                    val info = result.data

                    // 4. 성공하면 진짜 식단 DB에 저장 (원래 찍었던 시간 유지)
                    dao.insertMeal(
                        MealEntity(
                            foodName = info.foodName,
                            calories = info.calories,
                            protein = info.protein,
                            carbs = info.carbs,
                            fat = info.fat,
                            timestamp = request.timestamp // ★ 중요: 찍었던 시간 그대로 저장
                        )
                    )

                    // 5. 대기열에서 삭제
                    dao.deleteOfflineRequest(request.id)

                    // 6. 사용자에게 "분석 완료!" 알림 보내기
                    sendNotification(info.foodName)
                }
            }
            Result.success()
        } catch (e: Exception) {
            // 실패하면 나중에 다시 시도 (WorkManager가 알아서 재시도함)
            e.printStackTrace()
            Result.retry()
        }
    }

    // 분석 완료 알림 보내기 (상단바 알림)
    private fun sendNotification(foodName: String) {
        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "offline_analysis_channel"

        // 안드로이드 8.0 이상은 채널 필요
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "오프라인 분석 알림",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.mipmap.ic_launcher_round) // 아이콘 설정 (기본 앱 아이콘)
            .setContentTitle("식단 분석 완료! 🍎")
            .setContentText("아까 찍은 '$foodName' 분석이 끝났어요.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}

