package de.blankedv.sx4control

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.MotionEvent

/**
 * Based on : http://go-lambda.blogspot.fr/2012/02/rotary-knob-widget-on-android.html
 */
class RotaryKnobView : android.support.v7.widget.AppCompatImageView {

    private var angle = 0f
    private var thetaOld = 0f
    private var knobWidth: Int = 0
    private var knobHeight: Int = 0

    private var listener: RotaryKnobListener? = null

    constructor(context: Context) : super(context) {
        initialize()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        initialize()
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle) {
        initialize()
    }

    fun setKnobListener(l: RotaryKnobListener) {
        listener = l
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        knobWidth = w
        knobHeight = h
        initialize()
    }

    private fun getTheta(x: Float, y: Float): Float {
        val sx = x - knobWidth / 2.0f
        val sy = y - knobHeight / 2.0f

        val length = Math.sqrt((sx * sx + sy * sy).toDouble()).toFloat()
        val nx = sx / length
        val ny = sy / length
        val theta = Math.atan2(ny.toDouble(), nx.toDouble()).toFloat()

        val rad2deg = (180.0 / Math.PI).toFloat()
        val theta2 = theta * rad2deg

        return if (theta2 < 0) theta2 + 360.0f else theta2
    }

    fun initialize() {

        this.setImageResource(R.drawable.jog)
        setOnTouchListener { v, event ->
            val action = event.action
            val actionCode = action and MotionEvent.ACTION_MASK
            if (actionCode == MotionEvent.ACTION_POINTER_DOWN) {
                val x = event.getX(0)
                val y = event.getY(0)
                thetaOld = getTheta(x, y)

            } else if (actionCode == MotionEvent.ACTION_MOVE) {


                val x = event.getX(0)
                val y = event.getY(0)

                val theta = getTheta(x, y)
                val delta_theta = theta - thetaOld

                thetaOld = theta

                //int direction = (delta_theta > 0) ? 1 : -1;
                angle = theta - 270

                notifyListener(delta_theta, (theta + 90) % 360)
                invalidate()
            }
            true
        }
    }

    private fun notifyListener(delta: Float, angle: Float) {
        if (null != listener) {
            listener!!.onKnobChanged(delta, angle)
        }
    }

    override fun onDraw(c: Canvas) {
        c.rotate(angle, (knobWidth / 2).toFloat(), (knobHeight / 2).toFloat())
        super.onDraw(c)
    }

    interface RotaryKnobListener {
        fun onKnobChanged(delta: Float, angle: Float)
    }

}
