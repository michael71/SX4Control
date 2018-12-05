package de.blankedv.sx4control.model

import android.annotation.SuppressLint
import android.app.Application
import android.os.Handler
import android.os.Message
import android.preference.PreferenceManager
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.TextView
import org.jetbrains.anko.toast
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue


// TODO: handle absence ot connection to command station

/* holds all data which need to be persistent during configuration changes
 * like sxData, globalPower and selLocoAddr
 */
class MainApplication : Application() {



    //@SuppressLint("HandlerLeak")
    @SuppressLint("HandlerLeak")
    override fun onCreate() {
        super.onCreate()
        if (DEBUG)
            Log.d(TAG, "onCreate MainApplication")

        val myAndroidDeviceId =
            Settings.Secure.getString(applicationContext.contentResolver, Settings.Secure.ANDROID_ID)

        Log.d(TAG, "MainApplication - androidDeviceID=$myAndroidDeviceId")
        // scaling, zoom prefs are loaded from LanbahnPanelActivity


        // handler for receiving sxnet/loconet messages
        // this must be done in the "Application" (not activity) to keep track of changes
        // during other activities

        handler = object : Handler() {
            override fun handleMessage(msg: Message) {
                val what = msg.what
                val chan = msg.arg1
                val data = msg.arg2

                when (what) {
                    TYPE_POWER_MSG -> {
                        globalPower = (data != 0)
                        //Log.d(TAG, "rec gPower=$data")
                        timeOfLastReceivedMessage = System.currentTimeMillis()
                    }
                    TYPE_CONNECTION_MSG -> {
                        cmdStationConnected = (data != 0)
                        Log.d(TAG, "rec cmdStatConn=$data")
                    }

                    TYPE_SX_MSG -> {
                        sxData[chan] = data
                        if (data != 0) addRelevantChan(chan)
                        //Log.d(TAG, "rec sxMsg a=$chan d=$data")
                        timeOfLastReceivedMessage = System.currentTimeMillis()
                    }

                    /*  TYPE_SHUTDOWN_MSG -> {
                        if (DEBUG) Log.d(TAG, "client thread disconnecting")
                        toast("no response, disconnecting")
                        client = null
                    } */

                    TYPE_ERROR_MSG -> {
                        if (DEBUG) Log.d(TAG, "error msg $chan $data")
                        if (msg.obj != null) {
                            toast(msg.obj.toString())
                        }
                    }

                }

            }

        }
    }

    override fun onTerminate() {
        super.onTerminate()
        Log.d(TAG, "LanbahnPanelApp - terminating.")

    }

      /**
     * Display OnGoing Notification that indicates Network Thread is still Running.
     * Currently called from LanbahnPanelActivity onPause, passing the current intent
     * to return to when reopening.
     */
    /* TODO   internal fun addNotification(notificationIntent: Intent) {
           val channelId = getString(R.string.default_notification_channel_id)
           val builder = NotificationCompat.Builder(this, channelId)
                   .setSmallIcon(R.drawable.lb_icon)
                   .setContentTitle(this.getString(R.string.notification_title))
                   .setContentText(this.getString(R.string.notification_text))
                   .setOngoing(true)
                   .setPriority(NotificationCompat.PRIORITY_DEFAULT)

           val contentIntent = PendingIntent.getActivity(this, LBP_NOTIFICATION_ID, notificationIntent,
                   PendingIntent.FLAG_CANCEL_CURRENT)
           builder.setContentIntent(conteMainApplication.addRelevantChan(selLocoAddr)ntIntent)

           // Add as notification
           val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

           // Since android Oreo notification channel is needed.
           if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
               val channel = NotificationChannel(channelId,
                       "Lanbahn Channel",
                       NotificationManager.IMPORTANCE_DEFAULT)
               manager.createNotificationChannel(channel)
           }

           manager.notify(LBP_NOTIFICATION_ID, builder.build())
       }

       // Remove notification
       internal fun removeNotification() {
           val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
           manager.cancel(LBP_NOTIFICATION_ID)
       }  */

    companion object {

        lateinit var handler: Handler // used for communication from RRConnection Thread to UI (application)
        var connString = ""
        var cmdStationConnected = false
        var pauseTimer = false

        val sendQ: BlockingQueue<String> = ArrayBlockingQueue(500)

        @Volatile
        var relevantChans = mutableListOf<Int>()
        @Volatile
        var globalPower = false
        @Volatile
        var sxData = IntArray(SXMAX + 1)
        @Volatile
        var selLocoAddr = INVALID_INT

        var timeOfLastReceivedMessage = 0L

        fun connectionIsAlive() : Boolean {
            return ( (System.currentTimeMillis() - timeOfLastReceivedMessage) < 5000)
        }

        fun addRelevantChan ( chan  : Int) {
            if (!relevantChans.contains(chan)) {
                relevantChans.add(chan)
            }
        }

          fun isUsableSXAddress(a : Int) : Boolean {
              return ((a > 0) and (a <= SXMAX_USED))
          }

    }
}

