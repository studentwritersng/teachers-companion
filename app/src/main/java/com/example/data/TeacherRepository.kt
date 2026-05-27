package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class TeacherRepository(private val dao: TeacherDao) {

    // --- LESSON NOTES ---
    val allLessonNotes: Flow<List<LessonNote>> = dao.getAllLessonNotes()
    
    suspend fun insertLessonNote(note: LessonNote): Long = dao.insertLessonNote(note)
    
    suspend fun deleteLessonNoteById(id: Int) = dao.deleteLessonNoteById(id)

    // --- MCQ SETS ---
    val allMCQSets: Flow<List<MCQSet>> = dao.getAllMCQSets()
    
    suspend fun insertMCQSet(mcqSet: MCQSet): Long = dao.insertMCQSet(mcqSet)
    
    suspend fun deleteMCQSetById(id: Int) = dao.deleteMCQSetById(id)

    // --- THEORY SETS ---
    val allTheorySets: Flow<List<TheorySet>> = dao.getAllTheorySets()
    
    suspend fun insertTheorySet(theorySet: TheorySet): Long = dao.insertTheorySet(theorySet)
    
    suspend fun deleteTheorySetById(id: Int) = dao.deleteTheorySetById(id)

    // --- TIMETABLE ITEMS ---
    val allTimetableItems: Flow<List<TimetableItem>> = dao.getAllTimetableItems()
    
    suspend fun insertTimetableItem(item: TimetableItem): Long = dao.insertTimetableItem(item)
    
    suspend fun deleteTimetableItemById(id: Int) = dao.deleteTimetableItemById(id)

    // --- SYLLABUS ITEMS ---
    val allSyllabusItems: Flow<List<SyllabusItem>> = dao.getAllSyllabusItems()
    val syllabusSubjects: Flow<List<String>> = dao.getSyllabusSubjects()

    suspend fun insertSyllabusItem(item: SyllabusItem): Long = dao.insertSyllabusItem(item)
    
    suspend fun deleteSyllabusItemById(id: Int) = dao.deleteSyllabusItemById(id)

    suspend fun checkAndPrepopulateSyllabus() {
        // No-op to remove all demo content and let the user register real topics
    }

    // --- CLASSES ---
    val allSchoolClasses: Flow<List<SchoolClass>> = dao.getAllSchoolClasses()
    
    suspend fun insertSchoolClass(schoolClass: SchoolClass): Long = dao.insertSchoolClass(schoolClass)
    
    suspend fun deleteSchoolClassById(id: Int) = dao.deleteSchoolClassById(id)

    // --- STUDENTS ---
    fun getStudentsByClass(classId: Int): Flow<List<Student>> = dao.getStudentsByClass(classId)
    
    suspend fun insertStudent(student: Student): Long = dao.insertStudent(student)
    
    suspend fun deleteStudentById(id: Int) = dao.deleteStudentById(id)

    // --- PREFERENCES (FOR SUBSCRIPTIONS AND CONFIGS) ---
    suspend fun getPreference(key: String): String? {
        return dao.getPreference(key)?.value
    }

    suspend fun setPreference(key: String, value: String) {
        dao.insertPreference(UserPreference(key, value))
    }

    suspend fun getSubscriptionPlan(): String {
        return dao.getPreference("subscription_plan")?.value ?: "PREMIUM"
    }

    suspend fun setSubscriptionPlan(plan: String) {
        dao.insertPreference(UserPreference("subscription_plan", plan))
    }

    suspend fun getUsageLimit(key: String): Int {
        val str = dao.getPreference("limit_$key")?.value ?: "0"
        return str.toIntOrNull() ?: 0
    }

    suspend fun incrementUsageLimit(key: String) {
        val current = getUsageLimit(key)
        dao.insertPreference(UserPreference("limit_$key", (current + 1).toString()))
    }

    suspend fun resetMonthlyLimits() {
        dao.insertPreference(UserPreference("limit_lesson_notes", "0"))
        dao.insertPreference(UserPreference("limit_mcqs", "0"))
    }

    // --- USER ACCOUNTS ---
    suspend fun getUserAccount(email: String): UserAccount? {
        return dao.getUserAccount(email.trim().lowercase())
    }

    suspend fun insertUserAccount(user: UserAccount) {
        dao.insertUserAccount(user.copy(email = user.email.trim().lowercase()))
    }
}
