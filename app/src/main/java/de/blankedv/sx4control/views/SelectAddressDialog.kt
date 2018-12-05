package de.blankedv.sx4control.views

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.NumberPicker
import de.blankedv.sx4control.R
import de.blankedv.sx4control.model.*
import kotlinx.android.synthetic.main.activity_select_address_dialog.*
import org.jetbrains.anko.find
import org.jetbrains.anko.toast


class SelectAddressDialog : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_address_dialog)

        val whatType = intent.action
        Log.d(TAG,"type of dialog = "+whatType)

        if (whatType.contains("loco")) {
            tvHeader.text= "Neue Lok-Adresse auswählen?"
        } else {
            tvHeader.text= "Neue Monitor-Adresse auswählen?"
        }

        val numE = find(R.id.numberPickerE) as NumberPicker
        val numZ = find(R.id.numberPickerZ) as NumberPicker
        val numH = find(R.id.numberPickerH) as NumberPicker
        numE.minValue = 0
        numE.maxValue = 9
        numZ.minValue = 0
        numZ.maxValue = 9
        numH.minValue = 0
        numH.maxValue = 1

        // starting with address 40
        numE.value = 0
        numZ.value = 4
        numH.value = 0

        btnSave.setOnClickListener {
            val a = numE.value + 10 * numZ.value + 100 * numH.value
            val result = Intent()
            result.putExtra(RESULT_SEL_ADDRESS, a)
            setResult(Activity.RESULT_OK, result)
            finish()
        }

        btnCancel.setOnClickListener {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }
    }

}
