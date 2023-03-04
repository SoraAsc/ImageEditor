package com.fourdevsociety.imageeditor

import android.annotation.SuppressLint
import android.content.ContentValues
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
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
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.slider.Slider.OnChangeListener
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
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

        ActivityCompat.requestPermissions(this,
            arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE), 1)
        ActivityCompat.requestPermissions(this,
            arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE), 1)


        onItemClicked(accumulativeFilterAdapter.getItem(0))

        binding.btnChangeImage.setOnClickListener{
            getContent.launch("image/*")
        }
        binding.btnSaveImage.setOnClickListener{
            val bitmap = binding.ivMain.drawable.toBitmap()
            saveToStorage(bitmap)
        }
        binding.ivMain.setOnClickListener{

        }
    }

    private fun saveToStorage(bitmap: Bitmap)
    {
        val imageName = "img_editor${System.currentTimeMillis()}.jpg"
        var fos: OutputStream? = null
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
        {
            this.contentResolver?.also { resolver ->
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, imageName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image.jpg")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                }
                val imageUri: Uri? = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                fos = imageUri?.let {
                    resolver.openOutputStream(it)
                }
            }
        }
        else
        {
            val imagesDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val image = File(imagesDirectory, imageName)
            fos = FileOutputStream(image)
        }

        fos?.use {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
            Toast.makeText(this, "The Image was saved", Toast.LENGTH_SHORT).show()
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
        val bitmap = getResizedBitmap(originalBitmap)
        when(filterButton.generationMethod)
        {
            GenerationMethod.GENERATING_B_AND_W ->
                FilterUtils.filterHandle(bitmap, getResizedBitmap(bitmap), image, 0f, 1f, 0f, 4,
                    this,GenerationMethod.GENERATING_B_AND_W, true)
            GenerationMethod.GENERATING_GRAY ->
                FilterUtils.filterHandle(bitmap, getResizedBitmap(bitmap), image, 0f, 1f, 0f, 4,
                    this,GenerationMethod.GENERATING_GRAY, true)
            GenerationMethod.GENERATING_REDUCE_COLOR ->
                FilterUtils.filterHandle(bitmap, getResizedBitmap(bitmap), image, 0f, 1f, 0f, 4,
                    this,GenerationMethod.GENERATING_REDUCE_COLOR, true)
            else ->
                FilterUtils.goBackToOriginal(bitmap, image)
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
                FilterUtils.filterHandle(bitmap,getResizedBitmap(bitmap), binding.ivMain,
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

    private fun getResizedBitmap(image: Bitmap, maxSize: Int = 200): Bitmap {
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
        return Bitmap.createScaledBitmap(image, width, height,true)
    }
}

