package com.fourdevsociety.imageeditor.utils

import android.content.Context
import android.graphics.*
import android.widget.Toast
import com.fourdevsociety.imageeditor.enums.GenerationMethod
import com.google.android.material.imageview.ShapeableImageView
import kotlinx.coroutines.*
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt


val coroutineExceptionHandler = CoroutineExceptionHandler{_, throwable ->
    throwable.printStackTrace()
}

private val viewModelJob = Job()
private val uiScope = CoroutineScope(Dispatchers.Default + viewModelJob + coroutineExceptionHandler)
private var generationMethod: GenerationMethod = GenerationMethod.ORIGINAL

private var desiredColors = arrayOf<FilterUtils.PersonalColor>()
object FilterUtils
{
    fun filterHandle(bitmap: Bitmap, dummyBitmap: Bitmap, image: ShapeableImageView, brightness: Float,
                     contrast: Float, saturation: Float, colorsNumber: Int, ctx: Context,
                     currentGenMethod: GenerationMethod, isUnique: Boolean = false)
    {
        generationMethod = currentGenMethod

        var filterFun = ::blackAndWhiteFilter
        when(generationMethod)
        {
            GenerationMethod.GENERATING_B_AND_W -> filterFun = ::blackAndWhiteFilter
            GenerationMethod.GENERATING_GRAY -> filterFun = ::grayFilter
            GenerationMethod.GENERATING_REDUCE_COLOR -> filterFun = ::reduceColorFilter
            else -> {}
        }
        uiScope.launch(Dispatchers.Default)
        {
            val updateThreshold = 80000
            var updatedRows = 0
            val pixels = IntArray(bitmap.width * bitmap.height)
            bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
            if(generationMethod == GenerationMethod.GENERATING_REDUCE_COLOR)
            {
                val dummyPixels = IntArray(dummyBitmap.width * dummyBitmap.height)
                dummyBitmap.getPixels(dummyPixels,0,dummyBitmap.width,0,0,dummyBitmap.width,dummyBitmap.height)
                getRelevantColors(colorsNumber, dummyPixels)
            }

            for(i in pixels.indices)
            {
                if(generationMethod != currentGenMethod && !isUnique)
                    return@launch

                val temp = filterFun( PersonalColor(Color.red(pixels[i]),Color.green(pixels[i]),
                    Color.blue(pixels[i])))

                //val (red, green, blue) = changeBCS(temp, temp, temp, brightness, contrast, saturation)
                pixels[i] = temp

                updatedRows++
                if(updatedRows % updateThreshold == 0)
                {
                    bitmap.setPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
                    updateImage(image, bitmap)
                }
            }
            bitmap.setPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
            updateImage(image, bitmap, if(isUnique) null else ctx)
        }
    }

    private fun blackAndWhiteFilter(c: PersonalColor) : Int
    {
        return if((c.r + c.g + c.b) / 3 <= 127)
            Color.BLACK
        else
            Color.WHITE
    }

    private fun grayFilter(c: PersonalColor) : Int
    {
        val temp = (c.r + c.g + c.b) / 3
        return Color.rgb(temp, temp, temp)
    }

    private fun reduceColorFilter(c: PersonalColor) : Int
    {
        val fitColor = getFitColor(c)
        return Color.rgb(fitColor.r, fitColor.g, fitColor.b)
    }

    private fun getRGBDifference(c1 : PersonalColor, c2: PersonalColor) : Double
    {
        return sqrt((c2.r - c1.r).toDouble().pow(2) +
                (c2.g - c1.g).toDouble().pow(2) + (c2.b - c1.b).toDouble().pow(2))
    }

    private fun getFitColorIndex(fitColors: Array<PersonalColor>, current: PersonalColor): Int
    {
        var minDst: Long = 165813750
        var index = 0
        for(i in fitColors.indices)
        {
            val dst = getRGBDifference(current, fitColors[i])
            if(dst < minDst)
            {
                index = i
                minDst = dst.toLong()
            }
        }
        return index
    }

    private fun getFitColor(current: PersonalColor) : PersonalColor
    {
        return desiredColors[getFitColorIndex(desiredColors, current)]
    }

    private fun getRelevantColors(max : Int, pixels: IntArray)
    {
        desiredColors = Array(max) { PersonalColor(0,0,0, 1, 0)}
        val desiredColorsTemp = Array(max) { PersonalColor(0,0,0, 1, 0)}
        val meanDifference: Long; val meanFactor = -140; val minDiffOfAnt = 40

        val c = PersonalColor(0,0,0, 1, 0)
        for(i in pixels.indices)
        {
            c.r += Color.red(pixels[i])
            c.g += Color.green(pixels[i])
            c.b += Color.blue(pixels[i])
            c.cont+=1
        }
        meanDifference = ( (c.r + c.g + c.b) / c.cont + meanFactor).toLong()
        var current = 0; var currentRGBDifference: Long

        for(i in pixels.indices)
        {
            if(current >= desiredColors.size) break

            c.r = Color.red(pixels[i])
            c.g = Color.green(pixels[i])
            c.b = Color.blue(pixels[i])
            currentRGBDifference = getRGBDifference(desiredColors[current], c).toLong()
            if(currentRGBDifference >= meanDifference && isAValidColor(currentRGBDifference, minDiffOfAnt) == 0)
            {
                desiredColors[current].r = c.r
                desiredColors[current].g = c.g
                desiredColors[current].b = c.b
                desiredColors[current].lastDiff = currentRGBDifference

                desiredColorsTemp[current].r = c.r
                desiredColorsTemp[current].g = c.g
                desiredColorsTemp[current].b = c.b
                current++
            }
        }
        meanColor(desiredColorsTemp, pixels)
    }
    private fun isAValidColor(currentDiff: Long, minDiff: Int) : Int
    {
        for(element in desiredColors)
            if(abs(currentDiff - element.lastDiff) <= minDiff) return 1
        return 0
    }


    private fun meanColor(desiredColorsTemp: Array<PersonalColor>, pixels: IntArray)
        : Array<PersonalColor>
    {
        val currentColor = PersonalColor(0,0,0, 1, 0)

        var vk: Int
        for(i in pixels.indices)
        {
            currentColor.r = Color.red(pixels[i])
            currentColor.g = Color.green(pixels[i])
            currentColor.b = Color.blue(pixels[i])
            vk = getFitColorIndex(desiredColorsTemp, currentColor)

            desiredColors[vk].r += currentColor.r
            desiredColors[vk].g += currentColor.g
            desiredColors[vk].b += currentColor.b
            desiredColors[vk].cont+=1
        }

        for(i in desiredColors.indices)
        {
            desiredColors[i].r /= desiredColors[i].cont
            desiredColors[i].g /= desiredColors[i].cont
            desiredColors[i].b /= desiredColors[i].cont
        }
        return desiredColors
    }

    data class PersonalColor(var r: Int, var g:Int, var b: Int, var cont: Int = 1, var lastDiff: Long = 0)

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

    private suspend fun updateImage(image: ShapeableImageView, bitmap: Bitmap, ctx: Context? = null)
    {
        withContext(Dispatchers.Main)
        {
            image.setImageBitmap(bitmap)
            if(ctx!=null) Toast.makeText(ctx, "The generation is complete", Toast.LENGTH_SHORT).show()
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