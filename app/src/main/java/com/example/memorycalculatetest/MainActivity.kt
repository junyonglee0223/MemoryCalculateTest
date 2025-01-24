package com.example.memorycalculatetest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.memorycalculatetest.ui.theme.MemoryCalculateTestTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

// 문제를 저장하는 데이터 클래스
data class Problem(
    val n1: Int,
    val n2: Int,
    val op: String,
    val result: Int
)

// GridItem: 각각의 카드 정보
data class GridItem(
    val front: String,  // 숫자 또는 기호
    val back: String,   // 알파벳
    val isFlipped: Boolean = false // 현재 뒤집힘 상태
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MemoryCalculateTestTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    GameScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(modifier: Modifier = Modifier) {
    // 문제 5개 생성
    val problems = remember { generateProblems(5) }
    var currentIndex by remember { mutableStateOf(0) }
    var isGameOver by remember { mutableStateOf(false) }

    // 4x4 보드 상태
    var initialItems by remember { mutableStateOf(generateInitialItems()) }
    val items = remember { mutableStateListOf<GridItem>() }
    // 선택한 3개의 인덱스
    val selectedIndices = remember { mutableStateListOf<Int>() }

    // 알파벳 상태 / 타이머
    var isFlipped by remember { mutableStateOf(false) }
    var timer by remember { mutableStateOf(10) }

    val scope = rememberCoroutineScope()

    // currentProblem (결과만 표시)
    val currentProblem = if (currentIndex < problems.size) problems[currentIndex] else null

    // 초기 아이템 셋업
    LaunchedEffect(initialItems) {
        items.clear()
        items.addAll(initialItems)
    }

    // 타이머 로직
    LaunchedEffect(isFlipped, currentIndex) {
        if (currentProblem == null || isGameOver) return@LaunchedEffect
        if (!isFlipped) {
            timer = 10
            while (timer > 0 && !isFlipped && !isGameOver) {
                delay(1000L)
                timer -= 1
            }
            if (timer == 0 && !isFlipped) {
                // 알파벳 상태로 전환
                isFlipped = true
                items.forEachIndexed { idx, item ->
                    items[idx] = item.copy(isFlipped = true)
                }
            }
        }
    }

    // 카드 클릭 로직
    fun onItemClick(index: Int) {
        // 알파벳 상태에서만 클릭 가능 & 게임 진행 중이어야 함
        if (!isGameOver && isFlipped && currentProblem != null) {
            // 숫자/기호 상태로 뒤집기
            items[index] = items[index].copy(isFlipped = false)
            selectedIndices.add(index)

            if (selectedIndices.size == 3) {
                // 정답 체크
                val selectedValues = selectedIndices.map { items[it].front }
                val correct = isCorrectAnswer(selectedValues, currentProblem.result)

                if (correct) {
                    // 정답 처리
                    if (currentIndex < problems.size - 1) {
                        currentIndex++
//                        isFlipped = false
//                        selectedIndices.clear()
                    } else {
                        // 마지막 문제 정답
                        isGameOver = true
                    }
                }
                // 오답일 경우 상태 유지

                // **정답/오답 상관없이** 3개는 1초 뒤 알파벳으로 복귀
                scope.launch {
                    delay(1000L)
                    // 클릭했던 3장 알파벳 복귀
                    selectedIndices.forEach { idx ->
                        if (idx < items.size) {
                            items[idx] = items[idx].copy(isFlipped = true)
                        }
                    }
                    selectedIndices.clear()
                }
            }
        }
    }

    // 보드 초기화
    fun resetBoard() {
        initialItems = generateInitialItems()
        isFlipped = false
        timer = 10
        selectedIndices.clear()
        currentIndex = 0
        isGameOver = false
    }

    // 뒤집기 버튼
    fun toggleFlipState() {
        if (!isGameOver) {
            isFlipped = !isFlipped
            items.forEachIndexed { idx, item ->
                items[idx] = item.copy(isFlipped = isFlipped)
            }
        }
    }

    // UI
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Memory Calculate Test") },
                actions = {
                    Row {
                        Button(onClick = { resetBoard() }, modifier = Modifier.padding(end = 8.dp)) {
                            Text("재설정")
                        }
                        Button(onClick = { toggleFlipState() }) {
                            Text(if (isFlipped) "숫자/기호로 뒤집기" else "알파벳으로 뒤집기")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        if (isGameOver) {
            // 게임 종료
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("게임 종료! 모든 문제를 풀었습니다.", style = MaterialTheme.typography.headlineMedium)
            }
        } else {
            // 게임 진행 중
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // 숫자/기호 상태일 때만 타이머 표시
                    if (!isFlipped) {
                        Text(
                            text = "남은 시간: $timer 초",
                            style = MaterialTheme.typography.headlineMedium,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                    // 문제 결과만 표시
                    currentProblem?.let {
                        Text(
                            text = "문제: ${it.result}",
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.padding(16.dp)
                        )
                    }

                    // 4x4 그리드
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(items) { index, item ->
                            GridCard(item = item, onClick = { onItemClick(index) })
                        }
                    }
                }
            }
        }
    }
}

// === 문제/정답 관련 함수들 ===

// 문제 5개 생성
fun generateProblems(count: Int): List<Problem> {
    val problems = mutableListOf<Problem>()
    repeat(count) {
        problems.add(generateOneProblem())
    }
    return problems
}

// 1개 문제 생성
fun generateOneProblem(): Problem {
    while (true) {
        val n1 = (1..12).random()
        val n2 = (1..12).random()
        if (n1 == n2) continue
        val ops = listOf("+", "-", "*", "÷")
        val op = ops.random()

        val result = when (op) {
            "+" -> n1 + n2
            "-" -> n1 - n2
            "*" -> n1 * n2
            "÷" -> if (n2 != 0 && n1 % n2 == 0) n1 / n2 else null
            else -> null
        }
        if (result != null) {
            return Problem(n1, n2, op, result)
        }
    }
}

// 정답 체크
fun isCorrectAnswer(selectedValues: List<String>, correctResult: Int): Boolean {
    if (selectedValues.size != 3) return false
    val num1 = selectedValues[0].toIntOrNull() ?: return false
    val op = selectedValues[1]
    val num2 = selectedValues[2].toIntOrNull() ?: return false
    val calc = when (op) {
        "+" -> num1 + num2
        "-" -> num1 - num2
        "*" -> num1 * num2
        "÷" -> if (num2 != 0 && num1 % num2 == 0) num1 / num2 else return false
        else -> return false
    }
    return calc == correctResult
}

// === 4x4 아이템 생성 ===
fun generateInitialItems(): List<GridItem> {
    val numbers = (1..12).map { it.toString() }
    val symbols = listOf("+", "-", "*", "÷")
    val combined = (numbers + symbols).shuffled().take(16)
    val alphabets = ('A'..'Z').shuffled().take(16)
    return combined.mapIndexed { index, front ->
        GridItem(
            front = front,
            back = alphabets[index].toString(),
            isFlipped = false
        )
    }
}

// 카드 Composable
@Composable
fun GridCard(item: GridItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .aspectRatio(1f)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (item.isFlipped) MaterialTheme.colorScheme.secondaryContainer
            else MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = if (item.isFlipped) item.back else item.front,
                style = MaterialTheme.typography.headlineMedium
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GameScreenPreview() {
    MemoryCalculateTestTheme {
        GameScreen()
    }
}
