package de.blankedv.sx4control.views

import android.app.Activity

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.preference.PreferenceManager
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView
import android.util.DisplayMetrics
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import de.blankedv.sx4control.model.MainApplication.Companion.relevantChans
import de.blankedv.sx4control.model.MainApplication.Companion.globalPower
import de.blankedv.sx4control.model.MainApplication.Companion.pauseTimer
import de.blankedv.sx4control.model.MainApplication.Companion.selLocoAddr
import de.blankedv.sx4control.model.MainApplication.Companion.sendQ
import de.blankedv.sx4control.model.MainApplication.Companion.sxData
import org.jetbrains.anko.*
import android.support.v7.widget.GridLayoutManager
import de.blankedv.sx4control.*
import de.blankedv.sx4control.adapter.ChannelListAdapter
import de.blankedv.sx4control.adapter.SXD
import de.blankedv.sx4control.controls.FunctionButton
import de.blankedv.sx4control.util.LocoUtil
import de.blankedv.sx4control.model.*
import de.blankedv.sx4control.model.MainApplication.Companion.selSXData
import de.blankedv.sx4control.views.Dialogs.openEditSXDataDialog


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

    private lateinit var channelView: RecyclerView
    private lateinit var adapter : ChannelListAdapter
    private lateinit var mOptionsMenu: Menu

    private var mToast: Toast? = null
    private var mHandler = Handler()  // used for UI Update timer
    private var mCounter = 0

    private var client: SXnetClientThread? = null

    //private lateinit var sxSelectDialog : SelectSXDataDialog


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

        channelView = find(R.id.channelView) as RecyclerView
        // add some space between the 2 columns
        val spacing = 24 // px
        val includeEdge = false
        channelView.addItemDecoration(
            GridSpacingItemDecoration(
                2,
                spacing,
                includeEdge
            )
        )
        channelView.layoutManager = GridLayoutManager(this, 2)

        adapter =
                ChannelListAdapter(
                    channelList,
                    object : ChannelListAdapter.OnItemClickListener {
                        override fun invoke(sxd: SXD) {
                            toast(sxd.sx.toString())
                            openEditSXDataDialog(sxd , ctx)
                        }
                    })
        channelView.adapter = adapter

        tvAddr.text = "A = $selLocoAddr"
        changeDirBtn.imageOff = BitmapFactory.decodeResource(getResources(), R.drawable.left3);
        changeDirBtn.imageOn = BitmapFactory.decodeResource(getResources(), R.drawable.right3);
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
       // adapter =
        //        ChannelListAdapter(channelList, private val channelList = mutableListOf<String>("")
       // channelView.adapter = adapter
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
            selLocoAddr = prefs.getInt(
                KEY_LOCO_ADDR,
                DEFAULT_LOCO
            )
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

    // assertion kept in case of rotation during execution of this code
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
                channelList.add(
                    SXD(
                        i,
                        sx
                    )
                )
            }
        }

        adapter!!.notifyDataSetChanged()

        mHandler.postDelayed({ updateUI() }, 500)
    }

    // assertion kept in case of rotation during execution of this code
    private fun setPowerAndConnectionIcon() {
          if (MainApplication.connectionIsAlive()) {
            mOptionsMenu!!.findItem(R.id.action_connect)?.setIcon(R.drawable.commok)
            when (globalPower) {
                false -> {
                    mOptionsMenu!!.findItem(R.id.action_power)?.setIcon(R.drawable.power_red)
                } //power_red)
                true -> {
                    mOptionsMenu!!.findItem(R.id.action_power)?.setIcon(R.drawable.power_green)
                }
            }
        } else {
            mOptionsMenu!!.findItem(R.id.action_connect)?.setIcon(R.drawable.nocomm)
            mOptionsMenu!!.findItem(R.id.action_power)?.setIcon(R.drawable.power_unknown)
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

    fun onCheckboxClicked(view: View) {
        if (view is CheckBox) {
            val checked: Boolean = view.isChecked

            when (view.id) {
                R.id.checkBox1 -> {
                    if (checked) {
                        selSXData = selSXData or 0x01
                    } else {
                        selSXData = selSXData and 0x01.inv()
                    }
                 }
                R.id.checkBox2 -> {
                    if (checked) {
                        selSXData = selSXData or 0x02
                    } else {
                        selSXData = selSXData and 0x02.inv()
                    }
                }
                R.id.checkBox3 -> {
                    if (checked) {
                        selSXData = selSXData or 0x04
                    } else {
                        selSXData = selSXData and 0x04.inv()
                    }
                }
                R.id.checkBox4 -> {
                    if (checked) {
                        selSXData = selSXData or 0x08
                    } else {
                        selSXData = selSXData and 0x08.inv()
                    }
                }
                R.id.checkBox5 -> {
                    if (checked) {
                        selSXData = selSXData or 0x10
                    } else {
                        selSXData = selSXData and 0x10.inv()
                    }
                }
                R.id.checkBox6 -> {
                    if (checked) {
                        selSXData = selSXData or 0x20
                    } else {
                        selSXData = selSXData and 0x20.inv()
                    }
                }
                R.id.checkBox7 -> {
                    if (checked) {
                        selSXData = selSXData or 0x40
                    } else {
                        selSXData = selSXData and 0x40.inv()
                    }
                }
                R.id.checkBox8 -> {
                    if (checked) {
                        selSXData = selSXData or 0x80
                    } else {
                        selSXData = selSXData and 0x80.inv()
                    }
                }
            }
            Log.d(TAG,"new selSXData = $selSXData")
        }
    }
    companion object {

        val channelList = arrayListOf<SXD>()
    }
}