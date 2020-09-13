package com.mundocrativo.javier.solosonido.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import com.mundocrativo.javier.solosonido.R
import com.mundocrativo.javier.solosonido.ui.config.ConfigActivity
import kotlinx.android.synthetic.main.main_fragment.*
import kotlinx.android.synthetic.main.main_fragment.view.*

class MainFragment : Fragment() {

    companion object {
        fun newInstance() = MainFragment()
    }

    private lateinit var myPagerAdapter: MyPagerAdapter
    private lateinit var viewPager: ViewPager2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }


    //----  https://developer.android.com/guide/navigation/navigation-swipe-view-2
    //----  Explicacion de como usar el tablayout

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val view = inflater.inflate(R.layout.main_fragment, container, false)

        //--setup the action bar !  Para el menu
        (activity as AppCompatActivity).setSupportActionBar(view.app_bar2)

        return view
    }

    //--- Para el menu
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.principal_menu,menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.menuLogin ->{
                launchConfigActivity()
            }
        }
        return super.onOptionsItemSelected(item)
    }


    //--- esta función no está por defecto y es la que implemeta toda la funcionalidad del paged adapter
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        myPagerAdapter = MyPagerAdapter(this)
        viewPager = pager
        viewPager.adapter = myPagerAdapter

        TabLayoutMediator(tab_layout,viewPager){ tab, position ->
            tab.text = when(position){
                0 -> getString(R.string.tab0)
                1 -> getString(R.string.tab1)
                else -> getString(R.string.tab0)
            }
        }.attach()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        //viewModel = ViewModelProviders.of(this).get(MainViewModel::class.java)

    }

    fun launchConfigActivity(){
        val valor = "datos"
        val intent = Intent(context, ConfigActivity::class.java ).apply {
            putExtra("mensaje",valor)
        }
        startActivity(intent)
    }

}