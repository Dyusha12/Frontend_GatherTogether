package com.example.frontend_gathertogether.services

import android.content.Context
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation

import io.ktor.client.HttpClient;
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class ClientProvider(private val context: Context) {

    private val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    fun instance(): HttpClient = HttpClient{
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
            })
        }

        defaultRequest {
            val token = prefs.getString("TOKEN", null)

            if (!token.isNullOrEmpty()) {
                header(HttpHeaders.Authorization, "Bearer $token")
            }

            //contentType(ContentType.Application.Json)
        }
    }

    // Сохранение токена
    fun saveToken(token: String) {
        prefs.edit().putString("TOKEN", token).apply()
    }

    // Получение токена
    fun getToken(): String? {
        return prefs.getString("TOKEN", null)
    }

    // Очистка токена
    fun clearToken() {
        prefs.edit().remove("TOKEN").apply()
    }
}