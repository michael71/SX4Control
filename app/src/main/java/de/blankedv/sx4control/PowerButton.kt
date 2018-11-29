package de.blankedv.sx4control

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.support.v7.widget.AppCompatButton
import android.util.AttributeSet
import de.blankedv.sx4control.MainApplication.Companion.globalPower


/**
 * Created by mblank on 25.01.15.
 */
class PowerButton : AppCompatButton {

    protected var darken = false


    constructor(context: Context) : super(context) {}

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {}

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = this.width
        val h = this.height
        val paint = Paint()
        paint.isAntiAlias = true

        if (darken == false) {

            if (globalPower) {

                paint.color = Color.GREEN
            } else {
                paint.color = Color.RED
            }
        } else {
            if (globalPower) {
                paint.color = Color.rgb(0, 150, 0)
            } else {
                paint.color = Color.rgb(150, 0, 0)

            }
        }
        canvas.drawCircle((w / 2).toFloat(), (h / 2).toFloat(), (w / 4).toFloat(), paint)


    }

    fun deactivate() {
        setTextColor(Color.DKGRAY)
        darken = true
    }

    fun activate() {
        setTextColor(Color.WHITE)
        darken = false
    }
}
