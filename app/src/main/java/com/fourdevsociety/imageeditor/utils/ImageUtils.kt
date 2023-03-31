package com.fourdevsociety.imageeditor.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

object ImageUtils
{
    private const val minDiff = 70 // Min Value necessary to change the desired color
    data class PersonalColor(var r: Int, var g:Int, var b: Int, var cont: Int = 1,
                             var lastDiff: Long = 0)

    /**
     * Get RGB Difference:
     * Get The distance between the colors, using the distance between two vectors.
     * @param c1 Color 1
     * @param c2 Color 2
     * @return The distance of the colors
     */
    private fun getRGBDifference(c1 : PersonalColor, c2: PersonalColor) : Double
    {
        return sqrt((c2.r - c1.r).toDouble().pow(2) +
                (c2.g - c1.g).toDouble().pow(2) + (c2.b - c1.b).toDouble().pow(2))
    }

    /**
     * Get Fit Color Index:
     * Get the nearest Color compared to the Current Pixel Color
     * @param fitColors The array with the fit colors
     * @param current The current Pixel Color
     * @return The nearest Color Array Index
     */
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

    /**
     * Get Fit Color:
     * Get the nearest Color
     * @param current The Current Pixel Color
     * @return The nearest Color
     */
    fun getFitColor(desiredColors: Array<PersonalColor>,
                            current: PersonalColor) : PersonalColor
    {
        return desiredColors[getFitColorIndex(desiredColors, current)]
    }

    /**
     * Get Relevant Colors:
     * Collect the best colors and put them into desiredColors array
     * @param max the Max Colors that will be collected
     * @param pixels The array that contains all the pixels of a bitmap and their RGB
     */
    fun getRelevantColors(desiredColors: Array<PersonalColor>, max : Int, pixels: IntArray)
    {
        val desiredColorsTemp = Array(max) { PersonalColor(0,0,0, 1, 0)}

        val c = PersonalColor(0,0,0, 1, 0)
        for(i in 1 until max )
        {
            desiredColors[i] = PersonalColor(Color.red(pixels[i]), Color.green(pixels[i]),
                Color.blue(pixels[i]))
            desiredColors[i].lastDiff = getRGBDifference(desiredColors[i],desiredColors[i-1]).toLong()
            desiredColorsTemp[i] = PersonalColor(Color.red(pixels[i]), Color.green(pixels[i]), Color.blue(pixels[i]))
        }
        var current = 0; var currentRGBDifference: Long

        for(i in pixels.indices)
        {
            if(current >= desiredColors.size) current = 0

            c.r = Color.red(pixels[i])
            c.g = Color.green(pixels[i])
            c.b = Color.blue(pixels[i])
            currentRGBDifference = getRGBDifference(desiredColors[current], c).toLong()
            if(isAValidColor(desiredColors, currentRGBDifference) == 0)
            {
                desiredColors[current].r = c.r
                desiredColors[current].g = c.g
                desiredColors[current].b = c.b
                desiredColors[current].lastDiff = currentRGBDifference

                desiredColorsTemp[current].r = c.r
                desiredColorsTemp[current].g = c.g
                desiredColorsTemp[current].b = c.b
            } else current++
        }
        meanColor(desiredColors, desiredColorsTemp, pixels)
    }
    /**
     * Is A Valid Color:
     * Verify if the given color RGB distance is in the accepted range (above the minDiff)
     * @param currentDiff The Current RGB Difference
     * @return If is true the color is valid if not is invalid.
     */
    private fun isAValidColor(desiredColors: Array<PersonalColor>, currentDiff: Long) : Int
    {
        for(element in desiredColors)
            if(abs(currentDiff - element.lastDiff) >= minDiff) return 1
        return 0
    }


    /**
     * Mean Color:
     * Soft The desiredColors
     * @param desiredColorsTemp A Temp array with the Desired Colors
     * @param pixels The array that contains all the pixels of a bitmap and their RGB
     * @return The desired Colors
     */
    private fun meanColor(desiredColors: Array<PersonalColor>,
                          desiredColorsTemp: Array<PersonalColor>, pixels: IntArray)
    {
        val currentColor = PersonalColor(0, 0, 0, 1, 0)

        var vk: Int
        for (i in pixels.indices) {
            currentColor.r = Color.red(pixels[i])
            currentColor.g = Color.green(pixels[i])
            currentColor.b = Color.blue(pixels[i])
            vk = getFitColorIndex(desiredColorsTemp, currentColor)

            desiredColors[vk].r += currentColor.r
            desiredColors[vk].g += currentColor.g
            desiredColors[vk].b += currentColor.b
            desiredColors[vk].cont += 1
        }

        for (i in desiredColors.indices) {
            desiredColors[i].r /= desiredColors[i].cont
            desiredColors[i].g /= desiredColors[i].cont
            desiredColors[i].b /= desiredColors[i].cont
        }
    }

    fun getResizedBitmap(image: Bitmap, maxSize: Int = 200): Bitmap {
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

    fun saveToStorage(bitmap: Bitmap, ctx: Context)
    {
        val imageName = "img_editor${System.currentTimeMillis()}.jpg"
        var fos: OutputStream? = null
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
        {
            ctx.contentResolver?.also { resolver ->
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
            Toast.makeText(ctx, "The Image was saved", Toast.LENGTH_SHORT).show()
        }
    }

}