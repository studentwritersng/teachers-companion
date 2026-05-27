package com.example.api

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

object GeminiGenerator {
    private const val TAG = "GeminiGenerator"

    suspend fun generateLessonNote(
        subject: String,
        gradeClass: String,
        topic: String,
        duration: String,
        syllabusObjectives: String,
        syllabusContent: String,
        customInstructions: String,
        theme: String = ""
    ): String = withContext(Dispatchers.IO) {
        val themeLine = if (theme.isNotBlank()) "Theme / Aspect: $theme" else ""

        val prompt = """
            Create a highly professional and complete lesson note for the following settings conforming to the Nigerian curriculum:
            Subject: $subject
            Grade/Class: $gradeClass
            Topic: $topic
            Duration: $duration
            $themeLine
            Syllabus Objectives to Conform to: $syllabusObjectives
            Syllabus Content: $syllabusContent
            Custom Guidelines or Instructions: $customInstructions

            CRITICAL STYLE AND LANGUAGE DIRECTIVES:
            1. Language: You MUST use British English exclusively (e.g., use 'behaviour', 'programme', 'colour', 'centre', 'analyse', and avoid American spellings like 'behavior', 'program', 'color', 'center', 'analyze').
            2. Cultural Context & Examples: All examples, names, currencies, and scenarios used MUST align with the Nigerian context (e.g., using names like Chidi, Amina, Bukola, Emeka, places like Lagos, Abuja, Enugu, Kaduna, and currency like Naira ₦ where applicable).

            You must produce a professionally formatted lesson note with the following sections. Use the exact labelled headings shown below:

            INSTRUCTIONAL MATERIALS
            List 4-5 specific, concrete teaching aids (e.g. wall charts, photographs, real objects, models) appropriate for this topic in a Nigerian classroom.

            REFERENCE BOOKS
            List 2-3 standard Nigerian textbooks for this subject and class. For each, include the full citation and a short reference code in parentheses like (Ref:ShortCode). Example: Physical and Health Education for Junior Secondary Schools Book 2 by C.O. Egbunike (Cited as: Ref:Egbunike)

            BEHAVIOURAL OBJECTIVES
            Write 4-5 specific, measurable objectives starting with action verbs (Define, Identify, Categorize, Explain, Demonstrate, etc.) that conform strictly to the provided syllabus objectives. Each objective must be what a student will be able to do by the end of the lesson.

            PREVIOUS KNOWLEDGE
            State what students already know from prior classes that connects to this topic, written as a single paragraph referencing the previous grade or term.

            INTRODUCTION
            A brief narrative of how the teacher introduces the lesson — typically a thought-provoking question or scenario that connects the topic to students' daily Nigerian experience.

            PRESENTATION (STEPS)
            Write exactly 4 steps. For each step, provide:
            Step [N]
            Teacher's Activity: What the teacher does (explains, demonstrates, shows, asks).
            Student's Activity: What the students do (listen, observe, answer, discuss, write).

            CONTENT
            Write the detailed subject-matter content for this topic. Organize it with subheadings (e.g. DEFINITION, CATEGORIES, CAUSES, PREVENTION). Include specific facts, local examples (Nigerian foods, places, names), and inline citations using the reference codes from the REFERENCE BOOKS section like [Ref:ShortCode]. This section should be thorough and substantive — at least 3-5 paragraphs with subheadings.

            EVALUATION
            List 3-4 questions the teacher will use to assess understanding during or at the end of the lesson. These should be written in British English with Nigerian context.

            SUMMARY
            A 2-3 sentence summary the teacher will use to wrap up the lesson, reinforcing the key takeaways.

            ASSIGNMENT
            Write 2 homework tasks. One should involve real-world observation or family interaction (in the Nigerian context), and the other should be a short written or research task.
        """.trimIndent()

        val response = ProviderRouter.call(prompt)
        if (response.isNotBlank()) {
            response
        } else {
            Log.w(TAG, "All providers failed. Using fallback note.")
            getLocalFallbackNote(subject, gradeClass, topic, duration, syllabusObjectives, syllabusContent, customInstructions)
        }
    }

    suspend fun generateMCQs(
        subject: String,
        gradeClass: String,
        topic: String,
        difficulty: String,
        count: Int,
        lessonContext: String = "",
        lessonReliancePercent: Int = 0
    ): String = withContext(Dispatchers.IO) {
        val contextDirective = if (lessonContext.isNotBlank() && lessonReliancePercent > 0) """
            LESSON NOTE CONTEXT (existing lesson notes for this class and subject):
            $lessonContext

            CONTENT MIX INSTRUCTION: Base $lessonReliancePercent% of the questions strictly on the lesson notes above. The remaining ${100 - lessonReliancePercent}% may draw from your own general subject-matter knowledge, but must remain strictly within the curriculum scope for $subject ($gradeClass) in Nigeria.
        """.trimIndent() else ""

        val prompt = """
            Generate exactly $count multiple choice questions (MCQs) for:
            Subject: $subject
            Grade/Class: $gradeClass
            Topic: $topic
            Difficulty: $difficulty

            $contextDirective

            MCQ FORMAT VARIETY: Mix up the question types across the set. Use these different formats (do NOT use the same format for every question):
            1. Fill in the blank — e.g. "The primary function of the heart is to _____ blood."
            2. Most suitable — e.g. "Which of the following is the MOST suitable material for making a cooking pot?"
            3. Incomplete statement — e.g. "A triangle with all sides equal is called _____."
            4. Assertion-Reason — e.g. "Assertion (A): The sun rises in the east. Reason (R): The earth rotates from west to east."
            5. Odd one out — e.g. "Which of the following is the ODD ONE OUT? A. Kidney B. Liver C. Heart D. Lung" (the question should include the options inline and ask which is odd)
            6. Sequence arrangement — e.g. "Arrange the following stages in the correct order: A. Pupae B. Egg C. Adult D. Larva"
            7. Direct question — standard MCQ format

            CRITICAL STYLE AND LANGUAGE DIRECTIVES:
            1. Language: Use British English exclusively (e.g., use 'behaviour', 'programme', 'colour', 'centre', 'analyse', and avoid American spellings).
            2. Cultural Context & Examples: All examples, names, currencies, and scenarios used MUST align with the Nigerian context (e.g., names like Bola, Amina, Chidi, and currencies like Naira ₦).

            You must respond ONLY with a raw JSON array of objects representing questions. Do not include any markdown block markers such as ```json or ```. Provide strict JSON list structures. Each object must have these exactly spelled keys:
            - "question": text of the question (for odd-one-out, include the options inline in the question text)
            - "optionA": first choice
            - "optionB": second choice
            - "optionC": third choice
            - "optionD": fourth choice
            - "correctAnswer": the exact correct option text (must match exactly one of the optionA-D values)
            - "explanation": a brief explanation of why this answer is correct (optional but encouraged)
        """.trimIndent()

        val raw = ProviderRouter.call(prompt)
        if (raw.isBlank()) {
            Log.w(TAG, "All providers failed for MCQ. Using fallback.")
            return@withContext getLocalFallbackMCQs(subject, gradeClass, topic, count)
        }

        val cleaned = cleanJsonString(raw)
        if (isValidJsonArray(cleaned)) {
            cleaned
        } else {
            getLocalFallbackMCQs(subject, gradeClass, topic, count)
        }
    }

    suspend fun generateTheoryQuestions(
        subject: String,
        gradeClass: String,
        topic: String,
        count: Int,
        lessonContext: String = "",
        lessonReliancePercent: Int = 0
    ): String = withContext(Dispatchers.IO) {
        val contextDirective = if (lessonContext.isNotBlank() && lessonReliancePercent > 0) """
            LESSON NOTE CONTEXT (existing lesson notes for this class and subject):
            $lessonContext

            CONTENT MIX INSTRUCTION: Base $lessonReliancePercent% of the questions strictly on the lesson notes above. The remaining ${100 - lessonReliancePercent}% may draw from your own general subject-matter knowledge, but must remain strictly within the curriculum scope for $subject ($gradeClass) in Nigeria.
        """.trimIndent() else ""

        val prompt = """
            Generate exactly $count essay or analytical theory questions for:
            Subject: $subject
            Grade/Class: $gradeClass
            Topic: $topic

            $contextDirective

            CRITICAL STYLE AND LANGUAGE DIRECTIVES:
            1. Language: Use British English exclusively (e.g., use 'behaviour', 'programme', 'colour', 'centre', 'analyse', and avoid American spellings).
            2. Cultural Context & Examples: All examples, names, currencies, and scenarios used MUST align with the Nigerian context (e.g., names like Bola, Amina, Chidi, and currencies like Naira ₦).

            You must respond ONLY with a raw JSON array of objects representing theory questions. Do not include any HTML or markdown block markers such as ```json or ```. Each object must contain exactly:
            - "question": the essay question text
            - "suggestedAnswer": a bulleted outline or description of the expected correct response in British English with Nigerian context examples where applicable.
        """.trimIndent()

        val raw = ProviderRouter.call(prompt)
        if (raw.isBlank()) {
            Log.w(TAG, "All providers failed for theory. Using fallback.")
            return@withContext getLocalFallbackTheory(subject, gradeClass, topic, count)
        }

        val cleaned = cleanJsonString(raw)
        if (isValidJsonArray(cleaned)) {
            cleaned
        } else {
            getLocalFallbackTheory(subject, gradeClass, topic, count)
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
        val themeLine = if (customInstructions.contains("Theme", ignoreCase = true)) customInstructions else ""
        return """
            LESSON NOTE (Offline Generation)
            ==============================================
            Subject: $subject
            Class: $gradeClass
            Topic: $topic
            Duration: $duration
            $themeLine

            INSTRUCTIONAL MATERIALS
            1. Wall chart showing key concepts of $topic
            2. Relevant photographs and diagrams
            3. Whiteboard and markers
            4. Textbook references

            REFERENCE BOOKS
            Standard $subject Textbook for $gradeClass (Ref:Textbook)

            BEHAVIOURAL OBJECTIVES
            By the end of the lesson, students should be able to:
            - Define $topic in their own words
            - Identify key aspects or components related to the unit
            ${if (syllabusObjectives.isNotEmpty()) "- Conform to syllabus objectives: $syllabusObjectives" else "- Apply the knowledge to practical Nigerian scenarios"}
            - Answer evaluation questions correctly

            PREVIOUS KNOWLEDGE
            Students have foundational knowledge of related concepts from previous terms and can connect them to everyday Nigerian experiences.

            INTRODUCTION
            The teacher begins by asking students what they already know about $topic and how it relates to their daily lives in Nigeria. This creates a bridge from familiar experiences to the new concept.

            PRESENTATION (STEPS)
            Step 1
            Teacher's Activity: Introduce $topic with a clear definition and real-world Nigerian examples.
            Student's Activity: Students listen and take notes.
            
            Step 2
            Teacher's Activity: Explain the key components using the instructional materials.
            Student's Activity: Students observe and ask questions.
            
            Step 3
            Teacher's Activity: Guide students through practical examples and local applications.
            Student's Activity: Students participate in discussion and group work.
            
            Step 4
            Teacher's Activity: Summarise key points and assess understanding.
            Student's Activity: Students answer oral review questions.

            CONTENT
            DEFINITION
            $topic is an important concept in $subject at the $gradeClass level. It covers fundamental principles that students need to understand for academic progression.

            KEY CONCEPTS
            $syllabusContent

            IMPORTANCE
            Understanding $topic helps students relate classroom knowledge to real-world situations in Nigeria and prepares them for higher-level study.

            EVALUATION
            1. What is $topic?
            2. List three key points about $topic.
            3. How does $topic apply to everyday life in Nigeria?

            SUMMARY
            The teacher summarises the lesson by reviewing the definition and key points of $topic, emphasising its relevance to the Nigerian context.

            ASSIGNMENT
            1. Write a short note on $topic and give two Nigerian examples.
            2. Read ahead on the next topic in your textbook.

            NOTE: $customInstructions
        """.trimIndent()
    }

    private fun getLocalFallbackMCQs(subject: String, gradeClass: String, topic: String, count: Int): String {
        val formats = listOf(
            "Fill in the blank: The key property of $topic in $subject is _____.",
            "Which of the following is the MOST SUITABLE example of $topic in $subject?",
            "An incomplete statement about $topic: $topic is a concept that relates to _____.",
            "Which of the following is the ODD ONE OUT when discussing $topic in $subject?",
            "Arrange the following aspects of $topic in the correct sequence:"
        )
        val fallbackOptions = listOf("Definition and scope", "Practical applications in Nigeria", "Historical development", "Related concepts")
        val arr = JSONArray()
        for (i in 1..count) {
            val obj = JSONObject()
            val fmt = formats[(i - 1) % formats.size]
            obj.put("question", "Question $i: $fmt")
            obj.put("optionA", fallbackOptions[0])
            obj.put("optionB", fallbackOptions[1])
            obj.put("optionC", fallbackOptions[2])
            obj.put("optionD", fallbackOptions[3])
            obj.put("correctAnswer", fallbackOptions[0])
            obj.put("explanation", "This is the correct answer because it best describes $topic in $subject.")
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
