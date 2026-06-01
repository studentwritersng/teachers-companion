package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.api.GeminiGenerator
import com.example.api.PaystackManager
import com.example.api.supabase.SupabaseClient
import com.example.data.*
import com.example.notification.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.github.jan.supabase.gotrue.providers.builtin.Email
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject

const val PLAN_BASIC = "BASIC"
const val PLAN_ADVANCE = "ADVANCE"
const val PLAN_PREMIUM = "PREMIUM"

data class PlanInfo(
    val name: String,
    val displayName: String,
    val priceNaira: Int,
    val maxGenerations: Int,
    val color: Long,
    val features: List<String>
)

fun getPlanInfo(plan: String): PlanInfo = when (plan) {
    PLAN_BASIC -> PlanInfo(
        PLAN_BASIC, "Basic", 1000, 0, 0xFF4CAF50,
        listOf("Timetable & class management", "Syllabus tracking", "Student attendance", "No AI generation")
    )
    PLAN_ADVANCE -> PlanInfo(
        PLAN_ADVANCE, "Advance", 2000, 20, 0xFF1E88E5,
        listOf("Everything in Basic", "20 AI generations/month", "Lesson notes, MCQs & Theory", "PDF, Word & Excel exports")
    )
    PLAN_PREMIUM -> PlanInfo(
        PLAN_PREMIUM, "Premium", 4000, 50, 0xFFE65100,
        listOf("Everything in Advance", "50 AI generations/month", "Priority AI processing", "Unlimited exports")
    )
    else -> getPlanInfo(PLAN_BASIC)
}

// --- States representing AI actions ---

sealed interface AiGenerationState<out T> {
    object Idle : AiGenerationState<Nothing>
    object Generating : AiGenerationState<Nothing>
    data class Success<out T>(val data: T) : AiGenerationState<T>
    data class Error(val message: String) : AiGenerationState<Nothing>
}

// --- Moshi Models for UI and Deserialization ---

data class McqModel(
    val question: String,
    val optionA: String,
    val optionB: String,
    val optionC: String,
    val optionD: String,
    val correctAnswer: String,
    val explanation: String = ""
)

data class TheoryModel(
    val question: String,
    val markingScheme: String
)

class TeacherViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SupabaseRepository(
        (application as com.example.TeacherCompanionApp).database.teacherDao()
    )
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()

    // --- AUTHENTICATION & TEACHER PROFILE STATE ---
    private val _currentUser = MutableStateFlow<UserAccount?>(null)
    val currentUser: StateFlow<UserAccount?> = _currentUser.asStateFlow()

    private val _onboardingCompleted = MutableStateFlow(false)
    val onboardingCompleted: StateFlow<Boolean> = _onboardingCompleted.asStateFlow()

    private val _isDarkMode = MutableStateFlow(false)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _teacherName = MutableStateFlow("")
    val teacherName: StateFlow<String> = _teacherName.asStateFlow()

    private val _teacherType = MutableStateFlow("")
    val teacherType: StateFlow<String> = _teacherType.asStateFlow()

    private val _teacherSchools = MutableStateFlow<List<String>>(emptyList())
    val teacherSchools: StateFlow<List<String>> = _teacherSchools.asStateFlow()

    // --- SUBSCRIPTION & USAGE STATE ---
    private val _subscriptionPlan = MutableStateFlow(PLAN_BASIC)
    val subscriptionPlan: StateFlow<String> = _subscriptionPlan.asStateFlow()

    private val _usageGenerations = MutableStateFlow(0)
    val usageGenerations: StateFlow<Int> = _usageGenerations.asStateFlow()

    private var _paystackManager: PaystackManager? = null

    fun initPaystack(activity: android.app.Activity) {
        _paystackManager = PaystackManager(activity)
    }

    // --- NOTIFICATION STATE ---
    private val _notificationPrefs = MutableStateFlow(NotificationPrefs())
    val notificationPrefs: StateFlow<NotificationPrefs> = _notificationPrefs.asStateFlow()

    private val _notifications = MutableStateFlow<List<AppNotification>>(emptyList())
    val notifications: StateFlow<List<AppNotification>> = _notifications.asStateFlow()

    // --- SESSION STATE ---
    private val _sessionExpired = MutableStateFlow(false)
    val sessionExpired: StateFlow<Boolean> = _sessionExpired.asStateFlow()

    fun clearSessionExpired() { _sessionExpired.value = false }

    // --- SYNC STATUS ---
    val syncStatus: StateFlow<String> = repository.syncStatus
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "idle")

    // --- REACTIVE DATA FLOWS FROM SUPABASE ---
    val lessonNotes: StateFlow<List<LessonNote>> = repository.allLessonNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val mcqSets: StateFlow<List<MCQSet>> = repository.allMCQSets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val theorySets: StateFlow<List<TheorySet>> = repository.allTheorySets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val timetableItems: StateFlow<List<TimetableItem>> = repository.allTimetableItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val syllabusItems: StateFlow<List<SyllabusItem>> = repository.allSyllabusItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val schoolClasses: StateFlow<List<SchoolClass>> = repository.allSchoolClasses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- AI HUB GENERATION STATES ---
    private val _lessonGenerationState = MutableStateFlow<AiGenerationState<String>>(AiGenerationState.Idle)
    val lessonGenerationState: StateFlow<AiGenerationState<String>> = _lessonGenerationState.asStateFlow()

    private val _mcqGenerationState = MutableStateFlow<AiGenerationState<List<McqModel>>>(AiGenerationState.Idle)
    val mcqGenerationState: StateFlow<AiGenerationState<List<McqModel>>> = _mcqGenerationState.asStateFlow()

    private val _theoryGenerationState = MutableStateFlow<AiGenerationState<List<TheoryModel>>>(AiGenerationState.Idle)
    val theoryGenerationState: StateFlow<AiGenerationState<List<TheoryModel>>> = _theoryGenerationState.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                // Restore Supabase session if available
                val session = SupabaseClient.auth.currentSessionOrNull()
                if (session != null) {
                    val email = session.user?.email ?: ""
                    if (email.isNotEmpty()) {
                        val account = repository.getUserAccount(email)
                        if (account != null) {
                            _currentUser.value = account
                            _onboardingCompleted.value = account.isOnboardingCompleted
                            _subscriptionPlan.value = account.subscriptionPlan
                            _teacherName.value = account.fullName
                            _teacherType.value = account.teachingStatus

                            val schoolsJson = repository.getPreference("teacher_schools_${account.email}") ?: ""
                            _teacherSchools.value = if (schoolsJson.isEmpty()) {
                                emptyList()
                            } else {
                                try {
                                    val listType = Types.newParameterizedType(List::class.java, String::class.java)
                                    moshi.adapter<List<String>>(listType).fromJson(schoolsJson) ?: emptyList()
                                } catch (_: Exception) { emptyList() }
                            }

                            repository.refreshAll()
                            cacheDataForNotifications()
                        }
                    }
                }
            } catch (_: Exception) {
                // No session, stays logged out
            }

            _isDarkMode.value = repository.getPreference("is_dark_mode") == "true"

            // Migrate old plan names (FREE→BASIC, STANDARD→ADVANCE)
            val migratedPlan = repository.migratePlanIfNeeded()
            _subscriptionPlan.value = migratedPlan
            _usageGenerations.value = repository.getGenerationCount()

            // Load notification preferences
            loadNotificationPrefs()

            val loggedInEmail = repository.getPreference("logged_in_email") ?: ""

            if (_currentUser.value == null && loggedInEmail.isNotEmpty()) {
                val acc = repository.getUserAccount(loggedInEmail)
                if (acc != null) {
                    _currentUser.value = acc
                    _onboardingCompleted.value = acc.isOnboardingCompleted
                    _subscriptionPlan.value = acc.subscriptionPlan
                    _teacherName.value = acc.fullName
                    _teacherType.value = acc.teachingStatus

                    val schoolsJson = repository.getPreference("teacher_schools_${acc.email}") ?: ""
                    _teacherSchools.value = if (schoolsJson.isEmpty()) {
                        emptyList()
                    } else {
                        try {
                            val listType = Types.newParameterizedType(List::class.java, String::class.java)
                            moshi.adapter<List<String>>(listType).fromJson(schoolsJson) ?: emptyList()
                        } catch (_: Exception) { emptyList() }
                    }

                    repository.refreshAll()
                    cacheDataForNotifications()
                    _sessionExpired.value = true
                }
            }
        }
    }

    fun toggleDarkMode() {
        viewModelScope.launch {
            val newValue = !_isDarkMode.value
            _isDarkMode.value = newValue
            repository.setPreference("is_dark_mode", newValue.toString())
        }
    }

    // --- AUTHENTICATION ACTIONS ---

    fun register(email: String, password: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val trimmedEmail = email.trim().lowercase()
            if (trimmedEmail.isEmpty() || password.isEmpty()) {
                onResult(false, "Email and password cannot be empty.")
                return@launch
            }
            try {
                SupabaseClient.auth.signUpWith(Email) { this.email = trimmedEmail; this.password = password }
                // After sign-up, the DB trigger creates user_accounts row.
                // We insert additional profile fields separately.
                val newAcc = UserAccount(
                    email = trimmedEmail,
                    passwordHash = password,
                    isOnboardingCompleted = false,
                    subscriptionPlan = PLAN_BASIC
                )
                repository.insertUserAccount(newAcc)
                login(trimmedEmail, password, onResult)
            } catch (e: Exception) {
                val msg = e.message ?: "Registration failed"
                if (msg.contains("already", ignoreCase = true)) {
                    onResult(false, "An account with this email already exists.")
                } else {
                    onResult(false, msg)
                }
            }
        }
    }

    fun login(email: String, password: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val trimmedEmail = email.trim().lowercase()
            if (trimmedEmail.isEmpty() || password.isEmpty()) {
                onResult(false, "Fields cannot be empty.")
                return@launch
            }
            try {
                SupabaseClient.auth.signInWith(Email) { this.email = trimmedEmail; this.password = password }
                val acc = repository.getUserAccount(trimmedEmail)
                if (acc == null) {
                    onResult(false, "Account not found. Please register first.")
                    return@launch
                }
                repository.setPreference("logged_in_email", trimmedEmail)
                _currentUser.value = acc
                _onboardingCompleted.value = acc.isOnboardingCompleted
                _subscriptionPlan.value = acc.subscriptionPlan
                _teacherName.value = acc.fullName
                _teacherType.value = acc.teachingStatus

                val schoolsJson = repository.getPreference("teacher_schools_${acc.email}") ?: ""
                _teacherSchools.value = if (schoolsJson.isEmpty()) {
                    emptyList()
                } else {
                    try {
                        val listType = Types.newParameterizedType(List::class.java, String::class.java)
                        moshi.adapter<List<String>>(listType).fromJson(schoolsJson) ?: emptyList()
                    } catch (_: Exception) { emptyList() }
                }

                repository.refreshAll()
                delay(200)
                cacheDataForNotifications()
                onResult(true, "Successfully logged in!")
            } catch (e: Exception) {
                val msg = e.message ?: ""
                if (msg.contains("Invalid login credentials", ignoreCase = true)) {
                    onResult(false, "Invalid email or password.")
                } else {
                    onResult(false, msg)
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            try {
                SupabaseClient.auth.signOut()
            } catch (_: Exception) {}
            repository.setPreference("logged_in_email", "")
            _currentUser.value = null
            _onboardingCompleted.value = false
            _teacherName.value = ""
            _teacherType.value = "FULL_TIME"
            _teacherSchools.value = emptyList()

            val context = getApplication<Application>()
            val notifPrefs = context.getSharedPreferences("notification_prefs", Context.MODE_PRIVATE)
            notifPrefs.edit()
                .putString("timetable_cache", "[]")
                .putString("syllabus_cache", "[]")
                .putString("lesson_notes_cache", "[]")
                .apply()
            com.example.notification.AlarmScheduler.cancelScheduleCheck(context)
        }
    }

    // --- ONBOARDING ACTIONS ---
    fun finishProfileOnboarding(
        fullName: String,
        gender: String,
        dob: String,
        address: String,
        phone: String,
        teachingStatus: String,
        schools: List<String>,
        classes: List<SchoolClass>
    ) {
        viewModelScope.launch {
            val user = _currentUser.value ?: return@launch
            val updatedUser = user.copy(
                fullName = fullName,
                gender = gender,
                dob = dob,
                address = address,
                phone = phone,
                teachingStatus = teachingStatus,
                isOnboardingCompleted = true
            )
            repository.insertUserAccount(updatedUser)
            _currentUser.value = updatedUser
            _onboardingCompleted.value = true
            _teacherName.value = fullName
            _teacherType.value = teachingStatus
            _teacherSchools.value = schools

            try {
                val listType = Types.newParameterizedType(List::class.java, String::class.java)
                val schoolsJson = moshi.adapter<List<String>>(listType).toJson(schools)
                repository.setPreference("teacher_schools_${user.email}", schoolsJson)
            } catch (_: Exception) {}

            // Replace existing classes for the onboarded schools
            classes.forEach { schoolClass ->
                repository.insertSchoolClass(schoolClass)
            }
        }
    }

    fun updateProfile(
        fullName: String,
        gender: String,
        dob: String,
        address: String,
        phone: String,
        teachingStatus: String
    ) {
        viewModelScope.launch {
            val user = _currentUser.value ?: return@launch
            val updatedUser = user.copy(
                fullName = fullName,
                gender = gender,
                dob = dob,
                address = address,
                phone = phone,
                teachingStatus = teachingStatus
            )
            repository.insertUserAccount(updatedUser)
            _currentUser.value = updatedUser
            _teacherName.value = fullName
            _teacherType.value = teachingStatus
        }
    }

    fun resetOnboarding() {
        viewModelScope.launch {
            val user = _currentUser.value
            if (user != null) {
                val updated = user.copy(isOnboardingCompleted = false)
                repository.insertUserAccount(updated)
                _currentUser.value = updated
            }
            _onboardingCompleted.value = false
        }
    }

    fun updateTeacherType(type: String) {
        viewModelScope.launch {
            val user = _currentUser.value
            if (user != null) {
                val updated = user.copy(teachingStatus = type)
                repository.insertUserAccount(updated)
                _currentUser.value = updated
            }
            _teacherType.value = type
        }
    }

    // --- SUBSCRIPTION HELPERS ---

    private fun canGenerate(): String? {
        val plan = _subscriptionPlan.value
        val planInfo = getPlanInfo(plan)
        if (planInfo.maxGenerations == 0) {
            return "Your $planInfo.displayName plan (₦${planInfo.priceNaira}/mo) does not include AI generation. Upgrade to Advance or Premium."
        }
        val used = _usageGenerations.value
        if (used >= planInfo.maxGenerations) {
            return "You've used all $used/$used AI generations this month. Upgrade to a higher tier or wait for reset."
        }
        return null
    }

    private suspend fun recordGeneration() {
        repository.incrementGenerationCount()
        _usageGenerations.value = repository.getGenerationCount()
    }

    // --- CHANGE SUBSCRIPTION ---
    fun updatePlan(newPlan: String) {
        viewModelScope.launch {
            repository.setSubscriptionPlan(newPlan)
            _subscriptionPlan.value = newPlan
            val user = _currentUser.value
            if (user != null) {
                val updated = user.copy(subscriptionPlan = newPlan)
                repository.insertUserAccount(updated)
                _currentUser.value = updated
            }
        }
    }

    // --- PAYSTACK PAYMENT ---
    fun payForPlan(planName: String, email: String, onResult: (Boolean, String) -> Unit) {
        val pm = _paystackManager ?: run {
            onResult(false, "Payment not initialised. Try again.")
            return
        }
        if (!pm.isConfigured()) {
            onResult(false, "Paystack not configured. Add PAYSTACK_PUBLIC_KEY to .env")
            return
        }

        val planInfo = getPlanInfo(planName)
        val amountKobo = planInfo.priceNaira * 100
        val reference = pm.generateReference()

        pm.chargeCard(
            email = email,
            amountInKobo = amountKobo,
            reference = reference,
            onSuccess = { transaction ->
                viewModelScope.launch {
                    updatePlan(planName)
                    val user = _currentUser.value
                    if (user != null) {
                        val updated = user.copy(
                            subscriptionPlan = planName,
                            paymentEmail = email,
                            lastPaymentReference = transaction.reference ?: reference,
                            lastPaymentDate = System.currentTimeMillis()
                        )
                        repository.insertUserAccount(updated)
                        _currentUser.value = updated
                    }
                    onResult(true, "Payment successful! You're now on the ${planInfo.displayName} plan.")
                }
            },
            onError = { message ->
                onResult(false, message)
            }
        )
    }

    fun resetLimits() {
        viewModelScope.launch {
            repository.resetMonthlyLimits()
            _usageGenerations.value = 0
        }
    }

    // --- AI METHOD LOGICS WITH LIMIT SAFETY CHECKS ---

    fun generateLessonNote(
        subject: String,
        gradeClass: String,
        topic: String,
        duration: String,
        syllabusObjectives: String = "",
        syllabusContent: String = "",
        customInstructions: String = "",
        theme: String = ""
    ) {
        viewModelScope.launch {
            val blockReason = canGenerate()
            if (blockReason != null) {
                _lessonGenerationState.value = AiGenerationState.Error(blockReason)
                return@launch
            }

            _lessonGenerationState.value = AiGenerationState.Generating
            val result = GeminiGenerator.generateLessonNote(
                subject = subject,
                gradeClass = gradeClass,
                topic = topic,
                duration = duration,
                syllabusObjectives = syllabusObjectives,
                syllabusContent = syllabusContent,
                customInstructions = customInstructions,
                theme = theme
            )

            if (result.startsWith("Error:") || result.startsWith("Error -") || result.isEmpty()) {
                _lessonGenerationState.value = AiGenerationState.Error(result)
            } else {
                _lessonGenerationState.value = AiGenerationState.Success(result)
                recordGeneration()
            }
        }
    }

    fun saveGeneratedLessonNote(
        subject: String,
        gradeClass: String,
        topic: String,
        duration: String,
        content: String,
        syllabusItemId: Int? = null
    ) {
        viewModelScope.launch {
            val note = LessonNote(
                title = "Lesson Note: $topic",
                subject = subject,
                gradeClass = gradeClass,
                topic = topic,
                duration = duration,
                behavioralObjectives = "",
                entryBehavior = "",
                instructionalMaterials = "",
                introduction = "",
                presentation = content,
                evaluationQuestions = "",
                conclusion = "",
                assignment = ""
            )
            repository.insertLessonNote(note)

            val matchedSyllabus = syllabusItems.value.find { it.id == syllabusItemId } ?: syllabusItems.value.find {
                it.subject.equals(subject, ignoreCase = true) &&
                it.gradeClass.equals(gradeClass, ignoreCase = true) &&
                it.topic.equals(topic, ignoreCase = true)
            }
            if (matchedSyllabus != null) {
                repository.insertSyllabusItem(matchedSyllabus.copy(isCompleted = true, completionDate = System.currentTimeMillis()))
            }
            delay(200)
            cacheDataForNotifications()
        }
    }

    fun clearLessonState() {
        _lessonGenerationState.value = AiGenerationState.Idle
    }

    fun generateMCQSets(
        subject: String,
        gradeClass: String,
        topic: String,
        difficulty: String,
        count: Int,
        lessonReliancePercent: Int = 0
    ) {
        viewModelScope.launch {
            val blockReason = canGenerate()
            if (blockReason != null) {
                _mcqGenerationState.value = AiGenerationState.Error(blockReason)
                return@launch
            }

            val lessonContext = buildLessonNoteContext(subject, gradeClass)

            _mcqGenerationState.value = AiGenerationState.Generating
            val jsonResult = GeminiGenerator.generateMCQs(subject, gradeClass, topic, difficulty, count, lessonContext, lessonReliancePercent)

            if (jsonResult == "[]" || jsonResult.isEmpty()) {
                _mcqGenerationState.value = AiGenerationState.Error("Failed to generate or parse MCQs from academic servers. Please verify key.")
                return@launch
            }

            try {
                val listType = Types.newParameterizedType(List::class.java, Map::class.java)
                val adapter = moshi.adapter<List<Map<String, String>>>(listType)
                val rawList = adapter.fromJson(jsonResult) ?: emptyList()

                val mcqs = rawList.map { map ->
                    McqModel(
                        question = map["question"] ?: "N/A",
                        optionA = map["optionA"] ?: map["optiona"] ?: "A",
                        optionB = map["optionB"] ?: map["optionb"] ?: "B",
                        optionC = map["optionC"] ?: map["optionc"] ?: "C",
                        optionD = map["optionD"] ?: map["optiond"] ?: "D",
                        correctAnswer = map["correctAnswer"] ?: map["correct_answer"] ?: "A",
                        explanation = map["explanation"] ?: ""
                    )
                }

                val isFallback = mcqs.isNotEmpty() && mcqs.first().question.startsWith("Question 1:")
                if (!isFallback) {
                    recordGeneration()
                }
                _mcqGenerationState.value = AiGenerationState.Success(mcqs)
            } catch (e: Exception) {
                _mcqGenerationState.value = AiGenerationState.Error("Parsing error: ${e.localizedMessage}")
            }
        }
    }

    fun saveMcqSet(subject: String, gradeClass: String, topic: String, difficulty: String, mcqs: List<McqModel>) {
        viewModelScope.launch {
            val type = Types.newParameterizedType(List::class.java, McqModel::class.java)
            val adapter = moshi.adapter<List<McqModel>>(type)
            val json = adapter.toJson(mcqs)

            val mcqSet = MCQSet(
                title = "MCQ: $topic ($difficulty)",
                subject = subject,
                gradeClass = gradeClass,
                topic = topic,
                difficulty = difficulty,
                questionsJson = json
            )
            repository.insertMCQSet(mcqSet)
        }
    }

    fun clearMcqState() {
        _mcqGenerationState.value = AiGenerationState.Idle
    }

    fun generateTheoryQuestions(
        subject: String,
        gradeClass: String,
        topic: String,
        count: Int,
        lessonReliancePercent: Int = 0
    ) {
        viewModelScope.launch {
            val blockReason = canGenerate()
            if (blockReason != null) {
                _theoryGenerationState.value = AiGenerationState.Error(blockReason)
                return@launch
            }

            val lessonContext = buildLessonNoteContext(subject, gradeClass)

            _theoryGenerationState.value = AiGenerationState.Generating
            val jsonResult = GeminiGenerator.generateTheoryQuestions(subject, gradeClass, topic, count, lessonContext, lessonReliancePercent)

            if (jsonResult == "[]" || jsonResult.isEmpty()) {
                _theoryGenerationState.value = AiGenerationState.Error("Failed to generate standard essay sheets. Ensure API Key works.")
                return@launch
            }

            try {
                val listType = Types.newParameterizedType(List::class.java, Map::class.java)
                val adapter = moshi.adapter<List<Map<String, String>>>(listType)
                val rawList = adapter.fromJson(jsonResult) ?: emptyList()

                val theories = rawList.map { map ->
                    TheoryModel(
                        question = map["question"] ?: "N/A",
                        markingScheme = map["suggestedAnswer"] ?: map["suggested_answer"] ?: map["markingScheme"] ?: map["marking_scheme"] ?: "Mark distribution guideline"
                    )
                }
                val isFallback = theories.isNotEmpty() && theories.first().question.startsWith("Theory Problem 1:")
                if (!isFallback) {
                    recordGeneration()
                }
                _theoryGenerationState.value = AiGenerationState.Success(theories)
            } catch (e: Exception) {
                _theoryGenerationState.value = AiGenerationState.Error("Parsing error: ${e.localizedMessage}")
            }
        }
    }

    fun saveTheorySet(subject: String, gradeClass: String, topic: String, theoryQuestions: List<TheoryModel>) {
        viewModelScope.launch {
            val type = Types.newParameterizedType(List::class.java, TheoryModel::class.java)
            val adapter = moshi.adapter<List<TheoryModel>>(type)
            val json = adapter.toJson(theoryQuestions)

            val theorySet = TheorySet(
                title = "Theory Sheet: $topic",
                subject = subject,
                gradeClass = gradeClass,
                topic = topic,
                questionsJson = json
            )
            repository.insertTheorySet(theorySet)
        }
    }

    fun clearTheoryState() {
        _theoryGenerationState.value = AiGenerationState.Idle
    }

    // --- TIMETABLE ITEM MANAGEMENT ---
    fun addTimetableItem(day: String, start: String, end: String, subj: String, cls: String, school: String, color: String) {
        viewModelScope.launch {
            repository.insertTimetableItem(
                TimetableItem(
                    dayOfWeek = day,
                    startTime = start,
                    endTime = end,
                    subject = subj,
                    gradeClass = cls,
                    schoolName = school,
                    colorHex = color
                )
            )
            delay(200)
            cacheDataForNotifications()
        }
    }

    fun deleteTimetableItem(id: Int) {
        viewModelScope.launch {
            repository.deleteTimetableItemById(id)
            delay(200)
            cacheDataForNotifications()
        }
    }

    fun toggleTimetableComplete(item: TimetableItem) {
        viewModelScope.launch {
            repository.insertTimetableItem(item.copy(isCompleted = !item.isCompleted))
            delay(200)
            cacheDataForNotifications()
        }
    }

    // --- SYLLABUS MANAGEMENT ---
    fun toggleSyllabusComplete(item: SyllabusItem) {
        viewModelScope.launch {
            val updated = item.copy(
                isCompleted = !item.isCompleted,
                completionDate = if (!item.isCompleted) System.currentTimeMillis() else null
            )
            repository.insertSyllabusItem(updated)
            delay(200)
            cacheDataForNotifications()
        }
    }

    fun addSyllabusItem(
        schoolName: String,
        gradeClass: String,
        subject: String,
        term: String,
        week: Int,
        theme: String,
        topic: String,
        content: String,
        objectives: String
    ) {
        viewModelScope.launch {
            repository.insertSyllabusItem(
                SyllabusItem(
                    schoolName = schoolName,
                    gradeClass = gradeClass,
                    subject = subject,
                    term = term,
                    week = week,
                    theme = theme,
                    topic = topic,
                    content = content,
                    objectives = objectives
                )
            )
            delay(200)
            cacheDataForNotifications()
        }
    }

    fun updateSyllabusItem(item: SyllabusItem) {
        viewModelScope.launch {
            repository.insertSyllabusItem(item)
            delay(200)
            cacheDataForNotifications()
        }
    }

    fun deleteSyllabusItem(id: Int) {
        viewModelScope.launch {
            repository.deleteSyllabusItemById(id)
            delay(200)
            cacheDataForNotifications()
        }
    }

    // --- CLASSES & REGISTRIES ---
    fun addSchoolClass(name: String, school: String, subject: String) {
        viewModelScope.launch {
            repository.insertSchoolClass(SchoolClass(className = name, schoolName = school, subject = subject))
        }
    }

    fun updateSchoolClass(schoolClass: SchoolClass) {
        viewModelScope.launch {
            repository.insertSchoolClass(schoolClass)
        }
    }

    fun deleteSchoolClass(id: Int) {
        viewModelScope.launch {
            repository.deleteSchoolClassById(id)
        }
    }

    fun getStudentsFlow(classId: Int): Flow<List<Student>> = kotlinx.coroutines.flow.flow {
        emit(repository.getStudentsByClass(classId))
    }

    fun addStudent(classId: Int, fullName: String, notes: String) {
        viewModelScope.launch {
            repository.insertStudent(Student(classId = classId, fullName = fullName, performanceNotes = notes, attendanceCount = 0, totalSessions = 0))
        }
    }

    fun deleteStudent(id: Int) {
        viewModelScope.launch {
            repository.deleteStudentById(id)
        }
    }

    fun updateStudentAttendance(student: Student, present: Boolean) {
        viewModelScope.launch {
            val updated = student.copy(
                attendanceCount = if (present) student.attendanceCount + 1 else student.attendanceCount,
                totalSessions = student.totalSessions + 1
            )
            repository.insertStudent(updated)
        }
    }

    // --- HISTORY REMOVALS ---
    fun deleteLessonNote(id: Int) {
        viewModelScope.launch {
            repository.deleteLessonNoteById(id)
            delay(200)
            cacheDataForNotifications()
        }
    }

    fun deleteMcqSet(id: Int) {
        viewModelScope.launch {
            repository.deleteMCQSetById(id)
        }
    }

    fun deleteTheorySet(id: Int) {
        viewModelScope.launch {
            repository.deleteTheorySetById(id)
        }
    }

    // --- NOTIFICATION METHODS ---
    fun loadNotificationPrefs() {
        viewModelScope.launch {
            val prefs = NotificationPrefs(
                wakeUpAlarmEnabled = repository.getPreference("notif_wake_up_enabled") == "true",
                wakeUpHour = repository.getPreference("notif_wake_up_hour")?.toIntOrNull() ?: 5,
                wakeUpMinute = repository.getPreference("notif_wake_up_minute")?.toIntOrNull() ?: 30,
                scheduleReminderEnabled = repository.getPreference("notif_schedule_reminder_enabled") != "false",
                reminderMinutesBefore = repository.getPreference("notif_reminder_minutes")?.toIntOrNull() ?: 15,
                missedScheduleAlerts = repository.getPreference("notif_missed_alerts") != "false",
                uncompletedNotesReminder = repository.getPreference("notif_uncompleted_notes") != "false",
                dailyScheduleEnabled = repository.getPreference("notif_daily_schedule_enabled") != "false",
                dailyScheduleHour = repository.getPreference("notif_daily_schedule_hour")?.toIntOrNull() ?: 6,
                dailyScheduleMinute = repository.getPreference("notif_daily_schedule_minute")?.toIntOrNull() ?: 0,
                syllabusReminderEnabled = repository.getPreference("notif_syllabus_reminder_enabled") != "false",
                syllabusReminderHour = repository.getPreference("notif_syllabus_reminder_hour")?.toIntOrNull() ?: 7,
                syllabusReminderMinute = repository.getPreference("notif_syllabus_reminder_minute")?.toIntOrNull() ?: 0
            )
            _notificationPrefs.value = prefs
            saveNotifPrefsToLocal(prefs)
        }
    }

    private fun saveNotifPrefsToLocal(prefs: NotificationPrefs) {
        val context = getApplication<Application>()
        val localPrefs = context.getSharedPreferences("notification_prefs", Context.MODE_PRIVATE)
        localPrefs.edit()
            .putBoolean("wake_up_alarm_enabled", prefs.wakeUpAlarmEnabled)
            .putInt("wake_up_hour", prefs.wakeUpHour)
            .putInt("wake_up_minute", prefs.wakeUpMinute)
            .putBoolean("schedule_reminder_enabled", prefs.scheduleReminderEnabled)
            .putInt("reminder_minutes_before", prefs.reminderMinutesBefore)
            .putBoolean("missed_schedule_alerts", prefs.missedScheduleAlerts)
            .putBoolean("uncompleted_notes_reminder", prefs.uncompletedNotesReminder)
            .putBoolean("daily_schedule_enabled", prefs.dailyScheduleEnabled)
            .putInt("daily_schedule_hour", prefs.dailyScheduleHour)
            .putInt("daily_schedule_minute", prefs.dailyScheduleMinute)
            .putBoolean("syllabus_reminder_enabled", prefs.syllabusReminderEnabled)
            .putInt("syllabus_reminder_hour", prefs.syllabusReminderHour)
            .putInt("syllabus_reminder_minute", prefs.syllabusReminderMinute)
            .apply()
    }

    fun saveNotificationPrefs(prefs: NotificationPrefs) {
        viewModelScope.launch {
            repository.setPreference("notif_wake_up_enabled", prefs.wakeUpAlarmEnabled.toString())
            repository.setPreference("notif_wake_up_hour", prefs.wakeUpHour.toString())
            repository.setPreference("notif_wake_up_minute", prefs.wakeUpMinute.toString())
            repository.setPreference("notif_schedule_reminder_enabled", prefs.scheduleReminderEnabled.toString())
            repository.setPreference("notif_reminder_minutes", prefs.reminderMinutesBefore.toString())
            repository.setPreference("notif_missed_alerts", prefs.missedScheduleAlerts.toString())
            repository.setPreference("notif_uncompleted_notes", prefs.uncompletedNotesReminder.toString())
            repository.setPreference("notif_daily_schedule_enabled", prefs.dailyScheduleEnabled.toString())
            repository.setPreference("notif_daily_schedule_hour", prefs.dailyScheduleHour.toString())
            repository.setPreference("notif_daily_schedule_minute", prefs.dailyScheduleMinute.toString())
            repository.setPreference("notif_syllabus_reminder_enabled", prefs.syllabusReminderEnabled.toString())
            repository.setPreference("notif_syllabus_reminder_hour", prefs.syllabusReminderHour.toString())
            repository.setPreference("notif_syllabus_reminder_minute", prefs.syllabusReminderMinute.toString())
            _notificationPrefs.value = prefs
            saveNotifPrefsToLocal(prefs)
            val context = getApplication<Application>()
            AlarmScheduler.scheduleDailyWork(context)
        }
    }

    fun addNotification(notification: AppNotification) {
        val current = _notifications.value.toMutableList()
        current.add(0, notification)
        _notifications.value = current
    }

    fun clearNotifications() {
        _notifications.value = emptyList()
    }

    fun markNotificationRead(id: Int) {
        _notifications.value = _notifications.value.map {
            if (it.id == id) it.copy(isRead = true) else it
        }
    }

    fun getUncompletedNotesCount(): Int {
        val notes = lessonNotes.value
        return syllabusItems.value.count { sItem ->
            !sItem.isCompleted && notes.none { note ->
                note.topic.equals(sItem.topic, ignoreCase = true) &&
                note.gradeClass.equals(sItem.gradeClass, ignoreCase = true) &&
                note.subject.equals(sItem.subject, ignoreCase = true)
            }
        }
    }

    private fun buildLessonNoteContext(subject: String, gradeClass: String): String {
        val filtered = lessonNotes.value.filter {
            it.subject.equals(subject, ignoreCase = true) &&
            it.gradeClass.equals(gradeClass, ignoreCase = true)
        }
        if (filtered.isEmpty()) return ""
        return filtered.joinToString("\n\n---\n\n") { note ->
            "Topic: ${note.topic}\nContent: ${note.presentation}".take(3000)
        }.take(15000)
    }

    fun cacheDataForNotifications() {
        val context = getApplication<Application>()
        val prefs = context.getSharedPreferences("notification_prefs", Context.MODE_PRIVATE)

        val ttItems = timetableItems.value
        val sylItems = syllabusItems.value
        val noteItems = lessonNotes.value

        val ttArray = JSONArray()
        ttItems.forEach { item ->
            ttArray.put(JSONObject().apply {
                put("dayOfWeek", item.dayOfWeek)
                put("startTime", item.startTime)
                put("endTime", item.endTime)
                put("subject", item.subject)
                put("gradeClass", item.gradeClass)
                put("schoolName", item.schoolName)
                put("isCompleted", item.isCompleted)
            })
        }

        val syllabusArray = JSONArray()
        sylItems.forEach { item ->
            syllabusArray.put(JSONObject().apply {
                put("subject", item.subject)
                put("gradeClass", item.gradeClass)
                put("topic", item.topic)
                put("week", item.week)
                put("isCompleted", item.isCompleted)
                put("schoolName", item.schoolName)
            })
        }

        val notesArray = JSONArray()
        noteItems.forEach { note ->
            notesArray.put(JSONObject().apply {
                put("subject", note.subject)
                put("gradeClass", note.gradeClass)
                put("topic", note.topic)
            })
        }

        prefs.edit()
            .putString("timetable_cache", ttArray.toString())
            .putString("syllabus_cache", syllabusArray.toString())
            .putString("lesson_notes_cache", notesArray.toString())
            .apply()

        AlarmScheduler.scheduleClassReminders(context)
    }
}
