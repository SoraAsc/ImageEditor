package com.fourdevsociety.imageeditor.utils

import android.graphics.*
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

    fun reduceColorFilter(bitmap: Bitmap, dummyBitmap: Bitmap, image: ShapeableImageView, brightness: Float,
        contrast: Float, saturation: Float, colorsNumber: Int, isUnique: Boolean = false)
    {
        generationMethod = GenerationMethod.GENERATING_REDUCE_COLOR
        uiScope.launch(Dispatchers.Default)
        {
            val updateThreshold = 40000
            var updatedRows = 0
            val pixels = IntArray(bitmap.width * bitmap.height)
            bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
            val dummyPixels = IntArray(dummyBitmap.width * dummyBitmap.height)
            dummyBitmap.getPixels(dummyPixels,0,dummyBitmap.width,0,0,dummyBitmap.width,dummyBitmap.height)
            val desiredColors = getRelevantColors(colorsNumber, dummyPixels)
            var currentColor: PersonalColor
            for(i in pixels.indices)
            {
                if(generationMethod != GenerationMethod.GENERATING_REDUCE_COLOR && !isUnique)
                    return@launch
                currentColor = PersonalColor(Color.red(pixels[i]),
                    Color.green(pixels[i]),Color.blue(pixels[i]))
                val fitColor = getFitColor(currentColor, desiredColors)
                pixels[i] = Color.rgb(fitColor.r, fitColor.g, fitColor.b)
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

    private fun getRGBDifference(c1 : PersonalColor, c2: PersonalColor) : Long
    {
        return sqrt((c2.r - c1.r).toDouble().pow(2) +
                (c2.g - c1.g).toDouble().pow(2) + (c2.b - c1.b).toDouble().pow(2)).toLong()
    }

    private fun getFitColorIndex(current: PersonalColor, fitColors: Array<PersonalColor>): Int
    {
        var minDst = 165813750000
        var index = 0
        for(i in fitColors.indices)
        {
            val dst = getRGBDifference(current, fitColors[i])
            if(dst < minDst)
            {
                index = i
                minDst = dst
            }
        }
        return index
    }

    private fun getFitColor(current: PersonalColor, fitColors: Array<PersonalColor>) : PersonalColor
    {
        return fitColors[getFitColorIndex(current, fitColors)]
    }

    private fun getRelevantColors(max : Int, pixels: IntArray): Array<PersonalColor>
    {
        val desiredColors = Array(max) { PersonalColor(0,0,0)}
        val desiredColorsTemp = Array(max) { PersonalColor(0,0,0)}
        val meanDifference: Int; val meanFactor = -400; val minDiffOfAnt = 40
        //Getting The Initial Values
        for(i in 0 until max)
        {
            desiredColors[i] = PersonalColor(Color.red(pixels[i]),
                Color.green(pixels[i]),Color.blue(pixels[i]), 1)
        }
        val c = PersonalColor(0,0,0, 1)
        for(i in pixels.indices)
        {
            c.r += Color.red(pixels[i])
            c.g += Color.green(pixels[i])
            c.b += Color.blue(pixels[i])
            c.cont+=1
        }
        meanDifference = (c.r + c.g + c.b) / c.cont + meanFactor
        var current = 0; var currentRGBDifference: Long; var lastRGBDifference = 165813750000

        for(i in pixels.indices)
        {
            if(current >= max) break

            c.r = Color.red(pixels[i])
            c.g = Color.green(pixels[i])
            c.b = Color.blue(pixels[i])
            currentRGBDifference = getRGBDifference(desiredColors[current], c)
            if(currentRGBDifference >= meanDifference && abs(currentRGBDifference - lastRGBDifference) >= minDiffOfAnt)
            {
                //Log.i("MainTest", "$currentRGBDifference  $meanDifference")
                desiredColors[current].r = c.r
                desiredColors[current].g = c.g
                desiredColors[current].b = c.b

                desiredColorsTemp[current].r = c.r
                desiredColorsTemp[current].g = c.g
                desiredColorsTemp[current].b = c.b
                current++
                lastRGBDifference = currentRGBDifference
            }
        }
        return meanColor(max, desiredColors, desiredColorsTemp, pixels)
    }

    private fun meanColor(max: Int, desiredColors : Array<PersonalColor>,
        desiredColorsTemp: Array<PersonalColor>, pixels: IntArray)
        : Array<PersonalColor>
    {
        val currentColor = PersonalColor(0,0,0)

        var vk: Int
        for(i in pixels.indices)
        {
            currentColor.r = Color.red(pixels[i])
            currentColor.g = Color.green(pixels[i])
            currentColor.b = Color.blue(pixels[i])

            vk = getFitColorIndex(currentColor, desiredColorsTemp)
            desiredColors[vk].r += currentColor.r
            desiredColors[vk].g += currentColor.g
            desiredColors[vk].b += currentColor.b
            desiredColors[vk].cont+=1
        }

        for(i in 0 until max)
        {
            desiredColors[i].r /= desiredColors[i].cont
            desiredColors[i].g /= desiredColors[i].cont
            desiredColors[i].b /= desiredColors[i].cont
        }
        return desiredColors
    }

    data class PersonalColor(var r: Int, var g:Int, var b: Int, var cont: Int = 1 )

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