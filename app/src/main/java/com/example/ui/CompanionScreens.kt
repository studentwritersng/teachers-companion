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
        val onQuickAction: (String, String) -> Unit = { tab, presetType ->
            currentTab = tab
        }

        Scaffold(
            topBar = {
                val isDark by viewModel.isDarkMode.collectAsState()
                TopAppBar(
                    title = {
                        Image(
                            painter = painterResource(id = R.drawable.app_logo_1779831330845),
                            contentDescription = "Logo",
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        scrolledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    actions = {
                        // 1. Theme switch icon
                        IconButton(onClick = { viewModel.toggleDarkMode() }) {
                            Icon(
                                imageVector = if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = "Toggle Theme",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        
                        // 2. Support icon
                        IconButton(onClick = { showSupportDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Help,
                                contentDescription = "Support",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        
                        // 3. Profile icon
                        IconButton(onClick = { showProfileDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = "Profile",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        
                        // 4. Settings icon
                        IconButton(onClick = { currentTab = "settings" }) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = Color(0xFF04060A),
                    tonalElevation = 0.dp,
                    windowInsets = WindowInsets.navigationBars
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
                                    fontSize = 10.sp, 
                                    maxLines = 1,
                                    color = if (isDark) {
                                        if (isSelected) Color(0xFF5C93FC) else Color(0xFF5C93FC).copy(alpha = 0.5f)
                                    } else {
                                        if (isSelected) Color.White else Color.White.copy(alpha = 0.6f)
                                    }
                                ) 
                            },
                            icon = { 
                                Icon(
                                    imageVector = icon, 
                                    contentDescription = label,
                                    tint = if (isDark) {
                                        if (isSelected) Color(0xFF5C93FC) else Color(0xFF5C93FC).copy(alpha = 0.5f)
                                    } else {
                                        if (isSelected) Color.White else Color.White.copy(alpha = 0.6f)
                                    }
                                ) 
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = if (isDark) Color(0xFF5C93FC) else Color.White,
                                selectedTextColor = if (isDark) Color(0xFF5C93FC) else Color.White,
                                unselectedIconColor = if (isDark) Color(0xFF5C93FC).copy(alpha = 0.5f) else Color.White.copy(alpha = 0.6f),
                                unselectedTextColor = if (isDark) Color(0xFF5C93FC).copy(alpha = 0.5f) else Color.White.copy(alpha = 0.6f),
                                indicatorColor = if (isDark) Color(0xFF5C93FC).copy(alpha = 0.15f) else Color.White.copy(alpha = 0.2f)
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
                        onViewTheory = { activeTheoryDetails = it }
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
    val currentDayName = remember {
        val sdf = SimpleDateFormat("EEEE", Locale.US)
        sdf.format(Date())
    }

    val todaysLessons = remember(timetable) {
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
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Quick AI Co-Pilot Actions (horizontal, sleek row)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "AI GENERATOR HUB",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(start = 4.dp)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Lesson Notes Card
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onQuickAction("ai_hub", "lesson") },
                        shape = RoundedCornerShape(6.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.23f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp).fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Assignment,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Lesson Notes",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // MCQ Generator Card
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onQuickAction("ai_hub", "mcq") },
                        shape = RoundedCornerShape(6.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.23f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f))
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp).fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Quiz,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "MCQ Gen",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Essay Paper Card
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onQuickAction("ai_hub", "theory") },
                        shape = RoundedCornerShape(6.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp).fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Create,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Essay Paper",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
        
        // Today's schedule card with custom sleek theme-aware styling
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(6.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Today's Schedule",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        TextButton(
                            onClick = { onQuickAction("timetable", "") },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("VIEW ALL", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    if (todaysLessons.isEmpty()) {
                        if (nextLesson != null) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("No periods today. Next upcoming slot:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                        .border(BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)), RoundedCornerShape(6.dp))
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(
                                        modifier = Modifier.width(60.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = nextLesson.startTime,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = nextLesson.dayOfWeek.take(3).uppercase(),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Box(
                                        modifier = Modifier
                                            .width(3.dp)
                                            .height(34.dp)
                                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(1.dp))
                                    )
                                    Column(modifier = Modifier.padding(start = 10.dp)) {
                                        Text(
                                            text = "${nextLesson.subject} (${nextLesson.gradeClass})",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "School: ${nextLesson.schoolName}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                    .border(BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f)), RoundedCornerShape(6.dp))
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.School,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("No periods scheduled", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                                    Text("Set up your school sessions in the Timetable tab.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                }
                            }
                        }
                    } else {
                        todaysLessons.forEachIndexed { index, lesson ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier.width(55.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = lesson.startTime,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "TODAY",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                                    )
                                }
                                
                                Spacer(modifier = Modifier.width(10.dp))
                                
                                val indicatorColor = if (index == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                                
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f), RoundedCornerShape(6.dp))
                                        .border(BorderStroke(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)), RoundedCornerShape(6.dp)),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(3.dp)
                                            .height(34.dp)
                                            .background(indicatorColor, RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp))
                                    )
                                    
                                    Column(modifier = Modifier.padding(start = 10.dp, top = 6.dp, bottom = 6.dp, end = 8.dp)) {
                                        Text(
                                            text = "${lesson.subject} (${lesson.gradeClass})",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "School: ${lesson.schoolName}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // Syllabus Completion Card (Theme-conforming layout)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onQuickAction("syllabus", "") },
                shape = RoundedCornerShape(6.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "LESSON NOTES COMPLETED",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Syllabus Progress Tracker",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                        Text(
                            text = "${(percentageSyllabus * 100).toInt()}% of syllabus topics completed",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(52.dp)) {
                        CircularProgressIndicator(
                            progress = { percentageSyllabus },
                            strokeWidth = 4.dp,
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            modifier = Modifier.size(44.dp)
                        )
                        Text(
                            text = "${(percentageSyllabus * 100).toInt()}%",
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }

        // Lesson Note Action Section Title
        item {
            Text(
                text = "Pending Lesson Notes Tracker",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 4.dp, start = 4.dp)
            )
        }

        if (uncompletedSyllabus.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(6.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("All lesson notes generated.", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        } else {
            items(uncompletedSyllabus) { progress ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(6.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = progress.topic,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${progress.subject} · ${progress.gradeClass} · ${progress.term}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                        
                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                                .clickable { onQuickAction("ai_hub", "note") }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Draft Note",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
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
    onViewTheory: (TheorySet) -> Unit = {}
) {
    val context = LocalContext.current
    var innerTab by remember { mutableStateOf("note") } // note, mcq, theory, history

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
            .padding(16.dp)
    ) {
        // Selector Header Tabs
        TabRow(
            selectedTabIndex = when (innerTab) {
                "note" -> 0
                "mcq" -> 1
                "theory" -> 2
                else -> 3
            },
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            Tab(selected = innerTab == "note", onClick = { innerTab = "note" }, text = { Text("Note", fontSize = 11.sp, maxLines = 1) }, modifier = Modifier.testTag("ai_tab_note"))
            Tab(selected = innerTab == "mcq", onClick = { innerTab = "mcq" }, text = { Text("MCQs", fontSize = 11.sp, maxLines = 1) }, modifier = Modifier.testTag("ai_tab_mcq"))
            Tab(selected = innerTab == "theory", onClick = { innerTab = "theory" }, text = { Text("Theory", fontSize = 11.sp, maxLines = 1) }, modifier = Modifier.testTag("ai_tab_theory"))
            Tab(selected = innerTab == "history", onClick = { innerTab = "history" }, text = { Text("History", fontSize = 11.sp, maxLines = 1) }, modifier = Modifier.testTag("ai_tab_history"))
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (innerTab == "history") {
            HistoryScreen(
                viewModel = viewModel,
                onViewNote = onViewNote,
                onViewMcq = onViewMcq,
                onViewTheory = onViewTheory,
                modifier = Modifier.fillMaxSize().padding(top = 4.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
            // SHARED PARAMETERS INPUT BLOCK
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Academic Curriculum Alignment", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // School Select (shows registered schools list)
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = selectedSchool,
                                    onValueChange = {},
                                    readOnly = true,
                                    enabled = false,
                                    label = { Text("Selected School") },
                                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { schoolExpanded = true },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                                DropdownMenu(expanded = schoolExpanded, onDismissRequest = { schoolExpanded = false }) {
                                    mySchools.forEach { schoolItem ->
                                        DropdownMenuItem(text = { Text(schoolItem) }, onClick = { selectedSchool = schoolItem; schoolExpanded = false })
                                    }
                                }
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                // Class Select
                                Box(modifier = Modifier.weight(1f)) {
                                    OutlinedTextField(
                                        value = selectedClass,
                                        onValueChange = {},
                                        readOnly = true,
                                        enabled = false,
                                        label = { Text("Class") },
                                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { classExpanded = true },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                            disabledBorderColor = MaterialTheme.colorScheme.outline,
                                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                            disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
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
                                        enabled = false,
                                        label = { Text("Subject") },
                                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { subjExpanded = true },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                            disabledBorderColor = MaterialTheme.colorScheme.outline,
                                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                            disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                    DropdownMenu(expanded = subjExpanded, onDismissRequest = { subjExpanded = false }) {
                                        availableSubjects.forEach { s ->
                                            DropdownMenuItem(text = { Text(s) }, onClick = { selectedSubject = s; subjExpanded = false })
                                        }
                                    }
                                }
                            }
                        }

                        // Topic Selection Dropdown (instead of input field!)
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = topicText,
                                onValueChange = {},
                                readOnly = true,
                                enabled = false,
                                label = { Text("Select Syllabus Topic") },
                                trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { topicExpanded = true }
                                    .testTag("ai_topic_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                            DropdownMenu(
                                expanded = topicExpanded,
                                onDismissRequest = { topicExpanded = false },
                                modifier = Modifier.fillMaxWidth(0.9f)
                            ) {
                                if (matchingSyllabusItems.isEmpty()) {
                                    DropdownMenuItem(
                                        text = { Text("No topics found. Please add to syllabus first!", color = Color.Gray) },
                                        onClick = { topicExpanded = false }
                                    )
                                } else {
                                    matchingSyllabusItems.forEach { item ->
                                        DropdownMenuItem(
                                            text = { 
                                                Column {
                                                    Text(item.topic, fontWeight = FontWeight.Bold)
                                                    if (item.objectives.isNotEmpty()) {
                                                        Text("Objectives: ${item.objectives}", fontSize = 10.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
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

                        // 1. LESSON NOTE ADDS
                        if (innerTab == "note") {
                            OutlinedTextField(
                                value = durationText,
                                onValueChange = { durationText = it },
                                label = { Text("Duration") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                            )

                            OutlinedTextField(
                                value = lessonCustomInstructions,
                                onValueChange = { lessonCustomInstructions = it },
                                label = { Text("Custom Notes or Guidelines (Optional)") },
                                placeholder = { Text("e.g. Include local examples from Lagos markets") },
                                modifier = Modifier.fillMaxWidth(),
                                maxLines = 2,
                                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                            )
                        }

                        // 2. MCQ ADDS
                        if (innerTab == "mcq") {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.weight(1.5f)) {
                                    OutlinedTextField(
                                        value = selectedDifficulty,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Difficulty") },
                                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Box(
                                        modifier = Modifier
                                            .matchParentSize()
                                            .clickable { diffExpanded = true }
                                    )
                                    DropdownMenu(expanded = diffExpanded, onDismissRequest = { diffExpanded = false }) {
                                        difficulties.forEach { d ->
                                            DropdownMenuItem(text = { Text(d) }, onClick = { selectedDifficulty = d; diffExpanded = false })
                                        }
                                    }
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Questions: $mcqCount", fontSize = 11.sp, color = Color.Gray)
                                    Slider(
                                        value = mcqCount.toFloat(),
                                        onValueChange = { mcqCount = it.toInt().coerceIn(3, 50) },
                                        valueRange = 3f..50f,
                                        steps = 46
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.MenuBook, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                Text("Rely on lesson notes:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text("$mcqLessonReliance%", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            }
                            Slider(
                                value = mcqLessonReliance.toFloat(),
                                onValueChange = { mcqLessonReliance = it.toInt().coerceIn(0, 100) },
                                valueRange = 0f..100f,
                                steps = 19
                            )
                            Text("${100 - mcqLessonReliance}% from AI knowledge", fontSize = 10.sp, color = Color.Gray, modifier = Modifier.align(Alignment.End))
                        }

                        // 3. THEORY ADDS
                        if (innerTab == "theory") {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text("Theory essay questions to generate: $theoryCount", fontSize = 12.sp, color = Color.Gray)
                                Slider(
                                    value = theoryCount.toFloat(),
                                    onValueChange = { theoryCount = it.toInt().coerceIn(2, 10) },
                                    valueRange = 2f..10f,
                                    steps = 7
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.MenuBook, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                    Text("Rely on lesson notes:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text("$theoryLessonReliance%", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                }
                                Slider(
                                    value = theoryLessonReliance.toFloat(),
                                    onValueChange = { theoryLessonReliance = it.toInt().coerceIn(0, 100) },
                                    valueRange = 0f..100f,
                                    steps = 19
                                )
                                Text("${100 - theoryLessonReliance}% from AI knowledge", fontSize = 10.sp, color = Color.Gray, modifier = Modifier.align(Alignment.End))
                            }
                        }

                        // SUBMIT GENERATION BUTTON
                        Button(
                            onClick = {
                                if (topicText.trim().isEmpty()) {
                                    Toast.makeText(context, "Please select or provide a topic!", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                val matchedItem = matchingSyllabusItems.find { it.topic.equals(topicText, ignoreCase = true) }
                                val sObjectives = matchedItem?.objectives ?: ""
                                val sContent = matchedItem?.content ?: ""
                                val sTheme = matchedItem?.theme ?: ""
                                
                                when (innerTab) {
                                    "note" -> viewModel.generateLessonNote(
                                        subject = selectedSubject,
                                        gradeClass = selectedClass,
                                        topic = topicText,
                                        duration = durationText,
                                        syllabusObjectives = sObjectives,
                                        syllabusContent = sContent,
                                        customInstructions = lessonCustomInstructions,
                                        theme = sTheme
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
                                .height(50.dp)
                                .testTag("generate_fab_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = when (innerTab) {
                                    "note" -> "Generate Lesson Note"
                                    "mcq" -> "Generate MCQs"
                                    else -> "Generate Theory Questions"
                                },
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
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(24.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Drafting Lesson Plan structure...", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Structuring behavioral objectives, presentations steps, and assessments tailored to West African schools...", fontSize = 11.sp, textAlign = TextAlign.Center, color = Color.Gray, modifier = Modifier.padding(horizontal = 16.dp))
                }
            }
        }
        is AiGenerationState.Error -> {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("AI Error", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                    Text((state as AiGenerationState.Error).message, fontSize = 12.sp, color = MaterialTheme.colorScheme.onErrorContainer)
                    TextButton(onClick = { viewModel.clearLessonState() }) { Text("Dismiss", color = MaterialTheme.colorScheme.onErrorContainer) }
                }
            }
        }
        is AiGenerationState.Success -> {
            val content = (state as AiGenerationState.Success<String>).data
            var editableText by remember { mutableStateOf(content) }
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Draft Lesson Completed", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.primary)
                        Row {
                            IconButton(onClick = { viewModel.clearLessonState() }) { Icon(Icons.Default.Clear, "Close") }
                        }
                    }

                    // Native Rich text area editable before saving!
                    OutlinedTextField(
                        value = editableText,
                        onValueChange = { editableText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 250.dp, max = 400.dp),
                        label = { Text("Interactive Draft (Edit as needed)") },
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Save local History
                        Button(
                            onClick = {
                                viewModel.saveGeneratedLessonNote(subject, gradeClass, topic, duration, editableText)
                                Toast.makeText(con, "Saved locally to History!", Toast.LENGTH_SHORT).show()
                                viewModel.clearLessonState()
                            },
                            modifier = Modifier.weight(1.2f)
                        ) {
                            Icon(Icons.Default.Save, null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save Plan", fontSize = 12.sp)
                        }

                        // Export PDF
                        OutlinedButton(
                            onClick = {
                                val html = "<h1>$subject Lesson Note</h1><h2>Class: $gradeClass | Duration: $duration</h2><div>" + 
                                        editableText.replace("\n", "<br>") + "</div>"
                                ExportService.exportToPdf(con, topic, html)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.PictureAsPdf, null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("PDF", fontSize = 11.sp)
                        }

                        // Export Word
                        OutlinedButton(
                            onClick = {
                                val html = "<h1>$subject Lesson Plan</h1><h2>Class: $gradeClass | Topic: $topic</h2><p>" + 
                                        editableText.replace("\n", "<br>") + "</p>"
                                ExportService.exportToWord(con, topic, html)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Description, null, tint = Color(0xFF1976D2))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("DOCX", fontSize = 11.sp)
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
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(24.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Formulating NECO/WAEC MCQ items...", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
        is AiGenerationState.Error -> {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Compiler Error", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                    Text((state as AiGenerationState.Error).message, fontSize = 12.sp, color = MaterialTheme.colorScheme.onErrorContainer)
                    TextButton(onClick = { viewModel.clearMcqState() }) { Text("Dismiss", color = MaterialTheme.colorScheme.onErrorContainer) }
                }
            }
        }
        is AiGenerationState.Success -> {
            val mcqs = (state as AiGenerationState.Success<List<McqModel>>).data
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Forced MCQs Draft (${mcqs.size} items)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                        IconButton(onClick = { viewModel.clearMcqState() }) { Icon(Icons.Default.Clear, null) }
                    }

                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 280.dp)
                            .padding(vertical = 8.dp)
                    ) {
                        mcqs.forEachIndexed { i, item ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("${i+1}. ${item.question}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Text("A. ${item.optionA}   B. ${item.optionB}", fontSize = 11.sp, color = Color.DarkGray)
                                    Text("C. ${item.optionC}   D. ${item.optionD}", fontSize = 11.sp, color = Color.DarkGray)
                                    Text("Correct: Option ${item.correctAnswer}", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.saveMcqSet(subject, gradeClass, topic, difficulty, mcqs)
                                Toast.makeText(con, "Save finished!", Toast.LENGTH_SHORT).show()
                                viewModel.clearMcqState()
                            },
                            modifier = Modifier.weight(1.2f)
                        ) {
                            Icon(Icons.Default.Save, null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save Set")
                        }

                        // XL spreadsheet export
                        OutlinedButton(
                            onClick = {
                                val exportList = mcqs.map {
                                    McqExportItem(it.question, it.optionA, it.optionB, it.optionC, it.optionD, it.correctAnswer, it.explanation)
                                }
                                ExportService.exportToExcel(con, "${topic}_Spreadsheet_MCQ", exportList)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.GridOn, null, tint = Color(0xFF2E7D32))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Excel", fontSize = 11.sp)
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
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(24.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Formulating structural theory papers...", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
        is AiGenerationState.Error -> {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Theory Design Error", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                    Text((state as AiGenerationState.Error).message, fontSize = 12.sp, color = MaterialTheme.colorScheme.onErrorContainer)
                    TextButton(onClick = { viewModel.clearTheoryState() }) { Text("Dismiss", color = MaterialTheme.colorScheme.onErrorContainer) }
                }
            }
        }
        is AiGenerationState.Success -> {
            val theories = (state as AiGenerationState.Success<List<TheoryModel>>).data
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Draft Theories", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                        IconButton(onClick = { viewModel.clearTheoryState() }) { Icon(Icons.Default.Clear, null) }
                    }

                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 280.dp)
                            .padding(vertical = 8.dp)
                    ) {
                        theories.forEachIndexed { i, item ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("Question ${i+1}:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                    Text(item.question, fontWeight = FontWeight.Normal, fontSize = 12.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Marking Key Guideline:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.Gray)
                                    Text(item.markingScheme, fontSize = 11.sp, color = Color.DarkGray)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.saveTheorySet(subject, gradeClass, topic, theories)
                                Toast.makeText(con, "Theories saved locally!", Toast.LENGTH_SHORT).show()
                                viewModel.clearTheoryState()
                            },
                            modifier = Modifier.weight(1.2f)
                        ) {
                            Icon(Icons.Default.Save, null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save Questions")
                        }

                        // Word Document Export
                        OutlinedButton(
                            onClick = {
                                val html = "<h1>$subject Theory Papers</h1><h2>Class: $gradeClass | Topic: $topic</h2>" + 
                                    theories.joinToString("<hr>") { "<h3>Q: ${it.question}</h3><p><b>Marking Guide:</b><br>${it.markingScheme}</p>" }
                                ExportService.exportToWord(con, "${topic}_Theories", html)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Description, null, tint = Color(0xFF1976D2))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("DOCX")
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 3. HISTORY PAGE SCREEN
// ==========================================
@Composable
fun HistoryScreen(
    viewModel: TeacherViewModel,
    onViewNote: (LessonNote) -> Unit,
    onViewMcq: (MCQSet) -> Unit,
    onViewTheory: (TheorySet) -> Unit,
    modifier: Modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)
) {
    val notes by viewModel.lessonNotes.collectAsState()
    val mcqs by viewModel.mcqSets.collectAsState()
    val theories by viewModel.theorySets.collectAsState()

    var filterType by remember { mutableStateOf("ALL") } // ALL, NOTE, MCQ, THEORY
    var searchQuery by remember { mutableStateOf("") }

    // Aggregate everything into a structured card structure
    val aggregatedItems = remember(notes, mcqs, theories, filterType, searchQuery) {
        val list = mutableListOf<Triple<String, Any, Long>>() // Type, Object, creationMillis
        
        if (filterType == "ALL" || filterType == "NOTE") {
            notes.forEach { list.add(Triple("Lesson Note", it, it.createdAt)) }
        }
        if (filterType == "ALL" || filterType == "MCQ") {
            mcqs.forEach { list.add(Triple("MCQ Set", it, it.createdAt)) }
        }
        if (filterType == "ALL" || filterType == "THEORY") {
            theories.forEach { list.add(Triple("Theory Paper", it, it.createdAt)) }
        }

        // Sort of dates
        val sorted = list.sortedByDescending { it.third }
        
        // Match Search Query terms
        if (searchQuery.trim().isEmpty()) sorted
        else {
            sorted.filter { item ->
                when (item.second) {
                    is LessonNote -> {
                        val n = item.second as LessonNote
                        n.topic.contains(searchQuery, ignoreCase = true) || n.subject.contains(searchQuery, ignoreCase = true)
                    }
                    is MCQSet -> {
                        val m = item.second as MCQSet
                        m.topic.contains(searchQuery, ignoreCase = true) || m.subject.contains(searchQuery, ignoreCase = true)
                    }
                    else -> {
                        val t = item.second as TheorySet
                        t.topic.contains(searchQuery, ignoreCase = true) || t.subject.contains(searchQuery, ignoreCase = true)
                    }
                }
            }
        }
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Personal Academic Archive", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search by topic or subject...") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        // Type Filter Chips row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val filters = listOf("ALL" to "All", "NOTE" to "Notes", "MCQ" to "MCQs", "THEORY" to "Theories")
            filters.forEach { (fid, fLabel) ->
                FilterChip(
                    selected = filterType == fid,
                    onClick = { filterType = fid },
                    label = { Text(fLabel, fontSize = 11.sp) }
                )
            }
        }

        if (aggregatedItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.FindInPage, "Empty", tint = Color.Gray, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No documents discovered matching filter.", fontSize = 13.sp, color = Color.Gray)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(aggregatedItems) { item ->
                    val type = item.first
                    val obj = item.second
                    
                    val title: String
                    val subject: String
                    val klass: String
                    val dateFormatted: String
                    
                    when (obj) {
                        is LessonNote -> {
                            title = obj.title
                            subject = obj.subject
                            klass = obj.gradeClass
                            dateFormatted = SimpleDateFormat("dd MMM yyyy", Locale.US).format(Date(obj.createdAt))
                        }
                        is MCQSet -> {
                            title = obj.title
                            subject = obj.subject
                            klass = obj.gradeClass
                            dateFormatted = SimpleDateFormat("dd MMM yyyy", Locale.US).format(Date(obj.createdAt))
                        }
                        else -> {
                            val th = obj as TheorySet
                            title = th.title
                            subject = th.subject
                            klass = th.gradeClass
                            dateFormatted = SimpleDateFormat("dd MMM yyyy", Locale.US).format(Date(th.createdAt))
                        }
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                when (obj) {
                                    is LessonNote -> onViewNote(obj)
                                    is MCQSet -> onViewMcq(obj)
                                    else -> onViewTheory(obj as TheorySet)
                                }
                            }
                            .testTag("archive_card_item"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Badge(
                                    containerColor = when (type) {
                                        "Lesson Note" -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                        "MCQ Set" -> Color(0xFFE8F5E9)
                                        else -> Color(0xFFECEFF1)
                                    }
                                ) {
                                    Text(
                                        text = type.uppercase(),
                                        color = when (type) {
                                            "Lesson Note" -> MaterialTheme.colorScheme.primary
                                            "MCQ Set" -> Color(0xFF2E7D32)
                                            else -> Color(0xFF455A64)
                                        },
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                
                                Text(dateFormatted, fontSize = 11.sp, color = Color.Gray)
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(
                                text = "$subject | Class: $klass",
                                fontSize = 12.sp,
                                color = Color.Gray,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 4. TIMETABLE MANAGER SCREEN
// ==========================================
@Composable
fun TimetableScreen(viewModel: TeacherViewModel) {
    val timetable by viewModel.timetableItems.collectAsState()
    val mySchools by viewModel.teacherSchools.collectAsState()
    val myClasses by viewModel.schoolClasses.collectAsState()
    var openAddDialog by remember { mutableStateOf(false) }

    // Input States for New Lesson
    val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday")
    var selectedDay by remember { mutableStateOf("Monday") }
    var inputSubject by remember { mutableStateOf("") }
    var inputClass by remember { mutableStateOf("") }
    var inputSchool by remember { mutableStateOf("") }
    var inputStartTime by remember { mutableStateOf("08:00") }
    var inputEndTime by remember { mutableStateOf("08:40") }

    // Color Pick presets
    val colors = listOf("#1B4D3E", "#2E7D32", "#E9B306", "#D32F2F", "#1976D2")
    var selectedColorHex by remember { mutableStateOf(colors[0]) }

    LaunchedEffect(openAddDialog) {
        if (openAddDialog) {
            inputSchool = mySchools.firstOrNull() ?: ""
            val classesForSchool = myClasses.filter { it.schoolName.equals(inputSchool, ignoreCase = true) }.map { it.className }.distinct()
            inputClass = classesForSchool.firstOrNull() ?: (myClasses.firstOrNull()?.className ?: "")
            val subjectsForClass = myClasses.filter {
                it.schoolName.equals(inputSchool, ignoreCase = true) &&
                it.className.equals(inputClass, ignoreCase = true)
            }.map { it.subject }.distinct()
            inputSubject = subjectsForClass.firstOrNull() ?: (myClasses.firstOrNull()?.subject ?: "")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Timetable Master", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text("Manage multiple school class periods easily.", fontSize = 11.sp, color = Color.Gray)
            }
            
            Button(onClick = { openAddDialog = true }) {
                Icon(Icons.Default.Add, null)
                Text("Add Period")
            }
        }

        // Segment days lists
        var selectedWeekDayFilter by remember { mutableStateOf("Monday") }
        
        TabRow(
            selectedTabIndex = days.indexOf(selectedWeekDayFilter),
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            days.forEach { dayName ->
                Tab(
                    selected = dayName == selectedWeekDayFilter,
                    onClick = { selectedWeekDayFilter = dayName },
                    text = { Text(dayName.substring(0, 3), fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
            }
        }

        val dayFilteredSchedule = timetable.filter { it.dayOfWeek.equals(selectedWeekDayFilter, ignoreCase = true) }

        if (dayFilteredSchedule.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.School, null, modifier = Modifier.size(48.dp), tint = Color.LightGray)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No lessons arranged for $selectedWeekDayFilter.", fontSize = 13.sp, color = Color.Gray)
                }
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(dayFilteredSchedule) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.width(6.dp).height(40.dp).background(Color(android.graphics.Color.parseColor(item.colorHex)), RoundedCornerShape(3.dp)))
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "${item.subject} (${item.gradeClass})",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        style = if (item.isCompleted) androidx.compose.ui.text.TextStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough) else androidx.compose.ui.text.TextStyle.Default,
                                        color = if (item.isCompleted) Color.Gray else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${item.schoolName} · ${item.startTime} - ${item.endTime}",
                                        fontSize = 11.sp,
                                        color = if (item.isCompleted) Color.Gray.copy(alpha = 0.6f) else Color.Gray
                                    )
                                }
                            }
                            
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = item.isCompleted,
                                    onCheckedChange = { viewModel.toggleTimetableComplete(item) },
                                    modifier = Modifier.testTag("timetable_checkbox_${item.id}")
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                IconButton(onClick = { viewModel.deleteTimetableItem(item.id) }) {
                                    Icon(Icons.Default.Delete, "Delete", tint = Color.Red.copy(alpha = 0.7f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal dialog to add slot
    if (openAddDialog) {
        Dialog(onDismissRequest = { openAddDialog = false }) {
            Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Schedule New Period", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)

                    // 1. School Selector (starts with school selection first)
                    var schoolDropExpanded by remember { mutableStateOf(false) }
                    Text("Select School:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = inputSchool,
                            onValueChange = { inputSchool = it },
                            label = { Text("School Affiliation") },
                            placeholder = { Text("e.g. Govt College Kaduna") },
                            trailingIcon = {
                                IconButton(onClick = { schoolDropExpanded = !schoolDropExpanded }) {
                                    Icon(Icons.Default.ArrowDropDown, null)
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (mySchools.isNotEmpty()) {
                            DropdownMenu(expanded = schoolDropExpanded, onDismissRequest = { schoolDropExpanded = false }) {
                                mySchools.forEach { sName ->
                                    DropdownMenuItem(text = { Text(sName) }, onClick = { 
                                        inputSchool = sName
                                        schoolDropExpanded = false
                                    })
                                }
                            }
                        }
                    }

                    // 2. Class Selector (then class selection)
                    var classDropExpanded by remember { mutableStateOf(false) }
                    val availableClasses = remember(myClasses, inputSchool) {
                        myClasses.filter { it.schoolName.equals(inputSchool, ignoreCase = true) }.map { it.className }.distinct()
                    }
                    val displayClasses = if (availableClasses.isNotEmpty()) availableClasses else myClasses.map { it.className }.distinct()
                    Text("Select Class:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = inputClass,
                            onValueChange = { inputClass = it },
                            label = { Text("Class") },
                            placeholder = { Text("e.g. SS3 A") },
                            trailingIcon = {
                                IconButton(onClick = { classDropExpanded = !classDropExpanded }) {
                                    Icon(Icons.Default.ArrowDropDown, null)
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (displayClasses.isNotEmpty()) {
                            DropdownMenu(expanded = classDropExpanded, onDismissRequest = { classDropExpanded = false }) {
                                displayClasses.forEach { cls ->
                                    DropdownMenuItem(text = { Text(cls) }, onClick = { 
                                        inputClass = cls
                                        classDropExpanded = false
                                    })
                                }
                            }
                        }
                    }

                    // 3. Subject Selector (and subject selection before time)
                    var subjectDropExpanded by remember { mutableStateOf(false) }
                    val availableSubjects = remember(myClasses, inputSchool, inputClass) {
                        myClasses.filter { 
                            it.schoolName.equals(inputSchool, ignoreCase = true) && 
                            it.className.equals(inputClass, ignoreCase = true) 
                        }.map { it.subject }.distinct()
                    }
                    val displaySubjects = if (availableSubjects.isNotEmpty()) availableSubjects else myClasses.map { it.subject }.distinct()
                    Text("Select Subject:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = inputSubject,
                            onValueChange = { inputSubject = it },
                            label = { Text("Subject Name") },
                            placeholder = { Text("e.g. Mathematics") },
                            trailingIcon = {
                                IconButton(onClick = { subjectDropExpanded = !subjectDropExpanded }) {
                                    Icon(Icons.Default.ArrowDropDown, null)
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (displaySubjects.isNotEmpty()) {
                            DropdownMenu(expanded = subjectDropExpanded, onDismissRequest = { subjectDropExpanded = false }) {
                                displaySubjects.forEach { subj ->
                                    DropdownMenuItem(text = { Text(subj) }, onClick = { 
                                        inputSubject = subj
                                        subjectDropExpanded = false
                                    })
                                }
                            }
                        }
                    }

                    // 4. Day of week dropdown
                    var dayDropExpanded by remember { mutableStateOf(false) }
                    Text("Select Day:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = selectedDay,
                            onValueChange = {},
                            readOnly = true,
                            enabled = false,
                            label = { Text("Day of Week") },
                            trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { dayDropExpanded = true },
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                disabledBorderColor = MaterialTheme.colorScheme.outline,
                                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        DropdownMenu(expanded = dayDropExpanded, onDismissRequest = { dayDropExpanded = false }) {
                            days.forEach { d ->
                                DropdownMenuItem(text = { Text(d) }, onClick = { selectedDay = d; dayDropExpanded = false })
                            }
                        }
                    }

                    // 5. Time inputs
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(value = inputStartTime, onValueChange = { inputStartTime = it }, label = { Text("Start Time") }, placeholder = { Text("08:00") }, singleLine = true, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = inputEndTime, onValueChange = { inputEndTime = it }, label = { Text("End Time") }, placeholder = { Text("08:40") }, singleLine = true, modifier = Modifier.weight(1f))
                    }

                    Text("Pick Dashboard Tag Color:", fontSize = 12.sp, color = Color.Gray)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        colors.forEach { col ->
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(Color(android.graphics.Color.parseColor(col)), CircleShape)
                                    .clickable { selectedColorHex = col }
                                    .then(
                                        if (selectedColorHex == col) Modifier.clip(CircleShape).background(Color.White.copy(alpha = 0.5f)) else Modifier
                                    )
                            )
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { openAddDialog = false }) { Text("Cancel") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (inputSubject.trim().isEmpty() || inputClass.trim().isEmpty()) {
                                    return@Button
                                }
                                viewModel.addTimetableItem(
                                    selectedDay, inputStartTime, inputEndTime, inputSubject, inputClass,
                                    if (inputSchool.trim().isEmpty()) "Personal Teacher" else inputSchool,
                                    selectedColorHex
                                )
                                openAddDialog = false
                            }
                        ) {
                            Text("Arrange")
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 5. SYLLABUS TRACKER SCREEN
// ==========================================
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

    // Computed unique schools list
    val schoolsFilterList = remember(teacherSchools) {
        val list = teacherSchools.toMutableList()
        list.add(0, "All Schools")
        list
    }

    // Computed unique classes list dynamically matching the selected school/all schools
    val classesFilterList = remember(myClasses, filterSchool) {
        val filtered = if (filterSchool == "All Schools") {
            myClasses
        } else {
            myClasses.filter { it.schoolName.equals(filterSchool, ignoreCase = true) }
        }
        val list = filtered.map { it.className }.distinct().toMutableList()
        list.add(0, "All Classes")
        list
    }

    // Computed unique subjects list dynamically matching current selections
    val subjectsFilterList = remember(myClasses, filterSchool, filterClass) {
        val filtered = myClasses.filter {
            (filterSchool == "All Schools" || it.schoolName.equals(filterSchool, ignoreCase = true)) &&
            (filterClass == "All Classes" || it.className.equals(filterClass, ignoreCase = true))
        }
        val list = filtered.map { it.subject }.distinct().toMutableList()
        list.add(0, "All Subjects")
        list
    }

    // Filter items reactively based on role & dropdown selections
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
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Syllabus Progress Completion", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text("Keep tracker logs of term-based topic margins.", fontSize = 11.sp, color = Color.Gray)
            }
            
            IconButton(onClick = { openAddCourseDialog = true }, modifier = Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape)) {
                Icon(Icons.Default.Add, "Add", tint = MaterialTheme.colorScheme.primary)
            }
        }

        // Custom filtering UI based on user onboarding context
        Column(
            modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp)).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Syllabus Filter Constraints", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
            
            if (showSchoolFilter) {
                // If part-timer with multiple schools, filter by school first!
                var schoolDropExpanded by remember { mutableStateOf(false) }
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = { schoolDropExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.LocationOn, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("School: $filterSchool", fontSize = 12.sp)
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(Icons.Default.ArrowDropDown, null)
                    }
                    DropdownMenu(expanded = schoolDropExpanded, onDismissRequest = { schoolDropExpanded = false }, modifier = Modifier.fillMaxWidth()) {
                        schoolsFilterList.forEach { valSchool ->
                            DropdownMenuItem(text = { Text(valSchool) }, onClick = { 
                                filterSchool = valSchool
                                filterClass = "All Classes"
                                filterSubject = "All Subjects"
                                schoolDropExpanded = false 
                            })
                        }
                    }
                }
            }

            // Class and Subject filters
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Class filter
                var classDropExpanded by remember { mutableStateOf(false) }
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedButton(onClick = { classDropExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.School, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(filterClass, fontSize = 11.sp, maxLines = 1)
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(Icons.Default.ArrowDropDown, null)
                    }
                    DropdownMenu(expanded = classDropExpanded, onDismissRequest = { classDropExpanded = false }) {
                        classesFilterList.forEach { valClass ->
                            DropdownMenuItem(text = { Text(valClass) }, onClick = { 
                                filterClass = valClass
                                filterSubject = "All Subjects"
                                classDropExpanded = false 
                            })
                        }
                    }
                }

                // Subject filter
                var subjectDropExpanded by remember { mutableStateOf(false) }
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedButton(onClick = { subjectDropExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Book, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(filterSubject, fontSize = 11.sp, maxLines = 1)
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(Icons.Default.ArrowDropDown, null)
                    }
                    DropdownMenu(expanded = subjectDropExpanded, onDismissRequest = { subjectDropExpanded = false }) {
                        subjectsFilterList.forEach { valSubj ->
                            DropdownMenuItem(text = { Text(valSubj) }, onClick = { 
                                filterSubject = valSubj
                                subjectDropExpanded = false 
                            })
                        }
                    }
                }
            }
        }

        // Course completion percentage card block
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Filtered Completion Track", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("$compl / ${finalItems.size} Topics Completed", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                
                Spacer(modifier = Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
                )
            }
        }

        // Topics Checklist Tree sorted by Term classification
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(finalItems) { sItem ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(sItem.topic, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("${sItem.subject} · ${sItem.gradeClass} · Week ${sItem.week} · ${sItem.term} (${sItem.schoolName})", fontSize = 11.sp, color = Color.Gray)
                        }
                        
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(onClick = { viewSyllabusItem = sItem }) {
                                Icon(
                                    imageVector = Icons.Default.Visibility,
                                    contentDescription = "View details",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            IconButton(onClick = { editSyllabusItem = sItem }) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit topic",
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Checkbox(
                                checked = sItem.isCompleted,
                                onCheckedChange = { viewModel.toggleSyllabusComplete(sItem) },
                                modifier = Modifier.testTag("syllabus_checkbox_${sItem.id}")
                            )
                            
                            IconButton(onClick = { viewModel.deleteSyllabusItem(sItem.id) }) {
                                Icon(Icons.Default.Delete, "Delete", tint = Color.Gray, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    if (openAddCourseDialog) {
        var addSchool by remember { mutableStateOf("") }
        var addClass by remember { mutableStateOf("") }
        var addSubj by remember { mutableStateOf("") }

        var addTerm by remember { mutableStateOf("Term 1") }
        var addWeek by remember { mutableStateOf(1) } // 1 to 14
        var addThemeText by remember { mutableStateOf("") }
        var addTopicText by remember { mutableStateOf("") }
        var addContentText by remember { mutableStateOf("") }
        var addObjectivesText by remember { mutableStateOf("") }

        var schExpanded by remember { mutableStateOf(false) }
        var clsExpanded by remember { mutableStateOf(false) }
        var sbjExpanded by remember { mutableStateOf(false) }
        var termExpanded by remember { mutableStateOf(false) }
        var weekExpanded by remember { mutableStateOf(false) }

        // Initialize selectors cascaded
        val mySchools = teacherSchools
        val myClasses = myClasses

        LaunchedEffect(mySchools) {
            if (addSchool.isEmpty() || !mySchools.contains(addSchool)) {
                addSchool = mySchools.firstOrNull() ?: ""
            }
        }
        val availableAddClasses = remember(myClasses, addSchool) {
            myClasses.filter { it.schoolName.equals(addSchool, ignoreCase = true) }.map { it.className }.distinct()
        }
        LaunchedEffect(availableAddClasses, addSchool) {
            if (addClass.isEmpty() || !availableAddClasses.contains(addClass)) {
                addClass = availableAddClasses.firstOrNull() ?: ""
            }
        }
        val availableAddSubjects = remember(myClasses, addSchool, addClass) {
            myClasses.filter { it.schoolName.equals(addSchool, ignoreCase = true) && it.className.equals(addClass, ignoreCase = true) }.map { it.subject }.distinct()
        }
        LaunchedEffect(availableAddSubjects, addClass) {
            if (addSubj.isEmpty() || !availableAddSubjects.contains(addSubj)) {
                addSubj = availableAddSubjects.firstOrNull() ?: ""
            }
        }

        Dialog(onDismissRequest = { openAddCourseDialog = false }) {
            Card(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Add Syllabus Track Item", fontWeight = FontWeight.Bold, fontSize = 15.sp)

                    // 1. School Cascade Selection
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = addSchool,
                            onValueChange = {},
                            readOnly = true,
                            enabled = false,
                            label = { Text("School") },
                            trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { schExpanded = true },
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                disabledBorderColor = MaterialTheme.colorScheme.outline,
                                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        DropdownMenu(expanded = schExpanded, onDismissRequest = { schExpanded = false }, modifier = Modifier.fillMaxWidth()) {
                            mySchools.forEach { sItem ->
                                DropdownMenuItem(text = { Text(sItem) }, onClick = { addSchool = sItem; schExpanded = false })
                            }
                        }
                    }

                    // 2. Class Cascade Selection
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = addClass,
                            onValueChange = {},
                            readOnly = true,
                            enabled = false,
                            label = { Text("Class") },
                            trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { clsExpanded = true },
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                disabledBorderColor = MaterialTheme.colorScheme.outline,
                                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        DropdownMenu(expanded = clsExpanded, onDismissRequest = { clsExpanded = false }, modifier = Modifier.fillMaxWidth()) {
                            availableAddClasses.forEach { cItem ->
                                DropdownMenuItem(text = { Text(cItem) }, onClick = { addClass = cItem; clsExpanded = false })
                            }
                        }
                    }

                    // 3. Subject Cascade Selection
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = addSubj,
                            onValueChange = {},
                            readOnly = true,
                            enabled = false,
                            label = { Text("Subject") },
                            trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { sbjExpanded = true },
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                disabledBorderColor = MaterialTheme.colorScheme.outline,
                                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        DropdownMenu(expanded = sbjExpanded, onDismissRequest = { sbjExpanded = false }, modifier = Modifier.fillMaxWidth()) {
                            availableAddSubjects.forEach { sbjItem ->
                                DropdownMenuItem(text = { Text(sbjItem) }, onClick = { addSubj = sbjItem; sbjExpanded = false })
                            }
                        }
                    }

                    // 4. Week (dropdown 1 to 14) and Term selection
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        // Week drop
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = "Week $addWeek",
                                onValueChange = {},
                                readOnly = true,
                                enabled = false,
                                label = { Text("Week") },
                                trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { weekExpanded = true },
                                colors = OutlinedTextFieldDefaults.colors(
                                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                            DropdownMenu(expanded = weekExpanded, onDismissRequest = { weekExpanded = false }) {
                                (1..14).forEach { wNum ->
                                        DropdownMenuItem(text = { Text("Week $wNum") }, onClick = { addWeek = wNum; weekExpanded = false })
                                    }
                            }
                        }

                        // Term drop
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = addTerm,
                                onValueChange = {},
                                readOnly = true,
                                enabled = false,
                                label = { Text("Term") },
                                trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { termExpanded = true },
                                colors = OutlinedTextFieldDefaults.colors(
                                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                            DropdownMenu(expanded = termExpanded, onDismissRequest = { termExpanded = false }) {
                                listOf("Term 1", "Term 2", "Term 3").forEach { tVal ->
                                    DropdownMenuItem(text = { Text(tVal) }, onClick = { addTerm = tVal; termExpanded = false })
                                }
                            }
                        }
                    }

                    // Optional Theme, Topic, Content, Objectives (each configured with sentence-case options)
                    OutlinedTextField(
                        value = addThemeText,
                        onValueChange = { addThemeText = it },
                        label = { Text("Theme (Optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                    )

                    OutlinedTextField(
                        value = addTopicText,
                        onValueChange = { addTopicText = it },
                        label = { Text("Topic Description") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                    )

                    OutlinedTextField(
                        value = addContentText,
                        onValueChange = { addContentText = it },
                        label = { Text("Content Outline (Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                    )

                    OutlinedTextField(
                        value = addObjectivesText,
                        onValueChange = { addObjectivesText = it },
                        label = { Text("Objectives") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                    )

                    Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { openAddCourseDialog = false }) { Text("Cancel") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (addTopicText.trim().isEmpty() || addSchool.trim().isEmpty()) return@Button
                                viewModel.addSyllabusItem(
                                    schoolName = addSchool,
                                    gradeClass = addClass,
                                    subject = addSubj,
                                    term = addTerm,
                                    week = addWeek,
                                    theme = addThemeText,
                                    topic = addTopicText,
                                    content = addContentText,
                                    objectives = addObjectivesText
                                )
                                openAddCourseDialog = false
                            }
                        ) {
                            Text("Track Details")
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
            title = { Text(item.topic, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text("Classroom Alignment:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                    Text("School: ${item.schoolName}\nClass: ${item.gradeClass}\nSubject: ${item.subject}\nTerm: ${item.term} · Week ${item.week}", fontSize = 13.sp)

                    if (item.theme.isNotBlank()) {
                         Text("Theme Outline:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                         Text(item.theme, fontSize = 13.sp)
                    }

                    if (item.content.isNotBlank()) {
                         Text("Content Context:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                         Text(item.content, fontSize = 13.sp)
                    }

                    if (item.objectives.isNotBlank()) {
                         Text("Objectives / Goals:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                         Text(item.objectives, fontSize = 13.sp)
                    }
                    
                    Text("Status: ${if (item.isCompleted) "Completed" else "In Progress"}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            },
            confirmButton = {
                TextButton(onClick = { viewSyllabusItem = null }) { Text("Dismiss") }
            }
        )
    }

    if (editSyllabusItem != null) {
        val originalItem = editSyllabusItem!!
        var editTopic by remember { mutableStateOf(originalItem.topic) }
        var editTheme by remember { mutableStateOf(originalItem.theme) }
        var editWeek by remember { mutableStateOf(originalItem.week) }
        var editTerm by remember { mutableStateOf(originalItem.term) }
        var editContent by remember { mutableStateOf(originalItem.content) }
        var editObjectives by remember { mutableStateOf(originalItem.objectives) }
        
        var termDrop by remember { mutableStateOf(false) }
        var weekDrop by remember { mutableStateOf(false) }

        Dialog(onDismissRequest = { editSyllabusItem = null }) {
            Card(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Modify Syllabus Track Item", fontWeight = FontWeight.Bold, fontSize = 15.sp)

                    OutlinedTextField(
                        value = editTopic,
                        onValueChange = { editTopic = it },
                        label = { Text("Topic Description") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        // Week drop
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = "Week $editWeek",
                                onValueChange = {},
                                readOnly = true,
                                enabled = false,
                                label = { Text("Week") },
                                trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { weekDrop = true },
                                colors = OutlinedTextFieldDefaults.colors(
                                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                            DropdownMenu(expanded = weekDrop, onDismissRequest = { weekDrop = false }) {
                                (1..14).forEach { wNum ->
                                    DropdownMenuItem(text = { Text("Week $wNum") }, onClick = { editWeek = wNum; weekDrop = false })
                                }
                            }
                        }

                        // Term drop
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = editTerm,
                                onValueChange = {},
                                readOnly = true,
                                enabled = false,
                                label = { Text("Term") },
                                trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { termDrop = true },
                                colors = OutlinedTextFieldDefaults.colors(
                                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                            DropdownMenu(expanded = termDrop, onDismissRequest = { termDrop = false }) {
                                listOf("Term 1", "Term 2", "Term 3").forEach { tVal ->
                                    DropdownMenuItem(text = { Text(tVal) }, onClick = { editTerm = tVal; termDrop = false })
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = editTheme,
                        onValueChange = { editTheme = it },
                        label = { Text("Theme (Optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                    )

                    OutlinedTextField(
                        value = editContent,
                        onValueChange = { editContent = it },
                        label = { Text("Content Outline (Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                    )

                    OutlinedTextField(
                        value = editObjectives,
                        onValueChange = { editObjectives = it },
                        label = { Text("Objectives Outline (Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                    )

                    Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { editSyllabusItem = null }) { Text("Cancel") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (editTopic.trim().isEmpty()) return@Button
                                val updated = originalItem.copy(
                                    topic = editTopic,
                                    theme = editTheme,
                                    week = editWeek,
                                    term = editTerm,
                                    content = editContent,
                                    objectives = editObjectives
                                )
                                viewModel.updateSyllabusItem(updated)
                                editSyllabusItem = null
                            }
                        ) {
                            Text("Save Changes")
                        }
                    }
                }
            }
        }
    }
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
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Weekly Workload & Classrooms", 
                    style = MaterialTheme.typography.titleMedium, 
                    fontWeight = FontWeight.Bold, 
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Configure curriculum schedules & student lists per subject.", 
                    fontSize = 11.sp, 
                    color = Color.Gray
                )
            }
            
            IconButton(
                onClick = { openAddClassDialog = true },
                modifier = Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape)
            ) {
                Icon(Icons.Default.Add, "Add", tint = MaterialTheme.colorScheme.primary)
            }
        }

        if (classes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f), 
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.MenuBook, 
                        contentDescription = null, 
                        modifier = Modifier.size(64.dp), 
                        tint = MaterialTheme.colorScheme.primaryContainer
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No active workloads registered yet.", 
                        fontSize = 13.sp, 
                        fontWeight = FontWeight.Medium,
                        color = Color.Gray
                    )
                    Text(
                        text = "Click 'Add Workload' to map class streams and subjects.", 
                        fontSize = 11.sp, 
                        color = Color.LightGray
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f), 
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(classes) { classroom ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onViewClass(classroom) }
                            .testTag("classroom_item_card"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(), 
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = classroom.className, 
                                    fontWeight = FontWeight.Bold, 
                                    fontSize = 16.sp, 
                                    color = MaterialTheme.colorScheme.primary
                                )
                                
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { editingClass = classroom }, 
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit, 
                                            contentDescription = "Edit Class Info", 
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                    IconButton(
                                        onClick = { viewModel.deleteSchoolClass(classroom.id) }, 
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete, 
                                            contentDescription = "Remove Class Set", 
                                            tint = Color.Red.copy(alpha = 0.6f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "🏫 School site: ${classroom.schoolName}", 
                                fontSize = 12.sp, 
                                color = Color.DarkGray
                            )
                            Text(
                                text = "📚 Assigned Subject: ${classroom.subject}", 
                                fontSize = 12.sp, 
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant, 
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // --- DIALOG: ADD WORKLOAD SET (CLASS & MULTIPLE SUBJECTS) ---
    if (openAddClassDialog) {
        Dialog(onDismissRequest = { openAddClassDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .fillMaxHeight(0.85f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Add Academic Workload Record", 
                        fontWeight = FontWeight.Bold, 
                        fontSize = 16.sp, 
                        color = MaterialTheme.colorScheme.primary
                    )

                    // School Name
                    OutlinedTextField(
                        value = addSchoolName, 
                        onValueChange = { addSchoolName = it }, 
                        label = { Text("School Name") }, 
                        singleLine = true, 
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Class Level Selection Button
                    var dropdownExpanded by remember { mutableStateOf(false) }
                    Text(
                        text = "Select Class Stream:", 
                        fontSize = 12.sp, 
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { dropdownExpanded = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.School, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(addClassName)
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(Icons.Default.ArrowDropDown, null)
                        }
                        DropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false },
                            modifier = Modifier.fillMaxWidth(0.8f)
                        ) {
                            predefinedClassesList.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        addClassName = option
                                        dropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Multi-subject Setup
                    Text(
                        text = "Assigned Subjects for $addClassName:", 
                        fontSize = 12.sp, 
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    addSubjectsList.forEachIndexed { sIndex, subjName ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = subjName, 
                                onValueChange = { newVal ->
                                    addSubjectsList[sIndex] = newVal
                                }, 
                                label = { Text("Subject Focus (e.g. Chemistry)") }, 
                                placeholder = { Text("Chemistry") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                            )
                            
                            if (addSubjectsList.size > 1) {
                                IconButton(
                                    onClick = { addSubjectsList.removeAt(sIndex) }
                                ) {
                                    Icon(Icons.Default.Delete, "Remove", tint = Color.Red.copy(alpha = 0.6f))
                                }
                            }
                        }
                    }

                    TextButton(
                        onClick = { addSubjectsList.add("") },
                        modifier = Modifier.align(Alignment.Start)
                    ) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Another Subject under $addClassName", fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(), 
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { openAddClassDialog = false }) { Text("Cancel") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (addSchoolName.trim().isEmpty() || addClassName.trim().isEmpty()) return@Button
                                addSubjectsList.forEach { subj ->
                                    if (subj.trim().isNotEmpty()) {
                                        viewModel.addSchoolClass(addClassName.trim(), addSchoolName.trim(), subj.trim())
                                    }
                                }
                                openAddClassDialog = false
                            },
                            enabled = addSchoolName.isNotBlank() && addSubjectsList.any { it.isNotBlank() }
                        ) {
                            Text("Create Registry")
                        }
                    }
                }
            }
        }
    }

    // --- DIALOG: EDIT SINGLE WORKLOAD ENTRY ---
    if (editingClass != null) {
        Dialog(onDismissRequest = { editingClass = null }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Edit Classroom Record", 
                        fontWeight = FontWeight.Bold, 
                        fontSize = 16.sp, 
                        color = MaterialTheme.colorScheme.primary
                    )

                    // School Name
                    OutlinedTextField(
                        value = editSchoolName, 
                        onValueChange = { editSchoolName = it }, 
                        label = { Text("School Name") }, 
                        singleLine = true, 
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Predefined Class selector
                    var editDropdownExpanded by remember { mutableStateOf(false) }
                    Text(
                        text = "Class Stream:", 
                        fontSize = 12.sp, 
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { editDropdownExpanded = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.School, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(editClassName)
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(Icons.Default.ArrowDropDown, null)
                        }
                        DropdownMenu(
                            expanded = editDropdownExpanded,
                            onDismissRequest = { editDropdownExpanded = false },
                            modifier = Modifier.fillMaxWidth(0.8f)
                        ) {
                            predefinedClassesList.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        editClassName = option
                                        editDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Assigned Subject
                    OutlinedTextField(
                        value = editSubjectName, 
                        onValueChange = { editSubjectName = it }, 
                        label = { Text("Assigned Subject") }, 
                        singleLine = true, 
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(), 
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { editingClass = null }) { Text("Cancel") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                editingClass?.let { orig ->
                                    if (editSchoolName.trim().isEmpty() || editClassName.trim().isEmpty() || editSubjectName.trim().isEmpty()) return@Button
                                    viewModel.updateSchoolClass(
                                        orig.copy(
                                            className = editClassName.trim(),
                                            schoolName = editSchoolName.trim(),
                                            subject = editSubjectName.trim()
                                        )
                                    )
                                }
                                editingClass = null
                            },
                            enabled = editSchoolName.isNotBlank() && editClassName.isNotBlank() && editSubjectName.isNotBlank()
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
    val subPlan by viewModel.subscriptionPlan.collectAsState()
    val checkNotes by viewModel.usageLessonNotes.collectAsState()
    val checkMCQs by viewModel.usageMcqs.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Teacher’s Premium Plans", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text("Tailor limits to support multiple school classrooms.", fontSize = 11.sp, color = Color.Gray)
        }

        // Active usages trackers card
        item {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Interactive Month's AI Meter (Prototyping limits)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Lesson Notes Counter: $checkNotes / ${if (subPlan == "FREE") "5 limit" else "Unlimited"}", fontSize = 12.sp)
                    if (subPlan == "FREE") {
                        LinearProgressIndicator(progress = { checkNotes.toFloat() / 5f }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp))
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text("MCQ Compiler Counter: $checkMCQs / ${if (subPlan == "FREE") "10 limit" else "Unlimited"}", fontSize = 12.sp)
                    if (subPlan == "FREE") {
                        LinearProgressIndicator(progress = { checkMCQs.toFloat() / 10f }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp))
                    }

                    if (subPlan != "FREE") {
                        Text("✨ Unlimited AI Generator access enabled under the active $subPlan plan!", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                    }
                }
            }
        }

        // Tier Selection grid
        item {
            // Plan Tier A: FREE
            Card(
                modifier = Modifier.fillMaxWidth(),
                border = if (subPlan == "FREE") BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("🟢 Free Companion Plan", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("₦0 / Month", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.primary)
                    }
                    Text("• 5 Curriculum-aligned Lesson Notes / month\n• 10 MCQs limits / month\n• Simple Timetables & Class trackers\n• Standard watermarked PDFs.", fontSize = 12.sp, modifier = Modifier.padding(vertical = 8.dp))
                    
                    Button(
                        onClick = { viewModel.updatePlan("FREE") },
                        enabled = subPlan != "FREE",
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (subPlan == "FREE") "Active Plan" else "Select Free")
                    }
                }
            }
        }

        item {
            // Plan Tier B: STANDARD
            Card(
                modifier = Modifier.fillMaxWidth(),
                border = if (subPlan == "STANDARD") BorderStroke(2.dp, Color(0xFF1E88E5)) else null
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("🔵 Standard Plan pro", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("₦5,000 / Month", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF1E88E5))
                    }
                    Text("• UNLIMITED Nigeria-Standard Lesson Notes\n• UNLIMITED exam MCQs and explanations\n• UNLIMITED essay/theories generation + grading rubrics\n• Full PDF & Word exports\n• Dynamic Syllabus checklists.", fontSize = 12.sp, modifier = Modifier.padding(vertical = 8.dp))
                    
                    Button(
                        onClick = { viewModel.updatePlan("STANDARD") },
                        enabled = subPlan != "STANDARD",
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (subPlan == "STANDARD") "Active Plan" else "Upgrade Standard")
                    }
                }
            }
        }

        item {
            // Plan Tier C: PREMIUM POWER
            Card(
                modifier = Modifier.fillMaxWidth(),
                border = if (subPlan == "PREMIUM") BorderStroke(2.dp, Color(0xFFE65100)) else null
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("⭐ Premium Power Tier", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("₦10,000 / Month", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFFE65100))
                    }
                    Text("• Everything in Standard Plan PLUS\n• Native Excel / XLS MCQ roster download sheets\n• Custom complex exams combo packages\n• Fast AI Response servers\n• Comprehensive student attendance analytics.", fontSize = 12.sp, modifier = Modifier.padding(vertical = 8.dp))
                    
                    Button(
                        onClick = { viewModel.updatePlan("PREMIUM") },
                        enabled = subPlan != "PREMIUM",
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (subPlan == "PREMIUM") "Active Plan" else "Upgrade Premium")
                    }
                }
            }
        }

        item {
            TextButton(
                onClick = { viewModel.resetLimits(); Toast.makeText(viewModel.getApplication(), "Usage levels reset!", Toast.LENGTH_SHORT).show() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Reset limit levels for planning testing")
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
    val uncompletedCount = viewModel.getUncompletedNotesCount()
    val timetableItems by viewModel.timetableItems.collectAsState()

    var showSettingsDialog by remember { mutableStateOf(false) }

    // Request notification permission on first load
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    context as android.app.Activity,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    9001
                )
            }
        }
        NotificationHelper.createChannels(context)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "Notifications & Reminders",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Stay on top of your teaching day",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = { viewModel.clearNotifications() }) {
                    Icon(Icons.Default.ClearAll, "Clear all", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = { showSettingsDialog = true }) {
                    Icon(Icons.Default.Tune, "Alert Settings", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                }
            }
        }

        // Summary cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Wake-up status
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor =
                    if (prefs.wakeUpAlarmEnabled) Color(0xFF1B5E20).copy(alpha = 0.1f)
                    else MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Alarm, null, tint = if (prefs.wakeUpAlarmEnabled) Color(0xFF2E7D32) else Color.Gray, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Wake-up", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(
                        if (prefs.wakeUpAlarmEnabled) "${"%02d".format(prefs.wakeUpHour)}:${"%02d".format(prefs.wakeUpMinute)}" else "Off",
                        fontSize = 10.sp, color = Color.Gray
                    )
                }
            }
            // Reminders status
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor =
                    if (prefs.scheduleReminderEnabled) Color(0xFF0D47A1).copy(alpha = 0.1f)
                    else MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Schedule, null, tint = if (prefs.scheduleReminderEnabled) Color(0xFF1565C0) else Color.Gray, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Reminders", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(if (prefs.scheduleReminderEnabled) "${prefs.reminderMinutesBefore} min before" else "Off", fontSize = 10.sp, color = Color.Gray)
                }
            }
            // Missed alerts
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor =
                    if (prefs.missedScheduleAlerts) Color(0xFFB71C1C).copy(alpha = 0.1f)
                    else MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.ErrorOutline, null, tint = if (prefs.missedScheduleAlerts) Color(0xFFC62828) else Color.Gray, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Missed", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(if (prefs.missedScheduleAlerts) "On" else "Off", fontSize = 10.sp, color = Color.Gray)
                }
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
                            uncompletedNotesReminder = uncompletedNotes
                        )
                        viewModel.saveNotificationPrefs(prefs)

                        NotificationHelper.createChannels(context)

                        if (wakeUpEnabled) {
                            AlarmScheduler.scheduleWakeUpAlarm(context, wakeUpHour, wakeUpMinute)
                        } else {
                            AlarmScheduler.cancelWakeUpAlarm(context)
                        }

                        if (reminderEnabled || missedAlerts || uncompletedNotes) {
                            AlarmScheduler.scheduleScheduleCheck(context)
                        } else {
                            AlarmScheduler.cancelScheduleCheck(context)
                        }

                        viewModel.addNotification(
                            AppNotification(
                                id = NotificationIds.notifCounter++,
                                title = "Alert Settings Updated",
                                body = "Your notification preferences have been saved.",
                                type = NotificationType.WAKE_UP
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

fun SettingsScreen(viewModel: TeacherViewModel) {
    val con = LocalContext.current
    val name by viewModel.teacherName.collectAsState()
    val type by viewModel.teacherType.collectAsState()
    val schools by viewModel.teacherSchools.collectAsState()
    val plan by viewModel.subscriptionPlan.collectAsState()
    val notesCount by viewModel.usageLessonNotes.collectAsState()
    val mcqsCount by viewModel.usageMcqs.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "Settings & Account Center", 
                style = MaterialTheme.typography.titleMedium, 
                fontWeight = FontWeight.Bold, 
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Account / Profile Details card
        item {
            val userAccount by viewModel.currentUser.collectAsState()
            var isEditingProfile by remember { mutableStateOf(false) }

            var editName by remember { mutableStateOf("") }
            var editGender by remember { mutableStateOf("") }
            var editDob by remember { mutableStateOf("") }
            var editAddress by remember { mutableStateOf("") }
            var editPhone by remember { mutableStateOf("") }
            var editType by remember { mutableStateOf("FULL_TIME") }

            LaunchedEffect(isEditingProfile, userAccount) {
                if (isEditingProfile) {
                    val user = userAccount
                    editName = user?.fullName ?: name
                    editGender = user?.gender ?: ""
                    editDob = user?.dob ?: ""
                    editAddress = user?.address ?: ""
                    editPhone = user?.phone ?: ""
                    editType = user?.teachingStatus ?: type
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth().testTag("settings_profile_card"),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                        Text(
                            text = if (isEditingProfile) "Edit Profile Menu" else "Educator Identity Profile",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        
                        Icon(
                            imageVector = if (isEditingProfile) Icons.Default.EditNote else Icons.Default.Badge,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (!isEditingProfile) {
                        // Read Mode layout
                        val u = userAccount
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                                Column {
                                    Text("Full Name", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                    Text(u?.fullName?.ifEmpty { "Not specified" } ?: name.ifEmpty { "Not specified" }, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Icon(Icons.Default.Email, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                                Column {
                                    Text("Registered Account Email", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                    Text(u?.email ?: "Guest mode", style = MaterialTheme.typography.bodyMedium)
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Icon(Icons.Default.Wc, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                                    Column {
                                        Text("Gender", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                        Text(u?.gender?.ifEmpty { "None spec" } ?: "None spec", style = MaterialTheme.typography.bodyMedium)
                                    }
                                    Column {
                                        Text("Date of Birth", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                        Text(u?.dob?.ifEmpty { "None spec" } ?: "None spec", style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Icon(Icons.Default.Home, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                                Column {
                                    Text("Address", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                    Text(u?.address?.ifEmpty { "None spec" } ?: "None spec", style = MaterialTheme.typography.bodyMedium)
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Icon(Icons.Default.Phone, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                                Column {
                                    Text("Phone Number", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                    Text(u?.phone?.ifEmpty { "None spec" } ?: "None spec", style = MaterialTheme.typography.bodyMedium)
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Icon(Icons.Default.Work, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                                Column {
                                    Text("Teaching Status Category", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                    Text(if ((u?.teachingStatus ?: type) == "FULL_TIME") "Full-Time Centralized Site" else "Part-Time Multi-Institution", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                }
                            }

                            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Icon(Icons.Default.Business, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                                Column {
                                    Text("Registered Institutions of Record", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                    if (schools.isEmpty()) {
                                        Text("No sites listed", style = MaterialTheme.typography.bodyMedium, color = Color.LightGray)
                                    } else {
                                        schools.forEach { s ->
                                            Text("• ${s.ifEmpty { "Unnamed site" }}", style = MaterialTheme.typography.bodyMedium)
                                        }
                                    }
                                }
                            }

                            Divider(modifier = Modifier.padding(vertical = 4.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { isEditingProfile = true },
                                    modifier = Modifier.weight(1f).testTag("settings_edit_profile_btn"),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Edit Profile Details", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = { 
                                        viewModel.resetOnboarding()
                                        Toast.makeText(con, "Profile reset! Launching set-up onboarding...", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.weight(1f).testTag("settings_reset_profile"),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, Color.Red)
                                ) {
                                    Icon(Icons.Default.Refresh, "Reset profile", modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Reset & Re-Onboard", fontSize = 11.sp)
                                }
                            }
                        }
                    } else {
                        // Edit Mode Layout
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = editName,
                                onValueChange = { editName = it },
                                label = { Text("Full Name") },
                                leadingIcon = { Icon(Icons.Default.Person, null) },
                                modifier = Modifier.fillMaxWidth().testTag("edit_profile_name"),
                                singleLine = true
                            )

                            Text("Gender Identity:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("Male", "Female", "Other").forEach { g ->
                                    val isSelected = editGender == g
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { editGender = g },
                                        label = { Text(g) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = editDob,
                                onValueChange = { editDob = it },
                                label = { Text("Date of Birth (DD-MM-YYYY)") },
                                leadingIcon = { Icon(Icons.Default.CalendarToday, null) },
                                modifier = Modifier.fillMaxWidth().testTag("edit_profile_dob"),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = editAddress,
                                onValueChange = { editAddress = it },
                                label = { Text("Correspondence Address") },
                                leadingIcon = { Icon(Icons.Default.Home, null) },
                                modifier = Modifier.fillMaxWidth().testTag("edit_profile_address"),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = editPhone,
                                onValueChange = { editPhone = it },
                                label = { Text("Contact Phone") },
                                leadingIcon = { Icon(Icons.Default.Phone, null) },
                                modifier = Modifier.fillMaxWidth().testTag("edit_profile_phone"),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone)
                            )

                            Text("Teaching Status Category:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { editType = "FULL_TIME" },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (editType == "FULL_TIME") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                    border = if (editType == "FULL_TIME") BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text("Full-Time", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }

                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { editType = "PART_TIME" },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (editType == "PART_TIME") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                    border = if (editType == "PART_TIME") BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text("Part-Time", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { isEditingProfile = false },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Close, null)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Cancel")
                                }

                                Button(
                                    onClick = {
                                        if (editName.trim().isNotEmpty()) {
                                            viewModel.updateProfile(
                                                fullName = editName.trim(),
                                                gender = editGender,
                                                dob = editDob.trim(),
                                                address = editAddress.trim(),
                                                phone = editPhone.trim(),
                                                teachingStatus = editType
                                            )
                                            isEditingProfile = false
                                            Toast.makeText(con, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(con, "Name cannot be empty!", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.weight(1.2f).testTag("edit_profile_save_btn"),
                                    enabled = editName.trim().isNotEmpty()
                                ) {
                                    Icon(Icons.Default.Check, null)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Save Changes")
                                }
                            }
                        }
                    }
                }
            }
        }

        // AIS Active meters
        item {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Monthly AI Generation Usage Tracker", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Lesson Notes Active: $notesCount / ${if (plan == "FREE") "5 limit" else "Unlimited"}", fontSize = 12.sp)
                    if (plan == "FREE") {
                        LinearProgressIndicator(progress = { notesCount.toFloat() / 5f }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp))
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text("MCQ Compiler Items: $mcqsCount / ${if (plan == "FREE") "10 limit" else "Unlimited"}", fontSize = 12.sp)
                    if (plan == "FREE") {
                        LinearProgressIndicator(progress = { mcqsCount.toFloat() / 10f }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp))
                    }

                    if (plan != "FREE") {
                        Text("✨ Unlimited co-pilot access active under $plan tier!", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                    }
                }
            }
        }

        // Dynamic Plans Selection Header
        item {
            Text("Available Academic Plans & Upgrades", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
        }

        // Tiers
        // Tier 1: FREE
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                border = if (plan == "FREE") BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("🟢 Free Companion Plan", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("₦0 / Month", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                    }
                    Text("• 5 Curriculum-aligned Lesson Notes / month\n• 10 MCQs limits / month\n• Standard watermarked exports\n• Simple 1-School limit onboarding constraint.", fontSize = 11.sp, modifier = Modifier.padding(vertical = 8.dp))
                    
                    Button(
                        onClick = { viewModel.updatePlan("FREE") },
                        enabled = plan != "FREE",
                        modifier = Modifier.fillMaxWidth().testTag("plan_free_button")
                    ) {
                        Text(if (plan == "FREE") "Current Active Plan" else "Select Free")
                    }
                }
            }
        }

        // Tier 2: STANDARD
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                border = if (plan == "STANDARD") BorderStroke(2.dp, Color(0xFF1E88E5)) else null
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("🔵 Standard Plan pro", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("₦5,000 / Month", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF1E88E5))
                    }
                    Text("• UNLIMITED Nigeria-Standard Lesson Notes\n• UNLIMITED MCQ generation\n• UNLIMITED essay/theories generation + rubric rubrics\n• Full PDF & Word DOCX exports\n• Dynamic Syllabus checklists\n• Setup support for up to 3 Part-Time Schools.", fontSize = 11.sp, modifier = Modifier.padding(vertical = 8.dp))
                    
                    Button(
                        onClick = { viewModel.updatePlan("STANDARD") },
                        enabled = plan != "STANDARD",
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5)),
                        modifier = Modifier.fillMaxWidth().testTag("plan_standard_button")
                    ) {
                        Text(if (plan == "STANDARD") "Current Active Plan" else "Upgrade Standard")
                    }
                }
            }
        }

        // Tier 3: PREMIUM
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                border = if (plan == "PREMIUM") BorderStroke(2.dp, Color(0xFFE65100)) else null
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("⭐ Premium Power Tier", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("₦10,000 / Month", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFFE65100))
                    }
                    Text("• Everything in Standard Plan PLUS\n• Fast response AI servers priority\n• XLS MCQ student roster download sheets\n• Setup support for up to 10 Part-Time Schools.", fontSize = 11.sp, modifier = Modifier.padding(vertical = 8.dp))
                    
                    Button(
                        onClick = { viewModel.updatePlan("PREMIUM") },
                        enabled = plan != "PREMIUM",
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100)),
                        modifier = Modifier.fillMaxWidth().testTag("plan_premium_button")
                    ) {
                        Text(if (plan == "PREMIUM") "Current Active Plan" else "Upgrade Premium")
                    }
                }
            }
        }

        // Technical details card
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Document Export Design Defaults", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("Format target metrics: standard A4 portrait letter print", fontSize = 11.sp)
                    Text("Margins constraint: 0.75-inch academic border edge padding", fontSize = 11.sp)
                    Text("Verified syllabus version: latest West African National Curriculae", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        item {
            Box(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), contentAlignment = Alignment.Center) {
                Text("Teacher’s Companion v1.0\nMade natively in Nigeria and West Africa.", fontSize = 11.sp, textAlign = TextAlign.Center, color = Color.Gray)
            }
        }
    }
}


// ==========================================
// DEEP MODAL DIALOGS COMPOSABLES
// ==========================================

@Composable
fun LessonDetailsDialog(note: LessonNote, onDismiss: () -> Unit, onDelete: () -> Unit) {
    val con = LocalContext.current
    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.85f).padding(8.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("View Lesson Note", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Row {
                        IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null) }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        Text("Topic: ${note.topic}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("Subject: ${note.subject} | Grade: ${note.gradeClass} | Duration: ${note.duration}", fontSize = 12.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(note.presentation, fontSize = 13.sp)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, "Delete", tint = Color.Red.copy(alpha = 0.8f))
                    }
                    
                    Spacer(modifier = Modifier.weight(1f))

                    OutlinedButton(
                        onClick = {
                            val html = "<h1>${note.subject} Lesson Note</h1><h2>Class: ${note.gradeClass} | Topic: ${note.topic}</h2><div>" + 
                                    note.presentation.replace("\n", "<br>") + "</div>"
                            ExportService.exportToPdf(con, note.topic, html)
                        },
                        modifier = Modifier.size(height = 36.dp, width = 90.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("Print Pdf", fontSize = 11.sp)
                    }

                    Button(
                        onClick = {
                            val html = "<h1>${note.subject} Lesson Note</h1><h2>Class: ${note.gradeClass}</h2><p>" + 
                                    note.presentation.replace("\n", "<br>") + "</p>"
                            ExportService.exportToWord(con, note.topic, html)
                        },
                        modifier = Modifier.size(height = 36.dp, width = 100.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("Word Doc", fontSize = 11.sp)
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
        } catch (e: Exception) {
            emptyList()
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.85f).padding(8.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("MCQ Exam Details", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null) }
                }

                Text(mcqSet.title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text("${mcqSet.subject} · ${mcqSet.gradeClass}", fontSize = 12.sp, color = Color.Gray)

                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(parsedQuestions) { qItem ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(qItem.question, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("A) ${qItem.optionA}   B) ${qItem.optionB}\nC) ${qItem.optionC}   D) ${qItem.optionD}", fontSize = 11.sp, color = Color.DarkGray)
                                Text("Correct Answer: ${qItem.correctAnswer}", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                if (qItem.explanation.isNotEmpty()) {
                                    Text("Explanation: ${qItem.explanation}", fontSize = 10.sp, color = Color.Gray)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, "Delete", tint = Color.Red.copy(alpha = 0.8f))
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(
                            onClick = {
                                val exportList = parsedQuestions.map {
                                    McqExportItem(it.question, it.optionA, it.optionB, it.optionC, it.optionD, it.correctAnswer, it.explanation)
                                }
                                ExportService.exportToExcel(con, "${mcqSet.topic}_MCQs", exportList)
                            },
                            modifier = Modifier.size(height = 36.dp, width = 100.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("Excel Sheets", fontSize = 11.sp)
                        }

                        Button(
                            onClick = {
                                val html = "<h1>${mcqSet.subject} MCQ Exam</h1><h2>Class: ${mcqSet.gradeClass}</h2>" +
                                        parsedQuestions.joinToString("<br><br>") { "<b>Question:</b> ${it.question}<br>A) ${it.optionA} B) ${it.optionB} C) ${it.optionC} D) ${it.optionD}<br><b>Correct Answer: Option ${it.correctAnswer}</b>" }
                                ExportService.exportToPdf(con, mcqSet.topic, html)
                            },
                            modifier = Modifier.size(height = 36.dp, width = 90.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("Print Pdf", fontSize = 11.sp)
                        }
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

// ==========================================
// REGISTRATION, LOGIN, AND WIZARD FLOWS
// ==========================================

data class OnboardInstitution(
    val schoolName: String = "",
    val className: String = "JSS 1",
    val subjects: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(viewModel: TeacherViewModel) {
    val context = LocalContext.current
    var isLoginMode by remember { mutableStateOf(true) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    Scaffold { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .widthIn(max = 420.dp),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(26.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    Text(
                        text = "Teacher’s Companion",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Text(
                        text = if (isLoginMode) "Sign in to access your dashboard" else "Create a free collaborator account",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    if (errorMessage.isNotEmpty()) {
                        Text(
                            text = errorMessage,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it; errorMessage = "" },
                        label = { Text("Email Address") },
                        leadingIcon = { Icon(Icons.Default.Person, null) },
                        modifier = Modifier.fillMaxWidth().testTag("auth_email_input"),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Email
                        )
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it; errorMessage = "" },
                        label = { Text("Password") },
                        leadingIcon = { Icon(Icons.Default.Lock, null) },
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth().testTag("auth_password_input"),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Password
                        )
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    } else {
                        Button(
                            onClick = {
                                if (email.isBlank() || password.isBlank()) {
                                    errorMessage = "Please fill in all fields."
                                    return@Button
                                }
                                isLoading = true
                                if (isLoginMode) {
                                    viewModel.login(email, password) { success, msg ->
                                        isLoading = false
                                        if (!success) {
                                            errorMessage = msg
                                        }
                                    }
                                } else {
                                    viewModel.register(email, password) { success, msg ->
                                        isLoading = false
                                        if (!success) {
                                            errorMessage = msg
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("auth_submit_button")
                        ) {
                            Text(if (isLoginMode) "Sign In" else "Sign Up")
                        }

                        TextButton(
                            onClick = {
                                isLoginMode = !isLoginMode
                                errorMessage = ""
                            }
                        ) {
                            Text(
                                if (isLoginMode) 
                                    "Don't have an account? Sign Up Free" 
                                else 
                                    "Already have an account? Sign In"
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(viewModel: TeacherViewModel) {
    val context = LocalContext.current
    var currentStep by remember { mutableStateOf(1) }

    var fullName by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") } 
    var dob by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var isFullTime by remember { mutableStateOf(true) }

    val institutions = remember { mutableStateListOf<OnboardInstitution>().apply {
        add(OnboardInstitution())
    }}

    // First and last name validation: at least 2 words, each word at least 2 characters
    val isFirstAndLastName = remember(fullName) {
        val parts = fullName.trim().split("\\s+".toRegex()).filter { it.isNotEmpty() }
        parts.size >= 2 && parts.all { it.length >= 2 }
    }

    // Android Native Calendar picker setup for Date of Birth
    val calendar = remember { java.util.Calendar.getInstance() }
    val datePickerDialog = remember {
        android.app.DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                dob = String.format("%02d-%02d-%d", dayOfMonth, month + 1, year)
            },
            calendar.get(java.util.Calendar.YEAR) - 30, // Default opening around age 30
            calendar.get(java.util.Calendar.MONTH),
            calendar.get(java.util.Calendar.DAY_OF_MONTH)
        )
    }

    val isValidDob = remember(dob) {
        dob.matches("^\\d{1,2}-\\d{1,2}-\\d{4}$".toRegex())
    }

    // International format: must start with '+' followed by country code and digits
    val isValidPhone = remember(phone) {
        val cleaned = phone.replace("\\s".toRegex(), "").replace("-", "").trim()
        cleaned.matches("^\\+[1-9]\\d{9,14}$".toRegex())
    }

    val isValidAddress = remember(address) {
        address.trim().length >= 5
    }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Professional Profile Setup",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                // High polish step dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (step in 1..5) {
                        val active = step == currentStep
                        val passed = step < currentStep
                        val color = if (active || passed) MaterialTheme.colorScheme.primary else Color.LightGray
                        val size = if (active) 10.dp else 8.dp
                        Box(
                            modifier = Modifier
                                .size(size)
                                .background(color, CircleShape)
                        )
                    }
                }

                Text(
                    text = "Step $currentStep of 5",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 480.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    when (currentStep) {
                        1 -> {
                            Text(
                                text = "1. Full Name",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Please enter your first and last name as they should appear on all formal lesson structures.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            OutlinedTextField(
                                value = fullName,
                                onValueChange = { fullName = it },
                                label = { Text("First & Last Name (e.g. Aisha Okafor)") },
                                leadingIcon = { Icon(Icons.Default.Person, null) },
                                modifier = Modifier.fillMaxWidth().testTag("onboarding_fullname"),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                                isError = fullName.isNotEmpty() && !isFirstAndLastName
                            )
                            if (fullName.isNotEmpty() && !isFirstAndLastName) {
                                Text(
                                    text = "Please enter both your first and last name (each at least 2 characters).",
                                    color = Color.Red,
                                    fontSize = 11.sp
                                )
                            }
                        }
                        2 -> {
                            Text(
                                text = "2. Demographic Context",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            
                            Text("Gender Identity:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("Male", "Female", "Other").forEach { option ->
                                    val isSelected = gender == option
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { gender = option },
                                        label = { Text(option) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text("Date of Birth:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { datePickerDialog.show() }
                            ) {
                                OutlinedTextField(
                                    value = dob,
                                    onValueChange = { },
                                    readOnly = true,
                                    enabled = false,
                                    label = { Text("Select Date of Birth") },
                                    placeholder = { Text("Tap to pick from Calendar") },
                                    leadingIcon = { Icon(Icons.Default.CalendarToday, null) },
                                    trailingIcon = {
                                        IconButton(onClick = { datePickerDialog.show() }) {
                                            Icon(Icons.Default.CalendarMonth, "Pick date")
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().testTag("onboarding_dob"),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                        disabledBorderColor = if (isValidDob) MaterialTheme.colorScheme.outline else Color.Red.copy(alpha = 0.6f),
                                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        disabledTrailingIconColor = MaterialTheme.colorScheme.primary
                                    )
                                )
                            }
                            if (dob.isNotEmpty() && !isValidDob) {
                                Text(
                                    text = "Invalid date format. Please picker from calendar.",
                                    color = Color.Red,
                                    fontSize = 11.sp
                                )
                            }
                        }
                        3 -> {
                            Text(
                                text = "3. Contact Identity",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            
                            OutlinedTextField(
                                value = address,
                                onValueChange = { address = it },
                                label = { Text("Address") },
                                placeholder = { Text("e.g. 12 Milton Road, Enugu") },
                                modifier = Modifier.fillMaxWidth().testTag("onboarding_address"),
                                singleLine = true,
                                isError = address.isNotEmpty() && !isValidAddress
                            )
                            if (address.isNotEmpty() && !isValidAddress) {
                                Text(
                                    text = "Please enter a valid street address (minimum 5 characters).",
                                    color = Color.Red,
                                    fontSize = 11.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            OutlinedTextField(
                                value = phone,
                                onValueChange = { phone = it },
                                label = { Text("Phone Number (International format)") },
                                placeholder = { Text("e.g. +2348035551212") },
                                leadingIcon = { Icon(Icons.Default.Phone, null) },
                                modifier = Modifier.fillMaxWidth().testTag("onboarding_phone"),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone),
                                isError = phone.isNotEmpty() && !isValidPhone
                            )
                            if (phone.isNotEmpty() && !isValidPhone) {
                                Text(
                                    text = "Please enter phone number starting with Country Code + (e.g., +2348035551212).",
                                    color = Color.Red,
                                    fontSize = 11.sp
                                )
                            }
                        }
                        4 -> {
                            Text(
                                text = "4. Teaching Status Category",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "What is your main employment tier?",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { 
                                            isFullTime = true 
                                            while (institutions.size > 1) {
                                                institutions.removeAt(institutions.lastIndex)
                                            }
                                        },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isFullTime) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                    border = if (isFullTime) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(Icons.Default.Business, null, tint = if (isFullTime) MaterialTheme.colorScheme.primary else Color.Gray)
                                        Text("Full-Time", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("One central site", fontSize = 10.sp, color = Color.Gray, textAlign = TextAlign.Center)
                                    }
                                }

                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { isFullTime = false },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (!isFullTime) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                    border = if (!isFullTime) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(Icons.Default.Schedule, null, tint = if (!isFullTime) MaterialTheme.colorScheme.primary else Color.Gray)
                                        Text("Part-Time", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("Multi-institution", fontSize = 10.sp, color = Color.Gray, textAlign = TextAlign.Center)
                                    }
                                }
                            }
                        }
                        5 -> {
                            Text(
                                text = "5. Institution Details",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Specify your school name below to complete the setup.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            institutions.forEachIndexed { i, inst ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "Institution #${i + 1}",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )

                                    OutlinedTextField(
                                        value = inst.schoolName,
                                        onValueChange = { text ->
                                            institutions[i] = inst.copy(schoolName = text)
                                        },
                                        label = { Text("School Name") },
                                        placeholder = { Text("e.g. Community Girls Sec School") },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )

                                    if (!isFullTime && institutions.size > 1) {
                                        TextButton(
                                            onClick = { institutions.removeAt(i) },
                                            colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                                        ) {
                                            Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Remove")
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                            }

                            if (!isFullTime) {
                                Button(
                                    onClick = { institutions.add(OnboardInstitution()) },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                ) {
                                    Icon(Icons.Default.Add, null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Add more Institution")
                                }
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 480.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = {
                        if (currentStep > 1) currentStep--
                    },
                    enabled = currentStep > 1,
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    Icon(Icons.Default.ArrowBack, null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Back")
                }

                Spacer(modifier = Modifier.width(16.dp))

                val isValid = when (currentStep) {
                    1 -> isFirstAndLastName
                    2 -> gender.trim().isNotEmpty() && isValidDob
                    3 -> address.trim().isNotEmpty() && isValidPhone
                    4 -> true
                    5 -> institutions.all { it.schoolName.trim().isNotEmpty() }
                    else -> false
                }

                Button(
                    onClick = {
                        if (currentStep < 5) {
                            currentStep++
                        } else {
                            val listClasses = mutableListOf<SchoolClass>()
                            val listOfSchools = mutableListOf<String>()

                            institutions.forEach { inst ->
                                val sName = inst.schoolName.trim()
                                if (sName.isNotEmpty() && !listOfSchools.contains(sName)) {
                                    listOfSchools.add(sName)
                                }
                                // Pre-seed general default classes/subjects to provide immediate working values
                                val defaultClasses = listOf("SS 1", "SS 2", "SS 3", "JSS 1", "JSS 2", "JSS 3", "Primary 5")
                                val defaultSubjects = listOf("Mathematics", "English Language", "Basic Science", "Biology")
                                defaultClasses.forEach { cls ->
                                    defaultSubjects.forEach { subj ->
                                        listClasses.add(
                                            SchoolClass(
                                                className = cls,
                                                schoolName = sName,
                                                subject = subj
                                            )
                                        )
                                    }
                                }
                            }

                            viewModel.finishProfileOnboarding(
                                fullName = fullName.trim(),
                                gender = gender,
                                dob = dob.trim(),
                                address = address.trim(),
                                phone = phone.trim(),
                                teachingStatus = if (isFullTime) "FULL_TIME" else "PART_TIME",
                                schools = listOfSchools,
                                classes = listClasses
                            )
                            Toast.makeText(context, "Completed onboarding successfully!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = isValid,
                    modifier = Modifier.weight(1.5f).height(48.dp).testTag("onboarding_next_button")
                ) {
                    Text(if (currentStep == 5) "Submit and Launch" else "Next")
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(if (currentStep == 5) Icons.Default.Check else Icons.Default.ArrowForward, null)
                }
            }
        }
    }
}

/*

            // Employment Category Card Selector
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Teaching Status Category",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Full Time
                        Card(
                             modifier = Modifier
                                .weight(1f)
                                .clickable { 
                                    isFullTime = true 
                                    // clamp schools length to 1
                                    while (schools.size > 1) {
                                        schools.removeAt(schools.lastIndex)
                                    }
                                    onboardClasses.removeAll { it.schoolId > 0 }
                                }
                                .testTag("onboarding_fulltime"),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isFullTime) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            border = if (isFullTime) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                             ) {
                                Icon(Icons.Default.Business, null, tint = if (isFullTime) MaterialTheme.colorScheme.primary else Color.Gray)
                                Text("Full-Time", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("Works at 1 central school site.", style = MaterialTheme.typography.bodySmall, fontSize = 10.sp, textAlign = TextAlign.Center, color = Color.Gray)
                            }
                        }

                        // Part Time
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { isFullTime = false }
                                .testTag("onboarding_parttime"),
                            colors = CardDefaults.cardColors(
                                containerColor = if (!isFullTime) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            border = if (!isFullTime) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.Schedule, null, tint = if (!isFullTime) MaterialTheme.colorScheme.primary else Color.Gray)
                                Text("Part-Time", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("Teaches across multiple school locations.", style = MaterialTheme.typography.bodySmall, fontSize = 10.sp, textAlign = TextAlign.Center, color = Color.Gray)
                            }
                        }
                    }
                }
            }

            // Schools list
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = if (isFullTime) "School Location & Affiliated Classes" else "Registered School Locations & Affiliated Classes",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    schools.forEachIndexed { index, schoolValue ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = schoolValue,
                                    onValueChange = { schools[index] = it },
                                    label = { Text(if (isFullTime) "Primary School Name (Sentence Case)" else "School Location ${index + 1} Name") },
                                    leadingIcon = { Icon(Icons.Default.LocationOn, null) },
                                    modifier = Modifier.weight(1f).testTag("onboarding_school_$index"),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                                )
                                
                                if (!isFullTime && schools.size > 1) {
                                    IconButton(
                                        onClick = { 
                                            schools.removeAt(index)
                                            onboardClasses.removeAll { it.schoolId == index }
                                            val shifted = onboardClasses.map { classInput ->
                                                if (classInput.schoolId > index) {
                                                    classInput.copy(schoolId = classInput.schoolId - 1)
                                                } else {
                                                    classInput
                                                }
                                            }
                                            onboardClasses.clear()
                                            onboardClasses.addAll(shifted)
                                        }
                                    ) {
                                        Icon(Icons.Default.Delete, "Remove school", tint = Color.Red.copy(alpha = 0.7f))
                                    }
                                }
                            }

                            // Sub list of classes for this school
                            Text(
                                text = "Classes & Subjects at this School:",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.secondary
                            )

                            val schoolClassesFiltered = onboardClasses.filter { it.schoolId == index }
                            
                            schoolClassesFiltered.forEach { classInput ->
                                val classInputIndex = onboardClasses.indexOf(classInput)
                                if (classInputIndex >= 0) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                                            .padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // Header Row: Class Label and Remove Class Button
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Class Level Selection",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            
                                            if (onboardClasses.count { it.schoolId == index } > 1) {
                                                IconButton(
                                                    onClick = { onboardClasses.removeAt(classInputIndex) },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Close,
                                                        contentDescription = "Remove Class Group",
                                                        tint = Color.Red.copy(alpha = 0.7f),
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }

                                        // Dropdown Selector for Predefined Classes
                                        var classDropdownExpanded by remember { mutableStateOf(false) }
                                        val predefinedClasses = listOf(
                                            "Nursery 1", "Nursery 2", 
                                            "Primary 1", "Primary 2", "Primary 3", "Primary 4", "Primary 5", 
                                            "JSS 1", "JSS 2", "JSS 3", 
                                            "SSS 1", "SSS 2", "SSS 3"
                                        )

                                        Box(modifier = Modifier.fillMaxWidth()) {
                                            OutlinedButton(
                                                onClick = { classDropdownExpanded = true },
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                                            ) {
                                                Icon(Icons.Default.School, null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "Class: ${classInput.className}",
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                Spacer(modifier = Modifier.weight(1f))
                                                Icon(Icons.Default.ArrowDropDown, null)
                                            }
                                            DropdownMenu(
                                                expanded = classDropdownExpanded,
                                                onDismissRequest = { classDropdownExpanded = false },
                                                modifier = Modifier.fillMaxWidth(0.85f)
                                            ) {
                                                predefinedClasses.forEach { option ->
                                                    DropdownMenuItem(
                                                        text = { Text(option) },
                                                        onClick = {
                                                            onboardClasses[classInputIndex] = classInput.copy(className = option)
                                                            classDropdownExpanded = false
                                                        }
                                                    )
                                                }
                                            }
                                        }

                                        // Associated subjects list
                                        Text(
                                            text = "Core Subject Focus ($predefinedClasses is set above):",
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        classInput.subjects.forEachIndexed { subjIndex, subjectName ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                OutlinedTextField(
                                                    value = subjectName,
                                                    onValueChange = { newVal ->
                                                        val updatedSubjects = classInput.subjects.toMutableList().apply {
                                                            this[subjIndex] = newVal
                                                        }
                                                        onboardClasses[classInputIndex] = classInput.copy(subjects = updatedSubjects)
                                                    },
                                                    label = { Text("Subject Name (e.g. Mathematics)") },
                                                    placeholder = { Text("e.g. Mathematics") },
                                                    modifier = Modifier.weight(1f),
                                                    singleLine = true,
                                                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                                                )

                                                if (classInput.subjects.size > 1) {
                                                    IconButton(
                                                        onClick = {
                                                            val updatedSubjects = classInput.subjects.toMutableList().apply {
                                                                removeAt(subjIndex)
                                                            }
                                                            onboardClasses[classInputIndex] = classInput.copy(subjects = updatedSubjects)
                                                        }
                                                    ) {
                                                        Icon(Icons.Default.Delete, "Remove Subject", tint = Color.Red.copy(alpha = 0.6f))
                                                    }
                                                }
                                            }
                                        }

                                        // Button to add subject to this specific class
                                        TextButton(
                                            onClick = {
                                                val updatedSubjects = classInput.subjects + ""
                                                onboardClasses[classInputIndex] = classInput.copy(subjects = updatedSubjects)
                                            },
                                            modifier = Modifier.align(Alignment.End),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Icon(Icons.Default.Add, null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Add Subject to this Class", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                }
                            }

                            TextButton(
                                onClick = { onboardClasses.add(OnboardClassInput(schoolId = index, className = "JSS 1", subjects = listOf(""))) },
                                modifier = Modifier.align(Alignment.Start)
                            ) {
                                Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Class Level Set", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    
                    if (!isFullTime) {
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        if (schools.size < maxSchools) {
                            Button(
                                onClick = { 
                                    schools.add("")
                                    onboardClasses.add(OnboardClassInput(schoolId = schools.size - 1, className = "JSS 1", subjects = listOf("")))
                                },
                                modifier = Modifier.fillMaxWidth().testTag("onboarding_add_school"),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                            ) {
                                Icon(Icons.Default.Add, null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add School Location")
                            }
                        } else {
                            // Display warning layout if they hit the subscription plan tier limit
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Nesting Limit Reached", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                                        Text("Your active $subscriptionPlan plan is restricted to $maxSchools school profile registration. Save profile context or upgrade subscription in Settings.", fontSize = 10.sp, color = Color.DarkGray)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Complete onboarding button
            val canSubmit = name.trim().isNotEmpty() && 
                            schools.isNotEmpty() && 
                            schools.all { it.trim().isNotEmpty() } &&
                            onboardClasses.isNotEmpty() &&
                            onboardClasses.all { item ->
                                item.className.trim().isNotEmpty() &&
                                item.subjects.isNotEmpty() &&
                                item.subjects.all { it.trim().isNotEmpty() }
                            }
            
            Button(
                onClick = {
                    if (canSubmit) {
                        val finalSchoolClasses = mutableListOf<SchoolClass>()
                        onboardClasses.forEach { item ->
                            val schoolName = schools.getOrNull(item.schoolId)?.trim() ?: ""
                            item.subjects.forEach { subj ->
                                if (subj.trim().isNotEmpty()) {
                                    finalSchoolClasses.add(
                                        SchoolClass(
                                            className = item.className.trim(),
                                            schoolName = schoolName,
                                            subject = subj.trim()
                                        )
                                    )
                                }
                            }
                        }
                        viewModel.finishOnboarding(
                            name = name.trim(),
                            type = if (isFullTime) "FULL_TIME" else "PART_TIME",
                            schools = schools.filter { it.trim().isNotEmpty() }.map { it.trim() },
                            classes = finalSchoolClasses
                        )
                        Toast.makeText(context, "Completed onboarding successfully!", Toast.LENGTH_SHORT).show()
                    }
                },
                enabled = canSubmit,
                modifier = Modifier.fillMaxWidth().height(52.dp).testTag("onboarding_submit")
            ) {
                Icon(Icons.Default.Check, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save and Launch Assistant")
            }
        }
    }
}
*/
