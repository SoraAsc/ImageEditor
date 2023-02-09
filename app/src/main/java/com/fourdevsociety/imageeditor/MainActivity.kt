package com.fourdevsociety.imageeditor

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.fourdevsociety.imageeditor.databinding.ActivityMainBinding
import com.fourdevsociety.imageeditor.enums.GenerationMethod
import com.fourdevsociety.imageeditor.model.ImageFilterButton
import com.fourdevsociety.imageeditor.model.SelectButtonFilterListener
import com.fourdevsociety.imageeditor.ui.FilterButtonAdapter


class MainActivity : AppCompatActivity(), SelectButtonFilterListener {
    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val buttonRecyclerView = binding.rvButtons
        buttonRecyclerView.layoutManager = LinearLayoutManager(this,
            LinearLayoutManager.HORIZONTAL, false)

        val data = ArrayList<ImageFilterButton>()
        data.add(ImageFilterButton("BW", GenerationMethod.ORIGINAL, 0.3f))
        data.add(ImageFilterButton("Original", GenerationMethod.GENERATING_BLACK_AND_WHITE,1.0f))

        val adapter = FilterButtonAdapter(data, this)
        adapter.selectedPos = 1
        buttonRecyclerView.adapter = adapter

    }

    override fun onItemClicked(filterButton: ImageFilterButton)
    {
        when(filterButton.generationMethod)
        {
            GenerationMethod.GENERATING_BLACK_AND_WHITE -> blackAndWhite()
            else -> goBackToOriginal()
        }
        //filterButton.imageOpacity = 1f
        //linearLayout.alpha = filterButton.imageOpacity
    }

    private fun blackAndWhite()
    {

    }

    private fun goBackToOriginal()
    {

    }
}