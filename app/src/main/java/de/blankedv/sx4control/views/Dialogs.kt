package de.blankedv.sx4control.views

import android.app.AlertDialog
import android.view.LayoutInflater

import android.content.Context
import android.util.Log
import android.view.View
import android.widget.*
import de.blankedv.sx4control.R
import de.blankedv.sx4control.adapter.SXD

import de.blankedv.sx4control.model.MainApplication.Companion.sendQ
import de.blankedv.sx4control.model.MainApplication.Companion.sxData
import de.blankedv.sx4control.model.MainApplication.Companion.sxDataToEdit
import de.blankedv.sx4control.model.TAG
import org.jetbrains.anko.*


/**
 * predefined dialogs
 *
 * TODO review kotlin code
 */
object Dialogs {

    fun openEditSXDataDialog(sxd : SXD, ctx : Context) {

        val selAddress = sxd.sx
        sxDataToEdit = sxd.data
        val editDataView =
            LayoutInflater.from(ctx).inflate(R.layout.sxdata_dialog, null)

        val tvAddr = editDataView.find(R.id.tvEditAddress) as TextView
        val tvData = editDataView.find(R.id.tvEditData) as TextView

        var cb1 = editDataView.find(R.id.checkBox1) as CheckBox   // bit 1
        var cb2 = editDataView.find(R.id.checkBox2) as CheckBox
        var cb3 = editDataView.find(R.id.checkBox3) as CheckBox
        var cb4 = editDataView.find(R.id.checkBox4) as CheckBox
        var cb5 = editDataView.find(R.id.checkBox5) as CheckBox
        var cb6 = editDataView.find(R.id.checkBox6) as CheckBox
        var cb7 = editDataView.find(R.id.checkBox7) as CheckBox
        var cb8 = editDataView.find(R.id.checkBox8) as CheckBox   // bit 8

        tvAddr.text = "Addr=$selAddress D="
        tvData.text = sxDataToEdit.toString()

        if ((sxDataToEdit and 0x01) != 0) cb1.setChecked(true)
        if ((sxDataToEdit and 0x02) != 0) cb2.setChecked(true)
        if ((sxDataToEdit and 0x04) != 0) cb3.setChecked(true)
        if ((sxDataToEdit and 0x08) != 0) cb4.setChecked(true)
        if ((sxDataToEdit and 0x10) != 0) cb5.setChecked(true)
        if ((sxDataToEdit and 0x20) != 0) cb6.setChecked(true)
        if ((sxDataToEdit and 0x40) != 0) cb7.setChecked(true)
        if ((sxDataToEdit and 0x80) != 0) cb8.setChecked(true)



        val editDialog = AlertDialog.Builder(ctx)
            //.setMessage("")
            .setCancelable(false)
            .setView(editDataView)
            .setPositiveButton("Speichern") { dialog, id ->
                sxData[selAddress] = sxDataToEdit
                ctx.toast("setting adr=$selAddress to d=$sxDataToEdit")
                sendQ.offer("S $selAddress $sxDataToEdit")
                dialog.dismiss()
            }
            .setNegativeButton("Zurück") { dialog, id ->
                ctx.toast("changes cancelled")
                dialog.dismiss()
            }
            .create()
        editDialog.show()
    }

    fun onCheckboxClicked(view: View, data : Int) : Int {
        var d = data
        if (view is CheckBox) {
            val checked: Boolean = view.isChecked

            when (view.id) {
                R.id.checkBox1 -> {
                    if (checked) {
                        d = d or 0x01
                    } else {
                        d = d and 0x01.inv()
                    }
                }
                R.id.checkBox2 -> {
                    if (checked) {
                        d = d or 0x02
                    } else {
                        d = d and 0x02.inv()
                    }
                }
                R.id.checkBox3 -> {
                    if (checked) {
                        d = d or 0x04
                    } else {
                        d = d and 0x04.inv()
                    }
                }
                R.id.checkBox4 -> {
                    if (checked) {
                        d = d or 0x08
                    } else {
                        d = d and 0x08.inv()
                    }
                }
                R.id.checkBox5 -> {
                    if (checked) {
                        d = d or 0x10
                    } else {
                        d = d and 0x10.inv()
                    }
                }
                R.id.checkBox6 -> {
                    if (checked) {
                        d = d or 0x20
                    } else {
                        d = d and 0x20.inv()
                    }
                }
                R.id.checkBox7 -> {
                    if (checked) {
                        d = d or 0x40
                    } else {
                        d = d and 0x40.inv()
                    }
                }
                R.id.checkBox8 -> {
                    if (checked) {
                        d = d or 0x80
                    } else {
                        d = d and 0x80.inv()
                    }
                }
            }
            Log.d(TAG, "new data = $d")
        }

        return d
    }
/*    internal fun selectAddressDialog(el: PanelElement) {

        val factory = LayoutInflater.from(appContext)
        val selAddressView = factory.inflate(
                R.layout.alert_dialog_sel_address, null)

        val tvAdr2 = selAddressView
                .findViewById<View>(R.id.tvAddress2) as TextView
        val tvInv2 = selAddressViewimport de.blankedv.s.elements.SignalElement
import de.blankedv.lanbahnpanel.model.*
                .findViewById<View>(R.id.tvInverted2) as TextView
        val tvInv = selAddressView
                .findViewById<View>(R.id.tvInverted) as TextView
        val address = selAddressView
                .findViewById<View>(R.id.picker1) as NumberPicker
        val inverted = selAddressView.findViewById<View>(R.id.cbInverted) as CheckBox

        val address2 = selAddressView
                .findViewById<View>(R.id.picker2) as NumberPicker
        val inverted2 = selAddressView.findViewById<View>(R.id.cbInverted2) as CheckBox

        address.minValue = MIN_ADDR
        address.maxValue = MAX_ADDR

        address2.minValue = MIN_ADDR
        address2.maxValue = MAX_ADDR

        address.setOnLongPressUpdateInterval(100) // faster change for long press
        val e = el as ActivePanelElement
        if (e.adr2 != INVALID_INT) {
            address2.visibility = View.VISIBLE
            inverted2.visibility = View.VISIBLE
            tvAdr2.visibility = View.VISIBLE
            tvInv2.visibility = View.VISIBLE

        } else {   // hide second address selection if there is not second address for this PanelElement
            address2.visibility = View.GONE
            inverted2.visibility = View.GONE
            tvAdr2.visibility = View.GONE
            tvInv2.visibility = View.GONE
        }
        if (e is SignalElement) {
            tvInv.visibility = View.GONE
            inverted.visibility = View.GONE
        } else {
            tvInv.visibility = View.VISIBLE
            inverted.visibility = View.VISIBLE
        }
        val msg: String
        address.value = e.adr
        address2.value = e.adr2
        inverted.isChecked = (e.invert == DISP_INVERTED)
        inverted2.isChecked = (e.invert2 == DISP_INVERTED)
        val res = appContext?.resources
        msg = res!!.getString(R.string.address) + "?"
        val addrDialog = AlertDialog.Builder(appContext)
                .setMessage(msg)
                .setCancelable(false)
                .setView(selAddressView)
                .setPositiveButton(res.getString(R.string.save)
                ) { dialog, id ->
                    // Toast.makeText(appContext,"Adresse "+sxAddress.getCurrent()
                    // +"/"+sxBit.getCurrent()+" wurde selektiert",
                    // Toast.LENGTH_SHORT)
                    // .show();
                    e.adr = address.value
                    if (inverted.isChecked) {
                        e.invert = DISP_INVERTED
                    } else {
                        e.invert = DISP_STANDARD
                    }
                    if (e.adr2 != INVALID_INT) {
                        e.adr2 = address2.value
                        if (inverted2.isChecked) {
                            e.invert2 = DISP_INVERTED
                        } else {
                            e.invert2 = DISP_STANDARD
                        }
                    }
                    configHasChanged = true // flag for saving the
                    // configuration
                    // later when
                    // pausing the
                    // activity
                    dialog.dismiss()
                }
                .setNegativeButton(res.getString(R.string.back)
                ) { dialog, id ->
                    // dialog.cancel();
                    dialog.dismiss()
                }.create()
        addrDialog.show()
        if (e.adr2 != INVALID_INT) {
            addrDialog.window!!.setLayout(700, 400)
        } else {
            addrDialog.window!!.setLayout(350, 400)
        }
    }

    fun selectLocoDialog() {

        val factory = LayoutInflater.from(appContext)
        val selSxAddressView = factory.inflate(R.layout.alert_dialog_sel_loco_from_list, null)
        val selLoco = selSxAddressView.findViewById(R.id.spinner) as Spinner

        val locosToSelect = arrayOfNulls<String>(locolist?.size + 1)

        var index = 0
        var selection = 0
        for (l in locolist) {
            locosToSelect[index] = l.name + " (" + l.adr + ")"
            if (l == selectedLoco) {
                selection = index
            }
            index++
        }
        locosToSelect[index] = NEW_LOCO_NAME
        val NEW_LOCO = index

        val adapter = ArrayAdapter<String>(appContext,
                android.R.layout.simple_spinner_dropdown_item,
                locosToSelect)

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        selLoco.adapter = adapter
        selLoco.setSelection(selection)

        selLocoIndex = NOTHING
        selLoco.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(arg0: AdapterView<*>, arg1: View,
                                        arg2: Int, arg3: Long) {
                selLocoIndex = arg2   // save for later use when "SAVE" pressed
            }

            override fun onNothingSelected(arg0: AdapterView<*>) {
                selLocoIndex = NOTHING
            }
        }

        /*selLoco.setOnItemLongClickListener( new AdapterView.OnItemLongClickListener() {
            public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
                                   int arg2, long arg3) {
                int removeLocoIndex = arg2;
                //locolist.remove(removeLocoIndex);
                Log.d(TAG,"remove index "+removeLocoIndex);
                return true;
            }
        }); funktioniert nicht wie erwartet */

        val res = appContext?.resources

        val sxDialog = AlertDialog.Builder(appContext)
                //, R.style.Animations_GrowFromBottom ) => does  not work
                //.setMessage("Lok auswählen - "+locolist.name)
                .setCancelable(true)
                .setView(selSxAddressView)
                .setPositiveButton(res!!.getString(R.string.select), { dialog, id ->
                    if (selLocoIndex == NEW_LOCO) {
                        dialog.dismiss()
                        val l = Loco(NEW_LOCO_NAME)
                        locolist.add(l)
                        selectedLoco = l
                        openEditDialog()
                    } else if (selLocoIndex != NOTHING) {
                        //Toast.makeText(appContext,"Loco-index="+selLocoIndex
                        //		+" wurde selektiert", Toast.LENGTH_SHORT)
                        //		.show();
                        selectedLoco = locolist.get(selLocoIndex)
                        selectedLoco?.initFromSX()
                        dialog.dismiss()
                    }
                })
                .setNeutralButton(res!!.getString(R.string.edit), { dialog, id ->
                    if (selLocoIndex == NEW_LOCO) {
                        val l = Loco(NEW_LOCO_NAME)
                        locolist.add(l)
                        selectedLoco = l
                    } else if (selLocoIndex != NOTHING) {
                        //Toast.makeText(appContext,"Loco-index="+selLocoIndex
                        //		+" wurde selektiert", Toast.LENGTH_SHORT)
                        //		.show();
                        selectedLoco = locolist.get(selLocoIndex)
                    }
                    dialog.dismiss()
                    openEditDialog()
                })
                .setNegativeButton("Zurück", DialogInterface.OnClickListener { dialog, id ->
                    //dialog.cancel();
                })

                .show()
    }
*/

   /*
    fun openDeleteDialog(index: Int) {

        Log.d(TAG, "lok löschen Ja/nein $index")

        val delLoco = locolist.get(index)

        val deleteDialog = AlertDialog.Builder(appContext)
                .setMessage("Lok " + delLoco.name + " wirklich löschen")
                .setCancelable(false)
                .setPositiveButton("Löschen") { dialog, id ->
                    //e.setSxAdr(sxAddress.getValue());
                    //e.setSxBit(sxBit.getValue());

                    locolist.remove(delLoco)
                    configHasChanged = true // flag for saving the configuration later when pausing the activity
                    if (locolist?.size >= 1) {
                        selectedLoco = locolist.get(0)
                        selectedLoco?.initFromSX()
                    } else {
                        selectedLoco = Loco()
                        locolist.add(selectedLoco!!) // at least 1 loco should be in the list
                    }

                    dialog.dismiss()
                }

                .setNegativeButton("Zurück") { dialog, id ->
                    //dialog.cancel();
                    dialog.dismiss()
                }
                .create()
        deleteDialog.show()
        return
    }


*/

}
