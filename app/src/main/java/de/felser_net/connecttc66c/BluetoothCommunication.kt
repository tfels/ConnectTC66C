package de.felser_net.connecttc66c

import android.bluetooth.*
import android.content.Context
import android.util.Log
import java.util.*
import android.bluetooth.BluetoothGattDescriptor




class BluetoothCommunication(context: Context) {
    private val TAG = "BluetoothCommunication"
    private val mContext = context
    private var mBtAdapter: BluetoothAdapter? = null
    private var mGatt: BluetoothGatt? = null

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
        if(mGatt != null)
            disconnect()

        mBtAdapter?.let {
            val device = it.getRemoteDevice(deviceAddress)
            Log.d(TAG, "device.connectGatt")
            mGatt = device.connectGatt(mContext, false, mGattClientCallback)
            val ret = mGatt?.connect()
            Log.i(TAG, "gatt.connect()="+ret.toString())
        }
        return true
    }

    fun disconnect() {
        mGatt?.disconnect()
        mGatt?.close()
        mGatt = null
    }

    fun sendCommand(cmd_data: ByteArray) {

        val service = mGatt?.getService(SERVICE_UUID_TX)
        Log.d(TAG, "onServicesDiscovered: service: $service")

        val messageCharacteristic_tx = service?.getCharacteristic(MESSAGE_UUID_TX)
        val props = messageCharacteristic_tx?.properties
        val expectedProperties = BluetoothGattCharacteristic.PROPERTY_WRITE or BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE
        if(props?.and(expectedProperties) != expectedProperties)
            Log.d(TAG, "messageCharacteristic_tx: wrong properties: $props")

        // Update TX characteristic value.  Note the setValue overload that takes a byte array must be used.
        messageCharacteristic_tx?.setValue(cmd_data)
        mGatt?.writeCharacteristic(messageCharacteristic_tx)
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
                Log.d(TAG, "onServicesDiscovered: status: $status")
                return
            }
            if (gatt != mGatt) {
                mIsConnected = false
                mGatt = gatt
                Log.d(TAG, "onServicesDiscovered: gatt object differs: prev gatt $mGatt now: $gatt")
                return
            }

            val service = gatt?.getService(SERVICE_UUID_RX)
            Log.d(TAG, "onServicesDiscovered: service: $service")

            val messageCharacteristic_rx = service?.getCharacteristic(MESSAGE_UUID_RX)
            val props = messageCharacteristic_rx?.properties
            val expectedProperties = BluetoothGattCharacteristic.PROPERTY_NOTIFY or BluetoothGattCharacteristic.PROPERTY_READ
            if(props?.and(expectedProperties) != expectedProperties)
                Log.d(TAG, "messageCharacteristic_rx: wrong properties: $props")

            /*
            if (gatt?.setCharacteristicNotification(messageCharacteristic_rx, true) == false) {
                Log.d(TAG, "setCharacteristicNotification: failed on 1")
            }

            // UUID for the UART BTLE client characteristic which is necessary for notifications.
            val CLIENT_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

            // Next update the RX characteristic's client descriptor to enable notifications.
            val desc: BluetoothGattDescriptor? = messageCharacteristic_rx?.getDescriptor(CLIENT_UUID)
            if (desc == null) {
                Log.d(TAG, "getDescriptor: failed on 1")
            }
            desc?.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            if (!gatt!!.writeDescriptor(desc)) {
                Log.d(TAG, "writeDescriptor: failed on 1")
            }
*/
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            super.onCharacteristicRead(gatt, characteristic, status)
            Log.d(TAG, "onCharacteristicRead")
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            super.onCharacteristicWrite(gatt, characteristic, status)
            Log.d(TAG, "onCharacteristicWrite")
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?
        ) {
            super.onCharacteristicChanged(gatt, characteristic)
            Log.d(TAG, "onCharacteristicChanged")
        }

        override fun onDescriptorRead(
            gatt: BluetoothGatt?,
            descriptor: BluetoothGattDescriptor?,
            status: Int
        ) {
            super.onDescriptorRead(gatt, descriptor, status)
            Log.d(TAG, "onDescriptorRead")
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt?,
            descriptor: BluetoothGattDescriptor?,
            status: Int
        ) {
            super.onDescriptorWrite(gatt, descriptor, status)
            Log.d(TAG, "onDescriptorWrite")
        }

        override fun onReliableWriteCompleted(gatt: BluetoothGatt?, status: Int) {
            super.onReliableWriteCompleted(gatt, status)
            Log.d(TAG, "onReliableWriteCompleted")
        }

        override fun onReadRemoteRssi(gatt: BluetoothGatt?, rssi: Int, status: Int) {
            super.onReadRemoteRssi(gatt, rssi, status)
            Log.d(TAG, "onReadRemoteRssi")
        }

        override fun onMtuChanged(gatt: BluetoothGatt?, mtu: Int, status: Int) {
            super.onMtuChanged(gatt, mtu, status)
            Log.d(TAG, "onMtuChanged")
        }

        override fun onServiceChanged(gatt: BluetoothGatt) {
            super.onServiceChanged(gatt)
            Log.d(TAG, "onServiceChanged")
        }
    }
}