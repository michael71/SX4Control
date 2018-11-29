package de.blankedv.sx4control


import android.app.Activity
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.PackageManager.NameNotFoundException
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView


import de.blankedv.sx4control.MainActivity.Companion.connString


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

        var version = -1
        var vName = ""

        val pInfo: PackageInfo
        try {
            pInfo = packageManager.getPackageInfo(packageName, PackageManager.GET_META_DATA)
            version = pInfo.versionCode
            vName = pInfo.versionName
            vinfo = "Version: $vName  ($version)"

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

        //		upload = (Button)findViewById(R.id.upload);

        //		if ((DEBUG) && (debugFileEnabled) ){
        //			upload.setVisibility(View.VISIBLE);
        //
        //			upload.setOnClickListener(new View.OnClickListener() {
        //				public void onClick(View v) {
        //					app.uploadDebugFile();
        //				}
        //
        //			});
        //		}
        //		else  {
        //			// don't show upload button when there is no debug log file.
        //			upload.setVisibility(View.GONE);
        //		}
    }

}