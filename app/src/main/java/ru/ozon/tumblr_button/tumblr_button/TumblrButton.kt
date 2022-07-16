package ru.ozon.tumblr_button.tumblr_button

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.text.TextPaint
import android.util.AttributeSet
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.dynamicanimation.animation.FloatPropertyCompat
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import ru.ozon.tumblr_button.R

class TumblrButton @JvmOverloads constructor(
    context: Context,
    attrSet: AttributeSet,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : View(context, attrSet, defStyleAttr, defStyleRes) {

    var buttons: List<ButtonState> = emptyList()
        set(value) {
            setDefaultAnimationForButtons(value)
            field = value
        }

    private val paint = Paint()
    private val textPaint = TextPaint()

    private var radius: Float = DEFAULT_START_POSITION
    var gravity = Gravity.NO_GRAVITY
        private set
    private var color = Color.parseColor("#b00b69") // #d14d57
    private var buttonsMargin = 0f
    private var fillColor = -1
    private var title = ""

    @DrawableRes
    private var iconRes = -1
        set(value) {
            field = value
            if (value > 0)
                icon = AppCompatResources.getDrawable(context, iconRes)
        }
    private var icon: Drawable? = null
        set(value) {
            field = value
            invalidate()
        }
    private var iconPadding: Float = DEFAULT_ICON_PADDING

    private var labelColor = Color.BLACK
    private var isClicked = false
        set(value) {
            field = value
            changeAdditionalButtonsState(value)
            invalidate()
        }
    private var currentPosX: Float = DEFAULT_START_POSITION
    private var currentPosY: Float = DEFAULT_START_POSITION
    private var startPositionX: Float = DEFAULT_START_POSITION
    private var startPositionY: Float = DEFAULT_START_POSITION

    private var lastTouchX: Float = 0f
    private var lastTouchY: Float = 0f

    private var activePointerId = INVALID_POINTER_ID

    private val propertyAnimX = object : FloatPropertyCompat<TumblrButton>(PROPERTY_X) {
        override fun getValue(button: TumblrButton?): Float {
            return button?.currentPosX ?: DEFAULT_START_POSITION
        }

        override fun setValue(button: TumblrButton?, value: Float) {
            button?.setXValue(value)
        }
    }

    private val propertyAnimY = object : FloatPropertyCompat<TumblrButton>(PROPERTY_Y) {
        override fun getValue(button: TumblrButton?): Float {
            return button?.currentPosY ?: DEFAULT_START_POSITION
        }

        override fun setValue(button: TumblrButton?, value: Float) {
            button?.setYValue(value)
        }
    }

    private val animationX = SpringAnimation(this, propertyAnimX, 0f).apply {
        spring.stiffness = SpringForce.STIFFNESS_LOW
        spring.dampingRatio = SpringForce.DAMPING_RATIO_LOW_BOUNCY
    }

    private val animationY = SpringAnimation(this, propertyAnimY, 0f).apply {
        spring.stiffness = SpringForce.STIFFNESS_LOW
        spring.dampingRatio = SpringForce.DAMPING_RATIO_LOW_BOUNCY
    }

    private val statePropertyAnimX = object : FloatPropertyCompat<ButtonState>(PROPERTY_X) {
        override fun getValue(button: ButtonState?): Float {
            return button?.getX() ?: DEFAULT_START_POSITION
        }

        override fun setValue(button: ButtonState?, value: Float) {
            button?.setX(value)
            invalidate()
        }
    }

    private val statePropertyAnimY = object : FloatPropertyCompat<ButtonState>(PROPERTY_Y) {
        override fun getValue(button: ButtonState?): Float {
            return button?.getY() ?: DEFAULT_START_POSITION
        }

        override fun setValue(button: ButtonState?, value: Float) {
            button?.setY(value)
            invalidate()
        }
    }

    init {
        context.theme.obtainStyledAttributes(
            attrSet,
            R.styleable.TumblrButton,
            0, 0
        ).apply {
            try {
                title = getString(R.styleable.TumblrButton_tb_label) ?: ""
                color = getColor(R.styleable.TumblrButton_tb_color, color)
                fillColor = getColor(R.styleable.TumblrButton_tb_fill_color, fillColor)
                labelColor = getColor(R.styleable.TumblrButton_tb_label_color, labelColor)
                buttonsMargin = getDimension(R.styleable.TumblrButton_tb_margin, 0f)
                radius = getDimension(R.styleable.TumblrButton_tb_radius, radius)
                gravity = getInteger(R.styleable.TumblrButton_android_gravity, gravity)
                iconRes = getResourceId(R.styleable.TumblrButton_tb_icon, -1)
                iconPadding = getDimension(R.styleable.TumblrButton_tb_icon_padding, iconPadding)
            } finally {
                recycle()
            }
        }
        initPaint()
        initFillColor()
        elevation = 200f
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val minWidth = if (suggestedMinimumWidth > MIN_SIZE) suggestedMinimumWidth else MIN_SIZE
        val minHeight = if (suggestedMinimumHeight > MIN_SIZE) suggestedMinimumHeight else MIN_SIZE
        val needWidth = paddingStart + paddingEnd + minWidth
        val needHeight = paddingTop + paddingBottom + minHeight
        val measuredWidth = calculateSize(needWidth, widthMeasureSpec)
        val measuredHeight = calculateSize(needHeight, heightMeasureSpec)
        initButtonPosition(measuredWidth, measuredHeight)
        setMeasuredDimension(measuredWidth, measuredHeight)
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        if (isClicked) {
            canvas?.drawColor(fillColor)
        }
        drawButtonLabel(canvas, currentPosX, currentPosY, title)
        buttons.forEach {
            drawCircle(it.getX(), it.getY(), it.color, canvas)
            drawButtonLabel(canvas, it.getX(), it.getY(), it.label)
            drawButtonIcon(it.icon, it.getBounds(), canvas)
        }
        drawCircle(currentPosX, currentPosY, color, canvas)
        drawButtonIcon(icon, getMainButtonBounds(), canvas)
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        return when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                saveLastTouch(event)
                if (isOnButton() || isClicked) {
                    activePointerId = event.getPointerId(0)
                    true
                } else false
            }
            MotionEvent.ACTION_MOVE -> {
                if (isOnButton() && !isClicked) {
                    val (x: Float, y: Float) = findPointerCoords(event)
                    val xPosition = currentPosX + x - lastTouchX
                    val yPosition = currentPosY + y - lastTouchY
                    animateAdditionalButtons(xPosition, yPosition)
                    drawAt(xPosition, yPosition)

                    lastTouchX = x
                    lastTouchY = y
                }
                isOnButton()
            }
            MotionEvent.ACTION_UP -> {
                val (x: Float, y: Float) = findPointerCoords(event)
                val isOnStartPosition = isOnStartPosition(x, y)
                if (buttons.isEmpty()) {
                    if (isOnStartPosition)
                        performClick()
                } else {
                    if (!isClicked) {
                        if (isOnStartPosition) {
                            isClicked = true
                        }
                    } else {
                        isClicked = false
                        if (isOnStartPosition) {
                            performClick()
                        } else {
                            if (x > 0f && y > 0f)
                                buttons.forEach {
                                    if (it.isOnButtonPosition(x, y)) {
                                        it.action?.invoke()
                                    }
                                }
                        }
                    }
                }
                cancel()
            }
            MotionEvent.ACTION_CANCEL -> cancel()
            else -> super.onTouchEvent(event)
        }
    }

    override fun performClick(): Boolean {
        return if (isLastTouchOnStartPosition()) super.performClick() else false
    }

    fun setGravity(newGravity: Int) {
        gravity = newGravity
        initButtonPosition(width, height)
        animateTo(startPositionX, startPositionY)
    }


    private fun drawButtonIcon(@DrawableRes iconRes: Int?, bounds: Rect, canvas: Canvas?) {
        iconRes?.let {
            AppCompatResources.getDrawable(context, iconRes)?.let {
                drawButtonIcon(it, bounds, canvas)
            }
        }
    }

    private fun drawButtonIcon(icon: Drawable?, bounds: Rect, canvas: Canvas?) {
        canvas?.let {
            icon?.let {
                icon.bounds = bounds
                icon.draw(canvas)
            }
        }
    }

    private fun drawCircle(xValue:Float, yValue: Float, color: Int, canvas: Canvas?) {
        paint.color = color
        canvas?.drawCircle(xValue, yValue, radius, paint)
    }

    private fun drawButtonLabel(canvas: Canvas?, xValue: Float, yValue: Float, text: String) {
        if (isClicked) {
            var deltaX = radius + buttonsMargin
            if (gravity.and(Gravity.END) == Gravity.END)
                deltaX = -deltaX
            textPaint.textAlign = if (deltaX > 0) Paint.Align.LEFT else Paint.Align.RIGHT
            canvas?.drawText(text, xValue + deltaX, yValue + textPaint.textSize / 2, textPaint)
        }
    }

    private fun ButtonState.getBounds() = getBounds(getX(), getY())

    private fun getMainButtonBounds() = getBounds(currentPosX, currentPosY)

    private fun getBounds(xValue: Float, yValue: Float): Rect {
        val delta = radius - iconPadding
        val left = (xValue - delta).toInt()
        val top = (yValue - delta).toInt()
        val right = (xValue + delta).toInt()
        val bottom = (yValue + delta).toInt()
        return Rect(left, top, right, bottom)
    }

    private fun saveLastTouch(event: MotionEvent) {
        event.actionIndex.also { pointerIndex ->
            lastTouchX = event.getX(pointerIndex)
            lastTouchY = event.getY(pointerIndex)
        }
    }

    private fun findPointerCoords(event: MotionEvent): Pair<Float, Float> {
        return if (activePointerId != INVALID_POINTER_ID)
            event.findPointerIndex(activePointerId).let {
                event.getX(it) to event.getY(it)
            } else -1f to -1f
    }

    private fun cancel(): Boolean {
        activePointerId = INVALID_POINTER_ID
        return if (isOnButton()) {
            animateTo(startPositionX, startPositionY)
            true
        } else false
    }

    private fun initButtonPosition(width: Int, height: Int) {
        currentPosX = radius + paddingStart
        currentPosY = radius + paddingTop
        if (gravity.isGravityEnabled(Gravity.CENTER_HORIZONTAL))
            currentPosX = width / 2f + paddingStart - paddingEnd
        if (gravity.isGravityEnabled(Gravity.CENTER_VERTICAL))
            currentPosY = height / 2f + paddingTop - paddingBottom
        if (gravity.isGravityEnabled(Gravity.START))
            currentPosX = radius + paddingStart
        if (gravity.isGravityEnabled(Gravity.END))
            currentPosX = width - radius - paddingEnd
        if (gravity.isGravityEnabled(Gravity.TOP))
            currentPosY = radius + paddingTop
        if (gravity.isGravityEnabled(Gravity.BOTTOM))
            currentPosY = height - radius - paddingBottom
        startPositionX = currentPosX
        startPositionY = currentPosY

        buttons.forEach {
            it.setCurrentPosition(currentPosX, currentPosY)
        }
    }

    private fun Int.isGravityEnabled(gravityToConfirm: Int) =
        and(gravityToConfirm) == gravityToConfirm

    private fun initPaint() {
        paint.style = Paint.Style.FILL
        paint.isAntiAlias = true

        textPaint.style = Paint.Style.FILL
        textPaint.isAntiAlias = true
        textPaint.textSize = radius / 1.5f
        textPaint.color = labelColor
    }

    private fun initFillColor() {
        if (fillColor == -1) {
            val colorString = String.format("%06X", (0xFFFFFF and color))
            fillColor = Color.parseColor("#80$colorString")
        }
    }

    private fun isInCircle(xValue: Float, yValue: Float, centerX: Float, centerY: Float) =
        xValue in centerX - radius..centerX + radius &&
                yValue in centerY - radius..centerY + radius

    private fun isOnButton() = isInCircle(lastTouchX, lastTouchY, currentPosX, currentPosY)

    private fun isLastTouchOnStartPosition() = isOnStartPosition(lastTouchX, lastTouchY)

    private fun isOnStartPosition(x: Float, y: Float) =
        isInCircle(x, y, startPositionX, startPositionY)

    private fun ButtonState.isOnButtonPosition(x: Float, y: Float) =
        isInCircle(x, y, getX(), getY())

    private fun calculateSize(contentSize: Int, measureSpec: Int): Int {
        val mode = MeasureSpec.getMode(measureSpec)
        val specSize = MeasureSpec.getSize(measureSpec)
        return when (mode) {
            MeasureSpec.EXACTLY -> specSize
            MeasureSpec.AT_MOST -> contentSize.coerceAtMost(specSize)
            else -> contentSize // MeasureSpec.UNSPECIFIED
        }
    }

    private fun drawAt(xValue: Float, yValue: Float) {
        currentPosX = xValue
        currentPosY = yValue
        invalidate()
    }

    private fun setXValue(xValue: Float) {
        currentPosX = xValue
        invalidate()
    }

    private fun setYValue(yValue: Float) {
        currentPosY = yValue
        invalidate()
    }

    private fun animateTo(xValue: Float, yValue: Float) {
        animationX.animateToFinalPosition(xValue)
        animationY.animateToFinalPosition(yValue)

        animateAdditionalButtons(xValue, yValue)
    }

    private fun animateAdditionalButtons(xValue: Float, yValue: Float) {
        if (buttons.isNotEmpty()) {
            buttons.first().animationsFollow.first().animateToFinalPosition(xValue)
            buttons.first().animationsFollow.last().animateToFinalPosition(yValue)
        }
    }

    private fun changeAdditionalButtonsState(isActive: Boolean) {
        if (isActive) {
            buttons.forEachIndexed { index, buttonState ->
                buttonState.animationsFollow.forEach { it.skipToEnd() }
                var delta = (radius * 2 + buttonsMargin) * (index + 1)
                if (gravity.isGravityEnabled(Gravity.BOTTOM))
                    delta = -delta
                val newY = startPositionY + delta
                buttonState.animationsClick.last().animateToFinalPosition(newY)
            }
        } else {
            buttons.forEach {
                it.animationsClick.first().animateToFinalPosition(startPositionX)
                it.animationsClick.last().animateToFinalPosition(startPositionY)
            }
        }
    }

    private fun setDefaultAnimationForButtons(list: List<ButtonState>) {
        list.forEachIndexed { index, it ->
            it.animationsFollow.clear()
            val animX = SpringAnimation(it, statePropertyAnimX, DEFAULT_START_POSITION)
            val animY = SpringAnimation(it, statePropertyAnimY, DEFAULT_START_POSITION)
            it.animationsFollow.addAll(listOf(animX, animY))
            it.animationsFollow.forEachIndexed { animIndex, animation ->
                animation.spring.stiffness = SpringForce.STIFFNESS_LOW
                animation.spring.dampingRatio = SpringForce.DAMPING_RATIO_NO_BOUNCY
                if (index != list.lastIndex) {
                    animation.addUpdateListener { _, coordinate, _ ->
                        list[index + 1].animationsFollow[animIndex]
                            .animateToFinalPosition(coordinate)
                    }
                }
            }

            it.animationsClick.clear()
            val animClickX = SpringAnimation(it, statePropertyAnimX, DEFAULT_START_POSITION)
            val animClickY = SpringAnimation(it, statePropertyAnimY, DEFAULT_START_POSITION)
            it.animationsClick.addAll(listOf(animClickX, animClickY))
            it.animationsClick.forEach { animation ->
                animation.spring.stiffness = SpringForce.STIFFNESS_LOW
                animation.spring.dampingRatio = SpringForce.DAMPING_RATIO_NO_BOUNCY
            }
        }
    }

    private companion object {
        const val MIN_SIZE = 120
        const val INVALID_POINTER_ID = -1
        const val DEFAULT_START_POSITION = MIN_SIZE / 2f
        const val DEFAULT_ICON_PADDING = DEFAULT_START_POSITION / 2.5f

        const val PROPERTY_X = "PROPERTY_X"
        const val PROPERTY_Y = "PROPERTY_Y"
    }
}