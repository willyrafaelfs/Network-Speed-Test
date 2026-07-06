package com.example.networkspeedtest

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Entry point aplikasi untuk Hilt. Anotasi @HiltAndroidApp memicu code generation
 * Hilt dan membuat container DI tingkat Application yang menjadi induk semua
 * container lain (Activity, ViewModel, dll).
 */
@HiltAndroidApp
class NetworkSpeedTestApp : Application()
