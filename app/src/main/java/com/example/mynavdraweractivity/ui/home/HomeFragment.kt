package com.example.mynavdraweractivity.ui.home

import android.bluetooth.BluetoothDevice
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mynavdraweractivity.adapters.DeviceAdapter
import com.example.mynavdraweractivity.MainActivity
import com.example.mynavdraweractivity.databinding.FragmentHomeBinding
import android.os.Handler
import android.os.Looper

class HomeFragment : Fragment(), DeviceAdapter.OnDeviceClickListener{

    private var _binding: FragmentHomeBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    lateinit var deviceRecyclerView: RecyclerView
    lateinit var mainActivity: MainActivity
    lateinit var deviceAdapter: DeviceAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        //val textView: TextView = binding.textHome
        //homeViewModel.text.observe(viewLifecycleOwner) {
        //    textView.text = it
        //}
        deviceRecyclerView = binding.deviceRecyclerView
        deviceRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        mainActivity = activity as MainActivity
        deviceAdapter = DeviceAdapter(mainActivity.scannedDevices, this) // 将 MainActivity 的 scannedDevices 作为数据源
        deviceRecyclerView.adapter = deviceAdapter
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onDeviceClick(device: BluetoothDevice) {
        // 处理设备点击事件，例如连接设备
        Log.d("HomeFragment", "Device clicked: ${device.name}")
        mainActivity.onDeviceClick(device)
    }
}