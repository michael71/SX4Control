package de.blankedv.sx4control

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Message
import android.util.Log


class Loco {
    var adr: Int = 0
    var name: String

    // speed vars used for setting loco speed
    var speed: Int = 0        // -31 ... +31, speed currently sent via SXnet

    // actual speed read from SX bus - used for display
    var speed_from_sx: Int = 0

    private var last_sx = INVALID_INT   // used to avoid resending

    internal var lamp: Boolean = false
    var lamp_to_be: Boolean = false
    internal var function: Boolean = false
    var function_to_be: Boolean = false
    internal var lastToggleTime: Long = 0
    private var speedSetTime: Long = 0   // last time the speed was set on interface

    var lbm: Bitmap? = null


    private var sendNewDataFlag = false

    var incrFlag = false
    var decrFlag = false


    val isForward: Boolean
        get() = speed >= 0

    private// are the loco-controls touched in the last 5 seconds
    val isActive: Boolean
        get() = if (System.currentTimeMillis() - speedSetTime < 5000 || System.currentTimeMillis() - lastToggleTime < 5000) {
            true
        } else {
            false
        }


    constructor(name: String, adr: Int) { // TODO lbm: Bitmap) {

        this.adr = adr
        this.name = name
        //this.lbm = lbm

        lastToggleTime = 0 // no toggle so far
    }

    fun getAdr(): String {
        return "" + adr
    }


    fun initFromSX() {

        //TODO Commands.readLocoData(this.adr)
        resetToBe()
    }

    private fun resetToBe() {
        function_to_be = function
        lamp_to_be = lamp
    }

    fun updateLocoFromSX(d : Int) {
        if (DEBUG) Log.d(TAG,"updateLocoFromSX d="+d)
        var s = d and 0x1f
        if (d and 0x20 != 0) s = -s
        speed_from_sx = s
        lamp = d and 0x40 != 0
        function = d and 0x80 != 0
        if (System.currentTimeMillis() - lastToggleTime > 2000) {
            // safe to update "to-be" state as "as-is" state
            lamp_to_be = lamp
            function_to_be = function
        }
    }

    /** called every 100 milliseconds
     *
     */
    fun sendLocoToSXNet() {

        if (sendNewDataFlag) {  // if anything changed, send new value
            sendNewDataFlag = false

            // calc SX byte from speed, lamp, function
            var sx = 0
            if (lamp_to_be)
                sx = sx or 0x40
            if (function_to_be)
                sx = sx or 0x80
            if (speed < 0) {
                sx = sx or 0x20
                sx += -speed
            } else {
                sx += speed
            }
            if (sx != last_sx) { // avoid sending the same message again
                speedSetTime = System.currentTimeMillis()   // we are actively controlling the loco
                last_sx = sx

                /*if (demoFlag) {
                    Message m = Message.obtain();
                    m.what = SX_FEEDBACK_MESSAGE;
                    m.arg1 = adr;
                    m.arg2 = sx;
                    handler.sendMessage(m);  // send SX data to UI Thread via Message
                    return;
                } */
                //TODO Commands.setLocoData(adr, sx)
            }
        }
    }



    fun stopLoco() {
        speed = 0
        incrFlag = false
        decrFlag = false
        sendNewDataFlag = true
        speedSetTime = System.currentTimeMillis()
    }

    fun setLocoSpeed(s: Int) {
        speed  = s
        // limit range
        if (speed < -31) speed = -31
        if (speed > 31) speed = 31
        speedSetTime = System.currentTimeMillis()
    }


    fun toggleLocoLamp() {
        if (System.currentTimeMillis() - lastToggleTime > 250) {  // entprellen
            if (lamp_to_be) {
                lamp_to_be = false
            } else {
                lamp_to_be = true
            }
            lastToggleTime = System.currentTimeMillis()
            if (DEBUG) Log.d(TAG, "loco touched: toggle lamp_to_be")
            sendNewDataFlag = true
        }
    }

    fun toggleFunc() {
        if (System.currentTimeMillis() - lastToggleTime > 250) {  // entprellen
            if (function_to_be) {
                function_to_be = false
            } else {
                function_to_be = true
            }
            if (DEBUG) Log.d(TAG, "loco touched: toggle func")
            lastToggleTime = System.currentTimeMillis()
            sendNewDataFlag = true
        }
    }


    override fun toString(): String {
        return "$name ($adr)"
    }


}
