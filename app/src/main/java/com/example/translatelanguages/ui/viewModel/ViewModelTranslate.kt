package com.example.translatelanguages.ui.viewModel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.translatelanguages.ui.state.StateTranslate
import com.example.translatelanguages.data.local.database.EntityTranslate
import com.example.translatelanguages.data.repository.RepositoryTranslate
import com.google.mlkit.nl.translate.TranslateLanguage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ViewModelTranslate(private val repo: RepositoryTranslate) : ViewModel() {
    //  اللغات الحالية
    private var fromLang: String = TranslateLanguage.ENGLISH
    private var toLang: String = TranslateLanguage.ARABIC

    //  الحالة الحالية للواجهة
    private val _state = MutableStateFlow<StateTranslate>(StateTranslate.Idle)
    val state: StateFlow<StateTranslate> = _state.asStateFlow()

    //  init: تحميل آخر لغتين محفوظتين من DataStore
    init {
        viewModelScope.launch {
            val (from, to) = repo.getSaveLanguageRepo()
            if (from != null && to != null) {
                fromLang = from
                toLang = to
            }
            // إعلام الواجهة بأن اللغات الحالية تم تغييرها/تحميلها
            _state.value = StateTranslate.LanguageChanged(
                from = fromLang,
                to = toLang
            )
        }
    }

    //  دالة ترجمة النص
    fun translate(text: String) {
        // التحقق من النص
        if (text.isBlank()) {
            _state.value = StateTranslate.Error("النص فارغ، ادخل نصًا لترجمته")
            return
        }

        // إعلام الواجهة أن الترجمة جاري تنفيذها
        _state.value = StateTranslate.Loading

        // تنفيذ الترجمة بشكل غير متزامن
        viewModelScope.launch {
            val result = repo.translate(text, fromLang, toLang)

            // التعامل مع النتيجة
            result.onSuccess { translatedText ->
                // تحديث الحالة بالنجاح
                _state.value = StateTranslate.Success(
                    translatedText = translatedText,
                    from = fromLang,
                    to = toLang
                )
            }
            result.onFailure { exception ->
                // تحديث الحالة بالخطأ
                _state.value = StateTranslate.Error(
                    message = exception.localizedMessage ?: "حدث خطأ غير معروف"
                )
            }
        }
    }

    //  تبديل اللغات
    fun swapLanguage() {
        val temp = fromLang
        fromLang = toLang
        toLang = temp

        // حفظ آخر لغتين في DataStore
        viewModelScope.launch {
            repo.saveRepo(fromLang, toLang)
        }

        // إعلام الواجهة بتغيير اللغات
        _state.value = StateTranslate.LanguageChanged(
            from = fromLang,
            to = toLang
        )
    }

    //  جلب كل الترجمات من Room
    fun getAllTranslations(): Flow<List<EntityTranslate>> {
        return repo.getAllTranslations()
    }

    // حذف ترجمة واحدة حسب ID
    fun deleteById(id: Int) {
        viewModelScope.launch {
            repo.deleteById(id)
        }
    }

    //  مسح كل السجل
    fun clearHistory() {
        viewModelScope.launch {
            repo.clearHistory()
        }
    }
}
class TranslateViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ViewModelTranslate::class.java)) {
            val repository = RepositoryTranslate(context.applicationContext)
            @Suppress("UNCHECKED_CAST")
            return ViewModelTranslate(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
