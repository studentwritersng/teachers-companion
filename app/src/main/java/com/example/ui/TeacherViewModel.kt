package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.api.GeminiGenerator
import com.example.api.supabase.SupabaseClient
import com.example.data.*
import com.example.notification.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.github.jan.supabase.gotrue.provider.builtin.Email
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

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

    private val repository = SupabaseRepository()
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
    private val _subscriptionPlan = MutableStateFlow("FREE")
    val subscriptionPlan: StateFlow<String> = _subscriptionPlan.asStateFlow()

    private val _usageLessonNotes = MutableStateFlow(0)
    val usageLessonNotes: StateFlow<Int> = _usageLessonNotes.asStateFlow()

    private val _usageMcqs = MutableStateFlow(0)
    val usageMcqs: StateFlow<Int> = _usageMcqs.asStateFlow()

    // --- NOTIFICATION STATE ---
    private val _notificationPrefs = MutableStateFlow(NotificationPrefs())
    val notificationPrefs: StateFlow<NotificationPrefs> = _notificationPrefs.asStateFlow()

    private val _notifications = MutableStateFlow<List<AppNotification>>(emptyList())
    val notifications: StateFlow<List<AppNotification>> = _notifications.asStateFlow()

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
                    val email = session.user.email ?: ""
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
                        }
                    }
                }
            } catch (_: Exception) {
                // No session, stays logged out
            }

            _isDarkMode.value = repository.getPreference("is_dark_mode") == "true"
            _usageLessonNotes.value = repository.getUsageLimit("lesson_notes")
            _usageMcqs.value = repository.getUsageLimit("mcqs")

            // Load notification preferences
            loadNotificationPrefs()

            val loggedInEmail = repository.getPreference("logged_in_email") ?: ""
            if (loggedInEmail.isNotEmpty() && _currentUser.value == null) {
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
                }
            } else {
                _onboardingCompleted.value = false
                _subscriptionPlan.value = "FREE"
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
                SupabaseClient.auth.signUpWith(Email) {
                    this.email = trimmedEmail
                    this.password = password
                }
                // After sign-up, the DB trigger creates user_accounts row.
                // We insert additional profile fields separately.
                val newAcc = UserAccount(
                    email = trimmedEmail,
                    passwordHash = password,
                    isOnboardingCompleted = false,
                    subscriptionPlan = "FREE"
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
                SupabaseClient.auth.signInWith(Email) {
                    this.email = trimmedEmail
                    this.password = password
                }
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

            // Replace existing classes with onboarded ones
            schoolClasses.value.forEach { repository.deleteSchoolClassById(it.id) }
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

    // --- CHANGE SUBSCRIPTION IN BULK ---
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

    fun resetLimits() {
        viewModelScope.launch {
            repository.resetMonthlyLimits()
            _usageLessonNotes.value = 0
            _usageMcqs.value = 0
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
            val isFree = _subscriptionPlan.value == "FREE"
            if (isFree && _usageLessonNotes.value >= 5) {
                _lessonGenerationState.value = AiGenerationState.Error(
                    "Monthly Free Plan limit (5 Lesson Notes) exceeded. Please upgrade to Standard or Premium on the Billing tab to enjoy unlimited lesson preparation."
                )
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

            if (result.startsWith("Error")) {
                _lessonGenerationState.value = AiGenerationState.Error(result)
            } else {
                _lessonGenerationState.value = AiGenerationState.Success(result)
                if (isFree) {
                    repository.incrementUsageLimit("lesson_notes")
                    _usageLessonNotes.value = repository.getUsageLimit("lesson_notes")
                }
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
            val isFree = _subscriptionPlan.value == "FREE"
            if (isFree && _usageMcqs.value >= 10) {
                _mcqGenerationState.value = AiGenerationState.Error(
                    "Monthly Free Plan limit (10 MCQ questions) exceeded. Please upgrade to continue generating standardized exam questions."
                )
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

                _mcqGenerationState.value = AiGenerationState.Success(mcqs)

                if (isFree) {
                    repository.incrementUsageLimit("mcqs")
                    _usageMcqs.value = repository.getUsageLimit("mcqs")
                }
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
            val plan = _subscriptionPlan.value
            if (plan == "FREE") {
                _theoryGenerationState.value = AiGenerationState.Error(
                    "Theory Question Generator is exclusive to Standard and Premium tiers. Please upgrade on the billing tab to unlock standard theory papers!"
                )
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
                        markingScheme = map["markingScheme"] ?: map["marking_scheme"] ?: "Mark distribution guideline"
                    )
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
        }
    }

    fun deleteTimetableItem(id: Int) {
        viewModelScope.launch {
            repository.deleteTimetableItemById(id)
        }
    }

    fun toggleTimetableComplete(item: TimetableItem) {
        viewModelScope.launch {
            repository.insertTimetableItem(item.copy(isCompleted = !item.isCompleted))
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
        }
    }

    fun updateSyllabusItem(item: SyllabusItem) {
        viewModelScope.launch {
            repository.insertSyllabusItem(item)
        }
    }

    fun deleteSyllabusItem(id: Int) {
        viewModelScope.launch {
            repository.deleteSyllabusItemById(id)
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

    fun getStudentsFlow(classId: Int): Flow<List<Student>> {
        return repository.getStudentsByClass(classId)
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
                uncompletedNotesReminder = repository.getPreference("notif_uncompleted_notes") != "false"
            )
            _notificationPrefs.value = prefs
        }
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
            _notificationPrefs.value = prefs
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
        return lessonNotes.value.count { it.presentation.isEmpty() }
    }

    private fun buildLessonNoteContext(subject: String, gradeClass: String): String {
        val filtered = lessonNotes.value.filter {
            it.subject.equals(subject, ignoreCase = true) &&
            it.gradeClass.equals(gradeClass, ignoreCase = true)
        }
        if (filtered.isEmpty()) return ""
        return filtered.joinToString("\n\n---\n\n") { note ->
            "Topic: ${note.topic}\nContent: ${note.content}".take(3000)
        }.take(15000)
    }
}
