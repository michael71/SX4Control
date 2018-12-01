
// to make constants usable in JAVA via  Constants.GLOBAL_NUMBER ...
@file:JvmName("Constants")

package de.blankedv.sx4control

const val DEBUG = true // false for release-apk
const val TAG = "SX4Control"

const val INVALID_INT = -1

// preferences

const val KEY_IP = "ipPref"
const val KEY_PORT = "portPref"

const val KEY_ALLOW_POWER_CONTROL = "enablePowerControlPref"

const val SXNET_PORT = 4104
const val SXNET_START_IP = "192.168.178.29"

const val KEY_LOCO_ADDR = "lastLocoAddr"
const val DEFAULT_LOCO = 25

const val FNAME_FROM_SERVER = "from_server.xml"

const val DEMO_LOCOS_FILE = "locos-demo.xml"

const val LBP_NOTIFICATION_ID = 202 //arbitrary id for notification

// turnouts
const val STATE_CLOSED = 0
const val STATE_THROWN = 1

// doubleslips
// 4 states from 0 .. 3  (+ "unknown")


const val DISP_STANDARD = 0
const val DISP_INVERTED = 1

// signals
const val STATE_RED = 0
const val STATE_GREEN = 1
const val STATE_YELLOW = 2
const val STATE_YELLOW_FEATHER = 3
const val STATE_SWITCHING = 4


// sensors
const val STATE_FREE = 0    // bit0, mapped to occupation
const val STATE_OCCUPIED = 1   // bit0, mapped to occupation
const val SENSOR_NOT_INROUTE = 2  // bit1, mapped to "ausleuchtung"
const val SENSOR_INROUTE = 3      // bit1, mapped to "ausleuchtung"
const val STATE_UNKNOWN = INVALID_INT

const val SXMIN = 0
const val SXMAX = 111   // highest channel number for selectrix (lowest is 0)
const val LIFECHECK_SECONDS = 10  // every 10 seconds check if server connection is alive

const val DEFAULT_SXNET_PORT = "4104"  // string
const val DEFAULT_SXNET_IP = "192.168.178.29"  // string


// Message Types for UI Thread
const val TYPE_SX_MSG = 2
const val TYPE_ERROR_MSG = 3
const val TYPE_POWER_MSG = 8
const val TYPE_CONNECTION_MSG = 9
