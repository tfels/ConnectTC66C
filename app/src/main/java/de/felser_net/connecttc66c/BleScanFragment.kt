package de.felser_net.connecttc66c

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.*
import android.content.Context
import android.os.Bundle
import android.os.ParcelUuid
import android.os.SystemClock
import android.util.Log
import android.view.View
import androidx.fragment.app.ListFragment

class BleScanFragment : ListFragment() {

    // BLE device scan stuff
    private var mBtAdapter:BluetoothAdapter? = null
    private var mBleScanner: BluetoothLeScanner? = null
    private var mBleDeviceListAdapter: BleDeviceListAdapter? = null
    private var mApplyScanFilter= false

    private val mBleScanCallback: ScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            val lastSeenSec = result?.let { (SystemClock.elapsedRealtimeNanos() - it.timestampNanos).toFloat() / 1000 / 1000 / 1000}
            Log.i("BleScanFragment", "onScanResult():\n" +
                    "name:     ${result?.device?.name}\n" +
                    "scanName: ${result?.scanRecord?.deviceName}\n" +
                    "address:  ${result?.device?.address}\n" +
                    "rssi:     ${result?.rssi} dBm\n" +
                    "lastSeen: ${lastSeenSec}s ago\n" +
                    "serviceUuids: ${result?.scanRecord?.serviceUuids}\n"
            )

            if(result != null) {
                mBleDeviceListAdapter?.addData(result)
            }
        }
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mBleDeviceListAdapter = BleDeviceListAdapter(requireContext())
        listAdapter = mBleDeviceListAdapter
    }

    override fun onResume() {
        super.onResume()
        startBleScan()
    }

    override fun onPause() {
        stopBleScan()
        super.onPause()
    }

    override fun onDestroyView() {
        stopBleScan()
        super.onDestroyView()
    }

    private fun startBleScan() {
        if(mBleScanner != null) {
            stopBleScan()
        } else {
            val bluetoothManager = requireContext().getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            mBtAdapter = bluetoothManager.adapter
            mBleScanner = mBtAdapter?.bluetoothLeScanner
        }

        Log.i("BleScanFragment", "startScan")
        if(mApplyScanFilter) {
            val scanFilter = listOf(ScanFilter.Builder().setServiceUuid(ParcelUuid.fromString("0000ffe0-0000-1000-8000-00805f9b34fb")).build())
            val scanSettings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_POWER).build()
            mBleScanner?.startScan(scanFilter, scanSettings, mBleScanCallback)
        } else {
            mBleScanner?.startScan(mBleScanCallback)
        }
    }

    private fun stopBleScan() {
        Log.i("BleScanFragment", "stopScan")
        mBleScanner?.stopScan(mBleScanCallback)
        mBleDeviceListAdapter?.clear()
    }

}