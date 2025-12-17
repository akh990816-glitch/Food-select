#  Calo Snap (AI Food Analysis & Diet Tracker)

> **"사진 한 장으로 끝내는 스마트 영양 관리 비서"**
>
> Calo Snap은 Google Gemini와 OpenAI GPT-4o를 활용한 하이브리드 AI 분석 시스템과 오프라인 환경에서도 끊김 없는 기록을 보장하는 오프라인-퍼스트 전략이 적용된 안드로이드 식단 관리 애플리케이션입니다.

---

## Key Features

### 1. 하이브리드 AI 식단 분석 (Online)
* **Dual Engine:** 사용자의 선택에 따라 **Google Gemini Pro Vision** 또는 **OpenAI GPT-4o** 엔진을 사용하여 음식 사진을 분석합니다.
* **영양소 추출:** 음식명뿐만 아니라 칼로리, 탄수화물, 단백질, 지방 정보를 정밀하게 추출하여 JSON 데이터로 정형화합니다.

### 2. 오프라인-퍼스트 동기화 시스템 (Offline)
* **On-device Filtering:** 인터넷 연결이 없을 때, **TFLite(MobileNetV2)** 모델이 기기 내부에서 음식 여부를 1차 판별합니다.
* **Background Sync:** 네트워크 단절 시 요청을 **Room DB(Offline Queue)**에 적재하고, **WorkManager**를 통해 네트워크 복구 시 백그라운드에서 자동 전송 및 분석을 완료합니다.

### 3. 지능형 영양 컨설팅 (AI Recommendation)
* **Personalized Feedback:** 당일 섭취한 누적 영양 데이터를 분석하여 부족한 영양소를 채울 수 있는 최적의 다음 식사 메뉴를 제안합니다.

### 4. 데이터 시각화 (Visualization)
* **Weekly Statistics:** **Vico Chart**를 활용하여 최근 7일간의 영양 섭취 추이를 시계열 그래프로 제공합니다.

---

## 🛠 Tech Stack

### Android & UI
* **Language:** Kotlin
* **UI Framework:** Jetpack Compose (Material 3)
* **Architecture:** MVVM + Repository Pattern
* **Reactive Stream:** Coroutines, StateFlow, Flow

### Data & Background
* **Local Database:** Room (SQLite)
* **Background Task:** WorkManager
* **Networking:** Retrofit2, OkHttp3

### AI & Intelligence
* **Cloud AI:** Google Generative AI SDK (Gemini), OpenAI API
* **Edge AI:** TensorFlow Lite (TFLite)
* **Image Processing:** ImageProcessor (TFLite Support Library)

---

## 🏗 System Architecture



1. **Presentation Layer:** Jetpack Compose를 통해 상태 기반 UI를 구현하고 ViewModel을 통해 UI State를 관리합니다.
2. **Data Layer:** `MealRepository`가 로컬 DB(Room)와 원격 API(Gemini/GPT) 간의 데이터 흐름을 중재합니다.
3. **Offline Pipeline:** `NetworkHelper`가 상태를 감지하고, 오프라인 시 `OfflineWorker`가 백그라운드 동기화를 수행합니다.

---

## ⚠️ Troubleshooting

### 1. 안드로이드 13+ 권한 및 보안 정책 대응
* **Issue:** API 33 이상에서 카메라 실행 및 알림 발송 시 앱 런타임 권한 이슈 발생.
* **Solution:** `ActivityResultLauncher`를 활용하여 다중 권한(`CAMERA`, `POST_NOTIFICATIONS`) 요청 로직을 구현하고 시스템 가시성 정책(`queries`)을 선언하여 해결했습니다.

### 2. 빌드 환경 의존성 충돌
* **Issue:** 특정 라이브러리 버전(v2.2.21)과 Kotlin/Gradle 버전 간의 의존성 충돌로 인한 빌드 오류.
* **Solution:** 에러 로그 분석을 통해 충돌 모듈을 특정하고, 호환 가능한 안정 버전으로 마이그레이션하여 개발 환경을 안정화했습니다.

### 3. 시계열 데이터 가공 (Zero-Padding)
* **Issue:** 기록이 없는 날짜 데이터의 누락으로 그래프 왜곡 발생.
* **Solution:** 과거 7일 날짜 리스트를 생성하여 DB 데이터와 매핑하고, 비어있는 인덱스에 0을 할당하는 전처리 알고리즘을 적용하여 시각적 정확도를 확보했습니다.

---

## 📸 Screenshots
![Screenshot_20251217_193215_Food-Select](https://github.com/user-attachments/assets/42f2b247-c390-4fe7-af52-1bfd1e2eb59c)

| ![1046](https://github.com/user-attachments/assets/f439a96b-0285-4fbc-8075-1f8d983afefa) | 
![1044](https://github.com/user-attachments/assets/6c82ba30-782e-4b63-8190-e381bedaa200)

> *이미지 영역은 실제 스크린샷 파일로 교체하시기 바랍니다.*




## 👨‍💻 Author
* **Name:** [안강현[

* **Contact:** [jlpt25@proton.me[
