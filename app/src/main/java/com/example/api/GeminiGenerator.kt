package com.example.api

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiGenerator {
    private const val TAG = "GeminiGenerator"
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private fun getApiKey(): String {
        return try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }
    }

    suspend fun generateLessonNote(
        subject: String,
        gradeClass: String,
        topic: String,
        duration: String,
        syllabusObjectives: String,
        syllabusContent: String,
        customInstructions: String
    ): String = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            Log.w(TAG, "Gemini API key is missing. Using local fallback generation.")
            return@withContext getLocalFallbackNote(subject, gradeClass, topic, duration, syllabusObjectives, syllabusContent, customInstructions)
        }

        val prompt = """
            Create a highly professional and complete lesson note for the following settings conforming to the Nigerian curriculum:
            Subject: $subject
            Grade/Class: $gradeClass
            Topic: $topic
            Duration: $duration
            Syllabus Objectives to Conform to: $syllabusObjectives
            Syllabus Content: $syllabusContent
            Custom Guidelines or Instructions: $customInstructions

            CRITICAL STYLE AND LANGUAGE DIRECTIVES:
            1. Language: You MUST use British English exclusively (e.g., use 'behaviour', 'programme', 'colour', 'centre', 'analyse', and avoid American spellings like 'behavior', 'program', 'color', 'center', 'analyze').
            2. Cultural Context & Examples: All examples, names, currencies, and scenarios used MUST align with the Nigerian context (e.g., using names like Chidi, Amina, Bukola, Emeka, places like Lagos, Abuja, Enugu, Kaduna, and currency like Naira ₦ where applicable).

            Please provide:
            1. Lesson Objectives (conforming strictly to the provided syllabus objectives: Cognitive, Psychomotor elements)
            2. Entry Behaviour / Introduction (activating prior knowledge with local Nigerian context)
            3. Detailed Presentation Steps (Step 1, Step 2, Step 3)
            4. Classroom Evaluation Questions (written in British English and matching Nigeria context)
            5. Final Homework Assignment
            Make it look cleanly formatted with headings.
        """.trimIndent()

        try {
            val response = callGeminiApi(apiKey, prompt)
            if (response.isNotBlank()) {
                response
            } else {
                getLocalFallbackNote(subject, gradeClass, topic, duration, syllabusObjectives, syllabusContent, customInstructions)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gemini API failed, using fallback note", e)
            getLocalFallbackNote(subject, gradeClass, topic, duration, syllabusObjectives, syllabusContent, customInstructions)
        }
    }

    suspend fun generateMCQs(
        subject: String,
        gradeClass: String,
        topic: String,
        difficulty: String,
        count: Int
    ): String = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext getLocalFallbackMCQs(subject, gradeClass, topic, count)
        }

        val prompt = """
            Generate exactly $count multiple choice questions (MCQs) for:
            Subject: $subject
            Grade/Class: $gradeClass
            Topic: $topic
            Difficulty: $difficulty

            CRITICAL STYLE AND LANGUAGE DIRECTIVES:
            1. Language: Use British English exclusively (e.g., use 'behaviour', 'programme', 'colour', 'centre', 'analyse', and avoid American spellings).
            2. Cultural Context & Examples: All examples, names, currencies, and scenarios used MUST align with the Nigerian context (e.g., names like Bola, Amina, Chidi, and currencies like Naira ₦).

            You must respond ONLY with a raw JSON array of objects representing questions. Do not include any markdown block markers such as ```json or ```. Provide strict JSON list structures. Each object must have these exactly spelled keys:
            - "question": text of the question
            - "options": array of exactly 4 strings for choices
            - "answer": the exact correct option string matching one of the options
        """.trimIndent()

        try {
            val raw = callGeminiApi(apiKey, prompt)
            val cleaned = cleanJsonString(raw)
            if (isValidJsonArray(cleaned)) {
                cleaned
            } else {
                getLocalFallbackMCQs(subject, gradeClass, topic, count)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gemini MCQ API failed, using fallback list", e)
            getLocalFallbackMCQs(subject, gradeClass, topic, count)
        }
    }

    suspend fun generateTheoryQuestions(
        subject: String,
        gradeClass: String,
        topic: String,
        count: Int
    ): String = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext getLocalFallbackTheory(subject, gradeClass, topic, count)
        }

        val prompt = """
            Generate exactly $count essay or analytical theory questions for:
            Subject: $subject
            Grade/Class: $gradeClass
            Topic: $topic

            CRITICAL STYLE AND LANGUAGE DIRECTIVES:
            1. Language: Use British English exclusively (e.g., use 'behaviour', 'programme', 'colour', 'centre', 'analyse', and avoid American spellings).
            2. Cultural Context & Examples: All examples, names, currencies, and scenarios used MUST align with the Nigerian context (e.g., names like Bola, Amina, Chidi, and currencies like Naira ₦).

            You must respond ONLY with a raw JSON array of objects representing theory questions. Do not include any HTML or markdown block markers such as ```json or ```. Each object must contain exactly:
            - "question": the essay question text
            - "suggestedAnswer": a bulleted outline or description of the expected correct response in British English with Nigerian context examples where applicable.
        """.trimIndent()

        try {
            val raw = callGeminiApi(apiKey, prompt)
            val cleaned = cleanJsonString(raw)
            if (isValidJsonArray(cleaned)) {
                cleaned
            } else {
                getLocalFallbackTheory(subject, gradeClass, topic, count)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gemini Theory API failed, using fallback list", e)
            getLocalFallbackTheory(subject, gradeClass, topic, count)
        }
    }

    private fun callGeminiApi(apiKey: String, prompt: String): String {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
        
        val requestJson = JSONObject()
        val contentsArray = JSONArray()
        val contentObj = JSONObject()
        val partsArray = JSONArray()
        val partObj = JSONObject()
        
        partObj.put("text", prompt)
        partsArray.put(partObj)
        contentObj.put("parts", partsArray)
        contentsArray.put(contentObj)
        requestJson.put("contents", contentsArray)

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = requestJson.toString().toRequestBody(mediaType)
        
        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()
            
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                Log.e(TAG, "Request failed code: ${response.code} message: ${response.message}")
                return ""
            }
            val resBody = response.body?.string() ?: return ""
            val json = JSONObject(resBody)
            val candidates = json.optJSONArray("candidates") ?: return ""
            if (candidates.length() > 0) {
                val candidateOutput = candidates.getJSONObject(0)
                val content = candidateOutput.optJSONObject("content") ?: return ""
                val parts = content.optJSONArray("parts") ?: return ""
                if (parts.length() > 0) {
                    return parts.getJSONObject(0).optString("text", "")
                }
            }
            return ""
        }
    }

    private fun cleanJsonString(raw: String): String {
        var str = raw.trim()
        if (str.startsWith("```json")) {
            str = str.substringAfter("```json")
        } else if (str.startsWith("```")) {
            str = str.substringAfter("```")
        }
        if (str.endsWith("```")) {
            str = str.substringBeforeLast("```")
        }
        return str.trim()
    }

    private fun isValidJsonArray(str: String): Boolean {
        return try {
            JSONArray(str)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun getLocalFallbackNote(
        subject: String,
        gradeClass: String,
        topic: String,
        duration: String,
        syllabusObjectives: String,
        syllabusContent: String,
        customInstructions: String
    ): String {
        return """
            LESSON NOTE REFERENCE SHEET (Offline Generation)
            ==============================================
            SUBJECT: $subject
            GRADE CLASS: $gradeClass
            TOPIC: $topic
            DURATION: $duration
            
            1. BEHAVIOURAL OBJECTIVES:
               By the conclusion of this instructional unit, students should successfully be able to:
               - Explain the fundamentals of $topic
               ${if (syllabusObjectives.isNotEmpty()) "- Conforms to syllabus objectives: $syllabusObjectives" else "- Highlight key aspects or components related to the unit"}
               - Complete evaluation challenges based on the lessons
               
            2. ENTRY BEHAVIOUR:
               Students are presumed to have some baseline context of similar ideas in daily life in Nigeria.
               
            3. INSTRUCTIONAL RESOURCES:
               High contrast chalkboard diagrams, flash cards, physical replicas, and text manuals reflecting Nigerian scenarios.
               
            4. STEP-BY-STEP PRESENTATION (conforming to content: $syllabusContent):
               * Step 1 (Introduction): Activate background knowledge. Engage students with open ended queries about $topic. (Time: 5 mins)
               * Step 2 (Direct Teaching): Define the core principles of $topic clearly. Document details on the classroom chalkboard. (Time: 15 mins)
               * Step 3 (Interactive Discussion): Allow collaborative work in small groups of students to review real world applications in Nigeria. (Time: 15 mins)
               * Step 4 (Summary): Review critical ideas, reinforcing terminology and vocabulary in British English. (Time: 5 mins)
               
            5. CLASSROOM TEST QUESTIONS:
               - State a clear definition of $topic in your own words.
               - Outline three practical advantages of exploring $topic in Nigeria.
               
            6. HOMEWORK ASSIGNMENT:
               Read Chapter 4 of the curriculum handbook and draft a one page synthesis answering queries about $topic.
               
            NOTE:
            $customInstructions
        """.trimIndent()
    }

    private fun getLocalFallbackMCQs(subject: String, gradeClass: String, topic: String, count: Int): String {
        val arr = JSONArray()
        for (i in 1..count) {
            val obj = JSONObject()
            obj.put("question", "Question $i: What is the primary characteristic or fundamental function of $topic in $subject?")
            val opts = JSONArray()
            opts.put("Standard active element")
            opts.put("Irrelevant baseline placeholder")
            opts.put("A core supportive entity")
            opts.put("None of the standard answers")
            obj.put("options", opts)
            obj.put("answer", "Standard active element")
            arr.put(obj)
        }
        return arr.toString()
    }

    private fun getLocalFallbackTheory(subject: String, gradeClass: String, topic: String, count: Int): String {
        val arr = JSONArray()
        for (i in 1..count) {
            val obj = JSONObject()
            obj.put("question", "Theory Problem $i: Critically analyze how $topic can be effectively contextualized for modern uses in $gradeClass. Provide a detailed analysis.")
            obj.put("suggestedAnswer", "1. Define core terminology & parameters.\n2. Sketch standard historical development.\n3. Integrate relevant classroom metrics and outcomes.")
            arr.put(obj)
        }
        return arr.toString()
    }
}
