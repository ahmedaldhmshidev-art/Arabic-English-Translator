package com.example.translatelanguages

import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.translatelanguages.ui.adapter.AdapterTranslate
import com.example.translatelanguages.ui.adapter.SelectState
import com.example.translatelanguages.databinding.FragmentShowTranslatedBinding
import com.example.translatelanguages.ui.state.StateTranslate
import com.example.translatelanguages.ui.viewModel.TranslateViewModelFactory
import com.example.translatelanguages.ui.viewModel.ViewModelTranslate
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.mlkit.nl.translate.TranslateLanguage
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

class ShowTranslated : Fragment() {
    // متغيرات عامة
    private lateinit var binding: FragmentShowTranslatedBinding
    private lateinit var viewModel: ViewModelTranslate
    private lateinit var textToSpeech: TextToSpeech
    private lateinit var adapter: AdapterTranslate

    // Job = مهمة (نستخدمها لإلغاء الترجمة السابقة عند الكتابة السريعة)
    private var typingJob: Job? = null

    // اللغات الحالية (نستخدمها للنطق)
    private var fromLanguage :String = TranslateLanguage.ENGLISH
    private var toLanguage :String   = TranslateLanguage.ARABIC
    // onCreateView = بداية إنشاء الشاشة
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        // ربط الواجهة بالكود (ViewBinding)
        binding = FragmentShowTranslatedBinding.inflate(inflater, container, false)
        // إنشاء ViewModel باستخدام Factory
        val factory = TranslateViewModelFactory(requireContext())
        viewModel = ViewModelProvider(this, factory)[ViewModelTranslate::class.java]
        //  تهيئة محرك الصوت
        textToSpeech = TextToSpeech(requireContext()) { status ->
            if (status != TextToSpeech.SUCCESS) {
                Toast.makeText(requireContext(), getString(R.string.messageFailure), Toast.LENGTH_LONG).show()
            }
        }

        // إعداد RecyclerView + Adapter (سجل الترجمات)
        adapter = AdapterTranslate { item , action ->
            when(action){
                SelectState.Delete ->{
                    // هذا الكود ينفذ عند الضغط المطول على عنصر
                    // نحذف العنصر من قاعدة البيانات عبر ViewModel
                    viewModel.deleteById(item.id)
                    Toast.makeText(requireContext(), getString(R.string.messageDoDeletedRecord), Toast.LENGTH_SHORT).show()
                }
                SelectState.Data -> {
                    binding.etTranslatedTextIdFragShow.setText(item.originalTextEntity)
                }

            }

        }
        // ربط الـ Adapter بالـ RecyclerView
        binding.rvHistory.layoutManager = LinearLayoutManager(requireContext())
        binding.rvHistory.adapter = adapter

        //  مراقبة البيانات من Room

        // أي تغيير في قاعدة البيانات → يحدث تلقائي هنا
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.getAllTranslations().collect { list ->
                adapter.submitList(list)
            }
        }
        //  زر نطق النص الأصلي
        binding.btnSpeakInputIdFragShow.setOnClickListener {
            val input = binding.etTranslatedTextIdFragShow.text.toString().trim()

            if (input.isEmpty()) {
                Toast.makeText(requireContext(), getString(R.string.messageNoFundText), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // نحدد اللغة حسب اللغة الحالية
            val locale = when(fromLanguage){
                TranslateLanguage.ARABIC -> Locale("ar")
                TranslateLanguage.CHINESE -> Locale.US
                else -> Locale.US
            }

            // نمرر اللغة لمحرك الصوت
            textToSpeech.language = locale
            // ننطق النص
            textToSpeech.speak(input, TextToSpeech.QUEUE_FLUSH, null, null)
        }
        //  زر نطق النص المترجم
        binding.btnSpeakOutputIdFragShow.setOnClickListener {

            val text = binding.tvShowTranslatedTextIdFragShow.text.toString().trim()

            if (text.isEmpty()) {
                Toast.makeText(requireContext(), getString(R.string.messageNoFundText), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val locale = when(toLanguage) {
                TranslateLanguage.ARABIC -> Locale("ar")

                TranslateLanguage.ENGLISH -> Locale.US

                else -> Locale.US
            }
            textToSpeech.language = locale
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        }
        // الترجمة التلقائية عند الكتابة
        binding.etTranslatedTextIdFragShow.addTextChangedListener { text ->
            // نلغي المهمة السابقة (لو المستخدم يكتب بسرعة)
            typingJob?.cancel()
            // نبدأ مهمة جديدة
            typingJob = viewLifecycleOwner.lifecycleScope.launch {
                // ننتظر 0.5 ثانية بعد آخر حرف
                delay(500)
                val input = text.toString().trim()
                if (input.isNotEmpty()) {
                    // نطلب من ViewModel الترجمة
                    viewModel.translate(input)
                }
            }
        }

        //  زر تبديل اللغات
        binding.btnSwapFragShow.setOnClickListener {
            // نبدل في ViewModel
            viewModel.swapLanguage()
            // نبدل محليًا للنطق
            // نعيد الترجمة إن وجد نص
            val text = binding.etTranslatedTextIdFragShow.text.toString()
            if (text.isNotEmpty()) {
                viewModel.translate(text)
            }
            Toast.makeText(requireContext(), getString(R.string.imgChangeLanguage), Toast.LENGTH_SHORT).show()
        }


        // عند الضغط عل حذف  السجل
        binding.btnClearHistory.setOnClickListener {
            // dialog message
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.dialog_delete_history_title)
                .setMessage(R.string.dialog_delete_history_message)
                .setPositiveButton(R.string.dialog_delete_history_yes){
                    _ , _ ->
                    viewModel.clearHistory()
                    Toast.makeText(requireContext(), getString(R.string.messageDeletedRecord), Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton(R.string.dialog_delete_history_no,null)
                .show()
        }
        //  مراقبة حالة الترجمة من ViewModel
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.state.collect { state ->
                when (state) {
                    // أثناء التحميل
                    is StateTranslate.Loading -> {
                        binding.tvShowTranslatedTextIdFragShow.text = getString(R.string.messageTranslating)
                    }
                    // عند النجاح
                    is StateTranslate.Success -> {
                        binding.tvShowTranslatedTextIdFragShow.text = state.translatedText
                    }
                    // عند الخطأ
                    is StateTranslate.Error -> {
                        binding.tvShowTranslatedTextIdFragShow.text =   " ${state.message}"
                    }
                    // عند تغيير اللغة
                    is StateTranslate.LanguageChanged -> {
                        // تحديث المتغيرات المحلية للنطق
                        fromLanguage = state.from
                        toLanguage = state.to
                        binding.tvFromIdFragShow.text = languageLabel(fromLanguage)
                        binding.tvToIdFragShow.text = languageLabel(toLanguage)
                    }
                    else -> {}
                }
            }
        }
        return binding.root
    }
    // تحويل كود اللغة إلى اسم
    private fun languageLabel(lang: String): String {
        return when (lang) {
            TranslateLanguage.ENGLISH -> getString(R.string.tvFromLanguage)
            TranslateLanguage.ARABIC -> getString(R.string.tvToLanguage)
            else -> lang
        }
    }
    // إيقاف محرك الصوت عند الخروج

    override fun onDestroyView() {
        super.onDestroyView()
        textToSpeech.stop()
        textToSpeech.shutdown()
    }
}
