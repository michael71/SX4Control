package de.blankedv.sx4control

import android.app.AlertDialog
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.preference.PreferenceManager
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton
import android.util.DisplayMetrics
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import de.blankedv.sx4control.MainApplication.Companion.relevantChans
import de.blankedv.sx4control.MainApplication.Companion.globalPower
import de.blankedv.sx4control.MainApplication.Companion.pauseTimer
import de.blankedv.sx4control.MainApplication.Companion.selLocoAddr
import de.blankedv.sx4control.MainApplication.Companion.sendQ
import de.blankedv.sx4control.MainApplication.Companion.sxData
import org.jetbrains.anko.*

class MainActivity : AppCompatActivity(), SeekBar.OnSeekBarChangeListener,
    NumberPicker.OnValueChangeListener {

    private lateinit var builder: AlertDialog.Builder
    private lateinit var tvAddr: TextView
    private lateinit var loco_icon: ImageView
    // all buttons are of type "FunctionButton" because this seems to render them to the exact same size
    private lateinit var stopBtn: FunctionButton
    private lateinit var lampBtn: FunctionButton
    private lateinit var functionBtn: FunctionButton
    private lateinit var changeDirBtn: FunctionButton
    private lateinit var speedBar2: SeekBar

    private lateinit var channelView: ListView
    private lateinit var mOptionsMenu: Menu

    private var mToast: Toast? = null
    private var mHandler = Handler()  // used for UI Update timer
    private var mCounter = 0

    private var client: SXnetClientThread? = null

    private val channelList = mutableListOf<String>("")

    private lateinit var adapter : ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        builder = AlertDialog.Builder(this)

        tvAddr = findViewById<View>(R.id.tvAddr) as TextView
        loco_icon = findViewById<View>(R.id.loco_icon) as ImageView
        stopBtn = findViewById<View>(R.id.stopBtn) as FunctionButton
        lampBtn = findViewById<View>(R.id.f0) as FunctionButton
        functionBtn = findViewById<View>(R.id.f1) as FunctionButton
        changeDirBtn = findViewById<View>(R.id.changeBtn) as FunctionButton

        speedBar2 = findViewById<View>(R.id.speedBar2) as SeekBar
        speedBar2.setOnSeekBarChangeListener(this)

        channelView = findViewById<View>(R.id.channelView) as ListView



        tvAddr.text = "A = $selLocoAddr"
        changeDirBtn.im_off = BitmapFactory.decodeResource(getResources(), R.drawable.left3);
        changeDirBtn.im_on = BitmapFactory.decodeResource(getResources(), R.drawable.right3);
        stopBtn.darken = true   // => not changing appearance

        changeDirBtn.setOnClickListener {
            LocoUtil.setSpeed(0)
            LocoUtil.toggleDir()
            speedBar2.progress = 0
            changeDirBtn.setON(LocoUtil.isForward())
        }

        stopBtn.setOnClickListener {
            // Perform action on click: stop
            LocoUtil.setSpeed(0)
            speedBar2.progress = 0
        }

        lampBtn.setOnClickListener { LocoUtil.toggleLamp() }
        functionBtn.setOnClickListener { LocoUtil.toggleFunction() }

        builder = AlertDialog.Builder(this)
        builder.setMessage("Are you sure you want to exit?")
            .setCancelable(false)
            .setPositiveButton("Yes") { dialog, id ->
                shutdownSXClient()
                mySleep(100)
                finish()
            }
            .setNegativeButton("No") { dialog, id -> dialog.cancel() }

        tvAddr.setOnClickListener { addressPickerDialog() }
        adapter =
                ArrayAdapter(this, android.R.layout.simple_list_item_1, channelList)
        channelView.adapter = adapter
    }

    override fun onProgressChanged(
        seekBar: SeekBar, progress: Int,
        fromUser: Boolean
    ) {
        if (seekBar == speedBar2) {
            val sxSpeed = speedBar2.progress
            LocoUtil.setSpeed(sxSpeed)
            Log.d(TAG, "SeekBar sxSpeed=$sxSpeed")
        }

    }

    override fun onStartTrackingTouch(seekBar: SeekBar) {
        Log.d(TAG, "SeekBar startTouch")
    }

    override fun onStopTrackingTouch(seekBar: SeekBar) {
        Log.d(TAG, "SeekBar stopTouch")
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        mOptionsMenu = menu
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        when (item.itemId) {
            R.id.action_settings -> {
                val intent = Intent(this, Preferences::class.java)
                startActivity(intent)
            }
            R.id.action_about -> {
                val intent = Intent(this, AboutActivity::class.java)
                startActivity(intent)
            }
            R.id.action_reconnect, R.id.action_connect -> {
                startSXNetCommunication()
                pauseTimer = false
                toast("trying reconnect")
            }
            R.id.action_power -> {
                togglePower()
            }
            R.id.action_exit -> {
                val alert = builder.create()
                alert.show()
            }
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
        val prefs =
            PreferenceManager.getDefaultSharedPreferences(this)
        if (selLocoAddr == INVALID_INT) {
            selLocoAddr = prefs.getInt(KEY_LOCO_ADDR, DEFAULT_LOCO)
            if (DEBUG)
                Log.d(TAG, "loading lastLoco Adr from prefs - addr=$selLocoAddr")
        }
        sendQ.offer("R $selLocoAddr")   // request update of loco data from SXnet
        tvAddr.text = "A=$selLocoAddr"
        MainApplication.addRelevantChan(selLocoAddr)
        //loco_icon!!.setImageBitmap(selLocoAddr.getIcon())

        startSXNetCommunication()
        mHandler.postDelayed({ updateUI() }, 100)
        pauseTimer = false
    }

    override fun onValueChange(picker: NumberPicker, oldVal: Int, newVal: Int) {

        Log.d("address value is", "" + newVal)

    }

    private fun updateUI() {
        mCounter++

        // the actionBar icons are NOT updated via binding, because
        // "At the moment, data binding is only for layout resources, not menu resources" (google)
        // and the implementation to "work around" this limitation looks very complicated, see
        // https://stackoverflow.com/questions/38660735/how-bind-android-databinding-to-menu
        //setConnectionIcon()

        setPowerAndConnectionIcon()
        lampBtn.setON(LocoUtil.isLampOn())
        functionBtn.setON(LocoUtil.isFunctionOn())
        changeDirBtn.setON(LocoUtil.isForward())

        if (selLocoAddr != INVALID_INT) {
            //speedBar.setSXSpeed(sxData[selLocoAddr])
            val speed = (sxData[selLocoAddr] and 0x1f)
            speedBar2.progress = speed
            // Log.d(TAG,"sxD[L]=$speed")
            tvAddr.setText("A = $selLocoAddr")
        } else {
            tvAddr.text = getString(R.string.noLocoSelected)
        }

        channelList.clear()
        for (i in 0..SXMAX) {
            val sx = sxData[i]
            if (relevantChans.contains(i) or (sx != 0)) {
                val s = i.toString().padStart(3, '_')  + "  " +
                        sx.toString().padStart(3, '_') + "   (" + LocoUtil.SXBinaryString(sx) +")"
                channelList.add(s)
            }
        }

        adapter.notifyDataSetChanged() // = ArrayAdapter(this, android.R.layout.simple_list_item_1, channelList)

        //speedbar.sxSpeed = LocoUtil.getSpeed()
        mHandler.postDelayed({ updateUI() }, 500)
    }

    private fun setPowerAndConnectionIcon() {
        if (MainApplication.connectionIsAlive()) {
            mOptionsMenu.findItem(R.id.action_connect)?.setIcon(R.drawable.commok)
            when (globalPower) {
                false -> {
                    mOptionsMenu.findItem(R.id.action_power)?.setIcon(R.drawable.power_red)
                } //power_red)
                true -> {
                    mOptionsMenu.findItem(R.id.action_power)?.setIcon(R.drawable.power_green)
                }
            }
        } else {
            mOptionsMenu.findItem(R.id.action_connect)?.setIcon(R.drawable.nocomm)
            mOptionsMenu.findItem(R.id.action_power)?.setIcon(R.drawable.power_unknown)
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

    private fun shutdownSXClient() {
        Log.d(TAG, "MainActivity - shutting down SXnet Client.")
        client?.shutdown()
        client?.disconnectContext()
        client = null
    }

    private fun startSXNetCommunication() {
        Log.d(TAG, "MainActivity - startSXNetCommunication.")
        if (client != null) {
            client?.shutdown()
            mySleep(100)
        }
        // reset data
        for (i in 0..SXMAX) sxData[i] = 0
        relevantChans.clear()
        relevantChans.add(selLocoAddr)

        val prefs = PreferenceManager
            .getDefaultSharedPreferences(this)
        val ip = prefs.getString(KEY_IP, SXNET_START_IP)

        if (DEBUG) Log.d(TAG, "connecting to " + ip!!)
        client = SXnetClientThread(this, ip!!, SXNET_PORT)
        client?.start()

    }

    private fun togglePower() {
        val prefs = PreferenceManager
            .getDefaultSharedPreferences(this)
        if (prefs.getBoolean(KEY_ALLOW_POWER_CONTROL, false)) {
            if (!globalPower) {
                sendQ.offer("SETPOWER 1")
                globalPower = true
            } else {
                sendQ.offer("SETPOWER 0")
                globalPower = true
            }
        } else {
            toast("Power Kontrolle nicht erlaubt (siehe Settings)")
        }
    }

    private fun mySleep(delayMillis: Int) = try {
        Thread.sleep(delayMillis.toLong()) // give client some time to shut down.
    } catch (e: InterruptedException) {
        Log.e(TAG, "could not sleep...")
    }


    private fun addressPickerDialog() {
        val dialogBuilder = AlertDialog.Builder(this)
        val dialogView = this.layoutInflater.inflate(R.layout.select_address, null)
        val num = dialogView.findViewById<View>(R.id.numberPicker1) as NumberPicker
        num.minValue = 1
        num.maxValue = 99
        num.value = selLocoAddr
        dialogBuilder.setView(dialogView)
        dialogBuilder.setTitle("Lok-Adresse auswählen")
        dialogBuilder.setPositiveButton("Save") { dialog, btn ->
            selLocoAddr = num.value
            MainApplication.addRelevantChan(selLocoAddr)
            sendQ.offer("R $selLocoAddr")
            dialog.cancel()
        }
        dialogBuilder.setNegativeButton("Cancel") { dialog, btn ->
            dialog.cancel()
        }
        val b = dialogBuilder.create()
        b.show()
    }

    companion object {


    }
}
