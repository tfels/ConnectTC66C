package de.felser_net.connecttc66c

import android.bluetooth.*
import android.content.Context
import android.util.Log
import java.util.*


class BluetoothCommunication(context: Context) {
    private val TAG = "BluetoothCommunication"
    private val mContext = context
    private var mBtAdapter: BluetoothAdapter? = null
    private var mGattTx: BluetoothGatt? = null
    private var mGattRx: BluetoothGatt? = null

    // generic UUID for the UART BTLE client characteristic configuration descriptor (CCCD) which is necessary for notifications.
    private val CLIENT_CHARACTERISTIC_CONFIGURATION_DESCRIPTOR_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    // uuids for device name "BT24-M"
    private val SERVICE_UUID_TX_BT24 = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb")
    private val MESSAGE_UUID_TX_BT24 = UUID.fromString("0000ffe2-0000-1000-8000-00805f9b34fb")
    private val SERVICE_UUID_RX_BT24 = SERVICE_UUID_TX_BT24
    private val MESSAGE_UUID_RX_BT24 = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb")

    // uuids for device name "TC66C"
    private val SERVICE_UUID_TX_TC66C = UUID.fromString("0000ffe5-0000-1000-8000-00805f9b34fb")
    private val MESSAGE_UUID_TX_TC66C = UUID.fromString("0000ffe9-0000-1000-8000-00805f9b34fb")
    private val SERVICE_UUID_RX_TC66C = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb")
    private val MESSAGE_UUID_RX_TC66C = UUID.fromString("0000ffe4-0000-1000-8000-00805f9b34fb")

    // used values
    private val SERVICE_UUID_TX = SERVICE_UUID_TX_BT24
    private val MESSAGE_UUID_TX = MESSAGE_UUID_TX_BT24
    private val SERVICE_UUID_RX = SERVICE_UUID_RX_BT24
    private val MESSAGE_UUID_RX = MESSAGE_UUID_RX_BT24

    // some constants
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
    }

    fun sendCommand(cmd_data: ByteArray) {

        val service = mGattTx?.getService(SERVICE_UUID_TX)
        Log.d(TAG, "sendCommand: service: $service")

        val messageCharacteristic_tx = service?.getCharacteristic(MESSAGE_UUID_TX)
        val props = messageCharacteristic_tx?.properties
        val expectedProperties = BluetoothGattCharacteristic.PROPERTY_WRITE or BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE
        if(props?.and(expectedProperties) != expectedProperties)
            Log.e(TAG, "sendCommand: messageCharacteristic_tx: wrong properties: $props")

        // Update TX characteristic value.  Note the setValue overload that takes a byte array must be used.
        messageCharacteristic_tx?.setValue(cmd_data)
        mGattTx?.writeCharacteristic(messageCharacteristic_tx)
    }

    fun revcData() {
        val service = mGattRx?.getService(SERVICE_UUID_RX)
        Log.d(TAG, "revcData: service: $service")

        val messageCharacteristic_rx = service?.getCharacteristic(MESSAGE_UUID_RX)
        val props = messageCharacteristic_rx?.properties
        val expectedProperties = BluetoothGattCharacteristic.PROPERTY_NOTIFY or BluetoothGattCharacteristic.PROPERTY_READ
        if(props?.and(expectedProperties) != expectedProperties)
            Log.e(TAG, "revcData: messageCharacteristic_rx: wrong properties: $props")

        if (mGattRx?.setCharacteristicNotification(messageCharacteristic_rx, true) == false) {
            Log.e(TAG, "revcData: setCharacteristicNotification: failed")
        }

        // Next update the RX characteristic's client descriptor to enable notifications.
        val desc: BluetoothGattDescriptor? = messageCharacteristic_rx?.getDescriptor(CLIENT_CHARACTERISTIC_CONFIGURATION_DESCRIPTOR_UUID)
        if (desc == null) {
            Log.e(TAG, "revcData: getDescriptor: failed")
        }
        desc?.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        if (mGattRx?.writeDescriptor(desc) == false) {
            Log.e(TAG, "revcData: writeDescriptor: failed")
        }
    }

    // our BluetoothGattCallback implementation
    private val mGattClientCallback = object : BluetoothGattCallback() {
        private var mIsConnected = false

        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)

            val isSuccess = status == BluetoothGatt.GATT_SUCCESS
            var isConnected = newState == BluetoothProfile.STATE_CONNECTED
            Log.d(TAG, "onConnectionStateChange: gatt: $gatt success: $isSuccess connected: $isConnected")
            // try to send a message to the other device as a test
            if (isSuccess && isConnected)  // discover services
                mIsConnected = gatt?.discoverServices() == true
            else
                mIsConnected = false
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

        override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
            super.onCharacteristicChanged(gatt, characteristic)
            Log.d(TAG, "onCharacteristicChanged, len=${characteristic?.value?.size}")
            if(characteristic == null)
                return
            if(characteristic.value == null)
                return
            if(characteristic.value.size < 192)
                return
            val data = TC66Data(characteristic.value)
        }
    }
}