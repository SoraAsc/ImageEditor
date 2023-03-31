package com.fourdevsociety.imageeditor

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.util.TypedValue
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
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
import com.fourdevsociety.imageeditor.utils.ImageUtils.getResizedBitmap
import com.fourdevsociety.imageeditor.utils.ImageUtils.saveToStorage
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.slider.Slider.OnChangeListener
import java.util.*
import kotlin.collections.ArrayList


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

        ActivityCompat.requestPermissions(this,
            arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE), 1)
        ActivityCompat.requestPermissions(this,
            arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE), 1)

        originalBitmap = binding.ivMain.drawable.toBitmap()

        val filterBtnRecyclerView = binding.rvFilterButtons
        val accumulativeFilterBtnRecyclerView = binding.rvAccumulativeFilterButtons

        filterBtnRecyclerView.layoutManager = LinearLayoutManager(this,
            LinearLayoutManager.HORIZONTAL, false)

        accumulativeFilterBtnRecyclerView.layoutManager = LinearLayoutManager(this,
            LinearLayoutManager.HORIZONTAL, false)

        val fData = ArrayList<ImageFilterButton>()
        fData.add(ImageFilterButton("BW", GenerationMethod.GENERATING_B_AND_W,0.3f))
        fData.add(ImageFilterButton("Gray", GenerationMethod.GENERATING_GRAY,0.3f))
        fData.add(ImageFilterButton("Original", GenerationMethod.ORIGINAL,1.0f,
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                2F, resources.displayMetrics)))
        fData.add(ImageFilterButton("Reduce", GenerationMethod.GENERATING_REDUCE_COLOR,0.3f))
        fData.add(ImageFilterButton("Sepia", GenerationMethod.GENERATING_SEPIA,0.3f))
        fData.add(ImageFilterButton("Lark", GenerationMethod.GENERATING_LARK,0.3f))
        fData.add(ImageFilterButton("Red", GenerationMethod.GENERATING_RED,0.3f))
        fData.add(ImageFilterButton("Point", GenerationMethod.GENERATING_POINTILLISM,0.3f))

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

        binding.btnChangeImage.setOnClickListener{
            getContent.launch("image/*")
        }
        binding.btnSaveImage.setOnClickListener{
            val bitmap = binding.ivMain.drawable.toBitmap()
            saveToStorage(bitmap, this)
        }
        binding.ivMain.setOnClickListener{

        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private val getContent = registerForActivityResult(ActivityResultContracts.GetContent()) {
            uri: Uri? ->
        binding.ivMain.setImageURI(uri)
        originalBitmap = binding.ivMain.drawable.toBitmap()
        filterAdapter.notifyDataSetChanged()
        originalBitmap = originalBitmap.copy(originalBitmap.config, true)
    }

    override fun onInitialize(filterButton: ImageFilterButton, image: ShapeableImageView)
    {
        val bitmap = getResizedBitmap(originalBitmap)
        when(filterButton.generationMethod)
        {
            GenerationMethod.ORIGINAL ->
                FilterUtils.goBackToOriginal(bitmap, image)
            else ->
                FilterUtils.filterHandle(bitmap, getResizedBitmap(bitmap), image,
                    0f, 1f, 0f, 4,
                    this, filterButton.generationMethod, true)
        }

    }
    override fun onItemClicked(filterButton: ImageFilterButton)
    {
        val bitmap: Bitmap = originalBitmap.copy(originalBitmap.config, originalBitmap.isMutable)
        when(filterButton.generationMethod)
        {
            GenerationMethod.ORIGINAL ->
                FilterUtils.goBackToOriginal(originalBitmap, binding.ivMain)
            else ->
                FilterUtils.filterHandle(bitmap, getResizedBitmap(bitmap), binding.ivMain,
                    accumulativeFilterAdapter.getItem(0).current / 1f,
                    accumulativeFilterAdapter.getItem(1).current / 255f,
                    accumulativeFilterAdapter.getItem(2).current / 255f,
                    accumulativeFilterAdapter.getItem(3).current,this,
                    filterButton.generationMethod)
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

}

