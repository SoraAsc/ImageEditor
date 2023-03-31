package com.fourdevsociety.imageeditor.utils

import android.content.Context
import android.graphics.*
import android.widget.Toast
import com.fourdevsociety.imageeditor.enums.GenerationMethod
import com.google.android.material.imageview.ShapeableImageView
import kotlinx.coroutines.*
import kotlin.math.min
import com.fourdevsociety.imageeditor.utils.ImageUtils.PersonalColor
import com.fourdevsociety.imageeditor.utils.ImageUtils.getFitColor
import com.fourdevsociety.imageeditor.utils.ImageUtils.getRelevantColors

val coroutineExceptionHandler = CoroutineExceptionHandler{_, throwable ->
    throwable.printStackTrace()
}

private val viewModelJob = Job()
private val uiScope = CoroutineScope(Dispatchers.Default + viewModelJob + coroutineExceptionHandler)
private var generationMethod: GenerationMethod = GenerationMethod.ORIGINAL

private var desiredColors = arrayOf<PersonalColor>()
object FilterUtils
{
    /**
     * Filter Handle:
     * Apply/Create a new image with the selected filter
     * @param bitmap The Bitmap that will be modified.
     * @param dummyBitmap A copy of the "bitmap", but with a less resolution
     * @param image The component that will be draw the new image
     * @param brightness The brightness that will be applied in the bitmap [Not Used]
     * @param contrast The contrast that will be applied in the bitmap [Not Used]
     * @param saturation The saturation that will be applied in the bitmap [Not Used]
     * @param colorsNumber Only used in the reduce color. Represent how many colors will be selected.
     * @param ctx The context of the main application
     * @param currentGenMethod The Generation Filter that will be handled
     * @param isUnique If is true give permission to this method be called more than one time.
     */
    fun filterHandle(bitmap: Bitmap, dummyBitmap: Bitmap, image: ShapeableImageView, brightness: Float,
                     contrast: Float, saturation: Float, colorsNumber: Int, ctx: Context,
                     currentGenMethod: GenerationMethod, isUnique: Boolean = false)
    {
        generationMethod = currentGenMethod

        val filterFun = when(generationMethod) {
            GenerationMethod.GENERATING_B_AND_W -> ::blackAndWhiteFilter
            GenerationMethod.GENERATING_GRAY -> ::grayFilter
            GenerationMethod.GENERATING_REDUCE_COLOR -> ::reduceColorFilter
            GenerationMethod.GENERATING_SEPIA -> ::sepiaFilter
            GenerationMethod.GENERATING_LARK -> ::larkFilter
            GenerationMethod.GENERATING_RED -> ::redFilter
            GenerationMethod.GENERATING_POINTILLISM -> ::larkFilter//::pointillismFilter
            else -> ::blackAndWhiteFilter
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
                dummyBitmap.getPixels(dummyPixels,0,dummyBitmap.width,0,0,
                    dummyBitmap.width,dummyBitmap.height)
                desiredColors = Array(colorsNumber) { PersonalColor(0,0,0, 1, 0)}
                getRelevantColors(desiredColors, colorsNumber, dummyPixels)
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


    /**
     * Black and White Filter:
     * Chooses a color based in the RGB mean, for example if the mean
     * is near 0 is black if not is white
     * @param c The rgb of the current pixel
     * @return The Int that represents the RGB of WHITE or BLACK
     */
    private fun blackAndWhiteFilter(c: PersonalColor) : Int
    {
        return if((c.r + c.g + c.b) / 3 <= 127)
            Color.BLACK
        else
            Color.WHITE
    }

    /**
     * Gray Filter:
     * Takes the current pixel and readjust their color contribution to make a shade of gray
     * @param c The rgb of the current pixel
     * @return The Int that represents the new RGB
     */
    private fun grayFilter(c: PersonalColor) : Int
    {
        val temp = (0.3 * c.r + 0.59 * c.g + 0.11 * c.b).toInt() //(c.r + c.g + c.b) / 3
        return Color.rgb(temp, temp, temp)
    }

    /**
     * Reduce Color Filter:
     * Takes the closest color among those available
     * @param c The rgb of the current pixel
     * @return The Int rgb of the fit color between the available
     */
    private fun reduceColorFilter(c: PersonalColor) : Int
    {
        val fitColor = getFitColor(desiredColors, c)
        return Color.rgb(fitColor.r, fitColor.g, fitColor.b)
    }

    /**
     * Lark Filter:
     * Gives to the image a brightness tone
     * @param c The rgb of the current pixel
     * @return The Int that represents the new RGB
     */
    private fun larkFilter(c: PersonalColor) : Int
    {
        val red =  ( (c.r * 1.1) + (c.g * 0.1) + (c.b * 0.1) - 20).toInt().coerceIn(0, 255)
        val green = ( (c.r * 0.1) + (c.g * 1.1) + (c.b * 0.1) - 20).toInt().coerceIn(0, 255)
        val blue =  ( (c.r * 0.1) + (c.g * 0.1) + (c.b * 1.1) - 20).toInt().coerceIn(0, 255)
        return Color.rgb(red, green, blue)
    }

    private fun redFilter(c: PersonalColor) : Int
    {
        val red =  ( (c.r * 2.1) + (c.g * 0.5) + (c.b * 0.5) ).toInt().coerceIn(0, 255)
        val green = ( (c.r * 0.1) + (c.g * 0.5) + (c.b * 0.1)).toInt().coerceIn(0, 255)
        val blue =  ( (c.r * 0.1) + (c.g * 0.1) + (c.b * 0.5) ).toInt().coerceIn(0, 255)
        return Color.rgb(red, green, blue)
    }

    /**
     * Sepia Filter:
     * Gives to the image a warm/brown tone
     * @param c The rgb of the current pixel
     * @return The Int that represents the new RGB
     */
    private fun sepiaFilter(c: PersonalColor) : Int
    {
        val red = min( ( (c.r * 0.393) + (c.g * 0.769) + (c.b * 0.189)).toInt(), 255)
        val green = min( ( (c.r * 0.349) + (c.g * 0.686) + (c.b * 0.168)).toInt(), 255)
        val blue = min( ( (c.r * 0.272) + (c.g * 0.534) + (c.b * 0.131)).toInt(), 255)
        return Color.rgb(red, green, blue)
    }

    private fun pointillismFilter(c: PersonalColor) : Int
    {

        return 0
    }

    private fun changeBCS(
        red: Int, green: Int, blue: Int, brightness: Float, contrast: Float,
            saturation: Float) : Triple<Int, Int, Int>
    {
        val r = (red + brightness) * contrast
        val g = (green + brightness) * contrast
        val b = (blue + brightness) * contrast
        val intensity = (r + g + b) / 3
        return Triple( (r + (r - intensity) * saturation).toInt().coerceIn(0, 255),
            (g + (g - intensity) * saturation).toInt().coerceIn(0, 255),
            (b + (b - intensity) * saturation).toInt().coerceIn(0, 255))
    }
    /**
     * Update Image:
     * Update the current Bitmap and the view
     * @param image The component that will be draw the new image
     * @param bitmap The current Bitmap with filter
     * @param ctx The context of the main application
     */
    private suspend fun updateImage(image: ShapeableImageView, bitmap: Bitmap, ctx: Context? = null)
    {
        withContext(Dispatchers.Main)
        {
            image.setImageBitmap(bitmap)
            if(ctx!=null) Toast.makeText(ctx, "The generation is complete",
                Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Go Back To Original:
     * Return the image to their original state
     * @param originalBitmap The original Bitmap without filter
     * @param image The component that will be draw the new image
     */
    fun goBackToOriginal(originalBitmap: Bitmap, image: ShapeableImageView)
    {
        generationMethod = GenerationMethod.ORIGINAL
        uiScope.launch(Dispatchers.Main)
        {
            updateImage(image, originalBitmap)
        }

    }
}