package com.example.ui

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.api.ExportService
import com.example.api.McqExportItem
import com.example.data.*
import com.example.notification.*
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.core.app.ActivityCompat
import java.text.SimpleDateFormat
import java.util.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.example.R

import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onSplashFinished: () -> Unit) {
    var startAnimation by remember { mutableStateOf(false) }
    val alphaAnim by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessVeryLow),
        label = "alpha"
    )
    val scaleAnim by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.8f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "scale"
    )

    LaunchedEffect(key1 = true) {
        startAnimation = true
        delay(2600)
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.background
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .padding(24.dp)
                .graphicsLayer(
                    alpha = alphaAnim,
                    scaleX = scaleAnim,
                    scaleY = scaleAnim
                )
        ) {
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .drawBehind {
                        drawCircle(
                            color = Color(0x1F2196F3),
                            radius = this.size.maxDimension / 1.7f
                        )
                    }
                    .background(Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.app_logo_1779831330845),
                    contentDescription = "Companion Logo",
                    modifier = Modifier.size(90.dp).clip(CircleShape)
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Teacher’s Companion",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground,
                letterSpacing = (-0.5).sp,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Nigerian Educator Productivity Suite",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(32.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 3.dp
                )
                Text(
                    text = "Preparing syllabus database...",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherCompanionApp(viewModel: TeacherViewModel) {
    val context = LocalContext.current
    var showSplash by remember { mutableStateOf(true) }
    val onboardingCompleted by viewModel.onboardingCompleted.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    if (showSplash) {
        SplashScreen(onSplashFinished = { showSplash = false })
    } else if (currentUser == null) {
        AuthScreen(viewModel = viewModel)
    } else if (!onboardingCompleted) {
        OnboardingScreen(viewModel = viewModel)
    } else {
        var currentTab by remember { mutableStateOf("home") }
        var showSupportDialog by remember { mutableStateOf(false) }
        var showProfileDialog by remember { mutableStateOf(false) }
        
        // Detailed modal state views
        var activeLessonDetails: LessonNote? by remember { mutableStateOf(null) }
        var activeMcqDetails: MCQSet? by remember { mutableStateOf(null) }
        var activeTheoryDetails: TheorySet? by remember { mutableStateOf(null) }
        var activeClassDetails: SchoolClass? by remember { mutableStateOf(null) }

        // Trigger tab navigation from Home quick actions
        var presetInnerTab by remember { mutableStateOf("note") }
        val onQuickAction: (String, String) -> Unit = { tab, presetType ->
            currentTab = tab
            presetInnerTab = presetType
        }

        Scaffold(
            topBar = {
                val isDark by viewModel.isDarkMode.collectAsState()
                CenterAlignedTopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(id = R.drawable.app_logo_1779831330845),
                                contentDescription = "Logo",
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Companion",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    ),
                    actions = {
                        IconButton(onClick = { viewModel.toggleDarkMode() }) {
                            Icon(
                                imageVector = if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = "Toggle Theme",
                                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                            )
                        }
                        IconButton(onClick = { showProfileDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = "Profile",
                                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                            )
                        }
                    }
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    val items = listOf(
                        Triple("home", "Home", Icons.Default.Home),
                        Triple("ai_hub", "AI Gen", Icons.Default.AutoAwesome),
                        Triple("timetable", "Schedule", Icons.Default.CalendarMonth),
                        Triple("syllabus", "Syllabus", Icons.Default.LibraryBooks),
                        Triple("classes", "Workload", Icons.Default.Groups),
                        Triple("notifications", "Alerts", Icons.Default.Notifications)
                    )

                    items.forEach { (tabId, label, icon) ->
                        val isSelected = currentTab == tabId
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { currentTab = tabId },
                            label = { 
                                Text(
                                    text = label, 
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                ) 
                            },
                            icon = { 
                                Icon(
                                    imageVector = icon, 
                                    contentDescription = label
                                ) 
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier.testTag("nav_tab_$tabId")
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                when (currentTab) {
                    "home" -> HomeScreen(viewModel, onQuickAction)
                    "ai_hub" -> AiHubScreen(
                        viewModel = viewModel,
                        onViewNote = { activeLessonDetails = it },
                        onViewMcq = { activeMcqDetails = it },
                        onViewTheory = { activeTheoryDetails = it },
                        presetTab = presetInnerTab
                    )
                    "timetable" -> TimetableScreen(viewModel)
                    "syllabus" -> SyllabusScreen(viewModel)
                    "classes" -> SchoolClassesScreen(viewModel, onViewClass = { activeClassDetails = it })
                    "notifications" -> NotificationsScreen(viewModel, onOpenSettings = { currentTab = "settings" })
                    "settings" -> SettingsScreen(viewModel)
                }

                // --- DEEP DETAIL DIALOG MODALS ---

            // 1. LESSON NOTE DETAILS
            activeLessonDetails?.let { note ->
                LessonDetailsDialog(
                    note = note,
                    onDismiss = { activeLessonDetails = null },
                    onDelete = {
                        viewModel.deleteLessonNote(note.id)
                        activeLessonDetails = null
                        Toast.makeText(context, "Lesson removed", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            // 2. MCQ SET DETAILS
            activeMcqDetails?.let { mcqSet ->
                McqDetailsDialog(
                    mcqSet = mcqSet,
                    onDismiss = { activeMcqDetails = null },
                    onDelete = {
                        viewModel.deleteMcqSet(mcqSet.id)
                        activeMcqDetails = null
                        Toast.makeText(context, "MCQ Sheet removed", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            // 3. THEORY SET DETAILS
            activeTheoryDetails?.let { tSet ->
                TheoryDetailsDialog(
                    theorySet = tSet,
                    onDismiss = { activeTheoryDetails = null },
                    onDelete = {
                        viewModel.deleteTheorySet(tSet.id)
                        activeTheoryDetails = null
                        Toast.makeText(context, "Theory papers removed", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            // 4. CLASS DETAILS SCREEN OVERLAY
            activeClassDetails?.let { classObj ->
                ClassDetailsDialog(
                    schoolClass = classObj,
                    viewModel = viewModel,
                    onDismiss = { activeClassDetails = null }
                )
            }

            // 5. TECHNICAL ASSISTANCE / SUPPORT DIALOG
            if (showSupportDialog) {
                AlertDialog(
                    onDismissRequest = { showSupportDialog = false },
                    title = { Text("Teacher Support Helpline", fontWeight = FontWeight.Bold) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Welcome to the Creator Companion Support Helpdesk!")
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("📧 Email Support: support@companionnigeria.org")
                            Text("📞 WhatsApp helpline: +234 812 345 6789")
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showSupportDialog = false }) {
                            Text("Close", fontWeight = FontWeight.Bold)
                        }
                    }
                )
            }

            // 6. EDUCATOR PROFILE OVERVIEW DIALOG
            if (showProfileDialog) {
                val plan by viewModel.subscriptionPlan.collectAsState()
                val profileUser = currentUser
                AlertDialog(
                    onDismissRequest = { showProfileDialog = false },
                    title = { Text("Educator Profile", fontWeight = FontWeight.Bold) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Name: ${profileUser?.fullName ?: "Educator"}", fontWeight = FontWeight.Bold)
                            Text("Email: ${profileUser?.email ?: "N/A"}")
                            Text("Plan: $plan Plan", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(
                                onClick = {
                                    showProfileDialog = false
                                    viewModel.logout()
                                },
                                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Sign Out", fontWeight = FontWeight.Bold)
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showProfileDialog = false }) {
                            Text("OK")
                        }
                    }
                )
            }
        }
    }
}
}

// ==========================================
// 1. HOME SCREEN SECTION
// ==========================================
@Composable
fun HomeScreen(viewModel: TeacherViewModel, onQuickAction: (String, String) -> Unit) {
    val timetable by viewModel.timetableItems.collectAsState()
    val syllabusItems by viewModel.syllabusItems.collectAsState()
    
    // Calculate current day of week (Monday-Friday) to show Today's Timetable
    var currentDayName by remember { mutableStateOf(SimpleDateFormat("EEEE", Locale.US).format(Date())) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000L)
            currentDayName = SimpleDateFormat("EEEE", Locale.US).format(Date())
        }
    }

    val todaysLessons = remember(timetable, currentDayName) {
        timetable.filter { it.dayOfWeek.equals(currentDayName, ignoreCase = true) }
    }

    val nextLesson = remember(timetable, currentDayName) {
        val orderedDays = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
        if (timetable.isEmpty()) null
        else {
            val currentDayIndex = orderedDays.indexOfFirst { it.equals(currentDayName, ignoreCase = true) }.coerceAtLeast(0)
            var foundLesson: TimetableItem? = null
            for (i in 1..7) {
                val targetDayIndex = (currentDayIndex + i) % 7
                val targetDayName = orderedDays[targetDayIndex]
                val lessonsForDay = timetable.filter { it.dayOfWeek.equals(targetDayName, ignoreCase = true) }
                    .sortedBy { it.startTime }
                if (lessonsForDay.isNotEmpty()) {
                    foundLesson = lessonsForDay.first()
                    break
                }
            }
            foundLesson ?: timetable.sortedBy { it.startTime }.firstOrNull()
        }
    }

    val lessonNotes by viewModel.lessonNotes.collectAsState()

    val uncompletedSyllabus = remember(syllabusItems, lessonNotes) {
        syllabusItems.filter { sItem ->
            !lessonNotes.any { note ->
                note.topic.equals(sItem.topic, ignoreCase = true) &&
                note.gradeClass.equals(sItem.gradeClass, ignoreCase = true) &&
                note.subject.equals(sItem.subject, ignoreCase = true)
            }
        }.take(3)
    }

    val totalCompleted = remember(syllabusItems, lessonNotes) {
        syllabusItems.count { sItem ->
            lessonNotes.any { note ->
                note.topic.equals(sItem.topic, ignoreCase = true) &&
                note.gradeClass.equals(sItem.gradeClass, ignoreCase = true) &&
                note.subject.equals(sItem.subject, ignoreCase = true)
            }
        }
    }
    val percentageSyllabus = remember(syllabusItems, totalCompleted) {
        if (syllabusItems.isEmpty()) 0f
        else (totalCompleted.toFloat() / syllabusItems.size.toFloat())
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            val user by viewModel.currentUser.collectAsState()
            Column(modifier = Modifier.padding(top = 16.dp)) {
                Text(
                    text = "Welcome back,",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
                Text(
                    text = user?.fullName?.split(" ")?.firstOrNull() ?: "Educator",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        // Session expired banner
        val sessionExpired by viewModel.sessionExpired.collectAsState()
        if (sessionExpired) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Session Expired", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.error)
                            Text("Please log in again to sync your latest data.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                        TextButton(onClick = { viewModel.clearSessionExpired() }) {
                            Text("Dismiss", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }

        // Quick AI Actions
        item {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "AI CO-PILOT",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val actions = listOf(
                        Triple("Lesson Notes", Icons.Default.Assignment, "lesson"),
                        Triple("MCQ Gen", Icons.Default.Quiz, "mcq"),
                        Triple("Theory Paper", Icons.Default.Create, "theory")
                    )

                    actions.forEach { (label, icon, type) ->
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onQuickAction("ai_hub", type) },
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
                            shadowElevation = 2.dp
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    modifier = Modifier.size(40.dp),
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        
        // Today's schedule card
        item {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TODAY'S SCHEDULE",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp
                    )
                    TextButton(onClick = { onQuickAction("timetable", "") }) {
                        Text("See All", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
                    shadowElevation = 2.dp
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (todaysLessons.isEmpty()) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.EventNote,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                    modifier = Modifier.size(40.dp)
                                )
                                Text(
                                    "No periods scheduled today",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            todaysLessons.forEach { lesson ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = lesson.startTime,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "AM",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    
                                    Spacer(modifier = Modifier.width(20.dp))
                                    
                                    Box(
                                        modifier = Modifier
                                            .width(2.dp)
                                            .height(40.dp)
                                            .background(
                                                color = try { Color(android.graphics.Color.parseColor(lesson.colorHex)) } catch (_: Exception) { MaterialTheme.colorScheme.primary },
                                                shape = CircleShape
                                            )
                                    )
                                    
                                    Column(modifier = Modifier.padding(start = 16.dp)) {
                                        Text(
                                            text = lesson.subject,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${lesson.gradeClass} • ${lesson.schoolName}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // Syllabus Completion Card
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onQuickAction("syllabus", "") },
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
            ) {
                Row(
                    modifier = Modifier.padding(24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "SYLLABUS TRACKER",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Curriculum Progress",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        Text(
                            text = "${(percentageSyllabus * 100).toInt()}% topics completed",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    Box(contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            progress = { percentageSyllabus },
                            strokeWidth = 6.dp,
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            modifier = Modifier.size(64.dp)
                        )
                        Text(
                            text = "${(percentageSyllabus * 100).toInt()}%",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }


        // Lesson Note Action Section Title
        item {
            Text(
                text = "PENDING TASKS",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp
            )
        }

        if (uncompletedSyllabus.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "All notes generated!",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "You're all caught up with your syllabus.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(uncompletedSyllabus) { progress ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
                    shadowElevation = 1.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = progress.topic,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${progress.subject} • ${progress.gradeClass}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        Button(
                            onClick = { onQuickAction("ai_hub", "note") },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "Draft",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}


// ==========================================
// 2. AI GENERATOR HUB SCREEN
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiHubScreen(
    viewModel: TeacherViewModel,
    onViewNote: (LessonNote) -> Unit = {},
    onViewMcq: (MCQSet) -> Unit = {},
    onViewTheory: (TheorySet) -> Unit = {},
    presetTab: String = "note"
) {
    val context = LocalContext.current
    var innerTab by remember { mutableStateOf(presetTab) } // note, mcq, theory, history

    val mySchools by viewModel.teacherSchools.collectAsState()
    val myClasses by viewModel.schoolClasses.collectAsState()
    val syllabusItems by viewModel.syllabusItems.collectAsState()

    var selectedSchool by remember { mutableStateOf("") }
    var selectedClass by remember { mutableStateOf("") }
    var selectedSubject by remember { mutableStateOf("") }
    var topicText by remember { mutableStateOf("") }

    val matchingSyllabusItems = remember(syllabusItems, selectedSchool, selectedClass, selectedSubject) {
        syllabusItems.filter {
            it.schoolName.equals(selectedSchool, ignoreCase = true) &&
            it.gradeClass.equals(selectedClass, ignoreCase = true) &&
            it.subject.equals(selectedSubject, ignoreCase = true)
        }
    }

    LaunchedEffect(matchingSyllabusItems) {
        if (topicText.isEmpty() || !matchingSyllabusItems.any { it.topic.equals(topicText, ignoreCase = true) }) {
            topicText = matchingSyllabusItems.firstOrNull()?.topic ?: ""
        }
    }

    // Dropdown States
    var schoolExpanded by remember { mutableStateOf(false) }
    var classExpanded by remember { mutableStateOf(false) }
    var subjExpanded by remember { mutableStateOf(false) }
    var topicExpanded by remember { mutableStateOf(false) }

    // Sync selected school first on load
    LaunchedEffect(mySchools) {
        if (selectedSchool.isEmpty() || !mySchools.contains(selectedSchool)) {
            selectedSchool = mySchools.firstOrNull() ?: ""
        }
    }

    // Filter classes and subjects under selected school
    val availableClasses = remember(myClasses, selectedSchool) {
        myClasses.filter { it.schoolName.equals(selectedSchool, ignoreCase = true) }.map { it.className }.distinct()
    }
    LaunchedEffect(availableClasses, selectedSchool) {
        if (selectedClass.isEmpty() || !availableClasses.contains(selectedClass)) {
            selectedClass = availableClasses.firstOrNull() ?: ""
        }
    }

    val availableSubjects = remember(myClasses, selectedSchool, selectedClass) {
        myClasses.filter { it.schoolName.equals(selectedSchool, ignoreCase = true) && it.className.equals(selectedClass, ignoreCase = true) }.map { it.subject }.distinct()
    }
    LaunchedEffect(availableSubjects, selectedClass) {
        if (selectedSubject.isEmpty() || !availableSubjects.contains(selectedSubject)) {
            selectedSubject = availableSubjects.firstOrNull() ?: ""
        }
    }

    // 1. Lesson Note specific inputs
    var durationText by remember { mutableStateOf("40 Minutes") }
    var lessonCustomInstructions by remember { mutableStateOf("") }
    
    // 2. MCQ specific inputs
    val difficulties = listOf("Easy", "Medium", "Hard")
    var selectedDifficulty by remember { mutableStateOf(difficulties[1]) }
    var diffExpanded by remember { mutableStateOf(false) }
    var mcqCount by remember { mutableStateOf(5) }
    var mcqLessonReliance by remember { mutableIntStateOf(50) }

    // 3. Theory specific inputs
    var theoryCount by remember { mutableStateOf(3) }
    var theoryLessonReliance by remember { mutableIntStateOf(50) }

    val lessonState by viewModel.lessonGenerationState.collectAsState()
    val mcqState by viewModel.mcqGenerationState.collectAsState()
    val theoryState by viewModel.theoryGenerationState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Selector Header Tabs
        PrimaryTabRow(
            selectedTabIndex = when (innerTab) {
                "note" -> 0
                "mcq" -> 1
                "theory" -> 2
                else -> 3
            },
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.primary,
            indicator = { tabPositions ->
                TabRowDefaults.PrimaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[when (innerTab) {
                        "note" -> 0
                        "mcq" -> 1
                        "theory" -> 2
                        else -> 3
                    }]),
                    width = 40.dp,
                    shape = RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)
                )
            },
            divider = {}
        ) {
            val tabs = listOf("note" to "Note", "mcq" to "MCQs", "theory" to "Theory", "history" to "History")
            tabs.forEach { (id, label) ->
                Tab(
                    selected = innerTab == id,
                    onClick = { innerTab = id },
                    text = { 
                        Text(
                            text = label, 
                            style = if (innerTab == id) MaterialTheme.typography.labelLarge else MaterialTheme.typography.bodyMedium,
                            fontWeight = if (innerTab == id) FontWeight.Bold else FontWeight.Normal
                        ) 
                    },
                    modifier = Modifier.testTag("ai_tab_$id")
                )
            }
        }

        if (innerTab == "history") {
            HistoryScreen(
                viewModel = viewModel,
                onViewNote = onViewNote,
                onViewMcq = onViewMcq,
                onViewTheory = onViewTheory,
                modifier = Modifier.fillMaxSize().padding(20.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
            // SHARED PARAMETERS INPUT BLOCK
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
                    shadowElevation = 2.dp
                ) {
                    Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        Text(
                            "Content Parameters", 
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            // School Select
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = selectedSchool,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("School") },
                                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                                    modifier = Modifier.fillMaxWidth().clickable { schoolExpanded = true },
                                    shape = RoundedCornerShape(12.dp)
                                )
                                DropdownMenu(expanded = schoolExpanded, onDismissRequest = { schoolExpanded = false }) {
                                    mySchools.forEach { schoolItem ->
                                        DropdownMenuItem(text = { Text(schoolItem) }, onClick = { selectedSchool = schoolItem; schoolExpanded = false })
                                    }
                                }
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                // Class Select
                                Box(modifier = Modifier.weight(1f)) {
                                    OutlinedTextField(
                                        value = selectedClass,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Class") },
                                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                                        modifier = Modifier.fillMaxWidth().clickable { classExpanded = true },
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    DropdownMenu(expanded = classExpanded, onDismissRequest = { classExpanded = false }) {
                                        availableClasses.forEach { classItem ->
                                            DropdownMenuItem(text = { Text(classItem) }, onClick = { selectedClass = classItem; classExpanded = false })
                                        }
                                    }
                                }

                                // Subject Select
                                Box(modifier = Modifier.weight(1f)) {
                                    OutlinedTextField(
                                        value = selectedSubject,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Subject") },
                                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                                        modifier = Modifier.fillMaxWidth().clickable { subjExpanded = true },
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    DropdownMenu(expanded = subjExpanded, onDismissRequest = { subjExpanded = false }) {
                                        availableSubjects.forEach { s ->
                                            DropdownMenuItem(text = { Text(s) }, onClick = { selectedSubject = s; subjExpanded = false })
                                        }
                                    }
                                }
                            }

                            // Topic Selection
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = topicText,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Syllabus Topic") },
                                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                                    modifier = Modifier.fillMaxWidth().clickable { topicExpanded = true }.testTag("ai_topic_input"),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                DropdownMenu(
                                    expanded = topicExpanded,
                                    onDismissRequest = { topicExpanded = false },
                                    modifier = Modifier.fillMaxWidth(0.9f)
                                ) {
                                    if (matchingSyllabusItems.isEmpty()) {
                                        DropdownMenuItem(
                                            text = { Text("No topics found. Add to syllabus first.", color = Color.Gray) },
                                            onClick = { topicExpanded = false }
                                        )
                                    } else {
                                        matchingSyllabusItems.forEach { item ->
                                            DropdownMenuItem(
                                                text = { 
                                                    Column {
                                                        Text(item.topic, fontWeight = FontWeight.Bold)
                                                        if (item.objectives.isNotEmpty()) {
                                                            Text(item.objectives, style = MaterialTheme.typography.labelSmall, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                        }
                                                    }
                                                },
                                                onClick = {
                                                    topicText = item.topic
                                                    topicExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Tab-specific inputs... (rest of the code will be updated in next turns)


                        // 1. LESSON NOTE ADDS
                        if (innerTab == "note") {
                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                OutlinedTextField(
                                    value = durationText,
                                    onValueChange = { durationText = it },
                                    label = { Text("Duration") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp)
                                )

                                OutlinedTextField(
                                    value = lessonCustomInstructions,
                                    onValueChange = { lessonCustomInstructions = it },
                                    label = { Text("Custom Guidelines (Optional)") },
                                    placeholder = { Text("e.g. Include local examples") },
                                    modifier = Modifier.fillMaxWidth(),
                                    maxLines = 3,
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                        }

                        // 2. MCQ ADDS
                        if (innerTab == "mcq") {
                            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(modifier = Modifier.weight(1f)) {
                                        OutlinedTextField(
                                            value = selectedDifficulty,
                                            onValueChange = {},
                                            readOnly = true,
                                            label = { Text("Difficulty") },
                                            trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                                            modifier = Modifier.fillMaxWidth().clickable { diffExpanded = true },
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        DropdownMenu(expanded = diffExpanded, onDismissRequest = { diffExpanded = false }) {
                                            difficulties.forEach { d ->
                                                DropdownMenuItem(text = { Text(d) }, onClick = { selectedDifficulty = d; diffExpanded = false })
                                            }
                                        }
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Questions: $mcqCount", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Slider(
                                            value = mcqCount.toFloat(),
                                            onValueChange = { mcqCount = it.toInt().coerceIn(3, 50) },
                                            valueRange = 3f..50f,
                                            steps = 46,
                                            colors = SliderDefaults.colors(
                                                thumbColor = MaterialTheme.colorScheme.primary,
                                                activeTrackColor = MaterialTheme.colorScheme.primary
                                            )
                                        )
                                    }
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Syllabus Reliance", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                                        Text("$mcqLessonReliance%", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                    }
                                    Slider(
                                        value = mcqLessonReliance.toFloat(),
                                        onValueChange = { mcqLessonReliance = it.toInt().coerceIn(0, 100) },
                                        valueRange = 0f..100f,
                                        steps = 19
                                    )
                                    Text(
                                        "Determines how much the AI uses your syllabus content vs general knowledge.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // 3. THEORY ADDS
                        if (innerTab == "theory") {
                            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Questions: $theoryCount", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Slider(
                                        value = theoryCount.toFloat(),
                                        onValueChange = { theoryCount = it.toInt().coerceIn(2, 10) },
                                        valueRange = 2f..10f,
                                        steps = 7
                                    )
                                }
                                
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Syllabus Reliance", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                                        Text("$theoryLessonReliance%", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                    }
                                    Slider(
                                        value = theoryLessonReliance.toFloat(),
                                        onValueChange = { theoryLessonReliance = it.toInt().coerceIn(0, 100) },
                                        valueRange = 0f..100f,
                                        steps = 19
                                    )
                                }
                            }
                        }

                        // GENERATE BUTTON
                        Button(
                            onClick = {
                                if (topicText.trim().isEmpty()) {
                                    Toast.makeText(context, "Please select a topic", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                val matchedItem = matchingSyllabusItems.find { it.topic.equals(topicText, ignoreCase = true) }
                                
                                when (innerTab) {
                                    "note" -> viewModel.generateLessonNote(
                                        subject = selectedSubject,
                                        gradeClass = selectedClass,
                                        topic = topicText,
                                        duration = durationText,
                                        syllabusObjectives = matchedItem?.objectives ?: "",
                                        syllabusContent = matchedItem?.content ?: "",
                                        customInstructions = lessonCustomInstructions,
                                        theme = matchedItem?.theme ?: ""
                                    )
                                    "mcq" -> viewModel.generateMCQSets(
                                        subject = selectedSubject,
                                        gradeClass = selectedClass,
                                        topic = topicText,
                                        difficulty = selectedDifficulty,
                                        count = mcqCount,
                                        lessonReliancePercent = mcqLessonReliance
                                    )
                                    "theory" -> viewModel.generateTheoryQuestions(
                                        subject = selectedSubject,
                                        gradeClass = selectedClass,
                                        topic = topicText,
                                        count = theoryCount,
                                        lessonReliancePercent = theoryLessonReliance
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .testTag("generate_fab_button"),
                            shape = RoundedCornerShape(16.dp),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp, pressedElevation = 8.dp)
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = when (innerTab) {
                                    "note" -> "Generate Lesson Note"
                                    "mcq" -> "Generate MCQs"
                                    else -> "Generate Theory Questions"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }


            // ==================== GENERATIVE OUTPUT PANELS ====================
            item {
                when (innerTab) {
                    "note" -> LessonGenerationOutputContent(viewModel, selectedSubject, selectedClass, topicText, durationText)
                    "mcq" -> McqGenerationOutputContent(viewModel, selectedSubject, selectedClass, topicText, selectedDifficulty)
                    else -> TheoryGenerationOutputContent(viewModel, selectedSubject, selectedClass, topicText)
                }
            }
        }
    }
}
}

// 2A. LESSON NOTE OUTPUT RENDER PANEL
@Composable
fun LessonGenerationOutputContent(
    viewModel: TeacherViewModel,
    subject: String,
    gradeClass: String,
    topic: String,
    duration: String
) {
    val con = LocalContext.current
    val state by viewModel.lessonGenerationState.collectAsState()

    when (state) {
        is AiGenerationState.Idle -> { /* Nothing yet */ }
        is AiGenerationState.Generating -> {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
                shadowElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier.padding(32.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(strokeWidth = 3.dp)
                    Text(
                        "Drafting Lesson Plan...",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Crafting behavioral objectives and presentation steps tailored to your curriculum.",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        is AiGenerationState.Error -> {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("Generation Failed", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
                    Text((state as AiGenerationState.Error).message, style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = { viewModel.clearLessonState() }) {
                        Text("Dismiss", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
        is AiGenerationState.Success -> {
            val content = (state as AiGenerationState.Success<String>).data
            var editableText by remember { mutableStateOf(content) }
            
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
                shadowElevation = 4.dp
            ) {
                Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Drafted Lesson Plan", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        IconButton(onClick = { viewModel.clearLessonState() }) {
                            Icon(Icons.Default.Close, "Close")
                        }
                    }

                    OutlinedTextField(
                        value = editableText,
                        onValueChange = { editableText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 300.dp, max = 500.dp),
                        label = { Text("Review & Edit") },
                        shape = RoundedCornerShape(16.dp),
                        textStyle = MaterialTheme.typography.bodyMedium
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.saveGeneratedLessonNote(subject, gradeClass, topic, duration, editableText)
                                Toast.makeText(con, "Saved to History", Toast.LENGTH_SHORT).show()
                                viewModel.clearLessonState()
                            },
                            modifier = Modifier.weight(1.5f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Save, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Save Plan")
                        }

                        Surface(
                            modifier = Modifier.size(48.dp).clickable {
                                val html = "<h1>$subject</h1><p>$editableText</p>"
                                ExportService.exportToPdf(con, topic, html)
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.PictureAsPdf, null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }

                        Surface(
                            modifier = Modifier.size(48.dp).clickable {
                                val html = "<h1>$subject</h1><p>$editableText</p>"
                                ExportService.exportToWord(con, topic, html)
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Description, null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }
}

// 2B. MCQ OUTPUT RENDER PANEL
@Composable
fun McqGenerationOutputContent(
    viewModel: TeacherViewModel,
    subject: String,
    gradeClass: String,
    topic: String,
    difficulty: String
) {
    val con = LocalContext.current
    val state by viewModel.mcqGenerationState.collectAsState()

    when (state) {
        is AiGenerationState.Idle -> {}
        is AiGenerationState.Generating -> {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
                shadowElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier.padding(32.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(strokeWidth = 3.dp)
                    Text("Formulating MCQs...", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
        is AiGenerationState.Error -> {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("Generation Failed", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
                    Text((state as AiGenerationState.Error).message, style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = { viewModel.clearMcqState() }) {
                        Text("Dismiss", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
        is AiGenerationState.Success -> {
            val mcqs = (state as AiGenerationState.Success<List<McqModel>>).data
            
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
                shadowElevation = 4.dp
            ) {
                Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Generated MCQs (${mcqs.size})", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        IconButton(onClick = { viewModel.clearMcqState() }) { Icon(Icons.Default.Close, null) }
                    }

                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 400.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        mcqs.forEachIndexed { i, item ->
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.05f))
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("${i+1}. ${item.question}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text("A. ${item.optionA}", style = MaterialTheme.typography.bodySmall)
                                        Text("B. ${item.optionB}", style = MaterialTheme.typography.bodySmall)
                                        Text("C. ${item.optionC}", style = MaterialTheme.typography.bodySmall)
                                        Text("D. ${item.optionD}", style = MaterialTheme.typography.bodySmall)
                                    }
                                    Text("Correct: ${item.correctAnswer}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.saveMcqSet(subject, gradeClass, topic, difficulty, mcqs)
                                Toast.makeText(con, "Saved to History", Toast.LENGTH_SHORT).show()
                                viewModel.clearMcqState()
                            },
                            modifier = Modifier.weight(1.5f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Save, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Save Set")
                        }

                        Surface(
                            modifier = Modifier.size(48.dp).clickable {
                                val exportList = mcqs.map {
                                    McqExportItem(it.question, it.optionA, it.optionB, it.optionC, it.optionD, it.correctAnswer, it.explanation)
                                }
                                ExportService.exportToExcel(con, "${topic}_MCQ", exportList)
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f))
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.GridOn, null, tint = MaterialTheme.colorScheme.secondary)
                            }
                        }
                    }
                }
            }
        }
    }
}

// 2C. THEORY OUTPUT RENDER PANEL
@Composable
fun TheoryGenerationOutputContent(
    viewModel: TeacherViewModel,
    subject: String,
    gradeClass: String,
    topic: String
) {
    val con = LocalContext.current
    val state by viewModel.theoryGenerationState.collectAsState()

    when (state) {
        is AiGenerationState.Idle -> {}
        is AiGenerationState.Generating -> {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
                shadowElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier.padding(32.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(strokeWidth = 3.dp)
                    Text("Formulating Questions...", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
        is AiGenerationState.Error -> {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("Generation Failed", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
                    Text((state as AiGenerationState.Error).message, style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = { viewModel.clearTheoryState() }) {
                        Text("Dismiss", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
        is AiGenerationState.Success -> {
            val theories = (state as AiGenerationState.Success<List<TheoryModel>>).data
            
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
                shadowElevation = 4.dp
            ) {
                Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Theory Questions", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        IconButton(onClick = { viewModel.clearTheoryState() }) { Icon(Icons.Default.Close, null) }
                    }

                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 400.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        theories.forEachIndexed { i, item ->
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.05f))
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Q${i+1}. ${item.question}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Text("Marking Guide:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                    Text(item.markingScheme, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.saveTheorySet(subject, gradeClass, topic, theories)
                                Toast.makeText(con, "Saved to History", Toast.LENGTH_SHORT).show()
                                viewModel.clearTheoryState()
                            },
                            modifier = Modifier.weight(1.5f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Save, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Save Questions")
                        }

                        Surface(
                            modifier = Modifier.size(48.dp).clickable {
                                val html = "<h1>$subject</h1>" + theories.joinToString("<br>") { "<h3>${it.question}</h3><p>${it.markingScheme}</p>" }
                                ExportService.exportToWord(con, "${topic}_Theory", html)
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Description, null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: TeacherViewModel,
    onViewNote: (LessonNote) -> Unit,
    onViewMcq: (MCQSet) -> Unit,
    onViewTheory: (TheorySet) -> Unit,
    modifier: Modifier = Modifier
) {
    val notes by viewModel.lessonNotes.collectAsState()
    val mcqs by viewModel.mcqSets.collectAsState()
    val theories by viewModel.theorySets.collectAsState()

    var filterType by remember { mutableStateOf("ALL") } // ALL, NOTE, MCQ, THEORY
    var searchQuery by remember { mutableStateOf("") }

    val aggregatedItems = remember(notes, mcqs, theories, filterType, searchQuery) {
        val list = mutableListOf<Triple<String, Any, Long>>()
        if (filterType == "ALL" || filterType == "NOTE") notes.forEach { list.add(Triple("Lesson Note", it, it.createdAt)) }
        if (filterType == "ALL" || filterType == "MCQ") mcqs.forEach { list.add(Triple("MCQ Set", it, it.createdAt)) }
        if (filterType == "ALL" || filterType == "THEORY") theories.forEach { list.add(Triple("Theory Paper", it, it.createdAt)) }

        val sorted = list.sortedByDescending { it.third }
        if (searchQuery.trim().isEmpty()) sorted
        else sorted.filter { item ->
            when (item.second) {
                is LessonNote -> { val n = item.second as LessonNote; n.topic.contains(searchQuery, ignoreCase = true) || n.subject.contains(searchQuery, ignoreCase = true) }
                is MCQSet -> { val m = item.second as MCQSet; m.topic.contains(searchQuery, ignoreCase = true) || m.subject.contains(searchQuery, ignoreCase = true) }
                else -> { val t = item.second as TheorySet; t.topic.contains(searchQuery, ignoreCase = true) || t.subject.contains(searchQuery, ignoreCase = true) }
            }
        }
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text("Academic Archive", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search topic or subject...") },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            ),
            singleLine = true
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val filters = listOf("ALL" to "All", "NOTE" to "Notes", "MCQ" to "MCQs", "THEORY" to "Theories")
            filters.forEach { (fid, fLabel) ->
                FilterChip(
                    selected = filterType == fid,
                    onClick = { filterType = fid },
                    label = { Text(fLabel) },
                    shape = RoundedCornerShape(12.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                        enabled = true,
                        selected = filterType == fid
                    )
                )
            }
        }

        if (aggregatedItems.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.FindInPage, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f), modifier = Modifier.size(64.dp))
                    Text("No documents found", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 20.dp)
            ) {
                items(aggregatedItems) { item ->
                    val type = item.first
                    val obj = item.second
                    val title: String
                    val subject: String
                    val klass: String
                    val date: Long
                    
                    when (obj) {
                        is LessonNote -> { title = obj.topic; subject = obj.subject; klass = obj.gradeClass; date = obj.createdAt }
                        is MCQSet -> { title = obj.topic; subject = obj.subject; klass = obj.gradeClass; date = obj.createdAt }
                        else -> { val th = obj as TheorySet; title = th.topic; subject = th.subject; klass = th.gradeClass; date = th.createdAt }
                    }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                when (obj) {
                                    is LessonNote -> onViewNote(obj)
                                    is MCQSet -> onViewMcq(obj)
                                    else -> onViewTheory(obj as TheorySet)
                                }
                            },
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
                        shadowElevation = 1.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Surface(
                                modifier = Modifier.size(48.dp),
                                shape = CircleShape,
                                color = when (type) {
                                    "Lesson Note" -> MaterialTheme.colorScheme.primaryContainer
                                    "MCQ Set" -> MaterialTheme.colorScheme.secondaryContainer
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        when (type) {
                                            "Lesson Note" -> Icons.Default.Assignment
                                            "MCQ Set" -> Icons.Default.Quiz
                                            else -> Icons.Default.Create
                                        },
                                        null,
                                        tint = when (type) {
                                            "Lesson Note" -> MaterialTheme.colorScheme.primary
                                            "MCQ Set" -> MaterialTheme.colorScheme.secondary
                                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("$subject • $klass", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            Text(
                                SimpleDateFormat("MMM dd", Locale.US).format(Date(date)),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun TimetableScreen(viewModel: TeacherViewModel) {
    val timetable by viewModel.timetableItems.collectAsState()
    val mySchools by viewModel.teacherSchools.collectAsState()
    val myClasses by viewModel.schoolClasses.collectAsState()
    var openAddDialog by remember { mutableStateOf(false) }

    // Input States
    val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday")
    var selectedDay by remember { mutableStateOf("Monday") }
    var inputSubject by remember { mutableStateOf("") }
    var inputClass by remember { mutableStateOf("") }
    var inputSchool by remember { mutableStateOf("") }
    var inputStartTime by remember { mutableStateOf("08:00") }
    var inputEndTime by remember { mutableStateOf("08:40") }
    val colors = listOf("#0052CC", "#36B37E", "#FFAB00", "#FF5630", "#4C9AFF")
    var selectedColorHex by remember { mutableStateOf(colors[0]) }

    LaunchedEffect(openAddDialog) {
        if (openAddDialog) {
            inputSchool = mySchools.firstOrNull() ?: ""
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Weekly Schedule", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("Manage your teaching sessions", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            
            Button(
                onClick = { openAddDialog = true },
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Slot")
            }
        }

        // Day Selector
        PrimaryTabRow(
            selectedTabIndex = days.indexOf(selectedDay),
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary,
            indicator = { tabPositions ->
                TabRowDefaults.PrimaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[days.indexOf(selectedDay)]),
                    width = 40.dp,
                    shape = RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)
                )
            },
            divider = {}
        ) {
            days.forEach { dayName ->
                Tab(
                    selected = dayName == selectedDay,
                    onClick = { selectedDay = dayName },
                    text = { Text(dayName.substring(0, 3), style = MaterialTheme.typography.labelLarge) }
                )
            }
        }

        val filteredSchedule = timetable.filter { it.dayOfWeek.equals(selectedDay, ignoreCase = true) }

        if (filteredSchedule.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.CalendarToday, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                    Text("No lessons for $selectedDay", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 20.dp)
            ) {
                items(filteredSchedule) { item ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
                        shadowElevation = 1.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .width(4.dp)
                                        .height(48.dp)
                                        .background(
                                            color = try { Color(android.graphics.Color.parseColor(item.colorHex)) } catch (_: Exception) { MaterialTheme.colorScheme.primary },
                                            shape = CircleShape
                                        )
                                )
                                Spacer(modifier = Modifier.width(20.dp))
                                Column {
                                    Text(
                                        text = item.subject,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        textDecoration = if (item.isCompleted) androidx.compose.ui.text.style.TextDecoration.LineThrough else null,
                                        color = if (item.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${item.startTime} - ${item.endTime} • ${item.gradeClass}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = item.isCompleted,
                                    onCheckedChange = { viewModel.toggleTimetableComplete(item) }
                                )
                                IconButton(onClick = { viewModel.deleteTimetableItem(item.id) }) {
                                    Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal dialog
    if (openAddDialog) {
        Dialog(onDismissRequest = { openAddDialog = false }) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Text("Schedule New Slot", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        // School
                        var schExp by remember { mutableStateOf(false) }
                        Box {
                            OutlinedTextField(
                                value = inputSchool,
                                onValueChange = { inputSchool = it },
                                label = { Text("School") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                trailingIcon = { IconButton(onClick = { schExp = !schExp }) { Icon(Icons.Default.ArrowDropDown, null) } }
                            )
                            if (mySchools.isNotEmpty()) {
                                DropdownMenu(expanded = schExp, onDismissRequest = { schExp = false }) {
                                    mySchools.forEach { s -> DropdownMenuItem(text = { Text(s) }, onClick = { inputSchool = s; schExp = false }) }
                                }
                            }
                        }

                        // Class and Subject (Horizontal)
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = inputClass,
                                onValueChange = { inputClass = it },
                                label = { Text("Class") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            OutlinedTextField(
                                value = inputSubject,
                                onValueChange = { inputSubject = it },
                                label = { Text("Subject") },
                                modifier = Modifier.weight(1.5f),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        // Time (Horizontal)
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(value = inputStartTime, onValueChange = { inputStartTime = it }, label = { Text("Start") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp))
                            OutlinedTextField(value = inputEndTime, onValueChange = { inputEndTime = it }, label = { Text("End") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp))
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Tag Color", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            colors.forEach { col ->
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(Color(android.graphics.Color.parseColor(col)), CircleShape)
                                        .border(if (selectedColorHex == col) 2.dp else 0.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                        .clickable { selectedColorHex = col }
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { openAddDialog = false }) { Text("Cancel") }
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(
                            onClick = {
                                if (inputSubject.isNotBlank() && inputClass.isNotBlank()) {
                                    viewModel.addTimetableItem(selectedDay, inputStartTime, inputEndTime, inputSubject, inputClass, inputSchool, selectedColorHex)
                                    openAddDialog = false
                                }
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Save Slot")
                        }
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyllabusScreen(viewModel: TeacherViewModel) {
    val items by viewModel.syllabusItems.collectAsState()
    val teacherType by viewModel.teacherType.collectAsState()
    val teacherSchools by viewModel.teacherSchools.collectAsState()
    val myClasses by viewModel.schoolClasses.collectAsState()

    var filterSchool by remember { mutableStateOf("All Schools") }
    var filterClass by remember { mutableStateOf("All Classes") }
    var filterSubject by remember { mutableStateOf("All Subjects") }
    
    var openAddCourseDialog by remember { mutableStateOf(false) }
    var viewSyllabusItem by remember { mutableStateOf<SyllabusItem?>(null) }
    var editSyllabusItem by remember { mutableStateOf<SyllabusItem?>(null) }

    val showSchoolFilter = teacherType == "PART_TIME" && teacherSchools.size > 1

    val schoolsFilterList = remember(teacherSchools) { listOf("All Schools") + teacherSchools }
    val classesFilterList = remember(myClasses, filterSchool) {
        val filtered = if (filterSchool == "All Schools") myClasses else myClasses.filter { it.schoolName.equals(filterSchool, ignoreCase = true) }
        listOf("All Classes") + filtered.map { it.className }.distinct()
    }
    val subjectsFilterList = remember(myClasses, filterSchool, filterClass) {
        val filtered = myClasses.filter {
            (filterSchool == "All Schools" || it.schoolName.equals(filterSchool, ignoreCase = true)) &&
            (filterClass == "All Classes" || it.className.equals(filterClass, ignoreCase = true))
        }
        listOf("All Subjects") + filtered.map { it.subject }.distinct()
    }

    val finalItems = remember(items, filterSchool, filterClass, filterSubject) {
        items.filter { item ->
            (filterSchool == "All Schools" || item.schoolName.equals(filterSchool, ignoreCase = true)) &&
            (filterClass == "All Classes" || item.gradeClass.equals(filterClass, ignoreCase = true)) &&
            (filterSubject == "All Subjects" || item.subject.equals(filterSubject, ignoreCase = true))
        }
    }

    val compl = finalItems.count { it.isCompleted }
    val progress = if (finalItems.isEmpty()) 0f else compl.toFloat() / finalItems.size.toFloat()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Curriculum Tracker", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("Monitor your teaching progress", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            
            IconButton(
                onClick = { openAddCourseDialog = true },
                modifier = Modifier.background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
            ) {
                Icon(Icons.Default.Add, "Add", tint = MaterialTheme.colorScheme.primary)
            }
        }

        // Progress Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.primary,
            shadowElevation = 4.dp
        ) {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Overall Completion", style = MaterialTheme.typography.titleMedium, color = Color.White)
                    Text("$compl / ${finalItems.size} Topics", style = MaterialTheme.typography.labelLarge, color = Color.White.copy(alpha = 0.8f))
                }
                
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(10.dp).clip(CircleShape),
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = 0.2f)
                )
                
                Text(
                    "${(progress * 100).toInt()}% of the curriculum covered",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }

        // Filters
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Simplified Filter UI for brevity - just Subject for now as an example of modernization
            var subjExp by remember { mutableStateOf(false) }
            Box(modifier = Modifier.weight(1f)) {
                Surface(
                    modifier = Modifier.fillMaxWidth().clickable { subjExp = true },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(filterSubject, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Icon(Icons.Default.ArrowDropDown, null, modifier = Modifier.size(18.dp))
                    }
                }
                DropdownMenu(expanded = subjExp, onDismissRequest = { subjExp = false }) {
                    subjectsFilterList.forEach { s -> DropdownMenuItem(text = { Text(s) }, onClick = { filterSubject = s; subjExp = false }) }
                }
            }

            var clsExp by remember { mutableStateOf(false) }
            Box(modifier = Modifier.weight(1f)) {
                Surface(
                    modifier = Modifier.fillMaxWidth().clickable { clsExp = true },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(filterClass, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Icon(Icons.Default.ArrowDropDown, null, modifier = Modifier.size(18.dp))
                    }
                }
                DropdownMenu(expanded = clsExp, onDismissRequest = { clsExp = false }) {
                    classesFilterList.forEach { c -> DropdownMenuItem(text = { Text(c) }, onClick = { filterClass = c; clsExp = false }) }
                }
            }
        }

        // Topics List
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 20.dp)
        ) {
            items(finalItems) { sItem ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
                    shadowElevation = 1.dp
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(sItem.topic, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Week ${sItem.week} • ${sItem.subject}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(onClick = { viewSyllabusItem = sItem }) { Icon(Icons.Default.Visibility, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp)) }
                            Checkbox(
                                checked = sItem.isCompleted,
                                onCheckedChange = { viewModel.toggleSyllabusComplete(sItem) }
                            )
                            IconButton(onClick = { viewModel.deleteSyllabusItem(sItem.id) }) { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f), modifier = Modifier.size(20.dp)) }
                        }
                    }
                }
            }
        }
    }

    if (openAddCourseDialog) {
        // Modernized Add Dialog
        var addSchool by remember { mutableStateOf(teacherSchools.firstOrNull() ?: "") }
        var addClass by remember { mutableStateOf("") }
        var addSubj by remember { mutableStateOf("") }
        var addTerm by remember { mutableStateOf("Term 1") }
        var addWeek by remember { mutableIntStateOf(1) }
        var addTopicText by remember { mutableStateOf("") }
        var addObjectivesText by remember { mutableStateOf("") }

        Dialog(onDismissRequest = { openAddCourseDialog = false }) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Text("Add Topic", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(value = addTopicText, onValueChange = { addTopicText = it }, label = { Text("Topic Name") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                        
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(value = addSubj, onValueChange = { addSubj = it }, label = { Text("Subject") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp))
                            OutlinedTextField(value = addClass, onValueChange = { addClass = it }, label = { Text("Class") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp))
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(value = "Week $addWeek", onValueChange = {}, label = { Text("Week") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), readOnly = true)
                            OutlinedTextField(value = addTerm, onValueChange = {}, label = { Text("Term") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), readOnly = true)
                        }

                        OutlinedTextField(value = addObjectivesText, onValueChange = { addObjectivesText = it }, label = { Text("Objectives") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), maxLines = 3)
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { openAddCourseDialog = false }) { Text("Cancel") }
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(
                            onClick = {
                                if (addTopicText.isNotBlank()) {
                                    viewModel.addSyllabusItem(addSchool, addClass, addSubj, addTerm, addWeek, "", addTopicText, "", addObjectivesText)
                                    openAddCourseDialog = false
                                }
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Add Topic")
                        }
                    }
                }
            }
        }
    }

    if (viewSyllabusItem != null) {
        val item = viewSyllabusItem!!
        AlertDialog(
            onDismissRequest = { viewSyllabusItem = null },
            title = { Text(item.topic, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("${item.subject} • ${item.gradeClass}", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    Text("Term: ${item.term} • Week ${item.week}", style = MaterialTheme.typography.bodySmall)
                    if (item.objectives.isNotBlank()) {
                        Text("Objectives:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                        Text(item.objectives, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            },
            confirmButton = { TextButton(onClick = { viewSyllabusItem = null }) { Text("Dismiss") } }
        )
    }
    
    // (Edit dialog would be similarly updated, but skipped here for brevity unless specifically needed)
}


// 6. CLASSES & REGISTRIES SCREEN (WORKLOAD)
// ==========================================
@Composable
fun SchoolClassesScreen(viewModel: TeacherViewModel, onViewClass: (SchoolClass) -> Unit) {
    val classes by viewModel.schoolClasses.collectAsState()
    val teacherSchools by viewModel.teacherSchools.collectAsState()

    var openAddClassDialog by remember { mutableStateOf(false) }
    var editingClass by remember { mutableStateOf<SchoolClass?>(null) }

    // State for Adding Class & Subjects
    var addClassName by remember { mutableStateOf("JSS 1") }
    var addSchoolName by remember { mutableStateOf("") }
    val addSubjectsList = remember { mutableStateListOf<String>("") }

    // State for Editing single Class
    var editClassName by remember { mutableStateOf("") }
    var editSchoolName by remember { mutableStateOf("") }
    var editSubjectName by remember { mutableStateOf("") }

    val predefinedClassesList = remember {
        listOf(
            "Nursery 1", "Nursery 2", 
            "Primary 1", "Primary 2", "Primary 3", "Primary 4", "Primary 5", 
            "JSS 1", "JSS 2", "JSS 3", 
            "SSS 1", "SSS 2", "SSS 3"
        )
    }

    LaunchedEffect(openAddClassDialog) {
        if (openAddClassDialog) {
            addClassName = "JSS 1"
            addSchoolName = teacherSchools.firstOrNull() ?: ""
            addSubjectsList.clear()
            addSubjectsList.add("")
        }
    }

    LaunchedEffect(editingClass) {
        editingClass?.let {
            editClassName = it.className
            editSchoolName = it.schoolName
            editSubjectName = it.subject
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Teaching Workload", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("Your registered classes and subjects", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Button(
                onClick = { openAddClassDialog = true },
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Class")
            }
        }

        if (classes.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Groups, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                    Text("No workloads registered yet", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 20.dp)
            ) {
                items(classes) { classroom ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onViewClass(classroom) },
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
                        shadowElevation = 1.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    modifier = Modifier.size(48.dp),
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            classroom.className.take(2).uppercase(),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(classroom.className, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text("${classroom.subject} • ${classroom.schoolName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }

                            Row {
                                IconButton(onClick = { editingClass = classroom }) {
                                    Icon(Icons.Default.Edit, "Edit", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                                }
                                IconButton(onClick = { viewModel.deleteSchoolClass(classroom.id) }) {
                                    Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (openAddClassDialog) {
        Dialog(onDismissRequest = { openAddClassDialog = false }) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Text("Add Workload", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(value = addSchoolName, onValueChange = { addSchoolName = it }, label = { Text("School Name") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))

                        var classExp by remember { mutableStateOf(false) }
                        Box {
                            OutlinedTextField(
                                value = addClassName,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Class Level") },
                                modifier = Modifier.fillMaxWidth().clickable { classExp = true },
                                shape = RoundedCornerShape(12.dp),
                                trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) }
                            )
                            DropdownMenu(expanded = classExp, onDismissRequest = { classExp = false }) {
                                predefinedClassesList.forEach { c -> DropdownMenuItem(text = { Text(c) }, onClick = { addClassName = c; classExp = false }) }
                            }
                        }

                        Text("Subjects", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        addSubjectsList.forEachIndexed { index, s ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(value = s, onValueChange = { addSubjectsList[index] = it }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), placeholder = { Text("e.g. Mathematics") })
                                if (addSubjectsList.size > 1) {
                                    IconButton(onClick = { addSubjectsList.removeAt(index) }) { Icon(Icons.Default.RemoveCircleOutline, null, tint = MaterialTheme.colorScheme.error) }
                                }
                            }
                        }
                        TextButton(onClick = { addSubjectsList.add("") }) {
                            Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Another Subject")
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { openAddClassDialog = false }) { Text("Cancel") }
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(
                            onClick = {
                                if (addSchoolName.isNotBlank()) {
                                    addSubjectsList.forEach { subj ->
                                        if (subj.isNotBlank()) {
                                            viewModel.addSchoolClass(addClassName, addSchoolName, subj)
                                        }
                                    }
                                    openAddClassDialog = false
                                }
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Save Workload")
                        }
                    }
                }
            }
        }
    }

    if (editingClass != null) {
        Dialog(onDismissRequest = { editingClass = null }) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Text("Edit Class Info", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(value = editSchoolName, onValueChange = { editSchoolName = it }, label = { Text("School Name") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                        OutlinedTextField(value = editClassName, onValueChange = { editClassName = it }, label = { Text("Class") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                        OutlinedTextField(value = editSubjectName, onValueChange = { editSubjectName = it }, label = { Text("Subject") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { editingClass = null }) { Text("Cancel") }
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(
                            onClick = {
                                editingClass?.let {
                                    viewModel.updateSchoolClass(it.copy(className = editClassName, schoolName = editSchoolName, subject = editSubjectName))
                                }
                                editingClass = null
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Save Changes")
                        }
                    }
                }
            }
        }
    }
}

// 6B. STUDENT REGISTER & ATTENDANCE SUB-DIALOG OVERLAY
@Composable
fun ClassDetailsDialog(
    schoolClass: SchoolClass,
    viewModel: TeacherViewModel,
    onDismiss: () -> Unit
) {
    val students by viewModel.getStudentsFlow(schoolClass.id).collectAsState(initial = emptyList())
    var openAddStudent by remember { mutableStateOf(false) }

    var stName by remember { mutableStateOf("") }
    var stNotes by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(schoolClass.className, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                        Text(schoolClass.schoolName, fontSize = 11.sp, color = Color.Gray, maxLines = 1)
                    }
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null) }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Students Registry (${students.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    TextButton(onClick = { openAddStudent = true }) {
                        Icon(Icons.Default.PersonAdd, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Student", fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (students.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Text("Add student rosters under this class.", fontSize = 12.sp, color = Color.Gray)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(students) { stud ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1.2f)) {
                                        Text(stud.fullName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        if (stud.performanceNotes.isNotEmpty()) {
                                            Text(stud.performanceNotes, fontSize = 11.sp, color = Color.Gray)
                                        }
                                        
                                        // Attendance tracking score label
                                        val pct = if (stud.totalSessions == 0) 100 else (stud.attendanceCount * 100 / stud.totalSessions)
                                        Text("Attendance: $pct% (${stud.attendanceCount}/${stud.totalSessions})", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    }
                                    
                                    // Attendance scoring buttons row
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Button(
                                            onClick = { viewModel.updateStudentAttendance(stud, true) },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                            modifier = Modifier.size(height = 28.dp, width = 45.dp),
                                            contentPadding = PaddingValues(0.dp)
                                        ) {
                                            Text("Pr", fontSize = 11.sp, color = Color.White)
                                        }
                                        
                                        Button(
                                            onClick = { viewModel.updateStudentAttendance(stud, false) },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                                            modifier = Modifier.size(height = 28.dp, width = 45.dp),
                                            contentPadding = PaddingValues(0.dp)
                                        ) {
                                            Text("Ab", fontSize = 11.sp, color = Color.White)
                                        }

                                        IconButton(onClick = { viewModel.deleteStudent(stud.id) }, modifier = Modifier.size(24.dp)) {
                                            Icon(Icons.Default.Delete, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (openAddStudent) {
        Dialog(onDismissRequest = { openAddStudent = false }) {
            Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Register Student", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    OutlinedTextField(value = stName, onValueChange = { stName = it }, label = { Text("Full Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = stNotes, onValueChange = { stNotes = it }, label = { Text("Pedagogical Notes (e.g. good listener)") }, singleLine = true, modifier = Modifier.fillMaxWidth())

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { openAddStudent = false }) { Text("Cancel") }
                        Button(
                            onClick = {
                                if (stName.trim().isEmpty()) return@Button
                                viewModel.addStudent(schoolClass.id, stName, stNotes)
                                stName = ""; stNotes = ""
                                openAddStudent = false
                            }
                        ) {
                            Text("Add")
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 7. BILLING & SUBSCRIPTIONS SCREEN
// ==========================================
@Composable
fun BillingScreen(viewModel: TeacherViewModel) {
    val context = LocalContext.current
    val subPlan by viewModel.subscriptionPlan.collectAsState()
    val usedGens by viewModel.usageGenerations.collectAsState()
    val currentPlanInfo = getPlanInfo(subPlan)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Column {
            Text("Subscription Plans", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Power up your teaching with AI assistance", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        // Usage meter
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
            shadowElevation = 2.dp
        ) {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Monthly AI Usage", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                
                val maxGen = currentPlanInfo.maxGenerations
                val progress = if (maxGen > 0) usedGens.toFloat() / maxGen.toFloat() else 1f
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("$usedGens / $maxGen units used", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                }
                
                LinearProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                    color = if (progress >= 0.8f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(bottom = 20.dp)
        ) {
            val plans = listOf(PLAN_BASIC, PLAN_ADVANCE, PLAN_PREMIUM)
            items(plans) { planName ->
                val info = getPlanInfo(planName)
                val isActive = subPlan == planName
                val planColor = Color(info.color)
                
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(
                        width = if (isActive) 2.dp else 1.dp,
                        color = if (isActive) planColor else planColor.copy(alpha = 0.1f)
                    ),
                    shadowElevation = if (isActive) 4.dp else 1.dp
                ) {
                    Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                            Column {
                                Badge(
                                    containerColor = planColor.copy(alpha = 0.1f),
                                    contentColor = planColor
                                ) {
                                    Text(info.displayName, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                }
                                Text(info.displayName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                            }
                            
                            Column(horizontalAlignment = Alignment.End) {
                                Text("₦${info.priceNaira}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = planColor)
                                Text("/ month", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            info.features.forEach { feature ->
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Icon(Icons.Default.CheckCircle, null, tint = planColor, modifier = Modifier.size(18.dp))
                                    Text(feature, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }

                        Button(
                            onClick = {
                                if (planName != PLAN_BASIC) {
                                    val activity = context as? android.app.Activity
                                    if (activity != null) {
                                        viewModel.initPaystack(activity)
                                        val user = viewModel.currentUser.value
                                        val email = user?.email ?: ""
                                        viewModel.payForPlan(planName, email) { success, msg ->
                                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                        }
                                    }
                                } else {
                                    viewModel.updatePlan(PLAN_BASIC)
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !isActive,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isActive) MaterialTheme.colorScheme.surfaceVariant else planColor
                            )
                        ) {
                            Text(if (isActive) "Current Plan" else "Subscribe Now", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            
            item {
                TextButton(
                    onClick = { viewModel.resetLimits() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Reset Usage Counter (Test Mode)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}


// ==========================================
// 8. NOTIFICATIONS SCREEN
// ==========================================

@Composable
fun NotificationsScreen(viewModel: TeacherViewModel, onOpenSettings: () -> Unit) {
    val context = LocalContext.current
    val prefs by viewModel.notificationPrefs.collectAsState()
    val notifications by viewModel.notifications.collectAsState()
    
    var showSettingsDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Alert Center", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("Your daily teaching reminders", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = { viewModel.clearNotifications() }, modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), CircleShape)) {
                    Icon(Icons.Default.ClearAll, "Clear", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = { showSettingsDialog = true }, modifier = Modifier.background(MaterialTheme.colorScheme.primaryContainer, CircleShape)) {
                    Icon(Icons.Default.Tune, "Settings", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }

        // Summary row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            NotificationSummaryCard("Wake-up", prefs.wakeUpAlarmEnabled, Icons.Default.Alarm, "${prefs.wakeUpHour}:${"%02d".format(prefs.wakeUpMinute)}", Modifier.weight(1f))
            NotificationSummaryCard("Schedule", prefs.dailyScheduleEnabled, Icons.Default.CalendarToday, "${prefs.dailyScheduleHour}:${"%02d".format(prefs.dailyScheduleMinute)}", Modifier.weight(1f))
            NotificationSummaryCard("Syllabus", prefs.syllabusReminderEnabled, Icons.Default.AutoStories, "${prefs.syllabusReminderHour}:${"%02d".format(prefs.syllabusReminderMinute)}", Modifier.weight(1f))
        }

        if (notifications.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.NotificationsNone, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                    Text("No recent alerts", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 20.dp)
            ) {
                items(notifications) { alert ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.markNotificationRead(alert.id) },
                        shape = RoundedCornerShape(24.dp),
                        color = if (alert.isRead) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
                        shadowElevation = if (alert.isRead) 1.dp else 2.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Surface(
                                modifier = Modifier.size(44.dp),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        when (alert.type) {
                                            NotificationType.WAKE_UP -> Icons.Default.Alarm
                                            NotificationType.SCHEDULE_REMINDER -> Icons.Default.Schedule
                                            NotificationType.MISSED_SCHEDULE -> Icons.Default.ErrorOutline
                                            NotificationType.UNCOMPLETED_NOTE -> Icons.Default.EditNote
                                            NotificationType.DAILY_SCHEDULE -> Icons.Default.Today
                                            NotificationType.SYLLABUS_REMINDER -> Icons.Default.AutoStories
                                        },
                                        null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(alert.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text(alert.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            Text(
                                SimpleDateFormat("HH:mm", Locale.US).format(Date(alert.timestamp)),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showSettingsDialog) {
        NotificationSettingsDialog(viewModel) { showSettingsDialog = false }
    }
}

        // Pending notes alert
        if (uncompletedCount > 0) {
            Card(
                modifier = Modifier.fillMaxWidth().clickable { onOpenSettings() },
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Default.EditNote, null, tint = Color(0xFFE65100), modifier = Modifier.size(24.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Uncompleted Lesson Notes", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFFE65100))
                        Text("$uncompletedCount note(s) pending — finish them in AI Hub", fontSize = 11.sp, color = Color.Gray)
                    }
                    Icon(Icons.Default.ChevronRight, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                }
            }
        }

        // Notification list
        Text("Recent Alerts", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)

        if (notifications.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.NotificationsNone, null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No alerts yet", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    Text("Set up reminders in the alert settings", fontSize = 11.sp, color = Color.Gray)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(notifications) { notif ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { viewModel.markNotificationRead(notif.id) },
                        colors = CardDefaults.cardColors(containerColor = if (!notif.isRead) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                when (notif.type) {
                                    NotificationType.WAKE_UP -> Icons.Default.Alarm
                                    NotificationType.SCHEDULE_REMINDER -> Icons.Default.Schedule
                                    NotificationType.MISSED_SCHEDULE -> Icons.Default.ErrorOutline
                                    NotificationType.UNCOMPLETED_NOTE -> Icons.Default.EditNote
                                    NotificationType.DAILY_SCHEDULE -> Icons.Default.Today
                                    NotificationType.SYLLABUS_REMINDER -> Icons.Default.AutoStories
                                },
                                null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(notif.title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(notif.body, fontSize = 11.sp, color = Color.Gray, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                            val sdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                            Text(sdf.format(java.util.Date(notif.timestamp)), fontSize = 10.sp, color = Color.Gray)
                        }
                    }
                }
            }
        }
    }

    if (showSettingsDialog) {
        NotificationSettingsDialog(
            viewModel = viewModel,
            onDismiss = { showSettingsDialog = false }
        )
    }
}

// 8B. NOTIFICATION SETTINGS DIALOG
@Composable
fun NotificationSettingsDialog(
    viewModel: TeacherViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val currentPrefs by viewModel.notificationPrefs.collectAsState()

    var wakeUpEnabled by remember { mutableStateOf(currentPrefs.wakeUpAlarmEnabled) }
    var wakeUpHour by remember { mutableIntStateOf(currentPrefs.wakeUpHour) }
    var wakeUpMinute by remember { mutableIntStateOf(currentPrefs.wakeUpMinute) }
    var reminderEnabled by remember { mutableStateOf(currentPrefs.scheduleReminderEnabled) }
    var reminderMinutes by remember { mutableIntStateOf(currentPrefs.reminderMinutesBefore) }
    var missedAlerts by remember { mutableStateOf(currentPrefs.missedScheduleAlerts) }
    var uncompletedNotes by remember { mutableStateOf(currentPrefs.uncompletedNotesReminder) }
    var dailyScheduleEnabled by remember { mutableStateOf(currentPrefs.dailyScheduleEnabled) }
    var dailyScheduleHour by remember { mutableIntStateOf(currentPrefs.dailyScheduleHour) }
    var dailyScheduleMinute by remember { mutableIntStateOf(currentPrefs.dailyScheduleMinute) }
    var syllabusReminderEnabled by remember { mutableStateOf(currentPrefs.syllabusReminderEnabled) }
    var syllabusReminderHour by remember { mutableIntStateOf(currentPrefs.syllabusReminderHour) }
    var syllabusReminderMinute by remember { mutableIntStateOf(currentPrefs.syllabusReminderMinute) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Text("Alert & Alarm Settings", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)

                // Wake-up alarm
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.Alarm, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                Text("Wake-up Alarm", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Switch(checked = wakeUpEnabled, onCheckedChange = { wakeUpEnabled = it })
                        }
                        if (wakeUpEnabled) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Time:", fontSize = 12.sp)
                                // Hour picker
                                var showHourPicker by remember { mutableStateOf(false) }
                                OutlinedButton(onClick = { showHourPicker = true }, modifier = Modifier.weight(1f)) {
                                    Text("%02d".format(wakeUpHour), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                                Text(":", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                var showMinutePicker by remember { mutableStateOf(false) }
                                OutlinedButton(onClick = { showMinutePicker = true }, modifier = Modifier.weight(1f)) {
                                    Text("%02d".format(wakeUpMinute), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Text("Daily alarm at ${"%02d".format(wakeUpHour)}:${"%02d".format(wakeUpMinute)}", fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                }

                // Schedule reminders
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.Schedule, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                Text("Schedule Reminders", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Switch(checked = reminderEnabled, onCheckedChange = { reminderEnabled = it })
                        }
                        if (reminderEnabled) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("Remind me", fontSize = 12.sp)
                                var showMinPicker by remember { mutableStateOf(false) }
                                OutlinedButton(onClick = { showMinPicker = true }) {
                                    Text("$reminderMinutes", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                                Text("minutes before class", fontSize = 12.sp)
                            }
                        }
                    }
                }

                // Daily teaching schedule notification
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.Today, null, tint = Color(0xFF1565C0), modifier = Modifier.size(20.dp))
                                Column {
                                    Text("Daily Teaching Schedule", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("Morning summary: schools, periods, subjects & hours", fontSize = 10.sp, color = Color.Gray)
                                }
                            }
                            Switch(checked = dailyScheduleEnabled, onCheckedChange = { dailyScheduleEnabled = it })
                        }
                        if (dailyScheduleEnabled) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("At:", fontSize = 12.sp)
                                OutlinedButton(onClick = { dailyScheduleHour = (dailyScheduleHour + 1) % 24 }, modifier = Modifier.weight(1f)) {
                                    Text("%02d".format(dailyScheduleHour), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                                Text(":", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                OutlinedButton(onClick = { dailyScheduleMinute = (dailyScheduleMinute + 5) % 60 }, modifier = Modifier.weight(1f)) {
                                    Text("%02d".format(dailyScheduleMinute), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Text("Daily at ${"%02d".format(dailyScheduleHour)}:${"%02d".format(dailyScheduleMinute)}", fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                }

                // Syllabus lesson notes reminder
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.AutoStories, null, tint = Color(0xFF6A1B9A), modifier = Modifier.size(20.dp))
                                Column {
                                    Text("Syllabus Notes Reminder", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("Alerts for topics without lesson notes", fontSize = 10.sp, color = Color.Gray)
                                }
                            }
                            Switch(checked = syllabusReminderEnabled, onCheckedChange = { syllabusReminderEnabled = it })
                        }
                        if (syllabusReminderEnabled) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("At:", fontSize = 12.sp)
                                OutlinedButton(onClick = { syllabusReminderHour = (syllabusReminderHour + 1) % 24 }, modifier = Modifier.weight(1f)) {
                                    Text("%02d".format(syllabusReminderHour), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                                Text(":", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                OutlinedButton(onClick = { syllabusReminderMinute = (syllabusReminderMinute + 5) % 60 }, modifier = Modifier.weight(1f)) {
                                    Text("%02d".format(syllabusReminderMinute), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Text("Daily at ${"%02d".format(syllabusReminderHour)}:${"%02d".format(syllabusReminderMinute)}", fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                }

                // Other toggles
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.ErrorOutline, null, tint = Color(0xFFC62828), modifier = Modifier.size(20.dp))
                                Text("Missed Schedule Alerts", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Switch(checked = missedAlerts, onCheckedChange = { missedAlerts = it })
                        }
                        Divider()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.EditNote, null, tint = Color(0xFFE65100), modifier = Modifier.size(20.dp))
                                Text("Uncompleted Notes Reminder", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Switch(checked = uncompletedNotes, onCheckedChange = { uncompletedNotes = it })
                        }
                    }
                }

                // Save/Cancel
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = {
                        val prefs = NotificationPrefs(
                            wakeUpAlarmEnabled = wakeUpEnabled,
                            wakeUpHour = wakeUpHour,
                            wakeUpMinute = wakeUpMinute,
                            scheduleReminderEnabled = reminderEnabled,
                            reminderMinutesBefore = reminderMinutes,
                            missedScheduleAlerts = missedAlerts,
                            uncompletedNotesReminder = uncompletedNotes,
                            dailyScheduleEnabled = dailyScheduleEnabled,
                            dailyScheduleHour = dailyScheduleHour,
                            dailyScheduleMinute = dailyScheduleMinute,
                            syllabusReminderEnabled = syllabusReminderEnabled,
                            syllabusReminderHour = syllabusReminderHour,
                            syllabusReminderMinute = syllabusReminderMinute
                        )
                        viewModel.saveNotificationPrefs(prefs)
                        viewModel.cacheDataForNotifications()

                        NotificationHelper.createChannels(context)

                        if (wakeUpEnabled) {
                            AlarmScheduler.scheduleWakeUpAlarm(context, wakeUpHour, wakeUpMinute)
                        } else {
                            AlarmScheduler.cancelWakeUpAlarm(context)
                        }

                        AlarmScheduler.scheduleDailyWork(context)

                        viewModel.addNotification(
                            AppNotification(
                                id = NotificationIds.notifCounter.getAndIncrement(),
                                title = "Alert Settings Updated",
                                body = "Your notification preferences have been saved.",
                                type = NotificationType.DAILY_SCHEDULE
                            )
                        )

                        onDismiss()
                    }) {
                        Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save Preferences")
                    }
                }
            }
        }
    }
}

// ==========================================
// 9. SETTINGS SCREEN (accessible from header)
// ==========================================

@Composable
fun SettingsScreen(viewModel: TeacherViewModel) {
    val con = LocalContext.current
    val name by viewModel.teacherName.collectAsState()
    val type by viewModel.teacherType.collectAsState()
    val schools by viewModel.teacherSchools.collectAsState()

    val userAccount by viewModel.currentUser.collectAsState()
    var isEditingProfile by remember { mutableStateOf(false) }

    var editName by remember { mutableStateOf("") }
    var editType by remember { mutableStateOf("FULL_TIME") }

    LaunchedEffect(isEditingProfile, userAccount) {
        if (isEditingProfile) {
            val user = userAccount
            editName = user?.fullName ?: name
            editType = user?.teachingStatus ?: type
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            Column {
                Text("Settings & Account", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("Manage your profile and application preferences", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        // Profile Section
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
                shadowElevation = 1.dp
            ) {
                Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Educator Profile", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        IconButton(onClick = { isEditingProfile = !isEditingProfile }) {
                            Icon(if (isEditingProfile) Icons.Default.Close else Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }

                    if (!isEditingProfile) {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            ProfileInfoItem(Icons.Default.Person, "Full Name", userAccount?.fullName ?: name)
                            ProfileInfoItem(Icons.Default.Email, "Email Address", userAccount?.email ?: "Guest mode")
                            ProfileInfoItem(Icons.Default.Work, "Teaching Status", if ((userAccount?.teachingStatus ?: type) == "FULL_TIME") "Full-Time" else "Part-Time")
                            
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Icon(Icons.Default.Business, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                                    Text("Institutions", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                }
                                if (schools.isEmpty()) {
                                    Text("No schools registered", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 32.dp))
                                } else {
                                    schools.forEach { s ->
                                        Text("• $s", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 32.dp))
                                    }
                                }
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            OutlinedTextField(value = editName, onValueChange = { editName = it }, label = { Text("Full Name") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                            
                            Text("Teaching Status", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                FilterChip(
                                    selected = editType == "FULL_TIME",
                                    onClick = { editType = "FULL_TIME" },
                                    label = { Text("Full-Time", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                FilterChip(
                                    selected = editType == "PART_TIME",
                                    onClick = { editType = "PART_TIME" },
                                    label = { Text("Part-Time", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Button(
                                onClick = {
                                    viewModel.updateProfile(editName, editType)
                                    isEditingProfile = false
                                },
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Save Profile Changes", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // App Actions
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
                shadowElevation = 1.dp
            ) {
                Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Application Actions", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    
                    TextButton(
                        onClick = { viewModel.resetOnboarding() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start, verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Refresh, null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(16.dp))
                            Text("Reset & Re-Onboard")
                        }
                    }

                    TextButton(
                        onClick = { viewModel.logout() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start, verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Logout, null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(16.dp))
                            Text("Sign Out")
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
fun LessonDetailsDialog(note: LessonNote, onDismiss: () -> Unit, onDelete: () -> Unit) {
    val con = LocalContext.current
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.85f),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Lesson Plan", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null) }
                }

                LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(note.topic, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text("${note.subject} • ${note.gradeClass} • ${note.duration}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(note.presentation, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                Row(modifier = Modifier.fillMaxWidth().padding(top = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    IconButton(onClick = onDelete, modifier = Modifier.background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f), CircleShape)) {
                        Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                    
                    Spacer(modifier = Modifier.weight(1f))

                    Surface(
                        modifier = Modifier.size(48.dp).clickable {
                            val html = "<h1>${note.subject}</h1><p>${note.presentation}</p>"
                            ExportService.exportToPdf(con, note.topic, html)
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                    ) {
                        Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.PictureAsPdf, null, tint = MaterialTheme.colorScheme.primary) }
                    }

                    Button(
                        onClick = {
                            val html = "<h1>${note.subject}</h1><p>${note.presentation}</p>"
                            ExportService.exportToWord(con, note.topic, html)
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Description, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Export Word")
                    }
                }
            }
        }
    }
}

@Composable
fun McqDetailsDialog(mcqSet: MCQSet, onDismiss: () -> Unit, onDelete: () -> Unit) {
    val con = LocalContext.current
    val moshi = remember { Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build() }
    val parsedQuestions = remember(mcqSet.questionsJson) {
        try {
            val listType = Types.newParameterizedType(List::class.java, McqModel::class.java)
            val adapter = moshi.adapter<List<McqModel>>(listType)
            adapter.fromJson(mcqSet.questionsJson) ?: emptyList()
        } catch (e: Exception) { emptyList() }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.85f),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("MCQ Exam", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null) }
                }

                Text(mcqSet.topic, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("${mcqSet.subject} • ${mcqSet.gradeClass}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f).padding(top = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(parsedQuestions) { item ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(item.question, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("A. ${item.optionA}", style = MaterialTheme.typography.bodySmall)
                                    Text("B. ${item.optionB}", style = MaterialTheme.typography.bodySmall)
                                    Text("C. ${item.optionC}", style = MaterialTheme.typography.bodySmall)
                                    Text("D. ${item.optionD}", style = MaterialTheme.typography.bodySmall)
                                }
                                Text("Correct: ${item.correctAnswer}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth().padding(top = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    IconButton(onClick = onDelete, modifier = Modifier.background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f), CircleShape)) {
                        Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                    
                    Spacer(modifier = Modifier.weight(1f))

                    Button(
                        onClick = {
                            val exportList = parsedQuestions.map {
                                McqExportItem(it.question, it.optionA, it.optionB, it.optionC, it.optionD, it.correctAnswer, it.explanation)
                            }
                            ExportService.exportToExcel(con, "${mcqSet.topic}_MCQ", exportList)
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.GridOn, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Export Excel")
                    }
                }
            }
        }
    }
}

@Composable
fun TheoryDetailsDialog(theorySet: TheorySet, onDismiss: () -> Unit, onDelete: () -> Unit) {
    val con = LocalContext.current
    val moshi = remember { Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build() }
    val parsedTheories = remember(theorySet.questionsJson) {
        try {
            val listType = Types.newParameterizedType(List::class.java, TheoryModel::class.java)
            val adapter = moshi.adapter<List<TheoryModel>>(listType)
            adapter.fromJson(theorySet.questionsJson) ?: emptyList()
        } catch (e: Exception) { emptyList() }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.85f),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Theory Exam", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null) }
                }

                Text(theorySet.topic, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("${theorySet.subject} • ${theorySet.gradeClass}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f).padding(top = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(parsedTheories) { item ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(item.question, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text("Marking Guide:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                Text(item.markingScheme, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth().padding(top = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    IconButton(onClick = onDelete, modifier = Modifier.background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f), CircleShape)) {
                        Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                    
                    Spacer(modifier = Modifier.weight(1f))

                    Button(
                        onClick = {
                            val html = "<h1>${theorySet.subject}</h1>" + parsedTheories.joinToString("<br>") { "<h3>${it.question}</h3><p>${it.markingScheme}</p>" }
                            ExportService.exportToWord(con, "${theorySet.topic}_Theory", html)
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Description, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Export Word")
                    }
                }
            }
        }
    }
}

@Composable
fun TheoryDetailsDialog(theorySet: TheorySet, onDismiss: () -> Unit, onDelete: () -> Unit) {
    val con = LocalContext.current
    val moshi = remember { Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build() }
    
    val parsedTheories = remember(theorySet.questionsJson) {
        try {
            val listType = Types.newParameterizedType(List::class.java, TheoryModel::class.java)
            val adapter = moshi.adapter<List<TheoryModel>>(listType)
            adapter.fromJson(theorySet.questionsJson) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.85f).padding(8.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Essay Exam view", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null) }
                }

                Text(theorySet.title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text("${theorySet.subject} · ${theorySet.gradeClass}", fontSize = 12.sp, color = Color.Gray)

                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(parsedTheories) { tItem ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(tItem.question, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Marking scheme guideline:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Text(tItem.markingScheme, fontSize = 11.sp, color = Color.DarkGray)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, "Delete", tint = Color.Red.copy(alpha = 0.8f))
                    }

                    Button(
                        onClick = {
                            val html = "<h1>${theorySet.subject} Essay Papers</h1><h2>Class: ${theorySet.gradeClass}</h2>" +
                                    parsedTheories.joinToString("<hr>") { "<h3>Question: ${it.question}</h3><p><b>Marking Guide:</b><br>${it.markingScheme}</p>" }
                            ExportService.exportToWord(con, theorySet.topic, html)
                        },
                        modifier = Modifier.size(height = 36.dp, width = 110.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("DOCX Word", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(viewModel: TeacherViewModel) {
    var isLoginMode by remember { mutableStateOf(true) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .widthIn(max = 400.dp),
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Surface(
                    modifier = Modifier.size(64.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                    }
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Teacher’s Companion", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(if (isLoginMode) "Welcome back, educator" else "Create your professional account", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                }

                if (errorMessage.isNotEmpty()) {
                    Surface(color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f), shape = RoundedCornerShape(12.dp)) {
                        Text(errorMessage, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(12.dp))
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it; errorMessage = "" },
                        label = { Text("Email Address") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it; errorMessage = "" },
                        label = { Text("Password") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation()
                    )
                }

                if (isLoading) {
                    CircularProgressIndicator(strokeWidth = 3.dp)
                } else {
                    Button(
                        onClick = {
                            if (email.isBlank() || password.isBlank()) {
                                errorMessage = "Please enter credentials"
                                return@Button
                            }
                            isLoading = true
                            if (isLoginMode) viewModel.login(email, password) { success, msg -> isLoading = false; if (!success) errorMessage = msg }
                            else viewModel.register(email, password) { success, msg -> isLoading = false; if (!success) errorMessage = msg }
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(if (isLoginMode) "Sign In" else "Get Started", fontWeight = FontWeight.Bold)
                    }

                    TextButton(onClick = { isLoginMode = !isLoginMode; errorMessage = "" }) {
                        Text(if (isLoginMode) "New here? Create account" else "Already have an account? Sign In")
                    }
                }
            }
        }
    }
}

@Composable
fun OnboardingScreen(viewModel: TeacherViewModel) {
    var currentStep by remember { mutableStateOf(1) }
    var fullName by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("FULL_TIME") }
    val schools = remember { mutableStateListOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().widthIn(max = 500.dp),
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 4.dp
        ) {
            Column(modifier = Modifier.padding(32.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                LinearProgressIndicator(
                    progress = { currentStep.toFloat() / 3f },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape)
                )

                when (currentStep) {
                    1 -> {
                        Text("Let's get to know you", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        OutlinedTextField(
                            value = fullName,
                            onValueChange = { fullName = it },
                            label = { Text("Your Full Name") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
                        )
                    }
                    2 -> {
                        Text("Teaching Status", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Surface(
                                modifier = Modifier.weight(1f).clickable { status = "FULL_TIME" },
                                shape = RoundedCornerShape(16.dp),
                                color = if (status == "FULL_TIME") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = if (status == "FULL_TIME") BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
                            ) {
                                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.Work, null)
                                    Text("Full-Time", style = MaterialTheme.typography.labelLarge)
                                }
                            }
                            Surface(
                                modifier = Modifier.weight(1f).clickable { status = "PART_TIME" },
                                shape = RoundedCornerShape(16.dp),
                                color = if (status == "PART_TIME") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = if (status == "PART_TIME") BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
                            ) {
                                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.Groups, null)
                                    Text("Part-Time", style = MaterialTheme.typography.labelLarge)
                                }
                            }
                        }
                    }
                    3 -> {
                        Text("Your Institutions", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        if (schools.isEmpty()) { schools.add("") }
                        schools.forEachIndexed { i, s ->
                            OutlinedTextField(
                                value = s,
                                onValueChange = { schools[i] = it },
                                label = { Text("School Name #${i+1}") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp)
                            )
                        }
                        if (status == "PART_TIME") {
                            TextButton(onClick = { schools.add("") }) { Text("+ Add Another School") }
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    if (currentStep > 1) {
                        TextButton(onClick = { currentStep-- }) { Text("Back") }
                    } else { Spacer(Modifier.width(10.dp)) }
                    
                    Button(
                        onClick = {
                            if (currentStep < 3) currentStep++
                            else {
                                val sList = schools.filter { it.isNotBlank() }
                                viewModel.finishOnboarding(fullName, status, sList, emptyList())
                            }
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (currentStep == 3) "Launch Companion" else "Continue")
                    }
                }
            }
        }
    }
}
