package app.launcher.data

import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager

/**
 * #61 contextual music apps on headphone connect (toggleable, OFF by
 * default). Wired plug state comes from ACTION_HEADSET_PLUG;
 * Bluetooth connections come from the A2DP profile's own
 * connection-state broadcast — NOT the adapter-wide broadcast,
 * which fires for any Bluetooth device (watch, keyboard, car kit)
 * and used to pop the music sheet for all of them. A2DP is the
 * audio-output profile, so only actual audio devices trigger it.
 * The receiver only reports "headphones connected" — the ViewModel
 * decides what to show, and only when the toggle is on.
 * Nothing auto-plays: the user picks a music app from the sheet.
 */
class HeadphoneMonitor(private val onConnected: () -> Unit) : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_HEADSET_PLUG -> {
                if (intent.getIntExtra("state", 0) == 1) onConnected()
            }
            BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED -> {
                val state = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, -1)
                if (state == BluetoothProfile.STATE_CONNECTED) onConnected()
            }
        }
    }

    fun register(context: Context): HeadphoneMonitor {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_HEADSET_PLUG)
            addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)
        }
        context.registerReceiver(this, filter)
        return this
    }

    fun unregister(context: Context) {
        try {
            context.unregisterReceiver(this)
        } catch (e: IllegalArgumentException) {
            // Already unregistered; nothing to do.
        }
    }

    companion object {
        /** Is any headphone output active right now (wired or BT)? */
        fun isHeadphoneOutputActive(context: Context): Boolean {
            return try {
                val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                val devices = audio.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
                devices.any {
                    it.type == android.media.AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                        it.type == android.media.AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                        it.type == android.media.AudioDeviceInfo.TYPE_BLUETOOTH_A2DP
                }
            } catch (e: Exception) {
                false
            }
        }
    }
}
