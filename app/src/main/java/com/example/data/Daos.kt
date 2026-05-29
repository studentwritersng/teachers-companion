package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TeacherDao {

    // --- LESSON NOTES ---
    @Query("SELECT * FROM lesson_notes ORDER BY createdAt DESC")
    fun getAllLessonNotes(): Flow<List<LessonNote>>

    @Query("SELECT * FROM lesson_notes WHERE subject = :subject ORDER BY createdAt DESC")
    fun getLessonNotesBySubject(subject: String): Flow<List<LessonNote>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLessonNote(note: LessonNote): Long

    @Query("DELETE FROM lesson_notes WHERE id = :id")
    suspend fun deleteLessonNoteById(id: Int)

    @Query("DELETE FROM lesson_notes")
    suspend fun deleteAllLessonNotes()

    // --- MCQ SETS ---
    @Query("SELECT * FROM mcq_sets ORDER BY createdAt DESC")
    fun getAllMCQSets(): Flow<List<MCQSet>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMCQSet(mcqSet: MCQSet): Long

    @Query("DELETE FROM mcq_sets WHERE id = :id")
    suspend fun deleteMCQSetById(id: Int)

    @Query("DELETE FROM mcq_sets")
    suspend fun deleteAllMCQSets()

    // --- THEORY SETS ---
    @Query("SELECT * FROM theory_sets ORDER BY createdAt DESC")
    fun getAllTheorySets(): Flow<List<TheorySet>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTheorySet(theorySet: TheorySet): Long

    @Query("DELETE FROM theory_sets WHERE id = :id")
    suspend fun deleteTheorySetById(id: Int)

    @Query("DELETE FROM theory_sets")
    suspend fun deleteAllTheorySets()

    // --- TIMETABLE ITEMS ---
    @Query("SELECT * FROM timetable_items ORDER BY startTime ASC")
    fun getAllTimetableItems(): Flow<List<TimetableItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTimetableItem(item: TimetableItem): Long

    @Query("DELETE FROM timetable_items WHERE id = :id")
    suspend fun deleteTimetableItemById(id: Int)

    @Query("DELETE FROM timetable_items")
    suspend fun deleteAllTimetableItems()

    // --- SYLLABUS ITEMS ---
    @Query("SELECT * FROM syllabus_items ORDER BY id ASC")
    fun getAllSyllabusItems(): Flow<List<SyllabusItem>>

    @Query("SELECT DISTINCT subject FROM syllabus_items")
    fun getSyllabusSubjects(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSyllabusItem(item: SyllabusItem): Long

    @Query("DELETE FROM syllabus_items WHERE id = :id")
    suspend fun deleteSyllabusItemById(id: Int)

    @Query("DELETE FROM syllabus_items")
    suspend fun deleteAllSyllabusItems()

    // --- CLASSES ---
    @Query("SELECT * FROM school_classes ORDER BY className ASC")
    fun getAllSchoolClasses(): Flow<List<SchoolClass>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchoolClass(schoolClass: SchoolClass): Long

    @Query("DELETE FROM school_classes WHERE id = :id")
    suspend fun deleteSchoolClassById(id: Int)

    @Query("DELETE FROM school_classes")
    suspend fun deleteAllSchoolClasses()

    // --- STUDENTS ---
    @Query("SELECT * FROM students WHERE classId = :classId ORDER BY fullName ASC")
    fun getStudentsByClass(classId: Int): Flow<List<Student>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudent(student: Student): Long

    @Query("DELETE FROM students WHERE id = :id")
    suspend fun deleteStudentById(id: Int)

    @Query("DELETE FROM students")
    suspend fun deleteAllStudents()

    // --- PREFERENCES ---
    @Query("SELECT * FROM user_preferences WHERE `key` = :key LIMIT 1")
    suspend fun getPreference(key: String): UserPreference?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreference(pref: UserPreference)

    // --- USER ACCOUNTS ---
    @Query("SELECT * FROM user_accounts WHERE email = :email LIMIT 1")
    suspend fun getUserAccount(email: String): UserAccount?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserAccount(user: UserAccount)
}
