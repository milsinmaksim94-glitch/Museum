package com.museum.app.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.museum.app.R
import com.museum.app.adapters.GuideAdapter
import com.museum.app.data.MuseumRepository
import kotlinx.coroutines.launch

// Список экскурсоводов
class GuideListFragment : Fragment() {

    private lateinit var adapter: GuideAdapter
    private lateinit var tvEmpty: TextView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.layout_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvEmpty = view.findViewById(R.id.tvEmpty)
        val rv = view.findViewById<RecyclerView>(R.id.rvList)
        rv.layoutManager = LinearLayoutManager(requireContext())
        adapter = GuideAdapter(emptyList(), { guide ->
            val intent = Intent(requireContext(), GuideActivity::class.java)
            intent.putExtra(GuideActivity.EXTRA_ID, guide.id)
            startActivity(intent)
        })
        rv.adapter = adapter
    }

    override fun onResume() {
        super.onResume()
        viewLifecycleOwner.lifecycleScope.launch {
            val guides = MuseumRepository(requireContext()).getGuides()
            adapter.setItems(guides)
            tvEmpty.visibility = if (guides.isEmpty()) View.VISIBLE else View.GONE
        }
    }
}
