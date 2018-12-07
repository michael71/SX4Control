package de.blankedv.sx4control.util

import android.graphics.Bitmap
import android.os.SystemClock
import android.util.Log
import de.blankedv.sx4control.model.MainApplication.Companion.selLocoAddr
import de.blankedv.sx4control.model.MainApplication.Companion.sendQ
import de.blankedv.sx4control.model.MainApplication.Companion.sxData
import de.blankedv.sx4control.model.INVALID_INT
import de.blankedv.sx4control.model.TAG

class LocoUtil {

    var lbm: Bitmap? = null

    companion object {

        private var lastSX = INVALID_INT   // used to avoid resending
        private var lastSentTime = 0L
        private var locoBitmap: Bitmap? = null

        private fun updateLoco() {
            var toSend = sxData[selLocoAddr]
            if ((lastSX != toSend)
                or ((SystemClock.elapsedRealtime() - lastSentTime) > 1000) ) {
                sendQ.offer("S $selLocoAddr $toSend")
                lastSX = toSend
                lastSentTime = SystemClock.elapsedRealtime()
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

        fun SXBinaryString(data: Int): String {
            val s = StringBuffer("00000000")
            val pos = 0

            if (data == INVALID_INT) return "--------"   // empty data

            // Selectrix Schreibweise LSB vorn !!
            if (data and 0x01 == 0x01) {
                s.setCharAt(0 + pos, '1')
            }
            if (data and 0x02 == 0x02) {
                s.setCharAt(1 + pos, '1')
            }
            if (data and 0x04 == 0x04) {
                s.setCharAt(2 + pos, '1')
            }
            if (data and 0x08 == 0x08) {
                s.setCharAt(3 + pos, '1')
            }
            if (data and 0x10 == 0x10) {
                s.setCharAt(4 + pos, '1')
            }
            if (data and 0x20 == 0x20) {
                s.setCharAt(5 + pos, '1')
            }
            if (data and 0x40 == 0x40) {
                s.setCharAt(6 + pos, '1')
            }
            if (data and 0x80 == 0x80) {
                s.setCharAt(7 + pos, '1')
            }
            return s.toString()
        }
    }
}
