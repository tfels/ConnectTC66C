package de.felser_net.connecttc66c

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.*
import android.content.Context
import android.os.Bundle
import android.os.ParcelUuid
import android.os.SystemClock
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import android.widget.Toast
import androidx.fragment.app.ListFragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.floatingactionbutton.FloatingActionButton

class BleScanFragment : ListFragment() {

    // BLE device scan stuff
    private var mBtAdapter:BluetoothAdapter? = null
    private var mBleScanner: BluetoothLeScanner? = null
    private var mBleDeviceListAdapter: BleDeviceListAdapter? = null
    private var mApplyScanFilter= false
    private var mFab: FloatingActionButton? = null

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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val listFragView = super.onCreateView(inflater, container, savedInstanceState)

        // modify / setup the FloatingActionButton in our activity
        // I know this is a rather bad approach but I tried several others
        // e.g. a NavController.OnDestinationChangedListener
        // but nothing worked :-(
        if (activity is MainActivity)
            mFab = (activity as MainActivity).binding.fab

        mFab?.setImageResource(requireContext().resources.getIdentifier("@android:drawable/btn_check_off", null, null))
        //mFab.setImageDrawable(requireContext().resources.getDrawable(android.R.drawable.ic_dialog_email))
        mFab?.setOnClickListener { view ->
            this.mApplyScanFilter = !this.mApplyScanFilter
            val messageResId = if(this.mApplyScanFilter)
                R.string.uuid_filter_on
            else
                R.string.uuid_filter_off
            val imageResName = if(this.mApplyScanFilter)
                "@android:drawable/btn_check_on"
            else
                "@android:drawable/btn_check_off"
            Toast.makeText(view.context, view.context.resources.getString(messageResId), Toast.LENGTH_SHORT).show()
            mFab?.setImageResource(view.context.resources.getIdentifier(imageResName, null, null))
            startBleScan()
        }

        return listFragView
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
        // cleanup fab, unfortunately this approach cannot restore the previous state
        mFab?.setOnClickListener(null)
        mFab?.setImageResource(0)

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

    override fun onListItemClick(l: ListView, v: View, position: Int, id: Long) {
        super.onListItemClick(l, v, position, id)

        val item = listView.getItemAtPosition(position)
        if((item == null) or (item !is ScanResult))
            return
        val scanResult = item as ScanResult

        // save device address to our preferences
        val sharedPref = activity?.getPreferences(Context.MODE_PRIVATE) ?: return
        with (sharedPref.edit()) {
            putString(getString(R.string.saved_device_address), scanResult.device.address)
            apply()
            findNavController().navigate(R.id.action_BleScanFragment_to_DeviceCommunicationFragment)
        }
    }

    private fun stopBleScan() {
        Log.i("BleScanFragment", "stopScan")
        mBleScanner?.stopScan(mBleScanCallback)
        mBleDeviceListAdapter?.clear()
    }

}