package de.blankedv.sx4control.views

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
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
import android.view.LayoutInflater
import de.blankedv.sx4control.*
import de.blankedv.sx4control.adapter.ChannelListAdapter
import de.blankedv.sx4control.adapter.SXD
import de.blankedv.sx4control.controls.FunctionButton
import de.blankedv.sx4control.util.LocoUtil
import de.blankedv.sx4control.model.*
import android.widget.TextView
import de.blankedv.sx4control.model.MainApplication.Companion.isUsableSXAddress

class MainActivity : AppCompatActivity(), SeekBar.OnSeekBarChangeListener,
    NumberPicker.OnValueChangeListener {

    private lateinit var exitProgramAlert: AlertDialog.Builder
    private lateinit var loco_icon: ImageView
    // all buttons are of type "FunctionButton" because this seems to render them to the exact same size
    private lateinit var stopBtn: FunctionButton
    private lateinit var lampBtn: FunctionButton
    private lateinit var functionBtn: FunctionButton
    private lateinit var changeDirBtn: FunctionButton
    private lateinit var speedBar2: SeekBar
    private lateinit var spinnerLocoAddress : Spinner
    private lateinit var channelView: RecyclerView
    private lateinit var adapter: ChannelListAdapter
    private lateinit var mOptionsMenu: Menu

    private var mHandler = Handler()  // used for UI Update timer
    private var mCounter = 0

    private var client: SXnetClientThread? = null

    private var sxDataToEdit = INVALID_INT     // used by edit-data-dialog and by onClickCheckbox functions
    var tvData: TextView? = null       // used by edit-data-dialog and by onClickCheckbox functions

    private val channelList = arrayListOf<SXD>()  // list of SX data pairs which gets actually displayed

    private var locoAddressList = ArrayList<Int>(10)
    private var locoAddressStringList = ArrayList<String>(10)
    private lateinit var spinnerArrayAdapter : ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        exitProgramAlert = AlertDialog.Builder(this)

        spinnerLocoAddress = findViewById<View>(R.id.spAddress) as Spinner
        locoAddressStringList = arrayListOf("-?-")   // will be updated in onResume
        spinnerArrayAdapter = ArrayAdapter(this, R.layout.spinner_item, locoAddressStringList)

        loco_icon = findViewById<View>(R.id.loco_icon) as ImageView
        stopBtn = findViewById<View>(R.id.stopBtn) as FunctionButton
        lampBtn = findViewById<View>(R.id.f0) as FunctionButton
        functionBtn = findViewById<View>(R.id.f1) as FunctionButton
        changeDirBtn = findViewById<View>(R.id.changeBtn) as FunctionButton
        speedBar2 = findViewById<View>(R.id.speedBar2) as SeekBar
        speedBar2.setOnSeekBarChangeListener(this)


        channelView = find(R.id.channelView) as RecyclerView
        channelView.layoutManager = GridLayoutManager(this, 2)
        channelView.addItemDecoration(
            // add some space between the 2 columns
            GridSpacingItemDecoration(2, 24, false)
        )

        adapter = ChannelListAdapter(
            channelList,
            object : ChannelListAdapter.OnItemClickListener {
                override fun invoke(sxd: SXD) = editDataDialog(sxd)
            })
        channelView.adapter = adapter

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
            LocoUtil.setSpeed(0)
            speedBar2.progress = 0
        }

        lampBtn.setOnClickListener { LocoUtil.toggleLamp() }
        functionBtn.setOnClickListener { LocoUtil.toggleFunction() }

        exitProgramAlert = AlertDialog.Builder(this)
        exitProgramAlert.setMessage("Programm beenden?")
            .setCancelable(false)
            .setPositiveButton("Ja") { _, _ ->
                shutdownSXClient()
                mySleep(100)
                finish()
            }
            .setNegativeButton("Nein") { dialog, _ -> dialog.cancel() }

        spinnerLocoAddress.adapter = spinnerArrayAdapter

        spinnerLocoAddress.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(p0: AdapterView<*>?) {
                toast("nichts selektiert") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                val sLoco = locoAddressStringList[p2]
                if (sLoco.contains("+")) {
                    startLocoAddressPickerDialog()
                } else {
                    try {
                        val addr = locoAddressStringList[p2].toInt()
                        if (addr != selLocoAddr) {
                            selectNewLoco(addr)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG,"kann $sLoco (spinner) nicht in Adresse umwandeln ")
                        toast("kann $sLoco nicht in Adresse umwandeln ")  // do nothing
                    }
                }
            }

        }

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
            R.id.action_add_channel -> {
                val intent = Intent(this, SelectAddressDialog::class.java)
                intent.action = "monitor"
                startActivityForResult(intent, PICK_OTHER_ADDRESS_REQUEST)
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
                val alert = exitProgramAlert.create()
                alert.show()
            }
        }
        return true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        // Check which request we're responding to
        if (requestCode == PICK_LOCO_ADDRESS_REQUEST) {
            // Make sure the request was successful
            if (resultCode == Activity.RESULT_OK) {
                val addr = data?.getIntExtra(RESULT_SEL_ADDRESS, INVALID_INT)
                if (isUsableSXAddress(addr!!)) {
                    if (addr != selLocoAddr) {
                        Log.d(TAG, "NEW LOCO ADDR=$addr from select-dialog")
                        selectNewLoco(addr)   // TODO check redundancy
                        updateAddressSpinnerList()
                    }
                } else {
                    toast("Fehler Adresse muss kleiner oder gleich $SXMAX_USED sein")
                }
            }
        } else if (requestCode == PICK_OTHER_ADDRESS_REQUEST) {
            // Make sure the request was successful
            if (resultCode == Activity.RESULT_OK) {
                val addr = data?.getIntExtra(RESULT_SEL_ADDRESS, INVALID_INT)
                if (isUsableSXAddress(addr!!))  {
                    Log.d(TAG, "neue Monitor-Adresse $addr from select-dialog")
                    MainApplication.addRelevantChan(addr)
                } else {
                    toast("Fehler Adresse muss kleiner oder gleich $SXMAX_USED sein")
                }
            }
        } else {
            Log.e(TAG,"onActivityResult: other requestCode=$requestCode")
        }
    }
    /** returned by Android after user has given permissions */
    override fun onRequestPermissionsResult(requestCode: Int,
                             permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            READ_STORAGE_PERMISSION_REQUEST -> {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    // finally, we got the permission
                    Log.d(TAG, "read permission granted in the meantime")
                } else {
                    // permission denied, boo! Disable the functionality that depends on this permission.
                    LocoBitmap.readDisabled = true
                    Log.e(TAG, "NO read permission granted, disabling locoBitmap read forever")
                }
                return
            }

// Add other 'when' lines to check for other
// permissions this app might request.
            else -> {
                // Ignore all other requests.
            }
        }
    }


    override fun onPause() {
        super.onPause()
        Log.d(TAG, "MainActivcity - onPause")
        // store current loco data + the complete locoList
        saveLocoAddresses()

        pauseTimer = true
    }

    override fun onResume() {
        super.onResume()
        Log.i(TAG, "MainActivity - onResume")

        getLocoAddresses()
        loadLocoBitmap(selLocoAddr)
        updateAddressSpinnerList()
        spinnerArrayAdapter.notifyDataSetChanged()

        MainApplication.addRelevantChan(selLocoAddr)

        startSXNetCommunication()
        sendQ.offer("R $selLocoAddr")    // sendQ was cleared when starting sxnet-comm

        mHandler.postDelayed({ updateUI() }, 100)
        pauseTimer = false
    }

    override fun onValueChange(picker: NumberPicker, oldVal: Int, newVal: Int) {

        Log.d("address value is", "" + newVal)

    }

    private fun loadLocoBitmap(addr : Int) {
        val fileName = addr.toString() + ".png"
        val icon = LocoBitmap.read(fileName, ctx)

        if (icon == null) {
            val genLocoBitmap = resources.getDrawable(R.drawable.genloco)
            loco_icon.setImageDrawable(genLocoBitmap)
        } else {
            loco_icon.setImageDrawable(icon)
        }
    }

    private fun getSpinnerSelectionFromAddress(a : Int) : Int {
        // search for a matching spinner item
        for (i in 0..(locoAddressStringList.size-1)) {
            if ((locoAddressStringList.get(i)).equals(a.toString())) {
                return i
            }
        }
        return 0 // default
    }


    private fun updateUI() {
        mCounter++

        // the actionBar icons are NOT updated via binding, because
        // "At the moment, data binding is only for layout resources, not menu resources" (google)
        // and the implementation to "work around" this limitation looks very complicated, see
        // https://stackoverflow.com/questions/38660735/how-bind-android-databinding-to-menu

        setPowerAndConnectionIcon()
        lampBtn.setON(LocoUtil.isLampOn())
        functionBtn.setON(LocoUtil.isFunctionOn())
        changeDirBtn.setON(LocoUtil.isForward())

        if (selLocoAddr != INVALID_INT) {
            val speed = (sxData[selLocoAddr] and 0x1f)
            speedBar2.progress = speed
        }

        channelList.clear()
        for (i in 0..SXMAX) {
            val sx = sxData[i]
            if (relevantChans.contains(i) or (sx != 0)) {
                channelList.add(SXD(i, sx))
            }
        }

        adapter!!.notifyDataSetChanged()  // assertion kept in case of rotation during execution of this code

        mHandler.postDelayed({ updateUI() }, 500)
    }

    private fun selectNewLoco(addr : Int) {
        if(!isUsableSXAddress(addr)) return
        selLocoAddr = addr
        loadLocoBitmap(selLocoAddr)
        addNewLocoToAddressList(addr)
        MainApplication.addRelevantChan(selLocoAddr)
        sendQ.offer("R $selLocoAddr")
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

    private fun saveLocoAddresses() {
        Log.d(TAG, "saveLocoAddresses")
        var list = ""
        locoAddressList.sort()
        for (addr in locoAddressList) {
            if (!list.isBlank()) list += ","
            list += addr.toString()
        }
        val prefs =
            PreferenceManager.getDefaultSharedPreferences(this)
        val editor = prefs.edit()
        Log.d(TAG,"save list=$list")
        editor.putString(KEY_LOCO_ADDR_LIST, list)
        Log.d(TAG,"save selLoco=$selLocoAddr")
        editor.putInt(KEY_LOCO_ADDR, selLocoAddr)// last used loco address
        editor.apply()
    }

    private fun getLocoAddresses() {
        val prefs =
            PreferenceManager.getDefaultSharedPreferences(this)
        var addrListString = prefs.getString(KEY_LOCO_ADDR_LIST, "" )
        if (!isUsableSXAddress(selLocoAddr)) {
            selLocoAddr = prefs.getInt(KEY_LOCO_ADDR, 40 )
        }
        if (addrListString.isBlank()) {
            //TODO temp solution, replace by call to select-loco-address-dialog later
            addrListString += selLocoAddr.toString()
        }
        if (DEBUG)
            Log.d(TAG, "getLocoAddresses=$addrListString")

        val addrStrings = addrListString.split(",")
        //convert to Int
        for (s in addrStrings) {
            try {
                val a = s.toInt()
                if (!locoAddressList.contains(a)) {
                    locoAddressList.add(a)
                }
            } catch (e : Exception) {
                // should never happen
                Log.e(TAG,"invalid string in getLocoAddresses s=$s - ${e.message}")
            }
        }

        updateAddressSpinnerList()
    }

    private fun addNewLocoToAddressList(addr : Int) : Boolean {
        Log.d(TAG,"addNewLocoToAddressList a=$addr")
        if (!isUsableSXAddress(addr)) return false
        if (locoAddressList.contains(addr)) return false

        locoAddressList.add(addr)
        locoAddressList.sort()
        saveLocoAddresses()
        updateAddressSpinnerList()
        spinnerArrayAdapter.notifyDataSetChanged()
        return true
    }

    private fun updateAddressSpinnerList() {
        Log.d(TAG,"updateAddressSpinnerList")
        if (locoAddressList.isEmpty()) getLocoAddresses()
        locoAddressStringList.clear()
        for (a in locoAddressList) {
            locoAddressStringList.add(a.toString())
        }
        locoAddressStringList.add("+")  // always last entry: "+ add a new address"
        // update spinner

        val addressPos = getSpinnerSelectionFromAddress(selLocoAddr)
        spinnerLocoAddress.setSelection(addressPos)
        try {
            selLocoAddr = (spinnerLocoAddress.getItemAtPosition(addressPos).toString()).toInt()
        } catch (e : Exception) {
            selLocoAddr = 40
            spinnerLocoAddress.setSelection(getSpinnerSelectionFromAddress(40))  // TODO
            Log.e(TAG, "could not convert spinner item to int value - using 40 as loco address")
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
        sendQ.clear()
        MainApplication.addRelevantChan(selLocoAddr)

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


    private fun startLocoAddressPickerDialog() {

        val intent = Intent(this, SelectAddressDialog::class.java)
        intent.action = "loco"
        startActivityForResult(intent, PICK_LOCO_ADDRESS_REQUEST)

    }


    fun onCheckboxClicked(view: View) {
        var d = sxDataToEdit
        if (view is CheckBox) {
            val ids =
                intArrayOf(
                    R.id.checkBox1, R.id.checkBox2, R.id.checkBox3, R.id.checkBox4,
                    R.id.checkBox5, R.id.checkBox6, R.id.checkBox7, R.id.checkBox8
                )
            val checked: Boolean = view.isChecked
            for (bit in 1..8) {  // "sx"-bits start at 1
                if (view.id == ids.get(bit - 1)) {
                    val bval = 1.shl(bit - 1)
                    when (checked) {
                        true -> {
                            d = d or bval
                        }    // set bit
                        false -> {
                            d = d and bval.inv()
                        }   // clear bit
                    }
                }
            }
            Log.d(TAG, "new data = $d")
            sxDataToEdit = d
            tvData!!.text = sxDataToEdit.toString()
        }


    }

    /** returns list of 8 checkboxes for the 8 SX bits
     *
     */
    private fun checkBoxesList(v: View): List<CheckBox> {
        return listOf(
            v.find(R.id.checkBox1) as CheckBox,   // bit 1
            v.find(R.id.checkBox2) as CheckBox,
            v.find(R.id.checkBox3) as CheckBox,
            v.find(R.id.checkBox4) as CheckBox,
            v.find(R.id.checkBox5) as CheckBox,
            v.find(R.id.checkBox6) as CheckBox,
            v.find(R.id.checkBox7) as CheckBox,
            v.find(R.id.checkBox8) as CheckBox)   // bit 8
    }

    /** edit a single byte in sxData array and send update to SXnet
     *
     */
    fun editDataDialog(sxd: SXD) {

        val selAddress = sxd.sx
        sxDataToEdit = sxd.data
        val editDataView =
            LayoutInflater.from(ctx).inflate(R.layout.sxdata_dialog, null)

        val tvEditAddr = editDataView.find(R.id.tvEditAddress) as TextView
        tvData = editDataView.find(R.id.tvEditData) as TextView

        var cb = checkBoxesList(editDataView)

        tvEditAddr.text = "Addr=$selAddress D="
        tvData!!.text = sxDataToEdit.toString()

        for (bit in 1..8) {
            if (sxDataToEdit and (1 shl (bit - 1)) != 0) cb.elementAt(bit - 1).setChecked(true)
        }

        val editDialog = android.app.AlertDialog.Builder(ctx)
            .setMessage("Daten verändern?")
            .setCancelable(false)
            .setView(editDataView)
            .setPositiveButton("Speichern") { dialog, id ->
                sxData[selAddress] = sxDataToEdit
                sendQ.offer("S $selAddress $sxDataToEdit")
                Log.d(TAG, "setting a=$selAddress to d=$sxDataToEdit")
                dialog.dismiss()
            }
            .setNegativeButton("Zurück") { dialog, id ->
                dialog.dismiss()
            }
            .create()
        editDialog.show()


    }


    companion object {


    }
}
