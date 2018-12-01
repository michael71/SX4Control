package de.blankedv.sx4control

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.preference.PreferenceManager
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import de.blankedv.sx4control.MainApplication.Companion.globalPower
import de.blankedv.sx4control.MainApplication.Companion.pauseTimer
import de.blankedv.sx4control.MainApplication.Companion.selLocoAddr
import de.blankedv.sx4control.MainApplication.Companion.sendQ
import de.blankedv.sx4control.MainApplication.Companion.sxData

class MainActivity : AppCompatActivity(), SeekBar.OnSeekBarChangeListener {

    lateinit var builder: AlertDialog.Builder
    // TODO MDPI: Horn, Lautspr, kuppl, bell zu gross - Lokname zu gross
    // TODO XHPI: ---??
    // TODO TVDPI ??
    // TODO HDPI ??
    lateinit var locoAddr: TextView
    lateinit private var loco_icon: ImageView
    lateinit private var powerBtn: PowerButton
    lateinit private var stopBtn: Button
    lateinit private var lampBtn: FunctionButton
    lateinit private var functionBtn: FunctionButton
    lateinit private var changeDirBtn: ImageButton
    lateinit private var speedBar: SpeedBarView
    //lateinit private var speedBar2 : SeekBar
    lateinit private var jogView: RotaryKnobView
    private var mToast: Toast? = null
    private var mHandler = Handler()  // used for UI Update timer
    private var mCounter = 0

    private var client : SXnetClientThread? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        builder = AlertDialog.Builder(this)

        locoAddr = findViewById<View>(R.id.loco_adr) as TextView
        loco_icon = findViewById<View>(R.id.loco_icon) as ImageView
        powerBtn = findViewById<View>(R.id.powerBtn) as PowerButton
        stopBtn = findViewById<View>(R.id.loco_stop) as Button
        lampBtn = findViewById<View>(R.id.f0) as FunctionButton
        functionBtn = findViewById<View>(R.id.f1) as FunctionButton
        changeDirBtn = findViewById<View>(R.id.change_dir) as ImageButton
        speedBar = findViewById<View>(R.id.speedbar) as SpeedBarView
        jogView = findViewById<View>(R.id.jogView) as RotaryKnobView

        locoAddr.setText("$selLocoAddr")

        //speedbar!!.sxSpeed = 0
        jogView.setKnobListener(object : RotaryKnobView.RotaryKnobListener {
            override fun onKnobChanged(delta: Float, angle: Float) {
                speedBar.diffSpeed(delta)
                LocoUtil.setSpeed(speedBar.sxSpeed)
                speedBar.setTitle("S=${speedBar.sxSpeed}")
                Log.d(TAG,"knob changed delta=$delta sxSpeed=${speedBar.sxSpeed}")
            }
        })

        changeDirBtn.setOnClickListener {
            LocoUtil.setSpeed(0)
            LocoUtil.toggleDir()
            speedBar.setSXSpeed(0)
            speedBar.setTitle("S=0")

            if (LocoUtil.isForward() ) {
                changeDirBtn.setImageResource(R.drawable.right2)
            } else {
                changeDirBtn.setImageResource(R.drawable.left2)
            }
        }

        /* TODO
         loco_icon!!.isClickable = true
        loco_icon!!.setOnClickListener {
            // TODO startActivity(
                Intent(
                    this@MainActivityActivity,
                    SelectLocoActivity::class.java
                )
            )
        }  */



        stopBtn.setOnClickListener {
            // Perform action on click: stop
            LocoUtil.setSpeed(0)
            speedBar.setSXSpeed(0)
        }

        powerBtn.setOnClickListener {
            togglePower()
            powerBtn.invalidate()
        }

        lampBtn.setOnClickListener { LocoUtil.toggleLamp() }
        functionBtn.setOnClickListener { LocoUtil.toggleFunction() }


        builder = AlertDialog.Builder(this)
        builder.setMessage("Are you sure you want to exit?")
            .setCancelable(false)
            .setPositiveButton("Yes") { dialog, id ->
                    shutdownSXClient()
                    try {
                        Thread.sleep(100)
                    } catch (e: InterruptedException) {
                        Log.e(TAG,e.message)
                    }
                    finish()
                }
            .setNegativeButton("No") { dialog, id -> dialog.cancel() }

    }

    override fun onProgressChanged(seekBar: SeekBar, progress: Int,
                                   fromUser: Boolean) {
        Log.d(TAG,"Bar progress=$progress")
    }

    override fun onStartTrackingTouch(seekBar: SeekBar) {
        Log.d(TAG,"Bar startTouch")
    }

    override fun onStopTrackingTouch(seekBar: SeekBar) {
        Log.d(TAG,"Bar stopTouch")
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        val id = item.itemId

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            val intent = Intent(this, Preferences::class.java)
            startActivity(intent)
            return true
        } else if (id == R.id.action_about) {
            val intent = Intent(this, AboutActivity::class.java)
            startActivity(intent)
            return true
        /* } else if (id == R.id.action_reconnect) {
            startSXNetCommunication()
            forceDisplay = true // refresh display
            pauseTimer = false
            Toast.makeText(this, "reconnect", Toast.LENGTH_SHORT).show()
        */
        } else if (id == R.id.action_exit) {
            val alert = builder.create()
            alert.show()
            return true
        }
        return true
    }
    override fun onPause() {
        super.onPause()
        Log.d(TAG, "MainActivcity - onPause")
        // store current loco data including speed etc
        val prefs = PreferenceManager
            .getDefaultSharedPreferences(this)
        val editor = prefs.edit()
        Log.d(TAG, "saveCurrentLoco")
        // generic
        val adr = selLocoAddr
        editor.putInt(KEY_LOCO_ADDR, adr)  // last used loco address
        editor.apply()

        pauseTimer = true
    }

    override fun onResume() {
        super.onResume()
        Log.i(TAG, "MainActivcity - onResume")

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)

        selLocoAddr = 25
        /*
        if (selLocoAddr == INVALID_INT) {
            selLocoAddr = prefs.getInt(KEY_LOCO_ADDR, DEFAULT_LOCO)
            if (DEBUG)
                Log.d(TAG, "loading lastLoco Adr from prefs - addr=$selLocoAddr")
        } */
        sendQ.offer("R $selLocoAddr")   // try to update loco data from SXnet
        locoAddr.text = "A=" + selLocoAddr
        //loco_icon!!.setImageBitmap(selLocoAddr.getIcon())
        //speedbar!!.setTitle("LOCO#"+selLocoAddr) // TODO selLocoAddr.getName())

        if (prefs.getBoolean(KEY_ALLOW_POWER_CONTROL, true)) {
            powerBtn!!.activate()
        } else {
            powerBtn!!.deactivate()
        }

        startSXNetCommunication()
        mHandler.postDelayed({ updateUI() }, 500)
        pauseTimer = false
    }

    private fun updateUI() {
        mCounter++

        // the actionBar icons are NOT updated via binding, because
        // "At the moment, data binding is only for layout resources, not menu resources" (google)
        // and the implementation to "work around" this limitation looks very complicated, see
        // https://stackoverflow.com/questions/38660735/how-bind-android-databinding-to-menu
        //setConnectionIcon()
        if (globalPower) {
            powerBtn.activate()
        } else {
            powerBtn.deactivate()
        }
        lampBtn.setON(LocoUtil.isLampOn())
        functionBtn.setON(LocoUtil.isFunctionOn())
        if (LocoUtil.isForward() ) {
            changeDirBtn!!.setImageResource(R.drawable.right2)
        } else {
            changeDirBtn!!.setImageResource(R.drawable.left2)
        }
        if (selLocoAddr != INVALID_INT) {
            speedBar.setSXSpeed(sxData[selLocoAddr])
        }
        //speedbar.sxSpeed = LocoUtil.getSpeed()
        mHandler.postDelayed({ updateUI() }, 500)
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

    private fun shutdownSXClient() {
        Log.d(TAG, "MainActivity - shutting down SXnet Client.")

        client?.shutdown()
        client?.disconnectContext()
        client = null

    }

    private fun startSXNetCommunication() {
        Log.d(TAG, " - startSXNetCommunication.")
        if (client != null) {
            client?.shutdown()
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
        client?.start()

    }

    private fun togglePower() {
        if (!globalPower) {
            sendQ.offer("SETPOWER 1")
            globalPower = true

        } else {
            sendQ.offer("SETPOWER 0")
            globalPower = true
        }
    }

    companion object {


    }
}
