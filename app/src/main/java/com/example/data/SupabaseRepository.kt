package com.example.data

import com.example.api.supabase.SupabaseClient
import com.example.api.supabase.*
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.JsonObject
import java.util.concurrent.atomic.AtomicInteger

class SupabaseRepository {

    // ── In-memory ID mapping: Supabase UUID → local sequential Int ──
    private val noteIdMap     = mutableMapOf<String, Int>()
    private val mcqIdMap      = mutableMapOf<String, Int>()
    private val theoryIdMap   = mutableMapOf<String, Int>()
    private val ttIdMap       = mutableMapOf<String, Int>()
    private val syllabusIdMap = mutableMapOf<String, Int>()
    private val classIdMap    = mutableMapOf<String, Int>()
    private val studentIdMap  = mutableMapOf<String, Int>()

    private val nextNoteId     = AtomicInteger(1)
    private val nextMcqId      = AtomicInteger(1)
    private val nextTheoryId   = AtomicInteger(1)
    private val nextTtId       = AtomicInteger(1)
    private val nextSyllabusId = AtomicInteger(1)
    private val nextClassId    = AtomicInteger(1)
    private val nextStudentId  = AtomicInteger(1)

    // ── Reactive state ──
    private val _allLessonNotes     = MutableStateFlow<List<LessonNote>>(emptyList())
    private val _allMCQSets         = MutableStateFlow<List<MCQSet>>(emptyList())
    private val _allTheorySets      = MutableStateFlow<List<TheorySet>>(emptyList())
    private val _allTimetableItems  = MutableStateFlow<List<TimetableItem>>(emptyList())
    private val _allSyllabusItems   = MutableStateFlow<List<SyllabusItem>>(emptyList())
    private val _allSchoolClasses   = MutableStateFlow<List<SchoolClass>>(emptyList())
    private val _syllabusSubjects   = MutableStateFlow<List<String>>(emptyList())
    private val _studentsByClass    = mutableMapOf<Int, MutableStateFlow<List<Student>>>()

    val allLessonNotes: Flow<List<LessonNote>>     = _allLessonNotes.asStateFlow()
    val allMCQSets: Flow<List<MCQSet>>             = _allMCQSets.asStateFlow()
    val allTheorySets: Flow<List<TheorySet>>       = _allTheorySets.asStateFlow()
    val allTimetableItems: Flow<List<TimetableItem>> = _allTimetableItems.asStateFlow()
    val allSyllabusItems: Flow<List<SyllabusItem>>   = _allSyllabusItems.asStateFlow()
    val allSchoolClasses: Flow<List<SchoolClass>>    = _allSchoolClasses.asStateFlow()
    val syllabusSubjects: Flow<List<String>>         = _syllabusSubjects.asStateFlow()

    // ── Helper ──
    private suspend fun currentUserId(): String? {
        return try {
            SupabaseClient.auth.currentSessionOrNull()?.user?.id
        } catch (_: Exception) { null }
    }

    // ══════════════════════════════════════════════════════════════════
    //  REFRESH (load all data from Supabase into local state)
    // ══════════════════════════════════════════════════════════════════

    suspend fun refreshAll() {
        refreshLessonNotes()
        refreshMCQSets()
        refreshTheorySets()
        refreshTimetableItems()
        refreshSyllabusItems()
        refreshSchoolClasses()
    }

    private suspend fun refreshLessonNotes() {
        val uid = currentUserId() ?: return
        try {
            val dtos = SupabaseClient.postgrest["lesson_notes"]
                .select { order("created_at" to Order.DESCENDING) }
                .decodeList<LessonNoteDto>()
            _allLessonNotes.value = dtos.filter { it.userId == uid }.map { it.toEntity() }
        } catch (_: Exception) { _allLessonNotes.value = emptyList() }
    }

    private suspend fun refreshMCQSets() {
        val uid = currentUserId() ?: return
        try {
            val dtos = SupabaseClient.postgrest["mcq_sets"]
                .select { order("created_at" to Order.DESCENDING) }
                .decodeList<McqSetDto>()
            _allMCQSets.value = dtos.filter { it.userId == uid }.map { it.toEntity() }
        } catch (_: Exception) { _allMCQSets.value = emptyList() }
    }

    private suspend fun refreshTheorySets() {
        val uid = currentUserId() ?: return
        try {
            val dtos = SupabaseClient.postgrest["theory_sets"]
                .select { order("created_at" to Order.DESCENDING) }
                .decodeList<TheorySetDto>()
            _allTheorySets.value = dtos.filter { it.userId == uid }.map { it.toEntity() }
        } catch (_: Exception) { _allTheorySets.value = emptyList() }
    }

    private suspend fun refreshTimetableItems() {
        val uid = currentUserId() ?: return
        try {
            val dtos = SupabaseClient.postgrest["timetable_items"]
                .select { order("start_time" to Order.ASCENDING) }
                .decodeList<TimetableItemDto>()
            _allTimetableItems.value = dtos.filter { it.userId == uid }.map { it.toEntity() }
        } catch (_: Exception) { _allTimetableItems.value = emptyList() }
    }

    private suspend fun refreshSyllabusItems() {
        val uid = currentUserId() ?: return
        try {
            val dtos = SupabaseClient.postgrest["syllabus_items"]
                .select { order("id" to Order.ASCENDING) }
                .decodeList<SyllabusItemDto>()
            _allSyllabusItems.value = dtos.filter { it.userId == uid }.map { it.toEntity() }
            _syllabusSubjects.value = _allSyllabusItems.value.map { it.subject }.distinct()
        } catch (_: Exception) { _allSyllabusItems.value = emptyList() }
    }

    private suspend fun refreshSchoolClasses() {
        val uid = currentUserId() ?: return
        try {
            val dtos = SupabaseClient.postgrest["school_classes"]
                .select { order("class_name" to Order.ASCENDING) }
                .decodeList<SchoolClassDto>()
            _allSchoolClasses.value = dtos.filter { it.userId == uid }.map { it.toEntity() }
        } catch (_: Exception) { _allSchoolClasses.value = emptyList() }
    }

    // ══════════════════════════════════════════════════════════════════
    //  LESSON NOTES
    // ══════════════════════════════════════════════════════════════════

    suspend fun insertLessonNote(note: LessonNote): String {
        val uid = currentUserId() ?: return ""
        val existingUuid = noteIdMap.entries.firstOrNull { it.value == note.id }?.key
        val dto = note.toDto(uid)
        return if (existingUuid != null) {
            SupabaseClient.postgrest["lesson_notes"].update(dto) { eq("id", existingUuid) }
            refreshLessonNotes()
            existingUuid
        } else {
            val inserted = SupabaseClient.postgrest["lesson_notes"].insert(dto) { select() }
                .decodeSingleOrNull<LessonNoteDto>() ?: dto
            noteIdMap[inserted.id] = note.id
            refreshLessonNotes()
            inserted.id
        }
    }

    suspend fun deleteLessonNoteById(id: Int) {
        val uuid = noteIdMap.entries.firstOrNull { it.value == id }?.key ?: return
        SupabaseClient.postgrest["lesson_notes"].delete { eq("id", uuid) }
        noteIdMap.remove(uuid)
        refreshLessonNotes()
    }

    // ══════════════════════════════════════════════════════════════════
    //  MCQ SETS
    // ══════════════════════════════════════════════════════════════════

    suspend fun insertMCQSet(mcqSet: MCQSet): String {
        val uid = currentUserId() ?: return ""
        val existingUuid = mcqIdMap.entries.firstOrNull { it.value == mcqSet.id }?.key
        val dto = mcqSet.toDto(uid)
        return if (existingUuid != null) {
            SupabaseClient.postgrest["mcq_sets"].update(dto) { eq("id", existingUuid) }
            refreshMCQSets()
            existingUuid
        } else {
            val inserted = SupabaseClient.postgrest["mcq_sets"].insert(dto) { select() }
                .decodeSingleOrNull<McqSetDto>() ?: dto
            mcqIdMap[inserted.id] = mcqSet.id
            refreshMCQSets()
            inserted.id
        }
    }

    suspend fun deleteMCQSetById(id: Int) {
        val uuid = mcqIdMap.entries.firstOrNull { it.value == id }?.key ?: return
        SupabaseClient.postgrest["mcq_sets"].delete { eq("id", uuid) }
        mcqIdMap.remove(uuid)
        refreshMCQSets()
    }

    // ══════════════════════════════════════════════════════════════════
    //  THEORY SETS
    // ══════════════════════════════════════════════════════════════════

    suspend fun insertTheorySet(theorySet: TheorySet): String {
        val uid = currentUserId() ?: return ""
        val existingUuid = theoryIdMap.entries.firstOrNull { it.value == theorySet.id }?.key
        val dto = theorySet.toDto(uid)
        return if (existingUuid != null) {
            SupabaseClient.postgrest["theory_sets"].update(dto) { eq("id", existingUuid) }
            refreshTheorySets()
            existingUuid
        } else {
            val inserted = SupabaseClient.postgrest["theory_sets"].insert(dto) { select() }
                .decodeSingleOrNull<TheorySetDto>() ?: dto
            theoryIdMap[inserted.id] = theorySet.id
            refreshTheorySets()
            inserted.id
        }
    }

    suspend fun deleteTheorySetById(id: Int) {
        val uuid = theoryIdMap.entries.firstOrNull { it.value == id }?.key ?: return
        SupabaseClient.postgrest["theory_sets"].delete { eq("id", uuid) }
        theoryIdMap.remove(uuid)
        refreshTheorySets()
    }

    // ══════════════════════════════════════════════════════════════════
    //  TIMETABLE
    // ══════════════════════════════════════════════════════════════════

    suspend fun insertTimetableItem(item: TimetableItem): String {
        val uid = currentUserId() ?: return ""
        val existingUuid = ttIdMap.entries.firstOrNull { it.value == item.id }?.key
        val dto = item.toDto(uid)
        return if (existingUuid != null) {
            SupabaseClient.postgrest["timetable_items"].update(dto) { eq("id", existingUuid) }
            refreshTimetableItems()
            existingUuid
        } else {
            val inserted = SupabaseClient.postgrest["timetable_items"].insert(dto) { select() }
                .decodeSingleOrNull<TimetableItemDto>() ?: dto
            ttIdMap[inserted.id] = item.id
            refreshTimetableItems()
            inserted.id
        }
    }

    suspend fun deleteTimetableItemById(id: Int) {
        val uuid = ttIdMap.entries.firstOrNull { it.value == id }?.key ?: return
        SupabaseClient.postgrest["timetable_items"].delete { eq("id", uuid) }
        ttIdMap.remove(uuid)
        refreshTimetableItems()
    }

    // ══════════════════════════════════════════════════════════════════
    //  SYLLABUS
    // ══════════════════════════════════════════════════════════════════

    suspend fun insertSyllabusItem(item: SyllabusItem): String {
        val uid = currentUserId() ?: return ""
        val existingUuid = syllabusIdMap.entries.firstOrNull { it.value == item.id }?.key
        val dto = item.toDto(uid)
        return if (existingUuid != null) {
            SupabaseClient.postgrest["syllabus_items"].update(dto) { eq("id", existingUuid) }
            refreshSyllabusItems()
            existingUuid
        } else {
            val inserted = SupabaseClient.postgrest["syllabus_items"].insert(dto) { select() }
                .decodeSingleOrNull<SyllabusItemDto>() ?: dto
            syllabusIdMap[inserted.id] = item.id
            refreshSyllabusItems()
            inserted.id
        }
    }

    suspend fun deleteSyllabusItemById(id: Int) {
        val uuid = syllabusIdMap.entries.firstOrNull { it.value == id }?.key ?: return
        SupabaseClient.postgrest["syllabus_items"].delete { eq("id", uuid) }
        syllabusIdMap.remove(uuid)
        refreshSyllabusItems()
    }

    // ══════════════════════════════════════════════════════════════════
    //  SCHOOL CLASSES
    // ══════════════════════════════════════════════════════════════════

    suspend fun insertSchoolClass(schoolClass: SchoolClass): String {
        val uid = currentUserId() ?: return ""
        val existingUuid = classIdMap.entries.firstOrNull { it.value == schoolClass.id }?.key
        val dto = schoolClass.toDto(uid)
        return if (existingUuid != null) {
            SupabaseClient.postgrest["school_classes"].update(dto) { eq("id", existingUuid) }
            refreshSchoolClasses()
            existingUuid
        } else {
            val inserted = SupabaseClient.postgrest["school_classes"].insert(dto) { select() }
                .decodeSingleOrNull<SchoolClassDto>() ?: dto
            classIdMap[inserted.id] = schoolClass.id
            refreshSchoolClasses()
            inserted.id
        }
    }

    suspend fun deleteSchoolClassById(id: Int) {
        val uuid = classIdMap.entries.firstOrNull { it.value == id }?.key ?: return
        SupabaseClient.postgrest["school_classes"].delete { eq("id", uuid) }
        classIdMap.remove(uuid)
        refreshSchoolClasses()
    }

    // ══════════════════════════════════════════════════════════════════
    //  STUDENTS
    // ══════════════════════════════════════════════════════════════════

    fun getStudentsByClass(classId: Int): Flow<List<Student>> {
        return _studentsByClass.getOrPut(classId) { MutableStateFlow(emptyList()) }.asStateFlow()
    }

    private suspend fun refreshStudentsForClass(localClassId: Int) {
        val classUuid = classIdMap.entries.firstOrNull { it.value == localClassId }?.key ?: return
        try {
            val dtos = SupabaseClient.postgrest["students"]
                .select { eq("class_id", classUuid) }
                .decodeList<StudentDto>()
            val students = dtos.map { it.toEntity() }
            _studentsByClass.getOrPut(localClassId) { MutableStateFlow(emptyList()) }.value = students
        } catch (_: Exception) {
            _studentsByClass.getOrPut(localClassId) { MutableStateFlow(emptyList()) }.value = emptyList()
        }
    }

    suspend fun insertStudent(student: Student): String {
        val uid = currentUserId() ?: return ""
        val classUuid = classIdMap.entries.firstOrNull { it.value == student.classId }?.key ?: return ""
        val existingUuid = studentIdMap.entries.firstOrNull { it.value == student.id }?.key
        val dto = student.toDto(uid, classUuid)
        return if (existingUuid != null) {
            SupabaseClient.postgrest["students"].update(dto) { eq("id", existingUuid) }
            refreshStudentsForClass(student.classId)
            existingUuid
        } else {
            val inserted = SupabaseClient.postgrest["students"].insert(dto) { select() }
                .decodeSingleOrNull<StudentDto>() ?: dto
            studentIdMap[inserted.id] = student.id
            refreshStudentsForClass(student.classId)
            inserted.id
        }
    }

    suspend fun deleteStudentById(id: Int) {
        val classId = _studentsByClass.entries.firstOrNull { _, flow ->
            flow.value.any { it.id == id }
        }?.key
        val uuid = studentIdMap.entries.firstOrNull { it.value == id }?.key ?: return
        SupabaseClient.postgrest["students"].delete { eq("id", uuid) }
        studentIdMap.remove(uuid)
        if (classId != null) refreshStudentsForClass(classId)
    }

    // ══════════════════════════════════════════════════════════════════
    //  USER ACCOUNTS
    // ══════════════════════════════════════════════════════════════════

    suspend fun getUserAccount(email: String): UserAccount? {
        return try {
            SupabaseClient.postgrest["user_accounts"]
                .select { eq("email", email.trim().lowercase()) }
                .decodeSingleOrNull<UserAccountDto>()
                ?.toUserAccountEntity()
        } catch (_: Exception) { null }
    }

    suspend fun insertUserAccount(user: UserAccount) {
        val existing = getUserAccount(user.email)
        if (existing != null) {
            SupabaseClient.postgrest["user_accounts"].update(
                UserAccountDto(
                    email = user.email.trim().lowercase(),
                    passwordHash = user.passwordHash,
                    fullName = user.fullName,
                    gender = user.gender,
                    dob = user.dob,
                    address = user.address,
                    phone = user.phone,
                    teachingStatus = user.teachingStatus,
                    isOnboardingCompleted = user.isOnboardingCompleted,
                    subscriptionPlan = user.subscriptionPlan
                )
            ) { eq("email", user.email.trim().lowercase()) }
        } else {
            SupabaseClient.postgrest["user_accounts"].insert(
                UserAccountDto(
                    email = user.email.trim().lowercase(),
                    passwordHash = user.passwordHash,
                    fullName = user.fullName,
                    gender = user.gender,
                    dob = user.dob,
                    address = user.address,
                    phone = user.phone,
                    teachingStatus = user.teachingStatus,
                    isOnboardingCompleted = user.isOnboardingCompleted,
                    subscriptionPlan = user.subscriptionPlan
                )
            )
        }
    }

    // ══════════════════════════════════════════════════════════════════
    //  PREFERENCES
    // ══════════════════════════════════════════════════════════════════

    suspend fun getPreference(key: String): String? {
        val uid = currentUserId() ?: return null
        return try {
            SupabaseClient.postgrest["user_preferences"]
                .select { eq("user_id", uid) }
                .decodeList<UserPreferenceDto>()
                .firstOrNull { it.key == key }?.value
        } catch (_: Exception) { null }
    }

    suspend fun setPreference(key: String, value: String) {
        val uid = currentUserId() ?: return
        val existing = try {
            SupabaseClient.postgrest["user_preferences"]
                .select { eq("user_id", uid) }
                .decodeList<UserPreferenceDto>()
                .firstOrNull { it.key == key }
        } catch (_: Exception) { null }

        if (existing != null) {
            SupabaseClient.postgrest["user_preferences"].update(
                UserPreferenceDto(userId = uid, key = key, value = value)
            ) { eq("id", existing.id) }
        } else {
            SupabaseClient.postgrest["user_preferences"].insert(
                UserPreferenceDto(userId = uid, key = key, value = value)
            )
        }
    }

    suspend fun getSubscriptionPlan(): String {
        return getPreference("subscription_plan") ?: "PREMIUM"
    }

    suspend fun setSubscriptionPlan(plan: String) {
        setPreference("subscription_plan", plan)
    }

    suspend fun getUsageLimit(key: String): Int {
        return getPreference("limit_$key")?.toIntOrNull() ?: 0
    }

    suspend fun incrementUsageLimit(key: String) {
        val current = getUsageLimit(key)
        setPreference("limit_$key", (current + 1).toString())
    }

    suspend fun resetMonthlyLimits() {
        setPreference("limit_lesson_notes", "0")
        setPreference("limit_mcqs", "0")
    }

    suspend fun checkAndPrepopulateSyllabus() { /* no-op */ }

    // ══════════════════════════════════════════════════════════════════
    //  DTO → ENTITY mapping helpers
    // ══════════════════════════════════════════════════════════════════

    private fun LessonNoteDto.toEntity(): LessonNote {
        val localId = noteIdMap.getOrPut(id) { nextNoteId.getAndIncrement() }
        return LessonNote(
            id = localId,
            title = title,
            subject = subject,
            gradeClass = gradeClass,
            topic = topic,
            duration = duration,
            behavioralObjectives = behavioralObjectives,
            entryBehavior = entryBehavior,
            instructionalMaterials = instructionalMaterials,
            introduction = introduction,
            presentation = presentation,
            evaluationQuestions = evaluationQuestions,
            conclusion = conclusion,
            assignment = assignment,
            createdAt = parseTimestamp(createdAt)
        )
    }

    private fun McqSetDto.toEntity(): MCQSet {
        val localId = mcqIdMap.getOrPut(id) { nextMcqId.getAndIncrement() }
        return MCQSet(
            id = localId,
            title = title,
            subject = subject,
            gradeClass = gradeClass,
            topic = topic,
            difficulty = difficulty,
            questionsJson = questionsJson,
            createdAt = parseTimestamp(createdAt)
        )
    }

    private fun TheorySetDto.toEntity(): TheorySet {
        val localId = theoryIdMap.getOrPut(id) { nextTheoryId.getAndIncrement() }
        return TheorySet(
            id = localId,
            title = title,
            subject = subject,
            gradeClass = gradeClass,
            topic = topic,
            questionsJson = questionsJson,
            createdAt = parseTimestamp(createdAt)
        )
    }

    private fun TimetableItemDto.toEntity(): TimetableItem {
        val localId = ttIdMap.getOrPut(id) { nextTtId.getAndIncrement() }
        return TimetableItem(
            id = localId,
            dayOfWeek = dayOfWeek,
            startTime = startTime,
            endTime = endTime,
            subject = subject,
            gradeClass = gradeClass,
            schoolName = schoolName,
            colorHex = colorHex,
            isCompleted = isCompleted
        )
    }

    private fun SyllabusItemDto.toEntity(): SyllabusItem {
        val localId = syllabusIdMap.getOrPut(id) { nextSyllabusId.getAndIncrement() }
        return SyllabusItem(
            id = localId,
            schoolName = schoolName,
            gradeClass = gradeClass,
            subject = subject,
            term = term,
            week = week,
            theme = theme,
            topic = topic,
            content = content,
            objectives = objectives,
            isCompleted = isCompleted,
            completionDate = completionDate?.let { parseTimestamp(it) }
        )
    }

    private fun SchoolClassDto.toEntity(): SchoolClass {
        val localId = classIdMap.getOrPut(id) { nextClassId.getAndIncrement() }
        return SchoolClass(
            id = localId,
            className = className,
            schoolName = schoolName,
            subject = subject
        )
    }

    private fun StudentDto.toEntity(): Student {
        val localId = studentIdMap.getOrPut(id) { nextStudentId.getAndIncrement() }
        val localClassId = classIdMap.entries.firstOrNull { it.key == classId }?.value ?: 0
        return Student(
            id = localId,
            classId = localClassId,
            fullName = fullName,
            performanceNotes = performanceNotes,
            attendanceCount = attendanceCount,
            totalSessions = totalSessions
        )
    }

    private fun UserAccountDto.toUserAccountEntity(): UserAccount {
        return UserAccount(
            email = email,
            passwordHash = passwordHash,
            fullName = fullName,
            gender = gender,
            dob = dob,
            address = address,
            phone = phone,
            teachingStatus = teachingStatus,
            isOnboardingCompleted = isOnboardingCompleted,
            subscriptionPlan = subscriptionPlan
        )
    }

    // ══════════════════════════════════════════════════════════════════
    //  ENTITY → DTO mapping helpers
    // ══════════════════════════════════════════════════════════════════

    private fun LessonNote.toDto(uid: String) = LessonNoteDto(
        userId = uid,
        title = title,
        subject = subject,
        gradeClass = gradeClass,
        topic = topic,
        duration = duration,
        behavioralObjectives = behavioralObjectives,
        entryBehavior = entryBehavior,
        instructionalMaterials = instructionalMaterials,
        introduction = introduction,
        presentation = presentation,
        evaluationQuestions = evaluationQuestions,
        conclusion = conclusion,
        assignment = assignment
    )

    private fun MCQSet.toDto(uid: String) = McqSetDto(
        userId = uid,
        title = title,
        subject = subject,
        gradeClass = gradeClass,
        topic = topic,
        difficulty = difficulty,
        questionsJson = questionsJson
    )

    private fun TheorySet.toDto(uid: String) = TheorySetDto(
        userId = uid,
        title = title,
        subject = subject,
        gradeClass = gradeClass,
        topic = topic,
        questionsJson = questionsJson
    )

    private fun TimetableItem.toDto(uid: String) = TimetableItemDto(
        userId = uid,
        dayOfWeek = dayOfWeek,
        startTime = startTime,
        endTime = endTime,
        subject = subject,
        gradeClass = gradeClass,
        schoolName = schoolName,
        colorHex = colorHex,
        isCompleted = isCompleted
    )

    private fun SyllabusItem.toDto(uid: String) = SyllabusItemDto(
        userId = uid,
        schoolName = schoolName,
        gradeClass = gradeClass,
        subject = subject,
        term = term,
        week = week,
        theme = theme,
        topic = topic,
        content = content,
        objectives = objectives,
        isCompleted = isCompleted,
        completionDate = null
    )

    private fun SchoolClass.toDto(uid: String) = SchoolClassDto(
        userId = uid,
        className = className,
        schoolName = schoolName,
        subject = subject
    )

    private fun Student.toDto(uid: String, classUuid: String) = StudentDto(
        userId = uid,
        classId = classUuid,
        fullName = fullName,
        performanceNotes = performanceNotes,
        attendanceCount = attendanceCount,
        totalSessions = totalSessions
    )

    // ══════════════════════════════════════════════════════════════════
    //  UTILITY
    // ══════════════════════════════════════════════════════════════════

    private fun parseTimestamp(iso: String): Long {
        return try {
            java.time.Instant.parse(iso).toEpochMilli()
        } catch (_: Exception) { System.currentTimeMillis() }
    }
}
