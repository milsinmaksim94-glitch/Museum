package com.museum.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.museum.app.R
import com.museum.app.data.Hall
import com.museum.app.data.MuseumRepository
import kotlinx.coroutines.launch

// Главная: сверху вкладки с залами (TabLayout), снизу листаются списки (ViewPager2)
class HomeFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tabLayout = view.findViewById<TabLayout>(R.id.tabLayout)
        val viewPager = view.findViewById<ViewPager2>(R.id.viewPager)
        val repository = MuseumRepository(requireContext())

        // сначала грузим залы из базы, потом делаем по вкладке на каждый
        viewLifecycleOwner.lifecycleScope.launch {
            val halls = repository.getHalls()

            viewPager.adapter = HallsPagerAdapter(this@HomeFragment, halls)

            TabLayoutMediator(tabLayout, viewPager) { tab, position ->
                if (position == 0) {
                    tab.text = getString(R.string.tab_all)
                } else {
                    tab.text = halls[position - 1].name
                }
            }.attach()
        }
    }

    // Адаптер для страниц. Первая страница - все экспонаты, дальше по залам
    class HallsPagerAdapter(fragment: Fragment, private val halls: List<Hall>) :
        FragmentStateAdapter(fragment) {

        override fun getItemCount(): Int = halls.size + 1

        override fun createFragment(position: Int): Fragment {
            return if (position == 0) {
                ExhibitListFragment.newInstance(0)
            } else {
                ExhibitListFragment.newInstance(halls[position - 1].id)
            }
        }
    }
}
