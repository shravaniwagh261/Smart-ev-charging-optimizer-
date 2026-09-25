package com.example.data.remote

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

object GeminiService {
    private const val TAG = "GeminiService"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    data class TTSResult(
        val isSuccess: Boolean,
        val base64Audio: String? = null,
        val mimeType: String? = null,
        val errorMessage: String? = null
    )

    data class SearchGroundingResult(
        val isSuccess: Boolean,
        val content: String,
        val sources: List<String> = emptyList(),
        val errorMessage: String? = null
    )

    /**
     * Calls Gemini 3.8 Flash TTS for high-fidelity spoken station briefings.
     */
    suspend fun generateSpeechAudio(textToSpeak: String): TTSResult = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext TTSResult(
                isSuccess = false,
                errorMessage = "Gemini API key is not configured in .env / Secrets panel."
            )
        }

        try {
            val endpoint = "${BASE_URL}gemini-3.8-flash-tts:generateContent?key=$apiKey"

            val jsonBody = JSONObject().apply {
                val contents = JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", textToSpeak)
                            })
                        })
                    })
                }
                put("contents", contents)

                val generationConfig = JSONObject().apply {
                    put("responseModalities", JSONArray().apply { put("AUDIO") })
                    put("speechConfig", JSONObject().apply {
                        put("voiceConfig", JSONObject().apply {
                            put("prebuiltVoiceConfig", JSONObject().apply {
                                put("voiceName", "Kore")
                            })
                        })
                    })
                }
                put("generationConfig", generationConfig)
            }

            val requestBody = jsonBody.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(endpoint)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseString = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                Log.e(TAG, "TTS request failed: ${response.code} - $responseString")
                return@withContext TTSResult(isSuccess = false, errorMessage = "HTTP ${response.code}: $responseString")
            }

            val rootJson = JSONObject(responseString)
            val candidates = rootJson.optJSONArray("candidates")
            val candidate = candidates?.optJSONObject(0)
            val content = candidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")

            for (i in 0 until (parts?.length() ?: 0)) {
                val part = parts?.optJSONObject(i)
                val inlineData = part?.optJSONObject("inlineData")
                if (inlineData != null) {
                    val data = inlineData.optString("data")
                    val mime = inlineData.optString("mimeType", "audio/wav")
                    if (data.isNotBlank()) {
                        return@withContext TTSResult(isSuccess = true, base64Audio = data, mimeType = mime)
                    }
                }
            }

            TTSResult(isSuccess = false, errorMessage = "No audio data found in response")
        } catch (e: Exception) {
            Log.e(TAG, "Error generating TTS audio", e)
            TTSResult(isSuccess = false, errorMessage = e.localizedMessage ?: "Unknown error")
        }
    }

    /**
     * Calls Gemini 3.5 Flash with Google Search Grounding for live electricity tariff and EV specs.
     */
    suspend fun searchWithGrounding(userQuery: String): SearchGroundingResult = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext SearchGroundingResult(
                isSuccess = false,
                content = "Gemini API key is not configured. Live search grounding requires a valid GEMINI_API_KEY.",
                errorMessage = "Missing API key"
            )
        }

        try {
            val endpoint = "${BASE_URL}gemini-3.5-flash:generateContent?key=$apiKey"

            val jsonBody = JSONObject().apply {
                val contents = JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", userQuery)
                            })
                        })
                    })
                }
                put("contents", contents)

                // Add Google Search tool
                val tools = JSONArray().apply {
                    put(JSONObject().apply {
                        put("googleSearch", JSONObject())
                    })
                }
                put("tools", tools)
            }

            val requestBody = jsonBody.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(endpoint)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseString = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                Log.e(TAG, "Search Grounding request failed: ${response.code} - $responseString")
                return@withContext SearchGroundingResult(
                    isSuccess = false,
                    content = "Unable to fetch search results (HTTP ${response.code})",
                    errorMessage = responseString
                )
            }

            val rootJson = JSONObject(responseString)
            val candidate = rootJson.optJSONArray("candidates")?.optJSONObject(0)
            val textBuilder = StringBuilder()
            val parts = candidate?.optJSONObject("content")?.optJSONArray("parts")

            for (i in 0 until (parts?.length() ?: 0)) {
                val part = parts?.optJSONObject(i)
                val text = part?.optString("text")
                if (!text.isNullOrBlank()) {
                    textBuilder.append(text)
                }
            }

            val sources = mutableListOf<String>()
            val groundingMetadata = candidate?.optJSONObject("groundingMetadata")
            val webSearchQueries = groundingMetadata?.optJSONArray("webSearchQueries")
            if (webSearchQueries != null) {
                for (i in 0 until webSearchQueries.length()) {
                    sources.add("Query: " + webSearchQueries.optString(i))
                }
            }
            val groundingChunks = groundingMetadata?.optJSONArray("groundingChunks")
            if (groundingChunks != null) {
                for (i in 0 until groundingChunks.length()) {
                    val web = groundingChunks.optJSONObject(i)?.optJSONObject("web")
                    val title = web?.optString("title")
                    val uri = web?.optString("uri")
                    if (!title.isNullOrBlank()) {
                        sources.add(if (!uri.isNullOrBlank()) "$title ($uri)" else title)
                    }
                }
            }

            SearchGroundingResult(
                isSuccess = true,
                content = textBuilder.toString().ifBlank { "No text content generated." },
                sources = sources
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error in Search Grounding", e)
            SearchGroundingResult(
                isSuccess = false,
                content = "Network or parsing error occurred: ${e.message}",
                errorMessage = e.localizedMessage
            )
        }
    }
}
