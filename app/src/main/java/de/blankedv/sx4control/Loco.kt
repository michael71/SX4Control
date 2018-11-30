package de.blankedv.sx4control

import android.graphics.Bitmap
import android.util.Log

import de.blankedv.sx4control.MainApplication.Companion.sendQ

class Loco {
    var adr: Int = 0
    var name: String

    // speed vars used for setting loco speed
    @Volatile
    var speed: Int = 0        // 0 ... +31, speed currently sent via SXnet
    @Volatile
    var forward = true
    @Volatile
    var lamp: Boolean = false
    @Volatile
    var function: Boolean = false

    private var lastSX = INVALID_INT   // used to avoid resending
    private var lastSendTime = 0L

    private var lastToggleTime: Long = 0
    private var speedSetTime: Long = 0   // last time the speed was set on interface

    var lbm: Bitmap? = null

    private var sendNewDataFlag = false



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


    fun updateLocoFromSX(d: Int) {
        if (DEBUG) Log.d(TAG, "updateLocoFromSX d=" + d)
        forward = d and 0x20 != 0
        speed = d and 0x1f
        lamp = d and 0x40 != 0
        function = d and 0x80 != 0
    }

    /** called every 200 milliseconds from timer thread in activity
     *
     */
    fun sendLocoToSXNet() {

        if (sendNewDataFlag or ((System.currentTimeMillis() - lastSendTime) > 5000)) {
            // if anything changed (or after at least 5 seconds: send new value
            sendNewDataFlag = false

            // calc SX byte from speed, lamp, function
            var sx = getSXData()
            //if (sx != lastSX) { // avoid sending the same message again
            speedSetTime = System.currentTimeMillis()   // we are actively controlling the loco
            lastSX = sx

            /*if (demoFlag) {
                Message m = Message.obtain();
                m.what = SX_FEEDBACK_MESSAGE;
                m.arg1 = adr;
                m.arg2 = sx;
                handler.sendMessage(m);  // send SX data to UI Thread via Message
                return;
            } */
            sendQ.offer("S $adr $sx")
            lastSendTime = System.currentTimeMillis()
            //}
        }
    }

    fun getSXData(): Int {
        var sx = 0
        if (lamp)
            sx = sx or 0x40
        if (function)
            sx = sx or 0x80
        if (!forward)
            sx = sx or 0x20
        // finally add speed bits
        sx += speed and 0x1f
        return sx
    }

    fun toggleDir() {
        forward = !forward
        if (DEBUG) Log.d(TAG, "loco touched: toggle DIR, forward=$forward")
        sendNewDataFlag = true
        speedSetTime = System.currentTimeMillis()
        sendNewDataFlag = true
    }


    fun setLocoSpeed(s: Int) {   // always positive
        speed = s
        // limit range
        if (speed < 0) speed = 0
        if (speed > 31) speed = 31
        sendNewDataFlag = true
        speedSetTime = System.currentTimeMillis()
    }


    fun toggleLocoLamp() {
        if (System.currentTimeMillis() - lastToggleTime > 250) {  // entprellen
            lamp = !lamp
            lastToggleTime = System.currentTimeMillis()
            if (DEBUG) Log.d(TAG, "loco touched: toggle lamp")
            sendNewDataFlag = true
        }
    }

    fun toggleFunc() {
        if (System.currentTimeMillis() - lastToggleTime > 250) {  // entprellen
            function = !function
            if (DEBUG) Log.d(TAG, "loco touched: toggle func")
            lastToggleTime = System.currentTimeMillis()
            sendNewDataFlag = true
        }
    }


    override fun toString(): String {
        return "$name ($adr)"
    }


}
