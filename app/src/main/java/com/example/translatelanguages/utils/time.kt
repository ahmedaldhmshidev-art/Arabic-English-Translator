package com.example.translatelanguages.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// تنسيق الوقت
fun formatTime(time: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(time))
}