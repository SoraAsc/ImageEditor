package com.fourdevsociety.imageeditor.ui

import android.content.res.Resources
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Adapter
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.fourdevsociety.imageeditor.R
import com.fourdevsociety.imageeditor.model.ImageFilterButton
import com.fourdevsociety.imageeditor.model.SelectButtonFilterListener
import com.google.android.material.imageview.ShapeableImageView


class FilterButtonAdapter(private val bList: List<ImageFilterButton>,
                          private val listener: SelectButtonFilterListener)
    : RecyclerView.Adapter<FilterButtonAdapter.ViewHolder>()
{
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.button_filter_view, parent, false)
        r = parent.context.resources
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val buttonViewModel = bList[position]
        holder.text.text = buttonViewModel.name
        holder.llMain.alpha = buttonViewModel.imageOpacity
        holder.imageButton.strokeWidth = buttonViewModel.strokeWidth
        if(buttonViewModel.imageRefresh) listener.onInitialize(buttonViewModel, holder.imageButton)
        buttonViewModel.imageRefresh = true
        holder.imageButton.setOnClickListener {
            if(selectedPos != position)
            {
                bList[selectedPos].imageRefresh = false
                bList[selectedPos].imageOpacity = 0.3f
                bList[selectedPos].strokeWidth = 0f

                bList[position].imageRefresh = false
                bList[position].imageOpacity = 1f
                bList[position].strokeWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                    2F, r.displayMetrics)

                val pPos = selectedPos
                selectedPos = position
                notifyItemChanged(position, bList[position])
                notifyItemChanged(pPos, bList[pPos])

                listener.onItemClicked(buttonViewModel)
            }
        }
    }

    fun resetButtonProps()
    {
        selectedPos = 2
        for (i in bList.indices)
        {
            bList[i].imageRefresh = true
            if(i == selectedPos)
            {
                bList[i].strokeWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                    2F, r.displayMetrics)
                bList[i].imageOpacity = 1f
            }
            else
            {
                bList[i].strokeWidth = 0f
                bList[i].imageOpacity = 0.3f
            }
        }

    }

    private lateinit var r : Resources
    override fun getItemCount(): Int = bList.size
    class ViewHolder(itemView : View) : RecyclerView.ViewHolder(itemView)
    {
        val llMain : LinearLayout = itemView.findViewById(R.id.ll_main)
        val text : TextView = itemView.findViewById(R.id.tv_title)
        val imageButton : ShapeableImageView = itemView.findViewById(R.id.siv_button)
    }
    var selectedPos: Int = Adapter.NO_SELECTION
}