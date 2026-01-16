package com.ez2bg.anotherthread

import android.os.Build

class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
}

actual fun getPlatform(): Platform = AndroidPlatform()

actual fun developmentBaseUrl(): String = "http://10.0.2.2:8080"