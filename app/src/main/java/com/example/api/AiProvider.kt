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

sealed class ProviderResult {
    data class Success(val response: String) : ProviderResult()
    data class Failure(val reason: String, val retryable: Boolean) : ProviderResult()
}

data class ProviderConfig(
    val name: String,
    val baseUrl: String,
    val apiKey: String,
    val model: String,
    val apiFormat: ApiFormat
)

enum class ApiFormat {
    OPENAI_CHAT,
    GEMINI_CONTENT
}

object ProviderRouter {
    private const val TAG = "ProviderRouter"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    private fun getConfigs(): List<ProviderConfig> {
        val configs = mutableListOf<ProviderConfig>()

        val openRouterKey = try { BuildConfig.OPENROUTER_API_KEY } catch (_: Exception) { "" }
        val openRouterModel = try { BuildConfig.OPENROUTER_MODEL } catch (_: Exception) { "openai/gpt-4o" }
        if (openRouterKey.isNotBlank()) {
            configs.add(ProviderConfig("OpenRouter", "https://openrouter.ai/api/v1", openRouterKey, openRouterModel, ApiFormat.OPENAI_CHAT))
        }

        val openModelKey = try { BuildConfig.OPENMODEL_API_KEY } catch (_: Exception) { "" }
        val openModelModel = try { BuildConfig.OPENMODEL_MODEL } catch (_: Exception) { "gpt-4o" }
        if (openModelKey.isNotBlank()) {
            configs.add(ProviderConfig("OpenModel", "https://api.openmodel.ai/v1", openModelKey, openModelModel, ApiFormat.OPENAI_CHAT))
        }

        val geminiKey = try { BuildConfig.GEMINI_API_KEY } catch (_: Exception) { "" }
        if (geminiKey.isNotBlank() && geminiKey != "MY_GEMINI_API_KEY") {
            configs.add(ProviderConfig("Gemini", "https://generativelanguage.googleapis.com/v1beta", geminiKey, "gemini-2.5-flash", ApiFormat.GEMINI_CONTENT))
        }

        val mistralKey = try { BuildConfig.MISTRAL_API_KEY } catch (_: Exception) { "" }
        val mistralModel = try { BuildConfig.MISTRAL_MODEL } catch (_: Exception) { "mistral-large-latest" }
        if (mistralKey.isNotBlank()) {
            configs.add(ProviderConfig("Mistral", "https://api.mistral.ai/v1", mistralKey, mistralModel, ApiFormat.OPENAI_CHAT))
        }

        return configs
    }

    suspend fun call(prompt: String): String = withContext(Dispatchers.IO) {
        val configs = getConfigs()
        if (configs.isEmpty()) {
            Log.w(TAG, "No AI providers configured")
            return@withContext ""
        }

        for (config in configs) {
            Log.d(TAG, "Trying provider: ${config.name} (model: ${config.model})")
            val result = callProvider(config, prompt)
            when (result) {
                is ProviderResult.Success -> {
                    Log.i(TAG, "${config.name} succeeded")
                    return@withContext result.response
                }
                is ProviderResult.Failure -> {
                    Log.w(TAG, "${config.name} failed: ${result.reason} (retryable=${result.retryable})")
                }
            }
        }

        Log.e(TAG, "All AI providers failed")
        ""
    }

    private fun callProvider(config: ProviderConfig, prompt: String): ProviderResult {
        return try {
            val response = when (config.apiFormat) {
                ApiFormat.OPENAI_CHAT -> callOpenAICompatible(config, prompt)
                ApiFormat.GEMINI_CONTENT -> callGemini(config, prompt)
            }

            if (response.isBlank()) {
                ProviderResult.Failure("Empty response", true)
            } else {
                ProviderResult.Success(response)
            }
        } catch (e: Exception) {
            val message = e.message ?: e.javaClass.simpleName
            val isRetryable = isRetryableError(e)
            ProviderResult.Failure(message, isRetryable)
        }
    }

    private fun callOpenAICompatible(config: ProviderConfig, prompt: String): String {
        val url = "${config.baseUrl}/chat/completions"

        val bodyJson = JSONObject().apply {
            put("model", config.model)
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", prompt)
                })
            })
            put("max_tokens", 4096)
        }

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer ${config.apiKey}")
            .addHeader("Content-Type", "application/json")
            .post(bodyJson.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()

        client.newCall(request).execute().use { response ->
            val body = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                val statusCode = response.code
                val rateLimited = statusCode == 429 || statusCode == 503
                if (rateLimited) Log.w(TAG, "Rate limited on ${config.name}: $body")
                throw ProviderException("HTTP $statusCode: ${response.message}", retryable = rateLimited)
            }
            val json = JSONObject(body)
            val choices = json.optJSONArray("choices")
            if (choices != null && choices.length() > 0) {
                val message = choices.getJSONObject(0).optJSONObject("message")
                return message?.optString("content", "")?.trim() ?: ""
            }
            return ""
        }
    }

    private fun callGemini(config: ProviderConfig, prompt: String): String {
        val url = "${config.baseUrl}/models/${config.model}:generateContent?key=${config.apiKey}"

        val requestJson = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", prompt)
                        })
                    })
                })
            })
        }

        val request = Request.Builder()
            .url(url)
            .post(requestJson.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()

        client.newCall(request).execute().use { response ->
            val body = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                val rateLimited = response.code == 429 || response.code == 503
                if (rateLimited) Log.w(TAG, "Rate limited on Gemini: $body")
                throw ProviderException("HTTP ${response.code}: ${response.message}", retryable = rateLimited)
            }
            val json = JSONObject(body)
            val candidates = json.optJSONArray("candidates")
            if (candidates != null && candidates.length() > 0) {
                val content = candidates.getJSONObject(0).optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                if (parts != null && parts.length() > 0) {
                    return parts.getJSONObject(0).optString("text", "").trim()
                }
            }
            return ""
        }
    }

    private fun isRetryableError(e: Exception): Boolean {
        return when {
            e is ProviderException -> e.retryable
            e is java.net.SocketTimeoutException -> true
            e is java.net.ConnectException -> true
            e is java.net.UnknownHostException -> true
            else -> false
        }
    }
}

class ProviderException(message: String, val retryable: Boolean) : Exception(message)
