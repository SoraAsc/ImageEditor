package com.fourdevsociety.imageeditor

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.toBitmap
import androidx.recyclerview.widget.LinearLayoutManager
import com.fourdevsociety.imageeditor.databinding.ActivityMainBinding
import com.fourdevsociety.imageeditor.enums.GenerationMethod
import com.fourdevsociety.imageeditor.model.ImageFilterButton
import com.fourdevsociety.imageeditor.model.SelectButtonFilterListener
import com.fourdevsociety.imageeditor.ui.FilterButtonAdapter
import com.fourdevsociety.imageeditor.utils.FilterUtils
import com.google.android.material.imageview.ShapeableImageView


class MainActivity : AppCompatActivity(), SelectButtonFilterListener {
    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: FilterButtonAdapter
    private lateinit var originalBitmap: Bitmap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        originalBitmap = binding.ivMain.drawable.toBitmap()
        val buttonRecyclerView = binding.rvButtons
        buttonRecyclerView.layoutManager = LinearLayoutManager(this,
            LinearLayoutManager.HORIZONTAL, false)

        val data = ArrayList<ImageFilterButton>()
        data.add(ImageFilterButton("BW", GenerationMethod.GENERATING_B_AND_W,0.3f))
        data.add(ImageFilterButton("GRAY", GenerationMethod.GENERATING_GRAY,0.3f))
        data.add(ImageFilterButton("Original", GenerationMethod.ORIGINAL,1.0f))

        adapter = FilterButtonAdapter(data, this)
        adapter.selectedPos = 2
        buttonRecyclerView.adapter = adapter

        binding.ivMain.setOnClickListener{
            getContent.launch("image/*")
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private val getContent = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        binding.ivMain.setImageURI(uri)
        originalBitmap = binding.ivMain.drawable.toBitmap()
        adapter.notifyDataSetChanged()
        originalBitmap = originalBitmap.copy(originalBitmap.config, true)
    }

    override fun onInitialize(filterButton: ImageFilterButton, image: ShapeableImageView)
    {
        val bitmap = getResizedBitmap(originalBitmap)
        when(filterButton.generationMethod)
        {
            GenerationMethod.GENERATING_B_AND_W ->
                FilterUtils.blackAndWhiteFilter(bitmap, image, true)
            GenerationMethod.GENERATING_GRAY ->
                FilterUtils.grayFilter(bitmap, image, true)
            else ->
                FilterUtils.goBackToOriginal(bitmap, image)
        }

    }

    override fun onItemClicked(filterButton: ImageFilterButton)
    {
        val bitmap: Bitmap = originalBitmap.copy(originalBitmap.config, originalBitmap.isMutable)
        when(filterButton.generationMethod)
        {
            GenerationMethod.GENERATING_B_AND_W ->
                FilterUtils.blackAndWhiteFilter(bitmap, binding.ivMain)
            GenerationMethod.GENERATING_GRAY ->
                FilterUtils.grayFilter(bitmap, binding.ivMain)
            else ->
                FilterUtils.goBackToOriginal(originalBitmap, binding.ivMain)
        }
    }

    private fun getResizedBitmap(image: Bitmap, maxSize: Int = 95): Bitmap {
        var width = image.width
        var height = image.height
        val bitmapRatio = width.toFloat() / height.toFloat()
        if (bitmapRatio > 1) {
            width = maxSize
            height = (width / bitmapRatio).toInt()
        } else {
            height = maxSize
            width = (height * bitmapRatio).toInt()
        }
        return Bitmap.createScaledBitmap(image, width, height, true)
    }

}

