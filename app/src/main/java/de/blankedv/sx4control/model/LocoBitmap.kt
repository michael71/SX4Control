package de.blankedv.sx4control.model

import android.Manifest
import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Environment
import android.util.Log
import android.widget.Toast
import java.io.File
import java.lang.Exception
import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.os.Build
import android.app.Activity
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.support.v4.app.ActivityCompat


class LocoBitmap {

    companion object {

        var readDisabled = false

        fun read(fName: String, context: Context): Drawable? {

            // this functionality depends on the read permission for the external storage
            if (readDisabled) {
                Log.d(TAG, "LocoBitmap.read disabled permanently, no permission")
                return null
            }


            Log.d(TAG, "LocoBitmap.read fName=$fName")

            if (Environment.getExternalStorageState() != Environment.MEDIA_MOUNTED) {
                // We cannot read/write the media
                Log.e(TAG, "external storage not available or not writeable")
                Toast.makeText(
                    context, "ERROR:External storage not readable",
                    Toast.LENGTH_LONG
                ).show()
                return null
            }

            // check read permission
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val result = context.checkSelfPermission(READ_EXTERNAL_STORAGE)
                if (result != PERMISSION_GRANTED) {
                    Log.e(TAG, "external storage: NO READ PERMISSION")
                    try {
                        ActivityCompat.requestPermissions(
                            context as Activity, arrayOf(
                                READ_EXTERNAL_STORAGE
                            ),
                            READ_STORAGE_PERMISSION_REQUEST
                        )
                        Log.e(TAG, "external storage:  requesting read permission")
                    } catch (e: Exception) {
                        Log.e(TAG, "${e.message} when requesting read permission\")")
                    }
                    return null
                }


            }

            try {
                Log.d(TAG, "permission ok - reading bitmap")
                // auf dem Nexus 7 unter /mnt/shell/emulated/0/lanbahnpanel
                //ImageView MyImageView = (ImageView)findViewById(R.id.imageView1);
                var locoDrawable = Drawable.createFromPath(
                    Environment.getExternalStorageDirectory().toString()
                            + DIRECTORY + fName
                )
                return locoDrawable
            } catch (e: Exception) {
                Log.e(TAG, "reading bitmap failed e=${e.message}")
                return null
            }

        }
    }


}