package com.f0x1d.logfox.ui.fragment.filters

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.f0x1d.logfox.R
import com.f0x1d.logfox.adapter.FiltersAdapter
import com.f0x1d.logfox.databinding.FragmentFiltersBinding
import com.f0x1d.logfox.extensions.setClickListenerOn
import com.f0x1d.logfox.extensions.showAreYouSureDialog
import com.f0x1d.logfox.ui.fragment.base.BaseViewModelFragment
import com.f0x1d.logfox.viewmodel.filters.FiltersViewModel
import dagger.hilt.android.AndroidEntryPoint
import dev.chrisbanes.insetter.applyInsetter

@AndroidEntryPoint
class FiltersFragment: BaseViewModelFragment<FiltersViewModel, FragmentFiltersBinding>() {

    override val viewModel by viewModels<FiltersViewModel>()

    private val adapter = FiltersAdapter(click = {
        findNavController().navigate(
            FiltersFragmentDirections.actionFiltersFragmentToEditFilterFragment(it.id)
        )
    }, delete = {
        showAreYouSureDialog {
            viewModel.delete(it)
        }
    }, checked = { userFilter, checked ->
        viewModel.switch(userFilter, checked)
    })

    private val importFiltersLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) {
        viewModel.import(it ?: return@registerForActivityResult)
    }
    private val exportFiltersLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) {
        viewModel.exportAll(it ?: return@registerForActivityResult)
    }

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) = FragmentFiltersBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.addFab.applyInsetter {
            type(navigationBars = true) {
                margin(vertical = true)
            }
        }
        binding.filtersRecycler.applyInsetter {
            type(navigationBars = true) {
                padding(vertical = true)
            }
        }

        binding.toolbar.setNavigationOnClickListener { findNavController().popBackStack() }
        binding.toolbar.inflateMenu(R.menu.filters_menu)
        binding.toolbar.menu.apply {
            setClickListenerOn(R.id.clear_item) {
                showAreYouSureDialog {
                    viewModel.clearAll()
                }
            }
            setClickListenerOn(R.id.import_item) {
                importFiltersLauncher.launch(arrayOf(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) "application/json" else "*/*"))
            }
            setClickListenerOn(R.id.export_all_item) {
                exportFiltersLauncher.launch("filters.json")
            }
        }

        binding.filtersRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.filtersRecycler.adapter = adapter

        binding.addFab.setOnClickListener {
            findNavController().navigate(FiltersFragmentDirections.actionFiltersFragmentToEditFilterFragment())
        }

        viewModel.filters.observe(viewLifecycleOwner) {
            adapter.submitList(it)
        }
    }
}