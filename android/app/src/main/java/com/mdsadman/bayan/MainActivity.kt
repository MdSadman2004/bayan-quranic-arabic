package com.mdsadman.bayan

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var controller: AppController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        controller = AppController(applicationContext)
        controller.initTts(this)
        ToneFeedback.init()
        handleIntent(intent)
        setContent { BayanTheme { BayanApp(controller) } }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    override fun onDestroy() {
        controller.shutdownTts()
        ToneFeedback.release()
        super.onDestroy()
    }

    private fun handleIntent(intent: Intent?) {
        val i = intent ?: return
        when (i.getStringExtra("screen")) {
            "lesson" -> {
                controller.pendingLesson = i.getStringExtra("lesson") ?: "u1l1"
                controller.pendingAuto = i.getBooleanExtra("auto", false)
                controller.processPendingIntent()
            }
            "profile" -> controller.go("profile")
            "practice" -> controller.go("practice")
            "home" -> controller.go("home")
        }
    }
}

@Composable
fun BayanApp(c: AppController) {
    // Cold-start deep links are handled in MainActivity.handleIntent() (before composition);
    // warm links process there too, so no LaunchedEffect is needed here.
    Box(Modifier.fillMaxSize().background(Paper).statusBarsPadding()) {
        when (c.route) {
            "onboarding" -> OnboardingScreen(c)
            "home" -> ScreensWithTabs(c) { HomeScreen(c) }
            "practice" -> ScreensWithTabs(c) { PracticeScreen(c) }
            "rites" -> ScreensWithTabs(c) { RitesScreen(c) }
            "circle" -> ScreensWithTabs(c) { CircleScreen(c) }
            "boss" -> ScreensWithTabs(c) { BossClock(c) }
            "profile" -> ScreensWithTabs(c) { ProfileScreen(c) }
            "lesson" -> LessonScreen(c)
        }
        ToastView(c)
    }
}

@Composable
private fun ScreensWithTabs(c: AppController, content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().padding(bottom = 60.dp).navigationBarsPadding()) { content() }
        TabBar(c, Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
fun ToastView(c: AppController) {
    val text = c.toast ?: return
    LaunchedEffect(text) {
        delay(2000)
        c.toast = null
    }
    Box(
        Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .padding(bottom = 96.dp)
            .navigationBarsPadding(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Text(
            text,
            color = Paper,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(Ink)
                .padding(horizontal = 20.dp, vertical = 10.dp)
        )
    }
}
