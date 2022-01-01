package de.felser_net.connecttc66c

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.View
import androidx.fragment.app.ListFragment

class BleScanFragment : ListFragment() {

    // BLE device scan stuff
    private var mBtAdapter:BluetoothAdapter? = null
    private var mBleScanner: BluetoothLeScanner? = null
    private var mBleDeviceListAdapter: BleDeviceListAdapter? = null

    private val mBleScanCallback: ScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            val lastSeenSec = result?.let { (SystemClock.elapsedRealtimeNanos() - it.timestampNanos).toFloat() / 1000 / 1000 / 1000}
            Log.i("BleScanFragment", "onScanResult():\n" +
                    "name:     ${result?.device?.name}\n" +
                    "address:  ${result?.device?.address}\n" +
                    "rssi:     ${result?.rssi} dBm\n" +
                    "lastSeen: ${lastSeenSec}s ago"
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

        val bluetoothManager = requireContext().getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        mBtAdapter = bluetoothManager.adapter
        mBleScanner = mBtAdapter?.bluetoothLeScanner
        Log.i("BleScanFragment", "startScan")
        mBleScanner!!.startScan(mBleScanCallback)
    }

    override fun onPause() {
        mBleScanner!!.stopScan(mBleScanCallback)
        mBleDeviceListAdapter?.clear()
        super.onPause()
    }

    override fun onDestroyView() {
        Log.i("BleScanFragment", "stopScan")
        mBleScanner!!.stopScan(mBleScanCallback)
        super.onDestroyView()
    }
}