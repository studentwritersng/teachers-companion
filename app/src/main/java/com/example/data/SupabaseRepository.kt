package com.example.data

import com.example.api.supabase.SupabaseClient
import com.example.api.supabase.*
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import java.util.concurrent.atomic.AtomicInteger

class SupabaseRepository(private val dao: TeacherDao) {

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

    // ── Sync status ──
    private val _syncStatus = MutableStateFlow("idle")
    val syncStatus: Flow<String> = _syncStatus.asStateFlow()

    // ── Helper ──
    private suspend fun currentUserId(): String? {
        return try {
            SupabaseClient.auth.currentSessionOrNull()?.user?.id
        } catch (_: Exception) { null }
    }

    // ══════════════════════════════════════════════════════════════════
    //  REFRESH (load all data from Supabase, fall back to Room)
    // ══════════════════════════════════════════════════════════════════

    suspend fun refreshAll() {
        _syncStatus.value = "syncing"
        refreshLessonNotes()
        refreshMCQSets()
        refreshTheorySets()
        refreshTimetableItems()
        refreshSyllabusItems()
        refreshSchoolClasses()
        _syncStatus.value = "done"
    }

    private suspend fun refreshLessonNotes() {
        val uid = currentUserId()
        if (uid != null) {
            try {
                val dtos = SupabaseClient.postgrest["lesson_notes"]
                    .select { order("created_at", Order.DESCENDING) }
                    .decodeList<LessonNoteDto>()
                val entities = dtos.filter { it.userId == uid }.map { it.toEntity() }
                _allLessonNotes.value = entities
                cacheLessonNotesToRoom(entities)
                return
            } catch (_: Exception) { }
        }
        loadLessonNotesFromRoom()
    }

    private suspend fun refreshMCQSets() {
        val uid = currentUserId()
        if (uid != null) {
            try {
                val dtos = SupabaseClient.postgrest["mcq_sets"]
                    .select { order("created_at", Order.DESCENDING) }
                    .decodeList<McqSetDto>()
                val entities = dtos.filter { it.userId == uid }.map { it.toEntity() }
                _allMCQSets.value = entities
                cacheMCQSetsToRoom(entities)
                return
            } catch (_: Exception) { }
        }
        loadMCQSetsFromRoom()
    }

    private suspend fun refreshTheorySets() {
        val uid = currentUserId()
        if (uid != null) {
            try {
                val dtos = SupabaseClient.postgrest["theory_sets"]
                    .select { order("created_at", Order.DESCENDING) }
                    .decodeList<TheorySetDto>()
                val entities = dtos.filter { it.userId == uid }.map { it.toEntity() }
                _allTheorySets.value = entities
                cacheTheorySetsToRoom(entities)
                return
            } catch (_: Exception) { }
        }
        loadTheorySetsFromRoom()
    }

    private suspend fun refreshTimetableItems() {
        val uid = currentUserId()
        if (uid != null) {
            try {
                val dtos = SupabaseClient.postgrest["timetable_items"]
                    .select { order("start_time", Order.ASCENDING) }
                    .decodeList<TimetableItemDto>()
                val entities = dtos.filter { it.userId == uid }.map { it.toEntity() }
                _allTimetableItems.value = entities
                cacheTimetableToRoom(entities)
                return
            } catch (_: Exception) { }
        }
        loadTimetableFromRoom()
    }

    private suspend fun refreshSyllabusItems() {
        val uid = currentUserId()
        if (uid != null) {
            try {
                val dtos = SupabaseClient.postgrest["syllabus_items"]
                    .select { order("id", Order.ASCENDING) }
                    .decodeList<SyllabusItemDto>()
                val entities = dtos.filter { it.userId == uid }.map { it.toEntity() }
                _allSyllabusItems.value = entities
                _syllabusSubjects.value = entities.map { it.subject }.distinct()
                cacheSyllabusToRoom(entities)
                return
            } catch (_: Exception) { }
        }
        loadSyllabusFromRoom()
    }

    private suspend fun refreshSchoolClasses() {
        val uid = currentUserId()
        if (uid != null) {
            try {
                val dtos = SupabaseClient.postgrest["school_classes"]
                    .select { order("class_name", Order.ASCENDING) }
                    .decodeList<SchoolClassDto>()
                val entities = dtos.filter { it.userId == uid }.map { it.toEntity() }
                _allSchoolClasses.value = entities
                cacheClassesToRoom(entities)
                return
            } catch (_: Exception) { }
        }
        loadClassesFromRoom()
    }

    // ══════════════════════════════════════════════════════════════════
    //  ROOM CACHE: Write (after successful Supabase fetch)
    // ══════════════════════════════════════════════════════════════════

    private suspend fun cacheLessonNotesToRoom(entities: List<LessonNote>) {
        try { dao.deleteAllLessonNotes(); entities.forEach { dao.insertLessonNote(it) } } catch (_: Exception) {}
    }
    private suspend fun cacheMCQSetsToRoom(entities: List<MCQSet>) {
        try { dao.deleteAllMCQSets(); entities.forEach { dao.insertMCQSet(it) } } catch (_: Exception) {}
    }
    private suspend fun cacheTheorySetsToRoom(entities: List<TheorySet>) {
        try { dao.deleteAllTheorySets(); entities.forEach { dao.insertTheorySet(it) } } catch (_: Exception) {}
    }
    private suspend fun cacheTimetableToRoom(entities: List<TimetableItem>) {
        try { dao.deleteAllTimetableItems(); entities.forEach { dao.insertTimetableItem(it) } } catch (_: Exception) {}
    }
    private suspend fun cacheSyllabusToRoom(entities: List<SyllabusItem>) {
        try { dao.deleteAllSyllabusItems(); entities.forEach { dao.insertSyllabusItem(it) } } catch (_: Exception) {}
    }
    private suspend fun cacheClassesToRoom(entities: List<SchoolClass>) {
        try { dao.deleteAllSchoolClasses(); entities.forEach { dao.insertSchoolClass(it) } } catch (_: Exception) {}
    }

    // ══════════════════════════════════════════════════════════════════
    //  ROOM CACHE: Read (fallback when Supabase fails)
    // ══════════════════════════════════════════════════════════════════

    private suspend fun loadLessonNotesFromRoom() {
        try {
            val notes = dao.getAllLessonNotes().firstOrEmpty()
            if (notes.isNotEmpty()) _allLessonNotes.value = notes
        } catch (_: Exception) {}
    }
    private suspend fun loadMCQSetsFromRoom() {
        try {
            val sets = dao.getAllMCQSets().firstOrEmpty()
            if (sets.isNotEmpty()) _allMCQSets.value = sets
        } catch (_: Exception) {}
    }
    private suspend fun loadTheorySetsFromRoom() {
        try {
            val sets = dao.getAllTheorySets().firstOrEmpty()
            if (sets.isNotEmpty()) _allTheorySets.value = sets
        } catch (_: Exception) {}
    }
    private suspend fun loadTimetableFromRoom() {
        try {
            val items = dao.getAllTimetableItems().firstOrEmpty()
            if (items.isNotEmpty()) _allTimetableItems.value = items
        } catch (_: Exception) {}
    }
    private suspend fun loadSyllabusFromRoom() {
        try {
            val items = dao.getAllSyllabusItems().firstOrEmpty()
            if (items.isNotEmpty()) {
                _allSyllabusItems.value = items
                _syllabusSubjects.value = items.map { it.subject }.distinct()
            }
        } catch (_: Exception) {}
    }
    private suspend fun loadClassesFromRoom() {
        try {
            val classes = dao.getAllSchoolClasses().firstOrEmpty()
            if (classes.isNotEmpty()) _allSchoolClasses.value = classes
        } catch (_: Exception) {}
    }

    // ══════════════════════════════════════════════════════════════════
    //  LESSON NOTES
    // ══════════════════════════════════════════════════════════════════

    suspend fun insertLessonNote(note: LessonNote): String {
        val uid = currentUserId() ?: return ""
        val localId = if (note.id == 0) -nextNoteId.getAndIncrement() else note.id
        val noteWithId = note.copy(id = localId)
        val existingUuid = noteIdMap.entries.firstOrNull { it.value == localId }?.key
        val dto = noteWithId.toDto(uid)
        return if (existingUuid != null) {
            SupabaseClient.postgrest["lesson_notes"].update(dto) { filter { eq("id", existingUuid) } }
            refreshLessonNotes()
            existingUuid
        } else {
            val inserted = SupabaseClient.postgrest["lesson_notes"].insert(dto) { select() }
                .decodeSingleOrNull<LessonNoteDto>() ?: dto
            noteIdMap[inserted.id] = localId
            refreshLessonNotes()
            inserted.id
        }
    }

    suspend fun deleteLessonNoteById(id: Int) {
        val uuid = noteIdMap.entries.firstOrNull { it.value == id }?.key ?: return
        SupabaseClient.postgrest["lesson_notes"].delete { filter { eq("id", uuid) } }
        noteIdMap.remove(uuid)
        refreshLessonNotes()
    }

    // ══════════════════════════════════════════════════════════════════
    //  MCQ SETS
    // ══════════════════════════════════════════════════════════════════

    suspend fun insertMCQSet(mcqSet: MCQSet): String {
        val uid = currentUserId() ?: return ""
        val localId = if (mcqSet.id == 0) -nextMcqId.getAndIncrement() else mcqSet.id
        val setWithId = mcqSet.copy(id = localId)
        val existingUuid = mcqIdMap.entries.firstOrNull { it.value == localId }?.key
        val dto = setWithId.toDto(uid)
        return if (existingUuid != null) {
            SupabaseClient.postgrest["mcq_sets"].update(dto) { filter { eq("id", existingUuid) } }
            refreshMCQSets()
            existingUuid
        } else {
            val inserted = SupabaseClient.postgrest["mcq_sets"].insert(dto) { select() }
                .decodeSingleOrNull<McqSetDto>() ?: dto
            mcqIdMap[inserted.id] = localId
            refreshMCQSets()
            inserted.id
        }
    }

    suspend fun deleteMCQSetById(id: Int) {
        val uuid = mcqIdMap.entries.firstOrNull { it.value == id }?.key ?: return
        SupabaseClient.postgrest["mcq_sets"].delete { filter { eq("id", uuid) } }
        mcqIdMap.remove(uuid)
        refreshMCQSets()
    }

    // ══════════════════════════════════════════════════════════════════
    //  THEORY SETS
    // ══════════════════════════════════════════════════════════════════

    suspend fun insertTheorySet(theorySet: TheorySet): String {
        val uid = currentUserId() ?: return ""
        val localId = if (theorySet.id == 0) -nextTheoryId.getAndIncrement() else theorySet.id
        val setWithId = theorySet.copy(id = localId)
        val existingUuid = theoryIdMap.entries.firstOrNull { it.value == localId }?.key
        val dto = setWithId.toDto(uid)
        return if (existingUuid != null) {
            SupabaseClient.postgrest["theory_sets"].update(dto) { filter { eq("id", existingUuid) } }
            refreshTheorySets()
            existingUuid
        } else {
            val inserted = SupabaseClient.postgrest["theory_sets"].insert(dto) { select() }
                .decodeSingleOrNull<TheorySetDto>() ?: dto
            theoryIdMap[inserted.id] = localId
            refreshTheorySets()
            inserted.id
        }
    }

    suspend fun deleteTheorySetById(id: Int) {
        val uuid = theoryIdMap.entries.firstOrNull { it.value == id }?.key ?: return
        SupabaseClient.postgrest["theory_sets"].delete { filter { eq("id", uuid) } }
        theoryIdMap.remove(uuid)
        refreshTheorySets()
    }

    // ══════════════════════════════════════════════════════════════════
    //  TIMETABLE
    // ══════════════════════════════════════════════════════════════════

    suspend fun insertTimetableItem(item: TimetableItem): String {
        val uid = currentUserId() ?: return ""
        val localId = if (item.id == 0) -nextTtId.getAndIncrement() else item.id
        val itemWithId = item.copy(id = localId)
        val existingUuid = ttIdMap.entries.firstOrNull { it.value == localId }?.key
        val dto = itemWithId.toDto(uid)
        return if (existingUuid != null) {
            SupabaseClient.postgrest["timetable_items"].update(dto) { filter { eq("id", existingUuid) } }
            refreshTimetableItems()
            existingUuid
        } else {
            val inserted = SupabaseClient.postgrest["timetable_items"].insert(dto) { select() }
                .decodeSingleOrNull<TimetableItemDto>() ?: dto
            ttIdMap[inserted.id] = localId
            refreshTimetableItems()
            inserted.id
        }
    }

    suspend fun deleteTimetableItemById(id: Int) {
        val uuid = ttIdMap.entries.firstOrNull { it.value == id }?.key ?: return
        SupabaseClient.postgrest["timetable_items"].delete { filter { eq("id", uuid) } }
        ttIdMap.remove(uuid)
        refreshTimetableItems()
    }

    // ══════════════════════════════════════════════════════════════════
    //  SYLLABUS
    // ══════════════════════════════════════════════════════════════════

    suspend fun insertSyllabusItem(item: SyllabusItem): String {
        val uid = currentUserId() ?: return ""
        val localId = if (item.id == 0) -nextSyllabusId.getAndIncrement() else item.id
        val itemWithId = item.copy(id = localId)
        val existingUuid = syllabusIdMap.entries.firstOrNull { it.value == localId }?.key
        val dto = itemWithId.toDto(uid)
        return if (existingUuid != null) {
            SupabaseClient.postgrest["syllabus_items"].update(dto) { filter { eq("id", existingUuid) } }
            refreshSyllabusItems()
            existingUuid
        } else {
            val inserted = SupabaseClient.postgrest["syllabus_items"].insert(dto) { select() }
                .decodeSingleOrNull<SyllabusItemDto>() ?: dto
            syllabusIdMap[inserted.id] = localId
            refreshSyllabusItems()
            inserted.id
        }
    }

    suspend fun deleteSyllabusItemById(id: Int) {
        val uuid = syllabusIdMap.entries.firstOrNull { it.value == id }?.key ?: return
        SupabaseClient.postgrest["syllabus_items"].delete { filter { eq("id", uuid) } }
        syllabusIdMap.remove(uuid)
        refreshSyllabusItems()
    }

    // ══════════════════════════════════════════════════════════════════
    //  CLASSES
    // ══════════════════════════════════════════════════════════════════

    suspend fun insertSchoolClass(schoolClass: SchoolClass): String {
        val uid = currentUserId() ?: return ""
        val localId = if (schoolClass.id == 0) -nextClassId.getAndIncrement() else schoolClass.id
        val classWithId = schoolClass.copy(id = localId)
        val existingUuid = classIdMap.entries.firstOrNull { it.value == localId }?.key
        val dto = classWithId.toDto(uid)
        return if (existingUuid != null) {
            SupabaseClient.postgrest["school_classes"].update(dto) { filter { eq("id", existingUuid) } }
            refreshSchoolClasses()
            existingUuid
        } else {
            val inserted = SupabaseClient.postgrest["school_classes"].insert(dto) { select() }
                .decodeSingleOrNull<SchoolClassDto>() ?: dto
            classIdMap[inserted.id] = localId
            refreshSchoolClasses()
            inserted.id
        }
    }

    suspend fun deleteSchoolClassById(id: Int) {
        val uuid = classIdMap.entries.firstOrNull { it.value == id }?.key ?: return
        SupabaseClient.postgrest["school_classes"].delete { filter { eq("id", uuid) } }
        classIdMap.remove(uuid)
        refreshSchoolClasses()
    }

    // ══════════════════════════════════════════════════════════════════
    //  STUDENTS
    // ══════════════════════════════════════════════════════════════════

    suspend fun getStudentsByClass(classId: Int): List<Student> {
        val uid = currentUserId() ?: return emptyList()
        return try {
            val dtos = SupabaseClient.postgrest["students"]
                .select { filter { eq("class_id", classIdMap.entries.firstOrNull { it.value == classId }?.key ?: "") } }
                .decodeList<StudentDto>()
            dtos.filter { it.userId == uid }.map { it.toEntity() }
        } catch (_: Exception) { emptyList() }
    }

    suspend fun insertStudent(student: Student): String {
        val uid = currentUserId() ?: return ""
        val localId = if (student.id == 0) -nextStudentId.getAndIncrement() else student.id
        val studentWithId = student.copy(id = localId)
        val classUuid = classIdMap.entries.firstOrNull { it.value == student.classId }?.key ?: return ""
        val existingUuid = studentIdMap.entries.firstOrNull { it.value == localId }?.key
        val dto = studentWithId.toDto(uid, classUuid)
        return if (existingUuid != null) {
            SupabaseClient.postgrest["students"].update(dto) { filter { eq("id", existingUuid) } }
            existingUuid
        } else {
            val inserted = SupabaseClient.postgrest["students"].insert(dto) { select() }
                .decodeSingleOrNull<StudentDto>() ?: dto
            studentIdMap[inserted.id] = localId
            inserted.id
        }
    }

    suspend fun deleteStudentById(id: Int) {
        val uuid = studentIdMap.entries.firstOrNull { it.value == id }?.key ?: return
        SupabaseClient.postgrest["students"].delete { filter { eq("id", uuid) } }
        studentIdMap.remove(uuid)
    }

    // ══════════════════════════════════════════════════════════════════
    //  USER ACCOUNTS
    // ══════════════════════════════════════════════════════════════════

    suspend fun getUserAccount(email: String): UserAccount? {
        return try {
            SupabaseClient.postgrest["user_accounts"]
                .select { filter { eq("email", email.trim().lowercase()) } }
                .decodeSingleOrNull<UserAccountDto>()
                ?.toUserAccountEntity()
        } catch (_: Exception) { null }
    }

    suspend fun insertUserAccount(user: UserAccount) {
        val existingDto = try {
            SupabaseClient.postgrest["user_accounts"]
                .select { filter { eq("email", user.email.trim().lowercase()) } }
                .decodeSingleOrNull<UserAccountDto>()
        } catch (_: Exception) { null }
        val dto = UserAccountDto(
            id = existingDto?.id ?: "",
            authUid = existingDto?.authUid ?: "",
            email = user.email.trim().lowercase(),
            passwordHash = user.passwordHash,
            fullName = user.fullName,
            gender = user.gender,
            dob = user.dob,
            address = user.address,
            phone = user.phone,
            teachingStatus = user.teachingStatus,
            isOnboardingCompleted = user.isOnboardingCompleted,
            subscriptionPlan = user.subscriptionPlan,
            paymentEmail = user.paymentEmail,
            planExpiresAt = user.planExpiresAt?.let { java.time.Instant.ofEpochMilli(it).toString() },
            lastPaymentReference = user.lastPaymentReference,
            lastPaymentDate = user.lastPaymentDate?.let { java.time.Instant.ofEpochMilli(it).toString() }
        )
        if (existingDto != null) {
            SupabaseClient.postgrest["user_accounts"].update(dto) { filter { eq("email", user.email.trim().lowercase()) } }
        } else {
            SupabaseClient.postgrest["user_accounts"].insert(dto)
        }
    }

    // ══════════════════════════════════════════════════════════════════
    //  PREFERENCES
    // ══════════════════════════════════════════════════════════════════

    suspend fun getPreference(key: String): String? {
        val uid = currentUserId() ?: return null
        return try {
            SupabaseClient.postgrest["user_preferences"]
                .select { filter { eq("user_id", uid) } }
                .decodeList<UserPreferenceDto>()
                .firstOrNull { it.key == key }?.value
        } catch (_: Exception) { null }
    }

    suspend fun setPreference(key: String, value: String) {
        val uid = currentUserId() ?: return
        val existing = try {
            SupabaseClient.postgrest["user_preferences"]
                .select { filter { eq("user_id", uid) } }
                .decodeList<UserPreferenceDto>()
                .firstOrNull { it.key == key }
        } catch (_: Exception) { null }

        if (existing != null) {
            SupabaseClient.postgrest["user_preferences"].update(
                UserPreferenceDto(userId = uid, key = key, value = value)
            ) { filter { eq("id", existing.id) } }
        } else {
            SupabaseClient.postgrest["user_preferences"].insert(
                UserPreferenceDto(userId = uid, key = key, value = value)
            )
        }
    }

    suspend fun getSubscriptionPlan(): String {
        return getPreference("subscription_plan") ?: "BASIC"
    }

    suspend fun setSubscriptionPlan(plan: String) {
        setPreference("subscription_plan", plan)
    }

    suspend fun getGenerationCount(): Int {
        val str = getPreference("limit_generations") ?: return 0
        return str.toIntOrNull() ?: 0
    }

    suspend fun incrementGenerationCount() {
        val current = getGenerationCount()
        setPreference("limit_generations", (current + 1).toString())
    }

    suspend fun resetMonthlyLimits() {
        setPreference("limit_generations", "0")
    }

    // ══════════════════════════════════════════════════════════════════
    //  PLAN MIGRATION (FREE→BASIC, STANDARD→ADVANCE)
    // ══════════════════════════════════════════════════════════════════

    suspend fun migratePlanIfNeeded(): String {
        val plan = getSubscriptionPlan()
        return when (plan) {
            "FREE" -> { setSubscriptionPlan("BASIC"); "BASIC" }
            "STANDARD" -> { setSubscriptionPlan("ADVANCE"); "ADVANCE" }
            else -> plan
        }
    }

    // ══════════════════════════════════════════════════════════════════
    //  DTO → ENTITY MAPPING
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
            subscriptionPlan = subscriptionPlan,
            paymentEmail = paymentEmail,
            planExpiresAt = planExpiresAt?.let { parseTimestamp(it) },
            lastPaymentReference = lastPaymentReference,
            lastPaymentDate = lastPaymentDate?.let { parseTimestamp(it) }
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
        completionDate = completionDate?.let { java.time.Instant.ofEpochMilli(it).toString() }
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

    private suspend fun <T> Flow<List<T>>.firstOrEmpty(): List<T> {
        return try {
            this.first()
        } catch (_: Exception) { emptyList() }
    }
}
