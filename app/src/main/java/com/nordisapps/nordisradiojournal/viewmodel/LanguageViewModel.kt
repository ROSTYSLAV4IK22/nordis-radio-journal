package com.nordisapps.nordisradiojournal.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.nordisapps.nordisradiojournal.data.LanguageManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class LanguageViewModel(application: Application) : AndroidViewModel(application) {
    private val _languageFlow = MutableStateFlow(LanguageManager.getLanguage(application))
    val languageFlow: StateFlow<String> = _languageFlow

    fun changeLanguage(langCode: String) {
        _languageFlow.value = langCode
    }
}
