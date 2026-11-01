package com.waautopilot.app

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

// ============================================
// DATA MODELS
// ============================================
data class Rule(
    val id: String,
    val keyword: String,
    val reply_text: String,
    val is_active: Int = 1,
    val hit_count: Int = 0
)

data class Stats(
    val total_rules: Int,
    val total_messages: Int,
    val total_replies: Int
)

// ============================================
// RETROFIT API
// ============================================
interface RuleApi {
    @GET("api/rules")
    suspend fun getRules(@Header("X-User-Id") userId: String): List<Rule>

    @POST("api/rules")
    suspend fun addRule(
        @Header("X-User-Id") userId: String,
        @Body rule: Rule
    ): Rule

    @DELETE("api/rules/{id}")
    suspend fun deleteRule(
        @Header("X-User-Id") userId: String,
        @Path("id") id: String
    )

    @GET("api/stats")
    suspend fun getStats(@Header("X-User-Id") userId: String): Stats
}

// ============================================
// RETROFIT CLIENT
// ============================================
object RetrofitClient {
    // Ganti dengan URL worker lo setelah deploy
    private const val BASE_URL = "https://wa-autopilot.SUBDOMAIN_LO.workers.dev/"

    private val client = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()

    val api: RuleApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(RuleApi::class.java)
    }
}

// ============================================
// VIEWMODEL
// ============================================
class RuleViewModel : ViewModel() {

    private val _rules = MutableLiveData<List<Rule>>()
    val rules: LiveData<List<Rule>> = _rules

    private val _stats = MutableLiveData<Stats>()
    val stats: LiveData<Stats> = _stats

    fun loadRules(userId: String) {
        viewModelScope.launch {
            try {
                _rules.value = RetrofitClient.api.getRules(userId)
            } catch (e: Exception) {
                // Log error
            }
        }
    }

    fun addRule(userId: String, keyword: String, reply: String) {
        viewModelScope.launch {
            try {
                RetrofitClient.api.addRule(userId, Rule("", keyword, reply))
                loadRules(userId)
                loadStats(userId)
            } catch (e: Exception) {
                // Log error
            }
        }
    }

    fun deleteRule(userId: String, ruleId: String) {
        viewModelScope.launch {
            try {
                RetrofitClient.api.deleteRule(userId, ruleId)
                loadRules(userId)
                loadStats(userId)
            } catch (e: Exception) {
                // Log error
            }
        }
    }

    fun loadStats(userId: String) {
        viewModelScope.launch {
            try {
                _stats.value = RetrofitClient.api.getStats(userId)
            } catch (e: Exception) {
                // Log error
            }
        }
    }
}
