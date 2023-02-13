package com.fourdevsociety.imageeditor.utils

import android.graphics.Bitmap
import android.graphics.Color
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
    fun blackAndWhiteFilter(bitmap: Bitmap, image: ShapeableImageView, isUnique: Boolean = false)
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

    fun grayFilter(bitmap: Bitmap, image: ShapeableImageView, isUnique: Boolean = false)
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
                if(generationMethod != GenerationMethod.GENERATING_GRAY && !isUnique) return@launch

                val temp = (Color.red(pixels[i]) + Color.green(pixels[i]) + Color.blue(pixels[i])) / 3
                pixels[i] = Color.rgb(temp, temp, temp)
                updatedRows++
                if(updatedRows % updateThreshold == 0)
                {
                    bitmap.setPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
                    updateImage(image, bitmap)
                }
            }
        }
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