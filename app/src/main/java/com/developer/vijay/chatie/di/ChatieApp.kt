package com.developer.vijay.chatie.di

import android.app.Application
import com.developer.vijay.chatie.BuildConfig
import com.developer.vijay.chatie.utils.PrefUtils
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class ChatieApp : Application() {

    override fun onCreate() {
        super.onCreate()
        PrefUtils.init(this)
        if (BuildConfig.DEBUG)
            Timber.plant(Timber.DebugTree())
    }

}