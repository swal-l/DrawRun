package com.orbital.run.logic

import android.content.Context
import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class UpdateInfo(
    val latestVersionCode: Int,
    val latestVersionName: String,
    val downloadUrl: String,
    val releaseNotes: ReleaseNotes
)

data class ReleaseNotes(
    val features: List<String>,
    val fixes: List<String>
)

object UpdateManager {

    // TODO: REPLACE THIS URL WITH YOUR REAL GITHUB RAW URL
    // Example: "https://raw.githubusercontent.com/lomic/orbital-belt/main/version_info.json"
    private const val UPDATE_JSON_URL = "https://raw.githubusercontent.com/swal-l/DrawRun/main/version_info.json"
    
    private val client = OkHttpClient()

    suspend fun checkForUpdate(currentVersionCode: Int): UpdateInfo? {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url(UPDATE_JSON_URL)
                    .build()

                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    Log.e("UpdateManager", "Failed to fetch update info: ${response.code}")
                    return@withContext null
                }

                val jsonStr = response.body?.string() ?: return@withContext null
                val json = JSONObject(jsonStr)
                
                val latestCode = json.optInt("latestVersionCode", 0)
                
                if (latestCode > currentVersionCode) {
                    val latestName = json.optString("latestVersionName", "Unknown")
                    val downloadUrl = json.optString("downloadUrl", "")
                    
                    val notesJson = json.optJSONObject("releaseNotes")
                    val features = mutableListOf<String>()
                    val fixes = mutableListOf<String>()
                    
                    if (notesJson != null) {
                        val featuresArr = notesJson.optJSONArray("features")
                        if (featuresArr != null) {
                            for (i in 0 until featuresArr.length()) {
                                features.add(featuresArr.getString(i))
                            }
                        }
                        
                        val fixesArr = notesJson.optJSONArray("fixes")
                        if (fixesArr != null) {
                            for (i in 0 until fixesArr.length()) {
                                fixes.add(fixesArr.getString(i))
                            }
                        }
                    }
                    
                    return@withContext UpdateInfo(
                        latestVersionCode = latestCode,
                        latestVersionName = latestName,
                        downloadUrl = downloadUrl,
                        releaseNotes = ReleaseNotes(features, fixes)
                    )
                }
                
                return@withContext null
                
            } catch (e: Exception) {
                Log.e("UpdateManager", "Error checking for update", e)
                return@withContext null
            }
        }
    }
}
