package com.example.mynavdraweractivity.adapters

import android.bluetooth.BluetoothDevice
import android.text.method.ScrollingMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mynavdraweractivity.R
import android.util.Log


//class DeviceAdapter(private val devices: List<BluetoothDevice>, private val listener: OnDeviceClickListener, private val mainThreadHandler: Handler) :
//    RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder>() {
class DeviceAdapter(
        private val devices: List<BluetoothDevice>,
        private val listener: OnDeviceClickListener
) : RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder>()
{
    interface OnDeviceClickListener {
        fun onDeviceClick(device: BluetoothDevice)
    }

    class DeviceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val deviceNameTextView: TextView = itemView.findViewById(R.id.deviceNameTextView)
        val deviceAddressTextView: TextView = itemView.findViewById(R.id.deviceAddressTextView)
        val deviceDataTextView: TextView = itemView.findViewById(R.id.deviceDataTextView)

        // 添加一个 List<String> 来存储数据
        val dataList: MutableList<String> = mutableListOf()

        init {
            // 使 TextView 支持滚动
            deviceDataTextView.movementMethod = ScrollingMovementMethod()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int):
            DeviceViewHolder {
        val itemView =
            LayoutInflater.from(parent.context).inflate(
                R.layout.device_list_item,
                parent, false
            )
        return DeviceViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        val device = devices[position]
        holder.deviceNameTextView.text = device.name ?: "Unknown Device"
        holder.deviceAddressTextView.text = device.address
        // 初始化 deviceData TextView 的文本
        holder.deviceDataTextView.text = "Received Data: "
        holder.itemView.setOnClickListener {
            Log.d("DeviceAdapter-->", "Item clicked: ${device.name}") // 添加日志
            listener.onDeviceClick(device)
        }
        // 显示所有数据
        updateTextView(holder)
    }

    override fun getItemCount(): Int = devices.size

    // 更新设备数据的方法
    fun updateDeviceData(deviceAddress: String, data: String) {
        val position = devices.indexOfFirst { it.address == deviceAddress }
        if (position != -1) {
            val viewHolder =
                (recyclerView?.findViewHolderForAdapterPosition(position) as?
                        DeviceViewHolder)
            viewHolder?.dataList?.add(data)
            updateTextView(viewHolder)
            if (viewHolder?.dataList?.size!! > 25) viewHolder.dataList.clear()
        }
    }

    // 更新 TextView 的方法
    private fun updateTextView(holder: DeviceViewHolder?) {
        holder?.let {
            val text = it.dataList.joinToString("\n\n")
            it.deviceDataTextView.text = text
            // 滚动到底部
            it.deviceDataTextView.post {
                val scrollAmount = it.deviceDataTextView.layout.getLineTop(
                    it.deviceDataTextView
                        .lineCount
                ) - it.deviceDataTextView.height
                if (scrollAmount > 0)
                    it.deviceDataTextView.scrollTo(0, scrollAmount)
                else
                    it.deviceDataTextView.scrollTo(0, 0)
            }
        }
    }

    // 添加一个 RecyclerView 引用
    private var recyclerView: RecyclerView? = null

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        this.recyclerView = recyclerView
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        this.recyclerView = null
    }
}