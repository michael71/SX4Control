package de.blankedv.sx4control

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet

class SpeedBarView : android.support.v7.widget.AppCompatImageView {

    internal var myPaint = Paint()
    var sBarWidth: Int = 0
    var sBarHeight: Int = 0
    private var speed = 0f
    private var title: String? = null

    var intSpeed: Int
        get() = (speed / 1).toInt()
        set(ispeed) {
            speed = ispeed * 1f
            if (speed > 1000f) speed = 1000f
            if (speed < 0) speed = 0f
            invalidate()
        }


    constructor(context: Context) : super(context) {}

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        initialize()
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle) {}

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        sBarWidth = w
        sBarHeight = h
        initialize()
    }


    fun initialize() {
        myPaint.color = Color.rgb(50, 100, 255)
        myPaint.strokeWidth = 10f

    }

    fun diffSpeed(diff: Float) {
        if (Math.abs(diff) > 30) return   // maybe erratic moves

        speed = speed + diff /// or (diff/2f); =more turns needed
        if (speed > 1000f) speed = 1000f
        if (speed < 0) speed = 0f
        invalidate()
    }

    fun setSpeed(newspeed: Float) {
        speed = newspeed
        if (speed > 1000f) speed = 1000f
        if (speed < 0) speed = 0f
        invalidate()
    }

    override fun onDraw(c: Canvas) {
        myPaint.color = Color.rgb(30, 30, 30)
        c.drawRect(0f, 0f, sBarWidth.toFloat(), sBarHeight.toFloat(), myPaint)
        val len = Math.floor(speed.toDouble()).toInt()
        myPaint.color = Color.rgb(50, 100, 255)
        c.drawRect(
            0f,
            (sBarHeight * 0 / 10).toFloat(),
            ((15 + len) * sBarWidth / 1015).toFloat(),
            (sBarHeight * 9 / 10).toFloat(),
            myPaint
        )
        c.drawRect(0f, (sBarHeight * 9 / 10).toFloat(), sBarWidth.toFloat(), sBarHeight.toFloat(), myPaint)

        myPaint.color = Color.rgb(255, 255, 255)
        myPaint.textSize = 48f
        val yPos = (sBarHeight / 2 - (myPaint.descent() + myPaint.ascent()) / 2).toInt()
        c.drawText(title!!, (sBarWidth * 1 / 3).toFloat(), yPos.toFloat(), myPaint)
        super.onDraw(c)
    }

    fun setTitle(title: String) {
        this.title = title.substring(0, Math.min(title.length, 15))
    }
}