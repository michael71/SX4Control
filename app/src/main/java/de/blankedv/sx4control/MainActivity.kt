package de.blankedv.sx4control

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.preference.PreferenceManager
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import de.blankedv.sx4control.MainApplication.Companion.prefs
import de.blankedv.sx4control.MainApplication.Companion.selectedLoco

import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    lateinit var builder: AlertDialog.Builder
    // TODO MDPI: Horn, Lautspr, kuppl, bell zu gross - Lokname zu gross
    // TODO XHPI: ---??
    // TODO TVDPI ??
    // TODO HDPI ??
    private var loco_text: TextView? = null
    private var loco_icon: ImageView? = null
    private var powerBtn: PowerButton? = null
    private val f = arrayOfNulls<FunctionButton>(2)
    private var change_dir: ImageButton? = null
    private var app: MainApplication? = null

    private var speedbar: SpeedBarView? = null
    private var mToast: Toast? = null
    // used for reaction on received lanbahn commands
    private val mTimer = Handler()  // used for timer background task (lanbahn receive)

    private var client : SXnetClientThread? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        app = applicationContext as MainApplication   // needed for global status variables
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
                selectedLoco.set_speed(speedbar!!.intSpeed)
                sendLocoMessageToLanbahn()
            }
        })

        change_dir!!.setOnClickListener {
            // Perform action on click: toggle direction
            selectedLoco.stop()
            speedbar!!.speed = 0f
            selectedLoco.toggle_dir()
            if (selectedLoco.getDir() === BACKWARD) {
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
            selectedLoco.stop()
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
        f[0]?.setOnClickListener(View.OnClickListener { process_f_key(0) })
        f[1]?.setOnClickListener(View.OnClickListener { process_f_key(1) })

        builder = AlertDialog.Builder(this)
        builder.setMessage("Are you sure you want to exit?")
            .setCancelable(false)
            .setPositiveButton("Yes") { dialog, id ->
                    // TODO shutdownSXClient()
                    try {
                        Thread.sleep(100)
                    } catch (e: InterruptedException) {
                        // TODO Auto-generated catch block
                        e.printStackTrace()
                    }

                    finish()
                }
            .setNegativeButton("No") { dialog, id -> dialog.cancel() }

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
        Log.i(TAG, "MainActivcity - onPause")
        // store current loco data including speed etc
        app!!.saveLocoToPreferences(selectedLoco)
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
        Log.i(TAG, "MainActivcity - onResume")

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)

        if (selectedLoco == null) {
            selectedLoco = app!!.loadLastLocoFromPreferences()
            if (DEBUG)
                Log.d(TAG, "loading lastLocoFromPreferences - was adr=" + selectedLoco.getAdr())
        } else {
            val lstate = prefs.getString(KEY_CMD + selectedLoco.getAdr(), "")
            selectedLoco.setFromString(lstate)
            if (DEBUG) Log.d(TAG, "loading state for adr=" + selectedLoco.getAdr())
        }

        loco_text!!.text = "A: " + selectedLoco.getAdr()
        loco_icon!!.setImageBitmap(selectedLoco.getIcon())
        speedbar!!.setTitle(selectedLoco.getName())
        if (selectedLoco.getDir() === BACKWARD) {
            change_dir!!.setImageResource(R.drawable.left2)
        } else {
            change_dir!!.setImageResource(R.drawable.right2)
        }

        speedbar!!.intSpeed = selectedLoco.get_speed()
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
    // check if power button must be updated because of a received lanbahn "power" message
    private val mUpdateTimeTask = object : Runnable {
        override fun run() {
            powerBtn!!.invalidate()
            lastThrottleActiveTime = System.currentTimeMillis()
            mTimer.postDelayed(this, 200)
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

        client?.shutdown()
        client?.disconnectContext()
        client = null

    }

    fun startSXNetCommunication() {
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

        val ip = prefs.getString(KEY_IP, SXNET_START_IP)

        if (DEBUG) Log.d(TAG, "connecting to " + ip!!)
        client = SXnetClientThread(this, ip!!, SXNET_PORT)
        client?.start()

    }

    companion object {


    }
}
