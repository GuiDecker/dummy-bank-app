package com.guilherme.honeypot.helper

import android.location.Location
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class TelegramNotifier(
    private val token: String,
    private val chatId: String
) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    suspend fun sendAlert(location: Location?, photo: ByteArray?): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val timestamp = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale("pt", "BR")).format(Date())
                val locationText = if (location != null) {
                    val mapsLink = "https://maps.google.com/?q=${location.latitude},${location.longitude}"
                    "Lat: ${location.latitude}\nLon: ${location.longitude}\nMaps: $mapsLink"
                } else {
                    "Localização indisponível"
                }

                val caption = "⚠️ ALERTA HONEYPOT ⚠️\n\n" +
                        "Tentativa de acesso detectada!\n" +
                        "Horário: $timestamp\n\n" +
                        "📍 Localização:\n$locationText"

                if (photo != null) {
                    sendPhoto(photo, caption)
                } else {
                    sendMessage(caption)
                }
            } catch (e: Exception) {
                Log.e("Honeypot", "Telegram send failed: ${e.message}")
                false
            }
        }
    }

    private fun sendPhoto(photo: ByteArray, caption: String): Boolean {
        val url = "https://api.telegram.org/bot$token/sendPhoto"
        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("chat_id", chatId)
            .addFormDataPart("caption", caption)
            .addFormDataPart(
                "photo", "alert.jpg",
                photo.toRequestBody("image/jpeg".toMediaType())
            )
            .build()

        val request = Request.Builder().url(url).post(body).build()
        val response = client.newCall(request).execute()
        Log.d("Honeypot", "Telegram sendPhoto response: ${response.code}")
        return response.isSuccessful
    }

    private fun sendMessage(text: String): Boolean {
        val url = "https://api.telegram.org/bot$token/sendMessage"
        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("chat_id", chatId)
            .addFormDataPart("text", text)
            .build()

        val request = Request.Builder().url(url).post(body).build()
        val response = client.newCall(request).execute()
        Log.d("Honeypot", "Telegram sendMessage response: ${response.code}")
        return response.isSuccessful
    }
}
