package com.example.spark

import android.Manifest
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.spark.network.AuthState
import com.example.spark.network.ChallengeStateResponse
import com.example.spark.network.DataState
import com.example.spark.network.ProfileResponse
import com.example.spark.network.ProfileUpdateRequest
import com.example.spark.network.SparkViewModel
import com.example.spark.ui.theme.SparkCoral
import com.example.spark.ui.theme.SparkGold
import com.example.spark.ui.theme.SparkClay
import com.example.spark.ui.theme.SparkPeach
import com.example.spark.ui.theme.SparkTheme
import kotlinx.coroutines.delay

// ─── Connectivity helpers ──────────────────────────────────────────────────────

@Composable
private fun rememberIsOnline(): Boolean {
    val context = LocalContext.current
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    fun currentlyOnline(): Boolean {
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    var isOnline by remember { mutableStateOf(currentlyOnline()) }

    DisposableEffect(Unit) {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { isOnline = true }
            override fun onLost(network: Network) { isOnline = false }
        }
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        cm.registerNetworkCallback(request, callback)
        onDispose { cm.unregisterNetworkCallback(callback) }
    }

    return isOnline
}

@Composable
private fun NoInternetBanner() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFCC3333))
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Нет подключения к интернету",
            color = Color.White,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SparkTheme {
                SparkApp()
            }
        }
    }
}

private enum class SparkScreen(val label: String) {
    Home("Главная"),
    Challenges("Задания"),
    Achievements("Награды"),
    Profile("Профиль")
}

private enum class SparkAppStage {
    Onboarding,
    Auth,
    Main
}

private fun AnimatedContentTransitionScope<SparkAppStage>.sparkStageTransition(): ContentTransform {
    val direction = if (targetState.ordinal > initialState.ordinal) 1 else -1
    return ((
        slideInHorizontally(
            animationSpec = tween(520),
            initialOffsetX = { direction * it / 7 }
        ) + fadeIn(tween(430))
    ) togetherWith (
        slideOutHorizontally(
            animationSpec = tween(420),
            targetOffsetX = { -direction * it / 9 }
        ) + fadeOut(tween(340))
    )).using(SizeTransform(clip = false))
}

private fun AnimatedContentTransitionScope<SparkScreen>.sparkScreenTransition(): ContentTransform {
    val direction = if (targetState.ordinal > initialState.ordinal) 1 else -1
    return ((
        slideInHorizontally(
            animationSpec = tween(460),
            initialOffsetX = { direction * it / 8 }
        ) + fadeIn(tween(360))
    ) togetherWith (
        slideOutHorizontally(
            animationSpec = tween(360),
            targetOffsetX = { -direction * it / 10 }
        ) + fadeOut(tween(300))
    )).using(SizeTransform(clip = false))
}

private fun AnimatedContentTransitionScope<Int>.sparkOnboardingTransition(): ContentTransform {
    val direction = if (targetState > initialState) 1 else -1
    return ((
        slideInHorizontally(
            animationSpec = tween(460),
            initialOffsetX = { direction * it / 8 }
        ) + fadeIn(tween(360))
    ) togetherWith (
        slideOutHorizontally(
            animationSpec = tween(360),
            targetOffsetX = { -direction * it / 10 }
        ) + fadeOut(tween(300))
    )).using(SizeTransform(clip = false))
}

private enum class ChallengeCategory(val title: String, val shortTitle: String, val accent: Color) {
    Social("Социализация", "Люди", SparkCoral),
    Activity("Активность", "Движение", SparkGold),
    Awareness("Наблюдательность", "Внимание", SparkPeach),
    Courage("Смелость", "Новое", SparkClay)
}

private enum class ChallengeType {
    Simple,
    Photo
}

private enum class Difficulty(val title: String, val couragePoints: Int) {
    Easy("Легкое", 5),
    Medium("Среднее", 10),
    Hard("Сложное", 20)
}

private enum class CompletionStatus {
    Open,
    Active,
    Checking,
    Done,
    Skipped
}

private data class Challenge(
    val id: Int,
    val title: String,
    val description: String,
    val category: ChallengeCategory,
    val difficulty: Difficulty,
    val type: ChallengeType,
    val todayCount: Int,
    val status: CompletionStatus = CompletionStatus.Open,
    val selectedPhoto: Uri? = null
)

private data class Achievement(
    val title: String,
    val description: String,
    val progress: Int,
    val goal: Int,
    val unlocked: Boolean,
    val accent: Color
)

// SparkUser удалён — используется ProfileResponse из API

private val initialChallenges = listOf(
    Challenge(
        id = 1,
        title = "Сделать комплимент",
        description = "Скажи простой добрый комплимент человеку вне привычного круга.",
        category = ChallengeCategory.Social,
        difficulty = Difficulty.Medium,
        type = ChallengeType.Simple,
        todayCount = 138
    ),
    Challenge(
        id = 2,
        title = "7000 шагов",
        description = "Прогуляйся в спокойном темпе и дай себе время заметить город.",
        category = ChallengeCategory.Activity,
        difficulty = Difficulty.Easy,
        type = ChallengeType.Simple,
        todayCount = 312
    ),
    Challenge(
        id = 3,
        title = "Пять красных предметов",
        description = "Найди и сфотографируй пять красных объектов вокруг себя.",
        category = ChallengeCategory.Awareness,
        difficulty = Difficulty.Easy,
        type = ChallengeType.Photo,
        todayCount = 86
    ),
    Challenge(
        id = 4,
        title = "Новое место",
        description = "Зайди в место, где ты еще не был: кафе, магазин, двор или парк.",
        category = ChallengeCategory.Courage,
        difficulty = Difficulty.Hard,
        type = ChallengeType.Photo,
        todayCount = 49
    ),
    Challenge(
        id = 5,
        title = "Первый вопрос",
        description = "Спроси дорогу, рекомендацию или мнение у незнакомого человека.",
        category = ChallengeCategory.Social,
        difficulty = Difficulty.Hard,
        type = ChallengeType.Simple,
        todayCount = 24
    ),
    Challenge(
        id = 6,
        title = "Маршрут без автопилота",
        description = "Пройди домой или до магазина другой дорогой и отметь три детали.",
        category = ChallengeCategory.Activity,
        difficulty = Difficulty.Medium,
        type = ChallengeType.Simple,
        todayCount = 171
    )
)

@Composable
private fun SparkApp() {
    val context = LocalContext.current
    val vm: SparkViewModel = viewModel(factory = SparkViewModel.Factory(context))

    val token by vm.token.collectAsState()
    val authState by vm.authState.collectAsState()
    val profileState by vm.profile.collectAsState()
    val challengesState by vm.challenges.collectAsState()

    var showSplash by rememberSaveable { mutableStateOf(true) }
    var onboardingDone by rememberSaveable { mutableStateOf(false) }
    var currentScreen by rememberSaveable { mutableStateOf(SparkScreen.Home) }
    var selectedChallengeId by rememberSaveable { mutableStateOf<Int?>(null) }

    LaunchedEffect(Unit) {
        delay(1100)
        showSplash = false
    }

    // Сбрасываем экран при выходе из аккаунта
    LaunchedEffect(token) {
        if (token == null) {
            currentScreen = SparkScreen.Home
            selectedChallengeId = null
        }
    }

    val isSignedIn = token != null
    val isOnline = rememberIsOnline()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        AnimatedVisibility(
            visible = showSplash,
            enter = fadeIn(tween(500)) + scaleIn(initialScale = 0.96f),
            exit = fadeOut(tween(450))
        ) {
            SplashScreen()
        }

        if (!showSplash) {
            val stage = when {
                !onboardingDone -> SparkAppStage.Onboarding
                !isSignedIn -> SparkAppStage.Auth
                else -> SparkAppStage.Main
            }
            AnimatedContent(
                targetState = stage,
                transitionSpec = { sparkStageTransition() },
                label = "app-stage"
            ) { appStage ->
                when (appStage) {
                    SparkAppStage.Onboarding -> OnboardingScreen(onFinish = { onboardingDone = true })
                    SparkAppStage.Auth -> AuthScreen(vm = vm)
                    SparkAppStage.Main -> {
                        Scaffold(
                            containerColor = MaterialTheme.colorScheme.background,
                            bottomBar = {
                                SparkBottomBar(
                                    current = currentScreen,
                                    onSelect = { currentScreen = it }
                                )
                            }
                        ) { innerPadding ->
                            AnimatedContent(
                                targetState = currentScreen,
                                transitionSpec = { sparkScreenTransition() },
                                label = "main-screen"
                            ) { screen ->
                                SparkMainContent(
                                    modifier = Modifier.padding(innerPadding),
                                    screen = screen,
                                    profileState = profileState,
                                    challengesState = challengesState,
                                    onOpenChallenge = { selectedChallengeId = it },
                                    onNotificationsChanged = { enabled ->
                                        vm.updateProfile(ProfileUpdateRequest(notificationsEnabled = enabled))
                                    },
                                    onLogout = { vm.logout() }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Bottom sheet задания
        selectedChallengeId?.let { openId ->
            val apiStates = (challengesState as? DataState.Ready)?.data ?: emptyList()
            val apiState = apiStates.firstOrNull { it.challengeId == openId }
            val challenge = initialChallenges.firstOrNull { it.id == openId } ?: return@let
            val status = apiState?.status ?: "open"
            val activeId = apiStates.firstOrNull { it.status == "active" || it.status == "checking" }?.challengeId
            val activeChallenge = activeId?.let { id -> initialChallenges.firstOrNull { it.id == id } }

            ChallengeSheet(
                challenge = challenge,
                status = status,
                photoUrl = apiState?.photoUrl,
                activeChallenge = activeChallenge,
                onDismiss = { selectedChallengeId = null },
                onStart = {
                    vm.updateChallenge(openId, "active")
                },
                onOpenActive = { selectedChallengeId = activeId },
                onComplete = { photoUri ->
                    val newStatus = if (challenge.type == ChallengeType.Photo) "checking" else "done"
                    vm.updateChallenge(openId, newStatus, photoUri?.toString())
                    // Обновляем профиль: courage + streak
                    val profile = (profileState as? DataState.Ready)?.data
                    if (profile != null) {
                        val bonus = if ((profile.streak + 1) % 7 == 0) 25 else 0
                        val newCourage = (profile.courage + challenge.difficulty.couragePoints + bonus).coerceAtMost(100)
                        val newStreak = profile.streak + 1
                        vm.updateProfile(
                            ProfileUpdateRequest(
                                courage = newCourage,
                                completed = profile.completed + 1,
                                streak = newStreak,
                                level = levelFor(newCourage)
                            )
                        )
                    }
                    if (challenge.type != ChallengeType.Photo) selectedChallengeId = null
                },
                onPhotoVerified = { photoUri ->
                    vm.updateChallenge(openId, "done", photoUri?.toString())
                    selectedChallengeId = null
                },
                onSkip = {
                    vm.updateChallenge(openId, "skipped")
                    val profile = (profileState as? DataState.Ready)?.data
                    if (profile != null) {
                        vm.updateProfile(
                            ProfileUpdateRequest(
                                courage = (profile.courage - 3).coerceAtLeast(0),
                                skipped = profile.skipped + 1,
                                streak = 0
                            )
                        )
                    }
                    selectedChallengeId = null
                }
            )
        }

        // Баннер отсутствия интернета (поверх всего контента)
        AnimatedVisibility(
            visible = !isOnline,
            enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { -it },
            exit = fadeOut(tween(300)) + slideOutVertically(tween(300)) { -it },
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            NoInternetBanner()
        }
    }
}

@Composable
private fun SplashScreen() {
    val isDark = isSystemInDarkTheme()
    val gradientColors = if (isDark) {
        listOf(Color(0xFF202B46), Color(0xFF485C8B))
    } else {
        listOf(Color(0xFFFFE7B7), Color(0xFFFDD58D))
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = gradientColors
                )
            )
            .safeDrawingPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            SparkLogo(modifier = Modifier.size(112.dp))
            Spacer(Modifier.height(18.dp))
            Text(
                text = "Spark",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "маленькая смелость каждый день",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun OnboardingScreen(onFinish: () -> Unit) {
    var page by rememberSaveable { mutableStateOf(0) }
    val pages = listOf(
        "Spark помогает делать небольшие реальные шаги вне телефона.",
        "Выбирай задания дня: общение, прогулки, наблюдательность и новое.",
        "Прогресс растет мягко: без рейтингов и давления.",
        "Поддержка важнее идеальности. Начать снова — тоже смелость."
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SparkLogo(modifier = Modifier.size(54.dp))
                TextButton(onClick = onFinish) {
                    Text("Пропустить")
                }
            }
            Spacer(Modifier.height(44.dp))
            AnimatedContent(
                targetState = page,
                transitionSpec = { sparkOnboardingTransition() },
                label = "onboarding-page"
            ) { currentPage ->
                Column {
                    Text(
                        text = when (currentPage) {
                            0 -> "Маленькие шаги в реальную жизнь"
                            1 -> "Задания, которые можно сделать сегодня"
                            2 -> "Прогресс без гонки"
                            else -> "Спокойная поддержка рядом"
                        },
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        lineHeight = 36.sp
                    )
                    Spacer(Modifier.height(18.dp))
                    Text(
                        text = pages[currentPage],
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 25.sp
                    )
                }
            }
            Spacer(Modifier.height(32.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pages.indices.forEach { index ->
                    Box(
                        modifier = Modifier
                            .height(8.dp)
                            .width(if (index == page) 32.dp else 8.dp)
                            .clip(RoundedCornerShape(50))
                            .background(
                                if (index == page) {
                                    MaterialTheme.colorScheme.secondary
                                } else {
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
                                }
                            )
                    )
                }
            }
        }

        Button(
            onClick = {
                if (page == pages.lastIndex) onFinish() else page += 1
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(18.dp)
        ) {
            Text(if (page == pages.lastIndex) "Начать" else "Дальше")
        }
    }
}

@Composable
private fun AuthScreen(vm: SparkViewModel) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var isRegisterMode by rememberSaveable { mutableStateOf(false) }
    val authState by vm.authState.collectAsState()

    // Сбрасываем ошибку при смене режима
    LaunchedEffect(isRegisterMode) { vm.resetAuthState() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
            SparkLogo(modifier = Modifier.size(70.dp))
            Spacer(Modifier.height(22.dp))
            Text(
                text = if (isRegisterMode) "Создать аккаунт" else "Добро пожаловать",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                lineHeight = 34.sp
            )
            Text(
                text = "Задания, серия и шкала смелости сохраняются в аккаунте.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 24.sp
            )
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                label = { Text("Email") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                label = { Text("Пароль") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )
            if (authState is AuthState.Error) {
                SupportCard((authState as AuthState.Error).message)
            }
            SupportCard("Spark не показывает профили другим людям и не шарит данные.")
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = {
                    if (isRegisterMode) vm.register(email.trim(), password)
                    else vm.login(email.trim(), password)
                },
                enabled = authState !is AuthState.Loading && email.isNotBlank() && password.length >= 6,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(18.dp)
            ) {
                if (authState is AuthState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(if (isRegisterMode) "Зарегистрироваться" else "Войти")
                }
            }
            TextButton(
                onClick = { isRegisterMode = !isRegisterMode },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (isRegisterMode) "Уже есть аккаунт? Войти" else "Нет аккаунта? Зарегистрироваться",
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun SparkMainContent(
    modifier: Modifier,
    screen: SparkScreen,
    profileState: DataState<ProfileResponse>,
    challengesState: DataState<List<ChallengeStateResponse>>,
    onOpenChallenge: (Int) -> Unit,
    onNotificationsChanged: (Boolean) -> Unit,
    onLogout: () -> Unit
) {
    val profile = (profileState as? DataState.Ready)?.data
    val apiStates = (challengesState as? DataState.Ready)?.data ?: emptyList()

    // Мержим статичный список заданий с динамическими статусами из API
    val challenges = initialChallenges.map { challenge ->
        val apiState = apiStates.firstOrNull { it.challengeId == challenge.id }
        val status = when (apiState?.status) {
            "active" -> CompletionStatus.Active
            "checking" -> CompletionStatus.Checking
            "done" -> CompletionStatus.Done
            "skipped" -> CompletionStatus.Skipped
            else -> CompletionStatus.Open
        }
        challenge.copy(status = status)
    }

    Box(modifier = modifier.fillMaxSize()) {
        SparkScreenBackdrop()
        when (screen) {
            SparkScreen.Home -> HomeScreen(Modifier.fillMaxSize(), profile, challenges, onOpenChallenge)
            SparkScreen.Challenges -> ChallengesScreen(Modifier.fillMaxSize(), challenges, onOpenChallenge)
            SparkScreen.Achievements -> AchievementsScreen(
                Modifier.fillMaxSize(),
                achievementsFor(profile, challenges)
            )
            SparkScreen.Profile -> ProfileScreen(
                Modifier.fillMaxSize(),
                profile,
                challenges,
                onNotificationsChanged,
                onLogout
            )
        }
    }
}

@Composable
private fun SparkScreenBackdrop() {
    val blue = MaterialTheme.colorScheme.secondary
    val deepBlue = MaterialTheme.colorScheme.primary
    val orange = SparkCoral
    val line = MaterialTheme.colorScheme.primary.copy(alpha = 0.32f)

    Canvas(modifier = Modifier.fillMaxSize()) {
        drawCircle(
            color = orange.copy(alpha = 0.16f),
            radius = size.width * 0.32f,
            center = Offset(size.width * 0.92f, -size.height * 0.02f)
        )
        drawCircle(
            color = blue.copy(alpha = 0.18f),
            radius = size.width * 0.25f,
            center = Offset(size.width * 0.08f, size.height * 0.22f)
        )
        drawCircle(
            color = deepBlue.copy(alpha = 0.10f),
            radius = size.width * 0.18f,
            center = Offset(size.width * 0.98f, size.height * 0.78f)
        )
        val wave = Path().apply {
            moveTo(size.width * 0.76f, -size.height * 0.05f)
            cubicTo(
                size.width * 0.60f,
                size.height * 0.18f,
                size.width * 0.74f,
                size.height * 0.36f,
                size.width * 0.55f,
                size.height * 0.55f
            )
            cubicTo(
                size.width * 0.38f,
                size.height * 0.73f,
                size.width * 0.40f,
                size.height * 0.88f,
                size.width * 0.28f,
                size.height * 1.04f
            )
        }
        drawPath(
            path = wave,
            color = line,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

@Composable
private fun HomeScreen(
    modifier: Modifier,
    profile: ProfileResponse?,
    challenges: List<Challenge>,
    onOpenChallenge: (Int) -> Unit
) {
    val daily = challenges.take(3)
    val activeChallenge = challenges.firstOrNull { it.status.isLockedInProgress() }
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        GreetingHeader(profile)
        CourageCard(profile)
        StreakCard(profile)

        activeChallenge?.let { challenge ->
            SectionTitle("Текущее задание", "Сначала заверши его или спокойно откажись")
            ChallengeCard(challenge = challenge, compact = false, onClick = { onOpenChallenge(challenge.id) })
        }

        SectionTitle("Задания дня", "Выбери комфортный шаг")
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            daily.forEach { challenge ->
                ChallengeCard(challenge = challenge, compact = false, onClick = { onOpenChallenge(challenge.id) })
            }
        }

        SectionTitle("Я не один", "Анонимная активность сегодня")
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(end = 20.dp)
        ) {
            items(challenges.sortedByDescending { it.todayCount }.take(4)) { challenge ->
                SocialPulseCard(challenge)
            }
        }
    }
}

@Composable
private fun ChallengesScreen(
    modifier: Modifier,
    challenges: List<Challenge>,
    onOpenChallenge: (Int) -> Unit
) {
    var selectedCategory by rememberSaveable { mutableStateOf<ChallengeCategory?>(null) }
    val filtered = selectedCategory?.let { category ->
        challenges.filter { it.category == category }
    } ?: challenges

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Задания",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                FilterChipLike(
                    text = "Все",
                    selected = selectedCategory == null,
                    color = MaterialTheme.colorScheme.secondary,
                    onClick = { selectedCategory = null }
                )
            }
            items(ChallengeCategory.entries) { category ->
                FilterChipLike(
                    text = category.shortTitle,
                    selected = selectedCategory == category,
                    color = category.accent,
                    onClick = { selectedCategory = category }
                )
            }
        }
        filtered.forEach { challenge ->
            ChallengeCard(challenge = challenge, compact = false, onClick = { onOpenChallenge(challenge.id) })
        }
    }
}

@Composable
private fun AchievementsScreen(
    modifier: Modifier,
    achievements: List<Achievement>
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Достижения",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        SupportCard("Награды здесь не про соревнование. Это маленькие отметки того, что ты продолжаешь пробовать.")
        achievements.forEach { achievement ->
            AchievementCard(achievement)
        }
    }
}

@Composable
private fun ProfileScreen(
    modifier: Modifier,
    profile: ProfileResponse?,
    challenges: List<Challenge>,
    onNotificationsChanged: (Boolean) -> Unit,
    onLogout: () -> Unit
) {
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        onNotificationsChanged(granted)
    }
    val photoDone = challenges.count { it.type == ChallengeType.Photo && it.status == CompletionStatus.Done }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.secondary
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                val initial = profile?.email?.firstOrNull()?.uppercaseChar()?.toString() ?: "S"
                Text(initial, fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text(
                    profile?.email ?: "—",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(profile?.level ?: "Новичок", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        CourageCard(profile)

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("Выполнено", (profile?.completed ?: 0).toString(), Modifier.weight(1f))
            StatCard("Серия", "${profile?.streak ?: 0} дн.", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("Смелость", (profile?.courage ?: 0).toString(), Modifier.weight(1f))
            StatCard("Фото", photoDone.toString(), Modifier.weight(1f))
        }

        SupportCard("Ничего страшного, если день выпал. Возвращение к себе — уже действие.")

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Мягкие напоминания",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (profile?.notificationsEnabled == true) {
                            "Сегодня отличный день для маленькой смелости"
                        } else {
                            "Новые задания, поддержка и streak reminder"
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Button(
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            onNotificationsChanged(true)
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Text(if (profile?.notificationsEnabled == true) "Вкл" else "Включить")
                }
            }
        }

        OutlinedButton(
            onClick = onLogout,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text("Выйти из аккаунта")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChallengeSheet(
    challenge: Challenge,
    status: String,          // "open" | "active" | "checking" | "done" | "skipped"
    photoUrl: String?,
    activeChallenge: Challenge?,
    onDismiss: () -> Unit,
    onStart: () -> Unit,
    onOpenActive: (Int?) -> Unit,
    onComplete: (Uri?) -> Unit,
    onPhotoVerified: (Uri?) -> Unit,
    onSkip: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var photoUri by remember(challenge.id, photoUrl) { mutableStateOf<Uri?>(photoUrl?.let { Uri.parse(it) }) }
    var checking by remember(challenge.id, status) { mutableStateOf(status == "checking") }
    val isCurrentChallenge = status == "active" || status == "checking"
    val isFinished = status == "done" || status == "skipped"
    val anotherChallengeActive = activeChallenge != null && activeChallenge.id != challenge.id
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        photoUri = uri
    }

    LaunchedEffect(checking) {
        if (checking) {
            delay(1600)
            onPhotoVerified(photoUri)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                CategoryBadge(challenge.category)
                DifficultyBadge(challenge.difficulty)
            }
            Text(
                text = challenge.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = challenge.description,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 24.sp
            )
            Text(
                text = "${challenge.todayCount} человек сегодня тоже пробуют это задание",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            when {
                isCurrentChallenge -> SupportCard("Это твое текущее задание. Другое можно будет взять после выполнения или отказа.")
                anotherChallengeActive -> SupportCard("Сейчас у тебя уже есть активное задание: «${activeChallenge?.title}». Сначала заверши его или спокойно откажись.")
                isFinished -> SupportCard("Это задание уже закрыто. Можно выбрать новое, если нет активного вызова.")
                else -> SupportCard("Возьми задание, когда будешь готов. После этого другие задания подождут.")
            }

            if (challenge.type == ChallengeType.Photo && isCurrentChallenge) {
                PhotoUploadBox(
                    photoUri = photoUri,
                    checking = checking,
                    onPickPhoto = { photoPicker.launch("image/*") }
                )
            }

            if (checking) {
                SupportCard("Фото проверяется. Обычно это занимает пару секунд.")
            }

            when {
                anotherChallengeActive -> {
                    Button(
                        onClick = { onOpenActive(activeChallenge?.id) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Text("Открыть текущее задание")
                    }
                }

                isCurrentChallenge -> {
                    Button(
                        onClick = {
                            if (challenge.type == ChallengeType.Photo) {
                                if (photoUri != null) {
                                    checking = true
                                    onComplete(photoUri)
                                }
                            } else {
                                onComplete(null)
                            }
                        },
                        enabled = challenge.type == ChallengeType.Simple || photoUri != null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Text(if (challenge.type == ChallengeType.Photo) "Отправить на проверку" else "Выполнено")
                    }

                    OutlinedButton(
                        onClick = onSkip,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                    ) {
                        Text("Отказаться от текущего")
                    }
                }

                !isFinished -> {
                    Button(
                        onClick = onStart,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Text("Взять задание")
                    }
                }
            }
            Text(
                text = "Отказ не провал. Иногда бережный выбор — лучший шаг.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun GreetingHeader(profile: ProfileResponse?) {
    // Показываем часть email до @, либо fallback
    val displayName = profile?.email?.substringBefore("@") ?: "…"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Привет, $displayName",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Сегодня отличный день для маленькой смелости",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        SparkLogo(modifier = Modifier.size(48.dp))
    }
}

@Composable
private fun CourageCard(profile: ProfileResponse?) {
    val courage = profile?.courage ?: 0
    val level = profile?.level ?: "Новичок"
    val target = (courage / 100f).coerceIn(0f, 1f)
    val animated by animateFloatAsState(
        targetValue = target,
        animationSpec = tween(durationMillis = 900),
        label = "courage"
    )
    SparkCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column {
                Text("Шкала смелости", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    text = "$courage/100",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = level,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f))
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(Modifier.height(16.dp))
        CourageFlightProgress(progress = animated)
    }
}

@Composable
private fun CourageFlightProgress(progress: Float) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
    ) {
        val planeSize = 34.dp
        val clampedProgress = progress.coerceIn(0f, 1f)
        val travel = (maxWidth - planeSize).coerceAtLeast(0.dp)
        val planeSurface = MaterialTheme.colorScheme.surface
        val planeGlow = MaterialTheme.colorScheme.secondary.copy(alpha = 0.13f)
        val planeBody = MaterialTheme.colorScheme.primary
        val planeFold = MaterialTheme.colorScheme.secondary.copy(alpha = 0.72f)
        val planeOffset by animateDpAsState(
            targetValue = travel * clampedProgress,
            animationSpec = tween(1100),
            label = "plane-offset"
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .align(Alignment.Center)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.28f))
        )
        Box(
            modifier = Modifier
                .width(maxWidth * clampedProgress)
                .height(12.dp)
                .align(Alignment.CenterStart)
                .clip(RoundedCornerShape(50))
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.secondary
                        )
                    )
                )
        )
        Canvas(
            modifier = Modifier
                .offset(x = planeOffset)
                .size(planeSize)
                .align(Alignment.CenterStart)
                .graphicsLayer {
                    rotationZ = -4f + clampedProgress * 5f
                    shadowElevation = 4f
                    shape = CircleShape
                    clip = false
                }
        ) {
            drawCircle(
                color = planeSurface,
                radius = size.minDimension * 0.48f,
                center = center
            )
            drawCircle(
                color = planeGlow,
                radius = size.minDimension * 0.48f,
                center = center
            )
            val path = Path().apply {
                moveTo(size.width * 0.18f, size.height * 0.52f)
                lineTo(size.width * 0.82f, size.height * 0.20f)
                lineTo(size.width * 0.62f, size.height * 0.82f)
                lineTo(size.width * 0.48f, size.height * 0.58f)
                close()
            }
            drawPath(path, color = planeBody)
            drawPath(
                path = Path().apply {
                    moveTo(size.width * 0.48f, size.height * 0.58f)
                    lineTo(size.width * 0.82f, size.height * 0.20f)
                    lineTo(size.width * 0.40f, size.height * 0.50f)
                    close()
                },
                color = planeFold
            )
        }
    }
}

@Composable
private fun StreakCard(profile: ProfileResponse?) {
    val streak = profile?.streak ?: 0
    SparkCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Серия", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    text = "$streak дня подряд",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "До бонуса +25 осталось ${(7 - streak).coerceAtLeast(0)} дня",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                repeat(7) { index ->
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(
                                if (index < streak) {
                                    MaterialTheme.colorScheme.secondary
                                } else {
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                                }
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun ChallengeCard(challenge: Challenge, compact: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .shadow(2.dp, RoundedCornerShape(24.dp), spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.10f), RoundedCornerShape(24.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CategoryBadge(challenge.category)
                StatusDot(challenge.status)
            }
            Text(
                text = challenge.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (!compact) {
                Text(
                    text = challenge.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 21.sp
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                DifficultyBadge(challenge.difficulty)
                Text(
                    text = when (challenge.status) {
                        CompletionStatus.Active -> "В процессе"
                        CompletionStatus.Checking -> "Проверка"
                        CompletionStatus.Done -> "Готово"
                        CompletionStatus.Skipped -> "Отказ"
                        CompletionStatus.Open -> if (challenge.type == ChallengeType.Photo) "Фото" else "Кнопка"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SocialPulseCard(challenge: Challenge) {
    Card(
        modifier = Modifier
            .width(190.dp)
            .height(116.dp)
            .border(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f), RoundedCornerShape(22.dp)),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = challenge.title,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${challenge.todayCount} человек сегодня",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun AchievementCard(achievement: Achievement) {
    val progress = (achievement.progress.toFloat() / achievement.goal).coerceIn(0f, 1f)
    val containerColor = if (achievement.unlocked) {
        MaterialTheme.colorScheme.surface
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
    }
    Card(
        modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.10f), RoundedCornerShape(24.dp)),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            PaperPlaneBadge(achievement.accent, achievement.unlocked)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    achievement.title,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    achievement.description,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(50)),
                    color = achievement.accent,
                    trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                    strokeCap = StrokeCap.Round
                )
            }
            Text("${achievement.progress}/${achievement.goal}", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PhotoUploadBox(photoUri: Uri?, checking: Boolean, onPickPhoto: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.8f)
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f), RoundedCornerShape(24.dp))
            .clickable(enabled = !checking, onClick = onPickPhoto),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = when {
                    checking -> "Проверяем фото"
                    photoUri != null -> "Фото выбрано"
                    else -> "Загрузить фото"
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = when {
                    checking -> "ИИ ищет признаки выполнения задания"
                    photoUri != null -> "Можно отправлять на проверку"
                    else -> "Нажми, чтобы выбрать изображение"
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun SparkBottomBar(current: SparkScreen, onSelect: (SparkScreen) -> Unit) {
    Surface(
        tonalElevation = 4.dp,
        color = MaterialTheme.colorScheme.background
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .safeDrawingPadding()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            SparkScreen.entries.forEach { screen ->
                val selected = current == screen
                val itemColor by animateColorAsState(
                    targetValue = if (selected) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                    } else {
                        Color.Transparent
                    },
                    animationSpec = tween(260),
                    label = "bottom-item-color"
                )
                val itemScale by animateFloatAsState(
                    targetValue = if (selected) 1.015f else 1f,
                    animationSpec = tween(360),
                    label = "bottom-item-scale"
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .graphicsLayer {
                            scaleX = itemScale
                            scaleY = itemScale
                        }
                        .clip(RoundedCornerShape(18.dp))
                        .background(itemColor)
                        .clickable { onSelect(screen) }
                        .padding(vertical = 11.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = screen.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (selected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String, subtitle: String) {
    Column {
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun SparkCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.10f), RoundedCornerShape(26.dp)),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp), content = content)
    }
}

@Composable
private fun SupportCard(text: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.18f)),
        shape = RoundedCornerShape(22.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(16.dp),
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyMedium,
            lineHeight = 21.sp
        )
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .border(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f), RoundedCornerShape(22.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(22.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun CategoryBadge(category: ChallengeCategory) {
    Text(
        text = category.title,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(category.accent.copy(alpha = 0.14f))
            .padding(horizontal = 12.dp, vertical = 7.dp),
        color = category.accent,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun DifficultyBadge(difficulty: Difficulty) {
    Text(
        text = "${difficulty.title} +${difficulty.couragePoints}",
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.18f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        color = MaterialTheme.colorScheme.secondary,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
private fun StatusDot(status: CompletionStatus) {
    val color = when (status) {
        CompletionStatus.Open -> MaterialTheme.colorScheme.outline.copy(alpha = 0.75f)
        CompletionStatus.Active -> MaterialTheme.colorScheme.primary
        CompletionStatus.Checking -> MaterialTheme.colorScheme.secondary
        CompletionStatus.Done -> MaterialTheme.colorScheme.secondary
        CompletionStatus.Skipped -> MaterialTheme.colorScheme.primary
    }
    Box(
        modifier = Modifier
            .size(12.dp)
            .clip(CircleShape)
            .background(color)
    )
}

@Composable
private fun FilterChipLike(text: String, selected: Boolean, color: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(
                if (selected) {
                    color.copy(alpha = 0.18f)
                } else {
                    MaterialTheme.colorScheme.surface
                }
            )
            .border(1.dp, if (selected) color.copy(alpha = 0.45f) else Color.Transparent, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 15.dp, vertical = 10.dp)
    ) {
        Text(
            text = text,
            color = if (selected) color else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Composable
private fun SparkLogo(modifier: Modifier = Modifier) {
    val background = MaterialTheme.colorScheme.primary
    val mark = MaterialTheme.colorScheme.background
    Canvas(modifier = modifier) {
        drawCircle(
            color = background,
            radius = size.minDimension / 2f
        )
        val star = Path().apply {
            moveTo(size.width * 0.50f, size.height * 0.16f)
            lineTo(size.width * 0.58f, size.height * 0.42f)
            lineTo(size.width * 0.84f, size.height * 0.50f)
            lineTo(size.width * 0.58f, size.height * 0.58f)
            lineTo(size.width * 0.50f, size.height * 0.84f)
            lineTo(size.width * 0.42f, size.height * 0.58f)
            lineTo(size.width * 0.16f, size.height * 0.50f)
            lineTo(size.width * 0.42f, size.height * 0.42f)
            close()
        }
        drawPath(path = star, color = mark)
        drawPath(
            path = Path().apply {
                moveTo(size.width * 0.24f, size.height * 0.34f)
                lineTo(size.width * 0.29f, size.height * 0.46f)
                lineTo(size.width * 0.41f, size.height * 0.50f)
                lineTo(size.width * 0.29f, size.height * 0.54f)
                lineTo(size.width * 0.24f, size.height * 0.66f)
                lineTo(size.width * 0.19f, size.height * 0.54f)
                lineTo(size.width * 0.07f, size.height * 0.50f)
                lineTo(size.width * 0.19f, size.height * 0.46f)
                close()
            },
            color = mark.copy(alpha = 0.92f)
        )
        drawPath(
            path = Path().apply {
                moveTo(size.width * 0.76f, size.height * 0.62f)
                lineTo(size.width * 0.81f, size.height * 0.74f)
                lineTo(size.width * 0.93f, size.height * 0.78f)
                lineTo(size.width * 0.81f, size.height * 0.82f)
                lineTo(size.width * 0.76f, size.height * 0.94f)
                lineTo(size.width * 0.71f, size.height * 0.82f)
                lineTo(size.width * 0.59f, size.height * 0.78f)
                lineTo(size.width * 0.71f, size.height * 0.74f)
                close()
            },
            color = mark.copy(alpha = 0.92f)
        )
        drawArc(
            color = background,
            startAngle = 94f,
            sweepAngle = 250f,
            useCenter = false,
            topLeft = Offset(size.width * 0.31f, size.height * 0.24f),
            size = Size(size.width * 0.46f, size.height * 0.58f),
            style = Stroke(width = size.minDimension * 0.13f, cap = StrokeCap.Round)
        )
        drawArc(
            color = mark,
            startAngle = 102f,
            sweepAngle = 240f,
            useCenter = false,
            topLeft = Offset(size.width * 0.34f, size.height * 0.25f),
            size = Size(size.width * 0.39f, size.height * 0.55f),
            style = Stroke(width = size.minDimension * 0.09f, cap = StrokeCap.Round)
        )
    }
}

@Composable
private fun PaperPlaneBadge(color: Color, unlocked: Boolean) {
    val lockedColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
    Canvas(
        modifier = Modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = if (unlocked) 0.18f else 0.1f))
    ) {
        val path = Path().apply {
            moveTo(size.width * 0.22f, size.height * 0.50f)
            lineTo(size.width * 0.80f, size.height * 0.22f)
            lineTo(size.width * 0.62f, size.height * 0.78f)
            lineTo(size.width * 0.49f, size.height * 0.56f)
            close()
        }
        drawPath(path, color = if (unlocked) color else lockedColor)
        drawArc(
            color = Color.White.copy(alpha = 0.75f),
            startAngle = 210f,
            sweepAngle = 72f,
            useCenter = false,
            topLeft = Offset(size.width * 0.18f, size.height * 0.18f),
            size = Size(size.width * 0.62f, size.height * 0.62f),
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

private fun List<Challenge>.replaceChallenge(updated: Challenge): List<Challenge> {
    return map { challenge -> if (challenge.id == updated.id) updated else challenge }
}

private fun CompletionStatus.isLockedInProgress(): Boolean {
    return this == CompletionStatus.Active || this == CompletionStatus.Checking
}

private fun levelFor(courage: Int): String {
    return when {
        courage >= 90 -> "Spark Master"
        courage >= 70 -> "Смельчак"
        courage >= 50 -> "Исследователь"
        courage >= 25 -> "Искатель"
        else -> "Новичок"
    }
}

private fun achievementsFor(profile: ProfileResponse?, challenges: List<Challenge>): List<Achievement> {
    val completed = profile?.completed ?: 0
    val streak = profile?.streak ?: 0
    val socialDone = challenges.count {
        it.category == ChallengeCategory.Social && it.status == CompletionStatus.Done
    }
    val photoDone = challenges.count {
        it.type == ChallengeType.Photo && it.status == CompletionStatus.Done
    }
    return listOf(
        Achievement(
            title = "Первый шаг",
            description = "Первое выполненное задание",
            progress = completed.coerceAtMost(1),
            goal = 1,
            unlocked = completed >= 1,
            accent = SparkCoral
        ),
        Achievement(
            title = "7 дней рядом с собой",
            description = "Серия без пропусков",
            progress = streak.coerceAtMost(7),
            goal = 7,
            unlocked = streak >= 7,
            accent = SparkGold
        ),
        Achievement(
            title = "50 маленьких смелостей",
            description = "Выполненные задания за все время",
            progress = completed.coerceAtMost(50),
            goal = 50,
            unlocked = completed >= 50,
            accent = SparkClay
        ),
        Achievement(
            title = "Первый разговор",
            description = "Социальный челлендж без давления",
            progress = socialDone.coerceAtMost(1),
            goal = 1,
            unlocked = socialDone >= 1,
            accent = ChallengeCategory.Social.accent
        ),
        Achievement(
            title = "Фото-доказательство",
            description = "Первое задание с проверкой фото",
            progress = photoDone.coerceAtMost(1),
            goal = 1,
            unlocked = photoDone >= 1,
            accent = ChallengeCategory.Awareness.accent
        )
    )
}

@Preview(showBackground = true)
@Composable
private fun SparkPreview() {
    SparkTheme {
        HomeScreen(
            modifier = Modifier,
            profile = null,
            challenges = initialChallenges,
            onOpenChallenge = {}
        )
    }
}
