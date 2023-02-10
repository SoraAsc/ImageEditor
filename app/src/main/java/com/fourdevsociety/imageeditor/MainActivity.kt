package com.fourdevsociety.imageeditor

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.toBitmap
import androidx.recyclerview.widget.LinearLayoutManager
import com.fourdevsociety.imageeditor.databinding.ActivityMainBinding
import com.fourdevsociety.imageeditor.enums.GenerationMethod
import com.fourdevsociety.imageeditor.model.ImageFilterButton
import com.fourdevsociety.imageeditor.model.SelectButtonFilterListener
import com.fourdevsociety.imageeditor.ui.FilterButtonAdapter
import com.google.android.material.imageview.ShapeableImageView
import kotlinx.coroutines.*


class MainActivity : AppCompatActivity(), SelectButtonFilterListener {
    private lateinit var binding: ActivityMainBinding
    private lateinit var originalBitmap: Bitmap

    private var generationMethod: GenerationMethod = GenerationMethod.ORIGINAL
    private val viewModelJob = Job()
    private val uiScope = CoroutineScope(Dispatchers.Default + viewModelJob)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val buttonRecyclerView = binding.rvButtons
        buttonRecyclerView.layoutManager = LinearLayoutManager(this,
            LinearLayoutManager.HORIZONTAL, false)

        val data = ArrayList<ImageFilterButton>()
        data.add(ImageFilterButton("BW", GenerationMethod.GENERATING_BLACK_AND_WHITE, 0.3f))
        data.add(ImageFilterButton("GRAY", GenerationMethod.GENERATING_GRAY, 0.3f))
        data.add(ImageFilterButton("Original", GenerationMethod.ORIGINAL,1.0f))

        val adapter = FilterButtonAdapter(data, this)
        adapter.selectedPos = 2
        buttonRecyclerView.adapter = adapter

        originalBitmap = binding.ivMain.drawable.toBitmap()
    }

    override fun onItemClicked(filterButton: ImageFilterButton)
    {
        generationMethod = filterButton.generationMethod
        val bitmap: Bitmap = originalBitmap.copy(originalBitmap.config, originalBitmap.isMutable)
        when(filterButton.generationMethod)
        {
            GenerationMethod.GENERATING_BLACK_AND_WHITE ->
            {

                //FilterUtils.blackAndWhiteFilter(bitmap, binding.ivMain)
            }
            GenerationMethod.GENERATING_GRAY -> grayFilter()
            else ->{} //FilterUtils.goBackToOriginal(binding.ivMain, originalBitmap)
        }
    }

    private fun grayFilter()
    {
        val bitmap: Bitmap = originalBitmap.copy(originalBitmap.config, originalBitmap.isMutable)
        uiScope.launch(Dispatchers.Default)
        {
            val updateThreshold = 40000
            var updatedRows = 0
            for(col in 0 until bitmap.width)
            {
                for(row in 0 until bitmap.height)
                {
                    if(generationMethod != GenerationMethod.GENERATING_GRAY) return@launch

                    val pixel: Int = bitmap.getPixel(col, row)
                    val temp: Int = (Color.red(pixel) + Color.green(pixel) +Color.blue(pixel)) / 3
                    bitmap.setPixel( col, row, Color.rgb(temp,temp,temp))
                    updatedRows++
                    if(updatedRows % updateThreshold == 0) updateImage(bitmap)
                }
            }
        }
    }

    private fun blackAndWhiteFilter()
    {
        val bitmap: Bitmap = originalBitmap.copy(originalBitmap.config, originalBitmap.isMutable)
        uiScope.launch(Dispatchers.Default)
        {
            val updateThreshold = 40000
            var updatedRows = 0
            for(col in 0 until bitmap.width)
            {
                for(row in 0 until bitmap.height)
                {
                    if(generationMethod != GenerationMethod.GENERATING_BLACK_AND_WHITE) return@launch

                    val pixel: Int = bitmap.getPixel(col, row)
                    if((Color.red(pixel) + Color.green(pixel) + Color.blue(pixel)) / 3 <= 127)
                        bitmap.setPixel(col, row, Color.BLACK)
                    else
                        bitmap.setPixel(col, row, Color.WHITE)
                    updatedRows++
                    if(updatedRows % updateThreshold == 0) updateImage(bitmap)

                }
            }
        }
    }


    private suspend fun updateImage(bitmap: Bitmap)
    {
        withContext(Dispatchers.Main)
        {
            binding.ivMain.setImageBitmap(bitmap)
        }
    }


    private fun getResizedBitmap(image: Bitmap, maxSize: Int): Bitmap {
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

