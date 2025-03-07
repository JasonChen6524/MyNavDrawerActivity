package com.example.mynavdraweractivity

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.Menu
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.navigation.NavigationView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.app.AppCompatActivity
import com.example.mynavdraweractivity.databinding.ActivityMainBinding
import com.example.mynavdraweractivity.adapters.DeviceAdapter
import com.example.mynavdraweractivity.ui.home.HomeFragment

import android.bluetooth.BluetoothSocket
import android.os.Handler
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import android.os.Looper
import androidx.navigation.fragment.NavHostFragment
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter // 导入 IntentFilter

class MainActivity : AppCompatActivity(), DeviceAdapter.OnDeviceClickListener{

    companion object {
        const val MESSAGE_READ = 0
    }

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    private var bluetoothAdapter: BluetoothAdapter? = null
    private val bluetoothLeScanner: BluetoothLeScanner? by lazy {
        bluetoothAdapter?.bluetoothLeScanner
    }

    private val REQUEST_BLUETOOTH_PERMISSIONS = 1
    val scannedDevices: MutableList<BluetoothDevice> = mutableListOf()
    private lateinit var homeFragment: HomeFragment
    private var bluetoothSocket: BluetoothSocket? = null
    private var connectedThread: ConnectedThread? = null
    //private val MY_UUID: UUID = UUID.fromString("FEC26EC4-6D71-4442-9F81-55BC21D658D6") // 替换为你设备的 UUID
    private val MY_UUID: UUID = UUID.fromString("4880c12c-fdcb-4077-8920-a450d7f9b907") // 替换为你设备的 UUID
    //private val MY_UUID:  UUID = UUID.fromString("50242207-1A68-3C2B-23D9-7A59238C3FC4") // 替换为你设备的 UUID
    //private val MY_UUID:  UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") // 替换为你设备的 UUID

    private var isConnecting = false // 添加一个标志位
    // BroadcastReceiver to listen for bond state changes
    private val bondStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (BluetoothDevice.ACTION_BOND_STATE_CHANGED == action) {
                val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                val previousBondState = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, -1)
                val bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, -1)

                Log.d("Bluetooth", "Bond state changed for device: ${device?.name}")
                Log.d("Bluetooth", "Previous bond state: ${getBondStateString(previousBondState)}")
                Log.d("Bluetooth", "Current bond state: ${getBondStateString(bondState)}")

                when (bondState) {
                    BluetoothDevice.BOND_BONDED -> {
                        Log.d("Bluetooth", "Device is now bonded.")
                        // Now that the device is bonded, try to connect
                        if (!isConnecting) {
                            device?.let { connectToDevice(it) }
                        }
                    }
                    BluetoothDevice.BOND_NONE -> {
                        Log.d("Bluetooth", "Device is not bonded.")
                        isConnecting = false // Reset the flag when bonding fails
                        // Handle bond failure
                    }
                    BluetoothDevice.BOND_BONDING -> {
                        Log.d("Bluetooth", "Device is bonding...")
                        // Handle bonding process
                    }
                }
            }
        }
    }

    private fun getBondStateString(bondState: Int): String {
        return when (bondState) {
            BluetoothDevice.BOND_NONE -> "BOND_NONE"
            BluetoothDevice.BOND_BONDING -> "BOND_BONDING"
            BluetoothDevice.BOND_BONDED -> "BOND_BONDED"
            else -> "UNKNOWN"
        }
    }

    private fun connectToDevice(device: BluetoothDevice) {
        Log.d("MainActivity***", "connectToDevice called: ${device.name}") // 添加日志
        // 使用一个线程来执行连接操作，避免阻塞主线程
        Thread {
            isConnecting = true // Set the flag to true at the beginning
            try {
                Thread.sleep(500)
                //stopScan() // 在连接之前停止扫描
                //Thread.sleep(500)
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    Log.d("MainActivity@@@", "checkSelfPermission") // 添加日志
                    return@Thread
                }
                // 检查设备是否已配对
                if (device.bondState != BluetoothDevice.BOND_BONDED) {
                    Log.d("Bluetooth333", "设备未配对。正在启动配对...")
                    if (ActivityCompat.checkSelfPermission(
                            this,
                            Manifest.permission.BLUETOOTH_CONNECT
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        return@Thread
                    }
                    registerReceiver(bondStateReceiver,IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED))
                    device.createBond()
                    // 你可能需要在这里等待配对完成
                    // 然后再继续连接。
                    // 这通常涉及到监听
                    // ACTION_BOND_STATE_CHANGED 广播。
                    Log.d("Bluetooth444", "正在启动配对...。。。")
                    return@Thread
                }

                Log.d("MainActivity###", "connecting......") // 添加日志
                Log.d("MainActivity###", "connecting......:${device.uuids}") // 添加日志
                bluetoothSocket = device.createRfcommSocketToServiceRecord(MY_UUID)
                //bluetoothSocket = device.createInsecureRfcommSocketToServiceRecord(MY_UUID)
                Log.d("Bluetooth111", "bluetoothSocket: $bluetoothSocket")
                Log.d("Bluetooth222", "Before bluetoothSocket?.connect()") // New log
                bluetoothSocket?.connect()
                Log.d("Bluetooth", "已连接到设备: ${device.name}")
                // 连接成功后，启动数据接收线程
                connectedThread = ConnectedThread(bluetoothSocket!!)
                connectedThread?.start()
            } catch (e: IOException) {
                Log.e("Bluetooth", "连接设备失败: ${e.message}")
                //e.printStackTrace()
                bluetoothSocket?.close()
                bluetoothSocket = null

                //startScan()
            }finally {
                isConnecting = false // Set the flag to false in the finally block
            }
        }.start()
    }

    private inner class ConnectedThread(private val mmSocket: BluetoothSocket) : Thread() {
        private val mmInStream: InputStream?
        private val mmOutStream: OutputStream?
        private val handler: Handler

        init {
            var tmpIn: InputStream? = null
            var tmpOut: OutputStream? = null

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = mmSocket.inputStream
                tmpOut = mmSocket.outputStream
            } catch (e: IOException) {
                Log.e("Bluetooth", "Error occurred when creating input stream", e)
            }

            mmInStream = tmpIn
            mmOutStream = tmpOut
            // 创建一个 Handler，用于在 UI 线程中处理消息
            handler = Handler(Looper.getMainLooper()) { message ->
                when (message.what) {
                    MESSAGE_READ -> {
                        val readBuff = message.obj as ByteArray
                        val readMessage = String(readBuff, 0, message.arg1)
                        Log.d("Bluetooth", "Received message: $readMessage")
                        // 在这里处理接收到的数据，例如更新 UI
                        // ...
                        true
                    }
                    else -> false
                }
            }
        }

        override fun run() {
            val buffer = ByteArray(1024)  // buffer store for the stream
            var bytes: Int // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream!!.read(buffer)
                    // Send the obtained bytes to the UI activity
                    val readMsg = handler.obtainMessage(
                        MESSAGE_READ, bytes, -1,
                        buffer
                    )
                    readMsg.sendToTarget()
                } catch (e: IOException) {
                    Log.d("Bluetooth", "Input stream was disconnected", e)
                    break
                }
            }
        }

        fun write(bytes: ByteArray) {
            try {
                mmOutStream!!.write(bytes)
            } catch (e: IOException) {
                Log.e("Bluetooth", "Error occurred when sending data", e)
            }
        }

        fun cancel() {
            try {
                mmSocket.close()
            } catch (e: IOException) {
                Log.e("Bluetooth", "Could not close the connect socket", e)
            }
        }
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            // Log.d("Bluetooth ScanResult", "onScanResult() called")
            val device: BluetoothDevice = result.device
            if(device.name != null && device.name.startsWith("V5")) {

                stopScan()

                    Log.d("Bluetooth Found", "Found device: ${device.name} - ${device.address}")
                    // Add the device to the list if it's not already there
                    if (!scannedDevices.contains(device)) {
                        scannedDevices.add(device)
                        Log.d("Bluetooth Add", "Device added to list: ${device.name} - ${device.address}")
                        // 更新 RecyclerView
                        runOnUiThread {
                            homeFragment.deviceAdapter.notifyDataSetChanged()
                        }
                    }
                // connectToDevice(device)
            }
            // Handle the discovered device here
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Log.e("Bluetooth Fail?", "Scan failed with error code: $errorCode")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.appBarMain.toolbar)

        binding.appBarMain.fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null)
                .setAnchorView(R.id.fab).show()
        }
        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        //val navController = findNavController(R.id.nav_host_fragment_content_main)
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main) as NavHostFragment
        val navController = navHostFragment.navController
        homeFragment = navHostFragment.childFragmentManager.fragments[0] as HomeFragment

        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home, R.id.nav_gallery, R.id.nav_slideshow
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        // Get Bluetooth adapter
        val bluetoothManager: BluetoothManager =
            getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        if (bluetoothAdapter == null) {
            // Device doesn't support Bluetooth
            // Handle the case where Bluetooth is not supported
        } else {
            if (bluetoothAdapter?.isEnabled == false) {
                Log.d("Bluetooth", "Bluetooth is not enabled")
                // 蓝牙未开启，提示用户开启
                // You can prompt the user to enable Bluetooth here
            } else {
                Log.d("Bluetooth Enable?", "Bluetooth is enabled")
            }
            checkBluetoothPermissions()
        }
    }

    override fun onDeviceClick(device: BluetoothDevice) {
        Log.d("MainActivity-->", "onDeviceClick called: ${device.name}") // 添加日志
        // 处理设备点击事件，例如连接设备
        connectToDevice(device)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    private fun checkBluetoothPermissions() {
        val permissions = arrayOf(
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.ACCESS_FINE_LOCATION
        )

        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest, REQUEST_BLUETOOTH_PERMISSIONS)
        } else {
            // Permissions already granted, proceed with Bluetooth operations
            Log.d("Bluetooth Scan3", "startScan() called")
            startScan()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_BLUETOOTH_PERMISSIONS) {
            Log.d("Bluetooth Scan5", "startScan() called")
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                // Permissions granted, proceed with Bluetooth operations
                Log.d("Bluetooth Scan4", "startScan() called")
                startScan()
            } else {
                // Permissions denied, handle accordingly
            }
        }
    }

    private fun startScan() {
        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        Log.d("Bluetooth Scan1", "startScan() called")


        // Create a list of ScanFilters
        val scanFilters: List<ScanFilter> = emptyList()

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        bluetoothLeScanner?.startScan(scanFilters, scanSettings, scanCallback)
        Log.d("Bluetooth Scan2", "bluetoothLeScanner?.startScan() called")
    }

    private fun stopScan() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        Log.d("Bluetooth stopScan", "bluetoothLeScanner?.stopScan() called")
        bluetoothLeScanner?.stopScan(scanCallback)
    }
}