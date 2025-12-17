package com.example.food_select.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "offline_requests")
data class OfflineRequest(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val imageBase64: String, // 분석할 이미지 데이터
    val timestamp: Long = System.currentTimeMillis() // 요청 시각
)

