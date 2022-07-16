package ru.ozon.tumblr_button.tumblr_button

import android.graphics.Point
import android.graphics.PointF
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.core.graphics.toPointF
import androidx.dynamicanimation.animation.SpringAnimation

data class ButtonState(
    var label: String = "",
    @ColorInt var color: Int,
    @DrawableRes var icon: Int? = null,
    var action: (() -> Unit)? = null
) {
    val animationsFollow: MutableList<SpringAnimation> = mutableListOf()
    val animationsClick: MutableList<SpringAnimation> = mutableListOf()

    private val position: PointF = PointF()

    fun getX() = position.x
    fun getY() = position.y

    fun setX(x: Float) {
        position.x = x
    }

    fun setY(y: Float) {
        position.y = y
    }

    fun setCurrentPosition(x: Float, y: Float) {
        position.set(x, y)
    }

    fun setCurrentPosition(point: Point) {
        position.set(point.toPointF())
    }

    fun setCurrentPosition(point: PointF) {
        position.set(point)
    }
}