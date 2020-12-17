package gr.exm.agroxm.ui.field

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.whenCreated
import com.github.mikephil.charting.data.LineData
import gr.exm.agroxm.R
import gr.exm.agroxm.data.Resource
import gr.exm.agroxm.data.Status
import gr.exm.agroxm.databinding.FragmentDeviceBinding
import kotlinx.coroutines.launch
import java.util.*


class DeviceFragment : Fragment() {

    private lateinit var model: DeviceViewModel
    private lateinit var binding: FragmentDeviceBinding
    private lateinit var adapter: LineDataAdapter
    private var fieldId: String? = null

    companion object {
        private const val ARG_FIELD_ID = "field_id"

        fun newInstance(fieldId: String) = DeviceFragment().apply {
            arguments = Bundle().apply { putString(ARG_FIELD_ID, fieldId) }
        }
    }

    init {
        lifecycleScope.launch {
            whenCreated {
                fieldId = arguments?.getString(ARG_FIELD_ID)
                model = ViewModelProvider(this@DeviceFragment).get(DeviceViewModel::class.java)
            }
        }
    }

    private fun fetch(checkedFilterId: Int = binding.filters.checkedChipId) {
        val mode = getMode(checkedFilterId)
        model.fetch(requireNotNull(fieldId), mode)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentDeviceBinding.inflate(inflater, container, false)

        // Setup swipe-to-refresh
        binding.swiperefresh.setOnRefreshListener { fetch() }

        // Setup adapter
        adapter = LineDataAdapter()
        binding.recycler.adapter = adapter

        // Add listener for time window chip clicks
        binding.filters.setOnCheckedChangeListener { _, checkedId -> fetch(checkedId) }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Update UI when new data arrive
        model.data.observe(viewLifecycleOwner, { updateUI(it) })

        // Fetch data
        fetch()
    }

    private fun getMode(selectedFilterId: Int): DeviceViewModel.Mode {
        return when (selectedFilterId) {
            R.id.filter_last_hour -> DeviceViewModel.Mode.LAST_HOUR
            R.id.filter_today -> DeviceViewModel.Mode.TODAY
            R.id.filter_last_week -> DeviceViewModel.Mode.LAST_WEEK
            R.id.filter_last_month -> DeviceViewModel.Mode.LAST_MONTH
            else -> DeviceViewModel.Mode.LAST_HOUR
        }
    }

    private fun updateUI(resource: Resource<List<LineData>>) {
        when (resource.status) {
            Status.SUCCESS -> {
                binding.swiperefresh.isRefreshing = false
                if (!resource.data.isNullOrEmpty()) {
                    adapter.submitList(resource.data)
                    binding.recycler.visibility = View.VISIBLE
                    binding.empty.visibility = View.GONE
                    binding.progress.visibility = View.GONE
                } else {
                    binding.empty.visibility = View.VISIBLE
                    binding.empty.title("No data yet")
                    binding.empty.subtitle(null)
                    binding.empty.listener(null)
                    binding.recycler.visibility = View.GONE
                    binding.progress.visibility = View.GONE
                }
            }
            Status.ERROR -> {
                binding.swiperefresh.isRefreshing = false
                binding.empty.title("Oops! Could not find any data.")
                binding.empty.subtitle(resource.message)
                binding.empty.action("Retry")
                binding.empty.listener { fetch() }
                binding.empty.visibility = View.VISIBLE
                binding.recycler.visibility = View.GONE
                binding.progress.visibility = View.GONE
            }
            Status.LOADING -> {
                if (adapter.itemCount == 0 && !binding.swiperefresh.isRefreshing) {
                    binding.progress.visibility = View.VISIBLE
                } else {
                    binding.swiperefresh.isRefreshing = true
                    binding.progress.visibility = View.GONE
                }
                binding.empty.visibility = View.GONE
            }
        }
    }
}