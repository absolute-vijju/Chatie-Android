package com.developer.vijay.chatie.utils

import android.app.Application

class ChatieApp : Application() {

    override fun onCreate() {
        super.onCreate()
        PrefUtils.init(this)
    }

}