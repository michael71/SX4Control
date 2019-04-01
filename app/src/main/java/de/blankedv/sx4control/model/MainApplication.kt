package de.blankedv.sx4control.model

import android.annotation.SuppressLint
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Message
import android.os.SystemClock
import android.preference.PreferenceManager
import android.provider.Settings
import android.support.v4.app.NotificationCompat
import android.util.Log
import de.blankedv.sx4control.R
import org.jetbrains.anko.longToast
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue


// TODO: handle absence ot connection to command station

/* holds all data which need to be persistent during configuration changes
 * like sxData, globalPower and selLocoAddr
 */
class MainApplication : Application() {



    private var client: SXnetClientThread? = null

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
                        lastSXMessageFromClient = SystemClock.elapsedRealtime()
                    }
                    TYPE_CONNECTION_MSG -> {
                        cmdStationConnected = (data != 0)
                        Log.d(TAG, "rec cmdStatConn=$data")
                        lastSXMessageFromClient = SystemClock.elapsedRealtime()
                    }

                    TYPE_SX_MSG -> {
                        if ((chan != selLocoAddr) or
                            ((SystemClock.currentThreadTimeMillis() - waitForLocoFeedback) < 500)  ){
                            // ignore loco feedback, if we are controlling the loco - except for the first
                            // feedback message
                            sxData[chan] = data
                            if (data != 0) addRelevantChan(chan)
                            //Log.d(TAG, "rec sxMsg a=$chan d=$data")
                        } else {
                            Log.d(TAG,"rec loco msg ignored.")
                        }
                        lastSXMessageFromClient = SystemClock.elapsedRealtime()
                    }

                    /*  TYPE_SHUTDOWN_MSG -> {
                        if (DEBUG) Log.d(TAG, "client thread disconnecting")
                        toast("no response, disconnecting")
                        client = null
                    } */

                    TYPE_ERROR_MSG -> {
                        if (DEBUG) Log.d(TAG, "error msg $chan $data")
                        if (msg.obj != null) {
                            longToast(msg.obj.toString())
                        }
                    }

                    TYPE_RESTART_COMM_REQUEST -> {
                        startSXnet()
                    }

                    TYPE_FINISH_COMM_REQUEST -> {
                        shutdownSXNetClient()
                    }
                }

            }

        }
    }

    override fun onTerminate() {
        super.onTerminate()
        Log.d(TAG, "MainApplication - terminating.")

    }

    fun startSXnet() {
        if (!isConnectionAlive()) {
            startSXNetCommunication()
        } else {
            Log.d(TAG, "client is still connected. no restart")
        }
    }

    private fun shutdownSXNetClient() {
        Log.d(TAG, "MainApplication - shutting down SXnet Client.")
        client?.shutdown()
        client?.disconnectContext()
        client = null
    }

    private fun startSXNetCommunication() {
        Log.d(TAG, "MainApplication - (re-)startSXNetCommunication.")
        if ((client != null) && !isConnectionAlive()) {
            Log.d(TAG, "client still seems to be existing - but does not react => shutdown and restart")
            shutdownSXNetClient()
            SystemClock.sleep(100)
        }
        // reset data
        for (i in 0..SXMAX) sxData[i] = 0
        sendQ.clear()

        addRelevantChan(selLocoAddr)

        val prefs = PreferenceManager
            .getDefaultSharedPreferences(this)
        val ip = prefs.getString(KEY_IP, SXNET_START_IP)

        if (DEBUG) Log.d(TAG, "connecting to " + ip!!)
        client = SXnetClientThread(this, ip!!, SXNET_PORT)
        client?.start()

    }

    private fun isConnectionAlive() : Boolean {
        return ((SystemClock.elapsedRealtime() - lastSXMessageFromClient) < 10 * 1000)
    }

    /**
     * Display OnGoing Notification that indicates Network Thread is still Running.
     * Currently called from LanbahnPanelActivity onPause, passing the current intent
     * to return to when reopening.
     */
    internal fun addNotification(notificationIntent: Intent) {
           val channelId = getString(R.string.default_notification_channel_id)
           val builder = NotificationCompat.Builder(this, channelId)
                   .setSmallIcon(R.drawable.sx4_noti_icon)
                   .setContentTitle(this.getString(R.string.notification_title))
                   .setContentText(this.getString(R.string.notification_text))
                   .setOngoing(true)
                   .setPriority(NotificationCompat.PRIORITY_DEFAULT)

           val contentIntent = PendingIntent.getActivity(this, LBP_NOTIFICATION_ID, notificationIntent,
                   PendingIntent.FLAG_CANCEL_CURRENT)
           builder.setContentIntent(contentIntent)

           // Add as notification
           val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

           // Since android Oreo notification channel is needed.
           if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
               val channel = NotificationChannel(channelId,
                       "SX4 Channel",
                       NotificationManager.IMPORTANCE_DEFAULT)
               manager.createNotificationChannel(channel)
           }

           manager.notify(LBP_NOTIFICATION_ID, builder.build())
       }

       // Remove notification
       internal fun removeNotification() {
           val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
           manager.cancel(LBP_NOTIFICATION_ID)
       }

    companion object {

        lateinit var handler: Handler // used for communication from RRConnection Thread to UI (application)
        var connString = ""
        var cmdStationConnected = false

        val sendQ: BlockingQueue<String> = ArrayBlockingQueue(500)

        @Volatile
        var relevantChans = mutableListOf<Int>()
        @Volatile
        var globalPower = false
        @Volatile
        var sxData = IntArray(SXMAX + 1)
        @Volatile
        var selLocoAddr = INVALID_INT
        @Volatile
        var lastSXMessageFromClient = 0L
        @Volatile
        var waitForLocoFeedback = 0L


        fun addRelevantChan ( chan  : Int) {
            if (chan == INVALID_INT) return
            if (!relevantChans.contains(chan)) {
                relevantChans.add(chan)
            }
        }

        fun isUsableSXAddress(a : Int) : Boolean {
              return ((a > 0) and (a <= SXMAX_USED))
        }



    }
}

