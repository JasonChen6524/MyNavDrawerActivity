package com.example.mynavdraweractivity

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
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

import java.io.IOException
import java.util.UUID
import androidx.navigation.fragment.NavHostFragment

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
    private var bluetoothGatt: BluetoothGatt? = null
    private val TARGET_SERVICE_UUID    = UUID.fromString("4880c12c-fdcb-4077-8920-a450d7f9b907") // Your target service UUID
    private val MY_CHARACTERISTIC_UUID = UUID.fromString("FEC26EC4-6D71-4442-9F81-55BC21D658D6")
    private val CLIENT_CHARACTERISTIC_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb") // 客户端特征值配置 UUID (固定值)

    private var isConnecting = false // 添加一个标志位

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            val deviceAddress = gatt.device.address
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.d("Bluetooth Conn", "Connected to GATT server: $deviceAddress")
                    // Discover services
                    if (ActivityCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.BLUETOOTH_CONNECT
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        return
                    }
                    gatt.discoverServices()
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.d("Bluetooth Disconn", "Disconnected from GATT server: $deviceAddress")
                    disconnectGattServer()
                }
            } else {
                Log.e("Bluetooth Error", "Connection failed with status: $status for device: $deviceAddress")
                disconnectGattServer()
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d("Bluetooth Discovered", "Services discovered for device: ${gatt.device.address}")
                // Check if the target service is present
                val service = gatt.getService(TARGET_SERVICE_UUID)
                if (service != null) {
                    Log.d("Bluetooth Target Found", "Target service found: $TARGET_SERVICE_UUID")
                    // You can now interact with the service and its characteristics
                    // Example: Read a characteristic
                    val characteristic = service.getCharacteristic(MY_CHARACTERISTIC_UUID)
                    if (characteristic != null) {
                        // 启用特征值的通知
                        setCharacteristicNotification(gatt, characteristic, true)
                    }else{
                        Log.w("Bluetooth", "Target service not found: $TARGET_SERVICE_UUID")
                    }
                } else {
                    Log.w("Bluetooth", "Target service not found: $TARGET_SERVICE_UUID")
                }
            } else {
                Log.e("Bluetooth", "Service discovery failed with status: $status")
            }
            isConnecting = false
        }

        // 特征值改变 (接收到数据)
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            val data = characteristic.value
            val hexString = bytesToHex(data)
            Log.d("Bluetooth RECV", "Received data (hex): $hexString")
            // 在这里，你可以将 hexString 显示在你的 UI 上
            // 例如，更新一个 TextView 的文本：
            runOnUiThread {
                //binding.myTextView.text = hexString  // 假设你有一个名为 myTextView 的 TextView
            }
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(
                    "Bluetooth",
                    "Read characteristic ${characteristic.uuid}: ${value.contentToString()}"
                )
            } else {
                Log.e(
                    "Bluetooth",
                    "Failed to read characteristic ${characteristic.uuid} with status: $status"
                )
            }
        }
    }

    // 启用特征值的通知
    private fun setCharacteristicNotification(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        enable: Boolean
    ) {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        gatt.setCharacteristicNotification(characteristic, enable)

        // 找到 Client Characteristic Configuration Descriptor
        val descriptor = characteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG)
        if (descriptor != null) {
            descriptor.value =
                if (enable) BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE else BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
            gatt.writeDescriptor(descriptor)
            Log.d("Bluetooth Descripter", "$descriptor.value")
        }
    }

    // 将字节数组转换为十六进制字符串
    private fun bytesToHex(bytes: ByteArray): String {
        val hexArray = "0123456789ABCDEF".toCharArray()
        val hexChars = CharArray(bytes.size * 2)
        for (j in bytes.indices) {
            val v = bytes[j].toInt() and 0xFF
            hexChars[j * 2] = hexArray[v ushr 4]
            hexChars[j * 2 + 1] = hexArray[v and 0x0F]
        }
        return String(hexChars)
    }


    fun disconnectGattServer() {
        Log.d("Bluetooth Closing", "Closing Gatt connection")
        if (bluetoothGatt != null) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            bluetoothGatt?.disconnect()
            bluetoothGatt?.close()
            bluetoothGatt = null
        }
        isConnecting = false
        startScan()
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

                Log.d("MainActivity###", "connecting......") // 添加日志
                bluetoothGatt = device.connectGatt(this, false, gattCallback)
                Log.d("BLE Connecting to GATT server", "Connecting to GATT server: ${device.address}")

            } catch (e: IOException) {
                Log.e("Bluetooth", "连接设备失败: ${e.message}")
                //e.printStackTrace()
            //    bluetoothSocket?.close()
            //   bluetoothSocket = null

                //startScan()
            }finally {
                isConnecting = false // Set the flag to false in the finally block
            }
        }.start()
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            // Log.d("Bluetooth ScanResult", "onScanResult() called")
            val device: BluetoothDevice = result.device
            if(device.name != null && device.name.startsWith("V3")) {

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