package com.developer.vijay.chatie.utils

import android.content.Context
import com.android.volley.AuthFailureError
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.gson.Gson
import org.json.JSONObject
import timber.log.Timber

object FirebaseUtils {
    const val CHATS = "chats"
    const val NAME = "name"
    const val PROFILE_IMAGE = "profileImage"
    const val LAST_UPDATED = "lastUpdated"
    const val STORIES = "stories"
    const val USERS = "users"
    const val PROFILES = "Profiles"
    const val MESSAGES = "messages"
    const val LAST_MSG = "lastMsg"
    const val LAST_MSG_TIME = "lastMsgTime"
    const val STATUS = "Status"
    const val STATUSES = "statuses"
    const val DEVICE_TOKEN = "deviceToken"
    const val CHANNEL_ID = "channel_id"
    const val CHANNEL_NAME = "channel_name"
    const val BACKGROUND_IMAGE_URL = "backgroundImageUrl"
    const val BACKGROUND_COLOR = "backgroundColor"
    const val SHOW_BACKGROUND_IMAGE = "showBackgroundImage"
    const val IMAGE = "Image"
    const val PRESENCE = "presence"
    const val PUBLIC = "public"

    fun sendNotification(context: Context, name: String, message: String, token: String) {
        try {
            val queue: RequestQueue = Volley.newRequestQueue(context)
            val url = "https://fcm.googleapis.com/fcm/send"
            val data = JSONObject()
            data.put("title", name)
            data.put("body", message)
            val notificationData = JSONObject()
            notificationData.put("notification", data)
            notificationData.put("to", token)

            Timber.e("Notification: ${Gson().toJson(notificationData)}")

            val request: JsonObjectRequest = object : JsonObjectRequest(
                Method.POST, url, notificationData,
                Response.Listener<JSONObject?> {
                    //
                }, Response.ErrorListener {
                    Timber.e("Notification: ${it.message}")
                }) {
                @Throws(AuthFailureError::class)
                override fun getHeaders(): Map<String, String> {
                    val key = "Key=${Constants.SERVER_CLOUD_MESSAGING_KEY}"
                    var params = hashMapOf<String, String>()
                    params["Content-Type"] = "application/json";
                    params["Authorization"] = key;
                    return params
                }
            }
            queue.add(request)
        } catch (ex: Exception) {
        }
    }
}