package com.example.translatelanguages

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.addCallback
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.translatelanguages.databinding.FragmentRecordTranslationBinding
import com.example.translatelanguages.ui.adapter.AdapterTranslate
import com.example.translatelanguages.ui.adapter.SelectState
import com.example.translatelanguages.ui.viewModel.TranslateViewModelFactory
import com.example.translatelanguages.ui.viewModel.ViewModelTranslate
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class RecordTranslation : Fragment() {

    private var _binding: FragmentRecordTranslationBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: ViewModelTranslate
    private lateinit var adapterTranslate: AdapterTranslate

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentRecordTranslationBinding.inflate(inflater, container, false)
// view model تهيئة
        val factory = TranslateViewModelFactory(requireContext())
        viewModel = ViewModelProvider(this, factory)[ViewModelTranslate::class.java]

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observerData()
        onclickToolbar()
        handleBackPress()
    }

    private fun onclickToolbar() {
        binding.toolbarRecord.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material)
        binding.toolbarRecord.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
        binding.toolbarRecord.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                // deleted all record  // عند الضغط عل حذف  السجل
                R.id.action_delete_history -> {
                    dialogDelete()
                    true
                }

                else -> false
            }
        }
    }

    private fun dialogDelete() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_delete_history_title)
            .setMessage(R.string.dialog_delete_history_message)
            .setPositiveButton(R.string.dialog_delete_history_yes) { _, _ ->
                viewModel.clearHistory()
                Toast.makeText(
                    requireContext(),
                    getString(R.string.messageDeletedRecord),
                    Toast.LENGTH_SHORT
                ).show()
            }
            .setNegativeButton(R.string.dialog_delete_history_no, null)
            .show()
    }

    //  مراقبة البيانات من Room
    // أي تغيير في قاعدة البيانات → يحدث تلقائي هنا
    private fun observerData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.getAllTranslations().collect { list ->
                    adapterTranslate.submitList(list)
                }
            }
        }
    }

    private fun setupRecyclerView() {
        // إعداد RecyclerView + Adapter (سجل الترجمات)
        adapterTranslate = AdapterTranslate { item, action ->
            when (action) {
                SelectState.Delete -> {
                    // هذا الكود ينفذ عند الضغط المطول على عنصر
                    // نحذف العنصر من قاعدة البيانات عبر ViewModel
                    viewModel.deleteById(item.id)
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.messageDoDeletedRecord),
                        Toast.LENGTH_SHORT
                    ).show()
                }

                SelectState.Data -> {
                    // عند الضغط عل كلمة في السجل يتم نقل الي ترجمتها
                    val bundle = Bundle().apply {
                        putString("originalText", item.originalTextEntity)
                        putString("translated", item.translatedTextEntity)
                    }
                    findNavController().navigate(
                        R.id.action_recordTranslation_to_showTranslated,
                        bundle
                    )
                }
            }
        }
        // ربط الـ Adapter بالـ RecyclerView
        binding.rvHistory.layoutManager = LinearLayoutManager(requireContext())
        binding.rvHistory.adapter = adapterTranslate
    }

    private fun handleBackPress() {
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner
        ) {
            val navController = findNavController()
            if (!navController.popBackStack()) {
                isEnabled = false
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}