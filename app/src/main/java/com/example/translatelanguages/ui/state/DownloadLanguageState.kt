package com.example.translatelanguages.ui.state
sealed class DownloadLanguageState {

    object Idle : DownloadLanguageState()

    object Checking : DownloadLanguageState()

    object ModelsAvailable : DownloadLanguageState()

    object ModelsMissing : DownloadLanguageState()

    object Downloading : DownloadLanguageState()

    object Success : DownloadLanguageState()

    data class Error(
        val message: String
    ) : DownloadLanguageState()
}