package de.felser_net.connecttc66c

import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanResult
import android.content.Context
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView


class BleDeviceListAdapter: BaseAdapter {
    private val mBleScanResultList = ArrayList<ScanResult>()
    private var mContext: Context? = null
    private val mInflator: LayoutInflater

    constructor(mContext: Context?, mInflator: LayoutInflater) : super() {
        this.mContext = mContext
        this.mInflator = mInflator
    }

    fun addData(data: ScanResult) {
        if (!mBleScanResultList.contains(data)) {
            mBleScanResultList.add(data)
        }
    }

    fun getData(position: Int): ScanResult? {
        var result = getItem(position)
        if(result is ScanResult)
            return result
        else
            return null
    }

    fun clear() {
        mBleScanResultList.clear()
    }

    override fun getCount(): Int {
        return mBleScanResultList.size
    }

    override fun getItem(position: Int): Any {
        return mBleScanResultList[position]
    }

    override fun getItemId(position: Int): Long {
        return getData(position)?.device?.address.hashCode().toLong();
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        var viewHolder: ViewHolder?
        val view: View

        // optimized code: reuse existing view (we save a ViewHolder reference)
        if (convertView == null) {
            view = mInflator.inflate(R.layout.item_ble_scan_result, null);
            viewHolder = ViewHolder()
            viewHolder.deviceNameView = view.findViewById(R.id.device_name)
            viewHolder.deviceAddressView = view.findViewById(R.id.device_address)
            viewHolder.lastSeenView = view.findViewById(R.id.last_seen)
            view.setTag(viewHolder)
        } else {
            view = convertView
            viewHolder = view.getTag() as ViewHolder
        }

        // set data texts
        val scanResult: ScanResult = mBleScanResultList.get(position)
        val device: BluetoothDevice = scanResult.device
        val deviceName = device.getName()
        if (deviceName != null && deviceName?.length > 0)
            viewHolder.deviceNameView?.setText(deviceName)
        else
            viewHolder.deviceNameView?.setText("unknown device") //R.string.unknown_device);

        viewHolder.deviceAddressView?.setText(device.address)

        var lastSeenSec = scanResult?.let { (SystemClock.elapsedRealtimeNanos() - it.timestampNanos).toFloat() / 1000 / 1000 }
        viewHolder.lastSeenView?.setText("${lastSeenSec}s ago")

        return view;
    }

    // internal class for holding info about one line
    internal class ViewHolder {
        var deviceNameView: TextView? = null
        var deviceAddressView: TextView? = null
        var lastSeenView: TextView? = null
    }
}