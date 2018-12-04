package de.blankedv.sx4control.views


import android.app.Activity
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.PackageManager.NameNotFoundException
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView


import de.blankedv.sx4control.model.MainApplication.Companion.connString
import de.blankedv.sx4control.R


class AboutActivity : Activity() {

    private var cancel: Button? = null
    private var vinfo = ""
    private var versTv: TextView? = null
    private var connTo: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.about)
        versTv = findViewById<View>(R.id.version) as TextView
        connTo = findViewById<View>(R.id.connected_to) as TextView


        var vName = ""

        val pInfo: PackageInfo
        try {
            pInfo = packageManager.getPackageInfo(packageName, PackageManager.GET_META_DATA)

            vName = pInfo.versionName
            vinfo = "Version: $vName "

        } catch (e: NameNotFoundException) {
            e.printStackTrace()
        }

        versTv!!.text = vinfo

        if (connString.length > 0) {
            connTo!!.text = "connected to: $connString"
        } else {
            connTo!!.text = "currently not connected to any SXnet server"
        }

        cancel = findViewById<View>(R.id.cancel) as Button

        cancel!!.setOnClickListener { finish() }

    }

}