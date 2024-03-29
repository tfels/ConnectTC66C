package de.felser_net.connecttc66c

import android.Manifest
import android.bluetooth.*
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.util.*

private const val TAG = "BluetoothCommunication"

// generic UUID for the UART BTLE client characteristic configuration descriptor (CCCD) which is necessary for notifications.
private val CLIENT_CHARACTERISTIC_CONFIGURATION_DESCRIPTOR_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

// uuids for device name "BT24-M"
private val SERVICE_UUID_TX_BT24 = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb")
private val MESSAGE_UUID_TX_BT24 = UUID.fromString("0000ffe2-0000-1000-8000-00805f9b34fb")
private val SERVICE_UUID_RX_BT24 = SERVICE_UUID_TX_BT24
private val MESSAGE_UUID_RX_BT24 = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb")
/*
// uuids for device name "TC66C"
private val SERVICE_UUID_TX_TC66C = UUID.fromString("0000ffe5-0000-1000-8000-00805f9b34fb")
private val MESSAGE_UUID_TX_TC66C = UUID.fromString("0000ffe9-0000-1000-8000-00805f9b34fb")
private val SERVICE_UUID_RX_TC66C = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb")
private val MESSAGE_UUID_RX_TC66C = UUID.fromString("0000ffe4-0000-1000-8000-00805f9b34fb")
*/
// used values
private val SERVICE_UUID_TX = SERVICE_UUID_TX_BT24
private val MESSAGE_UUID_TX = MESSAGE_UUID_TX_BT24
private val SERVICE_UUID_RX = SERVICE_UUID_RX_BT24
private val MESSAGE_UUID_RX = MESSAGE_UUID_RX_BT24


class BluetoothCommunication(context: Context) {
    private val mContext = context
    private var mBtAdapter: BluetoothAdapter? = null
    private var mGattTx: BluetoothGatt? = null
    private var mGattRx: BluetoothGatt? = null
    private var mReceiveCallback: ((data: TC66Data) -> Unit )? = null

    // some constants
    @Suppress("SpellCheckingInspection")
    companion object {
        val CMD_PREV_PAGE = "blastp\r\n".toByteArray(Charsets.US_ASCII)
        val CMD_NEXT_PAGE = "bnextp\r\n".toByteArray(Charsets.US_ASCII)
        val CMD_ROTATE = "brotat\r\n".toByteArray(Charsets.US_ASCII)
        val CMD_GET_VALUES = "bgetva\r\n".toByteArray(Charsets.US_ASCII)
    }


    fun initBluetooth() {
        val bluetoothManager = mContext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        mBtAdapter = bluetoothManager.adapter
    }

    // see https://stackoverflow.com/questions/34573933/how-to-connect-to-bluetooth-low-energy-device
    fun connect(deviceAddress: String): Boolean {
        if(mBtAdapter == null)
            return false
        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED)
            return false

        if(mGattTx != null || mGattRx != null)
            disconnect()

        mBtAdapter?.let {
            val device = it.getRemoteDevice(deviceAddress)
            Log.d(TAG, "device.connectGatt")
            mGattTx = device.connectGatt(mContext, false, mGattClientCallback)
            val retTx = mGattTx?.connect()
            Log.i(TAG, "mGattTx.connect()="+retTx.toString())
            mGattRx = device.connectGatt(mContext, false, mGattClientCallback)
            val retRx = mGattRx?.connect()
            Log.i(TAG, "mGattRx.connect()="+retRx.toString())
        }
        return true
    }

    fun disconnect() {
        mGattTx?.disconnect()
        mGattTx?.close()
        mGattTx = null
        mGattRx?.disconnect()
        mGattRx?.close()
        mGattRx = null
        mReceiveCallback = null
    }

    fun sendCommand(cmd_data: ByteArray) {

        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED)
            return

        val service = mGattTx?.getService(SERVICE_UUID_TX)
        Log.d(TAG, "sendCommand: service: $service")

        val txCharacteristic = service?.getCharacteristic(MESSAGE_UUID_TX)
        val props = txCharacteristic?.properties
        val expectedProperties = BluetoothGattCharacteristic.PROPERTY_WRITE or BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE
        if(props?.and(expectedProperties) != expectedProperties)
            Log.e(TAG, "sendCommand: messageCharacteristic_tx: wrong properties: $props")

        // Update TX characteristic value.  Note the setValue overload that takes a byte array must be used.
        txCharacteristic?.value = cmd_data
        mGattTx?.writeCharacteristic(txCharacteristic)
    }

    fun receiveData(callback: (data: TC66Data) -> Unit ) {
        if(mGattRx == null) // not yet connected ==> no receive
            return
        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED)
            return

        var service: BluetoothGattService?
        var timeoutCounter = 5*2
        while((mGattRx?.getService(SERVICE_UUID_RX).also { service = it }) == null) {
            if(timeoutCounter -- == 0) {
                Log.d(TAG, "receiveData: waiting for RX service timeout")
                Toast.makeText(mContext, R.string.bt_rx_timeout, Toast.LENGTH_LONG).show()
                return
            }
            Log.d(TAG, "receiveData: waiting for RX service ($timeoutCounter) ...")
            runBlocking {
                delay(500)
            }
        }
        Log.d(TAG, "receiveData: service: $service")

        val rxCharacteristic = service?.getCharacteristic(MESSAGE_UUID_RX)
        val props = rxCharacteristic?.properties
        val expectedProperties = BluetoothGattCharacteristic.PROPERTY_NOTIFY or BluetoothGattCharacteristic.PROPERTY_READ
        if(props?.and(expectedProperties) != expectedProperties)
            Log.e(TAG, "receiveData: messageCharacteristic_rx: wrong properties: $props")

        if (mGattRx?.setCharacteristicNotification(rxCharacteristic, true) == false) {
            Log.e(TAG, "receiveData: setCharacteristicNotification: failed")
        }

        // Next update the RX characteristic's client descriptor to enable notifications.
        val desc: BluetoothGattDescriptor? = rxCharacteristic?.getDescriptor(CLIENT_CHARACTERISTIC_CONFIGURATION_DESCRIPTOR_UUID)
        if (desc == null) {
            Log.e(TAG, "receiveData: getDescriptor: failed")
        }
        desc?.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        if (mGattRx?.writeDescriptor(desc) == false) {
            Log.e(TAG, "receiveData: writeDescriptor: failed")
        }
        mReceiveCallback = callback
    }

    // our BluetoothGattCallback implementation
    private val mGattClientCallback = object : BluetoothGattCallback() {
        private var mIsConnected = false

        @RequiresPermission(value = "android.permission.BLUETOOTH_CONNECT")
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)

            val isSuccess = status == BluetoothGatt.GATT_SUCCESS
            val isConnected = newState == BluetoothProfile.STATE_CONNECTED
            Log.d(TAG, "onConnectionStateChange: gatt: $gatt success: $isSuccess connected: $isConnected")
            // try to send a message to the other device as a test
            mIsConnected = if (isSuccess && isConnected)  // discover services
                gatt?.discoverServices() == true
            else
                false
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            super.onServicesDiscovered(gatt, status)
            Log.d(TAG, "onServicesDiscovered: gatt: $gatt status: $status")

            if (status != BluetoothGatt.GATT_SUCCESS) {
                mIsConnected = false
                Log.w(TAG, "onServicesDiscovered: status: $status")
                return
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray) {
            super.onCharacteristicChanged(gatt, characteristic, value)
            Log.d(TAG, "onCharacteristicChanged, len=${value.size}")

            if(value.size < 192)
                return
            val data = TC66Data(value)
            mReceiveCallback?.invoke(data)
        }
    }
}