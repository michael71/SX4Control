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

package de.blankedv.sx4control

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Resources
import android.graphics.BitmapFactory
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.Bundle
import android.os.Handler
import android.preference.PreferenceManager
import android.util.DisplayMetrics
import android.util.Log
import android.view.*
import android.widget.*

import android.widget.Toast.LENGTH_SHORT
import de.blankedv.sx4control.MainApplication.*

// TODO finally delete .....

class ThrottleKnobActivity : Activity() {
/*
    // selLocoAddr is imported from SX4ThrottleApplication
    //      (therefor available during whole app lifetime)

    // TODO MDPI: Horn, Lautspr, kuppl, bell zu gross - Lokname zu gross
    // TODO XHPI: ---??
    // TODO TVDPI ??
    // TODO HDPI ??
    private var loco_text: TextView? = null
    private var loco_icon: ImageView? = null
    private var powerBtn: PowerButton? = null
    private val f = arrayOfNulls<FunctionButton>(MAX_FUNC)
    private var change_dir: ImageButton? = null
    private var builder: AlertDialog.Builder? = null
    private var app: SX4ThrottleApplication? = null

    private var speedbar: SpeedBarView? = null
    private var mToast: Toast? = null
    // used for reaction on received lanbahn commands
    private val mTimer = Handler()  // used for timer background task (lanbahn receive)


    // check if power button must be updated because of a received lanbahn "power" message
    private val mUpdateTimeTask = object : Runnable {
        override fun run() {
            powerBtn!!.invalidate()
            lastThrottleActiveTime = System.currentTimeMillis()
            mTimer.postDelayed(this, 200)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {

        val inflater = menuInflater
        inflater.inflate(R.menu.menu, menu)

        return super.onCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {

        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when (item.itemId) {
            R.id.menu_settings  // call preferences activity
            -> {
                startActivity(Intent(this, Preferences::class.java))
                return true
            }
            R.id.menu_throttle -> return true   // do nothing
            R.id.menu_locodb -> {
                startActivity(Intent(this, LocoDBActivity::class.java))
                return true
            }
            R.id.menu_about -> {
                startActivity(Intent(this, AboutActivity::class.java))
                return true
            }
            R.id.menu_imexport -> {
                startActivity(Intent(this, ManageDataActivity::class.java))
                return true
            }
            R.id.menu_quit -> {
                quitConfirmation()
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    /**
     * Called when the activity is first created.
     */
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(TAG, "ThrottleKnobActivcity - onCreate")

        setContentView(R.layout.main_knob)
        app = applicationContext as SX4ThrottleApplication   // needed for global status variables
        builder = AlertDialog.Builder(this)

        powerBtn = findViewById<View>(R.id.powerBtn) as PowerButton
        speedbar = findViewById<View>(R.id.speedbar) as SpeedBarView
        loco_text = findViewById<View>(R.id.loco_adr) as TextView
        loco_icon = findViewById<View>(R.id.loco_icon) as ImageView
        change_dir = findViewById<View>(R.id.change_dir) as ImageButton

        val jogView = findViewById<View>(R.id.jogView) as RotaryKnobView
        speedbar!!.intSpeed = 0

        jogView.setKnobListener(object : RotaryKnobView.RotaryKnobListener {
            override fun onKnobChanged(delta: Float, angle: Float) {
                speedbar!!.diffSpeed(delta)
                selLocoAddr.set_speed(speedbar!!.intSpeed)
                sendLocoMessageToLanbahn()
            }
        })

        change_dir!!.setOnClickListener {
            // Perform action on click: toggle direction
            selLocoAddr.stop()
            speedbar!!.speed = 0f
            selLocoAddr.toggle_dir()
            if (selLocoAddr.getDir() === BACKWARD) {
                change_dir!!.setImageResource(R.drawable.left2)
            } else {
                change_dir!!.setImageResource(R.drawable.right2)
            }

            sendLocoMessageToLanbahn()
        }

        loco_icon!!.isClickable = true
        loco_icon!!.setOnClickListener {
            startActivity(
                Intent(
                    this@ThrottleKnobActivity,
                    SelectLocoActivity::class.java
                )
            )
        }

        val loco_stop = findViewById<View>(R.id.loco_stop) as Button

        loco_stop.setOnClickListener {
            // Perform action on click: stop
            selLocoAddr.stop()
            sendLocoMessageToLanbahn()
            speedbar!!.intSpeed = 0
        }

        powerBtn!!.setOnClickListener {
            toggle_power()
            powerBtn!!.invalidate()
        }
        // indexes from 0 to (MAX_FUNC-1)
        f[0] = findViewById<View>(R.id.f0) as FunctionButton
        f[1] = findViewById<View>(R.id.f1) as FunctionButton

        if (DEBUG) logDensity()
    }

    override fun onPause() {
        super.onPause()
        Log.i(TAG, "ThrottleActivcity - onPause")
        // store current loco data including speed etc
        app!!.saveLocoToPreferences(selLocoAddr)
        super.onPause()
        if (DEBUG)
            Log.d(TAG, "onPause - MainActivity")
        // firstStart=false; // flag to avoid re-connection call during first
        // start
        //sendQ.add(DISCONNECT);
        // ((AndroPanelApplication) getApplication()).saveZoomEtc();
        // client.shutdown();
        pauseTimer = true
    }

    override fun onResume() {
        super.onResume()
        Log.i(TAG, "ThrottleKnobActivcity - onResume")

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)

        if (selLocoAddr == null) {
            selLocoAddr = app!!.loadLastLocoFromPreferences()
            if (DEBUG)
                Log.d(TAG, "loading lastLocoFromPreferences - was adr=" + selLocoAddr.getAdr())
        } else {
            val lstate = prefs.getString(KEY_CMD + selLocoAddr.getAdr(), "")
            selLocoAddr.setFromString(lstate)
            if (DEBUG) Log.d(TAG, "loading state for adr=" + selLocoAddr.getAdr())
        }

        loco_text!!.text = "A: " + selLocoAddr.getAdr()
        loco_icon!!.setImageBitmap(selLocoAddr.getIcon())
        speedbar!!.setTitle(selLocoAddr.getName())
        if (selLocoAddr.getDir() === BACKWARD) {
            change_dir!!.setImageResource(R.drawable.left2)
        } else {
            change_dir!!.setImageResource(R.drawable.right2)
        }

        speedbar!!.intSpeed = selLocoAddr.get_speed()
        sendLocoMessageToLanbahn()
        init_function_buttons()

        if (prefs.getBoolean(KEY_ALLOW_POWER_CONTROL, false)) {
            powerBtn!!.activate()
        } else {
            powerBtn!!.deactivate()
        }


        startSXNetCommunication()

        pauseTimer = false
    }

    fun startSXNetCommunication() {
        Log.d(TAG, " - startSXNetCommunication.")
        if (client != null) {
            client.shutdown()
            try {
                Thread.sleep(100) // give client some time to shut down.
            } catch (e: InterruptedException) {
                if (DEBUG)
                    Log.e(TAG, "could not sleep...")
            }

        }

        val prefs = PreferenceManager
            .getDefaultSharedPreferences(this)
        val ip = prefs.getString(KEY_IP, SXNET_START_IP)

        if (DEBUG) Log.d(TAG, "connecting to " + ip!!)
        client = SXnetClientThread(this, ip!!, SXNET_PORT)
        client.start()

    }


    /**
     * use individual buttons for the function keys of a loco if selected in settings AND
     * loco has individual images
     */
    private fun init_function_buttons() {
        if (DEBUG) Log.d(TAG, "init_function_buttons")
        val res = app!!.getResources()
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)

        for (i in 0 until MAX_FUNC) {
            if (selLocoAddr.getNfunc() > i) {
                if (prefs.getBoolean(KEY_INDIV_FUNC_KEYS, false) && selLocoAddr.FuncIconIsImage(i)) {
                    f[i].im_on = BitmapFactory.decodeResource(res, selLocoAddr.getFuncIconOn(i))
                    f[i].im_off = BitmapFactory.decodeResource(res, selLocoAddr.getFuncIconOff(i))
                    f[i].setText("")
                } else {
                    f[i].setText("F$i")
                    f[i].im_on = null
                    f[i].im_off = null
                }
                f[i].setON(selLocoAddr.func_on(i))
                f[i].activate()
                f[i].setClickable(true)
            } else {
                f[i].deactivate()
                f[i].setText("F$i")
                f[i].setClickable(false)
            }
            f[i].invalidate()
        }

    }




    private fun logDensity() {
        val density = resources.displayMetrics.densityDpi

        when (density) {
            DisplayMetrics.DENSITY_LOW -> Log.d(TAG, "density = LDPI")
            DisplayMetrics.DENSITY_MEDIUM -> Log.d(TAG, "density = MDPI")
            DisplayMetrics.DENSITY_TV -> Log.d(TAG, "density = TV")  // Nexus7
            DisplayMetrics.DENSITY_HIGH    // Samsung S3 mini
            -> Log.d(TAG, "density = HDPI")
            DisplayMetrics.DENSITY_XHIGH -> Log.d(TAG, "density = XHDPI")   // Samsung S3 neo, Nexus4
            DisplayMetrics.DENSITY_XXHIGH -> Log.d(TAG, "density = XXHDPI")
            DisplayMetrics.DENSITY_XXXHIGH -> Log.d(TAG, "density = XXHDPI")
        }
    }

    fun shutdownSXClient() {
        Log.d(TAG, "MainActivity - shutting down SXnet Client.")
        if (client != null)
            client.shutdown()
        if (client != null)
            client.disconnectContext()
        client = null

    }

    private fun checkWifi(): Boolean {
        // Check for WiFi connectivity
        val connManager = applicationContext
            .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val mWifi = connManager
            .getNetworkInfo(ConnectivityManager.TYPE_WIFI)

        if (mWifi == null || !mWifi.isConnected) {

            Log.e(TAG, "ThrottleApplication - no Wifi")
            if (mToast != null) mToast!!.cancel()  // avoid too many toasts
            mToast = Toast.makeText(applicationContext, getString(R.string.wifi_disconnected), LENGTH_SHORT)
            mToast!!.setGravity(Gravity.CENTER, 0, 0)
            mToast!!.show()
            return false
        } else {
            return true
        }
    }

    private fun sendLocoMessageToLanbahn() {
        if (selLocoAddr == null) {
            Log.e(TAG, "selLocoAddr = null")
        }
        if (checkWifi()) {
            sendQ.offer(selLocoAddr.loco_cmd())
        }
    }

    private fun toggle_power() {
        if (DEBUG) Log.d(TAG, "ThrottleApplication - toggle_power")

        if (!checkWifi()) {
            return
        }

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)

        if (prefs.getBoolean(KEY_ALLOW_POWER_CONTROL, false)) {
            if (!globalPower) {
                sendQ.offer("POWER 1")
                globalPower = true

            } else {
                sendQ.offer("POWER 0")
                globalPower = true
            }
        } else {
            if (DEBUG) Log.d(TAG, "ThrottleApplication - toggle_power not enabled.")
            if (mToast != null) mToast!!.cancel()  // avoid too many toasts
            mToast = Toast.makeText(
                applicationContext,
                getString(R.string.throttle_power_control_is_disabled),
                LENGTH_SHORT
            )
            mToast!!.setGravity(Gravity.CENTER, 0, 0)
            mToast!!.show()
        }

    }
}
*/

}
