package de.felser_net.connecttc66c

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import de.felser_net.connecttc66c.databinding.FragmentBleScanBinding

/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class BleScanFragment : Fragment() {

    private var _binding: FragmentBleScanBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentBleScanBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonSecond.setOnClickListener {
            findNavController().navigate(R.id.action_BleScanFragment_to_FirstFragment)
        }
        mBleDeviceListAdapter = BleDeviceListAdapter(requireContext())
        binding.listviewScanResults.adapter = mBleDeviceListAdapter
    }

    override fun onResume() {
        super.onResume()

        val bluetoothManager = requireContext().getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        mBtAdapter = bluetoothManager.adapter
        mBleScanner = mBtAdapter?.bluetoothLeScanner
        Log.i("BleScanFragment", "startScan")
        mBleScanner!!.startScan(mBleScanCallback)

        populateResultList()
    }

    override fun onPause() {
        super.onPause()
        mBleScanner!!.stopScan(mBleScanCallback)
        mBleDeviceListAdapter?.clear()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        Log.i("BleScanFragment", "stopScan")
        mBleScanner!!.stopScan(mBleScanCallback)
    }

    private fun populateResultList() {
        // some demo data for emulator
        val btDevice = mBtAdapter?.getRemoteDevice("00:11:22:33:AA:BB")
        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                ScanResult(btDevice, 0, 0, 0, 0, 0, 50, 0, null, SystemClock.elapsedRealtimeNanos())
            else
                ScanResult(btDevice, null, 50, SystemClock.elapsedRealtimeNanos())
        mBleDeviceListAdapter?.addData(result)
        mBleDeviceListAdapter?.notifyDataSetChanged()
    }
}