package com.example.mynavdraweractivity.ui.home

import android.bluetooth.BluetoothDevice
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mynavdraweractivity.adapters.DeviceAdapter
import com.example.mynavdraweractivity.databinding.FragmentHomeBinding

class HomeFragment : Fragment(), DeviceAdapter.OnDeviceClickListener{

    private var _binding: FragmentHomeBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private lateinit var deviceAdapter: DeviceAdapter
    private lateinit var deviceRecyclerView: RecyclerView
    private val scannedDevices: MutableList<BluetoothDevice> = mutableListOf()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val homeViewModel =
            ViewModelProvider(this).get(HomeViewModel::class.java)

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        //val textView: TextView = binding.textHome
        //homeViewModel.text.observe(viewLifecycleOwner) {
        //    textView.text = it
        //}
        deviceRecyclerView = binding.deviceRecyclerView
        deviceRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        deviceAdapter = DeviceAdapter(scannedDevices, this)
        deviceRecyclerView.adapter = deviceAdapter
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onDeviceClick(device: BluetoothDevice) {
        // 处理设备点击事件，例如连接设备
        //connectToDevice(device)
        Log.d("HomeFragment", "Device clicked: ${device.name}")
    }

}