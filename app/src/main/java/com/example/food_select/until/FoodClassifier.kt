package com.example.food_select.until

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter // ★ 표준 인터프리터 사용
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer


class FoodClassifier(context: Context) {

    // assets 폴더에 넣은 모델 파일명 (정확해야 함!)
    private val modelName = "food_classifier.tflite"

    // TFLite 모델 실행기
    private var interpreter: Interpreter? = null

    init {
        try {
            // 1. 모델 파일 불러오기 (FileUtil 사용)
            val modelFile = FileUtil.loadMappedFile(context, modelName)

            // 2. 인터프리터 생성 (옵션 설정 가능)
            val options = Interpreter.Options()
            interpreter = Interpreter(modelFile, options)

            Log.d("FoodClassifier", "TFLite 모델 로딩 성공!")
        } catch (e: Exception) {
            Log.e("FoodClassifier", "TFLite 모델 로딩 실패", e)
        }
    }

    // 3. 이미지 전처리기 설정
    // MobileNetV2 학습 조건: 224x224 크기, 0~1 사이 값으로 정규화
    private val imageProcessor = ImageProcessor.Builder()
        .add(ResizeOp(224, 224, ResizeOp.ResizeMethod.BILINEAR))
        .add(NormalizeOp(0f, 255f)) // 픽셀값(0~255) -> (0.0~1.0) 변환
        .build()

    fun isFood(bitmap: Bitmap): Boolean {
        if (interpreter == null) {
            Log.e("FoodClassifier", "인터프리터가 초기화되지 않음")
            return false
        }

        // [입력] 1. Bitmap을 TensorImage로 변환
        var tensorImage = TensorImage(DataType.FLOAT32)
        tensorImage.load(bitmap)

        // [입력] 2. 전처리 수행 (크기조절 + 정규화)
        tensorImage = imageProcessor.process(tensorImage)

        // [출력] 3. 결과를 담을 버퍼 생성 ([1, 1] 크기: 배치크기 1, 출력값 1개)
        val outputBuffer = TensorBuffer.createFixedSize(intArrayOf(1, 1), DataType.FLOAT32)

        // [실행] 4. 추론 (Inference)
        // input: tensorImage.buffer, output: outputBuffer.buffer
        interpreter?.run(tensorImage.buffer, outputBuffer.buffer)

        // [해석] 5. 결과값 확인
        // 학습 시: 0 = Food, 1 = Non_Food
        // 결과값(probability)은 '1(Non_Food)'일 확률
        val probability = outputBuffer.floatArray[0]

        Log.d("FoodClassifier", "비음식일 확률: $probability")

        // 0.5보다 작으면 '비음식이 아님' => 즉, '음식'
        return probability < 0.5f
    }

    // 메모리 해제 (필수)
    fun close() {
        interpreter?.close()
        interpreter = null
    }
}