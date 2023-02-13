package com.fourdevsociety.imageeditor.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Adapter
import android.widget.ImageButton
import androidx.recyclerview.widget.RecyclerView
import com.fourdevsociety.imageeditor.R
import com.fourdevsociety.imageeditor.model.AccumulativeFilterButton
import com.fourdevsociety.imageeditor.model.SelectButtonAccumulativeFilterListener

class AccumulativeFilterButtonAdapter(private val bList: List<AccumulativeFilterButton>,
                                      private val listener: SelectButtonAccumulativeFilterListener)
    : RecyclerView.Adapter<AccumulativeFilterButtonAdapter.ViewHolder>()
{

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.accumulative_filter_button_view, parent, false)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val buttonViewModel = bList[position]
        holder.imageButton.setImageResource(buttonViewModel.image)
        holder.itemView.alpha = buttonViewModel.opacity
        holder.imageButton.setOnClickListener {
            if(selectedPos != position)
            {
                bList[selectedPos].opacity = 0.3f
                bList[position].opacity = 1f
                notifyItemChanged(position, bList[position])
                notifyItemChanged(selectedPos, bList[selectedPos])
                selectedPos = position
                listener.onItemClicked(buttonViewModel)
            }
        }
    }

    override fun getItemCount(): Int = bList.size

    fun getItem(position: Int) : AccumulativeFilterButton = bList[position]

    class ViewHolder(itemView : View) : RecyclerView.ViewHolder(itemView)
    {
        val imageButton : ImageButton = itemView.findViewById(R.id.ib_accumulative_btn_image)
    }
    var selectedPos: Int = Adapter.NO_SELECTION
}