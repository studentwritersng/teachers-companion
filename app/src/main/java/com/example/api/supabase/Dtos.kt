package com.example.api.supabase

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class UserAccountDto(
    val id: String = "",
    @SerialName("auth_uid") val authUid: String = "",
    val email: String = "",
    @SerialName("password_hash") val passwordHash: String = "",
    @SerialName("full_name") val fullName: String = "",
    val gender: String = "",
    val dob: String = "",
    val address: String = "",
    val phone: String = "",
    @SerialName("teaching_status") val teachingStatus: String = "FULL_TIME",
    @SerialName("is_onboarding_completed") val isOnboardingCompleted: Boolean = false,
    @SerialName("subscription_plan") val subscriptionPlan: String = "FREE",
    val schools: List<String> = emptyList(),
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = ""
)

@Serializable
data class LessonNoteDto(
    val id: String = "",
    @SerialName("user_id") val userId: String = "",
    val title: String = "",
    val subject: String = "",
    @SerialName("grade_class") val gradeClass: String = "",
    val topic: String = "",
    val duration: String = "",
    @SerialName("behavioral_objectives") val behavioralObjectives: String = "",
    @SerialName("entry_behavior") val entryBehavior: String = "",
    @SerialName("instructional_materials") val instructionalMaterials: String = "",
    val introduction: String = "",
    val presentation: String = "",
    @SerialName("evaluation_questions") val evaluationQuestions: String = "",
    val conclusion: String = "",
    val assignment: String = "",
    @SerialName("syllabus_item_id") val syllabusItemId: String? = null,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = ""
)

@Serializable
data class McqSetDto(
    val id: String = "",
    @SerialName("user_id") val userId: String = "",
    val title: String = "",
    val subject: String = "",
    @SerialName("grade_class") val gradeClass: String = "",
    val topic: String = "",
    val difficulty: String = "Medium",
    @SerialName("questions_json") val questionsJson: String = "[]",
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = ""
)

@Serializable
data class TheorySetDto(
    val id: String = "",
    @SerialName("user_id") val userId: String = "",
    val title: String = "",
    val subject: String = "",
    @SerialName("grade_class") val gradeClass: String = "",
    val topic: String = "",
    @SerialName("questions_json") val questionsJson: String = "[]",
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = ""
)

@Serializable
data class TimetableItemDto(
    val id: String = "",
    @SerialName("user_id") val userId: String = "",
    @SerialName("day_of_week") val dayOfWeek: String = "",
    @SerialName("start_time") val startTime: String = "",
    @SerialName("end_time") val endTime: String = "",
    val subject: String = "",
    @SerialName("grade_class") val gradeClass: String = "",
    @SerialName("school_name") val schoolName: String = "",
    @SerialName("color_hex") val colorHex: String = "#4F81BD",
    @SerialName("is_completed") val isCompleted: Boolean = false,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = ""
)

@Serializable
data class SyllabusItemDto(
    val id: String = "",
    @SerialName("user_id") val userId: String = "",
    @SerialName("school_name") val schoolName: String = "",
    @SerialName("grade_class") val gradeClass: String = "",
    val subject: String = "",
    val term: String = "Term 1",
    val week: Int = 1,
    val theme: String = "",
    val topic: String = "",
    val content: String = "",
    val objectives: String = "",
    @SerialName("is_completed") val isCompleted: Boolean = false,
    @SerialName("completion_date") val completionDate: String? = null,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = ""
)

@Serializable
data class SchoolClassDto(
    val id: String = "",
    @SerialName("user_id") val userId: String = "",
    @SerialName("class_name") val className: String = "",
    @SerialName("school_name") val schoolName: String = "",
    val subject: String = "",
    @SerialName("created_at") val createdAt: String = ""
)

@Serializable
data class StudentDto(
    val id: String = "",
    @SerialName("user_id") val userId: String = "",
    @SerialName("class_id") val classId: String = "",
    @SerialName("full_name") val fullName: String = "",
    @SerialName("performance_notes") val performanceNotes: String = "",
    @SerialName("attendance_count") val attendanceCount: Int = 0,
    @SerialName("total_sessions") val totalSessions: Int = 0,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = ""
)

@Serializable
data class UserPreferenceDto(
    val id: String = "",
    @SerialName("user_id") val userId: String = "",
    val key: String = "",
    val value: String = "",
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = ""
)

@Serializable
data class UsageLimitDto(
    val id: String = "",
    @SerialName("user_id") val userId: String = "",
    val key: String = "",
    val value: Int = 0,
    val month: String = "",
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = ""
)
