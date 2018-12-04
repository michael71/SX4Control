/*  (C) 2011-2015, Michael Blank
 * 
 *  This file is part of Lanbahn Throttle.

    Lanbahn Throttle is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Lanbahn Throttle is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with LanbahnThrottle.  If not, see <http://www.gnu.org/licenses/>.

*/
package de.blankedv.sx4control.controls

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.support.v7.widget.AppCompatButton
import android.util.AttributeSet

/**
 * adds an "LED" indicator or an Image with an onState and and OFF state
 * to the standard button
 */
class FunctionButton : AppCompatButton {

    var active = true
    var imageOn: Bitmap? = null
    var imageOff: Bitmap? = null

    var darken = false
    private var onState = true


    constructor(context: Context) : super(context) {}

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {}

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (darken) return  // draw no image/LED in this case

        val w = this.width
        val h = this.height

        if (imageOn == null || imageOff == null) {

            if (onState) {
                val paint = Paint()
                paint.isAntiAlias = true
                paint.color = Color.GRAY
                canvas.drawCircle((w / 5).toFloat(), (h / 5).toFloat(), (w / 10).toFloat(), paint)
                paint.color = Color.YELLOW
                canvas.drawCircle((w / 5).toFloat(), (h / 5).toFloat(), (w / 10 - 2).toFloat(), paint)
            } else {
                val paint = Paint()
                paint.isAntiAlias = true
                paint.color = Color.GRAY
                canvas.drawCircle((w / 5).toFloat(), (h / 5).toFloat(), (w / 10).toFloat(), paint)
            }
        } else {
            val x = (w / 2 - imageOn!!.width / 2).toFloat()
            val y = (h / 2 - imageOn!!.height / 2).toFloat()
            if (onState) {
                canvas.drawBitmap(imageOn!!, x, y, null)
            } else {
                canvas.drawBitmap(imageOff!!, x, y, null)
            }
        }

    }

    fun deactivate() {
        setTextColor(Color.DKGRAY)
        darken = true
    }

    fun activate() {
        setTextColor(Color.WHITE)
        darken = false
    }

    fun setON(state: Boolean) {
        active = true
        onState = state
        invalidate()
    }


}
