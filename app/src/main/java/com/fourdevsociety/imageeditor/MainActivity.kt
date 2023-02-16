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
import com.fourdevsociety.imageeditor.model.AccumulativeFilterButton
import com.fourdevsociety.imageeditor.model.ImageFilterButton
import com.fourdevsociety.imageeditor.model.SelectButtonAccumulativeFilterListener
import com.fourdevsociety.imageeditor.model.SelectButtonFilterListener
import com.fourdevsociety.imageeditor.ui.AccumulativeFilterButtonAdapter
import com.fourdevsociety.imageeditor.ui.FilterButtonAdapter
import com.fourdevsociety.imageeditor.utils.FilterUtils
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.slider.Slider.OnChangeListener


class MainActivity : AppCompatActivity(), SelectButtonFilterListener,
    SelectButtonAccumulativeFilterListener
{
    private lateinit var binding: ActivityMainBinding
    private lateinit var filterAdapter: FilterButtonAdapter
    private var sliderChangeListener: OnChangeListener? = null
    private lateinit var accumulativeFilterAdapter: AccumulativeFilterButtonAdapter
    private lateinit var originalBitmap: Bitmap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        originalBitmap = binding.ivMain.drawable.toBitmap()

        val filterBtnRecyclerView = binding.rvFilterButtons
        val accumulativeFilterBtnRecyclerView = binding.rvAccumulativeFilterButtons

        filterBtnRecyclerView.layoutManager = LinearLayoutManager(this,
            LinearLayoutManager.HORIZONTAL, false)

        accumulativeFilterBtnRecyclerView.layoutManager = LinearLayoutManager(this,
            LinearLayoutManager.HORIZONTAL, false)

        val fData = ArrayList<ImageFilterButton>()
        fData.add(ImageFilterButton("BW", GenerationMethod.GENERATING_B_AND_W,0.3f))
        fData.add(ImageFilterButton("GRAY", GenerationMethod.GENERATING_GRAY,0.3f))
        fData.add(ImageFilterButton("Original", GenerationMethod.ORIGINAL,1.0f))
        fData.add(ImageFilterButton("Reduce Color", GenerationMethod.GENERATING_REDUCE_COLOR,0.3f))

        filterAdapter = FilterButtonAdapter(fData, this)
        filterAdapter.selectedPos = 2
        filterBtnRecyclerView.adapter = filterAdapter

        val accumulativeFData = ArrayList<AccumulativeFilterButton>()
        accumulativeFData.add(AccumulativeFilterButton("Brightness", 1.0f,
            R.drawable.brightness, -255, 255, 0))
        accumulativeFData.add(AccumulativeFilterButton("Contrast", 0.3f,
            R.drawable.contrast, 50, 765, 255))
        accumulativeFData.add(AccumulativeFilterButton("Saturation", 0.3f,
            R.drawable.saturation, 0, 255, 0))
        accumulativeFData.add(AccumulativeFilterButton("Reduce Color", 0.3f,
            R.drawable.reduce, 2, 10, 2))

        accumulativeFilterAdapter = AccumulativeFilterButtonAdapter(accumulativeFData, this)
        accumulativeFilterAdapter.selectedPos = 0
        accumulativeFilterBtnRecyclerView.adapter = accumulativeFilterAdapter


        onItemClicked(accumulativeFilterAdapter.getItem(0))

        binding.ivMain.setOnClickListener{
            getContent.launch("image/*")
        }

    }

    @SuppressLint("NotifyDataSetChanged")
    private val getContent = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        binding.ivMain.setImageURI(uri)
        originalBitmap = binding.ivMain.drawable.toBitmap()
        filterAdapter.notifyDataSetChanged()
        originalBitmap = originalBitmap.copy(originalBitmap.config, true)
    }

    override fun onInitialize(filterButton: ImageFilterButton, image: ShapeableImageView)
    {
        val bitmap = getResizedBitmap(originalBitmap, 200)
        when(filterButton.generationMethod)
        {
            GenerationMethod.GENERATING_B_AND_W ->
                FilterUtils.blackAndWhiteFilter(bitmap, image, 0f, 1f, 0f, true)
            GenerationMethod.GENERATING_GRAY ->
                FilterUtils.grayFilter(bitmap, image, 0f, 1f, 0f, true)
            GenerationMethod.GENERATING_REDUCE_COLOR ->
                FilterUtils.reduceColorFilter(
                    bitmap, getResizedBitmap(bitmap, 60), image, 0f, 1f,
                    0f, 4, true)
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
                FilterUtils.blackAndWhiteFilter(bitmap, binding.ivMain,
                    accumulativeFilterAdapter.getItem(0).current / 1f,
                    accumulativeFilterAdapter.getItem(1).current / 255f,
                    accumulativeFilterAdapter.getItem(2).current / 255f)
            GenerationMethod.GENERATING_GRAY ->
                FilterUtils.grayFilter(bitmap, binding.ivMain,
                    accumulativeFilterAdapter.getItem(0).current / 1f,
                    accumulativeFilterAdapter.getItem(1).current / 255f,
                    accumulativeFilterAdapter.getItem(2).current / 255f)
            GenerationMethod.GENERATING_REDUCE_COLOR ->
                FilterUtils.reduceColorFilter(
                    bitmap, getResizedBitmap(bitmap, 60), binding.ivMain,
                    accumulativeFilterAdapter.getItem(0).current / 1f,
                    accumulativeFilterAdapter.getItem(1).current / 255f,
                    accumulativeFilterAdapter.getItem(2).current / 255f,
                    accumulativeFilterAdapter.getItem(3).current)
            else ->
                FilterUtils.goBackToOriginal(originalBitmap, binding.ivMain)
        }
    }
    override fun onItemClicked(accumulativeFilterButton: AccumulativeFilterButton)
    {

        sliderChangeListener?.let { binding.sAccumulativeSlider.removeOnChangeListener(it) }
        binding.sAccumulativeSlider.valueFrom = accumulativeFilterButton.min.toFloat()
        binding.sAccumulativeSlider.valueTo = accumulativeFilterButton.max.toFloat()
        binding.sAccumulativeSlider.value = accumulativeFilterButton.current.toFloat()
        var value = "${accumulativeFilterButton.name} + ${accumulativeFilterButton.current}"
        binding.tvAccumulativeSlider.text = value

        sliderChangeListener = OnChangeListener { slider, fl, _ ->
            value = "${accumulativeFilterButton.name} + ${fl.toInt()}"
            binding.tvAccumulativeSlider.text = value
            accumulativeFilterButton.current = slider.value.toInt()
        }

        binding.sAccumulativeSlider.addOnChangeListener(sliderChangeListener!!)
    }

    private fun getResizedBitmap(image: Bitmap, maxSize: Int = 100): Bitmap {
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

