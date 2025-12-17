package com.example.food_select.ui.theme

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp // ★ sp 단위 사용을 위해 필요
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.column.columnChart
import com.patrykandpatrick.vico.compose.component.textComponent // ★ 텍스트 설정용
import com.patrykandpatrick.vico.core.axis.AxisPosition
import com.patrykandpatrick.vico.core.axis.formatter.AxisValueFormatter
import com.patrykandpatrick.vico.core.component.shape.Shapes
import com.patrykandpatrick.vico.core.component.shape.LineComponent

@Composable
fun WeeklyChart(viewModel: HomeViewModel) {
    val chartModelProducer by viewModel.chartEntryModel.collectAsState()
    val labels by viewModel.bottomAxisLabels.collectAsState()

    if (chartModelProducer == null || labels.isEmpty()) return

    // Y축: 정수 변환
    val startAxisValueFormatter = AxisValueFormatter<AxisPosition.Vertical.Start> { value, _ ->
        value.toInt().toString()
    }

    // X축: 날짜 라벨
    val bottomAxisValueFormatter = AxisValueFormatter<AxisPosition.Horizontal.Bottom> { value, _ ->
        labels.getOrNull(value.toInt()) ?: ""
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "📊 최근 7일 섭취량 (오늘부터)",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        Chart(
            chart = columnChart(
                columns = listOf(
                    LineComponent(
                        color = android.graphics.Color.parseColor("#FF9800"),
                        thicknessDp = 16f,
                        shape = Shapes.roundedCornerShape(
                            topLeftPercent = 40,
                            topRightPercent = 40,
                            bottomLeftPercent = 0,
                            bottomRightPercent = 0
                        )
                    )
                ),
                spacing = 12.dp // ★ 간격을 조금 좁혀서 전체가 한눈에 들어오게 조정
            ),
            chartModelProducer = chartModelProducer!!,

            startAxis = rememberStartAxis(
                valueFormatter = startAxisValueFormatter,
                itemPlacer = com.patrykandpatrick.vico.core.axis.AxisItemPlacer.Vertical.default(
                    maxItemCount = 5
                ),
                guideline = LineComponent(
                    color = Color.LightGray.copy(alpha = 0.4f).toArgb(),
                    thicknessDp = 1f
                )
            ),

            // ★ [수정 핵심] 기울기 제거 + 글자 크기 축소
            bottomAxis = rememberBottomAxis(
                valueFormatter = bottomAxisValueFormatter,
                guideline = null,

                // ★ 1. 기울기 0 (똑바로 표시)
                labelRotationDegrees = 0f,

                // ★ 2. 글자 설정을 직접 커스텀 (크기를 10sp로 줄임)
                label = textComponent(
                    color = Color.Gray,
                    textSize = 10.sp, // 글자를 작게 해서 짤리지 않게 함
                    padding = com.patrykandpatrick.vico.core.dimensions.MutableDimensions(0f, 0f, 0f, 0f)
                )
            ),

            modifier = Modifier.height(250.dp)
        )
    }
}