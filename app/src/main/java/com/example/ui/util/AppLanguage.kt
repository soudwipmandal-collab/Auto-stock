package com.example.ui.util

import androidx.compose.runtime.compositionLocalOf

enum class AppLanguage(val code: String, val label: String, val shortLabel: String) {
    ENGLISH("en", "English", "EN"),
    HINDI("hi", "हिंदी", "HI"),
    BENGALI("bn", "বাংলা", "BN")
}

val LocalAppLanguage = compositionLocalOf { AppLanguage.ENGLISH }
