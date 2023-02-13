package com.fourdevsociety.imageeditor.model
data class AccumulativeFilterButton(
    val name: String = "Brightness",
    var opacity: Float = 0.3f,
    val image: Int,
    val min: Int = 0,
    val max: Int = 50,
    var current: Int = max / 2,
)