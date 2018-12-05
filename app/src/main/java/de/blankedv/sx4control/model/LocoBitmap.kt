package de.blankedv.sx4control.model

import android.Manifest
import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Environment
import android.util.Log
import android.widget.Toast
import java.io.File
import java.lang.Exception
import android.content.pm.PackageManager
import android.Manifest.permission
import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.os.Build
import android.app.Activity
import android.support.v4.app.ActivityCompat




class LocoBitmap {

    companion object {
        fun read(fName: String, context: Context): Drawable? {

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
                val result = context.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                if (result != PackageManager.PERMISSION_GRANTED)
                    Log.e(TAG, "external storage: NO READ PERMISSION")
                try {
                    ActivityCompat.requestPermissions(
                        context as Activity, arrayOf(
                            Manifest.permission.READ_EXTERNAL_STORAGE),
                        READ_STORAGE_PERMISSION_REQUEST
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "${e.message}")
                }

                return null
            }

            try {

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