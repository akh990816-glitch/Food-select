package com.example.food_select.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// ★ 수정 1: OfflineRequest 엔티티 추가 & 버전 2로 변경
@Database(entities = [MealEntity::class, OfflineRequest::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun mealDao(): MealDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "calosnap_database" // ★ 기존 DB 이름 유지
                )
                    // ★ 수정 2: 버전 변경 시 기존 데이터 삭제 후 재생성 (개발 단계에서 필수)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}