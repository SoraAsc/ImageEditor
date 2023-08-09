package com.fourdevsociety.imageeditor.ui

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import kotlin.math.atan2
import kotlin.math.pow
import kotlin.math.sqrt

class ColorWheelView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    private var lastX = -1f
    private var lastY = -1f

    private val wheelPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 200f
        isAntiAlias = true
    }

    private val selectedPaint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    private val lastSelectedPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        isAntiAlias = true
    }

    var selectedColor = Color.WHITE

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        event?.let {
            val action = event.action

            val x = event.x
            val y = event.y

            val centerX = width / 2f
            val centerY = height / 2f
            val radius = (width - wheelPaint.strokeWidth) / 2f - 20f


//           // Check if touch event is inside the wheel bounds
            if (sqrt((x - centerX).toDouble().pow(2.0) + (y - centerY).toDouble().pow(2.0)) > radius + wheelPaint.strokeWidth)
            {
                return false
            }
            // Check if touch event is inside selected color circle
            if (sqrt((x - centerX).toDouble().pow(2.0) + (y - centerY).toDouble().pow(2.0)) <= radius) {
                return false
            }

            when (action) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                    val dx = x - width / 2
                    val dy = y - height / 2
                    val angle = (atan2(dy.toDouble(), dx.toDouble()) / Math.PI * 180 + 360) % 360
                    val hue = angle.toFloat()
                    lastX = x
                    lastY = y
                    Log.i("MainTest", "$lastX and $lastY ")
                    selectedColor = Color.HSVToColor(floatArrayOf(hue, 1f, 1f))
                    invalidate()

                }
            }
        }
        return performClick()
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        canvas?.let {
            val centerX = width / 2f
            val centerY = height / 2f
            val radius = (width - wheelPaint.strokeWidth) / 2f

            for (i in 0..359) {
                val hue = i.toFloat()
                wheelPaint.color = Color.HSVToColor(floatArrayOf(hue, 1f, 1f))
                canvas.drawArc(centerX - radius, centerY - radius, centerX + radius, centerY + radius, i.toFloat(), 1f, false, wheelPaint)
            }

            selectedPaint.color = selectedColor
            canvas.drawCircle(centerX, centerY, radius - 20, selectedPaint)

            if(lastX >= 0 && lastY >= 0)
            {
                lastSelectedPaint.color = Color.BLACK
                canvas.drawCircle(lastX, lastY, 25f, lastSelectedPaint)
            }
        }
    }
}
