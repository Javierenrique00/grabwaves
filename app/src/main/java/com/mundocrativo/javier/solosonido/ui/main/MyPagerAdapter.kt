package com.mundocrativo.javier.solosonido.ui.main

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.mundocrativo.javier.solosonido.ui.historia.HistoriaFragment
import com.mundocrativo.javier.solosonido.ui.search.SearchFragment

class MyPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int {
        return 2
    }

    override fun createFragment(position: Int): Fragment {
        val fragment = when(position){
            0 -> HistoriaFragment()
            1 -> SearchFragment()
            else -> HistoriaFragment()
        }
        return fragment
    }
}