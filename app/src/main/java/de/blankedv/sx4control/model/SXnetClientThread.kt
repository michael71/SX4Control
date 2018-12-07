package de.blankedv.sx4control.model

import android.content.Context
import android.os.Message
import android.os.SystemClock
import android.util.Log

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.InetSocketAddress
import java.net.Socket

import de.blankedv.sx4control.model.MainApplication.Companion.sendQ
import de.blankedv.sx4control.model.MainApplication.Companion.handler
import de.blankedv.sx4control.model.MainApplication.Companion.connString

/**
 * communicates with the SX3-PC server program (usually on port 4104)
 *
 * runs on own thread, using a BlockingQueue for queing the commands
 * can be shutdown by calling the shutdown method.
 *
 * @author mblank
 */
class SXnetClientThread(private var context: Context?, private val ip: String, private val port: Int) : Thread() {
    // threading und BlockingQueue siehe http://www.javamex.com/tutorials/blockingqueue_example.shtml

    @Volatile
    private var shuttingDown: Boolean
    @Volatile
    private var clientTerminated: Boolean = false
    private var lastReceived: Long = 0
    private var lastConnErrorSent: Long = 0

    private var shutdownFlag: Boolean = false
    private var socket: Socket? = null
    private var out: PrintWriter? = null
    private var `in`: BufferedReader? = null


    init {
        if (DEBUG) Log.d(TAG, "SXnetClientThread constructor.")
        shuttingDown = false
        clientTerminated = false
        shutdownFlag = false
        lastReceived = SystemClock.elapsedRealtime() + 5000  // initialize
        name = "sxnetClient"
    }

    fun shutdown() {
        if (DEBUG) Log.d(TAG, "SXnetClientThread shutdown called.")
        shutdownFlag = true
        this.interrupt()
    }

    fun isConnected() :Boolean {
        return ((socket != null) && (!shutdownFlag) && ((SystemClock.elapsedRealtime() - lastReceived) <= 10 * 1000) )
    }

    override fun run() {
        if (DEBUG) Log.d(TAG, "SXnetClientThread run.")
        shutdownFlag = false
        clientTerminated = false
        connect()


        while (!shutdownFlag && !Thread.currentThread().isInterrupted) {
            try {
                if (`in` != null && `in`!!.ready()) {
                    val in1 = `in`!!.readLine()
                    if (DEBUG) Log.d(TAG, "msgFromServer: $in1")
                    val cmds = in1.split(";".toRegex()).dropLastWhile { it.isEmpty() }
                        .toTypedArray()  // multiple commands per line possible, separated by semicolon
                    for (cmd in cmds) {
                        handleMsgFromServer(cmd.trim { it <= ' ' }.toUpperCase())
                        // sends feedback message  XL 'addr' 'data' (or INVALID_INT) back to mobile device
                    }
                    lastReceived = SystemClock.elapsedRealtime()
                }
            } catch (e: IOException) {
                Log.e(TAG, "ERROR: reading from socket - " + e.message)
            }

            // check send queue
            if (!sendQ.isEmpty()) {

                var comm = ""
                try {
                    comm = sendQ.take()
                    if (comm.length > 0) immediateSend(comm)
                } catch (e: InterruptedException) {
                    Log.e(TAG, "could not take command from sendQ")
                }

            }

            // send ERROR to UI if we didn't receive a message in the last 10 secs
            // but send this message only once per 20 secs
            if ( (SystemClock.elapsedRealtime() - lastReceived > 10 * 1000) and
                ((SystemClock.elapsedRealtime() - lastConnErrorSent) > 20 * 1000)) {
                Log.e(TAG, "SXnetClientThread - connection lost? ")
                val m = Message.obtain()
                m.what = TYPE_ERROR_MSG
                m.obj = "disconnected from SX4 server"
                handler!!.sendMessage(m)  // send SX data to UI Thread via Message
                lastConnErrorSent = SystemClock.elapsedRealtime()  // send this msg only every 10 secs
            }

            // automatic shutdown after 60 secs without message
            //if (SystemClock.elapsedRealtime() - commAlive > 60 * 1000) {
            //    shutdownFlag = true
            //}
        }

        clientTerminated = true
        if (socket != null) {
            try {
                socket!!.close()
                Log.e(TAG, "SXnetClientThread - socket closed")
            } catch (e: IOException) {
                Log.e(TAG, "SXnetClientThread - " + e.message)
            }

        }
        if (DEBUG) Log.d(TAG, "SXnetClientThread stopped.")
    }


    private fun connect() {
        if (DEBUG) Log.d(TAG, "SXnetClientThread trying conn to - $ip:$port")
        try {
            val socketAddress = InetSocketAddress(ip, port)

            // create a socket
            socket = Socket()
            socket!!.connect(socketAddress, 2000)
            //socket.setSoTimeout(2000);  // set read timeout to 2000 msec

            //socket.setSoLinger(true, 0);  // force close, dont wait.

            out = PrintWriter(socket!!.getOutputStream(), true)
            `in` = BufferedReader(
                InputStreamReader(
                    socket!!.getInputStream()
                )
            )
            connString = `in`!!.readLine()
            lastReceived = SystemClock.elapsedRealtime()

            if (DEBUG) Log.d(TAG, "SXnet connected to: $connString")

        } catch (e: Exception) {
            Log.e(TAG, "SXnetClientThread.connect - Exception: " + e.message)

            val m = Message.obtain()
            m.what = TYPE_ERROR_MSG
            m.obj = e.message
            handler!!.sendMessage(m)  // send SX data to UI Thread via Message
        }

    }

    fun disconnectContext() {
        this.context = null
        Log.d(TAG, "SXnet lost context, stopping thread")
        shutdown()
    }

    /* public void readChannel(int adr) {

		if (DEBUG) Log.d(TAG,"readChannel a="+adr+" shutd.="+shuttingDown+" clientTerm="+clientTerminated);
		if ( shutdownFlag || clientTerminated || (adr == INVALID_INT)) return;
		String command = "R "+adr;
		Boolean success = sendQ.offer(command);
		if ((success == false) && (DEBUG)) Log.d(TAG,"readChannel failed, queue full")	;
	} */


    private fun immediateSend(command: String) {
        if (shutdownFlag || clientTerminated) {
            if (DEBUG) Log.e(TAG, "shutdown, could not send: $command")
            return
        }
        if (out == null) {
            if (DEBUG) Log.e(TAG, "out=null, could not send: $command")
        } else {
            try {
                out!!.println(command)
                out!!.flush()
                if (DEBUG) Log.d(TAG, "sent: $command")
            } catch (e: Exception) {
                if (DEBUG) Log.d(TAG, "could not send: $command")
                Log.e(TAG, e.javaClass.name + " " + e.message)
            }

        }
    }


    /**
     * SX Net Protocol (all msg terminated with CR)
     *
     * for a list of channels (which the client has set or read in the past) all changes are
     * transmitted back to the client
     */

    private fun handleMsgFromServer(msg: String) {
        var msg = msg
        // check whether there is an application to send info to -
        // to avoid crash if application has stopped but thread is still running
        if (context == null) return

        var info: Array<String>? = null
        msg = msg.toUpperCase()

        val adr: Int
        val data: Int

        if (msg.isNotEmpty() &&  !msg.contains("OK") ) { // message should contain valid data

            info = msg.split("\\s+".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()  // one or more whitespace

            if (info.size >= 2 && info[0] == "XPOWER") {
                data = getDataFromString(info[1])
                if (data != ERROR) {
                    val m = Message.obtain()
                    m.what = TYPE_POWER_MSG
                    m.arg1 = 0
                    m.arg2 = data
                    handler!!.sendMessage(m)  // send SX data to UI Thread via Message
                }
            } else if (info.size >= 3 && info[0] == "X") {
                adr = getChannelFromString(info[1])
                data = getDataFromString(info[2])
                if (adr != ERROR && data != ERROR) {
                    val m = Message.obtain()
                    m.what = TYPE_SX_MSG
                    m.arg1 = adr
                    m.arg2 = data
                    handler!!.sendMessage(m)  // send SX data to UI Thread via Message
                }
            }
        }
    }

    private fun getDataFromString(s: String): Int {
        // converts String to integer between 0 and 255 (=SX Data)
        var data: Int? = ERROR
        try {
            data = Integer.parseInt(s)
            if (data < 0 || data > 255) {
                data = ERROR
            }
        } catch (e: Exception) {
            data = ERROR
        }

        return data!!
    }

    internal fun getChannelFromString(s: String): Int {
        var channel: Int? = ERROR
        try {
            channel = Integer.parseInt(s)
            if (channel >= 0 && channel <= SXMAX) {
                return channel
            } else {
                channel = ERROR
            }
        } catch (e: Exception) {

        }

        return channel!!
    }

    companion object {

        private val ERROR = 9999
    }


}
