package com.mundocrativo.javier.solosonido.ui.player

import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ItemTouchHelper.*
import androidx.recyclerview.widget.RecyclerView

//---https://medium.com/@yfujiki/drag-and-reorder-recyclerview-items-in-a-user-friendly-manner-1282335141e9
val playerItemTouchHelper by lazy {
    // 1. Note that I am specifying all 4 directions.
    //    Specifying START and END also allows
    //    more organic dragging than just specifying UP and DOWN.
    val simpleItemTouchCallback =
        object : ItemTouchHelper.SimpleCallback(UP or DOWN, RIGHT) {  //UP or DOWN or START or END

            override fun onMove(recyclerView: RecyclerView,
                                viewHolder: RecyclerView.ViewHolder,
                                target: RecyclerView.ViewHolder): Boolean {

                val adapter = recyclerView.adapter as VideoPlayerDataAdapter
                val from = viewHolder.adapterPosition
                val to = target.adapterPosition
                // 2. Update the backing model. Custom implementation in
                //    MainRecyclerViewAdapter. You need to implement
                //    reordering of the backing model inside the method.
                adapter.moveItem(from, to)
                // 3. Tell adapter to render the model update.
                adapter.notifyItemMoved(from, to)

                return true
            }
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder,
                                  direction: Int) {
                // 4. Code block for horizontal swipe.
                //    ItemTouchHelper handles horizontal swipe as well, but
                //    it is not relevant with reordering. Ignoring here.
                // Log.v("msg","Swipe to the right") --> todo si hace el swipe to the right
                if(direction== RIGHT){
                    val hv = viewHolder as VideoPlayerDataAdapter.VideoListViewHolder
                    hv.swipeRight(viewHolder.adapterPosition)
                }
            }

            //--- para detectar que se seleccion
            override fun onSelectedChanged(
                viewHolder: RecyclerView.ViewHolder?,
                actionState: Int
            ) {
                super.onSelectedChanged(viewHolder, actionState)

                if(actionState == ACTION_STATE_DRAG){
                    viewHolder?.itemView?.alpha = 0.5f
                }

            }

            override fun clearView(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ) {
                super.clearView(recyclerView, viewHolder)

                viewHolder.itemView.alpha = 1.0f
            }
        }

    ItemTouchHelper(simpleItemTouchCallback)
}
