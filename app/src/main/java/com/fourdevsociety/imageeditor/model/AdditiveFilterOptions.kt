package com.fourdevsociety.imageeditor.model

import com.fourdevsociety.imageeditor.utils.ImageUtils

object AdditiveFilterOptions {
    object ReduceColor
    {
        const val name = "Reduce Color"
        const val min = 2
        const val max = 10
        var current = 4
    }
    object KBits
    {
        const val name = "K Bits"
        const val min = 2
        const val max = 8
        var current = 4
    }
}