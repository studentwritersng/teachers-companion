package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "lesson_notes")
data class LessonNote(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val subject: String,
    val gradeClass: String,
    val topic: String,
    val duration: String,
    val behavioralObjectives: String,
    val entryBehavior: String,
    val instructionalMaterials: String,
    val introduction: String,
    val presentation: String, // Step-by-step presentation
    val evaluationQuestions: String,
    val conclusion: String,
    val assignment: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "mcq_sets")
data class MCQSet(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val subject: String,
    val gradeClass: String,
    val topic: String,
    val difficulty: String, // Easy, Medium, Hard
    val questionsJson: String, // List of questions in JSON
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "theory_sets")
data class TheorySet(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val subject: String,
    val gradeClass: String,
    val topic: String,
    val questionsJson: String, // Essay list in JSON
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "timetable_items")
data class TimetableItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val dayOfWeek: String, // Monday, Tuesday, Wednesday, Thursday, Friday
    val startTime: String, // HH:MM
    val endTime: String,   // HH:MM
    val subject: String,
    val gradeClass: String,
    val schoolName: String, // Multi-school support
    val colorHex: String,
    val isCompleted: Boolean = false
)

@Entity(tableName = "syllabus_items")
data class SyllabusItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val schoolName: String = "",
    val gradeClass: String = "",
    val subject: String = "",
    val term: String = "Term 1",
    val week: Int = 1,
    val theme: String = "",
    val topic: String = "",
    val content: String = "",
    val objectives: String = "",
    val isCompleted: Boolean = false,
    val completionDate: Long? = null
)

@Entity(tableName = "school_classes")
data class SchoolClass(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val className: String, // e.g. SS1 A, JSS3
    val schoolName: String, // e.g. Community Girls Sec, Zaria
    val subject: String
)

@Entity(tableName = "students")
data class Student(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val classId: Int,
    val fullName: String,
    val performanceNotes: String = "",
    val attendanceCount: Int = 0,
    val totalSessions: Int = 0
)

@Entity(tableName = "user_preferences")
data class UserPreference(
    @PrimaryKey val key: String,
    val value: String
)

@Entity(tableName = "user_accounts")
data class UserAccount(
    @PrimaryKey val email: String,
    val passwordHash: String,
    val fullName: String = "",
    val gender: String = "",
    val dob: String = "",
    val address: String = "",
    val phone: String = "",
    val teachingStatus: String = "FULL_TIME", // "FULL_TIME" or "PART_TIME"
    val isOnboardingCompleted: Boolean = false,
    val subscriptionPlan: String = "FREE"
)

