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
            for(col in 0 until bitmap.width)
            {
                for(row in 0 until bitmap.height)
                {
                    if(generationMethod != GenerationMethod.GENERATING_B_AND_W && !isUnique)
                        return@launch

                    val pixel: Int = bitmap.getPixel(col, row)
                    if((Color.red(pixel) + Color.green(pixel) + Color.blue(pixel)) / 3 <= 127)
                        bitmap.setPixel(col, row, Color.BLACK)
                    else
                        bitmap.setPixel(col, row, Color.WHITE)
                    updatedRows++
                    if(updatedRows % updateThreshold == 0) updateImage(image ,bitmap)

                }
            }
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
            for(col in 0 until bitmap.width)
            {
                for(row in 0 until bitmap.height)
                {
                    if(generationMethod != GenerationMethod.GENERATING_GRAY && !isUnique) return@launch

                    val pixel: Int = bitmap.getPixel(col, row)
                    val temp: Int = (Color.red(pixel) + Color.green(pixel) +Color.blue(pixel)) / 3
                    bitmap.setPixel( col, row, Color.rgb(temp,temp,temp))
                    updatedRows++
                    if(updatedRows % updateThreshold == 0) updateImage(image, bitmap)
                }
            }
            updateImage(image, bitmap)
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