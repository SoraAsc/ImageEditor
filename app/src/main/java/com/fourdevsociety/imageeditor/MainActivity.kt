package com.fourdevsociety.imageeditor

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.recyclerview.widget.LinearLayoutManager
import com.fourdevsociety.imageeditor.databinding.ActivityMainBinding
import com.fourdevsociety.imageeditor.enums.GenerationMethod
import com.fourdevsociety.imageeditor.model.AdditiveFilterOptions
import com.fourdevsociety.imageeditor.model.ImageFilterButton
import com.fourdevsociety.imageeditor.model.SelectButtonFilterListener
import com.fourdevsociety.imageeditor.ui.FilterButtonAdapter
import com.fourdevsociety.imageeditor.utils.FilterUtils
import com.fourdevsociety.imageeditor.utils.ImageUtils.getResizedBitmap
import com.fourdevsociety.imageeditor.utils.ImageUtils.saveToStorage
import com.google.android.material.imageview.ShapeableImageView
import java.util.*
import kotlin.collections.ArrayList



class MainActivity : AppCompatActivity(), SelectButtonFilterListener
{
    private lateinit var binding: ActivityMainBinding
    private lateinit var filterAdapter: FilterButtonAdapter
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

        filterBtnRecyclerView.layoutManager = LinearLayoutManager(this,
            LinearLayoutManager.HORIZONTAL, false)

        val fData = ArrayList<ImageFilterButton>()
        fData.add(ImageFilterButton("BW", GenerationMethod.GENERATING_B_AND_W,0.3f))
        fData.add(ImageFilterButton("Gray", GenerationMethod.GENERATING_GRAY,0.3f))
        fData.add(ImageFilterButton("Original", GenerationMethod.ORIGINAL,1.0f,
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,2F, resources.displayMetrics)))
        fData.add(ImageFilterButton("Reduce", GenerationMethod.GENERATING_REDUCE_COLOR,0.3f))
        fData.add(ImageFilterButton("Sepia", GenerationMethod.GENERATING_SEPIA,0.3f))
        fData.add(ImageFilterButton("Lark", GenerationMethod.GENERATING_LARK,0.3f))
        fData.add(ImageFilterButton("K Bits", GenerationMethod.GENERATING_BITS,0.3f))

        filterAdapter = FilterButtonAdapter(fData, this)
        filterAdapter.selectedPos = 2
        filterBtnRecyclerView.adapter = filterAdapter

        binding.btnChangeImage.setOnClickListener{
            getContent.launch("image/*")
        }
        binding.btnSaveImage.setOnClickListener{
            val bitmap = binding.ivMain.drawable.toBitmap()
            saveToStorage(bitmap, this)
        }
        configureSlider()
        configureColorSelect()
//        binding.ivMain.setOnClickListener{
//
//        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private val getContent = registerForActivityResult(ActivityResultContracts.GetContent()) {
            uri: Uri? ->
        binding.ivMain.setImageURI(uri)
        changeVisibleStateOf(binding.clColor0, View.GONE)
        changeVisibleStateOf(binding.llSlider0, View.GONE)
        originalBitmap = binding.ivMain.drawable.toBitmap()
        filterAdapter.resetButtonProps()
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
            GenerationMethod.GENERATING_REDUCE_COLOR ->
                FilterUtils.additionalFilterHandle(bitmap, bitmap, image,
                4,this, filterButton.generationMethod, true)
            GenerationMethod.GENERATING_BITS ->
                FilterUtils.additionalFilterHandle(bitmap, bitmap, image,
                    4,this, filterButton.generationMethod, true)
            else ->
                FilterUtils.filterHandle(bitmap, image,
                    this, filterButton.generationMethod, true)
        }
    }
    override fun onItemClicked(filterButton: ImageFilterButton)
    {
        val bitmap: Bitmap = originalBitmap.copy(originalBitmap.config, originalBitmap.isMutable)
        if(filterButton.generationMethod != GenerationMethod.GENERATING_REDUCE_COLOR)
            changeVisibleStateOf(binding.llSlider0, View.GONE)
        if(filterButton.generationMethod != GenerationMethod.GENERATING_BITS)
            changeVisibleStateOf(binding.clColor0, View.GONE)
        when(filterButton.generationMethod)
        {
            GenerationMethod.ORIGINAL ->
                FilterUtils.goBackToOriginal(originalBitmap, binding.ivMain)
            GenerationMethod.GENERATING_REDUCE_COLOR ->
            {
                changeVisibleStateOf(binding.llSlider0, View.VISIBLE)
            }
            GenerationMethod.GENERATING_BITS ->
            {
                changeVisibleStateOf(binding.clColor0, View.VISIBLE)
            }
            else ->
                FilterUtils.filterHandle(bitmap, binding.ivMain,this, filterButton.generationMethod)
        }
    }

    private fun changeVisibleStateOf(v: View, state: Int)
    {
        v.visibility = state
    }

    private fun configureColorSelect()
    {
        binding.sSliderColor0.valueFrom = AdditiveFilterOptions.KBits.min.toFloat()
        binding.sSliderColor0.valueTo = AdditiveFilterOptions.KBits.max.toFloat()
        binding.sSliderColor0.value = AdditiveFilterOptions.KBits.current.toFloat()
        var value = "${AdditiveFilterOptions.KBits.name} + " +
                "${AdditiveFilterOptions.KBits.current}"
        binding.tvSliderColor0.text = value

        binding.sSliderColor0.addOnChangeListener{ slider, fl, _ ->
            value = "${AdditiveFilterOptions.KBits.name} + ${fl.toInt()}"
            binding.tvSliderColor0.text = value
            AdditiveFilterOptions.KBits.current = slider.value.toInt()
        }

        binding.btnSliderColor0.setOnClickListener{
            val bitmap: Bitmap = originalBitmap.copy(originalBitmap.config, originalBitmap.isMutable)

            FilterUtils.additionalFilterHandle(bitmap, getResizedBitmap(bitmap), binding.ivMain,
                AdditiveFilterOptions.KBits.current,this, GenerationMethod.GENERATING_BITS)
        }
    }

    private fun configureSlider()
    {
        binding.sSlider0.valueFrom = AdditiveFilterOptions.ReduceColor.min.toFloat()
        binding.sSlider0.valueTo = AdditiveFilterOptions.ReduceColor.max.toFloat()
        binding.sSlider0.value = AdditiveFilterOptions.ReduceColor.current.toFloat()
        var value = "${AdditiveFilterOptions.ReduceColor.name} + " +
                "${AdditiveFilterOptions.ReduceColor.current}"
        binding.tvSlider0.text = value

        binding.sSlider0.addOnChangeListener{ slider, fl, _ ->
            val bitmap: Bitmap = originalBitmap.copy(originalBitmap.config, originalBitmap.isMutable)
            value = "${AdditiveFilterOptions.ReduceColor.name} + ${fl.toInt()}"
            binding.tvSlider0.text = value
            AdditiveFilterOptions.ReduceColor.current = slider.value.toInt()
            FilterUtils.additionalFilterHandle(bitmap, getResizedBitmap(bitmap), binding.ivMain,
                fl.toInt(),this, GenerationMethod.GENERATING_REDUCE_COLOR)

        }
    }
}

