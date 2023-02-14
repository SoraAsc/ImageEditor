package com.fourdevsociety.imageeditor.utils

import android.graphics.*
import com.fourdevsociety.imageeditor.enums.GenerationMethod
import com.google.android.material.imageview.ShapeableImageView
import kotlinx.coroutines.*


val coroutineExceptionHandler = CoroutineExceptionHandler{_, throwable ->
    throwable.printStackTrace()
}

private val viewModelJob = Job()
private val uiScope = CoroutineScope(Dispatchers.Default + viewModelJob + coroutineExceptionHandler)
private var generationMethod: GenerationMethod = GenerationMethod.ORIGINAL
object FilterUtils
{
    fun blackAndWhiteFilter(bitmap: Bitmap, image: ShapeableImageView,
        brightness: Float, contrast: Float, saturation: Float, isUnique: Boolean = false)
    {
        generationMethod = GenerationMethod.GENERATING_B_AND_W
        uiScope.launch(Dispatchers.Default)
        {
            val updateThreshold = 40000
            var updatedRows = 0
            val pixels = IntArray(bitmap.width * bitmap.height)
            bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
            for(i in pixels.indices)
            {
                if(generationMethod != GenerationMethod.GENERATING_B_AND_W && !isUnique)
                    return@launch

                if((Color.red(pixels[i]) + Color.green(pixels[i]) + Color.blue(pixels[i])) / 3 <= 127)
                    pixels[i] = Color.BLACK
                else
                    pixels[i] = Color.WHITE

                val (red, green, blue) = changeBCS(Color.red(pixels[i]), Color.green(pixels[i]),
                    Color.blue(pixels[i]), brightness, contrast, saturation)
                val temp = (red + green + blue) / 3
                pixels[i] = Color.rgb(temp, temp, temp)

                updatedRows++
                if(updatedRows % updateThreshold == 0)
                {
                    bitmap.setPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
                    updateImage(image, bitmap)
                }
            }
            bitmap.setPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
            updateImage(image, bitmap)
        }
    }

    fun grayFilter(bitmap: Bitmap, image: ShapeableImageView,
        brightness: Float, contrast: Float, saturation: Float, isUnique: Boolean = false)
    {
        generationMethod = GenerationMethod.GENERATING_GRAY
        uiScope.launch(Dispatchers.Default)
        {
            val updateThreshold = 40000
            var updatedRows = 0
            val pixels = IntArray(bitmap.width * bitmap.height)
            bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
            for(i in pixels.indices)
            {
                if(generationMethod != GenerationMethod.GENERATING_GRAY && !isUnique)
                    return@launch

                val temp = (Color.red(pixels[i]) + Color.green(pixels[i]) + Color.blue(pixels[i])) / 3
                val (red, green, blue) = changeBCS(temp, temp, temp, brightness, contrast, saturation)
                pixels[i] = Color.rgb(red, green, blue)

                updatedRows++
                if(updatedRows % updateThreshold == 0)
                {
                    bitmap.setPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
                    updateImage(image, bitmap)
                }
            }
            bitmap.setPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
            updateImage(image, bitmap)
        }
    }

    private fun changeBCS(
        red: Int, green: Int, blue: Int,
        brightness: Float, contrast: Float, saturation: Float)
        : Triple<Int, Int, Int>
    {
        val r = (red + brightness) * contrast
        val g = (green + brightness) * contrast
        val b = (blue + brightness) * contrast
        val intensity = (r + g + b) / 3
        return Triple( (r + (r - intensity) * saturation).toInt().coerceIn(0, 255),
            (g + (g - intensity) * saturation).toInt().coerceIn(0, 255),
            (b + (b - intensity) * saturation).toInt().coerceIn(0, 255))
    }

    private suspend fun updateImage(image: ShapeableImageView, bitmap: Bitmap)
    {
        withContext(Dispatchers.Main)
        {
            image.setImageBitmap(bitmap)
        }
    }

    fun goBackToOriginal(originalBitmap: Bitmap, image: ShapeableImageView)
    {
        generationMethod = GenerationMethod.ORIGINAL
        uiScope.launch(Dispatchers.Main)
        {
            updateImage(image, originalBitmap)
        }

    }

}