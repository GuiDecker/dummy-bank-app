package com.guilherme.honeypot.data

import android.content.Context
import android.util.Log
import org.json.JSONObject
import java.io.File
import java.util.UUID

data class PendingAlert(
    val id: String,
    val timestamp: Long,
    val latitude: Double?,
    val longitude: Double?,
    val photoPath: String?
) {
    fun toJson(): String = JSONObject().apply {
        put("id", id)
        put("timestamp", timestamp)
        put("latitude", latitude ?: JSONObject.NULL)
        put("longitude", longitude ?: JSONObject.NULL)
        put("photoPath", photoPath ?: JSONObject.NULL)
    }.toString()

    companion object {
        private const val DIR_NAME = "pending_alerts"

        fun dir(context: Context): File =
            File(context.filesDir, DIR_NAME).apply { if (!exists()) mkdirs() }

        fun fromJson(raw: String): PendingAlert {
            val o = JSONObject(raw)
            return PendingAlert(
                id = o.getString("id"),
                timestamp = o.getLong("timestamp"),
                latitude = if (o.isNull("latitude")) null else o.getDouble("latitude"),
                longitude = if (o.isNull("longitude")) null else o.getDouble("longitude"),
                photoPath = if (o.isNull("photoPath")) null else o.getString("photoPath")
            )
        }

        /**
         * Persists the alert: moves the photo (if any) into the pending dir
         * and writes the JSON manifest. Returns the saved alert (with updated paths).
         */
        fun persist(
            context: Context,
            timestamp: Long,
            latitude: Double?,
            longitude: Double?,
            photoFile: File?
        ): PendingAlert {
            val id = UUID.randomUUID().toString()
            val targetDir = dir(context)

            val finalPhotoPath = photoFile?.let { src ->
                val dest = File(targetDir, "$id.jpg")
                try {
                    src.copyTo(dest, overwrite = true)
                    src.delete()
                    dest.absolutePath
                } catch (e: Exception) {
                    Log.e("Honeypot", "Failed to move photo: ${e.message}")
                    null
                }
            }

            val alert = PendingAlert(id, timestamp, latitude, longitude, finalPhotoPath)
            File(targetDir, "$id.json").writeText(alert.toJson())
            Log.d("Honeypot", "Pending alert persisted: $id (photo=$finalPhotoPath)")
            return alert
        }

        fun listAll(context: Context): List<PendingAlert> {
            val dir = dir(context)
            return dir.listFiles { f -> f.extension == "json" }
                ?.mapNotNull { f ->
                    try {
                        fromJson(f.readText())
                    } catch (e: Exception) {
                        Log.e("Honeypot", "Failed to read alert ${f.name}: ${e.message}")
                        null
                    }
                }
                ?.sortedBy { it.timestamp }
                ?: emptyList()
        }

        fun delete(context: Context, alert: PendingAlert) {
            val targetDir = dir(context)
            File(targetDir, "${alert.id}.json").delete()
            alert.photoPath?.let { File(it).delete() }
            Log.d("Honeypot", "Alert ${alert.id} deleted")
        }
    }
}
