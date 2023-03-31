package com.fourdevsociety.imageeditor.model

import com.fourdevsociety.imageeditor.enums.GenerationMethod

data class ImageFilterButton(
    val name: String = "Original",
    val generationMethod: GenerationMethod,
    var imageOpacity: Float = 0.0f,
    var strokeWidth: Float = 0.0f,
    var imageRefresh: Boolean = true,
)
