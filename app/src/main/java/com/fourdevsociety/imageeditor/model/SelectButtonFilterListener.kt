package com.fourdevsociety.imageeditor.model

import com.google.android.material.imageview.ShapeableImageView

interface SelectButtonFilterListener
{
    fun onItemClicked(filterButton : ImageFilterButton)
    fun onInitialize(filterButton: ImageFilterButton, image: ShapeableImageView)

}