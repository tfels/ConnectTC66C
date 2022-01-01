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


class BleDeviceListAdapter(context: Context) : BaseAdapter() {

    private val mBleScanResultList = ArrayList<ScanResult>()
    private val mContext = context
    private val mInflator = LayoutInflater.from(context)

    fun addData(data: ScanResult) {
        val listData = listAlreadyContains(data)
        mBleScanResultList.remove(listData)

        mBleScanResultList.add(data)
        notifyDataSetChanged()
    }

    // my own `contains` method because we only compare some data
    private fun listAlreadyContains(searchFor: ScanResult?): ScanResult? {
        if (searchFor == null) {
            return null
        } else {
            for (listItem in mBleScanResultList)
                if (listItem.device == searchFor.device)
                    return listItem
        }
        return null
    }

    fun getData(position: Int): ScanResult? {
        val result = getItem(position)
        return if(result is ScanResult)
            result
        else
            null
    }

    fun clear() {
        mBleScanResultList.clear()
        notifyDataSetChanged()
    }

    override fun getCount(): Int {
        return mBleScanResultList.size
    }

    override fun getItem(position: Int): Any {
        return mBleScanResultList[position]
    }

    override fun getItemId(position: Int): Long {
        return getData(position)?.device?.address.hashCode().toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val viewHolder: ViewHolder?
        val view: View

        // optimized code: reuse existing view (we save a ViewHolder reference)
        if (convertView == null) {
            view = mInflator.inflate(R.layout.item_ble_scan_result, null)
            viewHolder = ViewHolder()
            viewHolder.deviceNameView = view.findViewById(R.id.device_name)
            viewHolder.deviceAddressView = view.findViewById(R.id.device_address)
            viewHolder.lastSeenView = view.findViewById(R.id.last_seen)
            viewHolder.rssi = view.findViewById(R.id.rssi)
            view.tag = viewHolder
        } else {
            view = convertView
            viewHolder = view.tag as ViewHolder
        }

        // set data texts
        val scanResult: ScanResult = mBleScanResultList[position]
        val device: BluetoothDevice = scanResult.device
        val deviceName = device.name
        if (deviceName != null && deviceName.isNotEmpty())
            viewHolder.deviceNameView?.text = deviceName
        else
            viewHolder.deviceNameView?.setText(R.string.unknown_device)

        viewHolder.deviceAddressView?.text = device.address

        val lastSeenSec = (SystemClock.elapsedRealtimeNanos() - scanResult.timestampNanos).toFloat() / 1000 / 1000 / 1000
        viewHolder.lastSeenView?.text = mContext.getString(R.string.device_last_seen_ago, lastSeenSec)

        viewHolder.rssi?.text = mContext.getString(R.string.device_rssi, scanResult.rssi)

        return view
    }

    // internal class for holding info about one line
    internal class ViewHolder {
        var deviceNameView: TextView? = null
        var deviceAddressView: TextView? = null
        var lastSeenView: TextView? = null
        var rssi: TextView? = null
    }
}