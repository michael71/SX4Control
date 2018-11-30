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
import de.blankedv.sx4control.MainApplication.Companion.selectedLoco
import de.blankedv.sx4control.MainApplication.Companion.sendQ

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
    private var speedbar: SpeedBarView? = null
    private var mToast: Toast? = null
    private var mHandler = Handler()  // used for UI Update timer
    private var mCounter = 0

    private var client : SXnetClientThread? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

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
                selectedLoco.setLocoSpeed(speedbar!!.intSpeed)
            }
        })

        change_dir!!.setOnClickListener {
            // Perform action on click: toggle direction

            selectedLoco.toggleDir()
            if (selectedLoco.speed < 0) {
                change_dir!!.setImageResource(R.drawable.left2)

            } else {
                change_dir!!.setImageResource(R.drawable.right2)
            }
            selectedLoco.setLocoSpeed(0)
            speedbar!!.setSpeed(0f)
        }

        loco_icon!!.isClickable = true
        loco_icon!!.setOnClickListener {
            /* TODO startActivity(
                Intent(
                    this@MainActivityActivity,
                    SelectLocoActivity::class.java
                )
            ) */
        }

        val loco_stop = findViewById<View>(R.id.loco_stop) as Button

        loco_stop.setOnClickListener {
            // Perform action on click: stop
            selectedLoco.setLocoSpeed(0)
            speedbar!!.intSpeed = 0

        }

        powerBtn!!.setOnClickListener {
            togglePower()
            powerBtn!!.invalidate()
        }
        // indexes from 0 to (MAX_FUNC-1)
        f[0] = findViewById<View>(R.id.f0) as FunctionButton
        f[1] = findViewById<View>(R.id.f1) as FunctionButton
        f[0]?.setOnClickListener  { processFunctionKey(0) }
        f[1]?.setOnClickListener  { processFunctionKey(1) }

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
        Log.d(TAG, "MainActivcity - onPause")
        // store current loco data including speed etc
        val prefs = PreferenceManager
            .getDefaultSharedPreferences(this)
        val editor = prefs.edit()
        Log.d(TAG, "saveCurrentLoco")
        // generic
        val adr = selectedLoco?.adr ?: 3
        editor.putInt(KEY_LOCO_ADDR, adr)  // last used loco address
        val data = selectedLoco?.getSXData() ?: 0
        editor.putInt(KEY_LOCO_DATA, data)  // last used loco address
        editor.apply()


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
            if (DEBUG)
                Log.d(TAG, "loading lastLoco Adr from prefs - last adr=" + selectedLoco.getAdr())
            selectedLoco.adr = prefs.getInt(KEY_LOCO_ADDR, DEFAULT_LOCO)
        }
        if (DEBUG) Log.d(TAG, "loading sx data from prefs" + selectedLoco.getAdr())
        selectedLoco.updateLocoFromSX(prefs.getInt(KEY_LOCO_DATA, 0))

        loco_text!!.text = "A: " + selectedLoco.getAdr()
        //loco_icon!!.setImageBitmap(selectedLoco.getIcon())
        speedbar!!.setTitle("LOCO#"+selectedLoco.getAdr()) // TODO selectedLoco.getName())
        if (selectedLoco.forward) {
            change_dir!!.setImageResource(R.drawable.right2)
        } else {
            change_dir!!.setImageResource(R.drawable.left2)
        }

        speedbar!!.intSpeed = selectedLoco.speed
        sendQ.offer("S "+ selectedLoco.adr + " " + selectedLoco.getSXData())
        f[0]!!.setON(selectedLoco.lamp)
        f[1]!!.setON(selectedLoco.function)

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

        // logString is updated via Binding mechanism

        // the actionBar icons are NOT updated via binding, because
        // "At the moment, data binding is only for layout resources, not menu resources" (google)
        // and the implementation to "work around" this limitation looks very complicated, see
        // https://stackoverflow.com/questions/38660735/how-bind-android-databinding-to-menu
        //setConnectionIcon()
        if (globalPower) {
            powerBtn!!.activate()
        } else {
            powerBtn!!.deactivate()
        }
        f[0]!!.setON(selectedLoco.lamp)
        f[1]!!.setON(selectedLoco.function)
        speedbar!!.intSpeed = selectedLoco.speed

        selectedLoco!!.sendLocoToSXNet()

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
        val prefs = PreferenceManager
            .getDefaultSharedPreferences(this)
        val ip = prefs.getString(KEY_IP, SXNET_START_IP)

        if (DEBUG) Log.d(TAG, "connecting to " + ip!!)
        client = SXnetClientThread(this, ip!!, SXNET_PORT)
        client?.start()

    }

    private fun processFunctionKey(k : Int) {
        when(k) {
            0 -> selectedLoco.toggleLocoLamp()
            1 -> selectedLoco.toggleFunc()
        }
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
