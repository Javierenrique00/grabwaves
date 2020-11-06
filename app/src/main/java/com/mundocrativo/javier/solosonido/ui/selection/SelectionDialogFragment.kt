package com.mundocrativo.javier.solosonido.ui.selection

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.mundocrativo.javier.solosonido.R
import kotlinx.android.synthetic.main.selection_dialog_fragment.view.*

class SelectionDialogFragment(val funcSel:(sel:Int)->Unit) : DialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.selection_dialog_fragment,container,false)

        view.queueNewSelBt.setOnClickListener { funcSel(SEL_NEW);dismiss() }
        view.queueAddBt.setOnClickListener {  funcSel(SEL_END);dismiss() }
        view.queueNextBt.setOnClickListener { funcSel(SEL_NEXT);dismiss() }
        view.downloadBt.setOnClickListener { funcSel(SEL_DOWNLOAD);dismiss() }
        view.cancelSelBt.setOnClickListener { dismiss() }
        return view
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

}

const val SEL_NEW = 0
const val SEL_NEXT = 1
const val SEL_END = 2
const val SEL_DOWNLOAD = 3