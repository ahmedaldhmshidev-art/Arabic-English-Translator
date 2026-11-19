package com.example.translatelanguages.ui.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.translatelanguages.data.repository.DownloadLanguageRepository
import com.example.translatelanguages.ui.state.DownloadLanguageState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DownloadLanguageViewModel(
    private val repository: DownloadLanguageRepository
) : ViewModel() {

    private val _downloadState =
        MutableStateFlow<DownloadLanguageState>(DownloadLanguageState.Idle)

    val downloadState: StateFlow<DownloadLanguageState> =
        _downloadState.asStateFlow()


    // فحص توفر نماذج اللغتين
    fun checkLanguageModels(from: String, to: String) {

        if (_downloadState.value == DownloadLanguageState.Checking ||
            _downloadState.value == DownloadLanguageState.Downloading
        ) return

        viewModelScope.launch {

            _downloadState.value = DownloadLanguageState.Checking

            val result = repository.areModelsDownloaded(from, to)

            _downloadState.value = result.fold(

                onSuccess = { isAvailable ->
                    if (isAvailable) {
                        DownloadLanguageState.ModelsAvailable
                    } else {
                        DownloadLanguageState.ModelsMissing
                    }
                },

                onFailure = { exception ->
                    DownloadLanguageState.Error(
                        exception.localizedMessage
                            ?: "حدث خطأ أثناء فحص نماذج اللغات"
                    )
                }
            )
        }
    }


    // تنزيل نماذج اللغات
    fun downloadLanguageModel(from: String, to: String) {

        if (_downloadState.value == DownloadLanguageState.Downloading ||
            _downloadState.value == DownloadLanguageState.Checking
        ) return

        viewModelScope.launch {

            _downloadState.value = DownloadLanguageState.Downloading

            val result = repository.downloadModel(from, to)

            _downloadState.value = result.fold(

                onSuccess = {
                    DownloadLanguageState.Success
                },

                onFailure = { exception ->
                    DownloadLanguageState.Error(
                        exception.localizedMessage
                            ?: "فشل تنزيل نماذج اللغات"
                    )
                }
            )
        }
    }


    // إعادة الحالة إلى الوضع الابتدائي
    fun resetDownloadState() {
        _downloadState.value = DownloadLanguageState.Idle
    }
}


class DownloadLanguageViewModelFactory :
    ViewModelProvider.Factory {
    override fun <T : ViewModel> create(
        modelClass: Class<T>
    ): T {
        if (modelClass.isAssignableFrom(
                DownloadLanguageViewModel::class.java
            )
        ) {
            val repository = DownloadLanguageRepository()
            @Suppress("UNCHECKED_CAST")
            return DownloadLanguageViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}