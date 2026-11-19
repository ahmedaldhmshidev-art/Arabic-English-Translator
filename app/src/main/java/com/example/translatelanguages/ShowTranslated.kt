
package com.example.translatelanguages
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.translatelanguages.databinding.FragmentShowTranslatedBinding
import com.example.translatelanguages.ui.state.DownloadLanguageState
import com.example.translatelanguages.ui.state.StateTranslate
import com.example.translatelanguages.ui.viewModel.DownloadLanguageViewModel
import com.example.translatelanguages.ui.viewModel.DownloadLanguageViewModelFactory
import com.example.translatelanguages.ui.viewModel.TranslateViewModelFactory
import com.example.translatelanguages.ui.viewModel.ViewModelTranslate
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.mlkit.nl.translate.TranslateLanguage
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

class ShowTranslated : Fragment() {

    // ViewBinding
    private var _binding: FragmentShowTranslatedBinding? = null
    private val binding get() = _binding!!

    // ViewModels
    private lateinit var viewModel: ViewModelTranslate
    private lateinit var downloadViewModel: DownloadLanguageViewModel

    // TextToSpeech
    private lateinit var textToSpeech: TextToSpeech

    // الكتابة والترجمة التلقائية
    private var typingJob: Job? = null

    // اللغات الحالية
    private var fromLanguage = TranslateLanguage.ENGLISH
    private var toLanguage = TranslateLanguage.ARABIC

    // آخر نص أدخله المستخدم
    private var pendingText = ""

    // آخر زوج لغات تم التأكد من توفر نماذجه
    private var checkedLanguagePair: Pair<String, String>? = null

    // منع تكرار ظهور Dialog
    private var isDialogShowing = false

    // منع طلب فحص جديد أثناء الفحص أو التنزيل
    private var isCheckingOrDownloading = false


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentShowTranslatedBinding.inflate(
            inflater,
            container,
            false
        )

        // إنشاء ViewModel الترجمة
        val translateFactory = TranslateViewModelFactory(requireContext())

        viewModel = ViewModelProvider(
            this,
            translateFactory
        )[ViewModelTranslate::class.java]

        // إنشاء ViewModel تنزيل النماذج
        val downloadFactory = DownloadLanguageViewModelFactory()

        downloadViewModel = ViewModelProvider(
            this,
            downloadFactory
        )[DownloadLanguageViewModel::class.java]

        return binding.root
    }


    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        // استقبال النصوص القادمة من الشاشة السابقة
        val originalText = arguments?.getString("originalText")
        val translatedText = arguments?.getString("translated")

        binding.etTranslatedTextIdFragShow.setText(originalText)
        binding.tvShowTranslatedTextIdFragShow.text = translatedText

        initializationSpeech()

        observerTranslated()
        observeDownloadState()

        onClickButton()
        toolbarMove()
    }


    // -----------------------------------------
    // Navigation
    // -----------------------------------------

    private fun toolbarMove() {

        binding.toolbar.setOnMenuItemClickListener { item ->

            when (item.itemId) {

                R.id.action_go_record -> {

                    findNavController().navigate(
                        R.id.action_showTranslated_to_recordTranslation
                    )

                    true
                }

                else -> false
            }
        }
    }

    // Text To Speech
    private fun initializationSpeech() {
        textToSpeech = TextToSpeech(requireContext()) { status ->
            if (status != TextToSpeech.SUCCESS) {
                if (isAdded) {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.messageFailure),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun speakInput() {
        val input = binding.etTranslatedTextIdFragShow
            .text.toString().trim()
        if (input.isEmpty()) {
            showMessage(R.string.messageNoFundText)
            return
        }
        val locale = when (fromLanguage) {
            TranslateLanguage.ARABIC -> Locale("ar")
            else -> Locale.US
        }
        textToSpeech.language = locale
        textToSpeech.speak(
            input,
            TextToSpeech.QUEUE_FLUSH,
            null,
            null
        )
    }

    private fun speakOutput() {
        val output = binding.tvShowTranslatedTextIdFragShow
            .text.toString().trim()
        if (output.isEmpty()) {
            showMessage(R.string.messageNoFundText)
            return
        }
        val locale = when (toLanguage) {
            TranslateLanguage.ARABIC -> Locale("ar")
            else -> Locale.US
        }
        textToSpeech.language = locale
        textToSpeech.speak(
            output,
            TextToSpeech.QUEUE_FLUSH,
            null,
            null
        )
    }

    // Buttons and Text Input
    private fun onClickButton() {
        // تبديل اللغات
        binding.btnSwapFragShow.setOnClickListener {

            val text = binding.etTranslatedTextIdFragShow
                .text.toString().trim()
            // تحديث اللغات في ViewModel
            viewModel.swapLanguage()
            // تحديث اللغات المحلية مباشرة
            val temp = fromLanguage
            fromLanguage = toLanguage
            toLanguage = temp
            // إعادة فحص النماذج والترجمة
            if (text.isNotEmpty()) {
                requestTranslation(text)
            }
            showMessage(R.string.imgChangeLanguage)
        }
        // نطق النص الأصلي
        binding.btnSpeakInputIdFragShow.setOnClickListener {
            speakInput()
        }
        // نطق النص المترجم
        binding.btnSpeakOutputIdFragShow.setOnClickListener {
            speakOutput()
        }
        // الترجمة التلقائية عند الكتابة
        binding.etTranslatedTextIdFragShow.addTextChangedListener { editable ->
            typingJob?.cancel()
            typingJob = viewLifecycleOwner.lifecycleScope.launch {
                // انتظار توقف المستخدم عن الكتابة
                delay(1000)

                val input = editable
                    ?.toString()
                    ?.trim()
                    .orEmpty()

                if (input.isNotEmpty()) {
                    requestTranslation(input)
                }
            }
        }
    }

    // Translation Request
    private fun requestTranslation(text: String) {
        if (text.isBlank()) return
        // حفظ آخر نص أدخله المستخدم
        pendingText = text
        val currentPair = Pair(
            fromLanguage,
            toLanguage
        )
        // إذا كان الفحص أو التنزيل جارياً،
        // نحتفظ بآخر نص فقط وننتظر انتهاء العملية
        if (isCheckingOrDownloading || isDialogShowing) {
            return
        }
        // إذا سبق التأكد من توفر النماذج
        // نترجم مباشرة دون فحص جديد
        if (checkedLanguagePair == currentPair) {

            viewModel.translate(pendingText)
            return
        }
        // نبدأ فحص النماذج
        downloadViewModel.checkLanguageModels(
            fromLanguage,
            toLanguage
        )
    }

    // Observe Download State
    private fun observeDownloadState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(
                Lifecycle.State.STARTED
            ) {
                downloadViewModel.downloadState.collect { state ->
                    when (state) {
                        DownloadLanguageState.Idle -> {
                            binding.downloadProgressContainer.visibility =
                                View.GONE
                            binding.btnSwapFragShow.isEnabled = true
                        }

                        DownloadLanguageState.Checking -> {
                            isCheckingOrDownloading = true
                            binding.downloadProgressContainer.visibility =
                                View.VISIBLE
                            binding.tvDownloadStatus.text =
                                getString(R.string.checking_language_models)
                            binding.btnSwapFragShow.isEnabled = false
                        }

                        DownloadLanguageState.ModelsAvailable -> {
                            binding.downloadProgressContainer.visibility =
                                View.GONE
                            // حفظ زوج اللغات الذي تم فحصه
                            checkedLanguagePair = Pair(
                                fromLanguage,
                                toLanguage
                            )
                            isCheckingOrDownloading = false
                            // بدء الترجمة
                            if (pendingText.isNotBlank()) {
                                viewModel.translate(pendingText)
                            }
                            downloadViewModel.resetDownloadState()
                        }

                        DownloadLanguageState.ModelsMissing -> {
                            binding.downloadProgressContainer.visibility =
                                View.GONE
                            isCheckingOrDownloading = false
                            showDownloadDialog()
                        }

                        DownloadLanguageState.Downloading -> {
                            isCheckingOrDownloading = true
                            binding.downloadProgressContainer.visibility =
                                View.VISIBLE
                            binding.tvDownloadStatus.text =
                                getString(R.string.downloading_language_models)
                            binding.btnSwapFragShow.isEnabled = false
                        }

                        DownloadLanguageState.Success -> {
                            binding.downloadProgressContainer.visibility =
                                View.GONE
                            isCheckingOrDownloading = false
                            // النماذج أصبحت متوفرة لهذا الزوج
                            checkedLanguagePair = Pair(
                                fromLanguage,
                                toLanguage
                            )
                            Toast.makeText(
                                requireContext(),
                                getString(R.string.download_completed),
                                Toast.LENGTH_SHORT
                            ).show()

                            // ترجمة آخر نص
                            if (pendingText.isNotBlank()) {
                                viewModel.translate(pendingText)
                            }
                            downloadViewModel.resetDownloadState()
                        }

                        is DownloadLanguageState.Error -> {
                            binding.downloadProgressContainer.visibility =
                                View.GONE
                            isCheckingOrDownloading = false
                            binding.btnSwapFragShow.isEnabled = true
                            Toast.makeText(
                                requireContext(),
                                state.message,
                                Toast.LENGTH_LONG
                            ).show()
                            downloadViewModel.resetDownloadState()
                        }
                    }
                }
            }
        }
    }

    // Download Confirmation Dialog
    private fun showDownloadDialog() {
        if (isDialogShowing) return
        isDialogShowing = true
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.download_language_models)
            .setMessage(R.string.download_confirmation_message)
            .setPositiveButton(R.string.download) { dialog, _ ->
                downloadViewModel.downloadLanguageModel(
                    fromLanguage,
                    toLanguage
                )
                dialog.dismiss()
            }
            .setNegativeButton(R.string.cancel) { dialog, _ ->
                dialog.dismiss()
            }
            .setOnDismissListener {
                isDialogShowing = false
                // إعادة الحالة الابتدائية إذا ألغى المستخدم
                if (
                    downloadViewModel.downloadState.value ==
                    DownloadLanguageState.ModelsMissing
                ) {
                    downloadViewModel.resetDownloadState()
                }
            }
            .show()
    }

    // Observe Translation State
    private fun observerTranslated() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(
                Lifecycle.State.STARTED
            ) {
                viewModel.state.collect { state ->
                    when (state) {
                        is StateTranslate.Loading -> {
                            binding.tvShowTranslatedTextIdFragShow.text =
                                getString(R.string.messageTranslating)
                        }

                        is StateTranslate.Success -> {
                            binding.tvShowTranslatedTextIdFragShow.text =
                                state.translatedText
                        }

                        is StateTranslate.Error -> {
                            binding.tvShowTranslatedTextIdFragShow.text =
                                state.message
                        }

                        is StateTranslate.LanguageChanged -> {
                            fromLanguage = state.from
                            toLanguage = state.to
                            binding.tvFromIdFragShow.text =
                                languageLabel(fromLanguage)
                            binding.tvToIdFragShow.text =
                                languageLabel(toLanguage)
                        }

                        else -> Unit
                    }
                }
            }
        }
    }

    // Language Labels
    private fun languageLabel(lang: String): String {
        return when (lang) {
            TranslateLanguage.ENGLISH ->
                getString(R.string.tvFromLanguage)

            TranslateLanguage.ARABIC ->
                getString(R.string.tvToLanguage)

            else -> lang
        }
    }

    // Helpers
    private fun showMessage(messageId: Int) {
        Toast.makeText(
            requireContext(),
            getString(messageId),
            Toast.LENGTH_SHORT
        ).show()
    }

    // Lifecycle
    override fun onDestroyView() {
        typingJob?.cancel()
        if (::textToSpeech.isInitialized) {
            textToSpeech.stop()
            textToSpeech.shutdown()
        }
        super.onDestroyView()
        _binding = null
    }
}