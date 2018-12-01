package de.blankedv.sx4control

import android.graphics.Bitmap
import android.util.Log
import de.blankedv.sx4control.MainApplication.Companion.selLocoAddr
import de.blankedv.sx4control.MainApplication.Companion.sendQ
import de.blankedv.sx4control.MainApplication.Companion.sxData

class LocoUtil {

    var lbm: Bitmap? = null

    companion object {

        private var lastSX = INVALID_INT   // used to avoid resending
        private var lastSentTime = 0L
        private var locoBitmap: Bitmap? = null

        fun updateLoco() {
            var toSend = sxData[selLocoAddr]
            if ((lastSX != toSend)
                or ((System.currentTimeMillis() - lastSentTime) > 1000) ) {
                sendQ.offer("S $selLocoAddr $toSend")
                lastSX = toSend
                lastSentTime = System.currentTimeMillis()
            }
        }

        fun getSpeed() : Int {
            return (sxData[selLocoAddr] and 0x1f)
        }

        fun setSpeed(s : Int) {
            var speed = s
             // limit range
            if (s < 0) speed = 0
            if (s > 31) speed = 31
            Log.d(TAG,"LocoUtil.setSpeed($s)")
            sxData[selLocoAddr] = ( sxData[selLocoAddr] and (0x1f).toBigInteger().inv().toInt()) or speed
            updateLoco()
        }
        fun isForward() : Boolean {
            return (sxData[selLocoAddr] and 0x20) == 0
        }

        fun toggleDir() {
            if (isForward()) {
                sxData[selLocoAddr] = sxData[selLocoAddr] or 0x20
            } else {
                sxData[selLocoAddr] = sxData[selLocoAddr] and (0x20).toBigInteger().inv().toInt()
            }
            updateLoco()
        }

        fun isLampOn() : Boolean {
            return (sxData[selLocoAddr] and 0x40) != 0
        }

        fun toggleLamp() {
            if (!isLampOn()) {
                sxData[selLocoAddr] = sxData[selLocoAddr] or 0x40
            } else {
                sxData[selLocoAddr] = sxData[selLocoAddr] and (0x40).toBigInteger().inv().toInt()
            }
            updateLoco()
        }

        fun isFunctionOn() : Boolean {
            return (sxData[selLocoAddr] and 0x80) != 0
        }

        fun toggleFunction() {
            if (!isFunctionOn()) {
                sxData[selLocoAddr] = sxData[selLocoAddr] or 0x80
            } else {
                sxData[selLocoAddr] = sxData[selLocoAddr] and (0x80).toBigInteger().inv().toInt()
            }
            updateLoco()
        }
    }
}
