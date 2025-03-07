package com.example.mynavdraweractivity.adapters

import android.bluetooth.BluetoothDevice
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mynavdraweractivity.R
import android.util.Log

class DeviceAdapter(private val devices: List<BluetoothDevice>, private val listener: OnDeviceClickListener, private val mainThreadHandler: Handler) :
    RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder>() {

    interface OnDeviceClickListener {
        fun onDeviceClick(device: BluetoothDevice)
    }

    class DeviceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val deviceNameTextView: TextView = itemView.findViewById(R.id.deviceNameTextView)
        val deviceAddressTextView: TextView = itemView.findViewById(R.id.deviceAddressTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.device_list_item, parent, false)
        return DeviceViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        val device = devices[position]
        holder.deviceNameTextView.text = device.name ?: "Unknown Device"
        holder.deviceAddressTextView.text = device.address
        holder.itemView.setOnClickListener {
            Log.d("DeviceAdapter+++", "Item clicked: ${device.name}") // 添加日志
            // listener.onDeviceClick(device)
            mainThreadHandler.post { // 使用 mainThreadHandler 来执行 listener.onDeviceClick(device)
                listener.onDeviceClick(device)
            }
        }
    }

    override fun getItemCount(): Int {
        return devices.size
    }
}