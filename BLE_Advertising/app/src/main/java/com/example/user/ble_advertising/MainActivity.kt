package com.example.user.ble_advertising

import android.Manifest
import android.app.AlertDialog
import android.app.Dialog
import android.support.v7.app.AppCompatActivity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.os.Bundle
import com.example.user.ble_advertising.R
import android.content.pm.PackageManager
import android.bluetooth.BluetoothManager
import android.bluetooth.le.*
import android.content.Context
import android.os.Build
import android.support.annotation.RequiresApi
import com.example.user.ble_advertising.MainActivity
import android.util.SparseArray
import android.support.v4.app.ActivityCompat
import android.content.DialogInterface
import android.os.ParcelUuid
import android.content.Intent
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStreamWriter
import java.lang.Exception
import java.lang.StringBuilder
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.ArrayList

class MainActivity() : AppCompatActivity() {
    private var mBluetoothAdapter: BluetoothAdapter? = null
    private var mBluetoothLeScanner: BluetoothLeScanner? = null
    private var isScanning = false
    private var mScanCallback: ScanCallback? = null
    private val mBluetoothDevices = ArrayList<BluetoothDevice>()
    private var scanResultList: ArrayList<String>? = null
    private val mHandler = Handler()
    private var mBluetoothLeAdvertiser: BluetoothLeAdvertiser? = null
    private var isAdvertising = false
    private var mAdvertiseCallback: AdvertiseCallback? = null
    private var dialogPermission: Dialog? = null
    private val REQUEST_ENABLE_BT = 1
    private var textView_info: TextView? = null
    private var listView_scanResult: ListView? = null
    private var editText_data: TextView? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        checkBluetoothLowEnergyFeature()
        initBluetoothService()
        initScanAndAdvertiseCallback()
        initUI()
    }

    private fun checkBluetoothLowEnergyFeature() {
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun initBluetoothService() {
        val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        mBluetoothAdapter = bluetoothManager.adapter
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.bt_not_supported, Toast.LENGTH_SHORT).show()
            finish()
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!mBluetoothAdapter!!.isLe2MPhySupported()) {
                Toast.makeText(this, "!mBluetoothAdapter.isLe2MPhySupported()", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    private fun initScanAndAdvertiseCallback() {
        mScanCallback = object : ScanCallback() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                super.onScanResult(callbackType, result)
                mBluetoothDevices.clear()
                scanResultList!!.clear()
                saveScanResult(result)
                //TODO: OnScanResult
                setAndUpdateListView()
                val device = result.device
                mBluetoothDevices.add(device)
                //                listAdapter.notifyDataSetChanged();
            }

            override fun onBatchScanResults(results: List<ScanResult>) {
                super.onBatchScanResults(results)
                mBluetoothDevices.clear()
                scanResultList!!.clear()
                for (result: ScanResult in results) {
                    saveScanResult(result)
                }
                setAndUpdateListView()
            }

            override fun onScanFailed(errorCode: Int) {
                super.onScanFailed(errorCode)
                Toast.makeText(
                    this@MainActivity, "Error scanning devices: $errorCode", Toast.LENGTH_LONG
                ).show()
            }
        }
        mAdvertiseCallback = object : AdvertiseCallback() {
            override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
                super.onStartSuccess(settingsInEffect)
            }

            override fun onStartFailure(errorCode: Int) {
                super.onStartFailure(errorCode)
            }
        }
    }

    private fun saveScanResult(result: ScanResult) {
        if (result.scanRecord != null && hasManufacturerData(result.scanRecord)) {
            var tempValue = unpackPayload(
                result.scanRecord!!
                    .getManufacturerSpecificData(GENERAL)
            )
            tempValue = tempValue.substring(1)
            if (!mBluetoothDevices.contains(result.device)) {
//                Log.d("predicted distance", String.valueOf(predict));
//                Log.d("TXPower" , String.valueOf(result.getTxPower()));
//                Log.d("getRSSI " , String.valueOf(result.getRssi()));
//                Log.d("y", String.valueOf(y));
//                Log.d("distance " , distance + "M");
//                Log.d("RSSI value", String.valueOf(result.getRssi()));
//                Log.d("Log-Distance Path" , String.valueOf(d.getCalculatedDistance()));
//                Log.d("ITU Model" , String.valueOf(itu_model))
                Log.d("Device Name:", result.scanRecord!!.deviceName.toString())
                //                int count = 0;
//                count++;
                //add to arraylist
                mBluetoothDevices.add(result.device)
                scanResultList!!.add(
                    (result.scanRecord!!.deviceName + ","
                            + result.rssi + ","
                            + tempValue + ","
                            + "50")
                )

//                for(int i = 0; i< scanResultList!!.size(); i++){
//                    nodes.add(scanResultList);
//                }
//
//
//                Log.d("scanResultList" , String.valueOf(nodes));
                writeToFile(scanResultList!!, this);
//                Log.d("write to file: ", "success");
            }
//            if( result.scanRecord!!.deviceName.toString() == "nRFBeacon"){
//                Log.d("beacon received", result.scanRecord!!.deviceName.toString())
//            }
        }
    }

    private fun hasManufacturerData(record: ScanRecord?): Boolean {
        val data = record!!.manufacturerSpecificData
        return (data != null && data[GENERAL] != null)
    }

    private fun unpackPayload(data: ByteArray?): String {
        val buffer = ByteBuffer.wrap(data)
            .order(ByteOrder.LITTLE_ENDIAN)
        buffer.get()
        val b = ByteArray(buffer.limit())
        for (i in 0 until buffer.limit()) {
            b[i] = buffer[i]
        }
        try {
            return (String(b, Charsets.UTF_8))
        } catch (e: Exception) {
            return " Unable to unpack."
        }
    }

    private fun setAndUpdateListView() {
        val listAdapter: ListAdapter = ArrayAdapter(
            baseContext, android.R.layout.simple_expandable_list_item_1, scanResultList?: emptyList(),
        )
        listView_scanResult!!.adapter = listAdapter
    }

    private fun initUI() {
        textView_info = findViewById(R.id.textView_info)
        listView_scanResult = findViewById(R.id.listView_scanResult)
        scanResultList = ArrayList()
        setAndUpdateListView()
        editText_data = findViewById(R.id.editText_data)
    }

    override fun onResume() {
        super.onResume()
        if (Build.VERSION.SDK_INT >= 23) {
            checkPermission()
        } else {
            checkBluetoothEnableThenScanAndAdvertising()
        }
    }

    private fun checkPermission() {
        val permission = ActivityCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        )
        if (permission == PackageManager.PERMISSION_GRANTED) {
            checkBluetoothEnableThenScanAndAdvertising()
        } else {
            showDialogForPermission()
        }
    }

    private fun showDialogForPermission() {
        val dialogBuilder = AlertDialog.Builder(this@MainActivity)
        dialogBuilder.setTitle(resources.getString(R.string.dialog_permission_title))
        dialogBuilder.setMessage(resources.getString(R.string.dialog_permission_message))
        dialogBuilder.setPositiveButton(resources.getString(R.string.dialog_permission_ok),
            DialogInterface.OnClickListener { dialogInterface, i ->
                ActivityCompat.requestPermissions(
                    this@MainActivity,
                    PERMISSIONS_ACCESS,
                    REQUEST_ACCESS_FINE_LOCATION
                )
            })
        dialogBuilder.setNegativeButton(
            resources.getString(R.string.dialog_permission_no),
            object : DialogInterface.OnClickListener {
                override fun onClick(dialogInterface: DialogInterface, i: Int) {
                    Toast.makeText(
                        this@MainActivity,
                        resources.getString(R.string.dialog_permission_toast_negative),
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
        if (dialogPermission == null) {
            dialogPermission = dialogBuilder.create()
        }
        if (!dialogPermission!!.isShowing) {
            dialogPermission!!.show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_ACCESS_FINE_LOCATION -> {
                if ((grantResults.size > 0
                            && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                ) {
                    checkBluetoothEnableThenScanAndAdvertising()
                } else {
                    Toast.makeText(
                        this@MainActivity,
                        resources.getString(R.string.dialog_permission_toast_negative),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun checkBluetoothEnableThenScanAndAdvertising() {
        if (mBluetoothAdapter!!.isEnabled) {
            startScanLeDevice()
            startAdvertising()
        } else {
            openBluetoothSetting()
        }
    }

    private fun startScanLeDevice() {
        if (isScanning) {
            return
        }
        mHandler.postDelayed(object : Runnable {
            override fun run() {
                stopScanLeDevice()
            }
        }, SCAN_PERIOD)
        isScanning = true
        mBluetoothLeScanner = mBluetoothAdapter!!.bluetoothLeScanner
        var reportDelay = 0
        if (mBluetoothAdapter!!.isOffloadedScanBatchingSupported) {
            reportDelay = 1000
        }
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY) // SCAN LATENCY
            .setReportDelay(reportDelay.toLong())
            .build()
        mBluetoothLeScanner!!.startScan(null, settings, mScanCallback)
        textView_info!!.text = resources.getString(R.string.bt_scanning)
    }

    private fun stopScanLeDevice() {
        if (isScanning) {
            mBluetoothLeScanner!!.stopScan(mScanCallback)
            isScanning = false
            textView_info!!.text = resources.getString(R.string.bt_stop_scan)
        }
    }

    //    private void setScanFilter(){
    //        ScanFilter filter = new ScanFilter.Builder()
    //                .setServiceUuid(ParcelUuid.fromString("0000b81d-0000-1000-8000-00805f9b34fb"))
    //                .setDeviceName("fromScan")
    //                .build();
    //        filters.add(filter);
    //
    //    }
    //    private List<ScanFilter> buildScanFilters() {
    //        List<ScanFilter> scanFilters = new ArrayList<>();
    //
    //        ScanFilter.Builder builder = new ScanFilter.Builder();
    //        // Comment out the below line to see all BLE devices around you
    //        builder.setServiceUuid(ParcelUuid.fromString("0000b81d-0000-1000-8000-00805f9b34fb"));
    //        scanFilters.add(builder.build());
    //
    //        return scanFilters;
    //    }
    private fun startAdvertising() {
        if (isAdvertising) {
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!mBluetoothAdapter!!.isLe2MPhySupported) {
                isAdvertising = true
                mBluetoothLeAdvertiser = mBluetoothAdapter!!.bluetoothLeAdvertiser
                val settings = AdvertiseSettings.Builder()
                    .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY) //ADVERTISING FREQUENCY
                    .setConnectable(false)
                    .setTimeout(0)
                    .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH) //ADJUST DISTANCE
                    .build()
                val advertiseData = AdvertiseData.Builder()
                    .addManufacturerData(
                        GENERAL,
                        buildPayload(editText_data!!.text.toString())
                    ) // maximum 24 bytes if alone else 22 bytes
                    .addServiceUuid(ParcelUuid.fromString("0000b81d-0000-1000-8000-00805f9b34fb")) //0xb81d random service uuid
                    .setIncludeDeviceName(false)
                    .build()
                val scanResponse =
                    AdvertiseData.Builder() //                        .addManufacturerData(GENERAL, buildPayload())
                        .setIncludeDeviceName(true) // TODO: Changed to false 3 Jan
                        .build()
                mBluetoothLeAdvertiser!!.startAdvertising(
                    settings,
                    advertiseData,
                    scanResponse,
                    mAdvertiseCallback
                )
            } else {
                isAdvertising = true
                mBluetoothLeAdvertiser = mBluetoothAdapter!!.bluetoothLeAdvertiser
                val settings = AdvertiseSettings.Builder()
                    .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY) //ADVERTISING FREQUENCY
                    .setConnectable(false)
                    .setTimeout(0)
                    .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH) //ADJUST DISTANCE
                    .build()
                val advertiseData = AdvertiseData.Builder()
                    .addManufacturerData(
                        GENERAL,
                        buildPayload(editText_data!!.text.toString())
                    ) // maximum 24 bytes if alone else 22 bytes
                    .addServiceUuid(ParcelUuid.fromString("0000b81d-0000-1000-8000-00805f9b34fb")) //0xb81d random service uuid
                    .setIncludeDeviceName(false)
                    .build()
                val scanResponse =
                    AdvertiseData.Builder() //                        .addManufacturerData(GENERAL, buildPayload("Scan BT 5.0"))
                        .setIncludeDeviceName(true) // TODO: Changed to false 3 Jan
                        .build()
                mBluetoothLeAdvertiser!!.startAdvertising(
                    settings,
                    advertiseData,
                    scanResponse,
                    mAdvertiseCallback
                )
            }
        }
    }

    private fun buildPayload(value: String): ByteArray {
        val flags = 0x8000000.toByte()
        var b = byteArrayOf()
        try {
            b = value.toByteArray(charset("UTF-8"))
        } catch (e: Exception) {
            return b
        }
        val max = 26
        val capacity: Int
        if (b.size <= max) {
            capacity = b.size + 1
        } else {
            capacity = max + 1
            System.arraycopy(b, 0, b, 0, max)
        }
        val output: ByteArray
        output = ByteBuffer.allocate(capacity)
            .order(ByteOrder.LITTLE_ENDIAN) //GATT APIs expect LE order
            .put(flags) //Add the flags byte
            .put(b)
            .array()
        return output
    }

    private fun openBluetoothSetting() {
        val bluetoothSettingIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        startActivityForResult(bluetoothSettingIntent, REQUEST_ENABLE_BT)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_ENABLE_BT) {
            checkBluetoothEnableThenScanAndAdvertising()
        }
    }

    fun btnClick(v: View) {
        when (v.id) {
            R.id.button_scan -> startScanLeDevice()
            R.id.button_stop -> stopScanLeDevice()
            R.id.button_save -> stopAndRestartAdvertising()
        }
    }

    private fun stopAndRestartAdvertising() {
        if (isAdvertising) {
            mBluetoothLeAdvertiser = mBluetoothAdapter!!.bluetoothLeAdvertiser
            mBluetoothLeAdvertiser!!.stopAdvertising(mAdvertiseCallback)
            isAdvertising = false
        }
        startAdvertising()
    }

    private fun writeToFile(scanResultList: ArrayList<String>, context: Context) {
        val str = StringBuilder("")
        for (eachstring: String in scanResultList) {
            str.append(eachstring).append(",")
        }
        var commaseparatedlist = str.toString()
        if (commaseparatedlist.length > 0) {
            commaseparatedlist = commaseparatedlist.substring(0, commaseparatedlist.length - 1)
        }
        //Log.d("write to file", commaseparatedlist);
        try {
            val outputStreamWriter =
                OutputStreamWriter(openFileOutput("config_100cm_2.txt", MODE_APPEND))
            outputStreamWriter.write(commaseparatedlist)
            outputStreamWriter.close()
        } catch (e: IOException) {
            Log.e("Exception", "File write failed: $e")
        }
        try {
            val path = context.filesDir
            val file = File(path, "test.txt")
            val stream = FileOutputStream(file)
            try {
                stream.write(commaseparatedlist.toByteArray())
            } finally {
                stream.close()
            }
        } catch (e: IOException) {
            Log.e("Exception", "File write failed: $e")
        }
    }

    companion object {
        val GENERAL =
            0xFFFF // reserved id that can be used for testing purposes; you cannot ship any product with 0xFFFF set as the manufacturer id.
        private val SCAN_PERIOD: Long = 999999999
        private val PERMISSIONS_ACCESS = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        private val REQUEST_ACCESS_FINE_LOCATION = 1
    }
}