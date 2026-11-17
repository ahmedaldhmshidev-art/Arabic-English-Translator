package com.example.translatelanguages.ui.state

//هنا كلاس الحالات
sealed class StateTranslate {
    object Idle : StateTranslate()
    object Loading : StateTranslate()
    data class Success(val translatedText : String , val from: String , val to:String): StateTranslate()
    data class Error(val message : String ) : StateTranslate()
    data class LanguageChanged(val from:String , val to : String): StateTranslate()
}