package de.blankedv.sx4control

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity;
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast

import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    lateinit var builder: AlertDialog.Builder
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        /* fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
        } */

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
    companion object {

        lateinit var appHandler: Handler // used for communication from RRConnection Thread to UI (application)
        // lateinit var pSett: PanelSettings
        var connString = ""
    }
}
